package qingning.db.common.mybatis.persistence;

import org.apache.ibatis.annotations.Param;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.List;
import java.util.Map;

/**
 * Created by Rouse on 2017/6/16.
 */
public interface ShopMapper {
    int deleteByPrimaryKey(String shopId);

    int insert(Map<String,Object> record);

    Map<String,Object> selectByPrimaryKey(@Param("shop_id") String shop_id);

    PageList<Map<String,Object>> getShopInfoList(Map<String, Object> param, PageBounds page);

    int updateByPrimaryKey(Map<String,Object> record);

    String selectUserIdByShopId(String shopId);

    String selectShopIdByUserId(String userId);
}
