package qingning.saas.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ISaaSModuleServer;

import java.util.Map;

/**
 * 用户已上架的单品列表缓存
 */
public class ReadSingleListOperation implements CommonReadOperation {
    private ISaaSModuleServer saaSModuleServer;

    public ReadSingleListOperation(ISaaSModuleServer saaSModuleServer) {
        this.saaSModuleServer = saaSModuleServer;
    }
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        return saaSModuleServer.findShopUpList(reqMap.get("shop_id").toString());
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
