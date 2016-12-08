package qingning.user.server.imp;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.CacheUtils;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.IUserModuleServer;
import qingning.user.server.other.ReadCourseOperation;
import qingning.user.server.other.ReadLiveRoomOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.text.DecimalFormat;
import java.util.*;

public class UserServerImpl extends AbstractQNLiveServer {

    private IUserModuleServer userModuleServer;

    private ReadLiveRoomOperation readLiveRoomOperation;
    private ReadCourseOperation readCourseOperation;

    @Override
    public void initRpcServer() {
        if (userModuleServer == null) {
            userModuleServer = this.getRpcService("userModuleServer");

            readLiveRoomOperation = new ReadLiveRoomOperation(userModuleServer);
            readCourseOperation = new ReadCourseOperation(userModuleServer);
        }
    }

    @SuppressWarnings("unchecked")
    @FunctionName("userFollowRoom")
    public Map<String, Object> userFollowRoom(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        //1.更新数据库中关注表的状态
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        //查询直播间是否存在 //TODO 需要进一步优化
        if (!jedis.exists(roomKey)) {
            throw new QNLiveException("100002");
        }
        String lecturerId = jedis.hget(roomKey, "lecturer_id");
        reqMap.put("lecturer_id", lecturerId);


        Map<String, Object> dbResultMap = userModuleServer.userFollowRoom(reqMap);
        if (dbResultMap == null || dbResultMap.get("update_count") == null || dbResultMap.get("update_count").toString().equals("0")) {
            throw new QNLiveException("110003");
        }

        //2.更新用户表中的关注数
        userModuleServer.updateLiveRoomNumForUser(reqMap);

        //3.延长用户缓存时间
        map.clear();
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String userKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, map);
        jedis.expire(userKey, 10800);

        //4.更新用户缓存中直播间的关注数
        //关注操作类型 0关注 1不关注
        Integer incrementNum = null;
        if (reqMap.get("follow_type").toString().equals("0")) {
            incrementNum = 1;
        } else {
            incrementNum = -1;
        }
        jedis.hincrBy(userKey, "live_room_num", incrementNum);

        //5.更新直播间缓存的粉丝数
        jedis.hincrBy(roomKey, "fans_num", incrementNum);

        //6.更新讲师缓存的粉丝数
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "fans_num", incrementNum);

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("userCourses")
    public Map<String, Object> getCourses(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();

        //查询类型 1 查询我加入的课程  2 查询平台所有的课程列表
        if (reqMap.get("query_type").toString().equals("1")) {
            return getUserJoinCourses(reqEntity);
        } else if (reqMap.get("query_type").toString().equals("2")) {
            return getPlatformCourses(reqEntity);
        }
        return null;
    }

    /**
     * 查询用户加入的课程
     *
     * @param reqEntity
     * @return
     */
    private Map<String, Object> getUserJoinCourses(RequestEntity reqEntity) {
        return null;
    }


    /**
     * 查询平台所有的课程
     *
     * @param reqEntity
     * @return
     */
    private Map<String, Object> getPlatformCourses(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        //进一步参数校验，传递了分页使用的课程id，则需要同时传递课程状态和数据来源信息
        if(StringUtils.isNotBlank(reqMap.get("course_id").toString())){
            if(StringUtils.isBlank(reqMap.get("status").toString()) || StringUtils.isBlank(reqMap.get("data_source").toString()) || StringUtils.isBlank(reqMap.get("start_time").toString())){
                throw new QNLiveException("120004");
            }
        }

        //1.如果课程id为空，则进行初始化查询，仅仅查询缓存即可
        Jedis jedis = jedisUtils.getJedis();
        if (reqMap.get("course_id") == null || StringUtils.isBlank(reqMap.get("course_id").toString())) {

            List<Map<String, String>> courseResultList = new ArrayList<>();
            String startIndex = "+inf";
            String endIndex = "-inf";
            int pageCount = Integer.parseInt(reqMap.get("page_count").toString());

            //1.1先查询直播列表
            Set<Tuple> liveList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_LIVE, startIndex, endIndex, 0, pageCount);
            Set<Tuple> predictionList = null;
            Set<Tuple> finishList = null;
            List<Map<String, Object>> dbList = new ArrayList<>();

            //1.2直播列表为空或者不足，则继续查询预告列表
            if (liveList == null || liveList.size() < pageCount) {
                if (liveList != null) {
                    pageCount = pageCount - liveList.size();
                }
                String startIndexPrediction = "-inf";
                String endIndexPrediction = "+inf";
                predictionList = jedis.zrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, startIndexPrediction, endIndexPrediction, 0, pageCount);

                //1.3预告列表为空或者不足，则继续查询结束列表
                if (predictionList == null || predictionList.size() < pageCount) {
                    if (predictionList != null) {
                        pageCount = pageCount - predictionList.size();
                    }

                    String startIndexFinish = "+inf";
                    String endIndexFinish = "-inf";

                    finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexFinish, 0, pageCount);

                    if(finishList == null || finishList.size() < pageCount){
                        if(finishList != null){
                            pageCount = pageCount - finishList.size();
                        }
                        //直接查询数据库中的结束课程
                        Map<String, Object> queryMap = new HashMap<>();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "2");
                        queryMap.put("pageCount", pageCount);
                        if(finishList != null && finishList.size() > 0){
                            Date queryDate = new Date(Long.parseLong(findLastElementForRedisSet(finishList).get("startIndexDB")));
                            queryMap.put("startIndex", queryDate);
                        }
                        dbList = userModuleServer.findCourseListForLecturer(queryMap);
                    }
                }
            }

            if (liveList != null) {
                for (Tuple tuple : predictionList) {
                    ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                    Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                    courseInfoMap.put("data_source", "1");
                    courseResultList.add(courseInfoMap);
                }
            }

            if (predictionList != null) {
                for (Tuple tuple : predictionList) {
                    ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                    Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                    courseInfoMap.put("data_source", "1");
                    courseResultList.add(courseInfoMap);
                }
            }

            if (finishList != null) {
                for (Tuple tuple : finishList) {
                    ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                    Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                    courseInfoMap.put("data_source", "1");
                    courseResultList.add(courseInfoMap);
                }
            }

            if (dbList != null) {
                for (Map<String, Object> courseDBMap : dbList) {
                    Map<String, String> courseDBMapString = new HashMap<>();
                    MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                    courseDBMapString.put("data_source", "2");
                    courseResultList.add(courseDBMapString);
                }
            }

            if (!CollectionUtils.isEmpty(courseResultList)) {
                resultMap.put("course_list", courseResultList);
            }
            return resultMap;

        } else {
            //2.如果课程id不为空，则进行分页查询
            //数据来源：1：缓存 2.：数据库
            List<Map<String, String>> courseResultList = new ArrayList<>();

            //从缓存中查询
            if (reqMap.get("data_source").toString().equals("1")) {

                Set<Tuple> liveList = null;
                Set<Tuple> predictionList = null;
                Set<Tuple> finishList = null;
                List<Map<String, Object>> dbList = new ArrayList<>();
                List<Map<String, Object>> finishDbList = new ArrayList<>();
                List<Map<String, Object>> predictionListDbList = new ArrayList<>();
                Map<String,Object> queryResultMap = new HashMap<>();

                int pageCount = Integer.parseInt(reqMap.get("page_count").toString());


                //课程状态 1:预告（对应为数据库中的已发布） 2:已结束 4:直播中
                if (reqMap.get("status").toString().equals("4")) {
                    String startIndex = reqMap.get("start_time").toString();
                    String endIndex = "-inf";

                    //1.1先查询直播列表
                    liveList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_LIVE, startIndex, endIndex, 0, pageCount);

                    //1.2直播列表为空或者不足，则继续查询预告列表
                    if (liveList == null || liveList.size() < pageCount) {
                        if (liveList != null) {
                            pageCount = pageCount - liveList.size();
                        }

                       queryResultMap = findCoursesStartWithPrediction(jedis, "-inf", "+inf", pageCount);

                    }

                } else if (reqMap.get("status").toString().equals("1")) {
                    queryResultMap = findCoursesStartWithPrediction(jedis, reqMap.get("start_time").toString(), "+inf", pageCount);

                } else {
                    String startIndexFinish =  reqMap.get("start_time").toString();
                    String endIndexFinish = "-inf";
                    finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexFinish, 0, pageCount);

                    if(finishList == null || finishList.size() < pageCount){
                        Map<String, Object> queryMap = new HashMap<>();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "2");
                        queryMap.put("pageCount", pageCount);
                        dbList = userModuleServer.findCourseListForLecturer(queryMap);
                    }
                }


                if(! CollectionUtils.isEmpty(queryResultMap)){
                    if(queryResultMap.get("predictionList") != null){
                        predictionList = (Set<Tuple>)queryResultMap.get("predictionList");
                    }

                    if(queryResultMap.get("finishList") != null){
                        finishList = (Set<Tuple>)queryResultMap.get("finishList");
                    }

                    if(queryResultMap.get("dbList") != null){
                        dbList = (List<Map<String,Object>>)queryResultMap.get("dbList");
                    }
                }


                if (liveList != null) {
                    for (Tuple tuple : predictionList) {
                        ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                        Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                        courseInfoMap.put("data_source", "1");
                        courseResultList.add(courseInfoMap);
                    }
                }

                if (predictionList != null) {
                    for (Tuple tuple : predictionList) {
                        ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                        Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                        courseInfoMap.put("data_source", "1");
                        courseResultList.add(courseInfoMap);
                    }
                }

                if (finishList != null) {
                    for (Tuple tuple : finishList) {
                        ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                        Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                        courseInfoMap.put("data_source", "1");
                        courseResultList.add(courseInfoMap);
                    }
                }

                if (dbList != null) {
                    for (Map<String, Object> courseDBMap : dbList) {
                        Map<String, String> courseDBMapString = new HashMap<>();
                        MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                        courseDBMapString.put("data_source", "2");
                        courseResultList.add(courseDBMapString);
                    }
                }

                if (!CollectionUtils.isEmpty(courseResultList)) {
                    resultMap.put("course_list", courseResultList);
                }


                return resultMap;


                //从数据库中查询
            } else {
                List<Map<String, Object>> dbList = new ArrayList<>();
                int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
                //预告中
                if(reqMap.get("status").equals("1")){
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "1");
                    queryMap.put("status", "1");
                    queryMap.put("pageCount", pageCount);
                    Date start_time = new Date(Long.parseLong(reqMap.get("start_time").toString()));
                    queryMap.put("startIndex", start_time);
                    dbList = userModuleServer.findCourseListForLecturer(queryMap);
                }else {
                    //已经结束
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "2");
                    queryMap.put("status", "2");
                    queryMap.put("pageCount", pageCount);
                    Date start_time = new Date(Long.parseLong(reqMap.get("start_time").toString()));
                    queryMap.put("startIndex", start_time);
                    dbList = userModuleServer.findCourseListForLecturer(queryMap);
                }

                if(! CollectionUtils.isEmpty(dbList)){
                    for (Map<String, Object> courseDBMap : dbList) {
                        Map<String, String> courseDBMapString = new HashMap<>();
                        MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                        courseDBMapString.put("data_source", "2");
                        courseResultList.add(courseDBMapString);
                    }
                }

                if (!CollectionUtils.isEmpty(courseResultList)) {
                    resultMap.put("course_list", courseResultList);
                }


                return resultMap;

            }

        }

    }

    private Map<String, Object> findCoursesStartWithPrediction(Jedis jedis, String startIndex, String endIndex, Integer pageCount) {
        Map<String, Object> resultMap = new HashMap<>();
        Set<Tuple> predictionList = null;
        Set<Tuple> finishList = null;
        List<Map<String, Object>> dbList = new ArrayList<>();
        List<Map<String, Object>> finishDbList = new ArrayList<>();
        List<Map<String, Object>> predictionListDbList = new ArrayList<>();

        String startIndexPrediction = startIndex;
        String endIndexPrediction = endIndex;
        predictionList = jedis.zrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, startIndexPrediction, endIndexPrediction, 0, pageCount);

        //1.3预告列表为空或者不足，则继续查询结束列表
        if (predictionList == null || predictionList.size() < pageCount) {
            if (predictionList != null) {
                pageCount = pageCount - predictionList.size();
            }

            if (predictionList == null) {
                if (jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)) {
                    //读结束列表，结束列表不足，则读取数据库,直接查询数据库中结束状态的课程
                    String startIndexFinish = "+inf";
                    String endIndexFinish = "-inf";
                    finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexFinish, 0, pageCount);

                    if (finishList.size() < pageCount) {
                        Map<String, Object> queryMap = new HashMap<>();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "2");
                        queryMap.put("pageCount", pageCount - finishList.size());
                        dbList = userModuleServer.findCourseListForLecturer(queryMap);
                    }
                } else {
                    //读取数据库，直接查询结束状态课程
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "2");
                    queryMap.put("status", "2");
                    queryMap.put("pageCount", pageCount);
                    dbList = userModuleServer.findCourseListForLecturer(queryMap);
                }
            } else {
                if (jedis.exists(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH)) {
                    //读结束列表，结束列表不足，则读取数据库,直接查询数据库中结束状态的课程
                    String startIndexFinish = "+inf";
                    String endIndexFinish = "-inf";
                    finishList = jedis.zrevrangeByScoreWithScores(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH, startIndexFinish, endIndexFinish, 0, pageCount);

                    if (finishList.size() < pageCount) {
                        Map<String, Object> queryMap = new HashMap<>();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "2");
                        queryMap.put("pageCount", pageCount - finishList.size());
                        dbList = userModuleServer.findCourseListForLecturer(queryMap);
                    }

                } else {
                    //查询数据库，先读未结束的课程，再读取已经结束的课程
                    Map<String, Object> queryMap = new HashMap<>();
                    queryMap.put("orderType", "1");
                    queryMap.put("status", "1");
                    queryMap.put("pageCount", pageCount);
                    predictionListDbList = userModuleServer.findCourseListForLecturer(queryMap);

                    if (CollectionUtils.isEmpty(dbList) || dbList.size() < pageCount) {
                        if (!CollectionUtils.isEmpty(dbList)) {
                            pageCount = pageCount - dbList.size();
                        }

                        queryMap.clear();
                        queryMap.put("orderType", "2");
                        queryMap.put("status", "1");
                        queryMap.put("pageCount", pageCount);
                        finishDbList = userModuleServer.findCourseListForLecturer(queryMap);

                        if (!CollectionUtils.isEmpty(finishDbList)) {
                            if (CollectionUtils.isEmpty(predictionListDbList)) {
                                dbList = finishDbList;
                            } else {
                                dbList.addAll(predictionListDbList);
                                dbList.addAll(finishDbList);
                            }
                        }
                    }
                }
            }
        }

        resultMap.put("predictionList", predictionList);
        resultMap.put("finishList", finishList);
        resultMap.put("dbList", dbList);
        return resultMap;

}


    /**
     * 查询直播间基本信息
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("roomInfo")
    public Map<String, Object> getRoomInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        //查询出直播间基本信息
        Map<String, String> infoMap = CacheUtils.readLiveRoom(reqMap.get("room_id").toString(), reqEntity, readLiveRoomOperation, jedisUtils, true);
        if (CollectionUtils.isEmpty(infoMap)) {
            throw new QNLiveException("100002");
        }

        //查询关注信息 //TODO 先查询数据库，后续确认是否查询缓存
        //关注状态 0未关注 1已关注
        Map<String, Object> fansMap = userModuleServer.findFansByFansKey(reqMap);
        if (CollectionUtils.isEmpty(fansMap)) {
            resultMap.put("follow_status", "0");
        } else {
            resultMap.put("follow_status", "1");
        }

        resultMap.put("avatar_address", infoMap.get("avatar_address"));
        resultMap.put("room_name", infoMap.get("room_name"));
        resultMap.put("room_remark", infoMap.get("room_remark"));
        resultMap.put("fans_num", infoMap.get("fans_num"));

        return resultMap;
    }


    /**
     * 查询课程详情
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("courseDetailInfo")
    public Map<String, Object> getCourseDetailInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, String> courseMap = new HashMap<String, String>();
        String room_id = null;
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

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
            Map<String, Object> courseDBMap = userModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());

            //3.如果缓存和数据库中都没有课程详情，则提示课程不存在
            if (CollectionUtils.isEmpty(courseDBMap)) {
                throw new QNLiveException("100004");
            } else {
                MiscUtils.converObjectMapToStringMap(courseDBMap, courseMap);
                room_id = courseDBMap.get("room_id").toString();
            }
        }
        if (CollectionUtils.isEmpty(courseMap)) {
            throw new QNLiveException("100004");
        }

        resultMap.put("course_type", courseMap.get("course_type"));
        resultMap.put("course_status", courseMap.get("status"));
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
        Map<String, String> liveRoomMap = CacheUtils.readLiveRoom(room_id, reqEntity, readLiveRoomOperation, jedisUtils, true);
        resultMap.put("room_avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));

        //TODO 查询加入课程状态

        //查询关注状态
        //关注状态 0未关注 1已关注
        reqMap.put("room_id", liveRoomMap.get("room_id"));
        Map<String, Object> fansMap = userModuleServer.findFansByFansKey(reqMap);
        if (CollectionUtils.isEmpty(fansMap)) {
            resultMap.put("follow_status", "0");
        } else {
            resultMap.put("follow_status", "1");
        }

        return resultMap;
    }


    @SuppressWarnings("unchecked")
    @FunctionName("roomCourses")
    public Map<String, Object> getRoomCourses(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);
        List<Map<String, String>> courseResultList = new ArrayList<>();

        //目前一个讲师仅能创建一个直播间，所以查询的是该讲师发布的课程
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String lecturerId = jedis.hget(roomKey, "lecturer_id");

        //TODO 目前只有查询讲师的课程列表，查询直播间的课程列表暂未实现
        //if (reqMap.get("room_id") == null || StringUtils.isBlank(reqMap.get("room_id").toString())) {
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
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
        List<Map<String, Object>> dbList = null;
        Map<String, Object> finishResultMap = new HashMap<>();

        if (predictionList == null || predictionList.isEmpty()) {
            if (startIndex.equals("0")) {
                endIndex = "-inf";
                startIndex = "+inf";
                startIndexDB = null;
            } else {
                endIndex = "-inf";
                startIndexDB = startIndex;
            }

            finishResultMap = findCourseFinishList(jedis, lecturerCoursesFinishKey, startIndex, startIndexDB, endIndex, 0, pageCount);

        } else {
            if (predictionList.size() < pageCount) {
                startIndex = "+inf";
                endIndex = "-inf";
                finishResultMap = findCourseFinishList(jedis, lecturerCoursesFinishKey, startIndex, null, endIndex, 0, pageCount - predictionList.size());
            }
        }

        //未结束课程列表，结束课程列表，数据库课程列表三者拼接到最终的课程结果列表，即可得到结果
        //分别迭代这三个列表
        if (!CollectionUtils.isEmpty(finishResultMap)) {
            if (finishResultMap.get("finishList") != null) {
                finishList = (Set<Tuple>) finishResultMap.get("finishList");
            }

            if (finishResultMap.get("dbList") != null) {
                dbList = (List<Map<String, Object>>) finishResultMap.get("dbList");
            }
        }


        if (predictionList != null) {
            for (Tuple tuple : predictionList) {
                ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                courseResultList.add(courseInfoMap);
            }
        }

        if (finishList != null) {
            for (Tuple tuple : finishList) {
                ((Map<String, Object>) reqEntity.getParam()).put("course_id", tuple.getElement());
                Map<String, String> courseInfoMap = CacheUtils.readCourse(tuple.getElement(), reqEntity, readCourseOperation, jedisUtils, true);
                courseResultList.add(courseInfoMap);
            }
        }

        if (dbList != null) {
            for (Map<String, Object> courseDBMap : dbList) {
                Map<String, String> courseDBMapString = new HashMap<>();
                MiscUtils.converObjectMapToStringMap(courseDBMap, courseDBMapString);
                courseResultList.add(courseDBMapString);
            }
        }

        //}

        if (!CollectionUtils.isEmpty(courseResultList)) {
            resultMap.put("course_list", courseResultList);
        }

        return resultMap;
    }

    Map<String, Object> findCourseFinishList(Jedis jedis, String key,
                                             String startIndexCache, String startIndexDB, String endIndex, Integer limit, Integer count) {
        Set<Tuple> finishList = jedis.zrevrangeByScoreWithScores(key, startIndexCache, endIndex, limit, count);
        Map<String, Object> queryMap = new HashMap<>();
        List<Map<String, Object>> dbList = null;

        //如果结束课程列表为空，则查询数据库
        if (finishList == null || finishList.isEmpty()) {
            queryMap.put("pageCount", count);
            if (startIndexDB != null) {
                Date date = new Date(Long.parseLong(startIndexDB.substring(1)));
                queryMap.put("startIndex", date);
            }
            dbList = userModuleServer.findCourseListForLecturer(queryMap);
        } else {
            //如果结束课程列表中的数量不够，则剩余需要查询数据库
            if (finishList.size() < count) {
                startIndexDB = findLastElementForRedisSet(finishList).get("startIndexDB");
                queryMap.put("pageCount", count - finishList.size());
                if (startIndexDB != null) {
                    Date date = new Date(Long.parseLong(startIndexDB));
                    queryMap.put("startIndex", date);
                }
                dbList = userModuleServer.findCourseListForLecturer(queryMap);
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("finishList", finishList);
        resultMap.put("dbList", dbList);
        return resultMap;
    }

    Map<String, String> findLastElementForRedisSet(Set<Tuple> redisSet) {
        Map<String, String> resultMap = new HashMap<>();
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
    @FunctionName("joinCourse")
    public Map<String, Object> joinCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

        Map<String,String> courseMap = new HashMap<>();
        //1.先检测课程存在情况和状态
        //1.1如果课程在缓存中
        if(jedis.exists(courseKey)){
            courseMap = jedis.hgetAll(courseKey);
        }else {
            //1.2如果课程在数据库中
            Map<String,Object> courseObjectMap = userModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            if(courseObjectMap == null){
                throw new QNLiveException("100004");
            }
            MiscUtils.converObjectMapToStringMap(courseObjectMap, courseMap);
        }

        //2.检测课程验证信息是否正确
        //2.1如果课程为私密课程则检验密码
        if(courseMap.get("course_type").equals("1")){
            if(reqMap.get("course_password") == null || StringUtils.isBlank(reqMap.get("course_password").toString())){
                throw new QNLiveException("000100");
            }
            if(! reqMap.get("course_password").toString().equals(courseMap.get("course_password").toString())){
                throw new QNLiveException("120006");
            }
        }else if(courseMap.get("course_type").equals("2")){
            //TODO 支付课程要验证支付信息
            if(reqMap.get("payment_id") == null){
                throw new QNLiveException("000100");
            }
        }

        //3.检测学生是否参与了该课程
        Map<String,Object> studentQueryMap = new HashMap<>();
        studentQueryMap.put("user_id",userId);
        studentQueryMap.put("lecturer_id",courseMap.get("lecturer_id"));
        studentQueryMap.put("room_id",courseMap.get("room_id"));
        studentQueryMap.put("course_id",courseMap.get("course_id"));
        Map<String,Object> studentMap = userModuleServer.findStudentByKey(studentQueryMap);
        if(studentMap != null){
            throw new QNLiveException("100004");
        }

        //3.将学员信息插入到学员参与表中
        courseMap.put("user_id",userId);
        Map<String,Object> insertResultMap = userModuleServer.joinCourse(courseMap);

        //4.修改讲师缓存中的课程参与人数
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseMap.get("lecturer_id").toString());
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "total_student_num", 1);

        if(jedis.exists(courseKey)){
            jedis.hincrBy(courseKey, "student_num", 1);
        }else {
            userModuleServer.increaseStudentNumByCourseId(reqMap.get("course_id").toString());
        }

        return resultMap;
    }

}
