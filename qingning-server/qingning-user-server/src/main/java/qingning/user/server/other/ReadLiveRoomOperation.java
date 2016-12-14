package qingning.user.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IUserModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadLiveRoomOperation implements CommonReadOperation {
    private IUserModuleServer userModuleServer;

    public ReadLiveRoomOperation(IUserModuleServer userModuleServer) {
        this.userModuleServer = userModuleServer;
    }


    @Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

        return userModuleServer.findLiveRoomByRoomId(reqMap.get("room_id").toString());
    }
}
