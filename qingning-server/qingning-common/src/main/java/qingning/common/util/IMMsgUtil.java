package qingning.common.util;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by loovee1 on 2016/4/5.
 */
public class IMMsgUtil {

    private static final Logger logger = LoggerFactory.getLogger(IMMsgUtil.class);
    // 获取配置文件信息
    public static Map<String,String> configMap;

    static {

        try {
            configMap = MiscUtils.convertPropertiesFileToMap("classpath:application.properties");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 注册IM账号
     * deviceid：设备ID
     */
    public static Map<String,String> createIMAccount(String deviceid){
        // app_id
        String appId = configMap.get("app_id");
        // 签名参数
        String sign = "appid=" + appId + "deviceid=" + deviceid + configMap.get("app_key");
        // 请求地址 url + "?appid=" + appId + "&deviceid=" + deviceId + "&sign=" + MD5Util.getMD5 (sign).toLowerCase ();
        String address = configMap.get("url") + "/mgr/register?appid=" +  appId + "&deviceid=" + deviceid + "&sign=" + MD5Util.getMD5 (sign).toLowerCase ();
        return parseText(HttpTookit.doGet(address));
    }

    /*
     * 自定义发送群组系统消息
     * mGroupId：IM群组ID
     * content：发送内容
     */
    public static String sendMessageInIM(String mGroupId, String content,String custom,String sender) {
        try {
            // app_id
            String appId = configMap.get("app_id");
            String AESEncryptString ;
            String aesKey = configMap.get("im_custom_secret_key");
            String aesIV = configMap.get("im_custom_iv");
            // 自定义参数
            String beforeCustom = custom;
            //custom = new BASE64Encoder().encode(custom.getBytes()).replaceAll("\r|\n", "");
            //AESEncryptString = CryptAES.AES_Encrypt(custom, aesKey, aesIV);
            AESEncryptString = custom;
            // 签名参数
            String sign = "appid=" + appId + "custom=<mytype>" + AESEncryptString + "</mytype>groupid=" + mGroupId + "sender=" + sender + configMap.get("app_key");
            // 请求地址
            String address = configMap.get("url") + "/mgr/groupmsg?appid=" + appId + "&custom=%3Cmytype%3E" + AESEncryptString + "%3C%2Fmytype%3E&content=" + URLEncoder.encode(content, "UTF-8") + "&groupid=" + mGroupId + "&sender=" + sender + "&sign=" + MD5Util.getMD5(sign).toLowerCase();;
            // 发送请求
            String sendResult = HttpTookit.doGet(address);

            String md5Sing = MD5Util.getMD5(sign).toLowerCase();
            logger.debug("======发送群聊IM消息======" + "签名:" + md5Sing + "        发送者:" + sender + "       接收者:" + mGroupId + "       内容:" + content + "       custom信息:" + beforeCustom);
            logger.debug("======发送群聊IM消息======" + "签名:" + md5Sing + "        地址:" + address);
            logger.debug("======发送群聊IM消息======" + "签名:" + md5Sing + "        发送结果:" + sendResult);

            return sendResult;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }



    /*
     * 删除IM群组
     * operator：操作者ID
     * mGroupId：IM群组ID
     */
    public static String deleteIMGroup(String operator,String mGroupId){
        // app_id
        String appId = configMap.get("app_id");
        // 签名参数
        String sign = "appid=" + appId + "groupid=" + mGroupId + "operator=" + operator + "type=delete" + configMap.get("app_key");
        // 请求地址
        String address = configMap.get("url") + "/mgr/group?appid=" + appId + "&groupid=" + mGroupId + "&operator=" + operator + "&type=delete&sign=" + MD5Util.getMD5(sign).toLowerCase();
        return HttpTookit.doGet(address);
    }

    /*
     * 创建IM群组
     * creator：创建者ID
     */
    public static Map<String,String> createIMGroup(String creator){
        // app_id
        String appId = configMap.get("app_id");
        // 签名参数
        String sign = "appid=" + appId + "creator=" + creator + "type=create" + configMap.get("app_key");
        // 请求地址
        String address = configMap.get("url") + "/mgr/group?appid=" + appId + "&creator=" + creator + "&type=create&sign=" + MD5Util.getMD5(sign).toLowerCase();
        String result = HttpTookit.doGet(address);
        return parseText(result);
    }

    /*
     * 加入IM群组
     * mGroupId：IM组ID
     * mUserId：IM用户ID
     * operator：IM操作者用户ID
     */
    public static String joinGroup(String mGroupId, String mUserId, String operator){
        String appId = configMap.get("app_id");
        String sign = "appid=" + appId + "groupid=" + mGroupId + "operator=" + operator + "type=addmemberuid=" + mUserId + configMap.get("app_key");
        String address = configMap.get("url") + "/mgr/group?appid=" + appId + "&groupid=" + mGroupId + "&operator=" + operator + "&type=addmember" + "&uid=" + mUserId + "&sign=" + MD5Util.getMD5(sign).toLowerCase();
        return HttpTookit.doGet(address);
    }


    /*
     * 删除IM群组成员
     * mGroupId：IM组ID
     * mUserId：IM用户ID
     * operator：IM操作者用户ID
     */
    public static String delGroupMember(String mGroupId, String mUserId, String operator){
        String appId = configMap.get("app_id");
        String sign = "appid=" + appId + "groupid=" + mGroupId + "operator=" + operator + "type=delmemberuid=" + mUserId + configMap.get("app_key");
        String address = configMap.get("url") + "/mgr/group?appid=" + appId + "&groupid=" + mGroupId + "&operator=" + operator + "&type=delmember" + "&uid=" + mUserId + "&sign=" + MD5Util.getMD5(sign).toLowerCase();
        return HttpTookit.doGet(address);
    }

    /*
     * 判断返回结果
     * 成功：true
     * 失败：false
     */
    public static boolean isSuccess(String result){
        try {
            Document doc = DocumentHelper.parseText(result);
            Element root = doc.getRootElement();
            if("result".equalsIgnoreCase(root.getName())){
                return true;
            }
            return false;
        } catch (DocumentException e) {
            e.printStackTrace();
            return false;
        }
    }


    /*
     * 解析返回结果
     */
    public static Map<String,String> parseText(String result){
        Map<String,String> map = new HashMap<String, String>();
        try {
            Document doc = DocumentHelper.parseText(result);
            Element root = doc.getRootElement();
            if("success".equalsIgnoreCase(root.getText())){
                map.put("result",root.getText());
                return map;
            }
            if("error".equalsIgnoreCase(root.getName())){
                map.put("error",root.getText());
                return map;
            }
            Iterator it = root.elementIterator();
            while (it.hasNext()) {
                Element e = (Element) it.next();
                map.put(e.getName(),e.getText());
            }
            return map;
        } catch (DocumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
   * 发送单条消息
   */
    public static boolean sendMessageForP2P(String sender, String receiver, String content, String custom)
    {
        try
        {
            String appId = configMap.get("app_id");
            String appKey = configMap.get("app_key");

            String beforeCustom = custom;
            String address;
            String sign;
            String AESEncryptString = "";
            String aesKey = configMap.get("im_custom_secret_key");
            String aesIV = configMap.get("im_custom_iv");

            if (StringUtils.isNotBlank(custom))
            {
                AESEncryptString = CryptAES.AES_Encrypt(custom,aesKey,aesIV);

                sign = "appid=" + appId + "custom=<mytype>" + AESEncryptString + "</mytype>" + "receiver=" + receiver + "sender=" + sender + appKey;
                address = configMap.get("url") + "/mgr/sendmsg?appid=" + appId + "&content=" + URLEncoder.encode(content, "UTF-8") + "&custom=%3Cmytype%3E" + AESEncryptString + "%3C%2Fmytype%3E" + "&receiver=" + receiver + "&sender=" + sender + "&sign=" + MD5Util.getMD5(sign).toLowerCase();
            }
            else
            {
                sign = "appid=" + appId + "receiver=" + receiver + "sender=" + sender + appKey;
                address = configMap.get("url") + "/mgr/sendmsg?appid=" + appId + "&content=" + URLEncoder.encode(content, "UTF-8") + "&receiver=" + receiver + "&sender=" + sender + "&sign=" + MD5Util.getMD5(sign).toLowerCase();
            }
            String md5Sing = MD5Util.getMD5(sign).toLowerCase();
            String result = HttpTookit.doGet(address);

            logger.debug("======发送单条IM消息======签名:" + md5Sing + "        发送者:" + sender + "       接收者:" + receiver + "       内容:" + content + "       custom信息:" + beforeCustom);
            logger.debug("======发送单条IM消息======签名:" + md5Sing + "        发送结果:" + result);

            return isSuccess(result);
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            System.out.println("发送单条聊天信息异常" + e);
        }
        return false;
    }

    public static void main(String[] args) throws Exception{

//        Map<String,String> map = createIMAccount("1234sfsfdsfds");
//        System.out.println(map);
        Map<String,String> map = createIMGroup("854165");
        System.out.println(map);
//        String groupid = "13843";
//        String muserid= "855179";
//        joinGroup(groupid,muserid,muserid);
//        String content = "pppsdmkkj测试推送AES6652";
//        String custom = "{\"type\":\"editgroup\",\"group_id\":\"443\",\"member\":{\"user_id\":\"68\",\"m_user_id\":\"600506\",\"group_nick_name\":\"数字证书test\"}}\n";
        //String custom = "looo+pppp+oooo+kkkkkk";
        //sendMessageForP2P("system","600561",content,custom);
        //sendMessageInIM("319","test",custom,"system");

        //String test = "{\"msg_type\":\"joingroup\",\"type\":\"joingroupmsg_applicant_agree\",\"apply_id\":999}";
        //String aaa = new String(Base64.encodeBase64(test.getBytes()));
        //System.out.println(aaa);
//        String test ="eyJ0eXBlIjoiYWRkZnJpZW5kIiwiYXBwbHlfaWQiOiI1MTQiLCJmcmllbmQiOnsidXNlcl9pZCI6IjI5IiwibV91c2VyX2lkIjoiNjAwNDMzIiwibmlja19uYW1lIjoi5a C6Z2Z5ZOm5ZOm5ZKq6KW/5ZKq6KW/5pWF5oSP5YiY5oWn5pWP5L2g56eD5aS0IiwiYXZhdGFyX2FkZHJlc3MiOiJodHRwOi8vN3h0M2xtLmNvbTEuejAuZ2xiLmNsb3VkZG4uY29tL2ltYWdlcy9BYjdiOTU3OGUwMjY0NjUwZGRiNTVlMDI3MmRhYjM3MTYucG5nIn19";
//        String repalce = new String(Base64.encodeBase64(URLEncoder.encode(custom, "utf-8").getBytes()));
//        System.out.println(repalce);
//        String decodeString = new String(Base64.decodeBase64(test));
//        System.out.println(decodeString);
//        String repalceBefore ="";
//        String afterCustom = "eyJ0eXBlIjoiYWRkZnJpZW5kIiwiYXBwbHlfaWQiOiI1MTgiLCJmcmllbmQiOnsidXNlcl9pZCI6IjI5IiwibV91c2VyX2lkIjoiNjAwNDMzIiwibmlja19uYW1lIjoi5a+C6Z2Z5ZOm5ZOm5ZKq6KW/5ZKq6KW/5pWF5oSP5YiYIiwiYXZhdGFyX2FkZHJlc3MiOiJodHRwOi8vN3h0M2xtLmNvbTEuejAuZ2xiLmNsb3VkZG4uY29tL2ltYWdlcy9BYjdiOTU3OGUwMjY0NjUwZGRiNTVlMDI3MmRhYjM3MTYucG5nIn19";
//        try {
//            repalceBefore  = new String(Base64.decodeBase64(afterCustom));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        boolean success = isSuccess("<result><groupid>497</groupid></result>");
//        System.out.println(repalceBefore);
        //String test = "<result>success</result>";
        //System.out.println(parseText(test));
//        Map<String,String> map = createIMAccount("1234sfsfdsfds");
//        System.out.println(map);
    }


}
