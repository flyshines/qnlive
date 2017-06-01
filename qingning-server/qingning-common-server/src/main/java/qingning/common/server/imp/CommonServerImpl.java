package qingning.common.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.model.FetchRet;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
//import jxl.Sheet;
//import jxl.Workbook;
//import jxl.write.WritableWorkbook;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import qingning.common.dj.DjSendMsg;
import qingning.common.entity.AccessToken;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.server.other.*;
import qingning.common.server.util.DES;
import qingning.common.util.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ICommonModuleServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonServerImpl extends AbstractQNLiveServer {
 
    private static final Logger logger   = LoggerFactory.getLogger(CommonServerImpl.class);
    private ICommonModuleServer commonModuleServer;
    private ReadDistributerOperation readDistributerOperation;
    private ReadUserOperation readUserOperation;
    private ReadCourseOperation readCourseOperation;
    private ReadLiveRoomOperation readLiveRoomOperation;
    private ReadAppVersionOperation readAPPVersionOperation;
	private ReadRoomDistributer readRoomDistributer;	
    private ReadForceVersionOperation readForceVersionOperation;
    private ReadLecturerOperation readLecturerOperation;
    private ReadRoomDistributerOperation readRoomDistributerOperation;

    @Override
    public void initRpcServer() {
        if(commonModuleServer == null){
            commonModuleServer = this.getRpcService("commonModuleServer");
            readDistributerOperation = new ReadDistributerOperation(commonModuleServer);
            readUserOperation = new ReadUserOperation(commonModuleServer);
            readCourseOperation = new ReadCourseOperation(commonModuleServer);
            readLiveRoomOperation = new ReadLiveRoomOperation(commonModuleServer);
			readRoomDistributer = new ReadRoomDistributer(commonModuleServer);
            readAPPVersionOperation = new ReadAppVersionOperation(commonModuleServer);
            readForceVersionOperation = new ReadForceVersionOperation(commonModuleServer);
            readLecturerOperation = new ReadLecturerOperation(commonModuleServer);
            readRoomDistributerOperation = new ReadRoomDistributerOperation(commonModuleServer);
        }        
    }
 
    private static Auth auth;
    static {
        auth = Auth.create (MiscUtils.getConfigKey("qiniu_AK"), MiscUtils.getConfigKey("qiniu_SK"));
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("logUserInfo")
    public Map<String,Object> collectClientInformation (RequestEntity reqEntity) throws Exception{
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String,Object> reqMap = (Map<String,Object>)reqEntity.getParam();
        long loginTime = System.currentTimeMillis();        
        if(!"2".equals(reqMap.get("status"))){
            reqMap.put("create_time", loginTime);
            reqMap.put("create_date", MiscUtils.getDate(loginTime));
            if(!MiscUtils.isEmpty(reqEntity.getAccessToken())){
                String user_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
                reqMap.put("user_id", user_id);                
                Map<String,String> userInfo = CacheUtils.readUser(user_id, reqEntity, readUserOperation, jedis);
                reqMap.put("gender", userInfo.get("gender"));
                Map<String,String> queryParam = new HashMap<>();                
                queryParam.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
                String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, queryParam);
                Map<String,String> accessTokenInfo = jedis.hgetAll(accessTokenKey);
                if(MiscUtils.isEmpty(accessTokenInfo)){
                    Map<String, Object> queryMap = new HashMap<String, Object>();                    
                    String login_id = (String)reqMap.get("login_id");
                    String login_type = (String)reqMap.get("login_type");
                    accessTokenInfo = new HashMap<String,String>();
                    if(!MiscUtils.isEmpty(login_id)){
                        queryMap.put("login_type", login_type);
                        queryMap.put("login_id", login_id);
                    	MiscUtils.converObjectMapToStringMap(commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap), accessTokenInfo);
                    } else {                    	
                    	MiscUtils.converObjectMapToStringMap(commonModuleServer.findLoginInfoByUserId(user_id), accessTokenInfo);
                    	reqMap.put("login_id", accessTokenInfo.get("union_id"));
                    }
                }
                reqMap.put("record_time", userInfo.get("create_time"));
                reqMap.put("old_subscribe", accessTokenInfo.get("subscribe"));
                reqMap.put("country", userInfo.get("country"));
                reqMap.put("province", userInfo.get("province"));
                reqMap.put("city", userInfo.get("city"));
                reqMap.put("district", userInfo.get("district"));
                String web_openid = (String)accessTokenInfo.get("web_openid");
                reqMap.put("subscribe", null);                
                reqMap.put("web_openid", null);
                if(MiscUtils.isEmpty(web_openid)){
                    reqMap.put("subscribe", "0");
                } else {
                    reqMap.put("web_openid", web_openid);
                }
                
                String user_role = MiscUtils.convertString(accessTokenInfo.get("user_role"));
                if(user_role.contains(Constants.USER_ROLE_LECTURER)){
                    reqMap.put("live_room_build", "1");
                } else {
                    reqMap.put("live_room_build", "0");
                }
                String status = (String)reqMap.get("status");
                if("1".equals(status) || "3".equals(status)){
                	Map<String,String> updateValue = new HashMap<String,String>();
                	updateValue.put("last_login_time", loginTime+"");
                	updateValue.put("last_login_ip", (String)reqMap.get("ip"));
                	String plateform = (String)userInfo.get("plateform");
                	String newPalteForm = (String)reqMap.get("plateform");
                	if(MiscUtils.isEmpty(plateform)){
                		plateform = newPalteForm;
                	} else if(!MiscUtils.isEmpty(newPalteForm) && plateform.indexOf(newPalteForm) == -1){
                		plateform=plateform+","+newPalteForm;
                	}
                	updateValue.put("plateform", plateform);
                	Map<String,Object> query = new HashMap<String,Object>();
                	query.put("user_id", user_id);
                	String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query);
                	jedis.hmset(key, updateValue);
                	jedis.sadd(Constants.CACHED_UPDATE_USER_KEY, user_id);
                }
            }
            RequestEntity requestEntity = this.generateRequestEntity("LogServer",  Constants.MQ_METHOD_ASYNCHRONIZED, "logUserInfo", reqMap);
            requestEntity.setAppName(appName);
            mqUtils.sendMessage(requestEntity);
        }
   //     loginTime = System.currentTimeMillis();
        Map<String,Object> resultMap = new HashMap<String, Object>();
        resultMap.put("server_time", System.currentTimeMillis());

        return resultMap;
    }

    /**
     * 获取版本信息 根据不同的平台
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("getVersion")
    public Map<String,Object> getVersion (RequestEntity reqEntity) throws Exception{
        Map<String,Object> map = (HashMap<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        Integer plateform = (Integer)map.get("plateform");//平台 0是直接放过 1是安卓 2是IOS 3是JS
        if(plateform != 0) {//安卓或者ios
            Map<String, String> versionInfoMap = CacheUtils.readAppVersion(plateform.toString(), reqEntity, readAPPVersionOperation, jedis, true);
            if (!MiscUtils.isEmpty(versionInfoMap) && Integer.valueOf(versionInfoMap.get("status")) != 0) { //判断有没有信息 判断是否存在总控 总控 0关闭就是不检查 1开启就是检查
                //1.先判断系统和当前version
                if (compareVersion(plateform.toString(), versionInfoMap.get("version_no"), map.get("version").toString())) {//当前version 小于 最小需要跟新的版本
                    Map<String,Object> reqMap = new HashMap<>();
                    reqMap.put("version_info",versionInfoMap);
                    return reqMap;
                }
            }
        }
    return null;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("control")
    public Map<String,String> control (RequestEntity reqEntity) throws Exception{
        Map<String,Object> map = (HashMap<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        Map<String,String> retMap = new HashMap<>();
        Integer plateform = (Integer)map.get("plateform");//平台 0是直接放过 1是安卓 2是IOS 3是JS
        if(plateform != 0) {
            Map<String, String> versionInfoMap = CacheUtils.readAppVersion(plateform.toString(), reqEntity, readAPPVersionOperation, jedis, true);
            if (!MiscUtils.isEmpty(versionInfoMap)) { //判断有没有信息
                if(versionInfoMap.get("os_audit_version") != null){
                    retMap.put("os_audit_version",versionInfoMap.get("os_audit_version"));
                    return retMap;
                }
            }
        }
        return null;
    }



    //客户版本号小于系统版本号 true， 否则为false
    private boolean compareVersion(String plateform, String systemVersion, String customerVersion) {
        //1：andriod 2:IOS 3是js
        if(plateform.equals("1")){
            if(customerVersion.compareTo(systemVersion) < 0){
                return true;
            }else {
                return false;
            }
        }else {
            String[] systemVersionArray = systemVersion.split("\\.");
            String[] customerVersionArray = customerVersion.split("\\.");

            if(systemVersionArray.length >= customerVersionArray.length){

                for(int i=0; i < systemVersionArray.length; i++){
                    if(customerVersionArray.length - 1 >= i){
                        Integer systemCode = Integer.parseInt(systemVersionArray[i]);
                        Integer customerCode =  Integer.parseInt(customerVersionArray[i]);
                        if(customerCode < systemCode){
                            return true;
                        }
                    }
                }
            }

            if(systemVersionArray.length < customerVersionArray.length){

                for(int i=0; i < customerVersionArray.length; i++){
                    if(systemVersionArray.length - 1 >= i){
                        Integer systemCode = Integer.parseInt(systemVersionArray[i]);
                        Integer customerCode =  Integer.parseInt(customerVersionArray[i]);
                        if(customerCode < systemCode){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    @FunctionName("serverTime")
    public Map<String,Object> getServerTime (RequestEntity reqEntity) throws Exception{
        Map<String,Object> resultMap = new HashMap<String, Object>();
        resultMap.put("server_time", System.currentTimeMillis());
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("userLogin")
    public Map<String,Object> userLogin (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Map<String,Object> resultMap = new HashMap<>();
        Map<String,Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(reqMap);
        Jedis jedis = jedisUtils.getJedis(appName);
        int login_type_input = Integer.parseInt(reqMap.get("login_type").toString());
        reqMap.put("app_name",appName);
        switch (login_type_input){
 
            case 0 : //微信登录
                //如果登录信息为空，则进行注册
            	if(MiscUtils.isEmpty(loginInfoMap)){
                    //注册IM
                    Map<String,String> imResultMap = null;
                    try {
                        imResultMap =    IMMsgUtil.createIMAccount(reqMap.get("device_id").toString());
                    }catch (Exception e){
                        //TODO 暂不处理
                    }
                        //初始化数据库相关表
                        reqMap.put("m_user_id", imResultMap.get("uid"));
                        reqMap.put("m_pwd", imResultMap.get("password"));
                        //设置默认用户头像
                        String transferAvatarAddress = (String)reqMap.get("avatar_address");
                        if(!MiscUtils.isEmpty(transferAvatarAddress)){
                            try{
                                transferAvatarAddress = qiNiuFetchURL(reqMap.get("avatar_address").toString(),appName);
                            } catch(Exception e){
                                transferAvatarAddress = null;
                            }
                        }
                        if(MiscUtils.isEmpty(transferAvatarAddress)){
                            transferAvatarAddress = MiscUtils.getConfigByKey("default_avatar_address",appName);
                        }
                        
                        reqMap.put("avatar_address",transferAvatarAddress);
                        
                        if(reqMap.get("nick_name") == null || StringUtils.isBlank(reqMap.get("nick_name").toString())){
                            reqMap.put("avatar_address","用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
                        }
                        Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
                        //生成access_token，将相关信息放入缓存，构造返回参数
                        processLoginSuccess(1, dbResultMap, null, resultMap,appName);
                }else{
                    //构造相关返回参数 TODO
                    processLoginSuccess(2, null, loginInfoMap, resultMap,appName);
                }
                break;
 
            case 1 : //QQ登录
                //TODO
                break;
            case 2 : //手机号登录
            	if(MiscUtils.isEmpty(loginInfoMap)){
                    //抛出用户不存在
                    throw new QNLiveException("120002");
                }else {
                    //校验用户名和密码
                    //登录成功
                    if(reqMap.get("certification").toString().equals(loginInfoMap.get("passwd").toString())){
                        //构造相关返回参数 TODO
                        processLoginSuccess(2, null, loginInfoMap, resultMap,appName);
                    }else{
                        //抛出用户名或者密码错误
                        throw new QNLiveException("120001");
                    }
                }
                break;
            case 4:
            	if(MiscUtils.isEmpty(loginInfoMap)){
            		throw new QNLiveException("120002");
            	}
            	break;
        }
        return resultMap;
    }

    //<editor-fold desc="微信授权code登录">
    /**
     * 微信授权code登录
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("weixinCodeUserLogin")
    public Map<String,Object> weixinCodeUserLogin (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<String, Object>();
        String subscribe = "0";
        resultMap.put("key","1");//钥匙 用于在controller判断跳转的页面

        //1.传递授权code及相关参数，调用微信验证code接口
        String code = reqMap.get("code").toString();
        String app_name = reqMap.get("state").toString();//携带回来的参数 返回appname 进行用户区分
        Jedis jedis = jedisUtils.getJedis(app_name);//获取缓存工具对象 db
        JSONObject getCodeResultJson = WeiXinUtil.getUserInfoByCode(code,app_name);
        if(getCodeResultJson == null || getCodeResultJson.getInteger("errcode") != null || getCodeResultJson.getString("openid") == null){
            if(getCodeResultJson.getString("openid") == null){
                resultMap.put("key","0");
                return resultMap;
            }
            throw new QNLiveException("120008");
        }
        String openid = getCodeResultJson.getString("openid");//拿到openid
        //获取用户信息
        try{
            AccessToken wei_xin_access_token =  WeiXinUtil.getAccessToken(null,null,jedis,app_name);//获取公众号access_token
            JSONObject user = WeiXinUtil.getUserByOpenid(wei_xin_access_token.getToken(),openid,app_name);//获取是否有关注公众信息
            if(user.get("subscribe") != null){
                subscribe = user.get("subscribe").toString();
            }
        }catch(Exception e){
            throw new QNLiveException("120008");
        }
        reqMap.put("subscribe",subscribe);
        //1.2如果验证成功，则得到用户的union_id和用户的access_token。
        //1.2.1根据 union_id查询数据库
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("login_type","4");//4.微信code方式登录
        queryMap.put("web_openid",openid);
        Map<String,Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);

        //1.2.1.1如果用户存在则进行登录流程
        if(loginInfoMap != null){//有
            if(!loginInfoMap.get("subscribe").toString().equals(subscribe)){
                Map<String,Object> userMap = new HashMap<>();
                userMap.put("subscribe",subscribe);
                userMap.put("web_openid",openid);
                userMap.put("user_id",loginInfoMap.get("user_id"));
                commonModuleServer.updateUserWebOpenIdByUserId(userMap);
            }
            processLoginSuccess(2, null, loginInfoMap, resultMap,app_name);//获取后台安全证书 access_token
            return resultMap;
        }else {
            //1.2.1.2 如果用户不存在，则根据用户的open_id和用户的access_token调用微信查询用户信息接口，得到用户的头像、昵称等相关信息
            String userWeixinAccessToken = getCodeResultJson.getString("access_token");
            JSONObject userJson = WeiXinUtil.getUserInfoByAccessToken(userWeixinAccessToken, openid,app_name);
            // 根据得到的相关用户信息注册用户，并且进行登录流程。
            if(userJson == null || userJson.getInteger("errcode") != null || userJson.getString("unionid") == null){
                if(userJson.getString("unionid") == null){
                    resultMap.put("key","0");
                    return resultMap;
                }
                throw new QNLiveException("120008");
            }

            queryMap.clear();
            queryMap.put("login_type","0");//0.微信方式登录
            queryMap.put("login_id",userJson.getString("unionid"));//unionid 登录
            Map<String,Object> loginInfoMapFromUnionid = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);
            if(loginInfoMapFromUnionid != null){
                //将open_id更新到login_info表中
                Map<String,Object> updateMap = new HashMap<>();
                updateMap.put("user_id", loginInfoMapFromUnionid.get("user_id").toString());
                updateMap.put("web_openid", openid);
                updateMap.put("subscribe",subscribe);
                commonModuleServer.updateUserWebOpenIdByUserId(updateMap);
                processLoginSuccess(2, null, loginInfoMapFromUnionid, resultMap,app_name);
                return resultMap;
            }

            String nickname = userJson.getString("nickname");//昵称
            String sex = userJson.getString("sex");//性别
            String headimgurl = userJson.getString("headimgurl");//头像
            if(sex == null || nickname == null || headimgurl == null){
                resultMap.put("key","0");
                return resultMap;
            }


            Map<String,String> imResultMap = null;
            try {
                imResultMap = IMMsgUtil.createIMAccount("weixinCodeLogin");//注册im
            }catch (Exception e){
                //TODO 暂不处理
            }

            //初始化数据库相关表
            reqMap.put("m_user_id", imResultMap.get("uid"));
            reqMap.put("m_pwd", imResultMap.get("password"));
            //设置默认用户头像
            if(MiscUtils.isEmpty(headimgurl)){
                reqMap.put("avatar_address",MiscUtils.getConfigByKey("default_avatar_address",app_name));//TODO
            }else {
                String transferAvatarAddress = qiNiuFetchURL(headimgurl,app_name);
                reqMap.put("avatar_address",transferAvatarAddress);
            }

            if(MiscUtils.isEmpty(nickname)){
                reqMap.put("nick_name","用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
            }else {
                reqMap.put("nick_name", nickname);
            }

            if(MiscUtils.isEmpty(sex)){
                reqMap.put("gender","2");//TODO
            }

            //微信性别与本系统性别转换
            //微信用户性别 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
            if(sex.equals("1")){
                reqMap.put("gender","1");//TODO
            }
            if(sex.equals("2")){
                reqMap.put("gender","0");//TODO
            }
            if(sex.equals("0")){
                reqMap.put("gender","2");//TODO
            }

            String unionid =  userJson.getString("unionid");
            reqMap.put("unionid",unionid);
            reqMap.put("web_openid",openid);
            reqMap.put("app_name",app_name);
            reqMap.put("login_type","4");

            Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
            //生成access_token，将相关信息放入缓存，构造返回参数
            processLoginSuccess(1, dbResultMap, null, resultMap,app_name);
            return resultMap;
        }
    }
    //</editor-fold>

    /**
     * PC端  微信授权code登录
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("pcCodeUserLogin")
    public Map<String,Object> pcCodeUserLogin (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<String, Object>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        String subscribe = "0";
        resultMap.put("key","0");//未绑定手机号码

        //1.传递授权code及相关参数，调用微信验证code接口
        String code = reqMap.get("code").toString();

        JSONObject accountJson = WeiXinUtil.getPCUserAccountInfo(code,appName);

        Object errCode = accountJson.get("errcode");
        if (errCode != null ) {
            throw new QNLiveException("120008");
        }

        String openid = accountJson.getString("openid");
        String union_id = accountJson.getString("unionid");

        //PC和公众平台共用的接口 getUserByOpenid是公众平台特有的接口
        JSONObject userJson = WeiXinUtil.getUserInfoByAccessToken(accountJson.getString("access_token"), openid,appName);
        errCode = userJson.get("errcode");
        if (errCode != null ) {
            throw new QNLiveException("120008");
        }
//        if(userJson.get("subscribe") != null){
//            subscribe = userJson.get("subscribe").toString();
//        }

        String nickname = userJson.getString("nickname");//昵称
        resultMap.put("name", nickname);

        //1.2如果验证成功，则得到用户的union_id和用户的access_token。
        //1.2.1根据 union_id查询数据库
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("login_type","0");//0与union查询
        queryMap.put("login_id",union_id);
        Map<String,Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);


        //1.2.1.1如果用户存在则进行登录流程
        if(loginInfoMap != null){//有
            processLoginSuccess(2, null, loginInfoMap, resultMap,reqEntity.getAppName());//获取后台安全证书 access_token
            Object phone = loginInfoMap.get("phone_number");
            if (phone != null) { //有直播间和手机号
                resultMap.put("key","1");
            } else {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(Constants.CACHED_KEY_LECTURER_FIELD, loginInfoMap.get("user_id"));
                String liveRoomListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
                Map<String,String> liveRoomsMap = jedis.hgetAll(liveRoomListKey);

                if(CollectionUtils.isEmpty(liveRoomsMap)){//登录过 没有直播间信息
                    resultMap.put("key","3");
                }
            }
            return resultMap;
        } else {
            String sex = userJson.getString("sex");//性别
            String headimgurl = userJson.getString("headimgurl");//头像

            Map<String,String> imResultMap = null;
            try {
                imResultMap = IMMsgUtil.createIMAccount("weixinCodeLogin");//注册im
            }catch (Exception e){
                //TODO 暂不处理
            }

            //初始化数据库相关表
            reqMap.put("m_user_id", imResultMap.get("uid"));
            reqMap.put("m_pwd", imResultMap.get("password"));
            //设置默认用户头像
            if(MiscUtils.isEmpty(headimgurl)){
                reqMap.put("avatar_address",MiscUtils.getConfigByKey("default_avatar_address",appName));//TODO
            }else {
                String transferAvatarAddress = qiNiuFetchURL(headimgurl,appName);
                reqMap.put("avatar_address",transferAvatarAddress);
            }

            if(MiscUtils.isEmpty(nickname)){
                reqMap.put("nick_name","用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
            }else {
                reqMap.put("nick_name", nickname);
            }

            if(MiscUtils.isEmpty(sex)){
                reqMap.put("gender","2");//TODO
            }

            //微信性别与本系统性别转换
            //微信用户性别 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
            if(sex.equals("1")){
                reqMap.put("gender","1");//TODO
            }
            if(sex.equals("2")){
                reqMap.put("gender","0");//TODO
            }
            if(sex.equals("0")){
                reqMap.put("gender","2");//TODO
            }

            union_id =  userJson.getString("unionid");
            reqMap.put("unionid",union_id);
            reqMap.put("web_openid",openid);
            reqMap.put("login_type","4");
            reqMap.put("subscribe",subscribe);
            Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
            //生成access_token，将相关信息放入缓存，构造返回参数
            processLoginSuccess(1, dbResultMap, null, resultMap,reqEntity.getAppName());

            return resultMap;
        }
    }

    /**
     *
     * @param type 1新注册用户处理方式，2老用户处理方式
     * @param dbResultMap 当type为1时，传入该值，该值为新用户注册后返回的信息；当type为2时该值传入null
     * @param loginInfoMap
     * @param resultMap service要返回的map
     */
    private void processLoginSuccess(Integer type, Map<String,String> dbResultMap, Map<String,Object> loginInfoMap,
                                     Map<String,Object> resultMap,String appName) throws Exception{
        Jedis jedis = jedisUtils.getJedis(appName);//根据appname 读取不同的redis db
        String last_login_date = (new Date()).getTime() + "";
        String user_id = null;
        String m_user_id = null;
        String m_pwd = null;
        Map<String,String> cacheMap = new HashMap<String,String>();
 
        //新注册用户,重新查询loginInfo
        if(type == 1){
            user_id = dbResultMap.get("user_id");
            loginInfoMap = commonModuleServer.findLoginInfoByUserId(user_id);
            //老用户
        }else if(type == 2){
            user_id = loginInfoMap.get("user_id").toString();
            //如果发现IM账号为空，则重新尝试注册IM账号
            if(loginInfoMap.get("m_user_id") == null){
                Map<String,String> imResultMap = null;
                try {
                    imResultMap = IMMsgUtil.createIMAccount("supply");
                }catch (Exception e){
                    //TODO 暂不处理
                }

                if(! MiscUtils.isEmpty(imResultMap)){
                    m_user_id = imResultMap.get("uid");
                    m_pwd = imResultMap.get("password");

                    if(!MiscUtils.isEmpty(m_user_id) && !MiscUtils.isEmpty(m_pwd)){
                        //更新login_info表
                        Map<String,Object> updateIMAccountMap = new HashMap<>();
                        updateIMAccountMap.put("m_user_id",m_user_id);
                        updateIMAccountMap.put("m_pwd",m_pwd);
                        updateIMAccountMap.put("user_id",user_id);
                        commonModuleServer.updateIMAccount(updateIMAccountMap);
                    }
                }
            }
        }
 
        //1.将objectMap转为StringMap
        m_user_id = loginInfoMap.get("m_user_id") == null ? null : loginInfoMap.get("m_user_id").toString();
        m_pwd = loginInfoMap.get("m_pwd") == null ? null : loginInfoMap.get("m_pwd").toString();
        MiscUtils.converObjectMapToStringMap(loginInfoMap, cacheMap);
 
        //2.根据相关信息生成access_token
        String access_token = AccessTokenUtil.generateAccessToken(user_id, last_login_date);
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("access_token", access_token);
        String process_access_token = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
        jedis.hmset(process_access_token, cacheMap);
        jedis.expire(process_access_token, Integer.parseInt(MiscUtils.getConfigByKey("access_token_expired_time",appName)));
        
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("user_id", user_id);        
        Map<String, String> userMap = CacheUtils.readUser(user_id, this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);
        
        //3.增加相关返回参数
        resultMap.put("access_token", access_token);
        resultMap.put("im_account_info", encryptIMAccount(m_user_id, m_pwd));
        resultMap.put("m_user_id", m_user_id);
        resultMap.put("user_id", user_id);
                
        resultMap.put("avatar_address", userMap.get("avatar_address"));
        //resultMap.put("nick_name", MiscUtils.RecoveryEmoji(userMap.get("nick_name")));
        resultMap.put("nick_name", userMap.get("nick_name"));
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("qiNiuUploadToken")
    public Map<String,Object> getQiNiuUploadToken (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<String, Object>();
        long expiredTime = 3600;
        String token = null;
        String url = null;
        if("1".equals (reqMap.get("upload_type"))){ //图片
            url = IMMsgUtil.configMap.get("images_space_domain_name");
            token = auth.uploadToken(IMMsgUtil.configMap.get("image_space"), null, expiredTime, new StringMap()
                    .putNotEmpty("returnBody", "{\"key\": $(key), \"hash\": $(etag), \"width\": $(imageInfo.width), \"height\": $(imageInfo.height)}"));
 
        } else if("2".equals (reqMap.get("upload_type"))){ //音频
            url = IMMsgUtil.configMap.get("audio_space_domain_name");
            token = auth.uploadToken(IMMsgUtil.configMap.get("audio_space"), null, expiredTime, (StringMap)null);
        }
        resultMap.put("upload_token", token);
        resultMap.put("access_prefix_url", url);
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("weiXinConfiguration")
    public Map<String,String> getWeiXinConfiguration (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        String appName = reqEntity.getAppName();
        String JSApiTIcket = WeiXinUtil.getJSApiTIcket(jedisUtils.getJedis(appName),appName);
        return WeiXinUtil.sign(JSApiTIcket, reqMap.get("url").toString(),appName);
    }

    /**
     * 查询用户信息
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("userInfo")
    public Map<String,Object> getUserInfo (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<String, Object>();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //1:个人中心信息 2：个人基本信息
        String queryType = reqMap.get("query_type").toString();
        Map<String,String> values = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
        if(queryType.equals("1")){
            reqMap.put("user_id", userId);
            resultMap.put("avatar_address", values.get("avatar_address"));
            resultMap.put("nick_name", MiscUtils.RecoveryEmoji(values.get("nick_name")));
            resultMap.put("course_num", MiscUtils.convertObjToObject(values.get("course_num"), Constants.SYSLONG, "course_num", 0l));
            resultMap.put("live_room_num", MiscUtils.convertObjToObject(values.get("live_room_num"), Constants.SYSLONG, "live_room_num", 0l));
            resultMap.put("today_distributer_amount",MiscUtils.convertObjToObject(values.get("today_distributer_amount"), Constants.SYSDOUBLE, "today_distributer_amount", 0d, true));
            resultMap.put("update_time", MiscUtils.convertObjToObject(values.get("update_time"),Constants.SYSLONG,"update_time", 0l));            
        }else if(queryType.equals("2")){            
            if(MiscUtils.isEmpty(values)){
                throw new QNLiveException("120002");
            }
            Map<String,Object> query = new HashMap<String,Object>();
            query.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, query);            
            Map<String,String> loginInfoMap = jedis.hgetAll(key);
            if(MiscUtils.isEmpty(loginInfoMap)){
                throw new QNLiveException("120002");
            }
            resultMap.put("access_token", reqEntity.getAccessToken());
            resultMap.put("im_account_info", encryptIMAccount(loginInfoMap.get("m_user_id").toString(), loginInfoMap.get("m_pwd").toString()));
            resultMap.put("m_user_id", loginInfoMap.get("m_user_id"));
            resultMap.put("user_id", userId);
            resultMap.put("avatar_address", values.get("avatar_address"));
            resultMap.put("nick_name", MiscUtils.RecoveryEmoji(values.get("nick_name")));
            return resultMap;
        }
        return resultMap;
    }

    //<editor-fold desc="生成具体微信订单">
    /**
     * 生成具体微信订单
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("generateWeixinPayBill")
    public Map<String,String> generateWeixinPayBill (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        //1.检测课程是否存在，课程不存在则给出提示（ 课程不存在，120009）
        String courseId = reqMap.get("course_id").toString();       
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("course_id", courseId);
        Map<String,String> courseMap = CacheUtils.readCourse(courseId, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, false);
       
        if(MiscUtils.isEmpty(courseMap)){    //如果课程不存在
        	throw new QNLiveException("120009");
        } 
        //2.如果支付类型为打赏，则检测内存中的打赏类型是否存在，如果不存在则给出提示（120010，打赏类型不存在）
        String profit_type = reqMap.get("profit_type").toString();
 
        //3.插入t_trade_bill表 交易信息表
        String goodName = null;
        Integer totalFee = 0;
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String,Object> insertMap = new HashMap<>();
        insertMap.put("user_id",userId);
        insertMap.put("room_id",courseMap.get("room_id"));
        insertMap.put("course_id",courseMap.get("course_id"));
        //判断类型为 0:课程收益 1:打赏
        if(profit_type.equals("1")){
            insertMap.put("amount", reqMap.get("reward_amount"));
            totalFee = ((Long)reqMap.get("reward_amount")).intValue();
            goodName = MiscUtils.getConfigByKey("weixin_pay_reward_course_good_name",appName) +"-" + MiscUtils.RecoveryEmoji(courseMap.get("course_title"));
        }else if(profit_type.equals("0")){
            insertMap.put("amount", courseMap.get("course_price"));
            totalFee = Integer.parseInt(courseMap.get("course_price"));
            goodName = MiscUtils.getConfigByKey("weixin_pay_buy_course_good_name",appName) +"-" + MiscUtils.RecoveryEmoji(courseMap.get("course_title"));
        }
        insertMap.put("status","0");
        String tradeId = MiscUtils.getWeiXinId();//TODO
        insertMap.put("trade_id",tradeId);
        insertMap.put("profit_type",profit_type);
        commonModuleServer.insertTradeBill(insertMap);
        
        query.clear();        
        query.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, query);
 
        //4.调用微信生成预付单接口
        String terminalIp = reqMap.get("remote_ip_address").toString();
        String outTradeNo = tradeId;
        String platform = (String) reqMap.get("platform");
        String openid = null;

        boolean isWeb = false;
        if (platform == null || platform.equals("3") || platform.equals("0")) {//0web 3是js调用 默认是js web调用
            Map<String,String> userMap =jedis.hgetAll(key);
            openid = userMap.get("web_openid");
            isWeb = true;
        }

        Map<String, String> payResultMap = TenPayUtils.sendPrePay(goodName, totalFee, terminalIp, outTradeNo, openid, platform,appName);
 
        //5.处理生成微信预付单接口
        if (payResultMap.get ("return_code").equals ("FAIL")) {
            //更新交易表
            Map<String,Object> failUpdateMap = new HashMap<>();
            failUpdateMap.put("status","3");
            failUpdateMap.put("close_reason","生成微信预付单失败 "+payResultMap.get("return_msg")+ payResultMap.get ("err_code_des"));
            failUpdateMap.put("trade_id",tradeId);
            commonModuleServer.closeTradeBill(failUpdateMap);
 
            throw new QNLiveException("120015");
        } else {
            //成功，则需要插入支付表
            Map<String,Object> insertPayMap = new HashMap<>();
            insertPayMap.put("trade_id",tradeId);
            insertPayMap.put("payment_id",MiscUtils.getUUId());
            insertPayMap.put("payment_type",0);
            insertPayMap.put("status","1");
            insertPayMap.put("pre_pay_no",payResultMap.get("prepay_id"));
            insertPayMap.put("create_time",new Date());
            insertPayMap.put("update_time",new Date());
            commonModuleServer.insertPaymentBill(insertPayMap);
 
            //返回相关参数给前端.
            SortedMap<String,String> resultMap = new TreeMap<>();
            if (isWeb) {
                resultMap.put("appId",MiscUtils.getConfigByKey("appid",appName));
                resultMap.put("package", "prepay_id="+payResultMap.get("prepay_id"));
            } else  {
                resultMap.put("prepayId", payResultMap.get("prepay_id"));
                resultMap.put("package", "Sign=WXPay");
            }
            resultMap.put("nonceStr", payResultMap.get("random_char"));
            resultMap.put("signType", "MD5");
            resultMap.put("timeStamp",System.currentTimeMillis()/1000 + "");

            String paySign = null;
            if (isWeb) {
                paySign = TenPayUtils.getSign(resultMap, platform,appName);
            } else {
                SortedMap<String,String> signMap = new TreeMap<>();
                signMap.put("appid", MiscUtils.getConfigByKey("app_app_id",appName));
                signMap.put("partnerid", MiscUtils.getConfigByKey("weixin_app_pay_mch_id",appName));
                signMap.put("prepayid", resultMap.get("prepayId"));
                signMap.put("package", resultMap.get("package"));
                signMap.put("noncestr", resultMap.get("nonceStr"));
                signMap.put("timestamp", resultMap.get("timeStamp"));
                paySign = TenPayUtils.getSign(signMap, platform,appName);
            }

            resultMap.put ("paySign", paySign);

            return resultMap;
        }
    }

    @FunctionName("checkWeixinPayBill")
    public Map<String,String> checkWeixinPayBill (RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String pre_pay_no = reqMap.get("payment_id").toString();
        Map<String,Object> billInfo = commonModuleServer.findTradeBillByPaymentid(pre_pay_no);

        Map<String, String> result = new HashMap<>();
        if (billInfo != null) {
            Map<String, String> payResultMap = TenPayUtils.checkPayResult(billInfo.get("trade_id").toString(), reqMap.get("platform").toString(),reqEntity.getAppName());

            if ("SUCCESS".equals (payResultMap.get ("return_code")) && "SUCCESS".equals (payResultMap.get ("result_code"))) {
                String trade_state = payResultMap.get("trade_state");
                if ("SUCCESS".equals(trade_state)) {
                    result.put("state", "0");
                } else if ("REFUND".equals(trade_state)) {
                    result.put("state", "1");
                } else if ("NOTPAY".equals(trade_state)) {
                    result.put("state", "2");
                } else if ("CLOSED".equals(trade_state)) {
                    result.put("state", "3");
                } else if ("REVOKED".equals(trade_state)) {
                    result.put("state", "4");
                } else if ("USERPAYING".equals(trade_state)) {
                    result.put("state", "5");
                } else if ("PAYERROR".equals(trade_state)) {
                    result.put("state", "6");
                } else  {
                    result.put("state", "7");
                }
            } else {
                result.put("state", "7");
            }
        } else {
            result.put("state", "8");
        }
        return result;
//        SUCCESS (0, "支付成功"), REFUND (1, "转入退款"), NOTPAY (2, "未支付"), CLOSED (3, "已关闭"), REVOKED (4, "已撤销"),
//        USERPAYING (5, "用户支付中"), PAYERROR (6, "支付失败"), OTHER_ERROR (7, "其它错误"); (8, 未查询到订单号）

    }

    /**
     * 微信支付成功后回调的方法
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("handleWeixinPayResult")
    public String handleWeixinPayResult (RequestEntity reqEntity) throws Exception{
        String resultStr = TenPayConstant.FAIL;//静态
        SortedMap<String,String> requestMapData = (SortedMap<String,String>)reqEntity.getParam();
        String outTradeNo = requestMapData.get("out_trade_no");
        String appid = requestMapData.get("appid");
        String appName = MiscUtils.getAppNameByAppid(appid);
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String,Object> billMap = commonModuleServer.findTradebillByOutTradeNo(outTradeNo);
        if(billMap != null && billMap.get("status").equals("2")){
            logger.debug("====>　已经处理完成, 不需要继续。流水号是: " + outTradeNo);
            return TenPayConstant.SUCCESS;
        }
 
       if (TenPayUtils.isValidSign(requestMapData,appName)){// MD5签名成功，处理课程打赏\购买课程等相关业务
        //if(true){
            logger.debug(" ===> 微信notify Md5 验签成功 <=== ");
 
            if("SUCCESS".equals(requestMapData.get("return_code")) &&
                    "SUCCESS".equals(requestMapData.get("result_code"))){
                String userId = billMap.get("user_id").toString();

                //0.先检测课程存在情况和状态
                String courseId = (String)billMap.get("course_id");       
                Map<String,Object> query = new HashMap<>();
                query.put("course_id", courseId);
                Map<String,String> courseMap = CacheUtils.readCourse(courseId, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, false);
                if(MiscUtils.isEmpty(courseMap)){
                	throw new QNLiveException("100004");
                }
                String courseKey  = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, query);


                String profit_type = (String)billMap.get("profit_type");
                //1.1如果为打赏，则先查询该用户是否打赏了该课程
                Map<String,Object> rewardMap = null;
                if("1".equals(profit_type)){
                    Map<String,Object> rewardQueryMap = new HashMap<>();
                    rewardQueryMap.put("course_id", courseId);
                    rewardQueryMap.put("user_id", userId);
                    rewardMap = commonModuleServer.findRewardByUserIdAndCourseId(rewardQueryMap);
                }

                //2.为插入数据库准备相关数据，处理数据库中相关部分
				Map<String,Object> requestValues = new HashMap<>();
				for(String key : requestMapData.keySet()){
					requestValues.put(key, requestMapData.get(key));
				}
				requestValues.put("courseInCache", courseMap);
				requestValues.put("tradeBillInCache", billMap);
				Map<String,String> distributeRoom = null;

                Map<String,Object> userDistributionInfo = null; 
                Map<String,Object> recommendMap = null;
				if("0".equals(profit_type)){ //profit_type 0 课程收益 1打赏收益
					//t_room_distributer
					query.clear();
					query.put("room_id", billMap.get("room_id"));
					query.put("user_id", billMap.get("user_id"));
					query.put("today_end_date", MiscUtils.getEndDateOfToday());
					recommendMap = commonModuleServer.findRoomDistributerRecommendAllInfo(query);

					if(!MiscUtils.isEmpty(recommendMap)){						
						distributeRoom = CacheUtils.readDistributerRoom((String)recommendMap.get(Constants.CACHED_KEY_DISTRIBUTER_FIELD),
								(String)billMap.get("room_id"), readRoomDistributer, jedis);
						requestValues.put("roomDistributerCache", distributeRoom);

                        //根据分销员id、用户id、rqCode、room_id、消费类型为购买，查询数据库
                        Map<String,Object> queryuserDistribution = new HashMap<>();
                        queryuserDistribution.put("distributer_id", distributeRoom.get("distributer_id"));
                        queryuserDistribution.put("user_id", userId);
                        queryuserDistribution.put("room_id", courseMap.get("room_id"));
                        queryuserDistribution.put("rq_code", recommendMap.get("rq_code"));
                        queryuserDistribution.put("profit_type", billMap.get("profit_type"));
                        userDistributionInfo = commonModuleServer.findUserDistributionInfo(queryuserDistribution);
					}
				}
				Map<String,Object> handleResultMap = commonModuleServer.handleWeixinPayResult(requestValues);
				
				query.clear();
		        query.put(Constants.CACHED_KEY_USER_FIELD, userId);		        
				String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);
				jedis.del(key);
                //3.处理缓存中的数据
                //3.4如果是购买课程，且存在分销行为，则进行相关缓存处理
                long lecturerProfit = 0L;
                Map<String,Object> sumInfo = null;
                if("0".equals(profit_type) && !MiscUtils.isEmpty(distributeRoom)){
                	query.clear();
                	String distributer_id = distributeRoom.get("distributer_id");
                	query.put("distributer_id", distributer_id);                	
                	query.put("profit_type", "0");
                	sumInfo = commonModuleServer.findCoursesSumInfo(query);
                	//3.4.1 分销员缓存t_distributer                	
                	query.clear();
                    query.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, distributeRoom.get("distributer_id"));
                    String distributerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_DISTRIBUTER, query);
                    jedis.hset(distributerKey, "total_amount", MiscUtils.convertObjectToLong(sumInfo.get("share_amount"))+"");
                    jedis.sadd(Constants.CACHED_UPDATE_DISTRIBUTER_KEY, distributeRoom.get("distributer_id"));
                	
                    //3.4.2 直播间分销员 更新t_room_distributer缓存
                    query.put("distributer_id", distributeRoom.get("distributer_id"));
                    query.put("room_id", courseMap.get("room_id"));
                    String roomDistributeKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, query);

                    if(courseMap.get("student_num") != null){
                        long student_num_original = Long.parseLong(courseMap.get("student_num"));
                        if(student_num_original <= 0){
                            jedis.hincrBy(roomDistributeKey, "course_num", 1);
                            jedis.hincrBy(roomDistributeKey, "last_course_num", 1);
                        }
                    }                	
                    jedis.hincrBy(roomDistributeKey, "done_num", 1);
                    jedis.hincrBy(roomDistributeKey, "last_done_num", 1);
                    query.put("room_id", courseMap.get("room_id")); 
                    sumInfo = commonModuleServer.findCoursesSumInfo(query);
                    jedis.hset(roomDistributeKey, "total_amount", MiscUtils.convertObjectToLong(sumInfo.get("share_amount"))+"");
                    query.put("rq_code", recommendMap.get("rq_code"));
                    sumInfo = commonModuleServer.findCoursesSumInfo(query);
                    jedis.hset(roomDistributeKey, "last_total_amount", MiscUtils.convertObjectToLong(sumInfo.get("share_amount"))+"");
                    jedis.sadd(Constants.CACHED_UPDATE_DISTRIBUTER_KEY, distributeRoom.get("distributer_id"));
                	/*
                    long share_amount = 0L;
                    if(handleResultMap.containsKey("share_amount")){
                        share_amount = (Long)handleResultMap.get("share_amount");
                    }
                    lecturerProfit = (Long)handleResultMap.get("profit_amount") - share_amount;
                    */
                    query.clear();
                    query.put("distributer_id", distributeRoom.get("distributer_id"));
                    Date date = MiscUtils.getEndDateOfToday();
                    query.put("create_date", date);
                    sumInfo = commonModuleServer.findCoursesSumInfo(query);
                    query.clear();
                    query.put(Constants.CACHED_KEY_USER_FIELD, distributeRoom.get("distributer_id"));
                    String userCacheKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query);
                    jedis.hset(userCacheKey, "today_distributer_amount", MiscUtils.convertObjectToLong(sumInfo.get("share_amount"))+"");
                    jedis.sadd(Constants.CACHED_UPDATE_USER_KEY, distributeRoom.get("distributer_id"));
                }else {
                    lecturerProfit = (Long)handleResultMap.get("profit_amount");
                }

                //3.1 t_lecturer  缓存
                query.clear();
                query.put(Constants.CACHED_KEY_LECTURER_FIELD, courseMap.get("lecturer_id"));
                String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query);
                if("0".equals(profit_type)){
                    jedis.hincrBy(lecturerKey,"total_student_num",1);
                    jedis.hincrBy(lecturerKey,"pay_student_num",1);
                    jedis.hincrBy(lecturerKey,"room_done_num",1);
                    if(!MiscUtils.isEmpty(distributeRoom)){
                    	jedis.hincrBy(lecturerKey,"room_distributer_done_num",1);
                    }
                }
                sumInfo = commonModuleServer.findCoursesSumInfo(query);
                jedis.hset(lecturerKey, "total_amount", MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit"))+"");
                jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, courseMap.get("lecturer_id"));

                //3.2 直播间缓存 t_live_room
                query.clear();
                query.put(Constants.FIELD_ROOM_ID, handleResultMap.get("room_id").toString());
                String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, query);
                sumInfo = commonModuleServer.findCoursesSumInfo(query);
                jedis.hset(liveRoomKey, "total_amount", MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit"))+"");

                //3.3 课程缓存或者表 t_courses
                if("0".equals(profit_type)){
                    //jedis.hincrBy(courseKey, "student_num", 1);
                	query.clear();
                	query.put("course_id", courseId);
                	Map<String,Object> numInfo = commonModuleServer.findCourseRecommendUserNum(query);
                	long num = 0;
                	if(!MiscUtils.isEmpty(numInfo)){
                		num=MiscUtils.convertObjectToLong(numInfo.get("recommend_num"));
                	}
                	jedis.hset(courseKey, "student_num", num+"");
/*                	long lastNum = MiscUtils.convertObjectToLong(jedis.hget(courseKey, "student_num"));
                	if(lastNum<num){
                		jedis.hset(courseKey, "student_num", num+"");
                	}*/
                    query.clear();
                    query.put("course_id", courseId);
                    query.put("profit_type", "0");
                    sumInfo = commonModuleServer.findCoursesSumInfo(query);
                    jedis.hset(courseKey, "course_amount", MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit"))+"");
                }else {
                    //如果之前并没有打赏该课程，则计入
                    if(MiscUtils.isEmpty(rewardMap)){
                        jedis.hincrBy(courseKey, "extra_num", 1);
                    }
                    query.clear();
                    query.put("course_id", courseId);
                    query.put("profit_type", "1");
                    sumInfo = commonModuleServer.findCoursesSumInfo(query);
                    jedis.hset(courseKey,  "extra_amount", MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit"))+"");
                }

                query.clear();
                String lecturerId = courseMap.get("lecturer_id");
                query.put("lecturer_id", lecturerId);
                Map<String,String> lecturerMap = CacheUtils.readLecturer(lecturerId, this.generateRequestEntity(null, null, null, query), readLecturerOperation, jedis);

                if(profit_type.equals("1")){
                    String mGroupId = courseMap.get("im_course_id").toString();
                    query.clear();
                    query.put("user_id", handleResultMap.get("user_id"));        
                    Map<String, String> payUserMap = CacheUtils.readUserNoCache((String)handleResultMap.get("user_id"), this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);
                    
            //        String message = payUserMap.get("nick_name") + "打赏了" + MiscUtils.RecoveryEmoji(lecturerMap.get("nick_name")) +" "+  (Long)handleResultMap.get("profit_amount")/100.0 + "元";
                    HashMap<String,String> payMessageMap = new HashMap<>();
                    payMessageMap.put("pay_user",payUserMap.get("nick_name"));//打赏人名字
                    payMessageMap.put("pay_message", MiscUtils.getConfigByKey("pay_message",appName));//打赏信息
                    payMessageMap.put("collect_user",lecturerMap.get("nick_name"));//被打赏人的名字
                    payMessageMap.put("money_num", ((Long)handleResultMap.get("profit_amount")/100.0)+"");//钱
                    payMessageMap.put("money_unit",MiscUtils.getConfigByKey("money_unit",appName));//金币单元
                    String message = JSON.toJSONString(payMessageMap);
                    long currentTime = System.currentTimeMillis();
                    String sender = "system";
                    Map<String,Object> infomation = new HashMap<>();
                    infomation.put("course_id", handleResultMap.get("course_id"));//课程id
					infomation.put("creator_id", (String)handleResultMap.get("user_id"));//消息发送人id
                    //TODO check it , infomation.put("creator_id", lecturerMap.get(handleResultMap.get("lecturer_id").toString()));
                    infomation.put("message", message);//消息
                    infomation.put("send_type", "4");//4.打赏信息
                    infomation.put("message_type", "1");//1.文字
                    infomation.put("create_time", currentTime);//创建时间
                    infomation.put("message_id",MiscUtils.getUUId());//
                    infomation.put("message_imid",infomation.get("message_id"));
                    Map<String,Object> messageMap = new HashMap<>();
                    messageMap.put("msg_type","1");
                    messageMap.put("app_name",appName);
                    messageMap.put("send_time",currentTime);
                    messageMap.put("information",infomation);
                    messageMap.put("mid",infomation.get("message_id"));
                    String content = JSON.toJSONString(messageMap);
                    IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);
 
                }else if(profit_type.equals("0")){
                    Long nowStudentNum = 0L;

                    //修改用户缓存信息中的加入课程数
                    query.clear();
                    query.put(Constants.CACHED_KEY_USER_FIELD, userId);
                    String userCacheKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query);
                    if(jedis.exists(userCacheKey)){
                        jedis.hincrBy(userCacheKey, "course_num", 1L);
                    }else {
                        CacheUtils.readUser(userId, reqEntity, readUserOperation,jedis);
                        jedis.hincrBy(userCacheKey, "course_num", 1L);
                    }
                    jedis.sadd(Constants.CACHED_UPDATE_USER_KEY, userId);
                    nowStudentNum = Long.parseLong(courseMap.get("student_num")) + 1;
                    String levelString = MiscUtils.getConfigByKey("jpush_course_students_arrive_level",appName);
                    JSONArray levelJson = JSON.parseArray(levelString);
                    if(levelJson.contains(nowStudentNum+"")){
                        JSONObject obj = new JSONObject();
                        String course_type = courseMap.get("course_type");
                        String course_type_content = MiscUtils.convertCourseTypeToContent(course_type);
                        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content",appName), course_type_content, MiscUtils.RecoveryEmoji(courseMap.get("course_title")), nowStudentNum+""));
                        obj.put("to", courseMap.get("lecturer_id"));
                        obj.put("msg_type","7");
                        Map<String,String> extrasMap = new HashMap<>();
                        extrasMap.put("msg_type","7");
                        extrasMap.put("course_id",courseMap.get("course_id"));
                        obj.put("extras_map", extrasMap);
                        JPushHelper.push(obj,appName);
                    }
                }
 
                resultStr = TenPayConstant.SUCCESS;
                logger.debug("====> 微信支付流水: " + outTradeNo + " 更新成功, return success === ");
 
                String user_id = (String) billMap.get("user_id");
                String course_id = (String) billMap.get("course_id");
                String room_id = (String) billMap.get("room_id");

                if (!StringUtils.isBlank(user_id)) {
                    Map<String, Object> loginInfoUser = commonModuleServer.findLoginInfoByUserId(user_id);
                    String openId=(String) loginInfoUser.get("web_openid");
                    //  成员报名付费通知 老师 暂时 不需要了
                    //wpushLecture(billMap, jedis, openId, courseByCourseId, user);
                    //成功购买课程则通知学生
                    if(profit_type.equals("0")){
                        wpushUser(jedis, openId, courseMap, lecturerMap,course_id, room_id,appName);
                    }
                }
            }else{
                logger.debug("==> 微信支付失败 ,流水 ：" + outTradeNo);
                resultStr = TenPayConstant.FAIL;
            }
 
        }else {// MD5签名失败
            logger.debug("==> fail -Md5 failed");
            resultStr = TenPayConstant.FAIL;
        }
        return resultStr;
    }
    //</editor-fold>

    //<editor-fold desc="姜军 购买成功后推送给学生的通知">

//    private void jpushUser(Map<String,String> courseInfo, String user_id, String msg_type) {
//        JSONObject obj = new JSONObject();
//        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content"), course_type_content, MiscUtils.RecoveryEmoji(courseMap.get("course_title")), nowStudentNum+""));
//        obj.put("to", user_id);
//        obj.put("msg_type","14");
//        Map<String,String> extrasMap = new HashMap<>();
//        extrasMap.put("msg_type","14");
//        extrasMap.put("course_id",courseInfo.get("course_id"));
//        obj.put("extras_map", extrasMap);
//        JPushHelper.push(obj);
//    }
    //</editor-fold>
 
    private void wpushUser(Jedis jedis, String openId,
            Map<String, String> courseByCourseId,
            Map<String, String> lecturerUser,String courseId,String roomId,String appName) {
        Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();


        TemplateData first = new TemplateData();
        first.setColor(Constants.WE_CHAT_PUSH_COLOR);
        String firstContent = String.format(MiscUtils.getConfigByKey("wpush_shop_course_first",appName), MiscUtils.RecoveryEmoji(courseByCourseId.get("course_title")));
        first.setValue(firstContent);
        templateMap.put("first", first);

        TemplateData courseTitle = new TemplateData();
        courseTitle.setColor(Constants.WE_CHAT_PUSH_COLOR);
        courseTitle.setValue(MiscUtils.RecoveryEmoji(courseByCourseId.get("course_title")));
        templateMap.put("keyword1", courseTitle);

        Date start_time = new Date(Long.parseLong(courseByCourseId.get("start_time")));
        TemplateData orderNo = new TemplateData();
        orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
        orderNo.setValue(MiscUtils.parseDateToFotmatString(start_time, "yyyy-MM-dd HH:mm:ss"));
        templateMap.put("keyword2", orderNo);

        String lastContent;
        lastContent = MiscUtils.getConfigByKey("wpush_shop_course_lecturer_name",appName) + MiscUtils.RecoveryEmoji(lecturerUser.get("nick_name"));
//        String thirdContent = MiscUtils.RecoveryEmoji(courseByCourseId.get("course_remark"));
//        if(! MiscUtils.isEmpty(thirdContent)){
//            lastContent += "\n" + thirdContent;
//        }
        lastContent += "\n" +MiscUtils.getConfigByKey("wpush_shop_course_remark",appName);

        TemplateData remark = new TemplateData();
        remark.setColor(Constants.WE_CHAT_PUSH_COLOR);
        remark.setValue(lastContent);
        templateMap.put("remark", remark);
        String url = String.format(MiscUtils.getConfigByKey("course_live_room_url",appName), courseId, roomId);
        WeiXinUtil.send_template_message(openId, MiscUtils.getConfigByKey("wpush_shop_course",appName),url, templateMap, jedis,appName);
    }
 

    
    @FunctionName("commonDistribution")
    public Map<String,Object> getCommonDistribution(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Long position = (Long)reqMap.get("position");
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("distributer_id", userId);
        int pageCount = (Integer)reqMap.get("page_count");

        Map<String,String> distributer = CacheUtils.readDistributer(userId, reqEntity, readDistributerOperation, jedis, true);
        if(MiscUtils.isEmpty(distributer)){
            throw new QNLiveException("120012");
        }
        Map<String,Object> resultMap = new HashMap<String, Object>();

        String cash_in_amount = "";
        String total_amount = distributer.get("total_amount");
        if(MiscUtils.isEmpty(total_amount)){ //判斷有沒有錢
            total_amount="0";
            cash_in_amount = "0";
        }else{//如果沒有錢
            cash_in_amount = String.valueOf(CountMoneyUtil.getCashInAmount(total_amount));//可提現的錢
        }
        resultMap.put("total_amount", total_amount);
        resultMap.put("cash_in_amount", cash_in_amount);
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("distributer_id", userId);
        if(position != null){
        	queryMap.put("position", position);
        }
        
        queryMap.put("page_count", pageCount);
        
        List<Map<String,Object>> list = commonModuleServer.findDistributionRoomDetailList(queryMap);
        
        if(MiscUtils.isEmpty(list)){
        	list = new LinkedList<Map<String,Object>>();
        } else {
        	final List<Map<String,Object>>  detailsList = list;        	     	
        	((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
				@Override
				public void batchOperation(Pipeline pipeline, Jedis jedis) {
					Map<String,Object> queryParam = new HashMap<String,Object>();
					Map<String,Response<String>> roomNameMap = new HashMap<String,Response<String>>();
					Map<String,Response<String>> paymentCourseMap = new HashMap<String,Response<String>>();
					
					Map<String,Response<Map<String,String>>> latestInfo = new HashMap<String,Response<Map<String,String>>>();
					Map<String,Response<String>> lecturerName = new HashMap<String,Response<String>>();
					Map<String,Response<String>> lecturerAvatar = new HashMap<String,Response<String>>();
										
					for(Map<String,Object> values: detailsList){
						String roomid = (String)values.get("room_id");
						String lecturerId = (String)values.get("lecturer_id");
						if(!roomNameMap.containsKey(roomid)){
							queryParam.put(Constants.FIELD_ROOM_ID, roomid);
							String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, queryParam);
							roomNameMap.put(roomid, pipeline.hget(roomKey, "room_name"));							
						}
						if(!lecturerName.containsKey(lecturerId)){
							queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
							String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, queryParam);
							lecturerName.put(lecturerId, pipeline.hget(lecturerKey, "nick_name"));
							lecturerAvatar.put(lecturerId, pipeline.hget(lecturerKey, "avatar_address"));
							paymentCourseMap.put(lecturerId, pipeline.hget(lecturerKey, "pay_course_num"));
						}
						
						queryParam.put("distributer_id", values.get("distributer_id"));
						queryParam.put(Constants.FIELD_ROOM_ID, roomid);							
						String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, queryParam);
						if(!latestInfo.containsKey(roomKey)){
							latestInfo.put(roomKey, pipeline.hgetAll(roomKey));
						}
					}
					pipeline.sync();
					for(Map<String,Object> values: detailsList){
						String roomid = (String)values.get("room_id");
						String lecturerId = (String)values.get("lecturer_id");
						Response<String> roomName = roomNameMap.get(roomid);
						if(roomName!=null){
							values.put("room_name", roomName.get());
						}
						
						queryParam.put("distributer_id", values.get("distributer_id"));
						queryParam.put(Constants.FIELD_ROOM_ID, roomid);							
						String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, queryParam);
						Response<Map<String,String>> response = latestInfo.get(roomKey);
						if(response !=null ){
							Map<String,String> curRoomInfo = response.get();
							if(curRoomInfo !=null && MiscUtils.isEqual(values.get("room_distributer_details_id"),curRoomInfo.get("room_distributer_details_id"))){
								values.put("recommend_num", curRoomInfo.get("last_recommend_num"));
								values.put("done_num", curRoomInfo.get("last_done_num"));
								values.put("total_amount", curRoomInfo.get("last_total_amount"));
	        				}
						}
						
						long courseNum = 0l;
						if(lecturerName.containsKey(lecturerId)){
							values.put("nick_name", lecturerName.get(lecturerId).get());
							values.put("avatar_address", lecturerAvatar.get(lecturerId).get());
							Response<String> responseStr = paymentCourseMap.get(lecturerId);
							if(responseStr != null){
								courseNum = MiscUtils.convertObjectToLong(responseStr.get());
							}
						}
						values.put("course_num", courseNum);
					}
				}
        	});        	
        }
        resultMap.put("room_list",list);
        return resultMap;
    }

    /**
     * 讲师查询分销员的推广用户/用户查询自己的推广用户
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("roomDistributerRecommendInfo")
    public Map<String,Object> getRoomDistributerRecommendInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        Map<String,Object> resultMap = new HashMap<>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        String distributer_id = (String)reqMap.get("distributer_id");
        String rqCode = (String)reqMap.get("rq_code");
        if(MiscUtils.isEmpty(distributer_id)){
            distributer_id = userId;
        }
        
        reqMap.put("distributer_id",distributer_id);
        Map<String,String> roomDistributerMap = null;
        Map<String,String> currentRoomDistributerMap = CacheUtils.readDistributerRoom(distributer_id, room_id, readRoomDistributerOperation, jedis);
        if(MiscUtils.isEmpty(rqCode)){
        	roomDistributerMap = currentRoomDistributerMap; 
        	reqMap.remove("rq_code");
        } else {
            RequestEntity queryOperation = this.generateRequestEntity(null, null, Constants.FUNCTION_DISTRIBUTERS_ROOM_RQ, reqMap);
            roomDistributerMap = CacheUtils.readRoomDistributerDetails(room_id, userId, rqCode, queryOperation, readDistributerOperation, jedis);
            if(roomDistributerMap!= null && currentRoomDistributerMap != null 
            		&& MiscUtils.isEqual(currentRoomDistributerMap.get("rq_code"), roomDistributerMap.get("rq_code"))){
            	roomDistributerMap.put("recommend_num", currentRoomDistributerMap.get("last_recommend_num"));
            }
        }
        if(MiscUtils.isEmpty(roomDistributerMap)){
            throw new QNLiveException("100028");
        }

        //2.查询直播间分销员的推荐用户数量
        long roomDistributerRecommendNum = 0l;
        if(!MiscUtils.isEmpty(roomDistributerMap)){
        	roomDistributerRecommendNum = Long.parseLong(roomDistributerMap.get("recommend_num"));
        }
        
        resultMap.put("recommend_num", roomDistributerRecommendNum);

        //3.如果推荐用户数量大于0，则查询列表
        if(roomDistributerRecommendNum > 0){
            if(MiscUtils.isEmpty(reqMap.get("position"))){
                reqMap.remove("position");
            }            
            List<Map<String,Object>> recommendUserList = null;
            if(MiscUtils.isEmpty(rqCode)){
            	recommendUserList = commonModuleServer.findRoomRecommendUserList(reqMap);
            } else {
            	recommendUserList = commonModuleServer.findRoomRecommendUserListByCode(reqMap);
            }
            

            for(Map<String,Object> map : recommendUserList){
                if(map.get("end_date") == null){
                    map.put("status", 0);
                }else {
                    Date endDate = (Date)map.get("end_date");
                    Date todayEndDate = MiscUtils.getEndDateOfToday();
                    if(endDate.getTime() >= todayEndDate.getTime()){
                        map.put("status", 0);
                    }else {
                        map.put("status", 1);
                    }
                }
            }
            resultMap.put("recommend_list", recommendUserList);
        }

        return resultMap;
    }

    /*
    	 * 用户查询自己对应指定的直播间的分销信息
     */
    @FunctionName("roomDistributionInfo")
    public Map<String,Object> getRoomDistributionInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        String rq_code = (String)reqMap.get("rq_code");
   		String distributer_id = (String)reqMap.get("distributer_id");
        if(MiscUtils.isEmpty(distributer_id)){
        	reqMap.put("distributer_id", userId);
        } else {
        	reqMap.put("distributer_id", distributer_id);
        }        Map<String,String> distributer = CacheUtils.readDistributer(userId, reqEntity, readDistributerOperation, jedis, true);
        if(MiscUtils.isEmpty(distributer)){
            throw new QNLiveException("120012");
        }         
        Map<String,Object> result = new HashMap<String,Object>();
        RequestEntity queryOperation = this.generateRequestEntity(null, null, Constants.FUNCTION_DISTRIBUTERS_ROOM_RQ, reqMap);
        Map<String,String> totalInfo = CacheUtils.readRoomDistributerDetails(room_id, userId, rq_code, queryOperation, readDistributerOperation, jedis);
        if(MiscUtils.isEmpty(totalInfo)){
        	result.put("total_amount", 0l);
        	result.put("course_list", new LinkedList<Map<String,Object>>());
        } else {			
			String total_amount = totalInfo.get("total_amount");			
			Map<String,Object> queryParam = new HashMap<String,Object>();
			queryParam.put("distributer_id", totalInfo.get("distributer_id"));
			queryParam.put(Constants.FIELD_ROOM_ID, room_id);							
			String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, queryParam);
			String crtRqCode = jedis.hget(roomKey, "rq_code");
			if(rq_code.equals(crtRqCode)){
				total_amount=jedis.hget(roomKey, "last_total_amount");				
			}
			result.put("total_amount",MiscUtils.convertObjectToLong(total_amount));
			List<Map<String,Object>> course_list = commonModuleServer.findCourseWithRoomDistributerCourseInfo(reqMap);
			if(!MiscUtils.isEmpty(course_list)){
				Date currentDate=MiscUtils.getEndDateOfToday();
				for(Map<String,Object> values:course_list){
					values.put("profit_share_rate", totalInfo.get("profit_share_rate"));
					values.put("effective_time", totalInfo.get("effective_time"));
					values.put("end_date", totalInfo.get("end_date"));
					Date end_Date = null;
					if(!MiscUtils.isEmpty(values.get("end_date"))){
						end_Date = new Date(MiscUtils.convertObjectToLong(values.get("end_date")));
					}					
					if(!MiscUtils.isEmpty(end_Date) && end_Date.before(currentDate)){
						values.put("effective_time", null);
					}
				}
			} else {
				course_list = new LinkedList<Map<String,Object>>(); 
			}
			result.put("course_list", course_list);
        }
        
        return result;
    }
    
    @FunctionName("courseDistributionInfo")
    public Map<String,Object> getCourseDistributionInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String course_id = (String)reqMap.get("course_id");
        String rqCode = (String)reqMap.get("rq_code");
        Long position = (Long)reqMap.get("position");
        
        Map<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("course_id", course_id);
        parameters.put("distributer_id", userId); 
        parameters.put("rq_code", rqCode);
        List<Map<String,Object>> course_list = commonModuleServer.findRoomDistributerCourseInfo(parameters);
        if(MiscUtils.isEmpty(course_list)){
            throw new QNLiveException("120013");
        }
        Map<String,Object> result = course_list.get(0);
        
        parameters.put("position", position);
        parameters.put("page_count", reqMap.get("page_count"));
        
        List<Map<String,Object>> list = commonModuleServer.findRoomDistributerCourseDetailsInfo(parameters);
        result.put("profit_list", list);
        return result;
    }
    
    @FunctionName("roomDistributionShareInfo")
    public Map<String,Object> getRoomDistributionShareInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("distributer_id", userId);
        List<Map<String,Object>> list = commonModuleServer.findDistributionInfoByDistributerId(reqMap);
        if(MiscUtils.isEmpty(list)){
            throw new QNLiveException("120021");
        }

        Map<String,Object> map = list.get(0);
        if(map.get("end_date") != null){
            Date end_date = (Date)map.get("end_date");
            Date todayEndDay = MiscUtils.getEndDateOfToday();
            if(end_date.getTime() < todayEndDay.getTime()){
                throw new QNLiveException("120021");
            }
        }

        String roomId = map.get("room_id").toString();
        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(roomId,reqEntity,readLiveRoomOperation,jedis,true);
        map.put("avatar_address",liveRoomMap.get("avatar_address"));

        return map;
    }
    
    @FunctionName("updateUserInfo")
    public Map<String,Object> updateUserInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        reqMap.put("user_id", userId);
        Map<String,String> values = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
        String update_time_str = values.get("update_time");
        Long update_time = (Long)reqMap.get("update_time");
        if(!update_time.toString().equals(update_time_str)){
            throw new QNLiveException("000104");
        }
        String nick_name = (String)reqMap.get("nick_name");
        String avatar_address = (String)reqMap.get("avatar_address");
        if(!MiscUtils.isEmpty(nick_name) || !MiscUtils.isEmpty(avatar_address)){
            Map<String,Object> parameters = new HashMap<String,Object>();
            if(nick_name != null){
                parameters.put("nick_name", nick_name);
            }
            if(avatar_address != null){
                parameters.put("avatar_address", avatar_address);
            }
            if(!parameters.isEmpty()){
                Date curDate = new Date(System.currentTimeMillis());
                parameters.put("update_time", curDate);
                parameters.put("last_update_Time", new Date(update_time));
                parameters.put("user_id", userId);
                int count = commonModuleServer.updateUser(parameters);
                if(count <1){
                    throw new QNLiveException("000104");
                } else {
                    parameters.remove("last_update_Time");
                    Map<String,Object> parameter = new HashMap<String,Object>();
                    parameter.put(Constants.CACHED_KEY_USER_FIELD, userId);
                    String cachedKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, parameter);
                    Map<String, String> cachedValues = new HashMap<String,String>();
                    MiscUtils.converObjectMapToStringMap(parameters, cachedValues);
                    jedis.hmset(cachedKey, cachedValues);
                    parameter.clear();
                    parameter.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
                    cachedKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, parameter);
                    if(jedis.exists(cachedKey)){
                    	Map<String,String> updateCacedValue = new HashMap<String,String>();
                    	if(nick_name != null){
                    		updateCacedValue.put("nick_name", nick_name);
                    	}
                    	if(avatar_address != null){
                    		updateCacedValue.put("avatar_address", avatar_address);
                    	}
                    	if(!MiscUtils.isEmpty(updateCacedValue)){
                            jedis.hmset(cachedKey, updateCacedValue);
                    	}
                    }
                }
            }
        }
        return new HashMap<String,Object>();
    }
 
 
    @SuppressWarnings("unchecked")
    @FunctionName("getCourseInviteCard")
    public Map<String,Object> getCourseInviteCard (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String,Object> resultMap = new HashMap<>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String courseId = reqMap.get("course_id").toString();
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("user_id", userId);        

 
        Map<String,String> courseMap =  CacheUtils.readCourse(courseId, reqEntity, readCourseOperation, jedis, false);
        resultMap.put("course_title",MiscUtils.RecoveryEmoji(courseMap.get("course_title")));
        resultMap.put("start_time",courseMap.get("start_time"));


        Map<String, String> userMap = CacheUtils.readUser(courseMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);
        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(courseMap.get("room_id"),reqEntity,readLiveRoomOperation,jedis,true);


        resultMap.put("avatar_address",liveRoomMap.get("avatar_address"));
        resultMap.put("nick_name",MiscUtils.RecoveryEmoji(userMap.get("nick_name")));
        resultMap.put("share_url",getCourseShareURL(userId, courseId, courseMap,jedis,appName));
        if(reqMap.get("png").toString().equals("Y")){
            resultMap.put("png_url",this.CreateRqPage(courseId,null,null,null,null,reqEntity.getAccessToken(),reqEntity.getVersion(),jedis,appName));
        }
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
     @FunctionName("getRoomInviteCard")
     public Map<String,Object> getRoomInviteCard (RequestEntity reqEntity) throws Exception{//TODO
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("user_id", userId);
        String roomId = reqMap.get("room_id").toString();
        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(roomId,reqEntity,readLiveRoomOperation,jedis,true);
        resultMap.put("room_name",MiscUtils.RecoveryEmoji(liveRoomMap.get("room_name")));

        Map<String, String> userMap = CacheUtils.readUser(liveRoomMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);
        resultMap.put("avatar_address",liveRoomMap.get("avatar_address"));
        resultMap.put("nick_name",MiscUtils.RecoveryEmoji(userMap.get("nick_name")));

        //查询该用户是否为该直播间的分销员
        resultMap.put("share_url",getLiveRoomShareURL(userId, roomId,jedis,appName));

        long timeS1 = System.currentTimeMillis();
        logger.debug("-------------------------"+String.valueOf(timeS1));
        if(reqMap.get("png").toString().equals("Y")) {
            resultMap.put("png_url",this.CreateRqPage(null,roomId,null,null,null,reqEntity.getAccessToken(),reqEntity.getVersion(),jedis,appName));
        }
        long timeS2 = System.currentTimeMillis();
        logger.debug("-------------------------"+String.valueOf(timeS2));
        return resultMap;
    }
 
    private String getLiveRoomShareURL(String userId, String roomId,Jedis jedis,String appName) throws Exception{
        String share_url;
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("distributer_id", userId);
        queryMap.put("room_id", roomId);
        Map<String,String> distributerRoom = CacheUtils.readDistributerRoom(userId, roomId, readRoomDistributerOperation, jedis);

		boolean isDistributer = false;
		String recommend_code = null;
		if (! MiscUtils.isEmpty(distributerRoom)) {
			isDistributer = true;
            recommend_code = distributerRoom.get("rq_code");
		}

        //是分销员
        if(isDistributer == true){
            share_url = MiscUtils.getConfigByKey("live_room_share_url_pre_fix",appName)+roomId+"&recommend_code="+recommend_code;
        }else {
            //不是分销员
            share_url = MiscUtils.getConfigByKey("live_room_share_url_pre_fix",appName)+roomId;
        }
        return share_url;
    }

    private String getCourseShareURL(String userId, String courseId, Map<String,String> courseMap,Jedis jedis,String appName) throws Exception{
        String share_url ;
        String roomId = courseMap.get("room_id");
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("distributer_id", userId);
        queryMap.put("room_id", roomId);
        Map<String,String> distributerRoom = CacheUtils.readDistributerRoom(userId, roomId, readRoomDistributerOperation, jedis);

        boolean isDistributer = false;
        String recommend_code = null;
        if (! MiscUtils.isEmpty(distributerRoom)) {
            isDistributer = true;
            recommend_code = distributerRoom.get("rq_code");
        }

        //是分销员
        if(isDistributer == true){
            share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix",appName) + courseId + "&recommend_code=" + recommend_code;
        }else {  //不是分销员
            share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix",appName) + courseId;
        }
        return share_url;
    }


    @SuppressWarnings("unchecked")
    @FunctionName("distributorsRecommendUser")
    public Map<String,Object> distributersRecommendUser (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<>();
        Date now = new Date();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String rqCode = reqMap.get("recommend_code").toString();
        //0.读取直播间分销员信息
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        Map<String,Object> queryParam = new HashMap<String,Object>();
        queryParam.put(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE_FIELD, rqCode);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE, queryParam);        
        Map<String, String> info = jedis.hgetAll(key);
        
        Map<String, String> distributerRoom = null;
        boolean find=false;
        if(!MiscUtils.isEmpty(info)){
        	String distributer_id = (String)info.get("distributer_id");
        	String room_id = (String)info.get("room_id");
        	if(!MiscUtils.isEmpty(distributer_id) && !MiscUtils.isEmpty(room_id)){
            	find=true;
        		distributerRoom = CacheUtils.readDistributerRoom(distributer_id, room_id, readRoomDistributer, jedis);
        	}
        }
        if(!find){
        	Map<String,Object> roomDistributerMap = commonModuleServer.findRoomDistributerInfoByRqCode(rqCode);
        	distributerRoom = new HashMap<String, String>();
        	if(!MiscUtils.isEmpty(roomDistributerMap)){
        		MiscUtils.converObjectMapToStringMap(roomDistributerMap, distributerRoom);
        		Map<String,String> query = new HashMap<String,String>();
        		query.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD,(String)roomDistributerMap.get(Constants.CACHED_KEY_DISTRIBUTER_FIELD));
        		query.put(Constants.FIELD_ROOM_ID, (String)roomDistributerMap.get(Constants.FIELD_ROOM_ID));
        		//TODO RQ_CODE失效需要删除
        		jedis.hmset(key, query);
        		key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, queryParam);
        		jedis.hmset(key, distributerRoom);
        	}
        }
        
        if(!MiscUtils.isEmpty(distributerRoom)){
            boolean isValidate =false;
            Date todayEnd = MiscUtils.getEndDateOfToday();  
            long end_date = MiscUtils.convertObjectToLong(distributerRoom.get("end_date")); 
            if("0".equals(distributerRoom.get("effective_time")) && end_date == 0){
                isValidate = true;
            }else {                
                //1.判断该分销员是否有效，有效则进行下一步验证
                if(end_date >= todayEnd.getTime()){
                    isValidate = true;
                }
            }
            
            if(isValidate == true){
                String lecturerId = distributerRoom.get("lecturer_id");
                String distributer_id = distributerRoom.get("distributer_id");
                String room_id = (String)distributerRoom.get("room_id");
                Map<String,Object> query = new HashMap<String,Object>();
                query.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, distributer_id);
        		query.put(Constants.FIELD_ROOM_ID, room_id);
                String distributerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, query);
                		
                //2.如果访问该推广链接的用户不是讲师,不是分销员，则进行下一步验证，验证用户目前是否已经属于了有效分销员
                if((! userId.equals(lecturerId)) && (! userId.equals(distributer_id))){
                    Map<String,Object> queryMap = new HashMap<>();
                    queryMap.put("room_id", room_id);
                    queryMap.put("user_id", userId);
                    Map<String,Object> roomDistributerRecommendMap = commonModuleServer.findRoomDistributerRecommendItem(queryMap);
 
                    //3.如果该用户没有成为有效分销员的推荐用户，则该用户成为该分销员的推荐用户
                    if(MiscUtils.isEmpty(roomDistributerRecommendMap)){
                        Map<String,Object> insertMap = new HashMap<>();
                        insertMap.put("distributer_recommend_id", MiscUtils.getUUId());
                        insertMap.put("distributer_recommend_detail_id", MiscUtils.getUUId());
                        insertMap.put("distributer_id", distributerRoom.get("distributer_id"));
                        insertMap.put("room_id", room_id);
                        insertMap.put("user_id", userId);
                        insertMap.put("end_date", new Date(end_date));
                        insertMap.put("rq_code", rqCode);
                        insertMap.put("now", now);
                        commonModuleServer.insertRoomDistributerRecommend(insertMap);
 
                        //4.直播间分销员的推荐人数增加一                        
                        jedis.hincrBy(distributerKey, "click_num", 1);
                        jedis.hincrBy(distributerKey, "recommend_num", 1);
                        jedis.hincrBy(distributerKey, "last_recommend_num", 1);                        
                        jedis.sadd(Constants.CACHED_UPDATE_RQ_CODE_KEY, rqCode);

                        //5.修改讲师缓存中的推荐用户数
                        Map<String,Object> cacheKeyMap = new HashMap<>();
                        cacheKeyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, cacheKeyMap);
                        jedis.hincrBy(lecturerKey, "room_recommend_num", 1);

                    }else {
                    	if(!rqCode.equals(roomDistributerRecommendMap.get("rq_code"))){
                    		Map<String,Object> param = new HashMap<String,Object>();
                    		param.put("distributer_id", distributer_id);
                    		param.put("rq_code", roomDistributerRecommendMap.get("rq_code"));
                    		Map<String,Object> result = commonModuleServer.findDistributionRoomDetail(param);
                    		if(!MiscUtils.isEmpty(result)){
                    			roomDistributerRecommendMap.put("end_date",result.get("end_date"));                    			
                    		}
                    	} else {
                    		roomDistributerRecommendMap.put("end_date",distributerRoom.get("end_date"));
                    	}
                        //判断如果该推荐用户如果处于该直播间的无效分销状态，则需要重新成为推荐用户
                        if(roomDistributerRecommendMap.get("end_date") != null){
                            Date endDate = (Date) roomDistributerRecommendMap.get("end_date");
                            if(endDate.getTime() < todayEnd.getTime()){
                                Map<String,Object> insertMap = new HashMap<>();
                                insertMap.put("distributer_recommend_id", roomDistributerRecommendMap.get("distributer_recommend_id"));
                                insertMap.put("distributer_recommend_detail_id", MiscUtils.getUUId());
                                insertMap.put("distributer_id", distributerRoom.get("distributer_id"));
                                insertMap.put("room_id", room_id);
                                insertMap.put("user_id", userId);
                                insertMap.put("end_date", new Date(end_date));
                                insertMap.put("rq_code", rqCode);
                                insertMap.put("now", now);
                                insertMap.put("recommend_num", 0);
                                insertMap.put("done_num", 0);
                                insertMap.put("course_num", 0);
                                insertMap.put("old_end_date", roomDistributerRecommendMap.get("end_date"));
                                insertMap.put("old_recommend_num", roomDistributerRecommendMap.get("recommend_num"));
                                insertMap.put("old_done_num", roomDistributerRecommendMap.get("done_num"));
                                insertMap.put("old_course_num", roomDistributerRecommendMap.get("course_num"));
                                insertMap.put("old_rq_code", roomDistributerRecommendMap.get("rq_code"));

                                commonModuleServer.updateRoomDistributerRecommend(insertMap);

                                //4.直播间分销员的推荐人数增加一
                                jedis.hincrBy(distributerKey, "click_num", 1);
                                jedis.hincrBy(distributerKey, "recommend_num", 1);
                                jedis.hincrBy(distributerKey, "last_recommend_num", 1);
                                jedis.sadd(Constants.CACHED_UPDATE_RQ_CODE_KEY, rqCode);

                                //5.修改讲师缓存中的推荐用户数
                                Map<String,Object> cacheKeyMap = new HashMap<>();
                                cacheKeyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                                String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, cacheKeyMap);
                                jedis.hincrBy(lecturerKey, "room_recommend_num", 1);
                            }
                        }
                    }
                }
            }
        }
 
 
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("createFeedback")
    public Map<String,Object> createFeedback (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<>();
 
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        reqMap.put("feedback_id",MiscUtils.getUUId());
        reqMap.put("now",new Date());
        commonModuleServer.insertFeedback(reqMap);
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("convertWeixinResource")
    public Map<String,Object> convertWeixinResource (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Map<String,Object> resultMap = new HashMap<>();

//        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //1.将多媒体server_id通过微信接口，得到微信资源访问链接
        String mediaUrl = WeiXinUtil.getMediaURL(reqMap.get("media_id").toString(),jedisUtils.getJedis(appName),appName);
        //2.调用七牛fetch将微信资源访问链接转换为七牛图片链接
        String fetchURL = qiNiuFetchURL(mediaUrl,appName);
 
        resultMap.put("url", fetchURL);
        return resultMap;
    }
 
    private String qiNiuFetchURL(String mediaUrl,String appName) throws Exception{
        Configuration cfg = new Configuration(Zone.zone0());
        BucketManager bucketManager = new BucketManager(auth,cfg);
        String bucket = MiscUtils.getConfigByKey("image_space",appName);
        String key = Constants.WEB_FILE_PRE_FIX + MiscUtils.parseDateToFotmatString(new Date(),"yyyyMMddHH")+MiscUtils.getUUId();
        FetchRet result = bucketManager.fetch(mediaUrl, bucket,key);
        String imageUrl = MiscUtils.getConfigByKey("images_space_domain_name",appName) + "/"+key;
        return imageUrl;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("getShareInfo")
    public Map<String,Object> getShareInfo (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<>();
        String query_type = reqMap.get("query_type").toString();
        String id = reqMap.get("id").toString();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        String title = null;
        String content = null;
        String icon_url = null;
        String simple_content = null;
        String share_url = null;
        String png_url = null;

        //1.课程分享 2.直播间分享 3.其他页面分享 4.成为直播间分销员分享
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        switch (query_type){
            case "1":
                ((Map<String, Object>) reqEntity.getParam()).put("course_id",id);//如果type==1 那么出入的id就是course_id
                Map<String,String> courseMap = CacheUtils.readCourse(id, reqEntity, readCourseOperation, jedis, true);
                if(MiscUtils.isEmpty(courseMap)){
                    throw new QNLiveException("120009");
                }
                MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);
                title = courseMap.get("course_title");
                ((Map<String, Object>) reqEntity.getParam()).put("room_id",courseMap.get("room_id"));//把roomid 放进参数中 传到后面
                Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(courseMap.get("room_id"), reqEntity, readLiveRoomOperation, jedis, true);
                if("2".equals(courseMap.get("status"))){
                    content = liveRoomMap.get("room_name")+"\n";
                }else if("1".equals(courseMap.get("status"))){
                    Date courseStartTime = new Date(Long.parseLong(courseMap.get("start_time")));
                    if(MiscUtils.isEmpty(content)){
                        content  = liveRoomMap.get("room_name") + "\n"+ MiscUtils.getConfigByKey("weixin_course_share_time",appName) + MiscUtils.parseDateToFotmatString(courseStartTime, "yyyy年MM月dd日 HH:mm");
                    }else {
                        content += "\n" + liveRoomMap.get("room_name") + "\n" + MiscUtils.getConfigByKey("weixin_course_share_time",appName) + MiscUtils.parseDateToFotmatString(courseStartTime, "yyyy年MM月dd日 HH:mm");
                    }
                }else if("4".equals(courseMap.get("status"))){
                    content  = liveRoomMap.get("room_name") + "\n" + MiscUtils.getConfigByKey("weixin_course_share_content",appName);
                }
                icon_url = liveRoomMap.get("avatar_address");
                simple_content = courseMap.get("course_title");
                share_url = getCourseShareURL(userId, id, courseMap,jedis,appName);

                if(reqMap.get("png").toString().equals("Y"))
                    png_url = this.CreateRqPage(id,null,null,null,null,reqEntity.getAccessToken(),reqEntity.getVersion(),jedis,appName);
                break;
 
            case "2":
                ((Map<String, Object>) reqEntity.getParam()).put("room_id",id);//如果是2 那么就是直播间分享 把roomid存入
                Map<String,String> liveRoomInfoMap =  CacheUtils.readLiveRoom(id, reqEntity, readLiveRoomOperation, jedis, true);
                if(MiscUtils.isEmpty(liveRoomInfoMap)){
                    throw new QNLiveException("120018");
                }
                title = liveRoomInfoMap.get("room_name");
                content = liveRoomInfoMap.get("room_remark");
                icon_url = liveRoomInfoMap.get("avatar_address");
                share_url = getLiveRoomShareURL(userId, id,jedis,appName);
                simple_content = MiscUtils.getConfigByKey("weixin_live_room_simple_share_content",appName) + liveRoomInfoMap.get("room_name");
                if(reqMap.get("png").toString().equals("Y"))
                    png_url = this.CreateRqPage(null,id,null,null,null,reqEntity.getAccessToken(),reqEntity.getVersion(),jedis,appName);


                break;
 
            case "3":
                title = MiscUtils.getConfigByKey("weixin_other_page_share_title",appName);
                content = MiscUtils.getConfigByKey("weixin_other_page_share_content",appName);
                icon_url = MiscUtils.getConfigByKey("weixin_other_page_share_icon_url",appName);
                simple_content = MiscUtils.getConfigByKey("weixin_other_page_share_simple_content",appName);
                share_url = MiscUtils.getConfigByKey("other_share_url",appName);
                break;
 
            case "4":
                Map<String, Object> map = new HashMap<>();
                map.put(Constants.CACHED_KEY_USER_ROOM_SHARE_FIELD, reqMap.get("id"));
                String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOM_SHARE, map);
                Map<String, String> values = jedis.hgetAll(key);
 
                if(MiscUtils.isEmpty(values)){
                    throw new QNLiveException("120019");
                }
                title = MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_title",appName);
                Map<String,String> liveRoomInfo = CacheUtils.readLiveRoom(values.get("room_id"), reqEntity, readLiveRoomOperation, jedis, true);
                if(MiscUtils.isEmpty(liveRoomInfo)){
                    throw new QNLiveException("120018");
                }
                content = liveRoomInfo.get("room_name") + "\n"
                        + MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_second_content",appName).replace("%s", (Integer.parseInt(values.get("profit_share_rate")) / 100.0)+"") + "\n"
                        + MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_third_content",appName);
                icon_url = liveRoomInfo.get("avatar_address");
                share_url = String.format(MiscUtils.getConfigByKey("be_distributer_url_pre_fix",appName), reqMap.get("id"), liveRoomInfo.get("room_id"), (Integer.parseInt(values.get("profit_share_rate")) / 100.0), values.get("effective_time"));
                if(reqMap.get("png").toString().equals("Y"))
                    png_url = this.CreateRqPage(null,
                            liveRoomInfo.get("room_id"),
                            reqMap.get("id").toString(),
                            (Integer.parseInt(values.get("profit_share_rate")) / 100.0),
                            Integer.valueOf(values.get("effective_time")),
                            reqEntity.getAccessToken(),
                            reqEntity.getVersion(),
                            jedis,
                            appName);
                break;
        }
 
        resultMap.put("title",MiscUtils.RecoveryEmoji(title));
        resultMap.put("content",content);
        resultMap.put("icon_url",icon_url);
        resultMap.put("simple_content",simple_content);
        resultMap.put("share_url",share_url);
        if(reqMap.get("png").toString().equals("Y"))
            resultMap.put("png_url",png_url);

        return resultMap;
    }
 
    private String encryptIMAccount(String mid, String mpwd){
        String name='\0'+mid+'\0'+mpwd;
        String keys= "l*c%@)c5";
        String res = DES.encryptDES(name, keys);
        return res;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("getCourseRecommendUsers")
    public Map<String,Object> getCourseRecommendUsers (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //1.检测课程是否存在
        Map<String,String> courseMap = CacheUtils.readCourse(reqMap.get("course_id").toString(), reqEntity, readCourseOperation, jedis, false);
        if(MiscUtils.isEmpty(courseMap)){
            throw new QNLiveException("120009");
        }

        //2.查询数据库得到相关数据
        if(MiscUtils.isEmpty(reqMap.get("distributer_id"))){
        	reqMap.put("distributer_id", userId);
        }        
        reqMap.put("current_date", MiscUtils.getEndDateOfToday());
        if(MiscUtils.isEmpty(reqMap.get("student_pos"))){
            reqMap.remove("student_pos");
        }
        String roomId = (String)reqMap.get("room_id");
        List<Map<String,Object>> recommendUsers = commonModuleServer.findcourseRecommendUsers(reqMap);
        Map<String,Object> recommendUserNum = commonModuleServer.findCourseRecommendUserNum(reqMap);
        long todayDateEndTime = MiscUtils.getEndDateOfToday().getTime();
        if(!MiscUtils.isEmpty(recommendUsers)){

        	((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
				@Override
				public void batchOperation(Pipeline pipeline, Jedis jedis) {
					Map<String,Response<Map<String,String>>> response = new HashMap<String,Response<Map<String,String>>>();
					Map<String,Object> query = new HashMap<String,Object>();
		            for(Map<String,Object> recommendUser : recommendUsers){
		            	String distributerId = (String)recommendUser.get("distributer_id");
		            	if(response.containsKey(distributerId)){
		            		continue;
		            	}
		            	query.clear();
		            	query.put("room_id", roomId);
		            	query.put("distributer_id", distributerId);
		            	String roomDistributeKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, query);
		            	response.put(distributerId, pipeline.hgetAll(roomDistributeKey));
		            }
		            pipeline.sync();
		            for(Map<String,Object> recommendUser : recommendUsers){
		            	String distributerId = (String)recommendUser.get("distributer_id");
		            	Response<Map<String,String>> values = response.get(distributerId);
		            	if(values!=null && !MiscUtils.isEmpty(values.get())){
		            		Map<String,String> roomInfo = values.get();
		            		String rqCode = (String)recommendUser.get("rq_code");
		            		if(MiscUtils.isEmpty(rqCode)){
		            			recommendUser.put("status", 0);
		            		} else if(!rqCode.equals(roomInfo.get("rq_code"))) {
		            			recommendUser.put("status", 2);
		            		} else {
		            			long endDateTime = MiscUtils.convertObjectToLong(roomInfo.get("end_date"));
			                    if(endDateTime == 0 || endDateTime >= todayDateEndTime){
			                        recommendUser.put("status", 0);
			                    }else {
			                        recommendUser.put("status", 2);
			                    }
		            		}
		            	} else {
		            		recommendUser.put("status", 0);
		            	}
		            }
				}
        	});
        }
        resultMap.put("student_list", recommendUsers);
        if((! MiscUtils.isEmpty(recommendUserNum)) && recommendUserNum.get("recommend_num") != null){
            resultMap.put("recommend_num", recommendUserNum.get("recommend_num"));
        }else {
            resultMap.put("recommend_num", 0);
        }

        return resultMap;
    }

    /**
     * 生成二维码
     * @param course_id 课程
     * @param room_id 直播间
     * @param access_token 需要检验
     * @param room_share_code
     * @param effective_time 月份
     * @param profit_share_rate 分销比例
     * @param version 版本
     * @return 返回流信息
     * @throws Exception
     */
    public String CreateRqPage(String course_id,String room_id,String room_share_code,Double profit_share_rate,Integer effective_time,String access_token, String version,Jedis jedis,String appName)throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(access_token);//用安全证书拿userId
        RequestEntity reqEntity = new RequestEntity();
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("user_id",userId);//
        Map<String, String> userMap = CacheUtils.readUser(userId, this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);
        String user_head_portrait = userMap.get("avatar_address");//用户头像
        String userName = userMap.get("nick_name");//用户姓名
        BufferedImage png = null;//返回的图片


        if(room_share_code!=null){//如果是分销链接
            String share_url = String.format(MiscUtils.getConfigByKey("be_distributer_url_pre_fix",appName),room_share_code,room_id,profit_share_rate,effective_time);
            png = ZXingUtil.createRoomDistributerPng(user_head_portrait,userName,share_url,profit_share_rate,appName);//生成图片

        }else if(room_id != null){ //判断分享直播间

            Map<String, String> liveRoomMap = CacheUtils.readLiveRoom(room_id,reqEntity,readLiveRoomOperation,jedis,true);//根据直播间id获取数据
            query.put("user_id",liveRoomMap.get("lecturer_id"));
            Map<String, String> lecturerMap = CacheUtils.readUser(liveRoomMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);
            String share_url = getLiveRoomShareURL(userId, room_id,jedis,appName);
            String lecturerName = lecturerMap.get("nick_name");
            png = ZXingUtil.createLivePng(user_head_portrait,userName,lecturerName,share_url,appName);//生成图片

        }else if(course_id != null){//分享课程
            Map<String,String> courseMap =  CacheUtils.readCourse(course_id, reqEntity, readCourseOperation, jedis, false);
            String share_url = getCourseShareURL(userId,course_id, courseMap,jedis,appName);
            png = ZXingUtil.createCoursePng(user_head_portrait,userName,courseMap.get("course_title"),share_url,System.currentTimeMillis(),appName);//生成图片
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();//io流
        ImageIO.write(png, "png", baos);//写入流中
        byte[] bytes = baos.toByteArray();//转换成字节
        BASE64Encoder encoder = new BASE64Encoder();
        String png_base64 =  encoder.encodeBuffer(bytes).trim();//转换成base64串
        png_base64 = png_base64.replaceAll("\n", "").replaceAll("\r", "");//删除 \r\n
        return png_base64;
    }

    /**
     *  发送验证码
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("sendVerificationCode")
    public void sendVerificationCode (RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        Map<String,String> map = (Map<String, String>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        String phoneNum = map.get("phone");//手机号
        Map<String,String> reqMap = new HashMap<>();
        reqMap.put("phone_num",phoneNum);
        reqMap.put("app_name",appName);
        if(!CollectionUtils.isEmpty(commonModuleServer.findByPhone(reqMap))){
            throw new QNLiveException("130008");
        }
      //  String ipAdress = map.get("ipAdress");//ip地址
        if(isMobile(phoneNum)){ //效验手机号码
            Jedis jedis = jedisUtils.getJedis(appName);
            Map<String,String> userMap = new HashMap<>();
            userMap.put("user_id",userId);
            String userKey =  MiscUtils.getKeyOfCachedData(Constants.SEND_MSG_TIME_S, userMap);
            jedis.setex(userKey,5*60,phoneNum);//把手机号码和userid还有效验码 存入缓存当中  //一分钟内
            String dayKey =  MiscUtils.getKeyOfCachedData(Constants.SEND_MSG_TIME_D, userMap);//判断日期 一天三次
            if(jedis.exists(dayKey)){
                Map<String,String> redisMap = JSON.parseObject(jedis.get(dayKey), new TypeReference<Map<String, String>>(){});
                if(Integer.parseInt(redisMap.get("count"))==5){
                    throw new QNLiveException("130007");//发送太频繁
                }else{
                    int count = Integer.parseInt(redisMap.get("count")) + 1;

                    int expireTime = (int) (System.currentTimeMillis()/1000 - Long.parseLong(redisMap.get("timestamp"))) ;

                    jedis.setex(dayKey,
                            86410 - expireTime , "{'timestamp':'"+redisMap.get("timestamp")+"','count':'"+count+"'}");
                }
            }else{
                jedis.setex(dayKey,86400,"{'timestamp':'"+System.currentTimeMillis()/1000+"','count':'1'}");//把手机号码和userid还有效验码 存入缓存当中  //一分钟内
            }

            String code = RandomUtil.createRandom(true, 6);   //6位 生成随机的效验码
            String codeKey =  MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_CODE, userMap);//存入缓存中
            jedis.setex(codeKey,20*60,code);

            Map<String,String> phoneMap = new HashMap<>();
            phoneMap.put("code",code);
            phoneMap.put("user_id",userId);
            String phoneKey =  MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_PHONE, phoneMap);
            jedis.setex(phoneKey,20*60,phoneNum);
            //如果是qnlive 就执行
            if(appName.equals(Constants.HEADER_APP_NAME)){
                String message = String.format("您的短信验证码:%s，请及时完成验证。",code);
                String result = SendMsgUtil.sendMsgCode(phoneNum, message,appName);
                logger.info("【梦网】（" + phoneNum + "）发送短信内容（" + message + "）返回结果：" + result);
                if(!"success".equalsIgnoreCase(SendMsgUtil.validateCode(result))){
                    throw new QNLiveException("130006");
                }
            }else{
                boolean djMsg = DjSendMsg.sendVerificationCode(phoneNum, code,jedis);
                if(!djMsg){
                    throw new QNLiveException("130006");
                }
            }
        }else{
            throw new QNLiveException("130001");
        }
    }


    public static boolean isMobile(final String str) {
        Pattern p = null;
        Matcher m = null;
        boolean b = false;
        p = Pattern.compile("^[1][3,4,5,7,8][0-9]{9}$"); // 验证手机号
        m = p.matcher(str);
        b = m.matches();
        return b;
    }


    /**
     * 获取课程消息列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("messageList")
    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());//查询的条数
        int userType =  Integer.parseInt(reqMap.get("user_type").toString());//查询角色类型 0老师/顾问  1用户
        int direction =  Integer.parseInt(reqMap.get("direction").toString());//方向  0旧  1新 默认为1
        String course_id = reqMap.get("course_id").toString();//课程id
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取缓存方法对象
        Map<String, Object> map = new HashMap<>();//map
        map.put(Constants.CACHED_KEY_COURSE_FIELD,course_id);//存入map中
        String messageListKey = null;//查找信息类的key
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("course_id",course_id);
        queryMap.put("page_count", pageCount);//要查询的记录数据
        queryMap.put("user_type", userType);//查询角色类型
        queryMap.put("direction", direction);//方向
        if(reqMap.get("message_imid") != null && StringUtils.isNotBlank(reqMap.get("message_imid").toString())){ //传过来的信息位置
            queryMap.put("message_imid", reqMap.get("message_imid").toString());
        }
        Map<String,String> courseMap = jedis.hgetAll(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map));
        if(courseMap.get("status").equals("2")){ //查询当前课程是否结束
            List<Map<String,Object>> messageList = commonModuleServer.findCourseMessageListByComm(queryMap);
            if(! CollectionUtils.isEmpty(messageList)){
                for(Map<String,Object> messageMap : messageList){
                    if(! MiscUtils.isEmpty(messageMap.get("message")))//信息
                        messageMap.put("message",MiscUtils.RecoveryEmoji(messageMap.get("message").toString()));

                    if(! MiscUtils.isEmpty(messageMap.get("message_question")))//问题 互动
                        messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));

                    if(!MiscUtils.isEmpty(messageMap.get("creator_nick_name")))//名字 表情
                        messageMap.put("creator_nick_name",MiscUtils.RecoveryEmoji(messageMap.get("creator_nick_name").toString()));
                }

                if(direction == 1 && reqMap.get("message_imid").toString().equals("")){
                    Collections.reverse(messageList);
                }
                if(direction == 0 && !reqMap.get("message_imid").toString().equals("")){
                    Collections.reverse(messageList);
                }


                resultMap.put("message_list", messageList);
            }
            if(queryMap.containsKey("message_imid")){//如果有imid
                resultMap.put("this_message",commonModuleServer.findCourseMessageByComm(queryMap));
            }
            resultMap.put("message_count", commonModuleServer.findCourseMessageSum(queryMap));
            return resultMap;
        }else{ //TODO 查询缓存
            //当前课程没有结束 可以直接查询缓存
            if(userType == 0){//查询老师
                if(reqMap.get("message_type") != null && reqMap.get("message_type").toString().equals("0")){
                    messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER_VOICE, map);
                }else{
                    messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
                }
            }else if(userType == 1){//查询用户
                messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_USER, map);
            }
            long message_sum = jedis.zcard(messageListKey);//总共有多少个总数
            resultMap.put("message_count", message_sum);
            //缓存中存在，则读取缓存中的内容
            //初始化下标
            long startIndex = 0; //开始下标
            long endIndex = -1;   //结束下标
            Set<String> messageImIdList = null;//消息集合你
            //来确定消息集合 0旧 -> 1...新
            if(reqMap.get("message_imid") != null && StringUtils.isNotBlank(reqMap.get("message_imid").toString())){   //message_imid 过来

                long endRank = jedis.zrank(messageListKey, reqMap.get("message_imid").toString());//message_imid 在缓存zset中的排名
                if(direction==0){ //获取比当前message旧的消息
                    endIndex = endRank - 1;
                    if(endIndex >= 0){
                        startIndex = endIndex - pageCount + 1;
                        if(startIndex < 0){
                            startIndex = 0;
                        }
                        messageImIdList = jedis.zrange(messageListKey, startIndex, endIndex);//消息集合
                    }else{
                        messageImIdList = null;//消息集合
                    }
                }else{//获取比当前message新消息
                    startIndex = endRank + 1;
                    endIndex = startIndex + pageCount - 1; //消息位置
                    messageImIdList = jedis.zrange(messageListKey, startIndex, endIndex);//消息集合
                }

                String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, queryMap);

                if(jedis.exists(messageKey)){
                    Map<String,String> messageMap = jedis.hgetAll(messageKey);
                    //更改用户名和昵称
                    if(!MiscUtils.isEmpty(messageMap)){
                        if(messageMap.get("creator_id") != null){
                            Map<String,Object> innerMap = new HashMap<>();
                            innerMap.put("user_id", messageMap.get("creator_id"));
                            Map<String,String> userMap = CacheUtils.readUser(messageMap.get("creator_id"), this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedis);
                            if(! MiscUtils.isEmpty(userMap)){
                                if(userMap.get("nick_name") != null){
                                    messageMap.put("creator_nick_name", userMap.get("nick_name"));
                                }
                                if(userMap.get("avatar_address") != null){
                                    messageMap.put("creator_avatar_address", userMap.get("avatar_address"));
                                }
                            }
                        }
                        String messageContent = messageMap.get("message");
                        if(! MiscUtils.isEmpty(messageContent)){
                            messageMap.put("message",MiscUtils.RecoveryEmoji(messageContent));
                        }
                        if(! MiscUtils.isEmpty(messageMap.get("message_question"))){
                            messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
                        }
                        resultMap.put("this_message",messageMap);
                    }
                }

            }else{// 如果没有传过来message_id
               //判断需求是要新的信息 还是就得信息
                if(direction == 0){//从最早的信息开始
                    startIndex = 0;
                    endIndex = pageCount-1;
                    messageImIdList = jedis.zrange(messageListKey, startIndex, endIndex);//消息集合
                }else{//从最新的信息开始
                    endIndex = -1;
                    startIndex = message_sum - pageCount;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                    messageImIdList = jedis.zrange(messageListKey, startIndex, endIndex);//消息集合
                }
            }

            if(! CollectionUtils.isEmpty(messageImIdList)){
                //缓存中存在则读取缓存内容
                List<Map<String,String>> messageListCache = new ArrayList<>();
                for(String messageImId : messageImIdList){
                    map.put(Constants.FIELD_MESSAGE_ID, messageImId);
                    String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
                    Map<String,String> messageMap = jedis.hgetAll(messageKey);
                    messageMap.put("message_pos", startIndex+"");
                    //更改用户名和昵称
                    if(MiscUtils.isEmpty(messageMap)){
                        continue;
                    }
                    if(messageMap.get("creator_id") != null){
                        Map<String,Object> innerMap = new HashMap<>();
                        innerMap.put("user_id", messageMap.get("creator_id"));
                        Map<String,String> userMap = CacheUtils.readUser(messageMap.get("creator_id"), this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedis);
                        if(! MiscUtils.isEmpty(userMap)){
                            if(userMap.get("nick_name") != null){
                                messageMap.put("creator_nick_name", userMap.get("nick_name"));
                            }
                            if(userMap.get("avatar_address") != null){
                                messageMap.put("creator_avatar_address", userMap.get("avatar_address"));
                            }
                        }
                    }
                    String messageContent = messageMap.get("message");
                    if(! MiscUtils.isEmpty(messageContent)){
                        messageMap.put("message",MiscUtils.RecoveryEmoji(messageContent));
                    }
                    if(! MiscUtils.isEmpty(messageMap.get("message_question"))){
                        messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
                    }
                    messageListCache.add(messageMap);
                    startIndex++;
                }
                    resultMap.put("message_list", messageListCache);
            }
            return resultMap;
        }
    }

    /**
     * 获取课程信息
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("courseInfo")
    public Map<String, Object> getCourseInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        String courseId = reqMap.get("course_id").toString();//课程id
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        //0.校验该课程是否属于该讲师
        Map<String,String> courseMap = jedis.hgetAll(courseKey);

        String courseOwner = courseMap.get("lecturer_id");
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//根据安全证书获取userId
        if(courseOwner.equals(userId)){//判断是否是老师
            //如果是老师
            resultMap = getCourseInfoByLecture(reqEntity);
            resultMap.put("user_type","0");
        }else{//学生
            resultMap = getCourseInfoByUser(reqEntity);
            resultMap.put("user_type","1");//用户类型
            Map<String,Object> roomMap = new HashMap<>();
            roomMap.put("room_id",courseMap.get("room_id"));//课程直播间
            roomMap.put("user_id",userId);//用户id
            Map<String, Object> fansMap = commonModuleServer.findFansByUserIdAndRoomId(roomMap);
            if (CollectionUtils.isEmpty(fansMap)) {
                resultMap.put("follow_status", "0");
            } else {
                resultMap.put("follow_status", "1");
            }
        }

        if(!resultMap.get("status").equals("2")){
            if(Long.parseLong(resultMap.get("start_time").toString())<=System.currentTimeMillis()){//如果课程开始时间小于服务器时间
                resultMap.put("status",4);
            }
        }
        resultMap.get("start_time");
        resultMap.put("qr_code",getQrCode(courseOwner,userId,jedis,appName));
        resultMap.put("room_id",courseMap.get("room_id"));
        return resultMap;
    }


    public Map<String, Object> getCourseInfoByLecture(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

        //0.校验该课程是否属于该讲师
        String courseOwner = jedis.hget(courseKey, "lecturer_id");
        if(courseOwner == null || !userId.equals(courseOwner)){
            throw new QNLiveException("100013");
        }

        Map<String,String> courseMap = new HashMap<>();
        //1.先检查该课程是否在缓存中
        if(jedis.exists(courseKey)){
            courseMap = jedis.hgetAll(courseKey);
            JSONArray pptList = null;
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String pptListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
            if(jedis.exists(pptListKey)){
                pptList = JSONObject.parseArray(jedis.get(pptListKey));
            }

            List<Map<String,String>> audioObjectMapList = new ArrayList<>();
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
            String audioJsonStringKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, map);
            Set<String> audioIdList = jedis.zrange(audioListKey, 0 , -1);

            //如果存在zsort列表，则从zsort列表中读取
            if(audioIdList != null && audioIdList.size() > 0){
                JedisBatchCallback callBack = (JedisBatchCallback)jedis;
                callBack.invoke(new JedisBatchOperation(){
                    @Override
                    public void batchOperation(Pipeline pipeline, Jedis jedis) {

                        List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
                        for(String audio : audioIdList){
                            map.put(Constants.FIELD_AUDIO_ID, audio);
                            String audioKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
                            redisResponseList.add(pipeline.hgetAll(audioKey));
                        }
                        pipeline.sync();

                        for(Response<Map<String, String>> redisResponse : redisResponseList){
                            Map<String,String> messageStringMap = redisResponse.get();
                            audioObjectMapList.add(messageStringMap);
                        }
                    }
                });

                resultMap.put("audio_list", audioObjectMapList);

                //如果存在讲课音频的json字符串，则读取讲课音频json字符串
            } else if(jedis.exists(audioJsonStringKey)){
                resultMap.put("audio_list", JSONObject.parse(jedis.get(audioJsonStringKey)));
            }

            if(! CollectionUtils.isEmpty(pptList)){
                resultMap.put("ppt_list", pptList);
            }

            resultMap.put("im_course_id", jedis.hget(courseKey, "im_course_id"));
        }else{
            //2.如果不在缓存中，则查询数据库
            Map<String,Object> courseInfoMap = commonModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            if(courseInfoMap == null){
                throw new QNLiveException("100004");
            }
            courseMap = new HashMap<>();
            MiscUtils.converObjectMapToStringMap(courseInfoMap,courseMap);

            //查询课程PPT列表
            List<Map<String,Object>> pptList = commonModuleServer.findPPTListByCourseId(reqMap.get("course_id").toString());

            //查询课程语音列表
            List<Map<String,Object>> audioList = commonModuleServer.findAudioListByCourseId(reqMap.get("course_id").toString());

            if(! CollectionUtils.isEmpty(pptList)){
                resultMap.put("ppt_list", pptList);
            }

            if(! CollectionUtils.isEmpty(audioList)){
                resultMap.put("audio_list", audioList);
            }

            resultMap.put("im_course_id", courseInfoMap.get("im_course_id"));

        }

        map.clear();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
        Set<String> banUserIdList = jedis.zrange(bandKey, 0 , -1);
        if(banUserIdList != null && banUserIdList.size() > 0){
            resultMap.put("ban_user_id_list", banUserIdList);
        }

        try {
            //检查学生上次加入课程，如果加入课程不为空，则退出上次课程
            Map<String,String> queryParam = new HashMap<>();
            queryParam.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
            String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, queryParam);
            Map<String,String> loginInfo = jedis.hgetAll(accessTokenKey);

            map.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String courseIMKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_LAST_JOIN_COURSE_IM_INFO, map);
            String imCourseId = jedis.get(courseIMKey);
            if(! MiscUtils.isEmpty(imCourseId)){
                IMMsgUtil.delGroupMember(imCourseId, loginInfo.get("m_user_id"), loginInfo.get("m_user_id"));
            }

            //加入新课程IM群组，并且将加入的群组记录入缓存中
            IMMsgUtil.joinGroup(courseMap.get("im_course_id"), loginInfo.get("m_user_id"), loginInfo.get("m_user_id"));
            jedis.set(courseIMKey, courseMap.get("im_course_id"));
        }catch (Exception e){
            //TODO 暂时不处理
        }

        //增加返回课程相应信息
        MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);
        resultMap.put("student_num",courseMap.get("student_num"));
        resultMap.put("start_time",courseMap.get("start_time"));
        resultMap.put("status",courseMap.get("status"));
        resultMap.put("course_type",courseMap.get("course_type"));
        resultMap.put("course_password",courseMap.get("course_password"));
        resultMap.put("share_url",MiscUtils.getConfigByKey("course_share_url",appName)+reqMap.get("course_id").toString());//TODO
        resultMap.put("course_update_time",courseMap.get("update_time"));
        resultMap.put("course_title",MiscUtils.RecoveryEmoji(courseMap.get("course_title")));
        resultMap.put("course_url",courseMap.get("course_url"));
        return resultMap;
    }

    public Map<String, Object> getCourseInfoByUser(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

        Map<String, String> courseMap = new HashMap<>();
        //1.先检查该课程是否在缓存中
        if (jedis.exists(courseKey)) {
            courseMap = jedis.hgetAll(courseKey);
            JSONArray pptList = null;
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String pptListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
            if (jedis.exists(pptListKey)) {
                pptList = JSONObject.parseArray(jedis.get(pptListKey));
            }
            List<Map<String, String>> audioObjectMapList = new ArrayList<>();
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
            String audioJsonStringKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, map);
            Set<String> audioIdList = jedis.zrange(audioListKey, 0, -1);

            //如果存在zsort列表，则从zsort列表中读取
            if (audioIdList != null && audioIdList.size() > 0) {
                JedisBatchCallback callBack = (JedisBatchCallback)jedis;
                callBack.invoke(new JedisBatchOperation() {
                    @Override
                    public void batchOperation(Pipeline pipeline, Jedis jedis) {

                        List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
                        for (String audio : audioIdList) {
                            map.put(Constants.FIELD_AUDIO_ID, audio);
                            String audioKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
                            redisResponseList.add(pipeline.hgetAll(audioKey));
                        }
                        pipeline.sync();

                        for (Response<Map<String, String>> redisResponse : redisResponseList) {
                            Map<String, String> messageStringMap = redisResponse.get();
                            audioObjectMapList.add(messageStringMap);
                        }
                    }
                });

                resultMap.put("audio_list", audioObjectMapList);

                //如果存在讲课音频的json字符串，则读取讲课音频json字符串
            } else if (jedis.exists(audioJsonStringKey)) {
                resultMap.put("audio_list", JSONObject.parse(jedis.get(audioJsonStringKey)));
            }

            if (!CollectionUtils.isEmpty(pptList)) {
                resultMap.put("ppt_list", pptList);
            }

            resultMap.put("im_course_id", jedis.hget(courseKey, "im_course_id"));

            //判断该课程状态，如果为直播中，则检查开播时间是否存在，如果开播时间存在，
            //则检查当前查询是否大于当前时间，如果大于，则查询用户是否存在于在线map中，
            //如果不存在，则将该学员加入的在线map中，并且修改课程缓存real_student_num实际课程人数(默认课程人数)
            String realStudentNum = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_REAL_STUDENT_NUM, map);
            boolean hasStudent = jedis.hexists(realStudentNum, userId);
            if (hasStudent == false) {
                if (courseMap.get("status").equals("4")) {
                    if (courseMap.get("real_start_time") != null) {
                        Long real_start_time = Long.parseLong(courseMap.get("real_start_time"));
                        long now = System.currentTimeMillis();
                        if (now > real_start_time) {
                            jedis.hset(realStudentNum, userId, "1");
                            jedis.hincrBy(courseKey, "real_student_num", 1);
                        }
                    }
                }
            }
        } else {
            //2.如果不在缓存中，则查询数据库
            Map<String, Object> courseInfoMap = commonModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            MiscUtils.converObjectMapToStringMap(courseInfoMap, courseMap);
            if (courseInfoMap == null) {
                throw new QNLiveException("100004");
            }
            //查询课程PPT列表
            List<Map<String, Object>> pptList = commonModuleServer.findPPTListByCourseId(reqMap.get("course_id").toString());
            //查询课程语音列表
            List<Map<String, Object>> audioList = commonModuleServer.findAudioListByCourseId(reqMap.get("course_id").toString());

            if (!CollectionUtils.isEmpty(pptList)) {
                resultMap.put("ppt_list", pptList);
            }

            if (!CollectionUtils.isEmpty(audioList)) {
                resultMap.put("audio_list", audioList);
            }

            resultMap.put("im_course_id", courseInfoMap.get("im_course_id"));

        }

        //4.将学生加入该课程的IM群组
        try {
            //检查学生上次加入课程，如果加入课程不为空，则退出上次课程
            Map<String,Object> studentUserMap = commonModuleServer.findLoginInfoByUserId(userId);
            map.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String courseIMKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_LAST_JOIN_COURSE_IM_INFO, map);
            String imCourseId = jedis.get(courseIMKey);
            if(! MiscUtils.isEmpty(imCourseId)){
                IMMsgUtil.delGroupMember(imCourseId, studentUserMap.get("m_user_id").toString(), studentUserMap.get("m_user_id").toString());
            }

            //加入新课程IM群组，并且将加入的群组记录入缓存中
            IMMsgUtil.joinGroup(courseMap.get("im_course_id"), studentUserMap.get("m_user_id").toString(),studentUserMap.get("m_user_id").toString());
            jedis.set(courseIMKey, courseMap.get("im_course_id"));
        }catch (Exception e){
            //TODO 暂时不处理
        }

        map.clear();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
        Double banScore = jedis.zscore(banKey, userId);
        if(banScore == null){
            resultMap.put("ban_status", "0");
        }else {
            resultMap.put("ban_status", "1");
        }

        //增加返回课程相应信息
        resultMap.put("student_num",courseMap.get("student_num"));
        resultMap.put("start_time",courseMap.get("start_time"));
        resultMap.put("status",courseMap.get("status"));
        resultMap.put("course_type",courseMap.get("course_type"));
        resultMap.put("course_password",courseMap.get("course_password"));
        resultMap.put("share_url",MiscUtils.getConfigByKey("course_share_url_pre_fix",appName)+reqMap.get("course_id").toString());//TODO
        resultMap.put("course_update_time",courseMap.get("update_time"));
        resultMap.put("course_title",MiscUtils.RecoveryEmoji(courseMap.get("course_title")));
        //Map<String,Object> userMap = userModuleServer.findUserInfoByUserId(courseMap.get("lecturer_id"));
        map.clear();
        map.put("lecturer_id", courseMap.get("lecturer_id"));
        Map<String, String> userMap = CacheUtils.readLecturer(courseMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);
        if(!MiscUtils.isEmpty(userMap)){
            resultMap.put("lecturer_nick_name",userMap.get("nick_name"));
            resultMap.put("lecturer_avatar_address",userMap.get("avatar_address"));
        }
        resultMap.put("course_url",courseMap.get("course_url"));

        return resultMap;
    }

    /**
     * TODO 有复用的方法 以后重构是需要进行合并
     * 获取讲师二维码/青柠二维码
     */
    public Map getQrCode(String lectureId, String userId, Jedis jedis,String appName) {
        Map<String, String> query = new HashMap();
        query.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, lectureId);
        //1.判断讲师是否有公众号 有就直接返回
        String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, query);
        if (jedis.exists(serviceNoKey)) {//判断当前是否有这个缓存
            query.put("qr_code_url",jedis.hgetAll(serviceNoKey).get("qr_code"));
            query.put("qr_code_title", MiscUtils.getConfigByKey("weixin_qr_code_title_lecturer",appName));
            query.put("qr_code_message", MiscUtils.getConfigByKey("weixin_qr_code_message",appName));
            query.put("qr_code_title", MiscUtils.getConfigByKey("weixin_qr_code_ad",appName));
            return query;
        } else {//2.判断是否有关注我们公众号
            Map<String,Object> map = commonModuleServer.findLoginInfoByUserId(userId);
            if(Integer.parseInt(map.get("subscribe").toString())==0){//没有关注
                query.put("qr_code_url", MiscUtils.getConfigByKey("weixin_qr_code",appName));
                query.put("qr_code_title", MiscUtils.getConfigByKey("weixin_qr_code_title_qingning",appName));
                query.put("qr_code_message", MiscUtils.getConfigByKey("weixin_qr_code_message",appName));
                query.put("qr_code_ad", MiscUtils.getConfigByKey("weixin_qr_code_ad",appName));
                return query;
            }
        }
        return null;
    }


    /**
     * 判断手机是否可以使用
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("isphone")
    public void isphone (RequestEntity reqEntity) throws Exception{
        Map<String,String> map = (Map<String, String>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        String phoneNum = map.get("phone_num");//手机号
        if(!isMobile(phoneNum)){
            throw new QNLiveException("130001");
        }
        Map<String,String> reqMap = new HashMap<>();
        reqMap.put("phone_number",phoneNum);
        reqMap.put("appName",appName);
        if(!MiscUtils.isEmpty(commonModuleServer.findByPhone(reqMap))){
            throw new QNLiveException("130008");
        }
    }



    /**
     * 搜索/类型
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("search")
    public Map<String, Object> search (RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String appName = reqEntity.getAppName();
        Map<String,Object> map = (Map<String, Object>) reqEntity.getParam();
        reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        String search_text = map.get("search_text").toString();//搜索文本
        if(search_text != null || !search_text.equals("")){
            search_text = "%"+search_text+"%";//拼接百分号 进行包含查询
            map.put("search_text",search_text);
        }
        Integer page_num = Integer.valueOf(map.get("page_num").toString());
        map.put("page_num",page_num);
        Integer page_count = Integer.valueOf(map.get("page_count").toString());
        map.put("page_count",page_count);
        int search_type = Integer.valueOf(map.get("search_type").toString()); //查询类型 0所有 1直播间 2课程
        Jedis jedis = jedisUtils.getJedis(appName);
        map.put("appName",appName);

        //查询所有 或者 查询直播间
        if(search_type ==0 || search_type == 1){
            List<Map<String, Object>> liveRoomBySearch = commonModuleServer.findLiveRoomBySearch(map);
            if(! MiscUtils.isEmpty(liveRoomBySearch)){
                Map<String,Object> query = new HashMap<String,Object>();
                query.put(Constants.CACHED_KEY_USER_FIELD, userId);
                String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOMS, query);//用来查询当前用户加入了那些课程
                if(!jedis.exists(key)){//判断是否存在key
                    Map<String,Object> queryParam = new HashMap<String,Object>();
                    queryParam.put("user_id", userId);
                    RequestEntity queryOperation = this.generateRequestEntity(null,null, null, queryParam);
                    CacheUtils.readUser(userId, queryOperation, readUserOperation, jedis);
                }
                for(Map<String, Object> liveRoom : liveRoomBySearch){
                    if(jedis.sismember(key, liveRoom.get("room_id").toString())){//判断当前用户是否有加入这个课程
                        liveRoom.put("fens", "1");
                    } else {
                        liveRoom.put("fens", "0");
                    }
                }
                resultMap.put("room_list",liveRoomBySearch);
            }
        }

        //查询所有 或者 查询课程
        if(search_type ==0 || search_type == 2){
            List<Map<String, Object>> courseBySearch = commonModuleServer.findCourseBySearch(map);//查询数据
            resultMap.put("course_list",this.setStudentAndLecturerNickName(courseBySearch,userId,jedis, Thread.currentThread().getStackTrace()[1].getMethodName()));
        }
        return resultMap;

    }

    private List<Map<String, Object>> setStudentAndLecturerNickName(List<Map<String,Object>> courseBySearch,String userId,Jedis jedis,String methodName) throws Exception {
        long currentTime = System.currentTimeMillis();//当前时间

        if(! MiscUtils.isEmpty(courseBySearch)){
            Map<String,Object> query = new HashMap<String,Object>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            query.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);//用来查询当前用户加入了那些课程
            for(Map<String, Object> course : courseBySearch ){
                if("recommendCourse".equals(methodName)){
                    MiscUtils.courseTranferState(currentTime,course,Long.valueOf(course.get("start_time").toString()));
                }else{
                    Date date = sdf.parse(course.get("start_time").toString());
                    MiscUtils.courseTranferState(currentTime,course,date.getTime());//进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                }


                if(jedis.sismember(key, course.get("course_id").toString())){//判断当前用户是否有加入这个课程
                    course.put("student", "Y");
                } else {
                    course.put("student", "N");
                }
            }
            Map<String,String> query1 = new HashMap<>();
            for(Map<String, Object> course : courseBySearch ){
                query1.put(Constants.CACHED_KEY_LECTURER_FIELD, course.get("lecturer_id").toString());
                key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query1);
                String lecturer_nick_name = jedis.hget(key,"nick_name");
                course.put("lecturer_nick_name",lecturer_nick_name);
            }
            return courseBySearch;
        }
        return null;
    }

    /**
     * 推荐
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("recommendCourse")
    public Map<String, Object> recommendCourse (RequestEntity reqEntity) throws Exception{
        String appName = reqEntity.getAppName();
        Map<String,Object> map = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        Integer page_num = Integer.valueOf(map.get("page_num").toString());
        map.put("page_num",page_num);
        Integer page_count = Integer.valueOf(map.get("page_count").toString());
        map.put("page_count",page_count);
        Map<String, Object> resultMap = new HashMap<String, Object>();
        int select_type = Integer.parseInt(map.get("select_type").toString());//查询类型1是推荐课程换一换 2推荐课程下拉
        Jedis jedis = jedisUtils.getJedis(appName);

        if(select_type == 2 || select_type == 1 ){
            List<Map<String, Object>> courseByRecommendList = new ArrayList<>();
            if(!jedis.exists(Constants.CACHED_KEY_RECOMMEND_COURSE)){//查看有没有推荐课程
                List<Map<String, Object>> recommendCourseList = commonModuleServer.findCourseByRecommend(appName);//查询推荐课程
                jedis.set(Constants.RECOMMEND_COURSE_NUM,""+recommendCourseList.size());
                for(Map<String, Object> recommendCourse : recommendCourseList){
                    jedis.zadd(Constants.CACHED_KEY_RECOMMEND_COURSE, Integer.valueOf( recommendCourse.get("recommend_seat").toString()),recommendCourse.get("course_id").toString());
                    Map<String, String> keyMap = new HashMap<String, String>();
                    keyMap.put(Constants.CACHED_KEY_COURSE_FIELD, recommendCourse.get("course_id").toString());
                    String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, keyMap);
                    jedis.hset(key,"recommend_seat",recommendCourse.get("recommend_seat").toString());
                }
            }
            if(select_type == 1){
                if(page_num == Integer.valueOf(jedis.get(Constants.RECOMMEND_COURSE_NUM))){ //比较是否是推荐课程的最大值  如果是最大值就归零
                    page_num = 0;
                    map.put("page_num",page_num);
                }
            }
            if(jedis.exists(Constants.CACHED_KEY_RECOMMEND_COURSE)){//存在
                int startIndex = page_num;
                int endIndex = page_num + page_count-1;
                Set<String> recommendCourseIdSet = jedis.zrange(Constants.CACHED_KEY_RECOMMEND_COURSE, startIndex, endIndex);//获取推荐课程
                if(select_type == 1 && recommendCourseIdSet.size() < page_count){//如果是换一换 并且 查询的结果不够
                    startIndex = 0;
                    endIndex = page_count - recommendCourseIdSet.size() - 1;
                    recommendCourseIdSet.addAll(jedis.zrange(Constants.CACHED_KEY_RECOMMEND_COURSE, startIndex, endIndex));
                }
                Map<String,String> queryParam = new HashMap<String,String>();
                for(String courseId : recommendCourseIdSet){//循环读取课程信息
                    queryParam.put("course_id", courseId);
                    Map<String, Object> courseInfoMap =(Map)CacheUtils.readCourse(courseId, this.generateRequestEntity(null, null, null, queryParam), readCourseOperation, jedis, true);//从缓存中读取课程信息
                    courseByRecommendList.add(courseInfoMap);
                }
            }
            if(! MiscUtils.isEmpty(courseByRecommendList)){
                resultMap.put("recommend_courses",this.setStudentAndLecturerNickName(courseByRecommendList,userId,jedis, Thread.currentThread().getStackTrace()[1].getMethodName()));
            }
        }
        return resultMap;
    }



    /**
     * 获取分类
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("classifyInfo")
    public Map<String, Object> classifyInfo (RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        List<Map<String, Object>> classifyList = new ArrayList<>();
        if(jedis.exists(Constants.CACHED_KEY_CLASSIFY_ALL)){ //判断当前是否有缓存key 存在
            Set<String> classifyIdSet = jedis.zrange(Constants.CACHED_KEY_CLASSIFY_ALL, 0, -1);//获取所有值
            Map<String,Object> map = new HashMap<>();
            for(String classify_id : classifyIdSet){
                map.put(Constants.CACHED_KEY_CLASSIFY,classify_id);
                String classifyInfoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_CLASSIFY_INFO, map);//生成查找分类的key
                if(jedis.exists(classifyInfoKey)){//获取分类
                    Map<String, String> classifyInfo = jedis.hgetAll(classifyInfoKey);
                    Map<String,Object> classify_info = new HashMap<>();
                    classify_info.put("classify_id",classifyInfo.get("classify_id"));
                    classify_info.put("classify_name",classifyInfo.get("classify_name"));
                    classify_info.put("is_use",classifyInfo.get("is_use"));
                    classify_info.put("create_time",classifyInfo.get("create_time"));
                    classifyList.add(classify_info);
                }
            }
        }else{ //没有
            String likeAppNmae = "%"+appName+"%";
             classifyList = commonModuleServer.findClassifyInfoByAppName(likeAppNmae);//读数据库
            Map<String,String> classify_info = new HashMap<>();
            for(Map<String, Object>classify : classifyList){
                jedis.zadd(Constants.CACHED_KEY_CLASSIFY_ALL,System.currentTimeMillis(),classify.get("classify_id").toString());
                classify_info.put("classify_id",classify.get("classify_id").toString());
                classify_info.put("classify_name",classify.get("classify_name").toString());
                classify_info.put("is_use",classify.get("is_use").toString());
                classify_info.put("create_time",classify.get("create_time").toString());
                Map<String,Object> map = new HashMap<>();
                map.put("classify_id",classify.get("classify_id").toString());
                jedis.hmset(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_CLASSIFY_INFO, map),classify_info);
            }
        }
//        jedis.del(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION);//分类
//        jedis.del(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH);//分类
//        for(Map<String, Object> classify :classifyList ) {
//            String classify_id = classify.get("classify_id").toString();
//            Map<String,Object> map = new HashMap<>();
//            map.put("appName",appName);
//            map.put("classify_id",classify_id);
//           jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map));//分类
//            jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map));//分类
//        }
        //讲师
        Set<String> lecturerSet = jedis.smembers(Constants.CACHED_LECTURER_KEY);
        if(!MiscUtils.isEmpty(lecturerSet)){
            for(String lecturerId : lecturerSet) {
                //删除缓存中的旧的课程列表及课程信息实体
                Map<String, Object> map = new HashMap<>();
                map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                String predictionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
                String finishListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
                jedis.del(predictionListKey);
                jedis.del(finishListKey);
            }
        }



        for(Map<String, Object> classify :classifyList ){
            String classify_id = classify.get("classify_id").toString();
            Map<String,Object> map = new HashMap<>();
            map.put("appName",appName);
            map.put("classify_id",classify_id);
            List<Map<String, Object>> courseByClassifyId = commonModuleServer.findCourseByClassifyId(map);
            for(Map<String, Object> course : courseByClassifyId){
                if(course.get("status").equals("2") || course.get("status").equals("1")){
                    String course_id = course.get("course_id").toString();
                    String lecturer_id = course.get("lecturer_id").toString();
                    map.put(Constants.CACHED_KEY_CLASSIFY, classify_id);//课程id
                    map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
                    map.put("course_id",course_id);
                    String courseClassifyIdKey = "";
                    String courseLectureKey = "";
                    //String courseListKey = "";
                    Long time = 0L ;
                    if(course.get("status").equals("2")){
                   //     courseListKey = Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;
 //                       courseClassifyIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map);//分类
                        courseLectureKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
                        time = MiscUtils.convertObjectToLong(course.get("end_time"));//Long.valueOf(course.get("end_time").toString());

                    }else{
                 //       courseListKey = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
             //           courseClassifyIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);//分类
                        courseLectureKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
                        time = MiscUtils.convertObjectToLong(course.get("start_time"));//Long.valueOf(course.get("start_time").toString());

                    }
//                    if(jedis.zrank(courseClassifyIdKey,course_id) ==  null ){
//
//                        map.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);//课程id

                    String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//"SYS:COURSE:{course_id}"

                    long lpos = MiscUtils.convertInfoToPostion(time, MiscUtils.convertObjectToLong(jedis.hget(courseKey, "position")));
             //        jedis.zadd(courseClassifyIdKey, lpos,course_id);//在结束中增加
                      jedis.zadd(courseLectureKey, lpos,course_id);//在结束中增加

//                        if(course.get("status").equals("2")){
//                            jedis.zadd(courseListKey,lpos, course_id);
////                            Map<String, String> updateCacheMap = new HashMap<String, String>();
////                            updateCacheMap.put("update_time", MiscUtils.convertObjectToLong(course.get("end_time")) + "");
////                            updateCacheMap.put("end_time", MiscUtils.convertObjectToLong(course.get("end_time")) + "");
////                            updateCacheMap.put("status", "2");
//                            jedis.hset(courseKey,"status","2");
//                           // jedis.hmset(courseKey, updateCacheMap);
////                            jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, course_id);
////                            if(jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)){
////
////                            }
//                        }else{
//
////                        jedis.zadd(courseClassifyIdKey, lpos,course_id);//在结束中增加
////                        jedis.zadd(courseLectureKey, lpos,course_id);//在结束中增加
//                            jedis.zadd(courseListKey, lpos,course_id);
//                        }

                    }
//                }else if(course.get("status").equals("5")){
//                    String coursekey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, course);//获取课程在缓存中的key
//                    jedis.hset(coursekey,"status","5");//把课程缓存中的状态改为已删除
//                }



                //<editor-fold desc="Description">
                //                String course_id = course.get("course_id").toString();
//
//                Map<String,Object> updateCourse = new HashMap<>();
//                updateCourse.put("course_id",course_id);
//                updateCourse.put("status","0");
//                String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//"SYS:COURSE:{course_id}"
//                jedis.del(courseKey);
//                map.put("course_id",course_id);
//                map.put("profit_type","0");
//                Map<String, Object> coursesSumInfo = commonModuleServer.findCoursesSumInfo(map);
//                if(!MiscUtils.isEmpty(coursesSumInfo)){
//                    String lecturer_profit = coursesSumInfo.get("lecturer_profit").toString();
//                    updateCourse.put("course_amount",lecturer_profit);
//
//                }
//                Map<String, Object> courseRecommendUserNum = commonModuleServer.findCourseRecommendUserNum(map);
//                if(!MiscUtils.isEmpty(courseRecommendUserNum)){
//                    String recommend_num = courseRecommendUserNum.get("recommend_num").toString();
//                    updateCourse.put("student_num",recommend_num);
//                }
//
//                map.put("profit_type","1");
//                Map<String, Object> coursesSumInfo1 = commonModuleServer.findCoursesSumInfo(map);
//                if(!MiscUtils.isEmpty(coursesSumInfo1)){
//                    String lecturer_profit = coursesSumInfo1.get("lecturer_profit").toString();
//                    updateCourse.put("extra_amount",lecturer_profit);
//                }
//
//                map.put("profit_type","1");
//                Map<String, Object> coursesSumInfo2 = commonModuleServer.findUserNumberByCourse(map);
//                if(!MiscUtils.isEmpty(coursesSumInfo1)){
//                    String lecturer_profit = coursesSumInfo2.get("user_number").toString();
//                    updateCourse.put("extra_num",lecturer_profit);
//                }
//
//                if(!MiscUtils.isEmpty(updateCourse)){
//                    commonModuleServer.updateCourse(updateCourse);
//                }
                //</editor-fold>
                map.clear();
            }
        }
//        Map<String,Object> map = new HashMap<>();
//        map.put("appName",appName);
//        map.put("status","5");
//        List<Map<String, Object>> courseIdList = commonModuleServer.findCourseByStatus(map);
//        for(Map<String, Object> courseid : courseIdList){
//            String id = courseid.get("course_id").toString();
//            jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION,id);
//            jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH,id);
//        }

        //进行排序
        List<Map<String, Object>> resultClassifyList = new ArrayList<>();//返回结果
        Map<String, Object> ortherClassify = new HashMap<>();//其他
        for(Map<String, Object>classify : classifyList){
            if (!classify.get("classify_id").toString().equals("9")) {//不是其他
                resultClassifyList.add(classify);
            }else if(classify.get("classify_id").toString().equals("9")){
                ortherClassify.putAll(classify);//其他
            }
        }

        if(!MiscUtils.isEmpty(ortherClassify)){
            resultClassifyList.add(ortherClassify);
        }


        if(!MiscUtils.isEmpty(resultClassifyList)){
            resultMap.put("classify_info",resultClassifyList);
        }
        return resultMap;
    }



    /**
     * 广告位
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("banner")
    public Map<String, Object> banner (RequestEntity reqEntity) throws Exception{
        Map<String,Object> map = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        String appName = reqEntity.getAppName();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Jedis jedis = jedisUtils.getJedis(appName);
        //<editor-fold desc="横幅">
            List<Map<String, Object>> bannerInfoList = new ArrayList<>();
            if(jedis.exists(Constants.CACHED_KEY_BANNER_ALL)){//查看是否有banner存在
                Set<String> bannerInfoIdSet = jedis.zrange(Constants.CACHED_KEY_BANNER_ALL, 0, -1);//获取所有值
                Map<String,Object> query = new HashMap<>();
                for(String bannerInfoId : bannerInfoIdSet){
                    query.put(Constants.CACHED_KEY_BANNER,bannerInfoId);
                    String bannerInfoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_BANNER_INFO, query);//查找横幅key
                    if(jedis.exists(bannerInfoKey)){//获取分类
                        Map<String, String> bannerInfo = jedis.hgetAll(bannerInfoKey);
                        Map<String,Object> banner_info = new HashMap<>();
                        banner_info.put("banner_id",bannerInfo.get("banner_id"));
                        banner_info.put("banner_remarks",bannerInfo.get("banner_remarks"));
                        banner_info.put("banner_img_url",bannerInfo.get("banner_img_url"));
                        banner_info.put("jump_url",bannerInfo.get("jump_url"));
                        banner_info.put("create_time",bannerInfo.get("create_time"));
                        banner_info.put("status",bannerInfo.get("status"));
                        banner_info.put("banner_type",bannerInfo.get("banner_type"));
                        bannerInfoList.add(banner_info);
                    }
                }
            }else{ //不存在
                bannerInfoList = commonModuleServer.findBannerInfoAllByAppName(appName);//查找广告位
                if(! MiscUtils.isEmpty(bannerInfoList)){
                    Map<String,String> banner_info = new HashMap<>();
                    for(Map<String, Object> bannerInfo : bannerInfoList){
                        jedis.zadd(Constants.CACHED_KEY_BANNER_ALL,System.currentTimeMillis(),bannerInfo.get("banner_id").toString());
                        banner_info.put("banner_id",bannerInfo.get("banner_id").toString());
                        banner_info.put("banner_remarks",bannerInfo.get("banner_remarks").toString());
                        banner_info.put("banner_img_url",bannerInfo.get("banner_img_url").toString());
                        banner_info.put("jump_url",bannerInfo.get("jump_url").toString());
                        banner_info.put("create_time",bannerInfo.get("create_time").toString());
                        banner_info.put("status",bannerInfo.get("status").toString());
                        banner_info.put("banner_type",bannerInfo.get("banner_type").toString());
                        Map<String,Object> query = new HashMap<>();
                        query.put(Constants.CACHED_KEY_BANNER,bannerInfo.get("banner_id").toString());
                        jedis.hmset(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_BANNER_INFO, query),banner_info);
                    }
                }
            }
            resultMap.put("banner_info",bannerInfoList);
        //</editor-fold>
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("saveCourseMsgList")
    public Map<String, Object> saveCourseMsgList (RequestEntity reqEntity) throws Exception{
        Map<String,Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String courseId = reqMap.get("course_id").toString();
        Jedis jedis = jedisUtils.getJedis(appName);

        //<editor-fold desc="单个讲师">
        //        Map<String,Object> query = new HashMap<>();
//        query.put("course_id", courseId);
//        Map<String,String> courseMap = CacheUtils.readCourse(courseId, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, false);
//        String lecturer_id = courseMap.get("lecturer_id");
//
//        Map<String,Object> map = new HashMap<>();
//        map.put("lecturer_id",lecturer_id);
//        String predictionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
//        String finishListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
//        jedis.del(predictionListKey);
//        jedis.del(finishListKey);
//
//        List<Map<String, Object>> lecturerCourseList = commonModuleServer.findLecturerCourseList(map);
//        for(Map<String, Object> course : lecturerCourseList){
//            Long time = 0L ;
//            if(course.get("status").toString().equals("2")){
//                time = MiscUtils.convertObjectToLong(course.get("end_time"));//Long.valueOf(course.get("end_time").toString());
//                long lpos = MiscUtils.convertInfoToPostion(time, MiscUtils.convertObjectToLong(course.get("position")));
//                jedis.zadd(finishListKey, lpos,course.get("course_id").toString());//在结束中增加
//            }else if(course.get("status").toString().equals("1")){
//                time = MiscUtils.convertObjectToLong(course.get("end_time"));//Long.valueOf(course.get("end_time").toString());
//                long lpos = MiscUtils.convertInfoToPostion(time, MiscUtils.convertObjectToLong(course.get("position")));
//                jedis.zadd(predictionListKey, lpos,course.get("course_id").toString());//在结束中增加
//            }
//        }
        //</editor-fold>



        //<editor-fold desc="讲师课程消息落地">
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD,courseId);
        String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);//COURSE:{course_id}:MESSAGE_LIST

        Jedis jedisObject = jedisUtils.getJedis(appName);

        //1.从缓存中查询该课程的消息列表
        Set<String> messageIdList = jedisObject.zrange(messageListKey, 0, -1);//jedisObject.zrevrange(messageListKey, 0 , -1);
        if(messageIdList == null || messageIdList.size() == 0){

            throw new QNLiveException("000001");
        }

        //2.批量从缓存中读取消息详细信息
        List<Map<String,Object>> messageList = new ArrayList<>();
        List<String> messageKeyList = new ArrayList<>();
        JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis(appName);
        callBack.invoke(new JedisBatchOperation(){
            @Override
            public void batchOperation(Pipeline pipeline, Jedis jedis) {

                long messagePos = 0L;
                List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
                for(String messageimid : messageIdList){
                    map.put(Constants.FIELD_MESSAGE_ID, messageimid);
                    String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
                    redisResponseList.add(pipeline.hgetAll(messageKey));
                    messageKeyList.add(messageKey);
                }
                pipeline.sync();

                for(Response<Map<String, String>> redisResponse : redisResponseList){
                    Map<String,String> messageStringMap = redisResponse.get();
                    Map<String,Object> messageObjectMap = new HashMap<>();
                    if(messageStringMap.get("message_imid") == null){
                        return;
                    }
                    if(!MiscUtils.isEmpty(messageStringMap.get("message_id"))){
                        messageObjectMap.put("message_id", messageStringMap.get("message_id"));
                    }

                    messageObjectMap.put("course_id", messageStringMap.get("course_id"));


                    if(!MiscUtils.isEmpty(messageStringMap.get("message"))){
                        messageObjectMap.put("message", messageStringMap.get("message"));
                    }else{
                        messageObjectMap.put("message", null);
                    }

                    if(!MiscUtils.isEmpty(messageStringMap.get("message_url"))){
                        messageObjectMap.put("message_url", messageStringMap.get("message_url"));
                    }else{
                        messageObjectMap.put("message_url",null);
                    }

                    if(!MiscUtils.isEmpty(messageStringMap.get("message_question"))){
                        messageObjectMap.put("message_question", messageStringMap.get("message_question"));
                    }else{
                        messageObjectMap.put("message_question", null);
                    }


                    if(!MiscUtils.isEmpty(messageStringMap.get("audio_time"))){
                        messageObjectMap.put("audio_time", Long.parseLong(messageStringMap.get("audio_time")));
                    }else {
                        messageObjectMap.put("audio_time", 0);
                    }


                    messageObjectMap.put("message_type", messageStringMap.get("message_type"));
                    messageObjectMap.put("send_type", messageStringMap.get("send_type"));


                    messageObjectMap.put("creator_id", messageStringMap.get("creator_id"));

                    if(!MiscUtils.isEmpty(messageStringMap.get("create_time"))){
                        Date createTime = new Date(Long.parseLong(messageStringMap.get("create_time")));
                        messageObjectMap.put("create_time", createTime);
                    }
                    if(!MiscUtils.isEmpty(messageStringMap.get("audio_image"))){
                        messageObjectMap.put("audio_image", messageStringMap.get("audio_image"));
                    }else{
                        messageObjectMap.put("audio_image", null);
                    }
                    if(!MiscUtils.isEmpty(messageStringMap.get("message_status"))){
                        messageObjectMap.put("message_status",messageStringMap.get("message_status"));
                    }else{
                        messageObjectMap.put("message_status",0);
                    }
                    if(!MiscUtils.isEmpty(messageStringMap.get("message_imid"))){
                        messageObjectMap.put("message_imid", messageStringMap.get("message_imid"));
                    }else{
                        messageObjectMap.put("message_imid",MiscUtils.getUUId());
                    }

                    messageObjectMap.put("message_pos", messagePos++);
                    messageList.add(messageObjectMap);
                }
            }
        });
        //3.批量插入到数据库中
        Integer insertResult = commonModuleServer.insertCourseMessageList(messageList);

        //4.如果插入数据库正常，则删除缓存中的内容
        if(insertResult != null && insertResult > 0){
            //删除redis中的key
            String[] messageKeyArray = new String[messageKeyList.size()];
            messageKeyList.toArray(messageKeyArray);
            jedis.del(messageKeyArray);
            jedis.del(messageListKey);
            String messageUserListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_USER, map);
            String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
            String messageLecturerVoiceListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER_VOICE, map);
            jedis.del(messageUserListKey);
            jedis.del(messageLecturerListKey);
            jedis.del(messageLecturerVoiceListKey);
        }
        //</editor-fold>













        return resultMap;
    }


    //<editor-fold desc="强制结束">
//        Map<String,Object> map = new HashMap<>();
//        map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
//        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
//        Map<String,String> courseMap = jedis.hgetAll(courseKey);
//        long realStartTime = MiscUtils.convertObjectToLong(courseMap.get("real_start_time"));
//
//        boolean processFlag = true;
////        if(type.equals("1") && realStartTime < 1){
////            processFlag = true;
////        }else if(type.equals("2")){
////            if(! courseMap.get("status").equals("2")){
////                processFlag = true;
////            }
////        }
//
//        //如果为未开播课程，则对课程进行结束处理
//        if(processFlag){
//            //1.1如果为课程结束，则取当前时间为课程结束时间
//            Date now = new Date();
//            //1.2更新课程详细信息（变更课程为已经结束）
//            Map<String,Object> course = new HashMap<String,Object>();
////            course.put("course_id", courseId);
////            course.put("end_time", now);
////            course.put("update_time", now);
////            course.put("status", "2");
////            commonModuleServer.updateCourse(course);
//
//            //1.3将该课程从讲师的预告课程列表 SYS: lecturer:{ lecturer_id }：courses  ：prediction移动到结束课程列表 SYS: lecturer:{ lecturer_id }：courses  ：finish
//            map.clear();
//            map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseMap.get("lecturer_id"));
//
//            String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
//            jedis.zrem(lecturerCoursesPredictionKey, courseId);
//
//            String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
//            long  position = MiscUtils.convertObjectToLong(jedis.hget(courseKey, "position"));
//            jedis.zadd(lecturerCoursesFinishKey, MiscUtils.convertInfoToPostion(now.getTime(), position), courseId);
//
//            //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移除。如果存在结束课程列表 SYS：courses ：finish，则增加到课程结束列表
//            jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, courseId);
//            if(jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)){
//                jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, MiscUtils.convertInfoToPostion( now.getTime(),position), courseId);
//            }
//            //将该课程从分类预告课程列表 SYS:COURSES:{classify_id}:PREDICTION 移除. 如果存在判断当前课程是否存在 加入结束列表中 SYS:COURSES:{classify_id}:FINISH
//            map.put(Constants.CACHED_KEY_CLASSIFY,courseMap.get("classify_id"));
//            jedis.zrem(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map), courseId);
//            if(jedis.zrank(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map),courseId)==null){
//                jedis.zadd(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map),  MiscUtils.convertInfoToPostion( now.getTime(),position), courseId);//在结束中增加
//            }
//
//
//
//            //1.5如果课程标记为结束，则清除该课程的禁言缓存数据
//            map.put(Constants.CACHED_KEY_COURSE_FIELD, courseMap.get("lecturer_id"));
//            String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
//            jedis.del(banKey);
//
//            //1.6更新课程缓存信息
//            Map<String, String> updateCacheMap = new HashMap<String, String>();
//            updateCacheMap.put("update_time", MiscUtils.convertObjectToLong(course.get("end_time")) + "");
//            updateCacheMap.put("end_time", MiscUtils.convertObjectToLong(course.get("end_time")) + "");
//            updateCacheMap.put("status", "2");
//            jedis.hmset(courseKey, updateCacheMap);
//
//            Date courseEndTime = new Date();//结束时间
//            reqMap.put("now",courseEndTime);
//            reqMap.put("status","2");
//            commonModuleServer.updateCourse(reqMap);
//
//
//            ////发送结束推送消息
//            SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
//            String str = sdf.format(now);
//            String courseEndMessage = "直播结束于"+str;
//            long currentTime = System.currentTimeMillis();
//            String mGroupId = jedis.hget(courseKey,"im_course_id");
//
//            String sender = "system";
//            String message = courseEndMessage;
//            Map<String,Object> infomation = new HashMap<>();
//            infomation.put("course_id", courseId);
//            infomation.put("creator_id", courseMap.get("lecturer_id"));
//            infomation.put("message", message);
//            infomation.put("message_id",MiscUtils.getUUId());
//            infomation.put("message_imid",infomation.get("message_id"));
//            infomation.put("message_type", "1");
//            infomation.put("send_type", "6");//5.结束消息
//            infomation.put("create_time", currentTime);
//
//
//
//                //课程直播超时结束
//                //1.9极光推送结束消息
//                JSONObject obj = new JSONObject();
//                obj.put("body",String.format(MiscUtils.getConfigKey("jpush_course_live_overtime_force_end"),MiscUtils.RecoveryEmoji(courseMap.get("course_title"))));
//                obj.put("to",courseMap.get("lecturer_id"));
//                obj.put("msg_type","5");
//                Map<String,String> extrasMap = new HashMap<>();
//                extrasMap.put("msg_type","5");
//                extrasMap.put("course_id",courseId);
//                extrasMap.put("im_course_id",courseMap.get("im_course_id"));
//                obj.put("extras_map", extrasMap);
//                JPushHelper.push(obj,appName);
//                infomation.put("is_force",1);
//                infomation.put("tip",MiscUtils.getConfigKey("over_time_message"));
//
//
//            Map<String,Object> messageMap = new HashMap<>();
//            messageMap.put("msg_type","1");
//            messageMap.put("app_name",appName);
//            messageMap.put("send_time",currentTime);
//            messageMap.put("information",infomation);
//            messageMap.put("mid",infomation.get("message_id"));
//            String content = JSON.toJSONString(messageMap);
//            IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);
//
//        }
    //</editor-fold>


//        Map<String,Object> map = new HashMap<>();
//        map.put("course_id",courseId);
//        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//"SYS:COURSE:{course_id}"
//        jedis.del(courseKey);
//        Map<String,Object> updateCourse = new HashMap<>();
//        updateCourse.put("course_id",courseId);
//        updateCourse.put("status","0");
//        Map<String, Object> coursesSumInfo = commonModuleServer.findCoursesSumInfo(map);
//        if(!MiscUtils.isEmpty(coursesSumInfo)){
//            String lecturer_profit = coursesSumInfo.get("lecturer_profit").toString();
//            updateCourse.put("course_amount",Long.valueOf(lecturer_profit));
//
//        }
//        Map<String, Object> courseRecommendUserNum = commonModuleServer.findCourseRecommendUserNum(map);
//        if(!MiscUtils.isEmpty(courseRecommendUserNum)){
//            String recommend_num = courseRecommendUserNum.get("recommend_num").toString();
//            updateCourse.put("student_num",Long.valueOf(recommend_num));
//        }
//        if(!MiscUtils.isEmpty(updateCourse)){
//            commonModuleServer.updateCourse(updateCourse);
//        }


    //<editor-fold desc="课程消息回复">
//        Map<String, Object> map = new HashMap<>();
//        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
//        Map<String,Object> course = new HashMap<String,Object>();
//        course.put("course_id", course_id);
//        course.put("status", "1");
//        String coursekey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, course);//获取课程在缓存中的key
//        jedis.hset(coursekey,"status","1");//把课程缓存中的状态改为已删除
//        commonModuleServer.updateCourse(course);
//        Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id, this.generateRequestEntity(null, null, null, course), readCourseOperation, jedis, true);//从缓存中读取课程信息
//        jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH,course_id);//删除平台结束课程
//        jedis.zrem(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH,courseInfoMap),course_id);//删除分类信息
//        jedis.zrem(MiscUtils.getKeyOfCachedData( Constants.CACHED_KEY_COURSE_FINISH,courseInfoMap),course_id);//老师结束课程
//        jedis.zrem(Constants.CACHED_KEY_COURSE_PREDICTION,course_id);//删除平台结束课程
//        jedis.zrem(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION,courseInfoMap),course_id);//删除分类信息
//        jedis.zrem(MiscUtils.getKeyOfCachedData( Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION,courseInfoMap),course_id);//老师结束课程
//
//        jedis.zadd(MiscUtils.getKeyOfCachedData( Constants.CACHED_KEY_COURSE_PREDICTION,courseInfoMap), MiscUtils.convertInfoToPostion( Long.valueOf(courseInfoMap.get("start_time")),Long.valueOf(courseInfoMap.get("position"))), course_id);//加入老师预告课程
//        jedis.zadd(MiscUtils.getKeyOfCachedData( Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION,courseInfoMap),MiscUtils.convertInfoToPostion( Long.valueOf(courseInfoMap.get("start_time")),Long.valueOf(courseInfoMap.get("position"))), course_id);//分类
//        jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, MiscUtils.convertInfoToPostion( Long.valueOf(courseInfoMap.get("start_time")),Long.valueOf(courseInfoMap.get("position"))), course_id);//平台
//
//        Map <String,Object> queryMap = new HashMap<>();
//        queryMap.put("course_id",course_id);
//        List<Map<String,Object>> messages = commonModuleServer.findCourseMessageListByComm(queryMap);
//        for(Map<String,Object> information:messages ){
//            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            double createTime = Double.parseDouble(format.parse(information.get("create_time").toString()).getTime()+"");
//            String imid = information.get("message_imid").toString();
//            if(information.get("send_type").equals("3") || information.get("send_type").equals("2")){//用户消息
//                String messageQuestionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_USER, map);
//                jedis.zadd(messageQuestionListKey,createTime,imid);
//                //3.如果该条信息为讲师发送的信息，则存入消息-讲师列表
//            }else if(information.get("send_type").equals("0") ||	//老师讲解
//                    information.get("send_type").equals("1")  ||	//老师回答
//                    information.get("send_type").equals("4")  ||	//用户互动
//                    information.get("send_type").equals("5")  ||	//课程开始
//                    information.get("send_type").equals("6")  ||	//结束消息
//                    information.get("send_type").equals("7")){		//老师回复
//                String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
//                jedis.zadd(messageLecturerListKey, createTime, imid);
//                if(information.get("send_type").equals("0") && information.get("message_type").equals("0")){//老师的语音消息
//                    String messageLecturerVoiceListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER_VOICE, map);
//                    jedis.zadd(messageLecturerVoiceListKey, createTime, imid);
//                }
//                if(information.get("send_type").equals("1") || information.get("send_type").equals("7")){//讲师回答 和 讲师回复
//                    Map<String, Object> map1 = JSON.parseObject(information.get("message_question").toString(), HashMap.class);
//                    map1.put("course_id",information.get("course_id"));
//                    String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map1);
//                    jedis.hset(key,"message_status","1");
//                }
//            }
//            map.put(Constants.FIELD_MESSAGE_ID, imid);
//            String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
//            Map<String,String> stringMap = new HashMap<>();
//            MiscUtils.converObjectMapToStringMap(information, stringMap);
//            jedis.hmset(messageKey, stringMap);
//        }
    //</editor-fold>





    /**
     * 恢复课程
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("userGains")
    public Map<String, Object> userGains (RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String appName = reqEntity.getAppName();
        return resultMap;

    }


}
