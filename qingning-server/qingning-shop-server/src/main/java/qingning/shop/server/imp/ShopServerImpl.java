package qingning.shop.server.imp;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
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
            map.clear();
            map.put("shop_id", shopId);
            this.readShop(shopId, map, CommonReadOperation.CACHE_READ_SHOP, false,jedis);
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

        //直播分享URL
        shop.put("live_url",MiscUtils.getConfigByKey("course_share_url_pre_fix"));
        return shop;
    }

    /**
     * 店铺设置
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopEdit")
    public void shopEdit(RequestEntity reqEntity) throws Exception{
        Map<String,Object> param = (Map<String, Object>) reqEntity.getParam();
        String userId = "";
        if(!MiscUtils.isEmpty(param.get("user_id"))){
            userId = param.get("user_id").toString();
        }else{
            userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        }
        param.put("user_id",userId);
        Jedis jedis = jedisUtils.getJedis();//获取jedis对象
        Map<String, String> shopInfo = this.readCurrentUserShop(reqEntity, jedis);
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_SHOP_FIELD, shopInfo.get("shop_id"));
        //清空店铺缓存
        String shopKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SHOP, map);
        jedis.del(shopKey);
        shopModuleServer.updateShop(param);
    }

    /**
     * 获取扫码URL
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
     * 店铺-轮播图列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopBannerList")
    public Map<String, Object>  shopBannerList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("lecturer_id",userId);
        Map<String, Object> result = shopModuleServer.getShopBannerList(reqMap);
        return result;
    }
    /**
     * 店铺-添加轮播图
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
     * 店铺-轮播图编辑
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopBannerEdit")
    public void shopBannerEdit(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //String bannerId = reqMap.get("banner_id").toString();
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
        if(shopModuleServer.updateBanner(reqMap)==0){
            throw new QNLiveException("210007");
        }
    }
    /**
     * 店铺-轮播图详情
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
     * 店铺-轮播图上下架
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopBannerUpdown")
    public void  shopBannerUpdown(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        reqMap.put("status",reqMap.get("type"));
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
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
    public Map<String, Object>  findShopSeriesList(RequestEntity reqEntity) throws Exception{
    	/*
    	 * 逻辑：
    	 * 1.根据shop_id获得讲师id
    	 * 2.根据讲师id查询缓存中讲师已上架系列课，需要传递分页标识
    	 */
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, String>> seriesInfoList = new ArrayList<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        //获取分页标识
        String lastSeriesId = reqMap.get("last_series_id").toString();
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        /*
         * 从缓存中获取店铺信息
         */
        Map<String, String> shopMap = readShop(shopId, reqMap, CommonReadOperation.CACHE_READ_SHOP, false,jedis);
        //获取讲师id
        String lecturerId = shopMap.get("user_id");
        //根据讲师id查询缓存中讲师已上架系列课，需要传递分页标识
        Set<String> seriesSet = CacheUtils.readLecturerSeriesUp(lecturerId, lastSeriesId, pageCount, jedis);
        if(seriesSet != null){
            log.info("saas店铺-获取店铺系列课程列表>>>>从缓存中获取到讲师的上架系列课");
            //生成用于缓存不存在时调用数据库的requestEntity
            Map<String, Object> readSeriesMap = new HashMap<>();
            for(String seriesId : seriesSet){
                readSeriesMap.put("seried_id", seriesId);
                //获取系列课程详情
                Map<String, String> seriesMap = this.readSeries(seriesId, readSeriesMap,null, jedis, true);
                //判断是否加入了该课程
                seriesMap.put("is_join",isJoinSeries(userId, seriesId, jedis)?"1":"0");
                seriesInfoList.add(seriesMap);
            }
        }
        resultMap.put("series_info_list", seriesInfoList);
        return resultMap;
    }
/*    *//**
     * H5_店铺-获取店铺单品直播课程列表
     * @param reqEntity
     * @return
     * @throws Exception
     *//*
    @FunctionName("findShopLiveSingleList")
    public Map<String, Object>  findShopLiveSingleList(RequestEntity reqEntity) throws Exception{
    	*//*
    	 * 逻辑：
    	 * 1.讲师所有直播课程（同时包括预告和直播结束）没有缓存，所以直接查询数据库
    	 *//*
        //返回结果集
        Map<String, Object> resultMap = new HashMap<>();
    	*//*
    	 * 获取请求参数
    	 *//*
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
//        reqMap.put("user_id",userId);
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取缓存jedis
        Jedis jedis = jedisUtils.getJedis();
        //获取前端上一次刷新时的服务器时间
        long lastUpdateTime = (long) reqMap.get("last_update_time");
        //获取前端已经加载的数量
        long readedCount = (long) reqMap.get("readed_count");
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        //获取当前服务器时间
        Date now = new Date();
        *//*
         * 从缓存中获取店铺信息
         *//*
        Map<String, String> shopMap = readShop(shopId,reqMap,CommonReadOperation.CACHE_READ_SHOP,false,jedis);
        if(shopMap == null || shopMap.isEmpty()){
            log.error("saas店铺-获取店铺单品直播课程列表>>>>请求的店铺不存在");
            throw new QNLiveException("190001");
        }
        //获取讲师id
        String lecturerId = shopMap.get("user_id");

        *//*
         * 从数据库查询讲师的所有直播课程列表（同时包括预告和已完成）
         *//*
        reqMap.put("lecturer_id", lecturerId);
        reqMap.put("status_in", "'1','2','4'");	//用于sql的where `status` in (1：已发布 2:已结束 4直播中)
        if(readedCount == 0 && lastUpdateTime == 0){
            //前端请求第一页数据
            reqMap.put("create_time", now);	//用于sql进行条件查询：create_time <= now
            //返回给前端当前服务器时间
            resultMap.put("last_update_time", now);
        }else{
            reqMap.put("create_time", new Date(lastUpdateTime));	//用于sql进行条件查询：create_time <= lastUpdateTime
            //返回给前端原来传递的时间
            resultMap.put("last_update_time", lastUpdateTime);
        }
        reqMap.put("course_updown", '1');	//单品上下架 单品课 1上架 2下架 0没有

        Map<String,Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
        String getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);//讲师

        courseIdSet = jedis.zrangeByScore(getCourseIdKey,startIndex,endIndex,offset,pageCount);

        List<Map<String, Object>> liveCourseList = shopModuleServer.findLiveCourseListByMap(reqMap);
        List<Map<String, String>> resultLiveCourseList = new ArrayList<>();
        *//*
         * 调用方法判断直播状态
         *//*
        if(liveCourseList != null){
            Map<String, Object> selectIsStudentMap = new HashMap<String, Object>();	//用于判断是否加入课程
            selectIsStudentMap.put("user_id", userId);
            for(Map<String, Object> liveCourseMap : liveCourseList){
                Map<String, String> liveInfoMap = new HashMap<String, String>();
                MiscUtils.converObjectMapToStringMap(liveCourseMap, liveInfoMap);
                //进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                MiscUtils.courseTranferState(now.getTime(), liveInfoMap);
	        	*//*
        		 * 判断是否加入了课程
        		 *//*
                String courseId = liveCourseMap.get("course_id").toString();
                liveInfoMap.put("is_join",isJoinSeries(userId, courseId, jedis)?"1":"0");
                resultLiveCourseList.add(liveInfoMap);
            }
        }

        resultMap.put("live_info_list", resultLiveCourseList);
        return resultMap;
    }*/
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
}
