package qingning.user.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.util.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.IUserModuleServer;
import qingning.user.server.other.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

import java.text.DecimalFormat;
import java.util.*;

public class UserServerImpl extends AbstractQNLiveServer {

    private IUserModuleServer userModuleServer;

    private ReadLiveRoomOperation readLiveRoomOperation;
    private ReadCourseOperation readCourseOperation;
    private ReadUserOperation readUserOperation;
    private ReadRoomDistributer readRoomDistributer;
    private ReadLecturerOperation readLecturerOperation;
    private ReadRoomDistributerOperation readRoomDistributerOperation;
    
    @Override
    public void initRpcServer() {
        if (userModuleServer == null) {
            userModuleServer = this.getRpcService("userModuleServer");

            readLiveRoomOperation = new ReadLiveRoomOperation(userModuleServer);
            readCourseOperation = new ReadCourseOperation(userModuleServer);            
            readUserOperation = new ReadUserOperation(userModuleServer);
            readRoomDistributer = new ReadRoomDistributer(userModuleServer);
            readLecturerOperation = new ReadLecturerOperation(userModuleServer);
            readRoomDistributerOperation = new ReadRoomDistributerOperation(userModuleServer);
        }
    }

    @SuppressWarnings("unchecked")
    @FunctionName("userFollowRoom")
    public Map<String, Object> userFollowRoom(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        //1.更新数据库中关注表的状态
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<>();
        String roomid = (String)reqMap.get("room_id");
        map.put(Constants.FIELD_ROOM_ID, roomid);
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        //查询直播间是否存在
        if (!jedis.exists(roomKey)) {           
        	CacheUtils.readLiveRoom(roomid, this.generateRequestEntity(null, null, null, map), readLiveRoomOperation, jedisUtils, true);
        	if (!jedis.exists(roomKey)) {
        		throw new QNLiveException("100002");
        	}
        }
        String lecturerId = jedis.hget(roomKey, "lecturer_id");
        reqMap.put("lecturer_id", lecturerId);

        if(MiscUtils.isEqual(lecturerId, userId)){
        	throw new QNLiveException("110004");
        }
        
        Map<String, Object> dbResultMap = userModuleServer.userFollowRoom(reqMap);
        if (dbResultMap == null || dbResultMap.get("update_count") == null || "0".equals(String.valueOf(dbResultMap.get("update_count")))) {
        	if("1".equals(reqMap.get("follow_type"))){
        		throw new QNLiveException("110006");
        	} else {
        		throw new QNLiveException("110003");
        	}
        }



        //4.更新用户缓存中直播间的关注数
        //关注操作类型 0关注 1不关注
        Integer incrementNum = null;
        if (reqMap.get("follow_type").toString().equals("0")) {
            incrementNum = 1;
        } else {
            incrementNum = -1;
        }

        //5.更新用户信息中的关注直播间数，更新直播间缓存的粉丝数
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String userCacheKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, map);
        if(jedis.exists(userCacheKey)){
            jedis.hincrBy(userCacheKey, "live_room_num", incrementNum);
        }else {
            CacheUtils.readUser(userId, reqEntity, readUserOperation,jedisUtils);
            jedis.hincrBy(userCacheKey, "live_room_num", incrementNum);
        }

        jedis.hincrBy(roomKey, "fans_num", incrementNum);

        //6.更新讲师缓存的粉丝数
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "fans_num", incrementNum);
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, lecturerId);
        jedis.sadd(Constants.CACHED_UPDATE_USER_KEY, userId);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("userCourses")
    public Map<String, Object> getCourses(RequestEntity reqEntity) throws Exception {        
        Map<String, Object> values = getPlatformCourses(reqEntity);
        if(!MiscUtils.isEmpty(values)){
        	final List<Map<String,Object>> courseList = (List<Map<String,Object>>)values.get("course_list");
        	if(!MiscUtils.isEmpty(courseList)){
        		((JedisBatchCallback)(this.jedisUtils.getJedis())).invoke(new JedisBatchOperation(){
					@Override
					public void batchOperation(Pipeline pipeline, Jedis jedis) {
						Map<String,Response<String>> nickNames = new HashMap<String,Response<String>>();
						Map<String,String> query = new HashMap<String,String>();
						for(Map<String,Object> course:courseList){
							String lecturer_id = (String)course.get("lecturer_id");
							if(!nickNames.containsKey(lecturer_id)){
								query.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
								String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query);
								nickNames.put(lecturer_id, pipeline.hget(key, "nick_name"));
							}							
						}
						pipeline.sync();
						for(Map<String,Object> course:courseList){
							course.put("lecturer_nick_name", nickNames.get((String)course.get("lecturer_id")).get());
						}
					}
        		});
        	}
        }
        return values;
    }

    /**
     * 查询平台所有的课程
     *
     * @param reqEntity
     * @return
     */
    @SuppressWarnings({ "unchecked"})
    private Map<String, Object> getPlatformCourses(RequestEntity reqEntity) throws Exception {
    	Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();

    	String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
    	reqMap.put("user_id", userId);
    	String status = (String)reqMap.get("status");
    	String course_id = (String)reqMap.get("course_id");
    	String data_source = (String)reqMap.get("data_source");
    	//String start_time_str = (String)reqMap.get("start_time");
    	//long start_time= MiscUtils.convertObjectToLong(reqMap.get("start_time")); 
    	Long query_time = (Long)reqMap.get("query_time");
    	Long position = (Long)reqMap.get("position");
    	long currentTime = System.currentTimeMillis();
    	//进一步参数校验，传递了分页使用的课程id，则需要同时传递课程状态和数据来源信息
    	if(StringUtils.isNotBlank(reqMap.get("course_id").toString())){
    		if(MiscUtils.isEmpty(status) || MiscUtils.isEmpty(data_source) || query_time==null || position==null){
    			throw new QNLiveException("120004");
    		}            
    	} else {
    		status="4";
    		data_source = "1";
    	}

    	int pageCount = Integer.parseInt(reqMap.get("page_count").toString());        
    	Jedis jedis = jedisUtils.getJedis();

    	Set<Tuple> dictionList = null;
    	String startIndexFinish = "+inf";//("+start_time_str;
    	if(query_time!=null && position!=null){
    		startIndexFinish="("+MiscUtils.convertInfoToPostion(query_time, position);
    	}
    	String endIndexPrediction = "-inf";
    	List<String> courseList = new LinkedList<String>();
    	List<Map<String,String>> courseDetailsList = new LinkedList<Map<String,String>>();
    	Map<String,String> lastCourse = null;

    	if(!"2".equals(data_source)){
    		
    		Set<String> courseSet = new HashSet<String>();        
    		boolean removePrediction=true;
    		if(MiscUtils.isEmpty(course_id) || "4".equals(status)){        	
    			if(query_time == null){ 
    				startIndexFinish = MiscUtils.convertInfoToPostion(currentTime, 0)+"";
    			}
    			dictionList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, startIndexFinish, endIndexPrediction, 0, pageCount);

    			if(!MiscUtils.isEmpty(dictionList)){
    				pageCount=pageCount-dictionList.size();
    				for(Tuple tuple:dictionList){
    					courseList.add(tuple.getElement());
    					courseSet.add(tuple.getElement());
    				}
    				removePrediction=false;
    			}
    		}
    		Set<Tuple> predictionList = null;
    		if(pageCount>0){
    			if(query_time == null){ 
    				startIndexFinish = "+inf";
    			} else {  
    				if("4".equals(status)){
    					startIndexFinish = "+inf";
    				} else {
    					startIndexFinish="("+MiscUtils.convertInfoToPostion(query_time, position);
    				}
    				
    			}
    			predictionList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, startIndexFinish, endIndexPrediction, 0, pageCount);
    			if(!MiscUtils.isEmpty(predictionList)){
    				for(Tuple tuple:predictionList){
    					if(courseSet.contains(tuple.getElement())){
    						continue;
    					}
    					courseList.add(tuple.getElement());
    					courseSet.add(tuple.getElement());
    				}
    			}
    		}

    		Map<String,Map<String,String>> cachedCourse = new HashMap<String,Map<String,String>>();
    		int count = courseList.size();
    		if(count > 0){
    			courseSet.clear();
    			Map<String,String> queryParam = new HashMap<String,String>();
    			RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
    			for(int i = 0; i < count; ++i){
    				String courseId = courseList.get(i);
    				queryParam.put("course_id", courseId);
    				Map<String, String> courseInfoMap = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedisUtils, true);
    				MiscUtils.courseTranferState(currentTime, courseInfoMap);
    				cachedCourse.put(courseId, courseInfoMap);
    				if(removePrediction && "4".equals(courseInfoMap.get("status"))){
    					courseSet.add(courseId);
    				} 
    				long endDate = MiscUtils.convertObjectToLong(courseInfoMap.get("end_date"));
    				if("2".equals(courseInfoMap.get("status")) && endDate> 0 ){
    					jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION,courseId);
    					if(jedis.zscore(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, course_id) == null){
    						jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, MiscUtils.convertInfoToPostion(endDate, MiscUtils.convertObjectToLong(courseInfoMap.get("position"))), courseId);
    					}
    					courseSet.add(courseId);
    				}
    			}

    			for(String courseId:courseSet){
    				courseList.remove(courseId);
    			}

    			courseSet.clear();
    			for(String courseId:courseList){
    				courseSet.add(courseId);
    			}
    		}        

    		pageCount = Integer.parseInt(reqMap.get("page_count").toString()) - courseList.size();
    		Set<Tuple> finishList = null;
    		Set<String> finishSet = new HashSet<String>();
    		if(pageCount>0){
    			if(query_time==null){ 
    				startIndexFinish = "+inf";
    			} else {
    				startIndexFinish = "("+MiscUtils.convertInfoToPostion(query_time, position);
    			}
    			finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexPrediction, 0, pageCount);
    			if(!MiscUtils.isEmpty(finishList)){
    				for(Tuple tuple:finishList){
    					String courseId = tuple.getElement();
    					if(courseSet.contains(courseId)){
    						courseList.remove(courseId);
    					}
    					courseList.add(courseId);
    					finishSet.add(courseId);
    				}
    			}
    		}        

    		if(!MiscUtils.isEmpty(courseList)){
    			Map<String,String> queryParam = new HashMap<String,String>();
    			RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
    			for(String courseId:courseList){
    				Map<String, String> courseInfoMap = cachedCourse.get(courseId);
    				if(courseInfoMap == null){
    					queryParam.put("course_id", courseId);
    					courseInfoMap = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedisUtils, true);
    				}
    				if(finishSet.contains(courseId) && !"2".equals(courseInfoMap.get("status"))){
    					queryParam.put("course_id", courseId);
    					String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, queryParam);
    					jedis.hset(courseKey, "status", "2");
    				}
    				courseInfoMap.put("data_source", "1");
    				courseDetailsList.add(courseInfoMap);
    				lastCourse=courseInfoMap;
    			}
    		}
    	}
    	pageCount = Integer.parseInt(reqMap.get("page_count").toString()) - courseList.size();
    	if(pageCount>0){
    		Map<String, Object> queryMap = new HashMap<>();
    		queryMap.put("orderType", "2");
    		queryMap.put("status", "2");
    		queryMap.put("pageCount", pageCount);
    		if(!MiscUtils.isEmpty(lastCourse)){
    			long lastCourseEnd = MiscUtils.convertObjectToLong(lastCourse.get("end_time"));
    			if(query_time ==null || lastCourseEnd<query_time){
    				query_time=lastCourseEnd;
    				position = MiscUtils.convertObjectToLong(lastCourse.get("position"));
    			}
    		}
    		if(query_time != null){
    			queryMap.put("position", MiscUtils.convertInfoToPostion(query_time, position));    			
    		}

    		List<Map<String, Object>> list = userModuleServer.findFinishCourseListForLecturer(queryMap);
    		if(!MiscUtils.isEmpty(list)){
    			for(Map<String, Object> course:list){
    				Map<String, String> courseStr = new HashMap<String,String>();
    				MiscUtils.converObjectMapToStringMap(course, courseStr);
    				courseStr.put("data_source", "2");
    				courseDetailsList.add(courseStr);
    			}        		
    		}
    	}
        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_USER_FIELD, userId);
        RequestEntity queryOperation = generateRequestEntity(null, null, null, query);
        CacheUtils.readUser(userId, queryOperation, readUserOperation, jedisUtils);

        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);
        for(Map<String,String> course:courseDetailsList){
            String courseId = course.get("course_id");
            if(jedis.sismember(key, courseId)){
                course.put("student", "Y");
            } else {
                course.put("student", "N");
            }
            if("2".equals(course.get(course.get("status")))){
                course.put("query_time", course.get("end_time"));
            } else {
                course.put("query_time", course.get("start_time"));
            }
        }

    	Map<String, Object> resultMap = new HashMap<String, Object>();        
    	resultMap.put("course_list", courseDetailsList);        
    	return resultMap;
    }

/*    *//**
     * 查询平台所有的课程
     *
     * @param reqEntity
     * @return
     *//*
    @SuppressWarnings({ "unchecked", "unused" })
    private Map<String, Object> getPlatformCoursesOld(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        //进一步参数校验，传递了分页使用的课程id，则需要同时传递课程状态和数据来源信息
        if(StringUtils.isNotBlank(reqMap.get("course_id").toString())){
            if(StringUtils.isBlank(reqMap.get("status").toString()) || StringUtils.isBlank(reqMap.get("data_source").toString()) || StringUtils.isBlank(reqMap.get("start_time").toString())){
                throw new QNLiveException("120004");
            }
        }

        //1.如果课程id为空，则进行初始化查询，仅仅查询缓存即可
        Jedis jedis = jedisUtils.getJedis();
        if (reqMap.get("course_id") == null || StringUtils.isBlank(reqMap.get("course_id").toString())) {

            List<Map<String, String>> courseResultList = new ArrayList<>();
            int pageCount = Integer.parseInt(reqMap.get("page_count").toString());

            //1.1先查询直播列表
            Set<Tuple> predictionList ;
            Set<Tuple> finishList = null;
            List<Map<String, Object>> dbList = new ArrayList<>();

            String startIndexPrediction = "-inf";
            String endIndexPrediction = "+inf";
            predictionList = jedis.zrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, startIndexPrediction, endIndexPrediction, 0, pageCount);

            //1.3预告列表为空或者不足，则继续查询结束列表
            if (predictionList == null || predictionList.size() < pageCount) {
                if (predictionList != null) {
                    pageCount = pageCount - predictionList.size();
                }

                String startIndexFinish = "+inf";
                String endIndexFinish = "-inf";

                finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexFinish, 0, pageCount);

                if(finishList == null || finishList.size() < pageCount){
                    if(finishList != null){
                        pageCount = pageCount - finishList.size();
                    }
                    //直接查询数据库中的结束课程
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "2");
                    queryMap.put("status", "2");
                    queryMap.put("pageCount", pageCount);
                    if(finishList != null && finishList.size() > 0){
                        Date queryDate = new Date(Long.parseLong(findLastElementForRedisSet(finishList).get("startIndexDB")));
                        queryMap.put("startIndex", queryDate);
                    }
                    dbList = userModuleServer.findCourseListForLecturer(queryMap);
                }
            }

            long currentTime = System.currentTimeMillis();

            if (predictionList != null) {
                for (Tuple tuple : predictionList) {
                    ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                    Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                    if(MiscUtils.isEmpty(courseInfoMap)){
                        continue;
                    }
                    courseInfoMap.put("data_source", "1");
                    MiscUtils.courseTranferState(currentTime, courseInfoMap);
                    courseResultList.add(courseInfoMap);
                }
            }

            if (finishList != null) {
                for (Tuple tuple : finishList) {
                    ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                    Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                    if(courseInfoMap != null){
                        courseInfoMap.put("data_source", "1");
                        MiscUtils.courseTranferState(currentTime, courseInfoMap);
                        courseResultList.add(courseInfoMap);
                    }
                }
            }

            if (dbList != null) {
                for (Map<String, Object> courseDBMap : dbList) {
                    Map<String, String> courseDBMapString = new HashMap<>();
                    MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                    courseDBMapString.put("data_source", "2");
                    MiscUtils.courseTranferState(currentTime, courseDBMapString);
                    courseResultList.add(courseDBMapString);
                }
            }

            if (!CollectionUtils.isEmpty(courseResultList)) {
                resultMap.put("course_list", courseResultList);
            }
            return resultMap;

        } else {
            //2.如果课程id不为空，则进行分页查询
            //数据来源：1：缓存 2.：数据库
            List<Map<String, String>> courseResultList = new ArrayList<>();

            //从缓存中查询
            if (reqMap.get("data_source").toString().equals("1")) {

                Set<Tuple> predictionList = null;
                Set<Tuple> finishList = null;
                List<Map<String, Object>> dbList = new ArrayList<>();
                Map<String,Object> queryResultMap = new HashMap<>();

                int pageCount = Integer.parseInt(reqMap.get("page_count").toString());

                //课程状态 1:预告（对应为数据库中的已发布） 2:已结束 4:直播中
                if (reqMap.get("status").toString().equals("1") || reqMap.get("status").toString().equals("4")) {
                    queryResultMap = findCoursesStartWithPrediction(jedis, reqMap.get("start_time").toString(), "+inf", pageCount);

                } else {
                    String startIndexFinish =  reqMap.get("start_time").toString();
                    String endIndexFinish = "-inf";
                    finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexFinish, 0, pageCount);

                    if(finishList == null || finishList.size() < pageCount){
                        Map<String, Object> queryMap = new HashMap<>();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "2");
                        queryMap.put("pageCount", pageCount);
                        dbList = userModuleServer.findCourseListForLecturer(queryMap);
                    }
                }


                if(! CollectionUtils.isEmpty(queryResultMap)){
                    if(queryResultMap.get("predictionList") != null){
                        predictionList = (Set<Tuple>)queryResultMap.get("predictionList");
                    }

                    if(queryResultMap.get("finishList") != null){
                        finishList = (Set<Tuple>)queryResultMap.get("finishList");
                    }

                    if(queryResultMap.get("dbList") != null){
                        dbList = (List<Map<String,Object>>)queryResultMap.get("dbList");
                    }
                }


                long currentTime = System.currentTimeMillis();

                if (predictionList != null) {
                    for (Tuple tuple : predictionList) {
                        ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                        Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                        courseInfoMap.put("data_source", "1");
                        MiscUtils.courseTranferState(currentTime, courseInfoMap);
                        courseResultList.add(courseInfoMap);
                    }
                }

                if (finishList != null) {
                    for (Tuple tuple : finishList) {
                        ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                        Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                        courseInfoMap.put("data_source", "1");
                        MiscUtils.courseTranferState(currentTime, courseInfoMap);
                        courseResultList.add(courseInfoMap);
                    }
                }

                if (dbList != null) {
                    for (Map<String, Object> courseDBMap : dbList) {
                        Map<String, String> courseDBMapString = new HashMap<>();
                        MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                        courseDBMapString.put("data_source", "2");
                        MiscUtils.courseTranferState(currentTime, courseDBMapString);
                        courseResultList.add(courseDBMapString);
                    }
                }

                if (!CollectionUtils.isEmpty(courseResultList)) {
                    resultMap.put("course_list", courseResultList);
                }


                return resultMap;


                //从数据库中查询
            } else {
                long currentTime = System.currentTimeMillis();
                List<Map<String, Object>> dbList = new ArrayList<>();
                List<Map<String, Object>> dbFinishList = new ArrayList<>();
                int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
                //预告中
                if(reqMap.get("status").equals("1")){
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "1");
                    queryMap.put("status", "1");
                    queryMap.put("pageCount", pageCount);
                    Date start_time = new Date(Long.parseLong(reqMap.get("start_time").toString()));
                    queryMap.put("startIndex", start_time);
                    dbList = userModuleServer.findCourseListForLecturer(queryMap);

                    //如果预告列表不足，则查询结束列表
                    if(dbList == null || dbList.size() < pageCount){
                        Date finish_list_start_time = null;
                        if(dbList != null){
                            pageCount = pageCount - dbList.size();
                            Map<String,Object> lastCourse = dbList.get(dbList.size() - 1);
                            finish_list_start_time = (Date)lastCourse.get("start_time");
                        }
                        queryMap.clear();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "2");
                        queryMap.put("pageCount", pageCount);
                        queryMap.put("startIndex", finish_list_start_time);
                        dbFinishList = userModuleServer.findCourseListForLecturer(queryMap);
                    }
                }else {
                    //已经结束
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "2");
                    queryMap.put("status", "2");
                    queryMap.put("pageCount", pageCount);
                    Date start_time = new Date(Long.parseLong(reqMap.get("start_time").toString()));
                    queryMap.put("startIndex", start_time);
                    dbList = userModuleServer.findCourseListForLecturer(queryMap);
                }

                if(! CollectionUtils.isEmpty(dbList)){
                    for (Map<String, Object> courseDBMap : dbList) {
                        Map<String, String> courseDBMapString = new HashMap<>();
                        MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                        courseDBMapString.put("data_source", "2");
                        MiscUtils.courseTranferState(currentTime, courseDBMapString);
                        courseResultList.add(courseDBMapString);
                    }
                }

                if (!CollectionUtils.isEmpty(courseResultList)) {
                    resultMap.put("course_list", courseResultList);
                }


                return resultMap;

            }

        }

    }*/

    private Map<String, Object> findCoursesStartWithPrediction(Jedis jedis, String startIndex, String endIndex, Integer pageCount) {
        Map<String, Object> resultMap = new HashMap<>();
        Set<Tuple> predictionList = null;
        Set<Tuple> finishList = null;
        List<Map<String, Object>> dbList = new ArrayList<>();
        List<Map<String, Object>> finishDbList = new ArrayList<>();
        List<Map<String, Object>> predictionListDbList = new ArrayList<>();

        String startIndexPrediction = startIndex;
        String endIndexPrediction = endIndex;
        predictionList = jedis.zrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, startIndexPrediction, endIndexPrediction, 0, pageCount);

        //1.3预告列表为空或者不足，则继续查询结束列表
        if (predictionList == null || predictionList.size() < pageCount) {
            if (predictionList != null) {
                pageCount = pageCount - predictionList.size();
            }

            if (predictionList == null) {
                if (jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)) {
                    //读结束列表，结束列表不足，则读取数据库,直接查询数据库中结束状态的课程
                    String startIndexFinish = "+inf";
                    String endIndexFinish = "-inf";
                    finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexFinish, 0, pageCount);

                    if (finishList.size() < pageCount) {
                        Map<String, Object> queryMap = new HashMap<>();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "2");
                        queryMap.put("pageCount", pageCount - finishList.size());
                        dbList = userModuleServer.findCourseListForLecturer(queryMap);
                    }
                } else {
                    //读取数据库，直接查询结束状态课程
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "2");
                    queryMap.put("status", "2");
                    queryMap.put("pageCount", pageCount);
                    dbList = userModuleServer.findCourseListForLecturer(queryMap);
                }
            } else {
                if (jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)) {
                    //读结束列表，结束列表不足，则读取数据库,直接查询数据库中结束状态的课程
                    String startIndexFinish = "+inf";
                    String endIndexFinish = "-inf";
                    finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexFinish, 0, pageCount);

                    if (finishList.size() < pageCount) {
                        Map<String, Object> queryMap = new HashMap<>();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "2");
                        queryMap.put("pageCount", pageCount - finishList.size());
                        dbList = userModuleServer.findCourseListForLecturer(queryMap);
                    }

                } else {
                    //查询数据库，先读未结束的课程，再读取已经结束的课程
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "1");
                    queryMap.put("status", "1");
                    queryMap.put("pageCount", pageCount);
                    predictionListDbList = userModuleServer.findCourseListForLecturer(queryMap);

                    if (CollectionUtils.isEmpty(dbList) || dbList.size() < pageCount) {
                        if (!CollectionUtils.isEmpty(dbList)) {
                            pageCount = pageCount - dbList.size();
                        }

                        queryMap.clear();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "1");
                        queryMap.put("pageCount", pageCount);
                        finishDbList = userModuleServer.findCourseListForLecturer(queryMap);

                        if (!CollectionUtils.isEmpty(finishDbList)) {
                            if (CollectionUtils.isEmpty(predictionListDbList)) {
                                dbList = finishDbList;
                            } else {
                                dbList.addAll(predictionListDbList);
                                dbList.addAll(finishDbList);
                            }
                        }
                    }
                }
            }
        }

        resultMap.put("predictionList", predictionList);
        resultMap.put("finishList", finishList);
        resultMap.put("dbList", dbList);
        return resultMap;

    }


    /**
     * 查询直播间基本信息
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("roomInfo")
    public Map<String, Object> getRoomInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        //查询出直播间基本信息
        Map<String, String> infoMap = CacheUtils.readLiveRoom(reqMap.get("room_id").toString(), reqEntity, readLiveRoomOperation, jedisUtils, true);
        if (CollectionUtils.isEmpty(infoMap)) {
            throw new QNLiveException("100002");
        }

        //查询关注信息 //TODO 先查询数据库，后续确认是否查询缓存
        //关注状态 0未关注 1已关注
        Map<String, Object> fansMap = userModuleServer.findFansByUserIdAndRoomId(reqMap);
        if (CollectionUtils.isEmpty(fansMap)) {
            resultMap.put("follow_status", "0");
        } else {
            resultMap.put("follow_status", "1");
        }

        resultMap.put("avatar_address", infoMap.get("avatar_address"));
        resultMap.put("room_name", infoMap.get("room_name"));
        resultMap.put("room_remark", infoMap.get("room_remark"));
        resultMap.put("fans_num", infoMap.get("fans_num"));

        //返回用户身份
        List<String> roles = new ArrayList<>();
        //1：普通用户、3：讲师 、4:分销员
        //先查询是否为讲师
        if(infoMap.get("lecturer_id").equals(userId)){
            roles.add("3");
            //不为讲师查询是否为分销员
        }else {
            Map<String,Object> queryMap = new HashMap<>();
            queryMap.put("distributer_id", userId);
            queryMap.put("room_id", reqMap.get("room_id").toString());
            Map<String,String> roomDistributer = CacheUtils.readDistributerRoom(userId, (String)reqMap.get("room_id"), readRoomDistributer, jedisUtils);
            
            if (! MiscUtils.isEmpty(roomDistributer)) {
               roles.add("4");
                resultMap.put("rq_code",roomDistributer.get("rq_code"));
            }else {
               roles.add("1");
            }
        }

        resultMap.put("roles",roles);
        return resultMap;
    }

    private String getCourseShareURL(String userId, String courseId, Map<String,String> courseMap) throws Exception{
        String share_url ;
        String roomId = courseMap.get("room_id");
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("distributer_id", userId);
        queryMap.put("room_id", roomId);
        Map<String,String> distributerRoom = CacheUtils.readDistributerRoom(userId, roomId, readRoomDistributerOperation, jedisUtils);

        boolean isDistributer = false;
        String recommend_code = null;
        if (! MiscUtils.isEmpty(distributerRoom)) {
            isDistributer = true;
            recommend_code = distributerRoom.get("rq_code");
        }

        //是分销员
        if(isDistributer == true){
            share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix") + courseId + "&recommend_code=" + recommend_code;
        }else {
            //不是分销员
            share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix") + courseId;
        }

        return share_url;
    }


    /**
     * 查询课程详情
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("courseDetailInfo")
    public Map<String, Object> getCourseDetailInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, String> courseMap = new HashMap<String, String>();
        String room_id = null;
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        //1.先从缓存中查询课程详情，如果有则从缓存中读取课程详情
        if (jedis.exists(courseKey)) {
            Map<String, String> courseCacheMap = jedis.hgetAll(courseKey);
            courseMap = courseCacheMap;
            room_id = courseCacheMap.get("room_id").toString();

        } else {
            //2.如果缓存中没有课程详情，则读取数据库
            Map<String, Object> courseDBMap = userModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());

            //3.如果缓存和数据库中都没有课程详情，则提示课程不存在
            if (CollectionUtils.isEmpty(courseDBMap)) {
                throw new QNLiveException("100004");
            } else {
                MiscUtils.converObjectMapToStringMap(courseDBMap, courseMap);
                room_id = courseDBMap.get("room_id").toString();
            }
        }
        if (CollectionUtils.isEmpty(courseMap)) {
            throw new QNLiveException("100004");
        }
        for(String key:courseMap.keySet()){
        	resultMap.put(key, courseMap.get(key));
        }

        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("size",10);
        queryMap.put("course_id",reqMap.get("course_id").toString());        
        resultMap.put("student_list", userModuleServer.findLatestStudentAvatarAddList(queryMap));
        //从缓存中获取直播间信息
        Map<String, String> liveRoomMap = CacheUtils.readLiveRoom(room_id, reqEntity, readLiveRoomOperation, jedisUtils, true);
        resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));
        resultMap.put("room_id", liveRoomMap.get("room_id"));

        queryMap.clear();
        queryMap.put("user_id", userId);
        queryMap.put("course_id", courseMap.get("course_id"));
        //判断访问者是普通用户还是讲师
        //如果为讲师，则返回讲师部分特定信息
        boolean isStudent = false;
        List<String> roles = new ArrayList<>();
        if(userId.equals(courseMap.get("lecturer_id"))){
            resultMap.put("update_time", courseMap.get("update_time"));
            resultMap.put("course_password", courseMap.get("course_password"));
            resultMap.put("im_course_id", courseMap.get("im_course_id"));
            resultMap.put("share_url",getCourseShareURL(userId, reqMap.get("course_id").toString(), courseMap));
        }else {
            //为用户，则返回用户部分信息
            isStudent = userModuleServer.isStudentOfTheCourse(queryMap);

            //返回用户身份
            //角色数组 1：普通用户、2：学员、3：讲师
            //加入课程状态 0未加入 1已加入
            if(isStudent){
                resultMap.put("join_status", "1");
            }else {
                resultMap.put("join_status", "0");
            }

            //查询关注状态
            //关注状态 0未关注 1已关注
            reqMap.put("room_id", liveRoomMap.get("room_id"));
            Map<String, Object> fansMap = userModuleServer.findFansByUserIdAndRoomId(reqMap);
            if (CollectionUtils.isEmpty(fansMap)) {
                resultMap.put("follow_status", "0");
            } else {
                resultMap.put("follow_status", "1");
            }

        }

        if(userId.equals(courseMap.get("lecturer_id"))){
            roles.add("3");
        }else {
            if(isStudent){
                roles.add("2");
            }else {
                roles.add("1");
            }
        }

        Map<String,String> roomDistributer = CacheUtils.readDistributerRoom(userId, courseMap.get("room_id"), readRoomDistributerOperation, jedisUtils);
        if(! MiscUtils.isEmpty(roomDistributer)){
            if(roomDistributer.get("end_date") != null){
                Date endDate = new Date(Long.parseLong(roomDistributer.get("end_date")));
                Date todayEndDate = MiscUtils.getEndDateOfToday();
                if(endDate.getTime() >= todayEndDate.getTime()){
                    roles.add("4");
                }
            }
        }
        resultMap.put("roles",roles);

        return resultMap;
    }

    /**
     * 逻辑roomCourses,courseList类似，注意重构同步
     * */
    @SuppressWarnings("unchecked")
    @FunctionName("roomCourses")
    public Map<String, Object> getRoomCourses(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam(); 
        //目前一个讲师仅能创建一个直播间，所以查询的是该讲师发布的课程
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String lecturerId = jedis.hget(roomKey, "lecturer_id");
        if(MiscUtils.isEmpty(lecturerId)){
        	throw new QNLiveException("120018");
        }
    	
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
        
        String course_id = (String)reqMap.get("course_id");
        int pageCount = (int)reqMap.get("page_count");
        Long query_time = (Long)reqMap.get("query_time");
        Long position = (Long)reqMap.get("position");
        
        long currentTime = System.currentTimeMillis();
        
        List<Map<String,String>> courseList = new LinkedList<Map<String,String>>();        
        String startIndexFinish = "+inf";
        String endIndexPrediction = "-inf";
        
        Set<Tuple> dictionList = null;
        boolean checkDiction = MiscUtils.isEmpty(course_id) || (jedis.zscore(lecturerCoursesPredictionKey, course_id) != null);
        boolean checkPrediction = checkDiction;
        if(query_time!=null && query_time>currentTime){
        	checkDiction=false;
        }
        List<String> courseIdList = new LinkedList<String>();
        Set<String> courseIdSet = new HashSet<String>();
        if(checkDiction){        	
        	if(MiscUtils.isEmpty(course_id)){
        		startIndexFinish = currentTime+"";        		
        	} else if(query_time!=null && query_time <= currentTime){
        		startIndexFinish = "("+query_time;
        	}
        	dictionList = jedis.zrevrangeByScoreWithScores(lecturerCoursesPredictionKey, startIndexFinish, endIndexPrediction, 0, pageCount);
        	if(dictionList != null){
        		pageCount = pageCount - dictionList.size();
        		for(Tuple tuple : dictionList){
        			String courseId = tuple.getElement();
        			courseIdList.add(courseId);
        			courseIdSet.add(courseId);
        		}
        	}
        }
        
        Map<String,Map<String,String>> cachedCourse = new HashMap<String,Map<String,String>>();
        
        Set<Tuple> preDictionSet = null;
        List<String> preDictionList = new LinkedList<String>();
        if(pageCount>0 && checkPrediction){
        	startIndexFinish = "+inf";
        	if(query_time!=null && query_time>=currentTime){
        		startIndexFinish="("+query_time;
        	}
        	preDictionSet = jedis.zrevrangeByScoreWithScores(lecturerCoursesPredictionKey, startIndexFinish, endIndexPrediction, 0, pageCount);
        	if(preDictionList != null){
        		Map<String,String> queryParam = new HashMap<String,String>();
        		RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
        		for(Tuple tuple : preDictionSet){
        			String courseId = tuple.getElement();
        			queryParam.put("course_id", courseId);
        			Map<String, String> courseInfoMap = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedisUtils, true);
        			MiscUtils.courseTranferState(currentTime, courseInfoMap);
        			cachedCourse.put(courseId, courseInfoMap);
        			String status = (String)courseInfoMap.get("status");
        			if(!"4".equals(status)){
        				if(courseIdSet.contains(courseId)){
        					courseIdList.remove(courseId);
        				}
        				if("2".equals(status)){
        					jedis.zrem(lecturerCoursesPredictionKey, courseId);
        					long lpos = 0;
        					if(courseInfoMap.get("end_time")==null){
        						//jedis.zadd(lecturerCoursesFinishKey, MiscUtils.convertObjectToLong(courseInfoMap.get("start_time")),courseId);
        						lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(courseInfoMap.get("start_time")), MiscUtils.convertObjectToLong(courseInfoMap.get("position")));
        					} else {
        						//jedis.zadd(lecturerCoursesFinishKey, MiscUtils.convertObjectToLong(courseInfoMap.get("end_time")),courseId);
        						lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(courseInfoMap.get("end_time")), MiscUtils.convertObjectToLong(courseInfoMap.get("position")));
        					}
        					jedis.zadd(lecturerCoursesFinishKey, lpos,courseId);
        					continue;
        				}
        				courseIdList.add(courseId);
        				courseIdSet.add(courseId);
        			}
        		}
        	}        	
        }
        boolean finExist = false;
        Set<Tuple> finishDictionSet = null;
        pageCount=((int)reqMap.get("page_count"))-courseIdList.size();
        if(pageCount>0){
        	if(MiscUtils.isEmpty(course_id) || query_time == null){
        		startIndexFinish = "+inf";
        	} else {        		
        		startIndexFinish = "("+ MiscUtils.convertInfoToPostion(query_time, position);
        	}
        	finishDictionSet = jedis.zrevrangeByScoreWithScores(lecturerCoursesFinishKey, startIndexFinish, endIndexPrediction, 0, pageCount);
        	if(!MiscUtils.isEmpty(finishDictionSet)){
        		for(Tuple tuple : finishDictionSet){
        			String courseId = tuple.getElement();
        			if(courseIdSet.contains(courseId)){
        				courseIdList.remove(courseId);
        			}
        			courseIdList.add(courseId);
        			finExist=true;
        		}        		
        	}
        }
        pageCount=((int)reqMap.get("page_count"))-courseIdList.size();
        Map<String,String> lastCourse = null;
        if(!MiscUtils.isEmpty(courseIdList)){
			Map<String,String> queryParam = new HashMap<String,String>();
			RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
            for(String courseId:courseIdList){
            	Map<String,String> courseInfoMap = cachedCourse.get(courseId);
            	queryParam.put("course_id", courseId);
            	if(courseInfoMap==null){
            		courseInfoMap = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedisUtils, true);
            	}
            	MiscUtils.courseTranferState(currentTime, courseInfoMap);
            	courseList.add(courseInfoMap);
            	lastCourse = courseInfoMap;
            }
        }

        
        if(pageCount > 0){
        	map.clear();
            map.put("pageCount", pageCount);
            map.put("lecturer_id", lecturerId);
            Long queryTime = null; 
            if(!MiscUtils.isEmpty(lastCourse)){
                if(finExist){
                	if(lastCourse.get("end_time")==null){
                		queryTime = Long.parseLong(lastCourse.get("start_time"));
                	} else {
                		queryTime = Long.parseLong(lastCourse.get("end_time"));
                	}
                	position = MiscUtils.convertObjectToLong(lastCourse.get("position"));
                } else {
                    queryTime = Long.parseLong(lastCourse.get("start_time"));
                    position = MiscUtils.convertObjectToLong(lastCourse.get("position"));
                }

            } else {
            	queryTime=(Long)reqMap.get("query_time");
            	position=(Long)reqMap.get("position");
            }
            if(queryTime != null){                
                map.put("position", MiscUtils.convertInfoToPostion(queryTime, position));
            }
            List<Map<String,Object>> finishCourse = userModuleServer.findFinishCourseListForLecturer(map);
            if(!MiscUtils.isEmpty(finishCourse)){               
                for(Map<String,Object> finish:finishCourse){
                    if(MiscUtils.isEqual(course_id, finish.get("course_id"))){
                        continue;
                    }
                    Map<String,String> finishMap = new HashMap<String,String>();
                    MiscUtils.converObjectMapToStringMap(finish, finishMap);
                    courseList.add(finishMap);
                }
            }
        }
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());   
        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_USER_FIELD, userId);
        RequestEntity queryOperation = generateRequestEntity(null, null, null, query);
        CacheUtils.readUser(userId, queryOperation, readUserOperation, jedisUtils);
		
		String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);
		
        for(Map<String,String> course:courseList){
        	String courseId = course.get("course_id");
        	if(jedis.sismember(key, courseId)){
        		course.put("student", "Y");
        	} else {
        		course.put("student", "N");
        	}
        	if("2".equals(course.get("status"))){
        		if(course.get("end_time")==null){
        			course.put("query_time", course.get("start_time"));
        		} else {
        			course.put("query_time", course.get("end_time"));
        		}
        	} else {
        		course.put("query_time", course.get("start_time"));
        	}
        }
        
        resultMap.put("course_list", courseList);
        
        return resultMap;
    }

    Map<String, Object> findCourseFinishList(Jedis jedis, String key,
                                             String startIndexCache, String startIndexDB, String endIndex, Integer limit, Integer count) {
        Set<Tuple> finishList = jedis.zrevrangeByScoreWithScores(key, startIndexCache, endIndex, limit, count);
        Map<String, Object> queryMap = new HashMap<>();
        List<Map<String, Object>> dbList = null;

        //如果结束课程列表为空，则查询数据库
        if (finishList == null || finishList.isEmpty()) {
            queryMap.put("pageCount", count);
            if (startIndexDB != null) {
                Date date = new Date(Long.parseLong(startIndexDB.substring(1)));
                queryMap.put("startIndex", date);
            }
            dbList = userModuleServer.findCourseListForLecturer(queryMap);
        } else {
            //如果结束课程列表中的数量不够，则剩余需要查询数据库
            if (finishList.size() < count) {
                startIndexDB = findLastElementForRedisSet(finishList).get("startIndexDB");
                queryMap.put("pageCount", count - finishList.size());
                if (startIndexDB != null) {
                    Date date = new Date(Long.parseLong(startIndexDB));
                    queryMap.put("startIndex", date);
                }
                dbList = userModuleServer.findCourseListForLecturer(queryMap);
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("finishList", finishList);
        resultMap.put("dbList", dbList);
        return resultMap;
    }

    Map<String, String> findLastElementForRedisSet(Set<Tuple> redisSet) {
        Map<String, String> resultMap = new HashMap<>();
        String startIndexCache = null;
        String startIndexDB = null;
        DecimalFormat decimalFormat = new DecimalFormat("#");
        for (Tuple tuple : redisSet) {
            startIndexCache = "(" + decimalFormat.format(tuple.getScore());
            startIndexDB = decimalFormat.format(tuple.getScore()) + "";
        }
        resultMap.put("startIndexCache", startIndexCache);
        resultMap.put("startIndexDB", startIndexDB);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("joinCourse")
    public Map<String, Object> joinCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        //1.1读取课程信息
        String course_id = (String)reqMap.get("course_id");
        map.put("course_id", course_id);
        Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedisUtils, false);
        if(MiscUtils.isEmpty(courseInfoMap)){
        	throw new QNLiveException("100004");
        }
        
        //1.2检测该用户是否为讲师，为讲师则不能加入该课程 
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        if(userId.equals(courseInfoMap.get("lecturer_id"))){
            throw new QNLiveException("100017");
        }
        
        //2.检测课程验证信息是否正确
        //2.1如果课程为私密课程则检验密码
        String course_type = courseInfoMap.get("course_type"); 
        if("1".equals(course_type)){
            if(reqMap.get("course_password") == null || StringUtils.isBlank(reqMap.get("course_password").toString())){
                throw new QNLiveException("000100");
            }
            if(! reqMap.get("course_password").toString().equals(courseInfoMap.get("course_password").toString())){
                throw new QNLiveException("120006");
            }
        }else if("2".equals(course_type)){
            //TODO 支付课程要验证支付信息
            if(reqMap.get("payment_id") == null){
                throw new QNLiveException("000100");
            }
        }

        //3.检测学生是否参与了该课程
        Map<String,Object> studentQueryMap = new HashMap<>();
        studentQueryMap.put("user_id",userId);
        studentQueryMap.put("course_id",course_id);
        if(userModuleServer.isStudentOfTheCourse(studentQueryMap)){
            throw new QNLiveException("100004");
        }

        //5.将学员信息插入到学员参与表中
        courseInfoMap.put("user_id",userId);
        Map<String,Object> insertResultMap = userModuleServer.joinCourse(courseInfoMap);
		String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, courseInfoMap);
		jedis.del(key);
        //6.修改讲师缓存中的课程参与人数
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseInfoMap.get("lecturer_id"));
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "total_student_num", 1);
        if("2".equals(course_type)){
        	jedis.hincrBy(lecturerKey, "pay_student_num", 1);
        }
        map.clear();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        Long nowStudentNum = 0L;
        if(jedis.exists(courseKey)){
            //jedis.hincrBy(courseKey, "student_num", 1);
        	map.clear();
        	map.put("course_id", course_id);
        	Map<String,Object> numInfo = userModuleServer.findCourseRecommendUserNum(map);
        	long num = 0;
        	if(!MiscUtils.isEmpty(numInfo)){
        		num=MiscUtils.convertObjectToLong(numInfo.get("recommend_num"));
        	}
        	long lastNum = MiscUtils.convertObjectToLong(jedis.hget(courseKey, "student_num"));
        	if(lastNum<num){
        		jedis.hset(courseKey, "student_num", num+"");
        	}
        }else {
            userModuleServer.increaseStudentNumByCourseId(reqMap.get("course_id").toString());
        }

        //7.修改用户缓存信息中的加入课程数
        map.clear();
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String userCacheKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, map);
        if(jedis.exists(userCacheKey)){
            jedis.hincrBy(userCacheKey, "course_num", 1L);
        }else {
            CacheUtils.readUser(userId, reqEntity, readUserOperation,jedisUtils);
            jedis.hincrBy(userCacheKey, "course_num", 1L);
        }

        nowStudentNum = MiscUtils.convertObjectToLong(courseInfoMap.get("student_num")) + 1;
        String levelString = MiscUtils.getConfigByKey("jpush_course_students_arrive_level");
        JSONArray levelJson = JSON.parseArray(levelString);
        if(levelJson.contains(nowStudentNum+"")){
            JSONObject obj = new JSONObject();
            //String course_type = courseMap.get("course_type");
            String course_type_content = MiscUtils.convertCourseTypeToContent(course_type);
            obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content"), course_type_content, MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")), nowStudentNum+""));
            obj.put("to", userId);
            obj.put("msg_type","7");
            Map<String,String> extrasMap = new HashMap<>();
            extrasMap.put("msg_type","7");
            extrasMap.put("course_id",courseInfoMap.get("course_id"));
            obj.put("extras_map", extrasMap);
            JPushHelper.push(obj);
        }
        //course_type 0:公开课程 1:加密课程 2:收费课程',
        //TODO 加入课程推送   收费课程支付成功才推送消息
        if (!"2".equals(courseInfoMap.get("course_type"))) {
        	//获取讲师的信息
        	map.clear();
        	map.put("lecturer_id", courseInfoMap.get("lecturer_id"));
        	Map<String, String> user = CacheUtils.readLecturer(courseInfoMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, map), readLecturerOperation, jedisUtils);
        	
    		Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
    		TemplateData first = new TemplateData();
    		first.setColor("#000000");
			String firstContent = String.format(MiscUtils.getConfigByKey("wpush_shop_course_first"), MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")));
    		first.setValue(firstContent);
    		templateMap.put("first", first);

			TemplateData courseTitle = new TemplateData();
			courseTitle.setColor("#000000");
			courseTitle.setValue(MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")));
			templateMap.put("keyword1", courseTitle);

    		Date start_time = new Date(Long.parseLong(courseInfoMap.get("start_time")));
    		TemplateData orderNo = new TemplateData();
    		orderNo.setColor("#000000");
    		orderNo.setValue(MiscUtils.parseDateToFotmatString(start_time, "yyyy-MM-dd HH:mm:ss"));
    		templateMap.put("keyword2", orderNo);

			String lastContent;
			lastContent = MiscUtils.getConfigByKey("wpush_shop_course_lecturer_name") + MiscUtils.RecoveryEmoji(user.get("nick_name"));
			String thirdContent = MiscUtils.RecoveryEmoji(courseInfoMap.get("course_remark"));
			if(! MiscUtils.isEmpty(thirdContent)){
				lastContent += "\n" + MiscUtils.getConfigByKey("wpush_shop_course_brief") + thirdContent;
			}
			lastContent += "\n" +MiscUtils.getConfigByKey("wpush_shop_course_remark");

            Map<String,Object> studentUserMap = userModuleServer.findLoginInfoByUserId(userId);
    		TemplateData remark = new TemplateData();
    		remark.setColor("#000000");
    		remark.setValue(lastContent);
    		templateMap.put("remark", remark);
			String url = String.format(MiscUtils.getConfigByKey("course_live_room_url"), courseInfoMap.get("course_id"),  courseInfoMap.get("room_id"));
    		WeiXinUtil.send_template_message((String) studentUserMap.get("web_openid"), MiscUtils.getConfigByKey("wpush_shop_course"),url, templateMap, jedis);
		}
		jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, courseInfoMap.get("lecturer_id").toString());
        return resultMap;
    }



    @SuppressWarnings("unchecked")
    @FunctionName("courseInfo")
    public Map<String, Object> getCourseInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

        Map<String,String> courseMap = new HashMap<>();
        //1.先检查该课程是否在缓存中
        if(jedis.exists(courseKey)){

            //任意课程都可以免费试听五分钟，不进行是否为该课程学生的校验
            //检测学员是否加入了该课程，加入课程才能查询相关信息，如果没加入课程则提示学员未加入该课程（120007）
//            Map<String,Object> queryStudentMap = new HashMap<>();
//            queryStudentMap.put("user_id", userId);
//            queryStudentMap.put("course_id",reqMap.get("course_id").toString());
//            if(!userModuleServer.isStudentOfTheCourse(queryStudentMap)){
//                throw new QNLiveException("120007");
//            }

            courseMap = jedis.hgetAll(courseKey);

            JSONArray pptList = null;
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String pptListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
            if(jedis.exists(pptListKey)){
                pptList = JSONObject.parseArray(jedis.get(pptListKey));
            }

            List<Map<String,String>> audioObjectMapList = new ArrayList<>();
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
            String audioJsonStringKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, map);
            Set<String> audioIdList = jedis.zrange(audioListKey, 0 , -1);

            //如果存在zsort列表，则从zsort列表中读取
            if(audioIdList != null && audioIdList.size() > 0){
                JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
                callBack.invoke(new JedisBatchOperation(){
                    @Override
                    public void batchOperation(Pipeline pipeline, Jedis jedis) {

                        List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
                        for(String audio : audioIdList){
                            map.put(Constants.FIELD_AUDIO_ID, audio);
                            String audioKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
                            redisResponseList.add(pipeline.hgetAll(audioKey));
                        }
                        pipeline.sync();

                        for(Response<Map<String, String>> redisResponse : redisResponseList){
                            Map<String,String> messageStringMap = redisResponse.get();
                            audioObjectMapList.add(messageStringMap);
                        }
                    }
                });

                resultMap.put("audio_list", audioObjectMapList);

                //如果存在讲课音频的json字符串，则读取讲课音频json字符串
            } else if(jedis.exists(audioJsonStringKey)){
                resultMap.put("audio_list", JSONObject.parse(jedis.get(audioJsonStringKey)));
            }

            if(! CollectionUtils.isEmpty(pptList)){
                resultMap.put("ppt_list", pptList);
            }

            resultMap.put("im_course_id", jedis.hget(courseKey, "im_course_id"));

            //判断该课程状态，如果为直播中，则检查开播时间是否存在，如果开播时间存在，
            //则检查当前查询是否大于当前时间，如果大于，则查询用户是否存在于在线map中，
            //如果不存在，则将该学员加入的在线map中，并且修改课程缓存real_student_num实际课程人数(默认课程人数)
            String realStudentNum = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_REAL_STUDENT_NUM, map);
            boolean hasStudent = jedis.hexists(realStudentNum, userId);
            if(hasStudent == false){
                if(courseMap.get("status").equals("4")){
                    if(courseMap.get("real_start_time") != null){
                        Long real_start_time = Long.parseLong(courseMap.get("real_start_time"));
                        long now = System.currentTimeMillis();
                        if(now > real_start_time){
                            jedis.hset(realStudentNum, userId, "1");
                            jedis.hincrBy(courseKey, "real_student_num", 1);
                        }
                    }
                }
            }

        }else{
            //2.如果不在缓存中，则查询数据库
            Map<String,Object> courseInfoMap = userModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            MiscUtils.converObjectMapToStringMap(courseInfoMap, courseMap);
            if(courseInfoMap == null){
                throw new QNLiveException("100004");
            }

            //任意课程都可以免费试听五分钟，不进行是否为该课程学生的校验
//            Map<String,Object> queryStudentMap = new HashMap<>();
//            queryStudentMap.put("user_id", userId);
//            queryStudentMap.put("course_id",reqMap.get("course_id").toString());
//            if(!userModuleServer.isStudentOfTheCourse(queryStudentMap)){
//                throw new QNLiveException("120007");
//            }

            //查询课程PPT列表
            List<Map<String,Object>> pptList = userModuleServer.findPPTListByCourseId(reqMap.get("course_id").toString());

            //查询课程语音列表
            List<Map<String,Object>> audioList = userModuleServer.findAudioListByCourseId(reqMap.get("course_id").toString());

            if(! CollectionUtils.isEmpty(pptList)){
                resultMap.put("ppt_list", pptList);
            }

            if(! CollectionUtils.isEmpty(audioList)){
                resultMap.put("audio_list", audioList);
            }

            resultMap.put("im_course_id",  courseInfoMap.get("im_course_id"));

        }

        //4.将学生加入该课程的IM群组
        try {
            //检查学生上次加入课程，如果加入课程不为空，则退出上次课程
            Map<String,Object> studentUserMap = userModuleServer.findLoginInfoByUserId(userId);
            map.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String courseIMKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_LAST_JOIN_COURSE_IM_INFO, map);
            String imCourseId = jedis.get(courseIMKey);
            if(! MiscUtils.isEmpty(imCourseId)){
                IMMsgUtil.delGroupMember(imCourseId, studentUserMap.get("m_user_id").toString(), studentUserMap.get("m_user_id").toString());
            }

            //加入新课程IM群组，并且将加入的群组记录入缓存中
            IMMsgUtil.joinGroup(courseMap.get("im_course_id"), studentUserMap.get("m_user_id").toString(),studentUserMap.get("m_user_id").toString());
            jedis.set(courseIMKey, courseMap.get("im_course_id"));
        }catch (Exception e){
            //TODO 暂时不处理
        }

        map.clear();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
        Double banScore = jedis.zscore(banKey, userId);
        if(banScore == null){
            resultMap.put("ban_status", "0");
        }else {
            resultMap.put("ban_status", "1");
        }

        //增加返回课程相应信息
        resultMap.put("student_num",courseMap.get("student_num"));
        resultMap.put("start_time",courseMap.get("start_time"));
        resultMap.put("status",courseMap.get("status"));
        resultMap.put("course_type",courseMap.get("course_type"));
        resultMap.put("course_password",courseMap.get("course_password"));
        resultMap.put("share_url",MiscUtils.getConfigByKey("course_share_url_pre_fix")+reqMap.get("course_id").toString());//TODO
        resultMap.put("course_update_time",courseMap.get("update_time"));
        resultMap.put("course_title",courseMap.get("course_title"));
        Map<String,Object> userMap = userModuleServer.findUserInfoByUserId(courseMap.get("lecturer_id"));
        resultMap.put("lecturer_nick_name",userMap.get("nick_name"));
        resultMap.put("lecturer_avatar_address",userMap.get("avatar_address"));
        resultMap.put("course_url",courseMap.get("course_url"));

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("messageList")
    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();

        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        String queryType = reqMap.get("query_type").toString();

        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String messageListKey;
        Map<String, Object> queryMap = new HashMap<>();

        //queryType为0则查询全部消息，为1则查询提问
        if(queryType.equals("0")){
            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
        }else {
            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
            queryMap.put("send_type_query", "0");//send_type_query 查询出 类型:0:讲师讲解 1：讲师回答 两种类型的聊天记录
        }

        //缓存不存在则读取数据库中的内容
        if(! jedis.exists(messageListKey)){
            queryMap.put("page_count", pageCount);
            if(reqMap.get("message_pos") != null && StringUtils.isNotBlank(reqMap.get("message_pos").toString())){
                queryMap.put("message_pos", Long.parseLong(reqMap.get("message_pos").toString()));
            }else {
                Map<String,Object> maxInfoMap = userModuleServer.findCourseMessageMaxPos(reqMap.get("course_id").toString());
                if(MiscUtils.isEmpty(maxInfoMap)){
                    return resultMap;
                }
                Long maxPos = (Long)maxInfoMap.get("message_pos");
                queryMap.put("message_pos", maxPos);
            }
            queryMap.put("course_id", reqMap.get("course_id").toString());
            List<Map<String,Object>> messageList = userModuleServer.findCourseMessageList(queryMap);

            if(! CollectionUtils.isEmpty(messageList)){
				for(Map<String,Object> messageMap : messageList){
					if(! MiscUtils.isEmpty(messageMap.get("message"))){
						messageMap.put("message",MiscUtils.RecoveryEmoji(messageMap.get("message").toString()));
					}
					if(! MiscUtils.isEmpty(messageMap.get("message_question"))){
						messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
					}
				}
                resultMap.put("message_list", messageList);
            }

            return resultMap;

        }else {
            //缓存中存在，则读取缓存中的内容
            //初始化下标
            long startIndex;
            long endIndex;
            Set<String> messageIdList;
            //如果分页的message_id不为空
            if(reqMap.get("message_id") != null && StringUtils.isNotBlank(reqMap.get("message_id").toString())){
                long endRank = jedis.zrank(messageListKey, reqMap.get("message_id").toString());
                endIndex = endRank - 1;
                //判断该列表向上再无信息，如果再无信息，则直接将查询结果列表设置为空
                if(endIndex < 0){
                    startIndex = 0;
                    messageIdList = null;
                }else {
                    startIndex = endIndex - pageCount + 1;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                    messageIdList = jedis.zrange(messageListKey, startIndex, endIndex);
                }

            }else {
                endIndex = -1;
                startIndex = jedis.zcard(messageListKey) - pageCount;
                if(startIndex < 0){
                    startIndex = 0;
                }
                messageIdList = jedis.zrange(messageListKey, startIndex, endIndex);
            }

            if(! CollectionUtils.isEmpty(messageIdList)){
                //缓存中存在则读取缓存内容
                List<Map<String,String>> messageListCache = new ArrayList<>();
                for(String messageId : messageIdList){
                    map.put(Constants.FIELD_MESSAGE_ID, messageId);
                    String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
                    Map<String,String> messageMap = jedis.hgetAll(messageKey);
					if(MiscUtils.isEmpty(messageMap)){
						continue;
					}
					if(messageMap.get("creator_id") != null){
						Map<String,Object> innerMap = new HashMap<>();
						innerMap.put("user_id", messageMap.get("creator_id"));
						Map<String,String> userMap = CacheUtils.readUser(messageMap.get("creator_id"), this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedisUtils);
						if(! MiscUtils.isEmpty(userMap)){
							if(userMap.get("nick_name") != null){
								messageMap.put("creator_nick_name", MiscUtils.RecoveryEmoji(userMap.get("nick_name")));
							}
							if(userMap.get("avatar_address") != null){
								messageMap.put("creator_avatar_address", userMap.get("avatar_address"));
							}
						}
					}

					String messageContent = messageMap.get("message");
					if(! MiscUtils.isEmpty(messageContent)){
						messageMap.put("message",MiscUtils.RecoveryEmoji(messageContent));
					}

					if(! MiscUtils.isEmpty(messageMap.get("message_question"))){
						messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
					}
                    messageMap.put("message_pos", startIndex+"");
                    messageListCache.add(messageMap);
                    startIndex++;
                }

                resultMap.put("message_list", messageListCache);
            }

            return resultMap;
        }

    }

    @SuppressWarnings("unchecked")
    @FunctionName("courseStudents")
    public Map<String, Object> getCourseStudentList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("page_count", Integer.parseInt(reqMap.get("page_count").toString()));
        if(reqMap.get("student_pos") != null && StringUtils.isNotBlank(reqMap.get("student_pos").toString())){
            queryMap.put("student_pos", Long.parseLong(reqMap.get("student_pos").toString()));
        }
        queryMap.put("course_id", reqMap.get("course_id").toString());
        List<Map<String,Object>> messageList = userModuleServer.findCourseStudentList(queryMap);

        if(! CollectionUtils.isEmpty(messageList)){
            resultMap.put("student_list", messageList);
        }

        Map<String,String> liveRoomMap = CacheUtils.readCourse(reqMap.get("course_id").toString(),reqEntity,readCourseOperation,jedisUtils,true);
        resultMap.put("student_num",liveRoomMap.get("student_num"));
        return resultMap;

    }

    @SuppressWarnings("unchecked")
    @FunctionName("getUserConsumeRecords")
    public Map<String, Object> getUserConsumeRecords(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> queryMap = new HashMap<>();

        //获得用户id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        queryMap.put("user_id", userId);
        //根据page_count、position（如果有）查询数据库
        queryMap.put("page_count", Integer.parseInt(reqMap.get("page_count").toString()));
        if(reqMap.get("position") != null && StringUtils.isNotBlank(reqMap.get("position").toString())){
            queryMap.put("position", Long.parseLong(reqMap.get("position").toString()));
        }

        List<Map<String,Object>> records = userModuleServer.findUserConsumeRecords(queryMap);

        if(! CollectionUtils.isEmpty(records)){
            Map<String,Object> cacheQueryMap = new HashMap<>();

            JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
            //从缓存中查询讲师的名字
            callBack.invoke(new JedisBatchOperation(){
                @Override
                public void batchOperation(Pipeline pipeline, Jedis jedis) {
                    for(Map<String,Object> recordMap : records){
                        cacheQueryMap.put(Constants.CACHED_KEY_LECTURER_FIELD, recordMap.get("lecturer_id"));
                        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, cacheQueryMap);
                        Response<String> cacheLecturerName = pipeline.hget(lecturerKey, "nick_name");
                        recordMap.put("cacheLecturerName",cacheLecturerName);
                    }
                    pipeline.sync();

                    for(Map<String,Object> recordMap : records){
                        Response<String> cacheLecturerName = (Response)recordMap.get("cacheLecturerName");
                        recordMap.put("lecturer_name",cacheLecturerName.get());
                        recordMap.remove("cacheLecturerName");
                        Date recordTime = (Date)recordMap.get("create_time");
                        recordMap.put("create_time", recordTime);
                    }
                }
            });

            resultMap.put("record_list", records);
        }

        return resultMap;

    }
	    
    @FunctionName("noticeRooms")
    public  Map<String, Object> getNoticeRooms(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
		Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);
        final List<Map<String,Object>> list = userModuleServer.findFanInfoByUserId(reqMap);
        resultMap.put("live_room_list", list);        
        if(!MiscUtils.isEmpty(list)){
    		((JedisBatchCallback)this.jedisUtils.getJedis()).invoke(new JedisBatchOperation(){
    			@Override
    			public void batchOperation(Pipeline pipeline, Jedis jedis) {
    				Map<String,Response<Set<Tuple>>> courseInfoDictMap = new HashMap<String,Response<Set<Tuple>>>();
    				Map<String,Response<Set<Tuple>>> courseInfoPreDictMap = new HashMap<String,Response<Set<Tuple>>>();
    				Map<String,Response<Set<Tuple>>> courseInfoFinDictMap = new HashMap<String,Response<Set<Tuple>>>();
    				long currentTime = System.currentTimeMillis();
    				Map<String,Object> map = new HashMap<String,Object>();
    				for(Map<String,Object> fansRoom:list){
    					String lecturer_id = (String)fansRoom.get("lecturer_id");
    					map.clear();
    			        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
    			        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
    			        Response<Set<Tuple>> response = pipeline.zrevrangeByScoreWithScores(lecturerCoursesPredictionKey, currentTime+"", "-inf", 0,  1);
    			        courseInfoDictMap.put(lecturer_id, response);    			        
    			        response = pipeline.zrevrangeByScoreWithScores(lecturerCoursesPredictionKey, "+inf", "-inf", 0,  1);
    			        courseInfoPreDictMap.put(lecturer_id, response);

						String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
						response = pipeline.zrevrangeByScoreWithScores(lecturerCoursesFinishKey, "+inf", "-inf", 0, 1);
						courseInfoFinDictMap.put(lecturer_id, response);
    				}
    				
    				pipeline.sync();
    				Map<String,Response<Map<String,String>>> courseInfo = new HashMap<String,Response<Map<String,String>>>();
    				
    				for(Map<String,Object> fansRoom:list){
    					String lecturer_id = (String)fansRoom.get("lecturer_id");
    					if(courseInfo.containsKey(lecturer_id)){
    						continue;
    					}
    					Response<Set<Tuple>> tupleSet = courseInfoDictMap.get(lecturer_id);
    					String courseId = null;
    					if(tupleSet!=null && !MiscUtils.isEmpty(tupleSet.get())){
    						Set<Tuple> set = tupleSet.get();
    						for(Tuple tupe:set){
    							courseId = tupe.getElement();
    						}    						
    					}
    					if(MiscUtils.isEmpty(courseId)){
    						tupleSet = courseInfoPreDictMap.get(lecturer_id);
    					}
    					
    					if(tupleSet!=null && !MiscUtils.isEmpty(tupleSet.get())){
    						Set<Tuple> set = tupleSet.get();
    						for(Tuple tupe:set){
    							courseId = tupe.getElement();
    						}    						
    					}
    					
    					if(MiscUtils.isEmpty(courseId)){
    						tupleSet = courseInfoFinDictMap.get(lecturer_id);
    					}
    					
    					if(tupleSet!=null && !MiscUtils.isEmpty(tupleSet.get())){
    						Set<Tuple> set = tupleSet.get();
    						for(Tuple tupe:set){
    							courseId = tupe.getElement();
    						}    						
    					}
    					if(!MiscUtils.isEmpty(courseId)){
    						map.clear();
    						map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
    						String cachedKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
    						courseInfo.put(lecturer_id, pipeline.hgetAll(cachedKey));
    					}
    				}
    				
    				pipeline.sync();
    				
    				for(Map<String,Object> fansRoom:list){
    					String lecturer_id = (String)fansRoom.get("lecturer_id");
    					Response<Map<String,String>> response = courseInfo.get(lecturer_id);
    					Map<String,String> courseValue = null;
    					if(response!=null){
    						courseValue = response.get();
    					}
    					if(MiscUtils.isEmpty(courseValue)){
    						continue;
    					}
    					MiscUtils.courseTranferState(currentTime, courseValue);
    					fansRoom.put("course_title", courseValue.get("course_title"));
    					fansRoom.put("course_id", courseValue.get("course_id"));
    					fansRoom.put("course_type", courseValue.get("course_type"));
    					fansRoom.put("start_time", courseValue.get("start_time"));
    					fansRoom.put("status", courseValue.get("status"));
    				}
    			}
    		});
        }
        
        return resultMap;
    }
    
    @FunctionName("studyCourses")
    public  Map<String, Object> getStudyCourses(RequestEntity reqEntity) throws Exception{
    	@SuppressWarnings("unchecked")
    	Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();        
    	String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
    	reqMap.put("user_id", userId);
    	Map<String,String> values = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedisUtils);
    	long course_num = 0;
    	try{
    		course_num = Long.parseLong(values.get("course_num").toString());
    	}catch(Exception e){
    		course_num = 0;
    	}
    	Map<String,Object> result = new HashMap<String,Object>();
    	result.put("course_num", course_num);
    	if(course_num>0){
	    	Map<String,Object> queryMap = new HashMap<String,Object>();
	    	queryMap.put("create_time", reqMap.get("record_time"));
	    	queryMap.put("page_count", reqMap.get("page_count"));
	    	queryMap.put("user_id", userId);
	
	    	final List<Map<String,Object>> list = userModuleServer.findCourseListOfStudent(queryMap);
	    	result.put("course_list", list);
	    	if(!MiscUtils.isEmpty(list)){
	    		((JedisBatchCallback)this.jedisUtils.getJedis()).invoke(new JedisBatchOperation(){
	    			@Override
	    			public void batchOperation(Pipeline pipeline, Jedis jedis) {
	    	    		long currentTime= System.currentTimeMillis();
	    	    		Map<String,Response<String>> nickeNameMap = new HashMap<String,Response<String>>();
	    	    		for(Map<String,Object> course:list){
	    	    			Map<String,String> course_map = new HashMap<String,String>();
	    	    			MiscUtils.converObjectMapToStringMap(course, course_map);
	    	    			MiscUtils.courseTranferState(currentTime, course_map);
	    	    			course.put("status", course_map.get("status"));
	    	    			String lecturer_id = course_map.get("lecturer_id");
	    	    			if(!nickeNameMap.containsKey(lecturer_id)){
	    	    				String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, course_map);
		    	    			nickeNameMap.put(lecturer_id, pipeline.hget(key, "nick_name"));
	    	    			}	    	    			
	    	    		}
	    	    		pipeline.sync();
	    	    		for(Map<String,Object> course:list){
	    	    			String lecturer_id =(String) course.get("lecturer_id");
	    	    			if(!MiscUtils.isEmpty(lecturer_id)){
	    	    				Response<String> response = nickeNameMap.get(lecturer_id);
	    	    				if(response != null)	course.put("nick_name", response.get());
	    	    			}	    	    			
	    	    		}
	    			}
	    		});
	    	}
    	}
    	return result;
    }
}
