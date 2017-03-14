package qingning.db.common.mybatis.persistence;

import java.util.Map;

/**
 * Created by DavidGHS on 2017/3/13.
 * 查询系统配置文件
 */
public interface SystemConfigMapper {
    /**
     * 获取客服信息
     */
    Map<String,Object> findCustomerServiceBySystemConfig(String config_key);
}
