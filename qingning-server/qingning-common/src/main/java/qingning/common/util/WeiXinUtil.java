package qingning.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qingning.common.entity.AccessToken;
import qingning.common.entity.TemplateData;
import qingning.common.entity.WxTemplate;
import redis.clients.jedis.Jedis;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

//import net.sf.json.JSONException;
//import net.sf.json.JSONObject;

/**
 * 公众平台通用接口工具类
 * @author xuhj
 * @date 2015-3-11
 */
public class WeiXinUtil {
    private static Logger log = LoggerFactory.getLogger(WeiXinUtil.class);

    // 获取access_token的接口地址（GET） 限2000（次/天）
//    public final static String access_token_url = MiscUtils.getConfigByKey("access_token_url");
//    public final static String get_user_info_by_code_url = MiscUtils.getConfigByKey("get_user_info_by_code_url");
//    public final static String get_user_info_by_access_token = MiscUtils.getConfigByKey("get_user_info_by_access_token");
//    public final static String get_base_user_info_by_access_token = MiscUtils.getConfigByKey("get_base_user_info_by_access_token");
//    public final static String get_user_by_openid = MiscUtils.getConfigByKey("get_user_by_openid");
//    //获取JSAPI_Ticket
//    public static String jsapi_ticket_url = MiscUtils.getConfigByKey("jsapi_ticket_url");
//
//    //获得微信素材多媒体URL
//    public static String get_media_url = MiscUtils.getConfigByKey("get_media_url");
//
//    private static final String appid = MiscUtils.getConfigByKey("appid");
//    private static final String appsecret = MiscUtils.getConfigByKey("appsecret");
//    private final static String weixin_template_push_url = MiscUtils.getConfigByKey("weixin_template_push_url");//"https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=ACCESS_TOKEN";
//
//    private static final String service_no_appid = MiscUtils.getConfigByKey("weixin_service_no_appid");
//    private static final String service_no_appsecret = MiscUtils.getConfigByKey("weixin_service_no_secret");
//
//    public final static String component_access_token_url = MiscUtils.getConfigByKey("service_component_access_token_url");
//    public final static String pre_auth_code_url = MiscUtils.getConfigByKey("service_pre_auth_code");
//    public final static String service_auth_url = MiscUtils.getConfigByKey("service_auth_url");
//    public final static String service_auth_info_url = MiscUtils.getConfigByKey("service_auth_info_url");
//
//    public final static String service_auth_refresh_info_url = MiscUtils.getConfigByKey("service_auth_refresh_info_url");
//    public final static String service_auth_account_info_url = MiscUtils.getConfigByKey("service_auth_account_info_url");
//
//    public final static String service_template_info_url = MiscUtils.getConfigByKey("service_template_info_url");
//
//    public final static String service_fans_url1 = MiscUtils.getConfigByKey("service_fans_url1");
//    public final static String service_fans_url2 = MiscUtils.getConfigByKey("service_fans_url2");
//
//    private static final String pc_no_appid = MiscUtils.getConfigByKey("weixin_pc_no_appid");
//    private static final String pc_no_appsecret = MiscUtils.getConfigByKey("weixin_pc_no_secret");
//    private static final String pc_auth_account_info_url = MiscUtils.getConfigByKey("pc_auth_account_info_url");


    /**
     * 获取accessToekn
     * @param appid 凭证
     * @param appsecret 密匙
     * @return
     */
    public static AccessToken getAccessToken(String appid, String appsecret,Jedis jedis,String appName) {
        AccessToken accessToken = null;
        String token = jedis.get(Constants.CACHED_KEY_WEIXIN_TOKEN);
        if(MiscUtils.isEmpty(appid) || MiscUtils.isEmpty(appsecret)){
        	appid=MiscUtils.getConfigByKey(Constants.APPID,appName);
        	appsecret=MiscUtils.getConfigByKey(Constants.APPSECRET,appName);
        }
        
        if(token == null){
            String requestUrl = MiscUtils.getConfigByKey(Constants.ACCESS_TOKEN_URL,appName).replace("APPID", appid).replace("APPSECRET", appsecret);
            String requestResult = HttpTookit.doGet(requestUrl);
            JSONObject jsonObject = JSON.parseObject(requestResult);
            // 如果请求成功
            if (null != jsonObject) {
                try {
                    accessToken = new AccessToken();
                    accessToken.setToken(jsonObject.getString("access_token"));
                    accessToken.setExpiresIn(jsonObject.getInteger("expires_in"));
                    jedis.setex(Constants.CACHED_KEY_WEIXIN_TOKEN, 7000,jsonObject.getString("access_token"));
                } catch (JSONException e) {
                    accessToken = null;
                    // 获取token失败
                    log.error("获取token失败 errcode:{} errmsg:{}", jsonObject.getInteger("errcode"), jsonObject.getString("errmsg"));
                }
            }
        }else {
            accessToken = new AccessToken();
            accessToken.setToken(token);
            accessToken.setExpiresIn(jedis.ttl(Constants.CACHED_KEY_WEIXIN_TOKEN).intValue());
        }

        return accessToken;
    }

//
    /**
     * 获取第三方平台component_access_token
     * @return
     */
    public static JSONObject getComponentAccessToken(String ticket,String appName) {
        String componentAppid = MiscUtils.getConfigByKey(Constants.SERVICE_NO_APPID,appName);
        String componentAppSecret =MiscUtils.getConfigByKey(Constants.SERVICE_NO_APPSECRET,appName) ;
        String requestUrl =MiscUtils.getConfigByKey(Constants.COMPONENT_ACCESS_TOKEN_URL,appName) ;

        Map<String, String> param = new HashMap<>();
        param.put("component_appid", componentAppid);
        param.put("component_appsecret", componentAppSecret);
        param.put("component_verify_ticket", ticket);

        String requestResult = HttpTookit.doPost(requestUrl, null, param, null);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }
    /**
     * 获取第三方平台预授权码pre_auth_code
     * @return
     */
    public static JSONObject getPreAuthCode(String accessToken,String appName) {
        String componentAppid = MiscUtils.getConfigByKey(Constants.SERVICE_NO_APPID,appName);
        String requestUrl = MiscUtils.getConfigByKey(Constants.PRE_AUTH_CODE_URL,appName) .replace("COMPONENT_ACCESS_TOKEN", accessToken);

        Map<String, String> param = new HashMap<>();
        param.put("component_appid", componentAppid);

        String requestResult = HttpTookit.doPost(requestUrl, null, param, null);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }
    /**
     * 获取第三方平台授权重定向到微信url
     * @return
     */
    public static String getServiceAuthUrl(String preAuthCode,String appName) {
        String componentAppid =  MiscUtils.getConfigByKey(Constants.SERVICE_NO_APPID,appName);
        String requestUrl =  MiscUtils.getConfigByKey(Constants.SERVICE_AUTH_URL,appName).replace("COMPONENT_APPID", componentAppid).replace("AUTH_CODE", preAuthCode).replace("REDIRECT_URI", MiscUtils.getConfigByKey("weixin_service_redirect_url",appName));
        log.debug("------微信--服务号重定向URL--  "+requestUrl);
        return requestUrl;
    }
    /**
     * 获取第三方平台授权信息
     * @return
     */
    public static JSONObject getServiceAuthInfo(String accessToken, String authCode,String appName) {
        String componentAppid = MiscUtils.getConfigByKey(Constants.SERVICE_NO_APPID,appName);
        String requestUrl =  MiscUtils.getConfigByKey(Constants.SERVICE_AUTH_INFO_URL,appName) .replace("COMPONENT_ACCESS_TOKEN", accessToken);

        Map<String, String> param = new HashMap<>();
        param.put("component_appid", componentAppid);
        param.put("authorization_code", authCode);

        String requestResult = HttpTookit.doPost(requestUrl, null, param, null);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }

    /**
     * 刷新第三方平台授权信息 主要是accessToken 和 refreshToken
     * @param accessToken
     * @param refreshToken
     * @param authorizerAppid
     * @return
     */
    public static JSONObject refreshServiceAuthInfo(String accessToken, String refreshToken, String authorizerAppid,String appName) {
        String componentAppid = MiscUtils.getConfigByKey(Constants.SERVICE_NO_APPID,appName);
        String requestUrl = MiscUtils.getConfigByKey(Constants.SERVICE_AUTH_REFRESH_INFO_URL,appName).replace("COMPONENT_ACCESS_TOKEN", accessToken);

        Map<String, String> param = new HashMap<>();
        param.put("component_appid", componentAppid);
        param.put("authorizer_appid", authorizerAppid);
        param.put("authorizer_refresh_token", refreshToken);

        String requestResult = HttpTookit.doPost(requestUrl, null, param, null);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }
    /**
     * 获取第三方平台账号信息
     * @return
     */
    public static JSONObject getServiceAuthAccountInfo(String accessToken, String authorizerAppid,String appName) {
        String componentAppid = MiscUtils.getConfigByKey(Constants.SERVICE_NO_APPID,appName);
        String requestUrl = MiscUtils.getConfigByKey(Constants.SERVICE_AUTH_ACCOUNT_INFO_URL,appName).replace("COMPONENT_ACCESS_TOKEN", accessToken);

        Map<String, String> param = new HashMap<>();
        param.put("component_appid", componentAppid);
        param.put("authorizer_appid", authorizerAppid);

        String requestResult = HttpTookit.doPost(requestUrl, null, param, null);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }

    public static JSONObject createServiceTemplateInfo(String accessToken, String type,String appName) {
        String requestUrl = MiscUtils.getConfigByKey(Constants.SERVICE_TEMPLATE_INFO_URL,appName).replace("ACCESS_TOKEN", accessToken);

        Map<String, String> param = new HashMap<>();
        String template_id_short = null;
        if (type.equals("1")) {
            template_id_short = MiscUtils.getConfigByKey("wpush_start_course_template_id",appName);
        } else if (type.equals("2")) {
            template_id_short = null;
        }
        param.put("template_id_short", template_id_short);

        String requestResult = HttpTookit.doPost(requestUrl, null, param, null);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }
    /**
     * 获取服务号的粉丝的openid
     * @param accessToken 服务号的
     * @param nextOpenId 粉丝的
     * @return
     */
    public static JSONObject getServiceFansList(String accessToken, String nextOpenId,String appName) {
        String requestUrl = null;
        if (nextOpenId == null) {
            requestUrl = MiscUtils.getConfigByKey(Constants.SERVICE_FANS_URL1,appName).replace("ACCESS_TOKEN", accessToken);
        } else {
            requestUrl = MiscUtils.getConfigByKey(Constants.SERVICE_FANS_URL2,appName).replace("ACCESS_TOKEN", accessToken).replace("NEXT_OPENID", nextOpenId);
        }

        String requestResult = HttpTookit.doGet(requestUrl);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }

    /**
     * 给服务号的粉丝发送模板消息
     * @param accessToken 服务号的
     * @param openId  服务号粉丝的
     * @param url 详情链接地址
     * @param templateId 模板消息id
     * @param templateMap 模板数据
     */
    public static int sendTemplateMessageToServiceNoFan(String accessToken, String openId, String url, String templateId, Map<String, TemplateData> templateMap,String appName) {
        String accessUrl = MiscUtils.getConfigByKey(Constants.WEIXIN_TEMPLATE_PUSH_URL,appName).replace("ACCESS_TOKEN", accessToken);
        WxTemplate temp = new WxTemplate();
        temp.setUrl(url);
        temp.setTouser(openId);
        temp.setTopcolor("#000000");
        temp.setTemplate_id(templateId);

        temp.setData(templateMap);
        String jsonString="";
        try {
            jsonString = com.alibaba.dubbo.common.json.JSON.json(temp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = WeiXinUtil.httpRequest(accessUrl, "POST", jsonString);
        log.info("微信推送消息发送参数：" + jsonObject);
        int result = 0;
        if (null != jsonObject) {
            if (0 != jsonObject.getIntValue("errcode")) {
                result = jsonObject.getIntValue("errcode");
                log.error("错误 errcode:{} errmsg:{}", jsonObject.getIntValue("errcode"), jsonObject.getString("errmsg"));
            }
        }
        return result;
    }
    /**
     * 获取PC端登录的账号信息
     * @return
     */
    public static JSONObject getPCUserAccountInfo(String code,String appName) {
        String requestUrl = MiscUtils.getConfigByKey(Constants.PC_AUTH_ACCOUNT_INFO_URL,appName).replace("APPID", MiscUtils.getConfigByKey(Constants.PC_NO_APPID,appName) ).replace("SECRET", MiscUtils.getConfigByKey(Constants.PC_NO_APPSECRET,appName) ).replace("CODE", code).replace("GRANTTYPE", "authorization_code");
        String requestResult = HttpTookit.doGet(requestUrl);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }


    public static JSONObject getUserInfoByCode(String code,String appName) {
        String requestUrl = MiscUtils.getConfigByKey(Constants.GET_USER_INFO_BY_ACCESS_TOKEN,appName).replace("APPID", MiscUtils.getConfigByKey(Constants.APPID,appName)).replace("APPSECRET",MiscUtils.getConfigByKey(Constants.APPSECRET,appName)).replace("CODE",code);
        log.debug("------微信--通过H5传递code获得access_token-请求URL  "+requestUrl);
        String requestResult = HttpTookit.doGet(requestUrl);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        log.debug("------微信--通过H5传递code获得access_token-返回参数  "+requestResult);
        return jsonObject;
    }

    /**
     * 通过微信openId 来判断是否关注公众号
     * @param accessToken 调用接口凭证 公众号的通用凭证accesstoken
     * @param openId 微信用户的openId
     * @return
     */
    public static JSONObject getUserByOpenid(String accessToken,String openId,String appName){
        log.debug("------微信--获得多媒体URL-请求URL ====================获取当前用户是否关注直播间 ");
        String requestUrl = MiscUtils.getConfigByKey(Constants.GET_USER_BY_OPENID,appName).replace("ACCESS_TOKEN", accessToken).replace("OPENID", openId);
        String requestResult = HttpTookit.doGet(requestUrl);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        log.debug("------微信--获得多媒体URL-请求URL  "+requestUrl);
        log.debug("------微信--通过access_token和openid获得用户详细信息-请求URL  "+requestResult);
        return jsonObject;
    }

    public static String getMediaURL(String server_id, Jedis jedis,String appName) {
        String accessToken = getAccessToken(MiscUtils.getConfigByKey(Constants.APPID,appName), MiscUtils.getConfigByKey(Constants.APPSECRET,appName), jedis,appName).getToken();
        String requestUrl = MiscUtils.getConfigByKey(Constants.GET_MEDIA_URL,appName).replace("ACCESS_TOKEN", accessToken).replace("MEDIA_ID", server_id);
        log.debug("------微信--获得多媒体URL-请求URL  "+requestUrl);
        return requestUrl;
    }
 

    /**
     * 未关注公众号，已经授权，通过accessToken和union_id获得用户详细信息
     * @param accessToken
     * @return
     */
    public static JSONObject getUserInfoByAccessToken(String accessToken, String unionId,String appName) {
        String requestUrl = MiscUtils.getConfigByKey(Constants.GET_USER_INFO_BY_ACCESS_TOKEN,appName).replace("ACCESS_TOKEN", accessToken).replace("OPENID", unionId);
        log.debug("------微信--通过access_token和unionId获得用户详细信息-请求URL  "+requestUrl);
        String requestResult = HttpTookit.doGet(requestUrl);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        log.debug("------微信--通过access_token和unionId获得用户详细信息-请求URL  "+requestResult);
        return jsonObject;
    }

    /**
     * 获取用户基本信息
     * @param accessToken
     * @return
     */
    public static JSONObject getBaseUserInfoByAccessToken(String accessToken, String unionId,String appName) {
        String requestUrl =MiscUtils.getConfigByKey(Constants.GET_BASE_USER_INFO_BY_ACCESS_TOKEN,appName) .replace("ACCESS_TOKEN", accessToken).replace("OPENID", unionId);
        log.debug("------微信--通过access_token和unionId获得用户详细信息-请求URL  "+requestUrl);
        String requestResult = HttpTookit.doGet(requestUrl);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        log.debug("------微信--通过access_token和unionId获得用户详细信息-请求URL  "+requestResult);
        return jsonObject;
    }

    /**
     * 获取jsapi_ticket
     * @return
     */
    public static String getJSApiTIcket(Jedis jedis,String appName){
        String jsApiTicket = null;
        jsApiTicket = jedis.get(Constants.CACHED_KEY_WEIXIN_JS_API_TIKET);
        if(jsApiTicket == null){
            String accessToken = getAccessToken(MiscUtils.getConfigByKey(Constants.APPID,appName),MiscUtils.getConfigByKey(Constants.APPSECRET,appName) , jedis,appName).getToken();
            int result = 0;

            //拼装创建菜单Url
            String url =  MiscUtils.getConfigByKey(Constants.JSAPI_TICKET_URL,appName).replace("ACCESS_TOKEN", accessToken);
            //调用接口获取jsapi_ticket
            String requestResult = HttpTookit.doGet(url);
            JSONObject jsonObject = JSON.parseObject(requestResult);
            // 如果请求成功
            if (null != jsonObject) {
                try {
                    jsApiTicket = jsonObject.getString("ticket");
                    jedis.setex(Constants.CACHED_KEY_WEIXIN_JS_API_TIKET, 7000, jsApiTicket);
                } catch (JSONException e) {
                    if (0 != jsonObject.getInteger("errcode")) {
                        result = jsonObject.getInteger("errcode");
                        log.error("JSAPI_Ticket获取失败 errcode:{} errmsg:{}", jsonObject.getInteger("errcode"), jsonObject.getString("errmsg"));
                    }
                }
            }
        }
        return jsApiTicket;
    }

    //获取计算后的signature，及其它字段 noncestr,timestamp,jsapi_ticket
    public static Map<String, String> sign(String jsapi_ticket, String url,String appName) {
        Map<String, String> ret = new HashMap<String, String>();
        String nonce_str = MiscUtils.getUUId();
        String timestamp = System.currentTimeMillis()/1000 + "";
        String string1;
        String signature = "";

        //注意这里参数名必须全部小写，且必须有序
        string1 = "jsapi_ticket=" + jsapi_ticket +
                "&noncestr=" + nonce_str +
                "&timestamp=" + timestamp +
                "&url=" + url;
        System.out.println(string1);

        try
        {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(string1.getBytes("UTF-8"));
            signature = byteToHex(crypt.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        ret.put("url", url);
        ret.put("jsapi_ticket", jsapi_ticket);
        ret.put("nonceStr", nonce_str);
        ret.put("timestamp", timestamp);
        ret.put("signature", signature);
        ret.put("appId",MiscUtils.getConfigByKey(Constants.APPID,appName) );

        return ret;
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }


	/**发送模板消息
	 * @param openId openId 用户标识
	 * @param url          详情链接地址
	 * @param templateId   推送模板信息
	 * @param templateMap  模板内容
	 * @param jedis  
	 */
	public static void send_template_message(String openId, String templateId,String url, Map<String, TemplateData> templateMap,Jedis jedis,String appName) {
	
		AccessToken token = getAccessToken(MiscUtils.getConfigByKey(Constants.APPID,appName),MiscUtils.getConfigByKey(Constants.APPSECRET,appName) ,jedis,appName);
		String access_token = token.getToken();
		String accessUrl = MiscUtils.getConfigByKey(Constants.WEIXIN_TEMPLATE_PUSH_URL,appName).replace("ACCESS_TOKEN", access_token);
		WxTemplate temp = new WxTemplate();
		temp.setUrl(url);
		temp.setTouser(openId);
		temp.setTopcolor("#000000");
		temp.setTemplate_id(templateId);

		temp.setData(templateMap);
		String jsonString="";
		try {
			jsonString = com.alibaba.dubbo.common.json.JSON.json(temp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = WeiXinUtil.httpRequest(accessUrl, "POST", jsonString);
		log.info("微信推送消息发送参数：" + jsonObject);
		int result = 0;
		if (null != jsonObject) {
			if (0 != jsonObject.getIntValue("errcode")) {
				result = jsonObject.getIntValue("errcode");
				log.error("错误 errcode:{} errmsg:{}", jsonObject.getIntValue("errcode"), jsonObject.getString("errmsg"));
			}
		}
		System.out.println(result);
		log.info("微信消息消息发送结果：" + result);
	}
    
	/**
	 * 发起https请求并获取结果
	 * 
	 * @param requestUrl 请求地址
	 * @param requestMethod 请求方式（GET、POST）
	 * @param outputStr 提交的数据
	 * @return JSONObject(通过JSONObject.get(key)的方式获取json对象的属性值)
	 */
	public static JSONObject httpRequest(String requestUrl, String requestMethod, String outputStr) {
		JSONObject jsonObject = null;
		StringBuffer buffer = new StringBuffer();
		try {
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);

			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if ("GET".equalsIgnoreCase(requestMethod))
				httpUrlConn.connect();
			System.err.println(outputStr);
			// 当有数据需要提交时
			if (null != outputStr) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes("UTF-8"));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String str = null;
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}
			bufferedReader.close();
			inputStreamReader.close();
			// 释放资源
			inputStream.close();
			inputStream = null;
			httpUrlConn.disconnect();
			//TODO 
			jsonObject = JSONObject.parseObject(buffer.toString());
			System.out.println(buffer.toString());
		} catch (ConnectException ce) {
			log.error("Weixin server connection timed out.");
//			ce.printStackTrace();
		} catch (Exception e) {
			log.error("https request error:{}", e);
//			e.printStackTrace();
		}
		return jsonObject;
	}
}
