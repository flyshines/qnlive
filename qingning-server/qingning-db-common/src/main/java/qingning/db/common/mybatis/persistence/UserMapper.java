package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

public interface UserMapper {
	int insertUser(Map<String,Object> record);
	int updateUser(Map<String,Object> record);
	int updateLiveRoomNumForUser(Map<String,Object> record);
    Map<String,Object> findByUserId(String user_id);
	Map<String,Object> findByPhone(Map<String,String> record);

	List<Map<String,String>> findRobotUsers(String user_role);
	
	/**
	 * 获取没有t_user_gains记录的user_id
	 * @param limit 查询的条数
	 * @return
	 */
	List<String> selectNotGainsUserId(@Param("limit") int limit);
}