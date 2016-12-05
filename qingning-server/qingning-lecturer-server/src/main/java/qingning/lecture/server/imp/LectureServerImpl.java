package qingning.lecture.server.imp;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.CacheUtils;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.lecture.server.other.ReadCourseOperation;
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

    @Override
    public void initRpcServer() {
        if (lectureModuleServer == null) {
            lectureModuleServer = this.getRpcService("lectureModuleServer");
        }

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

        //0.查询缓存中是否存在key，存在则将缓存中的信息返回
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        Jedis jedis = jedisUtils.getJedis();
        Map<String, String> liveRoomMap = null;

        //1.如果缓存中不存在则查询数据库
        if (!jedis.exists(liveRoomKey)) {
            Map<String, Object> dbResultMap = lectureModuleServer.findLiveRoomByRoomId(reqMap.get("room_id").toString());

            //2.如果缓存和数据库中均不存在，则返回直播间不存在
            if (CollectionUtils.isEmpty(dbResultMap)) {
                throw new QNLiveException("100002");
            } else {
                //1.1如果数据库中查询出数据，将查询出的结果放入缓存
                Map<String, String> liveRoomStringMap = new HashMap<String, String>();
                MiscUtils.converObjectMapToStringMap(dbResultMap, liveRoomStringMap);
                jedis.hmset(liveRoomKey, liveRoomStringMap);

                liveRoomMap = liveRoomStringMap;
            }

        } else {
            //缓存中有数据
            liveRoomMap = jedis.hgetAll(liveRoomKey);
        }


        String queryType = reqMap.get("query_type").toString();

        switch (queryType) {
            case "0":
                resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
                resultMap.put("room_name", liveRoomMap.get("room_name"));
                resultMap.put("last_course_amount", new BigDecimal(liveRoomMap.get("last_course_amount").toString()));
                resultMap.put("fans_num", Long.valueOf(liveRoomMap.get("fans_num").toString()));
                break;
            case "1":
                resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
                resultMap.put("room_name", liveRoomMap.get("room_name"));
                resultMap.put("room_remark", liveRoomMap.get("room_remark"));
                resultMap.put("rq_code", liveRoomMap.get("rq_code"));
                resultMap.put("room_address", liveRoomMap.get("room_address"));
                break;
            case "2":
                resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
                resultMap.put("room_name", liveRoomMap.get("room_name"));
                resultMap.put("room_remark", liveRoomMap.get("room_remark"));
                resultMap.put("fans_num", Long.valueOf(liveRoomMap.get("fans_num").toString()));
                break;
        }

        return resultMap;
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
        //TODO student_list 参与学生数组

        //从缓存中获取直播间信息
        map.clear();
        map.put(Constants.FIELD_ROOM_ID, room_id);
        String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        Map<String, String> liveRoomMap = jedis.hgetAll(liveRoomKey);
        resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));

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

            String statusFromCache = jedis.hget(courseKey, "status");
            if (statusFromCache == null || statusFromCache.equals("2")) {
                throw new QNLiveException("100011");
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

                //1.4将该课程从平台的预告课程列表 SYS：courses  ：prediction移动到结束课程列表 SYS：courses ：finish
                jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, reqMap.get("course_id").toString());
                jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, Double.parseDouble(courseStartTime), reqMap.get("course_id").toString());

                //1.5如果课程标记为结束，则清除该课程的禁言缓存数据
                //TODO

                //1.6更新课程缓存信息
                Map<String, String> updateCacheMap = new HashMap<String, String>();
                updateCacheMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
                updateCacheMap.put("status", "2");
                jedis.hmset(courseKey, updateCacheMap);

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


}
