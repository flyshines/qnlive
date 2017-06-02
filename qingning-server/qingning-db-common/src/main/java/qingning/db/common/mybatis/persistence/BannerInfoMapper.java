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
}