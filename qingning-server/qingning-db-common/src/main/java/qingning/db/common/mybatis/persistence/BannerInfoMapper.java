package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface BannerInfoMapper {
    List<Map<String, Object>> findBannerInfoAll();
    List<Map<String, Object>> findBannerInfoAllByAppName(String appName);
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
}