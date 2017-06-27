package qingning.db.common.mybatis.persistence;


import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.List;
import java.util.Map;

public interface SaaSCourseMapper {
    int deleteByPrimaryKey(String courseId);

    int insert(Map<String,Object> record);

    Map<String,Object> selectByPrimaryKey(String courseId);

    int updateByPrimaryKey(Map<String,Object> record);
    //查询单品列表
    PageList<Map<String,Object>> selectByShop(Map<String, Object> param, PageBounds page);

    /**查询店主已上架的课程ID
     * @param shopId
     * @return
     */
    List<String> selectUpListByShopId(String shopId);

    /**根据店铺获取单品列表
     * @param param
     * @param page
     * @return
     */
    PageList<Map<String,Object>> selectUpCourseListByShop(Map<String, Object> param, PageBounds page);

    /**查询系列课下的课程列表
     * @param param
     * @param page
     * @return
     */
    PageList<Map<String,Object>> findCourseBySeriesId(Map<String, Object> param, PageBounds page);
}