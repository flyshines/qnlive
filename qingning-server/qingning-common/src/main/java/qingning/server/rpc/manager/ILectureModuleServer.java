package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;

public interface ILectureModuleServer {
	Map<String,Object> createLiveRoom(Map<String, Object> reqMap);

	Map<String,Object> findLectureByLectureId(String lecture_id);

	Map<String,Object> findLiveRoomByRoomId(String room_id);

	Map<String,Object> updateLiveRoom(Map<String, Object> reqMap);

	Map<String,Object> createCourse(Map<String, Object> reqMap);

	Map<String,Object> findCourseByCourseId(String string);

	Map<String,Object> updateCourse(Map<String, Object> reqMap);

	List<Map<String,Object>> findCourseListForLecturer(Map<String, Object> queryMap);
}
