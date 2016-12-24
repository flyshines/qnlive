package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ILectureModuleServer {
	Map<String,Object> createLiveRoom(Map<String, Object> reqMap);

	Map<String,Object> findLectureByLectureId(String lecture_id);

	Map<String,Object> findLiveRoomByRoomId(String room_id);

	Map<String,Object> updateLiveRoom(Map<String, Object> reqMap);

	Map<String,Object> createCourse(Map<String, Object> reqMap);

	Map<String,Object> findCourseByCourseId(String string);

	Map<String,Object> updateCourse(Map<String, Object> reqMap);

	List<Map<String,Object>> findCourseListForLecturer(Map<String, Object> queryMap);

	void createCoursePPTs(Map<String, Object> reqMap);

	void deletePPTByCourseId(String string);

	List<Map<String,Object>> findPPTListByCourseId(String course_id);

	List<Map<String,Object>> findAudioListByCourseId(String course_id);

	List<Map<String,Object>> findCourseMessageList(Map<String, Object> queryMap);

	List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);

	Map<String,Object> findLoginInfoByUserId(String userId);

	List<Map<String,Object>> findBanUserListInfo(Set<String> banUserIdList);

	long findCourseMessageMaxPos(String course_id);
}
