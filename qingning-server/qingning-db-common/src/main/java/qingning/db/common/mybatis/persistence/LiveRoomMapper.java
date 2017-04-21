package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface LiveRoomMapper {
	Map<String,Object> findLiveRoomByRoomId(String room_id);
	int insertLiveRoom(Map<String,Object> record);
	int updateLiveRoom(Map<String,Object> record);
	List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id);
	List<Map<String, Object>> findLiveRoomBySearch(Map<String,Object> record);
}