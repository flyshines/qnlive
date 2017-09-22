package qingning.common.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

public class ReadRoomDistributerOperation implements CommonReadOperation {
    private ICommonModuleServer commonModuleServer;

    public ReadRoomDistributerOperation(ICommonModuleServer commonModuleServer) {
        this.commonModuleServer = commonModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        return commonModuleServer.findAvailableRoomDistributer(reqMap);
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
