package qingning.server.rpc.initcache;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.ShopModuleServer;

import java.util.Map;

/**
 * 包名: qingning.shop.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadShopOperation implements CommonReadOperation {
    private ShopModuleServer shopModuleServer;

    public ReadShopOperation(ShopModuleServer shopModuleServer) {
        this.shopModuleServer = shopModuleServer;
    }


	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        return null;
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        if(LECTURER_ROOM_LOAD.equals(functionName)){
            return shopModuleServer.findLiveRoomByLectureId((String)reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD));
        } else if(CACHE_READ_SHOP.equals(functionName)){
            return shopModuleServer.getShopInfo(reqMap.get("shop_id").toString(),null);
        } else if(CACHE_READ_SHOP_BY_USER.equals(functionName)){
            return shopModuleServer.getShopInfo(null,reqMap.get("user_id").toString());
        }else {
            return shopModuleServer.findLectureByLectureId((String)reqMap.get(Constants.CACHED_KEY_LECTURER_FIELD));
        }
    }
}
