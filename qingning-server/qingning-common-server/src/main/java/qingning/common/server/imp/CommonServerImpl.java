package qingning.common.server.imp;

import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.CodeVerifyficationUtil;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.ICommonModuleServer;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

public class CommonServerImpl  extends AbstractQNLiveServer {
    private static Logger logger = LoggerFactory.getLogger(CommonServerImpl.class);
    private ICommonModuleServer commonModuleServer;

    @Override
    public void initRpcServer() {
        if (commonModuleServer == null) {
            commonModuleServer = this.getRpcService("commonModuleServer");
            readShopOperation = new ReadShopOperation(commonModuleServer);
            readUserOperation = new ReadUserOperation(commonModuleServer);
            readCourseOperation = new ReadCourseOperation(commonModuleServer);
            readAPPVersionOperation = new ReadVersionOperation(commonModuleServer);
            readForceVersionOperation = new ReadVersionForceOperation(commonModuleServer);
            readLecturerOperation = new ReadLecturerOperation(commonModuleServer);
            readSeriesOperation = new ReadSeriesOperation(commonModuleServer);
            readConfigOperation = new ReadConfigOperation(commonModuleServer);
        }
    }

    private static Auth auth;

    static {
        auth = Auth.create(MiscUtils.getConfigByKey("qiniu_AK"), MiscUtils.getConfigByKey("qiniu_SK"));
    }

    /**
     * 绑定手机号码（校验手机号码）
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("bindPhone")
    public Map<String, Object> bindPhone(RequestEntity reqEntity) throws Exception{
        Map<String,String> reqMap = (Map<String, String>) reqEntity.getParam();
        String verification_code = reqMap.get("code");
        String phone = reqMap.get("phone");
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象

        if(CodeVerifyficationUtil.verifyVerificationCode(userId, verification_code, jedis)){
            //if("0000".equals(verification_code)){
            logger.info("绑定手机号码>>验证码验证通过");
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("user_id", userId);
            userMap.put("phone_number", phone);
            if(commonModuleServer.updateUserById(userMap)>0){
                throw new QNLiveException("130008");
            }
            //清空用户缓存
            Map<String,Object> userQuery = new HashMap<>();
            userQuery.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String userkey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, userQuery);
            jedis.hset(userkey,"phone_number",phone);
        }else{
            throw new QNLiveException("130002");
        }
        return null;
    }



}
