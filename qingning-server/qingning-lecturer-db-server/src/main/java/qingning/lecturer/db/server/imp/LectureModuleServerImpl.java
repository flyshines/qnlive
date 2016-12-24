
package qingning.lecturer.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import qingning.common.util.MiscUtils;
import qingning.lecturer.db.persistence.mybatis.*;
import qingning.lecturer.db.persistence.mybatis.entity.Courses;
import qingning.lecturer.db.persistence.mybatis.entity.Lecturer;
import qingning.lecturer.db.persistence.mybatis.entity.LiveRoom;
import qingning.lecturer.db.persistence.mybatis.entity.LoginInfo;
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

	@Override
	/**
	 * 创建直播间
	 */
	public Map<String,Object> createLiveRoom(Map<String, Object> reqMap) {
		Map<String,Object> userMap = null;
		Date now = new Date();
		//1.插入直播间表
		LiveRoom liveRoom = new LiveRoom();
		liveRoom.setRoomId(MiscUtils.getUUId());
		liveRoom.setLecturerId(reqMap.get("user_id").toString());
		liveRoom.setCourseNum(0L);
		liveRoom.setFansNum(0L);
		liveRoom.setDistributerNum(0L);

		liveRoom.setRqCode(liveRoom.getRoomId());
		liveRoom.setRoomAddress(reqMap.get("room_address").toString() + liveRoom.getRoomId());//TODO
		liveRoom.setTotalAmount(0.0);
		liveRoom.setLastCourseAmount(0.0);
		liveRoom.setCreateTime(now);
		liveRoom.setUpdateTime(now);

		//直播间名字、直播间头像地址、直播间简介的相关设置。
		//如果有输入的参数则使用输入的参数，否则使用t_user表中的数据
		if(reqMap.get("room_name") == null || reqMap.get("avatar_address") == null
				|| reqMap.get("room_remark") == null){
			userMap = userMapper.findByUserId(reqMap.get("user_id").toString());
		}
		if(reqMap.get("room_name") == null){
			liveRoom.setRoomName(userMap.get("nick_name").toString() + "的直播间");
		}else {
			liveRoom.setRoomName(reqMap.get("room_name").toString() + "的直播间");
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

		//2.如果该用户为普通用户，则需要插入讲师表，并且修改登录信息表中的身份
		if(reqMap.get("user_role") == null || reqMap.get("user_role").toString().split(",").length == 1){
			//2.1插入讲师表
			Lecturer lecturer = new Lecturer();
			lecturer.setLecturerId(reqMap.get("user_id").toString());
			lecturer.setCourseNum(0L);
			lecturer.setTotalStudentNum(0L);
			lecturer.setLiveRoomNum(0L);
			lecturer.setFansNum(0L);
			lecturer.setTotalAmount(0.0);
			lecturer.setCreateTime(now);
			lecturer.setUpdateTime(now);
			lecturerMapper.insert(lecturer);

			//2.2修改登录信息表 身份
			LoginInfo updateLoginInfo = new LoginInfo();
			updateLoginInfo.setUserId(reqMap.get("user_id").toString());
			updateLoginInfo.setUserRole("normal_user,lecture");
			updateLoginInfo.setUpdateTime(now);
			loginInfoMapper.updateByPrimaryKeySelective(updateLoginInfo);
		}

		Map<String,Object> resultMap = new HashMap<String,Object>();
		resultMap.put("room_id", liveRoom.getRoomId());
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
			courses.setCoursePrice((new BigDecimal(reqMap.get("course_price").toString())).doubleValue());
		}

		courses.setStudentNum(0L);
		courses.setCourseAmount(0.0);
		courses.setExtraNum(0L);
		courses.setExtraAmount(0.0);
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
	public long findCourseMessageMaxPos(String course_id) {
		return courseMessageMapper.findCourseMessageMaxPos(course_id);
	}

}
