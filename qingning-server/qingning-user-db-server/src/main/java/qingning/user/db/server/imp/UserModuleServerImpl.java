
package qingning.user.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import qingning.common.entity.QNLiveException;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.IUserModuleServer;

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

	@Autowired(required = true)
	private DistributerMapper distributerMapper;
	@Autowired(required = true)
	private RoomDistributerMapper roomDistributerMapper;
	
	@Autowired(required = true)
	private LecturerMapper lecturerMapper;
	
	@Override
	public Map<String, Object> userFollowRoom(Map<String, Object> reqMap) throws Exception {
		Map<String,Object> dbResultMap = new HashMap<>();
		//follow_type 关注操作类型 0关注 1不关注
		Date now = new Date();
		if("0".equals(reqMap.get("follow_type"))){
			try{
				Map<String,Object> fans = new HashMap<String,Object>();
				fans.put("fans_id", MiscUtils.getUUId());
				fans.put("user_id", reqMap.get("user_id"));
				fans.put("lecturer_id", reqMap.get("lecturer_id"));
				fans.put("room_id", reqMap.get("room_id"));
				fans.put("create_time", now);
				fans.put("create_date", now);				
				Integer updateCount = fansMapper.insertFans(fans);
				dbResultMap.put("update_count", updateCount);
				return dbResultMap;
			} catch(Exception e){
				if(e instanceof DuplicateKeyException){
					throw new QNLiveException("110005");
				} else {
					throw e;
				}
			}
		}else {
			Map<String,Object> updateMap = new HashMap<>();
			updateMap.put("user_id", reqMap.get("user_id").toString());
			updateMap.put("room_id", reqMap.get("room_id").toString());
			Integer updateCount = fansMapper.deleteFans(updateMap);
			dbResultMap.put("update_count", updateCount);
			return dbResultMap;
		}
	}

	@Override
	public void updateLiveRoomNumForUser(Map<String, Object> reqMap) {
		userMapper.updateLiveRoomNumForUser(reqMap);
	}

	@Override
	public Map<String, Object> findUserInfoByUserId(String user_id) {
		return userMapper.findByUserId(user_id);
	}

	@Override
	public Map<String, Object> findLiveRoomByRoomId(String room_id) {
		return liveRoomMapper.findLiveRoomByRoomId(room_id);
	}

	@Override
	public Map<String, Object> findFansByUserIdAndRoomId(Map<String, Object> reqMap) {
		Map<String,Object> fansKey = new HashMap<>();
		fansKey.put("user_id",reqMap.get("user_id").toString());
		fansKey.put("room_id", reqMap.get("room_id").toString());
		return fansMapper.findFansByUserIdAndRoomId(fansKey);
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
	public Map<String, Object> joinCourse(Map<String, String> courseMap) {
		Date now = new Date();
		Map<String,Object> student = new HashMap<String,Object>();
		student.put("student_id", MiscUtils.getUUId());
		student.put("user_id", courseMap.get("user_id"));
		student.put("lecturer_id", courseMap.get("lecturer_id"));
		student.put("room_id", courseMap.get("room_id"));
		student.put("course_id", courseMap.get("course_id"));
		student.put("course_password", courseMap.get("course_password"));
		student.put("student_type", "0"); //TODO distribution case
		student.put("create_time", now);
		student.put("create_date", now);		
		//students.setPaymentAmount();//TODO
		Integer insertCount = coursesStudentsMapper.insertStudent(student);
		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("insertCount", insertCount);
		return resultMap;
	}

	@Override
	public void increaseStudentNumByCourseId(String course_id) {
		coursesMapper.increaseStudent(course_id);
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
	public Map<String, Object> findLoginInfoByUserId(String user_id) {
		return loginInfoMapper.findLoginInfoByUserId(user_id);
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
	public List<Map<String, Object>> findFanInfoByUserId(Map<String, Object> queryMap) {
		return fansMapper.findFanInfoByUserId(queryMap);
	}

	@Override
	public List<Map<String, Object>> findCourseListOfStudent(Map<String, Object> queryMap) {
		return coursesStudentsMapper.findCourseListOfStudent(queryMap);
	}

	@Override
	public List<Map<String, Object>> findUserConsumeRecords(Map<String, Object> queryMap) {
		return lecturerCoursesProfitMapper.findUserConsumeRecords(queryMap);
	}

	@Override
	public List<Map<String, Object>> findDistributionInfoByDistributerId(Map<String, Object> queryMap) {
		return distributerMapper.findDistributionInfoByDistributerId(queryMap);
	}

	@Override
	public Map<String, Object> findAvailableRoomDistributer(Map<String, Object> queryMap) {
		return roomDistributerMapper.findRoomDistributer(queryMap);
	}

	@Override
	public boolean isStudentOfTheCourse(Map<String, Object> studentQueryMap) {
		return !MiscUtils.isEmpty(coursesStudentsMapper.isStudentOfTheCourse(studentQueryMap));
	}

	@Override
	public Map<String, Object> findLectureByLectureId(String lecture_id) {		
		return lecturerMapper.findLectureByLectureId(lecture_id);
	}
	
	@Override
	public List<Map<String,Object>> findFinishCourseListForLecturer(Map<String,Object> record){
		return coursesMapper.findFinishCourseListForLecturer(record);
	}
	
	@Override
	public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {		
		return coursesStudentsMapper.findCourseIdByStudent(reqMap);
	}
}
