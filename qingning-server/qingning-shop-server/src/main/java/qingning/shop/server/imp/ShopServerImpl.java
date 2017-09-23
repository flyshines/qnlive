package qingning.shop.server.imp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.RequestEntity;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.Constants;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.IShopModuleServer;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;

public class ShopServerImpl extends AbstractQNLiveServer {
    private static Logger log = LoggerFactory.getLogger(ShopServerImpl.class);

    private IShopModuleServer shopModuleServer;

    @Override
    public void initRpcServer() {
        if (shopModuleServer == null) {
            shopModuleServer = this.getRpcService("shopModuleServer");
            readCourseOperation = new ReadCourseOperation(shopModuleServer);
            readShopOperation = new ReadShopOperation(shopModuleServer);
            readUserOperation = new ReadUserOperation(shopModuleServer);
            readSeriesOperation = new ReadSeriesOperation(shopModuleServer);
            readLecturerOperation =  new ReadLecturerOperation(shopModuleServer);
        }
    }



    @SuppressWarnings("unchecked")
    @FunctionName("createCourse")
    public Map<String, Object> createCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String lecturer_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//通过token 获取讲师id
        Jedis jedis = jedisUtils.getJedis();
        Map query = new HashMap();
        query.put(Constants.CACHED_KEY_LECTURER_FIELD,lecturer_id);
        readLecturer(lecturer_id,this.generateRequestEntity(null, null, null, query),readLecturerOperation,jedis);



        return null;
    }

}
