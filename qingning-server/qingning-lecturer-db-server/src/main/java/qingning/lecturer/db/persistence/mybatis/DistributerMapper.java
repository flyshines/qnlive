package qingning.lecturer.db.persistence.mybatis;

import java.util.Map;

public interface DistributerMapper {
	int insert(Map<String,Object> record);
	int insertRoomDistributer(Map<String,Object> record);
}
