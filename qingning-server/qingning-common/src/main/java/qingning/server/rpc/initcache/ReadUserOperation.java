package qingning.server.rpc.initcache;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.UserModuleServer;

import java.util.Map;

public class ReadUserOperation implements CommonReadOperation {
	private UserModuleServer userModuleServer;

    public ReadUserOperation(UserModuleServer userModuleServer) {
        this.userModuleServer = userModuleServer;
    }


	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {    	
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        Object  result;
        String userId =(String)reqMap.get("user_id");
        if(SYS_READ_USER_COURSE_LIST.equals(functionName)){
        	result = userModuleServer.findCourseIdByStudent(reqMap);
        } else if(SYS_READ_USER_ROOM_LIST.equals(functionName)){
            result = userModuleServer.findShopIdByFans(reqMap);
        } else if (SYS_READ_USER_SERIES_LIST.equals(functionName)){
            result = userModuleServer.findSeriesIdByStudent(reqMap);
        } else if ("getCourseStudentListByMap".equals(functionName)) {	//根据条件获取课程学员列表
        	result = userModuleServer.findCourseStudentByMap(reqMap);
        }  else if(SYS_READ_USER_BY_UNIONID.equals(requestEntity.getFunctionName())) {
            result = userModuleServer.getLoginInfoByLoginId(reqMap.get("unionID").toString());
        }else if ("getSeriesStudentListByMap".equals(functionName)) {	//根据条件获取系列学员列表
        	result = userModuleServer.findSeriesStudentByMap(reqMap);
        } else{
        	result = userModuleServer.findUserInfoByUserId(userId);
        }
        return result;
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
