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
    public final static String access_token_url = MiscUtils.getConfigByKey("access_token_url");
    public final static String get_user_info_by_code_url = MiscUtils.getConfigByKey("get_user_info_by_code_url");
    public final static String get_user_info_by_access_token = MiscUtils.getConfigByKey("get_user_info_by_access_token");
    public final static String get_base_user_info_by_access_token = MiscUtils.getConfigByKey("get_base_user_info_by_access_token");
    public final static String get_user_by_openid = MiscUtils.getConfigByKey("get_user_by_openid");
    //获取JSAPI_Ticket
    public static String jsapi_ticket_url = MiscUtils.getConfigByKey("jsapi_ticket_url");

    //获得微信素材多媒体URL
    public static String get_media_url = MiscUtils.getConfigByKey("get_media_url");

    private static final String appid = MiscUtils.getConfigByKey("appid");
    private static final String appsecret = MiscUtils.getConfigByKey("appsecret");
    private final static String weixin_template_push_url = MiscUtils.getConfigByKey("weixin_template_push_url");//"https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=ACCESS_TOKEN";

    /**
     * 获取accessToekn
     * @param appid 凭证
     * @param appsecret 密匙
     * @return
     */
    public static AccessToken getAccessToken(String appid, String appsecret,Jedis jedis) {
        AccessToken accessToken = null;
        String token = jedis.get(Constants.CACHED_KEY_WEIXIN_TOKEN);
        if(MiscUtils.isEmpty(appid) || MiscUtils.isEmpty(appsecret)){
        	appid=WeiXinUtil.appid;
        	appsecret=WeiXinUtil.appsecret;
        }
        
        if(token == null){
            String requestUrl = access_token_url.replace("APPID", appid).replace("APPSECRET", appsecret);
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


    public static JSONObject getUserInfoByCode(String code) {
        String requestUrl = get_user_info_by_code_url.replace("APPID", appid).replace("APPSECRET", appsecret).replace("CODE", code);
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
    public static JSONObject getUserByOpenid(String accessToken,String openId){
        String requestUrl = get_user_by_openid.replace("ACCESS_TOKEN", accessToken).replace("OPENID", openId);
        String requestResult = HttpTookit.doGet(requestUrl);
        JSONObject jsonObject = JSON.parseObject(requestResult);
        return jsonObject;
    }

    public static String getMediaURL(String server_id, Jedis jedis) {
        String accessToken = getAccessToken(appid, appsecret, jedis).getToken();
        String requestUrl = get_media_url.replace("ACCESS_TOKEN", accessToken).replace("MEDIA_ID", server_id);
        log.debug("------微信--获得多媒体URL-请求URL  "+requestUrl);
        return requestUrl;
    }
 

    /**
     * 未关注公众号，已经授权，通过accessToken和union_id获得用户详细信息
     * @param accessToken
     * @return
     */
    public static JSONObject getUserInfoByAccessToken(String accessToken, String unionId) {
        String requestUrl = get_user_info_by_access_token.replace("ACCESS_TOKEN", accessToken).replace("OPENID", unionId);
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
    public static JSONObject getBaseUserInfoByAccessToken(String accessToken, String unionId) {
        String requestUrl = get_base_user_info_by_access_token.replace("ACCESS_TOKEN", accessToken).replace("OPENID", unionId);
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
    public static String getJSApiTIcket(Jedis jedis){
        String jsApiTicket = null;
        jsApiTicket = jedis.get(Constants.CACHED_KEY_WEIXIN_JS_API_TIKET);
        if(jsApiTicket == null){
            String accessToken = getAccessToken(appid, appsecret, jedis).getToken();
            int result = 0;

            //拼装创建菜单Url
            String url =  jsapi_ticket_url.replace("ACCESS_TOKEN", accessToken);
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
    public static Map<String, String> sign(String jsapi_ticket, String url) {
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
        ret.put("appId", appid);

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


	/**
	 * @param openId openId 用户标识
	 * @param url          详情链接地址
	 * @param templateId   推送模板信息
	 * @param templateMap  模板内容
	 * @param jedis  
	 */
	public static void send_template_message(String openId, String templateId,String url, Map<String, TemplateData> templateMap,Jedis jedis) {
	
		AccessToken token = getAccessToken(appid, appsecret,jedis);
		String access_token = token.getToken();
		String accessUrl = weixin_template_push_url.replace("ACCESS_TOKEN", access_token);
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
