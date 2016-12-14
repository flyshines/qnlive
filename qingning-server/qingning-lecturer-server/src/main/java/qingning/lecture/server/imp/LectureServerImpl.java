package qingning.lecture.server.imp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.lecture.server.other.ReadCourseOperation;
import qingning.lecture.server.other.ReadLiveRoomOperation;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ILectureModuleServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

public class LectureServerImpl extends AbstractQNLiveServer {

    private ILectureModuleServer lectureModuleServer;

    private ReadCourseOperation readCourseOperation;
    private ReadLiveRoomOperation readLiveRoomOperation;

    @Override
    public void initRpcServer() {
        if (lectureModuleServer == null) {
            lectureModuleServer = this.getRpcService("lectureModuleServer");
        }
        readLiveRoomOperation = new ReadLiveRoomOperation(lectureModuleServer);
        readCourseOperation = new ReadCourseOperation(lectureModuleServer);
    }

    @SuppressWarnings("unchecked")
    @FunctionName("createLiveRoom")
    public Map<String, Object> createLiveRoom(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("room_address", "");//TODO
        //0.目前校验每个讲师仅能创建一个直播间
        //1.缓存中读取直播间信息
        Map<String, Object> map = new HashMap<String, Object>();
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

        reqMap.put("user_role", user_role);
        reqMap.put("user_id", userId);
        Map<String, Object> createResultMap = lectureModuleServer.createLiveRoom(reqMap);

        //3.缓存修改
        //如果是刚刚成为讲师，则增加讲师信息缓存，并且修改access_token缓存中的身份
        if (reqMap.get("user_role") == null || reqMap.get("user_role").toString().split(",").length == 1) {
            Map<String, Object> lectureObjectMap = lectureModuleServer.findLectureByLectureId(reqMap.get("user_id").toString());
            Map<String, String> lectureStringMap = new HashMap<String, String>();
            MiscUtils.converObjectMapToStringMap(lectureObjectMap, lectureStringMap);
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, reqMap.get("user_id").toString());
            String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            jedis.hmset(lectureKey, lectureStringMap);

            jedis.hset(accessTokenKey, "user_role", "normal_user,lecture");
        }

        //增加讲师直播间信息缓存
        String room_id = createResultMap.get("room_id").toString();
        Map<String, Object> liveRoomObjectMap = lectureModuleServer.findLiveRoomByRoomId(room_id);
        Map<String, String> liveRoomStringMap = new HashMap<String, String>();
        MiscUtils.converObjectMapToStringMap(liveRoomObjectMap, liveRoomStringMap);
        map.clear();
        map.put(Constants.FIELD_ROOM_ID, createResultMap.get("room_id").toString());
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        jedis.hmset(liveRoomKey, liveRoomStringMap);

        //增加讲师直播间对应关系缓存(一对多关系)
        jedis.hset(lectureLiveRoomKey, createResultMap.get("room_id").toString(), "1");

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

        //0.参数详细校验
        //0:公开课程 1:加密课程 2:收费课程
        //加密课程需要校验是否有密码
        if (reqMap.get("course_type").toString().equals("1")) {
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
        }

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

        //2.将课程信息插入到数据库
        reqMap.put("user_id", userId);

        //2.1创建IM 聊天群组
        Map<String,Object> userMap = lectureModuleServer.findLoginInfoByUserId(userId);
        Map<String,String> groupMap = IMMsgUtil.createIMGroup(userMap.get("m_user_id").toString());
        if(groupMap == null || StringUtils.isBlank(groupMap.get("groupid"))){
            throw new QNLiveException("100015");
        }
        reqMap.put("im_course_id",groupMap.get("groupid"));
        //2.1.1将讲师加入到该群组中
        IMMsgUtil.joinGroup(groupMap.get("groupid"), userMap.get("m_user_id").toString(), userMap.get("m_user_id").toString());

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
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("courseDetail")
    public Map<String, Object> getCourseDetail(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, String> courseMap = new HashMap<String, String>();
        String room_id = null;

        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        //1.先从缓存中查询课程详情，如果有则从缓存中读取课程详情
        if (jedis.exists(courseKey)) {
            Map<String, String> courseCacheMap = jedis.hgetAll(courseKey);
            courseMap = courseCacheMap;
            room_id = courseCacheMap.get("room_id").toString();

        } else {
            //2.如果缓存中没有课程详情，则读取数据库
            Map<String, Object> courseDBMap = lectureModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());

            //3.如果缓存和数据库中都没有课程详情，则提示课程不存在
            if (CollectionUtils.isEmpty(courseDBMap)) {
                throw new QNLiveException("100004");
            } else {
                MiscUtils.converObjectMapToStringMap(courseDBMap, courseMap);
                room_id = courseDBMap.get("room_id").toString();
            }
        }
        if(CollectionUtils.isEmpty(courseMap)){
            throw new QNLiveException("100004");
        }

        resultMap.put("course_type", courseMap.get("course_type"));
        resultMap.put("status", courseMap.get("status"));
        resultMap.put("course_url", courseMap.get("course_url"));
        resultMap.put("course_title", courseMap.get("course_title"));
        if (courseMap.get("course_price") != null) {
            resultMap.put("course_price", Double.parseDouble(courseMap.get("course_price")));
        }
        resultMap.put("start_time", Long.parseLong(courseMap.get("start_time")));
        resultMap.put("student_num", Long.parseLong(courseMap.get("student_num")));
        resultMap.put("course_remark", courseMap.get("course_remark"));
        resultMap.put("update_time", courseMap.get("update_time"));
        //TODO student_list 参与学生数组

        //从缓存中获取直播间信息
        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(room_id, reqEntity, readLiveRoomOperation, jedisUtils, true);
        resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));
        resultMap.put("room_id", liveRoomMap.get("room_id"));

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("updateCourse")
    public Map<String, Object> updateCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        //0.检查课程是否存在并且状态是否正确。
        // （在缓存中的课程为未结束课程，缓存中有课程则代表课程存在且状态正确）
        if (jedis.exists(courseKey)) {
            //0.1校验课程更新时间
            String update_time_cache = jedis.hget(courseKey, "update_time");
            if (update_time_cache == null || !reqMap.get("update_time").toString().equals(update_time_cache)) {
                throw new QNLiveException("100011");
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
            if (reqMap.get("status") != null && reqMap.get("status").toString().equals("2")) {
                //1.1如果为课程结束，则取当前时间为课程结束时间
                //1.2更新课程详细信息(dubble服务)
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
                String courseStartTime = jedis.hget(courseKey, "start_time");
                jedis.zadd(lecturerCoursesFinishKey, Double.parseDouble(courseStartTime), reqMap.get("course_id").toString());

                //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移除。如果存在结束课程列表 SYS：courses ：finish，则增加到课程结束列表
                jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, reqMap.get("course_id").toString());
                if(jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)){
                    jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, Double.parseDouble(courseStartTime), reqMap.get("course_id").toString());
                }

                //1.5如果课程标记为结束，则清除该课程的禁言缓存数据
                map.put(Constants.CACHED_KEY_COURSE_FIELD, userId);
                String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
                jedis.del(banKey);

                //1.6更新课程缓存信息
                Map<String, String> updateCacheMap = new HashMap<String, String>();
                updateCacheMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
                updateCacheMap.put("status", "2");
                jedis.hmset(courseKey, updateCacheMap);

                //1.7如果存在课程聊天信息，则将聊天信息使用MQ，保存到数据库中
                RequestEntity mqRequestEntity = new RequestEntity();
                mqRequestEntity.setServerName("SaveCourseMessageServer");
                mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                mqRequestEntity.setParam(reqEntity.getParam());
                this.mqUtils.sendMessage(mqRequestEntity);

            } else {
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
                if (reqMap.get("start_time") != null) {
                    updateCacheMap.put("start_time", reqMap.get("start_time").toString());
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
                updateCacheMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
                jedis.hmset(courseKey, updateCacheMap);
            }

        } else {
            throw new Exception("100010");
        }

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("courseList")
    public Map<String, Object> getCourseList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        List<Map<String ,String>> courseResultList = new ArrayList<>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();

        //TODO 目前只有查询讲师的课程列表，查询直播间的课程列表暂未实现
        //if (reqMap.get("room_id") == null || StringUtils.isBlank(reqMap.get("room_id").toString())) {
            Map<String, Object> map = new HashMap<String, Object>();
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

                finishResultMap = findCourseFinishList(jedis, lecturerCoursesFinishKey, startIndex, startIndexDB, endIndex, 0 , pageCount);

            } else {
                if (predictionList.size() < pageCount) {
                    startIndex = "+inf";
                    endIndex = "-inf";
                    finishResultMap = findCourseFinishList(jedis, lecturerCoursesFinishKey, startIndex, null, endIndex, 0 , pageCount - predictionList.size());
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


            if(predictionList != null){
                for (Tuple tuple : predictionList) {
                    ((Map<String, Object>) reqEntity.getParam()).put("course_id",tuple.getElement());
                    Map<String ,String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(),reqEntity,readCourseOperation, jedisUtils,true);
                    courseResultList.add(courseInfoMap);
                }
            }

            if(finishList != null){
                for (Tuple tuple : finishList) {
                    ((Map<String, Object>) reqEntity.getParam()).put("course_id",tuple.getElement());
                    Map<String ,String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(),reqEntity,readCourseOperation, jedisUtils,true);
                    courseResultList.add(courseInfoMap);
                }
            }

            if(dbList != null){
                for (Map<String,Object> courseDBMap : dbList) {
                    Map<String,String> courseDBMapString = new HashMap<>();
                    MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                    courseResultList.add(courseDBMapString);
                }
            }

        //}

        if(! CollectionUtils.isEmpty(courseResultList)){
            resultMap.put("course_list", courseResultList);
        }
        return resultMap;
    }

    Map<String,Object> findCourseFinishList(Jedis jedis, String key,
                                    String startIndexCache, String startIndexDB, String endIndex, Integer limit, Integer count){
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
                dbList = lectureModuleServer.findCourseListForLecturer(queryMap);
            }
        }

        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("finishList",finishList);
        resultMap.put("dbList",dbList);
        return resultMap;
    }

    Map<String,String> findLastElementForRedisSet(Set<Tuple> redisSet){
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
            map.put(Constants.CACHED_KEY_COURSE_PPTS_FIELD, reqMap.get("course_id").toString());
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

        //1.先检查该课程是否在缓存中
        if(jedis.exists(courseKey)){
            JSONArray pptList = null;
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_PPTS_FIELD, reqMap.get("course_id").toString());
            String pptListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
            if(jedis.exists(pptListKey)){
                pptList = JSONObject.parseArray(jedis.get(pptListKey));
            }

            JSONArray audioList = null;
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_AUDIOS_FIELD, reqMap.get("course_id").toString());
            String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
            if(jedis.exists(audioListKey)){
                audioList = JSONObject.parseArray(jedis.get(audioListKey));
            }

            if(! CollectionUtils.isEmpty(pptList)){
                resultMap.put("ppt_list", pptList);
            }

            if(! CollectionUtils.isEmpty(audioList)){
                resultMap.put("audio_list", audioList);
            }

            resultMap.put("im_course_id", jedis.hget(courseKey, "im_course_id"));


        }else{
            //2.如果不在缓存中，则查询数据库
            Map<String,Object> courseInfoMap = lectureModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            if(courseInfoMap == null){
                throw new QNLiveException("100004");
            }

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
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("messageList")
    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();

        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());

        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);

        //初始化下标
        long startIndex;
        long endIndex;
        if(reqMap.get("message_pos") != null && StringUtils.isNotBlank(reqMap.get("message_pos").toString())){
            startIndex = Long.parseLong(reqMap.get("message_pos").toString()) + 1;
        }else {
            startIndex = 0L;
        }
        endIndex = startIndex + pageCount - 1;

        Set<String> messageIdList = jedis.zrange(messageListKey, startIndex, endIndex);
        //缓存不存在则读取数据库中的内容
        if(messageIdList == null || messageIdList.size() == 0){
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("page_count", pageCount);
            if(reqMap.get("message_pos") != null && StringUtils.isNotBlank(reqMap.get("message_pos").toString())){
                queryMap.put("message_pos", Long.parseLong(reqMap.get("message_pos").toString()));
            }
            queryMap.put("course_id", reqMap.get("course_id").toString());
            List<Map<String,Object>> messageList = lectureModuleServer.findCourseMessageList(queryMap);

            if(! CollectionUtils.isEmpty(messageList)){
                resultMap.put("message_list", messageList);
            }

            return resultMap;

        }else {
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


}
