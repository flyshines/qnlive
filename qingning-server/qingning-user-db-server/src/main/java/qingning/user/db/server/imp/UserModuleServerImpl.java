
package qingning.user.db.server.imp;


import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import qingning.server.rpc.manager.IUserModuleServer;
import qingning.user.db.persistence.mybatis.*;
import qingning.user.db.persistence.mybatis.entity.CoursesStudents;
import qingning.user.db.persistence.mybatis.entity.CoursesStudentsKey;
import qingning.user.db.persistence.mybatis.entity.Fans;
import qingning.user.db.persistence.mybatis.entity.FansKey;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserModuleServerImpl implements IUserModuleServer {

	@Autowired(required = true)
	private FansMapper fansMapper;

	@Autowired(required = true)
	private UserMapper userMapper;

	@Autowired(required = true)
	private LiveRoomMapper liveRoomMapper;

	@Autowired(required = true)
	private CoursesMapper coursesMapper;

	@Autowired(required = true)
	private CoursesStudentsMapper coursesStudentsMapper;

	@Autowired(required = true)
	private CourseImageMapper courseImageMapper;

	@Autowired(required = true)
	private CourseAudioMapper courseAudioMapper;

	@Autowired(required = true)
	private RewardConfigurationMapper rewardConfigurationMapper;

	@Autowired(required = true)
	private CourseMessageMapper courseMessageMapper;

	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;

	@Autowired(required = true)
	private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;


	@Override
	public Map<String,Object> userFollowRoom(Map<String, Object> reqMap) {
		Map<String,Object> dbResultMap = new HashMap<String,Object>();

		//follow_type 关注操作类型 0关注 1不关注
		if(reqMap.get("follow_type").toString().equals("0")){
			Date now = new Date();
			Fans fans = new Fans();
			fans.setFansId(reqMap.get("user_id").toString());
			fans.setLecturerId(reqMap.get("lecturer_id").toString());
			fans.setRoomId(reqMap.get("room_id").toString());
			fans.setCreateTime(now);
			fans.setCreateDate(now);
			Integer updateCount = fansMapper.insert(fans);
			dbResultMap.put("update_count", updateCount);
			return dbResultMap;
		}else {
			FansKey fansKey = new FansKey();
			fansKey.setRoomId(reqMap.get("room_id").toString());
			fansKey.setFansId(reqMap.get("user_id").toString());
			fansKey.setLecturerId(reqMap.get("lecturer_id").toString());
			Integer updateCount = fansMapper.deleteByPrimaryKey(fansKey);
			dbResultMap.put("update_count", updateCount);
			return dbResultMap;
		}
	}

	@Override
	public void updateLiveRoomNumForUser(Map<String, Object> reqMap) {
		userMapper.updateLiveRoomNumForUser(reqMap);
	}

	@Override
	public Map<String, Object> findLiveRoomByRoomId(String room_id) {
		return liveRoomMapper.findLiveRoomByRoomId(room_id);
	}

	@Override
	public Map<String, Object> findFansByFansKey(Map<String, Object> reqMap) {
		FansKey fansKey = new FansKey();
		fansKey.setFansId(reqMap.get("user_id").toString());
		fansKey.setRoomId(reqMap.get("room_id").toString());
		return fansMapper.findFansByFansKey(fansKey);
	}

	@Override
	public Map<String, Object> findCourseByCourseId(String string) {
		return coursesMapper.findCourseByCourseId(string);
	}

	@Override
	public List<Map<String, Object>> findCourseListForLecturer(Map<String, Object> queryMap) {
		return coursesMapper.findCourseListForLecturer(queryMap);
	}

	@Override
	public Map<String, Object> findStudentByKey(Map<String,Object> queryStudentMap) {
		CoursesStudentsKey key = new CoursesStudentsKey();
		key.setUserId(queryStudentMap.get("user_id").toString());
		key.setRoomId(queryStudentMap.get("room_id").toString());
		key.setLecturerId(queryStudentMap.get("lecturer_id").toString());
		key.setCourseId(queryStudentMap.get("course_id").toString());
		return coursesStudentsMapper.findStudentByKey(key);
	}

	@Override
	public Map<String, Object> joinCourse(Map<String, String> courseMap) {
		CoursesStudents students = new CoursesStudents();
		students.setUserId(courseMap.get("user_id"));
		students.setLecturerId(courseMap.get("lecturer_id"));
		students.setRoomId(courseMap.get("room_id"));
		students.setCourseId(courseMap.get("course_id"));
		//students.setPaymentAmount();//todo
		if(courseMap.get("course_password") != null){
			students.setCoursePassword(courseMap.get("course_password"));
		}
//		if(StringUtils.isNotBlank(courseMap.get("course_password"))){
//			students.setCoursePassword(courseMap.get("course_password"));
//		}
		students.setStudentType("0");//TODO
		Date now = new Date();
		students.setCreateTime(now);
		students.setCreateDate(now);
		Integer insertCount = coursesStudentsMapper.insert(students);
		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("insertCount", insertCount);
		return resultMap;
	}

	@Override
	public void increaseStudentNumByCourseId(String course_id) {
		coursesMapper.increaseStudentNumByCourseId(course_id);
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
	public List<Map<String, Object>> findRewardConfigurationList() {
		return rewardConfigurationMapper.findRewardConfigurationList();
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
	public Map<String,Object> findCourseMessageMaxPos(String course_id) {
		return courseMessageMapper.findCourseMessageMaxPos(course_id);
	}

	@Override
	public List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap) {
		return coursesStudentsMapper.findLatestStudentAvatarAddList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findUserConsumeRecords(Map<String, Object> queryMap) {
		return lecturerCoursesProfitMapper.findUserConsumeRecords(queryMap);
	}


}
