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
import qingning.user.server.other.ReadCourseOperation;
import qingning.user.server.other.ReadLiveRoomOperation;
import qingning.user.server.other.ReadUserOperation;
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
    @Override
    public void initRpcServer() {
        if (userModuleServer == null) {
            userModuleServer = this.getRpcService("userModuleServer");

            readLiveRoomOperation = new ReadLiveRoomOperation(userModuleServer);
            readCourseOperation = new ReadCourseOperation(userModuleServer);
            
            readUserOperation = new ReadUserOperation(userModuleServer);
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
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        //查询直播间是否存在 //TODO 需要进一步优化
        if (!jedis.exists(roomKey)) {
            throw new QNLiveException("100002");
        }
        String lecturerId = jedis.hget(roomKey, "lecturer_id");
        reqMap.put("lecturer_id", lecturerId);

        Map<String, Object> dbResultMap = userModuleServer.userFollowRoom(reqMap);
        if (dbResultMap == null || dbResultMap.get("update_count") == null || dbResultMap.get("update_count").toString().equals("0")) {
            throw new QNLiveException("110003");
        }

        //2.更新用户表中的关注数
        userModuleServer.updateLiveRoomNumForUser(reqMap);

        //3.延长用户缓存时间
        map.clear();
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String userKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, map);
        jedis.expire(userKey, 10800);

        //4.更新用户缓存中直播间的关注数
        //关注操作类型 0关注 1不关注
        Integer incrementNum = null;
        if (reqMap.get("follow_type").toString().equals("0")) {
            incrementNum = 1;
        } else {
            incrementNum = -1;
        }
        jedis.hincrBy(userKey, "live_room_num", incrementNum);

        //5.更新直播间缓存的粉丝数
        jedis.hincrBy(roomKey, "fans_num", incrementNum);

        //6.更新讲师缓存的粉丝数
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "fans_num", incrementNum);

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("userCourses")
    public Map<String, Object> getCourses(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();

        return getPlatformCourses(reqEntity);
    }


    /**
     * 查询平台所有的课程
     *
     * @param reqEntity
     * @return
     */
    @SuppressWarnings({ "unchecked", "unused" })
    private Map<String, Object> getPlatformCourses(RequestEntity reqEntity) throws Exception {
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

    }

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
            List<Map<String,Object>> roomDistributerList = userModuleServer.findRoomDistributionInfoByDistributerId(queryMap);

            long now = MiscUtils.getEndDateOfToday().getTime();
            if (! MiscUtils.isEmpty(roomDistributerList)) {
                boolean isDistributer = false;
                for(Map<String,Object> map : roomDistributerList){
                    //0:永久有效 1: 1个月内有效 2: 3个月内有效 3: 6个月内有效 4: 9个月内有效 5:一年有效 6: 两年有效
                    if(map.get("effective_time").toString().equals("0")){
                        isDistributer = true;
                        break;
                    }else {
                        if(map.get("end_date") != null){
                            Date end_date = (Date)map.get("end_date");
                            if(end_date.getTime() >= now){
                                isDistributer = true;
                                break;
                            }
                        }
                    }
                }

                if(isDistributer){
                    roles.add("4");
                }else {
                    roles.add("1");
                }

                //不为分销员则为普通用户
            }else {
                roles.add("1");
            }
        }

        resultMap.put("roles",roles);
        return resultMap;
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
        Map<String,Object> studentMap = userModuleServer.findStudentByCourseIdAndUserId(queryMap);

        //返回用户身份
        //角色数组 1：普通用户、2：学员、3：讲师
        List<String> roles = new ArrayList<>();
        //加入课程状态 0未加入 1已加入
        if(MiscUtils.isEmpty(studentMap)){
            resultMap.put("join_status", "0");
        }else {
            resultMap.put("join_status", "1");
        }

        if(userId.equals(courseMap.get("lecturer_id"))){
            roles.add("3");
        }else {
            if(MiscUtils.isEmpty(studentMap)){
                roles.add("2");
            }else {
                roles.add("1");
            }
        }
        resultMap.put("roles",roles);

        //查询关注状态
        //关注状态 0未关注 1已关注
        reqMap.put("room_id", liveRoomMap.get("room_id"));
        Map<String, Object> fansMap = userModuleServer.findFansByUserIdAndRoomId(reqMap);
        if (CollectionUtils.isEmpty(fansMap)) {
            resultMap.put("follow_status", "0");
        } else {
            resultMap.put("follow_status", "1");
        }

        return resultMap;
    }


    @SuppressWarnings("unchecked")
    @FunctionName("roomCourses")
    public Map<String, Object> getRoomCourses(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);
        List<Map<String, String>> courseResultList = new ArrayList<>();

        //目前一个讲师仅能创建一个直播间，所以查询的是该讲师发布的课程
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String lecturerId = jedis.hget(roomKey, "lecturer_id");

        //TODO 目前只有查询讲师的课程列表，查询直播间的课程列表暂未实现
        //if (reqMap.get("room_id") == null || StringUtils.isBlank(reqMap.get("room_id").toString())) {
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);

        String startIndex = null;
        String endIndex = "+inf";
        String startIndexDB = null;
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        if (reqMap.get("start_time") == null || StringUtils.isBlank(reqMap.get("start_time").toString())) {
            startIndex = "0";
        } else {
            startIndex = "(" + reqMap.get("start_time").toString();
        }

        Set<Tuple> predictionList = jedis.zrangeByScoreWithScores(lecturerCoursesPredictionKey, startIndex, endIndex, 0, pageCount);
        Set<Tuple> finishList = null;
        List<Map<String, Object>> dbList = null;
        Map<String, Object> finishResultMap = new HashMap<>();

        if (predictionList == null || predictionList.isEmpty()) {
            if (startIndex.equals("0")) {
                endIndex = "-inf";
                startIndex = "+inf";
                startIndexDB = null;
            } else {
                endIndex = "-inf";
                startIndexDB = startIndex;
            }

            finishResultMap = findCourseFinishList(jedis, lecturerCoursesFinishKey, startIndex, startIndexDB, endIndex, 0, pageCount);

        } else {
            if (predictionList.size() < pageCount) {
                startIndex = "+inf";
                endIndex = "-inf";
                finishResultMap = findCourseFinishList(jedis, lecturerCoursesFinishKey, startIndex, null, endIndex, 0, pageCount - predictionList.size());
            }
        }

        //未结束课程列表，结束课程列表，数据库课程列表三者拼接到最终的课程结果列表，即可得到结果
        //分别迭代这三个列表
        if (!CollectionUtils.isEmpty(finishResultMap)) {
            if (finishResultMap.get("finishList") != null) {
                finishList = (Set<Tuple>) finishResultMap.get("finishList");
            }

            if (finishResultMap.get("dbList") != null) {
                dbList = (List<Map<String, Object>>) finishResultMap.get("dbList");
            }
        }


        if (predictionList != null) {
            for (Tuple tuple : predictionList) {
                ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                courseResultList.add(courseInfoMap);
            }
        }

        if (finishList != null) {
            for (Tuple tuple : finishList) {
                ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                courseResultList.add(courseInfoMap);
            }
        }

        if (dbList != null) {
            for (Map<String, Object> courseDBMap : dbList) {
                Map<String, String> courseDBMapString = new HashMap<>();
                MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                courseResultList.add(courseDBMapString);
            }
        }

        //}

        if (!CollectionUtils.isEmpty(courseResultList)) {
            resultMap.put("course_list", courseResultList);
        }

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
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

        Map<String,String> courseMap = new HashMap<>();
        //1.2如果课程在数据库中
        Map<String,Object> courseObjectMap = userModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
        //1.先检测课程存在情况和状态
        //1.1如果课程在缓存中
        if(jedis.exists(courseKey)){
            courseMap = jedis.hgetAll(courseKey);
        }else {
            if(courseObjectMap == null){
                throw new QNLiveException("100004");
            }
            MiscUtils.converObjectMapToStringMap(courseObjectMap, courseMap);
        }

        //1.2检测该用户是否为讲师，为讲师则不能加入该课程
        if(userId.equals(courseMap.get("lecturer_id"))){
            throw new QNLiveException("100017");
        }
        //2.检测课程验证信息是否正确
        //2.1如果课程为私密课程则检验密码
        if(courseMap.get("course_type").equals("1")){
            if(reqMap.get("course_password") == null || StringUtils.isBlank(reqMap.get("course_password").toString())){
                throw new QNLiveException("000100");
            }
            if(! reqMap.get("course_password").toString().equals(courseMap.get("course_password").toString())){
                throw new QNLiveException("120006");
            }
        }else if(courseMap.get("course_type").equals("2")){
            //TODO 支付课程要验证支付信息
            if(reqMap.get("payment_id") == null){
                throw new QNLiveException("000100");
            }
        }

        //3.检测学生是否参与了该课程
        Map<String,Object> studentQueryMap = new HashMap<>();
        studentQueryMap.put("user_id",userId);
        studentQueryMap.put("course_id",courseMap.get("course_id"));
        Map<String,Object> studentMap = userModuleServer.findStudentByCourseIdAndUserId(studentQueryMap);
        if(studentMap != null){
            throw new QNLiveException("100004");
        }

        Map<String,Object> studentUserMap = null;
        Map<String,Object> lecturerUserMap = null;
        //4.将学生加入该课程的IM群组
        try {
           studentUserMap = userModuleServer.findLoginInfoByUserId(userId);
            lecturerUserMap = userModuleServer.findLoginInfoByUserId(courseMap.get("lecturer_id"));
            IMMsgUtil.joinGroup(courseMap.get("im_course_id"), studentUserMap.get("m_user_id").toString(),lecturerUserMap.get("m_user_id").toString());
        }catch (Exception e){
            //TODO 暂时不处理
        }

        //5.将学员信息插入到学员参与表中
        courseMap.put("user_id",userId);
        Map<String,Object> insertResultMap = userModuleServer.joinCourse(courseMap);

        //6.修改讲师缓存中的课程参与人数
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseMap.get("lecturer_id").toString());
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "total_student_num", 1);

        Long nowStudentNum = 0L;
        if(jedis.exists(courseKey)){
            jedis.hincrBy(courseKey, "student_num", 1);
        }else {
            userModuleServer.increaseStudentNumByCourseId(reqMap.get("course_id").toString());
        }

        nowStudentNum = Long.parseLong(courseMap.get("student_num")) + 1;
        String levelString = MiscUtils.getConfigByKey("jpush_course_students_arrive_level");
        JSONArray levelJson = JSON.parseArray(levelString);
        if(levelJson.contains(nowStudentNum+"")){
            JSONObject obj = new JSONObject();
            String course_type = courseMap.get("course_type");
            String course_type_content = MiscUtils.convertCourseTypeToContent(course_type);
            obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content"), course_type_content, courseMap.get("course_title"), nowStudentNum+""));
            obj.put("to", userId);
            obj.put("msg_type","7");
            Map<String,String> extrasMap = new HashMap<>();
            extrasMap.put("msg_type","7");
            extrasMap.put("course_id",courseMap.get("course_id"));
            obj.put("extras_map", extrasMap);
            JPushHelper.push(obj);
        }
        //course_type 0:公开课程 1:加密课程 2:收费课程',
        //TODO 加入课程推送   收费课程支付成功才推送消息
        if (!"2".equals(courseMap.get("course_type"))) {

    		Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
    		TemplateData first = new TemplateData();
    		first.setColor("#000000");
    		first.setValue(MiscUtils.getConfigByKey("wpush_shop_course_first"));
    		templateMap.put("first", first);
    		
    		Date start_time = (Date) courseObjectMap.get("start_time");
    		TemplateData orderNo = new TemplateData();
    		orderNo.setColor("#000000");
    		orderNo.setValue(MiscUtils.parseDateToFotmatString(start_time, "yyyy-MM-dd hh:mm:ss"));
    		templateMap.put("keyword1", orderNo);
    		
    		TemplateData wuliu = new TemplateData();
    		wuliu.setColor("#000000");
    		wuliu.setValue(lecturerUserMap.get("nick_name").toString());
    		templateMap.put("keyword2", wuliu);	

    		TemplateData name = new TemplateData();
    		name.setColor("#000000");
    		name.setValue((String) courseObjectMap.get("course_title"));
    		templateMap.put("keyword3", name);

    		TemplateData remark = new TemplateData();
    		remark.setColor("#000000");
    		remark.setValue(MiscUtils.getConfigByKey("wpush_shop_course_remark"));
    		templateMap.put("remark", remark);
    		WeiXinUtil.send_template_message((String) studentUserMap.get("web_openid"), MiscUtils.getConfigByKey("wpush_shop_course"), templateMap, jedis);
		}
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

            //检测学员是否加入了该课程，加入课程才能查询相关信息，如果没加入课程则提示学员未加入该课程（120007）
            Map<String,Object> queryStudentMap = new HashMap<>();
            queryStudentMap.put("user_id", userId);
            queryStudentMap.put("course_id",reqMap.get("course_id").toString());
            Map<String,Object> studentMap = userModuleServer.findStudentByCourseIdAndUserId(queryStudentMap);
            if(MiscUtils.isEmpty(studentMap)){
                throw new QNLiveException("120007");
            }

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

            Map<String,Object> queryStudentMap = new HashMap<>();
            queryStudentMap.put("user_id", userId);
            queryStudentMap.put("course_id",reqMap.get("course_id").toString());
            Map<String,Object> studentMap = userModuleServer.findStudentByCourseIdAndUserId(queryStudentMap);
            if(MiscUtils.isEmpty(studentMap)){
                throw new QNLiveException("120007");
            }

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
            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_QUESTION, map);
            queryMap.put("send_type", "3");//send_type 类型:0:讲师讲解 1：讲师回答 2 用户评论 3 用户提问
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
    				Map<String,Response<Set<Tuple>>> courseInfoMap = new HashMap<String,Response<Set<Tuple>>>();
    				Map<String,Object> map = new HashMap<String,Object>();
    				for(Map<String,Object> fansRoom:list){
    					String lecturer_id = (String)fansRoom.get("lecturer_id");
    					map.clear();
    			        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
    			        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);    			        
    			        Response<Set<Tuple>> response = pipeline.zrangeByScoreWithScores(lecturerCoursesPredictionKey, "-inf", "+inf", 0, 1);
    			        courseInfoMap.put(lecturer_id, response);
    				}
    				pipeline.sync();
    				for(String key:courseInfoMap.keySet()){
    					Response<Set<Tuple>> response = courseInfoMap.get(key);
    					boolean reQuery = false;
    					if(response==null){
    						reQuery = true;
    					} else {
    						Set<Tuple> set = response.get();
    						if(set==null || set.isEmpty()){
    							reQuery = true;
    						} else if(MiscUtils.isEmpty(set.iterator().next().getElement())){
    							reQuery = true;
    						}
    					}
    					
    					if(reQuery){
    						map.clear();
    						map.put(Constants.CACHED_KEY_LECTURER_FIELD, key);
    						String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
    						response = pipeline.zrevrangeByScoreWithScores(lecturerCoursesFinishKey, "+inf", "-inf", 0, 1);
    						courseInfoMap.put(key, response);
    					}
    				}
    				pipeline.sync();
    				Map<String,Response<Map<String,String>>> courseInfo = new HashMap<String,Response<Map<String,String>>>();
    				for(String key:courseInfoMap.keySet()){
    					Response<Set<Tuple>> response = courseInfoMap.get(key);
    					String courseId = null;
    					if(response!=null){
    						Set<Tuple> set = response.get();
    						if(set!=null && !set.isEmpty()){
    							courseId = set.iterator().next().getElement();    						
    						}    						
    					}
    					if(!MiscUtils.isEmpty(courseId)){
    						map.clear();
    						map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
    						String cachedKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
    						courseInfo.put(key, pipeline.hgetAll(cachedKey));
    					}
    				}
    				pipeline.sync();
    				long times = System.currentTimeMillis();
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
    					MiscUtils.courseTranferState(times, courseValue);
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
	
	    	final List<Map<String,Object>> list = userModuleServer.findStudentCourseList(queryMap);
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
