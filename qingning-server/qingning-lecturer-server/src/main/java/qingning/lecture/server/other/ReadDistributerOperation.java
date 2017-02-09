package qingning.lecture.server.other;

import java.util.Map;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ILectureModuleServer;

public class ReadDistributerOperation implements CommonReadOperation {
    private ILectureModuleServer lectureModuleServer;

    public ReadDistributerOperation(ILectureModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        return lectureModuleServer.findByDistributerId((String)reqMap.get("distributer_id"));
    }
}
