package qingning.server.rpc;

import qingning.common.entity.RequestEntity;

import java.util.Map;

public interface CommonReadOperation {
	String SYS_READ_USER_COURSE_LIST = "SYS_READ_USER_COURSE_LIST";//用户课程列表
	String SYS_READ_USER_ROOM_LIST = "SYS_READ_USER_ROOM_LIST";//用户直播间列表
	String SYS_READ_USER_SERIES_LIST = "SYS_READ_USER_SERIES_LIST";//用户系列列表
	String SYS_READ_USER_BY_UNIONID = "SYS_READ_USER_BY_UNIONID";//用户 unionId


	String SYS_INSERT_DISTRIBUTER ="INSERT_DISTRIBUTER";//增加分销这
	String SYS_READ_LAST_COURSE ="SYS_READ_LAST_COURSE";//直播课程
	String SYS_READ_SAAS_COURSE ="SYS_READ_SAAS_COURSE";//saas课程
	String SYS_READ_LAST_SERIES ="SYS_READ_LAST_SERIES";


	String LECTURER_ROOM_LOAD = "LECTURER_ROOM_LOAD";//讲师直播间

	//店铺
	String CACHE_READ_SHOP = "101";//店铺


	//系列
	String CACHE_SERIES_STUDENTLIST ="findSeriesStudentListBySeriesId";

	String FUNCTION_DISTRIBUTERS_ROOM_RQ = "FUNCTION_DISTRIBUTERS_ROOM_RQ";//直播间分销

	Object invokeProcess(RequestEntity requestEntity) throws Exception;

	Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception;
}
