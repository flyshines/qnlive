package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface BannerInfoMapper {
    List<Map<String, Object>> findBannerInfoAll();
    List<Map<String, Object>> findBannerInfoAllBy( );
    /**
     * 新增轮播
     * @param insertMap
     * @return
     */
	int insertBanner(Map<String, Object> insertMap);
	/**
	 * 根据map中的参数查询banner
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> selectBannerInfoByMap(Map<String, Object> reqMap);
	/**
	 * 根据map中的参数查询banner总数量
	 * @param reqMap
	 * @return
	 */
	int selectBannerCountByMap(Map<String, Object> reqMap);
	/**
	 * 更新banner所有字段
	 * @param reqMap 
	 * @return
	 */
	int updateBannerInfoByMap(Map<String, Object> reqMap);
	/**
	 * 移除banner
	 * @param reqMap
	 * @return
	 */
	int delectBannerInfoByMap(Map<String, Object> reqMap);
	/**
	 * 根据map中非空字段更新banner
	 * @param reqMap
	 * @return
	 */
	int updateBannerInfoByMapNotNull(Map<String, Object> reqMap);
}