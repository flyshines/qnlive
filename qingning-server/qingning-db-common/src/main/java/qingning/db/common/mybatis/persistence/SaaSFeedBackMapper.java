package qingning.db.common.mybatis.persistence;


import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.Map;

public interface SaaSFeedBackMapper {
    int deleteByPrimaryKey(String backId);

    int insert(Map<String,Object> record);

    Map<String,Object> selectByPrimaryKey(String backId);

    int updateByPrimaryKey(Map<String,Object> record);

    /**根据店铺获取店铺反馈
     * @param param
     * @param page
     * @return
     */
    PageList<Map<String,Object>> selectFeedBackByShop(Map<String, Object> param, PageBounds page);
}