package qingning.server.rpc.initcache;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.VersionModuleServer;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadVersionOperation implements CommonReadOperation {
    private VersionModuleServer versionModuleServer;

    public ReadVersionOperation(VersionModuleServer versionModuleServer) {
        this.versionModuleServer = versionModuleServer;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        return versionModuleServer.findVersionInfoByOS(reqMap);
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap,String functionName) throws Exception {
        return null;
    }
}
