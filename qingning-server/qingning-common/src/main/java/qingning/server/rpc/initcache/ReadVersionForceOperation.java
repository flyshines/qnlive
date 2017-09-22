package qingning.server.rpc.initcache;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.VersionForceModuleServer;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadVersionForceOperation implements CommonReadOperation {
    private VersionForceModuleServer versionForceModuleServer;

    public ReadVersionForceOperation(VersionForceModuleServer versionForceModuleServer) {
        this.versionForceModuleServer = versionForceModuleServer;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

        return versionForceModuleServer.findForceVersionInfoByOS(reqMap.get("force_version_key").toString());
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
