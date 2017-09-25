package qingning.db.common.mybatis.persistence;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Created by DavidGHS on 2017/3/13.
 * 查询系统配置文件
 */
public interface SystemConfigMapper {
    /**
     * 获取客服信息
     */
    Map<String,Object> findCustomerServiceBySystemConfig(Map<String,Object> map);

    List<Map<String,Object>> findSystemConfig();

    /**
     * 根据config_key IN 查询系统配置
     * @param selectSysConfigMap
     * @return
     */
	List<Map<String, Object>> selectSysConfigByInKey(Map<String, Object> selectSysConfigMap);
}
