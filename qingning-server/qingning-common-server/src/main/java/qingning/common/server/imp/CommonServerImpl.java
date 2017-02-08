package qingning.common.server.imp;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.qiniu.storage.BucketManager;
import com.qiniu.storage.model.DefaultPutRet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.server.other.*;
import qingning.common.server.util.DES;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.CacheUtils;
import qingning.common.util.Constants;
import qingning.common.util.IMMsgUtil;
import qingning.common.util.JPushHelper;
import qingning.common.util.MiscUtils;
import qingning.common.util.TenPayConstant;
import qingning.common.util.TenPayUtils;
import qingning.common.util.WeiXinUtil;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ICommonModuleServer;
import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
 
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
        }        
    }
 
    private static Auth auth;
    static {
        auth = Auth.create (IMMsgUtil.configMap.get("qiniu_AK"), IMMsgUtil.configMap.get("qiniu_SK"));
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("logUserInfo")
    public Map<String,Object> collectClientInformation (RequestEntity reqEntity) throws Exception{
        Jedis jedis = jedisUtils.getJedis();
        Map<String,Object> reqMap = (Map<String,Object>)reqEntity.getParam();
        long loginTime = System.currentTimeMillis();        
        if(!"2".equals(reqMap.get("status"))){
            reqMap.put("create_time", loginTime);
            reqMap.put("create_date", MiscUtils.getDate(loginTime));
            if(!MiscUtils.isEmpty(reqEntity.getAccessToken())){
                String user_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
                reqMap.put("user_id", user_id);                
                Map<String,String> userInfo = CacheUtils.readUser(user_id, reqEntity, readUserOperation, jedisUtils);
                reqMap.put("gender", userInfo.get("gender"));
                Map<String,String> queryParam = new HashMap<>();                
                queryParam.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
                String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, queryParam);
                Map<String,String> accessTokenInfo = jedis.hgetAll(accessTokenKey);
                if(MiscUtils.isEmpty(accessTokenInfo)){
                    Map<String, Object> queryMap = new HashMap<String, Object>();                    
                    String login_id = (String)reqMap.get("login_id");
                    String login_type = (String)reqMap.get("login_type");
                    queryMap.put("login_type", login_type);
                    queryMap.put("login_id", login_id);
                    accessTokenInfo = new HashMap<String,String>();
                    MiscUtils.converObjectMapToStringMap(commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap), accessTokenInfo);                    
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
            mqUtils.sendMessage(requestEntity);
        }
        loginTime = System.currentTimeMillis();
        Map<String,Object> resultMap = new HashMap<String, Object>();
        resultMap.put("server_time", System.currentTimeMillis());
 
        Map<String,Object> versionReturnMap = new HashMap<>();
        //增加下发版本号逻辑
        //平台：0： 微信 1：andriod 2:IOS
        if(! "0".equals(reqMap.get("plateform"))){
            Map<String,String> versionInfoMap = CacheUtils.readAppVersion(reqMap.get("plateform").toString(), reqEntity, readAPPVersionOperation, jedisUtils, true);
            if(! MiscUtils.isEmpty(versionInfoMap)){
                //状态 0：关闭 1：开启
                if(versionInfoMap.get("status").equals("1")){
                    if(MiscUtils.isEmpty(reqMap.get("version")) || compareVersion(reqMap.get("plateform").toString(), versionInfoMap.get("version_no"), reqMap.get("version").toString())){
                        Map<String,Object> cacheMap = new HashMap<>();
                        cacheMap.put(Constants.CACHED_KEY_APP_VERSION_INFO_FIELD, reqMap.get("plateform"));
                        String force_version_key = MiscUtils.getKeyOfCachedData(Constants.FORCE_UPDATE_VERSION, cacheMap);
                        ((Map<String, Object>) reqEntity.getParam()).put("force_version_key", force_version_key);
                        Map<String,String> forceVersionInfoMap = CacheUtils.readAppForceVersion(reqMap.get("plateform").toString(), reqEntity, readForceVersionOperation, jedisUtils, true);
                        versionReturnMap.put("is_force","2");
                        if(! MiscUtils.isEmpty(forceVersionInfoMap)){
                            if(MiscUtils.isEmpty(reqMap.get("version")) || compareVersion(reqMap.get("plateform").toString(), versionInfoMap.get("version_no"), reqMap.get("version").toString())){
                                versionReturnMap.put("is_force","1");//是否强制更新  1强制更新  2非强制更新
                            }
                        }
                        versionReturnMap.put("version_no",versionInfoMap.get("version_no"));
                        versionReturnMap.put("update_desc",versionInfoMap.get("update_desc"));
                        versionReturnMap.put("version_url",versionInfoMap.get("version_url"));
                        resultMap.put("version_info", versionReturnMap);
                    }
                }
            }
        }

        return resultMap;
    }

    //客户版本号小于系统版本号 true， 否则为false
    private boolean compareVersion(String plateform, String systemVersion, String customerVersion) {
        //1：andriod 2:IOS
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
        Map<String,Object> resultMap = new HashMap<>();
        Map<String,Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(reqMap);
 
        int login_type_input = Integer.parseInt(reqMap.get("login_type").toString());
 
        switch (login_type_input){
 
            case 0 : //微信登录
                //如果登录信息为空，则进行注册
                if(loginInfoMap == null){
                    //注册IM
                    Jedis jedis = jedisUtils.getJedis();
                    Map<String,String> imResultMap = null;
                    try {
                        imResultMap =    IMMsgUtil.createIMAccount(reqMap.get("device_id").toString());
                    }catch (Exception e){
                        //TODO 暂不处理
                    }
                    //if(imResultMap == null || imResultMap.get("uid") == null || imResultMap.get("password") == null){
                        //throw new QNLiveException("120003");
                    //}else {
                        //初始化数据库相关表
                        reqMap.put("m_user_id", imResultMap.get("uid"));
                        reqMap.put("m_pwd", imResultMap.get("password"));
                        //设置默认用户头像
                        String transferAvatarAddress = (String)reqMap.get("avatar_address");
                        if(!MiscUtils.isEmpty(transferAvatarAddress)){
                            try{
                                transferAvatarAddress = qiNiuFetchURL(reqMap.get("avatar_address").toString());
                            } catch(Exception e){
                                transferAvatarAddress = null;
                            }
                        }
                        if(MiscUtils.isEmpty(transferAvatarAddress)){
                            transferAvatarAddress = MiscUtils.getConfigByKey("default_avatar_address");
                        }
                        
                        reqMap.put("avatar_address",transferAvatarAddress);
                        
                        if(reqMap.get("nick_name") == null || StringUtils.isBlank(reqMap.get("nick_name").toString())){
                            reqMap.put("avatar_address","用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
                        }
                        Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
 
                        //生成access_token，将相关信息放入缓存，构造返回参数
                        processLoginSuccess(1, dbResultMap, null, resultMap);
                    //}
 
                }else{
                    //构造相关返回参数 TODO
                    processLoginSuccess(2, null, loginInfoMap, resultMap);
                }
                break;
 
            case 1 : //QQ登录
                //TODO
                break;
 
            case 2 : //手机号登录
                if(loginInfoMap == null){
                    //抛出用户不存在
                    throw new QNLiveException("120002");
                }else {
                    //校验用户名和密码
                    //登录成功
                    if(reqMap.get("certification").toString().equals(loginInfoMap.get("passwd").toString())){
                        //构造相关返回参数 TODO
                        processLoginSuccess(2, null, loginInfoMap, resultMap);
                    }else{
                        //抛出用户名或者密码错误
                        throw new QNLiveException("120001");
                    }
                }
                break;
 
        }
 
        return resultMap;
    }
 
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
        String code = reqMap.get("login_id").toString();
        //1.传递授权code及相关参数，调用微信验证code接口
        JSONObject getCodeResultJson = WeiXinUtil.getUserInfoByCode(code);
        if(getCodeResultJson == null || getCodeResultJson.getInteger("errcode") != null || getCodeResultJson.getString("openid") == null){
            throw new QNLiveException("120008");
        }
 
        //1.2如果验证成功，则得到用户的union_id和用户的access_token。
        //1.2.1根据 union_id查询数据库
        String openid = getCodeResultJson.getString("openid");
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("login_type","4");//4.微信code方式登录
        queryMap.put("web_openid",openid);
        Map<String,Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);
 
        //1.2.1.1如果用户存在则进行登录流程
        if(loginInfoMap != null){
            processLoginSuccess(2, null, loginInfoMap, resultMap);
            return resultMap;
        }else {
            //1.2.1.2如果用户不存在，则根据用户的open_id和用户的access_token调用微信查询用户信息接口，得到用户的头像、昵称等相关信息
            String userWeixinAccessToken = getCodeResultJson.getString("access_token");
            JSONObject userJson = WeiXinUtil.getUserInfoByAccessToken(userWeixinAccessToken, openid);
            // 根据得到的相关用户信息注册用户，并且进行登录流程。
            if(userJson == null || userJson.getInteger("errcode") != null || userJson.getString("unionid") == null){
                throw new QNLiveException("120008");
            }
 
            queryMap.clear();
            queryMap.put("login_type","0");//0.微信方式登录
            queryMap.put("login_id",userJson.getString("unionid"));
            Map<String,Object> loginInfoMapFromUnionid = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);
            if(loginInfoMapFromUnionid != null){
                //将open_id更新到login_info表中
                Map<String,Object> updateMap = new HashMap<>();
                updateMap.put("user_id", loginInfoMapFromUnionid.get("user_id").toString());
                updateMap.put("web_openid", openid);
                commonModuleServer.updateUserWebOpenIdByUserId(updateMap);
                processLoginSuccess(2, null, loginInfoMapFromUnionid, resultMap);
                return resultMap;
            }
 
            String nickname = userJson.getString("nickname");
            String sex = userJson.getString("sex");
            String headimgurl = userJson.getString("headimgurl");
 
            Jedis jedis = jedisUtils.getJedis();
            Map<String,String> imResultMap = null;
            try {
                imResultMap = IMMsgUtil.createIMAccount("weixinCodeLogin");
            }catch (Exception e){
                //TODO 暂不处理
            }
            //if(imResultMap == null || imResultMap.get("uid") == null || imResultMap.get("password") == null){
            //throw new QNLiveException("120003");
            //}else {
            //初始化数据库相关表
            reqMap.put("m_user_id", imResultMap.get("uid"));
            reqMap.put("m_pwd", imResultMap.get("password"));
            //设置默认用户头像
            if(MiscUtils.isEmpty(headimgurl)){
                reqMap.put("avatar_address",MiscUtils.getConfigByKey("default_avatar_address"));//TODO
            }else {
                String transferAvatarAddress = qiNiuFetchURL(headimgurl);
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
            reqMap.put("login_type","4");
            Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
 
            //生成access_token，将相关信息放入缓存，构造返回参数
            processLoginSuccess(1, dbResultMap, null, resultMap);
            //}
 
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
                                     Map<String,Object> resultMap) throws Exception{
 
        Jedis jedis = jedisUtils.getJedis();
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
        jedis.expire(process_access_token, 10800);
        
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("user_id", user_id);        
        Map<String, String> userMap = CacheUtils.readUser(user_id, this.generateRequestEntity(null, null, null, query), readUserOperation, jedisUtils);
        
        //3.增加相关返回参数
        resultMap.put("access_token", access_token);
        resultMap.put("im_account_info", encryptIMAccount(m_user_id, m_pwd));
        resultMap.put("m_user_id", m_user_id);
        resultMap.put("user_id", user_id);
                
        resultMap.put("avatar_address", userMap.get("avatar_address"));
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
//            StringMap putPolicy = new StringMap()
//                    .put("persistentPipeline","qnlive-audio-convert")//设置私有队列处理
//                    .put("persistentOps", "avthumb/mp3/ab/64k")
//                    .put("persistentNotifyUrl",MiscUtils.getConfigByKey("qiniu-audio-transfer-persistent-notify-url"));//转码策略
 
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
        String JSApiTIcket = WeiXinUtil.getJSApiTIcket(jedisUtils.getJedis());
        return WeiXinUtil.sign(JSApiTIcket, reqMap.get("url").toString());
    }
 
 
    @SuppressWarnings("unchecked")
    @FunctionName("userInfo")
    public Map<String,Object> getUserInfo (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<String, Object>();
 
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //1:个人中心信息 2：个人基本信息
        String queryType = reqMap.get("query_type").toString();
        Map<String,String> values = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedisUtils);
        if(queryType.equals("1")){
            reqMap.put("user_id", userId);
            
            resultMap.put("avatar_address", values.get("avatar_address"));
            resultMap.put("nick_name", values.get("nick_name"));            
            resultMap.put("course_num", MiscUtils.convertObjToObject(values.get("course_num"), Constants.SYSLONG, "course_num", 0l));
            resultMap.put("live_room_num", MiscUtils.convertObjToObject(values.get("live_room_num"), Constants.SYSLONG, "live_room_num", 0l));
            resultMap.put("today_distributer_amount",MiscUtils.convertObjToObject(values.get("today_distributer_amount"), Constants.SYSLONG, "today_distributer_amount", 0l));
            resultMap.put("update_time", MiscUtils.convertObjToObject(values.get("update_time"),Constants.SYSLONG,"update_time", 0l));            
        }else if(queryType.equals("2")){            
            if(MiscUtils.isEmpty(values)){
                throw new QNLiveException("120002");
            }
            Map<String,Object> query = new HashMap<String,Object>();
            query.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, query);            
            Map<String,String> loginInfoMap = jedisUtils.getJedis().hgetAll(key);
            if(MiscUtils.isEmpty(loginInfoMap)){
                throw new QNLiveException("120002");
            }
 
            resultMap.put("access_token", reqEntity.getAccessToken());
            resultMap.put("im_account_info", encryptIMAccount(loginInfoMap.get("m_user_id").toString(), loginInfoMap.get("m_pwd").toString()));
            resultMap.put("m_user_id", loginInfoMap.get("m_user_id"));
            resultMap.put("user_id", userId);
            resultMap.put("avatar_address", values.get("avatar_address"));
            resultMap.put("nick_name", values.get("nick_name"));
            return resultMap;
        }
 
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("generateWeixinPayBill")
    public Map<String,String> generateWeixinPayBill (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
 
        //1.检测课程是否存在，课程不存在则给出提示（ 课程不存在，120009）
        String courseId = reqMap.get("course_id").toString();       
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("course_id", courseId);
        Map<String,String> courseMap = CacheUtils.readCourse(courseId, generateRequestEntity(null, null, null, query), readCourseOperation, jedisUtils, false);
       
        if(MiscUtils.isEmpty(courseMap)){            
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
            goodName = new String(MiscUtils.getConfigByKey("weixin_pay_reward_course_good_name").getBytes(), "UTF-8")+courseMap.get("course_id");
        }else if(profit_type.equals("0")){
            insertMap.put("amount", courseMap.get("course_price"));
            totalFee = Integer.parseInt(courseMap.get("course_price"));
            goodName = new String(MiscUtils.getConfigByKey("weixin_pay_buy_course_good_name").getBytes(), "UTF-8")+courseMap.get("course_id");
        }
        insertMap.put("status","0");
        String tradeId = MiscUtils.getUUId();
        insertMap.put("trade_id",tradeId);
        insertMap.put("profit_type",profit_type);
        commonModuleServer.insertTradeBill(insertMap);
        
        query.clear();        
        query.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, query);
        Map<String,String> userMap =jedisUtils.getJedis().hgetAll(key);
 
        //4.调用微信生成预付单接口
        String terminalIp = reqMap.get("remote_ip_address").toString();
        String tradeType = "JSAPI";
        String outTradeNo = tradeId;
        String openid = userMap.get("web_openid");
        Map<String, String> payResultMap = TenPayUtils.sendPrePay(goodName, totalFee, terminalIp, tradeType, outTradeNo, openid);
 
        //5.处理生成微信预付单接口
        if (payResultMap.get ("return_code").equals ("FAIL")) {
            //更新交易表
            Map<String,Object> failUpdateMap = new HashMap<>();
            failUpdateMap.put("status","3");
            failUpdateMap.put("close_reason","生成微信预付单失败 "+payResultMap.get("return_msg"));
            failUpdateMap.put("trade_id",tradeId);
            commonModuleServer.closeTradeBill(failUpdateMap);
 
            throw new QNLiveException("120015");
        } else if (payResultMap.get ("result_code").equals ("FAIL")) {
            //更新交易表
            Map<String,Object> failUpdateMap = new HashMap<>();
            failUpdateMap.put("status","3");
            failUpdateMap.put("close_reason","生成微信预付单失败 "+ payResultMap.get ("err_code_des"));
            failUpdateMap.put("trade_id",tradeId);
            commonModuleServer.closeTradeBill(failUpdateMap);
 
            throw new QNLiveException("120015");
        } else {
            //成功，则需要插入支付表
            Map<String,Object> insertPayMap = new HashMap<>();
            insertPayMap.put("trade_id",tradeId);
            insertPayMap.put("payment_id",MiscUtils.getUUId());
            insertPayMap.put("payment_type",profit_type);
            insertPayMap.put("status","1");
            insertPayMap.put("pre_pay_no",payResultMap.get("prepay_id"));
            insertPayMap.put("create_time",new Date());
            commonModuleServer.insertPaymentBill(insertPayMap);
 
            //返回相关参数给前端.
            SortedMap<String,String> resultMap = new TreeMap<>();
            resultMap.put("appId",MiscUtils.getConfigByKey("appid"));
            resultMap.put("nonceStr", payResultMap.get("random_char"));
            resultMap.put("package", "prepay_id="+payResultMap.get("prepay_id"));
            resultMap.put("signType", "MD5");
            resultMap.put("timeStamp",System.currentTimeMillis()/1000 + "");
            String paySign  = TenPayUtils.getSign(resultMap);
            resultMap.put ("paySign", paySign);
 
            return resultMap;
        }
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("handleWeixinPayResult")
    public String handleWeixinPayResult (RequestEntity reqEntity) throws Exception{
        String resultStr = TenPayConstant.FAIL;
        SortedMap<String,String> requestMapData = (SortedMap<String,String>)reqEntity.getParam();
        String outTradeNo = requestMapData.get("out_trade_no");
 
        Map<String,Object> billMap = commonModuleServer.findTradebillByOutTradeNo(outTradeNo);
        if(billMap != null && billMap.get("status").equals("2")){
            logger.debug("====>　已经处理完成, 不需要继续。流水号是: " + outTradeNo);
            return TenPayConstant.SUCCESS;
        }
 
        if (TenPayUtils.isValidSign(requestMapData)){// MD5签名成功，处理课程打赏\购买课程等相关业务
        //if(true){
            logger.debug(" ===> 微信notify Md5 验签成功 <=== ");
 
            if("SUCCESS".equals(requestMapData.get("return_code")) &&
                    "SUCCESS".equals(requestMapData.get("result_code"))){
                Jedis jedis = jedisUtils.getJedis();
 
                String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());                
                //1.先检测课程存在情况和状态
                String courseId = (String)billMap.get("course_id");       
                Map<String,Object> query = new HashMap<String,Object>();
                query.put("course_id", courseId);
                Map<String,String> courseMap = CacheUtils.readCourse(courseId, generateRequestEntity(null, null, null, query), readCourseOperation, jedisUtils, false);
                if(MiscUtils.isEmpty(courseMap)){
                	throw new QNLiveException("100004");
                }
                String courseKey  = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, query);
                //更新交易表信息
                //更新支付表信息
                //更新收益表信息
                //如果为购买课程，则插入学员表
				Map<String,Object> requestValues = new HashMap<String,Object>();
				for(String key : requestMapData.keySet()){
					requestValues.put(key, requestMapData.get(key));
				}
				requestValues.put("courseInCache", courseMap);
				requestValues.put("tradeBillInCache", billMap);
				String profit_type = (String)billMap.get("profit_type");
				Map<String,String> distributeRoom = null;
				if("0".equals(profit_type)){
					//t_room_distributer
					query.clear();
					query.put("room_id", billMap.get("room_id"));
					query.put("user_id", billMap.get("user_id"));
					query.put("today_end_date", MiscUtils.getEndDateOfToday().getTime());
					Map<String,Object> recommendMap = commonModuleServer.findRoomDistributerRecommendAllInfo(query);
					if(!MiscUtils.isEmpty(recommendMap)){						
						distributeRoom = CacheUtils.readDistributerRoom((String)recommendMap.get(Constants.CACHED_KEY_DISTRIBUTER_FIELD), 
								(String)billMap.get("room_id"), readRoomDistributer, jedisUtils);
						requestValues.put("roomDistributerCache", billMap);
					}
					
					
					
				}
				Map<String,Object> handleResultMap = commonModuleServer.handleWeixinPayResult(requestValues);
				
				if(!MiscUtils.isEmpty(distributeRoom)){
					query.clear();
					query.put("room_id", billMap.get("room_id"));
					query.put("distributer_id", handleResultMap.get("distributer_id"));
					String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, query);
					jedis.hincrBy(key,"done_num",1);
					jedis.hincrBy(key,"total_amount",(Long)handleResultMap.get("profit_amount"));
					jedis.hincrBy(key,"done_time",MiscUtils.convertObjectToLong(handleResultMap.get("create_time")));
				}
				
				//如果为打赏，则需要发送推送
			
                //0:课程收益 1:打赏
				query.clear();
				query.put(Constants.CACHED_KEY_LECTURER_FIELD, handleResultMap.get("lecturer_id").toString());
                String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query);
 
                //处理缓存中的收益
                //讲师缓存中的收益
                Long amountLong = (Long)handleResultMap.get("pay_amount");
                jedis.hincrBy(lecturerKey, "total_amount", amountLong.longValue());
                
                query.clear();
                String lecturerId = courseMap.get("lecturer_id");
                query.put("lecturer_id", lecturerId);
                Map<String,String> lecturerMap = CacheUtils.readLecturer(lecturerId, this.generateRequestEntity(null, null, null, query), readLecturerOperation, jedisUtils);
                
                if(profit_type.equals("1")){
                    String mGroupId = handleResultMap.get("im_course_id").toString();
                    query.clear();
                    query.put("user_id", handleResultMap.get("user_id"));        
                    Map<String, String> payUserMap = CacheUtils.readUserNoCache((String)handleResultMap.get("user_id"), this.generateRequestEntity(null, null, null, query), readUserOperation, jedisUtils);
                    
                    String message = payUserMap.get("nick_name") + "打赏了" + lecturerMap.get("nick_name") + (Long)handleResultMap.get("profit_amount")/100.0 + "元";
                    long currentTime = System.currentTimeMillis();
                    String sender = "system";
                    Map<String,Object> infomation = new HashMap<>();
                    infomation.put("course_id", handleResultMap.get("course_id"));
					infomation.put("creator_id", lecturerId);
                    //TODO check it , infomation.put("creator_id", lecturerMap.get(handleResultMap.get("lecturer_id").toString()));
                    infomation.put("message", message);
                    infomation.put("send_type", "4");//4.打赏信息
                    infomation.put("message_type", "1");
                    infomation.put("create_time", currentTime);
                    Map<String,Object> messageMap = new HashMap<>();
                    messageMap.put("msg_type","1");
                    messageMap.put("send_time",currentTime);
                    messageMap.put("information",infomation);
                    messageMap.put("mid",MiscUtils.getUUId());
                    String content = JSON.toJSONString(messageMap);
                    IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);
 
                    if(jedis.exists(courseKey)) {
                        Map<String,Object> rewardQueryMap = new HashMap<>();
                        rewardQueryMap.put("course_id",courseMap.get("course_id"));
                        rewardQueryMap.put("user_id",billMap.get("user_id"));
                        Map<String,Object> rewardMap = commonModuleServer.findRewardByUserIdAndCourseId(rewardQueryMap);
 
                        if(MiscUtils.isEmpty(rewardMap)){
                            jedis.hincrBy(courseKey, "extra_num", 1);
                            jedis.hincrBy(courseKey, "extra_amount", amountLong.longValue());
                        }
                    }
 
                }else if(profit_type.equals("0")){
                    Long nowStudentNum = 0L;
                    //增加课程人数
                    jedis.hincrBy(lecturerKey, "total_student_num", 1);                    
                    jedis.hincrBy(lecturerKey, "pay_student_num", 1);                    
                    if(jedis.exists(courseKey)) {
                        jedis.hincrBy(courseKey, "student_num", 1);
                        jedis.hincrBy(courseKey, "course_amount", amountLong.longValue());
                    }
 
                    //修改用户缓存信息中的加入课程数
                    query.clear();
                    query.put(Constants.CACHED_KEY_USER_FIELD, userId);
                    String userCacheKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query);
                    if(jedis.exists(userCacheKey)){
                        jedis.hincrBy(userCacheKey, "course_num", 1L);
                    }else {
                        CacheUtils.readUser(userId, reqEntity, readUserOperation,jedisUtils);
                        jedis.hincrBy(userCacheKey, "course_num", 1L);
                    }
 
                    nowStudentNum = Long.parseLong(courseMap.get("student_num")) + 1;
                    String levelString = MiscUtils.getConfigByKey("jpush_course_students_arrive_level");
                    JSONArray levelJson = JSON.parseArray(levelString);
                    if(levelJson.contains(nowStudentNum+"")){
                        JSONObject obj = new JSONObject();
                        String course_type = courseMap.get("course_type");
                        String course_type_content = MiscUtils.convertCourseTypeToContent(course_type);
                        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content"), course_type_content, courseMap.get("course_title"), nowStudentNum+""));
                        obj.put("to", courseMap.get("lecturer_id"));
                        obj.put("msg_type","7");
                        Map<String,String> extrasMap = new HashMap<>();
                        extrasMap.put("msg_type","7");
                        extrasMap.put("course_id",courseMap.get("course_id"));
                        obj.put("extras_map", extrasMap);
                        JPushHelper.push(obj);
                    }
                }
 
                //直播间缓存中的收益
                query.clear();
                //query.put(Constants.CACHED_KEY_LECTURER_FIELD, handleResultMap.get("lecturer_id").toString());
                query.put(Constants.FIELD_ROOM_ID, handleResultMap.get("room_id").toString());
                String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, query);
                jedis.hincrBy(liveRoomKey, "total_amount", amountLong.longValue());
                //TODO  last_course_amount
 
                resultStr = TenPayConstant.SUCCESS;
                logger.debug("====> 微信支付流水: " + outTradeNo + " 更新成功, return success === ");
 
                String user_id = (String) billMap.get("user_id");
                String course_id = (String) billMap.get("course_id");
                
                
                if (!StringUtils.isBlank(user_id)) {
                    Map<String, Object> loginInfoUser = commonModuleServer.findLoginInfoByUserId(user_id);
                    String openId=(String) loginInfoUser.get("web_openid");
                    //  成员报名付费通知 老师 暂时 不需要了
//                    wpushLecture(billMap, jedis, openId, courseByCourseId, user);
                    //TODO 付费报名成功通知学员
                    wpushUser(jedis, openId, courseMap, lecturerMap,course_id);
                
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
 
 
    private void wpushUser(Jedis jedis, String openId,
            Map<String, String> courseByCourseId,
            Map<String, String> lecturerUser,String courseId) {
        Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
        TemplateData first = new TemplateData();
        first.setColor("#000000");
        first.setValue(MiscUtils.getConfigByKey("wpush_shop_course_first"));
        templateMap.put("first", first);
        
        Date start_time = new Date(MiscUtils.convertObjectToLong(courseByCourseId.get("start_time")));
        TemplateData orderNo = new TemplateData();
        orderNo.setColor("#000000");
        orderNo.setValue(MiscUtils.parseDateToFotmatString(start_time, "yyyy-MM-dd hh:mm:ss"));
        templateMap.put("keyword1", orderNo);
        
        TemplateData wuliu = new TemplateData();
        wuliu.setColor("#000000");
        wuliu.setValue(lecturerUser.get("nick_name"));
        templateMap.put("keyword2", wuliu);    
 
        TemplateData name = new TemplateData();
        name.setColor("#000000");
        name.setValue(courseByCourseId.get("course_title"));
        templateMap.put("keyword3", name);
 
        TemplateData remark = new TemplateData();
        remark.setColor("#000000");
        remark.setValue(MiscUtils.getConfigByKey("wpush_shop_course_remark"));
        templateMap.put("remark", remark);
        String url = MiscUtils.getConfigByKey("course_share_url_pre_fix")+courseId;
        WeiXinUtil.send_template_message(openId, MiscUtils.getConfigByKey("wpush_shop_course"),url, templateMap, jedis);
    }
 
 
//    private void wpushLecture(Map<String, Object> billMap, Jedis jedis, String openId,
//            Map<String, Object> courseByCourseId, Map<String, Object> user) {
//        Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
//        TemplateData first = new TemplateData();
//        first.setColor("#000000");
//        first.setValue(String.format(MiscUtils.getConfigByKey("wpush_shop_course_remark"),user.get("nick_name")));
//        templateMap.put("first", first);
//        
//        TemplateData name = new TemplateData();
//        name.setColor("#000000");
//        name.setValue((String) courseByCourseId.get("course_title"));
//        templateMap.put("keyword1", name);
// 
//        TemplateData wuliu = new TemplateData();
//        wuliu.setColor("#000000");
//        wuliu.setValue(billMap.get("payment").toString());
//        templateMap.put("keyword2", wuliu);    
// 
//        TemplateData orderNo = new TemplateData();
//        orderNo.setColor("#000000");
//        orderNo.setValue(MiscUtils.parseDateToFotmatString(new Date(), "yyyy-MM-dd hh:mm:ss"));
//        templateMap.put("keyword3", orderNo);
// 
//        TemplateData remark = new TemplateData();
//        remark.setColor("#000000");
//        remark.setValue(MiscUtils.getConfigByKey("wpush_lecture_shop_course_remark"));
//        templateMap.put("remark", remark);
//        WeiXinUtil.send_template_message(openId, MiscUtils.getConfigByKey("wpush_lecture_shop_course"),"#", templateMap, jedis);
//    }
    
    @FunctionName("commonDistribution")
    public Map<String,Object> getCommonDistribution(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //int page_count = (Integer)reqMap.get("page_count");
        Date record_date = (Date)reqMap.get("record_date");
        reqMap.put("create_time", record_date);
        reqMap.put("distributer_id", userId);
        Map<String,String> distributer = CacheUtils.readDistributer(userId, reqEntity, readDistributerOperation, jedisUtils, true);
        if(MiscUtils.isEmpty(distributer)){
            throw new QNLiveException("120012");
        }
        Map<String,Object> resultMap = new HashMap<String, Object>();
        resultMap.put("total_amount", distributer.get("total_amount"));
        List<Map<String,Object>> rooom_list = commonModuleServer.findDistributionInfoByDistributerId(reqMap);
        if(!MiscUtils.isEmpty(rooom_list)){
            Date currentDate = new Date(System.currentTimeMillis());
            for(Map<String,Object> values:rooom_list){
                Date endDate = (Date)values.get("end_date");
                if(!MiscUtils.isEmpty(endDate) && endDate.before(currentDate)){
                    values.put("effective_time", null);
                }
            }
        }
        resultMap.put("room_list", rooom_list);
        return resultMap;
    }
    
    @FunctionName("roomDistributerRecommendInfo")
    public Map<String,Object> getRoomDistributerRecommendInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        Map<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("room_id", room_id);
        String distributer_id = (String)reqMap.get("distributer_id");
        boolean isLecturer = false;
        if(MiscUtils.isEmpty(distributer_id)){
            distributer_id=userId;                    
        } else {
            parameters.put("lecturer_id", userId);
            isLecturer=true;
        }
        parameters.put("distributer_id", distributer_id);
        List<Map<String,Object>> rooom_list = commonModuleServer.findDistributionInfoByDistributerId(parameters);
        Map<String,Object> result = new HashMap<String,Object>();
        long recommend_num = 0l;
        if(MiscUtils.isEmpty(rooom_list)){
            if(isLecturer){
                throw new QNLiveException("100028");
            }
            result.put("recommend_num", 0l);
        } else {
            Object recommend_num_tmp = rooom_list.get(0).get("recommend_num");
            if(recommend_num_tmp!=null){
                recommend_num = Long.parseLong(recommend_num_tmp.toString());
            }
            result.put("recommend_num", recommend_num);
        }
        if(recommend_num>0){
            parameters.clear();
            parameters.put("room_id", room_id);
            parameters.put("distributer_id", distributer_id);
            parameters.put("page_count", reqMap.get("page_count"));
            parameters.put("position", reqMap.get("position"));
            List<Map<String,Object>> recommend_list = commonModuleServer.findRoomDistributerRecommendInfo(parameters);
            result.put("recommend_list", recommend_list);
        }
        return result;
    }
    
    @FunctionName("roomDistributionInfo")
    public Map<String,Object> getRoomDistributionInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");                
        reqMap.put("distributer_id", userId);
        Map<String,String> distributer = CacheUtils.readDistributer(userId, reqEntity, readDistributerOperation, jedisUtils, true);
        if(MiscUtils.isEmpty(distributer)){
            throw new QNLiveException("120012");
        }
        Map<String,Object> result = new HashMap<String,Object>();
        result.put("total_amount", distributer.get("total_amount"));
        
        Map<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("room_id", room_id);
        parameters.put("distributer_id", userId);
        parameters.put("page_count", reqMap.get("page_count"));
        parameters.put("start_time", reqMap.get("start_time"));
        
        List<Map<String,Object>> course_list = commonModuleServer.findRoomDistributerCourseInfo(parameters);
        if(!MiscUtils.isEmpty(course_list)){
            Date currentDate = new Date(System.currentTimeMillis());
            for(Map<String,Object> values:course_list){
                Date endDate = (Date)values.get("end_date");
                if(!MiscUtils.isEmpty(endDate) && endDate.before(currentDate)){
                    values.put("effective_time", null);
                }
            }
        }
        result.put("course_list", course_list);
        return result;
    }
    
    @FunctionName("courseDistributionInfo")
    public Map<String,Object> getCourseDistributionInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String course_id = (String)reqMap.get("course_id");
        Long position = (Long)reqMap.get("position");
        
        Map<String,Object> parameters = new HashMap<String,Object>();
        parameters.put("course_id", course_id);
        parameters.put("distributer_id", userId);        
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
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("distributer_id", userId);        
        List<Map<String,Object>> list = commonModuleServer.findDistributionInfoByDistributerId(reqMap);
        if(MiscUtils.isEmpty(list)){
            throw new QNLiveException("120014");
        }
        return list.get(0);
    }
    
    @FunctionName("updateUserInfo")
    public Map<String,Object> updateUserInfo(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        
        reqMap.put("user_id", userId);
        Map<String,String> values = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedisUtils);
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
                    jedisUtils.getJedis().hmset(cachedKey, cachedValues);
                    parameter.clear();
                    parameter.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
                    cachedKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, parameter);
                    if(jedisUtils.getJedis().exists(cachedKey)){
                    	Map<String,String> updateCacedValue = new HashMap<String,String>();
                    	if(nick_name != null){
                    		updateCacedValue.put("nick_name", nick_name);
                    	}
                    	if(avatar_address != null){
                    		updateCacedValue.put("avatar_address", avatar_address);
                    	}
                    	if(!MiscUtils.isEmpty(updateCacedValue)){
                    		jedisUtils.getJedis().hmset(cachedKey, updateCacedValue);
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
        Map<String,Object> resultMap = new HashMap<>();
 
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String courseId = reqMap.get("course_id").toString();
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("user_id", userId);        
        Map<String, String> userMap = CacheUtils.readUser(userId, this.generateRequestEntity(null, null, null, query), readUserOperation, jedisUtils);
        resultMap.put("avatar_address",userMap.get("avatar_address"));
        resultMap.put("nick_name",userMap.get("nick_name"));
 
        Map<String,String> courseMap =  CacheUtils.readCourse(courseId, reqEntity, readCourseOperation, jedisUtils, false);
        resultMap.put("course_title",courseMap.get("course_title"));
        resultMap.put("start_time",courseMap.get("start_time"));
        resultMap.put("share_url",MiscUtils.getConfigByKey("course_share_url_pre_fix")+courseId);
 
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
     @FunctionName("getRoomInviteCard")
     public Map<String,Object> getRoomInviteCard (RequestEntity reqEntity) throws Exception{//TODO
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<>();
 
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        
        Map<String,Object> query = new HashMap<String,Object>();
        query.put("user_id", userId);        
        Map<String, String> userMap = CacheUtils.readUser(userId, this.generateRequestEntity(null, null, null, query), readUserOperation, jedisUtils);
        
        String roomId = reqMap.get("room_id").toString();
        
        resultMap.put("avatar_address",userMap.get("avatar_address"));
        resultMap.put("nick_name",userMap.get("nick_name"));
 
        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(roomId,reqEntity,readLiveRoomOperation,jedisUtils,true);
        resultMap.put("room_name",liveRoomMap.get("room_name"));
 
        //查询该用户是否为该直播间的分销员
        String share_url = getLiveRoomShareURL(userId, roomId);
 
        resultMap.put("share_url",share_url);
        return resultMap;
    }
 
    private String getLiveRoomShareURL(String userId, String roomId) {
        String share_url ;
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("distributer_id", userId);
        queryMap.put("room_id", roomId);
		Map<String, Object>  roomDistributer = commonModuleServer.findRoomDistributionInfoByDistributerId(queryMap);

		boolean isDistributer = false;
		String recommend_code = null;
		//long now = MiscUtils.getEndDateOfToday().getTime();
		if (! MiscUtils.isEmpty(roomDistributer)) {
			isDistributer=true;
		}


 
        //是分销员
        if(isDistributer == true){
            share_url = MiscUtils.getConfigByKey("live_room_share_url_pre_fix")+roomId+"&recommend_code="+recommend_code;
        }else {
            //不是分销员
            share_url = MiscUtils.getConfigByKey("live_room_share_url_pre_fix")+roomId;
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
        Jedis jedis = jedisUtils.getJedis();
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
        		distributerRoom = CacheUtils.readDistributerRoom(userId, room_id, readRoomDistributer, jedisUtils);
        	}
        }
        if(!find){
        	Map<String,Object> roomDistributerMap = commonModuleServer.findRoomDistributerInfoByRqCode(rqCode);
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
                String room_id = (String)distributerRoom.get("room_id");
                Map<String,Object> query = new HashMap<String,Object>();
                query.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, lecturerId);
        		query.put(Constants.FIELD_ROOM_ID, room_id);
                String distributerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, queryParam);
                
                		
                		
                //2.如果访问该推广链接的用户不是讲师,不是分销员，则进行下一步验证，验证用户目前是否已经属于了有效分销员
                if((! userId.equals(lecturerId)) && (! userId.equals( distributerRoom.get("distributer_id").toString()))){
                    Map<String,Object> queryMap = new HashMap<>();
                    queryMap.put("room_id", room_id);
                    queryMap.put("user_id", userId);
                    queryMap.put("today_end_date", todayEnd.getTime());
                    Map<String,Object> roomDistributerRecommendMap = commonModuleServer.findRoomDistributerRecommendAllInfo(queryMap);
 
                    //3.如果该用户没有成为有效分销员的推荐用户，则该用户成为该分销员的推荐用户
                    if(MiscUtils.isEmpty(roomDistributerRecommendMap)){
                        Map<String,Object> insertMap = new HashMap<>();
                        insertMap.put("distributer_recommend_id", MiscUtils.getUUId());
                        insertMap.put("distributer_id", distributerRoom.get("distributer_id"));
                        insertMap.put("room_id", room_id);
                        insertMap.put("user_id", userId);
                        insertMap.put("end_date", end_date);
                        insertMap.put("rq_code", rqCode);
                        insertMap.put("now", now);
                        commonModuleServer.insertRoomDistributerRecommend(insertMap);
 
                        //4.直播间分销员的推荐人数增加一                        
                        jedis.hincrBy(distributerKey, "click_num", 1);
                        jedis.hincrBy(distributerKey, "recommend_num", 1);
                        jedis.hincrBy(distributerKey, "last_recommend_num", 1);                        
                        jedis.sadd(Constants.CACHED_UPDATE_RQ_CODE_KEY, rqCode);
                       /* Map<String,Object> updateMap = new HashMap<>();
                        updateMap.put("distributer_id",roomDistributerMap.get("distributer_id").toString());
                        updateMap.put("room_id",room_id);
                        commonModuleServer.increteRecommendNumForRoomDistributer(updateMap);*/
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
        Map<String,Object> resultMap = new HashMap<>();
 
//        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //1.将多媒体server_id通过微信接口，得到微信资源访问链接
        String mediaUrl = WeiXinUtil.getMediaURL(reqMap.get("media_id").toString(),jedisUtils.getJedis());
        //2.调用七牛fetch将微信资源访问链接转换为七牛图片链接
        String fetchURL = qiNiuFetchURL(mediaUrl);
 
        resultMap.put("url", fetchURL);
        return resultMap;
    }
 
    private String qiNiuFetchURL(String mediaUrl) throws Exception{
        BucketManager bucketManager = new BucketManager(auth);
        String bucket = MiscUtils.getConfigByKey("image_space");
        String key = Constants.WEB_FILE_PRE_FIX + MiscUtils.parseDateToFotmatString(new Date(),"yyyyMMddHH")+MiscUtils.getUUId();
        DefaultPutRet result = bucketManager.fetch(mediaUrl, bucket,key);
        String imageUrl = MiscUtils.getConfigByKey("images_space_domain_name") + "/"+key;
        return imageUrl;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("getShareInfo")
    public Map<String,Object> getShareInfo (RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
        Map<String,Object> resultMap = new HashMap<>();
        String query_type = reqMap.get("query_type").toString();
        String id = reqMap.get("id").toString();
 
        String title = null;
        String content = null;
        String icon_url = null;
        String simple_content = null;
        String share_url = null;
 
        //1.课程分享 2.直播间分享 3.其他页面分享 4.成为直播间分销员分享
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        switch (query_type){
            case "1":
                Map<String,String> courseMap = CacheUtils.readCourse(id, reqEntity, readCourseOperation, jedisUtils, true);
                if(MiscUtils.isEmpty(courseMap)){
                    throw new QNLiveException("120009");
                }
                title = courseMap.get("course_title");
                Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(courseMap.get("room_id"), reqEntity, readLiveRoomOperation, jedisUtils, true);
                if("2".equals(courseMap.get("status"))){
                    content = liveRoomMap.get("room_name") + "  " + MiscUtils.getConfigByKey("weixin_course_share_content");
                }else if("1".equals(courseMap.get("status"))){
                    Date courseStartTime = new Date(Long.parseLong(courseMap.get("start_time")));
                    if(MiscUtils.isEmpty(content)){
                        content  = MiscUtils.getConfigByKey("weixin_course_share_time") + MiscUtils.parseDateToFotmatString(courseStartTime, "MM月dd日 HH:mm");
                    }else {
                        content += "\n" + MiscUtils.getConfigByKey("weixin_course_share_time") + MiscUtils.parseDateToFotmatString(courseStartTime, "MM月dd日 HH:mm");
                    }
                }
                icon_url = liveRoomMap.get("avatar_address");
                simple_content = courseMap.get("course_title");
                share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix") + id;
                break;
 
            case "2":
                Map<String,String> liveRoomInfoMap =  CacheUtils.readLiveRoom(id, reqEntity, readLiveRoomOperation, jedisUtils, true);
                if(MiscUtils.isEmpty(liveRoomInfoMap)){
                    throw new QNLiveException("120018");
                }
                title = liveRoomInfoMap.get("room_name");
                content = liveRoomInfoMap.get("room_remark");
                icon_url = liveRoomInfoMap.get("avatar_address");
                share_url = getLiveRoomShareURL(userId, id);
                simple_content = MiscUtils.getConfigByKey("weixin_live_room_simple_share_content") + liveRoomInfoMap.get("room_name");
                break;
 
            case "3":
                title = MiscUtils.getConfigByKey("weixin_other_page_share_title");
                content = MiscUtils.getConfigByKey("weixin_other_page_share_content");
                icon_url = MiscUtils.getConfigByKey("weixin_other_page_share_icon_url");
                simple_content = MiscUtils.getConfigByKey("weixin_other_page_share_simple_content");
                break;
 
            case "4":
                Jedis jedis = jedisUtils.getJedis();
                Map<String, Object> map = new HashMap<>();
                map.put(Constants.CACHED_KEY_USER_ROOM_SHARE_FIELD, reqMap.get("room_share_code"));
                String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOM_SHARE, map);
                Map<String, String> values = jedis.hgetAll(key);
 
                if(MiscUtils.isEmpty(values)){
                    throw new QNLiveException("120019");
                }
                title = MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_title");
                Map<String,String> liveRoomInfo = CacheUtils.readCourse(id, reqEntity, readCourseOperation, jedisUtils, true);
                if(MiscUtils.isEmpty(liveRoomInfo)){
                    throw new QNLiveException("120018");
                }
                content = liveRoomInfo.get("room_name") + "\n"
                        + String.format(MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_second_content"), values.get("profit_share_rate")) + "\n"
                        + MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_third_content");
                icon_url = liveRoomInfo.get("avatar_address");
                share_url = MiscUtils.getConfigByKey("be_distributer_url_pre_fix") + id;
                break;
        }
 
        resultMap.put("title",title);
        resultMap.put("content",content);
        resultMap.put("icon_url",icon_url);
        resultMap.put("simple_content",simple_content);
        resultMap.put("share_url",share_url);
        return resultMap;
    }
 
    private String encryptIMAccount(String mid, String mpwd){
        String name='\0'+mid+'\0'+mpwd;
        String keys= "l*c%@)c5";
        String res = DES.encryptDES(name, keys);
        return res;
    }
 
}
