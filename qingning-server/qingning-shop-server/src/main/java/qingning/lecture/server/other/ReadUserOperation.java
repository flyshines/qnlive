package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IShopModuleServer;

import java.util.Map;

public class ReadUserOperation implements CommonReadOperation {

    private IShopModuleServer lectureModuleServer;

    public ReadUserOperation(IShopModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
    	Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String userId =(String)reqMap.get("user_id");
        Object  result = null;
        if(SYS_READ_USER_COURSE_LIST.equals(requestEntity.getFunctionName())){
        	result = lectureModuleServer.findCourseIdByStudent(reqMap);
        } else  if(SYS_READ_USER_ROOM_LIST.equals(requestEntity.getFunctionName())){
            result = lectureModuleServer.findRoomIdByFans(reqMap);
        } else if (SYS_READ_USER_SERIES_LIST.equals(requestEntity.getFunctionName())){
            result = lectureModuleServer.findSeriesIdByStudent(reqMap);
        } else if(SYS_READ_USER_BY_UNIONID.equals(requestEntity.getFunctionName())) {
            result = lectureModuleServer.getLoginInfoByLoginId(reqMap.get("unionID").toString());
        } else {
        	result = lectureModuleServer.findUserInfoByUserId(userId);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        String userId =(String)reqMap.get("user_id");
        Object  result;
        if(SYS_READ_USER_COURSE_LIST.equals(functionName)){
        	result = lectureModuleServer.findCourseIdByStudent(reqMap);
        } else  if(SYS_READ_USER_ROOM_LIST.equals(functionName)){
            result = lectureModuleServer.findRoomIdByFans(reqMap);
        } else if (SYS_READ_USER_SERIES_LIST.equals(functionName)){
            result = lectureModuleServer.findSeriesIdByStudent(reqMap);
        } else if(SYS_READ_USER_BY_UNIONID.equals(functionName)) {
            result = lectureModuleServer.getLoginInfoByLoginId(reqMap.get("unionID").toString());
        } else {
        	result = lectureModuleServer.findUserInfoByUserId(userId);
        }
        return result;
    }
}
