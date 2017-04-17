package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
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
        String userId =(String)reqMap.get("user_id");
        Object  result = null;
        if(Constants.SYS_READ_USER_COURSE_LIST.equals(requestEntity.getFunctionName())){
        	result = lectureModuleServer.findCourseIdByStudent(reqMap);
        } else  if(Constants.SYS_READ_USER_ROOM_LIST.equals(requestEntity.getFunctionName())){
            result = lectureModuleServer.findRoomIdByFans(reqMap);
        } else if(Constants.SYS_READ_USER_BY_UNIONID.equals(requestEntity.getFunctionName())) {
            result = lectureModuleServer.getLoginInfoByLoginId(reqMap.get("unionID").toString());
        } else {
        	result = lectureModuleServer.findUserInfoByUserId(userId);
        }
        return result;
    }
}
