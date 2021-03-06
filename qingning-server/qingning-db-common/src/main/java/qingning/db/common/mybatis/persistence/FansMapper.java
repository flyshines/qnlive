package qingning.db.common.mybatis.persistence;


import java.util.List;
import java.util.Map;

public interface FansMapper {
	List<Map<String,Object>> findShopFanList(Map<String,Object> record);
	Map<String,Object> findFansByUserIdAndShopId(Map<String,Object> record);
	List<Map<String,Object>> findFanInfoByUserId(Map<String,Object> record);
	int deleteFans(Map<String,Object> record);
	int insertFans(Map<String,Object> record);
	List<String> findFollowUserIdsByShopId(String shop_id);
	List<Map<String,Object>> findRoomFanListWithLoginInfo(Map<String,Object> record);

	List<Map<String,Object>> findShopIdByFans(Map<String, Object> reqMap);
}