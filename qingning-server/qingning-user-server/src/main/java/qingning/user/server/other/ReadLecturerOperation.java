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
public class ReadLecturerOperation implements CommonReadOperation {
	private IUserUserModuleServer userModuleServer;

    public ReadLecturerOperation(IUserUserModuleServer userModuleServer) {
        this.userModuleServer = userModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
    	Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
    	return userModuleServer.findLectureByLectureId((String)reqMap.get("lecture_id"));
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
