package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IShopModuleServer;
import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadLiveRoomOperation implements CommonReadOperation {
    private IShopModuleServer lectureModuleServer;

    public ReadLiveRoomOperation(IShopModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

        return lectureModuleServer.findLiveRoomByRoomId(reqMap.get("room_id").toString());
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap,String functionName) throws Exception {
        return null;
    }
}
