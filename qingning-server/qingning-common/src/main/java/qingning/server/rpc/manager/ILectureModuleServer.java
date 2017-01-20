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

	List<Map<String,Object>> findBanUserListInfo(Map<String, Object> banUserIdList);

	Map<String,Object> findCourseMessageMaxPos(String course_id);
	
	List<Map<String,Object>> findCourseProfitList(Map<String, Object> queryMap);
	
	List<Map<String,Object>> findRoomDistributerInfo(Map<String,Object> paramters);
	
	List<Map<String,Object>> findRoomDistributerCourseInfo(Map<String,Object> paramters);
	
	void createRoomDistributer(Map<String, String> reqMap) throws Exception;

	List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap);
	
	List<Map<String,Object>> findLiveRoomByLectureId(String lecture_id);
	Map<String,Object> findDistributerInfo(Map<String,Object> paramters);
	
	List<Map<String,Object>> findRoomFanList(Map<String,Object> paramters);

	Map<String,Object> findLecturerDistributionByLectureId(String user_id);

	Map<String,Object> findUserInfoByUserId(String userId);

	List<String> findUserIdsFromStudentsByCourseId(String course_id);

	List<String> findFollowUserIdsByRoomId(String roomId);
	
	/**
	 * 根据userid 获取 openid
	 * @param map
	 * @return
	 */
	List<String> findLoginInfoByUserIds(Map<String, Object> map);
}
