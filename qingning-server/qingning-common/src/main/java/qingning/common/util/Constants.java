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
	
	public static final String CHOREOGRAPHER_SINGLE_COUNT = "SYS:CHOREOGRAPHER:SINGLE:COUNT";
	public static final String SYS_CHOREOGRAPHER_SINGLE_IDS = "SYS:CHOREOGRAPHER:SINGLE:IDS";
	public static final String SYS_CHOREOGRAPHER_SINGLE_ID = "SYS:CHOREOGRAPHER:SINGLE:";
	public static final String CHOREOGRAPHER_CORP_COUNT = "SYS:CHOREOGRAPHER:CORP:COUNT";
	public static final String SYS_CHOREOGRAPHER_CORP_IDS = "SYS:CHOREOGRAPHER:CORP:IDS";
	public static final String SYS_CHOREOGRAPHER_CORP_ID = "SYS:CHOREOGRAPHER:CORP:";
	
	public static final String SYS_COURSE_COUNT = "SYS:COURSE:COUNT";
	public static final String SYS_COURSE_IDS = "SYS:COURSE:IDS";
	public static final String SYS_COURSE_ID = "SYS:COURSE:";
	
	 
	/**
	 * 系统平台收益
	 */
	public static final String SYS_WALLET = "SYS:WALLET:";
	
	/**
	 * 教练机构信息   SYS:CHOREOGRAPHER:+ 教练/机构id
	 */
	public static final String SYS_CHOREOGRAPHER = "SYS:CHOREOGRAPHER:";
	
	
	public static final String SYS_CARD_FLAG = "1758";
	/**
	 *  （系统级别缓存）订单的Id 集合    1000条
	 */
	public static final String SYS_BILL_IDS = "SYS:BILL:IDS";
	
	/**
	 *   （系统级别缓存）单个存储订单的对象  "SYS:BILL_ID:"+"订单的Id" 
	 */
	public static final String SYS_BILL_ID = "SYS:BILL:";
	
	/**
	 *  （系统级别缓存）订单总数 
	 */
	public static final String SYS_BILL_COUNT = "SYS:BILL:COUNT";
	
	/**
	 *（系统级别缓存） 所有 课程类别的 id 集合
	 */
	public static final String SYS_COURSE_TYPE_IDS = "SYS:COURSE_TYPE_IDS";
	
	/**
	 *  （系统级别缓存）用户管理 userID 集合
	 */
	public static final String SYS_LEAGUER_IDS = "SYS:LEAGUER_IDS";
	
	/**
	 *  （系统级别缓存）用户管理  "SYS:LEAGUER_ID:"+"userId" 
	 */
	public static final String SYS_LEAGUER_ID = "SYS:LEAGUER_ID:";
	
	/**
	 *  （系统级别缓存）用户管理  总人数 
	 */
	public static final String SYS_LEAGUER_COUNT = "SYS:LEAGUER_COUNT:";
	
	/**
	 *  （系统级别缓存）分销商管理 userID 集合
	 */
	public static final String SYS_DISTRIBUTOR_IDS = "SYS:DISTRIBUTOR:IDS:";
	/**
	 *  （系统级别缓存）分销商管理  "SYS:DISTRIBUTOR:"+"userId" 
	 */
	public static final String SYS_DISTRIBUTOR = "SYS:DISTRIBUTOR:";
	/**
	 *  （系统级别缓存）分销商管理  总人数 
	 */
	public static final String SYS_DISTRIBUTOR_COUNT = "SYS:DISTRIBUTOR:COUNT";
	 
	
	/**
	 * 全国地区列表信息
	 */
	public static final String NATION_INFO_KEY = "SYS:NATION_INFO_KEY";
	
	public static final String NATION_INFO_KEY_APP = "SYS:NATION_INFO_KEY_APP";
	

	/**
	 * 课程类别  "SYS:COURSESTYPE:"+课程Id
	 */
	public static final String SYS_COURSES_TYPE = "SYS:COURSESTYPE:";
	
	/**
	 * 系统级别  地区信息记录    key=  SYS:NATION_ID：+ id    value =  北京
	 */
	public static final String SYS_NATION_ID = "SYS:NATION_ID:";
	
	
	public final static Map<String, String> LABLE_MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 5032089487753365978L;
		{
	        put("1", "一级");
	        put("2", "二级");
	        put("3", "三级");
	        put("m", "高级");
	        put("s", "导师级");
	        put("n", "国家级");
	    }
	};
	
}
