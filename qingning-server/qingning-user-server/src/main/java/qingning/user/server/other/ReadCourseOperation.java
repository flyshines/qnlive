package qingning.user.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IUserUserModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadCourseOperation implements CommonReadOperation {
    private IUserUserModuleServer userModuleServer;

    public ReadCourseOperation(IUserUserModuleServer userModuleServer) {
        this.userModuleServer = userModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {

        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        if (SYS_READ_SAAS_COURSE.equals(functionName)) {    //根据课程id数据库查询saas课程
            return userModuleServer.findSaasCourseByCourseId(reqMap.get("course_id").toString());
        } else {
            return userModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
        }



    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
