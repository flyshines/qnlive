package qingning.common.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

public class ReadDistributerOperation implements CommonReadOperation {
    private ICommonModuleServer commonModuleServer;

    public ReadDistributerOperation(ICommonModuleServer commonModuleServer) {
        this.commonModuleServer = commonModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String function = requestEntity.getFunctionName();
        Object result = null;
        if(FUNCTION_DISTRIBUTERS_ROOM_RQ.equals(function)){
        	result = commonModuleServer.findDistributionRoomDetail(reqMap);
        } else {
        	result = commonModuleServer.findByDistributerId((String)reqMap.get("distributer_id"));
        }
        return result;
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap,String functionName) throws Exception {
        return null;
    }
}
