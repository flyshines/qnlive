package qingning.common.util;

public class TenPayConstant {
    public static final String getPrePayUrl(String appName){
       // public static final String PRE_PAY_URL = "weixin_pay_pre_pay_url";// MiscUtils.getConfigByKey("weixin_pay_pre_pay_url");
        return MiscUtils.getConfigByKey("weixin_pay_pre_pay_url",appName);
    }

    public static final String getAppId(String appName){
        //  public static final String APP_ID = "appid";//MiscUtils.getConfigByKey("appid");
        return MiscUtils.getConfigByKey("appid",appName);
    }

    public static final String getMchId(String appName){
        //public static final String MCH_ID = "weixin_pay_mch_id";//MiscUtils.getConfigByKey("weixin_pay_mch_id");
        return MiscUtils.getConfigByKey("weixin_pay_mch_id",appName);
    }



//    public static final String CHECK_PAY_RESULT_URL = MiscUtils.getConfigByKey("weixin_pay_check_result_url");


    public static String getNotifyUrl(String appName) {
     //   public static final String NOTIFY_URL = "weixin_pay_notify_url";//MiscUtils.getConfigByKey("weixin_pay_notify_url");

        return MiscUtils.getConfigByKey("weixin_pay_notify_url",appName);
    }

    public static String getAppKey(String appName) {
        // public static final String APP_KEY = "weixin_pay_app_key";//MiscUtils.getConfigByKey("weixin_pay_app_key");
        return MiscUtils.getConfigByKey("weixin_pay_app_key",appName);
    }

    public static String getOrderQueryUrl(String appName) {
        //  public static final String ORDER_QUERY_URL = "weixin_pay_order_query_url";//MiscUtils.getConfigByKey("weixin_pay_order_query_url");
        return MiscUtils.getConfigByKey("weixin_pay_order_query_url",appName);
    }

    public static String getRefundUrl(String appName) {
        // public static final String REFUND_URL ="weixin_refund_url";// MiscUtils.getConfigByKey("weixin_refund_url");
        return MiscUtils.getConfigByKey("weixin_refund_url",appName);
    }

    public static String getFAIL() {
        return FAIL;
    }

    public static String getSUCCESS() {
        return SUCCESS;
    }

    public static String getAppAppId(String appName) {
        //    public static final String APP_APP_ID ="app_app_id";// MiscUtils.getConfigByKey("app_app_id");
        return MiscUtils.getConfigByKey("app_app_id",appName);
    }

    public static String getAppMchId(String appName) {
        // public static final String APP_MCH_ID = "weixin_app_pay_mch_id";//MiscUtils.getConfigByKey("weixin_app_pay_mch_id");
        return MiscUtils.getConfigByKey("weixin_app_pay_mch_id",appName);
    }

    public static String getAppAppKey(String appName) {
        //   public static final String APP_APP_KEY = "weixin_app_pay_app_key";//MiscUtils.getConfigByKey("weixin_app_pay_app_key");
        return MiscUtils.getConfigByKey("weixin_app_pay_app_key",appName);
    }




    public static final String REFUND_URL ="weixin_refund_url";// MiscUtils.getConfigByKey("weixin_refund_url");
    public static final String FAIL = "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg></return_msg></xml>";
    public static final String SUCCESS = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";





//    public static final String APP_APP_ID = "wx2e1a960d749aaf39";
//    public static final String APP_MCH_ID = "1429696002";
//    public static final String APP_APP_KEY = "qingninglive987654321shaoyibolzd";

}
