package qingning.server.rpc.initcache;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.LecturerModuleServer;
import qingning.server.rpc.ShopModuleServer;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

/**
 * 包名: qingning.shop.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadLecturerOperation implements CommonReadOperation {
	private ShopModuleServer shopModuleServer;

    public ReadLecturerOperation(ShopModuleServer shopModuleServer) {
        this.shopModuleServer = shopModuleServer;
    }

	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
       	return shopModuleServer.findLectureByLectureId((String)reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD));
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
