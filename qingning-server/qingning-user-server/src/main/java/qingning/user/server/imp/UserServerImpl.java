package qingning.user.server.imp;

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
import qingning.server.AbstractQNLiveServer;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.IUserModuleServer;
import qingning.user.server.other.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class UserServerImpl extends AbstractQNLiveServer {

    private IUserModuleServer userModuleServer;

    private ReadLiveRoomOperation readLiveRoomOperation;
    private ReadCourseOperation readCourseOperation;
    private ReadUserOperation readUserOperation;
    private ReadRoomDistributer readRoomDistributer;
    private ReadLecturerOperation readLecturerOperation;
    private ReadRoomDistributerOperation readRoomDistributerOperation;
    private ReadSeriesOperation readSeriesOperation;
    private static Logger logger = LoggerFactory.getLogger(UserServerImpl.class);

    @Override
    public void initRpcServer() {
        if (userModuleServer == null) {
            userModuleServer = this.getRpcService("userModuleServer");

            readLiveRoomOperation = new ReadLiveRoomOperation(userModuleServer);
            readCourseOperation = new ReadCourseOperation(userModuleServer);
            readUserOperation = new ReadUserOperation(userModuleServer);
            readRoomDistributer = new ReadRoomDistributer(userModuleServer);
            readLecturerOperation = new ReadLecturerOperation(userModuleServer);
            readRoomDistributerOperation = new ReadRoomDistributerOperation(userModuleServer);
            readSeriesOperation = new ReadSeriesOperation(userModuleServer);
        }
    }

    @SuppressWarnings("unchecked")
    @FunctionName("userFollowRoom")
    public Map<String, Object> userFollowRoom(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        reqMap.put("user_id", userId);

        //1.更新数据库中关注表的状态
        Jedis jedis = jedisUtils.getJedis(appName);
        Map<String, Object> map = new HashMap<>();
        String roomid = (String)reqMap.get("room_id");
        map.put(Constants.FIELD_ROOM_ID, roomid);
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        //查询直播间是否存在
        if (!jedis.exists(roomKey)) {
        	CacheUtils.readLiveRoom(roomid, this.generateRequestEntity(null, null, null, map), readLiveRoomOperation, jedis, true);
        	if (!jedis.exists(roomKey)) {
        		throw new QNLiveException("100002");
        	}
        }
        String lecturerId = jedis.hget(roomKey, "lecturer_id");
        reqMap.put("lecturer_id", lecturerId);

        if(MiscUtils.isEqual(lecturerId, userId)){
        	throw new QNLiveException("110004");
        }

        Map<String, Object> dbResultMap = userModuleServer.userFollowRoom(reqMap);
        if (dbResultMap == null || dbResultMap.get("update_count") == null || "0".equals(String.valueOf(dbResultMap.get("update_count")))) {
        	if("0".equals(reqMap.get("follow_type"))){
        		throw new QNLiveException("110006");
        	} else {
        		throw new QNLiveException("110003");
        	}
        }
        //4.更新用户缓存中直播间的关注数
        //关注操作类型 1关注 0不关注
        Integer incrementNum = null;
        if (reqMap.get("follow_type").toString().equals("1")) {
            incrementNum = 1;
        } else {
            incrementNum = -1;
        }

        //5.更新用户信息中的关注直播间数，更新直播间缓存的粉丝数
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOMS, map);//删除已加入课程的key  在调用
        jedis.del(key);//删除

        jedis.hincrBy(roomKey, "fans_num", incrementNum);

        //6.更新讲师缓存的粉丝数
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "fans_num", incrementNum);
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, lecturerId);
        jedis.sadd(Constants.CACHED_UPDATE_USER_KEY, userId);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("userCourses")
    public Map<String, Object> getCourses(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        int status =  Integer.parseInt(reqMap.get("status").toString());//状态 1.预告 2.已结束 4.在直播中
        String course_id = (String)reqMap.get("course_id");//课程id
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        String classify_id = reqMap.get("classify_id").toString();
        int updown = 1;
        if(!MiscUtils.isEmpty(course_id)){
            Map<String,Object> query = new HashMap<>();
            query.put("course_id",course_id);
            Map<String,String> courseMap = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, false);
            updown = Integer.valueOf(courseMap.get("course_updown").toString());
        }
        Map<String, Object> values = getPlatformCourses(userId,status,pageCount,course_id,classify_id,appName,null, updown);
        //Map<String, Object> values = getPlatformCourses(reqEntity);
        //课程列表总数
        values.put("course_amount",jedis.zrange(Constants.CACHED_KEY_PLATFORM_COURSE_FINISH,0,-1).size()+jedis.zrange(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION,0,-1).size());
        if(!MiscUtils.isEmpty(values)){
        	final List<Map<String,Object>> courseList = (List<Map<String,Object>>)values.get("course_list");
        	if(!MiscUtils.isEmpty(courseList)){
        		((JedisBatchCallback)(jedis)).invoke(new JedisBatchOperation(){
					@Override
					public void batchOperation(Pipeline pipeline, Jedis jedis) {
						Map<String,Response<String>> nickNames = new HashMap<String,Response<String>>();
						Map<String,String> query = new HashMap<String,String>();
						for(Map<String,Object> course:courseList){
							String lecturer_id = (String)course.get("lecturer_id");
							if(!nickNames.containsKey(lecturer_id)){
								query.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
								String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query);
								nickNames.put(lecturer_id, pipeline.hget(key, "nick_name"));
							}
						}
						pipeline.sync();
						for(Map<String,Object> course:courseList){
							course.put("lecturer_nick_name", nickNames.get((String)course.get("lecturer_id")).get());
						}
					}
        		});
        	}
        }
        return values;
    }

    /**
     * 获取课程列表
     * 关键redis
     *  SYS:COURSES:PREDICTION    平台的预告中课程列表  Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION
     * SYS:COURSES:FINISH  平台的已结束课程列表   Constants.CACHED_KEY_PLATFORM_COURSE_FINISH
     * @param userId 正在查询的用户
     * @param courceStatus 课程状态(前台根据当前页最后一个课程的状态传参) 1预告 2结束 4正在直播
     * @param pageCount 需要几个对象
     * @param courseId 课程id  查找第一页的时候不传 进行分页 必传
     * @param classify_id 分类id
     * @param updown
     */
    @SuppressWarnings({ "unchecked"})
    private  Map<String, Object> getPlatformCourses(String userId, int courceStatus, int pageCount, String courseId, String classify_id, String appName, String lecture_id, int updown) throws Exception{
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        int pageConts = pageCount;
        long currentTime = System.currentTimeMillis();//当前时间
        int offset = 0;//偏移值
        Set<String> courseIdSet;//查询的课程idset
        List<String> courseIdList = new ArrayList<>();//课程id列表
        List<Map<String,String>> courseList = new LinkedList<>();//课程对象列表
        Map<String, Object> resultMap = new HashMap<String, Object>();//最后返回的结果对象

        if(MiscUtils.isEmpty(lecture_id)){
            if(updown == 2){
                courseId = null;
            }
        }

        //判断传过来的课程状态
        //<editor-fold desc="获取课程idList">
        /**
         * 直播
         */
        if(courceStatus == 4 ){//如果预告或者是正在直播的课程
            String startIndex ;//坐标起始位
            String endIndex ;//坐标结束位
            String getCourseIdKey;
            //平台的预告中课程列表 预告和正在直播放在一起  按照直播开始时间顺序排序  根据分类获取不同的缓存
            if(!MiscUtils.isEmpty(classify_id)){//有分类
                Map<String,Object> map = new HashMap<String,Object>();
                map.put(Constants.CACHED_KEY_CLASSIFY,classify_id);
                getCourseIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);//分类
            }else if(lecture_id != null){
                Map<String,Object> map = new HashMap<String,Object>();
                map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
                getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);//讲师
            }else{ //首页
                getCourseIdKey = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
            }
            if(MiscUtils.isEmpty(courseId)){//如果没有传入courceid 那么就是最开始的查询  进行倒叙查询 查询现在的
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(System.currentTimeMillis(),0L);
                startIndex = courseScoreByRedis+"";//设置起始位置
                endIndex = "-inf";//设置结束位置
            }else{//传了courseid
                Map<String,String> queryParam = new HashMap<String,String>();
                queryParam.put("course_id", courseId);
                RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
                Map<String, String> course = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedis, true);//获取当前课程参数
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("start_time")),  MiscUtils.convertObjectToLong(course.get("position")));//拿到当前课程在redis中的score
                startIndex = ""+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
                endIndex = "-inf";//设置结束位置
            }
            courseIdSet = jedis.zrevrangeByScore(getCourseIdKey,startIndex,endIndex,offset,pageCount); //顺序找出couseid  (正在直播或者预告的)
            for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                courseIdList.add(course_id);
            }

            pageCount =  pageConts - courseIdList.size();//用展示数量减去获取的数量  查看是否获取到了足够的课程数
            if( pageCount > 0){//如果返回的值不够
                courseId = null;//把课程id设置为null  用来在下面的代码中进行判断
                courceStatus = 1;//设置查询课程状态 为结束课程 因为查找出来的正在直播和预告的课程不够数量
            }
        }


        /**
         * 预告
         */
        if(courceStatus == 1 ){//如果预告或者是正在直播的课程
            String startIndex ;//坐标起始位
            String endIndex ;//坐标结束位
            String getCourseIdKey;

            //平台的预告中课程列表 预告和正在直播放在一起  按照直播开始时间顺序排序  根据分类获取不同的缓存
            if(!MiscUtils.isEmpty(classify_id)){//有分类
                Map<String,Object> map = new HashMap<String,Object>();
                map.put(Constants.CACHED_KEY_CLASSIFY,classify_id);
                getCourseIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);//分类
            }else if(lecture_id != null){
                Map<String,Object> map = new HashMap<String,Object>();
                map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
                getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);//讲师
            }else{ //首页
                getCourseIdKey = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
            }
            if(MiscUtils.isEmpty(courseId)){//如果没有传入courceid 那么就是最开始的查询  进行倒叙查询 查询现在的
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(System.currentTimeMillis(),0L);
                startIndex = "("+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
                endIndex ="+inf";//设置结束位置
            }else{//传了courseid
                Map<String,String> queryParam = new HashMap<String,String>();
                queryParam.put("course_id", courseId);
                RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
                Map<String, String> course = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedis, true);//获取当前课程参数
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("start_time")),  MiscUtils.convertObjectToLong(course.get("position")));//拿到当前课程在redis中的score
                startIndex = "("+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
                endIndex ="+inf";//设置结束位置
            }
            courseIdSet = jedis.zrangeByScore(getCourseIdKey,startIndex,endIndex,offset,pageCount); //顺序找出couseid  (正在直播或者预告的)
            for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                courseIdList.add(course_id);
            }

            pageCount =  pageConts - courseIdList.size();//用展示数量减去获取的数量  查看是否获取到了足够的课程数
            if( pageCount > 0){//如果返回的值不够
                courseId = null;//把课程id设置为null  用来在下面的代码中进行判断
                courceStatus = 2;//设置查询课程状态 为结束课程 因为查找出来的正在直播和预告的课程不够数量
            }
        }


        //=========================下面的缓存使用另外一种方式获取====================================
        /**
         * 结束
         */
        if(courceStatus == 2 ){//查询结束课程
            boolean key = true;//作为开关 用于下面是否需要接着执行方法
            long startIndex = 0; //开始下标
            long endIndex = -1;   //结束下标
            String getCourseIdKey ;
            //平台的已结束课程列表
            if(!MiscUtils.isEmpty(classify_id)){//有分类
                Map<String,Object> map = new HashMap<String,Object>();
                map.put(Constants.CACHED_KEY_CLASSIFY,classify_id);
                getCourseIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map);//分类
            }else if(lecture_id != null){
                Map<String,Object> map = new HashMap<String,Object>();
                map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
                getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);//讲师
            }else{ //首页
                getCourseIdKey = Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;
            }

            long endCourseSum = jedis.zcard(getCourseIdKey);//获取总共有多少个结束课程
            if(MiscUtils.isEmpty(courseId)){//如果课程ID没有 那么就从最近结束的课程找起
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
        if(!MiscUtils.isEmpty(lecture_id) && !MiscUtils.isEmpty(userId) && lecture_id.equals(userId)){
            pageCount =  pageConts - courseIdList.size();
            if(pageCount > 0 || updown == 2){//是讲师
                boolean key = true;//作为开关 用于下面是否需要接着执行方法
                long startIndex = 0; //开始下标
                long endIndex = -1;   //结束下标
                String getCourseIdKey ;
                Map<String,Object> map = new HashMap<String,Object>();
                map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
                getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_DOWN, map);//讲师 下架的课

                long endCourseSum = jedis.zcard(getCourseIdKey);//获取总共有多少个结束课程
                if(MiscUtils.isEmpty(courseId)){//如果课程ID没有 那么就从最近结束的课程找起
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
        }
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
                Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id, this.generateRequestEntity(null, null, null, queryParam), readCourseOperation, jedis, false);//从缓存中读取课程信息
                MiscUtils.courseTranferState(currentTime, courseInfoMap);//进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                if(jedis.sismember(key, course_id)){//判断当前用户是否有加入这个课程
                    courseInfoMap.put("student", "Y");
                } else {
                    courseInfoMap.put("student", "N");
                }
               courseList.add(courseInfoMap);
            }
        }
        //</editor-fold>
        resultMap.put("course_list", courseList);
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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        //查询出直播间基本信息
        Map<String, String> infoMap = CacheUtils.readLiveRoom(reqMap.get("room_id").toString(), reqEntity, readLiveRoomOperation, jedis, true);
        if (CollectionUtils.isEmpty(infoMap)) {
            throw new QNLiveException("100002");
        }


        //查询关注信息 //TODO 先查询数据库，后续确认是否查询缓存
        //关注状态 0未关注 1已关注
        Map<String, Object> fansMap = userModuleServer.findFansByUserIdAndRoomId(reqMap);
        if (CollectionUtils.isEmpty(fansMap)) {
            resultMap.put("follow_status", "0");
        } else {
            resultMap.put("follow_status", "1");
        }

        resultMap.put("avatar_address", infoMap.get("avatar_address"));
        resultMap.put("room_name", infoMap.get("room_name"));
        resultMap.put("room_remark", infoMap.get("room_remark"));
        resultMap.put("fans_num", infoMap.get("fans_num"));

        //返回用户身份
        List<String> roles = new ArrayList<>();
        //1：普通用户、3：讲师 、4:分销员
        //先查询是否为讲师
        if(infoMap.get("lecturer_id").equals(userId)){
            roles.add("3");
            //不为讲师查询是否为分销员
        }else {
            Map<String,Object> queryMap = new HashMap<>();
            queryMap.put("distributer_id", userId);
            queryMap.put("room_id", reqMap.get("room_id").toString());
            Map<String,String> roomDistributer = CacheUtils.readDistributerRoom(userId, (String)reqMap.get("room_id"), readRoomDistributer, jedis);

            if (! MiscUtils.isEmpty(roomDistributer)) {
               roles.add("4");
                resultMap.put("rq_code",roomDistributer.get("rq_code"));
            }else {
               roles.add("1");
            }
        }

        resultMap.put("roles", roles);
        resultMap.put("qr_code",getQrCode(infoMap.get("lecturer_id"),userId,jedis,appName));
        return resultMap;
    }

    private String getCourseShareURL(String userId, String courseId, Map<String,String> courseMap,String appName,Jedis jedis) throws Exception{
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
        String series_id = null;
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

        //1.先从缓存中查询课程详情，如果有则从缓存中读取课程详情
        if (jedis.exists(courseKey)) {
            Map<String, String> courseCacheMap = jedis.hgetAll(courseKey);
            courseMap = courseCacheMap;
            room_id = courseCacheMap.get("room_id");
            if(!MiscUtils.isEmpty( courseCacheMap.get("series_id"))){
                series_id = courseCacheMap.get("series_id");
            }
        } else {
            //2.如果缓存中没有课程详情，则读取数据库
            Map<String, Object> courseDBMap = userModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());

            //3.如果缓存和数据库中都没有课程详情，则提示课程不存在
            if (CollectionUtils.isEmpty(courseDBMap)) {
                throw new QNLiveException("100004");
            } else {
                MiscUtils.converObjectMapToStringMap(courseDBMap, courseMap);
                room_id = courseDBMap.get("room_id").toString();
                if(!MiscUtils.isEmpty( courseDBMap.get("series_id"))){
                    series_id = courseDBMap.get("series_id").toString();
                }
            }
        }
        boolean isStudent = false;
        if(!MiscUtils.isEmpty(series_id)){
            resultMap.put("series_id",series_id);
            String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, resultMap);
            String series_title = jedis.hget(seriesKey, "series_title");
            String series_updown = jedis.hget(seriesKey, "updown");
            resultMap.put("series_title",series_title);
            resultMap.put("series_updown",series_updown);
            Map<String,Object> queryMap = new HashMap<>();
            queryMap.put("user_id", userId);
            queryMap.put("series_id",series_id);
            //判断访问者是普通用户还是讲师
            isStudent = userModuleServer.isStudentOfTheSeries(queryMap);
            //返回用户身份
            //角色数组 1：普通用户、2：学员、3：讲师
            //加入课程状态 0未加入 1已加入
            if(isStudent){
                resultMap.put("series_join_status", "1");
            }else {
                resultMap.put("series_join_status", "0");
            }
        }
        if (CollectionUtils.isEmpty(courseMap)) {
            throw new QNLiveException("100004");
        }
        for(String key:courseMap.keySet()){
        	resultMap.put(key, courseMap.get(key));
        }

        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("size",10);
        queryMap.put("course_id",reqMap.get("course_id").toString());
        resultMap.put("student_list", userModuleServer.findLatestStudentAvatarAddList(queryMap));
        //从缓存中获取直播间信息
        Map<String, String> liveRoomMap = CacheUtils.readLiveRoom(room_id, reqEntity, readLiveRoomOperation, jedis, true);
        resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));
        resultMap.put("room_id", liveRoomMap.get("room_id"));

        queryMap.clear();
        queryMap.put("user_id", userId);
        queryMap.put("course_id", courseMap.get("course_id"));
        //判断访问者是普通用户还是讲师
        //如果为讲师，则返回讲师部分特定信息

        List<String> roles = new ArrayList<>();
        if(userId.equals(courseMap.get("lecturer_id"))){
            resultMap.put("update_time", courseMap.get("update_time"));
            resultMap.put("course_password", courseMap.get("course_password"));
            resultMap.put("im_course_id", courseMap.get("im_course_id"));
            resultMap.put("share_url",getCourseShareURL(userId, reqMap.get("course_id").toString(), courseMap,appName,jedis));
        }else {
            //为用户，则返回用户部分信息
            isStudent = userModuleServer.isStudentOfTheCourse(queryMap);
            //返回用户身份
            //角色数组 1：普通用户、2：学员、3：讲师
            //加入课程状态 0未加入 1已加入
            if(isStudent){
                resultMap.put("join_status", "1");
            }else {
                resultMap.put("join_status", "0");
            }

            //查询关注状态
            //关注状态 0未关注 1已关注
            reqMap.put("room_id", liveRoomMap.get("room_id"));
            Map<String, Object> fansMap = userModuleServer.findFansByUserIdAndRoomId(reqMap);
            if (CollectionUtils.isEmpty(fansMap)) {
                resultMap.put("follow_status", "0");
            } else {
                resultMap.put("follow_status", "1");
            }
        }
        if(userId.equals(courseMap.get("lecturer_id"))){
            roles.add("3");
        }else {
            if(isStudent){
                roles.add("2");
            }else {
                roles.add("1");
            }
        }

        Map<String,String> roomDistributer = CacheUtils.readDistributerRoom(userId, courseMap.get("room_id"), readRoomDistributerOperation, jedis);
        if(! MiscUtils.isEmpty(roomDistributer)){
            if(roomDistributer.get("end_date") != null){
                Date endDate = new Date(Long.parseLong(roomDistributer.get("end_date")));
                Date todayEndDate = MiscUtils.getEndDateOfToday();
                if(endDate.getTime() >= todayEndDate.getTime()){
                    roles.add("4");
                }
            }
        }
        resultMap.put("roles", roles);
        resultMap.put("qr_code",getQrCode(courseMap.get("lecturer_id"),userId,jedis,appName));
        if(!resultMap.get("status").equals("2")){
            resultMap.put("status",courseMap.get("status"));
        }
        return resultMap;
    }

    /**
     * 逻辑roomCourses,courseList类似，注意重构同步
     * */
    @SuppressWarnings("unchecked")
    @FunctionName("roomCourses")
    public Map<String, Object> getRoomCourses(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //目前一个讲师仅能创建一个直播间，所以查询的是该讲师发布的课程
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.FIELD_ROOM_ID, reqMap.get("room_id").toString());
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
        String lecturerId = jedis.hget(roomKey, "lecturer_id");
        if(MiscUtils.isEmpty(lecturerId)){
        	throw new QNLiveException("120018");
        }
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
        String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);

        String course_id = (String)reqMap.get("course_id");
        int pageCount = (int)reqMap.get("page_count");
        Map <String,Object> resultMap = new HashMap<>();
        int state = 4;
        int updown = 1;
        if(!MiscUtils.isEmpty(course_id)){
            Map<String,Object> query = new HashMap<>();
            query.put("course_id",course_id);
            Map<String,String> courseMap = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, false);
            state = Integer.valueOf(courseMap.get("status").toString());
            updown = Integer.valueOf(courseMap.get("course_updown").toString());
        }
        Map<String, Object> platformCourses = new HashMap<>();
        platformCourses = getPlatformCourses(userId, state, pageCount, course_id, null, appName, lecturerId,updown);

        resultMap.putAll(platformCourses);
      //  resultMap.put("course_list", courseList);
        resultMap.put("course_sum",jedis.zrange(lecturerCoursesPredictionKey,0,-1).size()+ jedis.zrange(lecturerCoursesFinishKey,0,-1).size());
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




    /**
     * 加入课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("joinCourse")
    public Map<String, Object> joinCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        //1.1读取课程信息
        String course_id = (String)reqMap.get("course_id");
        String query_type = reqMap.get("query_type").toString();
        map.put("course_id", course_id);
        Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, false);
        if(MiscUtils.isEmpty(courseInfoMap)){
        	throw new QNLiveException("100004");
        }
        //1.2检测该用户是否为讲师，为讲师则不能加入该课程 
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        if(userId.equals(courseInfoMap.get("lecturer_id"))){
            throw new QNLiveException("210006");
        }

        //2.检测课程验证信息是否正确
        //2.1如果课程为私密课程则检验密码

        String course_type = courseInfoMap.get("course_type");

        boolean isSeriesStudent = false;//为用户，则返回用户部分信息
        if(!MiscUtils.isEmpty(courseInfoMap.get("series_id"))){
            Map<String,Object> queryMap = new HashMap<>();
            queryMap.put("user_id", userId);
            queryMap.put("series_id",courseInfoMap.get("series_id").toString());
            //判断访问者是普通用户还是讲师
            //如果为讲师，则返回讲师部分特定信息
            isSeriesStudent = userModuleServer.isStudentOfTheSeries(queryMap);
        }
        if(isSeriesStudent){
            course_type = "0";
        }
        if(query_type.equals("0")){
            if("1".equals(course_type)){
                if(reqMap.get("course_password") == null || StringUtils.isBlank(reqMap.get("course_password").toString())){
                    throw new QNLiveException("000100");
                }
                if(! reqMap.get("course_password").toString().equals(courseInfoMap.get("course_password").toString())){
                    throw new QNLiveException("120006");
                }
            }else if("2".equals(course_type)){
                    if(reqMap.get("payment_id") == null){
                        throw new QNLiveException("000100");
                    }
                    Map<String,Object> queryMap = new HashMap<>();
                    queryMap.put("payment_id",reqMap.get("payment_id"));
                    queryMap.put("user_id",userId);
                    boolean userWhetherToPay = userModuleServer.findUserWhetherToPay(queryMap);
                    if(!userWhetherToPay){
                        throw new QNLiveException("120022");
                    }
                }
        }else{
            if("1".equals(course_type)){
                if(reqMap.get("payment_id") == null){
                    throw new QNLiveException("000100");
                }
            }
        }


        //3.检测学生是否参与了该课程
        Map<String,Object> studentQueryMap = new HashMap<>();
        studentQueryMap.put("user_id",userId);
        studentQueryMap.put("course_id",course_id);
        if(userModuleServer.isStudentOfTheCourse(studentQueryMap)){
            throw new QNLiveException("100004");
        }

        //5.将学员信息插入到学员参与表中
        courseInfoMap.put("user_id",userId);
        courseInfoMap.put("value_from",query_type);
        userModuleServer.joinCourse(courseInfoMap);

        if(query_type.equals("0")){
            //<editor-fold desc="app">
            //6.修改讲师缓存中的课程参与人数
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseInfoMap.get("lecturer_id"));
            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            jedis.hincrBy(lecturerKey, "total_student_num", 1);
            if("2".equals(course_type)){
                    jedis.hincrBy(lecturerKey, "pay_student_num", 1);
            }
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

            Long nowStudentNum = 0L;
            if(jedis.exists(courseKey)){
                //jedis.hincrBy(courseKey, "student_num", 1);
                map.clear();
                map.put("course_id", course_id);
                Map<String,Object> numInfo = userModuleServer.findCourseRecommendUserNum(map);
                long num = 0;
                if(!MiscUtils.isEmpty(numInfo)){
                    num=MiscUtils.convertObjectToLong(numInfo.get("recommend_num"));
                }
                jedis.hset(courseKey, "student_num", num+"");
                Map<String, String> courseMap = jedis.hgetAll(courseKey);
                switch (courseMap.get("status")){
                    case "1":
                        MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);//更新时间
                        if(courseMap.get("status").equals("4")){
                            jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_LIVE,1,course_id);
                        }else{
                            jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_PREDICTION,1,course_id);
                        }
                        break;
                    case "2":
                        jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_FINISH,1,course_id);
                        break;
                }
            }else {
                userModuleServer.increaseStudentNumByCourseId(reqMap.get("course_id").toString());
            }

            //7.修改用户缓存信息中的加入课程数
            map.clear();
            map.put(Constants.CACHED_KEY_USER_FIELD, userId);

            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, courseInfoMap);//删除已加入课程的key  在之后登录时重新加入
            jedis.del(key);//删除
            Map<String,String> userMap = CacheUtils.readUser(userId, this.generateRequestEntity(null, null, null, map), readUserOperation, jedis);
            nowStudentNum = MiscUtils.convertObjectToLong(courseInfoMap.get("student_num")) + 1;
            String levelString = MiscUtils.getConfigByKey("jpush_course_students_arrive_level",appName);
            JSONArray levelJson = JSON.parseArray(levelString);
            if (levelJson.contains(nowStudentNum + "")) {
                JSONObject obj = new JSONObject();
                //String course_type = courseMap.get("course_type");
                String course_type_content = MiscUtils.convertCourseTypeToContent(course_type);
                obj.put("body", String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content",appName), course_type_content, MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")), nowStudentNum + ""));
                obj.put("to", courseInfoMap.get("lecturer_id"));
                obj.put("msg_type", "7");
                Map<String, String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type", "7");
                extrasMap.put("course_id", courseInfoMap.get("course_id"));
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj,appName);
            }
            //course_type 0:公开课程 1:加密课程 2:收费课程',
            //TODO 加入课程推送   收费课程支付成功才推送消息
            if (!"2".equals(courseInfoMap.get("course_type"))) {
                //获取讲师的信息
                map.clear();
                map.put("lecturer_id", courseInfoMap.get("lecturer_id"));
                Map<String, String> user = CacheUtils.readLecturer(courseInfoMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);

                Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
                TemplateData first = new TemplateData();
                first.setColor(Constants.WE_CHAT_PUSH_COLOR);
                String firstContent = String.format(MiscUtils.getConfigByKey("wpush_shop_course_first",appName), MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")));
                first.setValue(firstContent);
                templateMap.put("first", first);

                TemplateData courseTitle = new TemplateData();
                courseTitle.setColor(Constants.WE_CHAT_PUSH_COLOR);
                courseTitle.setValue(MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")));
                templateMap.put("keyword1", courseTitle);

                Date start_time = new Date(Long.parseLong(courseInfoMap.get("start_time")));
                TemplateData orderNo = new TemplateData();
                orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
                orderNo.setValue(MiscUtils.parseDateToFotmatString(start_time, "yyyy-MM-dd HH:mm:ss"));
                templateMap.put("keyword2", orderNo);

                String lastContent;
                lastContent = MiscUtils.getConfigByKey("wpush_shop_course_lecturer_name",appName) + MiscUtils.RecoveryEmoji(user.get("nick_name"));
//			String thirdContent = MiscUtils.RecoveryEmoji(courseInfoMap.get("course_remark"));
//			if(! MiscUtils.isEmpty(thirdContent)){
//				lastContent += "\n" + MiscUtils.getConfigByKey("wpush_shop_course_brief",appName) + thirdContent;
//			}
                lastContent += "\n" +MiscUtils.getConfigByKey("wpush_shop_course_remark",appName);

                Map<String,Object> studentUserMap = userModuleServer.findLoginInfoByUserId(userId);
                TemplateData remark = new TemplateData();
                if(appName.equals(Constants.HEADER_APP_NAME)){
                    remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
                }else{
                    remark.setColor(Constants.WE_CHAT_PUSH_COLOR_DLIVE);
                }


                remark.setValue(lastContent);
                templateMap.put("remark", remark);
                String url = String.format(MiscUtils.getConfigByKey("course_live_room_url",appName), courseInfoMap.get("course_id"),  courseInfoMap.get("room_id"));
                WeiXinUtil.send_template_message((String) studentUserMap.get("web_openid"), MiscUtils.getConfigByKey("wpush_shop_course",appName),url, templateMap, jedis,appName);
            }

            //公开课 执行这段逻辑
            if (equals(courseInfoMap.get("course_type").equals("0"))) {
                //一个用户进入加入直播间带入1到2个人进入
                map.clear();
                map.put("course_id", course_id);
                RequestEntity mqRequestEntity = new RequestEntity();
                mqRequestEntity.setServerName("CourseRobotService");
                mqRequestEntity.setAppName(appName);
                mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                mqRequestEntity.setFunctionName("courseHaveStudentIn");
                mqRequestEntity.setParam(map);
                this.mqUtils.sendMessage(mqRequestEntity);
            }

            jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, courseInfoMap.get("lecturer_id").toString());
            //</editor-fold>
        }
        return resultMap;
    }

/**
     * 加入系列
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("joinSeries")
    public Map<String, Object> joinSeries(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        //1.1读取课程信息
        String series_id = (String)reqMap.get("series_id");
        String query_type = "0";
        if(!MiscUtils.isEmpty(reqMap.get("query_type"))){
            query_type = reqMap.get("query_type").toString();//0app 1saas
        }
        map.put("series_id", series_id);
        Map<String, String> seriesInfoMap = CacheUtils.readSeries(series_id, generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
        if(MiscUtils.isEmpty(seriesInfoMap)){
            throw new QNLiveException("100004");
        }

        //1.2检测该用户是否为讲师，为讲师则不能加入该课程
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        if(userId.equals(seriesInfoMap.get("lecturer_id"))){
            throw new QNLiveException("100017");
        }

        //查询支付订单
        String series_type = seriesInfoMap.get("series_type");
        if("1".equals(series_type)){
            //TODO 支付课程要验证支付信息
            if(reqMap.get("payment_id") == null){
                throw new QNLiveException("000100");
            }
            Map<String,Object> queryMap = new HashMap<>();
            queryMap.put("payment_id",reqMap.get("payment_id"));
            queryMap.put("user_id",userId);
            boolean userWhetherToPay = userModuleServer.findUserWhetherToPay(queryMap);
            if(!userWhetherToPay){
                throw new QNLiveException("120022");
            }

        }

        //3.检测学生是否参与了该课程
        Map<String,Object> studentQueryMap = new HashMap<>();
        studentQueryMap.put("user_id",userId);
        studentQueryMap.put("series_id",series_id);
        if(userModuleServer.isStudentOfTheSeries(studentQueryMap)){//判断是否有加入课程
            throw new QNLiveException("100004");
        }

        //5.将学员信息插入到学员参与表中
        seriesInfoMap.put("user_id",userId);
        seriesInfoMap.put("value_from",query_type);
        Map<String,Object> insertResultMap = userModuleServer.joinSeries(seriesInfoMap);

        //6.修改讲师缓存中的课程参与人数
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, seriesInfoMap.get("lecturer_id"));
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "total_student_num", 1);
        if("1".equals(series_type)){
            jedis.hincrBy(lecturerKey, "pay_student_num", 1);
        }
        map.clear();
        map.put(Constants.CACHED_KEY_SERIES_FIELD, series_id);
        String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);

        Long nowStudentNum = 0L;
        userModuleServer.increaseStudentNumBySeriesId(series_id);
        jedis.del(seriesKey);
        CacheUtils.readSeries(series_id, generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
        //7.修改用户缓存信息中的加入课程数
        map.clear();
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);

        String userSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_SERIES, seriesInfoMap);//删除已加入课程的key  在之后登录时重新加入
        jedis.zadd(userSeriesKey,System.currentTimeMillis(),series_id);
//        //series_type 0:公开课程  1:收费课程',
//        //TODO 加入课程推送   收费课程支付成功才推送消息
//        if (!"2".equals(seriesInfoMap.get("series_type"))) {
//            //获取讲师的信息
//            map.clear();
//            map.put("lecturer_id", seriesInfoMap.get("lecturer_id"));
//            Map<String, String> user = CacheUtils.readLecturer(seriesInfoMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);
//
//            Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
//            TemplateData first = new TemplateData();
//            first.setColor(Constants.WE_CHAT_PUSH_COLOR);
//            String firstContent = String.format(MiscUtils.getConfigByKey("wpush_shop_course_first",appName), MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")));
//            first.setValue(firstContent);
//            templateMap.put("first", first);
//
//            TemplateData courseTitle = new TemplateData();
//            courseTitle.setColor(Constants.WE_CHAT_PUSH_COLOR);
//            courseTitle.setValue(MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")));
//            templateMap.put("keyword1", courseTitle);
//
//            Date start_time = new Date(Long.parseLong(courseInfoMap.get("start_time")));
//            TemplateData orderNo = new TemplateData();
//            orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
//            orderNo.setValue(MiscUtils.parseDateToFotmatString(start_time, "yyyy-MM-dd HH:mm:ss"));
//            templateMap.put("keyword2", orderNo);
//
//            String lastContent;
//            lastContent = MiscUtils.getConfigByKey("wpush_shop_course_lecturer_name",appName) + MiscUtils.RecoveryEmoji(user.get("nick_name"));
//            lastContent += "\n" +MiscUtils.getConfigByKey("wpush_shop_course_remark",appName);
//
//            Map<String,Object> studentUserMap = userModuleServer.findLoginInfoByUserId(userId);
//            TemplateData remark = new TemplateData();
//            if(appName.equals(Constants.HEADER_APP_NAME)){
//                remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
//            }else{
//                remark.setColor(Constants.WE_CHAT_PUSH_COLOR_DLIVE);
//            }
//            remark.setValue(lastContent);
//            templateMap.put("remark", remark);
//            String url = String.format(MiscUtils.getConfigByKey("course_live_room_url",appName), courseInfoMap.get("course_id"),  courseInfoMap.get("room_id"));
//            WeiXinUtil.send_template_message((String) studentUserMap.get("web_openid"), MiscUtils.getConfigByKey("wpush_shop_course",appName),url, templateMap, jedis,appName);
//        }

//        //公开课 执行这段逻辑
//        if (equals(courseInfoMap.get("course_type").equals("0"))) {
//            //一个用户进入加入直播间带入1到2个人进入
//
//            map.clear();
//            map.put("course_id", course_id);
//            RequestEntity mqRequestEntity = new RequestEntity();
//            mqRequestEntity.setServerName("CourseRobotService");
//            mqRequestEntity.setAppName(appName);
//            mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
//            mqRequestEntity.setFunctionName("courseHaveStudentIn");
//            mqRequestEntity.setParam(map);
//            this.mqUtils.sendMessage(mqRequestEntity);
//        }
        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, seriesInfoMap.get("lecturer_id").toString());
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("courseInfo")
    public Map<String, Object> getCourseInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

        Map<String, String> courseMap = new HashMap<>();
        //1.先检查该课程是否在缓存中
        if (jedis.exists(courseKey)) {
            courseMap = jedis.hgetAll(courseKey);
            JSONArray pptList = null;
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String pptListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PPTS, map);
            if (jedis.exists(pptListKey)) {
                pptList = JSONObject.parseArray(jedis.get(pptListKey));
            }

            List<Map<String, String>> audioObjectMapList = new ArrayList<>();
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
            String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
            String audioJsonStringKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, map);
            Set<String> audioIdList = jedis.zrange(audioListKey, 0, -1);

            //如果存在zsort列表，则从zsort列表中读取
            if (audioIdList != null && audioIdList.size() > 0) {
                JedisBatchCallback callBack = (JedisBatchCallback)jedis;
                callBack.invoke(new JedisBatchOperation() {
                    @Override
                    public void batchOperation(Pipeline pipeline, Jedis jedis) {

                        List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
                        for (String audio : audioIdList) {
                            map.put(Constants.FIELD_AUDIO_ID, audio);
                            String audioKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
                            redisResponseList.add(pipeline.hgetAll(audioKey));
                        }
                        pipeline.sync();

                        for (Response<Map<String, String>> redisResponse : redisResponseList) {
                            Map<String, String> messageStringMap = redisResponse.get();
                            audioObjectMapList.add(messageStringMap);
                        }
                    }
                });

                resultMap.put("audio_list", audioObjectMapList);

                //如果存在讲课音频的json字符串，则读取讲课音频json字符串
            } else if (jedis.exists(audioJsonStringKey)) {
                resultMap.put("audio_list", JSONObject.parse(jedis.get(audioJsonStringKey)));
            }

            if (!CollectionUtils.isEmpty(pptList)) {
                resultMap.put("ppt_list", pptList);
            }

            resultMap.put("im_course_id", jedis.hget(courseKey, "im_course_id"));

            //判断该课程状态，如果为直播中，则检查开播时间是否存在，如果开播时间存在，
            //则检查当前查询是否大于当前时间，如果大于，则查询用户是否存在于在线map中，
            //如果不存在，则将该学员加入的在线map中，并且修改课程缓存real_student_num实际课程人数(默认课程人数)
            String realStudentNum = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_REAL_STUDENT_NUM, map);
            boolean hasStudent = jedis.hexists(realStudentNum, userId);
            if (hasStudent == false) {
                if (courseMap.get("status").equals("4")) {
                    if (courseMap.get("real_start_time") != null) {
                        Long real_start_time = Long.parseLong(courseMap.get("real_start_time"));
                        long now = System.currentTimeMillis();
                        if (now > real_start_time) {
                            jedis.hset(realStudentNum, userId, "1");
                            jedis.hincrBy(courseKey, "real_student_num", 1);
                        }
                    }
                }
            }

        } else {
            //2.如果不在缓存中，则查询数据库
            Map<String, Object> courseInfoMap = userModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
            MiscUtils.converObjectMapToStringMap(courseInfoMap, courseMap);
            if (courseInfoMap == null) {
                throw new QNLiveException("100004");
            }



            //查询课程PPT列表
            List<Map<String, Object>> pptList = userModuleServer.findPPTListByCourseId(reqMap.get("course_id").toString());

            //查询课程语音列表
            List<Map<String, Object>> audioList = userModuleServer.findAudioListByCourseId(reqMap.get("course_id").toString());

            if (!CollectionUtils.isEmpty(pptList)) {
                resultMap.put("ppt_list", pptList);
            }

            if (!CollectionUtils.isEmpty(audioList)) {
                resultMap.put("audio_list", audioList);
            }

            resultMap.put("im_course_id", courseInfoMap.get("im_course_id"));

        }

        //4.将学生加入该课程的IM群组
        try {
            //检查学生上次加入课程，如果加入课程不为空，则退出上次课程
            Map<String,Object> studentUserMap = userModuleServer.findLoginInfoByUserId(userId);
            map.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String courseIMKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_LAST_JOIN_COURSE_IM_INFO, map);
            String imCourseId = jedis.get(courseIMKey);
            if(! MiscUtils.isEmpty(imCourseId)){
                IMMsgUtil.delGroupMember(imCourseId, studentUserMap.get("m_user_id").toString(), studentUserMap.get("m_user_id").toString());
            }

            //加入新课程IM群组，并且将加入的群组记录入缓存中
            IMMsgUtil.joinGroup(courseMap.get("im_course_id"), studentUserMap.get("m_user_id").toString(),studentUserMap.get("m_user_id").toString());
            jedis.set(courseIMKey, courseMap.get("im_course_id"));
        }catch (Exception e){
            //TODO 暂时不处理
        }

        map.clear();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String banKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
        Double banScore = jedis.zscore(banKey, userId);
        if(banScore == null){
            resultMap.put("ban_status", "0");
        }else {
            resultMap.put("ban_status", "1");
        }

        //增加返回课程相应信息
        resultMap.put("student_num",courseMap.get("student_num"));
        resultMap.put("start_time",courseMap.get("start_time"));
        resultMap.put("status",courseMap.get("status"));
        resultMap.put("course_type",courseMap.get("course_type"));
        resultMap.put("course_password",courseMap.get("course_password"));
        resultMap.put("share_url",MiscUtils.getConfigByKey("course_share_url_pre_fix",appName)+reqMap.get("course_id").toString());//TODO
        resultMap.put("course_update_time",courseMap.get("update_time"));
        resultMap.put("course_title",courseMap.get("course_title"));
        //Map<String,Object> userMap = userModuleServer.findUserInfoByUserId(courseMap.get("lecturer_id"));
        map.clear();
        map.put("lecturer_id", courseMap.get("lecturer_id"));
        Map<String, String> userMap = CacheUtils.readLecturer(courseMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);
        if(!MiscUtils.isEmpty(userMap)){
        	resultMap.put("lecturer_nick_name",userMap.get("nick_name"));
        	resultMap.put("lecturer_avatar_address",userMap.get("avatar_address"));
        }
        resultMap.put("course_url",courseMap.get("course_url"));

        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("messageList")
    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();

        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        String queryType = reqMap.get("query_type").toString();

        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
        String messageListKey;
        Map<String, Object> queryMap = new HashMap<>();

        //queryType为0则查询全部消息，为1则查询提问
        if(queryType.equals("0")){
            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
        }else {
            messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
            queryMap.put("send_type_query", "0");//send_type_query 查询出 类型:0:讲师讲解 1：讲师回答 两种类型的聊天记录
        }

        //缓存不存在则读取数据库中的内容
        if(! jedis.exists(messageListKey)){
            queryMap.put("page_count", pageCount);
            if(reqMap.get("message_pos") != null && StringUtils.isNotBlank(reqMap.get("message_pos").toString())){
                queryMap.put("message_pos", Long.parseLong(reqMap.get("message_pos").toString()));
            }else {
/*              Map<String,Object> maxInfoMap = userModuleServer.findCourseMessageMaxPos(reqMap.get("course_id").toString());
                if(MiscUtils.isEmpty(maxInfoMap)){
                    return resultMap;
                }
                Long maxPos = (Long)maxInfoMap.get("message_pos");
                queryMap.put("message_pos", maxPos);*/
            }
            queryMap.put("course_id", reqMap.get("course_id").toString());
            List<Map<String,Object>> messageList = userModuleServer.findCourseMessageList(queryMap);

            if(! CollectionUtils.isEmpty(messageList)){
            	List<Map<String,Object>> resultList = new LinkedList<Map<String,Object>>();
				for(Map<String,Object> messageMap : messageList){
					if(! MiscUtils.isEmpty(messageMap.get("message"))){
						messageMap.put("message",MiscUtils.RecoveryEmoji(messageMap.get("message").toString()));
					}
					if(! MiscUtils.isEmpty(messageMap.get("message_question"))){
						messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
					}
					if(!MiscUtils.isEmpty(messageMap.get("creator_nick_name"))){
						messageMap.put("creator_nick_name",MiscUtils.RecoveryEmoji(messageMap.get("creator_nick_name").toString()));
					}
					resultList.add(0, messageMap);
				}
                resultMap.put("message_list", resultList);
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
					if(MiscUtils.isEmpty(messageMap)){
						continue;
					}
					if(messageMap.get("creator_id") != null){
						Map<String,Object> innerMap = new HashMap<>();
						innerMap.put("user_id", messageMap.get("creator_id"));
						Map<String,String> userMap = CacheUtils.readUser(messageMap.get("creator_id"), this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedis);
						if(! MiscUtils.isEmpty(userMap)){
							if(userMap.get("nick_name") != null){
								messageMap.put("creator_nick_name", MiscUtils.RecoveryEmoji(userMap.get("nick_name")));
							}
							if(userMap.get("avatar_address") != null){
								messageMap.put("creator_avatar_address", userMap.get("avatar_address"));
							}
						}
					}

					String messageContent = messageMap.get("message");
					if(! MiscUtils.isEmpty(messageContent)){
						messageMap.put("message",MiscUtils.RecoveryEmoji(messageContent));
					}

					if(! MiscUtils.isEmpty(messageMap.get("message_question"))){
						messageMap.put("message_question",MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
					}
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
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> queryMap = new HashMap<>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        queryMap.put("page_count", Integer.parseInt(reqMap.get("page_count").toString()));
        if(reqMap.get("student_pos") != null && StringUtils.isNotBlank(reqMap.get("student_pos").toString())){
            queryMap.put("student_pos", Long.parseLong(reqMap.get("student_pos").toString()));
        }
        queryMap.put("course_id", reqMap.get("course_id").toString());
        List<Map<String,Object>> messageList = userModuleServer.findCourseStudentList(queryMap);

        if(! CollectionUtils.isEmpty(messageList)){
            resultMap.put("student_list", messageList);
        }

        Map<String,String> liveRoomMap = CacheUtils.readCourse(reqMap.get("course_id").toString(),reqEntity,readCourseOperation,jedis,true);
        resultMap.put("student_num",liveRoomMap.get("student_num"));
        return resultMap;

    }

    @SuppressWarnings("unchecked")
    @FunctionName("getUserConsumeRecords")
    public Map<String, Object> getUserConsumeRecords(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> queryMap = new HashMap<>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        //获得用户id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        queryMap.put("user_id", userId);
        //根据page_count、position（如果有）查询数据库
        queryMap.put("page_count", Integer.parseInt(reqMap.get("page_count").toString()));
        if(reqMap.get("position") != null && StringUtils.isNotBlank(reqMap.get("position").toString())){
            queryMap.put("position", Long.parseLong(reqMap.get("position").toString()));
        }
        List<Map<String,Object>> records;
        if(reqMap.get("shop_id")!=null){
            queryMap.put("shop_id",reqMap.get("shop_id"));
            //本店铺所有消费记录
            records = userModuleServer.findUserShopRecords(queryMap);
        }else {
            //查询直播间消费记录（不包括SAAS后台）
            //课程类型(1：直播间，2：店铺（非直播间）)
            queryMap.put("course_type","1");
            records = userModuleServer.findUserConsumeRecords(queryMap);
        }

        if(! CollectionUtils.isEmpty(records)){
            Map<String,Object> cacheQueryMap = new HashMap<>();

            JedisBatchCallback callBack = (JedisBatchCallback)jedis;
            //从缓存中查询讲师的名字
            callBack.invoke(new JedisBatchOperation(){
                @Override
                public void batchOperation(Pipeline pipeline, Jedis jedis) {
                    for(Map<String,Object> recordMap : records){
                        cacheQueryMap.put(Constants.CACHED_KEY_LECTURER_FIELD, recordMap.get("lecturer_id"));
                        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, cacheQueryMap);
                        Response<String> cacheLecturerName = pipeline.hget(lecturerKey, "nick_name");
                        recordMap.put("cacheLecturerName",cacheLecturerName);
                    }
                    for(Map<String,Object> recordMap : records){
                        cacheQueryMap.clear();
                        cacheQueryMap.put(Constants.CACHED_KEY_COURSE_FIELD, recordMap.get("course_id"));
                        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, cacheQueryMap);
                        Response<String> courseName = pipeline.hget(courseKey, "course_title");
                        recordMap.put("courseTitle", courseName);
                    }
                    pipeline.sync();

                    for(Map<String,Object> recordMap : records){
                        Response<String> cacheLecturerName = (Response)recordMap.get("cacheLecturerName");
                        Response<String> courseName = (Response)recordMap.get("courseTitle");
                        recordMap.put("lecturer_name",cacheLecturerName.get());
                        if(recordMap.get("series_title")==null) {
                            recordMap.put("course_title", courseName.get());
                        }
                        recordMap.remove("cacheLecturerName");
                        Date recordTime = (Date)recordMap.get("create_time");
                        recordMap.put("create_time", recordTime);
                    }
                }
            });

            resultMap.put("record_list", records);
        }

        return resultMap;

    }

    @SuppressWarnings("unchecked")
    @FunctionName("getUserIncomeRecords")
    public Map<String, Object> getUserIncomeRecords(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> queryMap = new HashMap<>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        //获得用户id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        queryMap.put("user_id", userId);
        //根据page_count、position（如果有）查询数据库
        queryMap.put("page_count", Integer.parseInt(reqMap.get("page_count").toString()));
        if(reqMap.get("position") != null && StringUtils.isNotBlank(reqMap.get("position").toString())){
            queryMap.put("position", Long.parseLong(reqMap.get("position").toString()));
        }
        queryMap.put("type","2");
        List<Map<String,Object>> records = userModuleServer.findUserConsumeRecords(queryMap);

        if(! CollectionUtils.isEmpty(records)){
            Map<String,Object> cacheQueryMap = new HashMap<>();

            JedisBatchCallback callBack = (JedisBatchCallback)jedis;
            //从缓存中查询讲师的名字
            callBack.invoke(new JedisBatchOperation(){
                @Override
                public void batchOperation(Pipeline pipeline, Jedis jedis) {
                    for(Map<String,Object> recordMap : records){
                        cacheQueryMap.put(Constants.CACHED_KEY_LECTURER_FIELD, recordMap.get("user_id"));
                        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, cacheQueryMap);
                        Response<String> cacheLecturerName = pipeline.hget(lecturerKey, "nick_name");
                        recordMap.put("cacheLecturerName",cacheLecturerName);
                    }
                    for(Map<String,Object> recordMap : records){
                            cacheQueryMap.clear();
                            cacheQueryMap.put(Constants.CACHED_KEY_COURSE_FIELD, recordMap.get("course_id"));
                            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, cacheQueryMap);
                            Response<String> courseName = pipeline.hget(courseKey, "course_title");
                            recordMap.put("courseTitle",courseName);
                    }
                    pipeline.sync();

                    for(Map<String,Object> recordMap : records){
                        Response<String> cacheLecturerName = (Response)recordMap.get("cacheLecturerName");
                        Response<String> courseName = (Response)recordMap.get("courseTitle");
                        recordMap.put("lecturer_name",cacheLecturerName.get());
                        if(recordMap.get("series_title")==null) {
                            recordMap.put("course_title",courseName.get());
                        }
                        //打赏收益
                        /*if("1".equals(recordMap.get("profit_type").toString())){
                            ""
                        }*/

                        recordMap.put("title",recordMap.get("lecturer_name")+"  "+recordMap.get("course_title"));

                        recordMap.remove("cacheLecturerName");
                        Date recordTime = (Date)recordMap.get("create_time");
                        recordMap.put("create_time", recordTime);
                    }
                }
            });

            resultMap.put("record_list", records);
        }

        return resultMap;

    }

    @FunctionName("noticeRooms")
    public  Map<String, Object> getNoticeRooms(RequestEntity reqEntity) throws Exception{
        @SuppressWarnings("unchecked")
		Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);
        final List<Map<String,Object>> list = userModuleServer.findFanInfoByUserId(reqMap);
        resultMap.put("live_room_list", list);
        if(!MiscUtils.isEmpty(list)){
    		((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
    			@Override
    			public void batchOperation(Pipeline pipeline, Jedis jedis) {
    				Map<String,Response<Set<Tuple>>> courseInfoDictMap = new HashMap<String,Response<Set<Tuple>>>();
    				Map<String,Response<Set<Tuple>>> courseInfoPreDictMap = new HashMap<String,Response<Set<Tuple>>>();
    				Map<String,Response<Set<Tuple>>> courseInfoFinDictMap = new HashMap<String,Response<Set<Tuple>>>();
    				long currentTime = System.currentTimeMillis();
    				Map<String,Object> map = new HashMap<String,Object>();
    				for(Map<String,Object> fansRoom:list){
    					String lecturer_id = (String)fansRoom.get("lecturer_id");
    					map.clear();
    			        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
    			        String lecturerCoursesPredictionKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
    			        Response<Set<Tuple>> response = pipeline.zrevrangeByScoreWithScores(lecturerCoursesPredictionKey, currentTime+"", "-inf", 0,  1);
    			        courseInfoDictMap.put(lecturer_id, response);
    			        response = pipeline.zrevrangeByScoreWithScores(lecturerCoursesPredictionKey, "+inf", "-inf", 0,  1);
    			        courseInfoPreDictMap.put(lecturer_id, response);

						String lecturerCoursesFinishKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);
						response = pipeline.zrevrangeByScoreWithScores(lecturerCoursesFinishKey, "+inf", "-inf", 0, 1);
						courseInfoFinDictMap.put(lecturer_id, response);
    				}

    				pipeline.sync();
    				Map<String,Response<Map<String,String>>> courseInfo = new HashMap<String,Response<Map<String,String>>>();

    				for(Map<String,Object> fansRoom:list){
    					String lecturer_id = (String)fansRoom.get("lecturer_id");
    					if(courseInfo.containsKey(lecturer_id)){
    						continue;
    					}
    					Response<Set<Tuple>> tupleSet = courseInfoDictMap.get(lecturer_id);
    					String courseId = null;
    					if(tupleSet!=null && !MiscUtils.isEmpty(tupleSet.get())){
    						Set<Tuple> set = tupleSet.get();
    						for(Tuple tupe:set){
    							courseId = tupe.getElement();
    						}
    					}
    					if(MiscUtils.isEmpty(courseId)){
    						tupleSet = courseInfoPreDictMap.get(lecturer_id);
    					}

    					if(tupleSet!=null && !MiscUtils.isEmpty(tupleSet.get())){
    						Set<Tuple> set = tupleSet.get();
    						for(Tuple tupe:set){
    							courseId = tupe.getElement();
    						}
    					}

    					if(MiscUtils.isEmpty(courseId)){
    						tupleSet = courseInfoFinDictMap.get(lecturer_id);
    					}

    					if(tupleSet!=null && !MiscUtils.isEmpty(tupleSet.get())){
    						Set<Tuple> set = tupleSet.get();
    						for(Tuple tupe:set){
    							courseId = tupe.getElement();
    						}
    					}
    					if(!MiscUtils.isEmpty(courseId)){
    						map.clear();
    						map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
    						String cachedKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
    						courseInfo.put(lecturer_id, pipeline.hgetAll(cachedKey));
    					}
    				}

    				pipeline.sync();

    				for(Map<String,Object> fansRoom:list){
    					String lecturer_id = (String)fansRoom.get("lecturer_id");
    					Response<Map<String,String>> response = courseInfo.get(lecturer_id);
    					Map<String,String> courseValue = null;
    					if(response!=null){
    						courseValue = response.get();
    					}
    					if(MiscUtils.isEmpty(courseValue)){
    						continue;
    					}
    					MiscUtils.courseTranferState(currentTime, courseValue);
    					fansRoom.put("course_title", courseValue.get("course_title"));
    					fansRoom.put("course_id", courseValue.get("course_id"));
    					fansRoom.put("course_type", courseValue.get("course_type"));
    					fansRoom.put("start_time", courseValue.get("start_time"));
    					fansRoom.put("status", courseValue.get("status"));
    				}
    			}
    		});
        }

        return resultMap;
    }

    @FunctionName("studyCourses")
    public  Map<String, Object> getStudyCourses(RequestEntity reqEntity) throws Exception{
    	@SuppressWarnings("unchecked")
    	Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
    	String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
    	reqMap.put("user_id", userId);
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
    	Map<String,String> values = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
    	long course_num = 0;
    	try{
    		course_num = Long.parseLong(values.get("course_num").toString());
    	}catch(Exception e){
    		course_num = 0;
    	}
    	Map<String,Object> result = new HashMap<String,Object>();
    	result.put("course_num", course_num);
    	if(course_num>0){
	    	Map<String,Object> queryMap = new HashMap<String,Object>();
	    	queryMap.put("create_time", reqMap.get("record_time"));
	    	queryMap.put("page_count", reqMap.get("page_count"));
	    	queryMap.put("user_id", userId);

	    	final List<Map<String,Object>> list = userModuleServer.findCourseListOfStudent(queryMap);
	    	result.put("course_list", list);
	    	if(!MiscUtils.isEmpty(list)){
	    		((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
	    			@Override
	    			public void batchOperation(Pipeline pipeline, Jedis jedis) {
	    	    		long currentTime= System.currentTimeMillis();
	    	    		Map<String,Response<String>> nickeNameMap = new HashMap<String,Response<String>>();
	    	    		for(Map<String,Object> course:list){
	    	    			Map<String,String> course_map = new HashMap<String,String>();
	    	    			MiscUtils.converObjectMapToStringMap(course, course_map);
	    	    			MiscUtils.courseTranferState(currentTime, course_map);
	    	    			course.put("status", course_map.get("status"));
	    	    			String lecturer_id = course_map.get("lecturer_id");
	    	    			if(!nickeNameMap.containsKey(lecturer_id)){
	    	    				String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, course_map);
		    	    			nickeNameMap.put(lecturer_id, pipeline.hget(key, "nick_name"));
	    	    			}
	    	    		}
	    	    		pipeline.sync();
	    	    		for(Map<String,Object> course:list){
	    	    			String lecturer_id =(String) course.get("lecturer_id");
	    	    			if(!MiscUtils.isEmpty(lecturer_id)){
	    	    				Response<String> response = nickeNameMap.get(lecturer_id);
	    	    				if(response != null)	course.put("nick_name", response.get());
	    	    			}
	    	    		}
	    			}
	    		});
	    	}
    	}
    	return result;
    }

    /**
     * TODO 有复用的方法 以后重构是需要进行合并
     * 获取讲师二维码/青柠二维码
     */
    public Map getQrCode(String lectureId, String userId, Jedis jedis,String appName) {
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
        } else {//2.判断是否有关注我们公众号
            Map<String,Object> map = userModuleServer.findLoginInfoByUserId(userId);
            if(Integer.parseInt(map.get("subscribe").toString())==0){//没有关注
                query.put("qr_code_url", MiscUtils.getConfigByKey("weixin_qr_code",appName));
                query.put("qr_code_title", MiscUtils.getConfigByKey("weixin_qr_code_title_qingning",appName));
                query.put("qr_code_message", MiscUtils.getConfigByKey("weixin_qr_code_message",appName));
                query.put("qr_code_ad", MiscUtils.getConfigByKey("weixin_qr_code_ad",appName));
                return query;
            }
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    @FunctionName("getCourseOrLiveRoom")
    public  Map<String, Object> getCourseOrLiveRoom(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();//获取参数
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String id = reqMap.get("id").toString();
        String type = reqMap.get("type").toString();
        if(type.equals("0")){//课程

        }else if(type.equals("1")){//直播间

        }else{
            throw new QNLiveException("000100");
        }
      return null;
    }

	/**
	 * 判断新增所有用户的gains
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
    @FunctionName("insertAllUserGains")
    public  Map<String, Object> insertAllUserGains(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();//获取参数
        Map<String, Object> resultMap = new HashMap<String, Object>();
        List<Map<String, Object>> insertGainsList = null;
        List<Map<String,Object>> userIdList = null;
        List<String> ids = new ArrayList<>();
		/*
		 * 获得存在与用户表t_user而不存在与t_user_gains的数据
		 */
        int page_num = 0;
        int page_count= 1000;
		while (true){

            userIdList = userModuleServer.findNotGainsUserId(page_num,page_count);
            if(userIdList == null || userIdList.isEmpty()){
                break;
            }
            page_num+=page_count;
            List<Map<String, Object>> insertList = new ArrayList<Map<String, Object>>();
            try {
                for (Map<String, Object> user : userIdList) {
                    boolean isQnlive = Constants.HEADER_APP_NAME.equals(user.get("app_name"));
                    Map<String, Object> gainsMap = new HashMap<>();
                    gainsMap.put("user_id", user.get("user_id"));
				/*
				 * 计算直播间总收入
				 */
                    String live_room_total_amount = user.get("live_room_total_amount").toString();
                    if (MiscUtils.isEmptyString(live_room_total_amount)) {
                        live_room_total_amount = "0";
                    }
                    gainsMap.put("live_room_total_amount", live_room_total_amount);
				/*
				 * 计算直播间实际收益
				 */
                    double roomReal = DoubleUtil.divide(Double.valueOf(live_room_total_amount), 100D);
                    double live_room_real_incomes = isQnlive ? roomReal : CountMoneyUtil.getCashInAmount(String.valueOf(roomReal));
                    gainsMap.put("live_room_real_incomes", (long) (live_room_real_incomes * 100));

				/*
				 * 分销总收入
				 */
                    String distributer_total_amount = user.get("distributer_total_amount").toString();
                    if (MiscUtils.isEmptyString(distributer_total_amount) || "null".equals(distributer_total_amount)) {
                        distributer_total_amount = "0";
                    }
                    gainsMap.put("distributer_total_amount", distributer_total_amount);
				/*
				 * 计算分销实际收益
				 */
                    double distributerReal = DoubleUtil.divide(Double.valueOf(distributer_total_amount), 100D);
                    double distributer_real_incomes = isQnlive ? distributerReal : CountMoneyUtil.getCashInAmount(String.valueOf(distributerReal));
                    gainsMap.put("distributer_real_incomes", (long) (distributer_real_incomes * 100));

				/*
				 * 计算用户总收入
				 */
                    gainsMap.put("user_total_amount", user.get("user_total_amount").toString());
				/*
				 * 计算用户实际收入
				 */
                    double user_total_real_incomes = live_room_real_incomes * 100 + distributer_real_incomes * 100;
                    gainsMap.put("user_total_real_incomes", (long) user_total_real_incomes);
				/*
				 * 计算余额
				 */
//                    String user_withdraw_sum = String.valueOf(userWithdrawSumMap.get(user.get("user_id")));
//                    if(MiscUtils.isEmptyString(user_withdraw_sum) || "null".equals(user_withdraw_sum)){
//                        user_withdraw_sum = "0";
//                    }
//                    double balance = user_total_real_incomes-Double.parseDouble(user_withdraw_sum);
                    gainsMap.put("balance", (long) user_total_real_incomes);
                    insertList.add(gainsMap);
                }
                userModuleServer.insertUserGains(insertList);
            }catch(Exception e){
                e.printStackTrace();
                throw e;
            }
		}
//        do{
//			int limit = 100;
//
//			if(userIdList == null || userIdList.isEmpty()){
//				break;
//			}
//
//
////			insertGainsList = CountMoneyUtil.getGaoinsList(userIdList, userRoomAmountList,
////					userDistributerAmountList, userWithdrawSumList);
//            List<Map<String, Object>> insertList = new ArrayList<Map<String, Object>>();
//            try{
//                for(Map<String, Object> user : userIdList){
//                    boolean isQnlive = Constants.HEADER_APP_NAME.equals(user.get("app_name"));
//                    Map<String, Object> gainsMap = new HashMap<>();
//                    gainsMap.put("user_id", user.get("user_id"));
//				/*
//				 * 计算直播间总收入
//				 */
//                    String live_room_total_amount = user.get("live_room_total_amount").toString();
//                    if(MiscUtils.isEmptyString(live_room_total_amount)){
//                        live_room_total_amount = "0";
//                    }
//                    gainsMap.put("live_room_total_amount", live_room_total_amount);
//				/*
//				 * 计算直播间实际收益
//				 */
//                    double roomReal = DoubleUtil.divide( Double.valueOf(live_room_total_amount),100D);
//                    double live_room_real_incomes = isQnlive?roomReal:CountMoneyUtil.getCashInAmount(String.valueOf(roomReal));
//                    gainsMap.put("live_room_real_incomes", (long)(live_room_real_incomes*100));
//
//				/*
//				 * 分销总收入
//				 */
//                    String distributer_total_amount = user.get("distributer_total_amount").toString();
//                    if(MiscUtils.isEmptyString(distributer_total_amount) || "null".equals(distributer_total_amount)){
//                        distributer_total_amount = "0";
//                    }
//                    gainsMap.put("distributer_total_amount", distributer_total_amount);
//				/*
//				 * 计算分销实际收益
//				 */
//                    double distributerReal = DoubleUtil.divide( Double.valueOf(distributer_total_amount),100D);
//                    double distributer_real_incomes = isQnlive?distributerReal:CountMoneyUtil.getCashInAmount(String.valueOf(distributerReal));
//                    gainsMap.put("distributer_real_incomes", (long)(distributer_real_incomes*100));
//
//				/*
//				 * 计算用户总收入
//				 */
//                    gainsMap.put("user_total_amount", user.get("user_total_amount").toString());
//				/*
//				 * 计算用户实际收入
//				 */
//                    double user_total_real_incomes = live_room_real_incomes*100 + distributer_real_incomes*100;
//                    gainsMap.put("user_total_real_incomes", (long)user_total_real_incomes);
//				/*
//				 * 计算余额
//				 */
////                    String user_withdraw_sum = String.valueOf(userWithdrawSumMap.get(user.get("user_id")));
////                    if(MiscUtils.isEmptyString(user_withdraw_sum) || "null".equals(user_withdraw_sum)){
////                        user_withdraw_sum = "0";
////                    }
////                    double balance = user_total_real_incomes-Double.parseDouble(user_withdraw_sum);
//                    gainsMap.put("balance", (long)user_total_real_incomes);
//                    insertList.add(gainsMap);
//                }
//                userModuleServer.insertUserGains(insertList);
//            }catch(Exception e){
//                e.printStackTrace();
//                throw e;
//            }
//
//
//        }while(userIdList != null);

        
      return resultMap;
    }

	/**
	 * 获取用户收入
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("userGains")
    public  Map<String, Object> userGains(RequestEntity reqEntity) throws Exception{
        String appName = reqEntity.getAppName();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> userGainsByUserId = userModuleServer.findUserGainsByUserId(userId);
        Map<String,Object> innerMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        innerMap.put("user_id", userId);
        Map<String,String> userMap = CacheUtils.readUser(userId, this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedis);
        userGainsByUserId.put("phone",userMap.get("phone_number"));
        if(MiscUtils.isEmpty(userGainsByUserId)){
            throw new QNLiveException("170001");
        }
        return userGainsByUserId;
    }


    /**
     * 发起提现申请
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("createWithdraw")
    public Map<String, Object> createWithdraw(RequestEntity reqEntity) throws Exception{
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	String nowStr = sdf.format(new Date());
    	Map<String, Object> resultMap = new HashMap<>();
        /*
    	1.验证登录用户的手机验证码
    	2.判断该登录账户是否拥有至少一笔处理中的提现，是则返回错误码
    	4.判断用户余额是否大于100
    	5.判断提现金额是否小于等于余额
    	6.插入提现申请表
        */
    	/*
    	 * 获取请求参数
    	 */
    	Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
    	//获取请求金额
        BigDecimal amount = BigDecimal.valueOf(DoubleUtil.mul(Double.valueOf(reqMap.get("initial_amount").toString()),100D));
        Integer initialAmount = amount.intValue();
        /*
    	 * 判断提现余额是否大于10000
    	 */
        if(initialAmount < 10000){
            logger.error("提现金额不能小于100元");
            throw new QNLiveException("170003");
        }else{
            reqMap.put("actual_amount",initialAmount);
        }
    	//获取登录用户userId
    	String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
    	reqMap.put("user_id", userId);
    	//获取app_name
    	String appName = (String) reqMap.get("app_name");

    	 //  验证登录用户的手机验证码

        String verification_code = reqMap.get("verification_code").toString();//验证码
       /* Map<String,String> phoneMap = new HashMap();
        phoneMap.put("user_id",userId);
        phoneMap.put("code",verification_code);
        String codeKey =  MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_CODE, phoneMap);//根据userId 拿到 key*/
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        CodeVerifyficationUtil.verifyVerificationCode(reqEntity.getAppName(),userId,verification_code,jedis);
    	/*
    	 * 判断该登录账户是否拥有至少一笔处理中的提现，是则返回错误码
    	 */
    	Map<String, Object> selectMap = new HashMap<>();
    	selectMap.put("user_id", userId);
    	selectMap.put("state", '0');
    	selectMap.put("page_num", 0);
    	selectMap.put("page_count", 1);
    	//获得该登录用户“提现申请中”的首条记录
    	Map<String, Object> withdrawingMap = userModuleServer.findWithdrawCashByMap(selectMap);
    	if(withdrawingMap != null && !withdrawingMap.isEmpty()){
    	//	logger.error("该用户已经有一条申请中的提现记录");
    		throw new QNLiveException("170002");
    	}
    	/*
    	 * 判断提现金额是否小于等于余额
    	 */
    	//获得登录用户的余额信息
    	Map<String, Object> loginUserGainsMap = userModuleServer.findUserGainsByUserId(userId);
    	int balance = 0;
    	if(loginUserGainsMap != null && !loginUserGainsMap.isEmpty()){
    		balance = Integer.parseInt(loginUserGainsMap.get("balance").toString());
    	}
    	if(initialAmount > balance){
    	    // logger.error("提现金额大于账户余额");
    		throw new QNLiveException("180001");
    	}
    	
    	//从缓存中获取用户信息
    	Map<String, String> loginUserMap = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedisUtils.getJedis(appName));
    	if(loginUserMap == null || loginUserMap.isEmpty()){
    		//登录用户不存在
    		throw new QNLiveException("000005");
    	}
    	Map<String, Object> insertMap = new HashMap<>();
    	insertMap.put("withdraw_cash_id", MiscUtils.getUUId());
    	insertMap.put("user_id", userId);
    	insertMap.put("user_name", reqMap.get("user_name"));
    	insertMap.put("nick_name", loginUserMap.get("nick_name"));
    	insertMap.put("user_phone", loginUserMap.get("phone_number"));
    	insertMap.put("alipay_account_number", reqMap.get("alipay_account_number"));
    	insertMap.put("initial_amount", amount);
    	insertMap.put("actual_amount", amount);
    	insertMap.put("state", '0');
    	insertMap.put("create_time", nowStr);
    	insertMap.put("update_time", nowStr);
    	insertMap.put("app_name", appName);
    	// 插入提现申请表
        try{
            balance = balance - initialAmount;
            userModuleServer.insertWithdrawCash(insertMap,balance);
        }catch(Exception e){
            logger.error("插入提现记录异常,UserID:"+userId+"提现金额:"+initialAmount);
            throw new QNLiveException("000099");
        }
		return resultMap;
    }
    
    /**
     * 获取提现记录列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getWithdrawList")
    public Map<String, Object> getWithdrawList(RequestEntity reqEntity) throws Exception{
    	Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> param = (Map)reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        param.put("userId",userId);
    	if(param.get("create_time")!=null){
            Date time = new Date(Long.valueOf(param.get("create_time").toString()));
            param.put("create_time",time);
        }
        resultMap.put("withdraw_info_list",userModuleServer.findWithdrawList(param));
		return resultMap;
    }
    /**
     * 获取提现记录列表-后台
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getWithdrawListAll")
    public Map<String, Object> getWithdrawListAll(RequestEntity reqEntity) throws Exception{
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> param = (Map)reqEntity.getParam();
        param.put("app_name", reqEntity.getAppName());
        
        /*
         * TODO 判断后台是否登录
         */
        
        /*
         * 查询提现记录列表
         */
		return userModuleServer.findWithdrawListAll(param);
    }

    /**
     * 后台_处理提现
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("handleWithDrawResult")
    public Map<String, Object> handleWithDrawResult(RequestEntity reqEntity) throws Exception{
    	Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> param = (Map)reqEntity.getParam();
        String withdrawId = param.get("withdraw_cash_id").toString();
        String remark = "";
        if(param.get("remark") != null){
        	remark = param.get("remark").toString();
        }
        String appName = reqEntity.getAppName();
        param.put("app_name", appName);
        String result = param.get("result").toString();
        
        /*
         * TODO 获取登录帐号
         */
        
        /*
         * 查询提现记录
         */
        Map<String, Object> selectMap = new HashMap<>();
        selectMap.put("app_name", appName);
        selectMap.put("withdraw_cash_id", withdrawId);
        Map<String, Object> withdraw = userModuleServer.selectWithdrawSizeById(selectMap);
        
        if(withdraw==null||!"0".equals(withdraw.get("state"))){
            //未找到提现记录或重复提现
            throw new QNLiveException("170004");
        }else {
            //同意提现，更新提现记录，用户余额
        	long initial_amount = Long.valueOf(withdraw.get("initial_amount").toString());
            userModuleServer.updateWithdraw(withdrawId, remark, 
            		withdraw.get("user_id").toString(), result, initial_amount);
        }
		return resultMap;
    }




    /**
     * 用户-查询系列列表（正在直播（用户查看））
     * @return
     * @throws Exception
     */
    @FunctionName("userSeries")
    public Map<String, Object> userSeries(RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<>();
        String appName = reqEntity.getAppName();
        String user_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用户id
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        Set<String> seriesIdSet;//查询的课程idset
        List<String> seriesIdList = new ArrayList<>();//课程id列表
        List<Map<String,String>> seriesList = new LinkedList<>();//课程对象列表
        String series_id = "";
        //1.先判断是否有 series_id 如果有判断是不是下架的
        if(!MiscUtils.isEmpty(reqMap.get("series_id"))){
            Map<String,String> query = new HashMap<String,String>();
            query.put(Constants.CACHED_KEY_SERIES_FIELD,reqMap.get("series_id").toString());
            String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, query);
            String updown = jedis.hget(seriesKey,"updown");
            if(updown.equals("2")){
                series_id = null;
            }else{
                series_id = reqMap.get("series_id").toString();
            }
        }
        String seriesListKey = Constants.CACHED_KEY_PLATFORM_SERIES_APP_PLATFORM;

        long startIndex = 0;//坐标起始位
        long endIndex = -1;//坐标结束位
        //判断用哪个缓存
        long endSeriesSum = jedis.zcard(seriesListKey);//获取总共有多少个结束课程
        if(MiscUtils.isEmpty(series_id)){//如果课程ID没有 那么就从最近结束的课程找起
            endIndex = -1;
            startIndex = endSeriesSum - pageCount;//利用总数减去我这边需要获取的数
            if(startIndex < 0){
                startIndex = 0;
            }
        }else{ //如果有课程id  先获取课程id 在列表中的位置 然后进行获取其他课程id
            long endRank = jedis.zrank(seriesListKey, series_id);
            endIndex = endRank - 1;
            if(endIndex >= 0){
                startIndex = endIndex - pageCount + 1;
                if(startIndex < 0){
                    startIndex = 0;
                }
            }
        }
        seriesIdSet = jedis.zrange(seriesListKey, startIndex, endIndex);
        for(String seriesId : seriesIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
            seriesIdList.add(seriesId);
        }
        List<Map<String, String>> maps = seriesList(seriesIdList, jedis);
        Collections.reverse(maps);
        resultMap.put("series_list",maps);
        return resultMap;
    }

    /**
     * 用户 直播间系列列表
     * @return
     * @throws Exception
     */
    @FunctionName("getRoomSeries")
    public Map<String, Object> getRoomSeries(RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<>();
        String appName = reqEntity.getAppName();
        String user_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用户id
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        String room_id = reqMap.get("room_id").toString();
        Map<String,String> query = new HashMap<String,String>();
        query.put(Constants.FIELD_ROOM_ID,room_id);
        String roomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, query);
        String lecturer_id = jedis.hget(roomKey,"lecturer_id");
        boolean isLecturer = false;//是否是讲师
        if(lecturer_id.equals(user_id)){//是讲师
            isLecturer = true;
        }
        String series_id = "";
        boolean seriesIsUp = true;//系列是上架 还是 下架
        if(!MiscUtils.isEmpty(reqMap.get("series_id"))){
            series_id = reqMap.get("series_id").toString();
            //1.先判断是否有 series_id 如果有判断是不是下架的
            Map<String,String> map = new HashMap<String,String>();
            map.put(Constants.CACHED_KEY_SERIES_FIELD,reqMap.get("series_id").toString());
            String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
            Map<String, String> series = jedis.hgetAll(seriesKey);
            if(series.get("updown").equals("2")){
                seriesIsUp = false;
            }
        }
        query.put(Constants.CACHED_KEY_SERVICE_LECTURER_FIELD,lecturer_id);
        query.put(Constants.SERIES_COURSE_TYPE,Constants.DEFAULT_SERIES_COURSE_TYPE);
        boolean key = true;
        Set<String> seriesIdSet;//查询的课程idset
        List<String> seriesIdList = new ArrayList<>();//课程id列表
        List<Map<String,String>> seriesList = new LinkedList<>();//课程对象列表
        do{
            long startIndex = 0;//坐标起始位
            long endIndex = -1;//坐标结束位
            String seriesListKey = "";
            //判断用哪个缓存
            if(seriesIsUp){
                seriesListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_UP, query);//上架
            }else if(isLecturer){
                seriesListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_DOWN, query);//下架
            }
            long endSeriesSum = jedis.zcard(seriesListKey);//获取总共有多少个结束课程
            if(MiscUtils.isEmpty(series_id)){//如果课程ID没有 那么就从最近结束的课程找起
                endIndex = -1;
                startIndex = endSeriesSum - pageCount;//利用总数减去我这边需要获取的数
                if(startIndex < 0){
                    startIndex = 0;
                }
            }else{ //如果有课程id  先获取课程id 在列表中的位置 然后进行获取其他课程id
                long endRank = jedis.zrank(seriesListKey, series_id);
                endIndex = endRank - 1;
                if(endIndex >= 0){
                    startIndex = endIndex - pageCount + 1;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                }else{
                    break;
                }
            }
            seriesIdSet = jedis.zrange(seriesListKey, startIndex, endIndex);
            List<String> transfer = new ArrayList<>();
            for(String seriesId : seriesIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                transfer.add(seriesId);
            }
            Collections.reverse(transfer);
            seriesIdList.addAll(transfer);
            pageCount -= seriesIdSet.size();
            if(pageCount > 0){
                if(seriesIsUp){
                    if(isLecturer){
                        seriesIsUp = false;
                        series_id = null;
                    }else{
                        key=false;
                    }
                }else{
                    key = false;
                }
            }else{
                key = false;
            }
        }while (key);
        resultMap.put("series_list",seriesList(seriesIdList,jedis));
        return resultMap;
    }

    /**
     * 在seriesList 加入讲师名字
     * @param seriesIdList
     * @param jedis
     * @return
     */
    private List<Map<String,String>> seriesList(List<String> seriesIdList,Jedis jedis) throws Exception {
        List<Map<String,String>> seriesList = new ArrayList<>();
        for(String seriesId : seriesIdList){
            Map<String,String> queryParam = new HashMap<String,String>();
            queryParam.put(Constants.CACHED_KEY_SERIES_FIELD, seriesId);
            Map<String, String> series = CacheUtils.readSeries(seriesId, generateRequestEntity(null, null, null, queryParam), readSeriesOperation, jedis, true);
            String lecturer_id = series.get("lecturer_id");
            Map<String,String> query = new HashMap<String,String>();
            query.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query);
            String nick_name = jedis.hget(key, "nick_name");
            series.put("lecturer_nick_name",nick_name);
            seriesList.add(series);
        }
        return seriesList;
    }



    /**
     * 查询系列详情
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("getSeriesDetailInfo")
    public Map<String, Object> getSeriesDetailInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, String> seriesMap = new HashMap<String, String>();
        String room_id = null;
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_SERIES_FIELD, reqMap.get("series_id").toString());
        String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
        //1.先从缓存中查询课程详情，如果有则从缓存中读取课程详情
        if (jedis.exists(seriesKey)) {
            Map<String, String> seriesCacheMap = jedis.hgetAll(seriesKey);
            seriesMap = seriesCacheMap;
            String lecturer_id = seriesCacheMap.get("lecturer_id").toString();

            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturer_id);
            String lecturerRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
            Map<String,String> liveRoomsMap = jedis.hgetAll(lecturerRoomKey);
            for(String roomId :liveRoomsMap.keySet()){
                room_id = roomId;
                break;
            }

        } else {
            //2.如果缓存中没有课程详情，则读取数据库
            Map<String, Object> seriesDBMap = userModuleServer.findSeriesBySeriesId(reqMap.get("series_id").toString());

            //3.如果缓存和数据库中都没有课程详情，则提示课程不存在
            if (CollectionUtils.isEmpty(seriesDBMap)) {
                throw new QNLiveException("100004");
            } else {
                MiscUtils.converObjectMapToStringMap(seriesDBMap, seriesMap);
                room_id = seriesDBMap.get("room_id").toString();
            }
        }
        if (CollectionUtils.isEmpty(seriesMap)) {
            throw new QNLiveException("100004");
        }
        for(String key:seriesMap.keySet()){
            resultMap.put(key, seriesMap.get(key));
        }

        Map<String,Object> queryMap = new HashMap<>();

        //从缓存中获取直播间信息
        Map<String, String> liveRoomMap = CacheUtils.readLiveRoom(room_id, reqEntity, readLiveRoomOperation, jedis, true);
        resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));
        resultMap.put("room_id", liveRoomMap.get("room_id"));

        queryMap.clear();
        queryMap.put("user_id", userId);
        queryMap.put("series_id",reqMap.get("series_id").toString());
        //判断访问者是普通用户还是讲师
        //如果为讲师，则返回讲师部分特定信息
        boolean isStudent = false;
        List<String> roles = new ArrayList<>();
        //为用户，则返回用户部分信息
        isStudent = userModuleServer.isStudentOfTheSeries(queryMap);
        //返回用户身份
        //角色数组 1：普通用户、2：学员、3：讲师
        //加入课程状态 0未加入 1已加入
        if(isStudent){
            resultMap.put("series_join_status", "1");
        }else {
            resultMap.put("series_join_status", "0");
        }
        //查询关注状态
        //关注状态 0未关注 1已关注
        reqMap.put("room_id", liveRoomMap.get("room_id"));
        Map<String, Object> fansMap = userModuleServer.findFansByUserIdAndRoomId(reqMap);
        if (CollectionUtils.isEmpty(fansMap)) {
            resultMap.put("follow_status", "0");
        } else {
            resultMap.put("follow_status", "1");
        }
        if(userId.equals(seriesMap.get("lecturer_id"))){
            roles.add("3");
        }else {
            if(isStudent){
                roles.add("2");
            }else {
                roles.add("1");
            }
        }
        Map<String,String> roomDistributer = CacheUtils.readDistributerRoom(userId, seriesMap.get("room_id"), readRoomDistributerOperation, jedis);
        if(! MiscUtils.isEmpty(roomDistributer)){
            if(roomDistributer.get("end_date") != null){
                Date endDate = new Date(Long.parseLong(roomDistributer.get("end_date")));
                Date todayEndDate = MiscUtils.getEndDateOfToday();
                if(endDate.getTime() >= todayEndDate.getTime()){
                    roles.add("4");
                }
            }
        }
        resultMap.put("roles", roles);
        resultMap.put("qr_code",getQrCode(seriesMap.get("lecturer_id"),userId,jedis,appName));
        resultMap.put("series_status",seriesMap.get("series_status"));
        return resultMap;
    }



    /**
     * 查询系列课程
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("getSerieCourse")
    public Map<String, Object> getSerieCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String user_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        long pageCount = Long.valueOf(reqMap.get("page_count").toString());
        String series_id = reqMap.get("series_id").toString();
        Map<String,String> query = new HashMap<String,String>();
        query.put(Constants.CACHED_KEY_SERIES_FIELD,reqMap.get("series_id").toString());
        String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, query);
        String lecturer_id = jedis.hget(seriesKey, "lecturer_id");
        boolean isLecturer = false;//是否是讲师
        if(lecturer_id.equals(user_id)){//是讲师
            isLecturer = true;
        }
        String course_id = "";
        boolean courseIsUp = true;
        if(!MiscUtils.isEmpty(reqMap.get("course_id"))){
            course_id = reqMap.get("course_id").toString();
            //1.先判断是否有 series_id 如果有判断是不是下架的
            Map<String,String> map = new HashMap<String,String>();
            map.put(Constants.CACHED_KEY_COURSE_FIELD,course_id);
            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
            Map<String, String> course = jedis.hgetAll(courseKey);
            if(course.get("series_course_updown").equals("2")){//判断课程是否上架
                if(isLecturer){
                    courseIsUp = false;
                }else{
                    course_id = null;
                }
            }else if(course.get("series_course_updown").equals("0")){
                throw new QNLiveException("100004");
            }else if(!course.get("series_id").equals(series_id)){ //判断传过来的课程是否是当前系列
                throw new QNLiveException("210002");
            }
        }

        Set<String> seriesCourseIdSet;//查询的课程idset
        List<String> seriesCourseIdList = new ArrayList<>();//课程id列表
        List<Map<String,String>> seriesCourseList = new LinkedList<>();//课程对象列表
        boolean key=true;
        do{
            long startIndex = 0;//坐标起始位
            long endIndex = -1;//坐标结束位
            String seriesCourseListKey = "";
            //判断用哪个缓存
            if(courseIsUp){
                seriesCourseListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_UP, query);//上架
            }else if(isLecturer){
                seriesCourseListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_DOWN, query);//下架
            }
            long endSeriesCourseSum = jedis.zcard(seriesCourseListKey);//获取总共有多少个结束课程
            if(MiscUtils.isEmpty(course_id)){//如果课程ID没有 那么就从最近结束的课程找起
                endIndex = -1;
                startIndex = endSeriesCourseSum - pageCount;//利用总数减去我这边需要获取的数
                if(startIndex < 0){
                    startIndex = 0;
                }
            }else{ //如果有课程id  先获取课程id 在列表中的位置 然后进行获取其他课程id
                long endRank = jedis.zrank(seriesCourseListKey, course_id);
                endIndex = endRank - 1;
                if(endIndex >= 0){
                    startIndex = endIndex - pageCount + 1;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                }else{

                }
            }
            seriesCourseIdSet = jedis.zrange(seriesCourseListKey, startIndex, endIndex);
            List<String> transfer = new ArrayList<>();
            for(String seriesId : seriesCourseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                transfer.add(seriesId);
            }
            Collections.reverse(transfer);
            seriesCourseIdList.addAll(transfer);
            pageCount -= seriesCourseIdSet.size();
            if(pageCount > 0){
                if(courseIsUp){
                    if(isLecturer){
                        courseIsUp = false;
                        course_id = null;
                    }else{//学生
                        key=false;
                    }
                }else{
                    key = false;
                }
            }else{
                key = false;
            }
        }while (key);

        if(seriesCourseIdList.size() > 0){
            Map<String,Object> map = new HashMap<String,Object>();
            map.put(Constants.CACHED_KEY_USER_FIELD, user_id);
            String userCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, map);//用来查询当前用户加入了那些课程
            long now = System.currentTimeMillis();
            for(String courseId : seriesCourseIdList){
                Map<String,String> queryParam = new HashMap<String,String>();
                queryParam.put("course_id", courseId);
                Map<String, String> courseInfoMap =  CacheUtils.readCourse(courseId, generateRequestEntity(null, null, null, queryParam), readCourseOperation, jedis, false);//从缓存中读取课程信息
                MiscUtils.courseTranferState(now, courseInfoMap);//进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                if(jedis.sismember(userCourseKey, courseId)){//判断当前用户是否有加入这个课程
                    courseInfoMap.put("student", "Y");
                } else {
                    courseInfoMap.put("student", "N");
                }
                String lecturerId = courseInfoMap.get("lecturer_id");
                map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
                String nick_name = jedis.hget(lecturerKey, "nick_name");
                courseInfoMap.put("lecturer_nick_name",nick_name);
                seriesCourseList.add(courseInfoMap);
            }
        }
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("course_list",seriesCourseList);
        return resultMap;
    }


    /**
     * 查询用户加入的系列列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("studySeries")
    public  Map<String, Object> getStudySeries(RequestEntity reqEntity) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        String userKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, reqMap);
        Map<String,String> values = null;
        if(jedis.exists(userKey)) {
            values =  CacheUtils.readUser(userId, generateRequestEntity(null, null, null, reqMap), readUserOperation, jedis);
        }else{
            values =  jedis.hgetAll(userKey);
        }
       // jedis.del(userKey);
        long series_num = 0;
        try{
            series_num = Long.parseLong(values.get("series_num").toString());
        }catch(Exception e){
            series_num = 0;
        }
        Map<String,Object> result = new HashMap<String,Object>();
        result.put("series_num", series_num);
        if(series_num>0){
            long page_count = Long.valueOf(reqMap.get("page_count").toString());
            Map<String,Object> queryMap = new HashMap<String,Object>();
            queryMap.put("user_id", userId);
            Set<String> userSeriesIdSet = new HashSet<>();

            long startIndex = 0;//坐标起始位
            long endIndex = -1;//坐标结束位
            String userSeriesListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_SERIES, queryMap);
            //判断用哪个缓存

            long endSeriesCourseSum = jedis.zcard(userSeriesListKey);
            if(MiscUtils.isEmpty(reqMap.get("series_id"))){
                endIndex = -1;
                startIndex = endSeriesCourseSum - page_count;
                if(startIndex < 0){
                    startIndex = 0;
                }
            }else{
                String series_id = reqMap.get("series_id").toString();
                long endRank = jedis.zrank(userSeriesListKey, series_id);
                endIndex = endRank - 1;
                if(endIndex >= 0){
                    startIndex = endIndex - page_count + 1;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                }
            }
            userSeriesIdSet = jedis.zrange(userSeriesListKey, startIndex, endIndex);
            ArrayList<String> transfer = new ArrayList<>(userSeriesIdSet);
            Collections.reverse(transfer);
            result.put("series_list", seriesList(transfer ,jedis));
        }
        return result;
    }


}