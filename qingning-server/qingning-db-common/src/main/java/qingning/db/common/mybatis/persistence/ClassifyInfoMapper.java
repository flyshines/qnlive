package qingning.db.common.mybatis.persistence;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ClassifyInfoMapper {
    List<Map<String, Object>> findClassifyInfo();

    List<Map<String, Object>> findClassifyInfoByAppName(@Param("appName")String appName);

    /**
     * 新增分类
     * @param record
     * @return
     */
    int insertClassifyInfo(Map<String, Object> record);
    /**
     * 更新分类
     * @param record 更新非空字段
     * @return
     */
    int updateClassifyInfo(Map<String, Object> record);
    /**
     * 根据条件获取分类列表
     * @param reqMap
     * @return
     */
	List<Map<String, Object>> findClassifyListByMap(Map<String, Object> reqMap);
}