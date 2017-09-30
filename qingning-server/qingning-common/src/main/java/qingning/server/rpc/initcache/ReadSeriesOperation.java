package qingning.server.rpc.initcache;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.SeriesModuleServer;
import qingning.server.rpc.manager.IShopModuleServer;

import java.util.Map;

/**
 * 包名: qingning.shop.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadSeriesOperation implements CommonReadOperation {
    private SeriesModuleServer seriesModuleServer;

    public ReadSeriesOperation(SeriesModuleServer seriesModuleServer) {
        this.seriesModuleServer = seriesModuleServer;
    }


    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        if(Constants.SYS_SHOP_SERIES_ALL.equals(functionName)){
            return seriesModuleServer.findSeriesByLecturer(reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD).toString());
        }

        if(CACHE_SERIES_STUDENTLIST.equals(functionName)){
            return seriesModuleServer.findSeriesStudentListBySeriesId(reqMap.get("series_id").toString());
        }else{
            return seriesModuleServer.findSeriesBySeriesId(reqMap.get("series_id").toString());
        }
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        if(CACHE_SERIES_STUDENTLIST.equals(functionName)){
            return seriesModuleServer.findSeriesStudentListBySeriesId(reqMap.get("series_id").toString());
        }else{
            return seriesModuleServer.findSeriesBySeriesId(reqMap.get("series_id").toString());
        }
    }
}
