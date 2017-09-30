package qingning.shop.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import qingning.common.dj.HttpClientUtil;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.IShopModuleServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

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

    /**开通店铺
     * @param reqEntity
     * @return
     * @throws Exception
     */
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
        int i = shopModuleServer.insertShop(shop);
        if(i>0){
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
            this.readShop(shopId, false,jedis);
            resultMap.put("shop_id", shopId);
        }
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
        Map<String,String> shop = this.readCurrentUserShop(reqEntity, jedis);
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
        return shop;
    }

    /**
     * PC_店铺-店铺设置
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopEdit")
    public void shopEdit(RequestEntity reqEntity) throws Exception{
        Map<String,Object> param = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String, String> shopInfo = this.readCurrentUserShop(reqEntity, jedis);
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_SHOP_FIELD, shopInfo.get("shop_id"));
        param.put("shop_id",shopInfo.get("shop_id"));
        String shopKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SHOP, map);
        if(shopModuleServer.updateShop(param)>0){
            //清空店铺缓存
            jedis.del(shopKey);
        }
    }

    /**
     * PC_店铺-获取扫码URL
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("wechatLogin")
    public Map<String, Object>  wechatLogin(RequestEntity reqEntity) throws Exception{
        //重定向微信URL
        String requestUrl = WeiXinUtil.getWechatRqcodeLoginUrl();
        Map<String, Object> result = new HashMap<>();
        result.put("redirectUrl", requestUrl);
        return result;
    }
    /**
     * PC_店铺-轮播图列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopBannerList")
    public Map<String, Object>  shopBannerList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String shopId = getAccessInfoByToken(reqEntity.getAccessToken(),Constants.CACHED_KEY_SHOP_FIELD,jedisUtils.getJedis());
        reqMap.put("shop_id",shopId);
        Map<String, Object> result = shopModuleServer.getShopBannerList(reqMap);
        return result;
    }
    /**
     * PC_店铺-添加轮播图
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopBannerAdd")
    public void addBanner(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,String> shopInfo = this.readCurrentUserShop(reqEntity,jedisUtils.getJedis());
        String shopId = shopInfo.get("shop_id").toString();
        String linkType = reqMap.get("link_type").toString();
        String linkTo = MiscUtils.getLinkTo(reqMap,linkType);
        reqMap.put("link_type",linkType);
        reqMap.put("link_to",linkTo);

        reqMap.put("create_time",new Date());
        //默认下架
        reqMap.put("status","2");
        reqMap.put("banner_id",MiscUtils.getUUId());
        reqMap.put("shop_id",shopId);
        //添加到数据库
        shopModuleServer.addShopBanner(reqMap);
    }
    /**
     * PC_店铺-轮播图编辑
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopBannerEdit")
    public void shopBannerEdit(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        String shopId = getAccessInfoByToken(reqEntity.getAccessToken(),Constants.CACHED_KEY_SHOP_FIELD,jedisUtils.getJedis());
        //根据类型获取课程链接
        String linkType = reqMap.get("link_type").toString();
        if(linkType!=null){
            String linkTo = MiscUtils.getLinkTo(reqMap,linkType);
            reqMap.put("link_to",linkTo);
        }
        int bannerMaxSize = 3;
        if(systemConfigStringMap.get("shopBannerSize")!=null){
            bannerMaxSize = Integer.valueOf(systemConfigStringMap.get("shopBannerSize"));
        }
        reqMap.put("shopBannerSize",bannerMaxSize);
        reqMap.put("shop_id",shopId);
        if(shopModuleServer.updateBanner(reqMap)==0){
            throw new QNLiveException("210007");
        }
    }
    /**
     * PC_店铺-轮播图详情
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopBannerInfo")
    public Map<String, Object>  shopBannerInfo(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String bannerId = reqMap.get("banner_id").toString();
        Map<String, Object> result = shopModuleServer.getShopBannerInfo(bannerId);
        return result;
    }

    /**
     * PC_店铺-轮播图上下架
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopBannerUpdown")
    public void  shopBannerUpdown(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        reqMap.put("status",reqMap.get("type"));
        String shopId = getAccessInfoByToken(reqEntity.getAccessToken(),Constants.CACHED_KEY_SHOP_FIELD,jedisUtils.getJedis());
        reqMap.put("shop_id",shopId);
        //读取系统配置
        int bannerMaxSize = 3;
        if(systemConfigStringMap.get("shopBannerSize")!=null){
            bannerMaxSize = Integer.valueOf(systemConfigStringMap.get("shopBannerSize"));
        }
        reqMap.put("shopBannerSize",bannerMaxSize);
        int i = shopModuleServer.updateBanner(reqMap);
        if(i==0){
            throw new QNLiveException("210007");
        }
    }
    /**
     * PC_店铺-单品-添加(1语音 2视频 3图文)
     * @param reqEntity
     * @throws Exception
     */
    @FunctionName("addSingleGoods")
    public void  addSingleGoods(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        reqMap.put("user_id",userId);
        //店铺信息
        Map<String, String> shopInfo = readCurrentUserShop(reqEntity, jedis);
        String coursePrice = reqMap.get("price").toString();
        boolean courseUp = "1".equals(reqMap.get("course_updown").toString());

        String shopId = shopInfo.get("shop_id");
        reqMap.put("course_id",MiscUtils.getUUId());
        reqMap.put("shop_id",shopId);
        reqMap.put("lecturer_id",userId);
        Date now = new Date();
        reqMap.put("course_price",coursePrice);
        //0:公开课程 1:加密课程 2:收费课程
        reqMap.put("course_type","0".equals(coursePrice)?"0":"2");
        reqMap.put("goods_type",reqMap.get("type"));
        reqMap.put("course_duration",reqMap.get("course_duration"));
        //默认下列下架
        reqMap.put("series_course_updown","0");
        //首次上架时间
        if(courseUp)reqMap.put("first_up_time",now);

        reqMap.put("create_time",now);
        reqMap.put("create_date",now);

        //收益初始化
        reqMap.put("extra_amount",0);
        reqMap.put("extra_num",0);
        reqMap.put("sale_num",0);
        reqMap.put("course_amount",0);

        if(!MiscUtils.isEmpty(reqMap.get("buy_tips"))){
            reqMap.put("buy_tips",reqMap.get("buy_tips"));
        }
        if(!MiscUtils.isEmpty(reqMap.get("target_user"))){
            reqMap.put("target_user",reqMap.get("target_user"));
        }
        //插入课程
        shopModuleServer.addSingleCourse(reqMap);
        //缓存加入讲师创建的课程
        jedis.zadd(Constants.CACHED_KEY_COURSE_SAAS,now.getTime(),reqMap.get("course_id").toString());

        //更新店铺已上架的课程
        Map<String,Object> query = new HashMap<>();
        query.put("lecturer_id",userId);
        String upCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_COURSES_NOT_LIVE_UP, query);
        jedis.zadd(upCourseKey,now.getTime(),reqMap.get("course_id").toString());
    }

    /**
     * PC_店铺-系列-添加子课程
     * @param reqEntity
     * @throws Exception
     */
    @FunctionName("addSeriesCourseChild")
    public void  addSeriesCourseChild(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String seriesId = (String) reqMap.get("series_id");
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        reqMap.put("user_id",userId);
        /*
         * 获取系列的详情
         */
        Map<String, Object> readSeriesMap = new HashMap<>();
        readSeriesMap.put("series_id", seriesId);
        jedis.del(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES,readSeriesMap));
        Map<String, String> seriesInfoMap = this.readSeries(seriesId, this.generateRequestEntity(null,null,null,readSeriesMap), readSeriesOperation,jedis, true);
        if(MiscUtils.isEmpty(seriesInfoMap)){
            log.error("saas_店铺-系列-添加课程>>>>系列课程不存在");
            throw new QNLiveException("210004");
        }
        /*
         * 店铺信息
         */
        Map<String, String> shopInfo = readCurrentUserShop(reqEntity, jedis);

        String shopId = shopInfo.get("shop_id");
        String courseId = MiscUtils.getUUId();
        reqMap.put("course_id", courseId);
        reqMap.put("shop_id",shopId);
        reqMap.put("lecturer_id",userId);
        Date now = new Date();
        reqMap.put("create_time",now);
        reqMap.put("create_date",now);
        reqMap.put("course_price",reqMap.get("price"));
        //收益初始化
        reqMap.put("extra_amount",0);
        reqMap.put("extra_num",0);
        reqMap.put("series_or_course",0);
        reqMap.put("sale_num",0);
        reqMap.put("goods_type",reqMap.get("type"));
        reqMap.put("course_amount",0);
        //默认上架
        reqMap.put("course_updown","0");
        reqMap.put("course_duration",reqMap.get("course_duration"));
        //默认上架
        reqMap.put("series_course_updown","1");
        if(!MiscUtils.isEmpty(reqMap.get("updown"))){
            if(reqMap.get("updown").toString().equals("2")){
                reqMap.put("series_course_updown","2");
            }
        }
        if(!MiscUtils.isEmpty(reqMap.get("target_user"))){
            reqMap.put("target_user",reqMap.get("target_user"));
        }
        //插入课程
        shopModuleServer.addSingleCourse(reqMap);
        log.info("saas_店铺-系列-添加课程>>>>数据库插入子课程成功");

        /*
         * 更新缓存中系列已经上架的子课zset
         */
        //系列课已上架子课的zset列表
        String readSeriesUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_UP, seriesInfoMap);
        //新增到上架的缓存
        jedis.zadd(readSeriesUpKey,System.currentTimeMillis(), courseId);

        /*
         * 缓存加入讲师创建的课程
         */
        String readLecturerSaasCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_SAAS, seriesInfoMap);
        jedis.zadd(readLecturerSaasCourseKey, now.getTime(),courseId);

        /*
         * 更新系列课已更新课程数量（数据库、缓存）
         */
        Map<String, Object> updateSeriesMap = new HashMap<>();
        updateSeriesMap.put("series_id", seriesId);
        updateSeriesMap.put("course_num",
                Integer.parseInt(seriesInfoMap.get("course_num")) + 1);	//已更新的课程数量执行+1操作
        updateSeriesMap.put("update_course_time", now);
        shopModuleServer.updateSeriesByMap(updateSeriesMap);
        //更新缓存中系列的详情
        String readSeriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, seriesInfoMap);
        jedis.hincrBy(readSeriesKey, "course_num", 1);

        /*
         * 更新缓存中讲师所有上架系列的zset，目的是将该系列在zset中重新排序，根据子课最新的更新时间进行排序
         */
        if("1".equals(seriesInfoMap.get("updown").toString())){	//该系列为已上架，才需要更新讲师所有上架的系列
            //获取子课程更新时间
            String lecturerSeriesUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_UP, seriesInfoMap);//讲师所有上架系列
            long seriesScore = now.getTime();
            seriesScore = MiscUtils.convertLongByDesc(seriesScore);	//实现指定时间越大，返回值越小
            jedis.zadd(lecturerSeriesUpKey, seriesScore, seriesId);
        }

        if( !MiscUtils.isEmpty(seriesInfoMap.get("shelves_sharing")) && seriesInfoMap.get("shelves_sharing").equals("1")){
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("version", "1.2.0");
            headerMap.put("Content-Type", "application/json;charset=UTF-8");
            headerMap.put("access_token",reqEntity.getAccessToken() );

            Map<String,Object> map = new HashMap<>();
            map.put("course_id",courseId);
            Map<String, String> courseInfoMap = readCourse(courseId, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("course_id",seriesId);
            courseMap.put("s_course_id",courseInfoMap.get("course_id"));
            courseMap.put("course_title",courseInfoMap.get("course_title"));
            courseMap.put("course_url",courseInfoMap.get("course_image"));
            courseMap.put("course_duration",courseInfoMap.get("course_duration"));
            courseMap.put("course_remark",MiscUtils.isEmpty(courseInfoMap.get("course_remark")));
            courseMap.put("file_path",courseInfoMap.get("course_url"));
            courseMap.put("status",Integer.parseInt(courseInfoMap.get("series_course_updown"))-1);
            String getUrl = MiscUtils.getConfigByKey("sharing_api_url")/*"http://192.168.1.197:8088"*/
                    +SharingConstants.SHARING_SERVER_COURSE
                    +SharingConstants.SHARING_COURSE_SYNCHRONIZATION_SERIES_CHILD;
            String result = HttpClientUtil.doPostUrl(getUrl, headerMap, courseMap, "UTF-8");
        }
    }
    /**
     * PC_店铺-单品-编辑
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("editSingleGoods")
    public void editSingleGoods(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        if(reqMap.size()==1){
            throw new QNLiveException("000100");
        }
        Date now = new Date();
        reqMap.put("update_time",now);
        reqMap.put("course_price",reqMap.get("price"));
        //更新缓存前操作
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String,String> query = new HashMap<>();
        query.clear();
        String courseId = reqMap.get("course_id").toString();

        reqMap.put(Constants.CACHED_KEY_COURSE_FIELD,courseId);
        query.put(Constants.CACHED_KEY_COURSE_FIELD,courseId);
        //更新缓存
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, query);
        jedis.del(courseKey);

        //编辑课程
        shopModuleServer.updateCourse(reqMap);
        Map<String, String> course = readCourse(courseId, generateRequestEntity(null,null,null,reqMap), readCourseOperation, jedis, true);
        Map<String,Object> requestMap = new HashMap<>();
        if(course.get("goods_type").equals("0") || course.get("goods_type").equals("3")){
            throw new QNLiveException("310003");
        }

        if(course.get("shelves_sharing").equals("1")){
            Map<String, String> headerParams = new HashMap<>();
            headerParams.put("version", "1.2.0");
            headerParams.put("Content-Type", "application/json;charset=UTF-8");
            headerParams.put("access_token",reqEntity.getAccessToken() );

            requestMap.put("course_id",course.get("course_id"));
            requestMap.put("course_title",course.get("course_title"));
            requestMap.put("course_url",course.get("course_image"));
            if((course.get("goods_type")).equals("1")){
                requestMap.put("course_type","0");
            }else if((course.get("goods_type")).equals("2")){
                requestMap.put("course_type","1");
            }

            requestMap.put("classify_id",course.get("classify_id"));
            requestMap.put("course_remark",course.get("course_remark"));
            requestMap.put("course_price",course.get("course_price"));
            requestMap.put("file_path",course.get("course_url"));
            requestMap.put("status",Integer.parseInt(course.get("course_updown"))-1);
            requestMap.put("course_duration",MiscUtils.isEmpty(course.get("course_duration")));
            requestMap.put("target_user",MiscUtils.isEmpty(course.get("target_user"))?"":course.get("target_user").toString());
            requestMap.put("buy_tips",MiscUtils.isEmpty(course.get("buy_tips"))?"":course.get("target_user").toString());

            //     String getUrl = MiscUtils.getConfigByKey("sharing_api_url", Constants.HEADER_APP_NAME)
            String getUrl =MiscUtils.getConfigByKey("sharing_api_url") /*"http://192.168.1.197:8088"*/
                    +SharingConstants.SHARING_SERVER_COURSE
                    +SharingConstants.SHARING_COURSE_SYNCHRONIZATION_ADD;
            String result = HttpClientUtil.doPostUrl(getUrl, headerParams, requestMap, "UTF-8");
            Map<String, Object> resultMap = JSON.parseObject(result, new TypeReference<Map<String, Object>>() {});
        }else if(!course.get("series_course_updown").equals("0")){
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("version", "1.2.0");
            headerMap.put("Content-Type", "application/json;charset=UTF-8");
            headerMap.put("access_token",reqEntity.getAccessToken() );

            Map<String,Object> map = new HashMap<>();
            map.put("course_id",courseId);
            Map<String, String> courseInfoMap = readCourse(courseId, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("course_id",course.get("series_id"));
            courseMap.put("s_course_id",courseInfoMap.get("course_id"));
            courseMap.put("course_title",courseInfoMap.get("course_title"));
            courseMap.put("course_url",courseInfoMap.get("course_image"));
            courseMap.put("course_duration",courseInfoMap.get("course_duration"));
            courseMap.put("course_remark",MiscUtils.isEmpty(courseInfoMap.get("course_remark")));
            courseMap.put("file_path",courseInfoMap.get("course_url"));
            courseMap.put("status",Integer.parseInt(courseInfoMap.get("series_course_updown"))-1);
            String getUrl = MiscUtils.getConfigByKey("sharing_api_url")/*"http://192.168.1.197:8088"*/
                    +SharingConstants.SHARING_SERVER_COURSE
                    +SharingConstants.SHARING_COURSE_SYNCHRONIZATION_SERIES_CHILD;
            String result = HttpClientUtil.doPostUrl(getUrl, headerMap, courseMap, "UTF-8");
        }

    }
    /**
     * PC_店铺-用户列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getUserList")
    public Map<String, Object> getUserList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        //获取店铺信息
        Map<String,String> shopInfo = readCurrentUserShop(reqEntity, jedis);//saaSModuleServer.getShopInfo(param);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        //TODO 付费用户存入缓存   其他普通和所有放入数据库
        Map<String, Object> userList = shopModuleServer.getShopUsers(reqMap);
        return userList;
    }

    /**
     * PC_店铺-消息列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getMessageList")
    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        //获取店铺信息
        Map<String,String> shopInfo = readCurrentUserShop(reqEntity, jedis);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        //获取所有课程评论列表
        Map<String, Object> userList = shopModuleServer.getCourseComment(reqMap);
        return userList;
    }
    /**
     * PC_店铺-用户反馈列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getFeedbackList")
    public Map<String, Object> getFeedbackList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        //获取店铺信息
        Map<String,String> shopInfo = readCurrentUserShop(reqEntity, jedis);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        Map<String, Object> userList = shopModuleServer.getUserFeedBack(reqMap);
        return userList;
    }
    /**
     * PC_店铺-单品上下架
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("goodsSingleUpdown")
    public void  goodsSingleUpdown(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String type = reqMap.get("course_updown").toString();
        String courseId = reqMap.get("course_id").toString();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Date now = new Date();
        long timestamp = now.getTime();
        //该用户ID
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        //该讲师所有上架的单品ID
        String lecturerSingleSetKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_COURSES_NOT_LIVE_UP, keyMap);
        if("1".equals(type)){
            //上架
            jedis.zadd(lecturerSingleSetKey,timestamp,courseId);
        }else{
            //下架
            jedis.zrem(lecturerSingleSetKey,courseId);
        }
        //更新时间
        reqMap.put("update_time",now);
        reqMap.put("updown_time",now);
        shopModuleServer.updateCourse(reqMap);
    }

    /**
     * PC_店铺-单品列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getSingleList")
    public Map<String, Object> getSingleList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,String> shopInfo = readCurrentUserShop(reqEntity, jedis);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        return shopModuleServer.getSingleList(reqMap);
    }
    /**
     * PC_店铺-系列列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getSeriesList")
    public Map<String, Object> getSeriesList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //获取所有系列列表
        Map<String, Object> userList = shopModuleServer.getSeriesListByLecturerId(reqMap);
        return userList;
    }

    /**
     * PC_店铺-系列-详情
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getSeriesInfo")
    public Map<String, String> getSeriesInfo(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, String> resultMap = null;
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_SERIES_FIELD, reqMap.get("series_id").toString());
        String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
        //1.先从缓存中查询课程详情，如果有则从缓存中读取课程详情
        if (jedis.exists(seriesKey)) {
            resultMap = jedis.hgetAll(seriesKey);
        } else {
            //2.如果缓存中没有课程详情，则读取数据库
            Map<String, Object> seriesDBMap = shopModuleServer.findSeriesBySeriesId(reqMap.get("series_id").toString());
            if(seriesDBMap==null){
                throw new QNLiveException("210004");
            }
            MiscUtils.converObjectMapToStringMap(seriesDBMap, resultMap);
        }
        return resultMap;
    }

    /**
     * PC_店铺-系列-详情
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getSeriesCourseList")
    public Map<String, Object> getSeriesCourseList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_SERIES_FIELD, reqMap.get("series_id").toString());
        String seriesId = reqMap.get("series_id").toString();
        //系列详情
        Map<String,String> seriesInfo = readSeries(seriesId,this.generateRequestEntity(null,null,null,reqMap), readSeriesOperation,jedis,true);
        //系列类型，直播-商品
        reqMap.put("series_course_type",seriesInfo.get("series_course_type"));
        return shopModuleServer.getSeriesChildCourseList(reqMap);
    }
    /**
     * PC_店铺-轮播图-获取课程列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getBannerCourseList")
    public Map<String, Object> getBannerCourseList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String keyword = reqMap.get("keyword").toString();
        if(StringUtils.isEmpty(keyword)){
            reqMap.remove("keyword");
        }
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        reqMap.put("user_id",userId);
        //店铺信息
        Map<String, String> shopInfo = readCurrentUserShop(reqEntity, jedis);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        String type = reqMap.get("type").toString();
        Map<String,Object> result = null;
        if("1".equals(type)){
            //单品课程
            reqMap.put("not_live","1");
            result = shopModuleServer.findBannerUpCourseList(reqMap);
        }else if("2".equals(type)){
            //系列课
            result = shopModuleServer.findBannerUpSeriesList(reqMap);
        }else if("4".equals(type)){
            //直播课
            reqMap.put("live","1");
            result = shopModuleServer.findBannerUpCourseList(reqMap);
        }
        return result;
    }
    /**
     * PC_店铺-用户-余额明细
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("userGains")
    public Map<String, Object> userGains(RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> userGains = shopModuleServer.findUserGainsByUserId(userId);
        long todayMoney = 0L;
        long todayVisit = 0L;
        long todayPay = 0L;
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String,Object> query = new HashMap<>();
        query.put("user_id", userId);
        String userCountKey = MiscUtils.getKeyOfCachedData(Constants.SHOP_DAY_COUNT, query);
        if(jedis.exists(userCountKey)){
            String amount = jedis.hget(userCountKey,"day_amount");
            String visit = jedis.hget(userCountKey,"day_visit");
            String dayPayUser = jedis.hget(userCountKey,"day_pay_user");
            if(StringUtils.isNotEmpty(amount)){
                todayMoney = Long.valueOf(amount);
            }
            if(StringUtils.isNotEmpty(visit)){
                todayVisit = Long.valueOf(visit);
            }
            if(StringUtils.isNotEmpty(dayPayUser)){
                todayPay = Long.valueOf(dayPayUser);
            }
        }
        userGains.put("today_amount",todayMoney);
        userGains.put("today_visit",todayVisit);
        userGains.put("today_pay",todayPay);

        Long user_total_real_incomes = Long.valueOf(userGains.get("user_total_real_incomes").toString());
        Long distributer_real_incomes = Long.valueOf(userGains.get("distributer_real_incomes").toString());
        userGains.put("user_total_amount",user_total_real_incomes-distributer_real_incomes);

        if(MiscUtils.isEmpty(userGains)){
            throw new QNLiveException("170001");
        }
        return userGains;
    }
    /**
     * PC_店铺-订单记录
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("gainsOrdersList")
    public Map<String, Object> gainsOrdersList(RequestEntity reqEntity) throws Exception{
        Map<String,Object> query =  (Map<String, Object>)reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> resultMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        //店主ID
        query.put("lecturer_id",userId);
        Map<String,Object> records = shopModuleServer.getOrdersList(query);

        resultMap.put("total_count",records.get("total_count"));
        resultMap.put("total_page",records.get("total_page"));

        List<Map<String,Object>> list = (List<Map<String,Object>>)records.get("list");
        for(Map<String,Object> recordMap : list){
            String course_id =  recordMap.get("course_id").toString();
            Map<String ,Object> queryMap = new HashMap<>();

            Map<String, String> courseMap;
            if("2".equals(recordMap.get("profit_type")+"")){
                queryMap.put("series_id",course_id);
                courseMap = readSeries(course_id, this.generateRequestEntity(null,null,null,queryMap), readSeriesOperation, jedis, true);
            }else{
                queryMap.put("course_id",course_id);
                courseMap = readCourse(course_id, this.generateRequestEntity(null, null, null, queryMap), readCourseOperation, jedis, true);//从缓存中读取课程信息
            }
            if("2".equals(recordMap.get("profit_type")+"")){
                recordMap.put("goods_name", courseMap.get("series_title"));
                recordMap.put("price",  courseMap.get("series_price"));
                recordMap.put("create_time", courseMap.get("create_time"));
            }else{
                recordMap.put("goods_name", courseMap.get("course_title"));
                recordMap.put("price",  courseMap.get("course_price"));
                recordMap.put("create_time", courseMap.get("create_time"));
            }


        }
        resultMap.put("list", list);

        return resultMap;
    }
    /**
     * PC_店铺-上架到知享
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shelvesQNSharing")
    public Map<String, Object>  shelvesQNSharing(RequestEntity reqEntity) throws Exception{
        Map<String,Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();
        String shelves_type = reqMap.get("shelves_type").toString();
        Map<String,Object> queryMap = new HashMap<>();

        Map<String,String> headerParams = new HashMap<>();
        headerParams.put("version", Constants.SYS_QN_SHARING_VERSION);
        headerParams.put("Content-Type",Constants.SYS_QN_SHARING_CONTENT_TYPE);
        headerParams.put("access_token",reqEntity.getAccessToken());

        Map<String,Object> requestMap = new HashMap<>();


        if(!MiscUtils.isEmpty(reqMap.get("classify_id"))){
            queryMap.put("classify_id",reqMap.get("classify_id"));
        }
        queryMap.put("shelves_sharing","1");
        if(shelves_type.equals("0")){//课程
            queryMap.put("course_id",reqMap.get("shelves_id"));
            RequestEntity entity = new RequestEntity();
            entity.setFunctionName("findSaasCourseByCourseId");
            entity.setParam(reqMap);

            Map<String, String> saasCourse = readCourse(reqMap.get("shelves_id").toString(), generateRequestEntity(null, null, null, queryMap), readCourseOperation, jedis, true);
            if(!saasCourse.get("lecturer_id").equals(userId)){
                throw new QNLiveException("310007");
            }
            if(saasCourse.get("goods_type").equals("0") || saasCourse.get("goods_type").equals("3")){
                throw new QNLiveException("310003");
            }


            requestMap.put("course_id",saasCourse.get("course_id"));
            requestMap.put("course_title",saasCourse.get("course_title"));
            requestMap.put("course_url",saasCourse.get("course_image"));
            if((saasCourse.get("goods_type")).equals("1")){
                requestMap.put("course_type","0");
            }else if((saasCourse.get("goods_type")).equals("2")){
                requestMap.put("course_type","1");
            }
            if(!MiscUtils.isEmpty(reqMap.get("classify_id"))){
                requestMap.put("classify_id",reqMap.get("classify_id"));
            }else if(!MiscUtils.isEmpty(saasCourse.get("classify_id"))){
                requestMap.put("classify_id",saasCourse.get("classify_id"));
            }else{
                requestMap.put("classify_id",9);
            }

            requestMap.put("course_remark",saasCourse.get("course_remark"));
            requestMap.put("course_price",saasCourse.get("course_price"));
            requestMap.put("file_path",saasCourse.get("course_url"));
            requestMap.put("status",Integer.parseInt(saasCourse.get("course_updown"))-1);
            if(MiscUtils.isEmpty(saasCourse.get("course_duration"))){
                requestMap.put("course_duration",0);
            }else{
                requestMap.put("course_duration",saasCourse.get("course_duration"));
            }
            requestMap.put("target_user",saasCourse.get("target_user"));
            requestMap.put("buy_tips",saasCourse.get("buy_tips"));

            String getUrl = MiscUtils.getConfigByKey("sharing_api_url")
                    +SharingConstants.SHARING_SERVER_COURSE
                    +SharingConstants.SHARING_COURSE_SYNCHRONIZATION_ADD;
            String result = HttpClientUtil.doPostUrl(getUrl, headerParams, requestMap, "UTF-8");
            Map<String, Object> resMap = JSON.parseObject(result, new TypeReference<Map<String, Object>>() {});
            if (resMap.get("code").equals("0")) {
                resultMap.put("synchronization",result);
            }else {
                throw new QNLiveException("310008");
            }
            shopModuleServer.updateCourse(queryMap);


            jedis.del( MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE,queryMap));

            readCourse(reqMap.get("shelves_id").toString(),generateRequestEntity(null, null, null, queryMap),readCourseOperation, jedis, true);
        }else if(shelves_type.equals("1")){//系列
            queryMap.put("series_id",reqMap.get("shelves_id"));
            requestMap.put("type","add_series");
            Map<String, String> seriesInfoMap = readSeries(reqMap.get("shelves_id").toString(), this.generateRequestEntity(null,null,null,queryMap), readSeriesOperation, jedis, true);
            if(!seriesInfoMap.get("lecturer_id").equals(userId)){
                throw new QNLiveException("310007");
            }
            if(seriesInfoMap.get("series_course_type").equals("0") || seriesInfoMap.get("series_course_type").equals("3")){
                throw new QNLiveException("310003");
            }

            requestMap.put("course_id",seriesInfoMap.get("series_id"));
            requestMap.put("course_title",seriesInfoMap.get("series_title"));
            requestMap.put("course_url",seriesInfoMap.get("series_img"));
            if(seriesInfoMap.get("series_course_type").equals("1")){
                requestMap.put("course_type","0");
            }else if(seriesInfoMap.get("series_course_type").equals("2")){
                requestMap.put("course_type","1");
            }
            requestMap.put("update_type","2");
            requestMap.put("cycle",seriesInfoMap.get("update_plan"));
            requestMap.put("update_status",seriesInfoMap.get("series_status"));
            requestMap.put("updated_course_num",seriesInfoMap.get("course_num"));

            if(!MiscUtils.isEmpty(reqMap.get("classify_id"))){
                requestMap.put("classify_id",reqMap.get("classify_id"));
            }else if(!MiscUtils.isEmpty(seriesInfoMap.get("classify_id"))){
                requestMap.put("classify_id",seriesInfoMap.get("classify_id"));
            }else{
                requestMap.put("classify_id",9);
            }

            requestMap.put("course_remark",seriesInfoMap.get("series_remark"));
            requestMap.put("target_user",seriesInfoMap.get("target_user"));
            requestMap.put("course_price",seriesInfoMap.get("series_price"));
            requestMap.put("buy_tips",seriesInfoMap.get("series_pay_remark"));
            requestMap.put("status",Integer.parseInt(seriesInfoMap.get("updown"))-1);
            List<Map<String, Object>> seriesCourseList = shopModuleServer.findCourseBySeriesId(seriesInfoMap.get("series_id"));
            if(MiscUtils.isEmpty(seriesCourseList)){
                throw new QNLiveException("310009");
            }

            List<Map<String, Object>> requestCourseList = new ArrayList<>();
            for(Map<String, Object> course:seriesCourseList ){
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("course_id",seriesInfoMap.get("series_id"));
                courseMap.put("s_course_id",course.get("course_id"));
                courseMap.put("series_id",course.get("series_id"));
                courseMap.put("course_title",course.get("course_title"));
                courseMap.put("course_url",course.get("course_image"));
                courseMap.put("course_duration",course.get("course_duration"));
                courseMap.put("course_remark",course.get("course_remark"));
                courseMap.put("file_path",course.get("course_url"));
                courseMap.put("status",Integer.parseInt(course.get("series_course_updown").toString())-1);
                requestCourseList.add(courseMap);
            }
            requestMap.put("list",requestCourseList);

            String getUrl = MiscUtils.getConfigByKey("sharing_api_url")
                    +SharingConstants.SHARING_SERVER_COURSE
                    +SharingConstants.SHARING_COURSE_SYNCHRONIZATION_SERIES_ADD;
            String result = HttpClientUtil.doPostUrl(getUrl, headerParams, requestMap, "UTF-8");
            Map<String, String> resMap = JSON.parseObject(result, new TypeReference<Map<String, String>>() {});
            if (resMap.get("code").equals("0")) {
                resultMap.put("synchronization",result);
                shopModuleServer.updateSeriesByMap(queryMap);
            }else {
                throw new QNLiveException("310008");
            }
            String keyOfCachedData = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, queryMap);
            jedis.del(keyOfCachedData);
            readSeries(reqMap.get("shelves_id").toString(), this.generateRequestEntity(null,null,null,queryMap), readSeriesOperation, jedis, true);

        }
        return resultMap;
    }

    /**
     * PC_店铺-获取店铺
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getShops")
    public Map<String, Object>  getShops(RequestEntity reqEntity) throws Exception{
        Map<String,Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        return shopModuleServer.getShopInfoList(reqMap);
    }
    /**
     * PC_店铺-获取讲师详情
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("lecturerInfo")
    public Map<String, Object>  lecturerInfo(RequestEntity reqEntity) throws Exception{
        Map<String,Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String lecturerId = reqMap.get("lecturer_id").toString();
        Map<String,Object> userInfo = shopModuleServer.getLecturerImcome(lecturerId);
        if(userInfo==null)
            throw new QNLiveException("120002");
        return userInfo;
    }
    /**
     * PC_店铺-开通知享
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("sharingOpen")
    public Map<String, Object>  sharingOpen(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        reqMap.put("user_id", userId);
        Map<String, String> shopInfo = readCurrentUserShop(reqEntity, jedis);
        if (shopInfo.get("open_sharing").equals("1")) {
            throw new QNLiveException("310005");
        }
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_SHOP_FIELD, shopInfo.get("shop_id"));
        //清空店铺缓存
        String shopKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SHOP, map);
        jedis.del(shopKey);

        Map<String, Object> param = new HashMap<>();
        param.put("user_id", userId);
        param.put("open_sharing", 1);
        shopModuleServer.updateShop(param);

        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("version", "1.2.0");
        headerMap.put("Content-Type", "application/json;charset=UTF-8");
        headerMap.put("access_token", reqEntity.getAccessToken());

        map.clear();
        map.put("avatar_address", shopInfo.get("shop_logo"));
        map.put("lecturer_name", shopInfo.get("user_name"));
        map.put("lecturer_title", shopInfo.get("lecturer_title"));
        map.put("lecturer_remark", shopInfo.get("shop_remark"));
        map.put("open_code", reqMap.get("open_code"));
        //获取知享课程数
        String getUrl = MiscUtils.getConfigByKey("sharing_api_url")
                + SharingConstants.SHARING_SERVER_USER_COMMON
                + SharingConstants.SHARING_USER_COMMON_SHARING_OPEN;
        String result = HttpClientUtil.doPostUrl(getUrl, headerMap, map, "UTF-8");
        Map<String, String> resultMap = JSON.parseObject(result, new TypeReference<Map<String, String>>() {
        });
        if (resultMap.get("code").equals("0")) {
            map.clear();
            map.put("synchronization", result);
            return map;
        } else if(resultMap.get("code").equals("120035")){
            throw new QNLiveException("310005");
        }else {
            param.put("open_sharing", 0);
            shopModuleServer.updateShop(param);
            throw new QNLiveException(result);
        }
    }
    /**
     * PC_店铺-删除店铺轮播图
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("bannerRemove")
    public Map<String, Object>  bannerRemove(RequestEntity reqEntity) throws Exception{
        Map<String,Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String banner_id = reqMap.get("banner_id").toString();
        String shopId = getAccessInfoByToken(reqEntity.getAccessToken(),Constants.CACHED_KEY_SHOP_FIELD,jedisUtils.getJedis());
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put("shop_id",shopId);
        queryMap.put("banner_id",banner_id);
        shopModuleServer.deleteBanner(queryMap);
        return null;
    }

    /**
     * H5_店铺-获取店铺轮播列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("queryShopBannerList")
    public Map<String, Object>  queryShopBannerList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //添加状态要求：上架
        reqMap.put("status", "1");
        List<Map<String, Object>> bannerList = shopModuleServer.getShopBannerListForFront(reqMap);
        resultMap.put("banner_info_list", bannerList);
        return resultMap;
    }

    /**
     * H5_店铺-获取店铺系列课程列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findShopSeriesList")
    public Map<String, Object> findShopSeriesList(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, String>> courseInfoList = new ArrayList<>();
        /*
         * 获取请求参数
         */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String loginedUserId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
//        reqMap.put("user_id",userId);
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取请求中的上一页最后一门课程id
        String lastCourseId = (String) reqMap.get("last_course_id");
        //获取请求中的课程分类id
        String classifyId = (String) reqMap.get("classify_id");
        //获取请求中的课程内容goods_type
        String goodsType = (String) reqMap.get("goods_type");
        //获取请求中的是否直播课标识
        String isLive = (String) reqMap.get("is_live");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        //获取当前服务器时间
        Date now = new Date();
        //读取缓存必备参数
        Map<String, Object> readCacheMap = new HashMap<>();
        RequestEntity readCacheReqEntity = this.generateRequestEntity(null, null, null, readCacheMap);

        /*
         * 获取上一页最后一条课程详情，用于获取分页起始score
         */
        readCacheMap.clear();
        readCacheMap.put(Constants.CACHED_KEY_SERIES_FIELD, lastCourseId);
        readCacheReqEntity.setFunctionName(null);
        Map<String, String> lastCourseInfo = readSeries(lastCourseId, readCacheReqEntity, readSeriesOperation, jedis, true); //读取series时当operation不设置functionName默认读取系列详情
        //计算课程分页的score
        long lastCourseScore = 0;
        if (!MiscUtils.isEmpty(lastCourseInfo)) {
            //TODO 计算上一门课程的排序score
            lastCourseScore = MiscUtils.convertInfoToPostion(Long.valueOf(lastCourseInfo.get(Constants.FIRST_UP_TIME)), Long.valueOf(lastCourseInfo.get("position")));//算出位置
        }

        /*
         * TODO 拼接读取课程id列表的key
         */
        readCacheMap.clear();
        readCacheMap.put(Constants.CACHED_KEY_SHOP_FIELD, shopId);
        String indexKey = null, pageKey = null;
        if ("0".equals(isLive)) {   //获取非直播课程
            if (MiscUtils.isEmpty(goodsType)) { //请求中没有传递课程内容，则不用根据goods_type进行筛选
                indexKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP, readCacheMap);
                pageKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP_PAGING, readCacheMap);
            } else {
                readCacheMap.put("goods_type", goodsType);
                indexKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP, readCacheMap);
                pageKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP_PAGING, readCacheMap);
            }
        } else if ("1".equals(isLive)) {    //获取直播课程
            readCacheMap.put("goods_type", 0);  //直播的goods_type为0
            indexKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP, readCacheMap);
            pageKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP_PAGING, readCacheMap);
        } else if (MiscUtils.isEmpty(classifyId)) { //根据分类获取课程
            //TODO
        }

        /*
         * TODO 分页读取课程id集合
         */
        readCacheMap.clear();
        readCacheMap.put("shop_id", shopId);
        List<String> shopCourseIdList;
        if (0 == lastCourseScore) { //获取首页
            shopCourseIdList = SortUtils.getList(indexKey, pageKey, pageCount, null, jedis);
        } else {    //不是获取首页
            shopCourseIdList = SortUtils.getList(indexKey, pageKey, pageCount, String.valueOf(lastCourseScore), jedis);
        }
        if (MiscUtils.isEmpty(shopCourseIdList)) {
            log.info("H5_店铺 - 获取店铺单品课程列表>>>>获取课程id列表为空");
            return resultMap;
        }

        /*
         * 根据课程id列表获取课程详情
         */
        readCacheMap.clear();
        readCacheReqEntity.setFunctionName(null);
        Map<String, String> courseInfo;
        for (String courseId : shopCourseIdList) {
            readCacheMap.put(Constants.CACHED_KEY_SERIES_FIELD, courseId);
            courseInfo = readSeries(courseId, readCacheReqEntity, readSeriesOperation, jedis, true); //读取series时当operation不设置functionName默认读取系列详情
            if (!MiscUtils.isEmpty(courseInfo)) {
                //判断是否加入了该课程
                boolean joinStatus = isJoinSeries(loginedUserId, courseId, jedis);
                if(!joinStatus){	//用户未加入
                    courseInfo.put("is_join", "0");
                }else{	//用户已购买
                    courseInfo.put("is_join", "1");
                }
                courseInfoList.add(courseInfo);
            }
        }

        resultMap.put("course_info_list", courseInfoList);
        return resultMap;
    }

    /**
     * H5_店铺 - 获取店铺单品直播课程列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findShopSingleList")
    public Map<String, Object>  findShopSingleList(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, String>> courseInfoList = new ArrayList<>();
        /*
         * 获取请求参数
         */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String loginedUserId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
//        reqMap.put("user_id",userId);
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取请求中的上一页最后一门课程id
        String lastCourseId = (String) reqMap.get("last_course_id");
        //获取请求中的课程分类id
        String classifyId = (String) reqMap.get("classify_id");
        //获取请求中的课程内容goods_type
        String goodsType = (String) reqMap.get("goods_type");
        //获取请求中的是否直播课标识
        String isLive = (String) reqMap.get("is_live");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        //获取当前服务器时间
        Date now = new Date();
        //读取缓存必备参数
        Map<String, Object> readCacheMap = new HashMap<>();
        RequestEntity readCacheReqEntity = this.generateRequestEntity(null, null, null, readCacheMap);

        /*
         * 获取上一页最后一条课程详情，用于获取分页起始score
         */
        readCacheMap.clear();
        readCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, lastCourseId);
        readCacheReqEntity.setFunctionName(null);   //读取course时当operation不设置functionName默认读取课程详情
        Map<String, String> lastCourseInfo = readCourse(lastCourseId, readCacheReqEntity, readCourseOperation, jedis, true);
        //计算课程分页的score
        long lastCourseScore = 0;
        if (!MiscUtils.isEmpty(lastCourseInfo)) {
            lastCourseScore = MiscUtils.convertInfoToPostion(Long.valueOf(lastCourseInfo.get(Constants.FIRST_UP_TIME)), Long.valueOf(lastCourseInfo.get("position")));//算出位置
        }

        /*
         * 拼接读取课程id列表的key
         */
        readCacheMap.clear();
        readCacheMap.put(Constants.CACHED_KEY_SHOP_FIELD, shopId);
        String indexKey = null, pageKey = null;
        if ("0".equals(isLive)) {   //获取非直播课程
            if (MiscUtils.isEmpty(goodsType)) { //请求中没有传递课程内容，则不用根据goods_type进行筛选
                indexKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP, readCacheMap);
                pageKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP_PAGING, readCacheMap);
            } else {
                readCacheMap.put("goods_type", goodsType);
                indexKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP, readCacheMap);
                pageKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP_PAGING, readCacheMap);
            }
        } else if ("1".equals(isLive)) {    //获取直播课程
            readCacheMap.put("goods_type", 0);  //直播的goods_type为0
            indexKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP, readCacheMap);
            pageKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP_PAGING, readCacheMap);
        } else if (MiscUtils.isEmpty(classifyId)) { //根据分类获取课程
            //TODO
        }

        /*
         * 分页读取课程id集合
         */
        List<String> shopCourseIdList = null;
        if (0 == lastCourseScore) { //获取首页
            shopCourseIdList = SortUtils.getList(indexKey, pageKey, pageCount, null, jedis);
        } else {    //不是获取首页
            shopCourseIdList = SortUtils.getList(indexKey, pageKey, pageCount, String.valueOf(lastCourseScore), jedis);
        }
        if (MiscUtils.isEmpty(shopCourseIdList)) {
            log.info("H5_店铺 - 获取店铺单品课程列表>>>>获取课程id列表为空");
            return resultMap;
        }

        /*
         * 根据课程id列表获取课程详情
         */
        readCacheMap.clear();
        readCacheReqEntity.setFunctionName(null);   //读取course时当operation不设置functionName默认读取课程详情
        Map<String, String> courseInfo;
        for (String courseId : shopCourseIdList) {
            readCacheMap.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
            courseInfo = readCourse(courseId, readCacheReqEntity, readCourseOperation, jedis, true);
            if (!MiscUtils.isEmpty(courseInfo)) {
                /*
                 * 直播课和非直播课分开处理
                 */
                if ("0".equals(courseInfo.get("goods_type"))) { //直播课
                    //进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                    MiscUtils.courseTranferState(now.getTime(), courseInfo);
                    /*
                     * 判断是否加入了课程
                     */
                    boolean isStudent = isJoinLiveCourse(loginedUserId, courseId, jedis);
                    //加入课程状态 0未加入 1已加入
                    if (isStudent) {
                        courseInfo.put("is_join", "1");
                    } else {
                        courseInfo.put("is_join", "0");
                    }
                } else {    //非直播课
                    /*
                     * 判断是否加入了课程
                     */
                    boolean isStudent = isJoinCourse(loginedUserId, courseId, jedis);
                    //加入课程状态 0未加入 1已加入
                    if(isStudent){
                        courseInfo.put("is_join", "1");
                    }else {
                        courseInfo.put("is_join", "0");
                    }
                }
                courseInfoList.add(courseInfo);
            }
        }

        resultMap.put("course_info_list", courseInfoList);
        return resultMap;
    }
    /**
     * H5_店铺-获取店铺信息
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findShopInfo")
    public Map<String, Object>  findShopInfo(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        Map<String, String> shopInfoMap = readShop(shopId, false, jedis);
        resultMap.put("shop_info", shopInfoMap);
        return resultMap;
    }
    /**
     * H5_店铺-获取店铺单品课程（直播除外）列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findShopSingleCourseList")
    public Map<String, Object>  findShopSingleCourseList(RequestEntity reqEntity) throws Exception{
    	/*
    	 * 逻辑：
    	 * 1.根据shop_id获得讲师id
    	 * 2.根据讲师id查询缓存中讲师已上架单品课，需要传递分页标识
    	 */
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, String>> singleInfoList = new ArrayList<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        //获取分页标识
        String lastSingleId = reqMap.get("last_single_id").toString();
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        /*
         * 从缓存中获取店铺信息
         */
        Map<String, String> shopMap = readShop(shopId, false,jedis);
        if(shopMap == null || shopMap.isEmpty()){
            log.error("saas店铺-获取店铺单品课程（直播除外）列表>>>>请求的店铺不存在");
            throw new QNLiveException("190001");
        }
        //获取讲师id
        String lecturerId = shopMap.get("user_id");
        //根据讲师id查询缓存中讲师已上架单品课，需要传递分页标识
        //TODO 缓存
        Set<String> singleSet = null;//readLecturerSingleNotLiveUp(lecturerId, lastSingleId, pageCount,reqEntity,readSingleListOperation,jedis);
        if(singleSet != null){
            log.info("saas店铺-获取店铺单品课程（直播除外）列表>>>>从缓存中获取到讲师的上架单品课（直播除外）");
            //生成用于缓存不存在时调用数据库的requestEntity
            Map<String, Object> readSaasCourseMap = new HashMap<>();
            RequestEntity readSaasCourseReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readSaasCourseMap);
            for(String singleId : singleSet){
                readSaasCourseMap.put("course_id", singleId);
                //获取系列课程详情
                Map<String, String> singleMap = readCourse(singleId, readSaasCourseReqEntity, readCourseOperation, jedis, true);
            	/*
                 * 判断是否加入了课程
                 */
                boolean isStudent = isJoinCourse(userId, singleId, jedis);
                //加入课程状态 0未加入 1已加入
                if(isStudent){
                    singleMap.put("is_join", "1");
                }else {
                    singleMap.put("is_join", "0");
                }
                singleInfoList.add(singleMap);
            }
        }
        resultMap.put("single_info_list", singleInfoList);
        return resultMap;
    }
    /**
     * H5_课程-获取系列课程内容课程列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findSeriesCourseList")
    public Map<String, Object>  findSeriesCourseList(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, String>> courseInfoList = new ArrayList<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //获取请求查看的series_id
        String seriesId = (String) reqMap.get("series_id");
        reqMap.put("seried_id", seriesId);
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        //获取分页标识
        String lastCourseId = reqMap.get("last_course_id").toString();
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        //获取前端传递的系列课程类型	0：直播；1：音频；2：视频；3：图文；
        String seriesType = reqMap.get("series_type").toString();

        /*
         * 根据系列id查询缓存中系列内容课程id列表，需要传递分页标识
         */
        //read系列课的内容课程列表
        Set<String> courseSet = readSeriesCourseUp(seriesId, lastCourseId, pageCount, jedis);
        if(courseSet != null){
            log.info("saas课程-获取系列课程内容课程列表>>>>从缓存中获取到系列的内容课程列表");
            String courseDetailKey = null;	//获取内容课程详情的key

            //生成用于缓存不存在时调用数据库的requestEntity
            Map<String, Object> readCourseMap = new HashMap<>();
            RequestEntity readCourseReqEntity = null;
            if("0".equals(seriesType)){	//直播类型的系列课，从t_course中查询
                log.info("saas课程-获取系列课程内容课程列表>>>>请求的系列为直播类型");
                readCourseReqEntity = this.generateRequestEntity(null, null, "findCourseByCourseId", readCourseMap);

                for(String courseId : courseSet){
                    readCourseMap.put("course_id", courseId);
                    //获取课程详情
                    Map<String, String> courseMap = readCourse(courseId, readCourseReqEntity, readCourseOperation, jedis, true);
                	/*
                	 * 对直播课程的返回字段进行重命名
                	 */
                    courseMap.put("course_image", courseMap.get("course_url").toString());
                    courseMap.put("course_url", "");	//数据库或缓存“直播课”的course_url表示封面，前面已经存在course_image，所以这里置空

                	/*
                	 * 判断是否加入课程，直播类
                	 */
                    boolean isStudent = isJoinLiveCourse(userId, courseId, jedis);
                    //加入课程状态 0未加入 1已加入
                    if(isStudent){
                        courseMap.put("is_join", "1");
                    }else {
                        courseMap.put("is_join", "0");
                    }

                    courseInfoList.add(courseMap);
                }
            }else{	//非直播类型的系列课，从t_saas_course中查询
                log.info("saas课程-获取系列课程内容课程列表>>>>请求的系列为非直播类型");
                readCourseReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readCourseMap);

                for(String courseId : courseSet){
                    readCourseMap.put("course_id", courseId);
                    //获取课程详情
                    Map<String, String> courseMap = readCourse(courseId, readCourseReqEntity, readCourseOperation, jedis, true);

                	/*
                	 * 判断是否加入课程，非直播类
                	 */
                    boolean isStudent = isJoinCourse(userId, courseId, jedis);
                    //加入课程状态 0未加入 1已加入
                    if(isStudent){
                        courseMap.put("is_join", "1");
                    }else {
                        courseMap.put("is_join", "0");
                    }

                    courseInfoList.add(courseMap);
                }
            }
        }

        resultMap.put("course_info_list", courseInfoList);
        return resultMap;
    }
    /**
     * H5_课程-获取单品课程详情
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findSingleCourseDetail")
    public Map<String, Object>  findSingleCourseDetail(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //获取请求查看的single_id
        String singleId = (String) reqMap.get("single_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();

        //生成用于缓存不存在时调用数据库的requestEntity
        Map<String, Object> readSingleMap = new HashMap<String, Object>();
        //从产品原型上看，调用该接口获取单品课程详情的来源只有非直播课程，所以直接从t_saas_course查找
        RequestEntity readSingleReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readSingleMap);
        readSingleMap.put("course_id", singleId);
        //获取课程详情
        Map<String, String> singleMap = readCourse(singleId, readSingleReqEntity, readCourseOperation, jedis, true);
        if(singleMap == null || singleMap.isEmpty()){
            log.error("saas_H5_课程-获取单品课程详情>>>>课程不存在");
            throw new QNLiveException("100004");
        }

		/*
         * 判断是否加入了课程
         */
        boolean isStudent = isJoinCourse(userId, singleId, jedis);
        //加入课程状态 0未加入 1已加入
        if(isStudent){
            resultMap.put("is_join", "1");
            resultMap.put("is_bought", "1");
        }else {
            resultMap.put("is_join", "0");
            //判断是否购买了该课程
            boolean buyStatus = isBuySaasCourse(userId, singleId, jedis);
            if(!buyStatus){	//用户未购买
                resultMap.put("is_bought", "0");
            }else{	//用户已购买
                resultMap.put("is_bought", "1");
            }
        }

		/*
		 * 查找是否属于系列课
		 */
        String seriesId = singleMap.get("series_id");
        if(StringUtils.isNotBlank(seriesId)){
			/*
			 * 属于某一门系列课，需要查找到系列id
			 */
            resultMap.put("series_id", seriesId);
            resultMap.put("series_course_updown", singleMap.get("series_course_updown"));
			/*
			 * 判断是否订阅系列
			 */
            boolean isJoinSeries = isJoinSeries(userId, seriesId, jedis);
            if(isJoinSeries){
                resultMap.put("is_join_series", "1");
            }else{
                resultMap.put("is_join_series", "0");
            }
        }


        //更新缓存
        Map<String, Object> readCourseMap = new HashMap<String, Object>();
        readCourseMap.put(Constants.CACHED_KEY_COURSE_FIELD, singleId);
        String readCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, readCourseMap);
        jedis.hincrBy(readCourseKey, "click_num", 1);	//缓存中点击数+1

        resultMap.put("single_info", singleMap);
        return resultMap;
    }
    /**
     * 判断用户是否定购某Saas课程
     * @param userId
     * @param courseId Saas课程id
     * @param jedis
     * @return
     */
    private boolean isBuySaasCourse(String userId, String courseId, Jedis jedis){
        Map<String, Object> keyField = new HashMap<String, Object>();
        keyField.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String key = MiscUtils.getKeyOfCachedData(Constants.SYS_USER_BUY_LIST, keyField);
        Boolean result = jedis.sismember(key, courseId);
        return result;
    }
    /**
     * H5_课程-获取图文课程内容
     * @param reqEntity
     * @throws Exception
     */
    @FunctionName("vodArticleCourse")
    public Map<String, Object> vodArticleCourse(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //获取请求查看的article_id
        String articleId = (String) reqMap.get("article_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();

        //生成用于缓存不存在时调用数据库的requestEntity
        Map<String, Object> readArticleMap = new HashMap<String, Object>();
        //从产品原型上看，调用该接口为图文课程点播，所以直接从t_saas_course查找
        RequestEntity readArticleReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readArticleMap);
        readArticleMap.put("course_id", articleId);
        //获取课程详情
        Map<String, String> articleMap = readCourse(articleId, readArticleReqEntity, readCourseOperation, jedis, true);
        if(articleMap == null){
            log.error("saas_H5_课程-获取图文课程内容>>>>课程不存在");
            throw new QNLiveException("100004");
        }

		/*
		 * 判断登录是否购买该课程
		 */
        String lecturerId = articleMap.get("lecturer_id");
        if(!userId.equals(lecturerId)){	//登录用户不是课程讲师
            boolean isStudent = isJoinCourse(userId, articleId, jedis);
            if(!isStudent){	//未购买
                log.error("saas_H5_课程-获取图文课程内容>>>>用户未加入该图文课程(courseId=" + articleId + ")");
                throw new QNLiveException("120007");
            }
        }else{
            log.info("saas_H5_课程-获取图文课程内容>>>>用户为课程(courseId=" + articleId + ")的讲师，不用判断是否购买");
        }

		/*
		 * 查找是否属于系列课
		 */
        String seriesId = articleMap.get("series_id");
        if(StringUtils.isNotBlank(seriesId)){
			/*
			 * 属于某一门系列课，需要查找到系列id
			 */
            resultMap.put("series_id", articleMap.get("series_id"));
            resultMap.put("series_course_updown", articleMap.get("series_course_updown"));
			/*
			 * 判断是否加入系列课
			 */
            boolean isJoinSeries = isJoinSeries(userId, seriesId, jedis);
            if(isJoinSeries){
                resultMap.put("is_join_series", "1");
            }else{
                resultMap.put("is_join_series", "0");
            }
        }

		/*
		 * 拼接分享链接share_url
		 */
        String shareUrl = MiscUtils.getConfigByKey("saas_course_share_url_pre_fix") + articleId;
        resultMap.put("share_url", shareUrl);

        resultMap.put("article_info", articleMap);

        return resultMap;
    }
    /**
     * H5_课程-获取课程内容（音频或视频）
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("vodCourse")
    public Map<String, Object> vodCourse(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //获取请求查看的course_id
        String courseId = (String) reqMap.get("course_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();

        //生成用于缓存不存在时调用数据库的requestEntity
        Map<String, Object> readCourseMap = new HashMap<String, Object>();
        //从产品原型上看，调用该接口为非直播类课程点播，所以直接从t_saas_course查找
        RequestEntity readCourseReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readCourseMap);
        readCourseMap.put("course_id", courseId);
        //获取课程详情
        Map<String, String> courseMap = readCourse(courseId, readCourseReqEntity, readCourseOperation, jedis, true);
        if(courseMap == null){
            log.error("saas_H5_课程-获取课程内容（音频或视频）>>>>课程不存在");
            throw new QNLiveException("100004");
        }

		/*
         * 判断是否加入了课程
         */
        String lecturerId = courseMap.get("lecturer_id");
        if(!userId.equals(lecturerId)){	//登录用户不是课程讲师
            boolean isStudent = isJoinCourse(userId, courseId, jedis);
            if(!isStudent){	//未购买
                log.error("Saas_H5_课程-获取课程内容（音频或视频）>>>>用户未加入该课程(courseId=" + courseId + ")");
                throw new QNLiveException("120007");
            }
        }else{
            log.info("Saas_H5_课程-获取课程内容（音频或视频）>>>>用户为课程(courseId=" + courseId + ")的讲师，不用判断是否购买");
        }

		/*
		 * 查找是否属于系列课
		 */
        String seriesId = courseMap.get("series_id");
        if(StringUtils.isNotBlank(seriesId)){
			/*
			 * 属于某一门系列课，需要查找到系列id
			 */
            resultMap.put("series_id", courseMap.get("series_id"));
            resultMap.put("series_course_updown", courseMap.get("series_course_updown"));
			/*
			 * 判断是否加入系列课
			 */
            boolean isJoinSeries = isJoinSeries(userId, seriesId, jedis);
            if(isJoinSeries){
                resultMap.put("is_join_series", "1");
            }else{
                resultMap.put("is_join_series", "0");
            }
        }

		/*
		 * 拼接分享链接share_url
		 */
        String shareUrl = MiscUtils.getConfigByKey("saas_course_share_url_pre_fix") + courseId;
        resultMap.put("share_url", shareUrl);

        resultMap.put("course_info", courseMap);

        return resultMap;
    }
    /**
     * H5_课程-获取课程留言列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findCourseMessageList")
    public Map<String, Object> findCourseMessageList(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //获取请求查看的course_id
        String courseId = (String) reqMap.get("course_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        //获取分页标识
        String lastMessageId = reqMap.get("last_message_id").toString();
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        //课程留言列表
        List<Map<String, String>> messageInfoList = new ArrayList<>();

        /*
         * 获取课程讲师id
         */
        Map<String, String> courseMap = readCourse(courseId, reqEntity, readCourseOperation, jedis, true);

        /*
         * 判断是否加入了课程
         */
        String lecturerId = courseMap.get("lecturer_id");
        if(!userId.equals(lecturerId)){	//登录用户不是课程讲师
            boolean isStudent = isJoinCourse(userId, courseId, jedis);
            if(!isStudent){	//未购买
                log.error("Saas_H5_课程-获取课程留言列表>>>>用户未加入该课程(courseId=" + courseId + ")");
                throw new QNLiveException("120007");
            }
        }else{
            log.info("Saas_H5_课程-获取课程留言列表>>>>用户为课程(courseId=" + courseId + ")的讲师，不用判断是否购买");
        }

    	/*
         * 根据课程id查询缓存中课程的留言id列表，需要传递分页标识
         */
        //read课程的留言列表，以创建时间倒序排序
        Set<String> messageSet = readCourseMessageSet(courseId, lastMessageId, pageCount, jedis);
        if(messageSet != null){
            log.info("saas课程-获取课程留言列表>>>>从缓存中获取到课程（course_id=" + courseId + "）的留言列表");
            String messageDetailKey = null;	//获取内容课程详情的key
            //生成用于缓存不存在时调用数据库的requestEntity
            Map<String, Object> readMessageMap = new HashMap<String, Object>();
            //从产品原型上看，调用该接口为非直播类课程点播，所以直接从t_saas_course查找
            RequestEntity readMessageReqEntity = this.generateRequestEntity(null, null, "findSaasCourseCommentByCommentId", readMessageMap);

            String[] searchKeys = {courseId, ""};	//用于读取课程留言的
            for(String messageId : messageSet){
                searchKeys[1] = messageId;
                readMessageMap.put("message_id", messageId);
                //获取留言详情
                //TODO 缓存
                Map<String, String> messageMap = null;//readSaasCourseComment(searchKeys, readMessageReqEntity, readSaasCourseMessageOperation, jedis, true);

                messageInfoList.add(messageMap);
            }
        }

        resultMap.put("message_info_list", messageInfoList);
        return resultMap;
    }

    /**
     * H5_课程-添加课程留言
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("addMessageForCourse")
    public Map<String, Object> addMessageForCourse(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //获取请求查看的course_id
        String courseId = (String) reqMap.get("course_id");
        //获取请求中的评论内容
        String content = (String) reqMap.get("content");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        Date now = new Date();
        /*
         * 获取课程详情
         */
        Map<String, String> courseMap = readCourse(courseId, reqEntity, readCourseOperation, jedis, true);
        /*
         * 判断是否加入了课程
         */
        String lecturerId = courseMap.get("lecturer_id");
        if(!userId.equals(lecturerId)){	//登录用户不是课程讲师
            boolean isStudent = isJoinCourse(userId, courseId, jedis);
            if(!isStudent){	//未购买
                log.error("Saas_H5_课程-添加课程留言列表>>>>用户未加入该课程(courseId=" + courseId + ")");
                throw new QNLiveException("120007");
            }
        }else{
            log.info("Saas_H5_课程-添加课程留言列表>>>>用户为课程(courseId=" + courseId + ")的讲师，不用判断是否购买");
        }

        /*
         * 获得课程信息，主要用于后续获取店铺id进行存储
         */
        Map<String, Object> readCourseMap = new HashMap<String, Object>();
        readCourseMap.put("course_id", courseId);
        RequestEntity readCourseReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readCourseMap);

        Map<String, String> courseInfoMap = readCourse(courseId, readCourseReqEntity, readCourseOperation, jedis, true);
        if(courseInfoMap == null || courseInfoMap.isEmpty()){
            log.error("saas课程-添加课程留言>>>>课程不存在");
            throw new QNLiveException("100004");
        }

        /*
         * 获取登录用户信息，主要用于后续获取用户昵称等
         */
        reqMap.put("user_id", userId);
        Map<String, String> loginedUserMap = readUser(userId, reqEntity, readUserOperation, jedis);
        if(loginedUserMap == null || loginedUserMap.isEmpty()){
            log.error("saas课程-添加课程留言>>>>登录用户不存在");
            throw new QNLiveException("000005");
        }
        /*
         * 新增到数据库
         */
        //封装要新增的留言map
        String commentId = MiscUtils.getUUId();
        Map<String, Object> insertCommentMap = new HashMap<>();
        insertCommentMap.put("comment_id", commentId);
        insertCommentMap.put("shop_id", courseInfoMap.get("shop_id"));
        insertCommentMap.put("series_id", courseInfoMap.get("series_id"));
        insertCommentMap.put("course_id", courseId);
        insertCommentMap.put("user_id", loginedUserMap.get("user_id"));
        insertCommentMap.put("nick_name", loginedUserMap.get("nick_name"));
        insertCommentMap.put("content", reqMap.get("content"));
        insertCommentMap.put("course_name", courseInfoMap.get("course_title"));
        insertCommentMap.put("type", courseInfoMap.get("goods_type"));
        insertCommentMap.put("avatar_address", loginedUserMap.get("avatar_address"));
        insertCommentMap.put("create_time", now);

        //封装更新的saas课程评论数量map
        Map<String, Object> updateCourseMap = new HashMap<>();
        updateCourseMap.put("course_id", courseId);
        updateCourseMap.put("comment_num", 1);
        updateCourseMap.put("update_time", now);

        //用户在该店铺的评论数
        Map<String, Object> updateSaasShopUserMap = new HashMap<>();
        updateSaasShopUserMap.put("user_id", userId);
        updateSaasShopUserMap.put("shop_id", courseInfoMap.get("shop_id"));
        updateSaasShopUserMap.put("update_time", now);
        updateSaasShopUserMap.put("comment_num", 1);

        //新增数据库留言、更新数据库课程留言数量；更新缓存中saas课程评论id列表
        //shopModuleServer.addSaasCourseComment(insertCommentMap, updateCourseMap, updateSaasShopUserMap);
        /*
         * 插入到缓存中saas课程评论id列表
         */
        Map<String, Object> readSaasCourseCommentMap = new HashMap<>();
        readSaasCourseCommentMap.put(Constants.CACHED_KEY_COURSE_FIELD, updateCourseMap.get("course_id"));
        String readSaasCourseCommentKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_SAAS_COMMENT_ALL, readSaasCourseCommentMap);
        jedis.zadd(readSaasCourseCommentKey, now.getTime(), insertCommentMap.get("comment_id").toString());
        /*
         * 更新缓存中saas课程的评论数量
         */
        String readSaasCourseDetailKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, readSaasCourseCommentMap);
        jedis.hincrBy(readSaasCourseDetailKey, "comment_num", 1);
        return resultMap;
    }
    /**
     * H5_用户-提交反馈与建议
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("addFeedback")
    public Map<String, Object> addFeedback(RequestEntity reqEntity) throws Exception{
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", userId);
        //获取请求中的反馈内容
        String content = (String) reqMap.get("content");
        //获取请求中的联系方式
        String contact = (String) reqMap.get("contact");
        //获取请求中的店铺id
        String shopId = (String) reqMap.get("shop_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        //获取当前服务器时间
        Date now = new Date();

        /*
         * 从缓存中获取登录用户信息
         */
        Map<String, String> loginedUserMap = readUser(userId, reqEntity, readUserOperation, jedis);
        if(loginedUserMap == null || loginedUserMap.isEmpty()){
            log.error("Saas_H5_用户-提交反馈与建议>>>>登录用户不存在");
            throw new QNLiveException("000005");
        }

        /*
         * 新增到数据库
         */
        Map<String, Object> newFeedbackMap = new HashMap<String, Object>();
        newFeedbackMap.put("back_id", MiscUtils.getUUId());
        newFeedbackMap.put("shop_id", shopId);
        newFeedbackMap.put("user_id", userId);
        newFeedbackMap.put("avatar_address", loginedUserMap.get("avatar_address"));
        newFeedbackMap.put("nick_name", loginedUserMap.get("nick_name"));
        newFeedbackMap.put("content", content);
        newFeedbackMap.put("phone", contact);
        newFeedbackMap.put("create_time", now);
        //shopModuleServer.addFeedback(newFeedbackMap);

        return resultMap;
    }
    /**
     * 用户-访问店铺
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("userVisit")
    public void userVisit(RequestEntity reqEntity) throws Exception{
        Map<String,Object> query = new HashMap<>();
        String shopId = ((Map<String,String>)reqEntity.getParam()).get("shop_id");
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        //店铺信息
        Map<String, String> shopInfo = readShop(shopId, false ,jedis);
        //店主ID
        String lecturerId = shopInfo.get("user_id");
        query.put("user_id", lecturerId);
        String userCountKey = MiscUtils.getKeyOfCachedData(Constants.SHOP_DAY_COUNT, query);
        //本人访问自己的店铺不做统计
        if(lecturerId!=null&&!lecturerId.equals(userId)){
            if(!jedis.exists(userCountKey)){
                long milliSecondsLeftToday = 86400000 - DateUtils.getFragmentInMilliseconds(Calendar.getInstance(), Calendar.DATE);
                //今日浏览人数
                jedis.hincrBy(userCountKey,"day_visit",1);
                //设置失效时间为今天
                jedis.expire(userCountKey,Integer.valueOf((milliSecondsLeftToday/1000)+""));
            }else{
                jedis.hincrBy(userCountKey,"day_visit",1);
            }

            //插入t_saas_shop_users表
            shopModuleServer.userVisitShop(userId,shopId);
        }
    }
    /**
     * 用户-单品已购
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("buiedSingleList")
    public Map<String, Object> buiedSingleList(RequestEntity reqEntity) throws Exception{
        Map<String,Object> query =  (Map<String, Object>)reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        String shopId = query.get("shop_id").toString();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        //店铺信息
        Map<String, String> shopInfo = readShop(shopId, false,jedis);
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

        //店主ID
        query.put("lecturer_id",shopInfo.get("user_id"));
        query.put("user_id",userId);
        query.put("type","1");
        if(query.get("position") == null||StringUtils.isEmpty(query.get("position").toString())){
            query.remove("position");
        }
        List<Map<String,Object>> records = shopModuleServer.findUserBuiedRecords(query);

        if(! CollectionUtils.isEmpty(records)){
            Map<String,Object> cacheQueryMap = new HashMap<>();

            JedisBatchCallback callBack = (JedisBatchCallback)jedis;

            //从缓存中查询讲师的名字
            callBack.invoke((pipeline, jedis1) -> {
                for(Map<String,Object> recordMap : records){
                    cacheQueryMap.clear();
                    cacheQueryMap.put(Constants.CACHED_KEY_COURSE_FIELD, recordMap.get("course_id"));
                    String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, cacheQueryMap);
                    Response<String> courseName = pipeline.hget(courseKey, "course_title");
                    Response<String> course_updown = pipeline.hget(courseKey, "course_updown");
                    Response<String> course_image = pipeline.hget(courseKey, "course_image");
                    Response<String> goods_type = pipeline.hget(courseKey, "goods_type");
                    Response<String> course_duration = pipeline.hget(courseKey, "course_duration");
                    Response<String> status = pipeline.hget(courseKey, "status");
                    Response<String> course_url = pipeline.hget(courseKey, "course_url");
                    recordMap.put("courseTitle", courseName);
                    recordMap.put("course_updown", course_updown);
                    recordMap.put("course_image", course_image);
                    recordMap.put("course_url", course_url);
                    recordMap.put("goods_type", goods_type);
                    recordMap.put("course_duration", course_duration);
                    recordMap.put("status", status);
                }
                pipeline.sync();
                for(Map<String,Object> recordMap : records){
                    Response<String> courseName = (Response)recordMap.get("courseTitle");
                    Response<String> course_updown = (Response)recordMap.get("course_updown");
                    Response<String> course_image = (Response)recordMap.get("course_image");
                    Response<String> course_url = (Response)recordMap.get("course_url");
                    Response<String> goods_type = (Response)recordMap.get("goods_type");
                    Response<String> course_duration = (Response)recordMap.get("course_duration");
                    Response<String> status = (Response)recordMap.get("status");
                    recordMap.put("title", courseName.get());
                    //recordMap.remove("cacheLecturerName");
                    if(recordMap.get("start_time") instanceof Date){
                        Date start_time = (Date)recordMap.get("start_time");
                        Date end_time = (Date)recordMap.get("end_time");
                        String liveStatus = status.get();
                        if(! liveStatus.equals("2")&& !liveStatus.equals("5")){
                            long courseStartTime = start_time.getTime();
                            if(System.currentTimeMillis() > courseStartTime){
                                recordMap.put("live_status", "1");
                            }else{
                                recordMap.put("live_status", liveStatus);
                            }
                        }else{
                            recordMap.put("live_status", liveStatus);
                        }
                        recordMap.put("start_time", start_time);
                        recordMap.put("end_time", end_time);
                        recordMap.put("course_image", course_url.get());
                        recordMap.put("type", "0");
                    }else{
                        recordMap.remove("start_time");
                        recordMap.remove("end_time");
                        recordMap.put("course_image", course_image.get());
                        //非直播
                        recordMap.put("type", goods_type.get());
                    }
                    recordMap.put("status", course_updown.get());
                    recordMap.put("course_duration", course_duration.get());
                }
            });
            resultMap.put("list", records);
        }
        return resultMap;
    }
    /**
     * 用户-系列已购
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("buiedSeriesList")
    public Map<String, Object> buiedSeriesList(RequestEntity reqEntity) throws Exception{
        Map<String,Object> query =  (Map<String, Object>)reqEntity.getParam();
        Map<String, Object> resultMap = new HashMap<>();
        String shopId = query.get("shop_id").toString();
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        //店铺信息
        Map<String, String> shopInfo = readShop(shopId, false, jedis);
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

        //店主ID
        query.put("lecturer_id",shopInfo.get("user_id"));
        query.put("user_id",userId);
        query.put("type","2");
        if(query.get("position") == null||StringUtils.isEmpty(query.get("position").toString())){
            query.remove("position");
        }
        List<Map<String,Object>> records = shopModuleServer.findUserBuiedRecords(query);

        if(! CollectionUtils.isEmpty(records)){
            Map<String,Object> cacheQueryMap = new HashMap<>();

            JedisBatchCallback callBack = (JedisBatchCallback)jedis;
            //从缓存中查询讲师的名字
            callBack.invoke(new JedisBatchOperation(){
                @Override
                public void batchOperation(Pipeline pipeline, Jedis jedis) {
                    for(Map<String,Object> recordMap : records){
                        cacheQueryMap.put(Constants.CACHED_KEY_SERIES_FIELD, recordMap.get(Constants.CACHED_KEY_SERIES_FIELD));
                        String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, cacheQueryMap);
                        Response<String> studentNum = pipeline.hget(seriesKey, "student_num");
                        recordMap.put("studentNum",studentNum);
                    }
                    pipeline.sync();

                    for(Map<String,Object> recordMap : records){
                        Response<String> studentNum = (Response)recordMap.get("studentNum");
                        recordMap.put("student_num",studentNum.get());
                    }
                }
            });
            resultMap.put("list", records);
        }
        return resultMap;
    }
    /**
     * 创建课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("createCourse")
    public Map<String, Object> createCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String lecturer_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//通过token 获取讲师id
        Jedis jedis = jedisUtils.getJedis();
        Map<String,Object> query = new HashMap();
        query.put(Constants.CACHED_KEY_LECTURER_FIELD,lecturer_id);
        Map<String,String> lecturerMap = readLecturer(lecturer_id,this.generateRequestEntity(null, null, null, query),readLecturerOperation,jedis);

        //判断当前用户是否是讲师
        if(MiscUtils.isEmpty(lecturerMap)){
            throw new QNLiveException("100001");
        }
        String shop_id = lecturerMap.get("shop_id");
        reqMap.put("shop_id",shop_id);
        String lecturerCoursesAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, query);
        if(reqMap.get("good_type").toString().equals("0")){
            //课程之间需要间隔三十分钟
            SortUtils.whetherCreateLiveCourse(shop_id,Long.valueOf(reqMap.get("live_start_time").toString()),jedis);
            //2.1创建IM 聊天群组
            Map<String,String> queryParam = new HashMap<>();
            queryParam.put(Constants.CACHED_KEY_ACCESS_TOKEN_FIELD, reqEntity.getAccessToken());
            String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, queryParam);
            Map<String,String> userMap = jedis.hgetAll(accessTokenKey);
            try {
                Map<String,String> groupMap = IMMsgUtil.createIMGroup(userMap.get("m_user_id").toString());
                if(groupMap == null || StringUtils.isBlank(groupMap.get("groupid"))){
                    throw new QNLiveException("100015");
                }
                reqMap.put("im_course_id",groupMap.get("groupid"));
                //2.1.1将讲师加入到该群组中
                IMMsgUtil.joinGroup(groupMap.get("groupid"), userMap.get("m_user_id").toString(), userMap.get("m_user_id").toString());
            }catch (Exception e){
            }
        }

        //创建课程url
        if(reqMap.get("course_url") == null || StringUtils.isBlank(reqMap.get("course_url").toString())){
            String default_course_cover_url_original = MiscUtils.getConfigByKey("default_course_cover_url");
            JSONArray default_course_cover_url_array = JSON.parseArray(default_course_cover_url_original);
            int randomNum = MiscUtils.getRandomIntNum(0, default_course_cover_url_array.size() - 1);
            reqMap.put("course_url", default_course_cover_url_array.get(randomNum));
        }

        //创建课程到数据库
        Map<String, Object> dbCourseMap = shopModuleServer.createCourse(reqMap);
        Map<String, String> course = readCourse((String)dbCourseMap.get("course_id"),
                generateRequestEntity(null, null, null, dbCourseMap), readCourseOperation, jedis, true);//把课程刷新到缓存
        if(reqMap.get("good_type").toString().equals("0")){
            String course_type = course.get("course_type");
            String course_id = course.get("course_id");
            Map<String,Object> map = new HashMap<>();
            if ("0".equals(course_type)){//公开课才开启机器人
                log.info("创建课程，开始机器人加入功能");
                map.clear();
                map.put("course_id", course_id);
                RequestEntity mqRequestEntity = new RequestEntity();
                mqRequestEntity.setServerName("CourseRobotService");
                mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                mqRequestEntity.setFunctionName("courseCreateAndRobotStart");
                mqRequestEntity.setParam(map);
                this.mqUtils.sendMessage(mqRequestEntity);
            }
            Long startTime = Long.valueOf(course.get("start_time"));
            RequestEntity mqRequestEntity = new RequestEntity();
            mqRequestEntity.setServerName("MessagePushServer");
            mqRequestEntity.setParam(course);

            mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
            log.debug("课程直播超时处理 服务端逻辑 定时任务 course_id:"+course_id);
            mqRequestEntity.setFunctionName("processCourseLiveOvertime");
            this.mqUtils.sendMessage(mqRequestEntity);
            log.debug("进行超时预先提醒定时任务 提前60分钟 提醒课程结束 course_id:"+course_id);
            mqRequestEntity.setFunctionName("processLiveCourseOvertimeNotice");
            this.mqUtils.sendMessage(mqRequestEntity);
            if(MiscUtils.isTheSameDate(new Date(startTime), new Date())){
                log.debug("提前五分钟开课提醒 course_id:"+course_id);
                if(startTime-System.currentTimeMillis()> 5 * 60 *1000){
                    mqRequestEntity.setFunctionName("processCourseStartShortNotice");
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
                //如果该课程为今天内的课程，则调用MQ，将其加入课程超时未开播定时任务中  结束任务 开课时间到但是讲师未出现提醒  推送给参加课程者
                mqRequestEntity.setFunctionName("processCourseStartLecturerNotShow");
                this.mqUtils.sendMessage(mqRequestEntity);
                log.debug("直播间开始发送IM  course_id:"+course_id);
                mqRequestEntity.setFunctionName("processCourseStartIM");
                this.mqUtils.sendMessage(mqRequestEntity);
            }
            //提前24小时开课提醒
            if(MiscUtils.isTheSameDate(new Date(startTime- 60 * 60 *1000*24), new Date()) && startTime-System.currentTimeMillis()> 60 * 60 *1000*24){
                mqRequestEntity.setFunctionName("processCourseStartLongNotice");
                this.mqUtils.sendMessage(mqRequestEntity);
            }
            //给课程里面推消息
            Map<String, Object> userInfo = shopModuleServer.findUserInfoByUserId(course.get("lecturer_id"));
            Map<String,Object> startLecturerMessageInformation = new HashMap<>();
            startLecturerMessageInformation.put("creator_id",userInfo.get("user_id"));//发送人id
            startLecturerMessageInformation.put("course_id", course.get("course_id"));//课程id
            startLecturerMessageInformation.put("message",MiscUtils.getConfigByKey("start_lecturer_message"));
            startLecturerMessageInformation.put("message_type", "1");
            startLecturerMessageInformation.put("message_id",MiscUtils.getUUId());
            startLecturerMessageInformation.put("message_imid",startLecturerMessageInformation.get("message_id"));
            startLecturerMessageInformation.put("create_time",  System.currentTimeMillis());
            startLecturerMessageInformation.put("send_type","0");
            startLecturerMessageInformation.put("creator_avatar_address",userInfo.get("avatar_address"));
            startLecturerMessageInformation.put("creator_nick_name",userInfo.get("nick_name"));

            String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, startLecturerMessageInformation);
//					//1.将聊天信息id插入到redis zsort列表中
            jedis.zadd(messageListKey,  System.currentTimeMillis(), (String)startLecturerMessageInformation.get("message_imid"));
//					//添加到老师发送的集合中
            String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, startLecturerMessageInformation);
            jedis.zadd(messageLecturerListKey,  System.currentTimeMillis(),startLecturerMessageInformation.get("message_imid").toString());
            String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, startLecturerMessageInformation);//直播间开始于
            Map<String,String> result = new HashMap<String,String>();
            MiscUtils.converObjectMapToStringMap(startLecturerMessageInformation, result);
            jedis.hmset(messageKey, result);
            //</editor-fold>
            long lpos = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("start_time")) , MiscUtils.convertObjectToLong(course.get("position")));
            //设置讲师更新课程 30分钟
            jedis.zadd(lecturerCoursesAllKey,lpos,course.get("course_id"));
        }
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.putAll(course);
        return resultMap;
    }


    /**
     * 修改课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("updateCourse")
    public Map<String, Object> updateCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();//传过来的参数
        Map<String, Object> resultMap = new HashMap<String, Object>();//返回的参数
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        String lecturerId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//获取讲师id
        String courseId = (String) reqMap.get("course_id");

        Map<String, String> course = readCourse(courseId, generateRequestEntity(null, null, null, reqMap), readCourseOperation, jedis, true);//课程信息
        if (MiscUtils.isEmpty(course)) {//如果没有课程
            throw new QNLiveException("100004");//课程不存在 抛异常
        }
        if (!lecturerId.equals(course.get("lecturer_id"))) {//判断 这个课程是否是这个讲师的
            throw new QNLiveException("100013");
        }

        if (course.get("goods_type").equals("0")) {
            if (reqMap.get("live_start_time") != null) {                //如果要更改直播课 上课时间 课程之间需要间隔三十分钟
                SortUtils.whetherCreateLiveCourse(course.get("shop_id"), Long.valueOf(reqMap.get("live_start_time").toString()), jedis);
            }
        }
        Map<String, Object> dbResultMap = shopModuleServer.updateCourse(reqMap);
        if (course.get("goods_type").equals("0")) {
            if (reqMap.get("live_start_time") != null) { //开课时间修改
                Map<String, Object> query = new HashMap<String, Object>();
                query.put("course_id", courseId);
                course = readCourse(courseId, generateRequestEntity(null, null, null, query), readCourseOperation, jedis, true);
                long startTime = MiscUtils.convertObjectToLong(reqMap.get("start_time"));
                Map<String, Object> timerMap = new HashMap<>();
                timerMap.put("course_id", courseId);
                timerMap.put("start_time", new Date(startTime));
                timerMap.put("lecturer_id", lecturerId);
                timerMap.put("course_title", course.get("course_title"));
                timerMap.put("course_id", course.get("course_id"));
                timerMap.put("start_time", startTime + "");
                timerMap.put("position", course.get("position"));

                RequestEntity mqRequestEntity = new RequestEntity();
                mqRequestEntity.setServerName("MessagePushServer");
                mqRequestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                mqRequestEntity.setParam(timerMap);

                log.debug("清除所有定时任务 course_id:" + courseId);
                mqRequestEntity.setFunctionName("processCourseNotStartCancelAll"); //课程未开播所有清除所有定时任务
                this.mqUtils.sendMessage(mqRequestEntity);

                log.debug("课程直播超时处理 服务端逻辑 定时任务 course_id:" + courseId);
                mqRequestEntity.setFunctionName("processCourseLiveOvertime");
                this.mqUtils.sendMessage(mqRequestEntity);
                log.debug("进行超时预先提醒定时任务 提前60分钟 提醒课程结束 course_id:" + courseId);
                mqRequestEntity.setFunctionName("processLiveCourseOvertimeNotice");
                this.mqUtils.sendMessage(mqRequestEntity);
                if (MiscUtils.isTheSameDate(new Date(startTime), new Date())) {
                    log.debug("提前五分钟开课提醒 course_id:" + courseId);
                    if (startTime - System.currentTimeMillis() > 5 * 60 * 1000) {
                        mqRequestEntity.setFunctionName("processCourseStartShortNotice");
                        this.mqUtils.sendMessage(mqRequestEntity);
                    }
                    //如果该课程为今天内的课程，则调用MQ，将其加入课程超时未开播定时任务中  结束任务 开课时间到但是讲师未出现提醒  推送给参加课程者
                    mqRequestEntity.setFunctionName("processCourseStartLecturerNotShow");
                    this.mqUtils.sendMessage(mqRequestEntity);
                    log.debug("直播间开始发送IM  course_id:" + courseId);
                    mqRequestEntity.setFunctionName("processCourseStartIM");
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
                //提前24小时开课提醒
                if (MiscUtils.isTheSameDate(new Date(startTime - 60 * 60 * 1000 * 24), new Date()) && startTime - System.currentTimeMillis() > 60 * 60 * 1000 * 24) {
                    mqRequestEntity.setFunctionName("processCourseStartLongNotice");
                    this.mqUtils.sendMessage(mqRequestEntity);
                }
            }
        }
        return dbResultMap;
    }

  /**
     * 判断用户是否加入某系列
     * @param userId
     * @param seriesId
     * @param jedis
     * @return
     */
    private boolean isJoinSeries(String userId, String seriesId, Jedis jedis){
        Map<String, Object> keyField = new HashMap<String, Object>();
        keyField.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_SERIES, keyField);
        Long index = jedis.zrank(key, seriesId);
        if(index == null){	//获取不到下标，说明不在该用户定购的系列里
            return false;
        }else{
            return true;
        }
    }


    /**
     * 判断用户是否加入某单品课程（包括直播、非直播）
     * @param userId
     * @param courseId
     * @param jedis
     * @return
     */
    private boolean isJoinCourse(String userId, String courseId, Jedis jedis){
        Map<String, Object> selectIsStudentMap = new HashMap<String, Object>();	//用于判断是否加入课程
        selectIsStudentMap.put("user_id", userId);
        selectIsStudentMap.put("course_id", courseId);
		/*
         * 判断是否加入了课程
         */
        boolean isStudent = shopModuleServer.isStudentOfTheCourse(selectIsStudentMap);
        //加入课程状态 0未加入 1已加入
        if(isStudent){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 判断用户是否加入某课程
     * @param userId
     * @param liveCourseId
     * @param jedis
     * @return
     */
    private boolean isJoinLiveCourse(String userId, String liveCourseId, Jedis jedis){
        Map<String, Object> keyField = new HashMap<String, Object>();
        keyField.put(Constants.CACHED_KEY_USER_FIELD, userId);
        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, keyField);
        Boolean result = !MiscUtils.isEmpty(jedis.zrank(key, liveCourseId));
        return result;
    }



    /**
     * 上下架
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("updown")
    public Map<String, Object> updown(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();
        Map<String,Object> result = new HashMap<>();
        Date now = new Date();
        String user_id =  AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        String updown = reqMap.get("updown").toString();//上下架
        String query_type = reqMap.get("query_type").toString();//查询类型 0 单品课程 1系列  2系列里的课程
        String query_from = reqMap.get("query_from").toString();//来源 0 app  1 :店铺
        String updown_id = reqMap.get("updown_id").toString();//要进行操作的系列id或者课程id
        Map<String,Object> updownMap = new HashMap<>();
        updownMap.put("update_time",now);
        if(query_type.equals("1")){//系列
            //<editor-fold desc="系列">
            updownMap.put("series_id",updown_id);
            Map<String, String> seriesMap = readSeries(updown_id, generateRequestEntity(null, null, null, updownMap), readSeriesOperation, jedis, true);
            //判断当前系列是这个讲师的吗
            if(!seriesMap.get(Constants.CACHED_KEY_LECTURER_FIELD).equals(user_id)){
                throw new QNLiveException("210001");
            }
            updownMap.put("updown",updown);
            result = shopModuleServer.updateUpdown(updownMap);
            readSeries(updown_id, generateRequestEntity(null, null, null, updownMap), readSeriesOperation, jedis, true);
            SortUtils.updateSeriesListByRedis(seriesMap,null,jedis,readSeriesOperation,generateRequestEntity(null, null, null, null));
            //</editor-fold>
        }else{
            //<editor-fold desc="课程">
            String courseId = updown_id;
            //判断当前系列是这个讲师的吗
            //String lecturer_id = jedis.hget(courseKey, "lecturer_id");
            Map<String, String> course  = readCourse(courseId,
                    generateRequestEntity(null, null, null, updownMap), readCourseOperation, jedis, true);
            String lecturer_id = course.get("lecturer_id");
            if(!lecturer_id.equals(user_id)){
                throw new QNLiveException("210001");
            }
            if(query_type.equals("0")){
                updownMap.put("course_updown",updown);
            }else{
                updownMap.put("series_course_updown",updown);
            }
            result = shopModuleServer.updateUpdown(updownMap);//更改数据库
            Map<String,String> courseMap = readCourse(courseId,
                        generateRequestEntity(null, null, null, updownMap), readCourseOperation, jedis, true);
            if(query_type.equals("0")){//单品
                SortUtils.updateCourseListByRedis(courseMap,jedis,readCourseOperation, generateRequestEntity(null, null, null, null));
            }else{//系列课
                String series_id = courseMap.get("series_id").toString();
                Map<String,Object> map = new HashMap<>();
                map.put("series_id",series_id);
                Map<String,String> seriesMap = readSeries(series_id,generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
                SortUtils.updateSeriesListByRedis(seriesMap,courseMap,jedis,readSeriesOperation,generateRequestEntity(null, null, null, null));
                readSeries(series_id,generateRequestEntity(null, null, null, map), readSeriesOperation, jedis, true);
            }
            //</editor-fold>
        }
        return result ;
    }


    /**
     * 编辑学员成为嘉宾 或者取消嘉宾
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("editStudentOrGuest")
    public Map<String, Object> editStudentOrGuest(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();
        String inviter_user = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//邀请人 通过token获取 先判断是否是讲师
        String query_type = reqMap.get("query_type").toString();// 0创建嘉宾 1取消嘉宾
        String course_id = reqMap.get("course_id").toString();//课程id
        String guest_user_id = reqMap.get("guest_user_id").toString();//嘉宾用户id


        Map<String,Object> map = new HashMap<>();
        map.put("course_id",course_id);
        Map<String, String> courseInfoMap = readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);

        map.clear();
        map.put("user_id",guest_user_id);
        Map<String, String> userMap = readUser(guest_user_id, this.generateRequestEntity(null, null, null, map), readUserOperation, jedis);

        if(!inviter_user.equals(courseInfoMap.get("lecturer_id"))){
            throw new QNLiveException("410001");
        }

        String guest_role = 0+"";
        String guest_tag =Constants.DEFAULT_GUEST_TAG;
        if(!MiscUtils.isEmpty(reqMap.get("guest_role"))){
            guest_role = reqMap.get("guest_role").toString();
        }
        if(!MiscUtils.isEmpty(reqMap.get("guest_tag"))){
            guest_tag = reqMap.get("guest_tag").toString();
        }
        Map<String,Object> guestMap = new HashMap<>();
        guestMap.put("guest_id",MiscUtils.getUUId());
        guestMap.put("course_id",course_id);
        guestMap.put("user_id",guest_user_id);
        guestMap.put("guest_role",guest_role);
        guestMap.put("status",query_type);
        guestMap.put("guest_tag",guest_tag);
        guestMap.put("inviter_user",inviter_user);
        Map<String, Object> stringObjectMap = shopModuleServer.courseGurest(guestMap);

        map.clear();
        map.put("course_id", course_id);
        String sys_course_guest =  MiscUtils.getKeyOfCachedData(Constants.SYS_COURSE_GUEST, map);
        if(query_type.equals("1")){
            jedis.zadd(sys_course_guest,System.currentTimeMillis(),guest_user_id);
        }else{
            jedis.zrem(sys_course_guest,guest_user_id);
        }
        map.clear();
        map.put(Constants.GUEST_ID, guest_user_id);
        String predictionKey =  MiscUtils.getKeyOfCachedData(Constants.SYS_GUEST_COURSE_PREDICTION, map);
        String finishKey =  MiscUtils.getKeyOfCachedData(Constants.SYS_GUEST_COURSE_FINISH, map);
        jedis.del(predictionKey);
        jedis.del(finishKey);

        String mGroupId = courseInfoMap.get("im_course_id");//获取课程imid
        //发送成为嘉宾接口
        String sender = "system";
        Map<String,Object> infomation = new HashMap<>();
        infomation.put("ban_status",query_type);
        infomation.put("user_id",guest_user_id);
        infomation.put("course_id", course_id);
        infomation.put("nick_name",userMap.get("nick_name"));
        infomation.put("create_time", System.currentTimeMillis());
        Map<String,Object> messageMap = new HashMap<>();
        messageMap.put("msg_type","5");
        messageMap.put("send_time", System.currentTimeMillis());
        messageMap.put("create_time", System.currentTimeMillis());
        messageMap.put("information",infomation);
        messageMap.put("mid",infomation.get("message_id"));
        String content = JSON.toJSONString(messageMap);
        IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);

        return stringObjectMap;
    }

}
