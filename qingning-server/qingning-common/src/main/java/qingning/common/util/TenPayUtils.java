package qingning.common.util;

import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.ServletInputStream;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TenPayUtils {

    public static final String  ALL_CHAR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Logger logger   = LoggerFactory.getLogger (TenPayUtils.class);
    private static DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * 格式化string为Date
     *
     * @param datestr
     * @return date
     */
    public static Date parseDate(String datestr) {
        if(com.alibaba.dubbo.common.utils.StringUtils.isBlank(datestr)){
            return null;
        }
        Date date = null;
        try {
            date  = dateTimeFormat.parse(datestr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }


    public static String getOrderNo(){
        return UUID.randomUUID ().toString ().replaceAll ("-", "");
    }

    public static String getRandomStr(){
        StringBuffer sb = new StringBuffer ();
        Random random = new Random ();
        for ( int i = 0 ; i < 32 ; i++ ) {
            sb.append (ALL_CHAR.charAt (random.nextInt (ALL_CHAR.length ())));
        }
        return sb.toString ();
    }

    public static String getSign(Map<String, String> params, String type,String appName){
        StringBuilder sb = new StringBuilder ();
        Set es = params.entrySet();//所有参与传参的参数按照accsii排序（升序）
        Iterator it = es.iterator();
        while (it.hasNext ()) {
            Map.Entry entry = (Map.Entry)it.next();
            String k = (String)entry.getKey ();
            Object v = entry.getValue ();
            sb.append (k + "=" + v + "&");
        }
        String app_key = null;
        if (type == null || type.equals("0") || type.equals(3)) { //web
            app_key = TenPayConstant.getAppKey(appName);
        } else { //app
            app_key = TenPayConstant.getAppAppKey(appName);
        }
        System.out.println (sb.toString () + "key=" + app_key);
        return MD5Util.getMD5(sb.toString() + "key=" + app_key).toUpperCase ();
    }

    public static Map<String, String> sendPrePay(String goodName,Integer totalFee,String terminalIp,String outTradeNo,String openid, String type,String appName) throws IOException,ParserConfigurationException,SAXException{
        Map<String, String> params = createParams (goodName, totalFee, terminalIp, outTradeNo, openid, type,appName);
        params.put ("sign", getSign (params, type,appName));
        logger.debug("-----微信预支付请求参数"+ params);
        String response = TenPayHttpClientUtil.doPost (TenPayHttpClientUtil.getHttpURLConnection(TenPayConstant.getPrePayUrl(appName)), TenPayXmlUtil.doXMLCreate(params).getBytes());
        Map<String, String> payResultMap = TenPayXmlUtil.doXMLParse(response);
        logger.debug("-----生成微信预支付单结果"+ payResultMap.toString());
        return payResultMap;
    }

    public static Map<String, String> checkPayResult(String outTradeNo, String type,String appName) throws Exception {
        Map<String, String> params = new TreeMap<String, String> ();
        params.put ("nonce_str", getRandomStr ());
        params.put ("out_trade_no", outTradeNo);
        if (type == null || type.equals("0") || type.equals(3)) {//web
            params.put ("appid", TenPayConstant.getAppId(appName));
            params.put ("mch_id", TenPayConstant.getMchId(appName));
        } else {//app
            params.put("appid", TenPayConstant.getAppAppId(appName));
            params.put("mch_id", TenPayConstant.getAppMchId(appName));
        }
        params.put ("sign", getSign (params, type,appName));

        String response = TenPayHttpClientUtil.doPost (TenPayHttpClientUtil.getHttpURLConnection(TenPayConstant.getOrderQueryUrl(appName)), TenPayXmlUtil.doXMLCreate(params).getBytes());
        Map<String, String> payResultMap = TenPayXmlUtil.doXMLParse(response);

        return payResultMap;
    }

    public static Map<String, String> sendRefundApply(String outTradeNo, String outRefundNo, Integer totalFee,Integer refundFee,String opUserId, String refundAccount,String appName) throws Exception {
        Map<String, String> params = createRefundParams(outTradeNo, outRefundNo, totalFee, refundFee, opUserId, refundAccount,appName);
        params.put ("sign", getSign (params, "1",appName));
        String weixin_refund_url = MiscUtils.getConfigByKey("weixin_refund_url",appName);
        CloseableHttpClient httpclient = TenPayHttpClientUtil.getWinxinRefundHttpClient(MiscUtils.getConfigByKey("weixin_pay_mch_id",appName));
        String sendContent = TenPayXmlUtil.doXMLCreate(params);
        logger.debug("------发起微信退款请求参数"+ params);
        String response = TenPayHttpClientUtil.doPost(weixin_refund_url, sendContent, httpclient);
        Map<String, String> resultMap;
        resultMap = TenPayXmlUtil.doXMLParse(response);
        logger.debug("------发起微信退款结果"+ resultMap.toString());
        return resultMap;
    }


    private static Map<String, String> createRefundParams(String outTradeNo, String outRefundNo, Integer totalFee, Integer refundFee, String opUserId, String refundAccount,String appName) {
        Map<String, String> params = new TreeMap<String, String> ();
        params.put ("appid", TenPayConstant.getAppId(appName));
        params.put ("mch_id", TenPayConstant.getMchId(appName));
        params.put ("nonce_str", getRandomStr ());
        params.put ("out_trade_no", outTradeNo);
        params.put ("nonce_str", getRandomStr ());
        params.put ("out_refund_no", outRefundNo);
        params.put ("total_fee", totalFee.toString());
        params.put ("refund_fee", refundFee.toString());
        if(StringUtils.isBlank(opUserId)){
            params.put ("op_user_id", TenPayConstant.getMchId(appName));
        }else {
            params.put ("op_user_id", opUserId);
        }

        if(StringUtils.isNotBlank(refundAccount)){
            params.put ("op_user_id", refundAccount);
        }

        return params;
    }

    public static void main(String[] args) throws Exception {
//        String outTradeNo = "201612230100015";
//        String outRefundNo = MiscUtils.getUUId();
//        Integer totalFee = 1001;
//        Integer refundFee=1001;
//        sendRefundApply(outTradeNo, outRefundNo, totalFee, refundFee, null,null);
    }



    private static Map<String, String> createParams(String goodName,Integer totalFee,String terminalIp,String outTradeNo,String openid,String type,String appName){
        Map<String, String> params = new TreeMap<String, String> ();
        params.put ("body", goodName);
        params.put ("nonce_str", getRandomStr ());
        params.put ("notify_url", TenPayConstant.getNotifyUrl(appName));
        params.put ("out_trade_no", outTradeNo);
        params.put ("spbill_create_ip", terminalIp);
        params.put ("total_fee", totalFee.toString ());

        if (type == null || type.equals("0") || type.equals(3)) {//web
            params.put ("openid", openid);
            params.put ("appid", TenPayConstant.getAppId(appName));
            params.put ("trade_type", "JSAPI");
            params.put ("mch_id", TenPayConstant.getMchId(appName));
        } else {//app
            params.put("appid", TenPayConstant.getAppAppId(appName));
            params.put("trade_type", "APP");
            params.put("mch_id", TenPayConstant.getAppMchId(appName));
        }

        return params;
    }

    public static boolean checkSign(Map<String, String> params,String sign){
        // if (params.remove ("sign") != null) { return getSign (params).equals (sign); }
        return true;
    }

    public static TradeState queryOrder(String transactionId,String outTradeNo,String appName) throws IOException,ParserConfigurationException,SAXException{
        String tradeState = "";
        Map<String, String> params = new TreeMap<String, String> ();
        params.put ("appid", TenPayConstant.getAppId(appName));
        params.put ("mch_id", TenPayConstant.getMchId(appName));
        params.put ("nonce_str", getRandomStr ());
        params.put ("out_trade_no", outTradeNo);
        params.put ("sign", getSign (params, "0",appName));
        String response = TenPayHttpClientUtil.doPost (TenPayHttpClientUtil.getHttpURLConnection(TenPayConstant.getOrderQueryUrl(appName)), TenPayXmlUtil.doXMLCreate(params).getBytes());
        Map<String, String> returnMap = TenPayXmlUtil.doXMLParse (response);
        if ("SUCCESS".equals (returnMap.get ("return_code")) && "SUCCESS".equals (returnMap.get ("result_code"))) {
            tradeState = returnMap.get ("trade_state");
            return getTradeState (tradeState);
        } else {
            String errDesc = returnMap.get ("err_code_des");
            TradeState.OTHER_ERROR.setStateInfo (errDesc);
            return TradeState.OTHER_ERROR;
        }
    }

    public static Map<String, String> getRequestBody(ServletInputStream is) throws IOException,ParserConfigurationException,SAXException{
        BufferedReader streamReader;
        streamReader = new BufferedReader (new InputStreamReader (is,"UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder ();
        int n = 1024;
        char buffer[] = new char[n];
        while (streamReader.read (buffer, 0, n) != -1) {
            responseStrBuilder.append (new String (buffer));
        }
        return TenPayXmlUtil.doXMLParse (responseStrBuilder.toString ().trim ());
    }

    public enum TradeState {
        SUCCESS (0, "支付成功"), REFUND (1, "转入退款"), NOTPAY (2, "未支付"), CLOSED (3, "已关闭"), REVOKED (4, "已撤销"), USERPAYING (5, "用户支付中"), PAYERROR (6, "支付失败"), OTHER_ERROR (7, "其它错误");

        private String stateInfo;
        private int    code;

        private TradeState(int code, String stateInfo) {
            this.code = code;
            this.stateInfo = stateInfo;
        }

        public String getStateInfo(){
            return stateInfo;
        }

        public void setStateInfo(String stateInfo){
            this.stateInfo = stateInfo;
        }

        public int getCode(){
            return code;
        }
    }

    private static TradeState getTradeState(String state){
        if (state.equals ("SUCCESS")) { return TradeState.SUCCESS; }
        if (state.equals ("REFUND")) { return TradeState.REFUND; }
        if (state.equals ("NOTPAY")) { return TradeState.NOTPAY; }
        if (state.equals ("CLOSED")) { return TradeState.CLOSED; }
        if (state.equals ("REVOKED")) { return TradeState.REVOKED; }
        if (state.equals ("USERPAYING")) { return TradeState.USERPAYING; }
        if (state.equals ("PAYERROR")) { return TradeState.PAYERROR; }
        return TradeState.OTHER_ERROR;
    }

    /**
     * 微信notify 验签方法验签方法。
     * @param requestMapData
     * @return
     * @throws Exception
     */
    public static boolean isValidSign(SortedMap<String, String> requestMapData,String appName) throws Exception {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : requestMapData.entrySet()) {
            if(!"sign".equalsIgnoreCase(entry.getKey())){
                sb.append(entry.getKey() + "=" + entry.getValue());
                sb.append("&");
            }
        }
        String trade_type = requestMapData.get("trade_type");
        if (trade_type.equals("APP")) {
            sb.append("key=" + TenPayConstant.getAppAppKey(appName));
        } else  {
            sb.append("key=" + TenPayConstant.getAppKey(appName));
        }

        String sign = MD5Util.getMD5(sb.toString());
        String ValidSign = requestMapData.get("sign").toUpperCase();
        return ValidSign.equals(sign);
    }


}
