package qingning.common.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

public class ReadUserOperation implements CommonReadOperation {
    private ICommonModuleServer commonModuleServer;

    public ReadUserOperation(ICommonModuleServer commonModuleServer) {
        this.commonModuleServer = commonModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        return commonModuleServer.findUserInfoByUserId(reqMap.get("user_id").toString());
    }
}
