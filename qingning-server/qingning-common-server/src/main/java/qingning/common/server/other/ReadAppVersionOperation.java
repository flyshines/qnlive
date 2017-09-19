package qingning.common.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadAppVersionOperation implements CommonReadOperation {
    private ICommonModuleServer iCommonModuleServer;

    public ReadAppVersionOperation(ICommonModuleServer iCommonModuleServer) {
        this.iCommonModuleServer = iCommonModuleServer;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        return iCommonModuleServer.findVersionInfoByOS(reqMap);
    }
}
