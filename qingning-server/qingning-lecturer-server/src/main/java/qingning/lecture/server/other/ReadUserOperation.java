package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ILectureModuleServer;

import java.util.Map;

public class ReadUserOperation implements CommonReadOperation {
    private ILectureModuleServer lectureModuleServer;

    public ReadUserOperation(ILectureModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        return lectureModuleServer.findUserInfoByUserId(reqMap.get("user_id").toString());
    }
}
