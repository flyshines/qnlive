package qingning.saas.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ISaaSModuleServer;

import java.util.Map;

/**
 * 包名: qingning.saas.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadLiveRoomOperation implements CommonReadOperation {
    private ISaaSModuleServer saaSModuleServer;

    public ReadLiveRoomOperation(ISaaSModuleServer saaSModuleServer) {
        this.saaSModuleServer = saaSModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

        return null;//userModuleServer.findLiveRoomByRoomId(reqMap.get("room_id").toString());
    }
}
