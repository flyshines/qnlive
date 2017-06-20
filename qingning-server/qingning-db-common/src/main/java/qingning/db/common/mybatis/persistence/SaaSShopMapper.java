package qingning.db.common.mybatis.persistence;

import java.util.Map;

/**
 * Created by Rouse on 2017/6/16.
 */
public interface SaaSShopMapper {
    int deleteByPrimaryKey(String shopId);

    int insert(Map<String,Object> record);

    Map<String,Object> selectByPrimaryKey(String shopId);

    int updateByPrimaryKey(Map<String,Object> record);
}
