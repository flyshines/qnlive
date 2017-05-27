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
	
}
