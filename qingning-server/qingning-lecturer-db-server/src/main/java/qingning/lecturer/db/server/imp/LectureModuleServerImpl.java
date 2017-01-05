
package qingning.lecturer.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;


import qingning.common.entity.QNLiveException;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.lecturer.db.persistence.mybatis.*;
import qingning.lecturer.db.persistence.mybatis.entity.*;
import qingning.server.rpc.manager.ILectureModuleServer;

import java.math.BigDecimal;
import java.util.*;

public class LectureModuleServerImpl implements ILectureModuleServer {

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
	
	@Override
	/**
	 * 创建直播间
	 */
	public Map<String,Object> createLiveRoom(Map<String, Object> reqMap) {
		Map<String,Object> userMap = null;
		Date now = new Date();
		//1.插入直播间表
		LiveRoom liveRoom = new LiveRoom();
		liveRoom.setRoomId(reqMap.get("room_id").toString());
		liveRoom.setLecturerId(reqMap.get("user_id").toString());
		liveRoom.setCourseNum(0L);
		liveRoom.setFansNum(0L);
		liveRoom.setDistributerNum(0L);

		liveRoom.setRqCode(liveRoom.getRoomId());
		liveRoom.setRoomAddress(reqMap.get("room_address").toString());
		liveRoom.setTotalAmount(0L);
		liveRoom.setLastCourseAmount(0L);
		liveRoom.setCreateTime(now);
		liveRoom.setUpdateTime(now);

		//直播间名字、直播间头像地址、直播间简介的相关设置。
		//如果有输入的参数则使用输入的参数，否则使用t_user表中的数据
		userMap = userMapper.findByUserId(reqMap.get("user_id").toString());
		if(reqMap.get("room_name") == null || MiscUtils.isEmpty(reqMap.get("room_name").toString())){
			liveRoom.setRoomName(userMap.get("nick_name").toString() + "的直播间");
		}else {
			liveRoom.setRoomName(reqMap.get("room_name").toString());
		}

		if(reqMap.get("avatar_address") == null){
			liveRoom.setAvatarAddress((userMap.get("avatar_address").toString()));
		}else {
			liveRoom.setAvatarAddress((reqMap.get("avatar_address").toString()));
		}

		if(reqMap.get("room_remark") != null){
			liveRoom.setRoomRemark(reqMap.get("room_remark").toString());
		}
		liveRoomMapper.insert(liveRoom);

		//2.如果该用户为普通用户，则需要插入讲师表，并且修改登录信息表中的身份，
		// 同时插入t_lecturer_distribution_info讲师分销信息表(统计冗余表)
		boolean isLecturer = (Boolean)reqMap.get("isLecturer");
		if(isLecturer == false){
			//2.1插入讲师表
			Lecturer lecturer = new Lecturer();
			lecturer.setLecturerId(reqMap.get("user_id").toString());
			lecturer.setCourseNum(0L);
			lecturer.setTotalStudentNum(0L);
			lecturer.setLiveRoomNum(0L);
			lecturer.setFansNum(0L);
			lecturer.setTotalAmount(0L);
			lecturer.setPayStudentNum(0L);
			lecturer.setTotalTime(0L);
			lecturer.setPayCourseNum(0L);
			lecturer.setPrivateCourseNum(0L);
			lecturer.setCreateTime(now);
			lecturer.setUpdateTime(now);
			lecturerMapper.insert(lecturer);

			//2.2修改登录信息表 身份
			Map<String,Object> updateMap = new HashMap<>();
			updateMap.put("user_id",reqMap.get("user_id").toString());
			updateMap.put("add_role",","+ Constants.USER_ROLE_LECTURER);
			loginInfoMapper.updateUserRole(updateMap);

			//2.3插入讲师分销信息表(统计冗余表)
			LecturerDistributionInfo ldbi = new LecturerDistributionInfo();
			ldbi.setLecturerId(reqMap.get("user_id").toString());
			ldbi.setLiveRoomNum(0L);
			ldbi.setRoomDistributerNum(0L);
			ldbi.setRoomRecommendNum(0L);
			ldbi.setRoomDoneNum(0L);
			ldbi.setCourseDistributionNum(0L);
			ldbi.setCourseDistributerNum(0L);
			ldbi.setCourseRecommendNum(0L);
			ldbi.setCourseDoneNum(0L);
			ldbi.setCreateTime(now);
			ldbi.setUpdateTime(now);
			lecturerDistributionInfoMapper.insert(ldbi);
		}

		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("room_id", liveRoom.getRoomId());
		resultMap.put("nick_name", userMap.get("nick_name").toString());
		return resultMap;
	}

	@Override
	public Map<String, Object> findLectureByLectureId(String user_id) {
		return lecturerMapper.findLectureByLectureId(user_id);
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

		LiveRoom updateLiveRoom = new LiveRoom();
		updateLiveRoom.setRoomId(reqMap.get("room_id").toString());
		updateLiveRoom.setUpdateTime(new Date());

		if(reqMap.get("avatar_address") != null ){
			updateLiveRoom.setAvatarAddress(reqMap.get("avatar_address").toString());
		}
		if(reqMap.get("room_name") != null ){
			updateLiveRoom.setRoomName(reqMap.get("room_name").toString());
		}
		if(reqMap.get("room_remark") != null ){
			updateLiveRoom.setRoomRemark(reqMap.get("room_remark").toString());
		}

		Integer updateCount = liveRoomMapper.updateByPrimaryKeySelective(updateLiveRoom);
		Map<String,Object> dbResultMap = new HashMap<String,Object>();
		dbResultMap.put("updateCount", updateCount);
		dbResultMap.put("update_time", updateLiveRoom.getUpdateTime());
		return dbResultMap;
	}

	@Override
	public Map<String,Object> createCourse(Map<String, Object> reqMap) {
		Courses courses = new Courses();
		courses.setCourseId(MiscUtils.getUUId());
		courses.setRoomId(reqMap.get("room_id").toString());
		courses.setLecturerId(reqMap.get("user_id").toString());
		courses.setCourseTitle(reqMap.get("course_title").toString());
		courses.setCourseUrl(reqMap.get("course_url").toString());
		//courses.setCourseRemark();
		Date startTime = new Date(Long.parseLong(reqMap.get("start_time").toString()));
		courses.setStartTime(startTime);
		courses.setCourseType(reqMap.get("course_type").toString());
		courses.setStatus("1");
		courses.setRqCode(courses.getCourseId());

		if(reqMap.get("course_type").toString().equals("1")){
			courses.setCoursePassword(reqMap.get("course_password").toString());
		}else if(reqMap.get("course_type").toString().equals("2")){
			courses.setCoursePrice((Long)reqMap.get("course_price"));
		}

		courses.setStudentNum(0L);
		courses.setCourseAmount(0L);
		courses.setExtraNum(0L);
		courses.setExtraAmount(0L);
		courses.setRealStudentNum(0L);

		Date now = new Date();
		courses.setCreateTime(now);
		courses.setCreateDate(now);
		courses.setUpdateTime(now);
		if(reqMap.get("im_course_id") != null){
			courses.setImCourseId(reqMap.get("im_course_id").toString());
		}
		coursesMapper.insert(courses);

		Map<String ,Object> dbResultMap = new HashMap<String,Object>();
		dbResultMap.put("course_id",courses.getCourseId());
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
		if(reqMap.get("status") != null && reqMap.get("status").toString().equals("2")){
			Courses courses = new Courses();
			courses.setCourseId(reqMap.get("course_id").toString());
			courses.setEndTime(now);
			courses.setUpdateTime(now);
			courses.setStatus("2");
			updateCount = coursesMapper.updateByPrimaryKeySelective(courses);
		}else {
			Courses courses = new Courses();
			courses.setCourseId(reqMap.get("course_id").toString());
			if(reqMap.get("course_title") != null){
				courses.setCourseTitle(reqMap.get("course_title").toString());
			}
			if(reqMap.get("start_time") != null){
				courses.setStartTime(new Date(Long.parseLong(reqMap.get("start_time").toString())));
			}
			if(reqMap.get("course_remark") != null){
				courses.setCourseRemark(reqMap.get("course_remark").toString());
			}
			if(reqMap.get("course_url") != null){
				courses.setCourseUrl(reqMap.get("course_url").toString());
			}
			if(reqMap.get("course_password") != null){
				courses.setCoursePassword(reqMap.get("course_password").toString());
			}
			courses.setUpdateTime(now);
			updateCount = coursesMapper.updateByPrimaryKeySelective(courses);
		}

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
		courseImageMapper.batchInsertPPT(reqMap);
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
	public Map<String, Object> findLoginInfoByUserId(String user_id) {
		return loginInfoMapper.findLoginInfoByUserId(user_id);
	}

	@Override
	public List<Map<String, Object>> findBanUserListInfo(Set<String> banUserIdList) {

		return coursesStudentsMapper.findBanUserListInfo(banUserIdList);
	}

	@Override
	public Map<String,Object> findCourseMessageMaxPos(String course_id) {
		return courseMessageMapper.findCourseMessageMaxPos(course_id);
	}

	@Override
	public List<Map<String, Object>> findCourseProfitList(Map<String, Object> queryMap) {		
		return coursesMapper.findCourseProfitList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerInfo(Map<String, Object> paramters) {
		return liveRoomMapper.findRoomDistributerInfo(paramters);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerCourseInfo(Map<String, Object> paramters) {
		return liveRoomMapper.findRoomDistributerCourseInfo(paramters);
	}

	@Override
	public void createRoomDistributer(Map<String, String> reqMap) throws Exception{		 
		Map<String,Object> record = new HashMap<String,Object>();		
		record.put("room_id", reqMap.get("room_id"));
		record.put("distributer_id", reqMap.get("distributer_id"));
		Map<String,Object> distributer = distributerMapper.findDistributerInfo(record);
		boolean update =false;
		Date date = new Date(System.currentTimeMillis());
		if(!MiscUtils.isEmpty(distributer)){
			if("0".equals(distributer.get("effective_time"))){
				throw new QNLiveException("100027");
			} else {
				Date endDate = (Date)distributer.get("end_date");
				if(endDate !=null && endDate.after(date)){
					throw new QNLiveException("100027");
				}
			}
			update =true;
		}	
		record.put("current_time", date);
		//1.插入t_distributer
		try{
			if(!update){
				distributerMapper.insertDistributer(record);
			}
		}catch(Exception e){			
		}
		if(!update){
			record.put("room_distributer_id", MiscUtils.getUUId());
	        record.put("room_id", reqMap.get("room_id"));
	        record.put("lecturer_id", reqMap.get("lecturer_id"));
		} else {
			record.put("room_distributer_id", distributer.get("room_distributer_id"));
		}
        //2.插入t_room_distributer        
        record.put("profit_share_rate", Double.parseDouble(reqMap.get("profit_share_rate")));
        String effective_time = reqMap.get("effective_time");
        record.put("effective_time", reqMap.get("effective_time"));
        record.put("rq_code", MiscUtils.getUUId());
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
        	record.put("end_date",calEndDate.getTime());
        } else {
        	record.put("end_date",null);
        }   
        if(!update){
        	distributerMapper.insertRoomDistributer(record);
        } else {
        	distributerMapper.updateRoomDistributerbyPrimaryKey(record);
        }
	}

	@Override
	public List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap) {
		return coursesStudentsMapper.findLatestStudentAvatarAddList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
		return liveRoomMapper.findLiveRoomByLectureId(lecture_id);
	}

	@Override
	public Map<String, Object> findDistributerInfo(Map<String, Object> paramters) {		
		return distributerMapper.findDistributerInfo(paramters);
	}

	@Override
	public List<Map<String, Object>> findRoomFanList(Map<String, Object> paramters) {		
		return liveRoomMapper.findRoomFanList(paramters);
	}

	@Override
	public Map<String, Object> findLecturerDistributionByLectureId(String user_id) {
		return lecturerDistributionInfoMapper.findLecturerDistributionByLectureId(user_id);
	}
}
