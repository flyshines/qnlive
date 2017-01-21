package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface RoomDistributerMapper {
	Map<String,Object> findAvailableRoomDistributer(Map<String,Object> record);
	Map<String,Object> findRoomDistributerInfoByRqCode(String rq_code);
	int insertRoomDistributer(Map<String,Object> record);
	int updateRoomDistributer(Map<String,Object> record);
	int increteRecommendNumForRoomDistributer(Map<String,Object> record);	
	List<Map<String, Object>> findRoomDistributerInfo(Map<String, Object> paramters);
}