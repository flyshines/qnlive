package qingning.db.common.mybatis.persistence;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ClassifyInfoMapper {
    List<Map<String, Object>> findClassifyInfo();

    List<Map<String, Object>> findClassifyInfoByAppName(@Param("appName")String appName);

    int insertSelective(Map<String, Object> record);

    int updateByPrimaryKeySelective(Map<String, Object> record);
}