package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface RoomDistributerDetailsMapper {
	int insertRoomDistributerDetails(Map<String,Object> record);
	int updateRoomDistributerDetails(Map<String,Object> record);
}