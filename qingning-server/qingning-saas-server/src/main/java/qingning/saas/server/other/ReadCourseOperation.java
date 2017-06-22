package qingning.saas.server.other;

import java.util.Map;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ISaaSModuleServer;

public class ReadCourseOperation implements CommonReadOperation {
    private ISaaSModuleServer saaSModuleServer;

    public ReadCourseOperation(ISaaSModuleServer saaSModuleServer) {
        this.saaSModuleServer = saaSModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        if("findSaasCourseByCourseId".equals(functionName)){	//根据课程id数据库查询saas课程
        	return saaSModuleServer.findSaasCourseByCourseId(reqMap.get("course_id").toString());
        }else if("findCourseByCourseId".equals(functionName)){
        	return saaSModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
        }
        
        return null;
    }
}