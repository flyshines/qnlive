package qingning.common.util;

public final class Constants {
	private Constants(){}
	public static final long SEQUENCE =1000000000l;
	public static final String OVERTIME_NOTICE_TYPE_30="OVERTIME_NOTICE_TYPE_30";
	public static final String SYS_FIELD_COUNTRY="country";
	public static final String SYS_FIELD_COUNTRY_SHORT="country_short";
	public static final String SYS_FIELD_PROVINCE="province";
	public static final String SYS_FIELD_CITY="city";//城市
	public static final String SYS_FIELD_DISTRICT="district";
	public static final String SYS_FIELD_LAST_UPDATE_TIME ="last_update_time";


	public static final String WE_CHAT_PUSH_COLOR = "#595959";
	public static final String WE_CHAT_PUSH_COLOR_QNCOLOR = "#5AD1A1";
	public static final String WE_CHAT_PUSH_COLOR_DLIVE = "#FD6924";

	public static final String SYS_INSERT_DISTRIBUTER ="INSERT_DISTRIBUTER";
	public static final String SYS_READ_LAST_COURSE ="SYS_READ_LAST_COURSE";
	public static final String SYS_READ_LAST_SERIES ="SYS_READ_LAST_SERIES";
	public static final String SYS_READ_USER_COURSE_LIST = "SYS_READ_USER_COURSE_LIST";
	public static final String SYS_READ_USER_ROOM_LIST = "SYS_READ_USER_ROOM_LIST";
	public static final String SYS_READ_USER_SERIES_LIST = "SYS_READ_USER_SERIES_LIST";
	public static final double DIVIDED_PROPORTION = 0.3;//分成比例

	public static final String SYS_READ_USER_BY_UNIONID = "SYS_READ_USER_BY_UNIONID";

	public static final String REFRESH = "refresh";
	public static final String SYSINT="int";
	public static final String SYSLONG="long";
	public static final String SYSDOUBLE="double";
	public static final String SYSMAP= "map";
	public static final String SYSLIST= "list";
	public static final String SYSOBJECT= "object";
	public static final String SYSDATE="date";
	public static final String SYSSTR="string";
	public static final String SYSRICHSTR="rstring";
	public static final String SYS_CLASS_NAME  = "_SYS_CLASS_NAME";
	public static final String SYS_CLASS_VALUE = "_SYS_CLASS_VALUE";
	public static final int COURSE_MAX_INTERVAL = 10;
	public static final int CACHED_MAX_COURSE_TIME_LIFE = 1000*60 *24 *7;
	public static final int MAX_QUERY_LIMIT = 1000;
	public static final String LECTURER_ROOM_LOAD = "LECTURER_ROOM_LOAD";

	public static final String DEFAULT = "default";
	public static final String SPECIAL = "_SPECIAL";
	public static final String CONVERT = "convert";
	public static final String SERVER = "server";
	public static final String NAME = "name";
	public static final String FIELDNAME = "fieldname";
	public static final String VERSION = "version";
	public static final String NUM = "num";
	public static final String CLASS = "class";
	public static final String AUTH = "auth";
	public static final String INPUTS = "inputs";
	public static final String OUTPUTS = "outputs";
	public static final String ACCESSTOKEN = "accessToken";
	public static final String APP_NAME = "appname";
	public static final String REQUIRE = "require";
	public static final String TIMESLIMIT = "timesLimit";
	public static final String MILLISECOND = "millisecond";
	public static final String FORMAT = "format";
	public static final String TYPE = "type";
	public static final String IM_TYPE_CHAT="chat";
	//public static final String ERRORCODE = "errorcode";
	public static final String VALIDATE = "validate";
	public static final String PARAM = "param";
	public static final String REDIS_POOL_MAXACTIVE="redis.pool.maxActive";
	public static final String REDIS_POOL_MAXIDLE="redis.pool.maxIdle";
	public static final String REDIS_POOL_MAXWAIT="redis.pool.maxWait";
	public static final String REDIS_POOL_TESTONBORROW="redis.pool.testOnBorrow";
	public static final String REDIS_POOL_TESTONRETURN="redis.pool.testOnReturn";
	public static final String REDIS_POOL_OUTTIME="redis.pool.outTime";

	public static final String REDIS_IP="redis.ip";
	public static final String REDIS_PORT="redis.port";
	public static final String REDIS_PASS="redis.pass";
	public static final String LAST_VISIT_FUN="LAST_VISIT_FUN";
	public static final String LAST_VISIT_TIME="LAST_VISIT_TIME";

//	public static final String MONGODB_IP="mongodb.ip";
//	public static final String MONGODB_PORT="mongodb.port";
//	public static final String MONGODB_MAXCONNECTTIMEOUT="mongodb.maxConnectTimeout";
//	public static final String MONGODB_MAXCONNECTIONS="mongodb.maxConnections";
//	public static final String MONGODB_MAXWAITTIME="mongodb.maxWaitTime";
//	public static final String MONGODB_MAXWAITTHREADS="mongodb.maxWaitThreads";
//	public static final String MONGODB_MAXIDLETIME="mongodb.maxIdleTime";
//	public static final String MONGODB_MAXLIFETIME="mongodb.maxLifeTime";
//	public static final String MONGODB_SOKETTIMEOUT="mongodb.soketTimeout";
//	public static final String MONGODB_SOKETKEEPALIVE="mongodb.soketKeepAlive";

	public static final String FUNCTION = "function";
	public static final String VALIDATION = "validation";
	public static final String CONVERTVALUE = "convertValue";
	public static final String JS_ENGINE = "nashorn";

//	public static final String SYS_SINGLE_KEY = "SYS_SINGLE_KEY";
//	public static final String SYS_OUT_TRADE_NO_KEY = "SYS_OUT_TRADE_NO_KEY";

	public static final String CACHED_UPDATE_LECTURER_KEY="SYS:UPDATE:LECTURER:KEYS";
	public static final String CACHED_UPDATE_DISTRIBUTER_KEY="SYS:UPDATE:DISTRIBUTER:KEYS";
//	public static final String CACHED_UPDATE_ROOM_DISTRIBUTER_KEY ="SYS:UPDATE:ROOM:DISTRIBUTER:KEYS";
//	public static final String CACHED_KEY_ROOM_DISTRIBUTER_FIELD = "room_distributer_id";
	public static final String CACHED_LECTURER_KEY="SYS:LECTURER:KEYS";

	public static final String CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE="SYS:ROOM:DISTRIBUTER:RQCODE:{rq_code}";
	public static final String CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE_FIELD="rq_code";
	public static final String CACHED_UPDATE_RQ_CODE_KEY = "SYS:UPDATE:RQCODE:KEYS";

	public static final String CACHED_KEY_ROOM_DISTRIBUTER ="SYS:ROOM:{room_id}:DISTRIBUTER:{distributer_id}";

	public static final String CACHED_KEY_ACCESS_TOKEN_FIELD = "access_token";
	public static final String CACHED_KEY_ACCESS_TOKEN = "SYS:ACCESSTOKEN:{access_token}";
	public static final String CACHED_KEY_USER_FIELD ="user_id";
	public static final String CACHED_KEY_USER = "SYS:USER:{user_id}";
	public static final String CACHED_UPDATE_USER_KEY = "SYS:UPDATE:USER:KEYS";

	public static final String CACHED_KEY_LECTURER_FIELD = "lecturer_id";//讲师id
	public static final String CACHED_KEY_LECTURER = "SYS:LECTURER:{lecturer_id}";//讲师信息
	public static final String CACHED_KEY_LECTURER_ROOMS = "SYS:LECTURER:{lecturer_id}:ROOMS";//讲师直播间列表



	//public static final String CACHED_KEY_SPECIAL_LECTURER_ROOM = "SYS:LECTURER:{lecturer_id}:ROOM:{room_id}";

	public static final String FIELD_CREATE_TIME="create_time";
	public static final String FIELD_ROOM_ID="room_id";

	public static final String CACHED_KEY_DISTRIBUTER_FIELD = "distributer_id";
	public static final String CACHED_KEY_DISTRIBUTER_RECOMMEND_FIELD = "recommend_code";
	public static final String CACHED_KEY_DISTRIBUTER = "SYS:DISTRIBUTER:{distributer_id}";
	public static final String CACHED_KEY_DISTRIBUTER_RECOMMEND_INFO = "SYS:DISTRIBUTER:RECOMMEND_INFO:{recommend_code}";

	public static final String CACHED_KEY_USER_DISTRIBUTERS = "USER:{user_id}:LECTURER:{room_id}:DISTRIBUTERS";
	public static final String CACHED_KEY_USER_DISTRIBUTERS_LEN = "USER:{user_id}:LECTURER:{room_id}:DISTRIBUTERS:LEN";

	public static final String CACHED_KEY_USER_DISTRIBUTERS_ROOM_RQ = "USER:{distributer_id}:ROOM:{room_id}:RQ:{rq_code}";
	public static final String FUNCTION_DISTRIBUTERS_ROOM_RQ = "FUNCTION_DISTRIBUTERS_ROOM_RQ";

	public static final String CACHED_KEY_USER_DISTRIBUTER_COURSES = "USER:{user_id}:LECTURER:{room_id}:COURSES:{distributer_id}";
	public static final String CACHED_KEY_USER_DISTRIBUTER_COURSES_MIN_TIME = "USER:{user_id}:LECTURER:{room_id}:COURSE:{distributer_id}:MINTIME";

	public static final String CACHED_KEY_SYS_DISTRIBUTERS = "SYS:LECTURER:{room_id}:DISTRIBUTERS";

	public static final String CACHED_KEY_USER_COURSES = "USER:{user_id}:COURSES";//用户加入的课程
	public static final String CACHED_KEY_USER_ROOMS = "USER:{user_id}:ROOMS";//用户关注的直播间
	public static final String CACHED_KEY_USER_SERIES = "USER:{user_id}:SERIES";//用户订阅的系列

	public static final String CACHED_KEY_COURSE = "SYS:COURSE:{course_id}";
	public static final String CACHED_KEY_COURSE_FIELD = "course_id";
	//店铺缓存
	public static final String CACHED_KEY_SHOP = "SYS:SHOP:{shop_id}";
	public static final String CACHED_KEY_SHOP_FIELD = "shop_id";
	//店铺每日统计
	public static final String SHOP_DAY_COUNT = "SYS:SHOP:COUNT:{user_id}";
	//系列课已购人员列表(收费课程)
	public static final String CACHED_KEY_SERIES_USERS = "SYS:SERIES:USERS:{series_id}";
	//店铺付费人员列表
	public static final String CACHED_KEY_SHOP_USERS = "SYS:SHOP:USERS:{lecturer_id}";


	public static final String CACHED_KEY_COURSE_ROBOT = "SYS:COURSEROBOT:{course_id}";
	public static final String CACHED_KEY_COURSE_ROBOT_FIELD = "course_id";

	public static final String CACHED_KEY_COURSE_PPTS = "SYS:COURSE:{course_id}:PPTS";
	public static final String CACHED_KEY_COURSE_PPTS_FIELD = "ppt_list";
	public static final String CACHED_KEY_COURSE_AUDIOS = "SYS:COURSE:{course_id}:AUDIOS";
	public static final String CACHED_KEY_COURSE_AUDIOS_JSON_STRING = "SYS:COURSE:{course_id}:AUDIOS:JSON_STRING";
	public static final String CACHED_KEY_COURSE_AUDIOS_FIELD = "audio_list";
	public static final String FIELD_AUDIO_ID="audio_id";
	public static final String CACHED_KEY_COURSE_AUDIO = "SYS:COURSE:{course_id}:AUDIO:{audio_id}";

	public static final String CACHED_KEY_COURSE_PREDICTION = "SYS:LECTURER:{lecturer_id}:COURSES:PREDICTION";//讲师预告和直播的课程
	public static final String CACHED_KEY_COURSE_FINISH = "SYS:LECTURER:{lecturer_id}:COURSES:FINISH";//讲师结束的课程
	public static final String CACHED_KEY_COURSE_DEL = "SYS:LECTURER:{lecturer_id}:COURSES:DEL";//讲师删除的课程
	public static final String CACHED_KEY_COURSE_DOWN = "SYS:LECTURER:{lecturer_id}:COURSES:DOWN";//讲师下架的课程




	public static final String CACHED_KEY_USER_ROOM_SHARE = "USER:ROOM_SHARE_CODE:{room_share_code}";
	public static final String CACHED_KEY_USER_ROOM_SHARE_FIELD = "room_share_code";
	public static final String CACHED_KEY_USER_SHARE_CODES = "USER:SHARE_CODES:{lecturer_id}";

	public static final String MSG_TYPE_ATTR = "type";
	public static final String MSG_NEWSTYPE_ATTR = "newstype";
	public static final String MSG_IP_ATTR = "ip";
	public static final String MSG_MID_ATTR = "mid";
	public static final String MSG_ID_ATTR = "id";
	public static final String MSG_TO_ATTR = "to";

	public static final String MSG_FROMJID_ELEMENT = "fromjid";
	public static final String MSG_GROUPID_ELEMENT = "groupid";
	public static final String MSG_BODY_ELEMENT = "body";

	public static final String MQ_METHOD_SYNCHRONIZED = "SYNCHRONIZED";
	public static final String MQ_METHOD_ASYNCHRONIZED = "ASYNCHRONIZED";

	public static final String CACHED_KEY_ROOM = "SYS:ROOM:{room_id}";

	public static final String CACHED_KEY_PLATFORM_COURSE_PREDICTION = "SYS:COURSES:PREDICTION";//预告/正在直播
	public static final String CACHED_KEY_PLATFORM_COURSE_FINISH = "SYS:COURSES:FINISH";//已结束
	public static final String CACHED_KEY_PLATFORM_COURSE_LIVE = "SYS:COURSES:LIVE";
	public static final String CACHED_KEY_PLATFORM_COURSE_DEL = "SYS:COURSES:DEL";//已删除


	public static final String DEFAULT_SERIES_COURSE_TYPE = "0";//正在直播 默认
	public static final String CACHED_KEY_PLATFORM_SERIES_APP_PLATFORM = "SYS:SERIES:APP:PLATFORM";// zset 平台系列正在更新的  (series_id,最近更新时间+排序) 在正在直播使用  如果下架直接删掉value (series_id,上架时间)
	public static final String CACHED_KEY_SERIES_FIELD = "series_id";//系列id
	public static final String CACHED_KEY_SERIES = "SYS:SERIES:DETAIL:{series_id}";//系列 hmap 系列具体信息

	public static final String CACHED_KEY_SERIES_COURSE_UP = "SYS:SERIES:COURSE:{series_id}:UP";//zset 系列的课程 (course_id,上架时间)
	public static final String CACHED_KEY_SERIES_COURSE_DOWN = "SYS:SERIES:COURSE:{series_id}:DOWN";//zset 系列的课程 (course_id,下架时间)下架

	//讲师所有的系列
	public static final String CACHED_KEY_LECTURER_SERIES_UP = "SYS:LECTURER:{lecturer_id}:SERIES:UP";//zset 讲师所有上架系列 (series_id,上架时间)
	public static final String CACHED_KEY_LECTURER_SERIES_DOWN = "SYS:LECTURER:{lecturer_id}:SERIES:DOWN";//zset 讲师所有下架系列 (series_id,下架时间)
	//讲师不同内容系列
	public static final String SERIES_COURSE_TYPE = "series_course_type";
	public static final String CACHED_KEY_LECTURER_SERIES_COURSE_UP = "SYS:LECTURER:{lecturer_id}:SERIES:{series_course_type}:UP";//zset 讲师所有指定类型的上架系列 (series_id,上架时间)
	public static final String CACHED_KEY_LECTURER_SERIES_COURSE_DOWN = "SYS:LECTURER:{lecturer_id}:SERIES:{series_course_type}:DOWN";//zset 讲师所有指定类型的下架系列 (series_id,下架时间)


	//讲师所有的单品课程（直播间类型除外）
	public static final String CACHED_KEY_LECTURER_COURSES_NOT_LIVE_UP = "SYS:LECTURER:{lecturer_id}:COURSES_NOT_LIVE:UP";//zset 讲师所有上架单品（直播课除外） (series_id,上架时间)
	public static final String CACHED_KEY_LECTURER_COURSES_NOT_LIVE_DOWN = "SYS:LECTURER:{lecturer_id}:COURSES_NOT_LIVE";//zset 讲师所有下架单品（直播课除外） (series_id,下架时间)

   /*
    * ***************saas课程留言相关*********************
    */
	public static final String CACHED_KEY_COMMENT_FIELD = "comment_id";	//留言id匹配模式
	public static final String CACHED_KEY_COURSE_SAAS_COMMENT_ALL = "SYS:COURSE:{course_id}:SAAS_COMMENT_ALL";	//zset saas课程的留言id排序列表(score:创建时间)
	public static final String CACHED_KEY_COURSE_SAAS_COMMENT_DETAIL = "SYS:COURSE:{course_id}:{comment_id}";	//hash saas课程的留言详情




	public static final String CACHED_KEY_WEIXIN_TOKEN="SYS:WEIXIN:TOKEN";//微信token
	public static final String CACHED_KEY_WEIXIN_JS_API_TIKET="SYS:WEIXIN:JS_API_TIKET";

	public static final String CACHED_KEY_COURSE_BAN_USER_LIST="COURSE:{course_id}:BAN_USER_LIST";
	public static final String CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM="SYS:USER_NICK_NAME_INCREMENT_NUM";
	public static final String CACHED_KEY_COURSE_MESSAGE_LIST="COURSE:{course_id}:MESSAGE_LIST";
	public static final String FIELD_MESSAGE_ID="message_imid";
	public static final String CACHED_KEY_COURSE_MESSAGE="COURSE:{course_id}:MESSAGE:{message_imid}";
//	public static final String CACHED_KEY_COURSE_MESSAGE_LIST_QUESTION = "COURSE:{course_id}:MESSAGE_LIST:QUESTION";
	public static final String CACHED_KEY_COURSE_MESSAGE_LIST_USER = "COURSE:{course_id}:MESSAGE_LIST:USER";
	public static final String CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER = "COURSE:{course_id}:MESSAGE_LIST:LECTURER";
	public static final String CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER_VOICE = "COURSE:{course_id}:MESSAGE_LIST:LECTURER_VOICE";
	public static final String CACHED_KEY_COURSE_REAL_STUDENT_NUM = "COURSE:{course_id}:REAL_STUDENT_NUM";
	public static final String CACHED_KEY_USER_LAST_JOIN_COURSE_IM_INFO = "SYS:USER:{user_id}:LAST_JOIN_COURSE_IM_INFO";
	public static final String CACHED_KEY_APP_VERSION_INFO_FIELD = "os";
	public static final String CACHED_KEY_APP_VERSION_INFO = "SYS:APP_VERSION_INFO:{os}";
//	public static final String CACHED_KEY_USER_ROOM_DISTRIBUTION_LIST_INFO = "USER:{user_id}:ROOM:DISTRIBUTION:LIST:INFO";


	public static final String USER_ROLE_LECTURER="lecturer";
	public static final String USER_ROLE_LISTENER="listener";
	public static final String USER_ROLE_ADMIN="admin";

	public static final String SERVICE_NO_ACCESS_TOKEN = "SERVICE_NO_ACCESS_TOKEN_MAP";

	public static final int LECTURER_PREDICTION_COURSE_LIST_SIZE = 20;
	public static final int PLATFORM_PREDICTION_COURSE_LIST_SIZE = 200;


	public static final String MONGODB_ORG_LOG_DB_FORMAT = "ORG_LOG_DB_%s";//根据月份
	public static final String MONGODB_ORG_LOG_COLLECTION_FORMAT = "ORG_LOG_COLLECTION_%s";//根据日期
	public static final String MONGODB_ADDRESS_DATABASE = "CITY_ADDRESS";//城市地址
	public static final String MONGODB_USER_REGISTRY_COLLECTION_FORMAT = "USER_REGISTRY_COLL_%s";//用户注册集合 日期
	public static final String MONGODB_USER_REGISTRY_DATABASE = "USER_REGISTRY_DB";//用户注册DBb
	public static final String MONGODB_CITY_IP_COLL_FORMAT = "CITY_IP_COLL_%s";
	public static final String MONGODB_DEVICE_ACTIVE_DATABASE = "DEVICE_ACTIVE_DB";//激活设备的表
	public static final String MONGODB_DEVICE_ACTIVE_COLLECTION_FORMAT = "DEVICE_ACTIVE_COLL_%s";//激活设备的表 根据时间
	public static final String MONGODB_DEVICE_ACTIVE_COLLECTION_DETAILS_FORMAT = "DEVICE_ACTIVE_DETAILS_COLL_%s";//日期

	public static final String MONGODB_LECTURER_COURSE_DATABASE_FORMAT =    "LECTURER_COURSE_%s";
	public static final String MONGODB_ROOM_DISTRIBUTER_DATABASE_FORMAT =   "ROOM_DISTRIBUTER_%s";
	public static final String MONGODB_COURSE_DISTRIBUTER_DATABASE_FORMAT = "COURSE_DISTRIBUTER_%s";

	public static final String MONGODB_LECTURER_COURSE_COLLECTION_FORMAT =    "LECTURER_COURSE_COLL_%s";
	public static final String MONGODB_ROOM_DISTRIBUTER_COLLECTION_FORMAT =   "ROOM_DISTRIBUTER_COLL_%s";
	public static final String MONGODB_COURSE_DISTRIBUTER_COLLECTION_FORMAT = "COURSE_DISTRIBUTER_COLL_%s";

//	public static final String JPUSH_TAG_FOLLOW_ROOM = "follow_room_";
	public static final String JPUSH_SEND_TYPE_ALIAS = "ALIAS";
//	public static final String JPUSH_SEND_TYPE_TAGS = "TAGS";


	public static final String CACHED_KEY_COURSE_MESSAGE_ID_INFO = "COURSE:{course_id}:MESSAGE_ID_INFO";

	public static final String WEB_FILE_PRE_FIX = "WEB_FILE/";//web路由

	public static final String FORCE_UPDATE_VERSION = "FORCE_UPDATE_VERSION:{os}";

	public static final String CACHED_KEY_SERVICE_LECTURER_FIELD = "lecturer_id";
	public static final String CACHED_KEY_SERVICE_LECTURER = "LECTURER:WEIXIN:{lecturer_id}";

	public static final String SEND_MSG_TIME_S = "CAPTCHA:SEND_MSG_TIME_S:{user_id}";//时间 秒
	public static final String SEND_MSG_TIME_D = "CAPTCHA:SEND_MSG_TIME_D:{user_id}";//时间天数

	public static final String CAPTCHA_KEY_CODE = "CAPTCHA:KEY:{user_id}";//存放验证码
	public static final String CAPTCHA_KEY_PHONE = "CAPTCHA:KEY:PHONE:{user_id}";//存放验证码


	public static final String CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION = "SYS:COURSES:{classify_id}:PREDICTION";// 分类 预告/正在直播
	public static final String CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH = "SYS:COURSES:{classify_id}:FINISH";//分类 已结束




	public static final String CACHED_KEY_CLASSIFY = "classify_id";//分类id
	public static final String CACHED_KEY_CLASSIFY_INFO = "SYS:CLASSIFY:{classify_id}";//分类缓存
	public static final String CACHED_KEY_CLASSIFY_ALL = "SYS:CLASSIFY:ALL";//存入所有分类id
	public static final String CACHED_KEY_CLASSIFY_PATTERN = "SYS:CLASSIFY:*";//所有分类匹配模式
	

	public static final String CACHED_KEY_BANNER_ALL = "SYS:BANNER:ALL";//横幅所有id
	public static final String CACHED_KEY_BANNER_INFO = "SYS:BANNER:{banner_id}";//横幅信息
	public static final String CACHED_KEY_BANNER = "banner_id";//横幅id
	public static final String CACHED_KEY_BANNER_PATTERN = "SYS:BANNER:*";//横幅文件夹匹配模式

	public static final String RECOMMEND_COURSE_NUM = "SYS:RECOMMEND:COURSE:COUNT";//推荐课程最大数
	public static final String CACHED_KEY_RECOMMEND_COURSE="SYS:RECOMMEND:COURSE";//推荐课程


	//zset 按照学生人数和打赏人数进行排列
	public static final String SYS_COURSES_RECOMMEND_LIVE = "SYS:COURSES:RECOMMEND:LIVE";//热门推荐正在直播
	public static final String SYS_COURSES_RECOMMEND_PREDICTION = "SYS:COURSES:RECOMMEND:PREDICTION";//热门推荐预告
	public static final String SYS_COURSES_RECOMMEND_FINISH = "SYS:COURSES:RECOMMEND:FINISH";//热门推荐结束




	public static final String COURSE_DEFAULT_CLASSINFY="9";//课程默认属性


	public static final String APP_REDIS_INDEX = "app_redis";//配置文件 app的redis 的分区index


	// 获取access_token的接口地址（GET） 限2000（次/天）#微信api  获取微信公众号获取全局 access token
	public final static String ACCESS_TOKEN_URL = "access_token_url";
	//微信API 通过用户code换取网页授权access_token
	public final static String GET_USER_INFO_BY_CODE_URL = "get_user_info_by_code_url";
	//微信API ：拉取用户信息
	public final static String GET_USER_INFO_BY_ACCESS_TOKEN = "get_user_info_by_access_token";

	public final static String GET_BASE_USER_INFO_BY_ACCESS_TOKEN = "get_base_user_info_by_access_token";
	//微信api 获取用户基本信息（包括UnionID机制）
	public final static String GET_USER_BY_OPENID = "get_user_by_openid";
	//获取JSAPI_Ticket
	public static String JSAPI_TICKET_URL = "jsapi_ticket_url";
	//获得微信素材多媒体URL
	public static String GET_MEDIA_URL = "get_media_url";
	//微信appid
	public static final String APPID = "appid";
	//微信appsecret
	public static final String APPSECRET = "appsecret";
	//微信app 支付的appid
	public static final String APP_APP_ID = "app_app_id";
	//#微信api 模板消息推送
	public final static String WEIXIN_TEMPLATE_PUSH_URL = "weixin_template_push_url";//"https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=ACCESS_TOKEN";
	//#开放平台中AppID
	public static final String SERVICE_NO_APPID = "weixin_service_no_appid";
	//#开放平台中AppSecret
	public static final String SERVICE_NO_APPSECRET = "weixin_service_no_secret";
	//#微信api 获取第三方平台access_token POST
	public final static String COMPONENT_ACCESS_TOKEN_URL = "service_component_access_token_url";
	//#微信api 获取预授权码  POST
	public final static String PRE_AUTH_CODE_URL = "service_pre_auth_code";
	//#微信api 引导进入授权页面 该网址中第三方平台方需要提供第三方平台方appid、预授权码和回调URI
	public final static String SERVICE_AUTH_URL = "service_auth_url";
	//#微信api 使用授权码换取公众号的授权信息 POST
	public final static String SERVICE_AUTH_INFO_URL = "service_auth_info_url";
	//#微信api 获取（刷新）授权公众号的令牌
	public final static String SERVICE_AUTH_REFRESH_INFO_URL = "service_auth_refresh_info_url";
	//#微信api 获取授权方的账户信息
	public final static String SERVICE_AUTH_ACCOUNT_INFO_URL = "service_auth_account_info_url";
	//
	public final static String SERVICE_TEMPLATE_INFO_URL = "service_template_info_url";
	//#微信api 获取用户列表
	public final static String SERVICE_FANS_URL1 = "service_fans_url1";
	//#微信api  关注者数量超过10000时当公众号关注者数量超过10000时，可通过填写next_openid的值，从而多次拉取列表的方式来满足需求。
	//##具体而言，就是在调用接口时，将上一次调用得到的返回中的next_openid值，作为下一次调用中的next_openid值。
	public final static String SERVICE_FANS_URL2 = "service_fans_url2";
	//微信pcappid
	public static final String PC_NO_APPID = "weixin_pc_no_appid";
	//微信pcappsectet
	public static final String PC_NO_APPSECRET = "weixin_pc_no_secret";
	//
	public static final String PC_AUTH_ACCOUNT_INFO_URL = "pc_auth_account_info_url";

	public static final String HEADER_APP_NAME = "qnlive";

	public static final double USER_RATE = 0.7;





}
