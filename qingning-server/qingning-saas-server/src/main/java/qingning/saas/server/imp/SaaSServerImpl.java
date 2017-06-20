package qingning.saas.server.imp;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.saas.server.other.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ISaaSModuleServer;
import redis.clients.jedis.Jedis;

import java.net.URLEncoder;
import java.util.*;

public class SaaSServerImpl extends AbstractQNLiveServer {
    private static Logger log = LoggerFactory.getLogger(SaaSServerImpl.class);

    private ISaaSModuleServer saaSModuleServer;

    private ReadUserOperation readUserOperation;

    private ReadShopOperation readShopOperation;
    //private ReadShopOperation readCourseOperation;
    //private ReadRoomDistributer readRoomDistributer;
    //private ReadLecturerOperation readLecturerOperation;
    //private ReadRoomDistributerOperation readRoomDistributerOperation;
    private static Logger logger = LoggerFactory.getLogger(SaaSServerImpl.class);

    @Override
    public void initRpcServer() {
        if (saaSModuleServer == null) {
            saaSModuleServer = this.getRpcService("saaSModuleServer");
            readUserOperation = new ReadUserOperation(saaSModuleServer);
            readShopOperation = new ReadShopOperation(saaSModuleServer);

            //readCourseOperation = new ReadShopOperation(saaSModuleServer);
            //readRoomDistributer = new ReadRoomDistributer(saaSModuleServer);
            //readLecturerOperation = new ReadLecturerOperation(saaSModuleServer);
            //readRoomDistributerOperation = new ReadRoomDistributerOperation(saaSModuleServer);
        }
    }

	/**
	 * 扫码登录 1 获取二维码接口
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("wechatLogin")
    public Map<String, Object>  wechatLogin(RequestEntity reqEntity) throws Exception{

        //重定向微信URL
        String requestUrl = WeiXinUtil.getWechatRqcodeLoginUrl();

        Map<String, Object> result = new HashMap<>();
        result.put("redirectUrl", requestUrl);
        return result;

    }

	/**
	 * 扫码登录 2 登录
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("wechatCheckLogin")
    public Map<String, Object>  wechatCheckLogin(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String code = (String) reqMap.get("code");

        log.debug("------微信SaaS登录授权回调------"+reqMap);

        //根据微信回调的URL的参数去获取公众号的接口调用凭据和授权信息
        Jedis jedis = jedisUtils.getJedis(Constants.HEADER_APP_NAME);//获取jedis对象

        String access_token = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "component_access_token");

        JSONObject authJsonObj = WeiXinUtil.getServiceAuthInfo(access_token, code,Constants.HEADER_APP_NAME );
        Object errCode = authJsonObj.get("errcode");
        if (errCode != null ) {
            log.error("微信授权回调 获取授权信息失败-----------"+authJsonObj);
            throw new QNLiveException("150001");
        }

        JSONObject authauthorizer_info = authJsonObj.getJSONObject("authorization_info");

        String authorizer_appid = authauthorizer_info.getString("authorizer_appid");

        //获取公众号的头像 昵称 QRCode等相关信息
        JSONObject serviceNoJsonObj = WeiXinUtil.getServiceAuthAccountInfo(access_token, authorizer_appid,Constants.HEADER_APP_NAME);
        errCode = serviceNoJsonObj.get("errcode");
        if (errCode != null ) {
            log.error("微信授权回调 获取服务号相关信息失败-----------"+authJsonObj);
            throw new QNLiveException("150001");
        }
        Map<String,Object> result = new HashMap<String,Object>();//返回重定向的url
        return result;

    }

    /**
     * 店铺-店铺信息
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopInfo")
    public Map<String,String> shopInfo(RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String,Object> param = new HashMap<>();
        param.put("user_id",userId);
        reqEntity.setParam(param);
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        Map<String,String> userMap = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
        Map<String,String> shop = CacheUtils.readShop(userId, reqEntity, readShopOperation, jedis);//saaSModuleServer.getShopInfo(param);
        if(shop == null){
            throw new QNLiveException("190001");
        }
        shop.put("user_id",userId);
        shop.put("nick_name",userMap.get("nick_name"));
        shop.put("phone_num",userMap.get("phone_number"));
        return shop;
    }
    /**
     * 店铺设置
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopEdit")
    public void shopEdit(RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String,Object> param = (Map<String, Object>) reqEntity.getParam();
        param.put("user_id",userId);
        saaSModuleServer.updateShop(param);
    }

	/**
	 * 店铺-轮播图列表
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("shopBannerList")
    public Map<String, Object>  shopBannerList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
       /* Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //用户信息
        Map<String, String> userInfo = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
        String shopId = userInfo.get("shop_id");*/
        Map<String, Object> result = saaSModuleServer.getShopBannerList(reqMap);
        return result;
    }
	/**
	 * 店铺-添加轮播图
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("shopBannerAdd")
    public Map<String, Object>  addBanner(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,Object> shopInfo = saaSModuleServer.getShopInfo(reqMap);
        //判断店铺存在
        if(shopInfo == null){
            throw new QNLiveException("190001");
        }
        String shopId = shopInfo.get("shop_id").toString();
        String linkType = reqMap.get("link_type").toString();
        String linkTo = null;
        if("1".equals(linkType)){
            //外部链接
            if(reqMap.get("link_id")==null){
                //链接为空判断
                throw new QNLiveException("000004");
            }
            linkTo = reqMap.get("link_type").toString();
            reqMap.put("link_to",linkTo);
        }else {
            if(reqMap.get("link_id")==null){
                //课程ID为空判断
                throw new QNLiveException("000004");
            }
            //课程，系列ID
            String linkId = reqMap.get("link_id").toString();
            if("2".equals(linkType)){
                //单品ID
                linkTo = "/course/single/detail/"+linkId;
            }else if("3".equals(linkType)){
                //系列ID
                linkTo = "/course/series/detail/"+linkId;
            }else if("3".equals(linkType)){
                //直播
                linkTo = "/user/courses/"+linkId;
            }
            reqMap.put("link_to",linkTo);
        }
        reqMap.put("create_time",new Date());
        reqMap.put("app_name",reqEntity.getAppName());
        reqMap.put("banner_id",MiscUtils.getUUId());
        reqMap.put("shop_id",shopId);
        Map<String, Object> result = saaSModuleServer.addShopBanner(reqMap);
        return result;
    }
    
    /**
     * 获取店铺轮播列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("queryShopBannerList")
    public Map<String, Object>  queryShopBannerList(RequestEntity reqEntity) throws Exception{
    	Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //添加状态要求：上架
        reqMap.put("status", "1");
        reqMap.put("app_name", reqEntity.getAppName());
        
        List<Map<String, Object>> bannerList = saaSModuleServer.getShopBannerListForFront(reqMap);
        
        resultMap.put("banner_info_list", bannerList);
        return resultMap;
    }

}