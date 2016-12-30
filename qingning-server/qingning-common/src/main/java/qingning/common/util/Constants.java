package qingning.common.util;

public final class Constants {
	


	private Constants(){};
	
	public static final String SYSINT="int";
	public static final String SYSLONG="long";
	public static final String SYSDOUBLE="double";
	public static final String SYSMAP= "map";
	public static final String SYSLIST= "list";
	public static final String SYSOBJECT= "object";
	public static final String SYSDATE="date";
	public static final String SYSSTR="string";
	public static final int COURSE_MAX_INTERVAL = 10;
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

	public static final String MONGODB_IP="mongodb.ip";
	public static final String MONGODB_PORT="mongodb.port";
	public static final String MONGODB_MAXCONNECTTIMEOUT="mongodb.maxConnectTimeout";
	public static final String MONGODB_MAXCONNECTIONS="mongodb.maxConnections";
	public static final String MONGODB_MAXWAITTIME="mongodb.maxWaitTime";
	public static final String MONGODB_MAXWAITTHREADS="mongodb.maxWaitThreads";
	public static final String MONGODB_MAXIDLETIME="mongodb.maxIdleTime";
	public static final String MONGODB_MAXLIFETIME="mongodb.maxLifeTime";
	public static final String MONGODB_SOKETTIMEOUT="mongodb.soketTimeout";
	public static final String MONGODB_SOKETKEEPALIVE="mongodb.soketKeepAlive";
	
	public static final String FUNCTION = "function";
	public static final String VALIDATION = "validation";
	public static final String CONVERTVALUE = "convertValue";
	public static final String JS_ENGINE = "nashorn";
	
	public static final String SYS_SINGLE_KEY = "SYS_SINGLE_KEY";
	public static final String SYS_OUT_TRADE_NO_KEY = "SYS_OUT_TRADE_NO_KEY";
	
	public static final String CACHED_KEY_ACCESS_TOKEN_FIELD = "access_token";
	public static final String CACHED_KEY_ACCESS_TOKEN = "SYS:USER:{access_token}";
	public static final String CACHED_KEY_USER_FIELD ="user_id";
	public static final String CACHED_KEY_USER = "SYS:USER:{user_id}";
	
	
	public static final String CACHED_KEY_LECTURER_FIELD = "lecturer_id";
	public static final String CACHED_KEY_LECTURER = "SYS:LECTURER:{lecturer_id}";
	public static final String CACHED_KEY_LECTURER_ROOMS = "SYS:LECTURER:{lecturer_id}:ROOMS";
	
	public static final String CACHED_KEY_SPECIAL_LECTURER_ROOM = "SYS:LECTURER:{lecturer_id}:ROOM:{room_id}";
	
	public static final String FIELD_CREATE_TIME="create_time";
	public static final String FIELD_ROOM_ID="room_id";
	
	public static final String CACHED_KEY_DISTRIBUTER_FIELD = "distributer_id";
	public static final String CACHED_KEY_DISTRIBUTER = "SYS:DISTRIBUTER:{distributer_id}";
	
	public static final String CACHED_KEY_USER_DISTRIBUTERS = "USER:{user_id}:LECTURER:{room_id}:DISTRIBUTERS";
	public static final String CACHED_KEY_USER_DISTRIBUTERS_LEN = "USER:{user_id}:LECTURER:{room_id}:DISTRIBUTERS:LEN";
	
	public static final String CACHED_KEY_USER_DISTRIBUTER_COURSES = "USER:{user_id}:LECTURER:{room_id}:COURSES:{distributer_id}";
	public static final String CACHED_KEY_USER_DISTRIBUTER_COURSES_MIN_TIME = "USER:{user_id}:LECTURER:{room_id}:COURSE:{distributer_id}:MINTIME";
	
	public static final String CACHED_KEY_SYS_DISTRIBUTERS = "SYS:LECTURER:{room_id}:DISTRIBUTERS";
	
	public static final String CACHED_KEY_COURSE = "SYS:COURSE:{course_id}";
	public static final String CACHED_KEY_COURSE_FIELD = "course_id";
	
	public static final String CACHED_KEY_COURSE_PPTS = "SYS:COURSE:{course_id}:PPTS";
	public static final String CACHED_KEY_COURSE_PPTS_FIELD = "ppt_list";
	public static final String CACHED_KEY_COURSE_AUDIOS = "SYS:COURSE:{course_id}:AUDIOS";
	public static final String CACHED_KEY_COURSE_AUDIOS_JSON_STRING = "SYS:COURSE:{course_id}:AUDIOS:JSON_STRING";
	public static final String CACHED_KEY_COURSE_AUDIOS_FIELD = "audio_list";
	public static final String FIELD_AUDIO_ID="audio_id";
	public static final String CACHED_KEY_COURSE_AUDIO = "SYS:COURSE:{course_id}:AUDIO:{audio_id}";
	
	public static final String CACHED_KEY_COURSE_PREDICTION = "SYS:LECTURER:{lecturer_id}:COURSES:PREDICTION";
	public static final String CACHED_KEY_COURSE_FINISH = "SYS:LECTURER:{lecturer_id}:COURSES:FINISH";

	public static final String CACHED_KEY_USER_ROOM_SHARE = "USER:ROOM_SHARE_CODE:{room_share_code}";
	public static final String CACHED_KEY_USER_ROOM_SHARE_FIELD = "room_share_code";
	
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

	public static final String CACHED_KEY_PLATFORM_COURSE_PREDICTION = "SYS:COURSES:PREDICTION";
	public static final String CACHED_KEY_PLATFORM_COURSE_FINISH = "SYS:COURSES:FINISH";
	public static final String CACHED_KEY_PLATFORM_COURSE_LIVE = "SYS:COURSES:LIVE";

	public static final String CACHED_KEY_WEIXIN_TOKEN="SYS:WEIXIN:TOKEN";
	public static final String CACHED_KEY_WEIXIN_JS_API_TIKET="SYS:WEIXIN:JS_API_TIKET";

	public static final String CACHED_KEY_COURSE_BAN_USER_LIST="COURSE:{course_id}:BAN_USER_LIST";
	public static final String CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM="SYS:USER_NICK_NAME_INCREMENT_NUM";
	public static final String CACHED_KEY_COURSE_MESSAGE_LIST="COURSE:{course_id}:MESSAGE_LIST";
	public static final String FIELD_MESSAGE_ID="message_id";
	public static final String CACHED_KEY_COURSE_MESSAGE="COURSE:{course_id}:MESSAGE:{message_id}";
	public static final String CACHED_KEY_COURSE_MESSAGE_LIST_QUESTION = "COURSE:{course_id}:MESSAGE_LIST:QUESTION";
	public static final String CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER = "COURSE:{course_id}:MESSAGE_LIST:LECTURER";


	public static final String USER_ROLE_LECTURER="lecture";
	public static final int LECTURER_PREDICTION_COURSE_LIST_SIZE = 20;
	public static final int PLATFORM_PREDICTION_COURSE_LIST_SIZE = 500;
	
}
