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
import qingning.server.rpc.manager.ILectureModuleServer;
import qingning.server.rpc.manager.IUserModuleServer;
import qingning.user.server.other.ReadLiveRoomOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;
import sun.misc.Cache;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

public class UserServerImpl extends AbstractQNLiveServer {

    private IUserModuleServer userModuleServer;

    private ReadLiveRoomOperation readLiveRoomOperation;

    @Override
    public void initRpcServer() {
        if (userModuleServer == null) {
            userModuleServer = this.getRpcService("userModuleServer");

            readLiveRoomOperation = new ReadLiveRoomOperation(userModuleServer);
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
        if(!jedis.exists(roomKey)){
            throw new QNLiveException("100002");
        }
        String lecturerId = jedis.hget(roomKey, "lecturer_id");
        reqMap.put("lecturer_id",lecturerId);


        Map<String,Object> dbResultMap = userModuleServer.userFollowRoom(reqMap);
        if(dbResultMap == null || dbResultMap.get("update_count") == null || dbResultMap.get("update_count").toString().equals("0")){
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
        if(reqMap.get("follow_type").toString().equals("0")){
            incrementNum = 1;
        }else {
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
        if(reqMap.get("query_type").toString().equals("1")){
            return getUserJoinCourses(reqEntity);
        }else if(reqMap.get("query_type").toString().equals("2")){
            return getPlatformCourses (reqEntity);
        }
        return null;
    }

    /**
     * 查询用户加入的课程
     * @param reqEntity
     * @return
     */
    private Map<String, Object> getUserJoinCourses(RequestEntity reqEntity) {
        return null;
    }


    /**
     * 查询平台所有的课程
     * @param reqEntity
     * @return
     */
    private Map<String, Object> getPlatformCourses(RequestEntity reqEntity) {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);




        return resultMap;
    }


    /**
     * 查询直播间基本信息
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
        Map<String,String> infoMap = CacheUtils.readLiveRoom(reqMap.get("room_id").toString(), reqEntity, readLiveRoomOperation, jedisUtils, true);
        if(CollectionUtils.isEmpty(infoMap)){
            throw new QNLiveException("100002");
        }

        //查询关注信息 //TODO 先查询数据库，后续确认是否查询缓存
        //关注状态 0未关注 1已关注
        Map<String,Object> fansMap = userModuleServer.findFansByFansKey(reqMap);
        if(CollectionUtils.isEmpty(fansMap)){
            resultMap.put("follow_status","0");
        }else {
            resultMap.put("follow_status","1");
        }

        resultMap.put("avatar_address",infoMap.get("avatar_address"));
        resultMap.put("room_name",infoMap.get("room_name"));
        resultMap.put("room_remark",infoMap.get("room_remark"));
        resultMap.put("fans_num",infoMap.get("fans_num"));

        return resultMap;
    }


    /**
     * 查询课程详情
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
        if(CollectionUtils.isEmpty(courseMap)){
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
        Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(room_id, reqEntity, readLiveRoomOperation, jedisUtils, true);
        resultMap.put("room_avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("room_name", liveRoomMap.get("room_name"));
        resultMap.put("room_remark", liveRoomMap.get("room_remark"));

        //TODO 查询加入课程状态

        //查询关注状态
        //关注状态 0未关注 1已关注
        reqMap.put("room_id", liveRoomMap.get("room_id"));
        Map<String,Object> fansMap = userModuleServer.findFansByFansKey(reqMap);
        if(CollectionUtils.isEmpty(fansMap)){
            resultMap.put("follow_status","0");
        }else {
            resultMap.put("follow_status","1");
        }

        return resultMap;
    }



}
