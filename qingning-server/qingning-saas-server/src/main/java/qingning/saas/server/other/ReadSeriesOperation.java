package qingning.saas.server.other;

import java.util.Map;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ISaaSModuleServer;

/**
 * 读取系列课程
 * @author Administrator
 *
 */
public class ReadSeriesOperation implements CommonReadOperation {
    private ISaaSModuleServer saaSModuleServer;

    public ReadSeriesOperation(ISaaSModuleServer saaSModuleServer) {
        this.saaSModuleServer = saaSModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
	public Object invokeProcess(RequestEntity requestEntity) throws Exception {
		Map<String, Object> param = (Map<String, Object>) requestEntity.getParam();
		String functionName = requestEntity.getFunctionName();
		if("findSeriesBySeriesId".equals(functionName)){
			return saaSModuleServer.findSeriesBySeriesId(param.get("series_id").toString());
		}
		return null;
	}
}
