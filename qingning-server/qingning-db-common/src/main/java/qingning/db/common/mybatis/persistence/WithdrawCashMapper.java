package qingning.db.common.mybatis.persistence;

import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.List;
import java.util.Map;

public interface WithdrawCashMapper {
	/**
	 * 获取用户提现成功总金额
	 * @param userIdList
	 * @return
	 */
	List<Map<String, Object>> selectUserWithdrawSum(List<String> userIdList);
	
    void insertWithdrawCash(List<Map<String ,Object>> list);
    int insertWithdrawCashByNewUser(Map<String ,Object> reqMap);
	/**更新-提醒记录
	 * @param reqMap
	 * @return
	 */
    int updateWithdrawCash(Map<String ,Object> reqMap);
    List<Map<String ,Object>> findWithdrawCashByUser(Map<String ,Object> reqMap);

	/**分页查询-用户提现记录
	 * @param param
	 * @return
	 */
	List<Map<String,Object>> selectWithdrawList(Map<String, Object> param);

	/**
	 * 根据ID查询条数
	 * @param selectMap
	 * @return
	 */
	Map<String,Object> selectWithdrawSizeById(Map<String, Object> selectMap);

	/**分页查询-后台提现记录
	 * @param param
	 * @return
	 */
	PageList<Map<String,Object>> selectWithdrawListAll(Map<String, Object> param, PageBounds pageBounds);
}
