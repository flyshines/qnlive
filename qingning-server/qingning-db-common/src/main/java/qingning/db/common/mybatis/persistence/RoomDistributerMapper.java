package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface RoomDistributerMapper {
	
	Map<String,Object> findRoomDistributerInfoByRqCode(String rq_code);
	int increteRecommendNumForRoomDistributer(Map<String,Object> record);	
	List<Map<String, Object>> findRoomDistributerInfo(Map<String, Object> paramters);
	
	
	Map<String,Object> findRoomDistributer(Map<String,Object> record);
	int insertRoomDistributer(Map<String,Object> record);
	int updateRoomDistributer(Map<String,Object> record);
}