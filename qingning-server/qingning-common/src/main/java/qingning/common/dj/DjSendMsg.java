package qingning.common.dj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import qingning.common.entity.QNLiveException;
import qingning.common.util.HttpTookit;
import qingning.common.util.MD5Util;
import qingning.common.util.MiscUtils;
import redis.clients.jedis.Jedis;

/**
 * Created by GHS on 2017/5/11.
 */
public class DjSendMsg {
    private static Logger logger = Logger.getLogger(DjSendMsg.class);

    private static String SECURECODE = MiscUtils.getConfigByKey("dj_securecode","dlive");//"d6d3a5e9-3acf-5452-4c30-fbf25ecf852e";
    private static String LOGIN_NAME = MiscUtils.getConfigByKey("dj_login_name","dlive");
    private static String LOGIN_PW = MiscUtils.getConfigByKey("dj_login_pw","dlive");
    private static String SYS_AREA_CODE = MiscUtils.getConfigByKey("dj_sys_area_code","dlive");
    private static String COUNTRY = MiscUtils.getConfigByKey("dj_country","dlive");
    private static String APPKEY = MiscUtils.getConfigByKey("dj_appkey","dlive");
    private static String VERSION = MiscUtils.getConfigByKey("dj_version","dlive");
    private static String LANGUAGE = MiscUtils.getConfigByKey("dj_language","dlive");
    private static String APPTYPE =MiscUtils.getConfigByKey("dj_apptype","dlive");
    private static String LOGIN_URL = MiscUtils.getConfigByKey("dj_login_url","dlive");
    private static String SEND_VERIFICATIONCODE_URL = MiscUtils.getConfigByKey("dj_send_verificationcode_url","dlive");
    private static String CHECK_VERIFICATIONCODE_URL = MiscUtils.getConfigByKey("dj_check_verificationcode_url","dlive");
    private static String SYS_DJ_TOKEN = "SYS:DJ:TOKEN";

    public static boolean djLogin(Jedis jedis) throws Exception {

        Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("login_name", LOGIN_NAME);
        contentMap.put("login_pw", LOGIN_PW);
        contentMap.put("sys_area_code", SYS_AREA_CODE);
        contentMap.put("country", COUNTRY);
        String httpOrgCreateTestRtn = httpClient(LOGIN_URL, contentMap, jedis, true);
        Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
        if (resultMap != null && resultMap.get("ret") != null && resultMap.get("ret").equals("0") && resultMap.get("items") != null) {
            List<Map<String, String>> list = JSON.parseObject(resultMap.get("items"), new TypeReference<List<Map<String, String>>>() {});
            if (list != null && list.size() > 0) {
                Map<String, String> item = list.get(0);
                if (item != null && item.get("token") != null) {
                    jedis.setex(SYS_DJ_TOKEN, 27000, item.get("token"));
                    return true;
                }
            }
        } else {
            String ret = resultMap.get("ret");
            if (ret.equals("500004") || ret.equals("200123") || ret.equals("200464")) {
                httpOrgCreateTestRtn = httpClient(LOGIN_URL, contentMap, jedis, true);
                resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {
                });
                if (resultMap != null && resultMap.get("ret") != null && resultMap.get("ret").equals("0") && resultMap.get("items") != null) {
                    List<Map<String, String>> list = JSON.parseObject(resultMap.get("items"), new TypeReference<List<Map<String, String>>>() {
                    });
                    if (list != null && list.size() > 0) {
                        Map<String, String> item = list.get(0);
                        if (item != null && item.get("token") != null) {
                            jedis.setex(SYS_DJ_TOKEN, 27000, item.get("token"));
                            return true;
                        }
                    }
                }
            } else {
                logger.info("登录德家助理返回的异常信息!");
                if (resultMap != null && resultMap.get("ret") != null) {
                    logger.info("登录德家助理返回的消息码ret:" + resultMap.get("ret"));
                }
                if (resultMap != null && StringUtils.isNotBlank(resultMap.get("msg"))) {
                    logger.info("登录德家助理返回的消息内容msg:" + resultMap.get("msg"));
                }
                return false;
            }
        }
        return false;
    }

    /**
     *  发送验证码
     * @param phone
     * @param businessId
     */
    public static boolean sendVerificationCode(String phone,String businessId,Jedis jedis) throws Exception {
        Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("notice_type", "1");
        contentMap.put("notice_obj", phone);
        contentMap.put("notice_msg", "123");
        contentMap.put("business_id", businessId);
        contentMap.put("sys_area_code", SYS_AREA_CODE);
        contentMap.put("country",COUNTRY);
        String httpOrgCreateTestRtn = httpClient(SEND_VERIFICATIONCODE_URL,contentMap,jedis,false);
        Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
        return resultMap.get("data_string").equals("Y");
    }


    /**
     *  效验验证码
     * @param phone
     * @param businessId
     */
    public static boolean checkVerificationCode(String phone,String businessId,String verification_code,Jedis jedis) throws Exception {
        Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("notice_type", "1");
        contentMap.put("notice_obj", phone);
        contentMap.put("verification_code", verification_code);
        contentMap.put("business_id", businessId);
        contentMap.put("sys_area_code", SYS_AREA_CODE);
        contentMap.put("country",COUNTRY);
        String httpOrgCreateTestRtn = httpClient(CHECK_VERIFICATIONCODE_URL,contentMap,jedis,false);
        Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
        return resultMap.get("data_string").equals("Y");
    }



    private static String httpClient(String url,Map<String, String> contentMap,Jedis jedis,boolean isLogin) throws Exception {
        String josnMd5 = MD5Util.test( JSONObject.toJSONString(contentMap),SECURECODE, 1);
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("appkey", APPKEY);
        headerMap.put("verification", josnMd5);
        headerMap.put("version", VERSION);
        headerMap.put("language",LANGUAGE);
        if(jedis.exists(SYS_DJ_TOKEN)){
            headerMap.put("token",jedis.get(SYS_DJ_TOKEN));
        }else{
            boolean login = djLogin(jedis);
            if(login){
                headerMap.put("token", jedis.get(SYS_DJ_TOKEN));
            }else{
                throw new QNLiveException("130006");
            }
        }
        headerMap.put("logincode", LOGIN_NAME);
        headerMap.put("apptype", APPTYPE);
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        return httpClientUtil.doPost(url,headerMap,contentMap,"UTF-8");
    }







    //<editor-fold desc="测试广州德家">
    //    private static void sendMsg(String token){
//        Map<String, String> contentMap = new HashMap<String, String>();
//            contentMap.put("notice_type", "1");
//	    	contentMap.put("notice_obj", "18676365713");
//	    	contentMap.put("notice_msg", "122");
//	    	contentMap.put("business_id", "112123312132");
//            contentMap.put("sys_area_code", "440100");
//            contentMap.put("country", "86");
//        try {
//            String jsonString = JSONObject.toJSONString(contentMap);
//            System.out.println("json:============"+jsonString);
//            String md5 = MD5Util.test(jsonString, "c291f783-7a43-e2cc-a720-d47c54b4640f", 1);
//
//
//            Map<String, String> headerMap = new HashMap<String, String>();
//            headerMap.put("appkey", "90fadbdf-9bd6-a8dd-3160-6a3f088a9110");
//            headerMap.put("verification", md5);
//            headerMap.put("version", "1.0");
//            headerMap.put("language", "zh_CN");
//            headerMap.put("logincode", "LJ6RJA6Z");
//            headerMap.put("apptype", "16");
//            headerMap.put("token", token);
//            HttpClientUtil httpClientUtil = new HttpClientUtil();
//            String httpOrgCreateTestRtn = httpClientUtil.doPost("https://service.blessi.cn/oppf/service/common/notice/send_verificationcode",headerMap,contentMap,"UTF-8");
//            System.out.println(httpOrgCreateTestRtn);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private static void checkVerificationCode(String token){
//        Map<String, String> contentMap = new HashMap<String, String>();
//        contentMap.put("notice_type", "1");
//        contentMap.put("notice_obj", "18676365713");
//        contentMap.put("verification_code", "369736");
//        contentMap.put("business_id", "123456");
//        contentMap.put("sys_area_code", "440100");
//        contentMap.put("country", "86");
//        try {
//            String jsonString = JSONObject.toJSONString(contentMap);
//            System.out.println("json:============"+jsonString);
//            String md5 = MD5Util.test(jsonString, "d6d3a5e9-3acf-5452-4c30-fbf25ecf852e", 1);
//
//
//            Map<String, String> headerMap = new HashMap<String, String>();
//            headerMap.put("appkey", "90fadbdf-9bd6-a8dd-3160-6a3f088a9110");
//            headerMap.put("verification", md5);
//            headerMap.put("version", "1.0");
//            headerMap.put("language", "zh_CN");
//            headerMap.put("logincode", "LJ6RJA6Z");
//            headerMap.put("apptype", "16");
//            headerMap.put("token", token);
//            HttpClientUtil httpClientUtil = new HttpClientUtil();
//            String httpOrgCreateTestRtn = httpClientUtil.doPost("https://www.opercenter.com/oppf/service/common/notice/check_verificationcode",headerMap,contentMap,"UTF-8");
//            Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
//            System.out.println(resultMap.get("data_string").equals("Y"));
//            System.out.println(httpOrgCreateTestRtn);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // https://service.blessi.cn/oppf/service
//    public static void main(String[] args){
//        Map<String, String> contentMap = new HashMap<String, String>();
//        contentMap.put("login_name", "LJ6RJA6Z");
//        contentMap.put("login_pw", "583c93729cf89a3cce5e5a0bd830451f");
//        contentMap.put("sys_area_code", "440100");
//        contentMap.put("country", "86");
//
//        Object o = JSONObject.toJSON(contentMap);
//        String json = o.toString();
//        try {
//            String test = MD5Util.test(json,"c291f783-7a43-e2cc-a720-d47c54b4640f", 1);
//            System.out.println(test);
//            Map<String, String> headerMap = new HashMap<String, String>();
//            headerMap.put("appkey", "90fadbdf-9bd6-a8dd-3160-6a3f088a9110");
//            headerMap.put("verification", test);
//            headerMap.put("version", "1.0");
//            headerMap.put("language", "zh_CN");
//            headerMap.put("logincode", "LJ6RJA6Z");
//            headerMap.put("apptype", "16");
//            HttpClientUtil httpClientUtil = new HttpClientUtil();
//            String httpOrgCreateTestRtn = httpClientUtil.doPost("https://service.blessi.cn/oppf/service/common/sys/user_login",headerMap,contentMap,"UTF-8");
//            System.out.println(httpOrgCreateTestRtn);
//            Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
//            if (resultMap != null && resultMap.get("ret") != null && resultMap.get("ret").equals("0") && resultMap.get("items") != null) {
//                List<Map<String, String>> list = JSON.parseObject(resultMap.get("items"), new TypeReference<List<Map<String, String>>>() {});
//
//
//                if (list != null && list.size() > 0) {
//                    Map<String, String> item = list.get(0);
//                    if (item != null && item.get("token") != null) {
//                        System.out.println(item.get("token") );
//                        String token = item.get("token");
//                        sendMsg(token);
//                        //  checkVerificationCode(token);
//                        //sendMsg("18676365713","123456",token);
//                        //      jedis.setex("serverTokenForAccessingDJZL", 27000, item.get("token"));//serverToken失效时间保守设置为7个小时30分钟
//                    }
//                }
//            }else {
//                ///  logger.info("登录德家助理返回的异常信息!");
//                if(resultMap != null && resultMap.get("ret") != null){
//                    //     logger.info("登录德家助理返回的消息码ret:"+resultMap.get("ret"));
//                }
//                if(resultMap != null && StringUtils.isNotBlank(resultMap.get("msg"))){
//                    //  logger.info("登录德家助理返回的消息内容msg:"+resultMap.get("msg"));
//                }
//            }
//
//            //  String tokenResultString = HttpTookit.doPost("https://www.opercenter.com/oppf/service/common/sys/user_login", headerMap, contentMap, "UTF-8");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    //</editor-fold>













}
