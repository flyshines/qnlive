package qingning.user.server.imp;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.util.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.IUserUserModuleServer;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        courseInfoMap.put("value_from",query_type);
        map.put(Constants.CACHED_KEY_LECTURER_FIELD,  courseInfoMap.get("lecturer_id"));
        String lecturerRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);

        Map<String,String> liveRoomsMap = jedis.hgetAll(lecturerRoomKey);

        for(String roomId :liveRoomsMap.keySet()){
            courseInfoMap.put("room_id",roomId);
            break;
        }
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

            Long nowStudentNum = 0L;
            userModuleServer.increaseStudentNumByCourseId(reqMap.get("course_id").toString());
            jedis.del(courseKey);
            HashMap<String,Object> queryMap = new HashMap<>();
            queryMap.put("course_id",course_id);
            Map<String,String> courseMap = readCourse(course_id, generateRequestEntity(null, null, null, queryMap), readCourseOperation, jedis, true);
            if(courseMap.get("goods_type").equals("0")) {
                switch (courseMap.get("status")) {
                    case "1":
                        MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);//更新时间
                        if (courseMap.get("status").equals("4")) {
                            jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_LIVE, 1, course_id);
                        } else {
                            jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_PREDICTION, 1, course_id);
                        }
                        break;
                    case "2":
                        jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_FINISH, 1, course_id);
                        break;
                }
            }


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


























































}
