package qingning.common.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadLecturerOperation implements CommonReadOperation {
	private ICommonModuleServer iCommonModuleServer;

    public ReadLecturerOperation(ICommonModuleServer iCommonModuleServer) {
        this.iCommonModuleServer = iCommonModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
       	return iCommonModuleServer.findLectureByLectureId((String)reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD));
    }
}
