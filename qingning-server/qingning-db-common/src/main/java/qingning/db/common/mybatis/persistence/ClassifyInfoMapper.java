package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface ClassifyInfoMapper {
    List<Map<String, Object>> findClassifyInfo();
}