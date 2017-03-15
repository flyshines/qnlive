package qingning.common.util;

public class TenPayConstant {
    public static final String PRE_PAY_URL = MiscUtils.getConfigByKey("weixin_pay_pre_pay_url");
    public static final String APP_ID = MiscUtils.getConfigByKey("appid");
    public static final String MCH_ID = MiscUtils.getConfigByKey("weixin_pay_mch_id");
    public static final String NOTIFY_URL = MiscUtils.getConfigByKey("weixin_pay_notify_url");
    public static final String APP_KEY = MiscUtils.getConfigByKey("weixin_pay_app_key");
    public static final String ORDER_QUERY_URL = MiscUtils.getConfigByKey("weixin_pay_order_query_url");
    public static final String PARTNER_ID = MiscUtils.getConfigByKey("weixin_pay_partner_id");
    public static final String REFUND_URL = MiscUtils.getConfigByKey("weixin_refund_url");
    public static final String FAIL = "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg></return_msg></xml>";
    public static final String SUCCESS = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";

    public static final String APP_APP_ID = MiscUtils.getConfigByKey("app_app_id");
    public static final String APP_MCH_ID = MiscUtils.getConfigByKey("weixin_app_pay_mch_id");
    public static final String APP_APP_KEY = MiscUtils.getConfigByKey("weixin_app_pay_app_key");
}
