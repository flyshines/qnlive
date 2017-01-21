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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by loovee on 2016/12/20.
 */
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
    
    @Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
    	Jedis jedis = jedisUtils.getJedis();
    	
    	((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
		        //查询出所有需要更新的讲师
				Set<String> lecturerSet = jedis.hkeys(Constants.CACHED_UPDATE_LECTURER_KEY);
				jedis.del(Constants.CACHED_UPDATE_LECTURER_KEY);
				if(!MiscUtils.isEmpty(lecturerSet)){
			        //同步讲师数据
			        updateLecturerData(lecturerSet,pipeline);		        
			        //同步直播间
			        updateLiveRoomData(lecturerSet,pipeline);
			        //同步课程数据
			        updateCourseData(lecturerSet,pipeline);
				}
				
		        //查询出所有需要更新的分销员        
				Set<String> distributerIdSet = jedis.hkeys(Constants.CACHED_UPDATE_DISTRIBUTER_KEY);
				jedis.del(Constants.CACHED_UPDATE_DISTRIBUTER_KEY);
				if(!MiscUtils.isEmpty(distributerIdSet)){
					updateDistributerData(distributerIdSet,pipeline);
				}
				//查询出所有需要更新的分销
				Set<String> roomDistributerIdSet = jedis.hkeys(Constants.CACHED_UPDATE_ROOM_DISTRIBUTER_KEY);
				jedis.del(Constants.CACHED_UPDATE_ROOM_DISTRIBUTER_KEY);
				if(!MiscUtils.isEmpty(roomDistributerIdSet)){
					updateRoomDistributerData(roomDistributerIdSet,pipeline);
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
		        for(String lecturerId : lecturerDataMap.keySet()){
		        	try{
		        		Map<String,String> values = lecturerDataMap.get(lecturerId).get();
		        		if(MiscUtils.isEmpty(values)){
		        			continue;
		        		}
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
		        	} catch (Exception e){
		        		//TODO
		        	}
		        }
		        //t_lecturer_distribution_info
		        for(String lecturerId : lecturerDataMap.keySet()){
		        	try{
		        		Map<String,String> values = lecturerDataMap.get(lecturerId).get();
		        		if(MiscUtils.isEmpty(values)){
		        			continue;
		        		}
		        		Map<String,Object> lecturerDistributionInfo = new HashMap<String,Object>();
		        		lecturerDistributionInfo.put("lecturer_id", lecturerId);
		        		
		        		lecturerDistributionInfo.put("live_room_num", MiscUtils.convertObjectToLong(values.get("live_room_num")));
		        		lecturerDistributionInfo.put("room_distributer_num", MiscUtils.convertObjectToLong(values.get("room_distributer_num")));
		        		lecturerDistributionInfo.put("room_recommend_num", MiscUtils.convertObjectToLong(values.get("room_recommend_num")));		        		
		        		lecturerDistributionInfo.put("room_done_num", MiscUtils.convertObjectToLong(values.get("room_done_num")));
		        		
		        		lecturerDistributionInfo.put("course_distribution_num", MiscUtils.convertObjectToLong(values.get("course_distribution_num")));
		        		lecturerDistributionInfo.put("course_distributer_num", MiscUtils.convertObjectToLong(values.get("course_distributer_num")));
		        		lecturerDistributionInfo.put("course_recommend_num", MiscUtils.convertObjectToLong(values.get("course_recommend_num")));
		        		lecturerDistributionInfo.put("course_done_num", MiscUtils.convertObjectToLong(values.get("course_done_num")));
		        	
		        		lecturerDistributionInfoMapper.updateLecturerDistributionInfo(lecturerDistributionInfo);
		        	} catch (Exception e){
		        		//TODO
		        	}
		        }
			}

			private void updateLiveRoomData(Set<String> lecturerSet, Pipeline pipeline){
		    	Map<String,Object> queryParam = new HashMap<String,Object>();
		    	Map<String,Response<Set<String>>> roomKeyMap = new HashMap<String,Response<Set<String>>>();
				for(String lecturerId : lecturerSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
					String lecturerRoomsKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, queryParam);
					if(!MiscUtils.isEmpty(lecturerRoomsKey)){
						continue;
					}
					roomKeyMap.put(lecturerId, pipeline.hkeys(lecturerRoomsKey));
				}
				pipeline.sync();
				
				Map<String,Response<Map<String,String>>> roomDataMap = new HashMap<String,Response<Map<String,String>>>();
				
				for(String lecturerId : roomKeyMap.keySet()){
					Set<String> liveRoomIdList = roomKeyMap.get(lecturerId).get();
					if(MiscUtils.isEmpty(liveRoomIdList)){
						continue;
					}
					for(String liveRoomId: liveRoomIdList){
						queryParam.clear();
						queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
						queryParam.put(Constants.FIELD_ROOM_ID, liveRoomId);
                        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SPECIAL_LECTURER_ROOM, queryParam);                        
                        roomDataMap.put(liveRoomId, pipeline.hgetAll(liveRoomKey));                        
                        pipeline.hset(liveRoomKey, "room_address",MiscUtils.getConfigByKey("live_room_share_url_pre_fix")+liveRoomId);                        
					}
				}				
				pipeline.sync();
				
				for(String liveRoomId: roomDataMap.keySet()){
					Map<String,String> values = roomDataMap.get(liveRoomId).get();
					if(MiscUtils.isEmpty(values)){
						continue;
					}
					try{						
						Map<String,Object> liveRoom = new HashMap<String,Object>();
						liveRoom.put("room_id", liveRoomId);
						liveRoom.put("fans_num", MiscUtils.convertObjectToLong(values.get("fans_num")));
						liveRoom.put("course_num", MiscUtils.convertObjectToLong(values.get("course_num")));
						liveRoom.put("distributer_num", MiscUtils.convertObjectToLong(values.get("distributer_num")));
						liveRoom.put("total_amount", MiscUtils.convertObjectToLong(values.get("total_amount")));
						liveRoom.put("last_course_amount", MiscUtils.convertObjectToLong(values.get("last_course_amount")));
						liveRoom.put("live_room_share_url_pre_fix", MiscUtils.convertObjectToLong(values.get("live_room_share_url_pre_fix")));						
						liveRoomMapper.updateLiveRoom(liveRoom);						
					} catch(Exception e){
						//TODO
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
						//TODO
					}
				}
				//t_course_image// Constants.CACHED_KEY_COURSE_PPTS
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
                    	if(MiscUtils.isEmpty(checkValues)){                    		
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
                    		log.error(e.getMessage());
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
					distributerMap.put(distributerId, distributerMap.get(distributerId));				
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
			
			private void updateRoomDistributerData(Set<String> roomDistributerSet, Pipeline pipeline){
				Map<String,Object> queryParam = new HashMap<String,Object>();
				Map<String,Response<Map<String,String>>> roomDistributerMap = new HashMap<String,Response<Map<String,String>>>();
				for(String roomDistributerId:roomDistributerSet){
					queryParam.clear();
					queryParam.put(Constants.CACHED_KEY_ROOM_DISTRIBUTER_FIELD, roomDistributerId);
					roomDistributerMap.put(roomDistributerId, roomDistributerMap.get(roomDistributerId));				
				}
				pipeline.sync();
				
				//t_room_distributer
				Map<String,Object> roomDistributerValues = new HashMap<String,Object>();
				for(String roomDistributerId:roomDistributerMap.keySet()){
					Map<String,String> values = roomDistributerMap.get(roomDistributerId).get();
					if(MiscUtils.isEmpty(values)){
						continue;
					}
					try{
						roomDistributerValues.clear();
						Map<String,Object> roomDistributer = new HashMap<String,Object>();
						roomDistributer.put("room_distributer_id", roomDistributerId);
						roomDistributer.put("recommend_num", MiscUtils.convertObjectToLong(values.get("recommend_num")));
						roomDistributer.put("course_num", MiscUtils.convertObjectToLong(values.get("course_num")));
						roomDistributer.put("done_num", MiscUtils.convertObjectToLong(values.get("done_num")));
						roomDistributer.put("profit_share_rate", MiscUtils.convertObjectToLong(values.get("profit_share_rate")));
						roomDistributer.put("effective_time", MiscUtils.convertObjectToLong(values.get("effective_time")));
						roomDistributer.put("click_num", MiscUtils.convertObjectToLong(values.get("click_num")));						
						roomDistributerMapper.updateRoomDistributer(roomDistributer);
					}catch(Exception e){
						//TODO
					}
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
}
