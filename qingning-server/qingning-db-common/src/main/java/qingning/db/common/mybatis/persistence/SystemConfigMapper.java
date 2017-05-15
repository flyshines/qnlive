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

    List<Map<String,Object>> findSystemConfigByAppName(@Param("appName")String appName);
}
