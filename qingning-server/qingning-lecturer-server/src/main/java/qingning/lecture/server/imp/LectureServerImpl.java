package qingning.lecture.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import javafx.beans.binding.ObjectBinding;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import qingning.common.dj.DjSendMsg;
import qingning.common.dj.HttpClientUtil;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.util.*;
import qingning.lecture.server.other.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ILectureModuleServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class LectureServerImpl extends AbstractQNLiveServer {
	private static Logger log = LoggerFactory.getLogger(LectureServerImpl.class);

    private ILectureModuleServer lectureModuleServer;

    private ReadCourseOperation readCourseOperation;
    private ReadLiveRoomOperation readLiveRoomOperation;
    private ReadLecturerOperation readLecturerOperation;
	private ReadUserOperation readUserOperation;
	private ReadRoomDistributerOperation readRoomDistributerOperation;
	private ReadSeriesOperation readSeriesOperation;
    private ReadDistributerOperation readDistributerOperation;
    private ReadShopOperation readShopOperation;

    @Override
    public void initRpcServer() {
        if (lectureModuleServer == null) {
            lectureModuleServer = this.getRpcService("lectureModuleServer");
            readLiveRoomOperation = new ReadLiveRoomOperation(lectureModuleServer);
            readCourseOperation = new ReadCourseOperation(lectureModuleServer);
            readLecturerOperation = new ReadLecturerOperation(lectureModuleServer);
            readUserOperation = new ReadUserOperation(lectureModuleServer);
            readRoomDistributerOperation = new ReadRoomDistributerOperation(lectureModuleServer);
            readDistributerOperation = new ReadDistributerOperation(lectureModuleServer);
            readShopOperation = new ReadShopOperation(lectureModuleServer);
            readSeriesOperation = new ReadSeriesOperation(lectureModuleServer);
        }
    }

    @SuppressWarnings("unchecked")
    @FunctionName("createLiveRoom")
    public Map<String, Object> createLiveRoom(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> resultMap = new HashMap<>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = MiscUtils.getUUId();
        reqMap.put("room_address", MiscUtils.getConfigByKey("live_room_share_url_pre_fix",appName) + room_id);
        reqMap.put("room_id", room_id);
        reqMap.put("appName",appName);
        //0.目前校验每个讲师仅能创建一个直播间
        //1.缓存中读取直播间信息
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
        if (jedis.exists(lectureLiveRoomKey)) {
            throw new QNLiveException("100006");
        }
        //2.数据库修改
        //2.如果为新讲师用户，插入讲师表。插入直播间表。更新登录信息表中的用户身份
        map.clear();
        map.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
        String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
        String user_role = jedis.hget(accessTokenKey, "user_role");
        boolean isLecturer = false;
        if(MiscUtils.isEmpty(user_role)){
            isLecturer = false;
        }else {
            String[] roleArray = user_role.split(",");
            for(String role : roleArray){
                if(role.equals(Constants.USER_ROLE_LECTURER)){
                    isLecturer = true;
                }
            }
        }
        reqMap.put("isLecturer", isLecturer);
        reqMap.put("user_id", userId);
        Map<String,Object> queryParam = new HashMap<String,Object>();
        queryParam.put("user_id", userId);
        RequestEntity queryOperation = this.generateRequestEntity(null,null, null, queryParam);
        //TODO 手机号
        Map<String,String> userInfo = CacheUtils.readUser(userId, queryOperation, readUserOperation, jedis);
        if(MiscUtils.isEmpty(reqMap.get("avatar_address"))){
        	reqMap.put("avatar_address",userInfo.get("avatar_address"));
        }
        if(MiscUtils.isEmpty(reqMap.get("room_name"))){        	
        	reqMap.put("room_name", String.format(MiscUtils.getConfigByKey("room.default.name",appName), userInfo.get("nick_name")));
        }
       lectureModuleServer.createLiveRoom(reqMap);

        //3.缓存修改
        //如果是刚刚成为讲师，则增加讲师信息缓存，并且修改access_token缓存中的身份
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, reqMap.get("user_id").toString());
        String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        if (isLecturer == false) {
        	queryOperation = this.generateRequestEntity(null,null, null, map);        	
        	CacheUtils.readLecturer(userId, queryOperation, readLecturerOperation, jedis);
            //3.2修改access_token中的缓存
            jedis.hset(accessTokenKey, "user_role", user_role+","+Constants.USER_ROLE_LECTURER);
            //3.3增加讲师直播间信息缓存
            jedis.sadd(Constants.CACHED_LECTURER_KEY, userId);
            map.clear();
            map.put("room_id", room_id);
            CacheUtils.readLiveRoom(room_id, this.generateRequestEntity(null, null, null, map), readLiveRoomOperation, jedis, true,true);
            //增加讲师直播间对应关系缓存(一对多关系)
            //jedis.hset(lectureLiveRoomKey, createResultMap.get("room_id").toString(), "1");
        } else {
        	jedis.hincrBy(lectureKey, "live_room_num", 1L);
        }
        //增加讲师缓存中的直播间数        
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
        resultMap.put("room_id", room_id);
        return resultMap;
    }
 
 
 
    @SuppressWarnings("unchecked")
    @FunctionName("updateLiveRoom")
    public Map<String, Object> updateLiveRoom(RequestEntity reqEntity) throws Exception {
        Map<String, Object> values = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
 
        //0.检测是否包含修改信息
        if (values.get("avatar_address") == null && values.get("room_name") == null
                && values.get("room_remark") == null) {
            throw new QNLiveException("100007");
        }
        Map<String, Object> reqMap = new HashMap<String, Object>();
        
        for(String key:values.keySet()){
        	Object obj = values.get(key);
        	if(obj != null){
        		reqMap.put(key, obj);
        	}
        }
        
        //1.检测该直播间是否属于修改人
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String liveRoomOwner = CacheUtils.readLiveRoomInfoFromCached((String)reqMap.get("room_id"), "lecturer_id",
                reqEntity, readLiveRoomOperation, jedis, true);
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
 
        //2.检测更新时间是否与系统一致
        String liveRoomUpdateTime = CacheUtils.readLiveRoomInfoFromCached((String)reqMap.get("room_id"), "update_time",
                reqEntity, readLiveRoomOperation, jedis, true);
        if (!reqMap.get("update_time").toString().equals(liveRoomUpdateTime)) {
            throw new QNLiveException("100003");
        }
 
        //3.修改数据库
        reqMap.put("user_id", userId);
        Map<String, Object> dbResultMap = lectureModuleServer.updateLiveRoom(reqMap);
        if (dbResultMap == null || dbResultMap.get("updateCount") == null ||
                ((Integer) dbResultMap.get("updateCount")) == 0) {
            throw new QNLiveException("100003");
        }
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
        //4.修改缓存
        Map<String, String> updateCacheMap = new HashMap<String, String>();
        if (reqMap.get("avatar_address") != null) {
            updateCacheMap.put("avatar_address", reqMap.get("avatar_address").toString());
        }
        if (reqMap.get("room_name") != null) {
            updateCacheMap.put("room_name", reqMap.get("room_name").toString());
        }
        if (reqMap.get("room_remark") != null) {
            updateCacheMap.put("room_remark", reqMap.get("room_remark").toString());
        }
        updateCacheMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, (String)reqMap.get("room_id"));
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map); 
        jedis.hmset(liveRoomKey, updateCacheMap);
 
        resultMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
        resultMap.put("qr_code",getQrCode(userId,jedis,appName));
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("liveRoom")
    public Map<String, Object> queryLiveRoomDetail(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String,Object> map = new HashMap<>();
        map.put("appName",appName);
        if(appName.equals(Constants.HEADER_APP_NAME)){
            map.put("config_key","bindingServiceUrl");
            resultMap.put("binding_service_url",lectureModuleServer.findCustomerServiceBySystemConfig(map).get("config_value"));
        }
        resultMap.put("qr_code",getQrCode(userId,jedis,appName));
        String queryType = reqMap.get("query_type").toString();
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String liveRoomListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
        
        RequestEntity request = new RequestEntity();                   
        request.setParam(map); 
        Map<String,String> lectureInfo = CacheUtils.readLecturer(userId, request, readLecturerOperation, jedis);
        Map<String,Object> userInfo = lectureModuleServer.findUserInfoByUserId(userId);

        long payCourseNum = MiscUtils.convertObjectToLong(lectureInfo.get("pay_course_num"));
        if(MiscUtils.isEmpty(userInfo.get("phone_number"))){//如果没有手机号就直接返回
            resultMap.put("phone_number", "");
        }else{
            resultMap.put("phone_number", userInfo.get("phone_number"));
        }
        //0查询我创建的直播间列表
        if(queryType.equals("0")){
            if(jedis.exists(liveRoomListKey)){
                Map<String,String> liveRoomsMap = jedis.hgetAll(liveRoomListKey);

                if(CollectionUtils.isEmpty(liveRoomsMap)){
                    return resultMap;
 
                }else {
                    List<Map<String,Object>> liveRoomListResult = new ArrayList<>();
                    for(String roomIdCache : liveRoomsMap.keySet()){
                        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(roomIdCache, reqEntity, readLiveRoomOperation, jedis, true);
                        Map<String,Object> peocessLiveRoomMap;
                        if(! CollectionUtils.isEmpty(liveRoomMap)){
                        	String roomId = liveRoomMap.get("room_id");
                        	String lectureId = liveRoomMap.get("lecturer_id");
                        	Map<String,Object> query = new HashMap<String,Object>();
                        	query.put("room_id", roomId);
                        	query.put("room_id", lectureId);
                        	RequestEntity entity = this.generateRequestEntity(null, null, Constants.SYS_READ_LAST_COURSE, query);
                        	Map<String,String> courseInfo = CacheUtils.readLastCourseOfTheRoom(roomId, lectureId, entity, readCourseOperation, jedis);
                        	
                            peocessLiveRoomMap = new HashMap<>();
                            peocessLiveRoomMap.put("avatar_address", MiscUtils.convertString(liveRoomMap.get("avatar_address")));
                            peocessLiveRoomMap.put("room_name", MiscUtils.RecoveryEmoji(liveRoomMap.get("room_name")));
                            //peocessLiveRoomMap.put("last_course_amount", MiscUtils.convertObjectToDouble(liveRoomMap.get("last_course_amount"),true));
                            if(!MiscUtils.isEmpty(courseInfo)){
                            	long amount = MiscUtils.convertObjectToLong(courseInfo.get("course_amount")) + MiscUtils.convertObjectToLong(courseInfo.get("extra_amount"));
                            	peocessLiveRoomMap.put("last_course_amount", MiscUtils.convertObjectToDouble(amount,true));
                            } else {
                            	peocessLiveRoomMap.put("last_course_amount", 0d);
                            }
                            peocessLiveRoomMap.put("fans_num", MiscUtils.convertObjectToLong(liveRoomMap.get("fans_num")));
                            peocessLiveRoomMap.put("room_id", MiscUtils.convertString(liveRoomMap.get("room_id")));
                            peocessLiveRoomMap.put("update_time", MiscUtils.convertObjectToLong(liveRoomMap.get("update_time")));
                            peocessLiveRoomMap.put("pay_course_num", payCourseNum);
                            liveRoomListResult.add(peocessLiveRoomMap);
                        }
                    }
                    if(! CollectionUtils.isEmpty(liveRoomListResult)){
                        resultMap.put("room_list", liveRoomListResult);
                    }
                    return resultMap;
                }
 
            }else {
                List<Map<String, Object>> liveRoomByLectureId = lectureModuleServer.findLiveRoomByLectureId(userId);
                List<Map<String,Object>> liveRoomListResult = new ArrayList<>();
                for(Map<String, Object> room : liveRoomByLectureId){
                    String roomIdCache = room.get("room_id").toString();
                    Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(roomIdCache, reqEntity, readLiveRoomOperation, jedis, true);
                    Map<String,Object> peocessLiveRoomMap;
                    if(! CollectionUtils.isEmpty(liveRoomMap)){
                        String roomId = liveRoomMap.get("room_id");
                        String lectureId = liveRoomMap.get("lecturer_id");
                        Map<String,Object> query = new HashMap<String,Object>();
                        query.put("room_id", roomId);
                        query.put("room_id", lectureId);
                        RequestEntity entity = this.generateRequestEntity(null, null, Constants.SYS_READ_LAST_COURSE, query);
                        Map<String,String> courseInfo = CacheUtils.readLastCourseOfTheRoom(roomId, lectureId, entity, readCourseOperation, jedis);

                        peocessLiveRoomMap = new HashMap<>();
                        peocessLiveRoomMap.put("avatar_address", MiscUtils.convertString(liveRoomMap.get("avatar_address")));
                        peocessLiveRoomMap.put("room_name", MiscUtils.RecoveryEmoji(liveRoomMap.get("room_name")));
                        //peocessLiveRoomMap.put("last_course_amount", MiscUtils.convertObjectToDouble(liveRoomMap.get("last_course_amount"),true));
                        if(!MiscUtils.isEmpty(courseInfo)){
                            long amount = MiscUtils.convertObjectToLong(courseInfo.get("course_amount")) + MiscUtils.convertObjectToLong(courseInfo.get("extra_amount"));
                            peocessLiveRoomMap.put("last_course_amount", MiscUtils.convertObjectToDouble(amount,true));
                        } else {
                            peocessLiveRoomMap.put("last_course_amount", 0d);
                        }
                        peocessLiveRoomMap.put("fans_num", MiscUtils.convertObjectToLong(liveRoomMap.get("fans_num")));
                        peocessLiveRoomMap.put("room_id", MiscUtils.convertString(liveRoomMap.get("room_id")));
                        peocessLiveRoomMap.put("update_time", MiscUtils.convertObjectToLong(liveRoomMap.get("update_time")));
                        peocessLiveRoomMap.put("pay_course_num", payCourseNum);
                        liveRoomListResult.add(peocessLiveRoomMap);
                    }
                }
                if(! CollectionUtils.isEmpty(liveRoomListResult)){
                    resultMap.put("room_list", liveRoomListResult);
                }
                return resultMap;
            }
        }else {
            //查询单个直播间的详细信息
            if(reqMap.get("room_id") == null || StringUtils.isBlank(reqMap.get("room_id").toString())){
                throw new QNLiveException("000100");
            }
 
            Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(reqMap.get("room_id").toString(), reqEntity, readLiveRoomOperation, jedis, true);
            if(CollectionUtils.isEmpty(liveRoomMap)){
                throw new QNLiveException("100002");
            }
        	String roomId = liveRoomMap.get("room_id");
        	String lectureId = liveRoomMap.get("lecturer_id");
        	Map<String,Object> query = new HashMap<String,Object>();
        	query.put("room_id", roomId);
        	query.put("lecturer_id", lectureId);
        	RequestEntity entity = this.generateRequestEntity(null, null, Constants.SYS_READ_LAST_COURSE, query);
        	Map<String,String> courseInfo = CacheUtils.readLastCourseOfTheRoom(roomId, lectureId, entity, readCourseOperation, jedis);
        	long amount = 0l;
        	if(!MiscUtils.isEmpty(courseInfo)){
            	amount = MiscUtils.convertObjectToLong(courseInfo.get("course_amount")) + MiscUtils.convertObjectToLong(courseInfo.get("extra_amount"));
        	}            
            if(queryType.equals("1")){
                //resultMap.put("last_course_amount",MiscUtils.convertObjectToDouble(liveRoomMap.get("last_course_amount")));//上次课程收益  当前直播间 最近结束的课程
                resultMap.put("last_course_amount", MiscUtils.convertObjectToDouble(amount,true));//上次课程收益  当前直播间 最近结束的课程
                resultMap.put("avatar_address", MiscUtils.convertString(liveRoomMap.get("avatar_address")));
                resultMap.put("room_name", MiscUtils.RecoveryEmoji(liveRoomMap.get("room_name")));
                resultMap.put("room_remark",  MiscUtils.RecoveryEmoji(liveRoomMap.get("room_remark")));
                resultMap.put("rq_code",  MiscUtils.convertString(liveRoomMap.get("rq_code")));
                resultMap.put("room_address",  MiscUtils.convertString(liveRoomMap.get("room_address")));
                resultMap.put("update_time",  MiscUtils.convertObjectToLong(Long.valueOf(liveRoomMap.get("update_time"))));
                resultMap.put("pay_course_num", payCourseNum);
                return resultMap;

            }else {
                resultMap.put("last_course_amount", MiscUtils.convertObjectToDouble(amount,true));//上次课程收益  当前直播间 最近结束的课程
                resultMap.put("avatar_address", MiscUtils.convertString(liveRoomMap.get("avatar_address")));
                resultMap.put("room_name", MiscUtils.RecoveryEmoji(liveRoomMap.get("room_name")));
                resultMap.put("room_remark", MiscUtils.RecoveryEmoji(liveRoomMap.get("room_remark")));
                resultMap.put("fans_num",  MiscUtils.convertObjectToLong(liveRoomMap.get("fans_num")));
                resultMap.put("room_address", MiscUtils.convertString(liveRoomMap.get("room_address")));
                resultMap.put("update_time",  MiscUtils.convertObjectToLong(liveRoomMap.get("update_time")));
                resultMap.put("pay_course_num", payCourseNum);

                return resultMap;
            }
        }
    }
 
 
    @SuppressWarnings("unchecked")
    @FunctionName("createCourse")
    public Map<String, Object> createCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Long startTime = (Long)reqMap.get("start_time");
        boolean pass=true;
        if(MiscUtils.isEmpty(startTime)){
            pass=false;
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.MINUTE, Constants.COURSE_MAX_INTERVAL);
            if(cal.getTimeInMillis()>startTime){
                pass=false;
            }
        }
        if(!pass){
            throw new QNLiveException("100019");
        }
 
        //1.判断直播间是否属于当前讲师
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//获取userid
        String appName = reqEntity.getAppName();
        reqMap.put("appName",appName);
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String liveRoomOwner = CacheUtils.readLiveRoomInfoFromCached((String)reqMap.get("room_id"), "lecturer_id", reqEntity, readLiveRoomOperation, jedis, true);
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }

        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);

        //课程之间需要间隔三十分钟
        String lecturerCoursesAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, query);
        long startIndex = startTime-30*60*1000;
        long endIndex = startTime+30*60*1000;
        long start = MiscUtils.convertInfoToPostion(startIndex , 0L);
        long end = MiscUtils.convertInfoToPostion(endIndex , 0L);
        Set<String> aLong = jedis.zrangeByScore(lecturerCoursesAllKey, start, end);
        for(String course_id : aLong){
            Map<String,Object> map = new HashMap<>();
            map.put("course_id",course_id);
            Map<String, String> course = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);
            if(course.get("status").equals("1")){
                throw new QNLiveException("100029");
            }
        }

        reqMap.put("user_id", userId);

        //2.1创建IM 聊天群组
        Map<String,String> queryParam = new HashMap<>();                
        queryParam.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
        String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, queryParam);
        Map<String,String> userMap = jedis.hgetAll(accessTokenKey);
        try {
            Map<String,String> groupMap = IMMsgUtil.createIMGroup(userMap.get("m_user_id").toString());
            if(groupMap == null || StringUtils.isBlank(groupMap.get("groupid"))){
                throw new QNLiveException("100015");
            }
            reqMap.put("im_course_id",groupMap.get("groupid"));
            //2.1.1将讲师加入到该群组中
            IMMsgUtil.joinGroup(groupMap.get("groupid"), userMap.get("m_user_id").toString(), userMap.get("m_user_id").toString());
        }catch (Exception e){
        }
        //创建课程url
        if(reqMap.get("course_url") == null || StringUtils.isBlank(reqMap.get("course_url").toString())){
            String default_course_cover_url_original = MiscUtils.getConfigByKey("default_course_cover_url",appName);
            JSONArray default_course_cover_url_array = JSON.parseArray(default_course_cover_url_original);
            int randomNum = MiscUtils.getRandomIntNum(0, default_course_cover_url_array.size() - 1);
            reqMap.put("course_url", default_course_cover_url_array.get(randomNum));
        }
        //创建课程到数据库
        Map<String, Object> dbResultMap = lectureModuleServer.createCourse(reqMap);
        //4 修改相关缓存
        //4.1修改讲师个人信息缓存中的课程数 讲师个人信息SYS: lecturer:{lecturer_id}     
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, (String)reqMap.get("room_id"));
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map); 
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lectureKey, "course_num", 1);
        //4.2 修改讲师直播间信息中的课程数  讲师直播间信息SYS: room:{room_id}
        jedis.hincrBy(liveRoomKey, "course_num", 1);

        String course_type = (String)reqMap.get("course_type");
        if("1".equals(course_type)){
            jedis.hincrBy(liveRoomKey, "private_course_num", 1);
            jedis.hincrBy(lectureKey, "private_course_num", 1);
        } else if("2".equals(course_type)){
            jedis.hincrBy(liveRoomKey, "pay_course_num", 1);
            jedis.hincrBy(lectureKey, "pay_course_num", 1);
        }
        //4.3 生成该课程缓存 课程基本信息：SYS: course:{course_id}
        Map<String, String> course = CacheUtils.readCourse((String)dbResultMap.get("course_id"), 
        		generateRequestEntity(null, null, null, dbResultMap), readCourseOperation, jedis, true);
        if("1".equals(reqMap.get("updown"))){
            //<editor-fold desc="上架">
            String courseId = (String)course.get("course_id");
            String roomId = course.get("room_id");
            if(!MiscUtils.isEmpty(reqMap.get("series_id"))){
                map.clear();
                String series_id = reqMap.get("series_id").toString();
                map.put("series_id",series_id);
                String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
                String lecturer_id = jedis.hget(seriesKey, "lecturer_id");
                if(!lecturer_id.equals(userId)){
                    throw new QNLiveException("210001");
                }
                String lectureSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_UP, map);
                long seriesLpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis() , MiscUtils.convertObjectToLong(course.get("position")));
                jedis.zadd(lectureSeriesKey, seriesLpos, course.get("course_id"));
                lectureModuleServer.increaseSeriesCourse(series_id);
                jedis.del(seriesKey);
                //获取系列课程详情
                Map<String, String> series = CacheUtils.readSeries(series_id, generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
                //更新排序
                jedis.zrem(Constants.CACHED_KEY_PLATFORM_SERIES_APP_PLATFORM,series_id);
                long lpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(series.get("position")));
                jedis.zadd(Constants.CACHED_KEY_PLATFORM_SERIES_APP_PLATFORM,lpos,series_id);
                //TODO 订阅的系列发布了新的课程 推送提醒
                map.put("lecturer_id", userId);
                Map<String, String> lecturer = CacheUtils.readLecturer(userId, generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);
                String nickName = MiscUtils.RecoveryEmoji(lecturer.get("nick_name"));
                List<Map<String, Object>> seriesStudentList = lectureModuleServer.findSeriesStudentListBySeriesId(series_id);
                if(!MiscUtils.isEmpty(seriesStudentList)){
                    Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
                    TemplateData first = new TemplateData();
                    first.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    first.setValue(String.format(MiscUtils.getConfigByKey("wpush_follow_series_first",appName),MiscUtils.RecoveryEmoji(series.get("series_title"))));
                    templateMap.put("first", first);
                    TemplateData name = new TemplateData();
                    name.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    name.setValue(MiscUtils.RecoveryEmoji(course.get("course_title")));
                    templateMap.put("keyword1", name);
                    TemplateData wuliu = new TemplateData();
                    wuliu.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    String content = "";
                    wuliu.setValue(content);
                    templateMap.put("keyword2", wuliu);
                    TemplateData orderNo = new TemplateData();
                    orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    orderNo.setValue(MiscUtils.RecoveryEmoji(nickName));
                    templateMap.put("keyword3", orderNo);
                    Date  startTime1 = new Date(Long.parseLong(reqMap.get("start_time").toString()));
                    TemplateData receiveAddr = new TemplateData();
                    receiveAddr.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    receiveAddr.setValue(MiscUtils.parseDateToFotmatString(startTime1, "yyyy-MM-dd HH:mm"));
                    templateMap.put("keyword4", receiveAddr);
                    TemplateData remark = new TemplateData();
                    if(appName.equals(Constants.HEADER_APP_NAME)){
                        remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
                    }else{
                        remark.setColor(Constants.WE_CHAT_PUSH_COLOR_DLIVE);
                    }
                    remark.setValue(MiscUtils.getConfigByKey("wpush_follow_series_remark",appName));
                    templateMap.put("remark", remark);

                    Map<String, Object> wxPushParam = new HashMap<>();
                    wxPushParam.put("templateParam", templateMap);//模板消息
                    course.put("series_title", series.get("series_title"));
                    wxPushParam.put("course", course);//课程ID
                    wxPushParam.put("followers", seriesStudentList);//直播间关注者
                    wxPushParam.put("pushType", "3");//1创建课程 2更新课程 3系列更新课程
                    RequestEntity mqRequestEntity = new RequestEntity();
                    mqRequestEntity.setServerName("MessagePushServer");
                    mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);//异步进行处理
                    mqRequestEntity.setFunctionName("noticeCourseToFollower");
                    mqRequestEntity.setParam(wxPushParam);
                    mqRequestEntity.setAppName(appName);
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
            }else{
                /*4.4 将课程插入到 我的课程列表预告课程列表 SYS: lecturer:{lecturer_id}courses:prediction*/
                map.clear();
                map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
                String predictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
                String classify_id = "";
                if(MiscUtils.isEmpty(reqMap.get("classify_id"))){
                    classify_id= Constants.COURSE_DEFAULT_CLASSINFY;
                }else{
                    classify_id = reqMap.get("classify_id").toString();
                }
                long lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("start_time")) , MiscUtils.convertObjectToLong(course.get("position")));
                //4.5 将课程插入到平台课程列表 预告课程列表 SYS:courses:prediction
                String platformCourseList = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
                jedis.zadd(platformCourseList, lpos, courseId);
                jedis.zadd(predictionKey, lpos, courseId);
                jedis.zadd(Constants.SYS_COURSES_RECOMMEND_PREDICTION, 0, courseId);//热门推荐 预告
                map.put(Constants.CACHED_KEY_CLASSIFY, classify_id);
                String classifyCourseKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);
                jedis.zadd(classifyCourseKey, lpos, courseId);

                //</editor-fold>
                //<editor-fold desc="单品课推送">
                map.clear();
                map.put("lecturer_id", userId);
                Map<String, String> lecturer = CacheUtils.readLecturer(userId, generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);
                String nickName = MiscUtils.RecoveryEmoji(lecturer.get("nick_name"));
                String courseTitle = MiscUtils.RecoveryEmoji(course.get("course_title"));
                //取出粉丝列表
                List<Map<String,Object>> findFollowUser = lectureModuleServer.findRoomFanListWithLoginInfo(roomId);
                Map<String,Object> queryNo = new HashMap<String,Object>();
                queryNo.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, userId);
                String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, queryNo);
                Map<String, String> serviceNoMap = jedis.hgetAll(serviceNoKey);
                if (MiscUtils.isEmpty(serviceNoMap)) {
                    Map<String, Object> serviceNoMapObj = lectureModuleServer.findServiceNoInfoByLecturerId(userId); //查找授权信息
                    MiscUtils.converObjectMapToStringMap(serviceNoMapObj, serviceNoMap);
                }
                //关注的直播间有新的课程，推送提醒
                if (!MiscUtils.isEmpty(findFollowUser) || !MiscUtils.isEmpty(serviceNoMap)) {
                    Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
                    TemplateData first = new TemplateData();
                    first.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    first.setValue(MiscUtils.getConfigByKey("wpush_follow_course_first",appName));
                    templateMap.put("first", first);
                    TemplateData name = new TemplateData();
                    name.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    name.setValue(MiscUtils.RecoveryEmoji(courseTitle));
                    templateMap.put("keyword1", name);
                    TemplateData wuliu = new TemplateData();
                    wuliu.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    String content = MiscUtils.RecoveryEmoji(course.get("course_remark"));
                    if(MiscUtils.isEmpty(content)){
                        content = "";
                    }
                    wuliu.setValue(content);
                    templateMap.put("keyword2", wuliu);

                    TemplateData orderNo = new TemplateData();
                    orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    orderNo.setValue(MiscUtils.RecoveryEmoji(nickName));
                    templateMap.put("keyword3", orderNo);
                    Date  startTime1 = new Date(Long.parseLong(reqMap.get("start_time").toString()));
                    TemplateData receiveAddr = new TemplateData();
                    receiveAddr.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    receiveAddr.setValue(MiscUtils.parseDateToFotmatString(startTime1, "yyyy-MM-dd HH:mm"));
                    templateMap.put("keyword4", receiveAddr);
                    TemplateData remark = new TemplateData();
                    if(appName.equals(Constants.HEADER_APP_NAME)){
                        remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
                    }else{
                        remark.setColor(Constants.WE_CHAT_PUSH_COLOR_DLIVE);
                    }
                    remark.setValue(String.format(MiscUtils.getConfigByKey("wpush_follow_course_remark",appName),MiscUtils.RecoveryEmoji(nickName)));
                    templateMap.put("remark", remark);

                    if (!MiscUtils.isEmpty(findFollowUser) ) { //有关注者
                        Map<String, Object> wxPushParam = new HashMap<>();
                        wxPushParam.put("templateParam", templateMap);//模板消息
                        course.put("room_name", jedis.hget(liveRoomKey, "room_name"));
                        wxPushParam.put("course", course);//课程ID
                        wxPushParam.put("followers", findFollowUser);//直播间关注者
                        wxPushParam.put("pushType", "1");//1创建课程 2更新课程
                        RequestEntity mqRequestEntity = new RequestEntity();
                        mqRequestEntity.setServerName("MessagePushServer");
                        mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);//异步进行处理
                        mqRequestEntity.setFunctionName("noticeCourseToFollower");
                        mqRequestEntity.setParam(wxPushParam);
                        mqRequestEntity.setAppName(appName);
                        this.mqUtils.sendMessage(mqRequestEntity);
                    }
                    if (!MiscUtils.isEmpty(serviceNoMap)) { //该讲师绑定服务号，推送提醒给粉丝 1.判断不为空
                        log.debug("进入讲师有绑定服务号--------------------:"+serviceNoMap);
                        String authorizer_access_token = getWeServiceNo(serviceNoMap, userId, serviceNoKey, jedis,appName);
                        log.debug("验证讲师服务号token--------------------");
                        if (authorizer_access_token != null) {
                            Map<String, Object> wxPushParam = new HashMap<>();
                            wxPushParam.put("templateParam", templateMap);//模板消息
                            wxPushParam.put("course_id", courseId);//课程ID
                            wxPushParam.put("lecturer_id", userId);
                            wxPushParam.put("authorizer_appid", serviceNoMap.get("authorizer_appid"));//第三方服务号的
                            wxPushParam.put("accessToken", authorizer_access_token);//课程ID
                            wxPushParam.put("pushType", "1");//1创建课程 2更新课程
                            String url = MiscUtils.getConfigByKey("course_share_url_pre_fix",appName)+courseId;//推送url
                            log.debug("发送mq消息进行异步处理--------------------");
                            Map<String, Object> weCatTemplateInfo = getWeCatTemplateInfo(wxPushParam, appName);
                            RequestEntity mqRequestEntity = new RequestEntity();
                            mqRequestEntity.setServerName("MessagePushServer");
                            mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);//异步进行处理
                            mqRequestEntity.setFunctionName("noticeCourseToServiceNoFollow");
                            mqRequestEntity.setParam(weCatTemplateInfo);
                            mqRequestEntity.setAppName(appName);
                            noticeCourseToServiceNoFollow(mqRequestEntity,jedisUtils,null);
                        }
                    }
                }
                if ("0".equals(course_type)){//公开课才开启机器人
                    log.info("创建课程，开始机器人加入功能");
                    map.clear();
                    map.put("course_id", courseId);
                    RequestEntity mqRequestEntity = new RequestEntity();
                    mqRequestEntity.setServerName("CourseRobotService");
                    mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                    mqRequestEntity.setFunctionName("courseCreateAndRobotStart");
                    mqRequestEntity.setParam(map);
                    mqRequestEntity.setAppName(appName);
                    this.mqUtils.sendMessage(mqRequestEntity);
                    jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
                }
                //</editor-fold>

            }
            //4.6 将课程插入到平台分类列表 分类列表
            map.clear();
            //<editor-fold desc="加入定时任务">
            resultMap.put("course_id", courseId);
            Map<String,Object> timerMap = new HashMap<>();
            timerMap.put("course_id", courseId);
            timerMap.put("room_id", roomId);
            timerMap.put("lecturer_id", userId);
            timerMap.put("course_title", course.get("course_title"));
            timerMap.put("start_time", startTime);
            timerMap.put("position", course.get("position"));
            timerMap.put("im_course_id", course.get("im_course_id"));
            timerMap.put("real_start_time",  startTime);
            if(true){
                RequestEntity mqRequestEntity = new RequestEntity();
                mqRequestEntity.setServerName("MessagePushServer");
                mqRequestEntity.setParam(timerMap);
                mqRequestEntity.setAppName(appName);
                mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                log.debug("课程直播超时处理 服务端逻辑 定时任务 course_id:"+courseId);
                mqRequestEntity.setFunctionName("processCourseLiveOvertime");
                this.mqUtils.sendMessage(mqRequestEntity);
                log.debug("进行超时预先提醒定时任务 提前60分钟 提醒课程结束 course_id:"+courseId);
                mqRequestEntity.setFunctionName("processLiveCourseOvertimeNotice");
                this.mqUtils.sendMessage(mqRequestEntity);
                if(MiscUtils.isTheSameDate(new Date(startTime), new Date())){
                    log.debug("提前五分钟开课提醒 course_id:"+courseId);
                    if(startTime-System.currentTimeMillis()> 5 * 60 *1000){
                        mqRequestEntity.setFunctionName("processCourseStartShortNotice");
                        this.mqUtils.sendMessage(mqRequestEntity);
                    }
                    //如果该课程为今天内的课程，则调用MQ，将其加入课程超时未开播定时任务中  结束任务 开课时间到但是讲师未出现提醒  推送给参加课程者
                    mqRequestEntity.setFunctionName("processCourseStartLecturerNotShow");
                    this.mqUtils.sendMessage(mqRequestEntity);
                    log.debug("直播间开始发送IM  course_id:"+courseId);
                    mqRequestEntity.setFunctionName("processCourseStartIM");
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
                //提前24小时开课提醒
                if(MiscUtils.isTheSameDate(new Date(startTime- 60 * 60 *1000*24), new Date()) && startTime-System.currentTimeMillis()> 60 * 60 *1000*24){
                    mqRequestEntity.setFunctionName("processCourseStartLongNotice");
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
            }

            //</editor-fold>
        }else if("2".equals(reqMap.get("updown"))){
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD,course.get(Constants.CACHED_KEY_LECTURER_FIELD));
            long lpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(course.get("position")));
            //加入讲师下架课程列表
            resultMap.put("course_id", course.get("course_id"));
            if(!MiscUtils.isEmpty(course.get("series_id"))){
                map.put("series_id",course.get("series_id"));
                String lectureSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_DOWN, map);
                jedis.zadd(lectureSeriesKey, lpos, course.get("course_id"));
            }else{
                String lectureCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_DOWN, map);
                jedis.zadd(lectureCourseKey, lpos, course.get("course_id"));
            }
        }
        //给课程里面推消息
        Map<String, Object> userInfo = lectureModuleServer.findUserInfoByUserId(course.get("lecturer_id"));
        Map<String,Object> startLecturerMessageInformation = new HashMap<>();
        startLecturerMessageInformation.put("creator_id",userInfo.get("user_id"));//发送人id
        startLecturerMessageInformation.put("course_id", course.get("course_id"));//课程id
        startLecturerMessageInformation.put("message",MiscUtils.getConfigByKey("start_lecturer_message",appName));
        startLecturerMessageInformation.put("message_type", "1");
        startLecturerMessageInformation.put("message_id",MiscUtils.getUUId());
        startLecturerMessageInformation.put("message_imid",startLecturerMessageInformation.get("message_id"));
        startLecturerMessageInformation.put("create_time",  System.currentTimeMillis());
        startLecturerMessageInformation.put("send_type","0");
        startLecturerMessageInformation.put("creator_avatar_address",userInfo.get("avatar_address"));
        startLecturerMessageInformation.put("creator_nick_name",userInfo.get("nick_name"));

        String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, startLecturerMessageInformation);
//					//1.将聊天信息id插入到redis zsort列表中
        jedis.zadd(messageListKey,  System.currentTimeMillis(), (String)startLecturerMessageInformation.get("message_imid"));
//					//添加到老师发送的集合中
        String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, startLecturerMessageInformation);
        jedis.zadd(messageLecturerListKey,  System.currentTimeMillis(),startLecturerMessageInformation.get("message_imid").toString());
        String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, startLecturerMessageInformation);//直播间开始于
        Map<String,String> result = new HashMap<String,String>();
        MiscUtils.converObjectMapToStringMap(startLecturerMessageInformation, result);
        jedis.hmset(messageKey, result);
        //</editor-fold>
        long lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("start_time")) , MiscUtils.convertObjectToLong(course.get("position")));
        //设置讲师更新课程 30分钟
        jedis.zadd(lecturerCoursesAllKey,lpos,course.get("course_id"));
        return resultMap;
    }


    public void noticeCourseToServiceNoFollow(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();//转换参数
        log.debug("---------------将课程创建的信息推送给服务号的粉丝"+reqMap);
        String appName = requestEntity.getAppName();
        String accessToken = reqMap.get ("accessToken").toString();
        String type = reqMap.get("pushType").toString();//类型 1 是创建课程 2 是更新课程时间
        String authorizer_appid = reqMap.get("authorizer_appid").toString();
        String courseId = reqMap.get("course_id").toString();//课程id
        String templateId = reqMap.get("template_id").toString();
        if(templateId != null){
            log.info("===================================微信发送模板消息:"+templateId+"========================================");
            Map<String, TemplateData> templateMap = (Map<String, TemplateData>) reqMap.get("templateParam");//模板数据

            String url = MiscUtils.getConfigByKey("course_share_url_pre_fix",appName)+courseId;//推送url

            String next_openid = toServiceNoFollow(null, accessToken, url, templateId, templateMap,appName);
            while (next_openid != null) {
                next_openid = toServiceNoFollow(next_openid, accessToken, url, templateId, templateMap,appName);
            }
        }
    }


//         * 把课程创建的模板消息推送给服务号粉丝的具体逻辑
//     */
    public String toServiceNoFollow(String next_openid, String accessToken, String url, String templateId, Map<String, TemplateData> templateMap,String appName) {
        //step1 获取粉丝信息 可能多页
        JSONObject fansInfo = WeiXinUtil.getServiceFansList(accessToken, next_openid,appName);
        JSONArray fansOpenIDArr = fansInfo.getJSONObject("data").getJSONArray("openid");

        //step2 循环推送模板消息
        for (Object openID: fansOpenIDArr) {
            int result = WeiXinUtil.sendTemplateMessageToServiceNoFan(accessToken, String.valueOf(openID), url, templateId, templateMap,appName);
            if (result != 0) {
                //step3 出现失败的情况（服务号可能没设置这个行业和模板ID ）就中断发送
                log.error("给第三方服务号：{} 粉丝推送模板消息出现错误：{}", accessToken, result);
                return null;
            }
        }
        if (fansInfo.getIntValue("count") == 10000) {
            return fansInfo.getString("next_openid");
        }
        return null;
    }


    /**
     * 获取微信模板对象
     * @return
     */
    private Map<String,Object> getWeCatTemplateInfo(Map<String, Object> reqMap,String appName){
        String accessToken = reqMap.get ("accessToken").toString();
        String type = reqMap.get("pushType").toString();//类型 1 是创建课程 2 是更新课程时间
        String authorizer_appid = reqMap.get("authorizer_appid").toString();
        //判断是否有模板id
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("authorizer_appid", authorizer_appid);
        paramMap.put("template_type", type);

        Map<String,Object> templateInfo  = lectureModuleServer.findServiceTemplateInfoByLecturerId(paramMap);//查找微信推送模板
        String templateId = null;
        //没有 则创建
        if (MiscUtils.isEmpty(templateInfo)) {//为空
            JSONObject templateJson = WeiXinUtil.createServiceTemplateInfo(accessToken, type,appName);//增加推送末班
            Object errcode = templateJson.get("errcode");
            if (errcode != null && Integer.parseInt(errcode.toString()) != 0) {//创建失败
                log.error("创建模板消息出错 +++++ ", templateJson);
                reqMap.put("template_id",null);
            } else {//创建成功 则存到数据库
                templateId = templateJson.getString("template_id");
                reqMap.put("template_id",templateId);
                paramMap.put("template_id", templateId);
                paramMap.put("lecturer_id", reqMap.get("lecturer_id").toString());
                lectureModuleServer.insertServiceTemplateInfo(paramMap);
            }
        } else {
            templateId = templateInfo.get("template_id").toString();
            reqMap.put("template_id",templateId);
        }
        return reqMap;
    }



    public String getWeServiceNo(Map<String, String> serviceNoMap, String userId, String serviceNoKey, Jedis jedis,String appName){
        String expiresTimes = serviceNoMap.get("expires_time");
        String authorizer_access_token = serviceNoMap.get("authorizer_access_token");

        long expiresTimeStamp = Long.parseLong(expiresTimes);
        //是否快要超时 令牌是存在有效期（2小时）
        long nowTimeStamp = System.currentTimeMillis();
        if (nowTimeStamp-expiresTimeStamp > 0) {  //accessToken已经过期了

            String authorizer_appid = serviceNoMap.get("authorizer_appid");
            String authorizer_refresh_token = serviceNoMap.get("authorizer_refresh_token");

            String component_access_token = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "component_access_token");
            JSONObject authJsonObj = WeiXinUtil.refreshServiceAuthInfo(component_access_token, authorizer_refresh_token, authorizer_appid,appName);
            Object errCode = authJsonObj.get("errcode");
            if (errCode != null ) {
                log.error("创建课程推送给讲师的服务号粉丝过程中出现错误----"+authJsonObj);
                return null;
            } else {
                authorizer_access_token = authJsonObj.getString("authorizer_access_token");
                authorizer_refresh_token = authJsonObj.getString("authorizer_refresh_token");
                long expiresIn = authJsonObj.getLongValue("expires_in")*1000;//有效毫秒值
                expiresTimeStamp = nowTimeStamp+expiresIn;//当前毫秒值+有效毫秒值

                //更新服务号的授权信息
                Map<String, String> authInfoMap = new HashMap<>();
                authInfoMap.put("lecturer_id", userId);
                authInfoMap.put("authorizer_appid", authorizer_appid);
                authInfoMap.put("authorizer_access_token", authorizer_access_token);
                authInfoMap.put("authorizer_refresh_token", authorizer_refresh_token);
                authInfoMap.put("expiresTimeStamp", String.valueOf(expiresTimeStamp));

                //更新服务号信息插入数据库
                authInfoMap.put("update_time", String.valueOf(nowTimeStamp));
                lectureModuleServer.updateServiceNoInfo(authInfoMap);

                //access_tokens刷新之后 需要更新redis里的相关数据
                jedis.hset(serviceNoKey, "authorizer_appid", authorizer_appid);
                jedis.hset(serviceNoKey, "authorizer_access_token", authorizer_access_token);
                jedis.hset(serviceNoKey, "authorizer_refresh_token", authorizer_refresh_token);
                jedis.hset(serviceNoKey, "expiresTimeStamp", String.valueOf(expiresTimeStamp));
            }
        }
        return authorizer_access_token;

        //现在开启定时器直接刷新accessToken所以 不需要以上代码
//        return serviceNoMap.get("authorizer_access_token");
    }

    @SuppressWarnings("unchecked")
    @FunctionName("courseDetail")
    public Map<String, Object> getCourseDetail(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String course_id = (String)reqMap.get("course_id");
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String ,String> courseInfoMap = CacheUtils.readCourse(course_id,reqEntity,readCourseOperation, jedis,false);
        if(MiscUtils.isEmpty(courseInfoMap)){
            throw new QNLiveException("100004");
        }
        MiscUtils.courseTranferState(System.currentTimeMillis(), courseInfoMap);
        Map<String ,Object> resultMap = new HashMap<>();
        resultMap.putAll(courseInfoMap);
        
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("size",10);
        queryMap.put("course_id",reqMap.get("course_id").toString());



        List<String> latestStudentAvatarAddList = lectureModuleServer.findLatestStudentAvatarAddList(queryMap);



        if(! MiscUtils.isEmpty(latestStudentAvatarAddList)){
            resultMap.put("student_list", latestStudentAvatarAddList);
        }        
        String room_id = courseInfoMap.get("room_id");
        reqMap.put("room_id", room_id);
        //从缓存中获取直播间信息
        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(room_id, reqEntity, readLiveRoomOperation, jedis, true);
        if(MiscUtils.isEmpty(liveRoomMap)){
            throw new QNLiveException("100031");
        }
        resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));
        
        //分享URL
        resultMap.put("share_url",getCourseShareURL(userId, course_id, courseInfoMap,jedis,appName));
        List<String> roles = new ArrayList<>();
        roles.add("3");//角色数组  1：普通用户、2：学员、3：讲师
        resultMap.put("roles",roles);
 
        return resultMap;
    }

    private String getCourseShareURL(String userId, String courseId, Map<String,String> courseMap,Jedis jedis,String appName) throws Exception{
        String share_url ;
        String roomId = courseMap.get("room_id");
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("distributer_id", userId);
        queryMap.put("room_id", roomId);
        Map<String,String> distributerRoom = CacheUtils.readDistributerRoom(userId, roomId, readRoomDistributerOperation, jedis);

        boolean isDistributer = false;
        String recommend_code = null;
        if (! MiscUtils.isEmpty(distributerRoom)) {
            isDistributer = true;
            recommend_code = distributerRoom.get("rq_code");
        }
        //是分销员
        if(isDistributer == true){
            share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix",appName) + courseId + "&recommend_code=" + recommend_code;
        }else {
            //不是分销员
            share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix",appName) + courseId;
        }
        return share_url;
    }


    /**
     * 修改课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("updateCourse")
    public Map<String, Object> updateCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();//传过来的参数
        Map<String, Object> resultMap = new HashMap<String, Object>();//返回的参数

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//获取userId
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());//课程id
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//"SYS:COURSE:{course_id}"
        String course_id = (String) reqMap.get("course_id");
        
        Map<String, String> course = CacheUtils.readCourse((String)course_id, generateRequestEntity(null, null, null, reqMap), readCourseOperation, jedis, false);//课程信息
        if(MiscUtils.isEmpty(course)){//如果没有课程
        	throw new QNLiveException("100004");//课程不存在 抛异常
        }

        String status = course.get("status");//状态
        if("2".equals(status)){//如果查出来的课程是结束
        	if(reqMap.get("start_time") != null){
        		throw new QNLiveException("100010");//课程状态不正确，无法修改
        	}
        } else if("2".equals(status) && "2".equals(reqMap.get("status"))){
        	throw new QNLiveException("100010");
        }
        
        String newStatus = (String)reqMap.get("status");//穿过来的状态
        //<editor-fold desc="1校验课程更新时间">
        if (!"2".equals(newStatus)){ //如果不是结束课程
        	if(MiscUtils.isEmpty(reqMap.get("update_time"))){
        		throw new QNLiveException("000100");
        	}
            String update_time_cache = course.get("update_time");
            if (!MiscUtils.isEqual(update_time_cache, String.valueOf(reqMap.get("update_time")))) {
                throw new QNLiveException("000104");
            }
        }
        //</editor-fold>
        
        //0.3校验该课程是否属于该用户
        String courseOwner = course.get("lecturer_id");//讲师id
        if(!userId.equals(courseOwner)){//如果没有
            throw new QNLiveException("100013");
        }
        
        String original_start_time = course.get("start_time");//课程开课时间

        //<editor-fold desc="结束课程">
        if ("2".equals(newStatus)) {
            //1.1如果为课程结束，则取当前时间为课程结束时间
            //1.2更新课程详细信息(dubble服务)
            String start_time = original_start_time;
            //判断是否可以结束
            try{
                if(System.currentTimeMillis() <= Long.parseLong(start_time)){//判断是否可以结束
                    throw new QNLiveException("100032");
                }
            } catch(Exception e){
                if(e instanceof QNLiveException){
                    throw e;
                }
            }

            Date courseEndTime = new Date();//结束时间
            reqMap.put("now",courseEndTime);
            Map<String, Object> dbResultMap = lectureModuleServer.updateCourse(reqMap);
            if (dbResultMap == null || dbResultMap.get("update_count") == null || dbResultMap.get("update_count").toString().equals("0")) {
                throw new QNLiveException("100005");
            }

            //1.3将该课程从讲师的预告课程列表 SYS: lecturer:{ lecturer_id }：courses  ：prediction移动到结束课程列表 SYS: lecturer:{ lecturer_id }：courses  ：finish
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
            String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
            jedis.zrem(lecturerCoursesPredictionKey, reqMap.get("course_id").toString());
            
            long lpos = MiscUtils.convertInfoToPostion(courseEndTime.getTime(), MiscUtils.convertObjectToLong(jedis.hget(courseKey, "position")));
            
            String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
            
            jedis.zadd(lecturerCoursesFinishKey, lpos, reqMap.get("course_id").toString());

            //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移除。如果存在结束课程列表 SYS：courses ：finish，则增加到课程结束列表
            jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, reqMap.get("course_id").toString());
            jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, lpos, reqMap.get("course_id").toString());

            jedis.zrem(Constants.SYS_COURSES_RECOMMEND_LIVE,course_id);//从热门推荐正在直播列表删除
            long lops = Long.valueOf(course.get("student_num"))+ Long.valueOf(course.get("extra_num"));
            jedis.zadd(Constants.SYS_COURSES_RECOMMEND_FINISH,lops,course_id);//加入热门推荐结束列表


            map.put(Constants.CACHED_KEY_CLASSIFY,course.get("classify_id"));
            jedis.zrem(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map), reqMap.get("course_id").toString());//删除预告
            jedis.zadd(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map), lpos, reqMap.get("course_id").toString());//在结束中增加

            //1.5如果课程标记为结束，则清除该课程的禁言缓存数据
            map.put(Constants.CACHED_KEY_COURSE_FIELD, userId);
            String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
            jedis.del(banKey);
            //1.6更新课程缓存信息
            Map<String, String> updateCacheMap = new HashMap<String, String>();
            updateCacheMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
            updateCacheMap.put("end_time", courseEndTime.getTime() + "");
            updateCacheMap.put("status", "2");
            jedis.hmset(courseKey, updateCacheMap);


            //1.9如果该课程没有真正开播，并且开播时间在今天之内，则需要取消课程超时未开播定时任务
            if(jedis.hget(courseKey, "real_start_time") == null){
                String courseStartTime = jedis.hget(courseKey, "start_time");
                if(Long.parseLong(courseStartTime) < MiscUtils.getEndTimeOfToday().getTime()){
                    RequestEntity timerRequestEntity = generateRequestEntity("MessagePushServer",Constants.MQ_METHOD_ASYNCHRONIZED,"processCourseNotStartCancel",reqEntity.getParam());
                    timerRequestEntity.setAppName(appName);
                   this.mqUtils.sendMessage(timerRequestEntity);
                }
            }

            resultMap.put("update_time", courseEndTime.getTime());
            String mGroupId = jedis.hget(courseKey,"im_course_id");
            Map<String, Object> userInfo = lectureModuleServer.findUserInfoByUserId(courseOwner);
            Map<String,Object> startLecturerMessageInformation = new HashMap<>();
            String imid = MiscUtils.getUUId();
            startLecturerMessageInformation.put("creator_id",courseOwner);//发送人id
            startLecturerMessageInformation.put("course_id", reqMap.get("course_id").toString());//课程id
            startLecturerMessageInformation.put("message",MiscUtils.getConfigByKey("end_lecturer_message",appName));
            startLecturerMessageInformation.put("message_type", "1");
            startLecturerMessageInformation.put("message_id",imid);
            startLecturerMessageInformation.put("message_imid",imid);
            startLecturerMessageInformation.put("create_time",  System.currentTimeMillis());
            startLecturerMessageInformation.put("send_type","0");
            startLecturerMessageInformation.put("creator_avatar_address",userInfo.get("avatar_address"));
            startLecturerMessageInformation.put("creator_nick_name",userInfo.get("nick_name"));
            Map<String,Object> startLecturerMessageMap = new HashMap<>();
            startLecturerMessageMap.put("msg_type","1");
            startLecturerMessageMap.put("app_name",appName);
            startLecturerMessageMap.put("send_time", System.currentTimeMillis());
            startLecturerMessageMap.put("create_time", System.currentTimeMillis());
            startLecturerMessageMap.put("information",startLecturerMessageInformation);
            startLecturerMessageMap.put("mid",imid);
            String startLecturerMessageInformationContent = JSON.toJSONString(startLecturerMessageMap);
            IMMsgUtil.sendMessageInIM(mGroupId, startLecturerMessageInformationContent, "", lectureModuleServer.findLoginInfoByUserId(courseOwner).get("m_user_id").toString());//发送信息

            String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, startLecturerMessageInformation);
//					//1.将聊天信息id插入到redis zsort列表中
            jedis.zadd(messageListKey,  System.currentTimeMillis(), (String)startLecturerMessageInformation.get("message_imid"));
//					//添加到老师发送的集合中
            String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
            jedis.zadd(messageLecturerListKey,  System.currentTimeMillis(),startLecturerMessageInformation.get("message_imid").toString());

            String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, startLecturerMessageInformation);//直播间
            Map<String,String> result = new HashMap<String,String>();
            MiscUtils.converObjectMapToStringMap(startLecturerMessageInformation, result);
            jedis.hmset(messageKey, result);
            SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
            String str = sdf.format(courseEndTime);
            String courseEndMessage = "直播结束于"+str;
            //发送结束推送消息
            long currentTime = System.currentTimeMillis();
            String message = courseEndMessage;
            String sender = "system";
            Map<String,Object> infomation = new HashMap<>();
            infomation.put("course_id", reqMap.get("course_id").toString());
            infomation.put("creator_id", userId);
            infomation.put("message", message);
            infomation.put("message_type", "1");
            infomation.put("send_type", "6");//5.结束消息
            infomation.put("message_id",MiscUtils.getUUId());
            infomation.put("message_imid",infomation.get("message_id"));
            infomation.put("create_time", currentTime);
            Map<String,Object> messageMap = new HashMap<>();
            messageMap.put("msg_type","1");
            messageMap.put("app_name",appName);
            messageMap.put("send_time", System.currentTimeMillis());
            messageMap.put("create_time", System.currentTimeMillis());
            messageMap.put("information",infomation);
            messageMap.put("mid",infomation.get("message_id"));
            String content = JSON.toJSONString(messageMap);
            IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);
        }


        //</editor-fold>


        //<editor-fold desc="不是结束课程,编辑课程其他操作">
        if(!"2".equals(newStatus)){
            Map<String,Object> query = new HashMap<String,Object>();
            query.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
            String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, query);
            Date now = new Date();
			if(reqMap.get("start_time") != null){
            	//课程之间需要间隔三十分钟
                Calendar cal = Calendar.getInstance();
                long startTime = (Long)reqMap.get("start_time");
                cal.setTimeInMillis(startTime);
                cal.add(Calendar.MINUTE, -3*Constants.COURSE_MAX_INTERVAL);
                long preStartTime = cal.getTimeInMillis();
                cal.setTimeInMillis(startTime);
                cal.add(Calendar.MINUTE, 3*Constants.COURSE_MAX_INTERVAL);
                long nextStartTime = cal.getTimeInMillis();
                Set<Tuple> courseList = jedis.zrangeByScoreWithScores(lecturerCoursesPredictionKey, preStartTime+"", nextStartTime+"", 0, 1);
                course_id = (String)reqMap.get("course_id");
                if(!MiscUtils.isEmpty(courseList)){
                    for(Tuple tuple:courseList){
                        if(!course_id.equals(tuple.getElement())){
                            throw new QNLiveException("100029");
                        }
                    }                        
                }                                    
                if(now.getTime() > startTime){
                	throw new QNLiveException("100034");
                }
            }
			reqMap.put("now",now);
            //2.不为课程结束
            //修改缓存，同时修改数据库
            Map<String, Object> dbResultMap = lectureModuleServer.updateCourse(reqMap);
            if (dbResultMap == null || dbResultMap.get("update_count") == null || dbResultMap.get("update_count").toString().equals("0")) {
                throw new QNLiveException("100005");
            }

            Map<String, String> updateCacheMap = new HashMap<String, String>();
            if (!MiscUtils.isEmpty(reqMap.get("course_title"))) {
                updateCacheMap.put("course_title", reqMap.get("course_title").toString());
            }
            if (reqMap.get("course_remark") != null) {
                updateCacheMap.put("course_remark", reqMap.get("course_remark").toString());
            }
            if (!MiscUtils.isEmpty(reqMap.get("course_url"))) {
                updateCacheMap.put("course_url", reqMap.get("course_url").toString());
            }
            if (!MiscUtils.isEmpty(reqMap.get("course_password"))) {
                updateCacheMap.put("course_password", reqMap.get("course_password").toString());
            }
            if (reqMap.get("start_time") != null) {
            	updateCacheMap.put("start_time", reqMap.get("start_time").toString());
            }
            updateCacheMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
            if(jedis.exists(courseKey)){
            	jedis.hmset(courseKey, updateCacheMap);
            }
            
            if (reqMap.get("start_time") != null) { //开课时间修改
                String newStartTime = reqMap.get("start_time").toString();

                jedis.zadd(lecturerCoursesPredictionKey, Long.parseLong(newStartTime), course_id); //lecturerCoursesPredictionKey

                query.clear();
                query.put("course_id", course_id);
                course = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, true);
                long startTime = MiscUtils.convertObjectToLong(reqMap.get("start_time"));
                Map<String, Object> timerMap = new HashMap<>();
                timerMap.put("course_id", course_id);
                timerMap.put("start_time", new Date(startTime));
                timerMap.put("lecturer_id", userId);
                timerMap.put("course_title", course.get("course_title"));
                timerMap.put("course_id", course.get("course_id"));
                timerMap.put("start_time", startTime + "");
                timerMap.put("position", course.get("position"));

                RequestEntity mqRequestEntity = new RequestEntity();
                mqRequestEntity.setServerName("MessagePushServer");
                mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                mqRequestEntity.setParam(timerMap);
                mqRequestEntity.setAppName(appName);

                log.debug("清除所有定时任务 course_id:"+course_id);
                mqRequestEntity.setFunctionName("processCourseNotStartCancelAll"); //课程未开播所有清除所有定时任务
                this.mqUtils.sendMessage(mqRequestEntity);

                log.debug("课程直播超时处理 服务端逻辑 定时任务 course_id:"+course_id);
                mqRequestEntity.setFunctionName("processCourseLiveOvertime");
                this.mqUtils.sendMessage(mqRequestEntity);
                log.debug("进行超时预先提醒定时任务 提前60分钟 提醒课程结束 course_id:"+course_id);
                mqRequestEntity.setFunctionName("processLiveCourseOvertimeNotice");
                this.mqUtils.sendMessage(mqRequestEntity);
                if(MiscUtils.isTheSameDate(new Date(startTime), new Date())){
                    log.debug("提前五分钟开课提醒 course_id:"+course_id);
                    if(startTime-System.currentTimeMillis()> 5 * 60 *1000){
                        mqRequestEntity.setFunctionName("processCourseStartShortNotice");
                        this.mqUtils.sendMessage(mqRequestEntity);
                    }
                    //如果该课程为今天内的课程，则调用MQ，将其加入课程超时未开播定时任务中  结束任务 开课时间到但是讲师未出现提醒  推送给参加课程者
                    mqRequestEntity.setFunctionName("processCourseStartLecturerNotShow");
                    this.mqUtils.sendMessage(mqRequestEntity);
                    log.debug("直播间开始发送IM  course_id:"+course_id);
                    mqRequestEntity.setFunctionName("processCourseStartIM");
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
                //提前24小时开课提醒
                if(MiscUtils.isTheSameDate(new Date(startTime- 60 * 60 *1000*24), new Date()) && startTime-System.currentTimeMillis()> 60 * 60 *1000*24){
                    mqRequestEntity.setFunctionName("processCourseStartLongNotice");
                    this.mqUtils.sendMessage(mqRequestEntity);
                }

                long lpos = MiscUtils.convertInfoToPostion(Long.parseLong(newStartTime), MiscUtils.convertObjectToLong(course.get("position")));
                jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, lpos, reqMap.get("course_id").toString());


                //TODO 推送多人  报名的所有人
                List<Map<String, Object>> userInfo = lectureModuleServer.findCourseStudentListWithLoginInfo(course_id);

                //是否存在讲师服务号信息
                Map<String, Object> queryNo = new HashMap<String, Object>();
                queryNo.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, userId);
                String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, queryNo);
                Map<String, String> serviceNoMap = jedis.hgetAll(serviceNoKey);
                if (MiscUtils.isEmpty(serviceNoMap)) {
                    Map<String, Object> serviceNoMapObj = lectureModuleServer.findServiceNoInfoByLecturerId(userId);
                    MiscUtils.converObjectMapToStringMap(serviceNoMapObj, serviceNoMap);
                }

                if (!MiscUtils.isEmpty(userInfo) || !MiscUtils.isEmpty(serviceNoMap)) {

                    Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
                    TemplateData first = new TemplateData();
                    first.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    first.setValue(MiscUtils.getConfigByKey("wpush_update_course_first",appName));
                    templateMap.put("first", first);

                    TemplateData wuliu = new TemplateData();
                    wuliu.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    wuliu.setValue(course.get("course_title").toString());
                    templateMap.put("keyword1", wuliu);

                    TemplateData name = new TemplateData();
                    name.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    name.setValue("修改部分课程信息！");
                    templateMap.put("keyword2", name);

                    TemplateData orderNo = new TemplateData();
                    orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    orderNo.setValue(MiscUtils.parseDateToFotmatString(new Date(MiscUtils.convertObjectToLong(original_start_time)), "yyyy-MM-dd HH:mm:ss"));
                    templateMap.put("keyword3", orderNo);

                    TemplateData nowDate = new TemplateData();
                    nowDate.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    nowDate.setValue(MiscUtils.parseDateToFotmatString(new Date(Long.parseLong(newStartTime)), "yyyy-MM-dd HH:mm:ss"));
                    templateMap.put("keyword4", nowDate);

                    TemplateData remark = new TemplateData();
                    if(appName.equals(Constants.HEADER_APP_NAME)){
                        remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
                    }else{
                        remark.setColor(Constants.WE_CHAT_PUSH_COLOR_DLIVE);
                    }
                    remark.setValue(MiscUtils.getConfigByKey("wpush_update_course_remark",appName));
                    templateMap.put("remark", remark);

                    if (!MiscUtils.isEmpty(userInfo)) { //存在关注者信息
                        Map<String, Object> wxPushParam = new HashMap<>();
                        wxPushParam.put("templateParam", templateMap);

                        Date startDate = new Date(startTime);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
                        String str = sdf.format(startDate);
                        course.put("start_time", str);
                        wxPushParam.put("courseInfo", course);
                        wxPushParam.put("pushType", "2");

                        RequestEntity wxMqRequestEntity = new RequestEntity();
                        wxMqRequestEntity.setServerName("MessagePushServer");
                        wxMqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                        wxMqRequestEntity.setFunctionName("noticeCourseToFollower");
                        wxMqRequestEntity.setParam(wxPushParam);
                        wxMqRequestEntity.setAppName(appName);
                        this.mqUtils.sendMessage(wxMqRequestEntity);
                    }

                    if (!MiscUtils.isEmpty(serviceNoMap)) { //存在服务号信息
                        String authorizer_access_token = getWeServiceNo(serviceNoMap, userId, serviceNoKey, jedis,appName);
                        if (authorizer_access_token != null) {
                            Map<String, Object> wxPushParam = new HashMap<>();
                            wxPushParam.put("templateParam", templateMap);//模板消息
                            wxPushParam.put("course_id", course_id);//课程ID
                            wxPushParam.put("lecturer_id", userId);
                            wxPushParam.put("authorizer_appid", serviceNoMap.get("authorizer_appid"));
                            wxPushParam.put("accessToken", authorizer_access_token);//课程ID
                            wxPushParam.put("pushType", "2");//1创建课程 2更新课程

                            RequestEntity serviceRequestEntity = new RequestEntity();
                            serviceRequestEntity.setServerName("MessagePushServer");
                            serviceRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);//异步进行处理
                            serviceRequestEntity.setFunctionName("noticeCourseToServiceNoFollow");
                            serviceRequestEntity.setParam(getWeCatTemplateInfo(wxPushParam,appName));
                            serviceRequestEntity.setAppName(appName);
                            this.mqUtils.sendMessage(serviceRequestEntity);
                        }
                    }
                }
            }

            resultMap.put("update_time", updateCacheMap.get("update_time"));
        }
        //</editor-fold>
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
        return resultMap;
    }
    /**
     * 直播间收入
     * */
    @FunctionName("courseList")
    public Map<String, Object> getCourseList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //目前一个讲师仅能创建一个直播间，所以查询的是该讲师发布的课程
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        String lecturerId = userId;
        if(MiscUtils.isEmpty(lecturerId)){
            throw new QNLiveException("120018");
        }
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerCoursesAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, map);
        String course_id = (String)reqMap.get("course_id");
        int pageCount = (int)reqMap.get("page_count");
        Map <String,Object> resultMap = new HashMap<>();
        Map<String, Object> platformCourses = getPlatformCourses(pageCount, course_id, appName, lecturerId);
        resultMap.putAll(platformCourses);
        resultMap.put("course_num",jedis.zrange(lecturerCoursesAllKey,0,-1).size());
        return resultMap;
    }
    /**
     * 获取课程列表
     * 关键redis
     *  SYS:COURSES:PREDICTION    平台的预告中课程列表  Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION
     * SYS:COURSES:FINISH  平台的已结束课程列表   Constants.CACHED_KEY_PLATFORM_COURSE_FINISH
     * @param pageCount 需要几个对象
     * @param courseId 课程id  查找第一页的时候不传 进行分页 必传
     */
    @SuppressWarnings({ "unchecked"})
    private  Map<String, Object> getPlatformCourses(int pageCount,String courseId,String appName,String lecture_id) throws Exception{
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        int pageConts = pageCount;
        long currentTime = System.currentTimeMillis();//当前时间
        int offset = 0;//偏移值
        Set<String> courseIdSet;//查询的课程idset
        List<String> courseIdList = new ArrayList<>();//课程id列表
        List<Map<String,String>> courseList = new LinkedList<>();//课程对象列表
        Map<String, Object> resultMap = new HashMap<String, Object>();//最后返回的结果对象
        //判断传过来的课程状态
        //<editor-fold desc="获取课程idList">
        long startIndex = 0L;//坐标起始位
        long endIndex = 0L;//坐标结束位


        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
        String getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, map);//讲师
        long endCourseSum = jedis.zcard(getCourseIdKey);//获取总共有多少个结束课程
        if(MiscUtils.isEmpty(courseId)){//如果没有传入courceid 那么就是最开始的查询  进行倒叙查询 查询现在的
            endIndex = -1;
            startIndex = endCourseSum - pageCount;//利用总数减去我这边需要获取的数
            if(startIndex < 0){
                startIndex = 0;
            }
        }else{//传了courseid
            long endRank = jedis.zrank(getCourseIdKey, courseId);
            endIndex = endRank - 1;
            if(endIndex >= 0){
                startIndex = endIndex - pageCount + 1;
                if(startIndex < 0){
                    startIndex = 0;
                }
            }
        }
        courseIdSet = jedis.zrange(getCourseIdKey, startIndex, endIndex);
        for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
            courseIdList.add(course_id);
        }
        Collections.reverse(courseIdList);
        //</editor-fold>
        if(courseIdList.size() > 0){
            Map<String,String> queryParam = new HashMap<String,String>();
            for(String course_id :courseIdList){
                queryParam.put("course_id", course_id);
                Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id, this.generateRequestEntity(null, null, null, queryParam), readCourseOperation, jedis, true);//从缓存中读取课程信息
                MiscUtils.courseTranferState(currentTime, courseInfoMap);//进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                courseList.add(courseInfoMap);
            }
        }
        resultMap.put("course_list", courseList);
        return resultMap;
    }


 
    @SuppressWarnings("unchecked")
    @FunctionName("processCoursePPTs")
    public Map<String, Object> createCoursePPTs(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        List<Map<String,Object>> pptList;
        //具体的参数校验
        if(reqMap.get("ppt_list") == null){
            throw new QNLiveException("000100");
        }else {
            pptList = (List<Map<String,Object>>)reqMap.get("ppt_list");
        }
 
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
 
        //3 如果该课程信息在缓存中，则直接修改缓存中的信息，不修改数据库，每天凌晨由定时任务将该部分数据存入输入数据库
        if(jedis.exists(courseKey)){
            String status = jedis.hget(courseKey, "status");
            if(StringUtils.isBlank(status) || !"1".equals(status)){
                throw new QNLiveException("100012");
            }
 
            //判断该课程是否属于该讲师
            String lecturerId = jedis.hget(courseKey, "lecturer_id");
            if(StringUtils.isBlank(lecturerId) || !lecturerId.equals(userId)){
                throw new QNLiveException("100013");
            }
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String pptListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
            if(pptList.size() == 0){
                jedis.del(pptListKey);
            }else {
                //将PPT信息存入缓存中
                Date pptDate = new Date();
                String pptDateString = pptDate.getTime()+"";
                for(Map<String,Object> pptMap : pptList){
                    pptMap.put("image_id", MiscUtils.getUUId());
                    pptMap.put("create_time", pptDateString);
                    pptMap.put("update_time", pptDateString);
                }
                jedis.set(pptListKey, JSONObject.toJSONString(pptList));
            }
 
        }else {
            //4 如果该课程为非当天课程，则直接修改数据库
            //4.1检查课程是否存在
            Map<String,Object> courseMap = lectureModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            if(CollectionUtils.isEmpty(courseMap)){
                throw new QNLiveException("100004");
            }
 
            if(courseMap.get("status") == null || !courseMap.get("status").toString().equals("1")){
                throw new QNLiveException("100012");
            }
 
            if(courseMap.get("lecturer_id") == null || !courseMap.get("lecturer_id").toString().equals(userId)){
                throw new QNLiveException("100013");
            }
 
            //4.2先删除数据库表中的该课程所有PPT
            lectureModuleServer.deletePPTByCourseId(reqMap.get("course_id").toString());
 
            //4.3如果传递的课程列表长度大于0，则将PPT信息存入课程PPT表
            if(pptList.size() > 0){
                for(Map<String,Object> pptMap : pptList){
                    pptMap.put("image_id", MiscUtils.getUUId());
                }
                Map<String,Object> insertMap = new HashMap<>();
                insertMap.put("course_id",reqMap.get("course_id").toString());
                insertMap.put("list",pptList);
                insertMap.put("pptTime",new Date());
 
                lectureModuleServer.createCoursePPTs(insertMap);
            }
        }
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
        return resultMap;
    }

 
    @SuppressWarnings("unchecked")
    @FunctionName("courseStudents")
    public Map<String, Object> getCourseStudentList(RequestEntity reqEntity) throws Exception {
    	Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
    	Map<String, Object> resultMap = new HashMap<>();       
    	int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
    	List<Map<String,Object>> studentList = new ArrayList<>();

    	//1.先判断数据来源  数据来源：1：缓存 2.：数据库
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
    	Map<String, Object> map = new HashMap<>();
    	map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
    	String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
    	String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
    	String studentNum = null;

    	studentNum = jedis.hget(courseKey, "student_num");
    	if(StringUtils.isBlank(studentNum)){
    		Map<String,String> courseMap = CacheUtils.readCourse((String)reqMap.get("course_id"),
                    generateRequestEntity(null, null, null, reqMap), readCourseOperation, jedis, false);
    		if(MiscUtils.isEmpty(courseMap)){
    			throw new QNLiveException("100004");
    		}
    		studentNum = courseMap.get("student_num");
    	}

    	resultMap.put("student_num",studentNum);
    	Long minStudentPos = (Long)reqMap.get("student_pos");
    	boolean userStudentPos = minStudentPos!=null;
    	DecimalFormat decimalFormat = new DecimalFormat("#");

    	//1.数据来源为缓存
    	if( MiscUtils.isEmpty(reqMap.get("data_source")) || "1".equals(reqMap.get("data_source"))){
    		//1.1先查找缓存中的数据
    		String startIndex;
    		String endIndex = "-inf";
    		if(reqMap.get("student_pos") != null && StringUtils.isNotBlank(reqMap.get("student_pos").toString())){
    			startIndex ="("+reqMap.get("student_pos").toString();
    		}else {
    			startIndex = "+inf";
    		}
    		Set<String> banUserIdList = jedis.zrevrangeByScore(bandKey, startIndex, endIndex, 0, pageCount);
    		List<Map<String,Object>> banUserList;
    		List<Map<String,Object>> processBanUserList = new ArrayList<>();

    		//1.1.1如果存在禁言列表，则根据禁言列表中的用户id从数据库中查询用户相应信息
    		if(banUserIdList != null && banUserIdList.size() > 0){    			
    			Map<String, Object> paraMap = new HashMap<String, Object>();
    			paraMap.put("list", banUserIdList);
    			paraMap.put("course_id", reqMap.get("course_id").toString());
    			banUserList = lectureModuleServer.findBanUserListInfo(paraMap);
    			if(!MiscUtils.isEmpty(banUserList)){
	    			for(Map<String,Object> banMap : banUserList){					
						banMap.put("ban_status","1");
						banMap.put("data_source","1");
						processBanUserList.add(banMap);
	    			}
    			}
    			if(!CollectionUtils.isEmpty(processBanUserList)){
    				studentList.addAll(processBanUserList);
    			}
    		}
    	}

    	Map<String, Object> queryMap = new HashMap<>();
    	if(studentList.size()< pageCount){
    		Set<Tuple> allBanUserIdList = jedis.zrangeByScoreWithScores(bandKey, "-inf", "+inf");
    		List<String> banUserIdList = new LinkedList<String>();
    		if(!MiscUtils.isEmpty(allBanUserIdList)){
    			for(Tuple tuple:allBanUserIdList){
    				banUserIdList.add(tuple.getElement());
    				if(userStudentPos && minStudentPos==Long.parseLong(decimalFormat.format(tuple.getScore()))){
    					userStudentPos=false;
    				}
    			}
    			queryMap.put("all_ban_user_id_list", banUserIdList);
    		}
    		pageCount = pageCount - banUserIdList.size();
    		queryMap.put("page_count", pageCount);
    		queryMap.put("course_id", reqMap.get("course_id").toString());
    		if(userStudentPos){
    			queryMap.put("student_pos", minStudentPos);
    		}
    		List<Map<String,Object>> studentListDB = lectureModuleServer.findCourseStudentList(queryMap);

    		if(!MiscUtils.isEmpty(studentListDB)){
    			for(Map<String,Object> banMap : studentListDB){
    				banMap.put("data_source","2");
    			}
    			studentList.addAll(studentListDB);
    		}
    	}   
    	resultMap.put("student_list",studentList);
    	return resultMap;
    }
 
    @FunctionName("roomProfitList")
    public Map<String, Object> getRoomProfitList(RequestEntity reqEntity) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        String cash_in_amount = "";
        String total_amount = jedis.hget(liveRoomKey, "total_amount");
        String course_num = jedis.hget(liveRoomKey, "course_num");
        String total_student_num = jedis.hget(liveRoomKey, "total_student_num");
        if(MiscUtils.isEmpty(total_amount)){ //判斷有沒有錢
            total_amount="0";
            cash_in_amount = "0";
        }else{//如果沒有錢
            cash_in_amount = String.valueOf(CountMoneyUtil.getCashInAmount(total_amount));//可提現的錢
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("total_amount", total_amount);
        result.put("cash_in_amount", cash_in_amount);
        result.put("course_num", course_num);
        result.put("total_student_num", total_student_num);
        return result;
    }



    @SuppressWarnings("unchecked")
    @FunctionName("courseProfitList")
    public Map<String, Object> getCourseProfitList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String room_id = (String)reqMap.get("room_id");
        String course_id = (String)reqMap.get("course_id");
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        Map<String ,String> courseInfoMap = CacheUtils.readCourse(course_id,reqEntity,readCourseOperation, jedis,false);
        if(MiscUtils.isEmpty(courseInfoMap)){
            throw new QNLiveException("100013");
        }
        Map<String,Object> result = new HashMap<String,Object>();
        for(String key:courseInfoMap.keySet()){
            result.put(key, courseInfoMap.get(key));
        }

        List<Map<String,Object>> list = lectureModuleServer.findCourseProfitList(reqMap);
        result.put("profit_list", list);   
        //key:distributer_id; value:收益流水list
        final Map<String, List<Map<String,Object>>> profitMap = new HashMap<String, List<Map<String,Object>>>();
        for(Map<String,Object> profit:list){
            String distributer_id = (String)profit.get("distributer_id");
            if(profit.get("share_amount") != null){	//分销员收益不为空
                Long trueProfit = (Long)profit.get("profit_amount") - (Long)profit.get("share_amount");	//真实收益=总收益-分销收益
                profit.put("profit_amount", trueProfit);
            }
            if(!MiscUtils.isEmpty(distributer_id)){	//分销员id不为空
                List<Map<String,Object>> profitList = profitMap.get(distributer_id);
                if(profitList==null){
                    profitList = new LinkedList<Map<String,Object>>();
                    profitMap.put(distributer_id, profitList);
                }
                profitList.add(profit);
            }
        }
        if(!profitMap.isEmpty()){	//map:分销员id-收益列表                       
            ((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
                @Override
                public void batchOperation(Pipeline pipeline, Jedis jedis) {
                    Map<String, Object> keyMap = new HashMap<String, Object>();
                    Map<String, Response<String>> distributerNameMap = new HashMap<String, Response<String>>();
                    for(String key:profitMap.keySet()){	//循环分销员id
                        keyMap.clear();
                        keyMap.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, key);
                        String distributerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_DISTRIBUTER, keyMap);
                        //map:分销员id-分销员昵称
                        distributerNameMap.put(key, pipeline.hget(distributerKey, "nick_name"));
                    }
                    pipeline.sync();
                    for(String key:profitMap.keySet()){
                        List<Map<String,Object>> profitList = profitMap.get(key);	//该分销员的收益列表
                        Response<String> response = distributerNameMap.get(key);
                        if(response!=null){
                            String distributer = response.get();	//该分销员昵称
                            if(MiscUtils.isEmpty(distributer)){
                                log.warn(key + " can't get the nick name of distributer.");
                                continue;
                            }
                            for(Map<String,Object> profit : profitList){
                                profit.put("distributer", distributer);
                            }
                        } else {
                            log.warn(key + " can't get the nick name of distributer.");
                        }
                    }
                }
            });
        }
        return result;
    }
 
    @FunctionName("distributionInfo")
    public Map<String, String> getDistributionInfo(RequestEntity reqEntity) throws Exception {
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //从讲师信息中加载，数据不存在需加载缓存
        Map<String,String> keyMap = new HashMap<String,String>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);//加入老师id
        reqEntity.setParam(keyMap);
        Map<String,String> values = CacheUtils.readLecturer(userId, reqEntity, readLecturerOperation, jedis);
        return values;
    }
    
    @SuppressWarnings("unchecked")
    @FunctionName("roomDistributerInfo")
    public Map<String, Object> getRoomDistributerInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);     
        String liveRoomOwner =  CacheUtils.readLiveRoomInfoFromCached(room_id, "lecturer_id", reqEntity, readLiveRoomOperation, jedis,true);
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        
        reqMap.put("lecturer_id", userId);        
        
        
        long distributer_num = MiscUtils.convertObjectToLong(CacheUtils.readLiveRoomInfoFromCached(room_id, "distributer_num", reqEntity, readLiveRoomOperation, jedis,true));
        Map<String, Object> result = new HashMap<String,Object>();
        result.put("distributer_num", distributer_num);
        List<Map<String,Object>> list = null;
        if(distributer_num>0){
        	list = lectureModuleServer.findDistributionRoomByLectureInfo(reqMap);
        	if(!MiscUtils.isEmpty(list)){
        		final List<Map<String,Object>>  listInfo=list;
        		((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
					@Override
					public void batchOperation(Pipeline pipeline, Jedis jedis) {
		        		Map<String,Response<Map<String,String>>> roomInfo = new HashMap<String,Response<Map<String,String>>>();		        		
		        		for(Map<String,Object> values:listInfo){
		        			Map<String,Object> queryParam = new HashMap<String,Object>();
		        			queryParam.put("distributer_id", values.get("distributer_id"));
		        			queryParam.put(Constants.FIELD_ROOM_ID, room_id);							
		        			String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, queryParam);
		        			if(!roomInfo.containsKey(roomKey)){
		        				roomInfo.put(roomKey, pipeline.hgetAll(roomKey));
		        			}
		        		}
		        		pipeline.sync();
		        		for(Map<String,Object> values:listInfo){		        			
		        			Map<String,Object> queryParam = new HashMap<String,Object>();
		        			queryParam.put("distributer_id", values.get("distributer_id"));
		        			queryParam.put(Constants.FIELD_ROOM_ID, room_id);							
		        			String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, queryParam);
		        			Response<Map<String,String>> response = roomInfo.get(roomKey);
		        			if(response!=null && response.get() != null){
		        				Map<String,String> curRoomInfo = response.get();
		        				if(MiscUtils.isEqual(values.get("room_distributer_details_id"),curRoomInfo.get("room_distributer_details_id"))){
									values.put("recommend_num", curRoomInfo.get("last_recommend_num"));
									values.put("done_num", curRoomInfo.get("last_done_num"));
									values.put("total_amount", curRoomInfo.get("last_total_amount"));
		        				}
		        			}
		        		}		        		
					}        			
        		});

        		
        		Map<String,Object> query = new HashMap<String,Object>();
        		RequestEntity entity= generateRequestEntity(null, null, null, query);
        		for(Map<String,Object> values:list){
        			String distributerId = (String)values.get("distributer_id");
        			query.put("user_id", distributerId);
        			Map<String,String> userInfo = CacheUtils.readUser(distributerId, entity, readUserOperation, jedis);
        			if(!MiscUtils.isEmpty(userInfo)){
        				values.put("nick_name", userInfo.get("nick_name"));
        				values.put("avatar_address", userInfo.get("avatar_address"));
        			}
        		}
        	}
        } 
        if(list==null){
        	list = new LinkedList<Map<String,Object>>();
        }
        result.put("distributer_list", list);
        return result;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("roomDistributerCoursesInfo")
    public Map<String, Object> getRoomDistributerCoursesInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        
        String distributer_id = (String)reqMap.get("distributer_id");
        int page_count = (Integer)reqMap.get("page_count");
        Long start_time = (Long)reqMap.get("start_time");
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);
        map.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, distributer_id);
        String min_time_key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_DISTRIBUTER_COURSES_MIN_TIME, map);
        String min_date_str = jedis.get(min_time_key);
        String course_num = null;
        Long min_date = null;
        if(!MiscUtils.isEmpty(min_date_str)){
            String[] tmp = min_date_str.split(":");
            min_date = Long.parseLong(tmp[0]);
            course_num = tmp[1];
        } else {
            start_time = null;
        }
        List<String> courseList = null;
        if(start_time == null || min_date == null || start_time <= min_date){
            courseList = getDistributerCourseListFromSys(room_id, distributer_id, 
                    MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_DISTRIBUTER_COURSES, map), start_time, min_time_key, page_count,appName);
            if(courseList==null){
                courseList = new LinkedList<String>();
            }
            min_date_str = jedis.get(min_time_key);
            if(!MiscUtils.isEmpty(min_date_str)){
                course_num = min_date_str.split(":")[1];
            }
        } else {
            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_DISTRIBUTER_COURSES, map);
            Set<String> courseSet = jedis.zrangeByScore(key, "("+start_time, "+inf", 0, (int)page_count);
            courseList = new LinkedList<String>();
            if(!MiscUtils.isEmpty(courseSet)){
                for(String value:courseSet){
                    courseList.add(value);
                }
            }
        }
        
        List<Map<String,Object>>resultList = new LinkedList<Map<String,Object>>();        
        for(String valueStr:courseList){
            resultList.add(CacheUtils.convertCachedStringToMap(valueStr));
        }
        Map<String,Object> result = new HashMap<String,Object>();
        result.put("course_list", resultList);
        result.put("course_num", course_num);
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @FunctionName("createRoomShareCode")
    public Map<String, Object> createRoomShareCode(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
 
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        String type = (String)reqMap.get("type");
        if(MiscUtils.isEmpty(type)){
            type = "0";
        }
        reqMap.put("type", type);
        String room_share_code = MiscUtils.getUUId();
        Map<String,String> values = new HashMap<String,String>();
        reqMap.put("lecturer_id",userId);
        Date currentDate = new Date();        
        reqMap.put("lecturer_distribution_id", room_share_code);
        reqMap.put("create_date", currentDate);
        reqMap.put("distributer_num", 0l);
        reqMap.put("click_num", 0l);
        reqMap.put("distributer_num", 0l);
        reqMap.put("status", "0");
        reqMap.put("link_type", "0");
        lectureModuleServer.insertLecturerDistributionLink(reqMap);
        
        MiscUtils.converObjectMapToStringMap(reqMap, values);
        reqMap.clear();
        reqMap.put(Constants.CACHED_KEY_USER_ROOM_SHARE_FIELD,room_share_code);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOM_SHARE, reqMap);
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        values.put("end_date", cal.getTimeInMillis()+"");
        jedis.hmset(key, values);
        jedis.expire(key, 60*60*24*2);
        Map<String,Object> queryParam = new HashMap<String,Object>();
        queryParam.put("lecturer_id", userId);
        key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, queryParam);
        //目前只支持单直播间
        jedis.hset(key, "distribution_live_room_num" ,"1");
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
        
        queryParam.clear();
        queryParam.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_SHARE_CODES, queryParam);
        jedis.sadd(key, room_share_code);
        
        queryParam.clear();
        queryParam.put("user_id", userId);
        RequestEntity queryOperation = generateRequestEntity(null,null, null, queryParam);
        
        Map<String,String> userMap = CacheUtils.readUser(userId, queryOperation, readUserOperation, jedis);
        reqMap.clear();
        double profit_share_rate = Long.parseLong(values.get("profit_share_rate")) / 100.0;
        String room_share_url = String.format(MiscUtils.getConfigByKey("be_distributer_url_pre_fix",appName), room_share_code, room_id, profit_share_rate, values.get("effective_time"));
        reqMap.put("room_share_url", room_share_url);
        reqMap.put("avatar_address", userMap.get("avatar_address"));
        reqMap.put("nick_name", userMap.get("nick_name"));
        reqMap.put("room_name", jedis.hget(liveRoomKey, "room_name"));
        reqMap.put("profit_share_rate", values.get("profit_share_rate"));
        return reqMap;
    }
    
    @SuppressWarnings("unchecked")
    @FunctionName("createRoomDistributer")
    public void createRoomDistributer(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_USER_ROOM_SHARE_FIELD, reqMap.get("room_share_code"));
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOM_SHARE, map);        
        Map<String, String> values = jedis.hgetAll(key);

        if(MiscUtils.isEmpty(values)){
            throw new QNLiveException("100025");
        }
        
        long endDate = MiscUtils.convertObjectToLong(values.get("end_date"));
        if(System.currentTimeMillis() >= endDate){
        	throw new QNLiveException("100025");
        }
        if("1".equals(values.get("status"))){
        	throw new QNLiveException("100025");
        }
        jedis.hincrBy(key, "click_num", 1);
		
        String room_id = (String)values.get("room_id");        
        map.clear();        
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = values.get("lecturer_id");//CacheUtils.readLiveRoomInfoFromCached(room_id, "lecturer_id", reqEntity, readLiveRoomOperation, jedisUtils,true);
        if (liveRoomOwner == null) {
            throw new QNLiveException("100002");
        } else if(liveRoomOwner.equals(userId)){
            throw new QNLiveException("100026");
        }
        
        Map<String,String> distributerRoom = CacheUtils.readDistributerRoom(userId, room_id, readRoomDistributerOperation, jedis);
        if(!MiscUtils.isEmpty(distributerRoom)){
        	throw new QNLiveException("100027");
        }
        Map<String,String> query = new HashMap<String,String>();
        query.put("distributer_id", userId);
        Map<String,String> distributer = CacheUtils.readDistributer(userId,
                generateRequestEntity(null, null, null, query), readDistributerOperation, jedis, true);
        if(MiscUtils.isEmpty(distributer)){
        	values.put(Constants.SYS_INSERT_DISTRIBUTER, "1");
        }
        values.put("distributer_id", userId);
        String newRqCode = MiscUtils.getUUId();
        values.put("newRqCode",newRqCode);
        
		Map<String,String> roomInfo =  CacheUtils.readDistributerRoom(userId, room_id, readRoomDistributerOperation, jedis,true);
		if(!MiscUtils.isEmpty(roomInfo)){
			values.put("last_room_distributer_details_id", roomInfo.get("room_distributer_details_id"));
			values.put("last_recommend_num", roomInfo.get("last_recommend_num"));
			values.put("last_done_num", roomInfo.get("last_done_num"));
			values.put("last_total_amount", roomInfo.get("last_total_amount"));	
			values.put("last_course_num", roomInfo.get("last_course_num"));	
			values.put("last_done_time", roomInfo.get("done_time"));	
			values.put("last_end_date", roomInfo.get("end_date"));
			values.put("last_click_num", roomInfo.get("click_num"));
			values.put("rq_code", roomInfo.get("rq_code"));	
			values.put("new_recommend_num", roomInfo.get("recommend_num"));
			values.put("new_course_num", roomInfo.get("course_num"));
			values.put("new_done_num", roomInfo.get("done_num"));
			values.put("new_total_amount", roomInfo.get("total_amount"));
		}
        
        Map<String,Object> insertResultMap = lectureModuleServer.createRoomDistributer(values);
        query.clear();
        query.put("room_id", room_id);
        query.put("distributer_id", userId);
		String liveRoomDistributeKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, query);		
        String oldRQcode = (String)values.get("rq_code");
        query.clear();
        query.put(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE_FIELD, oldRQcode);
        String oldRQcodeKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE, query);			        
        jedis.del(liveRoomDistributeKey,oldRQcodeKey);
        
        distributerRoom = CacheUtils.readDistributerRoom(userId, room_id, readRoomDistributerOperation, jedis);
        //boolean totaldistributerAdd = MiscUtils.convertObjectToLong(distributerRoom.get("create_time")) == MiscUtils.convertObjectToLong(distributerRoom.get("update_time"));
        boolean totaldistributerAdd = true;
        if(totaldistributerAdd){
        	jedis.hincrBy(liveRoomKey, "distributer_num", 1);
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, liveRoomOwner);
            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            if(!jedis.exists(lecturerKey)){
                CacheUtils.readLecturer(userId, generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);
            }
            jedis.hincrBy(lecturerKey, "room_distributer_num", 1L);
        }        
        jedis.hincrBy(key, "distributer_num", 1);


        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, liveRoomOwner);		

        //在缓存中删除旧的RQCode，插入新的RQCode
        /*String oldRQcode = distributerRoom.get("rq_code");
        Map<String,Object> queryParam = new HashMap<String,Object>();
        queryParam.put(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE_FIELD, oldRQcode);
        String oldRQcodeKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE, queryParam);
        jedis.del(oldRQcodeKey);*/
        query.clear();
        query.put(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE_FIELD, newRqCode);
        String newRQcodeKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER_RQ_CODE, query);
        query.clear();
        query.put("distributer_id", userId);
        query.put("room_id", room_id);
        jedis.hmset(newRQcodeKey, query);

        //发送成为新分销员极光推送
        JSONObject obj = new JSONObject();
        String roomName = CacheUtils.readLiveRoomInfoFromCached(room_id, "room_name", reqEntity, readLiveRoomOperation, jedis,true);
        obj.put("body", String.format(MiscUtils.getConfigByKey("jpush_room_new_distributer",appName), MiscUtils.RecoveryEmoji(roomName)));
        obj.put("to", values.get("lecturer_id"));
        obj.put("msg_type", "9");
        Map<String,String> extrasMap = new HashMap<>();
        extrasMap.put("msg_type","9");
        extrasMap.put("course_id",room_id);
        obj.put("extras_map", extrasMap);
        JPushHelper.push(obj,appName);
    }
    
    @SuppressWarnings("unchecked")
    @FunctionName("courseStatistics")
    public Map<String, Object> getCourseStatistics(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();                
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());                
        //从讲师信息中加载，数据不存在需加载缓存
        Map<String,String> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        reqEntity.setParam(keyMap);
        Map<String,String> values = CacheUtils.readLecturer(userId, reqEntity, readLecturerOperation, jedis);
        
        String total_amount_str = values.get("total_amount");
        String course_num_str = values.get("course_num");
        String total_student_num_str = values.get("total_student_num");
        
        Map<String,Object> result = new HashMap<>();
        result.put("total_student_num", total_student_num_str);
        result.put("course_num", course_num_str);
        result.put("total_amount", total_amount_str);
        //数据统计
        List<Map<String,String>> courseList = getPlatformCourses(userId,(int)reqMap.get("page_count"),(String)reqMap.get("course_id"),appName);

        result.put("course_list", courseList);
        return result;
    }
    
    @FunctionName("fanList")
    public Map<String,Object> getFanList(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();                
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomOwner = CacheUtils.readLiveRoomInfoFromCached(room_id, "lecturer_id", reqEntity, readLiveRoomOperation, jedis,true);
        String fans_num_str = CacheUtils.readLiveRoomInfoFromCached(room_id, "fans_num", reqEntity, readLiveRoomOperation, jedis,true);
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        
        Map<String,Object> result = new HashMap<String,Object>();
        result.put("fans_num", fans_num_str);
        if(!MiscUtils.isEmpty(fans_num_str) && Long.parseLong(fans_num_str)>0){
            map.put("lecturer_id", userId);
            if(! MiscUtils.isEmpty(reqMap.get("position"))){
                map.put("position", reqMap.get("position"));
            }
            map.put("page_count", reqMap.get("page_count"));
            
            List<Map<String,Object>> list = lectureModuleServer.findRoomFanList(map);
            result.put("fan_list", list);
        }
        return result;
    }

    @FunctionName("wechatTicketNotify")
    public void wechatTicketNotify(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();

        log.debug("------微信服务号接收微信推送事件------"+reqMap);

        String appidOrTicket = (String) reqMap.get("appidOrTicket");  //微信每10分钟推送一次verifyTicket
        String type = (String) reqMap.get("type");
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象

        if (type.equals("1")) {//微信通知ticket
            //刷新component_access_token相关信息
            String expiresTimes = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "expires_in");
            JSONObject jsonObj = null;
            if (expiresTimes != null) { //已经存在accessTokenMap
                long expiresTimeStamp = Long.parseLong(expiresTimes);
                //是否快要超时 令牌是存在有效期（2小时）
                long nowTimeStamp = System.currentTimeMillis();
                if (nowTimeStamp-expiresTimeStamp > 0) {  //如果超时
                    jsonObj = WeiXinUtil.getComponentAccessToken(appidOrTicket,appName);
                }
            } else {//不存在accessTokenMap
                jsonObj = WeiXinUtil.getComponentAccessToken(appidOrTicket,appName);
            }
            if (jsonObj != null) { //需要再次刷新数据
                Object errCode = jsonObj.get("errcode");
                if (errCode != null ) {
                    log.error("获取微信AccessToken失败-----------"+jsonObj);
                } else {
                    long expiresIn = jsonObj.getLongValue("expires_in")*1000;//有效毫秒值
                    long expiresTimeStamp = System.currentTimeMillis()+expiresIn;//当前毫秒值+有效毫秒值
                    String access_token = jsonObj.getString("component_access_token");//第三方平台access_token
                    Map<String, String> accessMap = new HashMap<>();
                    accessMap.put("component_access_token", access_token);//存入redis
                    accessMap.put("expires_in", String.valueOf(expiresTimeStamp));//存入redis
                    jedis.hmset(Constants.SERVICE_NO_ACCESS_TOKEN, accessMap);
                }
            }
        } else if (type.equals("2")) {//授权成功

        } else if (type.equals("3")) {//授权更新

        } else if (type.equals("4")) {//取消授权

        }
    }

    @FunctionName("wechatAuthRedirect")
    public Map<String, Object> wechatAuthRedirect(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String auth_code = (String) reqMap.get("auth_code");
//        String expires_in = (String) reqMap.get("expires_in");

        log.debug("------微信服务号授权回调------"+reqMap);

        //根据微信回调的URL的参数去获取公众号的接口调用凭据和授权信息
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String access_token = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "component_access_token");

        JSONObject authJsonObj = WeiXinUtil.getServiceAuthInfo(access_token, auth_code,appName );
        Object errCode = authJsonObj.get("errcode");
        if (errCode != null ) {
            log.error("微信授权回调 获取服务号授权信息失败-----------"+authJsonObj);
            throw new QNLiveException("150001");
        }

        JSONObject authauthorizer_info = authJsonObj.getJSONObject("authorization_info");

        String authorizer_appid = authauthorizer_info.getString("authorizer_appid");

        //获取公众号的头像 昵称 QRCode等相关信息
        JSONObject serviceNoJsonObj = WeiXinUtil.getServiceAuthAccountInfo(access_token, authorizer_appid,appName);
        errCode = serviceNoJsonObj.get("errcode");
        if (errCode != null ) {
            log.error("微信授权回调 获取服务号相关信息失败-----------"+authJsonObj);
            throw new QNLiveException("150001");
        }

        JSONObject authauthorizer_info_base = serviceNoJsonObj.getJSONObject("authorizer_info");

        //授权方公众号类型，0代表订阅号，1代表由历史老帐号升级后的订阅号，2代表服务号
        JSONObject typeInfo = authauthorizer_info_base.getJSONObject("service_type_info");
        JSONObject verifyInfo = authauthorizer_info_base.getJSONObject("verify_type_info");

        //-1代表未认证，0代表微信认证，1代表新浪微博认证，2代表腾讯微博认证，3代表已资质认证通过但还未通过名称认证，
        // 4代表已资质认证通过、还未通过名称认证，但通过了新浪微博认证，
        // 5代表已资质认证通过、还未通过名称认证，但通过了腾讯微博认证

        Map<String,Object> result = new HashMap<String,Object>();//返回重定向的url
        if (typeInfo.getString("id").equals("2") && !verifyInfo.getString("id").equals("-1")) {

            Map<String, Object> oldServiceInfo = lectureModuleServer.findServiceNoInfoByAppid(authorizer_appid);
            //存储公众号的授权信息
            Map<String, String> authInfoMap = new HashMap<>();

            authInfoMap.put("authorizer_appid", authorizer_appid);
            authInfoMap.put("authorizer_access_token", authauthorizer_info.getString("authorizer_access_token"));
            authInfoMap.put("authorizer_refresh_token", authauthorizer_info.getString("authorizer_refresh_token"));
            long expiresIn =  authauthorizer_info.getLong("expires_in")*1000; //*1000;//有效毫秒值
            long expiresTimeStamp = System.currentTimeMillis()+expiresIn;//当前毫秒值+有效毫秒值
            authInfoMap.put("expires_time", String.valueOf(expiresTimeStamp));


            authInfoMap.put("nick_name", authauthorizer_info_base.getString("nick_name"));
            authInfoMap.put("head_img", authauthorizer_info_base.getString("head_img"));
            authInfoMap.put("service_type_info", typeInfo.getString("id"));

            String qr_code = authauthorizer_info_base.getString("qrcode_url");
            if (!MiscUtils.isEmptyString(qr_code)) {
                qr_code = QiNiuUpUtils.uploadImage(qr_code, authorizer_appid);
                authInfoMap.put("qr_code", qr_code);
            }

            //先获取部分信息 还未和直播间绑定起来
            if (!MiscUtils.isEmpty(oldServiceInfo)) { //更新授权信息 重复授权 token会失效
                authInfoMap.put("update_time", String.valueOf(System.currentTimeMillis()));
                lectureModuleServer.updateServiceNoInfo(authInfoMap);

                //更新授权信息  缓存到jedis
                Map<String,Object> query = new HashMap<String,Object>();
                query.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, oldServiceInfo.get("lecturer_id"));
                String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, query);
                jedis.hmset(serviceNoKey, authInfoMap);

            } else {  //服务号信息插入数据库
                //1 插入的时候 不更新缓存
                //2 更新的时候 更新缓存
                //3 更新的时候分两种情况 授权完成更新缓存
                //4 刷新accessToken的时候 更新缓存
                //5 目前先不缓存
                authInfoMap.put("create_time", String.valueOf(System.currentTimeMillis()));
                lectureModuleServer.insertServiceNoInfo(authInfoMap);
            }
            //重定向成功页面 然后扫码登录 绑定直播间
            result.put("redirectUrl", MiscUtils.getConfigByKey("weixin_pc_no_login_qr_url",appName).replace("APPID", authorizer_appid).replace("APPNAME", URLEncoder.encode(authauthorizer_info_base.getString("nick_name"), "utf-8")));

            log.info("绑定服务号授权成功");
        } else {
            result.put("redirectUrl", MiscUtils.getConfigByKey("service_auth_no_real_service_no",appName));

            log.info("非认证服务号 绑定失败");
        }
        return result;
    }

    @FunctionName("bindServiceNo")
    public Map<String, Object> bindServiceNo(RequestEntity reqEntity) throws Exception {

        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象

        //获取预授权码pre_auth_code 进入微信平台
        String access_token = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "component_access_token");
        JSONObject jsonObj = WeiXinUtil.getPreAuthCode(access_token,appName);

        //重定向微信URL
        Map<String,Object> result = new HashMap<String,Object>();
        Object errCode = jsonObj.get("errcode");
        if (errCode != null ) {
            throw new QNLiveException("150001");
        } else {
            String pre_auth_code = jsonObj.getString("pre_auth_code");//预授权码 有效期为20分
            result.put("redirectUrl", WeiXinUtil.getServiceAuthUrl(pre_auth_code,appName));
        }
        return result;
    }

    @FunctionName("bindingRoom")
    public void bindingRoom(RequestEntity reqEntity) throws Exception {

        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();

        //用户id就是讲师id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appid = (String) reqMap.get("appid");

        //1 进行关联 更新数据库
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("authorizer_appid", appid);
        paramMap.put("lecturer_id", userId);
        int count = lectureModuleServer.updateServiceNoLecturerId(paramMap);
        if (count < 1) {
            throw new QNLiveException("150001");
        }

        //先取出数据库的信息
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> authInfoMap = lectureModuleServer.findServiceNoInfoByAppid(appid);
        Map<String, String> authInfo = new HashMap<>();

        MiscUtils.converObjectMapToStringMap(authInfoMap, authInfo);
        authInfo.remove("lecturer_id");

        //缓存授权信息到jedis
        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, userId);
        String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, query);
        jedis.hmset(serviceNoKey, authInfo);
    }

    /**
     * 查询课程
     * @param lecturerId 讲师id
     * @param pageCount 分页数
     * @param course_id 课程id
     * @param courceStatus 课程状态
     * @param queryTime 时间
     * @param postion  位置
     * @return
     */
    private List<Map<String,String>> getCourseList(String lecturerId,int pageCount,String course_id,int courceStatus,Long queryTime,Long postion, boolean preDesc, boolean finDesc,String appName) throws Exception {
        Map<String,Object> map = new HashMap<String,Object>();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String,String> queryParam = new HashMap<String,String>();
        queryParam.put("course_id", course_id);
        Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id,
                this.generateRequestEntity(null, null, null, queryParam), readCourseOperation, jedis, true);//从缓存中读取课程信息

        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);//预告或直播
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);//结束
        String lecturerCoursesDelKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_DEL, map);//删除
        List<Map<String,String>> courseList = new ArrayList<>();
        if(courceStatus == 1 || courceStatus == 4){//预告和正在直播
            courseList.addAll(getCourseOnlyFromCached(jedis, lecturerCoursesPredictionKey, Long.valueOf(courseInfoMap.get("start_time").toString()), null, pageCount, preDesc));//查询分页
            if(!MiscUtils.isEmpty(courseList)){
                pageCount=pageCount-courseList.size();//分页数
                long currentTime = System.currentTimeMillis();
                for(Map<String,String> courseInfo : courseList){
                    MiscUtils.courseTranferState(currentTime, courseInfo);//把预告状态修改成正在直播状态
                }
            }
        }

        if(pageCount > 0 || courceStatus == 2){ //结束
            courseList.addAll(getCourseOnlyFromCached(jedis, lecturerCoursesFinishKey, queryTime, postion,pageCount, finDesc));
            if(!MiscUtils.isEmpty(courseList)) {
                pageCount = pageCount - courseList.size();//分页数
            }
        }

        if(pageCount > 0 || courceStatus == 5){ //删除
            courseList.addAll(getCourseOnlyFromCached(jedis, lecturerCoursesDelKey, queryTime, postion,pageCount, finDesc));

        }
        return courseList;
    }

    /**
     * 获取课程列表
     * 关键redis
     *  SYS:COURSES:PREDICTION    平台的预告中课程列表  Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION
     * SYS:COURSES:FINISH  平台的已结束课程列表   Constants.CACHED_KEY_PLATFORM_COURSE_FINISH
     * @param userId 正在查询的用户
     * @param pageCount 需要几个对象
     * @param courseId 课程id  查找第一页的时候不传 进行分页 必传
     */
    @SuppressWarnings({ "unchecked"})
    private   List<Map<String,String>> getPlatformCourses(String userId,int pageCount,String courseId,String appName) throws Exception{
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        long currentTime = System.currentTimeMillis();//当前时间
        int offset = 0;//偏移值
        Set<String> courseIdSet;//查询的课程idset
        List<String> courseIdList = new ArrayList<>();//课程id列表
        List<Map<String,String>> courseList = new LinkedList<>();//课程对象列表
        Map<String, Object> resultMap = new HashMap<String, Object>();//最后返回的结果对象
        int courceStatus = 0;
        if(courseId == null || courseId.equals("")){
            courceStatus = 4;
        }else{
            Map<String,String> param = new HashMap<String,String>();
            param.put("course_id", courseId);
            Map<String, String> courseMap = CacheUtils.readCourse(courseId, this.generateRequestEntity(null, null, null, param), readCourseOperation, jedis, true);//从缓存中读取课程信息
            courceStatus = Integer.valueOf(courseMap.get("status"));
        }
        //判断传过来的课程状态
        //<editor-fold desc="获取课程idList">
        if(courceStatus == 1 || courceStatus == 4){//如果预告或者是正在直播的课程
            String startIndex ;//坐标起始位
            String endIndex ;//坐标结束位
            String getCourseIdKey;
            //平台的预告中课程列表 预告和正在直播放在一起  按照直播开始时间顺序排序  根据分类获取不同的缓存
            Map<String,Object> map = new HashMap<String,Object>();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
            String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);//预告或直播
            if(courseId == null || courseId.equals("")){//如果没有传入courceid 那么就是最开始的查询
                startIndex = "-inf";//设置起始位置
                endIndex = "+inf";//设置结束位置
            }else{//传了courseid
                Map<String,String> param = new HashMap<String,String>();
                param.put("course_id", courseId);
                Map<String, String> courseMap = CacheUtils.readCourse(courseId, this.generateRequestEntity(null, null, null, param), readCourseOperation, jedis, true);//从缓存中读取课程信息
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(courseMap.get("start_time")),  MiscUtils.convertObjectToLong(courseMap.get("position")));//拿到当前课程在redis中的score
                startIndex = "("+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
                endIndex = "+inf";//设置结束位置
            }
            courseIdSet = jedis.zrangeByScore(lecturerCoursesPredictionKey,startIndex,endIndex,offset,pageCount); //顺序找出couseid  (正在直播或者预告的)
            for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                courseIdList.add(course_id);
            }
            pageCount =  pageCount - courseIdList.size();//用展示数量减去获取的数量  查看是否获取到了足够的课程数
            if( pageCount > 0){//如果返回的值不够
                courseId = null;//把课程id设置为null  用来在下面的代码中进行判断
                courceStatus = 2;//设置查询课程状态 为结束课程 因为查找出来的正在直播和预告的课程不够数量
            }
        }
        //=========================下面的缓存使用另外一种方式获取====================================
        if(courceStatus == 2 ){//查询结束课程
            boolean key = true;//作为开关 用于下面是否需要接着执行方法
            long startIndex = 0; //开始下标
            long endIndex = -1;   //结束下标
            //平台的已结束课程列表
            Map<String,Object> map = new HashMap<String,Object>();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
            String getCourseIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);

            long endCourseSum = jedis.zcard(getCourseIdKey);//获取总共有多少个结束课程
            if(courseId == null){//如果课程ID没有 那么就从最近结束的课程找起
                endIndex = -1;
                startIndex = endCourseSum - pageCount;//利用总数减去我这边需要获取的数
                if(startIndex < 0){
                    startIndex = 0;
                }
            }else{ //如果有课程id  先获取课程id 在列表中的位置 然后进行获取其他课程id
                long endRank = jedis.zrank(getCourseIdKey, courseId);
                endIndex = endRank - 1;
                if(endIndex >= 0){
                    startIndex = endIndex - pageCount + 1;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                }else{
                    key = false;//因为已经查到最后的课程没有必要往下查了
                }
            }
            if(key){
                courseIdSet = jedis.zrange(getCourseIdKey, startIndex, endIndex);
                List<String> transfer = new ArrayList<>();
                for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                    transfer.add(course_id);
                }
                Collections.reverse(transfer);
                courseIdList.addAll(transfer);
            }
            pageCount =  pageCount - courseIdList.size();//用展示数量减去获取的数量  查看是否获取到了足够的课程数
            if( pageCount > 0){//如果返回的值不够
                courseId = null;//把课程id设置为null  用来在下面的代码中进行判断
                courceStatus = 5;//设置查询课程状态 为结束课程 因为查找出来的正在直播和预告的课程不够数量
            }
        }

        if( courceStatus == 5){//查询结束课程
            boolean key = true;//作为开关 用于下面是否需要接着执行方法
            long startIndex = 0; //开始下标
            long endIndex = -1;   //结束下标
            //平台的已结束课程列表
            Map<String,Object> map = new HashMap<String,Object>();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
            String getCourseIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_DEL, map);

            long endCourseSum = jedis.zcard(getCourseIdKey);//获取总共有多少个结束课程
            if(courseId == null){//如果课程ID没有 那么就从最近结束的课程找起
                endIndex = -1;
                startIndex = endCourseSum - pageCount;//利用总数减去我这边需要获取的数
                if(startIndex < 0){
                    startIndex = 0;
                }
            }else{ //如果有课程id  先获取课程id 在列表中的位置 然后进行获取其他课程id
                long endRank = jedis.zrank(getCourseIdKey, courseId);
                endIndex = endRank - 1;
                if(endIndex >= 0){
                    startIndex = endIndex - pageCount + 1;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                }else{
                    key = false;//因为已经查到最后的课程没有必要往下查了
                }
            }
            if(key){
                courseIdSet = jedis.zrange(getCourseIdKey, startIndex, endIndex);
                List<String> transfer = new ArrayList<>();
                for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                    transfer.add(course_id);
                }
                Collections.reverse(transfer);
                courseIdList.addAll(transfer);
            }
        }
        //</editor-fold>

        //====================================================上面是获取课程id集合======================================================================
        //====================================================下面是针对用户 等其他操作=================================================================
        //<editor-fold desc="根据课程id 获取课程对象并判断当前用户是否有加入课程">
        if(courseIdList.size() > 0){
            Map<String,String> queryParam = new HashMap<String,String>();
            Map<String,Object> query = new HashMap<String,Object>();
            query.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);//用来查询当前用户加入了那些课程
            for(String course_id : courseIdList){
                queryParam.put("course_id", course_id);
                Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id, this.generateRequestEntity(null, null, null, queryParam), readCourseOperation, jedis, true);//从缓存中读取课程信息
                MiscUtils.courseTranferState(currentTime, courseInfoMap);//进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                if(!MiscUtils.isEmpty(jedis.zrank(key, course_id))){//判断当前用户是否有加入这个课程
                    courseInfoMap.put("student", "Y");
                } else {
                    courseInfoMap.put("student", "N");
                }
                courseList.add(courseInfoMap);
            }
        }
        //</editor-fold>
        return courseList;
    }


    private List<String> getDistributerCourseListFromSys(String room_id, String distributer_id,String distributeCourseKey,
            final Long start_time,String min_time_key, final int page_count,String appName){
        final Map<String, Object> query = new HashMap<String,Object>();
        query.put(Constants.FIELD_ROOM_ID, room_id);
        Jedis jedis = this.jedisUtils.getJedis(appName);
        if(start_time != null){
            query.put("start_time", new Date(start_time));
        } else {
            jedis.del(min_time_key);
            jedis.del(distributeCourseKey);
        }        
        query.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, distributer_id);
        query.put("limit_count", Constants.MAX_QUERY_LIMIT);
        final List<String> result = new LinkedList<String>();
        
        ((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
            @Override
            public void batchOperation(Pipeline pipeline, Jedis jedis) {
                String course_num = jedis.get(min_time_key);
                if(MiscUtils.isEmpty(course_num)){                    
                    Map<String,Object> distributeInfo = lectureModuleServer.findDistributerInfo(query);
                    if(!MiscUtils.isEmpty(distributeInfo)){                        
                        course_num= String.valueOf(distributeInfo.get("course_num"));
                    }
                    if(MiscUtils.isEmpty(course_num) || "0".equals(course_num)){
                        pipeline.set(min_time_key,0+":"+0);
                        pipeline.expire(min_time_key, 60*60);
                        return;
                    }
                } else {
                    course_num=course_num.split(":")[1];
                }
                List<Map<String,Object>> list = lectureModuleServer.findRoomDistributerCourseInfo(query);
                Date start_time = null;
                for(Map<String,Object> value : list){
                    String valueStr = CacheUtils.convertMaptoCachedString(value);
                    start_time = (Date)value.get("start_time");
                    pipeline.zadd(distributeCourseKey, start_time.getTime(), valueStr);
                    result.add(valueStr);                    
                }
                if(start_time!=null){
                    pipeline.set(min_time_key,start_time.getTime()+":"+course_num);
                    pipeline.expire(min_time_key, 60*60);
                    pipeline.expire(distributeCourseKey, 60*60);
                }                                
                pipeline.sync();                
            }
        });
        return result;
    }



    private List<Map<String,String>> getCourseOnlyFromCached(Jedis jedis, String key, Long startTime, Long postion, int pageCount, boolean desc){
        Set<Tuple> courseList = null;
        String startIndex = null;
        String endIndex = null;
        if(desc){
            if (MiscUtils.isEmpty(startTime)) {
                startIndex = "+inf";
            } else {
            	if(postion==null){
            		startIndex = "(" + startTime;
            	} else {
            		startIndex = "(" + MiscUtils.convertInfoToPostion(startTime, postion);
            	}
            }
            endIndex = "-inf";
            courseList = jedis.zrevrangeByScoreWithScores(key, startIndex, endIndex, 0, pageCount);
        } else {
            if (MiscUtils.isEmpty(startTime)) {
                startIndex = "-inf";
            } else {
            	if(postion==null){
            		startIndex = "(" + startTime;
            	} else {
            		startIndex = "(" + MiscUtils.convertInfoToPostion(startTime, postion);
            	}
            }
            endIndex = "+inf";
            courseList = jedis.zrangeByScoreWithScores(key, startIndex, endIndex, 0, pageCount);
        }        
        List<String> list = new LinkedList<String>();
        for (Tuple tuple : courseList) {            
        	list.add(tuple.getElement());
        }
        return CacheUtils.readCourseListInfoOnlyFromCached(jedis, list,readCourseOperation);
    }

    private List<Map<String,String>> getCourseOnlyFromCached(Jedis jedis, String key,String courceid,  int pageCount, boolean desc){
        long startIndex = 0; //开始下标
        long endIndex = -1;   //结束下标
        Map<String,Object> map = new HashMap<String,Object>();
        long endCourseSum = jedis.zcard(key);//获取总共有多少个课程
        if( courceid !=null && jedis.zrank(key, courceid) != null){//如果有
            long endRank = jedis.zrank(key, courceid);
            endIndex = endRank - 1;
            if(endIndex >= 0){
                startIndex = endIndex - pageCount + 1;
                if(startIndex < 0){
                    startIndex = 0;
                }
            }
        }else{//如果课程ID没有 那么就从最近结束的课程找起
            endIndex = -1;
            startIndex = endCourseSum - pageCount;//利用总数减去我这边需要获取的数
            if (startIndex < 0) {
                startIndex = 0;
            }

        }
        Set<String> courseIdSet = jedis.zrange(key, startIndex, endIndex);
        List<String> list = new ArrayList<>();
        for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
            list.add(course_id);
        }
        return CacheUtils.readCourseListInfoOnlyFromCached(jedis, list,readCourseOperation);
    }


	@SuppressWarnings("unchecked")
    @FunctionName("getCustomerService")
    public Map<String, Object> getCustomerService(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Map<String,Object> map = new HashMap<>();
        map.put("appName",appName);
        Map<String,Object> retMap = new HashMap<>();

        map.put("config_key","customerQrCodeUrl");
        Map<String,Object> customerQrCodeUrl = lectureModuleServer.findCustomerServiceBySystemConfig(map);//获取客服二维码url
        retMap.put("customerQrCodeUrl",customerQrCodeUrl.get("config_value"));

        map.put("config_key","customerPhoneNum");
        Map<String,Object> customerPhoneNum = lectureModuleServer.findCustomerServiceBySystemConfig(map);//获取客服电话
        retMap.put("customerPhoneNum",customerPhoneNum.get("config_value"));

        map.put("config_key","customerTitle");
        Map<String,Object> customerTitle = lectureModuleServer.findCustomerServiceBySystemConfig(map);//标题
        retMap.put("customerTitle",customerTitle.get("config_value"));

        map.put("config_key","customerHint");
        Map<String,Object> cystomerHint = lectureModuleServer.findCustomerServiceBySystemConfig(map);//客服二维码提示
        retMap.put("customerHint",cystomerHint.get("config_value"));
        return retMap;
    }


    /**
     * 效验手机验证码
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("verifyVerificationCode")
    public  Map<String, Object>  verifyVerificationCode (RequestEntity reqEntity) throws Exception {
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        Map<String,String> map = (Map<String, String>) reqEntity.getParam();
        String verification_code = map.get("verification_code");//验证码
        Map<String,String> phoneMap = new HashMap();
        phoneMap.put("user_id",userId);
        phoneMap.put("code",verification_code);
        MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, phoneMap);
        String codeKey =  MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_CODE, phoneMap);//根据userId 拿到 key
        if(!jedis.exists(codeKey)){
            throw new QNLiveException("130009");
        }
        String code = jedis.get(codeKey);//拿到值
        if(appName.equals(Constants.HEADER_APP_NAME)){//如果是qnlive就进行判断
            if(!code.equals(verification_code)){//进行判断
                throw new QNLiveException("130002");
            }
        }

        String phoneKey =  MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_PHONE, phoneMap);//根据userId 拿到 key
        String phone = jedis.get(phoneKey);//拿到电话
        if(!appName.equals(Constants.HEADER_APP_NAME)){//不是qnlive
            boolean key = DjSendMsg.checkVerificationCode(phone, code, verification_code,jedis);
            if(!key){
                throw new QNLiveException("130002");
            }
        }

        //判断当前用户是否有直播间
        Map<String, Object> roomMap = new HashMap<>();
        roomMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, roomMap);

        //更新用户缓存
        Map<String, String> keyMap = new HashMap<String, String>();
        keyMap.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, keyMap);
        map.put("user_id",userId);

        if (jedis.exists(lectureLiveRoomKey)) {//有直播间
            Map<String,String> userInfo = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);//查找当前用户是否有直播间
            if(MiscUtils.isEmpty(userInfo.get("phone_number"))){//如果没有
                updateUserPhone(phone,userId);
                jedis.del(key);
                CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
                Map<String, Object> param = new HashMap<String, Object>();
                param.put("query_type", "2");
               if(map.get("room_id") == null){
                   throw new QNLiveException("000100");//没有roomid 报错
               }
                param.put("room_id", map.get("room_id"));
                reqEntity.setParam(param);
                return queryLiveRoomDetail(reqEntity);
            }else{//有
                throw new QNLiveException("130004");//直播间已经有手机号
            }
        }else{//创建直播间
            updateUserPhone(phone,userId);
            jedis.del(key);
            CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
            return createLiveRoom(reqEntity);
        }
    }


    public void updateUserPhone(String phone,String userid){
        Map<String,Object> map = new HashMap<>();
        map.put("user_id",userid);
        map.put("phone_number",phone);
        lectureModuleServer.updateUser(map);
        lectureModuleServer.updateLoginInfo(map);
    }




    /**
     * TODO 有复用的方法 以后重构是需要进行河滨
     * 获取讲师二维码/青柠二维码
     */
    public Map getQrCode(String lectureId,Jedis jedis,String appName) {
        Map<String, String> query = new HashMap();
        query.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, lectureId);
        //1.判断讲师是否有公众号 有就直接返回
        String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, query);
        if (jedis.exists(serviceNoKey)) {//判断当前是否有这个缓存
            query.put("qr_code_url",jedis.hgetAll(serviceNoKey).get("qr_code"));
            query.put("qr_code_title", MiscUtils.getConfigByKey("weixin_qr_code_title_lecturer",appName));
            query.put("qr_code_message", MiscUtils.getConfigByKey("weixin_qr_code_message",appName));
            query.put("qr_code_title", MiscUtils.getConfigByKey("weixin_qr_code_ad",appName));
            return query;
        }
        return null;
    }

    /**
     * 删除课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("delCourse")
    public Map<String, Object> delCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Date now = new Date();
        String course_id = reqMap.get("course_id").toString();
        Map<String,String> queryParam = new HashMap<String,String>();
        queryParam.put("course_id", course_id);
        Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id, this.generateRequestEntity(null, null, null, queryParam), readCourseOperation, jedis, true);//从缓存中读取课程信息
        String mGroupId = courseInfoMap.get("im_course_id");//获取课程imid
        if(! userId.equals(courseInfoMap.get("lecturer_id"))){//判断当前用户是否是 课程的创建人
            throw new QNLiveException("160003");
        }
        MiscUtils.courseTranferState(new Date().getTime(), courseInfoMap);//进行课程转换
        //<editor-fold desc="IM推送,删除相关缓存">
        if(courseInfoMap.get("status").equals("2") || courseInfoMap.get("status").equals("1")){
            if(Long.valueOf(courseInfoMap.get("course_amount")) > 0 || Long.valueOf(courseInfoMap.get("extra_amount")) > 0){//判断当前课程是否产生收益
                throw new QNLiveException("160002");
            }else{//没有产生金钱方面的数据
                Map<String,Object> course = new HashMap<String,Object>();
                course.put("course_id", course_id);
                course.put("status", "5");
                course.put("update_time", now);
                String coursekey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, course);//获取课程在缓存中的key
                jedis.hset(coursekey,"status","5");//把课程缓存中的状态改为已删除

                Map<String,Object> map = new HashMap<String,Object>();
                map.put(Constants.CACHED_KEY_CLASSIFY,courseInfoMap.get(Constants.CACHED_KEY_CLASSIFY));//获取课程分类

                if(courseInfoMap.get("status").equals("2")){ //结束
                    jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH,course_id);//删除平台结束课程
                    jedis.zrem(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH,map),course_id);//删除分类信息
                    map.clear();
                    map.put(Constants.CACHED_KEY_LECTURER_FIELD,courseInfoMap.get("lecturer_id"));
                    jedis.zrem(MiscUtils.getKeyOfCachedData( Constants.CACHED_KEY_COURSE_FINISH,map),course_id);//老师结束课程
                    jedis.zrem(Constants.SYS_COURSES_RECOMMEND_FINISH,course_id);//删除热门推荐


                }else{//预告
                    jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION,course_id);//删除平台预告课程
                    jedis.zrem(Constants.SYS_COURSES_RECOMMEND_PREDICTION,course_id);//删除热门推荐
                    jedis.zrem(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION,map),course_id);//删除分类预告信息
                    map.clear();
                    map.put(Constants.CACHED_KEY_LECTURER_FIELD,courseInfoMap.get("lecturer_id"));
                    jedis.zrem(MiscUtils.getKeyOfCachedData( Constants.CACHED_KEY_COURSE_PREDICTION,map),course_id);//老师预告课程
                    //1.进行强制结束
                    course.put("end_time", now);//结束时间
                }
                lectureModuleServer.updateCourse(course);//修改数据库数据
                //把删除课程存入缓存中用于统计
                String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, queryParam);
                long  position = MiscUtils.convertObjectToLong(jedis.hget(courseKey, "position"));
                jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_DEL, MiscUtils.convertInfoToPostion( now.getTime(),position), course_id);//把删除的课程存入缓存用于统计
                jedis.zadd(MiscUtils.getKeyOfCachedData( Constants.CACHED_KEY_COURSE_DEL,map), MiscUtils.convertInfoToPostion( now.getTime(),position), course_id);//把课程加入老师删除课程缓存中



                //发送删除课程消息 通知前端强制踢人
                String sender = "system";
                Map<String,Object> infomation = new HashMap<>();
                infomation.put("course_id", reqMap.get("course_id").toString());
                infomation.put("creator_id", userId);
                infomation.put("message", MiscUtils.getConfigByKey("del_cource_message",appName));
                infomation.put("message_type", "1");
                infomation.put("send_type", "8");//8.删除课程踢人
                infomation.put("message_id",MiscUtils.getUUId());
                infomation.put("message_imid",infomation.get("message_id"));
                infomation.put("create_time", System.currentTimeMillis());
                Map<String,Object> messageMap = new HashMap<>();
                messageMap.put("msg_type","1");
                messageMap.put("app_name",appName);
                messageMap.put("send_time", System.currentTimeMillis());
                messageMap.put("create_time", System.currentTimeMillis());
                messageMap.put("information",infomation);
                messageMap.put("mid",infomation.get("message_id"));
                String content = JSON.toJSONString(messageMap);
                IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);

                //获取当前课程的学生
                List<Map<String, Object>> courseAllStudentList = lectureModuleServer.findCourseAllStudentList(course_id);

                //<editor-fold desc="微信推送">
                if(!MiscUtils.isEmpty(courseAllStudentList)){//如果有学生
                    Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();//模板信息
                    TemplateData first = new TemplateData();//针对微信模板{{first.DATA}}
                    first.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    first.setValue(MiscUtils.getConfigByKey("wpush_delete_course_first",appName));
                    templateMap.put("first", first);

                    TemplateData keyword1 = new TemplateData();//针对微信模板 课程名称：{{keyword1.DATA}}
                    keyword1.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    keyword1.setValue(MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")));//课程名称
                    templateMap.put("keyword1", keyword1);

                    TemplateData keyword2 = new TemplateData();
                    keyword2.setColor(Constants.WE_CHAT_PUSH_COLOR);
                    keyword2.setValue(MiscUtils.getConfigByKey("wpush_delete_course_keyword2",appName));
                    templateMap.put("keyword2", keyword2);


                    TemplateData remark = new TemplateData();
                    if(appName.equals(Constants.HEADER_APP_NAME)){
                        remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
                    }else{
                        remark.setColor(Constants.WE_CHAT_PUSH_COLOR_DLIVE);
                    }
                    remark.setValue(MiscUtils.getConfigByKey("wpush_delete_course_remark",appName));
                    templateMap.put("remark", remark);

                    Map<String, Object> wxPushParam = new HashMap<>();
                    wxPushParam.put("templateParam", templateMap);//模板消息
                    wxPushParam.put("course", courseInfoMap);//课程
                    wxPushParam.put("followers", courseAllStudentList);//直播间关注者
                    wxPushParam.put("pushType", "2");//1创建课程 2更新课程
                    RequestEntity mqRequestEntity = new RequestEntity();
                    mqRequestEntity.setServerName("MessagePushServer");
                    mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);//异步进行处理
                    mqRequestEntity.setFunctionName("noticeCourseToFollower");
                    mqRequestEntity.setParam(wxPushParam);
                    mqRequestEntity.setAppName(appName);
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
                //</editor-fold>

                //删除学生表中的这个课程的所有学生记录
                Map<String, Object> studentMap = new HashMap<>();
                studentMap.put("courseAllStudentList", courseAllStudentList);//模板消息
                RequestEntity mqRequestEntity = new RequestEntity();
                mqRequestEntity.setServerName("MessagePushServer");
                mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);//异步进行处理
                mqRequestEntity.setFunctionName("delUserAddCourseList");
                mqRequestEntity.setParam(studentMap);
                mqRequestEntity.setAppName(appName);
                this.mqUtils.sendMessage(mqRequestEntity);


                RequestEntity timerRequestEntity = generateRequestEntity("MessagePushServer",Constants.MQ_METHOD_ASYNCHRONIZED,"processCourseNotStartCancelAll",reqEntity.getParam());
                timerRequestEntity.setAppName(appName);
                this.mqUtils.sendMessage(timerRequestEntity);



            }
        }else{
            throw new QNLiveException("160001");
        }
        //</editor-fold>
        reqMap.clear();
        return reqMap;
    }


    /**
     * 创建系列
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("createSeries")
    public Map<String, Object> createSeries(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
            String appName = reqEntity.getAppName();
        reqMap.put("appName",appName);
        Jedis jedis = jedisUtils.getJedis(appName);

        String query_type = reqMap.get("query_type").toString();//查詢類型 0 app 1 saas
        String user_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//获取userid
        String updown = reqMap.get("updown").toString();//上下架

        if(!updown.equals("1") && !updown.equals("2")){
            reqMap.put("updown","2");
        }
        if(MiscUtils.isEmpty(reqMap.get("classify_id"))){
            reqMap.put("classify_id",Constants.COURSE_DEFAULT_CLASSINFY);
        }
        if(query_type.equals("0")){
            //1.判断直播间是否属于当前讲师
            String liveRoomOwner = CacheUtils.readLiveRoomInfoFromCached((String)reqMap.get("room_id"), "lecturer_id", reqEntity, readLiveRoomOperation, jedis, true);
            if (!liveRoomOwner.equals(user_id)) {
                throw new QNLiveException("100002");
            }
        }else{
            Map<String,String> shopInfo = CacheUtils.readShop((String)reqMap.get("shop_id"), reqEntity, readShopOperation,jedis);
            if(MiscUtils.isEmpty(shopInfo) || !shopInfo.get("user_id").equals(user_id)){
                throw new QNLiveException("190001");
            }
        }

        reqMap.put("user_id", user_id);
        reqMap.put("series_status", "0");
        //2创建系列封面
        if(reqMap.get("series_img") == null || StringUtils.isBlank(reqMap.get("series_img").toString())){
            String default_course_cover_url_original = MiscUtils.getConfigByKey("default_course_cover_url",appName);
            JSONArray default_course_cover_url_array = JSON.parseArray(default_course_cover_url_original);
            int randomNum = MiscUtils.getRandomIntNum(0, default_course_cover_url_array.size() - 1);
            reqMap.put("series_img", default_course_cover_url_array.get(randomNum));
        }
        //3创建系列db
        Map<String, Object> dbSeries = lectureModuleServer.createSeries(reqMap);

        //4把系列加入缓存
        Map<String, String> series = CacheUtils.readSeries((String)dbSeries.get("series_id"),
                        generateRequestEntity(null, null, null, dbSeries), readSeriesOperation, jedis, true);
        String series_id = series.get("series_id").toString();

        //5 修改相关缓存
        Map<String, Object> map = new HashMap<String, Object>();
        if(query_type.equals("0")){
            //5.1修改讲师个人信息缓存中的直播系列数 讲师直播间信息SYS: room:{room_id}
            map.put(Constants.FIELD_ROOM_ID, (String)reqMap.get("room_id"));
            String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
            jedis.hincrBy(liveRoomKey, "live_series_num", 1);
            //5.2 修改讲师直播间信息中的直播系列数  讲师个人信息SYS: lecturer:{lecturer_id}
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, user_id);
            String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            jedis.hincrBy(lectureKey, "live_serise_num", 1);
        }

        long lpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(series.get("position")));//根据最近更新课程时间和系列的排序
        if(updown.equals("1")){ //上架
            setSeriesRedis(series_id,user_id,jedis);

        }else{ //下架
            //将系列id 加入讲师下架列表
            String lectureSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_DOWN, map);
            jedis.zadd(lectureSeriesKey, lpos, series_id);
            map.put("series_course_type",reqMap.get("series_course_type"));
            String lectureSeriesCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_DOWN, map);
            jedis.zadd(lectureSeriesCourseKey, lpos, series_id);
        }
        return dbSeries ;
    }


    private void setSeriesRedis(String series_id,String lecturer_id,Jedis jedis){
        Map<String,Object> map = new HashMap<>();
        map.put("series_id",series_id);
        map.put("lecturer_id",lecturer_id);
        String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
        Map<String, String> series = jedis.hgetAll(seriesKey);
        long lpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(series.get("position")));//根据最近更新课程时间和系列的排序

        //1.将课程加入讲师 系列上架列表
        String lectureSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_UP, map);
        long seriesScore = Long.parseLong(series.get("update_course_time"));
        seriesScore = MiscUtils.convertLongByDesc(seriesScore);	//实现指定时间越大，返回值越小
        jedis.zadd(lectureSeriesKey, seriesScore,series_id );

        //课程内容分类
        map.put("series_course_type",series.get("series_course_type"));
        String lectureSeriesCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_UP, map);
        jedis.zrem(lectureSeriesCourseKey,series_id);
        jedis.zadd(lectureSeriesCourseKey, lpos, series_id);
        if(Constants.DEFAULT_SERIES_COURSE_TYPE.equals(series.get("series_course_type").toString())){
            //4.将课程上架到平台
            jedis.zrem(Constants.CACHED_KEY_PLATFORM_SERIES_APP_PLATFORM,series_id);
            jedis.zadd(Constants.CACHED_KEY_PLATFORM_SERIES_APP_PLATFORM, lpos, series_id);
        }
    }

    /**
     * 编辑系列
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("updateSeries")
    public Map<String, Object> updateSeries(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        String user_id =  AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String series_id = reqMap.get("series_id").toString();//分类id
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String, String> keyMap = new HashMap<String, String>();
        keyMap.put(Constants.CACHED_KEY_SERIES_FIELD, series_id);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, keyMap);
        Map<String, String> series = CacheUtils.readSeries(series_id, generateRequestEntity(null, null, null, keyMap), readSeriesOperation, jedis, true);
        String lecturer_id = series.get("lecturer_id");
        if(!lecturer_id.equals(user_id)){
            throw new QNLiveException("210001");
        }
        Map<String, Object> resultMap = lectureModuleServer.updateSeries(reqMap);
        jedis.del(key);//删除现在的缓存 进行更新
        Map<String, String> seriesInfoMap = CacheUtils.readSeries(series_id, generateRequestEntity(null, null, null, reqMap), readSeriesOperation, jedis, true);
        if(seriesInfoMap.get("shelves_sharing").equals("1")){
            Map<String,String> headerParams = new HashMap<>();
            headerParams.put("version", Constants.SYS_QN_SHARING_VERSION);
            headerParams.put("Content-Type",Constants.SYS_QN_SHARING_CONTENT_TYPE);
            headerParams.put("access_token",reqEntity.getAccessToken());

            Map<String,Object> requestMap = new HashMap<>();
            requestMap.put("course_id",seriesInfoMap.get("series_id"));
            requestMap.put("course_title",seriesInfoMap.get("series_title"));
            requestMap.put("course_url",seriesInfoMap.get("series_img"));
            if(seriesInfoMap.get("series_course_type").equals("1")){
                requestMap.put("course_type","0");
            }else if(seriesInfoMap.get("series_course_type").equals("2")){
                requestMap.put("course_type","1");
            }

            requestMap.put("update_type","2");
            requestMap.put("update_status",seriesInfoMap.get("series_status"));
            requestMap.put("updated_course_num",seriesInfoMap.get("course_num"));
            requestMap.put("classify_id",seriesInfoMap.get("classify_id"));
            requestMap.put("cycle",seriesInfoMap.get("update_plan"));
            requestMap.put("course_remark",seriesInfoMap.get("series_remark"));
            requestMap.put("course_price",seriesInfoMap.get("series_price"));
            requestMap.put("buy_tips",seriesInfoMap.get("series_pay_remark"));
            requestMap.put("cycle",seriesInfoMap.get("update_plan"));
            requestMap.put("target_user",seriesInfoMap.get("target_user"));

            String getUrl = MiscUtils.getConfigByKey("sharing_api_url", Constants.HEADER_APP_NAME)
                    +SharingConstants.SHARING_SERVER_COURSE
                    +SharingConstants.SHARING_COURSE_SYNCHRONIZATION_SERIES_ADD;
            String result = HttpClientUtil.doPostUrl(getUrl, headerParams, requestMap, "UTF-8");
            Map<String, Object> resMap = JSON.parseObject(result, new TypeReference<Map<String, Object>>() {});
            resultMap.put("synchronization",resMap);
        }
        return resultMap ;
    }

    /**
     * 上下架
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("updown")
    public Map<String, Object> updown(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String,Object> result = new HashMap<>();
        Date now = new Date();
        String user_id =  AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String updown = reqMap.get("updown").toString();//上下架
        String query_type = reqMap.get("query_type").toString();//查询类型 0 单品课程 1系列  2系列里的课程
        String query_from = reqMap.get("query_from").toString();//来源 0 app  1 :店铺
        String updown_id = reqMap.get("updown_id").toString();//要进行操作的系列id或者课程id
        Map<String,Object> updownMap = new HashMap<>();
        updownMap.put("update_time",now);
        updownMap.put("query_from",query_from);

        if(query_type.equals("1")){//系列
            //<editor-fold desc="系列">
            updownMap.put("series_id",updown_id);
            String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, updownMap);
            //判断当前系列是这个讲师的吗
            String lecturer_id = jedis.hget(seriesKey, "lecturer_id");
            if(!lecturer_id.equals(user_id)){
                throw new QNLiveException("210001");
            }
            updownMap.put("updown",updown);
            result = lectureModuleServer.updateUpdown(updownMap);

            jedis.del(seriesKey);
            Map<String, String> seriesMap = CacheUtils.readSeries(updown_id, generateRequestEntity(null, null, null, updownMap), readSeriesOperation, jedis, true);
            long seriesLpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(seriesMap.get("position")));//根据最近更新课程时间和系列的排序
            String series_id = seriesMap.get("series_id");
            String lecturerSeriesUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_UP, seriesMap);//讲师所有上架系列
            String lecturerSeriesDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_DOWN, seriesMap);//讲师所有下架系列
            String seriesCourseTypeDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_UP, seriesMap);//讲师在不同的课程内容下架的系列
            String seriesCourseTypeUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_DOWN, seriesMap);//讲师在不同的课程内容上架的系列

            if(updown.equals("1")){//往上架加入
            	/*
            	 * 维护讲师所有上架系列，根据子课最新的更新时间进行排序
            	 */
            	//获取子课程更新时间
            	long seriesScore = Long.parseLong(seriesMap.get("update_course_time"));
            	seriesScore = MiscUtils.convertLongByDesc(seriesScore);	//实现指定时间越大，返回值越小
                jedis.zadd(lecturerSeriesUpKey, seriesScore,series_id );
                
                jedis.zadd(seriesCourseTypeUpKey, seriesLpos, series_id);
                jedis.zrem(lecturerSeriesDownKey,series_id);
                jedis.zrem(seriesCourseTypeDownKey,series_id);
                if(seriesMap.get("series_course_type").equals("0")){
                    long lpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(seriesMap.get("position")));
                    jedis.zadd(Constants.CACHED_KEY_PLATFORM_SERIES_APP_PLATFORM,lpos,series_id);
                }
            }else{//往下架加入
                jedis.zrem(lecturerSeriesUpKey,series_id);
                jedis.zrem(seriesCourseTypeUpKey,series_id);
                jedis.zadd(lecturerSeriesDownKey, seriesLpos, series_id);
                jedis.zadd(seriesCourseTypeDownKey, seriesLpos, series_id);
                if(seriesMap.get("series_course_type").equals("0")){
                    jedis.zrem(Constants.CACHED_KEY_PLATFORM_SERIES_APP_PLATFORM,series_id);
                }
            }
            //</editor-fold>
        }else{
            //<editor-fold desc="课程">
            String courseId = updown_id;
            updownMap.put("course_id",courseId);
            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, updownMap);
            //判断当前系列是这个讲师的吗
            //String lecturer_id = jedis.hget(courseKey, "lecturer_id");
            Map<String, String> course = null;
            if(query_from.equals("0")){
                course  = CacheUtils.readCourse(courseId,
                        generateRequestEntity(null, null, null, updownMap), readCourseOperation, jedis, true);
            }else{
                course  = CacheUtils.readCourse(courseId,
                        generateRequestEntity(null, null, Constants.SYS_READ_SAAS_COURSE, updownMap), readCourseOperation, jedis, true);
            }
            String lecturer_id = course.get("lecturer_id");
            if(!lecturer_id.equals(user_id)){
                throw new QNLiveException("210001");
            }
            if(query_type.equals("0")){
                updownMap.put("course_updown",updown);
            }else{
                if(MiscUtils.isEmpty(jedis.hget(courseKey, "series_id"))){
                    throw new QNLiveException("210003");
                }
                updownMap.put("series_course_updown",updown);

            }
            result = lectureModuleServer.updateUpdown(updownMap);//更改数据库
            jedis.del(courseKey);
            Map<String,String> courseMap = null;
            if(query_from.equals("0")){
                courseMap  = CacheUtils.readCourse(courseId,
                        generateRequestEntity(null, null, null, updownMap), readCourseOperation, jedis, true);
            }else{
                courseMap  = CacheUtils.readCourse(courseId,
                        generateRequestEntity(null, null, Constants.SYS_READ_SAAS_COURSE, updownMap), readCourseOperation, jedis, true);
            }
            if(query_type.equals("0")){//单品
                //<editor-fold desc="单品">
                if(query_from.equals("0")){ //判断来源
                    //<editor-fold desc="直播课">
                    MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);
                    String predictionKey = "";
                    long lpos = 0L;
                    String classifyCourseKey = "";
                    String courseList = "";
                    String remmendList = "";
                    if(courseMap.get("status").equals("1") || courseMap.get("status").equals("4") ){ //预告或正在直播
                        predictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, courseMap);
                        lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(courseMap.get("start_time")) , MiscUtils.convertObjectToLong(courseMap.get("position")));
                        //4.5 将课程插入到平台课程列表 预告课程列表 SYS:courses:prediction
                        courseList = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
                        classifyCourseKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, courseMap);
                        if(courseMap.get("status").equals("1")){
                            remmendList = Constants.SYS_COURSES_RECOMMEND_PREDICTION;
                        }else{
                            remmendList = Constants.SYS_COURSES_RECOMMEND_LIVE;
                        }
                    }else{//结束
                        courseList = Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;
                        classifyCourseKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, courseMap);
                        predictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, courseMap);
                        lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(courseMap.get("end_time")) , MiscUtils.convertObjectToLong(courseMap.get("position")));
                        remmendList = Constants.SYS_COURSES_RECOMMEND_FINISH;
                    }
                    String lectureCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_DOWN, courseMap);
                    if(updown.equals("1")){//上架
                        jedis.zrem(lectureCourseKey,courseId);
                        jedis.zadd(courseList, lpos, courseId);
                        jedis.zadd(predictionKey, lpos, courseId);
                        jedis.zadd(remmendList,Long.valueOf(courseMap.get("student_num")) +Long.valueOf(courseMap.get("extra_num")) , courseId);//热门推荐 预告
                        jedis.zadd(classifyCourseKey, lpos, courseId);
                    }else{//下架
                        jedis.zrem(courseList,courseId);
                        jedis.zrem(predictionKey,courseId);
                        jedis.zrem(remmendList,courseId);
                        jedis.zrem(classifyCourseKey,courseId);
                        //加入讲师下架课程列表
                        jedis.zadd(lectureCourseKey, MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(courseMap.get("position"))), courseId);
                    }
                    //</editor-fold>
                }else{
                    //<editor-fold desc="其他课">
                    Map<String, Object> keyMap = new HashMap<>();
                    //该讲师所有上架的单品ID
                    String lecturerSingleSetUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_COURSES_NOT_LIVE_UP, courseMap);
                    String lecturerSingleSetDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_COURSES_NOT_LIVE_DOWN, courseMap);

                    if(updown.equals("1")){//上架
                        jedis.zadd(lecturerSingleSetUpKey,System.currentTimeMillis(),courseId);
                        jedis.zrem(lecturerSingleSetDownKey,courseId);
                    }else{//下架
                        jedis.zadd(lecturerSingleSetDownKey,System.currentTimeMillis(),courseId);
                        jedis.zrem(lecturerSingleSetUpKey,courseId);
                    }
                    //</editor-fold>
                }
                //</editor-fold>
            }else{//系列课
                //<editor-fold desc="系列课">
                String series_id = courseMap.get("series_id").toString();
                Map<String,Object> map = new HashMap<>();
                map.put("series_id",series_id);
                String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
                long courselops = MiscUtils.convertInfoToPostion(System.currentTimeMillis() , MiscUtils.convertObjectToLong(courseMap.get("position")));
                String seriesCourseDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_DOWN, map);
                String seriesCourseUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_UP, map);
                if(updown.equals("1")){//往上架加入
                    jedis.zadd(seriesCourseUpKey, courselops, courseId);
                    jedis.zrem(seriesCourseDownKey,courseId);
                    Map<String, Object> dbResultMap = lectureModuleServer.increaseSeriesCourse(series_id);
                    /*
                     * 更新讲师所有上架系列课缓存
                     */
                    Map<String, Object> readSeriesMap = new HashMap<>();
                    readSeriesMap.put("series_id", series_id);
                    RequestEntity readSeriesReqEntity = this.generateRequestEntity(null, null, "findSeriesBySeriesId", readSeriesMap);
                    Map<String, String> seriesMap = CacheUtils.readSeries(series_id, readSeriesReqEntity, readSeriesOperation, jedis, true);

                    if(seriesMap.get("updown").equals("1")){
                        String seriesCourseTypeUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_UP, seriesMap);
                        long seriesLpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(seriesMap.get("position")));//根据最近更新课程时间和系列的排序
                        jedis.zadd(seriesCourseTypeUpKey, seriesLpos, series_id);

                        String lecturerSeriesUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_UP, seriesMap);//讲师所有上架系列
                        long score = ((Date)dbResultMap.get("update_time")).getTime();
                        score = MiscUtils.convertLongByDesc(score);	//获取新的排序分值
                        jedis.zadd(lecturerSeriesUpKey, score, series_id);
                    }
                }else{//往下架加入
                    jedis.zrem(seriesCourseUpKey,courseId);
                    jedis.zadd(seriesCourseDownKey, courselops, courseId);
                    lectureModuleServer.delSeriesCourse(series_id);
                }
                jedis.del(seriesKey);
                //获取系列课程详情
                CacheUtils.readSeries(series_id,generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
                //</editor-fold>
            }
            //</editor-fold>
        }
        return result ;
    }


    /**
     * 移进移出
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("updateSeriesCourse")
    public Map<String, Object> updateSeriesCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        String user_id =  AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> resltMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis(appName);
        String query_type = reqMap.get("query_type").toString();//查询类型 0加入系列 1移除系列
        String course_id = reqMap.get("course_id").toString();
        Map<String,Object> map = new HashMap<>();
        map.put("course_id",course_id);
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        Map<String, String> courseInfo = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);//获取课程信息
        if(MiscUtils.isEmpty(courseInfo)){
            courseInfo = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, Constants.SYS_READ_SAAS_COURSE, map), readCourseOperation, jedis, true);//获取课程信息
        }
        if(!courseInfo.get("lecturer_id").equals(user_id)){//课程不是这个用户的
            throw new QNLiveException("100013");
        }
        jedis.del(courseKey);
        Map<String,Object> course = new HashMap<>();
        course.put("course_id",course_id);
        if(query_type.equals("1")){//移出系列
            map.clear();
            String oldSeriesId =courseInfo.get("series_id");
            map.put("series_id",oldSeriesId);
            lectureModuleServer.delSeriesCourse(oldSeriesId);
            String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
            jedis.del(seriesKey);
            //获取系列课程详情
            Map<String, String> oldSeriesInfo = CacheUtils.readSeries(oldSeriesId, generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
            String series_course_updown = courseInfo.get("series_course_updown");
            String course_updown =courseInfo.get("course_updown");
            String series_course_type =oldSeriesInfo.get( "series_course_type");
            course.put("series_course_updown","0");
            course.put("series_id","");
            if(courseInfo.get("course_updown").equals("0")){
                if(series_course_type.equals("0")) {
                    String lectureCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_DOWN, courseInfo);
                    jedis.zadd(lectureCourseKey, MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(courseInfo.get("position"))), course_id);
                }else{
                    String courses_not_live_down =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_COURSES_NOT_LIVE_DOWN, courseInfo);
                    jedis.zadd(courses_not_live_down, System.currentTimeMillis(), course_id);
                }
                course.put("course_updown","2");
            }

            if(series_course_type.equals("0")){
                resltMap = lectureModuleServer.updateSeriesCourse(course);
            }else{
                resltMap = lectureModuleServer.updateSaasSeriesCourse(course);
            }
            //删除系列缓存中的课程
            if(series_course_updown.equals("1")){
                String lecturerSeriesCourseUp = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_UP, map);
                jedis.zrem(lecturerSeriesCourseUp,course_id);

            }else if(series_course_updown.equals("2")){
                String lecturerSeriesCourseDown = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_DOWN, map);
                jedis.zrem(lecturerSeriesCourseDown,course_id);
            }
            map.clear();
            map.put("course_id", course_id);

            courseInfo = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);//获取课程信息
            if(MiscUtils.isEmpty(courseInfo)){
                courseInfo =  CacheUtils.readCourse(course_id, generateRequestEntity(null, null, Constants.SYS_READ_SAAS_COURSE, map), readCourseOperation, jedis, true);//获取课程信息
            }

            if(oldSeriesInfo.get("shelves_sharing").equals("1")){
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("version", "1.2.0");
                headerMap.put("Content-Type", "application/json;charset=UTF-8");
                headerMap.put("access_token",reqEntity.getAccessToken() );
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("s_course_id",course_id);

                String getUrl = MiscUtils.getConfigByKey("sharing_api_url", Constants.HEADER_APP_NAME)/*"http://192.168.1.197:8088"*/
                        +SharingConstants.SHARING_SERVER_COURSE
                        +SharingConstants.SHARING_COURSE_SYNCHRONIZATION_SERIES_CHILD_REMOVE;
                String result = HttpClientUtil.doPostUrl(getUrl, headerMap, courseMap, "UTF-8");
                Map<String, String> resMap = JSON.parseObject(result, new TypeReference<Map<String, String>>() {});
                resltMap.put("synchronization",resMap);
            }

        }else if(query_type.equals("0")){//加入系列
            if(MiscUtils.isEmpty(reqMap.get("series_id"))){
                throw new QNLiveException("000004");
            }
            String series_id = reqMap.get("series_id").toString();
            map.put("series_id",series_id);
            Map<String, Object> stringObjectMap = lectureModuleServer.increaseSeriesCourse(series_id);
            String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
            jedis.del(seriesKey);
            //获取系列课程详情
            Map<String, String> seriesMap = CacheUtils.readSeries(series_id, generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
            String series_course_type = seriesMap.get("series_course_type");
            map.clear();
            map.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD,user_id);
            if(!jedis.hget(seriesKey,"lecturer_id").equals(user_id)){
                throw new QNLiveException("210001");
            }else{
                course.put("series_id",series_id);
            }
            courseInfo = jedis.hgetAll(courseKey);//判断之前是否有加入系列
            if(!MiscUtils.isEmpty(courseInfo.get("series_id"))){
                throw new QNLiveException("210003");
            }
            course.put("series_course_updown","1");
            if(series_course_type.equals("0")){
                resltMap = lectureModuleServer.updateSeriesCourse(course);
            }else{
                resltMap = lectureModuleServer.updateSaasSeriesCourse(course);
            }
            map.clear();
            map.put("series_id",reqMap.get("series_id"));
            String lectureSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_UP, map);
            long seriesLpos = MiscUtils.convertInfoToPostion(System.currentTimeMillis() , MiscUtils.convertObjectToLong(course.get("position")));
            jedis.zadd(lectureSeriesKey, seriesLpos, course_id);

            map.put("course_id",course_id);
            courseInfo = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);//获取课程信息
            if(MiscUtils.isEmpty(courseInfo)){
                courseInfo = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, Constants.SYS_READ_SAAS_COURSE, map), readCourseOperation, jedis, true);//获取课程信息
            }


            if(seriesMap.get("shelves_sharing").equals("1")){
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("version", "1.2.0");
                headerMap.put("Content-Type", "application/json;charset=UTF-8");
                headerMap.put("access_token",reqEntity.getAccessToken() );

                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("course_id",series_id);
                courseMap.put("s_course_id",courseInfo.get("course_id"));
                courseMap.put("series_id",courseInfo.get("series_id"));
                courseMap.put("course_title",courseInfo.get("course_title"));
                courseMap.put("course_url",courseInfo.get("course_image"));
                courseMap.put("course_duration",courseInfo.get("course_duration"));
                courseMap.put("course_remark",MiscUtils.isEmpty(courseInfo.get("course_remark")));
                courseMap.put("file_path",courseInfo.get("course_url"));
                if(courseInfo.get("series_course_updown").equals("1")){
                    courseMap.put("status",0);
                }else if(courseInfo.get("series_course_updown").equals("2")){
                    courseMap.put("status",1);
                }
                String getUrl = MiscUtils.getConfigByKey("sharing_api_url", Constants.HEADER_APP_NAME)/*"http://192.168.1.197:8088"*/
                        +SharingConstants.SHARING_SERVER_COURSE
                        +SharingConstants.SHARING_COURSE_SYNCHRONIZATION_SERIES_CHILD;
                String result = HttpClientUtil.doPostUrl(getUrl, headerMap, courseMap, "UTF-8");
                Map<String, String> resMap = JSON.parseObject(result, new TypeReference<Map<String, String>>() {});
                resltMap.put("synchronization",resMap);
            }
        }else {
            throw new QNLiveException("000004");
        }
        return resltMap ;
    }


    /**
     * 设置单品
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("updateSeriesCourseLonely")
    public Map<String, Object> updateSeriesCourseLonely(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        String user_id =  AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> resltMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis(appName);
        String course_id = reqMap.get("course_id").toString();
        String updown = reqMap.get("course_updown").toString();
        //判断 当前课程是否是这个讲师的
        Map<String,Object> map = new HashMap<>();
        map.put("course_id",course_id);
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        Map<String, String> courseInfo = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);//获取课程信息
        if(MiscUtils.isEmpty(courseInfo)){
            courseInfo = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, Constants.SYS_READ_SAAS_COURSE, map), readCourseOperation, jedis, true);//获取课程信息
        }
        if(!courseInfo.get("lecturer_id").equals(user_id)){//课程不是这个用户的
            throw new QNLiveException("100013");
        }
        resltMap = lectureModuleServer.updateCourseLonely(reqMap);
        jedis.del(courseKey);

        Map<String,Object> query = new HashMap<>();
        query.put("course_id", course_id);
        Map<String, String> courseMap = new HashMap<>();
        if(reqMap.get("series_course_type").toString().equals("0")){
             MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);
            //<editor-fold desc="直播课">
            String predictionKey = "";
            long lpos = 0L;
            String classifyCourseKey = "";
            String courseList = "";
            String remmendList = "";
            if(courseMap.get("status").equals("1") || courseMap.get("status").equals("4") ){ //预告或正在直播
                predictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, courseMap);
                lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(courseMap.get("start_time")) , MiscUtils.convertObjectToLong(courseMap.get("position")));
                //4.5 将课程插入到平台课程列表 预告课程列表 SYS:courses:prediction
                courseList = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
                classifyCourseKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, courseMap);
                if(courseMap.get("status").equals("1")){
                    remmendList = Constants.SYS_COURSES_RECOMMEND_PREDICTION;
                }else{
                    remmendList = Constants.SYS_COURSES_RECOMMEND_LIVE;
                }
            }else{//结束
                courseList = Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;
                classifyCourseKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, courseMap);
                predictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, courseMap);
                lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(courseMap.get("end_time")) , MiscUtils.convertObjectToLong(courseMap.get("position")));
                remmendList = Constants.SYS_COURSES_RECOMMEND_FINISH;
            }


            String lectureCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_DOWN, courseMap);
            if(updown.equals("1")){//上架
                if(reqMap.get("series_course_type").equals("0")){
                    jedis.zrem(lectureCourseKey,course_id);
                }
                jedis.zadd(courseList, lpos, course_id);
                jedis.zadd(predictionKey, lpos, course_id);
                jedis.zadd(remmendList,Long.valueOf(courseMap.get("student_num")) +Long.valueOf(courseMap.get("extra_num")) , course_id);//热门推荐 预告
                jedis.zadd(classifyCourseKey, lpos, course_id);
            }else{//下架
                jedis.zrem(courseList,course_id);
                jedis.zrem(predictionKey,course_id);
                jedis.zrem(remmendList,course_id);
                jedis.zrem(classifyCourseKey,course_id);
                if(reqMap.get("series_course_type").equals("0")){
                    //加入讲师下架课程列表
                    jedis.zadd(lectureCourseKey, MiscUtils.convertInfoToPostion(System.currentTimeMillis(), MiscUtils.convertObjectToLong(courseMap.get("position"))), course_id);
                }
            }
            //</editor-fold>
        }else{
            String courses_not_live_down =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_COURSES_NOT_LIVE_DOWN, courseInfo);//zset 讲师所有下架单品（直播课除外） (course_id,下架时间)
            String courses_not_live_up =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_COURSES_NOT_LIVE_UP, courseInfo);//zset 讲师所有上架单品（直播课除外） (course_id,上架时间)
            if(updown.equals("1")){//上架
                jedis.zrem(courses_not_live_down,course_id);
                jedis.zadd(courses_not_live_up, System.currentTimeMillis(), course_id);
            }else{//下架
                jedis.zadd(courses_not_live_down, System.currentTimeMillis(), course_id);
                jedis.zrem(courses_not_live_up,course_id);
            }
        }
        return resltMap;
    }
    
    /**
     * 分页获取系列课的收益列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("findSeriesIncomeList")
    public Map<String, Object> findSeriesIncomeList(RequestEntity reqEntity) throws Exception {
    	//返回结果集
    	Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //获取缓存jedis
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        //获取请求的系列id
        String seriesId = reqMap.get("series_id").toString();
        //获取前端上一次更新时间
        long lastUpdateTime = (long) reqMap.get("last_update_time");
        //获取已加载的数量
        long readedCount = (long) reqMap.get("readed_count");
        //获取每页数量
        long pageCount = (long) reqMap.get("page_count");
        Date now = new Date();
        
        /*
         * 从缓存中获取店铺信息
         */
/*        Map<String, String> shopMap = CacheUtils.readShop(shopId, reqEntity, readShopOperation, jedis);
        if(shopMap == null || shopMap.isEmpty()){
        	logger.error("saas店铺-获取店铺单品直播课程列表>>>>请求的店铺不存在");
        	throw new QNLiveException("190001");
        }
        //获取讲师id
        String lecturerId = shopMap.get("user_id");
 */       
        /*
         * 从缓存中获取系列课的详情
         */
        Map<String, String> seriesInfoMap = CacheUtils.readSeries(seriesId, reqEntity, readSeriesOperation, jedis, true);
        if(seriesInfoMap == null || seriesInfoMap.isEmpty()){
        	log.error("分页获取系列课的收益列表>>>>系列课程不存在");
        	throw new QNLiveException("210003");
        }
        
        /*
         * 从数据库查询系列课收益列表
         */
        if(readedCount == 0 && lastUpdateTime == 0){
        	//前端请求第一页数据
        	reqMap.put("create_time", now);	//用于sql进行条件查询：create_time <= now
        	//返回给前端当前服务器时间
        	resultMap.put("last_update_time", now);
        }else{
        	reqMap.put("create_time", new Date(lastUpdateTime));	//用于sql进行条件查询：create_time <= lastUpdateTime
        	//返回给前端原来传递的时间
        	resultMap.put("last_update_time", lastUpdateTime);
        }
        
        List<Map<String, Object>> profitInfoList = lectureModuleServer.findSeriesProfitListByMap(reqMap);
        
        /*
         * 从数据库中获取系列课收入统计（门票收入合计，总收入）
         * 下标为0表示门票总收入
         * 下标为1表示总收入
         */
        List<Map<String, Object>> seriesProfitStatistics = lectureModuleServer.findSeriesProfitStatistics(reqMap);
        if(seriesProfitStatistics != null && !seriesProfitStatistics.isEmpty()){
        	seriesInfoMap.put("ticket_amount", String.valueOf(seriesProfitStatistics.get(0).get("amount")));
        	seriesInfoMap.put("totle_amount", String.valueOf(seriesProfitStatistics.get(1).get("amount")));
        }
        resultMap.put("series_info", seriesInfoMap);
        resultMap.put("profit_info_list", profitInfoList);
        return resultMap;
    
    }


    /**
     * 获取讲师单个课程
     *  1、显示当天内将要开始的直播
     2、如果直播已经开始，而接下去没有其他直播，那么就一直显示这个直播，知道直播结束
     3、如果上一个直播已经开始，有下一个直播，那么在下一个直播的前30分钟，开始显示下一个直播
     4、如果当前没有直播，并且今天接下来的时间也没有直播，那么就显示空状态“今天暂无直播，您可以点击上面“新增课程”，以创建直播和系列”
     */
    @SuppressWarnings("unchecked")
    @FunctionName("getSingleLecturerLiveCourse")
    public Map<String, Object> getSingleLecturerLiveCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String, Object> resultMap = new HashMap<>();
        String lecturer_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String,Object> map = new HashMap<>();
        map.put("lecturer_id",lecturer_id);
        String lecturerCourseAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, map);//讲师预告和直播课程list key

        String startIndex ;//坐标起始位
        String endIndex ;//坐标结束位
        Map<String, String> courseMap = new HashMap<>();
        Long now = System.currentTimeMillis();
        boolean iskey = true;
        //直播的
        long courseScoreByRedis = MiscUtils.convertInfoToPostion( now,0L);
        String courseId = "";
        //预告
        startIndex ="("+courseScoreByRedis;//设置结束位置
        endIndex = "+inf";//设置起始位置 '(' 是要求大于这个参数
        Set<String> previewCourseIdSet = jedis.zrangeByScore(lecturerCourseAllKey,startIndex,endIndex,0,1);//找出最靠近当前时间的预告直播课程
        if(!MiscUtils.isEmpty(previewCourseIdSet)){
            for(String course_id : previewCourseIdSet){
                courseId = course_id;
            }
            map.clear();
            map.put("course_id",courseId);
            courseMap = CacheUtils.readCourse(courseId, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);
            Long start_time = Long.valueOf(courseMap.get("start_time"));
            long min30 = 30L * 60L * 1000L;
            if( now > ( start_time- min30)){//创建时间小于 当前时间减去30分钟
                resultMap.putAll(courseMap);
                iskey = false;
            }
        }
        if(iskey){
            startIndex =courseScoreByRedis+"";//设置结束位置
            endIndex = "-inf";//
            Set<String> liveCourseIdSet = jedis.zrevrangeByScore(lecturerCourseAllKey,startIndex,endIndex,0,1);//找出最靠近当前时间的正在直播课程
            if(!MiscUtils.isEmpty(liveCourseIdSet)) {
                for (String course_id : liveCourseIdSet) {
                    courseId = course_id;
                }
                map.clear();
                map.put("course_id", courseId);
                courseMap = CacheUtils.readCourse(courseId, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);
            }
        }
        if(!MiscUtils.isEmpty(courseMap)){
            MiscUtils.courseTranferState(now, courseMap);
            if(courseMap.get("status").toString().equals("4")){
                resultMap.putAll(courseMap);
            }
        }
        return resultMap ;
    }


}
