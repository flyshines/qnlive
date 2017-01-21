package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;

public interface IUserModuleServer {

	Map<String,Object> userFollowRoom(Map<String, Object> reqMap) throws Exception;

	void updateLiveRoomNumForUser(Map<String, Object> reqMap);
	Map<String,Object> findUserInfoByUserId(String user_id);

	Map<String,Object> findLiveRoomByRoomId(String room_id);


	Map<String,Object> findFansByUserIdAndRoomId(Map<String, Object> reqMap);

	Map<String,Object> findCourseByCourseId(String string);

	List<Map<String,Object>> findCourseListForLecturer(Map<String, Object> queryMap);

	Map<String,Object> joinCourse(Map<String, String> courseMap);

	void increaseStudentNumByCourseId(String course_id);

	List<Map<String,Object>> findPPTListByCourseId(String course_id);

	List<Map<String,Object>> findAudioListByCourseId(String course_id);

	List<Map<String,Object>> findRewardConfigurationList();


	List<Map<String,Object>> findCourseMessageList(Map<String, Object> queryMap);

	List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);

	Map<String,Object> findLoginInfoByUserId(String user_id);

	Map<String,Object> findCourseMessageMaxPos(String course_id);

	List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap);
	
	List<Map<String,Object>> findFanInfoByUserId(Map<String, Object> queryMap);
	
    List<Map<String,Object>> findCourseListOfStudent(Map<String, Object> queryMap);
	
	List<Map<String,Object>> findUserConsumeRecords(Map<String, Object> queryMap);

	List<Map<String,Object>> findDistributionInfoByDistributerId(Map<String, Object> queryMap);

	Map<String,Object> findAvailableRoomDistributer(Map<String, Object> queryMap);

	boolean isStudentOfTheCourse(Map<String, Object> studentQueryMap);
}
