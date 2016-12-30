package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ILectureModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadLecturerOperation implements CommonReadOperation {
    private ILectureModuleServer lectureModuleServer;

    public ReadLecturerOperation(ILectureModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        if(Constants.LECTURER_ROOM_LOAD.equals(requestEntity.getFunctionName())){
        	return lectureModuleServer.findLiveRoomByLectureId((String)reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD));
        } else {
        	return lectureModuleServer.findLectureByLectureId((String)reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD));
        }
    }
}
