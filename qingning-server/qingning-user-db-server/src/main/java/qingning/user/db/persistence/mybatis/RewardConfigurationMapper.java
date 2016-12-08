package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.RewardConfiguration;

import java.util.List;
import java.util.Map;

public interface RewardConfigurationMapper {
    int deleteByPrimaryKey(Long rewardId);

    int insert(RewardConfiguration record);

    int insertSelective(RewardConfiguration record);

    RewardConfiguration selectByPrimaryKey(Long rewardId);

    int updateByPrimaryKeySelective(RewardConfiguration record);

    int updateByPrimaryKey(RewardConfiguration record);

    List<Map<String,Object>> findRewardConfigurationList();

}