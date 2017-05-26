package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface WithdrawCashMapper {
    void insertWithdrawCash(List<Map<String ,Object>> list);
    void insertWithdrawCashByNewUser(Map<String ,Object> reqMap);
    void updateWithdrawCash(Map<String ,Object> reqMap);
    List<Map<String ,Object>> findWithdrawCashByUser(Map<String ,Object> reqMap);
}