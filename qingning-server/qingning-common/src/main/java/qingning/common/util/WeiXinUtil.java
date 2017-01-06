package qingning.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.AccessToken;
import redis.clients.jedis.Jedis;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;


/**
 * 公众平台通用接口工具类
 * @author xuhj
 * @date 2015-3-11
 */
public class WeiXinUtil {
    private static Logger log = LoggerFactory.getLogger(WeiXinUtil.class);

    // 获取access_token的接口地址（GET） 限2000（次/天）
    public final static String access_token_url = IMMsgUtil.configMap.get("access_token_url");
    public final static String get_user_info_by_code_url = IMMsgUtil.configMap.get("get_user_info_by_code_url");
    public final static String get_user_info_by_access_token = IMMsgUtil.configMap.get("get_user_info_by_access_token");
    public final static String get_base_user_info_by_access_token = IMMsgUtil.configMap.get("get_base_user_info_by_access_token");
    //获取JSAPI_Ticket
    public static String jsapi_ticket_url = IMMsgUtil.configMap.get("jsapi_ticket_url");

    private static final String appid = IMMsgUtil.configMap.get("appid");
    private static final String appsecret = IMMsgUtil.configMap.get("appsecret");


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

}