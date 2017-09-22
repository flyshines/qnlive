package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IShopModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadLecturerOperation implements CommonReadOperation {
    private IShopModuleServer lectureModuleServer;

    public ReadLecturerOperation(IShopModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        if(LECTURER_ROOM_LOAD.equals(functionName)){
        	return lectureModuleServer.findLiveRoomByLectureId((String)reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD));
        } else {
        	return lectureModuleServer.findLectureByLectureId((String)reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD));
        }
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
