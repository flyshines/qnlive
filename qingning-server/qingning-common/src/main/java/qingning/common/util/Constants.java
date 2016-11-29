package qingning.common.util;

import java.util.HashMap;
import java.util.Map;

public final class Constants {
	


	private Constants(){};
	
	public static final String SYSINT="int";
	public static final String SYSLONG="long";
	public static final String SYSDOUBLE="double";
	public static final String SYSDATE="date";
	
	public static final String QUEUE_NAME="GCW_DB_W";
	
	public static final String DEFAULT = "default";
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
	
	public static final String FUNCTION = "function";
	public static final String VALIDATION = "validation";
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
		
	public static final String CACHED_KEY_COURSE = "SYS:COURSE:{course_id}";
	public static final String CACHED_KEY_COURSE_FIELD = "course_id";
	
	public static final String CACHED_KEY_COURSE_PPTS = "SYS:COURSE:{course_id}:PPTS";
	public static final String CACHED_KEY_COURSE_PPTS_FIELD = "ppt_list";
	public static final String CACHED_KEY_COURSE_AUDIOS = "SYS:COURSE:{course_id}:AUDIOS";
	public static final String CACHED_KEY_COURSE_AUDIOS_FIELD = "audio_list";
	
	public static final String CACHED_KEY_COURSE_PREDICTION = "SYS:LECTURER:{lecturer_id}:COURSES:PREDICTION";
	public static final String CACHED_KEY_COURSE_FINISH = "SYS:LECTURER:{lecturer_id}:COURSES:FINISH";
	
	
}
