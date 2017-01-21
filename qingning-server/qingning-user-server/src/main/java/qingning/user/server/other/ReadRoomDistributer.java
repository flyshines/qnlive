package qingning.user.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.MiscUtils;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IUserModuleServer;

import java.util.Map;

public class ReadRoomDistributer implements CommonReadOperation {
    private IUserModuleServer userModuleServer;

    public ReadRoomDistributer(IUserModuleServer userModuleServer) {
        this.userModuleServer = userModuleServer;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        reqMap.put("current_date", MiscUtils.getEndTimeOfToday());
        return userModuleServer.findAvailableRoomDistributer(reqMap);
    }
}
