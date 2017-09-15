package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface AdminUserMapper {
	/**
	 * 根据手机号码查询后台登录帐号
	 * @param reqMap
	 * @return
	 */
	Map<String, Object> selectAdminUserByMobile(Map<String, Object> reqMap);
	/**
	 * 更新后台账户所有字段
	 * @param adminUserMap
	 * @return
	 */
	int updateAdminUserByAllMap(Map<String, Object> adminUserMap);

	/**查找管理员
	 * @return
	 */
	Map<String,Object> selectAdminUserById(String userId);
}
