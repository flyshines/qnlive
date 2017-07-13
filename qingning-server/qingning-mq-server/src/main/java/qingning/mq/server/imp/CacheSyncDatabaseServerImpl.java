package qingning.mq.server.imp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.JedisBatchCallback;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CacheSyncDatabaseServerImpl extends AbstractMsgService {
	private static Logger log = LoggerFactory.getLogger(CacheSyncDatabaseServerImpl.class);
    private CoursesMapper coursesMapper;
    private LiveRoomMapper liveRoomMapper;
    private LecturerMapper lecturerMapper;
    private LoginInfoMapper loginInfoMapper;
    private DistributerMapper distributerMapper;
    private LecturerDistributionInfoMapper lecturerDistributionInfoMapper;
    private RoomDistributerMapper roomDistributerMapper;
    private CourseImageMapper courseImageMapper;
    private LecturerDistributionLinkMapper lecturerDistributionLinkMapper;
    private RoomDistributerDetailsMapper roomDistributerDetailsMapper;
    private UserMapper userMapper;
    private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;
	private SaaSCourseMapper saaSCourseMapper;

	@Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
    	String appName = requestEntity.getAppName();
    	Jedis jedis = jedisUtils.getJedis(appName);
    	
    	((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
				//查询出所有需要更新的用户
				Set<String> userSet = jedis.smembers(Constants.CACHED_UPDATE_USER_KEY);
				jedis.del(Constants.CACHED_UPDATE_USER_KEY);
				if(!MiscUtils.isEmpty(userSet)){
					updateUserData(userSet,pipeline);
				}
				
		        //查询出所有需要更新的讲师
				Set<String> lecturerSet = jedis.smembers(Constants.CACHED_UPDATE_LECTURER_KEY);
				jedis.del(Constants.CACHED_UPDATE_LECTURER_KEY);
				if(!MiscUtils.isEmpty(lecturerSet)){
			        //同步讲师数据
			        updateLecturerData(lecturerSet,pipeline);		        
			        //同步直播间
			        updateLiveRoomData(lecturerSet,pipeline);
			        //同步课程数据
			        updateCourseData(lecturerSet,pipeline);
			        //更新讲师分销信息
			        updateLecturerDistributionLink(lecturerSet, jedis);
					//更新讲师saas课程
					updateSaasCourseData(lecturerSet,jedis);

				}
		        //查询出所有需要更新的分销员
				Set<String> distributerIdSet = jedis.smembers(Constants.CACHED_UPDATE_DISTRIBUTER_KEY);
				jedis.del(Constants.CACHED_UPDATE_DISTRIBUTER_KEY);
				if(!MiscUtils.isEmpty(distributerIdSet)){
					updateDistributerData(distributerIdSet,pipeline);
				}
				//查询出所有需要更新的分销
				Set<String> rqCodeSet = jedis.smembers(Constants.CACHED_UPDATE_RQ_CODE_KEY);
				jedis.del(Constants.CACHED_UPDATE_RQ_CODE_KEY);
				if(!MiscUtils.isEmpty(rqCodeSet)){
					updateRoomDistributerData(rqCodeSet,pipeline);
				}
			}
			
			private void updateUserData(Set<String> userSet, Pipeline pipeline){
		    	Map<String,Object> queryParam = new HashMap<String,Object>();
		    	Map<String,Response<Map<String,String>>> userDataMap = new HashMap<String,Response<Map<String,String>>>();
		    	Calendar cal = Calendar.getInstance();
		    	cal.setTimeInMillis(System.currentTimeMillis());
		    	 Date currentDate = MiscUtils.getEndDateOfToday();
		    	for(String userId:userSet){
		    		queryParam.clear();
		    		queryParam.put("distributer_id", userId);                   
		    		queryParam.put("create_date", currentDate);
                    Map<String,Object> sumInfo = lecturerCoursesProfitMapper.findCoursesSumInfo(queryParam);
                    if(!MiscUtils.isEmpty(sumInfo)){
    		    		queryParam.clear();
    		    		queryParam.put(Constants.CACHED_KEY_USER_FIELD, userId);
    		    		String userKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, queryParam);
    		    		pipeline.hset(userKey, "today_distributer_amount", MiscUtils.convertObjectToLong(sumInfo.get("share_amount"))+"");
                    }
		    	}
		    	pipeline.sync();
				
				for(String userId:userSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_USER_FIELD, userId);
					String userKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, queryParam);
					userDataMap.put(userKey, pipeline.hgetAll(userKey));
				}
				pipeline.sync();
				Set<String> errorIds = new HashSet<String>();
				for(String userId:userDataMap.keySet()){
					try{
		        		Map<String,String> values = userDataMap.get(userId).get();
		        		if(MiscUtils.isEmpty(values)){
		        			continue;
		        		}
		        		Map<String,Object> user = new HashMap<String,Object>();
		        		user.put("nick_name", values.get("nick_name"));
		        		user.put("avatar_address", values.get("avatar_address"));
		        		user.put("phone_number", values.get("phone_number"));
		        		user.put("gender", values.get("gender"));
		        		user.put("country", values.get("country"));
		        		user.put("province", values.get("province"));
		        		user.put("city", values.get("city"));
		        		user.put("district", values.get("district"));
		        		user.put("course_num", MiscUtils.convertObjectToLong(values.get("course_num")));
		        		user.put("live_room_num", MiscUtils.convertObjectToLong(values.get("live_room_num")));
		        		user.put("status", values.get("status"));
		        		user.put("plateform", values.get("plateform"));
		        		if(!MiscUtils.isEmpty(values.get("last_login_time"))){
		        			user.put("last_login_time",  new Date(MiscUtils.convertObjectToLong(values.get("last_login_time"))));
		        		}
		        		user.put("last_login_ip", values.get("last_login_ip"));
		        		user.put("user_id", userId);
		        		
		        		userMapper.updateUser(user);
					}catch(Exception e){
		        		errorIds.add(userId);
		        		log.error("Sync user ["+userId+"]:"+e.getMessage());
					}
				}
				
		        if(!MiscUtils.isEmpty(errorIds)){
		        	String[] members = new String[errorIds.size()];
		        	int count=0;
		        	for(String lecturerId : errorIds){
		        		userSet.remove(lecturerId);
		        		members[count++] = lecturerId;
		        	}
		        	jedis.sadd(Constants.CACHED_UPDATE_USER_KEY, members);
		        }
			}
			
			private void updateLecturerData(Set<String> lecturerSet, Pipeline pipeline){
		    	Map<String,Object> queryParam = new HashMap<String,Object>();
		    	Map<String,Response<Map<String,String>>> lecturerDataMap = new HashMap<String,Response<Map<String,String>>>();
				for(String lecturerId : lecturerSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
					String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, queryParam);					
					lecturerDataMap.put(lecturerId, pipeline.hgetAll(lecturerKey));
				}
				
		        pipeline.sync();
		        Set<String> errorIds = new HashSet<String>();
		        for(String lecturerId : lecturerDataMap.keySet()){
		        	try{
		        		Map<String,String> values = lecturerDataMap.get(lecturerId).get();
		        		if(MiscUtils.isEmpty(values)){
		        			continue;
		        		}
		        		//t_lecturer
		        		Map<String,Object> lecturer = new HashMap<String,Object>();
		        		lecturer.put("lecturer_id", lecturerId);
		        		lecturer.put("fans_num", MiscUtils.convertObjectToLong(values.get("fans_num")));
		        		lecturer.put("live_room_num", MiscUtils.convertObjectToLong(values.get("live_room_num")));
		        		lecturer.put("course_num", MiscUtils.convertObjectToLong(values.get("course_num")));
		        		lecturer.put("total_student_num", MiscUtils.convertObjectToLong(values.get("total_student_num")));
		        		lecturer.put("pay_course_num", MiscUtils.convertObjectToLong(values.get("pay_course_num")));		        		              
		        		lecturer.put("private_course_num", MiscUtils.convertObjectToLong(values.get("private_course_num")));
		        		lecturer.put("total_amount", MiscUtils.convertObjectToLong(values.get("total_amount")));
		        		lecturer.put("total_time", MiscUtils.convertObjectToLong(values.get("total_time")));
		        		lecturerMapper.updateLecture(lecturer);
		        		
		        		//t_lecturer_distribution_info
		        		Map<String,Object> lecturerDistributionInfo = new HashMap<String,Object>();
		        		lecturerDistributionInfo.put("lecturer_id", lecturerId);
		        		
		        		lecturerDistributionInfo.put("live_room_num", MiscUtils.convertObjectToLong(values.get("distribution_live_room_num")));
		        		lecturerDistributionInfo.put("room_distributer_num", MiscUtils.convertObjectToLong(values.get("room_distributer_num")));
		        		lecturerDistributionInfo.put("room_recommend_num", MiscUtils.convertObjectToLong(values.get("room_recommend_num")));		        		
		        		lecturerDistributionInfo.put("room_done_num", MiscUtils.convertObjectToLong(values.get("room_done_num")));
		        		
		        		lecturerDistributionInfo.put("course_distribution_num", MiscUtils.convertObjectToLong(values.get("course_distribution_num")));
		        		lecturerDistributionInfo.put("course_distributer_num", MiscUtils.convertObjectToLong(values.get("course_distributer_num")));
		        		lecturerDistributionInfo.put("course_recommend_num", MiscUtils.convertObjectToLong(values.get("course_recommend_num")));
		        		lecturerDistributionInfo.put("course_done_num", MiscUtils.convertObjectToLong(values.get("course_done_num")));
		        		lecturerDistributionInfo.put("room_distributer_done_num", MiscUtils.convertObjectToLong(values.get("room_distributer_done_num")));
		        		lecturerDistributionInfo.put("course_distributer_done_num", MiscUtils.convertObjectToLong(values.get("course_distributer_done_num")));
		        		lecturerDistributionInfoMapper.updateLecturerDistributionInfo(lecturerDistributionInfo);
		        	} catch (Exception e){
		        		errorIds.add(lecturerId);
		        		log.error("Sync lecturer ["+lecturerId+"]:"+e.getMessage());
		        	}
		        }
		        
		        if(!MiscUtils.isEmpty(errorIds)){
		        	String[] members = new String[errorIds.size()];
		        	int count=0;
		        	for(String lecturerId : errorIds){
		        		lecturerSet.remove(lecturerId);
		        		members[count++] = lecturerId;
		        	}
		        	jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, members);
		        }
			}

			private void updateLiveRoomData(Set<String> lecturerSet, Pipeline pipeline){
		    	Map<String,Object> queryParam = new HashMap<String,Object>();
		    	Map<String,Response<Set<String>>> roomKeyMap = new HashMap<String,Response<Set<String>>>();
				for(String lecturerId : lecturerSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
					String lecturerRoomsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, queryParam);
					roomKeyMap.put(lecturerId, pipeline.hkeys(lecturerRoomsKey));
				}
				pipeline.sync();
				
				Map<String,Response<Map<String,String>>> roomDataMap = new HashMap<String,Response<Map<String,String>>>();
				Map<String,String> liveRoomKeyMap = new HashMap<String,String>();
				for(String lecturerId : roomKeyMap.keySet()){
					Set<String> liveRoomIdList = roomKeyMap.get(lecturerId).get();
					if(MiscUtils.isEmpty(liveRoomIdList)){
						continue;
					}
					for(String liveRoomId: liveRoomIdList){
						queryParam.clear();						
						queryParam.put(Constants.FIELD_ROOM_ID, liveRoomId);
                        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, queryParam);                        
                        roomDataMap.put(liveRoomId, pipeline.hgetAll(liveRoomKey));
                        liveRoomKeyMap.put(liveRoomId, liveRoomKey);                                      
					}
				}				
				pipeline.sync();
				
				for(String liveRoomId: roomDataMap.keySet()){
					Map<String,String> values = roomDataMap.get(liveRoomId).get();
					if(MiscUtils.isEmpty(values)){
						continue;
					}
					try{
						String liveRoomKey = liveRoomKeyMap.get(liveRoomId);
						String roomAddress = MiscUtils.getConfigByKey("live_room_share_url_pre_fix",appName)+liveRoomId;
						
						Map<String,Object> liveRoom = new HashMap<String,Object>();
						liveRoom.put("room_id", liveRoomId);
						liveRoom.put("fans_num", MiscUtils.convertObjectToLong(values.get("fans_num")));
						liveRoom.put("course_num", MiscUtils.convertObjectToLong(values.get("course_num")));
						liveRoom.put("distributer_num", MiscUtils.convertObjectToLong(values.get("distributer_num")));
						liveRoom.put("total_amount", MiscUtils.convertObjectToLong(values.get("total_amount")));
						//liveRoom.put("last_course_amount", MiscUtils.convertObjectToLong(values.get("last_course_amount")));
						liveRoom.put("room_address", roomAddress);						
						liveRoomMapper.updateLiveRoom(liveRoom);
						pipeline.hset(liveRoomKey, "room_address", roomAddress);
					} catch(Exception e){
						log.error("Sync room ["+liveRoomId+"]:"+e.getMessage());
					}
				}
				pipeline.sync();
			}
			
			private void updateLecturerDistributionLink(Set<String> lecturerSet, Jedis jedis){
				Map<String,Object> queryParam = new HashMap<String,Object>();
				
				Set<String> available = new HashSet<String>();				
				for(String lecturerId : lecturerSet){
					available.clear();
					queryParam.clear();
			        queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
			        String userShareCodesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_SHARE_CODES, queryParam);
			        Set<String> shareCodes = jedis.smembers(userShareCodesKey);
			        if(MiscUtils.isEmpty(shareCodes)){
			        	continue;
			        }
			        jedis.del(userShareCodesKey);
			        
			        for(String shareCode : shareCodes){
			        	queryParam.clear();
						queryParam.put(Constants.CACHED_KEY_USER_ROOM_SHARE_FIELD,shareCode);
				        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOM_SHARE, queryParam);
				        Map<String,String> cachedValues = jedis.hgetAll(key);
				        if(!MiscUtils.isEmpty(cachedValues)){
				        	 long end_date = MiscUtils.convertObjectToLong(cachedValues.get("end_date"));
				        	 if(System.currentTimeMillis()>=end_date || "1".equals(cachedValues.get("status"))){
				        		 jedis.del(key);
				        		 Map<String,Object> lecturerDistributionLink = new HashMap<String,Object>();
				        		 lecturerDistributionLink.put("lecturer_distribution_id", cachedValues.get("lecturer_distribution_id"));
				        		 lecturerDistributionLink.put("distributer_num", MiscUtils.convertObjectToLong(cachedValues.get("distributer_num")));
				        		 lecturerDistributionLink.put("click_num", MiscUtils.convertObjectToLong(cachedValues.get("click_num")));
				        		 lecturerDistributionLink.put("status", "1");
				        		 lecturerDistributionLinkMapper.updateLecturerDistributionLink(lecturerDistributionLink);
				        	 } else {
				        		 available.add(shareCode);
				        	 }
				        }
			        }
			        if(!available.isEmpty()){
			        	String[] shareCodesArr = new String[available.size()];
			        	int count = 0;
			        	for(String shareCode : available){
			        		shareCodesArr[count++]=shareCode;
			        	}
			        	jedis.sadd(userShareCodesKey, shareCodesArr);
			        	jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, lecturerId);
			        }
				}
			}

			private void updateSaasCourseData(Set<String> lecturerSet, Jedis jedis){
				Map<String,Set<String>> preCourseKeyMap = new HashMap<>();
				Map<String,Object> queryParam = new HashMap<String,Object>();
				Set<String> courseIdSet = new HashSet<String>();
				for(String lecturerId : lecturerSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
					String saasCourseListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_SAAS, queryParam);
					Set<String> zrange = jedis.zrange(saasCourseListKey, 0, -1);
					if(!MiscUtils.isEmpty(zrange))
						courseIdSet.addAll(zrange);
				}

				Map<String, Map<String, String>> courseData = new HashMap<>();
				for(String course_id : courseIdSet ){
					queryParam.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
					String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, queryParam);
					courseData.put(course_id, jedis.hgetAll(courseKey));
				}

				for(String courseId:courseData.keySet()){
					Map<String,String> values = courseData.get(courseId);
					if(MiscUtils.isEmpty(values)){
						continue;
					}
					try{
						Map<String,Object> courses = new HashMap<String,Object>();
						courses.put("course_id", courseId);
						courses.put("click_num", MiscUtils.convertObjectToLong(values.get("click_num")));
						saaSCourseMapper.updateByPrimaryKey(courses);
					}catch(Exception e){
						log.error("Sync courses ["+courseId+"]:"+e.getMessage());
					}
				}


			}


			@SuppressWarnings("unchecked")
			private void updateCourseData(Set<String> lecturerSet, Pipeline pipeline){
				Map<String,Object> queryParam = new HashMap<String,Object>();
		    	Map<String,Response<Set<String>>> preCourseKeyMap = new HashMap<String,Response<Set<String>>>();
		    	Map<String,Response<Set<String>>> finishCourseKeyMap = new HashMap<String,Response<Set<String>>>();
		    	
				for(String lecturerId : lecturerSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
		            String predictionListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, queryParam);
		            String finishListKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, queryParam);		            
		            preCourseKeyMap.put(lecturerId, pipeline.zrange(predictionListKey, 0 , -1));		          
		            finishCourseKeyMap.put(lecturerId, pipeline.zrange(finishListKey, 0 , -1));
				}
				pipeline.sync();
				
				Set<String> courseIdSet = new HashSet<String>();
				for(String lecturerId : lecturerSet){
					Set<String> set = preCourseKeyMap.get(lecturerId).get();
					if(!MiscUtils.isEmpty(set)){
						courseIdSet.addAll(set);
					}
					set = finishCourseKeyMap.get(lecturerId).get();
					if(!MiscUtils.isEmpty(set)){
						courseIdSet.addAll(set);
					}
				}
				
				Map<String, Response<Map<String,String>>> courseData = new HashMap<String, Response<Map<String,String>>>();
				for(String courseId:courseIdSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
                    String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, queryParam);
                    courseData.put(courseId, pipeline.hgetAll(courseKey));
				}
				
				pipeline.sync();
				
				for(String courseId:courseData.keySet()){
					Map<String,String> values = courseData.get(courseId).get();
					if(MiscUtils.isEmpty(values)){
						continue;
					}
					try{
						Map<String,Object> courses = new HashMap<String,Object>();
						courses.put("course_id", courseId);
						courses.put("student_num", MiscUtils.convertObjectToLong(values.get("student_num")));
						courses.put("course_amount", MiscUtils.convertObjectToLong(values.get("course_amount")));
						courses.put("extra_num", MiscUtils.convertObjectToLong(values.get("extra_num")));
						courses.put("extra_amount", MiscUtils.convertObjectToLong(values.get("extra_amount")));
						courses.put("real_student_num", MiscUtils.convertObjectToLong(values.get("real_student_num")));
						courses.put("click_num", MiscUtils.convertObjectToLong(values.get("click_num")));
                    	String real_start_time = values.get("real_start_time");
                    	if(!MiscUtils.isEmpty(real_start_time)){
                    		courses.put("real_start_time",new Date(Long.parseLong(real_start_time)));
                    	}
                    	String end_time = (String)values.get("end_time");
                    	if(!MiscUtils.isEmpty(end_time)){
                    		courses.put("end_time",new Date(Long.parseLong(end_time)));
                    	}						
                    	coursesMapper.updateAfterStudentBuyCourse(courses);						
					}catch(Exception e){
						log.error("Sync courses ["+courseId+"]:"+e.getMessage());
					}
				}
				//t_course_image// Constants.CACHED_KEY_COURSE_PPTS
				if(!MiscUtils.isEmpty(finishCourseKeyMap)){
					for(String courseId:courseData.keySet()){
						if(!finishCourseKeyMap.containsKey(courseId)){
							continue;
						}				
						queryParam.clear();
						queryParam.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
						String pptKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, queryParam);
						String value = jedis.get(pptKey);                   
						if(!MiscUtils.isEmpty(value)){
							Map<String,Object> checkValues = courseImageMapper.findOnePPTByCourseId(courseId);
							if(!MiscUtils.isEmpty(checkValues)){                    		
								continue;
							}
							JSONArray pptList = JSONObject.parseArray(value);
							if(MiscUtils.isEmpty(pptList) || pptList.size()< 1){                    		
								continue;
							}
							List<Map<String,Object>> list = new LinkedList<Map<String,Object>>();
							try{
								for(Object curValue: pptList){
									if(!(curValue instanceof Map)){
										log.warn("The ppt data["+curValue+"] is abnormal");
									} else {
										Map<String,Object> values = (Map<String,Object>)curValue;
										long create_time_lng = MiscUtils.convertObjectToLong(values.get("create_time"));
										long update_time_lng = MiscUtils.convertObjectToLong(values.get("update_time"));
										long image_pos_lng = MiscUtils.convertObjectToLong(values.get("image_pos"));
										if(update_time_lng < 1){
											update_time_lng=create_time_lng;
										}
										values.put("create_time", new Date(create_time_lng));
										values.put("update_time", new Date(update_time_lng));
										values.put("image_pos", image_pos_lng);
										list.add(values);
									}
								}

								Map<String,Object> insertMap = new HashMap<>();
								insertMap.put("course_id",courseId);
								insertMap.put("list",pptList);
								courseImageMapper.createCoursePPTs(insertMap);	                    	
							}catch(Exception e){
								log.error("Sync courses ppt ["+courseId+"]:"+e.getMessage());
							}
						}
					}
				}
				pipeline.sync();
			}







			
			private void updateDistributerData(Set<String> distributerSet, Pipeline pipeline){
				Map<String,Object> queryParam = new HashMap<String,Object>();
				Map<String,Response<Map<String,String>>> distributerMap = new HashMap<String,Response<Map<String,String>>>();
				for(String distributerId:distributerSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, distributerId);
					String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_DISTRIBUTER,queryParam);
					distributerMap.put(distributerId, pipeline.hgetAll(key));
				}
				pipeline.sync();
				
				Map<String,Object> distributerValues = new HashMap<String,Object>();
				//t_distributer
				for(String distributerId:distributerMap.keySet()){
					Map<String,String> values = distributerMap.get(distributerId).get();
					if(MiscUtils.isEmpty(values)){
						continue;
					}
					try{
						distributerValues.clear();
						distributerValues.put("distributer_id", distributerId);
						distributerValues.put("total_amount", MiscUtils.convertObjectToLong(values.get("total_amount")));
						distributerMapper.updateDistributer(distributerValues);
					}catch(Exception e){
						//TODO
					}
				}
			}
			
			private void updateRoomDistributerData(Set<String> rqCodeSet, Pipeline pipeline){
				Map<String,Object> queryParam = new HashMap<String,Object>();
				Map<String,Response<Map<String,String>>> rqCodeMap = new HashMap<String,Response<Map<String,String>>>();
				for(String rqCode:rqCodeSet){
					queryParam.clear();					
					queryParam.put(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE_FIELD, rqCode);
					String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE, queryParam);					
					rqCodeMap.put(rqCode, pipeline.hgetAll(key));				
				}
				pipeline.sync();
				
				Map<String,Response<Map<String,String>>> roomDistributerMap = new HashMap<String,Response<Map<String,String>>>();
				for(String rqCode:rqCodeMap.keySet()){
					Map<String,String> values = rqCodeMap.get(rqCode).get();
					if(MiscUtils.isEmpty(values)){
						continue;
					}					
					queryParam.clear();					
					queryParam.put("distributer_id", values.get("distributer_id"));
					queryParam.put("room_id", values.get("room_id"));
					String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, queryParam);					
					roomDistributerMap.put(rqCode, pipeline.hgetAll(key));
				}
				pipeline.sync();
				if(MiscUtils.isEmpty(roomDistributerMap)){
					return;
				}
				//t_room_distributer
				Map<String,Object> roomDistributerValues = new HashMap<String,Object>();
				Map<String,Map<String,String>> roomDistributerDetailsMap = new HashMap<String,Map<String,String>>();				
				for(String rqCode:roomDistributerMap.keySet()){
					Map<String,String> values = roomDistributerMap.get(rqCode).get();
					if(MiscUtils.isEmpty(values)){
						continue;
					}
					try{
						roomDistributerValues.clear();
						Map<String,Object> roomDistributer = new HashMap<String,Object>();
						roomDistributer.put("room_distributer_id", values.get("room_distributer_id"));
						roomDistributer.put("recommend_num", MiscUtils.convertObjectToLong(values.get("recommend_num")));
						roomDistributer.put("course_num", MiscUtils.convertObjectToLong(values.get("course_num")));
						roomDistributer.put("done_num", MiscUtils.convertObjectToLong(values.get("done_num")));
						roomDistributer.put("profit_share_rate", MiscUtils.convertObjectToLong(values.get("profit_share_rate")));
						roomDistributer.put("effective_time", MiscUtils.convertObjectToLong(values.get("effective_time")));
						roomDistributer.put("click_num", MiscUtils.convertObjectToLong(values.get("click_num")));
						roomDistributerDetailsMap.put(values.get("room_distributer_details_id"), values);
						roomDistributerMapper.updateRoomDistributer(roomDistributer);
					}catch(Exception e){
						//TODO
					}
				}
				if(MiscUtils.isEmpty(roomDistributerDetailsMap)){
					return;
				}
				for(String roomDistributerDetailsId : roomDistributerDetailsMap.keySet()){
					Map<String,Object> roomDistributerDetails = new HashMap<String,Object>();
					Map<String,String> values = roomDistributerDetailsMap.get(roomDistributerDetailsId);
					if(MiscUtils.isEmpty(values)){
						continue;
					}
					roomDistributerDetails.put("done_time", values.get("done_time"));
					roomDistributerDetails.put("click_num", values.get("click_num"));
					roomDistributerDetails.put("recommend_num", values.get("last_recommend_num"));
					roomDistributerDetails.put("course_num", values.get("last_course_num"));
					roomDistributerDetails.put("done_num", values.get("last_done_num"));
					roomDistributerDetails.put("total_amount", values.get("last_total_amount"));
					roomDistributerDetails.put("room_distributer_details_id", roomDistributerDetailsId);
					roomDistributerDetailsMapper.updateRoomDistributerDetails(roomDistributerDetails);
				}
			}
    	});
    	
    	
    }

    public CoursesMapper getCoursesMapper() {
        return coursesMapper;
    }

    public void setCoursesMapper(CoursesMapper coursesMapper) {
        this.coursesMapper = coursesMapper;
    }

    public LiveRoomMapper getLiveRoomMapper() {
        return liveRoomMapper;
    }

    public void setLiveRoomMapper(LiveRoomMapper liveRoomMapper) {
        this.liveRoomMapper = liveRoomMapper;
    }

    public LecturerMapper getLecturerMapper() {
        return lecturerMapper;
    }

    public void setLecturerMapper(LecturerMapper lecturerMapper) {
        this.lecturerMapper = lecturerMapper;
    }

    public LoginInfoMapper getLoginInfoMapper() {
        return loginInfoMapper;
    }

    public void setLoginInfoMapper(LoginInfoMapper loginInfoMapper) {
        this.loginInfoMapper = loginInfoMapper;
    }

	public void setDistributerMapper(DistributerMapper distributerMapper) {
		this.distributerMapper = distributerMapper;
	}

	public void setLecturerDistributionInfoMapper(LecturerDistributionInfoMapper lecturerDistributionInfoMapper) {
		this.lecturerDistributionInfoMapper = lecturerDistributionInfoMapper;
	}

	public void setRoomDistributerMapper(RoomDistributerMapper roomDistributerMapper) {
		this.roomDistributerMapper = roomDistributerMapper;
	}

	public void setCourseImageMapper(CourseImageMapper courseImageMapper) {
		this.courseImageMapper = courseImageMapper;
	}
	
	public void setLecturerDistributionLinkMapper(LecturerDistributionLinkMapper lecturerDistributionLinkMapper){
		this.lecturerDistributionLinkMapper=lecturerDistributionLinkMapper;
	}

	public void setRoomDistributerDetailsMapper(RoomDistributerDetailsMapper roomDistributerDetailsMapper) {
		this.roomDistributerDetailsMapper = roomDistributerDetailsMapper;
	}
	
	public void setUserMapper(UserMapper userMapper){
		this.userMapper = userMapper;
	}

	public void setLecturerCoursesProfitMapper(LecturerCoursesProfitMapper lecturerCoursesProfitMapper) {
		this.lecturerCoursesProfitMapper = lecturerCoursesProfitMapper;
	}

	public SaaSCourseMapper getSaaSCourseMapper() {
		return saaSCourseMapper;
	}

	public void setSaaSCourseMapper(SaaSCourseMapper saaSCourseMapper) {
		this.saaSCourseMapper = saaSCourseMapper;
	}
}
