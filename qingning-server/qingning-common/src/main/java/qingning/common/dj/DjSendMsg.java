package qingning.common.dj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import qingning.common.util.HttpTookit;
import qingning.common.util.MD5Util;

/**
 * Created by GHS on 2017/5/11.
 */
public class DjSendMsg {
    private static Logger logger = Logger.getLogger(DjSendMsg.class);

    private static String SECURECODE = "d6d3a5e9-3acf-5452-4c30-fbf25ecf852e";
    private static String LOGIN_NAME = "W44G9WRB";
    private static String LOGIN_PW = "3a01dda81e8cada809b70f33677b43e3";
    private static String SYS_AREA_CODE = "440100";
    private static String COUNTRY = "86";

    private static String APPKEY = "2a86ad9f-b3b6-02ed-f087-88770ca3c36e";
    private static String VERSION = "1.0";
    private static String LANGUAGE = "zh_CN";
    private static String APPTYPE = "16";

    private static String LOGIN_URL = "https://www.opercenter.com/oppf/service/common/sys/user_login";
    private static String SEND_VERIFICATIONCODE_URL = "https://www.opercenter.com/oppf/service/common/notice/send_verificationcode";
    private static String CHECK_VERIFICATIONCODE_URL = "https://www.opercenter.com/oppf/service/common/notice/check_verificationcode";

    public static void main(String[] args){
        Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("login_name", "W44G9WRB");
        contentMap.put("login_pw", "3a01dda81e8cada809b70f33677b43e3");
        contentMap.put("sys_area_code", "440100");
        contentMap.put("country", "86");

        Object o = JSONObject.toJSON(contentMap);
        String json = o.toString();
        try {
            String test = MD5Util.test(json,"d6d3a5e9-3acf-5452-4c30-fbf25ecf852e", 1);
            System.out.println(test);
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put("appkey", "2a86ad9f-b3b6-02ed-f087-88770ca3c36e");
            headerMap.put("verification", test);
            headerMap.put("version", "1.0");
            headerMap.put("language", "zh_CN");
            headerMap.put("logincode", "W44G9WRB");
            headerMap.put("apptype", "16");
            HttpClientUtil httpClientUtil = new HttpClientUtil();
            String httpOrgCreateTestRtn = httpClientUtil.doPost("https://www.opercenter.com/oppf/service/common/sys/user_login",headerMap,contentMap,"UTF-8");
            System.out.println(httpOrgCreateTestRtn);
            Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
            if (resultMap != null && resultMap.get("ret") != null && resultMap.get("ret").equals("0") && resultMap.get("items") != null) {
                List<Map<String, String>> list = JSON.parseObject(resultMap.get("items"), new TypeReference<List<Map<String, String>>>() {});


                if (list != null && list.size() > 0) {
                    Map<String, String> item = list.get(0);
                    if (item != null && item.get("token") != null) {
                        System.out.println(item.get("token") );
                        String token = item.get("token");
                        sendMsg(token);
                      //  checkVerificationCode(token);
                        //sendMsg("18676365713","123456",token);
                  //      jedis.setex("serverTokenForAccessingDJZL", 27000, item.get("token"));//serverToken失效时间保守设置为7个小时30分钟
                    }
                }
            }else {
              ///  logger.info("登录德家助理返回的异常信息!");
                if(resultMap != null && resultMap.get("ret") != null){
               //     logger.info("登录德家助理返回的消息码ret:"+resultMap.get("ret"));
                }
                if(resultMap != null && StringUtils.isNotBlank(resultMap.get("msg"))){
                  //  logger.info("登录德家助理返回的消息内容msg:"+resultMap.get("msg"));
                }
            }

          //  String tokenResultString = HttpTookit.doPost("https://www.opercenter.com/oppf/service/common/sys/user_login", headerMap, contentMap, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String djLogin() throws Exception {
        Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("login_name", LOGIN_NAME);
        contentMap.put("login_pw", LOGIN_PW);
        contentMap.put("sys_area_code", SYS_AREA_CODE);
        contentMap.put("country", COUNTRY);
        String httpOrgCreateTestRtn = httpClient(LOGIN_URL,contentMap,true);
        Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
        if (resultMap != null && resultMap.get("ret") != null && resultMap.get("ret").equals("0") && resultMap.get("items") != null) {
            List<Map<String, String>> list = JSON.parseObject(resultMap.get("items"), new TypeReference<List<Map<String, String>>>() {});
            if (list != null && list.size() > 0) {
                Map<String, String> item = list.get(0);
                if (item != null && item.get("token") != null) {
                    return item.get("token");
                }
            }
        }else {
            ///  logger.info("登录德家助理返回的异常信息!");
            if(resultMap != null && resultMap.get("ret") != null){
                //     logger.info("登录德家助理返回的消息码ret:"+resultMap.get("ret"));
            }
            if(resultMap != null && StringUtils.isNotBlank(resultMap.get("msg"))){
                //  logger.info("登录德家助理返回的消息内容msg:"+resultMap.get("msg"));
            }
        }
        return null;
    }

    /**
     *  发送验证码
     * @param phone
     * @param businessId
     */
    public static boolean sendVerificationCode(String phone,String businessId) throws Exception {
        Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("notice_type", "1");
        contentMap.put("notice_obj", phone);
        contentMap.put("notice_msg", "123");
        contentMap.put("business_id", businessId);
        contentMap.put("sys_area_code", SYS_AREA_CODE);
        contentMap.put("country",COUNTRY);
        String httpOrgCreateTestRtn = httpClient(SEND_VERIFICATIONCODE_URL,contentMap,false);
        Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
        return resultMap.get("data_string").equals("Y");
    }


    /**
     *  效验验证码
     * @param phone
     * @param businessId
     */
    public static boolean checkVerificationCode(String phone,String businessId,String verification_code) throws Exception {
        Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("notice_type", "1");
        contentMap.put("notice_obj", phone);
        contentMap.put("verification_code", verification_code);
        contentMap.put("business_id", businessId);
        contentMap.put("sys_area_code", SYS_AREA_CODE);
        contentMap.put("country",COUNTRY);
        String httpOrgCreateTestRtn = httpClient(CHECK_VERIFICATIONCODE_URL,contentMap,false);
        Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
        return resultMap.get("data_string").equals("Y");
    }



    private static String httpClient(String url,Map<String, String> contentMap,boolean isLogin) throws Exception {
        String josnMd5 = MD5Util.test( JSONObject.toJSONString(contentMap),SECURECODE, 1);
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("appkey", APPKEY);
        headerMap.put("verification", josnMd5);
        headerMap.put("version", VERSION);
        headerMap.put("language",LANGUAGE);
        if(!isLogin){
            headerMap.put("token",djLogin());
        }
        headerMap.put("logincode", LOGIN_NAME);
        headerMap.put("apptype", APPTYPE);
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        return httpClientUtil.doPost(url,headerMap,contentMap,"UTF-8");
    }





    private static void sendMsg(String token){
        Map<String, String> contentMap = new HashMap<String, String>();
            contentMap.put("notice_type", "1");
	    	contentMap.put("notice_obj", "18676365713");
	    	contentMap.put("notice_msg", "122");
	    	contentMap.put("business_id", "112123312132");
            contentMap.put("sys_area_code", "440100");
            contentMap.put("country", "86");
        try {
            String jsonString = JSONObject.toJSONString(contentMap);
            System.out.println("json:============"+jsonString);
            String md5 = MD5Util.test(jsonString, "d6d3a5e9-3acf-5452-4c30-fbf25ecf852e", 1);


            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put("appkey", "2a86ad9f-b3b6-02ed-f087-88770ca3c36e");
            headerMap.put("verification", md5);
            headerMap.put("version", "1.0");
            headerMap.put("language", "zh_CN");
            headerMap.put("logincode", "W44G9WRB");
            headerMap.put("apptype", "16");
            headerMap.put("token", token);
            HttpClientUtil httpClientUtil = new HttpClientUtil();
            String httpOrgCreateTestRtn = httpClientUtil.doPost("https://www.opercenter.com/oppf/service/common/notice/send_verificationcode",headerMap,contentMap,"UTF-8");
            System.out.println(httpOrgCreateTestRtn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void checkVerificationCode(String token){
        Map<String, String> contentMap = new HashMap<String, String>();
        contentMap.put("notice_type", "1");
        contentMap.put("notice_obj", "18676365713");
        contentMap.put("verification_code", "369736");
        contentMap.put("business_id", "123456");
        contentMap.put("sys_area_code", "440100");
        contentMap.put("country", "86");
        try {
            String jsonString = JSONObject.toJSONString(contentMap);
            System.out.println("json:============"+jsonString);
            String md5 = MD5Util.test(jsonString, "d6d3a5e9-3acf-5452-4c30-fbf25ecf852e", 1);


            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put("appkey", "2a86ad9f-b3b6-02ed-f087-88770ca3c36e");
            headerMap.put("verification", md5);
            headerMap.put("version", "1.0");
            headerMap.put("language", "zh_CN");
            headerMap.put("logincode", "W44G9WRB");
            headerMap.put("apptype", "16");
            headerMap.put("token", token);
            HttpClientUtil httpClientUtil = new HttpClientUtil();
            String httpOrgCreateTestRtn = httpClientUtil.doPost("https://www.opercenter.com/oppf/service/common/notice/check_verificationcode",headerMap,contentMap,"UTF-8");
            Map<String, String> resultMap = JSON.parseObject(httpOrgCreateTestRtn, new TypeReference<Map<String, String>>() {});
            System.out.println(resultMap.get("data_string").equals("Y"));
            System.out.println(httpOrgCreateTestRtn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
















}
