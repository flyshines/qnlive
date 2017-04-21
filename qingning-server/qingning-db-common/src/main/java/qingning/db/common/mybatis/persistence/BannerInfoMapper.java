package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface BannerInfoMapper {
    List<Map<String, Object>> findBannerInfoAll();
}