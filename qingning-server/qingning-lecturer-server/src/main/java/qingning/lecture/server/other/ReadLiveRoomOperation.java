package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ILectureModuleServer;
import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadLiveRoomOperation implements CommonReadOperation {
    private ILectureModuleServer lectureModuleServer;

    public ReadLiveRoomOperation(ILectureModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

        return lectureModuleServer.findLiveRoomByRoomId(reqMap.get("room_id").toString());
    }
}
