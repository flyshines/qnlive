package qingning.lecture.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
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

import java.net.URLEncoder;
import java.text.DecimalFormat;
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
	private ReadDistributerOperation readDistributerOperation;

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
        }
    }

    @SuppressWarnings("unchecked")
    @FunctionName("createLiveRoom")
    public Map<String, Object> createLiveRoom(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = MiscUtils.getUUId();
        reqMap.put("room_address", MiscUtils.getConfigByKey("live_room_share_url_pre_fix") + room_id);
        reqMap.put("room_id", room_id);
        //0.目前校验每个讲师仅能创建一个直播间
        //1.缓存中读取直播间信息
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);

        Jedis jedis = jedisUtils.getJedis();
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
        Map<String,String> userInfo = CacheUtils.readUser(userId, queryOperation, readUserOperation, jedisUtils);
        if(MiscUtils.isEmpty(reqMap.get("avatar_address"))){
        	reqMap.put("avatar_address",userInfo.get("avatar_address"));
        }
        if(MiscUtils.isEmpty(reqMap.get("room_name"))){        	
        	reqMap.put("room_name", String.format(MiscUtils.getConfigByKey("room.default.name"), userInfo.get("nick_name")));
        }
        Map<String, Object> createResultMap = lectureModuleServer.createLiveRoom(reqMap);

        //3.缓存修改
        //如果是刚刚成为讲师，则增加讲师信息缓存，并且修改access_token缓存中的身份
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, reqMap.get("user_id").toString());
        String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        if (isLecturer == false) {
        	queryOperation = this.generateRequestEntity(null,null, null, map);        	
        	CacheUtils.readLecturer(userId, queryOperation, readLecturerOperation, jedisUtils);
        	//Map<String, String> lectureStringMap = new HashMap<String,String>();
            //lectureStringMap.put("nick_name",(String)userInfo.get("nick_name"));
            //lectureStringMap.put("avatar_address",(String)userInfo.get("avatar_address"));
            //3.1新增讲师缓存
            //jedis.hmset(lectureKey, lectureStringMap);

            //3.2修改access_token中的缓存
            jedis.hset(accessTokenKey, "user_role", user_role+","+Constants.USER_ROLE_LECTURER);
            
            //3.3增加讲师直播间信息缓存
            jedis.sadd(Constants.CACHED_LECTURER_KEY, userId);
            
            map.clear();
            map.put("room_id", room_id);
            CacheUtils.readLiveRoom(room_id, this.generateRequestEntity(null, null, null, map), readLiveRoomOperation, jedisUtils, true,true);
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
        Jedis jedis = jedisUtils.getJedis();
        String liveRoomOwner = CacheUtils.readLiveRoomInfoFromCached((String)reqMap.get("room_id"), "lecturer_id",
                reqEntity, readLiveRoomOperation, jedisUtils, true);
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
 
        //2.检测更新时间是否与系统一致
        String liveRoomUpdateTime = CacheUtils.readLiveRoomInfoFromCached((String)reqMap.get("room_id"), "update_time",
                reqEntity, readLiveRoomOperation, jedisUtils, true);
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
        resultMap.put("qr_code",getQrCode(userId,jedis));
        return resultMap;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("liveRoom")
    public Map<String, Object> queryLiveRoomDetail(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        resultMap.put("binding_service_url",lectureModuleServer.findCustomerServiceBySystemConfig("bindingServiceUrl").get("config_value"));
        resultMap.put("qr_code",getQrCode(userId,jedis));
        String queryType = reqMap.get("query_type").toString();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String liveRoomListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
        
        RequestEntity request = new RequestEntity();                   
        request.setParam(map); 
        Map<String,String> lectureInfo = CacheUtils.readLecturer(userId, request, readLecturerOperation, jedisUtils);
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
                        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(roomIdCache, reqEntity, readLiveRoomOperation, jedisUtils, true);
                        Map<String,Object> peocessLiveRoomMap;
 
                        if(! CollectionUtils.isEmpty(liveRoomMap)){
                        	String roomId = liveRoomMap.get("room_id");
                        	String lectureId = liveRoomMap.get("lecturer_id");
                        	Map<String,Object> query = new HashMap<String,Object>();
                        	query.put("room_id", roomId);
                        	query.put("room_id", lectureId);
                        	RequestEntity entity = this.generateRequestEntity(null, null, Constants.SYS_READ_LAST_COURSE, query);
                        	Map<String,String> courseInfo = CacheUtils.readLastCourseOfTheRoom(roomId, lectureId, entity, readCourseOperation, jedisUtils);
                        	
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
                return resultMap;
            }
 
 
        }else {
            //查询单个直播间的详细信息
            if(reqMap.get("room_id") == null || StringUtils.isBlank(reqMap.get("room_id").toString())){
                throw new QNLiveException("000100");
            }
 
            Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(reqMap.get("room_id").toString(), reqEntity, readLiveRoomOperation, jedisUtils, true);
            if(CollectionUtils.isEmpty(liveRoomMap)){
                throw new QNLiveException("100002");
            }
        	String roomId = liveRoomMap.get("room_id");
        	String lectureId = liveRoomMap.get("lecturer_id");
        	Map<String,Object> query = new HashMap<String,Object>();
        	query.put("room_id", roomId);
        	query.put("lecturer_id", lectureId);
        	RequestEntity entity = this.generateRequestEntity(null, null, Constants.SYS_READ_LAST_COURSE, query);
        	Map<String,String> courseInfo = CacheUtils.readLastCourseOfTheRoom(roomId, lectureId, entity, readCourseOperation, jedisUtils);
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
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        String liveRoomOwner = CacheUtils.readLiveRoomInfoFromCached((String)reqMap.get("room_id"), "lecturer_id",
                reqEntity, readLiveRoomOperation, jedisUtils, true);
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        //课程之间需要间隔三十分钟
        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, query);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        cal.add(Calendar.MINUTE, -3*Constants.COURSE_MAX_INTERVAL);
        long preStartTime = cal.getTimeInMillis();
        cal.setTimeInMillis(startTime);
        cal.add(Calendar.MINUTE, 3*Constants.COURSE_MAX_INTERVAL);
        long nextStartTime = cal.getTimeInMillis();
        Set<Tuple> courseList = jedis.zrangeByScoreWithScores(lecturerCoursesPredictionKey, preStartTime+"", nextStartTime+"", 0, 1);
        if(!MiscUtils.isEmpty(courseList)){
            throw new QNLiveException("100029");
        }
        /*
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, query);
        courseList = jedis.zrangeByScoreWithScores(lecturerCoursesFinishKey, preStartTime+"", nextStartTime+"", 0, 1);
        if(!MiscUtils.isEmpty(courseList)){
            throw new QNLiveException("100029");
        }
        */
        //2.将课程信息插入到数据库
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
            //TODO  暂时不处理
        }
 
        if(reqMap.get("course_url") == null || StringUtils.isBlank(reqMap.get("course_url").toString())){
            String default_course_cover_url_original = MiscUtils.getConfigByKey("default_course_cover_url");
            JSONArray default_course_cover_url_array = JSON.parseArray(default_course_cover_url_original);
            int randomNum = MiscUtils.getRandomIntNum(0, default_course_cover_url_array.size() - 1);
            reqMap.put("course_url", default_course_cover_url_array.get(randomNum));
        }
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
        		generateRequestEntity(null, null, null, dbResultMap), readCourseOperation, jedisUtils, true);
        //4.4 将课程插入到 我的课程列表
        //预告课程列表 SYS: lecturer:{lecturer_id}courses:prediction
        String courseId = (String)course.get("course_id");
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String predictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        double pos = MiscUtils.convertObjectToDouble(course.get("start_time"));
        jedis.zadd(predictionKey, pos, courseId);
        long lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("start_time")) , MiscUtils.convertObjectToLong(course.get("position")));
        //4.5 将课程插入到平台课程列表 预告课程列表 SYS:courses:prediction
        String platformCourseList = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
        jedis.zadd(platformCourseList, lpos, courseId);
 
        resultMap.put("course_id", courseId);
        
        Map<String,Object> timerMap = new HashMap<>();
        timerMap.put("course_id", courseId);
        //timerMap.put("start_time", new Date(startTime));
        timerMap.put("lecturer_id", userId);
        timerMap.put("course_title", course.get("course_title"));        
        //timerMap.put("course_id", dbResultMap.get("course_id").toString());
        timerMap.put("start_time", startTime + "");
        timerMap.put("position", course.get("position"));
        timerMap.put("im_course_id", course.get("im_course_id"));

        if(MiscUtils.isTheSameDate(new Date(startTime), new Date())){
        	//提前五分钟开课提醒        
        	if(startTime-System.currentTimeMillis()> 5 * 60 *1000){        
        		RequestEntity mqRequestEntity = new RequestEntity();
        		mqRequestEntity.setServerName("MessagePushServer");
        		mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
        		mqRequestEntity.setFunctionName("processCourseStartShortNotice");
        		mqRequestEntity.setParam(timerMap);
        		this.mqUtils.sendMessage(mqRequestEntity);
        		if(startTime-System.currentTimeMillis()> 15 * 60 *1000){
            		mqRequestEntity.setFunctionName("processCourseStartStudentStudyNotice");
            		this.mqUtils.sendMessage(mqRequestEntity);
        		}
        	}
            //如果该课程为今天内的课程，则调用MQ，将其加入课程超时未开播定时任务中  结束任务
            RequestEntity mqRequestEntity = new RequestEntity();
            mqRequestEntity.setServerName("MessagePushServer");
            mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
            mqRequestEntity.setFunctionName("processCourseNotStart");           
            mqRequestEntity.setParam(timerMap);
            this.mqUtils.sendMessage(mqRequestEntity);
            
            //开课时间到但是讲师未出现提醒  推送给参加课程者
            mqRequestEntity.setFunctionName("processCourseStartLecturerNotShow");
            this.mqUtils.sendMessage(mqRequestEntity);
        }
        //提前24小时开课提醒
        if(MiscUtils.isTheSameDate(new Date(startTime- 60 * 60 *1000*24), new Date()) && startTime-System.currentTimeMillis()> 60 * 60 *1000*24){
            RequestEntity mqRequestEntity = new RequestEntity();
            mqRequestEntity.setServerName("MessagePushServer");
            mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
            mqRequestEntity.setFunctionName("processCourseStartLongNotice");           
            mqRequestEntity.setParam(timerMap);
            this.mqUtils.sendMessage(mqRequestEntity);
        }
 
        //向关注者进行极光推送，使用标签进行推送 //TODO
//        JSONObject obj = new JSONObject();
        String roomId = course.get("room_id");
//        List<String> followUserIds = lectureModuleServer.findFollowUserIdsByRoomId(roomId);
//        if(MiscUtils.isEmpty(followUserIds)){
//            return resultMap;
//        }
//        String roomName = jedis.hget(liveRoomKey,"room_name");
//        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_room_follow_new_course"), roomName,reqMap.get("course_title").toString()));
//        obj.put("user_ids", followUserIds);
//        obj.put("msg_type","11");
//        Map<String,String> extrasMap = new HashMap<>();
//        extrasMap.put("msg_type","11");
//        extrasMap.put("course_id",dbResultMap.get("course_id").toString());
//        obj.put("extras_map", extrasMap);
//        JPushHelper.push(obj);//TODO
         
        map.clear();
        map.put("lecturer_id", userId);        
        Map<String, String> lecturer = CacheUtils.readLecturer(userId, generateRequestEntity(null, null, null, map), readLecturerOperation, jedisUtils);
        String nickName = MiscUtils.RecoveryEmoji(lecturer.get("nick_name"));
        String courseTitle = MiscUtils.RecoveryEmoji(course.get("course_title"));


        //取出粉丝列表
        List<Map<String,Object>> findFollowUser = lectureModuleServer.findRoomFanListWithLoginInfo(roomId);

        Map<String,Object> queryNo = new HashMap<String,Object>();
        queryNo.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, userId);
        String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, queryNo);
        Map<String, String> serviceNoMap = jedis.hgetAll(serviceNoKey);

        //TODO  关注的直播间有新的课程，推送提醒
        if (!MiscUtils.isEmpty(findFollowUser) || !MiscUtils.isEmpty(serviceNoMap)) {
        	Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
        	TemplateData first = new TemplateData();
        	first.setColor("#000000");
        	first.setValue(MiscUtils.getConfigByKey("wpush_follow_course_first"));
        	templateMap.put("first", first);

        	TemplateData name = new TemplateData();
        	name.setColor("#000000");
        	name.setValue(MiscUtils.RecoveryEmoji(courseTitle));
        	templateMap.put("keyword1", name);

        	TemplateData wuliu = new TemplateData();
        	wuliu.setColor("#000000");
            String content = MiscUtils.RecoveryEmoji(course.get("course_remark"));
            if(MiscUtils.isEmpty(content)){
                content = "";
            }
            wuliu.setValue(content);

        	templateMap.put("keyword2", wuliu);    

        	TemplateData orderNo = new TemplateData();
        	orderNo.setColor("#000000");
        	orderNo.setValue(MiscUtils.RecoveryEmoji(nickName));
        	templateMap.put("keyword3", orderNo);

        	Date  startTime1 = new Date(Long.parseLong(reqMap.get("start_time").toString()));
        	TemplateData receiveAddr = new TemplateData();
        	receiveAddr.setColor("#000000");
        	receiveAddr.setValue(MiscUtils.parseDateToFotmatString(startTime1, "yyyy-MM-dd HH:mm"));
        	templateMap.put("keyword4", receiveAddr);

        	TemplateData remark = new TemplateData();
        	remark.setColor("#000000");
        	remark.setValue(String.format(MiscUtils.getConfigByKey("wpush_follow_course_remark"),MiscUtils.RecoveryEmoji(nickName)));
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
                this.mqUtils.sendMessage(mqRequestEntity);
            }


            if (!MiscUtils.isEmpty(serviceNoMap)) { //该讲师绑定服务号，推送提醒给粉丝

                String authorizer_access_token = getWeServiceNo(serviceNoMap, userId, serviceNoKey, jedis);

                if (authorizer_access_token != null) {
                    Map<String, Object> wxPushParam = new HashMap<>();
                    wxPushParam.put("templateParam", templateMap);//模板消息
                    wxPushParam.put("course_id", courseId);//课程ID
                    wxPushParam.put("lecturer_id", userId);//课程ID
                    wxPushParam.put("accessToken", authorizer_access_token);//课程ID
                    wxPushParam.put("pushType", "1");//1创建课程 2更新课程

                    RequestEntity mqRequestEntity = new RequestEntity();
                    mqRequestEntity.setServerName("MessagePushServer");
                    mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);//异步进行处理
                    mqRequestEntity.setFunctionName("noticeCourseToServiceNoFollow");
                    mqRequestEntity.setParam(wxPushParam);
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
            }
        }

        if ("0".equals(course_type)) {//公开课才开启机器人
            log.info("创建课程，开始机器人加入功能");

            map.clear();
            map.put("course_id", courseId);
            RequestEntity mqRequestEntity = new RequestEntity();
            mqRequestEntity.setServerName("CourseRobotService");
            mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
            mqRequestEntity.setFunctionName("courseCreateAndRobotStart");
            mqRequestEntity.setParam(map);
            this.mqUtils.sendMessage(mqRequestEntity);
            jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
        }

        //给课程里面推消息
        Map<String, Object> userInfo = lectureModuleServer.findUserInfoByUserId(timerMap.get("lecturer_id").toString());
        Map<String,Object> startLecturerMessageInformation = new HashMap<>();
        startLecturerMessageInformation.put("creator_id",userInfo.get("user_id"));//发送人id
        startLecturerMessageInformation.put("course_id", timerMap.get("course_id").toString());//课程id
        startLecturerMessageInformation.put("message",MiscUtils.getConfigByKey("start_lecturer_message"));
        startLecturerMessageInformation.put("message_type", "1");
        startLecturerMessageInformation.put("message_id",MiscUtils.getUUId());
        startLecturerMessageInformation.put("message_imid",startLecturerMessageInformation.get("message_id"));
        startLecturerMessageInformation.put("create_time",  System.currentTimeMillis());
        startLecturerMessageInformation.put("send_type","0");
        startLecturerMessageInformation.put("creator_avatar_address",userInfo.get("avatar_address"));
        startLecturerMessageInformation.put("creator_nick_name",userInfo.get("nick_name"));
//        Map<String,Object> startLecturerMessageMap = new HashMap<>();
//        startLecturerMessageMap.put("msg_type","1");
//        startLecturerMessageMap.put("send_time", System.currentTimeMillis());
//        startLecturerMessageMap.put("create_time", System.currentTimeMillis());
//        startLecturerMessageMap.put("information",startLecturerMessageInformation);
//        startLecturerMessageMap.put("mid",startLecturerMessageInformation.get("message_id"));

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

        return resultMap;
    }

    public String getWeServiceNo(Map<String, String> serviceNoMap, String userId, String serviceNoKey, Jedis jedis){
        String expiresTimes = serviceNoMap.get("expires_time");
        String authorizer_access_token = serviceNoMap.get("authorizer_access_token");

        long expiresTimeStamp = Long.parseLong(expiresTimes);
        //是否快要超时 令牌是存在有效期（2小时）
        long nowTimeStamp = System.currentTimeMillis();
        if (nowTimeStamp-expiresTimeStamp > 0) {  //accessToken已经过期了

            String authorizer_appid = serviceNoMap.get("authorizer_appid");
            String authorizer_refresh_token = serviceNoMap.get("authorizer_refresh_token");

            JSONObject authJsonObj = WeiXinUtil.refreshServiceAuthInfo(authorizer_access_token, authorizer_refresh_token, authorizer_appid);
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
    }

    @SuppressWarnings("unchecked")
    @FunctionName("courseDetail")
    public Map<String, Object> getCourseDetail(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String course_id = (String)reqMap.get("course_id");

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String ,String> courseInfoMap = CacheUtils.readCourse(course_id,reqEntity,readCourseOperation, jedisUtils,false);
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
        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(room_id, reqEntity, readLiveRoomOperation, jedisUtils, true);
        if(MiscUtils.isEmpty(liveRoomMap)){
            throw new QNLiveException("100031");
        }
        resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));
        
        //分享URL
        resultMap.put("share_url",getCourseShareURL(userId, course_id, courseInfoMap));
        List<String> roles = new ArrayList<>();
        roles.add("3");//角色数组  1：普通用户、2：学员、3：讲师
        resultMap.put("roles",roles);
 
        return resultMap;
    }

    private String getCourseShareURL(String userId, String courseId, Map<String,String> courseMap) throws Exception{
        String share_url ;
        String roomId = courseMap.get("room_id");
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("distributer_id", userId);
        queryMap.put("room_id", roomId);
        Map<String,String> distributerRoom = CacheUtils.readDistributerRoom(userId, roomId, readRoomDistributerOperation, jedisUtils);

        boolean isDistributer = false;
        String recommend_code = null;
        if (! MiscUtils.isEmpty(distributerRoom)) {
            isDistributer = true;
            recommend_code = distributerRoom.get("rq_code");
        }

        //是分销员
        if(isDistributer == true){
            share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix") + courseId + "&recommend_code=" + recommend_code;
        }else {
            //不是分销员
            share_url = MiscUtils.getConfigByKey("course_share_url_pre_fix") + courseId;
        }

        return share_url;
    }
 
    @SuppressWarnings("unchecked")
    @FunctionName("updateCourse")
    public Map<String, Object> updateCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();//传过来的参数
        Map<String, Object> resultMap = new HashMap<String, Object>();//返回的参数

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//获取userId
        Jedis jedis = jedisUtils.getJedis();//缓存
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());//课程id
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);//"SYS:COURSE:{course_id}"
        String course_id = (String) reqMap.get("course_id");
        
        Map<String, String> course = CacheUtils.readCourse((String)course_id, generateRequestEntity(null, null, null, reqMap), readCourseOperation, jedisUtils, false);//课程信息
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
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
            String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
            jedis.zrem(lecturerCoursesPredictionKey, reqMap.get("course_id").toString());
            
            long lpos = MiscUtils.convertInfoToPostion(courseEndTime.getTime(), MiscUtils.convertObjectToLong(jedis.hget(courseKey, "position")));
            
            String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
            
            jedis.zadd(lecturerCoursesFinishKey, lpos, reqMap.get("course_id").toString());

            //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移除。如果存在结束课程列表 SYS：courses ：finish，则增加到课程结束列表
            jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, reqMap.get("course_id").toString());
            jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, lpos, reqMap.get("course_id").toString());

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

            //1.7如果存在课程聊天信息，则将聊天信息使用MQ，保存到数据库中            
            //RequestEntity mqRequestEntity = generateRequestEntity("SaveCourseMessageServer",Constants.MQ_METHOD_ASYNCHRONIZED, null ,reqEntity.getParam());
            //this.mqUtils.sendMessage(mqRequestEntity);


            //1.8如果存在课程音频信息，则将课程音频信息使用MQ，保存到数据库
            //RequestEntity mqAudioRequestEntity = new RequestEntity();
            //mqAudioRequestEntity.setServerName("SaveAudioMessageServer");
            //mqAudioRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
            //mqAudioRequestEntity.setParam(reqEntity.getParam());
            //this.mqUtils.sendMessage(mqAudioRequestEntity);

            //1.9如果该课程没有真正开播，并且开播时间在今天之内，则需要取消课程超时未开播定时任务
            if(jedis.hget(courseKey, "real_start_time") == null){
                String courseStartTime = jedis.hget(courseKey, "start_time");
                if(Long.parseLong(courseStartTime) < MiscUtils.getEndTimeOfToday().getTime()){
                    RequestEntity timerRequestEntity = generateRequestEntity("MessagePushServer",Constants.MQ_METHOD_ASYNCHRONIZED,"processCourseNotStartCancel",reqEntity.getParam());
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
            startLecturerMessageInformation.put("message",MiscUtils.getConfigByKey("end_lecturer_message"));
            startLecturerMessageInformation.put("message_type", "1");
            startLecturerMessageInformation.put("message_id",imid);
            startLecturerMessageInformation.put("message_imid",imid);
            startLecturerMessageInformation.put("create_time",  System.currentTimeMillis());
            startLecturerMessageInformation.put("send_type","0");
            startLecturerMessageInformation.put("creator_avatar_address",userInfo.get("avatar_address"));
            startLecturerMessageInformation.put("creator_nick_name",userInfo.get("nick_name"));
            Map<String,Object> startLecturerMessageMap = new HashMap<>();
            startLecturerMessageMap.put("msg_type","1");
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

            String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, startLecturerMessageInformation);//直播间开始于
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
            messageMap.put("send_time", System.currentTimeMillis());
            messageMap.put("create_time", System.currentTimeMillis());
            messageMap.put("information",infomation);
            messageMap.put("mid",infomation.get("message_id"));
            String content = JSON.toJSONString(messageMap);
            IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);

        } else {
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
                course = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, query), readCourseOperation, jedisUtils, true);
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

                mqRequestEntity.setFunctionName("processCourseStartShortNoticeUpdate");
                this.mqUtils.sendMessage(mqRequestEntity);

                mqRequestEntity.setFunctionName("processCourseNotStartUpdate");
                this.mqUtils.sendMessage(mqRequestEntity);

                mqRequestEntity.setFunctionName("processCourseStartLongNoticeUpdate");
                this.mqUtils.sendMessage(mqRequestEntity);

                mqRequestEntity.setFunctionName("processCourseStartStudentStudyNoticeUpdate");
                this.mqUtils.sendMessage(mqRequestEntity);

                mqRequestEntity.setFunctionName("processCourseStartLecturerNotShowUpdate");
                mqUtils.sendMessage(mqRequestEntity);

                long lpos = MiscUtils.convertInfoToPostion(Long.parseLong(newStartTime), MiscUtils.convertObjectToLong(course.get("position")));
                jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, lpos, reqMap.get("course_id").toString());


                //TODO 推送多人  报名的所有人
                List<Map<String, Object>> userInfo = lectureModuleServer.findCourseStudentListWithLoginInfo(course_id);

                //是否存在讲师服务号信息
                Map<String, Object> queryNo = new HashMap<String, Object>();
                queryNo.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, userId);
                String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, queryNo);
                Map<String, String> serviceNoMap = jedis.hgetAll(serviceNoKey);

                if (!MiscUtils.isEmpty(userInfo) || !MiscUtils.isEmpty(serviceNoMap)) {

                    Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
                    TemplateData first = new TemplateData();
                    first.setColor("#000000");
                    first.setValue(MiscUtils.getConfigByKey("wpush_update_course_first"));
                    templateMap.put("first", first);

                    TemplateData wuliu = new TemplateData();
                    wuliu.setColor("#000000");
                    wuliu.setValue(course.get("course_title").toString());
                    templateMap.put("keyword1", wuliu);

                    TemplateData name = new TemplateData();
                    name.setColor("#000000");
                    name.setValue("修改部分课程信息！");
                    templateMap.put("keyword2", name);

                    TemplateData orderNo = new TemplateData();
                    orderNo.setColor("#000000");
                    orderNo.setValue(MiscUtils.parseDateToFotmatString(new Date(MiscUtils.convertObjectToLong(original_start_time)), "yyyy-MM-dd HH:mm:ss"));
                    templateMap.put("keyword3", orderNo);

                    TemplateData nowDate = new TemplateData();
                    nowDate.setColor("#000000");
                    nowDate.setValue(MiscUtils.parseDateToFotmatString(new Date(Long.parseLong(newStartTime)), "yyyy-MM-dd HH:mm:ss"));
                    templateMap.put("keyword4", nowDate);

                    TemplateData remark = new TemplateData();
                    remark.setColor("#000000");
                    remark.setValue(MiscUtils.getConfigByKey("wpush_update_course_remark"));
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
                        this.mqUtils.sendMessage(wxMqRequestEntity);
                    }

                    if (!MiscUtils.isEmpty(serviceNoMap)) { //存在服务号信息
                        String authorizer_access_token = getWeServiceNo(serviceNoMap, userId, serviceNoKey, jedis);
                        if (authorizer_access_token != null) {
                            Map<String, Object> wxPushParam = new HashMap<>();
                            wxPushParam.put("templateParam", templateMap);//模板消息
                            wxPushParam.put("course_id", course_id);//课程ID
                            wxPushParam.put("lecturer_id", userId);//课程ID
                            wxPushParam.put("accessToken", authorizer_access_token);//课程ID
                            wxPushParam.put("pushType", "2");//1创建课程 2更新课程

                            RequestEntity serviceRequestEntity = new RequestEntity();
                            serviceRequestEntity.setServerName("MessagePushServer");
                            serviceRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);//异步进行处理
                            serviceRequestEntity.setFunctionName("noticeCourseToServiceNoFollow");
                            serviceRequestEntity.setParam(wxPushParam);
                            this.mqUtils.sendMessage(serviceRequestEntity);
                        }
                    }
                }
            }

            resultMap.put("update_time", updateCacheMap.get("update_time"));
        }
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, userId);
        return resultMap;
    }
    /**
     * 逻辑roomCourses,courseList类似，注意重构同步
     * */
    @FunctionName("courseList")
    public Map<String, Object> getCourseList(RequestEntity reqEntity) throws Exception {
        
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();                
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());                
        //从讲师信息中加载，数据不存在需加载缓存
        Map<String,String> keyMap = new HashMap<String,String>();
        keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        reqEntity.setParam(keyMap);
        Map<String,String> values = CacheUtils.readLecturer(userId, reqEntity, readLecturerOperation, jedisUtils);
        String course_num_str =  values.get("course_num");
        Map<String,Object> result = new HashMap<String,Object>();        
        result.put("course_num", course_num_str);
        
        Jedis jedis = jedisUtils.getJedis();
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
        
        String course_id = (String)reqMap.get("course_id");
        int pageCount = (int)reqMap.get("page_count");
        Long query_time = (Long)reqMap.get("query_time");
        Long position = (Long)reqMap.get("position");
        long currentTime = System.currentTimeMillis();
        
        List<Map<String,String>> courseList = new LinkedList<Map<String,String>>();        
        String startIndexFinish = "+inf";
        String endIndexPrediction = "-inf";
        
        Set<Tuple> dictionList = null;
        boolean checkDiction = MiscUtils.isEmpty(course_id) || (jedis.zscore(lecturerCoursesPredictionKey, course_id) != null);
        boolean checkPrediction = checkDiction;
        if(query_time!=null && query_time>currentTime){
        	checkDiction=false;
        }
        List<String> courseIdList = new LinkedList<String>();
        Set<String> courseIdSet = new HashSet<String>();
        if(checkDiction){        	
        	if(MiscUtils.isEmpty(course_id)){
        		startIndexFinish = currentTime+"";        		
        	} else if(query_time!=null && query_time <= currentTime){
        		startIndexFinish = "("+query_time;
        	}
        	dictionList = jedis.zrevrangeByScoreWithScores(lecturerCoursesPredictionKey, startIndexFinish, endIndexPrediction, 0, pageCount);
        	if(dictionList != null){
        		pageCount = pageCount - dictionList.size();
        		for(Tuple tuple : dictionList){
        			String courseId = tuple.getElement();
        			courseIdList.add(courseId);
        			courseIdSet.add(courseId);
        		}
        	}
        }
        
        Map<String,Map<String,String>> cachedCourse = new HashMap<String,Map<String,String>>();
        
        Set<Tuple> preDictionSet = null;
        List<String> preDictionList = new LinkedList<String>();
        if(pageCount>0 && checkPrediction){
        	startIndexFinish = "+inf";
        	if(query_time!=null && query_time>=currentTime){
        		startIndexFinish="("+query_time;
        	}
        	preDictionSet = jedis.zrevrangeByScoreWithScores(lecturerCoursesPredictionKey, startIndexFinish, endIndexPrediction, 0, pageCount);
        	if(preDictionList != null){
        		Map<String,String> queryParam = new HashMap<String,String>();
        		RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
        		for(Tuple tuple : preDictionSet){
        			String courseId = tuple.getElement();
        			queryParam.put("course_id", courseId);
        			Map<String, String> courseInfoMap = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedisUtils, true);
        			MiscUtils.courseTranferState(currentTime, courseInfoMap);
        			cachedCourse.put(courseId, courseInfoMap);
        			String status = (String)courseInfoMap.get("status");
        			if(!"4".equals(status)){
        				if(courseIdSet.contains(courseId)){
        					courseIdList.remove(courseId);
        				}
        				if("2".equals(status)){
        					jedis.zrem(lecturerCoursesPredictionKey, courseId);
        					if(courseInfoMap.get("end_time")==null){
        						jedis.zadd(lecturerCoursesFinishKey, MiscUtils.convertObjectToLong(courseInfoMap.get("start_time")),courseId);
        					} else {
        						jedis.zadd(lecturerCoursesFinishKey, MiscUtils.convertObjectToLong(courseInfoMap.get("end_time")),courseId);
        					}
        					
        					continue;
        				}
        				courseIdList.add(courseId);
        				courseIdSet.add(courseId);
        			}
        		}
        	}        	
        }
        boolean finExist = false;
        Set<Tuple> finishDictionSet = null;
        pageCount=((int)reqMap.get("page_count"))-courseIdList.size();
        if(pageCount>0){
        	if(MiscUtils.isEmpty(course_id) || query_time == null){
        		startIndexFinish = "+inf";
        	} else {
        		startIndexFinish = "("+MiscUtils.convertInfoToPostion(query_time,position);
        	}
        	finishDictionSet = jedis.zrevrangeByScoreWithScores(lecturerCoursesFinishKey, startIndexFinish, endIndexPrediction, 0, pageCount);
        	if(!MiscUtils.isEmpty(finishDictionSet)){
        		for(Tuple tuple : finishDictionSet){
        			String courseId = tuple.getElement();
        			if(courseIdSet.contains(courseId)){
        				courseIdList.remove(courseId);
        			}
        			courseIdList.add(courseId);
        			finExist=true;
        		}        		
        	}
        }
        pageCount=((int)reqMap.get("page_count"))-courseIdList.size();
        Map<String,String> lastCourse = null;
        if(!MiscUtils.isEmpty(courseIdList)){
			Map<String,String> queryParam = new HashMap<String,String>();
			RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
            for(String courseId:courseIdList){
            	Map<String,String> courseInfoMap = cachedCourse.get(courseId);
            	queryParam.put("course_id", courseId);
            	if(courseInfoMap==null){
            		courseInfoMap = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedisUtils, true);
            	}
            	MiscUtils.courseTranferState(currentTime, courseInfoMap);
            	courseList.add(courseInfoMap);
            	lastCourse = courseInfoMap;
            }
        }

        
        if(pageCount > 0){
        	map.clear();
            map.put("pageCount", pageCount);
            map.put("lecturer_id", userId);
            Long queryTime = null; 
            if(!MiscUtils.isEmpty(lastCourse)){
                if(finExist){
                	if(lastCourse.get("end_time")==null){
                		queryTime = Long.parseLong(lastCourse.get("start_time"));
                	} else {
                		queryTime = Long.parseLong(lastCourse.get("end_time"));
                	}
                	position = MiscUtils.convertObjectToLong(lastCourse.get("position"));
                } else {
                    queryTime = Long.parseLong(lastCourse.get("start_time"));
                    position = MiscUtils.convertObjectToLong(lastCourse.get("position"));
                }

            } else {
            	queryTime=(Long)reqMap.get("query_time");
            	position=(Long)reqMap.get("position");
            }
            if(queryTime != null){                
                map.put("position", MiscUtils.convertInfoToPostion(queryTime, position));
            }
            List<Map<String,Object>> finishCourse = lectureModuleServer.findFinishCourseListForLecturer(map);
            if(!MiscUtils.isEmpty(finishCourse)){               
                for(Map<String,Object> finish:finishCourse){
                    if(MiscUtils.isEqual(course_id, finish.get("course_id"))){
                        continue;
                    }
                    Map<String,String> finishMap = new HashMap<String,String>();
                    MiscUtils.converObjectMapToStringMap(finish, finishMap);
                    courseList.add(finishMap);
                }
            }
        }
        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_USER_FIELD, userId);
        RequestEntity queryOperation = generateRequestEntity(null, null, null, query);
        CacheUtils.readUser(userId, queryOperation, readUserOperation, jedisUtils);
		
		final String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);
        for(Map<String,String> courseInfo:courseList){
        	String courseId = courseInfo.get("course_id");
        	if(jedis.sismember(key, courseId)){
        		courseInfo.put("student", "Y");
        	} else {
        		courseInfo.put("student", "N");
        	}
        }
        
/*        List<Map<String,String>> courseList = getCourseList(userId,(int)reqMap.get("page_count"),(String)reqMap.get("course_id"), 
                (Long)reqMap.get("query_time"), false, true);*/
        result.put("course_list", courseList);    
        
        return result;

        //<editor-fold desc="暂时无用">
        /*
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String ,String>> courseResultList = new ArrayList<>();
 
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
 
        //TODO 目前只有查询讲师的课程列表，查询直播间的课程列表暂未实现
        //if (reqMap.get("room_id") == null || StringUtils.isBlank(reqMap.get("room_id").toString())) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
 
        String startIndex = null;
        String endIndex = "+inf";
        String startIndexDB = null;
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        if (reqMap.get("start_time") == null || StringUtils.isBlank(reqMap.get("start_time").toString())) {
            startIndex = "0";
        } else {
            startIndex = "(" + reqMap.get("start_time").toString();
        }
 
        Set<Tuple> predictionList = jedis.zrangeByScoreWithScores(lecturerCoursesPredictionKey, startIndex, endIndex, 0, pageCount);
        Set<Tuple> finishList = null;
        List<Map<String,Object>> dbList = null;
        Map<String,Object> finishResultMap = new HashMap<>();
 
        if (predictionList == null || predictionList.isEmpty()) {
            if(startIndex.equals("0")){
                endIndex = "-inf";
                startIndex = "+inf";
                startIndexDB = null;
            }else {
                endIndex = "-inf";
                startIndexDB = startIndex;
            }
 
            finishResultMap = findCourseFinishList(jedis, lecturerCoursesFinishKey, startIndex, startIndexDB, endIndex, 0 , pageCount,userId);
 
        } else {
            if (predictionList.size() < pageCount) {
                startIndex = "+inf";
                endIndex = "-inf";
                finishResultMap = findCourseFinishList(jedis, lecturerCoursesFinishKey, startIndex, null, endIndex, 0 , pageCount - predictionList.size(), userId);
            }
        }
 
        //未结束课程列表，结束课程列表，数据库课程列表三者拼接到最终的课程结果列表，即可得到结果
        //分别迭代这三个列表
        if(!CollectionUtils.isEmpty(finishResultMap)){
            if(finishResultMap.get("finishList") != null){
                finishList = (Set<Tuple>)finishResultMap.get("finishList");
            }
 
            if(finishResultMap.get("dbList") != null){
                dbList = (List<Map<String,Object>>)finishResultMap.get("dbList");
            }
        }
 
 
        long currentTime = System.currentTimeMillis();
        if(predictionList != null){
            for (Tuple tuple : predictionList) {
                ((Map<String, Object>) reqEntity.getParam()).put("course_id",tuple.getElement());
                Map<String ,String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(),reqEntity,readCourseOperation, jedisUtils,true);
                MiscUtils.courseTranferState(currentTime, courseInfoMap);
                courseResultList.add(courseInfoMap);
            }
        }
 
        if(finishList != null){
            for (Tuple tuple : finishList) {
                ((Map<String, Object>) reqEntity.getParam()).put("course_id",tuple.getElement());
                Map<String ,String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(),reqEntity,readCourseOperation, jedisUtils,true);
                MiscUtils.courseTranferState(currentTime, courseInfoMap);
                courseResultList.add(courseInfoMap);
            }
        }
 
        if(dbList != null){
            for (Map<String,Object> courseDBMap : dbList) {
                Map<String,String> courseDBMapString = new HashMap<>();
                MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                MiscUtils.courseTranferState(currentTime, courseDBMapString);
                courseResultList.add(courseDBMapString);
            }
        }
 
        //}
 
        if(! CollectionUtils.isEmpty(courseResultList)){
            resultMap.put("course_list", courseResultList);
        }
 
        //返回课程总数
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        String course_num = jedis.hget(lecturerKey, "course_num");//TODO 目前仅查询讲师缓存的课程数，尚未完成查询直播间缓存的课程数
        if(! MiscUtils.isEmpty(course_num)){
            resultMap.put("course_num", Long.parseLong(course_num));
        }else {
            resultMap.put("course_num", 0);
        }
        return resultMap;
        */
        //</editor-fold>
    }



    //<editor-fold desc="暂时不用此方法">
/*    private Map<String,Object> findCourseFinishList(Jedis jedis, String key,
                                                    String startIndexCache, String startIndexDB, String endIndex, Integer limit, Integer count,String userId){
        Set<Tuple> finishList = jedis.zrevrangeByScoreWithScores(key, startIndexCache, endIndex, limit, count);
        Map<String,Object> queryMap = new HashMap<>();
        List<Map<String,Object>> dbList = null;
 
        //如果结束课程列表为空，则查询数据库
        if (finishList == null || finishList.isEmpty()) {
            queryMap.put("pageCount", count);
            if(startIndexDB != null){
                Date date = new Date(Long.parseLong(startIndexDB.substring(1)));
                queryMap.put("startIndex", date);
            }
            queryMap.put("lecturer_id", userId);
            dbList = lectureModuleServer.findCourseListForLecturer(queryMap);
        } else {
            //如果结束课程列表中的数量不够，则剩余需要查询数据库
            if (finishList.size() < count) {
                startIndexDB = findLastElementForRedisSet(finishList).get("startIndexDB");
                queryMap.put("pageCount", count - finishList.size());
                if(startIndexDB != null){
                    Date date = new Date(Long.parseLong(startIndexDB));
                    queryMap.put("startIndex", date);
                }
                queryMap.put("lecturer_id", userId);
                dbList = lectureModuleServer.findCourseListForLecturer(queryMap);
            }
        }
 
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("finishList",finishList);
        resultMap.put("dbList",dbList);
        return resultMap;
    }*/
 
/*    private Map<String,String> findLastElementForRedisSet(Set<Tuple> redisSet){
        Map<String,String> resultMap = new HashMap<>();
        String startIndexCache = null;
        String startIndexDB = null;
        DecimalFormat decimalFormat = new DecimalFormat("#");
        for (Tuple tuple : redisSet) {
            startIndexCache = "(" + decimalFormat.format(tuple.getScore());
            startIndexDB = decimalFormat.format(tuple.getScore()) + "";
        }
        resultMap.put("startIndexCache", startIndexCache);
        resultMap.put("startIndexDB", startIndexDB);
        return resultMap;
    }*/
    //</editor-fold>
 
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
        Jedis jedis = jedisUtils.getJedis();
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
 
//    @SuppressWarnings("unchecked")
//    @FunctionName("courseInfo")
//    public Map<String, Object> getCourseInfo(RequestEntity reqEntity) throws Exception {
//        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
//        Map<String, Object> resultMap = new HashMap<String, Object>();
//
//        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
//        Jedis jedis = jedisUtils.getJedis();
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
//        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
//
//        //0.校验该课程是否属于该讲师
//        String courseOwner = jedis.hget(courseKey, "lecturer_id");
//        if(courseOwner == null || !userId.equals(courseOwner)){
//            throw new QNLiveException("100013");
//        }
//
//        Map<String,String> courseMap = new HashMap<>();
//        //1.先检查该课程是否在缓存中
//        if(jedis.exists(courseKey)){
//            courseMap = jedis.hgetAll(courseKey);
//            JSONArray pptList = null;
//            map.clear();
//            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
//            String pptListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
//            if(jedis.exists(pptListKey)){
//                pptList = JSONObject.parseArray(jedis.get(pptListKey));
//            }
//
//            List<Map<String,String>> audioObjectMapList = new ArrayList<>();
//            map.clear();
//            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
//            String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
//            String audioJsonStringKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, map);
//            Set<String> audioIdList = jedis.zrange(audioListKey, 0 , -1);
//
//            //如果存在zsort列表，则从zsort列表中读取
//            if(audioIdList != null && audioIdList.size() > 0){
//                JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
//                callBack.invoke(new JedisBatchOperation(){
//                    @Override
//                    public void batchOperation(Pipeline pipeline, Jedis jedis) {
//
//                        List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
//                        for(String audio : audioIdList){
//                            map.put(Constants.FIELD_AUDIO_ID, audio);
//                            String audioKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
//                            redisResponseList.add(pipeline.hgetAll(audioKey));
//                        }
//                        pipeline.sync();
//
//                        for(Response<Map<String, String>> redisResponse : redisResponseList){
//                            Map<String,String> messageStringMap = redisResponse.get();
//                            audioObjectMapList.add(messageStringMap);
//                        }
//                    }
//                });
//
//                resultMap.put("audio_list", audioObjectMapList);
//
//                //如果存在讲课音频的json字符串，则读取讲课音频json字符串
//            } else if(jedis.exists(audioJsonStringKey)){
//                resultMap.put("audio_list", JSONObject.parse(jedis.get(audioJsonStringKey)));
//            }
//
//            if(! CollectionUtils.isEmpty(pptList)){
//                resultMap.put("ppt_list", pptList);
//            }
//
//            resultMap.put("im_course_id", jedis.hget(courseKey, "im_course_id"));
//        }else{
//            //2.如果不在缓存中，则查询数据库
//            Map<String,Object> courseInfoMap = lectureModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
//            if(courseInfoMap == null){
//                throw new QNLiveException("100004");
//            }
//            courseMap = new HashMap<>();
//            MiscUtils.converObjectMapToStringMap(courseInfoMap,courseMap);
//
//            //查询课程PPT列表
//            List<Map<String,Object>> pptList = lectureModuleServer.findPPTListByCourseId(reqMap.get("course_id").toString());
//
//            //查询课程语音列表
//            List<Map<String,Object>> audioList = lectureModuleServer.findAudioListByCourseId(reqMap.get("course_id").toString());
//
//            if(! CollectionUtils.isEmpty(pptList)){
//                resultMap.put("ppt_list", pptList);
//            }
//
//            if(! CollectionUtils.isEmpty(audioList)){
//                resultMap.put("audio_list", audioList);
//            }
//
//            resultMap.put("im_course_id", courseInfoMap.get("im_course_id"));
//
//        }
//
//        map.clear();
//        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
//        String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
//        Set<String> banUserIdList = jedis.zrange(bandKey, 0 , -1);
//        if(banUserIdList != null && banUserIdList.size() > 0){
//            resultMap.put("ban_user_id_list", banUserIdList);
//        }
//
//        try {
//            //检查学生上次加入课程，如果加入课程不为空，则退出上次课程
//            Map<String,String> queryParam = new HashMap<>();
//            queryParam.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
//            String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, queryParam);
//            Map<String,String> loginInfo = jedis.hgetAll(accessTokenKey);
//
//            map.put(Constants.CACHED_KEY_USER_FIELD, userId);
//            String courseIMKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_LAST_JOIN_COURSE_IM_INFO, map);
//            String imCourseId = jedis.get(courseIMKey);
//            if(! MiscUtils.isEmpty(imCourseId)){
//                IMMsgUtil.delGroupMember(imCourseId, loginInfo.get("m_user_id"), loginInfo.get("m_user_id"));
//            }
//
//            //加入新课程IM群组，并且将加入的群组记录入缓存中
//            IMMsgUtil.joinGroup(courseMap.get("im_course_id"), loginInfo.get("m_user_id"), loginInfo.get("m_user_id"));
//            jedis.set(courseIMKey, courseMap.get("im_course_id"));
//        }catch (Exception e){
//            //TODO 暂时不处理
//        }
//
//        //增加返回课程相应信息
//        MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);
//        resultMap.put("student_num",courseMap.get("student_num"));
//        resultMap.put("start_time",courseMap.get("start_time"));
//        resultMap.put("status",courseMap.get("status"));
//        resultMap.put("course_type",courseMap.get("course_type"));
//        resultMap.put("course_password",courseMap.get("course_password"));
//        resultMap.put("share_url",MiscUtils.getConfigByKey("course_share_url")+reqMap.get("course_id").toString());//TODO
//        resultMap.put("course_update_time",courseMap.get("update_time"));
//        resultMap.put("course_title",courseMap.get("course_title"));
//        resultMap.put("course_url",courseMap.get("course_url"));
//
//        return resultMap;
//    }
 
//    @SuppressWarnings("unchecked")
//    @FunctionName("messageList")
//    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception {
//        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
//        Map<String, Object> resultMap = new HashMap<>();
//
//        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
//        String queryType = reqMap.get("query_type").toString();
//
//        Jedis jedis = jedisUtils.getJedis();
//        Map<String, Object> map = new HashMap<>();
//        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
//        String messageListKey;
//        Map<String, Object> queryMap = new HashMap<>();
//
//        //queryType为0则查询全部消息，为1则查询提问
//        if(queryType.equals("0")){
//            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
//        }else {
//            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_QUESTION, map);
//            queryMap.put("send_type", "3");//send_type 类型:0:讲师讲解 1：讲师回答 2 用户评论 3 用户提问
//        }
//
//        //缓存不存在则读取数据库中的内容
//        if(! jedis.exists(messageListKey)){
//            queryMap.put("page_count", pageCount);
//            if(reqMap.get("message_pos") != null && StringUtils.isNotBlank(reqMap.get("message_pos").toString())){
//                queryMap.put("message_pos", Long.parseLong(reqMap.get("message_pos").toString()));
//            }else {
//
//            }
//            queryMap.put("course_id", reqMap.get("course_id").toString());
//            List<Map<String,Object>> messageList = lectureModuleServer.findCourseMessageList(queryMap);
//
//            if(! CollectionUtils.isEmpty(messageList)){
//                for(Map<String,Object> messageMap : messageList){
//                    if(! MiscUtils.isEmpty(messageMap.get("message"))){
//                        messageMap.put("message",MiscUtils.RecoveryEmoji(messageMap.get("message").toString()));
//                    }
//
//                    if(! MiscUtils.isEmpty(messageMap.get("message_question"))){
//                        messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
//                    }
//
//					if(!MiscUtils.isEmpty(messageMap.get("creator_nick_name"))){
//						messageMap.put("creator_nick_name",MiscUtils.RecoveryEmoji(messageMap.get("creator_nick_name").toString()));
//					}
//					//resultList.add(0, messageMap);
//                }
//                resultMap.put("message_list", messageList);
//            }
//
//            return resultMap;
//
//        }else {
//            //缓存中存在，则读取缓存中的内容
//            //初始化下标
//            long startIndex;
//            long endIndex;
//            Set<String> messageIdList;
//            //如果分页的message_id不为空
//            if(reqMap.get("message_id") != null && StringUtils.isNotBlank(reqMap.get("message_id").toString())){
//                long endRank = jedis.zrank(messageListKey, reqMap.get("message_id").toString());
//                endIndex = endRank - 1;
//                //判断该列表向上再无信息，如果再无信息，则直接将查询结果列表设置为空
//                if(endIndex < 0){
//                    startIndex = 0;
//                    messageIdList = null;
//                }else {
//                    startIndex = endIndex - pageCount + 1;
//                    if(startIndex < 0){
//                        startIndex = 0;
//                    }
//                    messageIdList = jedis.zrange(messageListKey, startIndex, endIndex);
//                }
//
//            }else {
//                endIndex = -1;
//                startIndex = jedis.zcard(messageListKey) - pageCount;
//                if(startIndex < 0){
//                    startIndex = 0;
//                }
//                messageIdList = jedis.zrange(messageListKey, startIndex, endIndex);
//            }
//
//            if(! CollectionUtils.isEmpty(messageIdList)){
//                //缓存中存在则读取缓存内容
//                List<Map<String,String>> messageListCache = new ArrayList<>();
//                for(String messageId : messageIdList){
//                    map.put(Constants.FIELD_MESSAGE_ID, messageId);
//                    String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
//                    Map<String,String> messageMap = jedis.hgetAll(messageKey);
//                    messageMap.put("message_pos", startIndex+"");
//                    //更改用户名和昵称
//                    if(MiscUtils.isEmpty(messageMap)){
//                        continue;
//                    }
//                    if(messageMap.get("creator_id") != null){
//                        Map<String,Object> innerMap = new HashMap<>();
//                        innerMap.put("user_id", messageMap.get("creator_id"));
//                        Map<String,String> userMap = CacheUtils.readUser(messageMap.get("creator_id"), this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedisUtils);
//                        if(! MiscUtils.isEmpty(userMap)){
//                            if(userMap.get("nick_name") != null){
//                                messageMap.put("creator_nick_name", userMap.get("nick_name"));
//                            }
//                            if(userMap.get("avatar_address") != null){
//                                messageMap.put("creator_avatar_address", userMap.get("avatar_address"));
//                            }
//                        }
//                    }
//
//                    String messageContent = messageMap.get("message");
//                    if(! MiscUtils.isEmpty(messageContent)){
//                        messageMap.put("message",MiscUtils.RecoveryEmoji(messageContent));
//                    }
//
//                    if(! MiscUtils.isEmpty(messageMap.get("message_question"))){
//                        messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
//                    }
//                    messageListCache.add(messageMap);
//                    startIndex++;
//                }
//
//                resultMap.put("message_list", messageListCache);
//            }
//
//            return resultMap;
//        }
//
//
//    }
 
 
    @SuppressWarnings("unchecked")
    @FunctionName("courseStudents")
    public Map<String, Object> getCourseStudentList(RequestEntity reqEntity) throws Exception {
    	Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
    	Map<String, Object> resultMap = new HashMap<>();       
    	int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
    	List<Map<String,Object>> studentList = new ArrayList<>();

    	//1.先判断数据来源  数据来源：1：缓存 2.：数据库
    	Jedis jedis = jedisUtils.getJedis();
    	Map<String, Object> map = new HashMap<>();
    	map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
    	String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
    	String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
    	String studentNum = null;

    	studentNum = jedis.hget(courseKey, "student_num");
    	if(StringUtils.isBlank(studentNum)){
    		Map<String,String> courseMap = CacheUtils.readCourse((String)reqMap.get("course_id"), generateRequestEntity(null, null, null, reqMap), readCourseOperation, jedisUtils, false);
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
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        String total_amount = jedis.hget(liveRoomKey, "total_amount");
        if(MiscUtils.isEmpty(total_amount)){
            total_amount="0";
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("total_amount", total_amount);        
        List<Map<String,String>> courseList = getCourseList(userId,(int)reqMap.get("page_count"), (String)reqMap.get("course_id"), (Long)reqMap.get("query_time"), (Long)reqMap.get("position"), true,true);
        result.put("course_list", courseList);
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @FunctionName("courseProfitList")
    public Map<String, Object> getCourseProfitList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        String room_id = (String)reqMap.get("room_id");
        String course_id = (String)reqMap.get("course_id");
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        Map<String ,String> courseInfoMap = CacheUtils.readCourse(course_id,reqEntity,readCourseOperation, jedisUtils,false);
        if(MiscUtils.isEmpty(courseInfoMap)){
            throw new QNLiveException("100013");
        }
        Map<String,Object> result = new HashMap<String,Object>();
        for(String key:courseInfoMap.keySet()){
            result.put(key, courseInfoMap.get(key));
        }

        List<Map<String,Object>> list = lectureModuleServer.findCourseProfitList(reqMap);
        result.put("profit_list", list);       
        final Map<String, List<Map<String,Object>>> profitMap = new HashMap<String, List<Map<String,Object>>>();
        for(Map<String,Object> profit:list){
            String distributer_id = (String)profit.get("distributer_id");
            if(profit.get("share_amount") != null){
                Long trueProfit = (Long)profit.get("profit_amount") - (Long)profit.get("share_amount");
                profit.put("profit_amount", trueProfit);
            }
            if(!MiscUtils.isEmpty(distributer_id)){
                List<Map<String,Object>> profitList = profitMap.get(distributer_id);
                if(profitList==null){
                    profitList = new LinkedList<Map<String,Object>>();
                    profitMap.put(distributer_id, profitList);
                }
                profitList.add(profit);
            }
        }
        if(!profitMap.isEmpty()){                       
            ((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
                @Override
                public void batchOperation(Pipeline pipeline, Jedis jedis) {
                    Map<String, Object> keyMap = new HashMap<String, Object>();
                    Map<String, Response<String>> distributerNameMap = new HashMap<String, Response<String>>();
                    for(String key:profitMap.keySet()){
                        keyMap.clear();
                        keyMap.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, key);
                        String distributerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_DISTRIBUTER, keyMap);
                        distributerNameMap.put(key, pipeline.hget(distributerKey, "nick_name"));
                    }
                    pipeline.sync();
                    for(String key:profitMap.keySet()){
                        List<Map<String,Object>> profitList = profitMap.get(key);
                        Response<String> response = distributerNameMap.get(key);
                        if(response!=null){
                            String distributer = response.get();
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

        keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);//加入老师id
        reqEntity.setParam(keyMap);
        Map<String,String> values = CacheUtils.readLecturer(userId, reqEntity, readLecturerOperation, jedisUtils);
        return values;
    }
    
    @SuppressWarnings("unchecked")
    @FunctionName("roomDistributerInfo")
    public Map<String, Object> getRoomDistributerInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam(); 
        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);     
        String liveRoomOwner =  CacheUtils.readLiveRoomInfoFromCached(room_id, "lecturer_id", reqEntity, readLiveRoomOperation, jedisUtils,true);
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        
        reqMap.put("lecturer_id", userId);        
        
        
        long distributer_num = MiscUtils.convertObjectToLong(CacheUtils.readLiveRoomInfoFromCached(room_id, "distributer_num", reqEntity, readLiveRoomOperation, jedisUtils,true));
        Map<String, Object> result = new HashMap<String,Object>();
        result.put("distributer_num", distributer_num);
        List<Map<String,Object>> list = null;
        if(distributer_num>0){
        	list = lectureModuleServer.findDistributionRoomByLectureInfo(reqMap);
        	if(!MiscUtils.isEmpty(list)){
        		final List<Map<String,Object>>  listInfo=list;
        		((JedisBatchCallback)jedisUtils.getJedis()).invoke(new JedisBatchOperation(){
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
        			Map<String,String> userInfo = CacheUtils.readUser(distributerId, entity, readUserOperation, jedisUtils);
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

    //<editor-fold desc="暂时无用">
    /*
    @SuppressWarnings("unchecked")
    @FunctionName("roomDistributerInfo")
    public Map<String, Object> getRoomDistributerInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam(); 
        Jedis jedis = jedisUtils.getJedis();
        
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);     
        String liveRoomOwner =  CacheUtils.readLiveRoomInfoFromCached(room_id, "lecturer_id", reqEntity, readLiveRoomOperation, jedisUtils,true);
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }
        
        int page_count = (Integer)reqMap.get("page_count");
        Long position = (Long)reqMap.get("position");
        String distributer_id = (String)reqMap.get("distributer_id");
        
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_DISTRIBUTERS_LEN, map);
        String distributer_num_len = jedis.get(key);
        Long distributer_num = null;
        Long max_postion = null;
        if(!MiscUtils.isEmpty(distributer_num_len)){
            String[] tmp = distributer_num_len.split(":");
            distributer_num = Long.parseLong(tmp[0]);
            max_postion = Long.parseLong(tmp[1]);
        }
        String distributeKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_DISTRIBUTERS, map);
        List<String> distributerIdList = null;
        if(position == null || max_postion == null || max_postion < position){
            if(distributer_num==null){
                String distributer_num_str = CacheUtils.readLiveRoomInfoFromCached(room_id, "distributer_num", reqEntity, readLiveRoomOperation, jedisUtils,true);
                if(MiscUtils.isEmpty(distributer_num_str)){
                    distributer_num=0l;
                } else {
                    distributer_num = Long.parseLong(distributer_num_str);
                }
            }
            if(distributer_num>0){
                distributerIdList = getDistributerListFromSys(room_id, distributer_id, distributeKey, key,position==null?0:position,page_count,distributer_num);
                distributer_num_len = jedis.get(key);                       
                if(!MiscUtils.isEmpty(distributer_num_len)){
                    String[] tmp = distributer_num_len.split(":");
                    distributer_num = Long.parseLong(tmp[0]);                
                }
            }
            if(distributerIdList==null){
                distributerIdList = new LinkedList<String>();
            }
        } else {            
            long pos = page_count+position+1;
            if(pos >= distributer_num){
                pos=-1;
            }            
            distributerIdList = new LinkedList<String>();
            if(position<distributer_num){
                Set<String> distributerIdSet = jedis.zrangeByScore(distributeKey, "("+position, "+inf", 0, (int)page_count);
                if(!MiscUtils.isEmpty(distributerIdSet)){
                    for(String value:distributerIdSet){
                        distributerIdList.add(value);
                    }
                }
            }
        }
        
        Map<String, Object> result = new HashMap<String,Object>();
        result.put("distributer_num", distributer_num);
        List<Map<String,Object>>distributerList = new LinkedList<Map<String,Object>>();
        result.put("distributer_list",distributerList);
        for(String valueStr:distributerIdList){
            distributerList.add(CacheUtils.convertCachedStringToMap(valueStr));
        }
        return result;
    }
    */
    //</editor-fold>
    @SuppressWarnings("unchecked")
    @FunctionName("roomDistributerCoursesInfo")
    public Map<String, Object> getRoomDistributerCoursesInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam(); 
        Jedis jedis = jedisUtils.getJedis();
        
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
                    MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_DISTRIBUTER_COURSES, map), start_time, min_time_key, page_count);
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
        Jedis jedis = jedisUtils.getJedis();
        
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
        
        Map<String,String> userMap = CacheUtils.readUser(userId, queryOperation, readUserOperation, jedisUtils);
        reqMap.clear();
        double profit_share_rate = Long.parseLong(values.get("profit_share_rate")) / 100.0;
        String room_share_url = String.format(MiscUtils.getConfigByKey("be_distributer_url_pre_fix"), room_share_code, room_id, profit_share_rate, values.get("effective_time"));
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
        Jedis jedis = jedisUtils.getJedis();        
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
        
        Map<String,String> distributerRoom = CacheUtils.readDistributerRoom(userId, room_id, readRoomDistributerOperation, jedisUtils);
        if(!MiscUtils.isEmpty(distributerRoom)){
        	throw new QNLiveException("100027");
        }
        Map<String,String> query = new HashMap<String,String>();
        query.put("distributer_id", userId);
        Map<String,String> distributer = CacheUtils.readDistributer(userId, generateRequestEntity(null, null, null, query), readDistributerOperation, jedisUtils, true);
        if(MiscUtils.isEmpty(distributer)){
        	values.put(Constants.SYS_INSERT_DISTRIBUTER, "1");
        }
        values.put("distributer_id", userId);
        String newRqCode = MiscUtils.getUUId();
        values.put("newRqCode",newRqCode);
        
		Map<String,String> roomInfo =  CacheUtils.readDistributerRoom(userId, room_id, readRoomDistributerOperation, jedisUtils,true);		
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
        jedisUtils.getJedis().del(liveRoomDistributeKey,oldRQcodeKey);
        
        distributerRoom = CacheUtils.readDistributerRoom(userId, room_id, readRoomDistributerOperation, jedisUtils);
        //boolean totaldistributerAdd = MiscUtils.convertObjectToLong(distributerRoom.get("create_time")) == MiscUtils.convertObjectToLong(distributerRoom.get("update_time"));
        boolean totaldistributerAdd = true;
        if(totaldistributerAdd){
        	jedis.hincrBy(liveRoomKey, "distributer_num", 1);
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, liveRoomOwner);
            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            if(!jedis.exists(lecturerKey)){
                CacheUtils.readLecturer(userId, generateRequestEntity(null, null, null, map), readLecturerOperation, jedisUtils);
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
        String roomName = CacheUtils.readLiveRoomInfoFromCached(room_id, "room_name", reqEntity, readLiveRoomOperation, jedisUtils,true);
        obj.put("body", String.format(MiscUtils.getConfigByKey("jpush_room_new_distributer"), MiscUtils.RecoveryEmoji(roomName)));
        obj.put("to", values.get("lecturer_id"));
        obj.put("msg_type", "9");
        Map<String,String> extrasMap = new HashMap<>();
        extrasMap.put("msg_type","9");
        extrasMap.put("course_id",room_id);
        obj.put("extras_map", extrasMap);
        JPushHelper.push(obj);
    }
    
    @SuppressWarnings("unchecked")
    @FunctionName("courseStatistics")
    public Map<String, Object> getCourseStatistics(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();                
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());                
        //从讲师信息中加载，数据不存在需加载缓存
        Map<String,String> keyMap = new HashMap<String,String>();
        keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        reqEntity.setParam(keyMap);
        Map<String,String> values = CacheUtils.readLecturer(userId, reqEntity, readLecturerOperation, jedisUtils);
        
        String total_amount_str = (String)values.get("total_amount");
        String course_num_str =  (String)values.get("course_num");
        String total_student_num_str =  (String)values.get("total_student_num");
        
        Map<String,Object> result = new HashMap<String,Object>();
        result.put("total_student_num", total_student_num_str);
        result.put("course_num", course_num_str);
        result.put("total_amount", total_amount_str);
 
        List<Map<String,String>> courseList = getCourseList(userId,(int)reqMap.get("page_count"),(String)reqMap.get("course_id"), (Long)reqMap.get("query_time"), (Long)reqMap.get("position"),true,true);
        result.put("course_list", courseList);        
        return result;
    }
    
    @FunctionName("fanList")
    public Map<String,Object> getFanList(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();                
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String room_id = (String)reqMap.get("room_id");
        
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomOwner = CacheUtils.readLiveRoomInfoFromCached(room_id, "lecturer_id", reqEntity, readLiveRoomOperation, jedisUtils,true);
        String fans_num_str = CacheUtils.readLiveRoomInfoFromCached(room_id, "fans_num", reqEntity, readLiveRoomOperation, jedisUtils,true);
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
        Jedis jedis = jedisUtils.getJedis();

        if (type.equals("1")) {//微信通知ticket
            //刷新component_access_token相关信息
            String expiresTimes = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "expires_in");
            JSONObject jsonObj = null;
            if (expiresTimes != null) { //已经存在accessTokenMap
                long expiresTimeStamp = Long.parseLong(expiresTimes);
                //是否快要超时 令牌是存在有效期（2小时）
                long nowTimeStamp = System.currentTimeMillis();
                if (nowTimeStamp-expiresTimeStamp > 0) {  //如果超时
                    jsonObj = WeiXinUtil.getComponentAccessToken(appidOrTicket);
                }
            } else {//不存在accessTokenMap
                jsonObj = WeiXinUtil.getComponentAccessToken(appidOrTicket);
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
        Jedis jedis = jedisUtils.getJedis();
        String access_token = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "component_access_token");

        JSONObject authJsonObj = WeiXinUtil.getServiceAuthInfo(access_token, auth_code);
        Object errCode = authJsonObj.get("errcode");
        if (errCode != null ) {
            log.error("微信授权回调 获取服务号授权信息失败-----------"+authJsonObj);
            throw new QNLiveException("150001");
        }

        JSONObject authauthorizer_info = authJsonObj.getJSONObject("authorization_info");

        String authorizer_appid = authauthorizer_info.getString("authorizer_appid");

        //获取公众号的头像 昵称 QRCode等相关信息
        JSONObject serviceNoJsonObj = WeiXinUtil.getServiceAuthAccountInfo(access_token, authorizer_appid);
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
            authInfoMap.put("qr_code", authauthorizer_info_base.getString("qrcode_url"));

            //先获取部分信息 还未和直播间绑定起来
            //服务号信息插入数据库
            if (oldServiceInfo != null && oldServiceInfo.size() > 0) {
                authInfoMap.put("update_time", String.valueOf(System.currentTimeMillis()));
                lectureModuleServer.updateServiceNoInfo(authInfoMap);

                //缓存授权信息到jedis
                Map<String,Object> query = new HashMap<String,Object>();
                query.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, oldServiceInfo.get("lecturer_id"));
                String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, query);
                jedis.hmset(serviceNoKey, authInfoMap);

            } else {
                //1 插入的时候 不更新缓存
                //2 更新的时候 更新缓存
                //3 更新的时候分两种情况 授权完成更新缓存
                //4 刷新accessToken的时候 更新缓存
                //5 目前先不缓存
                authInfoMap.put("create_time", String.valueOf(System.currentTimeMillis()));
                lectureModuleServer.insertServiceNoInfo(authInfoMap);
            }
            //重定向成功页面 然后扫码登录 绑定直播间
            result.put("redirectUrl", MiscUtils.getConfigByKey("weixin_pc_no_login_qr_url").replace("APPID", authorizer_appid).replace("APPNAME", URLEncoder.encode(authauthorizer_info_base.getString("nick_name"), "utf-8")));

            log.info("绑定服务号授权成功");
        } else {
            result.put("redirectUrl", MiscUtils.getConfigByKey("service_auth_no_real_service_no"));

            log.info("非认证服务号 绑定失败");
        }
        return result;
    }

    @FunctionName("bindServiceNo")
    public Map<String, Object> bindServiceNo(RequestEntity reqEntity) throws Exception {

        Jedis jedis = jedisUtils.getJedis();

        //获取预授权码pre_auth_code 进入微信平台
        String access_token = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "component_access_token");
        JSONObject jsonObj = WeiXinUtil.getPreAuthCode(access_token);

        //重定向微信URL
        Map<String,Object> result = new HashMap<String,Object>();
        Object errCode = jsonObj.get("errcode");
        if (errCode != null ) {
            throw new QNLiveException("150001");
        } else {
            String pre_auth_code = jsonObj.getString("pre_auth_code");//预授权码 有效期为20分
            result.put("redirectUrl", WeiXinUtil.getServiceAuthUrl(pre_auth_code));
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

        //先取出数据库的信息
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> authInfoMap = lectureModuleServer.findServiceNoInfoByAppid(appid);
        Map<String, String> authInfo = new HashMap<>();
        authInfo.put("authorizer_appid", authInfoMap.get("authorizer_appid").toString());
        authInfo.put("authorizer_access_token", authInfoMap.get("authorizer_access_token").toString());
        authInfo.put("authorizer_refresh_token", authInfoMap.get("authorizer_refresh_token").toString());
        authInfo.put("qr_code", authInfoMap.get("qr_code").toString());
        authInfo.put("expires_time", authInfoMap.get("expires_time").toString());
        authInfo.put("create_time", authInfoMap.get("create_time").toString());
        authInfo.put("update_time", authInfoMap.get("update_time").toString());
        authInfo.put("nick_name", authInfoMap.get("nick_name").toString());
        authInfo.put("head_img", authInfoMap.get("head_img").toString());
        authInfoMap.put("service_type_info", authInfoMap.get("service_type_info").toString());
        //缓存授权信息到jedis
        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, userId);
        String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, query);
        jedis.hmset(serviceNoKey, authInfo);

        if (count < 1) {
            throw new QNLiveException("150001");
        }
    }

    private List<Map<String,String>> getCourseList(String userId,int pageCount,String course_id, Long queryTime, Long postion,
                                                   boolean preDesc, boolean finDesc) throws Exception{
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        Jedis jedis = jedisUtils.getJedis();
        boolean checkPreList = true;
        if(!MiscUtils.isEmpty(course_id)){
            if(jedis.zscore(lecturerCoursesPredictionKey, course_id)==null){
                checkPreList=false;
            }
        }
        List<Map<String,String>> courseList = null;
        if(checkPreList){
            courseList = getCourseOnlyFromCached(jedis, lecturerCoursesPredictionKey, queryTime, null, pageCount, preDesc);
        }
        if(!MiscUtils.isEmpty(courseList)){
            pageCount=pageCount-courseList.size();
            long currentTime = System.currentTimeMillis();
            for(Map<String,String> courseInfo : courseList){
                MiscUtils.courseTranferState(currentTime, courseInfo);
            }
        } else {
            courseList = new LinkedList<Map<String,String>>();
        }
        boolean finExist = false;
        if(pageCount>0){
            String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
            List<Map<String,String>> finishCourse = null;
            if(MiscUtils.isEmpty(courseList)){
                finishCourse = getCourseOnlyFromCached(jedis, lecturerCoursesFinishKey, queryTime, postion,pageCount, finDesc);
            } else {
                finishCourse = getCourseOnlyFromCached(jedis, lecturerCoursesFinishKey, queryTime, postion, pageCount+1, finDesc);
                if(!MiscUtils.isEmpty(finishCourse)){
                    Map<String,String> lastCourseMap = null;
                    Map<String,String> firstCourseMap =null;
                    if(preDesc && finDesc){
                        lastCourseMap = courseList.get(courseList.size()-1);
                        firstCourseMap = finishCourse.get(0);
                        if(MiscUtils.isEqual(lastCourseMap.get("course_id"), firstCourseMap.get("course_id"))){
                            courseList.remove(courseList.size()-1);
                            pageCount=pageCount+1;
                        } else {
                            if(finishCourse.size() > pageCount){
                                finishCourse.remove(finishCourse.size()-1);
                            }
                        }
                    } else if(!preDesc && finDesc){
                        lastCourseMap = courseList.get(0);
                        firstCourseMap = finishCourse.get(0);
                        if(MiscUtils.isEqual(lastCourseMap.get("course_id"), firstCourseMap.get("course_id"))){
                            courseList.remove(0);
                            pageCount=pageCount+1;
                        } else {
                            if(finishCourse.size() > pageCount){
                                finishCourse.remove(finishCourse.size()-1);
                            }
                        }
                    } else {
                        throw new QNLiveException("100030");
                    }
                }
            }
            if(!MiscUtils.isEmpty(finishCourse)){
                pageCount=pageCount-finishCourse.size();
                courseList.addAll(finishCourse);
                finExist=true;
            }
        }

        if(pageCount>0){
            map.clear();
            map.put("pageCount", pageCount);
            map.put("lecturer_id", userId);
            if(!MiscUtils.isEmpty(courseList)){
                Map<String,String> lastCourse = null;
                if(finExist || preDesc){
                    lastCourse = courseList.get(courseList.size()-1);
                } else {
                    lastCourse = courseList.get(0);
                }
                if(finExist){
                    queryTime = Long.parseLong(lastCourse.get("end_time"));
                } else {
                    queryTime = Long.parseLong(lastCourse.get("start_time"));
                }

            }
            if(queryTime != null){
                Date date = new Date(queryTime);
                map.put("startIndex", date);
            }
            List<Map<String,Object>> finishCourse = lectureModuleServer.findCourseListForLecturer(map);
            if(!MiscUtils.isEmpty(finishCourse)){
                boolean checkCourse = !MiscUtils.isEmpty(course_id);
                for(Map<String,Object> finish:finishCourse){
                    if(checkCourse && MiscUtils.isEqual(course_id, finish.get("course_id"))){
                        continue;
                    }
                    Map<String,String> finishMap = new HashMap<String,String>();
                    MiscUtils.converObjectMapToStringMap(finish, finishMap);
                    courseList.add(finishMap);
                }
            }
        }
        
        Map<String,Object> query = new HashMap<String,Object>();
        query.put(Constants.CACHED_KEY_USER_FIELD, userId);
        RequestEntity queryOperation = generateRequestEntity(null, null, null, query);
        CacheUtils.readUser(userId, queryOperation, readUserOperation, jedisUtils);
		
		String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);
        for(Map<String,String> courseInfo:courseList){
        	String courseId = courseInfo.get("course_id");
        	if(jedis.sismember(key, courseId)){
        		courseInfo.put("student", "Y");
        	} else {
        		courseInfo.put("student", "N");
        	}
        }
        
        return courseList;
    }
    
    private List<String> getDistributerCourseListFromSys(String room_id, String distributer_id,String distributeCourseKey,
            final Long start_time,String min_time_key, final int page_count){
        final Map<String, Object> query = new HashMap<String,Object>();
        query.put(Constants.FIELD_ROOM_ID, room_id);
        Jedis jedis = this.jedisUtils.getJedis();        
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
    
    private List<String> getDistributerListFromSys(String room_id, String distributer_id,String distributeKey,String distributeLenKey,final long start_pos,final int page_count, long distributer_num){
        final Map<String, Object> query = new HashMap<String,Object>();
        query.put(Constants.FIELD_ROOM_ID, room_id);
        query.put("position", start_pos);
        if(MiscUtils.isEmpty(distributer_id)){
            distributer_id=null;
        }
        query.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD, distributer_id);
        query.put("limit_count", Constants.MAX_QUERY_LIMIT);
        final boolean ajustPos = start_pos> 0;
        final List<String> result = new LinkedList<String>();
        Jedis jedis = this.jedisUtils.getJedis();
        if(start_pos==0){
            jedis.del(distributeKey);
            jedis.del(distributeLenKey);
        }
        ((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
            @Override
            public void batchOperation(Pipeline pipeline, Jedis jedis) {
                Date curDate = new Date(System.currentTimeMillis());
                List<Map<String,Object>> list = lectureModuleServer.findRoomDistributerInfo(query);
                int count = 0;
                Map<String,Response<Map<String,String>>> roomInfo = new HashMap<String,Response<Map<String,String>>>();
                for(Map<String,Object> value : list){
                	String roomid = (String)value.get("room_id");
                	String distributerId = (String)value.get("distributer_id");
                	Map<String,String> query = new HashMap<String,String>();
            		query.put(Constants.CACHED_KEY_DISTRIBUTER_FIELD,distributerId);
            		query.put(Constants.FIELD_ROOM_ID, roomid);
            		String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM_DISTRIBUTER, query);
            		String roomDistributerId = (String)value.get("room_distributer_id");
            		roomInfo.put(roomDistributerId, pipeline.hgetAll(key));
                }
                pipeline.sync();
                
                for(Map<String,Object> value : list){
                    Date date = (Date)value.get("end_date");
                    if(date!=null && !date.after(curDate)){
                        value.put("effective_time", null);
                    }
                    String roomDistributerId = (String)value.get("room_distributer_id");
                    Response<Map<String,String>> response = roomInfo.get(roomDistributerId);
                    if(response!=null && !MiscUtils.isEmpty(response.get())){
                    	Map<String,String> currentValue = response.get();
                    	value.put("recommend_num", currentValue.get("recommend_num"));
                    	value.put("course_num", currentValue.get("course_num"));
                    	value.put("done_num", currentValue.get("done_num"));
                    	value.put("total_amount", currentValue.get("total_amount"));
                    }
                    long position = 0;
                    if(ajustPos){
                        position= start_pos+1+(count++);                        
                    } else {
                        position= count++;                        
                    }
                    value.put("position", position);
                    String valueStr = CacheUtils.convertMaptoCachedString(value);
                    pipeline.zadd(distributeKey, position, valueStr);
                    if(count <= page_count){
                        result.add(valueStr);
                    }
                }
                pipeline.set(distributeLenKey, distributer_num+":"+(start_pos+list.size()));
                pipeline.expire(distributeKey, 60*60);
                pipeline.expire(distributeLenKey, 60*60);
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
        return CacheUtils.readCourseListInfoOnlyFromCached(jedisUtils, list,readCourseOperation);
    }

	@SuppressWarnings("unchecked")
    @FunctionName("getCustomerService")
    public Map<String, Object> getCustomerService(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String,Object> retMap = new HashMap<>();
        Map<String,Object> customerQrCodeUrl = lectureModuleServer.findCustomerServiceBySystemConfig("customerQrCodeUrl");//获取客服二维码url
        retMap.put("customerQrCodeUrl",customerQrCodeUrl.get("config_value"));

        Map<String,Object> customerPhoneNum = lectureModuleServer.findCustomerServiceBySystemConfig("customerPhoneNum");//获取客服电话
        retMap.put("customerPhoneNum",customerPhoneNum.get("config_value"));

        Map<String,Object> customerTitle = lectureModuleServer.findCustomerServiceBySystemConfig("customerTitle");//标题
        retMap.put("customerTitle",customerTitle.get("config_value"));

        Map<String,Object> cystomerHint = lectureModuleServer.findCustomerServiceBySystemConfig("customerHint");//客服二维码提示
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
        Jedis jedis = jedisUtils.getJedis();
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
        String code = jedis.get(codeKey);
        if(!code.equals(verification_code)){//进行判断
            throw new QNLiveException("130002");
        }
        String phoneKey =  MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_PHONE, phoneMap);//根据userId 拿到 key
        String phone = jedis.get(phoneKey);//拿到电话
        //判断当前用户是否有直播间
        Map<String, Object> roomMap = new HashMap<>();
        roomMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, roomMap);

        if (jedis.exists(lectureLiveRoomKey)) {//有直播间
            Map<String,String> userInfo = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedisUtils);//查找当前用户是否有直播间
            if(MiscUtils.isEmpty(userInfo.get("phone_number"))){//如果没有
                //TODO 把手机号加入user表
                updateUserPhone(phone,userId);
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
            //TODO 把手机号加入user表
            updateUserPhone(phone,userId);
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
    public Map getQrCode(String lectureId,Jedis jedis) {
        Map<String, String> query = new HashMap();
        query.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD, lectureId);
        //1.判断讲师是否有公众号 有就直接返回
        String serviceNoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERVICE_LECTURER, query);
        if (jedis.exists(serviceNoKey)) {//判断当前是否有这个缓存
            query.put("qr_code_url",jedis.hgetAll(serviceNoKey).get("qr_code"));
            query.put("qr_code_title", MiscUtils.getConfigByKey("weixin_qr_code_title_lecturer"));
            query.put("qr_code_message", MiscUtils.getConfigByKey("weixin_qr_code_message"));
            query.put("qr_code_title", MiscUtils.getConfigByKey("weixin_qr_code_ad"));
            return query;
        }
        return null;
    }







}
