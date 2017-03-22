package qingning.common.util;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.*;

/**
 * Created by qingning on 2015/9/22.
 */
public class SendMsgUtil {

    public static void main(String[] args) {
        sendMsgCode("18676365713", "短信测试青柠！");
//    	getMsgDeliver();
    }
    private static ResourceBundle bundle = null;

    
    /**
     * 发短信验证码
     */
    public static String sendMsgCode(String phone, String content) {
        String str = "";
        List<String> list = new ArrayList<String>();

        for (String temp : phone.split(",")){
            if(StringUtils.isNotBlank(temp) && !list.contains(temp))
                list.add(temp);
        }
        //短信接口验证
        Map<String, String> params = new HashMap<String, String>();
        String msgUrl = "http://61.145.229.29:7791/MWGate/wmgw.asmx/MongateSendSubmit";//;//http://61.145.229.29:9006/MWGate/wmgw.asmx/MongateCsSpSendSmsNew?userId=J01967&password=901882&pszMobis=
        params.put("userId", "H11559");//msg_user_id=J03219
        params.put("password", "262247");//msg_password=262241

        params.put("pszMobis", phone);
        params.put("pszMsg", content);
        params.put("iMobiCount", list.size()+"");
        params.put("pszSubPort", "*");
        params.put("MsgId", "0");

        String postData = HttpTookit.doPost(msgUrl, params);
        try {
            Document doc = DocumentHelper.parseText(postData); // 将字符串转为XML;
            Element rootElt = doc.getRootElement(); // 获取根节点
            str = rootElt.getText();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return str;
    }


//    /**
//     * 发短信消息
//     * 舞队邀请对应subPost  1013
//     */
//    public static String sendMsgContent(String phone, String content, String subPort) {
//        String str = "";
//        List<String> list = new ArrayList<String>();
//
//        for (String temp : phone.split(","))
//        {
//            if(StringUtils.isNotBlank(temp) && !list.contains(temp))
//                list.add(temp);
//        }
//        //短信接口验证
//        Map<String, String> params = new HashMap<String, String>();
//        String msgUrl = bundle.getString("msg_url");//;//http://61.145.229.29:9006/MWGate/wmgw.asmx/MongateCsSpSendSmsNew?userId=J01967&password=901882&pszMobis=
//        params.put("userId", bundle.getString("msg_user_id"));//
//        params.put("password", bundle.getString("msg_password"));//
//        params.put("pszMobis", phone);
//        params.put("pszMsg", content);
//        params.put("iMobiCount", list.size()+"");
//        params.put("pszSubPort", subPort);
//        params.put("MsgId", "9223372036854775806");
//
//        String postData = HttpTookit.doPost(msgUrl, params);
//        System.out.println(postData);
//        try {
//            Document doc = DocumentHelper.parseText(postData); // 将字符串转为XML;
//            Element rootElt = doc.getRootElement(); // 获取根节点
//            str = rootElt.getText();
////            System.out.println("根节点：" + rootElt.getText()); // 拿到根节点的名称
//        } catch (DocumentException e) {
//            e.printStackTrace();
//        }
//
//        return str;
//    }
//
//
//    /**
//     * 获取上行短信消息
//     * */
//    public static String getMsgDeliver()
//    {
//    	String str = "";
//    	//短信接口验证
//        Map<String, String> params = new HashMap<String, String>();
//        String msgUrl = bundle.getString("msg_deliver_url");//;//http://61.145.229.29:9006/MWGate/wmgw.asmx/MongateCsSpSendSmsNew?userId=J01967&password=901882&pszMobis=
//        params.put("userId", bundle.getString("msg_user_id"));//
//        params.put("password", bundle.getString("msg_password"));//
//        params.put("iReqType", "1");
//
//
//        String postData = HttpTookit.doPost(msgUrl, params);
//        /**
//         * 返回格式
//         * <?xml version="1.0" encoding="utf-8"?><ArrayOfString xmlns="http://tempuri.org/"><string>1,2016-03-15 16:28:06,15002078832,106903290157981013,1013,*,回复test</string>
//			</ArrayOfString>
//         * */
//        System.out.println(postData);
//
//    	return str;
//    }

    /**
     * 返回结果判断
     * */
    public static String validateCode(String str)
    {
        String msg = "";
        if("-1".equals(str))
        {
            msg += "参数为空。信息、电话号码等有空指针，登陆失败";
        }
        else if("-2".equals(str))
        {
            msg += "电话号码个数超过100";
        }
        else if("-10".equals(str))
        {
            msg += "申请缓存空间失败";
        }else if("-11".equals(str))
        {
            msg += "电话号码中有非数字字符";
        }else if("-12".equals(str))
        {
            msg += "有异常电话号码";
        }else if("-13".equals(str))
        {
            msg += "电话号码个数与实际个数不相等";
        }else if("-14".equals(str))
        {
            msg += "实际号码个数超过100";
        }else if("-101".equals(str))
        {
            msg += "发送消息等待超时";
        }else if("-102".equals(str))
        {
            msg += "发送或接收消息失败";
        }else if("-103".equals(str))
        {
            msg += "接收消息超时";
        }else if("-200".equals(str))
        {
            msg += "其他错误";
        }else if("-999".equals(str))
        {
            msg += "服务器内部错误";
        }else if("-10001".equals(str))
        {
            msg += "用户登陆不成功(帐号不存在/停用/密码错误)";
        }else if("-10003".equals(str))
        {
            msg += "用户余额不足";
        }else if("-10011".equals(str))
        {
            msg += "信息内容超长";
        }else if("-10029".equals(str))
        {
            msg += "此用户没有权限从此通道发送信息(用户没有绑定该性质的通道，比如：用户发了小灵通的号码)";
        }else if("-10030".equals(str))
        {
            msg += "不能发送移动号码";
        }else if("-10031".equals(str))
        {
            msg += "手机号码(段)非法";
        }else if("-10057".equals(str))
        {
            msg += "IP受限";
        }else if("-10056".equals(str))
        {
            msg += "连接数超限";
        }
        else
            msg = "success";

        return msg;
    }

}
