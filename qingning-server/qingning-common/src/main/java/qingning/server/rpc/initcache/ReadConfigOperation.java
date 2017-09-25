package qingning.server.rpc.initcache;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.ConfigModuleServer;

import java.util.Map;

/**
 * Created by Rouse on 2017/9/25.
 */
public class ReadConfigOperation  implements CommonReadOperation {

    private ConfigModuleServer configModuleServer;

    public ReadConfigOperation(ConfigModuleServer configModuleServer) {
        this.configModuleServer = configModuleServer;
    }

    @Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        return configModuleServer.findSystemConfig();
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
