package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.IShopModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadSeriesOperation implements CommonReadOperation {
    private IShopModuleServer lectureModuleServer;

    public ReadSeriesOperation(IShopModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        if("findSeriesStudentListBySeriesId".equals(functionName)){
        	return lectureModuleServer.findSeriesStudentListBySeriesId(reqMap.get("series_id").toString());
        }else{
        	return lectureModuleServer.findSeriesBySeriesId(reqMap.get("series_id").toString());
        }
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
