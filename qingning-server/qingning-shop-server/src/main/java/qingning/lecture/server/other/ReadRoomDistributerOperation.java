package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IShopModuleServer;

import java.util.Map;

public class ReadRoomDistributerOperation implements CommonReadOperation {
    private IShopModuleServer lectureModuleServer;

    public ReadRoomDistributerOperation(IShopModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        return lectureModuleServer.findAvailableRoomDistributer(reqMap);
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
