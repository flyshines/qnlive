package qingning.server;


import qingning.server.rpc.initcache.ReadConfigOperation;

import java.util.Map;

/**
 * Created by Rouse on 2017/9/22.
 */
public class JedisServer {
    /**
     * 系统配置key-Map
     */
    protected Map<String,Object> systemConfigMap;
    /**
     * 系统配置key-value
     */
    protected Map<String,String> systemConfigStringMap;
    protected ReadConfigOperation readConfigOperation;

}
