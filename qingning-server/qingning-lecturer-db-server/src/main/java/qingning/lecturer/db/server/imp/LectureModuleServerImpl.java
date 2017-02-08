
package qingning.lecturer.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.entity.QNLiveException;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.ILectureModuleServer;

import java.util.*;

public class LectureModuleServerImpl implements ILectureModuleServer {

	@Autowired(required = true)
	private FansMapper fansMapper;

	@Autowired(required = true)
	private LecturerMapper lecturerMapper;

	@Autowired(required = true)
	private LiveRoomMapper liveRoomMapper;

	@Autowired(required = true)
	private UserMapper userMapper;

	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;

	@Autowired(required = true)
	private CoursesMapper coursesMapper;

	@Autowired(required = true)
	private CourseImageMapper courseImageMapper;

	@Autowired(required = true)
	private CourseAudioMapper courseAudioMapper;

	@Autowired(required = true)
	private CourseMessageMapper courseMessageMapper;

	@Autowired(required = true)
	private CoursesStudentsMapper coursesStudentsMapper;
	
	@Autowired(required = true)
	private DistributerMapper distributerMapper;

	@Autowired(required = true)
	private LecturerDistributionInfoMapper lecturerDistributionInfoMapper;
	
	@Autowired(required = true)
	private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;
	
	@Autowired(required = true)
	private RoomDistributerMapper roomDistributerMapper;
	
	@Autowired(required = true)
	private RoomDistributerCoursesMapper roomDistributerCoursesMapper;
	
	@Autowired(required = true)
	private RoomDistributerDetailsMapper roomDistributerDetailsMapper;
	
	@Autowired(required = true)
	private LecturerDistributionLinkMapper lecturerDistributionLinkMapper;
	
	@Transactional(rollbackFor=Exception.class)
	@Override
	/**
	 * 创建直播间
	 */
	public Map<String,Object> createLiveRoom(Map<String, Object> reqMap) {
		Date now = new Date();
		//1.插入直播间表
		Map<String,Object> liveRoom = new HashMap<String,Object>();
		liveRoom.put("room_id", reqMap.get("room_id"));
		liveRoom.put("user_id", reqMap.get("user_id"));
		liveRoom.put("rq_code", reqMap.get("room_id"));
		liveRoom.put("room_address", reqMap.get("room_address"));
		liveRoom.put("room_name", reqMap.get("room_name"));
		liveRoom.put("avatar_address", reqMap.get("avatar_address"));
		liveRoom.put("room_remark", reqMap.get("room_remark"));
		liveRoom.put("lecturer_id", reqMap.get("user_id"));
		liveRoom.put("create_time", now);		
		liveRoom.put("update_time", now);
		liveRoomMapper.insertLiveRoom(liveRoom);

		//2.如果该用户为普通用户，则需要插入讲师表，并且修改登录信息表中的身份，
		// 同时插入t_lecturer_distribution_info讲师分销信息表(统计冗余表)
		boolean isLecturer = (Boolean)reqMap.get("isLecturer");
		if(isLecturer == false){
			//2.1插入讲师表
			Map<String,Object> lecturer = new HashMap<String,Object>();
			lecturer.put("lecturer_id", reqMap.get("user_id"));
			lecturer.put("live_room_num", 1L);
			lecturer.put("create_time", now);
			lecturer.put("update_time", now);
			lecturerMapper.insertLecture(lecturer);

			//2.2修改登录信息表 身份
			Map<String,Object> updateMap = new HashMap<>();
			updateMap.put("user_id",reqMap.get("user_id").toString());
			updateMap.put("add_role",","+ Constants.USER_ROLE_LECTURER);
			loginInfoMapper.updateUserRole(updateMap);

			//2.3插入讲师分销信息表(统计冗余表)
			Map<String,Object> lecturerDistributionInfo = new HashMap<String,Object>();
			lecturerDistributionInfo.put("lecturer_id", reqMap.get("user_id"));
			lecturerDistributionInfo.put("create_time", now);
			lecturerDistributionInfo.put("update_time", now);			
			lecturerDistributionInfoMapper.insertLecturerDistributionInfo(lecturerDistributionInfo);
		}

		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("room_id", reqMap.get("room_id"));		
		return resultMap;
	}

	@Override
	public Map<String, Object> findLectureByLectureId(String lecture_id) {
		return lecturerMapper.findLectureByLectureId(lecture_id);
	}

	@Override
	public Map<String, Object> findLiveRoomByRoomId(String room_id) {
		return liveRoomMapper.findLiveRoomByRoomId(room_id);
	}

	@Override
	/**
	 * 更新直播间
	 */
	public Map<String, Object> updateLiveRoom(Map<String, Object> reqMap) {
		Date date = new Date(System.currentTimeMillis());
		Date last_update_time = null;
		if(!MiscUtils.isEmpty(reqMap.get("update_time"))){
			last_update_time = new Date(MiscUtils.convertObjectToLong(reqMap.get("update_time")));
		}
		reqMap.put("update_time", date);
		reqMap.put(Constants.SYS_FIELD_LAST_UPDATE_TIME, last_update_time);
		
		Integer updateCount = liveRoomMapper.updateLiveRoom(reqMap);
		
		Map<String,Object> dbResultMap = new HashMap<String,Object>();
		dbResultMap.put("updateCount", updateCount);
		dbResultMap.put("update_time", date);
		return dbResultMap;
	}

	@Override
	public Map<String,Object> createCourse(Map<String, Object> reqMap) {
		Map<String,Object> course = new HashMap<String,Object>();
		course.put("course_id", MiscUtils.getUUId());
		course.put("room_id", reqMap.get("room_id"));
		course.put("lecturer_id", reqMap.get("user_id"));
		course.put("course_title", reqMap.get("course_title"));
		course.put("course_url", reqMap.get("course_url"));
		Date startTime = new Date(MiscUtils.convertObjectToLong(reqMap.get("start_time")));
		course.put("start_time", startTime);
		String course_type = (String)reqMap.get("course_type");
		course.put("course_type", reqMap.get("course_type"));
		course.put("status", "1");
		course.put("rq_code", course.get("course_id"));
		course.put("room_id", reqMap.get("room_id"));
		if("1".equals(course_type)){
			course.put("course_password",reqMap.get("course_password").toString());
		}else if("2".equals(course_type)){
			course.put("course_price", (Long)reqMap.get("course_price"));
		}
		Date now = new Date();
		course.put("create_time", now);
		course.put("create_date", now);
		course.put("update_time", now);
		course.put("im_course_id", reqMap.get("im_course_id"));

		coursesMapper.insertCourse(course);

		Map<String ,Object> dbResultMap = new HashMap<String,Object>();
		dbResultMap.put("course_id",course.get("course_id"));
		return dbResultMap;
	}

	@Override
	public Map<String, Object> findCourseByCourseId(String courseId) {
		return coursesMapper.findCourseByCourseId(courseId);
	}

	@Override
	public Map<String, Object> updateCourse(Map<String, Object> reqMap) {
		Integer updateCount = null;
		Date now = (Date)reqMap.get("now");
		Map<String,Object> course = new HashMap<String,Object>();
		course.put("course_id", reqMap.get("course_id"));
		
		if("2".equals(reqMap.get("status"))){
			course.put("end_time", now);
			course.put("status", "2");			
		}else {
			Object course_title = reqMap.get("course_title");
			Object start_time = reqMap.get("start_time");
			if(!MiscUtils.isEmpty(course_title)){
				course.put("course_title", course_title);
			}
			if(!MiscUtils.isEmpty(start_time)){
				course.put("start_time", new Date(MiscUtils.convertObjectToLong(start_time)));
			}
			course.put("course_remark", reqMap.get("course_remark"));
			course.put("course_url", reqMap.get("course_url"));
			course.put("course_password", reqMap.get("course_password"));
			if(!MiscUtils.isEmpty(reqMap.get("update_time"))){
				course.put(Constants.SYS_FIELD_LAST_UPDATE_TIME, new Date(MiscUtils.convertObjectToLong(reqMap.get("update_time"))));
			}		
		}
		course.put("update_time", now);		
		coursesMapper.updateCourse(course);
		
		Map<String, Object> dbResultMap = new HashMap<String, Object>();
		dbResultMap.put("update_count", updateCount);
		dbResultMap.put("update_time", now);
		return dbResultMap;		
	}

	@Override
	public List<Map<String, Object>> findCourseListForLecturer(Map<String, Object> queryMap) {
		queryMap.put("status","2");
		queryMap.put("orderType","2");
		return coursesMapper.findCourseListForLecturer(queryMap);
	}

	@Override
	public void createCoursePPTs(Map<String, Object> reqMap) {
		courseImageMapper.createCoursePPTs(reqMap);
	}

	@Override
	public void deletePPTByCourseId(String course_id) {
		courseImageMapper.deletePPTByCourseId(course_id);
	}

	@Override
	public List<Map<String, Object>> findPPTListByCourseId(String course_id) {
		return courseImageMapper.findPPTListByCourseId(course_id);
	}

	@Override
	public List<Map<String, Object>> findAudioListByCourseId(String course_id) {
		return courseAudioMapper.findAudioListByCourseId(course_id);
	}

	@Override
	public List<Map<String, Object>> findCourseMessageList(Map<String, Object> queryMap) {
		return courseMessageMapper.findCourseMessageList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findCourseStudentList(Map<String, Object> queryMap) {
		return coursesStudentsMapper.findCourseStudentList(queryMap);
	}

	@Override
	public Map<String, Object> findLoginInfoByUserId(String userId) {
		return loginInfoMapper.findLoginInfoByUserId(userId);
	}

	@Override
	public List<Map<String, Object>> findBanUserListInfo(Map<String,Object> banUserIdList) {
		return coursesStudentsMapper.findBanUserListInfo(banUserIdList);
	}

	@Override
	public Map<String,Object> findCourseMessageMaxPos(String course_id) {
		return courseMessageMapper.findCourseMessageMaxPos(course_id);
	}

	@Override
	public List<Map<String, Object>> findCourseProfitList(Map<String, Object> queryMap) {
		return lecturerCoursesProfitMapper.findCourseProfitList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerInfo(Map<String, Object> paramters) {
		return roomDistributerMapper.findRoomDistributerInfo(paramters);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerCourseInfo(Map<String, Object> paramters) {
		return roomDistributerCoursesMapper.findRoomDistributerCourseInfo(paramters);
	}
	
	@Transactional(rollbackFor=Exception.class)
	@Override
	public void createRoomDistributer(Map<String, String> reqMap) throws Exception {
		Map<String,Object> record = new HashMap<String,Object>();		
		record.put("room_id", reqMap.get("room_id"));
		record.put("distributer_id", reqMap.get("distributer_id"));			
		Date date = new Date(System.currentTimeMillis());
		record.put("current_time", date);
		//1.插入t_distributer
		try{
			distributerMapper.insertDistributer(record);
		}catch(Exception e){			
		}
		
		 //2.插入t_room_distributer    
		Map<String,Object> distributer = roomDistributerMapper.findRoomDistributer(record);
		Map<String,Object> roomDistributer = new HashMap<String,Object>();
		roomDistributer.put("done_time", date);
		roomDistributer.put("click_num", 0l);
		String room_distributer_details_id = MiscUtils.getUUId();
		roomDistributer.put("lecturer_distribution_id", reqMap.get("lecturer_distribution_id"));
		roomDistributer.put("room_distributer_details_id", room_distributer_details_id);
		roomDistributer.put("profit_share_rate", MiscUtils.convertObjectToLong(reqMap.get("profit_share_rate")));		
        String effective_time = reqMap.get("effective_time");
        roomDistributer.put("effective_time", reqMap.get("effective_time"));
        roomDistributer.put("rq_code", room_distributer_details_id);
        Calendar calEndDate = Calendar.getInstance();
        calEndDate.setTime(date);
        if("1".equals(effective_time)){
        	calEndDate.add(Calendar.MONTH, 1);
        } else if("2".equals(effective_time)){
        	calEndDate.add(Calendar.MONTH, 3);
        } else if("3".equals(effective_time)){
        	calEndDate.add(Calendar.MONTH, 6);
        } else if("4".equals(effective_time)){
        	calEndDate.add(Calendar.MONTH, 9);
        } else if("5".equals(effective_time)){
        	calEndDate.add(Calendar.YEAR, 1);
        } else if("6".equals(effective_time)){
        	calEndDate.add(Calendar.YEAR, 2);
        } else {
        	calEndDate = null;
        }
        if(calEndDate!=null){
        	roomDistributer.put("end_date",calEndDate.getTime());
        } else {
        	roomDistributer.put("end_date",null);
        } 
		if(!MiscUtils.isEmpty(distributer)){
			roomDistributer.put("room_distributer_id", distributer.get("room_distributer_id"));
			roomDistributer.put("update_time", date);
			roomDistributer.put("last_update_time", distributer.get("update_time"));
			roomDistributer.put("last_recommend_num", 0l);
			roomDistributer.put("last_course_num", 0l);
			roomDistributer.put("last_done_num", 0l);
			roomDistributer.put("last_total_amount", 0l);
			int count = roomDistributerMapper.updateRoomDistributer(roomDistributer);
			if(count<1){
				throw new QNLiveException("100027");
			}
			for(String key:roomDistributer.keySet()){
				distributer.put(key, roomDistributer.get(key));
			}
		} else {
			roomDistributer.put("room_distributer_id", MiscUtils.getUUId());
			roomDistributer.put("room_id", reqMap.get("room_id"));
			roomDistributer.put("lecturer_id", reqMap.get("lecturer_id"));
			roomDistributer.put("distributer_id", reqMap.get("distributer_id"));
			roomDistributer.put("update_time", date);
			roomDistributer.put("create_time", date);
			roomDistributerMapper.insertRoomDistributer(roomDistributer);
			distributer=roomDistributer;
		}
		Map<String,Object> roomDistributerDetails = new HashMap<String,Object>();
		roomDistributerDetails.put("lecturer_distribution_id", distributer.get("lecturer_distribution_id"));

		roomDistributerDetails.put("room_distributer_details_id", room_distributer_details_id);
		roomDistributerDetails.put("distributer_id", distributer.get("distributer_id"));
		roomDistributerDetails.put("lecturer_id", distributer.get("lecturer_id"));
		roomDistributerDetails.put("room_id", distributer.get("room_id"));
		roomDistributerDetails.put("effective_time", distributer.get("effective_time"));
		roomDistributerDetails.put("end_date", distributer.get("end_date"));
		roomDistributerDetails.put("create_date", date);
		roomDistributerDetails.put("update_time", date);
		roomDistributerDetails.put("done_time", date);
		roomDistributerDetails.put("profit_share_rate", MiscUtils.convertObjectToLong(reqMap.get("profit_share_rate")));
		roomDistributerDetails.put("rq_code", room_distributer_details_id);
		roomDistributerDetails.put("create_time", date);
		roomDistributerDetails.put("status", "0");
		roomDistributerDetailsMapper.insertRoomDistributerDetails(roomDistributerDetails);
	}

	@Override
	public List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap) {
		return coursesStudentsMapper.findLatestStudentAvatarAddList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
		return liveRoomMapper.findLiveRoomByLectureId(lecture_id);
	}
    //TODO
	@Override
	public Map<String, Object> findDistributerInfo(Map<String, Object> paramters) {
		return null;
	}

	@Override
	public List<Map<String, Object>> findRoomFanList(Map<String, Object> paramters) {
		return fansMapper.findRoomFanList(paramters);
	}

	@Override
	public Map<String, Object> findLecturerDistributionByLectureId(String user_id) {
		return lecturerDistributionInfoMapper.findLecturerDistributionByLectureId(user_id);
	}

	@Override
	public Map<String, Object> findUserInfoByUserId(String userId) {
		return userMapper.findByUserId(userId);
	}

	@Override
	public List<Map<String, Object>> findCourseStudentListWithLoginInfo(String course_id){
		Map<String,Object> query = new HashMap<String,Object>();
		query.put("course_id", course_id);
		return coursesStudentsMapper.findCourseStudentListWithLoginInfo(query);
	}

	@Override
	public List<Map<String,Object>> findRoomFanListWithLoginInfo(String roomId) {
		Map<String,Object> query = new HashMap<String,Object>();
		query.put("room_id", roomId);
		return fansMapper.findRoomFanListWithLoginInfo(query);
	}

	@Override
	public List<String> findLoginInfoByUserIds(Map<String, Object> map) {
		return loginInfoMapper.findLoginInfoByUserIds(map);
	}

	@Override
	public int insertLecturerDistributionLink(Map<String, Object> map) {		
		return lecturerDistributionLinkMapper.insertLecturerDistributionLink(map);
	}

	@Override
	public Map<String, Object> findAvailableRoomDistributer(Map<String, Object> record) {
		return roomDistributerMapper.findRoomDistributer(record);
	}
}
