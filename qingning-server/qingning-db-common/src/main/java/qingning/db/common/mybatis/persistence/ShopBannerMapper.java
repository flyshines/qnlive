package qingning.db.common.mybatis.persistence;


import qingning.db.common.mybatis.cache.annotion.RedisCache;
import qingning.db.common.mybatis.cache.annotion.RedisEvict;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.List;
import java.util.Map;

public interface ShopBannerMapper {
    @RedisEvict(type = ShopBannerMapper.class,filedId="shop_id")
    int insert(Map<String,Object> record);
    @RedisEvict(type = ShopBannerMapper.class,filedId="shop_id")
    int deleteBanner(Map<String,Object> record);
    Map<String,Object> selectByPrimaryKey(String bannerId);
    @RedisEvict(type = ShopBannerMapper.class,filedId="shop_id")
    int updateByPrimaryKey(Map<String,Object> record);

    /**获取轮播图列表
     * @param param
     * @param page
     * @return
     */
    @RedisCache(type = ShopBannerMapper.class,pageList = true,hashKey ="shop_id")
    PageList<Map<String,Object>> selectListByUserId(Map<String, Object> param, PageBounds page);

    /**
     * 根据map中的条件查询轮播列表
     * @param paramMap
     * @return
     */
    @RedisCache(type = ShopBannerMapper.class,hashKey = "shop_id")
    List<Map<String, Object>> selectBannerListByMap(Map<String, Object> paramMap);



    /**获取已上架轮播图数量
     * @param shop_id
     * @return
     */
    @RedisCache(type = ShopBannerMapper.class,indexId = 0)
    int selectUpCount(String shop_id);


}