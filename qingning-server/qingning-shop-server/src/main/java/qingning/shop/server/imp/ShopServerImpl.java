package qingning.shop.server.imp;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.dj.HttpClientUtil;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.common.util.SharingConstants;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.IShopModuleServer;
import redis.clients.jedis.Jedis;

import java.util.*;

public class ShopServerImpl extends AbstractQNLiveServer {
    private static Logger log = LoggerFactory.getLogger(ShopServerImpl.class);

    private IShopModuleServer shopModuleServer;

    @Override
    public void initRpcServer() {
        if (shopModuleServer == null) {
            shopModuleServer = this.getRpcService("shopModuleServer");
            readCourseOperation = new ReadCourseOperation(shopModuleServer);
            readShopOperation = new ReadShopOperation(shopModuleServer);
            readUserOperation = new ReadUserOperation(shopModuleServer);
            readSeriesOperation = new ReadSeriesOperation(shopModuleServer);
            readLecturerOperation =  new ReadLecturerOperation(shopModuleServer);
            readConfigOperation = new ReadConfigOperation(shopModuleServer);
        }
    }
    @FunctionName("shopOpen")
    public Map<String, Object> shopOpen(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = new HashMap<>();
        reqEntity.setParam(reqMap);
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String, Object> resultMap = new HashMap<>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //1.查看用户是否开通店铺
        String shopId = getAccessInfoByToken(reqEntity.getAccessToken(),Constants.CACHED_KEY_SHOP_FIELD,jedis);
        if(StringUtils.isNotEmpty(shopId)){
            throw new QNLiveException("210005");
        }
        shopId = MiscUtils.getUUId();
        reqMap.put("user_id",userId);
        //2.插入店铺信息
        Map<String,String> userInfo = readUser(userId,reqEntity,readUserOperation,jedis);
        Map<String,Object> shop = new HashMap<>();
        shop.put("lecturer_id",userId);
        shop.put("shop_id",shopId);
        shop.put("user_name",userInfo.get("nick_name")+"");
        shop.put("shop_name",userInfo.get("nick_name")+"的知识店铺");
        shop.put("shop_remark","");
        String shopUrl = MiscUtils.getConfigByKey("share_url_shop_index")+shop.get("shop_id");
        shop.put("shop_url",shopUrl);
        shop.put("status","1");
        shop.put("create_time",new Date());
        shop.put("shop_logo",userInfo.get("avatar_address"));
        shopModuleServer.insertShop(shop);

        String user_role = this.getAccessInfoByToken(reqEntity.getAccessToken(),"user_role",jedis);

        //3.缓存修改
        //如果是刚刚成为讲师，则增加讲师信息缓存，并且修改access_token缓存中的身份
        Map<String,Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, reqMap.get("user_id").toString());

        RequestEntity queryOperation = this.generateRequestEntity(null,null, null, map);
        this.readLecturer(userId, queryOperation, readShopOperation, jedis);
        this.updateAccessInfoByToken(reqEntity.getAccessToken(),"user_role",user_role+","+Constants.USER_ROLE_LECTURER,jedis);
        this.updateAccessInfoByToken(reqEntity.getAccessToken(),Constants.CACHED_KEY_SHOP_FIELD,shopId,jedis);
        //4.增加讲师直播间信息缓存
        jedis.sadd(Constants.CACHED_LECTURER_KEY, userId);
        map.clear();
        map.put("shop_id", shopId);
        this.readShop(shopId, map, CommonReadOperation.CACHE_READ_SHOP, false,jedis);

        resultMap.put("shop_id", shopId);
        return resultMap;
    }
    /**
     * 店铺-店铺信息
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopInfo")
    public Map<String,String> shopInfo(RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String,Object> param = new HashMap<>();
        param.put("user_id",userId);
        reqEntity.setParam(param);
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String,String> userMap = this.readUser(userId, reqEntity,readUserOperation, jedis);
        Map<String,String> shop = this.readShopByUserId(userId, reqEntity, jedis);
        /*if(shop.get("open_sharing").equals("1")){
            log.debug("同步讲师token  user_id : "+userId +" token : "+reqEntity.getAccessToken());
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("version", "1.2.0");
            headerMap.put("Content-Type", "application/json;charset=UTF-8");
            headerMap.put("access_token",reqEntity.getAccessToken());
            //获取知享课程数
            String getUrl = MiscUtils.getConfigByKey("sharing_api_url")
                    + SharingConstants.SHARING_SERVER_USER_COMMON
                    +SharingConstants.SHARING_USER_COMMON_GENERATE_TOKEN;
            String result = HttpClientUtil.doGet(getUrl, headerMap, null, "UTF-8");
            shop.put("synchronization_token",result);
        }*/

        shop.put("avatar_address",userMap.get("avatar_address"));
        shop.put("user_id",userId);
        shop.put("nick_name",userMap.get("nick_name"));
        shop.put("phone_num",userMap.get("phone_number"));
        /*
         * 返回店铺预览url、电脑端特性url、使用教程url
         */
        //从数据库查询url
        String sysKeys = "'shop_preview_url','pc_introduce_url','use_url'";
        Map<String, Object> selectSysConfigMap = new HashMap<>();
        selectSysConfigMap.put("config_key", sysKeys);
        shop.put("shop_preview_url", systemConfigStringMap.get("shop_preview_url"));
        shop.put("pc_introduce_url", systemConfigStringMap.get("pc_introduce_url"));
        shop.put("use_url", systemConfigStringMap.get("use_url"));

        //直播分享URL
        shop.put("live_url",MiscUtils.getConfigByKey("course_share_url_pre_fix"));
        return shop;
    }

    @SuppressWarnings("unchecked")
    @FunctionName("createCourse")
    public Map<String, Object> createCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String lecturer_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//通过token 获取讲师id
        Jedis jedis = jedisUtils.getJedis();
        Map query = new HashMap();
        query.put(Constants.CACHED_KEY_LECTURER_FIELD,lecturer_id);
        Map<String,String> lecturerMap = readLecturer(lecturer_id,this.generateRequestEntity(null, null, null, query),readLecturerOperation,jedis);
        //判断当前用户是否是讲师
        if(MiscUtils.isEmpty(lecturerMap)){
            throw new QNLiveException("100001");
        }


        if(reqMap.get("good_type").toString().equals("0")){
            //课程之间需要间隔三十分钟
            Long startTime = (Long)reqMap.get("start_time");
            String lecturerCoursesAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, query);
            long startIndex = startTime-30*60*1000;
            long endIndex = startTime+30*60*1000;
            long start = MiscUtils.convertInfoToPostion(startIndex , 0L);
            long end = MiscUtils.convertInfoToPostion(endIndex , 0L);
            Set<String> aLong = jedis.zrangeByScore(lecturerCoursesAllKey, start, end);
            for(String course_id : aLong){
                Map<String,Object> map = new HashMap<>();
                map.put("course_id",course_id);
                Map<String, String> course = readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);
                if(course.get("status").equals("1")){
                    throw new QNLiveException("100029");
                }
            }
        }




        return null;
    }

}
