package qingning.mq.persistence.mybatis;

import java.util.Map;

public interface DistributerMapper {
	int insertDistributer(Map<String,Object> record);
	int insertRoomDistributer(Map<String,Object> record);
	
	Map<String,Object> findDistributerInfo(Map<String,Object> paramters);
	int updateRoomDistributerbyPrimaryKey(Map<String,Object> paramters);
	int updateDistributerbyPrimaryKey(Map<String,Object> paramters);
}
