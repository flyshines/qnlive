package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;

public interface IUserModuleServer {

	Map<String,Object> userFollowRoom(Map<String, Object> reqMap);

	void updateLiveRoomNumForUser(Map<String, Object> reqMap);


	Map<String,Object> findLiveRoomByRoomId(String room_id);


	Map<String,Object> findFansByFansKey(Map<String, Object> reqMap);

	Map<String,Object> findCourseByCourseId(String string);

	List<Map<String,Object>> findCourseListForLecturer(Map<String, Object> queryMap);


	Map<String,Object> findStudentByKey(Map<String, Object> studentQueryMap);

	Map<String,Object> joinCourse(Map<String, String> courseMap);

	void increaseStudentNumByCourseId(String course_id);

	List<Map<String,Object>> findPPTListByCourseId(String string);

	List<Map<String,Object>> findAudioListByCourseId(String string);

	List<Map<String,Object>> findRewardConfigurationList();


	List<Map<String,Object>> findCourseMessageList(Map<String, Object> queryMap);

	List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);

	Map<String,Object> findLoginInfoByUserId(String user_id);
}
