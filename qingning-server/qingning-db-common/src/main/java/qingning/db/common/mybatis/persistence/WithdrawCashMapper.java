package qingning.db.common.mybatis.persistence;

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

	/**分页查询-提现记录
	 * @param param
	 * @return
	 */
	List<Map<String,Object>> selectWithdrawList(Map<String, Object> param);

	/**根据ID查询条数
	 * @param withdraw_cash_id
	 * @return
	 */
	Map<String,Object> selectWithdrawSizeById(String withdraw_cash_id);


}
