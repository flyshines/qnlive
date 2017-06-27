package qingning.saas.server.other;

import java.util.Map;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ISaaSModuleServer;

public class ReadSaasCourseMessageOperation implements CommonReadOperation {
    private ISaaSModuleServer saaSModuleServer;

    public ReadSaasCourseMessageOperation(ISaaSModuleServer saaSModuleServer) {
        this.saaSModuleServer = saaSModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        if("findSaasCourseCommentByCommentId".equals(functionName)){	//根据留言id数据库查询saas课程留言详情
        	return saaSModuleServer.findSaasCourseCommentByCommentId(reqMap.get("message_id").toString());
        }
        
        return null;
    }
}
