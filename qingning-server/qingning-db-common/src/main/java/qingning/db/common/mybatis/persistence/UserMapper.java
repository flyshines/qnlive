package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

public interface UserMapper {
	int insertUser(Map<String,Object> record);
	int updateUser(Map<String,Object> record);
	int updateLiveRoomNumForUser(Map<String,Object> record);
    Map<String,Object> findByUserId(String user_id);
	Map<String,Object> findByPhone(Map<String,Object> record);

	List<Map<String,String>> findRobotUsers(String user_role);
	
	/**
	 * 获取没有t_user_gains记录的user_id
	 * @return
	 */
	List<Map<String,Object>> selectNotGainsUserId(@Param("page_num") int page_num,@Param("page_count") int page_count);

	/**查找数据库是否有该手机号码的用户
	 * @param userMap
	 * @return
	 */
	int existByPhone(Map<String, Object> userMap);

	int deleteUserByUserId(String user_id);

}