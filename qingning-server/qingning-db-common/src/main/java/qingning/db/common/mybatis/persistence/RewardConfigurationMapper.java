package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface RewardConfigurationMapper {
    List<Map<String,Object>> findRewardConfigurationList();
    Map<String,Object> findRewardInfoByRewardId(long reward_id);
}