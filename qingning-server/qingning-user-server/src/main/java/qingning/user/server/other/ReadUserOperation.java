package qingning.user.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IUserModuleServer;

import java.util.HashMap;
import java.util.Map;

public class ReadUserOperation implements CommonReadOperation {
	private IUserModuleServer userModuleServer;

    public ReadUserOperation(IUserModuleServer userModuleServer) {
        this.userModuleServer = userModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {    	
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        Object  result = null;
        String userId =(String)reqMap.get("user_id");
        if(Constants.SYS_READ_USER_COURSE_LIST.equals(functionName)){
        	result = userModuleServer.findCourseIdByStudent(reqMap);
        } else if(Constants.SYS_READ_USER_ROOM_LIST.equals(functionName)){
            result = userModuleServer.findRoomIdByFans(reqMap);
        } else if (Constants.SYS_READ_USER_SERIES_LIST.equals(functionName)){
            result = userModuleServer.findSeriesIdByStudent(reqMap);
        } else if ("getCourseStudentListByMap".equals(functionName)) {	//根据条件获取课程学员列表
        	result = userModuleServer.findCourseStudentByMap(reqMap);
        } else{
        	result = userModuleServer.findUserInfoByUserId(userId);
        }        
        return result;    
    }
}
