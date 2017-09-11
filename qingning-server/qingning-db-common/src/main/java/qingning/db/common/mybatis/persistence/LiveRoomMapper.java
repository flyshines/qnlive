package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface LiveRoomMapper {
	Map<String,Object> findLiveRoomByRoomId(String room_id);
	int insertLiveRoom(Map<String,Object> record);
	int updateLiveRoom(Map<String,Object> record);
	List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id);
	List<Map<String, Object>> findLiveRoomBySearch(Map<String,Object> record);
	
	/**
	 * 获得userId列表里用户的直播间收益
	 * @param userIdList
	 * @return
	 */
	List<Map<String, Object>> selectRoomAmount(List<String> userIdList);
	/**
	 * 后台_搜索课程列表(同时搜索直播间名、直播间id)
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> findLiveRoomListBySearch(Map<String, Object> reqMap);

	/**查找直播间ID
	 * @param lecturer_id
	 * @return
	 */
	String findLiveRoomIdByLectureId(String lecturer_id);
}