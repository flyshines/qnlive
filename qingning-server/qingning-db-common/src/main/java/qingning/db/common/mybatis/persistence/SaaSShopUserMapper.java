package qingning.db.common.mybatis.persistence;


import org.apache.ibatis.annotations.Param;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.Map;

public interface SaaSShopUserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Map<String,Object> record);

    int updateByPrimaryKey(Map<String,Object> record);

    /**根据用户ID获取该用户店铺的所有用户
     * @param param
     * @param page
     * @return
     */
    PageList<Map<String,Object>> selectUsersByShop(Map<String, Object> param, PageBounds page);

    int updateTypeById(@Param(value = "shopId") String shopId, @Param(value = "userId")String userId);
}