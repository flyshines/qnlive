package qingning.db.common.mybatis.persistence;


import java.util.List;
import java.util.Map;

public interface FansMapper {
	List<Map<String,Object>> findRoomFanList(Map<String,Object> record);
	Map<String,Object> findFansByUserIdAndRoomId(Map<String,Object> record);
	List<Map<String,Object>> findFanInfoByUserId(Map<String,Object> record);
	int deleteFans(Map<String,Object> record);
	int insertFans(Map<String,Object> record);
	List<String> findFollowUserIdsByRoomId(String room_id);
}