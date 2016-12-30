package qingning.common.db.persistence.mybatis;

import java.util.List;
import java.util.Map;

public interface DistributerMapper {
	Map<String,Object> findByDistributerId(String distributer_id);
	List<Map<String,Object>> findDistributionInfoByDistributerId(Map<String,Object> parameters);	
	List<Map<String,Object>> findRoomDistributerRecommendInfo(Map<String,Object> parameters);
	List<Map<String,Object>> findRoomDistributerCourseInfo(Map<String,Object> parameters);	
	List<Map<String,Object>> findRoomDistributerCourseDetailsInfo(Map<String,Object> parameters);
}
