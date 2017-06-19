package qingning.saas.server.imp;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.common.util.WeiXinUtil;
import qingning.saas.server.other.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ISaaSModuleServer;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

public class SaaSServerImpl extends AbstractQNLiveServer {

    private ISaaSModuleServer saaSModuleServer;

    private ReadUserOperation readUserOperation;

    //private ReadLiveRoomOperation readLiveRoomOperation;
    //private ReadCourseOperation readCourseOperation;
    //private ReadRoomDistributer readRoomDistributer;
    //private ReadLecturerOperation readLecturerOperation;
    //private ReadRoomDistributerOperation readRoomDistributerOperation;
    private static Logger logger = LoggerFactory.getLogger(SaaSServerImpl.class);

    @Override
    public void initRpcServer() {
        if (saaSModuleServer == null) {
            saaSModuleServer = this.getRpcService("saaSModuleServer");
            readUserOperation = new ReadUserOperation(saaSModuleServer);

            //readLiveRoomOperation = new ReadLiveRoomOperation(saaSModuleServer);
            //readCourseOperation = new ReadCourseOperation(saaSModuleServer);
            //readRoomDistributer = new ReadRoomDistributer(saaSModuleServer);
            //readLecturerOperation = new ReadLecturerOperation(saaSModuleServer);
            //readRoomDistributerOperation = new ReadRoomDistributerOperation(saaSModuleServer);
        }
    }


	/**
	 * 店铺设置
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("shopEdit")
    public void shopEdit(RequestEntity reqEntity) throws Exception{
        Map<String,Object> param = (Map<String, Object>) reqEntity.getParam();
        saaSModuleServer.updateShop(param);
    }
	/**
	 * 扫码登录 1 获取二维码接口
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("wechatLogin")
    public Map<String, Object>  wechatLogin(RequestEntity reqEntity) throws Exception{
        //Jedis jedis = jedisUtils.getJedis(Constants.HEADER_APP_NAME);//获取jedis对象

        //获取预授权码pre_auth_code 进入微信平台
        //String access_token = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "component_access_token");
        //JSONObject jsonObj = WeiXinUtil.getPreAuthCode(access_token,Constants.HEADER_APP_NAME);

        //重定向微信URL
        String requestUrl = WeiXinUtil.getWechatRqcodeLoginUrl();

        Map<String, Object> result = new HashMap<>();
        result.put("redirectUrl", requestUrl);
        return result;

    }

}