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
import qingning.common.util.*;
import qingning.lecture.server.other.ReadCourseOperation;
import qingning.lecture.server.other.ReadLecturerOperation;
import qingning.lecture.server.other.ReadLiveRoomOperation;
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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class LectureServerImpl extends AbstractQNLiveServer {
	private static Logger log = LoggerFactory.getLogger(LectureServerImpl.class);
    private ILectureModuleServer lectureModuleServer;

    private ReadCourseOperation readCourseOperation;
    private ReadLiveRoomOperation readLiveRoomOperation;
    private ReadLecturerOperation readLecturerOperation;
    @Override
    public void initRpcServer() {
        if (lectureModuleServer == null) {
            lectureModuleServer = this.getRpcService("lectureModuleServer");
            readLiveRoomOperation = new ReadLiveRoomOperation(lectureModuleServer);
            readCourseOperation = new ReadCourseOperation(lectureModuleServer);
            readLecturerOperation = new ReadLecturerOperation(lectureModuleServer);
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
        Map<String, Object> createResultMap = lectureModuleServer.createLiveRoom(reqMap);

        //3.缓存修改
        //如果是刚刚成为讲师，则增加讲师信息缓存，并且修改access_token缓存中的身份
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, reqMap.get("user_id").toString());
        String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        if (isLecturer == false) {
            Map<String, Object> lectureObjectMap = lectureModuleServer.findLectureByLectureId(reqMap.get("user_id").toString());
            Map<String, String> lectureStringMap = new HashMap<>();
            MiscUtils.converObjectMapToStringMap(lectureObjectMap, lectureStringMap);
            lectureStringMap.put("nick_name",createResultMap.get("nick_name").toString());

            Map<String, Object> lecturerDistributionObjectMap = lectureModuleServer.findLecturerDistributionByLectureId(reqMap.get("user_id").toString());
            Map<String, String> lecturerDistributionStringMap = new HashMap<>();
            MiscUtils.converObjectMapToStringMap(lecturerDistributionObjectMap, lecturerDistributionStringMap);
            lecturerDistributionStringMap.remove("create_time");
            lecturerDistributionStringMap.remove("update_time");
            lectureStringMap.putAll(lecturerDistributionStringMap);

            //3.1新增讲师缓存
            jedis.hmset(lectureKey, lectureStringMap);

            //3.2修改access_token中的缓存
            jedis.hset(accessTokenKey, "user_role", user_role+","+Constants.USER_ROLE_LECTURER);
        }

        //3.3增加讲师直播间信息缓存
        Map<String, Object> liveRoomObjectMap = lectureModuleServer.findLiveRoomByRoomId(room_id);
        Map<String, String> liveRoomStringMap = new HashMap<>();
        MiscUtils.converObjectMapToStringMap(liveRoomObjectMap, liveRoomStringMap);

        map.clear();
        map.put(Constants.FIELD_ROOM_ID, createResultMap.get("room_id").toString());
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        jedis.hmset(liveRoomKey, liveRoomStringMap);

        //增加讲师直播间对应关系缓存(一对多关系)
        jedis.hset(lectureLiveRoomKey, createResultMap.get("room_id").toString(), "1");

        //增加讲师缓存中的直播间数
        jedis.hincrBy(lectureKey, "live_room_num", 1L);

        resultMap.put("room_id", room_id);
        return resultMap;
    }


    @SuppressWarnings("unchecked")
    @FunctionName("updateLiveRoom")
    public Map<String, Object> updateLiveRoom(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //0.检测是否包含修改信息
        if (reqMap.get("avatar_address") == null && reqMap.get("room_name") == null
                && reqMap.get("room_remark") == null) {
            throw new QNLiveException("100007");
        }

        //1.检测该直播间是否属于修改人
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
        if (liveRoomOwner == null || !liveRoomOwner.equals(userId)) {
            throw new QNLiveException("100002");
        }

        //2.检测更新时间是否与系统一致
        String liveRoomUpdateTime = jedis.hget(liveRoomKey, "update_time");
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
        jedis.hmset(liveRoomKey, updateCacheMap);

        resultMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("liveRoom")
    public Map<String, Object> queryLiveRoomDetail(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        String queryType = reqMap.get("query_type").toString();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String liveRoomListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);

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
                            peocessLiveRoomMap = new HashMap<>();
                            peocessLiveRoomMap.put("avatar_address", liveRoomMap.get("avatar_address"));
                            peocessLiveRoomMap.put("room_name", liveRoomMap.get("room_name"));
                            peocessLiveRoomMap.put("last_course_amount", new BigDecimal(liveRoomMap.get("last_course_amount").toString()));
                            peocessLiveRoomMap.put("fans_num", Long.valueOf(liveRoomMap.get("fans_num").toString()));
                            peocessLiveRoomMap.put("room_id", liveRoomMap.get("room_id"));
                            peocessLiveRoomMap.put("update_time", Long.valueOf(liveRoomMap.get("update_time").toString()));
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

            if(queryType.equals("1")){
                resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
                resultMap.put("room_name", liveRoomMap.get("room_name"));
                resultMap.put("room_remark", liveRoomMap.get("room_remark"));
                resultMap.put("rq_code", liveRoomMap.get("rq_code"));
                resultMap.put("room_address", liveRoomMap.get("room_address"));
                resultMap.put("update_time", Long.valueOf(liveRoomMap.get("update_time")));
                return resultMap;

            }else {
                resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
                resultMap.put("room_name", liveRoomMap.get("room_name"));
                resultMap.put("room_remark", liveRoomMap.get("room_remark"));
                resultMap.put("fans_num", Long.valueOf(liveRoomMap.get("fans_num").toString()));
                resultMap.put("room_address", liveRoomMap.get("room_address"));
                resultMap.put("update_time", Long.valueOf(liveRoomMap.get("update_time").toString()));
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
                
        //0.参数详细校验
        //0:公开课程 1:加密课程 2:收费课程
        //加密课程需要校验是否有密码
/*      if (reqMap.get("course_type").toString().equals("1")) {
            if (reqMap.get("course_password") == null) {
                throw new QNLiveException("000100");
            }
            //收费课程校验是否有价格并且价格是否为非0数字
        } else if (reqMap.get("course_type").toString().equals("2")) {
            if (reqMap.get("course_price") == null) {
                throw new QNLiveException("000100");
            }

            String regex = "^([1-9][0-9]*)+(.[0-9]{1,2})?$";
            if (!Pattern.matches(regex, reqMap.get("course_price").toString())) {
                throw new QNLiveException("100009");
            }
        }*/

        //1.判断直播间是否属于当前讲师
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
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
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, query);
        courseList = jedis.zrangeByScoreWithScores(lecturerCoursesFinishKey, preStartTime+"", nextStartTime+"", 0, 1);
        if(!MiscUtils.isEmpty(courseList)){
        	throw new QNLiveException("100029");
        }
        //2.将课程信息插入到数据库
        reqMap.put("user_id", userId);

        //2.1创建IM 聊天群组
        Map<String,Object> userMap = lectureModuleServer.findLoginInfoByUserId(userId);
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
            reqMap.put("course_url", "http://7xt3lm.com1.z0.glb.clouddn.com/images/A21c457d4bab7640bb5eeb9ee053cbdca.png");//TODO
        }
        Map<String, Object> dbResultMap = lectureModuleServer.createCourse(reqMap);

        //4 修改相关缓存
        //4.1修改讲师个人信息缓存中的课程数 讲师个人信息SYS: lecturer:{lecturer_id}
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lectureKey, "course_num", 1);

        //4.2 修改讲师直播间信息中的课程数  讲师直播间信息SYS: room:{room_id}
        jedis.hincrBy(liveRoomKey, "course_num", 1);

        //4.3 生成该课程缓存 课程基本信息：SYS: course:{course_id}
        map.clear();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, dbResultMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        Map<String, Object> courseObjectMap = lectureModuleServer.findCourseByCourseId(dbResultMap.get("course_id").toString());
        Map<String, String> courseStringMap = new HashMap<String, String>();
        MiscUtils.converObjectMapToStringMap(courseObjectMap, courseStringMap);
        jedis.hmset(courseKey, courseStringMap);

        //4.4 将课程插入到 我的课程列表
        //预告课程列表 SYS: lecturer:{lecturer_id}courses:prediction
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String userCourseList = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        jedis.zadd(userCourseList, Double.parseDouble(courseStringMap.get("start_time").toString()), dbResultMap.get("course_id").toString());

        //4.5 将课程插入到平台课程列表 预告课程列表 SYS:courses:prediction
        String platformCourseList = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
        jedis.zadd(platformCourseList, Double.parseDouble(courseStringMap.get("start_time").toString()), dbResultMap.get("course_id").toString());

        resultMap.put("course_id", dbResultMap.get("course_id").toString());

        //如果该课程为今天内的课程，则调用MQ，将其加入课程超时未开播定时任务中
        Date end = MiscUtils.getEndTimeOfToday();
        if(startTime < end.getTime()){
            RequestEntity mqRequestEntity = new RequestEntity();
            mqRequestEntity.setServerName("MessagePushServer");
            //mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
            mqRequestEntity.setFunctionName("processCourseNotStart");
            Map<String,Object> timerMap = new HashMap<>();
            timerMap.put("course_id", dbResultMap.get("course_id").toString());
            timerMap.put("start_time", startTime + "");
            mqRequestEntity.setParam(timerMap);
            this.mqUtils.sendMessage(mqRequestEntity);
        }

        //向关注者进行极光推送，使用标签进行推送
        JSONObject obj = new JSONObject();
        String roomId = jedis.hget(courseKey,"room_id");
        List<String> followUserIds = lectureModuleServer.findFollowUserIdsByRoomId(roomId);
        if(MiscUtils.isEmpty(followUserIds)){
            return resultMap;
        }
        String roomName = jedis.hget(liveRoomKey,"room_name");
        obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_room_follow_new_course"), roomName,reqMap.get("course_title").toString()));
        obj.put("user_ids", followUserIds);
        obj.put("msg_type","11");
        Map<String,String> extrasMap = new HashMap<>();
        extrasMap.put("msg_type","11");
        extrasMap.put("course_id",dbResultMap.get("course_id").toString());
        obj.put("extras_map", extrasMap);
        JPushHelper.push(obj);//TODO

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("courseDetail")
    public Map<String, Object> getCourseDetail(RequestEntity reqEntity) throws Exception {
    	Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
    	String course_id = (String)reqMap.get("course_id");
    	
        Map<String ,String> courseInfoMap = CacheUtils.readCourse(course_id,reqEntity,readCourseOperation, jedisUtils,false);
        if(MiscUtils.isEmpty(courseInfoMap)){
        	throw new QNLiveException("100004");
        }
        MiscUtils.courseTranferState(System.currentTimeMillis(), courseInfoMap);
        Map<String ,Object> resultMap = new HashMap<>();
        MiscUtils.courseTranferState(System.currentTimeMillis(), courseInfoMap);
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
        resultMap.put("share_url",MiscUtils.getConfigByKey("course_share_url_pre_fix")+reqMap.get("course_id").toString());//TODO
    	List<String> roles = new ArrayList<>();
        roles.add("3");//角色数组  1：普通用户、2：学员、3：讲师
        resultMap.put("roles",roles);

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("updateCourse")
    public Map<String, Object> updateCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();


        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        //0.检查课程是否存在并且状态是否正确。
        // （在缓存中的课程为未结束课程，缓存中有课程则代表课程存在且状态正确）
        if (jedis.exists(courseKey)) {
            //0.1校验课程更新时间
            if (!"2".equals(reqMap.get("status"))){
                String update_time_cache = jedis.hget(courseKey, "update_time");
                if (!MiscUtils.isEqual(update_time_cache, String.valueOf(reqMap.get("update_time")))) {
                    throw new QNLiveException("100011");
                }
            }

            //0.2进一步校验课程状态
            String statusFromCache = jedis.hget(courseKey, "status");
            if (statusFromCache == null || statusFromCache.equals("2")) {
                throw new QNLiveException("100011");
            }

            //0.3校验该课程是否属于该用户
            String courseOwner = jedis.hget(courseKey, "lecturer_id");
            if(courseOwner == null || !userId.equals(courseOwner)){
                throw new QNLiveException("100013");
            }

            //1如果为课程结束
            if ("2".equals(reqMap.get("status"))) {
                //1.1如果为课程结束，则取当前时间为课程结束时间
                //1.2更新课程详细信息(dubble服务)
            	String start_time = jedis.hget(courseKey, "start_time");
            	try{
            		if(System.currentTimeMillis() <= Long.parseLong(start_time)){
            			throw new QNLiveException("100032");
            		}
            	} catch(Exception e){
            		if(e instanceof QNLiveException){
            			throw e;
            		}
            	}
                Date courseEndTime = new Date();
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

                String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
                jedis.zadd(lecturerCoursesFinishKey, (double)courseEndTime.getTime(), reqMap.get("course_id").toString());

                //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移除。如果存在结束课程列表 SYS：courses ：finish，则增加到课程结束列表
                jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, reqMap.get("course_id").toString());
                if(jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)){
                    jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, (double)courseEndTime.getTime(), reqMap.get("course_id").toString());
                }

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
                RequestEntity mqRequestEntity = generateRequestEntity("SaveCourseMessageServer",Constants.MQ_METHOD_ASYNCHRONIZED, null ,reqEntity.getParam());
                this.mqUtils.sendMessage(mqRequestEntity);


                //1.8如果存在课程音频信息，则将课程音频信息使用MQ，保存到数据库
                RequestEntity mqAudioRequestEntity = new RequestEntity();
                mqAudioRequestEntity.setServerName("SaveAudioMessageServer");
                mqAudioRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                mqAudioRequestEntity.setParam(reqEntity.getParam());
                this.mqUtils.sendMessage(mqAudioRequestEntity);

                //1.9如果该课程没有真正开播，并且开播时间在今天之内，则需要取消课程超时未开播定时任务
                if(jedis.hget(courseKey, "real_start_time") == null){
                    String courseStartTime = jedis.hget(courseKey, "start_time");
                    if(Long.parseLong(courseStartTime) < MiscUtils.getEndTimeOfToday().getTime()){
                        RequestEntity timerRequestEntity = generateRequestEntity("MessagePushServer",Constants.MQ_METHOD_ASYNCHRONIZED,"processCourseNotStartCancel",reqEntity.getParam());
                        this.mqUtils.sendMessage(timerRequestEntity);
                    }
                }

                resultMap.put("update_time", courseEndTime.getTime());
                SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
                String str = sdf.format(courseEndTime);
                String courseEndMessage = "直播结束于"+str;
                //发送结束推送消息
                long currentTime = System.currentTimeMillis();
                String mGroupId = jedis.hget(courseKey,"im_course_id");
                String message = courseEndMessage;
                String sender = "system";
                Map<String,Object> infomation = new HashMap<>();
                infomation.put("course_id", reqMap.get("course_id").toString());
                infomation.put("creator_id", userId);
                infomation.put("message", message);
                infomation.put("message_type", "1");
                infomation.put("send_type", "5");//5.结束消息
                infomation.put("create_time", currentTime);
                Map<String,Object> messageMap = new HashMap<>();
                messageMap.put("msg_type","1");
                messageMap.put("send_time",currentTime);
                messageMap.put("information",infomation);
                messageMap.put("mid",MiscUtils.getUUId());
                String content = JSON.toJSONString(messageMap);
                IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);

            } else {
            	if(reqMap.get("start_time") != null){
	                //课程之间需要间隔三十分钟
	        		Map<String,Object> query = new HashMap<String,Object>();
	        		query.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
	                String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, query);
	                Calendar cal = Calendar.getInstance();
	                long startTime = (Long)reqMap.get("start_time");
	                cal.setTimeInMillis(startTime);
	                cal.add(Calendar.MINUTE, -3*Constants.COURSE_MAX_INTERVAL);
	                long preStartTime = cal.getTimeInMillis();
	                cal.setTimeInMillis(startTime);
	                cal.add(Calendar.MINUTE, 3*Constants.COURSE_MAX_INTERVAL);
	                long nextStartTime = cal.getTimeInMillis();
	                Set<Tuple> courseList = jedis.zrangeByScoreWithScores(lecturerCoursesPredictionKey, preStartTime+"", nextStartTime+"", 0, 1);
	                String course_id = (String)reqMap.get("course_id");
	                if(!MiscUtils.isEmpty(courseList)){
	                	for(Tuple tuple:courseList){
	                		if(!course_id.equals(tuple.getElement())){
	                			throw new QNLiveException("100029");
	                		}
	                	}	                	
	                }
	                String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, query);
	                courseList = jedis.zrangeByScoreWithScores(lecturerCoursesFinishKey, preStartTime+"", nextStartTime+"", 0, 1);
	                if(!MiscUtils.isEmpty(courseList)){
	                	for(Tuple tuple:courseList){
	                		if(!course_id.equals(tuple.getElement())){
	                			throw new QNLiveException("100029");
	                		}
	                	}
	                }
            	}            	
                Date now = new Date();
                reqMap.put("now",now);
                //2.不为课程结束
                //修改缓存，同时修改数据库
                Map<String, Object> dbResultMap = lectureModuleServer.updateCourse(reqMap);
                if (dbResultMap == null || dbResultMap.get("update_count") == null || dbResultMap.get("update_count").toString().equals("0")) {
                    throw new QNLiveException("100005");
                }

                Map<String, String> updateCacheMap = new HashMap<String, String>();
                if (reqMap.get("course_title") != null) {
                    updateCacheMap.put("course_title", reqMap.get("course_title").toString());
                }
                if (reqMap.get("course_remark") != null) {
                    updateCacheMap.put("course_remark", reqMap.get("course_remark").toString());
                }
                if (reqMap.get("course_url") != null) {
                    updateCacheMap.put("course_url", reqMap.get("course_url").toString());
                }
                if (reqMap.get("course_password") != null) {
                    updateCacheMap.put("course_password", reqMap.get("course_password").toString());
                }
                if (reqMap.get("start_time") != null) {
                    String newStartTime = reqMap.get("start_time").toString();
                    Date end = MiscUtils.getEndTimeOfToday();
                    //如果原有的课程开播时间为今天，新的课程开播时间为今天，则需要先取消原有定时任务，再新增新的定时任务
                    String originalCourseStartTime = jedis.hget(courseKey, "start_time");
                    if(Long.parseLong(originalCourseStartTime) < end.getTime() && Long.parseLong(newStartTime) < end.getTime()){
                        RequestEntity timerRequestEntity = new RequestEntity();
                        timerRequestEntity.setServerName("MessagePushServer");
                        timerRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                        timerRequestEntity.setFunctionName("processCourseNotStartUpdate");
                        timerRequestEntity.setParam(reqEntity.getParam());
                        this.mqUtils.sendMessage(timerRequestEntity);
                        //如果旧课程时间为今天，新的课程时间为今天之后，则需要取消原有定时任务
                    }else if(Long.parseLong(originalCourseStartTime) < end.getTime() && Long.parseLong(newStartTime) > end.getTime()){
                        RequestEntity timerRequestEntity = new RequestEntity();
                        timerRequestEntity.setServerName("MessagePushServer");
                        timerRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                        timerRequestEntity.setFunctionName("processCourseNotStartCancel");
                        timerRequestEntity.setParam(reqEntity.getParam());
                        this.mqUtils.sendMessage(timerRequestEntity);
                        //如果旧课程时间为今天之后，新课程时间在今天之内，则需要新增定时任务
                    }else if(Long.parseLong(originalCourseStartTime) > end.getTime() && Long.parseLong(newStartTime) < end.getTime()){
                        RequestEntity mqRequestEntity = new RequestEntity();
                        mqRequestEntity.setServerName("MessagePushServer");
                        mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                        mqRequestEntity.setFunctionName("processCourseNotStart");
                        mqRequestEntity.setParam(reqEntity.getParam());
                        this.mqUtils.sendMessage(mqRequestEntity);
                    }

                    updateCacheMap.put("start_time", reqMap.get("start_time").toString());

                    //发送极光推送,通知学员上课时间变更
                    JSONObject obj = new JSONObject();
                    Date newStartTimeDate = new Date(Long.parseLong(newStartTime));
                    String courseTitle = jedis.hget(courseKey,"course_title");
                    String startTimeFormat = MiscUtils.parseDateToFotmatString(newStartTimeDate,"MM月dd日HH:mm");
                    List<String> studentIds = lectureModuleServer.findUserIdsFromStudentsByCourseId(reqMap.get("course_id").toString());
                    obj.put("body", String.format(MiscUtils.getConfigByKey("jpush_course_start_time_modify"), courseTitle,startTimeFormat));
                    obj.put("user_ids", studentIds);
                    obj.put("msg_type", "13");
                    Map<String,String> extrasMap = new HashMap<>();
                    extrasMap.put("msg_type","13");
                    extrasMap.put("course_id",reqMap.get("course_id").toString());
                    obj.put("extras_map", extrasMap);
                    JPushHelper.push(obj);

                }
                updateCacheMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
                jedis.hmset(courseKey, updateCacheMap);
                resultMap.put("update_time", now.getTime());
            }

        } else {
            throw new QNLiveException("100010");
        }

        return resultMap;
    }

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
		String course_num_str =  (String)values.get("course_num");
		Map<String,Object> result = new HashMap<String,Object>();		
		result.put("course_num", course_num_str);
		

        List<Map<String,String>> courseList = getCourseList(userId,(int)reqMap.get("page_count"),(String)reqMap.get("course_id"), 
        		(Long)reqMap.get("query_time"), false, true);
        result.put("course_list", courseList);	
    	
		return result;
    	
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
    }



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
            if(StringUtils.isBlank(status) || !status.equals("1")){
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

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("courseInfo")
    public Map<String, Object> getCourseInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

        //0.校验该课程是否属于该讲师
        String courseOwner = jedis.hget(courseKey, "lecturer_id");
        if(courseOwner == null || !userId.equals(courseOwner)){
            throw new QNLiveException("100013");
        }

        Map<String,String> courseMap = new HashMap<>();
        //1.先检查该课程是否在缓存中
        if(jedis.exists(courseKey)){
            courseMap = jedis.hgetAll(courseKey);
            JSONArray pptList = null;
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String pptListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
            if(jedis.exists(pptListKey)){
                pptList = JSONObject.parseArray(jedis.get(pptListKey));
            }

            List<Map<String,String>> audioObjectMapList = new ArrayList<>();
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
            String audioJsonStringKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, map);
            Set<String> audioIdList = jedis.zrange(audioListKey, 0 , -1);

            //如果存在zsort列表，则从zsort列表中读取
            if(audioIdList != null && audioIdList.size() > 0){
                JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
                callBack.invoke(new JedisBatchOperation(){
                    @Override
                    public void batchOperation(Pipeline pipeline, Jedis jedis) {

                        List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
                        for(String audio : audioIdList){
                            map.put(Constants.FIELD_AUDIO_ID, audio);
                            String audioKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
                            redisResponseList.add(pipeline.hgetAll(audioKey));
                        }
                        pipeline.sync();

                        for(Response<Map<String, String>> redisResponse : redisResponseList){
                            Map<String,String> messageStringMap = redisResponse.get();
                            audioObjectMapList.add(messageStringMap);
                        }
                    }
                });

                resultMap.put("audio_list", audioObjectMapList);

                //如果存在讲课音频的json字符串，则读取讲课音频json字符串
            } else if(jedis.exists(audioJsonStringKey)){
                resultMap.put("audio_list", JSONObject.parse(jedis.get(audioJsonStringKey)));
            }

            if(! CollectionUtils.isEmpty(pptList)){
                resultMap.put("ppt_list", pptList);
            }

            resultMap.put("im_course_id", jedis.hget(courseKey, "im_course_id"));


        }else{
            //2.如果不在缓存中，则查询数据库
            Map<String,Object> courseInfoMap = lectureModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            if(courseInfoMap == null){
                throw new QNLiveException("100004");
            }
            courseMap = new HashMap<>();
            MiscUtils.converObjectMapToStringMap(courseInfoMap,courseMap);

            //查询课程PPT列表
            List<Map<String,Object>> pptList = lectureModuleServer.findPPTListByCourseId(reqMap.get("course_id").toString());

            //查询课程语音列表
            List<Map<String,Object>> audioList = lectureModuleServer.findAudioListByCourseId(reqMap.get("course_id").toString());

            if(! CollectionUtils.isEmpty(pptList)){
                resultMap.put("ppt_list", pptList);
            }

            if(! CollectionUtils.isEmpty(audioList)){
                resultMap.put("audio_list", audioList);
            }

            resultMap.put("im_course_id", courseInfoMap.get("im_course_id"));

        }

        map.clear();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
        Set<String> banUserIdList = jedis.zrange(bandKey, 0 , -1);
        if(banUserIdList != null && banUserIdList.size() > 0){
            resultMap.put("ban_user_id_list", banUserIdList);
        }

        //增加返回课程相应信息
        resultMap.put("student_num",courseMap.get("student_num"));
        resultMap.put("start_time",courseMap.get("start_time"));
        resultMap.put("status",courseMap.get("status"));
        resultMap.put("course_type",courseMap.get("course_type"));
        resultMap.put("course_password",courseMap.get("course_password"));
        resultMap.put("share_url","http://test.qnlive.1758app.com/web/#/nav/living/detail?course_id"+reqMap.get("course_id").toString());//TODO
        resultMap.put("course_update_time",courseMap.get("update_time"));
        resultMap.put("course_title",courseMap.get("course_title"));

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("messageList")
    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();

        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        String queryType = reqMap.get("query_type").toString();

        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String messageListKey;
        Map<String, Object> queryMap = new HashMap<>();

        //queryType为0则查询全部消息，为1则查询提问
        if(queryType.equals("0")){
            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
        }else {
            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_QUESTION, map);
            queryMap.put("send_type", "3");//send_type 类型:0:讲师讲解 1：讲师回答 2 用户评论 3 用户提问
        }

        //缓存不存在则读取数据库中的内容
        if(! jedis.exists(messageListKey)){
            queryMap.put("page_count", pageCount);
            if(reqMap.get("message_pos") != null && StringUtils.isNotBlank(reqMap.get("message_pos").toString())){
                queryMap.put("message_pos", Long.parseLong(reqMap.get("message_pos").toString()));
            }else {
                Map<String,Object> maxInfoMap = lectureModuleServer.findCourseMessageMaxPos(reqMap.get("course_id").toString());
                if(MiscUtils.isEmpty(maxInfoMap)){
                    return resultMap;
                }
                Long maxPos = (Long)maxInfoMap.get("message_pos");
                queryMap.put("message_pos", maxPos);
            }
            queryMap.put("course_id", reqMap.get("course_id").toString());
            List<Map<String,Object>> messageList = lectureModuleServer.findCourseMessageList(queryMap);

            if(! CollectionUtils.isEmpty(messageList)){
                resultMap.put("message_list", messageList);
            }

            return resultMap;

        }else {
            //缓存中存在，则读取缓存中的内容
            //初始化下标
            long startIndex;
            long endIndex;
            Set<String> messageIdList;
            //如果分页的message_id不为空
            if(reqMap.get("message_id") != null && StringUtils.isNotBlank(reqMap.get("message_id").toString())){
                long endRank = jedis.zrank(messageListKey, reqMap.get("message_id").toString());
                endIndex = endRank - 1;
                //判断该列表向上再无信息，如果再无信息，则直接将查询结果列表设置为空
                if(endIndex < 0){
                    startIndex = 0;
                    messageIdList = null;
                }else {
                    startIndex = endIndex - pageCount + 1;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                    messageIdList = jedis.zrange(messageListKey, startIndex, endIndex);
                }

            }else {
                endIndex = -1;
                startIndex = jedis.zcard(messageListKey) - pageCount;
                if(startIndex < 0){
                    startIndex = 0;
                }
                messageIdList = jedis.zrange(messageListKey, startIndex, endIndex);
            }

            if(! CollectionUtils.isEmpty(messageIdList)){
                //缓存中存在则读取缓存内容
                List<Map<String,String>> messageListCache = new ArrayList<>();
                for(String messageId : messageIdList){
                    map.put(Constants.FIELD_MESSAGE_ID, messageId);
                    String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
                    Map<String,String> messageMap = jedis.hgetAll(messageKey);
                    messageMap.put("message_pos", startIndex+"");
                    messageListCache.add(messageMap);
                    startIndex++;
                }

                resultMap.put("message_list", messageListCache);
            }

            return resultMap;
        }


    }


    @SuppressWarnings("unchecked")
    @FunctionName("courseStudents")
    public Map<String, Object> getCourseStudentList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> queryMap = new HashMap<>();
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
            Map<String,Object> courseMap = lectureModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            if(MiscUtils.isEmpty(courseMap)){
                throw new QNLiveException("100004");
            }
            studentNum = courseMap.get("student_num").toString();
        }
        resultMap.put("student_num",studentNum);

        //1.数据来源为缓存
        if( StringUtils.isBlank(reqMap.get("data_source").toString()) || reqMap.get("data_source").toString().equals("1")){
            //1.1先查找缓存中的数据
            String startIndex;
            String endIndex = "-inf";
            if(reqMap.get("student_pos") != null && StringUtils.isNotBlank(reqMap.get("student_pos").toString())){
                startIndex = reqMap.get("student_pos").toString();
            }else {
                startIndex = "+inf";
            }
            Set<String> banUserIdList = jedis.zrevrangeByScore(bandKey, startIndex, endIndex, 0, pageCount);
            List<Map<String,Object>> banUserList;
            List<Map<String,Object>> processBanUserList = new ArrayList<>();

            //1.1.1如果存在禁言列表，则根据禁言列表中的用户id从数据库中查询用户相应信息
            if(banUserIdList != null && banUserIdList.size() > 0){
                Set<Tuple> banUserTupleList = jedis.zrevrangeByScoreWithScores(bandKey, startIndex, endIndex, 0, pageCount);
                banUserList = lectureModuleServer.findBanUserListInfo(banUserIdList);

                DecimalFormat decimalFormat = new DecimalFormat("#");
                for(Tuple tuple : banUserTupleList){
                    for(Map<String,Object> banMap : banUserList){
                        if(tuple.getElement().equals(banMap.get("user_id").toString())){
                            banMap.put("student_pos",decimalFormat.format(tuple.getScore()));
                            banMap.put("ban_status","1");
                            banMap.put("data_source","1");
                            processBanUserList.add(banMap);
                            break;
                        }
                    }
                }

                if(! CollectionUtils.isEmpty(processBanUserList)){
                    studentList.addAll(processBanUserList);
                }
            }

            //1.2缓存中数据不够，则查询数据库中的数据补足
            List<Map<String,Object>> studentListDB;
            if(banUserIdList == null || banUserIdList.size() < pageCount){
                //1.2.1查找被禁言的所有学生用户id列表，查询数据库时对这部分数据进行排除
                Set<String> allBanUserIdList = null;
                if(! startIndex.equals("+inf")){
                    allBanUserIdList = jedis.zrevrangeByScore(bandKey, "+inf", "-inf", 0, pageCount);
                }else {
                    allBanUserIdList = banUserIdList;
                }

                if(banUserIdList != null && banUserIdList.size() > 0){
                    pageCount = pageCount - banUserIdList.size();
                }

                queryMap.put("page_count", pageCount);
                queryMap.put("course_id", reqMap.get("course_id").toString());
                if(allBanUserIdList != null && allBanUserIdList.size() > 0){
                    queryMap.put("all_ban_user_id_list", allBanUserIdList);
                }
                studentListDB = lectureModuleServer.findCourseStudentList(queryMap);

                if(! CollectionUtils.isEmpty(studentListDB)){
                    for(Map<String,Object> banMap : studentListDB){
                        banMap.put("data_source","2");
                    }

                    studentList.addAll(studentListDB);
                }
            }

            if(! CollectionUtils.isEmpty(studentList)){
                resultMap.put("student_list",studentList);
            }
            return resultMap;


        }else {
            //2.数据来源为数据库，则直接查询数据库
            queryMap.put("page_count", pageCount);
            if(reqMap.get("student_pos") != null && StringUtils.isNotBlank(reqMap.get("student_pos").toString())){
                queryMap.put("student_pos", Long.parseLong(reqMap.get("student_pos").toString()));
            }
            queryMap.put("course_id", reqMap.get("course_id").toString());
            Set<String> allBanUserIdList = jedis.zrevrangeByScore(bandKey, "+inf", "-inf", 0, pageCount);
            if(allBanUserIdList != null && allBanUserIdList.size() > 0){
                queryMap.put("all_ban_user_id_list", allBanUserIdList);
            }
            List<Map<String,Object>> studentListDB = lectureModuleServer.findCourseStudentList(queryMap);

            if(! CollectionUtils.isEmpty(studentListDB)){
                for(Map<String,Object> banMap : studentListDB){
                    banMap.put("data_source","2");
                }
                resultMap.put("student_list", studentListDB);
            }

            return resultMap;
        }
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
        List<Map<String,String>> courseList = getCourseList(userId,(int)reqMap.get("page_count"), (String)reqMap.get("course_id"), (Long)reqMap.get("query_time"),true,true);
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
    			        String distributerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_DISTRIBUTER, map);
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
		keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
		reqEntity.setParam(keyMap);
		Map<String,String> values = CacheUtils.readLecturer(userId, reqEntity, readLecturerOperation, jedisUtils);		
        return values;
	}
	
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
        MiscUtils.converObjectMapToStringMap(reqMap, values); 
        
        reqMap.clear();
        reqMap.put(Constants.CACHED_KEY_USER_ROOM_SHARE_FIELD,room_share_code);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOM_SHARE, reqMap);
        jedis.hmset(key, values);
        jedis.expire(key, 60*60*24);
               
        reqMap.clear();        
        reqMap.put("room_share_url", MiscUtils.getConfigByKey("be_distributer_url_pre_fix")+room_share_code);
        Map<String,Object> userMap = lectureModuleServer.findUserInfoByUserId(userId);
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
		String room_id = (String)values.get("room_id");        
		map.clear();		
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String liveRoomOwner = CacheUtils.readLiveRoomInfoFromCached(room_id, "lecturer_id", reqEntity, readLiveRoomOperation, jedisUtils,true);
        if (liveRoomOwner == null) {
            throw new QNLiveException("100002");
        } else if(liveRoomOwner.equals(userId)){
        	throw new QNLiveException("100026");
        }
        values.put("distributer_id", userId);
        lectureModuleServer.createRoomDistributer(values);
        jedis.hincrBy(liveRoomKey, "distributer_num", 1);

        //发送成为新分销员极光推送
        JSONObject obj = new JSONObject();
        obj.put("body", String.format(MiscUtils.getConfigByKey("jpush_room_new_distributer"), values.get("room_name")));
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
		String total_student_num_str =  (String)values.get("live_room_num");
		
		Map<String,Object> result = new HashMap<String,Object>();
		result.put("total_student_num", total_student_num_str);
		result.put("course_num", course_num_str);
		result.put("total_amount", total_amount_str);

        List<Map<String,String>> courseList = getCourseList(userId,(int)reqMap.get("page_count"),null, (Long)reqMap.get("query_time"),true,true);
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
            map.put("position", reqMap.get("position"));
            map.put("page_count", reqMap.get("page_count"));
            
            List<Map<String,Object>> list = lectureModuleServer.findRoomFanList(map);
            result.put("fan_list", list);
        }
		return result;
	}

	private List<Map<String,String>> getCourseList(String userId,int pageCount,String course_id, Long queryTime,
			boolean preDesc, boolean finDesc) throws Exception{
		Map<String,Object> map = new HashMap<String,Object>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        Jedis jedis = jedisUtils.getJedis();
        
        List<Map<String,String>> courseList = getCourseOnlyFromCached(jedis, lecturerCoursesPredictionKey, queryTime, pageCount, preDesc);
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
        		finishCourse = getCourseOnlyFromCached(jedis, lecturerCoursesFinishKey, queryTime, pageCount, finDesc);
        	} else {
        		finishCourse = getCourseOnlyFromCached(jedis, lecturerCoursesFinishKey, queryTime, pageCount+1, finDesc);
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
        						finishCourse.remove(courseList.size()-1);
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
				for(Map<String,Object> value : list){
					Date date = (Date)value.get("end_date");
					if(date!=null && !date.after(curDate)){
						value.put("effective_time", null);
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
					if(count < page_count){
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

    private List<Map<String,String>> getCourseOnlyFromCached(Jedis jedis, String key, Long startTime, int pageCount, boolean desc){
    	Set<Tuple> courseList = null;
    	String startIndex = null;
    	String endIndex = null;
    	if(desc){
    		if (MiscUtils.isEmpty(startTime)) {
    			startIndex = "+inf";
    		} else {
    			startIndex = "(" + startTime;
    		}
    		endIndex = "-inf";
    		courseList = jedis.zrevrangeByScoreWithScores(key, startIndex, endIndex, 0, pageCount);
    	} else {
    		if (MiscUtils.isEmpty(startTime)) {
    			startIndex = "-inf";
    		} else {
    			startIndex = "(" + startTime;
    		}
    		endIndex = "+inf";
    		courseList = jedis.zrangeByScoreWithScores(key, startIndex, endIndex, 0, pageCount);
    	}    	
        List<String> list = new LinkedList<String>();
        for (Tuple tuple : courseList) {            
        	list.add(tuple.getElement());
        }
        return CacheUtils.readCourseListInfoOnlyFromCached(jedisUtils, list);
    }
}
