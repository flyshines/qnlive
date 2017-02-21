package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface DistributerMapper {
	Map<String,Object> findByDistributerId(String distributer_id);
	List<Map<String,Object>> findDistributionInfoByDistributerId(Map<String,Object> parameters);	
	List<Map<String,Object>> findRoomDistributerRecommendInfo(Map<String,Object> parameters);
	List<Map<String,Object>> findRoomDistributerCourseInfo(Map<String,Object> parameters);	
	List<Map<String,Object>> findRoomDistributerCourseDetailsInfo(Map<String,Object> parameters);
	List<Map<String,Object>> findCourseWithRoomDistributerCourseInfo(Map<String,Object> parameters);
	int insertDistributer(Map<String,Object> record);
	int updateDistributer(Map<String,Object> record);
}
