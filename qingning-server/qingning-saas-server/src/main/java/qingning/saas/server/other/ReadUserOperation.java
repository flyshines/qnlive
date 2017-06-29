package qingning.saas.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ISaaSModuleServer;

import java.util.Map;

public class ReadUserOperation implements CommonReadOperation {
	private ISaaSModuleServer saaSModuleServer;

    public ReadUserOperation(ISaaSModuleServer saaSModuleServer) {
        this.saaSModuleServer = saaSModuleServer;
    }




    @SuppressWarnings("unchecked")
    @Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        Object  result = null;
        String userId =(String)reqMap.get("user_id");
        if(Constants.SYS_READ_USER_COURSE_LIST.equals(requestEntity.getFunctionName())){
            result = saaSModuleServer.findCourseIdByStudent(reqMap);
        } else  if(Constants.SYS_READ_USER_ROOM_LIST.equals(requestEntity.getFunctionName())){
            result = saaSModuleServer.findRoomIdByFans(reqMap);
        }else if (Constants.SYS_READ_USER_SERIES_LIST.equals(requestEntity.getFunctionName())){
            result = saaSModuleServer.findSeriesIdByStudent(reqMap);
        } else{
            result = saaSModuleServer.findUserInfoByUserId(userId);
        }
        return result;
    }

}
