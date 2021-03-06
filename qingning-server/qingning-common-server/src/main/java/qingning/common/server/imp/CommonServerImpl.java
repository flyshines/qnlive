package qingning.common.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.qiniu.common.Zone;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.model.FetchRet;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import qingning.common.dj.HttpClientUtil;
import qingning.common.entity.AccessToken;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.util.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.ICommonModuleServer;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.*;

public class CommonServerImpl  extends AbstractQNLiveServer {
    private static Logger logger = LoggerFactory.getLogger(CommonServerImpl.class);
    private ICommonModuleServer commonModuleServer;

    @Override
    public void initRpcServer() {
        if (commonModuleServer == null) {
            commonModuleServer = this.getRpcService("commonModuleServer");
            readShopOperation = new ReadShopOperation(commonModuleServer);
            readUserOperation = new ReadUserOperation(commonModuleServer);
            readCourseOperation = new ReadCourseOperation(commonModuleServer);
            readAPPVersionOperation = new ReadVersionOperation(commonModuleServer);
            readForceVersionOperation = new ReadVersionForceOperation(commonModuleServer);
            readLecturerOperation = new ReadLecturerOperation(commonModuleServer);
            readSeriesOperation = new ReadSeriesOperation(commonModuleServer);
            readConfigOperation = new ReadConfigOperation(commonModuleServer);
        }
    }

    private static Auth auth;

    static {
        auth = Auth.create(MiscUtils.getConfigByKey("qiniu_AK"), MiscUtils.getConfigByKey("qiniu_SK"));
    }

    /**
     * 绑定手机号码（校验手机号码）
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("bindPhone")
    public Map<String, Object> bindPhone(RequestEntity reqEntity) throws Exception{
        Map<String,String> reqMap = (Map<String, String>) reqEntity.getParam();
        String verification_code = reqMap.get("code");
        String phone = reqMap.get("phone");
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象

        if(CodeVerifyficationUtil.verifyVerificationCode(userId, verification_code, jedis)){
            //if("0000".equals(verification_code)){
            logger.info("绑定手机号码>>验证码验证通过");
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("user_id", userId);
            userMap.put("phone_number", phone);
            if(commonModuleServer.updateUserById(userMap)>0){
                throw new QNLiveException("130008");
            }
            //清空用户缓存
            Map<String,Object> userQuery = new HashMap<>();
            userQuery.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String userkey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, userQuery);
            jedis.hset(userkey,"phone_number",phone);
        }else{
            throw new QNLiveException("130002");
        }
        return null;
    }


    @FunctionName("logUserInfo")
    public Map<String, Object> collectClientInformation(RequestEntity reqEntity) throws Exception {
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        long loginTime = System.currentTimeMillis();
        if (!"2".equals(reqMap.get("status"))) {
            reqMap.put("create_time", loginTime);
            reqMap.put("create_date", MiscUtils.getDate(loginTime));
            if (!MiscUtils.isEmpty(reqEntity.getAccessToken())) {
                String user_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
                reqMap.put("user_id", user_id);
                Map<String, String> userInfo = readUser(user_id, reqEntity, readUserOperation, jedis);
                reqMap.put("gender", userInfo.get("gender"));
                Map<String, String> queryParam = new HashMap<>();
                queryParam.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
                String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, queryParam);
                Map<String, String> accessTokenInfo = jedis.hgetAll(accessTokenKey);
                if (MiscUtils.isEmpty(accessTokenInfo)) {
                    Map<String, Object> queryMap = new HashMap<String, Object>();
                    String login_id = (String) reqMap.get("login_id");
                    String login_type = (String) reqMap.get("login_type");
                    accessTokenInfo = new HashMap<>();
                    if (!MiscUtils.isEmpty(login_id)) {
                        queryMap.put("login_type", login_type);
                        queryMap.put("login_id", login_id);
                        MiscUtils.converObjectMapToStringMap(commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap), accessTokenInfo);
                    } else {
                        MiscUtils.converObjectMapToStringMap(commonModuleServer.findLoginInfoByUserId(user_id), accessTokenInfo);
                        reqMap.put("login_id", accessTokenInfo.get("union_id"));
                    }
                }
                reqMap.put("record_time", userInfo.get("create_time"));
                reqMap.put("old_subscribe", accessTokenInfo.get("subscribe"));
                reqMap.put("country", userInfo.get("country"));
                reqMap.put("province", userInfo.get("province"));
                reqMap.put("city", userInfo.get("city"));
                reqMap.put("district", userInfo.get("district"));
                String web_openid = accessTokenInfo.get("web_openid");
                reqMap.put("subscribe", null);
                reqMap.put("web_openid", null);
                if (MiscUtils.isEmpty(web_openid)) {
                    reqMap.put("subscribe", "0");
                } else {
                    reqMap.put("web_openid", web_openid);
                }

                String user_role = MiscUtils.convertString(accessTokenInfo.get("user_role"));
                if (user_role.contains(Constants.USER_ROLE_LECTURER)) {
                    reqMap.put("live_room_build", "1");
                } else {
                    reqMap.put("live_room_build", "0");
                }
                String status = (String) reqMap.get("status");
                if ("1".equals(status) || "3".equals(status)) {
                    Map<String, String> updateValue = new HashMap<String, String>();
                    updateValue.put("last_login_time", loginTime + "");
                    updateValue.put("last_login_ip", (String) reqMap.get("ip"));
                    String plateform = userInfo.get("plateform");
                    String newPalteForm = (String) reqMap.get("plateform");
                    if (MiscUtils.isEmpty(plateform)) {
                        plateform = newPalteForm;
                    } else if (!MiscUtils.isEmpty(newPalteForm) && plateform.indexOf(newPalteForm) == -1) {
                        plateform = plateform + "," + newPalteForm;
                    }
                    updateValue.put("plateform", plateform);
                    Map<String, Object> query = new HashMap<String, Object>();
                    query.put("user_id", user_id);
                    String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query);
                    jedis.hmset(key, updateValue);
                    jedis.sadd(Constants.CACHED_UPDATE_USER_KEY, user_id);
                }
            }
            RequestEntity requestEntity = this.generateRequestEntity("LogServer", Constants.MQ_METHOD_ASYNCHRONIZED, "logUserInfo", reqMap);
            mqUtils.sendMessage(requestEntity);
        }
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("server_time", System.currentTimeMillis());

        return resultMap;
    }

    /**
     * 获取版本信息 根据不同的平台
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getVersion")
    public Map<String, Object> getVersion(RequestEntity reqEntity) throws Exception {
        Map<String, Object> map = (HashMap<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();
        Integer plateform = (Integer) map.get("plateform");//平台 0是直接放过 1是安卓 2是IOS 3是JS
        if (plateform != 0) {//安卓或者ios
            Map<String, String> versionInfoMap = readAppVersion(plateform.toString(), reqEntity, readAPPVersionOperation, jedis, true);
            if (!MiscUtils.isEmpty(versionInfoMap) && Integer.valueOf(versionInfoMap.get("status")) != 0) { //判断有没有信息 判断是否存在总控 总控 0关闭就是不检查 1开启就是检查
                //1.先判断系统和当前version
                if (MiscUtils.compareVersion(plateform.toString(), versionInfoMap.get("version_no"), map.get("version").toString())) {//当前version 小于 最小需要跟新的版本
                    Map<String, Object> reqMap = new HashMap<>();
                    reqMap.put("version_info", versionInfoMap);
                    return reqMap;
                }
            }
        }
        return null;
    }
    @FunctionName("control")
    public Map<String, String> control(RequestEntity reqEntity) throws Exception {
        Map<String, Object> map = (HashMap<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();
        Map<String, String> retMap = new HashMap<>();
        Integer plateform = (Integer) map.get("plateform");//平台 0是直接放过 1是安卓 2是IOS 3是JS
        if (plateform != 0) {
            Map<String, String> versionInfoMap = readAppVersion(plateform.toString(), reqEntity, readAPPVersionOperation, jedis, true);
            if (!MiscUtils.isEmpty(versionInfoMap)) { //判断有没有信息
                if (versionInfoMap.get("os_audit_version") != null) {
                    retMap.put("os_audit_version", versionInfoMap.get("os_audit_version"));
                    return retMap;
                }
            }
        }
        return null;
    }

    @FunctionName("serverTime")
    public Map<String, Object> getServerTime(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("server_time", System.currentTimeMillis());
        return resultMap;
    }
    @FunctionName("qiNiuUploadToken")
    public Map<String, Object> getQiNiuUploadToken(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        long expiredTime = 3600;
        String token = null;
        String url = null;
        if ("1".equals(reqMap.get("upload_type"))) { //图片
            url = IMMsgUtil.configMap.get("images_space_domain_name");
            token = auth.uploadToken(IMMsgUtil.configMap.get("image_space"), null, expiredTime, new StringMap()
                    .putNotEmpty("returnBody", "{\"key\": $(key), \"hash\": $(etag), \"width\": $(imageInfo.width), \"height\": $(imageInfo.height)}"));
        } else if ("2".equals(reqMap.get("upload_type"))) { //音频
            StringMap map = new StringMap();
            map.putNotEmpty("returnBody","{\"key\": $(key), \"hash\": $(etag),\"duration\": $(avinfo.format.duration),\"fsize\": $(fsize),\"mimeType\": $(mimeType)}");
            url = IMMsgUtil.configMap.get("audio_space_domain_name");
            token = auth.uploadToken(IMMsgUtil.configMap.get("audio_space"), null, expiredTime, map);
        } else if ("3".equals(reqMap.get("upload_type"))) {//视频
            StringMap map = new StringMap();
            map.putNotEmpty("returnBody","{\"key\": $(key), \"hash\": $(etag),\"duration\": $(avinfo.format.duration),\"fsize\": $(fsize),\"mimeType\": $(mimeType)}");
            url = IMMsgUtil.configMap.get("video_space_domain_name");
            token = auth.uploadToken(IMMsgUtil.configMap.get("video_space"), null, expiredTime, map);
        }
        resultMap.put("upload_token", token);
        resultMap.put("access_prefix_url", url);
        return resultMap;
    }
    @FunctionName("getShopCard")
    public Map<String, Object> getRoomInviteCard(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

    /*    String roomId = reqMap.get("room_id").toString();
        Map<String, String> liveRoomMap = readShop(roomId, reqEntity, readLiveRoomOperation, jedis, true);
        resultMap.put("room_name", MiscUtils.RecoveryEmoji(liveRoomMap.get("room_name")));

        Map<String, Object> query = new HashMap<String, Object>();
        query.put("user_id", liveRoomMap.get("lecturer_id"));
        Map<String, String> userMap = CacheUtils.readUser(liveRoomMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);
        resultMap.put("avatar_address", liveRoomMap.get("avatar_address"));
        resultMap.put("nick_name", MiscUtils.RecoveryEmoji(userMap.get("nick_name")));

        //????????????????????????
        resultMap.put("share_url", getLiveRoomShareURL(userId, roomId, jedis, appName));

        long timeS1 = System.currentTimeMillis();
        logger.debug("-------------------------" + String.valueOf(timeS1));
        if (reqMap.get("png").toString().equals("Y")) {
            resultMap.put("png_url", this.CreateRqPage(null, roomId, null, null, null, null, reqEntity.getAccessToken(), reqEntity.getVersion(), jedis, appName));
        }*/
        long timeS2 = System.currentTimeMillis();
        logger.debug("-------------------------" + String.valueOf(timeS2));
        return resultMap;
    }
    @FunctionName("createFeedback")
    public Map<String, Object> createFeedback(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();

        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);
        reqMap.put("feedback_id", MiscUtils.getUUId());
        reqMap.put("now", new Date());
        commonModuleServer.insertFeedback(reqMap);
        return resultMap;
    }
   /*{  @FunctionName("getShareInfo")
    public Map<String, Object> getShareInfo(RequestEntity reqEntity) throws Exception
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        String query_type = reqMap.get("query_type").toString();
        String id = reqMap.get("id").toString();
        Jedis jedis = jedisUtils.getJedis();
        String title = null;
        String content = null;
        String icon_url = null;
        String simple_content = null;
        String share_url = null;
        String png_url = null;

        //1.课程分享 2.直播间分享 3.其他页面分享 4.成为直播间分销员分享
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        switch (query_type) {
            case "1":
                ((Map<String, Object>) reqEntity.getParam()).put("course_id", id);//如果type==1 那么出入的id就是course_id
                Map<String, String> courseMap = readCourse(id, reqEntity, readCourseOperation, jedis, true);
                if (MiscUtils.isEmpty(courseMap)) {
                    throw new QNLiveException("120009");
                }
                MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);
                title = courseMap.get("course_title");
                ((Map<String, Object>) reqEntity.getParam()).put("room_id", courseMap.get("room_id"));//把roomid 放进参数中 传到后面
                //Map<String, String> liveRoomMap = CacheUtils.readLiveRoom(courseMap.get("room_id"), reqEntity, readLiveRoomOperation, jedis, true);
                Map<String,Object> query = new HashMap<>();
                query.put("user_id",courseMap.get("lecturer_id"));
               // Map<String, String> shop = CacheUtils.readShopByUserId(courseMap.get("lecturer_id"), generateRequestEntity(null, null, null, query), readShopOperation, jedis);
                String shopName = shop.get("shop_name");
                if ("2".equals(courseMap.get("status"))) {    //2：已结束
                    content = shopName + "\n";
                } else if ("1".equals(courseMap.get("status"))) {    //1：已发布
                    Date courseStartTime = new Date(Long.parseLong(courseMap.get("start_time")));
                    if (MiscUtils.isEmpty(content)) {
                        content = shopName + "\n" + MiscUtils.getConfigByKey("weixin_course_share_time", appName) + MiscUtils.parseDateToFotmatString(courseStartTime, "yyyy年MM月dd日 HH:mm");
                    } else {
                        content += "\n" + shopName + "\n" + MiscUtils.getConfigByKey("weixin_course_share_time", appName) + MiscUtils.parseDateToFotmatString(courseStartTime, "yyyy年MM月dd日 HH:mm");
                    }
                } else if ("4".equals(courseMap.get("status"))) {    //4：直播中
                    content = shopName + "\n" + MiscUtils.getConfigByKey("weixin_course_share_content", appName);
                }
                icon_url = liveRoomMap.get("avatar_address");    //直播间头像
                simple_content = courseMap.get("course_title");
                share_url = getCourseShareURL(userId, id, courseMap, jedis, appName);

                if (reqMap.get("png").toString().equals("Y"))
                    png_url = this.CreateRqPage(id, null, null, null, null, null, reqEntity.getAccessToken(), reqEntity.getVersion(), jedis, appName);
                break;

            case "2":
                ((Map<String, Object>) reqEntity.getParam()).put("room_id", id);//如果是2 那么就是直播间分享 把roomid存入
                Map<String, String> liveRoomInfoMap = CacheUtils.readLiveRoom(id, reqEntity, readLiveRoomOperation, jedis, true);
                if (MiscUtils.isEmpty(liveRoomInfoMap)) {
                    throw new QNLiveException("120018");
                }
                title = liveRoomInfoMap.get("room_name");
                content = liveRoomInfoMap.get("room_remark");
                icon_url = liveRoomInfoMap.get("avatar_address");
                share_url = getLiveRoomShareURL(userId, id, jedis, appName);
                simple_content = MiscUtils.getConfigByKey("weixin_live_room_simple_share_content", appName) + liveRoomInfoMap.get("room_name");
                if (reqMap.get("png").toString().equals("Y"))
                    png_url = this.CreateRqPage(null, id, null, null, null, null, reqEntity.getAccessToken(), reqEntity.getVersion(), jedis, appName);
                break;

            case "3":
                title = MiscUtils.getConfigByKey("weixin_other_page_share_title", appName);
                content = MiscUtils.getConfigByKey("weixin_other_page_share_content", appName);
                icon_url = MiscUtils.getConfigByKey("weixin_other_page_share_icon_url", appName);
                simple_content = MiscUtils.getConfigByKey("weixin_other_page_share_simple_content", appName);
                share_url = MiscUtils.getConfigByKey("other_share_url", appName);
                break;

            case "4":
                Map<String, Object> map = new HashMap<>();
                map.put(Constants.CACHED_KEY_USER_ROOM_SHARE_FIELD, reqMap.get("id"));
                String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_ROOM_SHARE, map);
                Map<String, String> values = jedis.hgetAll(key);

                if (MiscUtils.isEmpty(values)) {
                    throw new QNLiveException("120019");
                }
                title = MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_title", appName);
                Map<String, String> liveRoomInfo = CacheUtils.readLiveRoom(values.get("room_id"), reqEntity, readLiveRoomOperation, jedis, true);
                if (MiscUtils.isEmpty(liveRoomInfo)) {
                    throw new QNLiveException("120018");
                }
                content = liveRoomInfo.get("room_name") + "\n"
                        + MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_second_content", appName).replace("%s", (Integer.parseInt(values.get("profit_share_rate")) / 100.0) + "") + "\n"
                        + MiscUtils.getConfigByKey("weixin_live_room_be_distributer_share_third_content", appName);
                icon_url = liveRoomInfo.get("avatar_address");
                share_url = String.format(MiscUtils.getConfigByKey("be_distributer_url_pre_fix", appName), reqMap.get("id"), liveRoomInfo.get("room_id"), (Integer.parseInt(values.get("profit_share_rate")) / 100.0), values.get("effective_time"));
                if (reqMap.get("png").toString().equals("Y"))
                    png_url = this.CreateRqPage(null,
                            liveRoomInfo.get("room_id"),
                            null,
                            reqMap.get("id").toString(),
                            (Integer.parseInt(values.get("profit_share_rate")) / 100.0),
                            Integer.valueOf(values.get("effective_time")),
                            reqEntity.getAccessToken(),
                            reqEntity.getVersion(),
                            jedis,
                            appName);
                break;
            case "5":
                logger.info("通用-查询分享信息>>>>获取系列课程分享信息");
                ((Map<String, Object>) reqEntity.getParam()).put("series_id", id);//如果type==5 那么传入的id就是series_id
                Map<String, String> seriesMap = CacheUtils.readSeries(id, reqEntity, readSeriesOperation, jedis, true);

                if (seriesMap == null || MiscUtils.isEmpty(seriesMap)) {
                    logger.error("通用-查询分享信息>>>>系列不存在");
                    throw new QNLiveException("210003");
                }
                title = seriesMap.get("series_title");

                *//*
                 * 获取登录账户信息
                 *//*
                reqMap.put("user_id", userId);
                Map<String, String> loginedUserMap = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);

                *//*
                 * 根据讲师id获取直播间id
                 *//*
                String lecturerId = seriesMap.get("lecturer_id");
                Map<String, Object> readRoom = new HashMap<>();
                readRoom.put("lecturer_id", lecturerId);
                String readRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, readRoom);

                Map<String, String> rooms = jedis.hgetAll(readRoomKey);
                String roomId = null;
                if (rooms != null && !rooms.isEmpty()) {
                    Set keys = rooms.keySet();
                    Iterator iter = keys.iterator();
                    roomId = (String) iter.next();    //如果有多个直播间，默认第一个

                    ((Map<String, Object>) reqEntity.getParam()).put("room_id", roomId);//把roomid 放进参数中 传到后面
                    liveRoomMap = CacheUtils.readLiveRoom(roomId, reqEntity, readLiveRoomOperation, jedis, true);
                } else {
                    liveRoomMap = null;
                }

                content =  loginedUserMap.get("nick_name").toString() + "推荐了一个系列课";
                *//*
                 * 优先使用直播间头像，若直播间头像不存在使用课程封面
                 *//*
                if (liveRoomMap != null && !liveRoomMap.isEmpty()) {
                    icon_url = liveRoomMap.get("avatar_address");
                } else {
                    icon_url = seriesMap.get("series_img");
                }
                simple_content = title;
                //获取系列课分享链接
                share_url = MiscUtils.getConfigByKey("series_share_url_pre_fix", appName) + id;

                if (reqMap.get("png").toString().equals("Y"))
                    //生成邀请卡二维码base64
                    png_url = this.CreateSeriesRqPage(seriesMap, loginedUserMap, share_url, reqEntity.getVersion(), jedis, appName);
                break;
        }

        resultMap.put("title", MiscUtils.RecoveryEmoji(title));
        resultMap.put("content", content);
        resultMap.put("icon_url", icon_url);
        resultMap.put("simple_content", simple_content);
        resultMap.put("share_url", share_url);
        if (reqMap.get("png").toString().equals("Y"))
            resultMap.put("png_url", png_url);

        return resultMap;
    }*/
    /**
     * 发送验证码
     *
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("sendVerificationCode")
    public void sendVerificationCode(RequestEntity reqEntity) throws Exception {
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        Map<String, String> map = (Map<String, String>) reqEntity.getParam();
        String phoneNum = map.get("phone");//手机号
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("phone_num", phoneNum);
        /*if(!CollectionUtils.isEmpty(commonModuleServer.findByPhone(reqMap))){
            throw new QNLiveException("130008");
        }*/
        //  String ipAdress = map.get("ipAdress");//ip地址
        if (MiscUtils.isMobile(phoneNum)) { //效验手机号码
            Jedis jedis = jedisUtils.getJedis();
            Map<String, String> userMap = new HashMap<>();
            userMap.put("user_id", userId);
            String userKey = MiscUtils.getKeyOfCachedData(Constants.SEND_MSG_TIME_S, userMap);
            jedis.setex(userKey, 5 * 60, phoneNum);//把手机号码和userid还有效验码 存入缓存当中  //一分钟内
            String dayKey = MiscUtils.getKeyOfCachedData(Constants.SEND_MSG_TIME_D, userMap);//判断日期 一天三次
            if (jedis.exists(dayKey)) {
                Map<String, String> redisMap = JSON.parseObject(jedis.get(dayKey), new TypeReference<Map<String, String>>() {
                });
                if (Integer.parseInt(redisMap.get("count")) == 5) {
                    throw new QNLiveException("130007");//发送太频繁
                } else {
                    int count = Integer.parseInt(redisMap.get("count")) + 1;

                    int expireTime = (int) (System.currentTimeMillis() / 1000 - Long.parseLong(redisMap.get("timestamp")));

                    jedis.setex(dayKey,
                            86410 - expireTime, "{'timestamp':'" + redisMap.get("timestamp") + "','count':'" + count + "'}");
                }
            } else {
                jedis.setex(dayKey, 86400, "{'timestamp':'" + System.currentTimeMillis() / 1000 + "','count':'1'}");//把手机号码和userid还有效验码 存入缓存当中  //一分钟内
            }

            String code = RandomUtil.createRandom(true, 6);   //6位 生成随机的效验码
            String codeKey = MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_CODE, userMap);//存入缓存中
            jedis.setex(codeKey, 20 * 60, code);

            Map<String, String> phoneMap = new HashMap<>();
            phoneMap.put("code", code);
            phoneMap.put("user_id", userId);
            String phoneKey = MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_PHONE, phoneMap);
            jedis.setex(phoneKey, 20 * 60, phoneNum);
            String message = String.format("您的短信验证码:%s，请及时完成验证。", code);
            String result = SendMsgUtil.sendMsgCode(phoneNum, message);
            logger.info("【梦网】（" + phoneNum + "）发送短信内容（" + message + "）返回结果：" + result);
            if (!"success".equalsIgnoreCase(SendMsgUtil.validateCode(result))) {
                throw new QNLiveException("130006");
            }
        } else {
            throw new QNLiveException("130001");
        }
    }
    /**
     * 发送验证码
     *
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("sendIMError")
    public void sendIMError(RequestEntity reqEntity) throws Exception {
        Map<String, String> map = (Map<String, String>) reqEntity.getParam();
        String phoneNum = map.get("phone");//手机号
        String ipAdress = map.get("ipAdress");//ip地址
        if (!ipAdress.equals("120.25.104.68")) {
            throw new QNLiveException("000003");
        }
        //如果是qnlive 就执行
        String message = "IMERROR";
        String result = SendMsgUtil.sendMsgCode(phoneNum, message);
        logger.info("【梦网】（" + phoneNum + "）发送短信内容（" + message + "）返回结果：" + result);
        if (!"success".equalsIgnoreCase(SendMsgUtil.validateCode(result))) {
            throw new QNLiveException("130006");
        }
    }


    /**
     * 七牛音视频截取
     *
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("cutMedia")
    public Map<String, Object> cutMedia(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resMap = new HashMap<>();
        Long time = null;
        if (reqMap.containsKey("time_second"))
            time = Long.valueOf(reqMap.get("time_second").toString());
        String type = reqMap.get("type").toString();
        Map<String, String> qiniuRes;
        String fileUrl = reqMap.get("file_url").toString();
        if ("1".equals(type)) {
            //音频截取
            String autoDomain = MiscUtils.getConfigByKey("audio_space_domain_name");
            String autoZXDomain = MiscUtils.getConfigByKey("audio_zx_space_domain_name");
            //转移到新的空间名称
            String audioSpace = MiscUtils.getConfigByKey("audio_space");
            //转移到新的空间名称
            String audioZXSpace = MiscUtils.getConfigByKey("audio_zx_space");
            if (fileUrl.contains(autoDomain)) {
                qiniuRes = QiNiuUpUtils.cutAuto(audioSpace, autoDomain, reqMap.get("file_url").toString(), time, false);
            } else if (fileUrl.contains(autoZXDomain)) {
                qiniuRes = QiNiuUpUtils.cutAuto(audioZXSpace, autoZXDomain, reqMap.get("file_url").toString(), time, false);
            } else {
                throw new QNLiveException("210009");
            }
        } else {
            //视频截取
            //目前支持的空间文件转码
            String videoDomain = MiscUtils.getConfigByKey("video_space_domain_name");
            String autoDomain = MiscUtils.getConfigByKey("audio_space_domain_name");

            //转移到新的视频空间名称
            String videoSpace = MiscUtils.getConfigByKey("video_space");
            String audioSpace = MiscUtils.getConfigByKey("audio_space");
            if (fileUrl.contains(autoDomain)) {
                qiniuRes = QiNiuUpUtils.cutVideo(audioSpace, autoDomain, reqMap.get("file_url").toString(), time, false);
            } else if (fileUrl.contains(videoDomain)) {
                qiniuRes = QiNiuUpUtils.cutVideo(videoSpace, videoDomain, reqMap.get("file_url").toString(), time, false);
            } else {
                throw new QNLiveException("210009");
            }
        }
        resMap.put("persistent_id", qiniuRes.get("persistentId"));
        resMap.put("new_url", qiniuRes.get("newUrl"));
        return resMap;
    }
    /**
     * 发送验证码
     *
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("sendVipVerificationCode")
    public void sendVipVerificationCode(RequestEntity reqEntity) throws Exception {
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        Map<String, String> map = (Map<String, String>) reqEntity.getParam();
        Map<String, String> reqMap = new HashMap<>();
        map.put("user_id", userId);
        Jedis jedis = jedisUtils.getJedis();
        Map<String, String> user = readUser(userId, reqEntity, readUserOperation, jedis);
        String phoneNum = user.get("phone_number");
        if (MiscUtils.isMobile(phoneNum)) { //效验手机号码
            Map<String, String> userMap = new HashMap<>();
            userMap.put("user_id", userId);
            String userKey = MiscUtils.getKeyOfCachedData(Constants.SEND_MSG_TIME_S, userMap);
            jedis.setex(userKey, 5 * 60, phoneNum);//把手机号码和userid还有效验码 存入缓存当中  //一分钟内
            String dayKey = MiscUtils.getKeyOfCachedData(Constants.SEND_MSG_TIME_D, userMap);//判断日期 一天三次
            if (jedis.exists(dayKey)) {
                Map<String, String> redisMap = JSON.parseObject(jedis.get(dayKey), new TypeReference<Map<String, String>>() {
                });
                if (Integer.parseInt(redisMap.get("count")) == 5) {
                    throw new QNLiveException("130007");//发送太频繁
                } else {
                    int count = Integer.parseInt(redisMap.get("count")) + 1;

                    int expireTime = (int) (System.currentTimeMillis() / 1000 - Long.parseLong(redisMap.get("timestamp")));

                    jedis.setex(dayKey,
                            86410 - expireTime, "{'timestamp':'" + redisMap.get("timestamp") + "','count':'" + count + "'}");
                }
            } else {
                jedis.setex(dayKey, 86400, "{'timestamp':'" + System.currentTimeMillis() / 1000 + "','count':'1'}");//把手机号码和userid还有效验码 存入缓存当中  //一分钟内
            }

            String code = RandomUtil.createRandom(true, 6);   //6位 生成随机的效验码
            String codeKey = MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_CODE, userMap);//存入缓存中
            jedis.setex(codeKey, 20 * 60, code);

            Map<String, String> phoneMap = new HashMap<>();
            phoneMap.put("code", code);
            phoneMap.put("user_id", userId);
            String phoneKey = MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_PHONE, phoneMap);
            jedis.setex(phoneKey, 20 * 60, phoneNum);
            String message = String.format("您的短信验证码:%s，请及时完成验证。", code);
            String result = SendMsgUtil.sendMsgCode(phoneNum, message);
            logger.info("【梦网】（" + phoneNum + "）发送短信内容（" + message + "）返回结果：" + result);
            if (!"success".equalsIgnoreCase(SendMsgUtil.validateCode(result))) {
                throw new QNLiveException("130006");
            }

        } else {
            throw new QNLiveException("130001");
        }
    }


    /**
     * 微信登录
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("weixinCodeUserLogin")
    public Map<String, Object> weixinCodeUserLogin(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String subscribe = "0";
        //是否是SaaS登录
        //      boolean isSaas = "0".equals(reqMap.get("saas_login").toString())?false:true;
        resultMap.put("key", "1");//钥匙 用于在controller判断跳转的页面

        //1.传递授权code及相关参数，调用微信验证code接口
        String code = reqMap.get("code").toString();
        Jedis jedis = jedisUtils.getJedis();//获取缓存工具对象 db
        JSONObject getCodeResultJson = WeiXinUtil.getUserInfoByCode(code);
        if (getCodeResultJson == null || getCodeResultJson.getInteger("errcode") != null || getCodeResultJson.get("openid") == null) {
            if (getCodeResultJson.get("openid") == null) {
                resultMap.put("key", "0");
                return resultMap;
            }
            throw new QNLiveException("120008");
        }
        String openid = getCodeResultJson.get("openid").toString();//拿到openid
        //获取用户信息
        try {
            AccessToken wei_xin_access_token = WeiXinUtil.getAccessToken(null, null, jedis);//获取公众号access_token
            JSONObject user = WeiXinUtil.getUserByOpenid(wei_xin_access_token.getToken(), openid);//获取是否有关注公众信息
            if (user.get("subscribe") != null) {
                subscribe = user.get("subscribe").toString();
            }
        } catch (Exception e) {
            throw new QNLiveException("120008");
        }
        reqMap.put("subscribe", subscribe);
        //1.2如果验证成功，则得到用户的union_id和用户的access_token。
        //1.2.1根据 union_id查询数据库
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("login_type", "4");//4.微信code方式登录
        queryMap.put("web_openid", openid);
        Map<String, Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);

        //1.2.1.1如果用户存在则进行登录流程
        if (loginInfoMap != null) {//有
            if (!loginInfoMap.get("subscribe").toString().equals(subscribe)) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("subscribe", subscribe);
                userMap.put("web_openid", openid);
                userMap.put("user_id", loginInfoMap.get("user_id"));
                commonModuleServer.updateUserWebOpenIdByUserId(userMap);
            }
            processLoginSuccess(2, null, loginInfoMap, resultMap);//获取后台安全证书 access_token
//            if(isSaas){
//                //SaaS登录检查用户店铺逻辑
//                reqMap.put("user_id",loginInfoMap.get("user_id").toString());
//                checkShopInfo(loginInfoMap,reqEntity,jedis);
//            }
            return resultMap;
        } else {
//            if(isSaas){
//                //SaaS登录为找到用户
//                throw new QNLiveException("000005");
//            }
            //1.2.1.2 如果用户不存在，则根据用户的open_id和用户的access_token调用微信查询用户信息接口，得到用户的头像、昵称等相关信息
            String userWeixinAccessToken = getCodeResultJson.getString("access_token");
            JSONObject userJson = WeiXinUtil.getUserInfoByAccessToken(userWeixinAccessToken, openid);
            // 根据得到的相关用户信息注册用户，并且进行登录流程。
            //开发环境配置
            if("dev".equals(MiscUtils.getConfigByKey("environment"))){
                if (userJson == null || userJson.getInteger("errcode") != null) {
                    if (userJson.getString("unionid") == null) {
                        resultMap.put("key", "0");
                        return resultMap;
                    }
                    throw new QNLiveException("120008");
                }
                userJson.put("unionid",openid);
            }else{
                if (userJson == null || userJson.getInteger("errcode") != null || userJson.getString("unionid") == null) {
                    if (userJson.getString("unionid") == null) {
                        resultMap.put("key", "0");
                        return resultMap;
                    }
                    throw new QNLiveException("120008");
                }
            }
            queryMap.clear();
            queryMap.put("login_type", "0");//0.微信方式登录
            queryMap.put("login_id", userJson.getString("unionid"));//unionid 登录
            Map<String, Object> loginInfoMapFromUnionid = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);
            if (loginInfoMapFromUnionid != null) {
                //将open_id更新到login_info表中
                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("user_id", loginInfoMapFromUnionid.get("user_id").toString());
                updateMap.put("web_openid", openid);
                updateMap.put("subscribe", subscribe);
                commonModuleServer.updateUserWebOpenIdByUserId(updateMap);
                processLoginSuccess(2, null, loginInfoMapFromUnionid, resultMap);
                return resultMap;
            }

            String nickname = userJson.getString("nickname");//昵称
            String sex = userJson.getString("sex");//性别
            String headimgurl = userJson.getString("headimgurl");//头像
            if (sex == null || nickname == null || headimgurl == null) {
                resultMap.put("key", "0");
                return resultMap;
            }


            Map<String, String> imResultMap = null;
            try {
                imResultMap = IMMsgUtil.createIMAccount("weixinCodeLogin");//注册im
            } catch (Exception e) {
                //TODO 暂不处理
            }

            //初始化数据库相关表
            reqMap.put("m_user_id", imResultMap.get("uid"));
            reqMap.put("m_pwd", imResultMap.get("password"));
            //设置默认用户头像
            if (MiscUtils.isEmpty(headimgurl)) {
                reqMap.put("avatar_address", MiscUtils.getConfigByKey("default_avatar_address"));//TODO
            } else {
                String transferAvatarAddress = qiNiuFetchURL(headimgurl);
                reqMap.put("avatar_address", transferAvatarAddress);
            }

            if (MiscUtils.isEmpty(nickname)) {
                reqMap.put("nick_name", "用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
            } else {
                reqMap.put("nick_name", nickname);
            }

            if (MiscUtils.isEmpty(sex)) {
                reqMap.put("gender", "2");//TODO
            }

            //微信性别与本系统性别转换
            //微信用户性别 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
            if (sex.equals("1")) {
                reqMap.put("gender", "1");//TODO
            }
            if (sex.equals("2")) {
                reqMap.put("gender", "0");//TODO
            }
            if (sex.equals("0")) {
                reqMap.put("gender", "2");//TODO
            }

            String unionid = userJson.getString("unionid");
            reqMap.put("unionid", unionid);
            reqMap.put("web_openid", openid);
            reqMap.put("login_type", "4");

            Map<String, String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
            //生成access_token，将相关信息放入缓存，构造返回参数
            processLoginSuccess(1, dbResultMap, null, resultMap);
            return resultMap;
        }
    }

    /**
     * @param reqEntity
     * @throws Exception
     */
    @FunctionName("userLoginByUserId")
    public Map<String, Object> userLoginByUserId(RequestEntity reqEntity) throws Exception {
        Map<String, String> map =  new HashMap<String, String>();
        Map<String, String> cacheMap = new HashMap<String, String>();
        Map<String, Object> resultMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis();

        List<Map<String, Object>> loginInfos = commonModuleServer.findLoginInfo();

        for(Map<String, Object> login : loginInfos){
            String user_id = login.get("user_id").toString();
            Map<String, Object> loginInfoMap = commonModuleServer.findLoginInfoByUserId(user_id);
            MiscUtils.converObjectMapToStringMap(loginInfoMap, cacheMap);
            String last_login_date = (new Date()).getTime() + "";

            //2.根据相关信息生成access_token
            String access_token = AccessTokenUtil.generateAccessToken(user_id, last_login_date);

            map.put("access_token", access_token);
            String process_access_token = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
            jedis.hmset(process_access_token, cacheMap);
            jedis.expire(process_access_token, Integer.parseInt(MiscUtils.getConfigByKey("access_token_expired_time")));

            Map<String, Object> query = new HashMap<String, Object>();
            query.put("user_id", user_id);
            jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query));
            Map<String, String> userMap = readUser(user_id, this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);
            //3.增加相关返回参数
            login.put("access_token", access_token);
        }
        resultMap.put("loginArray",loginInfos);
        resultMap.put("num",loginInfos.size());
        return resultMap;
    }
    /**
     * 微信登录
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("newWeixinCodeUserLogin")
    public Map<String, Object> newWeixinCodeUserLogin(RequestEntity reqEntity) throws Exception {
        Jedis jedis = jedisUtils.getJedis();//获取缓存工具对象 db

        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<String, Object>();
        String subscribe = "0";
        //是否是SaaS登录
        //      boolean isSaas = "0".equals(reqMap.get("saas_login").toString())?false:true;
        resultMap.put("key", "1");//钥匙 用于在controller判断跳转的页面
        String code = reqMap.get("code").toString();  //1.传递授权code及相关参数，调用微信验证code接口
        String state = reqMap.get("state").toString();//携带回来的参数 返回appname 进行用户区分
        String appId = "";
        String appsecret = "";
        if(state.equals("old")){
            appId = MiscUtils.getConfigByKey("old_appid");
            appsecret = MiscUtils.getConfigByKey("old_appsecret");
        }else{
            appId = MiscUtils.getConfigByKey("appid");
            appsecret = MiscUtils.getConfigByKey("appsecret");
        }


        JSONObject getCodeResultJson = WeiXinUtil.getUserInfoByCode(code,appId, appsecret);
        if (getCodeResultJson == null || getCodeResultJson.getInteger("errcode") != null
                || getCodeResultJson.getString("openid") == null) {
            if (getCodeResultJson.getString("openid") == null) {
                resultMap.put("key", "0");
                return resultMap;
            }
            throw new QNLiveException("120008");
        }
        String openid = getCodeResultJson.getString("openid");
        if(StringUtils.isEmpty(openid)){
            throw new QNLiveException("120008");
        }
        String userWeixinAccessToken = getCodeResultJson.getString("access_token");
        JSONObject userJson = WeiXinUtil.getUserInfoByAccessToken(userWeixinAccessToken, openid);
        // 根据得到的相关用户信息注册用户，并且进行登录流程。
        if (userJson == null || userJson.getInteger("errcode") != null || userJson.getString("unionid") == null) {
            if (userJson.getString("unionid") == null) {
                resultMap.put("key", "0");
                return resultMap;
            }
            throw new QNLiveException("120008");
        }
        String unionid = userJson.getString("unionid");

        if(state.equals("new")){//判断是否有user
            String user_id = reqMap.get("user_id").toString();
            Map<String,String> userMap = readUser(user_id,
                    this.generateRequestEntity(null, null, null, reqMap), readUserOperation, jedis);
            if(!MiscUtils.isEmpty(userMap.get("old_user"))){
                if(userMap.get("old_user").equals("1")){
                    // 将open_id更新到login_info表中
                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("user_id",user_id);
                    updateMap.put("web_openid", openid);
                    updateMap.put("union_id", unionid);
                    updateMap.put("login_type", "0");
                    if(commonModuleServer.getLoginInfoByLoginIdAndLoginType(updateMap)!=null){
                        commonModuleServer.updateUserWebOpenIdByUserId(updateMap);
                    }
                }
            }
        }

        // 1.2如果验证成功，则得到用户的union_id和用户的access_token。
        // 1.2.1根据 union_id查询数据库
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("login_type", "4");
        queryMap.put("web_openid", openid);
        Map<String, Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);

        if(!MiscUtils.isEmpty(loginInfoMap)){
            if(state.equals("old")){
                resultMap.putAll(loginInfoMap);
                return resultMap;
            }else{
                processLoginSuccess(2, null, loginInfoMap, resultMap);
                return resultMap;
            }
        }else {
            queryMap.clear();
            queryMap.put("login_type", "0");// 0.微信方式登录
            queryMap.put("login_id", unionid);
            Map<String, Object> loginInfoMapFromUnionid = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);
            if(!MiscUtils.isEmpty(loginInfoMapFromUnionid)){
                if(state.equals("old")){
                    resultMap.putAll(loginInfoMapFromUnionid);
                    return resultMap;
                }else{
                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("user_id", loginInfoMapFromUnionid.get("user_id").toString());
                    updateMap.put("web_openid", openid);
                    updateMap.put("subscribe", subscribe);
                    commonModuleServer.updateUserWebOpenIdByUserId(updateMap);
                    processLoginSuccess(2, null, loginInfoMapFromUnionid, resultMap);
                    return resultMap;
                }
            }else{
                if(state.equals("old")){
                    return resultMap;
                }
            }
        }

        String nickname = userJson.getString("nickname");//昵称
        String sex = userJson.getString("sex");//性别
        String headimgurl = userJson.getString("headimgurl");//头像
        if (sex == null || nickname == null || headimgurl == null) {
            resultMap.put("key", "0");
            return resultMap;
        }


        Map<String, String> imResultMap = null;
        try {
            imResultMap = IMMsgUtil.createIMAccount("weixinCodeLogin");//注册im
        } catch (Exception e) {
            //TODO 暂不处理
        }

        //初始化数据库相关表
        reqMap.put("m_user_id", imResultMap.get("uid"));
        reqMap.put("m_pwd", imResultMap.get("password"));
        //设置默认用户头像
        if (MiscUtils.isEmpty(headimgurl)) {
            reqMap.put("avatar_address", MiscUtils.getConfigByKey("default_avatar_address"));//TODO
        } else {
            String transferAvatarAddress = qiNiuFetchURL(headimgurl);
            reqMap.put("avatar_address", transferAvatarAddress);
        }

        if (MiscUtils.isEmpty(nickname)) {
            reqMap.put("nick_name", "用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
        } else {
            reqMap.put("nick_name", nickname);
        }

        if (MiscUtils.isEmpty(sex)) {
            reqMap.put("gender", "2");//TODO
        }

        //微信性别与本系统性别转换
        //微信用户性别 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
        if (sex.equals("1")) {
            reqMap.put("gender", "1");//TODO
        }
        if (sex.equals("2")) {
            reqMap.put("gender", "0");//TODO
        }
        if (sex.equals("0")) {
            reqMap.put("gender", "2");//TODO
        }
        reqMap.put("unionid", unionid);
        reqMap.put("web_openid", openid);
        reqMap.put("login_type", "4");

        Map<String, String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
        //生成access_token，将相关信息放入缓存，构造返回参数
        processLoginSuccess(1, dbResultMap, null, resultMap);
        return resultMap;

    }

    /**
     * @param type         1新注册用户处理方式，2老用户处理方式
     * @param dbResultMap  当type为1时，传入该值，该值为新用户注册后返回的信息；当type为2时该值传入null
     * @param loginInfoMap
     * @param resultMap    service要返回的map
     */
    private void processLoginSuccess(Integer type, Map<String, String> dbResultMap, Map<String, Object> loginInfoMap,
                                     Map<String, Object> resultMap) throws Exception {
        Jedis jedis = jedisUtils.getJedis();//根据appname 读取不同的redis db
        String last_login_date = (new Date()).getTime() + "";
        String user_id = null;
        String m_user_id = null;
        String m_pwd = null;
        Map<String, String> cacheMap = new HashMap<String, String>();

        //新注册用户,重新查询loginInfo
        if (type == 1) {
            user_id = dbResultMap.get("user_id");
            loginInfoMap = commonModuleServer.findLoginInfoByUserId(user_id);
            //老用户
        } else if (type == 2) {
            user_id = loginInfoMap.get("user_id").toString();
            //如果发现IM账号为空，则重新尝试注册IM账号
            if (loginInfoMap.get("m_user_id") == null) {
                Map<String, String> imResultMap = null;
                try {
                    imResultMap = IMMsgUtil.createIMAccount("supply");
                } catch (Exception e) {
                    //TODO 暂不处理
                }

                if (!MiscUtils.isEmpty(imResultMap)) {
                    m_user_id = imResultMap.get("uid");
                    m_pwd = imResultMap.get("password");

                    if (!MiscUtils.isEmpty(m_user_id) && !MiscUtils.isEmpty(m_pwd)) {
                        //更新login_info表
                        Map<String, Object> updateIMAccountMap = new HashMap<>();
                        updateIMAccountMap.put("m_user_id", m_user_id);
                        updateIMAccountMap.put("m_pwd", m_pwd);
                        updateIMAccountMap.put("user_id", user_id);
                        commonModuleServer.updateIMAccount(updateIMAccountMap);
                    }
                }
            }
        }else if(type == 5){
            user_id = loginInfoMap.get("user_id").toString();
        }

        if(type != 5){
            //1.将objectMap转为StringMap
            m_user_id = loginInfoMap.get("m_user_id") == null ? null : loginInfoMap.get("m_user_id").toString();
            m_pwd = loginInfoMap.get("m_pwd") == null ? null : loginInfoMap.get("m_pwd").toString();
        }
        MiscUtils.converObjectMapToStringMap(loginInfoMap, cacheMap);
        //2.根据相关信息生成access_token
        String access_token = AccessTokenUtil.generateAccessToken(user_id, last_login_date);
        Map<String, Object> map = new HashMap<>();
        map.put("access_token", access_token);
        String process_access_token = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
        jedis.hmset(process_access_token, cacheMap);
        jedis.expire(process_access_token, Integer.parseInt(MiscUtils.getConfigByKey("access_token_expired_time")));

        Map<String, Object> query = new HashMap<>();
        query.put("user_id", user_id);
        jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query));
        Map<String, String> userMap = readUser(user_id, this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);

        //3.增加相关返回参数
        resultMap.put("access_token", access_token);
        if(type != 5){
            resultMap.put("im_account_info", MiscUtils.encryptIMAccount(m_user_id, m_pwd));
            resultMap.put("m_user_id", m_user_id);
            resultMap.put("user_id", user_id);
        }

        //注册店铺用到
        loginInfoMap.put("user_name", userMap.get("nick_name"));
        loginInfoMap.put("avatar_address", userMap.get("avatar_address"));
        loginInfoMap.put("lecturer_id", user_id);

        //</editor-fold>
        String shop_id = "";
        if (!MiscUtils.isEmpty(userMap.get("shop_id"))) {
            query.clear();
            Map<String, String> shopInfo = readShop(shop_id,false, jedis);
            if(shopInfo.get("open_sharing").equals("1")){
                logger.debug("同步讲师token  user_id : "+user_id +" token : "+access_token);
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("version", "1.2.0");
                headerMap.put("Content-Type", "application/json;charset=UTF-8");
                headerMap.put("access_token",access_token);
                //获取知享课程数
                String getUrl = MiscUtils.getConfigByKey("sharing_api_url")
                        +SharingConstants.SHARING_SERVER_USER_COMMON
                        +SharingConstants.SHARING_USER_COMMON_GENERATE_TOKEN;
                String result = HttpClientUtil.doGet(getUrl, headerMap, null, "UTF-8");
                resultMap.put("synchronization_token",result);
                resultMap.put("open_sharing", shopInfo.get("open_sharing"));
            }
        }
        resultMap.put("shop_id", shop_id);
        resultMap.put("avatar_address", userMap.get("avatar_address"));
        resultMap.put("nick_name", userMap.get("nick_name"));
    }

    private String qiNiuFetchURL(String mediaUrl) throws Exception {
        Configuration cfg = new Configuration(Zone.zone0());
        BucketManager bucketManager = new BucketManager(auth, cfg);
        String bucket = MiscUtils.getConfigByKey("image_space");
        String key = Constants.WEB_FILE_PRE_FIX + MiscUtils.parseDateToFotmatString(new Date(), "yyyyMMddHH") + MiscUtils.getUUId();
        FetchRet result = bucketManager.fetch(mediaUrl, bucket, key);
        String imageUrl = MiscUtils.getConfigByKey("images_space_domain_name") + "/" + key;
        return imageUrl;
    }

    /**
     * app 登录
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("userLogin")
    public Map<String, Object> userLogin(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(reqMap);
        Jedis jedis = jedisUtils.getJedis();
        int login_type_input = Integer.parseInt(reqMap.get("login_type").toString());
        switch (login_type_input) {
            case 0: //?????
                if (MiscUtils.isEmpty(loginInfoMap)) {
                    //???IM
                    Map<String, String> imResultMap = null;
                    try {
                        imResultMap = IMMsgUtil.createIMAccount(reqMap.get("device_id").toString());
                    } catch (Exception e) {
                        //TODO ???????
                    }
                    reqMap.put("m_user_id", imResultMap.get("uid"));
                    reqMap.put("m_pwd", imResultMap.get("password"));
                    //?????????????
                    String transferAvatarAddress = (String) reqMap.get("avatar_address");
                    if (!MiscUtils.isEmpty(transferAvatarAddress)) {
                        try {
                            transferAvatarAddress = qiNiuFetchURL(reqMap.get("avatar_address").toString());
                        } catch (Exception e) {
                            transferAvatarAddress = null;
                        }
                    }
                    if (MiscUtils.isEmpty(transferAvatarAddress)) {
                        transferAvatarAddress = MiscUtils.getConfigByKey("default_avatar_address");
                    }

                    reqMap.put("avatar_address", transferAvatarAddress);

                    if (reqMap.get("nick_name") == null || StringUtils.isBlank(reqMap.get("nick_name").toString())) {
                        reqMap.put("avatar_address", "???" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
                    }
                    Map<String, String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
                    processLoginSuccess(1, dbResultMap, null, resultMap);
                } else {
                    processLoginSuccess(2, null, loginInfoMap, resultMap);
                }
                break;
            case 2: //???????
                if (MiscUtils.isEmpty(loginInfoMap)) {
                    throw new QNLiveException("120002");
                } else {
                    if (reqMap.get("certification").toString().equals(loginInfoMap.get("passwd").toString())) {
                        processLoginSuccess(2, null, loginInfoMap, resultMap);
                    } else {
                        throw new QNLiveException("120001");
                    }
                }
                break;
            case 4:
                if (MiscUtils.isEmpty(loginInfoMap)) {
                    throw new QNLiveException("120002");
                }
                break;
        }


        return resultMap;
    }
    /**
     * 账号登录
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("accountLogin")
    public Map<String, Object> accountLogin(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("login_type",reqMap.get("login_type"));
        queryMap.put("account",reqMap.get("account"));
        Map<String, Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(reqMap);

        if(MiscUtils.isEmpty(loginInfoMap)){
            throw new QNLiveException("000005");
        }
        String passwd = MD5Util.getMD5(reqMap.get("passwd").toString()+Constants.USER_DEFAULT_MD5);
        if(!loginInfoMap.get("passwd").toString().equals(passwd)){
            throw new QNLiveException("310002");
        }
        processLoginSuccess(5,null,loginInfoMap,resultMap);
        return resultMap;
    }

    /**
     * 账号用户注册
     *1.用户注册
     * 2.开通店铺
     * 3,开通直播间
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("accountRegister")
    public Map<String, Object> accountRegister(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        /**
         * 1.创建用户
         */
        //    reqMap.put("login_type","5");
        Map<String, Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(reqMap);
        Jedis jedis = jedisUtils.getJedis();
        if(!MiscUtils.isEmpty(loginInfoMap)){
            throw new QNLiveException("310001");
        }
        if(MiscUtils.isEmpty( reqMap.get("avatar_address"))){
            reqMap.put("avatar_address",MiscUtils.getConfigByKey("default_avatar_address"));
        }
        Map<String, String> dbUserMap = commonModuleServer.initializeAccountRegisterUser(reqMap);
        String userId = dbUserMap.get("user_id");
        resultMap.put("user_id", userId);
        reqMap.put("user_id", userId);


        Map<String, Object> map = new HashMap<>();

        /**
         * 3.开通店铺
         */
        Map<String,Object> shop = new HashMap<>();
        shop.put("user_id",userId);
        shop.put("shop_id",MiscUtils.getUUId());
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        shop.put("user_name",reqMap.get("nick_name")+"");
        shop.put("shop_name",reqMap.get("nick_name")+"的知识店铺");
        shop.put("shop_remark",reqMap.get("remark"));
        shop.put("lecturer_title",reqMap.get("lecturer_title"));
        shop.put("lecturer_identity",reqMap.get("lecturer_identity"));
        String shopUrl = MiscUtils.getConfigByKey("share_url_shop_index")+shop.get("shop_id");
        shop.put("shop_url",shopUrl);
        shop.put("status","1");
        shop.put("create_time",new Date());
        shop.put("shop_logo",reqMap.get("avatar_address"));
        commonModuleServer.insertShop(shop);
        readCurrentUserShop(reqEntity, jedis);

        //更新用户缓存
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String userKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, queryMap);
        jedis.hset(userKey,"shop_id",shop.get("shop_id").toString());
        resultMap.put("shop_id",shop.get("shop_id").toString());
        return resultMap;
    }

    /**
     * PC端  微信授权code登录
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("pcCodeUserLogin")
    public Map<String, Object> pcCodeUserLogin(RequestEntity reqEntity) throws Exception {
            Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
            Map<String, Object> resultMap = new HashMap<String, Object>();
            Jedis jedis = jedisUtils.getJedis();
            String subscribe = "0";
            resultMap.put("key", "0");//未绑定手机号码

            //1.传递授权code及相关参数，调用微信验证code接口
            String code = reqMap.get("code").toString();

            JSONObject accountJson = WeiXinUtil.getPCUserAccountInfo(code);

            Object errCode = accountJson.get("errcode");
            if (errCode != null) {
                throw new QNLiveException("120008");
            }

            String openid = accountJson.getString("openid");
            String union_id = accountJson.getString("unionid");

            //PC和公众平台共用的接口 getUserByOpenid是公众平台特有的接口
            JSONObject userJson = WeiXinUtil.getUserInfoByAccessToken(accountJson.getString("access_token"), openid);
            errCode = userJson.get("errcode");
            if (errCode != null) {
                throw new QNLiveException("120008");
            }
            String nickname = userJson.getString("nickname");//昵称
            resultMap.put("name", nickname);

            //1.2如果验证成功，则得到用户的union_id和用户的access_token。
            //1.2.1根据 union_id查询数据库
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("login_type", "0");//0与union查询
            queryMap.put("login_id", union_id);
            Map<String, Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);


            //1.2.1.1如果用户存在则进行登录流程
            if (loginInfoMap != null) {//有
                processLoginSuccess(2, null, loginInfoMap, resultMap);//获取后台安全证书 access_token
                return resultMap;
            } else {
                String sex = userJson.getString("sex");//性别
                String headimgurl = userJson.getString("headimgurl");//头像

                Map<String, String> imResultMap = null;
                try {
                    imResultMap = IMMsgUtil.createIMAccount("weixinCodeLogin");//注册im
                } catch (Exception e) {
                    //TODO 暂不处理
                }

                //初始化数据库相关表
                reqMap.put("m_user_id", imResultMap.get("uid"));
                reqMap.put("m_pwd", imResultMap.get("password"));
                //设置默认用户头像
                if (MiscUtils.isEmpty(headimgurl)) {
                    reqMap.put("avatar_address", MiscUtils.getConfigByKey("default_avatar_address"));//TODO
                } else {
                    String transferAvatarAddress = qiNiuFetchURL(headimgurl);
                    reqMap.put("avatar_address", transferAvatarAddress);
                }

                if (MiscUtils.isEmpty(nickname)) {
                    reqMap.put("nick_name", "用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
                } else {
                    reqMap.put("nick_name", nickname);
                }

                if (MiscUtils.isEmpty(sex)) {
                    reqMap.put("gender", "2");//TODO
                }

                //微信性别与本系统性别转换
                //微信用户性别 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
                reqMap.put("gender", sex.equals("2")?"0":(sex.equals("0")?"2":sex));

                union_id = userJson.getString("unionid");
                reqMap.put("unionid", union_id);
                reqMap.put("web_openid", openid);
                reqMap.put("login_type", "4");
                reqMap.put("subscribe", subscribe);
                Map<String, String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);
                //生成access_token，将相关信息放入缓存，构造返回参数
                processLoginSuccess(1, dbResultMap, null, resultMap);
                /*if (reqMap.get("is_saas") != null) {
                    //SaaS登录检查用户店铺逻辑
                    reqMap.put("user_id", resultMap.get("user_id").toString());
                    checkShopInfo(loginInfoMap, reqEntity, jedis);
                }*/

                return resultMap;
            }
    }

    @FunctionName("weiXinConfiguration")
    public Map<String, String> getWeiXinConfiguration(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String JSApiTIcket = WeiXinUtil.getJSApiTIcket(jedisUtils.getJedis());
        return WeiXinUtil.sign(JSApiTIcket, reqMap.get("url").toString());
    }
    /**
     * 生成具体微信订单
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("generateWeixinPayBill")
    public Map<String, String> generateWeixinPayBill(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();
        //1.检测课程是否存在，课程不存在则给出提示（ 课程不存在，120009）
        String courseId = reqMap.get("course_id").toString();
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("course_id", courseId);
        //2.如果支付类型为打赏，则检测内存中的打赏类型是否存在，如果不存在则给出提示（120010，打赏类型不存在）
        String profit_type = reqMap.get("profit_type").toString();

        Map<String, String> courseMap;
        if ("2".equals(profit_type)) {
            //系列课
            query.put("series_id", courseId);
            courseMap = readSeries(courseId, generateRequestEntity(null, null, null, query), readSeriesOperation, jedis, true);
        } else {
            //打赏，课程收益
            courseMap = readCourse(courseId, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, false);
        }
        if (MiscUtils.isEmpty(courseMap)) {    //如果课程/系列不存在
            throw new QNLiveException("120009");
        }


        //3.插入t_trade_bill表 交易信息表
        String goodName = null;
        Integer totalFee = 0;
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> insertMap = new HashMap<>();
        insertMap.put("user_id", userId);
        //判断roomID
        if (courseMap.get("shop_id") == null) {
            courseMap.put("shop_id",readShopByUserId(courseMap.get("lecturer_id").toString(),jedis).get("shop_id"));
        }
        //讲师店铺查询
        query.put("user_id", courseMap.get("lecturer_id"));
        insertMap.put("room_id", courseMap.get("room_id"));
        insertMap.put("course_id", courseId);
        insertMap.put("shop_id", courseMap.get("shop_id"));
        //判断类型为 0:课程收益 1:打赏 2:系列课收益
        if (profit_type.equals("1")) {
            //打赏嘉宾ID
            if(reqMap.get("guest_id")!=null&&!reqMap.get("guest_id").toString().equals(courseMap.get("lecturer_id"))){
                String guestId = reqMap.get("guest_id").toString();
                insertMap.put("shop_id",readShopByUserId(reqMap.get("guest_id").toString(),jedis).get("shop_id"));
                insertMap.put("guest_id", guestId);
            }
            insertMap.put("amount", reqMap.get("reward_amount"));
            totalFee = ((Long) reqMap.get("reward_amount")).intValue();
            goodName = MiscUtils.getConfigByKey("weixin_pay_reward_course_good_name") + "-" + MiscUtils.RecoveryEmoji(courseMap.get("course_title"));
            //区分店铺课程
            if (courseMap.get("goods_type") != null) {
                insertMap.put("course_type", "2");
            } else {
                insertMap.put("course_type", "1");
            }
            insertMap.put("course_type", "1");
        } else if (profit_type.equals("0")) {
            insertMap.put("amount", courseMap.get("course_price"));
            totalFee = Integer.parseInt(courseMap.get("course_price"));
            goodName = MiscUtils.getConfigByKey("weixin_pay_buy_course_good_name") + "-" + MiscUtils.RecoveryEmoji(courseMap.get("course_title"));
            //区分店铺课程
            if (courseMap.get("goods_type") != null) {
                insertMap.put("course_type", "2");
            } else {
                insertMap.put("course_type", "1");
            }
        } else if (profit_type.equals("2")) {
            //系列课收益
            insertMap.put("amount", courseMap.get("series_price"));
            totalFee = Integer.parseInt(courseMap.get("series_price"));
            goodName = MiscUtils.getConfigByKey("weixin_pay_buy_course_good_name") + "-" + MiscUtils.RecoveryEmoji(courseMap.get("series_title"));
            //系列类型（1直播 2店铺课程）
            if ("0".equals(courseMap.get("series_course_type"))) {
                //直播系列
                insertMap.put("course_type", "1");
            } else {
                //店铺课程
                insertMap.put("course_type", "2");
            }

        }
        insertMap.put("status", "0");
        String tradeId = MiscUtils.getWeiXinId();//TODO
        insertMap.put("trade_id", tradeId);
        insertMap.put("profit_type", profit_type);
        insertMap.put("goods_type", courseMap.get("goods_type"));
        commonModuleServer.insertTradeBill(insertMap);

        query.clear();
        query.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, query);

        //4.调用微信生成预付单接口
        String terminalIp = reqMap.get("remote_ip_address").toString();
        String outTradeNo = tradeId;
        String platform = reqMap.get("platform").toString();
        String openid = null;

        boolean isWeb = false;
        if (platform == null || platform.equals("3") || platform.equals("0")) {//0web 3是js调用 默认是js web调用
            Map<String, String> userMap = jedis.hgetAll(key);
            openid = userMap.get("web_openid");
            isWeb = true;
        }

        Map<String, String> payResultMap = TenPayUtils.sendPrePay(goodName, totalFee, terminalIp, outTradeNo, openid, platform);

        //5.处理生成微信预付单接口
        if (payResultMap.get("return_code").equals("FAIL")) {
            //更新交易表
            Map<String, Object> failUpdateMap = new HashMap<>();
            failUpdateMap.put("status", "3");
            failUpdateMap.put("close_reason", "生成微信预付单失败 " + payResultMap.get("return_msg") + payResultMap.get("err_code_des"));
            failUpdateMap.put("trade_id", tradeId);
            commonModuleServer.closeTradeBill(failUpdateMap);

            throw new QNLiveException("120015");
        } else {
            //成功，则需要插入支付表
            Map<String, Object> insertPayMap = new HashMap<>();
            insertPayMap.put("trade_id", tradeId);
            insertPayMap.put("payment_id", MiscUtils.getUUId());
            insertPayMap.put("payment_type", 0);
            insertPayMap.put("status", "1");
            insertPayMap.put("pre_pay_no", payResultMap.get("prepay_id"));
            insertPayMap.put("create_time", new Date());
            insertPayMap.put("update_time", new Date());
            commonModuleServer.insertPaymentBill(insertPayMap);

            //返回相关参数给前端.
            SortedMap<String, String> resultMap = new TreeMap<>();
            if (isWeb) {
                resultMap.put("appId", MiscUtils.getConfigByKey("appid"));
                resultMap.put("package", "prepay_id=" + payResultMap.get("prepay_id"));
            } else {
                resultMap.put("prepayId", payResultMap.get("prepay_id"));
                resultMap.put("package", "Sign=WXPay");
            }
            resultMap.put("nonceStr", payResultMap.get("random_char"));
            resultMap.put("signType", "MD5");
            resultMap.put("timeStamp", System.currentTimeMillis() / 1000 + "");

            String paySign = null;
            if (isWeb) {
                paySign = TenPayUtils.getSign(resultMap, platform);
            } else {
                SortedMap<String, String> signMap = new TreeMap<>();
                signMap.put("appid", MiscUtils.getConfigByKey("app_app_id"));
                signMap.put("partnerid", MiscUtils.getConfigByKey("weixin_app_pay_mch_id"));
                signMap.put("prepayid", resultMap.get("prepayId"));
                signMap.put("package", resultMap.get("package"));
                signMap.put("noncestr", resultMap.get("nonceStr"));
                signMap.put("timestamp", resultMap.get("timeStamp"));
                paySign = TenPayUtils.getSign(signMap, platform);
            }

            resultMap.put("paySign", paySign);

            return resultMap;
        }
    }

    @FunctionName("convertWeixinResource")
    public Map<String, Object> convertWeixinResource(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();

//        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //1.将多媒体server_id通过微信接口，得到微信资源访问链接
        String mediaUrl = WeiXinUtil.getMediaURL(reqMap.get("media_id").toString(), jedisUtils.getJedis());
        //2.调用七牛fetch将微信资源访问链接转换为七牛图片链接
        String fetchURL = qiNiuFetchURL(mediaUrl);

        resultMap.put("url", fetchURL);
        return resultMap;
    }

    private Map<String, Object> joinCourse(String course_id, Jedis jedis, String user_id, String query_type) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        //1.1读取课程信息
        map.put("course_id", course_id);
        Map<String, String> courseInfoMap = readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, false);
        String appName = courseInfoMap.get("app_name");
        if (MiscUtils.isEmpty(courseInfoMap)) {
            throw new QNLiveException("100004");
        }
        //1.2检测该用户是否为讲师，为讲师则不能加入该课程

        //如果是app 就判断是否是讲师
        if (query_type.equals("0")) {
            if (user_id.equals(courseInfoMap.get("lecturer_id"))) {
                throw new QNLiveException("210006");
            }
        }
        String course_type = courseInfoMap.get("course_type");

        //3.检测学生是否参与了该课程
        Map<String, Object> studentQueryMap = new HashMap<>();
        studentQueryMap.put("user_id", user_id);
        studentQueryMap.put("course_id", course_id);
        if (commonModuleServer.isStudentOfTheCourse(studentQueryMap)) {
            throw new QNLiveException("100004");
        }

        //5.将学员信息插入到学员参与表中
        courseInfoMap.put("user_id", user_id);
        courseInfoMap.put("value_from", query_type);
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseInfoMap.get("lecturer_id"));
        String lecturerRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);

        Map<String, String> liveRoomsMap = jedis.hgetAll(lecturerRoomKey);

        for (String roomId : liveRoomsMap.keySet()) {
            courseInfoMap.put("room_id", roomId);
            break;
        }
        Map<String, Object> resultMap = commonModuleServer.joinCourse(courseInfoMap);

        if (query_type.equals("0")) {
            //<editor-fold desc="app">
            //6.修改讲师缓存中的课程参与人数
            map.clear();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseInfoMap.get("lecturer_id"));
            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
            jedis.hincrBy(lecturerKey, "total_student_num", 1);
            if ("2".equals(course_type)) {
                jedis.hincrBy(lecturerKey, "pay_student_num", 1);
            }
            map.clear();
            map.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);
            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

            Long nowStudentNum = 0L;
            commonModuleServer.increaseStudentNumByCourseId(course_id);
            jedis.del(courseKey);
            HashMap<String, Object> queryMap = new HashMap<>();
            queryMap.put("course_id", course_id);
            Map<String, String> courseMap = readCourse(course_id, generateRequestEntity(null, null, null, queryMap), readCourseOperation, jedis, false);
            switch (courseMap.get("live_course_status")) {
                case "1":
                    MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);//更新时间
                    if (courseMap.get("live_course_status").equals("4")) {
                        jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_LIVE, 1, course_id);
                    } else {
                        jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_PREDICTION, 1, course_id);
                    }
                    break;
                case "2":
                    jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_FINISH, 1, course_id);
                    break;
            }
            //7.修改用户缓存信息中的加入课程数
            map.clear();
            map.put(Constants.CACHED_KEY_USER_FIELD, user_id);

            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, courseInfoMap);//删除已加入课程的key  在之后登录时重新加入
            jedis.del(key);//删除
            Map<String, String> userMap = readUser(user_id, this.generateRequestEntity(null, null, null, map), readUserOperation, jedis);
            nowStudentNum = MiscUtils.convertObjectToLong(courseInfoMap.get("student_num")) + 1;
            String levelString = MiscUtils.getConfigByKey("jpush_course_students_arrive_level");
            JSONArray levelJson = JSON.parseArray(levelString);
            if (levelJson.contains(nowStudentNum + "")) {
                JSONObject obj = new JSONObject();
                //String course_type = courseMap.get("course_type");
                String course_type_content = MiscUtils.convertCourseTypeToContent(course_type);
                obj.put("body", String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content"), course_type_content, MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")), nowStudentNum + ""));
                obj.put("to", courseInfoMap.get("lecturer_id"));
                obj.put("msg_type", "7");
                Map<String, String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type", "7");
                extrasMap.put("course_id", courseInfoMap.get("course_id"));
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
            }
            //course_type 0:公开课程 1:加密课程 2:收费课程',
            //TODO 加入课程推送   收费课程支付成功才推送消息
            if (!"2".equals(courseInfoMap.get("course_type"))) {
                //获取讲师的信息
                map.clear();
                map.put("lecturer_id", courseInfoMap.get("lecturer_id"));
                Map<String, String> user = readLecturer(courseInfoMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);

                Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
                TemplateData first = new TemplateData();
                first.setColor(Constants.WE_CHAT_PUSH_COLOR);
                String firstContent = String.format(MiscUtils.getConfigByKey("wpush_shop_course_first"), MiscUtils.RecoveryEmoji(courseInfoMap.get("course_title")));
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
                lastContent = MiscUtils.getConfigByKey("wpush_shop_course_lecturer_name") + MiscUtils.RecoveryEmoji(user.get("nick_name"));

                lastContent += "\n" + MiscUtils.getConfigByKey("wpush_shop_course_remark");

                Map<String, Object> studentUserMap = commonModuleServer.findLoginInfoByUserId(user_id);
                TemplateData remark = new TemplateData();
                remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
                remark.setValue(lastContent);
                templateMap.put("remark", remark);
                String url = String.format(MiscUtils.getConfigByKey("course_live_room_url"), courseInfoMap.get("course_id"), courseInfoMap.get("room_id"));
                WeiXinUtil.send_template_message((String) studentUserMap.get("web_openid"), MiscUtils.getConfigByKey("wpush_shop_course"), url, templateMap, jedis);
            }

            //公开课 执行这段逻辑
            if (equals(courseInfoMap.get("course_type").equals("0"))) {
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
        }
        return resultMap;
    }

    private Map<String, Object> joinSeries(String series_id, Jedis jedis, String user_id, String query_type) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        //1.1读取系列
        map.put("series_id", series_id);
        Map<String, String> seriesInfoMap = readSeries(series_id, generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
        String appName = seriesInfoMap.get("app_name");
        if (MiscUtils.isEmpty(seriesInfoMap)) {
            throw new QNLiveException("100004");
        }
        //如果是app 就判断是否是讲师
        if (query_type.equals("0")) {
            //1.2检测该用户是否为讲师，为讲师则不能加入该课程
            if (user_id.equals(seriesInfoMap.get("lecturer_id"))) {
                throw new QNLiveException("100017");
            }
        }

        //查询支付订单
        String series_type = seriesInfoMap.get("series_type");
        //3.检测学生是否参与了该课程
        Map<String, Object> studentQueryMap = new HashMap<>();
        studentQueryMap.put("user_id", user_id);
        studentQueryMap.put("series_id", series_id);
        if (commonModuleServer.isStudentOfTheSeries(studentQueryMap)) {//判断是否有加入课程
            throw new QNLiveException("120005");
        }

        //5.将学员信息插入到学员参与表中
        seriesInfoMap.put("user_id", user_id);
        seriesInfoMap.put("value_from", query_type);
        Map<String, Object> resultMap = commonModuleServer.joinSeries(seriesInfoMap);

        //6.修改讲师缓存中的课程参与人数
        map.clear();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, seriesInfoMap.get("lecturer_id"));
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
        jedis.hincrBy(lecturerKey, "total_student_num", 1);
        if ("1".equals(series_type)) {
            jedis.hincrBy(lecturerKey, "pay_student_num", 1);
        }
        map.clear();
        map.put(Constants.CACHED_KEY_SERIES_FIELD, series_id);
        String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);

        Long nowStudentNum = 0L;
        commonModuleServer.increaseStudentNumBySeriesId(series_id);
        jedis.del(seriesKey);
        Map<String, String> stringMap = readSeries(series_id, generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
        //7.修改用户缓存信息中的加入课程数
        map.clear();
        map.put(Constants.CACHED_KEY_USER_FIELD, user_id);

        String userSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_SERIES, seriesInfoMap);//删除已加入课程的key  在之后登录时重新加入
        jedis.zadd(userSeriesKey, System.currentTimeMillis(), series_id);
        //series_type 0:公开系列  1:收费系列',
        if (!"2".equals(seriesInfoMap.get("series_type"))) {
            //获取讲师的信息
            map.clear();
            map.put("lecturer_id", seriesInfoMap.get("lecturer_id"));
            Map<String, String> lecturer = readLecturer(seriesInfoMap.get("lecturer_id"), this.generateRequestEntity(null, null, null, map), readLecturerOperation, jedis);

            Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
            TemplateData first = new TemplateData();
            first.setColor(Constants.WE_CHAT_PUSH_COLOR);
            String firstContent = String.format(MiscUtils.getConfigByKey("wpush_shop_series_first"), MiscUtils.RecoveryEmoji(stringMap.get("series_title")));
            first.setValue(firstContent);
            templateMap.put("first", first);

            TemplateData courseTitle = new TemplateData();
            courseTitle.setColor(Constants.WE_CHAT_PUSH_COLOR);
            courseTitle.setValue(MiscUtils.RecoveryEmoji(lecturer.get("nick_name")));
            templateMap.put("keyword1", courseTitle);

            Date start_time = new Date(System.currentTimeMillis());
            TemplateData orderNo = new TemplateData();
            orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
            orderNo.setValue(MiscUtils.parseDateToFotmatString(start_time, "yyyy-MM-dd HH:mm:ss"));
            templateMap.put("keyword2", orderNo);

            Map<String, Object> studentUserMap = commonModuleServer.findLoginInfoByUserId(user_id);
            TemplateData remark = new TemplateData();
            remark.setColor(Constants.WE_CHAT_PUSH_COLOR_QNCOLOR);
            remark.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_series_remark"));
            templateMap.put("remark", remark);
            String url = MiscUtils.getConfigByKey("series_share_url_pre_fix") + series_id;//String.format(MiscUtils.getConfigByKey("series_share_url_pre_fix",appName), courseInfoMap.get("course_id"),  courseInfoMap.get("room_id"));
            WeiXinUtil.send_template_message((String) studentUserMap.get("web_openid"), MiscUtils.getConfigByKey("wpush_shop_series"), url, templateMap, jedis);
        }

        jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, seriesInfoMap.get("lecturer_id").toString());
        return resultMap;
    }
    /**
     * 查询订单
     * 根据订单结果进行加入课或系列
     * 1.查询当前订单结果
     * 2.如果不为成功或失败 访问微信查询订单
     * 3.根据订单结果进行处理
     *
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("queryOrder")
    public Map<String, Object> queryOrder(RequestEntity reqEntity) throws Exception {
        Map<String, String> reqMap = (Map<String, String>) reqEntity.getParam();
        Map<String, Object> resutltMap = new HashMap<>();
        String user_id = reqMap.get("user_id").toString();
        String pre_pay_no = reqMap.get("pre_pay_no").toString();//
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> tradeBill = commonModuleServer.findTradeBillByPrePayNo(pre_pay_no);
        String trade_id = tradeBill.get("trade_id").toString();
        Map<String, Object> billMap = commonModuleServer.findTradebillByOutTradeNo(trade_id);

        TenPayUtils.TradeState tradeState = TenPayUtils.queryOrder(null, trade_id);

        if (tradeState.getCode() == 0) {//订单处理成功
            //加入课程/加入系列
            resutltMap.put("tenPay",tradeState.getCode());
            String query_type = billMap.get("course_type").toString();
            if( billMap.get("profit_type").toString().equals("0")){//课程收益
                //3.检测学生是否参与了该课程
                Map<String, Object> studentQueryMap = new HashMap<>();
                studentQueryMap.put("user_id", user_id);
                studentQueryMap.put("course_id", billMap.get("course_id").toString());
                if (commonModuleServer.isStudentOfTheCourse(studentQueryMap)) {
                    resutltMap.put("join",true);
                }else{
                    try{
                        Map<String, Object> join = joinCourse(billMap.get("course_id").toString(), jedis, billMap.get("user_id").toString(), query_type);
                        resutltMap.putAll(join);
                        resutltMap.put("join",true);
                    }catch(Exception e){
                        resutltMap.put("join",false);
                    }
                }
            }else if( billMap.get("profit_type").toString().equals("2")){//系列课程
                Map<String, Object> studentQueryMap = new HashMap<>();
                studentQueryMap.put("user_id", user_id);
                studentQueryMap.put("series_id", billMap.get("course_id").toString());
                if (commonModuleServer.isStudentOfTheSeries(studentQueryMap)) {//判断是否有加入课程
                    resutltMap.put("join",true);
                }else{
                    try{
                        Map<String, Object> join = joinSeries(billMap.get("course_id").toString(), jedis, billMap.get("user_id").toString(), query_type);
                        resutltMap.putAll(join);
                        resutltMap.put("join",true);
                    }catch(Exception e){
                        resutltMap.put("join",false);
                    }
                }
            }
        }
        return resutltMap;
    }

    /**
     * 微信支付成功后回调的方法
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("handleWeixinPayResult")
    public String handleWeixinPayResult(RequestEntity reqEntity) throws Exception {
        String resultStr;//静态
        SortedMap<String, String> requestMapData = (SortedMap<String, String>) reqEntity.getParam();
        String outTradeNo = requestMapData.get("out_trade_no");
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> billMap = commonModuleServer.findTradebillByOutTradeNo(outTradeNo);
        if (billMap != null && billMap.get("status").equals("2")) {
            logger.debug("====>　已经处理完成, 不需要继续。流水号是: " + outTradeNo);
            return TenPayConstant.SUCCESS;
        }
        if (TenPayUtils.isValidSign(requestMapData)) {// MD5签名成功，处理课程打赏\购买课程等相关业务
            //if(true){
            logger.debug(" ===> 微信notify Md5 验签成功 <=== ");

            if ("SUCCESS".equals(requestMapData.get("return_code")) && "SUCCESS".equals(requestMapData.get("result_code"))) {

                resultStr = payBill(billMap, jedis, requestMapData);

                //加入课程/加入系列
                String query_type = billMap.get("course_type").toString();
                if( billMap.get("profit_type").toString().equals("0")) {//课程收益
                    //3.检测学生是否参与了该课程
                    Map<String, Object> studentQueryMap = new HashMap<>();
                    studentQueryMap.put("user_id", billMap.get("user_id").toString());
                    studentQueryMap.put("course_id", billMap.get("course_id").toString());
                    if (!commonModuleServer.isStudentOfTheCourse(studentQueryMap)) {
                        try {
                            Map<String, Object> join = joinCourse(billMap.get("course_id").toString(), jedis, billMap.get("user_id").toString(), query_type);
                        } catch (Exception e) {
                            logger.error(e.toString());
                        }
                    }
                }


                if( billMap.get("profit_type").toString().equals("2")){//系列课程
                    Map<String, Object> studentQueryMap = new HashMap<>();
                    studentQueryMap.put("user_id", billMap.get("user_id").toString());
                    studentQueryMap.put("series_id", billMap.get("course_id").toString());
                    if (!commonModuleServer.isStudentOfTheSeries(studentQueryMap)) {//判断是否有加入课程
                        try{
                            Map<String, Object> join = joinSeries(billMap.get("course_id").toString(), jedis, billMap.get("user_id").toString(), query_type);
                        }catch (Exception e){
                            logger.error(e.toString());
                        }

                    }
                }
            } else {
                logger.debug("==> 微信支付失败 ,流水 ：" + outTradeNo);
                resultStr = TenPayConstant.FAIL;
            }

        } else {// MD5签名失败
            logger.debug("==> fail -Md5 failed");
            resultStr = TenPayConstant.FAIL;
        }
        return resultStr;
    }
    private String payBill(Map<String, Object> billMap, Jedis jedis,Map<String, String> requestMapData) throws Exception {
        //数据初始化
        String userId = billMap.get("user_id").toString();
        String shopId = null;
        //2.为插入数据库准备相关数据，处理数据库中相关部分
        Map<String, Object> requestValues = new HashMap<>();

        if (billMap.get("shop_id") != null) {
            shopId = billMap.get("shop_id").toString();
            requestValues.put("shop_id",shopId);
        }
        //0.先检测课程存在情况和状态
        String courseId = (String) billMap.get("course_id");
        Map<String, Object> query = new HashMap<>();
        query.put("course_id", courseId);
        Map<String, String> courseMap;

        //判断系列课与课程
        if ("2".equals(billMap.get("profit_type").toString())) {
            //系列课
            query.put("series_id", courseId);
            courseMap = readSeries(courseId, generateRequestEntity(null, null, null, query), readSeriesOperation, jedis, false);
        } else {
            //课程打赏，课程（分销，出售）
            courseMap = readCourse(courseId, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, false);
        }
        String lecturerId = courseMap.get("lecturer_id");
        //受益人ID
        String profitUserId = billMap.get("guest_id")==null?lecturerId:billMap.get("guest_id").toString();

        if (MiscUtils.isEmpty(courseMap)) {
            throw new QNLiveException("100004");
        }

        String profit_type = billMap.get("profit_type").toString();
        //系列类型（1直播(包括系列直播) 2店铺课程）
        String courseType = billMap.get("course_type") + "";

        //根据不同app处理不同收益情况
        requestValues.put("appid", requestMapData.get("appid"));
        requestValues.put("attach", requestMapData.get("attach"));
        requestValues.put("bank_type", requestMapData.get("bank_type"));
        requestValues.put("cash_fee", requestMapData.get("cash_fee"));
        requestValues.put("coupon_count", requestMapData.get("coupon_count"));
        requestValues.put("coupon_fee", requestMapData.get("coupon_fee"));
        requestValues.put("coupon_fee_0", requestMapData.get("coupon_fee_0"));
        requestValues.put("coupon_id_0", requestMapData.get("coupon_id_0"));
        requestValues.put("fee_type", requestMapData.get("fee_type"));
        requestValues.put("is_subscribe", requestMapData.get("is_subscribe"));
        requestValues.put("mch_id", requestMapData.get("mch_id"));
        requestValues.put("nonce_str", requestMapData.get("nonce_str"));
        requestValues.put("openid", requestMapData.get("openid"));
        requestValues.put("out_trade_no", requestMapData.get("out_trade_no"));
        requestValues.put("result_code", requestMapData.get("result_code"));
        requestValues.put("return_code", requestMapData.get("return_code"));
        requestValues.put("sign", requestMapData.get("sign"));
        requestValues.put("time_end", requestMapData.get("time_end"));
        requestValues.put("total_fee", requestMapData.get("total_fee"));
        requestValues.put("trade_type", requestMapData.get("trade_type"));
        requestValues.put("transaction_id", requestMapData.get("transaction_id"));

        requestValues.put("courseInCache", courseMap);
        requestValues.put("tradeBillInCache", billMap);
        Map<String, String> distributeRoom = null;

        //Map<String,Object> userDistributionInfo = null;
        Map<String, Object> recommendMap = null;
        Map<String, Object> handleResultMap = commonModuleServer.handleWeixinPayResult(requestValues);
        long lecturerProfit = (Long) handleResultMap.get("lecture_amount");

        query.clear();
        query.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);
        jedis.del(key);
        //3.处理缓存中的数据
        //3.4如果是购买课程，且存在分销行为，则进行相关缓存处理
        Map<String, Object> sumInfo;
        //3.1 t_lecturer  缓存
        query.clear();
        query.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query);
        //如果是系列课直播也做以前的 直播统计
        if ("0".equals(profit_type) || "2".equals(profit_type)) {
            //直播统计(过滤店铺统计)
            if ("1".equals(courseType)) {
                jedis.hincrBy(lecturerKey, "total_student_num", 1);
                jedis.hincrBy(lecturerKey, "pay_student_num", 1);
                jedis.hincrBy(lecturerKey, "room_done_num", 1);
                if (!MiscUtils.isEmpty(distributeRoom)) {
                    jedis.hincrBy(lecturerKey, "room_distributer_done_num", 1);
                }
            }
        }

        //直播间,讲师统计
        if ("1".equals(courseType)) {
            query.clear();
            query.put(Constants.CACHED_KEY_LECTURER_FIELD, courseMap.get("lecturer_id"));
            sumInfo = commonModuleServer.findCoursesSumInfo(query);
            jedis.hset(lecturerKey, "total_amount", MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit")) + "");
            jedis.sadd(Constants.CACHED_UPDATE_LECTURER_KEY, lecturerId);

            //3.2 直播间缓存 t_live_room(直播间存在时)
            if (handleResultMap.get("room_id") != null) {
                query.clear();
                query.put(Constants.CACHED_KEY_SHOP_FIELD, handleResultMap.get("room_id").toString());
                String liveShopKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SHOP, query);
                sumInfo = commonModuleServer.findCoursesSumInfo(query);
                jedis.hset(liveShopKey, "total_amount", MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit")) + "");
            }
        }
        //用户已购课程
        query.clear();
        query.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String userBuyKey = MiscUtils.getKeyOfCachedData(Constants.SYS_USER_BUY_LIST, query);
        jedis.sadd(userBuyKey, courseId);

        //店铺今日统计
        query.clear();
        query.put(Constants.CACHED_KEY_USER_FIELD, profitUserId);
        String userCountKey = MiscUtils.getKeyOfCachedData(Constants.SHOP_DAY_COUNT, query);
        if (!jedis.exists(userCountKey)) {
            long milliSecondsLeftToday = 86400000 - DateUtils.getFragmentInMilliseconds(Calendar.getInstance(), Calendar.DATE);
            //今日收入
            jedis.hincrBy(userCountKey, "day_amount", lecturerProfit);
            //今日付费人数
            jedis.hincrBy(userCountKey, "day_pay_user", 1);
            //设置失效时间为今天
            jedis.expire(userCountKey, Integer.valueOf((milliSecondsLeftToday / 1000) + ""));
        } else {
            jedis.hincrBy(userCountKey, "day_amount", lecturerProfit);
        }

        //3.3 课程缓存或者表 t_courses
        if ("0".equals(profit_type)) {
            //jedis.hincrBy(courseKey, "student_num", 1);
            Map<String, Object> param = new HashMap<>();
            param.put("course_id", courseId);
            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, param);

            Map<String, Object> numInfo = commonModuleServer.findCourseRecommendUserNum(param);
            long num = 0;
            if (!MiscUtils.isEmpty(numInfo)) {
                num = MiscUtils.convertObjectToLong(numInfo.get("recommend_num"));
            }
            jedis.hset(courseKey, "student_num", num + "");

            param = new HashMap<>();
            param.put("course_id", courseId);
            param.put("profit_type", "0");
            sumInfo = commonModuleServer.findCoursesSumInfo(param);
            String courseAmount = MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit")) + "";
            jedis.hset(courseKey, "course_amount", courseAmount);

            Map<String, Object> course = new HashMap<>();
            course.put("course_id", courseId);
            course.put("course_amount", courseAmount);
            course.put("course_type", courseType);
            //同步课程收入到数据库
            commonModuleServer.updateCourseCmountByCourseId(course);
            //规避
            if (!courseAmount.equals(jedis.hget(courseKey, "series_amount"))) {
                jedis.hset(courseKey, "course_amount", courseAmount);
            }
        } else if ("1".equals(profit_type)&&billMap.get("guest_id")==null) {
            query.clear();
            query.put("course_id", courseId);
            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, query);

            query.put("profit_type", "1");
            //直播间打赏
            query.put("lecturer_id", lecturerId);
            sumInfo = commonModuleServer.findCoursesSumInfo(query);
            String extraAmount = MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit")) + "";
            jedis.hset(courseKey, "extra_amount", extraAmount);
            String count = "0";
            if (sumInfo != null) {
                count = sumInfo.get("counts").toString();
            }
            //同步打赏收入到数据库
            Map<String, Object> course = new HashMap<>();
            course.put("course_id", courseId);
            course.put("course_type", courseType);
            course.put("extra_amount", extraAmount);
            course.put("extra_num", count);
            jedis.hset(courseKey, "extra_num", count);
            commonModuleServer.updateCourseCmountByCourseId(course);
        } else if ("2".equals(profit_type)) {
            //系列课收益统计
            Map<String, Object> param = new HashMap<>();
            param.put("series_id", courseId);
            String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, param);
            param.put("course_id", courseId);
            param.put("profit_type", "2");
            sumInfo = commonModuleServer.findCoursesSumInfo(param);
            String lecturer_profit = MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit")) + "";
            logger.error("series_test_13006660" + seriesKey + "  " + lecturer_profit);

            Map<String, Object> course = new HashMap<>();
            course.put("series_id", courseId);
            course.put("series_amount", lecturer_profit);
            //同步系列课收入到数据库
            commonModuleServer.updateSeriesCmountByCourseId(course);
        }
        //TODO 系列课除外 暂时不做热门推荐
        if (courseMap.get("live_course_status")!=null) {
            switch (courseMap.get("live_course_status")) {
                case "1":
                    MiscUtils.courseTranferState(System.currentTimeMillis(), courseMap);//更新时间
                    if (courseMap.get("live_course_status").equals("4")) {
                        jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_LIVE, 1, courseId);
                    } else {
                        jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_PREDICTION, 1, courseId);
                    }
                    break;
                case "2":
                    jedis.zincrby(Constants.SYS_COURSES_RECOMMEND_FINISH, 1, courseId);
                    break;
            }
        }
        query.clear();
        query.put("lecturer_id", lecturerId);
        Map<String, String> lecturerMap;
        if(billMap.get("guest_id")!=null){
            String guestId = billMap.get("guest_id").toString();
            //嘉宾信息
            lecturerMap = readLecturer(guestId, this.generateRequestEntity(null, null, null, query), readLecturerOperation, jedis);

            //嘉宾在直播间的收益缓存统计
            query.clear();
            query.put("course_id", courseId);
            query.put("profit_type", "1");
            query.put("lecturer_id", guestId);
            sumInfo = commonModuleServer.findCoursesSumInfo(query);
            query.put("user_id",guestId);
            String guestProfitKey = MiscUtils.getKeyOfCachedData(Constants.SYS_GUEST_COURSE_PROFIT, query);
            String guestProfit = MiscUtils.convertObjectToLong(sumInfo.get("lecturer_profit")) + "";
            //嘉宾-课程-->打赏收益
            jedis.hset(guestProfitKey,courseId,guestProfit);
        }else{
            //讲师信息
            lecturerMap = readLecturer(lecturerId, this.generateRequestEntity(null, null, null, query), readLecturerOperation, jedis);
        }

        if (profit_type.equals("1")) {
            String mGroupId = courseMap.get("im_course_id").toString();
            query.clear();
            query.put("user_id", handleResultMap.get("user_id"));
            Map<String, String> payUserMap = readUser((String) handleResultMap.get("user_id"), this.generateRequestEntity(null, null, null, query), readUserOperation, jedis);

            HashMap<String, String> payMessageMap = new HashMap<>();
            payMessageMap.put("pay_user", payUserMap.get("nick_name"));//打赏人名字
            payMessageMap.put("pay_message", MiscUtils.getConfigByKey("pay_message"));//打赏信息
            payMessageMap.put("collect_user", lecturerMap.get("nick_name"));//被打赏人的名字
            payMessageMap.put("money_num", ((Long) handleResultMap.get("profit_amount") / 100.0) + "");//钱
            payMessageMap.put("money_unit", MiscUtils.getConfigByKey("money_unit"));//金币单元
            String message = JSON.toJSONString(payMessageMap);
            long currentTime = System.currentTimeMillis();
            String sender = "system";
            Map<String, Object> infomation = new HashMap<>();
            infomation.put("course_id", handleResultMap.get("course_id"));//课程id
            infomation.put("creator_id", handleResultMap.get("user_id"));//消息发送人id
            infomation.put("message", message);//消息
            infomation.put("send_type", "4");//4.打赏信息
            infomation.put("message_type", "1");//1.文字
            infomation.put("create_time", currentTime);//创建时间
            infomation.put("message_id", MiscUtils.getUUId());//
            infomation.put("message_imid", infomation.get("message_id"));
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("msg_type", "1");
            messageMap.put("send_time", currentTime);
            messageMap.put("information", infomation);
            messageMap.put("mid", infomation.get("message_id"));
            String content = JSON.toJSONString(messageMap);
            IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);

        } else if (profit_type.equals("0")) {
            Long nowStudentNum;
            //修改用户缓存信息中的加入课程数
            query.clear();
            query.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String userCacheKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query);
            if (jedis.exists(userCacheKey)) {
                jedis.hincrBy(userCacheKey, "course_num", 1L);
            } else {
                Map<String,Object> map = new HashMap<>();
                map.put("user_id",userId);
                readUser(userId, this.generateRequestEntity(null, null, null, map), readUserOperation, jedis);
                jedis.hincrBy(userCacheKey, "course_num", 1L);
            }
            jedis.sadd(Constants.CACHED_UPDATE_USER_KEY, userId);
            nowStudentNum = Long.parseLong(courseMap.get("student_num")) + 1;
            String levelString = MiscUtils.getConfigByKey("jpush_course_students_arrive_level");
            JSONArray levelJson = JSON.parseArray(levelString);
            if (levelJson.contains(nowStudentNum + "")) {
                JSONObject obj = new JSONObject();
                String course_type = courseMap.get("course_type");
                String course_type_content = MiscUtils.convertCourseTypeToContent(course_type);
                obj.put("body", String.format(MiscUtils.getConfigByKey("jpush_course_students_arrive_level_content"), course_type_content, MiscUtils.RecoveryEmoji(courseMap.get("course_title")), nowStudentNum + ""));
                obj.put("to", courseMap.get("lecturer_id"));
                obj.put("msg_type", "7");
                Map<String, String> extrasMap = new HashMap<>();
                extrasMap.put("msg_type", "7");
                extrasMap.put("course_id", courseMap.get("course_id"));
                obj.put("extras_map", extrasMap);
                JPushHelper.push(obj);
            }
        }
        String resultStr = TenPayConstant.SUCCESS;
        String user_id = (String) billMap.get("user_id");
        String course_id = (String) billMap.get("course_id");
        //直播间存在时则通知
        if (!StringUtils.isBlank(user_id) && StringUtils.isNotEmpty(shopId)) {
            Map<String, Object> loginInfoUser = commonModuleServer.findLoginInfoByUserId(user_id);
            String openId = (String) loginInfoUser.get("web_openid");
            //  成员报名付费通知 老师 暂时 不需要了
            //wpushLecture(billMap, jedis, openId, courseByCourseId, user);
            //成功购买课程则通知学生
            if (profit_type.equals("0")) {
                wpushUser(jedis, openId, courseMap, lecturerMap, course_id, shopId);
            }
        }
        return resultStr;
    }

    private void wpushUser(Jedis jedis, String openId,
                           Map<String, String> courseByCourseId,
                           Map<String, String> lecturerUser, String courseId, String shopId) {
        Map<String, TemplateData> templateMap = new HashMap<>();
        TemplateData first = new TemplateData();
        first.setColor(Constants.WE_CHAT_PUSH_COLOR);
        String firstContent = String.format(MiscUtils.getConfigByKey("wpush_shop_course_first"), MiscUtils.RecoveryEmoji(courseByCourseId.get("course_title")));
        first.setValue(firstContent);
        templateMap.put("first", first);

        TemplateData courseTitle = new TemplateData();
        courseTitle.setColor(Constants.WE_CHAT_PUSH_COLOR);
        courseTitle.setValue(MiscUtils.RecoveryEmoji(courseByCourseId.get("course_title")));
        templateMap.put("keyword1", courseTitle);

        Date start_time = new Date(Long.parseLong(courseByCourseId.get("start_time")));
        TemplateData orderNo = new TemplateData();
        orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
        orderNo.setValue(MiscUtils.parseDateToFotmatString(start_time, "yyyy-MM-dd HH:mm:ss"));
        templateMap.put("keyword2", orderNo);

        String lastContent;
        lastContent = MiscUtils.getConfigByKey("wpush_shop_course_lecturer_name") + MiscUtils.RecoveryEmoji(lecturerUser.get("nick_name"));
        lastContent += "\n" + MiscUtils.getConfigByKey("wpush_shop_course_remark");

        TemplateData remark = new TemplateData();
        remark.setColor(Constants.WE_CHAT_PUSH_COLOR);
        remark.setValue(lastContent);
        templateMap.put("remark", remark);
        String url = String.format(MiscUtils.getConfigByKey("course_live_shop_url"), courseId, shopId);
        WeiXinUtil.send_template_message(openId, MiscUtils.getConfigByKey("wpush_shop_course"), url, templateMap, jedis);
    }
    /**
     * 搜索/类型
     *
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("search")
    public Map<String, Object> search(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> map = (Map<String, Object>) reqEntity.getParam();
        reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//用安全证书拿userId
        String search_text = map.get("search_text").toString();//搜索文本
        if (search_text != null || !search_text.equals("")) {
            search_text = "%" + search_text + "%";//拼接百分号 进行包含查询
            map.put("search_text", search_text);
        }
        Integer page_num = Integer.valueOf(map.get("page_num").toString());
        map.put("page_num", page_num);
        Integer page_count = Integer.valueOf(map.get("page_count").toString());
        map.put("page_count", page_count);
        int search_type = Integer.valueOf(map.get("search_type").toString()); //查询类型 0所有 1直播间 2课程 3系列
        Jedis jedis = jedisUtils.getJedis();
        //TODO 查询所有 或者 查询店铺
        if (search_type == 0 || search_type == 1) {
            //List<Map<String, Object>> liveRoomBySearch = commonModuleServer.findshop(map);
        }

        //查询所有 或者 查询课程
        if (search_type == 0 || search_type == 2) {
            List<Map<String, Object>> courseBySearch = commonModuleServer.findCourseBySearch(map);//查询数据
            resultMap.put("course_list", this.setStudentAndLecturerNickName(courseBySearch, userId, jedis, Thread.currentThread().getStackTrace()[1].getMethodName()));
        }


        //查询所有 或者 查询系列
        if (search_type == 0 || search_type == 3) {
            map.put("series_course_type", Constants.DEFAULT_SERIES_COURSE_TYPE);
            List<Map<String, Object>> seriesBySearch = commonModuleServer.findSeriesBySearch(map);//查询数据
            setLecturerNickName(seriesBySearch, jedis);
            resultMap.put("series_list", seriesBySearch);
        }

        return resultMap;
    }
    private List<Map<String, Object>> setStudentAndLecturerNickName(List<Map<String, Object>> courseBySearch, String userId, Jedis jedis, String methodName) throws Exception {
        long currentTime = System.currentTimeMillis();//当前时间

        if (!MiscUtils.isEmpty(courseBySearch)) {
            Map<String, Object> query = new HashMap<String, Object>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            query.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);//用来查询当前用户加入了那些课程
            for (Map<String, Object> course : courseBySearch) {
                if ("recommendCourse".equals(methodName)) {
                    MiscUtils.courseTranferState(currentTime, course, Long.valueOf(course.get("start_time").toString()));
                } else {
                    Date date = sdf.parse(course.get("start_time").toString());
                    MiscUtils.courseTranferState(currentTime, course, date.getTime());//进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                }


                if (!MiscUtils.isEmpty(jedis.zrank(key, course.get("course_id").toString()))) {//判断当前用户是否有加入这个课程
                    course.put("student", "Y");
                } else {
                    course.put("student", "N");
                }
            }
            this.setLecturerNickName(courseBySearch, jedis);

            return courseBySearch;
        }
        return null;
    }
    private void setLecturerNickName(List<Map<String, Object>> list, Jedis jedis) throws Exception {
        Map<String, String> query1 = new HashMap<>();
        for (Map<String, Object> course : list) {
            query1.put(Constants.CACHED_KEY_LECTURER_FIELD, course.get("lecturer_id").toString());
            String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query1);
            String lecturer_nick_name = jedis.hget(key, "nick_name");
            course.put("lecturer_nick_name", lecturer_nick_name);
        }
    }
    /**
     * 获取课程消息列表
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("messageList")
    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());//查询的条数
        int userType = Integer.parseInt(reqMap.get("user_type").toString());//查询角色类型 0老师/顾问  1用户
        int direction = Integer.parseInt(reqMap.get("direction").toString());//方向  0旧  1新 默认为1
        String course_id = reqMap.get("course_id").toString();//课程id
        Jedis jedis = jedisUtils.getJedis();//获取缓存方法对象
        Map<String, Object> map = new HashMap<>();//map
        map.put(Constants.CACHED_KEY_COURSE_FIELD, course_id);//存入map中
        String messageListKey = null;//查找信息类的key
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("course_id", course_id);
        queryMap.put("page_count", pageCount);//要查询的记录数据
        queryMap.put("user_type", userType);//查询角色类型
        queryMap.put("direction", direction);//方向
        if (reqMap.get("message_imid") != null && StringUtils.isNotBlank(reqMap.get("message_imid").toString())) { //传过来的信息位置
            queryMap.put("message_imid", reqMap.get("message_imid").toString());
        }
        Map<String, String> courseMap = jedis.hgetAll(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map));
        if (courseMap.get("status").equals("2")) { //查询当前课程是否结束
            List<Map<String, Object>> messageList = commonModuleServer.findCourseMessageListByComm(queryMap);
            if (!CollectionUtils.isEmpty(messageList)) {
                for (Map<String, Object> messageMap : messageList) {
                    if (!MiscUtils.isEmpty(messageMap.get("message")))//信息
                        messageMap.put("message", MiscUtils.RecoveryEmoji(messageMap.get("message").toString()));

                    if (!MiscUtils.isEmpty(messageMap.get("message_question")))//问题 互动
                        messageMap.put("message_question", MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));

                    if (!MiscUtils.isEmpty(messageMap.get("creator_nick_name")))//名字 表情
                        messageMap.put("creator_nick_name", MiscUtils.RecoveryEmoji(messageMap.get("creator_nick_name").toString()));
                }

                if (direction == 1 && reqMap.get("message_imid").toString().equals("")) {
                    Collections.reverse(messageList);
                }
                if (direction == 0 && !reqMap.get("message_imid").toString().equals("")) {
                    Collections.reverse(messageList);
                }


                resultMap.put("message_list", messageList);
            }
            if (queryMap.containsKey("message_imid")) {//如果有imid
                resultMap.put("this_message", commonModuleServer.findCourseMessageByComm(queryMap));
            }
            resultMap.put("message_count", commonModuleServer.findCourseMessageSum(queryMap));
            return resultMap;
        } else { //TODO 查询缓存
            //当前课程没有结束 可以直接查询缓存
            if (userType == 0) {//查询老师
                if (reqMap.get("message_type") != null && reqMap.get("message_type").toString().equals("0")) {
                    messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER_VOICE, map);
                } else {
                    messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
                }
            } else if (userType == 1) {//查询用户
                messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_USER, map);
            }
            long message_sum = jedis.zcard(messageListKey);//总共有多少个总数
            resultMap.put("message_count", message_sum);
            //缓存中存在，则读取缓存中的内容
            //初始化下标
            long startIndex = 0; //开始下标
            long endIndex = -1;   //结束下标
            Set<String> messageImIdList = null;//消息集合你
            //来确定消息集合 0旧 -> 1...新
            if (reqMap.get("message_imid") != null && StringUtils.isNotBlank(reqMap.get("message_imid").toString())) {   //message_imid 过来

                long endRank = jedis.zrank(messageListKey, reqMap.get("message_imid").toString());//message_imid 在缓存zset中的排名
                if (direction == 0) { //获取比当前message旧的消息
                    endIndex = endRank - 1;
                    if (endIndex >= 0) {
                        startIndex = endIndex - pageCount + 1;
                        if (startIndex < 0) {
                            startIndex = 0;
                        }
                        messageImIdList = jedis.zrange(messageListKey, startIndex, endIndex);//消息集合
                    } else {
                        messageImIdList = null;//消息集合
                    }
                } else {//获取比当前message新消息
                    startIndex = endRank + 1;
                    endIndex = startIndex + pageCount - 1; //消息位置
                    messageImIdList = jedis.zrange(messageListKey, startIndex, endIndex);//消息集合
                }

                String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, queryMap);

                if (jedis.exists(messageKey)) {
                    Map<String, String> messageMap = jedis.hgetAll(messageKey);
                    //更改用户名和昵称
                    if (!MiscUtils.isEmpty(messageMap)) {
                        if (messageMap.get("creator_id") != null) {
                            Map<String, Object> innerMap = new HashMap<>();
                            innerMap.put("user_id", messageMap.get("creator_id"));
                            Map<String, String> userMap = readUser(messageMap.get("creator_id"), this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedis);
                            if (!MiscUtils.isEmpty(userMap)) {
                                if (userMap.get("nick_name") != null) {
                                    messageMap.put("creator_nick_name", userMap.get("nick_name"));
                                }
                                if (userMap.get("avatar_address") != null) {
                                    messageMap.put("creator_avatar_address", userMap.get("avatar_address"));
                                }
                            }
                        }
                        String messageContent = messageMap.get("message");
                        if (!MiscUtils.isEmpty(messageContent)) {
                            messageMap.put("message", MiscUtils.RecoveryEmoji(messageContent));
                        }
                        if (!MiscUtils.isEmpty(messageMap.get("message_question"))) {
                            messageMap.put("message_question", MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
                        }
                        resultMap.put("this_message", messageMap);
                    }
                }

            } else {// 如果没有传过来message_id
                //判断需求是要新的信息 还是就得信息
                if (direction == 0) {//从最早的信息开始
                    startIndex = 0;
                    endIndex = pageCount - 1;
                    messageImIdList = jedis.zrange(messageListKey, startIndex, endIndex);//消息集合
                } else {//从最新的信息开始
                    endIndex = -1;
                    startIndex = message_sum - pageCount;
                    if (startIndex < 0) {
                        startIndex = 0;
                    }
                    messageImIdList = jedis.zrange(messageListKey, startIndex, endIndex);//消息集合
                }
            }

            if (!CollectionUtils.isEmpty(messageImIdList)) {
                //缓存中存在则读取缓存内容
                List<Map<String, String>> messageListCache = new ArrayList<>();
                for (String messageImId : messageImIdList) {
                    map.put(Constants.FIELD_MESSAGE_ID, messageImId);
                    String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
                    Map<String, String> messageMap = jedis.hgetAll(messageKey);
                    messageMap.put("message_pos", startIndex + "");
                    //更改用户名和昵称
                    if (MiscUtils.isEmpty(messageMap)) {
                        continue;
                    }
                    if (messageMap.get("creator_id") != null) {
                        Map<String, Object> innerMap = new HashMap<>();
                        innerMap.put("user_id", messageMap.get("creator_id"));
                        Map<String, String> userMap = readUser(messageMap.get("creator_id"), this.generateRequestEntity(null, null, null, innerMap), readUserOperation, jedis);
                        if (!MiscUtils.isEmpty(userMap)) {
                            if (userMap.get("nick_name") != null) {
                                messageMap.put("creator_nick_name", userMap.get("nick_name"));
                            }
                            if (userMap.get("avatar_address") != null) {
                                messageMap.put("creator_avatar_address", userMap.get("avatar_address"));
                            }
                        }
                    }
                    String messageContent = messageMap.get("message");
                    if (!MiscUtils.isEmpty(messageContent)) {
                        messageMap.put("message", MiscUtils.RecoveryEmoji(messageContent));
                    }
                    if (!MiscUtils.isEmpty(messageMap.get("message_question"))) {
                        messageMap.put("message_question", MiscUtils.RecoveryEmoji(messageMap.get("message_question").toString()));
                    }
                    messageListCache.add(messageMap);
                    startIndex++;
                }
                resultMap.put("message_list", messageListCache);
            }
            return resultMap;
        }
    }
}
