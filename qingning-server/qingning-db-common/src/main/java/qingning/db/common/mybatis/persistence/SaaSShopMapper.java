package qingning.db.common.mybatis.persistence;

import java.util.Map;

/**
 * Created by Rouse on 2017/6/16.
 */
public interface SaaSShopMapper {
    int deleteByPrimaryKey(String shopId);

    int insert(Map<String,Object> record);

    Map<String,Object> selectByPrimaryKey(Map<String,Object> record);

    int updateByPrimaryKey(Map<String,Object> record);

    String selectUserIdByShopId(String shopId);

    String selectShopIdByUserId(String userId);
}
