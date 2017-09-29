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


import qingning.common.entity.RequestEntity;
import qingning.common.util.*;

import qingning.server.AbstractQNLiveServer;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.IUserUserModuleServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import sun.misc.Request;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
public class UserServerImpl extends AbstractQNLiveServer {

    private IUserUserModuleServer userModuleServer;

    private static Logger logger = LoggerFactory.getLogger(UserServerImpl.class);

    @Override
    public void initRpcServer() {
        if (userModuleServer == null) {
            userModuleServer = this.getRpcService("userModuleServer");

            readCourseOperation = new ReadCourseOperation(userModuleServer);
            readUserOperation = new ReadUserOperation(userModuleServer);
            readLecturerOperation = new ReadLecturerOperation(userModuleServer);
            readSeriesOperation = new ReadSeriesOperation(userModuleServer);
            readShopOperation = new ReadShopOperation(userModuleServer);
            readConfigOperation = new ReadConfigOperation(userModuleServer);

        }
    }

    /**
     * 获取已购买的系列课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("mySeriesCourseList")
    public Map<String, Object> mySeriesCourseList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, String>> seriesInfoList = new ArrayList<>();
        /*
         * 获取请求参数
         */
        Map<String, Object> reqMap = (Map)reqEntity.getParam();
        String loginedUserId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", loginedUserId);
        String lastCourseId = (String) reqMap.get("last_course_id");
        int pageCount = ((Long)reqMap.get("page_count")).intValue();
        Jedis jedis = jedisUtils.getJedis();
        String shopId = (String) reqMap.get("shop_id");
        long now = System.currentTimeMillis();
        //读取缓存必备参数
        Map<String, Object> readCacheMap = new HashMap<>();
        RequestEntity readCacheReqEntity = this.generateRequestEntity(null, null, null, readCacheMap);

        /*
         * 分页获取用户加入的系列课程id列表
         */
        Set<String> seriesIdSet;
        readCacheMap.clear();
        readCacheMap.put("user_id", loginedUserId);
        if (MiscUtils.isEmpty(shopId)) {    //请求中没有传递店铺id，说明获取用户在全平台下的已购系列
            readCacheReqEntity.setFunctionName("getSeriesStudentListByMap");
            seriesIdSet = CacheUtils.readUserSeriesIdSet(loginedUserId, lastCourseId, pageCount,
                    readCacheReqEntity, readUserOperation, jedis);
        } else {    //请求中传递店铺id，说明获取用户在该店铺下的已购系列
            /*
             * 根据请求店铺获得该店铺的讲师id
             */

            Map<String, Object> readShopMap = new HashMap<>();
            readShopMap.put("shop_id", shopId);
            Map<String, String> shopInfo = readShop(shopId, readShopMap, "getShopInfoByMap", false, jedis);
            if (MiscUtils.isEmpty(shopInfo)) {
                logger.error("获取已购买的系列课程>>>>请求的店铺不存在");
                throw new QNLiveException("190001");
            }
            readCacheMap.put("lecturer_id", shopInfo.get("user_id"));
            readCacheReqEntity.setFunctionName("getSeriesStudentListByMap");
            seriesIdSet = readUserShopSeriesIdSet(loginedUserId, shopId, lastCourseId, pageCount,
                    readCacheReqEntity, readUserOperation, jedis);
        }

        if (!MiscUtils.isEmpty(seriesIdSet)) {
            logger.info("获取已购买的系列课程>>>>分页获取到用户加入的系列课程id列表：" + seriesIdSet.toString());
            /*
             * 遍历课程id列表获取课程详情
             */
            readCacheMap.clear();
            Map<String, String> seriesInfo;
            for (String seriesId : seriesIdSet) {
                readCacheMap.put("series_id", seriesId);
                seriesInfo = readSeries(seriesId, this.generateRequestEntity(null,null,null,readCacheMap), readSeriesOperation, jedis, true);                if (!MiscUtils.isEmpty(seriesInfo)) {
                    seriesInfoList.add(seriesInfo);
                } else {
                    logger.error("获取已购买的系列课程>>>>遍历课程id列表获取课程详情发现课程不存在，series_id=" + seriesId);
                }
            }
        }

        resultMap.put("series_info_list", seriesInfoList);
        return resultMap;
    }

   /**
     * 获取已购买的单品课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("mySingleCourseList")
    public Map<String, Object> mySingleCourseList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, String>> courseInfoList = new ArrayList<>();
        /*
         * 获取请求参数
         */
        Map<String, Object> reqMap = (Map)reqEntity.getParam();
        //获取登录用户id
        String loginedUserId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", loginedUserId);
        //获取请求中的分页标识（上一页最后一门课程id）
        String lastCourseId = (String) reqMap.get("last_course_id");
        int pageCount = ((Long)reqMap.get("page_count")).intValue();
        Jedis jedis = jedisUtils.getJedis();
        String shopId = (String) reqMap.get("shop_id");
        long now = System.currentTimeMillis();
        //读取缓存必备参数
        Map<String, Object> readCacheMap = new HashMap<>();
        RequestEntity readCacheReqEntity = this.generateRequestEntity(null, null, null, readCacheMap);

        /*
         * 分页获取用户加入的单品课程id列表
         */
        Set<String> courseIdSet;
        readCacheMap.clear();
        readCacheMap.put("user_id", loginedUserId);
        if (MiscUtils.isEmpty(shopId)) {	//请求中没有传递店铺id，说明获取用户在全平台下的已购单品
            readCacheReqEntity.setFunctionName("getCourseStudentListByMap");
            courseIdSet = readUserCourseIdSet(loginedUserId, lastCourseId, pageCount,
                    readCacheReqEntity, readUserOperation, jedis);
        } else {	//请求中传递店铺id，说明获取用户在该店铺下的已购单品
        	/*
        	 * 根据请求店铺获得该店铺的讲师id
        	 */
            Map<String, Object> readShopMap = new HashMap<>();
            readShopMap.put("shop_id", shopId);
            Map<String, String> shopInfo = readShop(shopId, readShopMap, "getShopInfoByMap", false, jedis);
            if (MiscUtils.isEmpty(shopInfo)) {
                logger.error("获取已购买的单品课程>>>>请求的店铺不存在");
                throw new QNLiveException("190001");
            }
        	/*
        	 * 查找用户在指定店铺加入的单品课程id列表
        	 */
            readCacheMap.put("lecturer_id", shopInfo.get("user_id"));
            readCacheReqEntity.setFunctionName("getCourseStudentListByMap");
            courseIdSet = readUserShopCourseIdSet(loginedUserId, shopId, lastCourseId, pageCount,
                    readCacheReqEntity, readUserOperation, jedis);
        }

        if (!MiscUtils.isEmpty(courseIdSet)) {
            logger.info("获取已购买的单品课程>>>>分页获取到用户加入的单品课程id列表：" + courseIdSet.toString());
        	/*
        	 * 遍历课程id列表获取课程详情
        	 */
        	readCacheMap.clear();
        	readCacheReqEntity.setFunctionName(null);
            Map<String, String> courseInfo;
            for (String courseId : courseIdSet) {
                readCacheMap.put("course_id", courseId);
                courseInfo = readCourse(courseId, readCacheReqEntity, readCourseOperation, jedis, true);
                if (!MiscUtils.isEmpty(courseInfo)) {
                    //直播课程进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                    if ("0".equals(courseInfo.get("goods_type"))) {
                        MiscUtils.courseTranferState(now, courseInfo);
                    }
                    courseInfoList.add(courseInfo);
                } else {
                    logger.error("获取已购买的单品课程>>>>遍历课程id列表获取课程详情发现课程不存在，course_id=" + courseId);
                }
            }
        }

        resultMap.put("course_info_list", courseInfoList);
        return resultMap;

//        /*
//         * 分页获取用户加入的单品课程id列表
//         */
//        //生成分页索引key，详细分页key
//        String courseIndexKey, coursePageKey;
//        readCacheMap.clear();
//        readCacheMap.put(Constants.CACHED_KEY_USER_FIELD, loginedUserId);
//        if (MiscUtils.isEmpty(shopId)) {    //请求中没有传递店铺id，说明获取用户在全平台下的已购单品
//            courseIndexKey = MiscUtils.getKeyOfCachedData(, readCacheMap);
//        } else {    //请求中传递店铺id，说明获取用户在该店铺下的已购单品
//
//        }
//
//        List<String> courseIdList = SortUtils.getCourses();
//
//        /*
//         * 根据课程id列表获取课程详情
//         */
//        if (!MiscUtils.isEmpty(courseIdList)) {
//            logger.info("获取已购买的单品课程>>>>分页获取到用户加入的单品课程id列表：" + courseIdList.toString());
//            /*
//             * 遍历课程id列表获取课程详情
//             */
//            readCacheMap.clear();
//            readCacheReqEntity.setFunctionName("getCourseByCourseId");
//            Map<String, String> courseInfo;
//            for (String courseId : courseIdList) {
//                courseInfo = readCourse(courseId, readCacheReqEntity, readCourseOperation, jedis, true);
//                if (!MiscUtils.isEmpty(courseInfo)) {
//                    if ("0".equals(courseInfo.get("goods_type"))) { //课程是直播课，需要判断直播状态
//                        //进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
//                        MiscUtils.courseTranferState(now, courseInfo);
//                    }
//                    courseInfoList.add(courseInfo);
//                } else {
//                     logger.error("获取已购买的单品课程>>>>遍历课程id列表获取课程详情发现课程不存在，course_id=" + courseId);
//                }
//            }
//        }
//
//        resultMap.put("course_info_list", courseInfoList);
//        return resultMap;
    }


    /** 加入课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("joinCourse")
    public Map<String, Object> joinCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        //1.1读取课程信息
        String course_id = (String)reqMap.get("course_id");
        String query_type = reqMap.get("query_type").toString();
        map.put("course_id", course_id);
        Map<String, String> courseInfoMap = new HashMap<>();
        courseInfoMap = readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);
        if(MiscUtils.isEmpty(courseInfoMap)){
            throw new QNLiveException("100004");
        }
        String shopId = courseInfoMap.get("shop_id");
        //1.2检测该用户是否为讲师，为讲师则不能加入该课程
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //如果是app 就判断是否是讲师
        if(query_type.equals("0")){
            if(userId.equals(courseInfoMap.get("lecturer_id"))){
                throw new QNLiveException("210006");
            }
        }

        //2.检测课程验证信息是否正确
        //2.1如果课程为私密课程则检验密码

        String course_type = courseInfoMap.get("course_type");

        if(!MiscUtils.isEmpty(courseInfoMap.get("series_id"))){
            Map<String,Object> queryMap = new HashMap<>();
            queryMap.put("user_id", userId);
            queryMap.put("series_id",courseInfoMap.get("series_id").toString());
            //判断访问者是普通用户还是讲师
            String userJoinSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_SERIES, queryMap);//用来查询当前用户加入了那些课程
            if (!MiscUtils.isEmpty(jedis.zrank(userJoinSeriesKey, courseInfoMap.get("series_id").toString()))) {//判断当前用户是否有加入这个课程
                course_type = "0";
            }
        }

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



        //3.检测学生是否参与了该课程
        Map<String,Object> studentQueryMap = new HashMap<>();
        studentQueryMap.put("user_id",userId);
        studentQueryMap.put("course_id",course_id);
        if(userModuleServer.isStudentOfTheCourse(studentQueryMap)){
            throw new QNLiveException("1000035");
        }

        //5.将学员信息插入到学员参与表中
        courseInfoMap.put("user_id",userId);
        userModuleServer.joinCourse(courseInfoMap);

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
        userModuleServer.increaseStudentNumByCourseId(reqMap.get("course_id").toString());
        jedis.del(courseKey);

        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_USER_FIELD, userId);
        keyMap.put(Constants.CACHED_KEY_SHOP_FIELD, shopId);
        String userShopCoursesSetKey = MiscUtils.getKeyOfCachedData(Constants.USER_SHOP_COURSE_ZSET, keyMap);
        jedis.zadd(userShopCoursesSetKey, System.currentTimeMillis(), map.get("course_id").toString());//用户加入指定店铺的课程列表

//        HashMap<String,Object> queryMap = new HashMap<>();
//        queryMap.put("course_id",course_id);
//        Map<String,String> courseMap = readCourse(course_id, generateRequestEntity(null, null, null, queryMap), readCourseOperation, jedis, true);
//        if(courseMap.get("goods_type").equals("0")) {
//            switch (courseMap.get("status")) {
//                case "1":
//                    MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);//更新时间
//                    if (courseMap.get("status").equals("4")) {
//                        jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_LIVE, 1, course_id);
//                    } else {
//                        jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_PREDICTION, 1, course_id);
//                    }
//                    break;
//                case "2":
//                    jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_FINISH, 1, course_id);
//                    break;
//            }
//        }


        //7.修改用户缓存信息中的加入课程数
        map.clear();
        map.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, courseInfoMap);//删除已加入课程的key  在之后登录时重新加入
        jedis.del(key);//删除
//            nowStudentNum = MiscUtils.convertObjectToLong(courseInfoMap.get("student_num")) + 1;
//            String levelString = MiscUtils.getConfigByKey("jpush_course_students_arrive_level");
//            JSONArray levelJson = JSON.parseArray(levelString);
//            if (levelJson.contains(nowStudentNum + "")) {
//                JSONObject obj = new JSONObject();
//                //String course_type = courseMap.get("course_type");
//                String course_type_content = MiscUtils.convertCourseTypeToContent(course_type);
//                obj.put("body", String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content"), course_type_content, MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")), nowStudentNum + ""));
//                obj.put("to", courseInfoMap.get("lecturer_id"));
//                obj.put("msg_type", "7");
//                Map<String, String> extrasMap = new HashMap<>();
//                extrasMap.put("msg_type", "7");
//                extrasMap.put("course_id", courseInfoMap.get("course_id"));
//                obj.put("extras_map", extrasMap);
//                JPushHelper.push(obj);
//            }
            //公开课 执行这段逻辑
            if (courseInfoMap.get("goods_type").equals("0") && courseInfoMap.get("course_type").equals("0")) {
                //一个用户进入加入直播间带入1到2个人进入
                map.clear();
                map.put("course_id", course_id);
                RequestEntity mqRequestEntity = new RequestEntity();
                mqRequestEntity.setServerName("CourseRobotService");
                mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                mqRequestEntity.setFunctionName("courseHaveStudentIn");
                mqRequestEntity.setParam(map);
                this.mqUtils.sendMessage(mqRequestEntity);
            }

            jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, courseInfoMap.get("lecturer_id").toString());
            //</editor-fold>
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("getUserConsumeRecords")
    public Map<String, Object> getUserConsumeRecords(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> queryMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
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
                        cacheQueryMap.put(Constants.CACHED_KEY_USER_FIELD, recordMap.get("lecturer_id"));
                        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, cacheQueryMap);
                        //获得讲师的昵称
                        Response<String> cacheLecturerName = pipeline.hget(lecturerKey, "nick_name");
                        recordMap.put("cacheLecturerName",cacheLecturerName);

                        cacheQueryMap.clear();
                        cacheQueryMap.put(Constants.CACHED_KEY_COURSE_FIELD, recordMap.get("course_id"));
                        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, cacheQueryMap);
                        Response<String> courseName = pipeline.hget(courseKey, "course_title");
                        //获得课程的标题
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
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
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
                        cacheQueryMap.clear();
                        cacheQueryMap.put(Constants.CACHED_KEY_COURSE_FIELD, recordMap.get("course_id"));
                        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, cacheQueryMap);
                        Response<String> courseName = pipeline.hget(courseKey, "course_title");
                        recordMap.put("courseTitle",courseName);
                    }
                    pipeline.sync();

                    for(Map<String,Object> recordMap : records){
                        Response<String> courseName = (Response)recordMap.get("courseTitle");
                        if(recordMap.get("series_title")==null) {
                            recordMap.put("course_title",courseName.get());
                        }
                        //打赏收益
                        /*if("1".equals(recordMap.get("profit_type").toString())){
                            ""
                        }*/
                        if(recordMap.get("buy_name")==null){
                            System.out.println(recordMap.get("user_id"));
                        }
                        //商品类型
                        String type;
                        if(recordMap.get("profit_type").toString().equals("2")){
                            type = "系列";
                        }else if("1".equals(recordMap.get("course_type").toString())){
                            type = "直播";
                        }else{
                            type = "单品";
                        }
                        //操作类型
                        String typeIn;
                        if(recordMap.get("profit_type").toString().equals("1")){
                            typeIn = "打赏";
                        }else{
                            typeIn = "购买";
                        }
                        StringBuffer title = new StringBuffer();
                        if(recordMap.get("distributer_id")!=null){
                            //分销收入
                            recordMap.put("is_share","1");
                            title.append(recordMap.get("buy_name")).append("  通过  ")
                                    .append(recordMap.get("dist_name")).append("  ").append(typeIn).append(type).append("【").append(recordMap.get("course_title")).append("】").append("分销比例为");
                            double rate = 1D-(DoubleUtil.divide(Double.valueOf(recordMap.get("share_amount").toString()),Double.valueOf(recordMap.get("profit_amount").toString()),2));                            title.append(rate*100).append("%");
                            recordMap.put("profit_amount",(Long.valueOf(recordMap.get("profit_amount").toString())-Long.valueOf(recordMap.get("share_amount").toString())));
                            recordMap.put("title",title.toString());

                        }else{
                            //购买，打赏
                            recordMap.put("is_share","0");
                            title.append(recordMap.get("buy_name")).append("  ").append(typeIn).append(type).append("【").append(recordMap.get("course_title")).append("】");
                            recordMap.put("title",title.toString());
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


    /**
     * 获取用户收入
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("userGains")
    public  Map<String, Object> userGains(RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> userGains = userModuleServer.findUserGainsByUserId(userId);
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象

        Map<String,Object> innerMap = new HashMap<>();
        innerMap.put("user_id", userId);
        Map<String,String> userMap = readUser(userId, this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedis);
        if (userMap.get("shop_id")!=null) {//有店铺

            //直播间信息查询
            Long user_total_real_incomes = Long.valueOf(userGains.get("user_total_real_incomes").toString());
            Long distributer_real_incomes = Long.valueOf(userGains.get("distributer_real_incomes").toString());

            userGains.put("shop_total_amount",user_total_real_incomes-distributer_real_incomes);


            userGains.put("has_shop","1");
        }else{//没店铺
            userGains.put("has_shop","0");
            //直播间+分销收入计算
            Long userTotalRealIncome = Long.valueOf(userGains.get("live_room_real_incomes").toString())+Long.valueOf(userGains.get("distributer_real_incomes").toString());
            Long userTotalIncome = Long.valueOf(userGains.get("live_room_total_amount").toString())+Long.valueOf(userGains.get("distributer_total_amount").toString());
            userGains.put("user_total_real_incomes",userTotalRealIncome);
            userGains.put("user_total_amount",userTotalIncome);
        }
        userGains.put("phone",userMap.get("phone_number"));
        if(MiscUtils.isEmpty(userGains)){
            throw new QNLiveException("170001");
        }
        return userGains;
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
        //BigDecimal amount = BigDecimal.valueOf(DoubleUtil.mul(Double.valueOf(reqMap.get("initial_amount").toString()),100D));
        BigDecimal amount = new BigDecimal(reqMap.get("initial_amount").toString()).multiply(new BigDecimal("100"));
        //超出最大提现金额
        if(amount.compareTo(new BigDecimal("100000000"))==1){
            throw new QNLiveException("170005");
        }
         /*
    	 * 判断提现余额是否大于10000
    	 */
        if(amount.compareTo(new BigDecimal("10000"))==-1){
            logger.error("提现金额不能小于100元");
            throw new QNLiveException("170003");
        }else{
            reqMap.put("actual_amount",amount.longValue());
        }
        //获取登录用户userId
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);

        //  验证登录用户的手机验证码

        String verification_code = reqMap.get("verification_code").toString();//验证码
       /* Map<String,String> phoneMap = new HashMap();
        phoneMap.put("user_id",userId);
        phoneMap.put("code",verification_code);
        String codeKey =  MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_CODE, phoneMap);//根据userId 拿到 key*/
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        CodeVerifyficationUtil.verifyVerificationCode(userId,verification_code,jedis);
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
        if(amount.compareTo(new BigDecimal(balance))==-1){
            // logger.error("提现金额大于账户余额");
            throw new QNLiveException("180001");
        }

        //从缓存中获取用户信息
        Map<String, String> loginUserMap = readUser(userId, reqEntity, readUserOperation, jedisUtils.getJedis());
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
        //实际提现金额
        BigDecimal actualAmount = BigDecimal.valueOf(DoubleUtil.sub(amount.doubleValue() , DoubleUtil.mul(amount.doubleValue(),Constants.SYS_WX_RATE)));
        insertMap.put("actual_amount", actualAmount.longValue());
        insertMap.put("state", '0');
        insertMap.put("create_time", nowStr);
        // 插入提现申请表
        try{
            //balance = balance - initialAmount;
            userModuleServer.insertWithdrawCash(insertMap,new BigDecimal(balance).subtract(amount).longValue());
        }catch(Exception e){
            logger.error("插入提现记录异常,UserID:"+userId+"提现金额:"+amount.toString());
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
     * 获取提现记录列表-SaaS
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getWithdrawListSaaS")
    public Map<String, Object> getWithdrawListSaaS(RequestEntity reqEntity) throws Exception{
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> param = (Map)reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        param.put("user_id",userId);
        /*
         * 查询提现记录列表
         */
        return userModuleServer.findWithdrawListAll(param);
    }






}
