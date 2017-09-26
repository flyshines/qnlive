package qingning.db.common.mybatis.persistence;


import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.List;
import java.util.Map;

public interface ShopBannerMapper {
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

    /**
     * 根据map中的条件查询轮播列表
     * @param paramMap
     * @return
     */
	List<Map<String, Object>> selectBannerListByMap(Map<String, Object> paramMap);

    /**获取已上架轮播图数量
     * @param user_id
     * @return
     */
    int selectUpCount(String user_id);

    int deleteBanner(Map<String,Object> record);


}