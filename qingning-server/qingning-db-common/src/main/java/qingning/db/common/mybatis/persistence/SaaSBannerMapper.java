package qingning.db.common.mybatis.persistence;


import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.Map;

public interface SaaSBannerMapper {
    int deleteByPrimaryKey(String bannerId);

    int insert(Map<String,Object> record);

    Map<String,Object> selectByPrimaryKey(String bannerId);

    int updateByPrimaryKey(Map<String,Object> record);

    /**获取轮播图列表
     * @param param
     * @param page
     * @return
     */
    PageList<Map<String,Object>> selectListByUserId(Map<String, Object> param, PageBounds page);
}