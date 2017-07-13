package qingning.saas.server.imp;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.saas.server.other.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ISaaSModuleServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.*;

public class SaaSServerImpl extends AbstractQNLiveServer {
    private static Logger log = LoggerFactory.getLogger(SaaSServerImpl.class);

    private ISaaSModuleServer saaSModuleServer;

    private ReadUserOperation readUserOperation;

    private ReadShopOperation readShopOperation;
    private ReadSeriesOperation readSeriesOperation;
    private ReadCourseOperation readCourseOperation;
    private ReadSingleListOperation readSingleListOperation;
    private ReadSaasCourseMessageOperation readSaasCourseMessageOperation;
    private static Logger logger = LoggerFactory.getLogger(SaaSServerImpl.class);

    @Override
    public void initRpcServer() {
        if (saaSModuleServer == null) {
            saaSModuleServer = this.getRpcService("saaSModuleServer");
            readUserOperation = new ReadUserOperation(saaSModuleServer);
            readSingleListOperation = new ReadSingleListOperation(saaSModuleServer);
            readShopOperation = new ReadShopOperation(saaSModuleServer);
            readSeriesOperation = new ReadSeriesOperation(saaSModuleServer);
            readCourseOperation = new ReadCourseOperation(saaSModuleServer);
            readSaasCourseMessageOperation = new ReadSaasCourseMessageOperation(saaSModuleServer);

        }
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
     * 判断用户是否购买系列
     * @param userId
     * @param seriesId
     * @param jedis
     * @return
     */
    private boolean isBuySeries(String userId, String seriesId, Jedis jedis){
    	Map<String, Object> selectTradeBillMap = new HashMap<>();
    	selectTradeBillMap.put("user_id", userId);
    	selectTradeBillMap.put("course_id", seriesId);
    	selectTradeBillMap.put("status", '2');	//交易状态，0：待付款 1：处理中 2：已完成 3：已关闭 
    	
    	Map<String, Object> buyBillMap = saaSModuleServer.findTradeBillByMap(selectTradeBillMap);
    	
    	if(MiscUtils.isEmpty(buyBillMap)){	//已经支付的订单不存在
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
        boolean isStudent = saaSModuleServer.isStudentOfTheCourse(selectIsStudentMap);
        //加入课程状态 0未加入 1已加入
        if(isStudent){
        	return true;
        }else {
        	return false;
        }
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
     * 判断用户是否加入某直播课程
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
	 * 扫码登录 1 获取二维码接口
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
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        Map<String,String> userMap = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
        Map<String,String> shop = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);//saaSModuleServer.getShopInfo(param);
        shop.put("avatar_address",userMap.get("avatar_address"));
        if(shop == null){
            //店铺不存在
            throw new QNLiveException("190001");
        }
        shop.put("user_id",userId);
        shop.put("nick_name",userMap.get("nick_name"));
        shop.put("phone_num",userMap.get("phone_number"));
        
        /*
         * 返回店铺预览url、电脑端特性url、使用教程url
         */
        //从数据库查询url
        String sysKeys = "'shop_preview_url','pc_introduce_url','use_url'";
        String appName = reqEntity.getAppName();
        Map<String, Object> selectSysConfigMap = new HashMap<String, Object>();
        selectSysConfigMap.put("config_key", sysKeys);
        selectSysConfigMap.put("app_name", appName);
        List<Map<String, Object>> sysConfigList = saaSModuleServer.findSystemConfigByInKey(selectSysConfigMap);
        for(Map<String, Object> sysConfigMap : sysConfigList){
        	String sysConfigKey = sysConfigMap.get("config_key").toString();
        	String sysConfigValue = sysConfigMap.get("config_value").toString();
        	if("shop_preview_url".equals(sysConfigKey)){
        		shop.put("shop_preview_url",sysConfigValue);
        	}else if("pc_introduce_url".equals(sysConfigKey)){
        		shop.put("pc_introduce_url",sysConfigValue);
        	}else if("use_url".equals(sysConfigKey)){
        		shop.put("use_url",sysConfigValue);
        	}
        }
        //直播分享URL
        shop.put("live_url",MiscUtils.getConfigByKey("course_share_url_pre_fix", appName));
        return shop;
    }
    /**
     * 店铺-店铺信息
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("openShop")
    public Map<String,String> openShop(RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String,Object> param = new HashMap<>();
        param.put("user_id",userId);
        reqEntity.setParam(param);
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        Map<String,String> userMap = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
        Map<String,Object> shopInfo = saaSModuleServer.getShopInfo(param);
        if(shopInfo!=null){
            throw new QNLiveException("210005");
        }
        Map<String,Object> shop = new HashMap<>();
        shop.put("user_id",userId);
        shop.put("shop_id",MiscUtils.getUUId());
        //直播间信息查询
        Map<String,String> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_LECTURER_FIELD, userId);
        String liveRoomListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);
        Map<String, String> key = jedis.hgetAll(liveRoomListKey);
        String roomId = null;
        if(key!=null&&!key.isEmpty()) {
            for (String id : key.keySet()) {
                roomId = id;
            }
        }

        shop.put("room_id",roomId);
        shop.put("user_name",userMap.get("nick_name")+"");
        shop.put("shop_name",userMap.get("nick_name")+"的店铺");
        shop.put("shop_remark","");
        String shopUrl = MiscUtils.getConfigByKey("share_url_shop_index",Constants.HEADER_APP_NAME)+shop.get("shop_id");
        shop.put("shop_url",shopUrl);
        shop.put("status","1");
        shop.put("create_time",new Date());
        shop.put("shop_logo",userMap.get("avatar_address"));
        saaSModuleServer.openShop(shop);
        //插入缓存
        CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);
        return null;
    }
    /**
     * 店铺设置
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("shopEdit")
    public void shopEdit(RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String,Object> param = (Map<String, Object>) reqEntity.getParam();
        param.put("user_id",userId);
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        Map<String, String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.CACHED_KEY_SHOP_FIELD, shopInfo.get("shop_id"));
        //清空店铺缓存
        String shopKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SHOP, map);
        jedis.del(shopKey);
        saaSModuleServer.updateShop(param);
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
        reqMap.put("user_id",userId);
       /* Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //用户信息
        Map<String, String> userInfo = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
        String shopId = userInfo.get("shop_id");*/
        Map<String, Object> result = saaSModuleServer.getShopBannerList(reqMap);
        return result;
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

        Map<String, Object> result = saaSModuleServer.getShopBannerInfo(bannerId);
        return result;
    }
	/**
	 * 店铺-添加轮播图
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("shopBannerAdd")
    public Map<String, Object>  addBanner(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,Object> shopInfo = saaSModuleServer.getShopInfo(reqMap);
        //判断店铺存在
        if(shopInfo == null){
            throw new QNLiveException("190001");
        }
        String shopId = shopInfo.get("shop_id").toString();
        String linkType = reqMap.get("link_type").toString();
        String linkTo = getLinkTo(reqMap,linkType);
        reqMap.put("link_type",linkType);
        reqMap.put("link_to",linkTo);

        reqMap.put("create_time",new Date());
        reqMap.put("app_name",reqEntity.getAppName());
        //默认下架
        reqMap.put("status","2");
        reqMap.put("banner_id",MiscUtils.getUUId());
        reqMap.put("shop_id",shopId);
        //添加到数据库
        Map<String, Object> result = saaSModuleServer.addShopBanner(reqMap);
        return result;
    }

    /**根据课程类型生成课程链接
     * @param reqMap
     * @param linkType
     * @return
     * @throws Exception
     */
    private String getLinkTo(Map<String, Object> reqMap,String linkType) throws Exception{
        //数据库类型(0:无跳转，1:单品，2，系列ID，3:外部链接，4：语音直播)
        String linkTo = null;
        if("3".equals(linkType)){
            //外部链接
            linkTo = reqMap.get("link_to").toString();
            reqMap.put("link_type","3");
        }else if("1".equals(linkType)||"2".equals(linkType)||"4".equals(linkType)){
            if(reqMap.get("link_id")==null){
                //课程ID为空判断
                throw new QNLiveException("000004");
            }
            //课程，系列ID
            String linkId = reqMap.get("link_id").toString();
            if("2".equals(linkType)||"3".equals(linkType)||"4".equals(linkType)){
                //单品ID
                linkTo = "/course/single/detail/"+linkId;
            }else if("5".equals(linkType)){
                //系列ID
                linkTo = "/course/series/detail/"+linkId;
            }else if("1".equals(linkType)){
                //直播
                linkTo = "/user/courses/"+linkId;
            }
        }
        return linkTo;
    }
	/**
	 * 店铺-轮播图编辑
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("shopBannerEdit")
    public void  shopBannerEdit(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //String bannerId = reqMap.get("banner_id").toString();
        //根据类型获取课程链接
        String linkType = reqMap.get("link_type").toString();
        if(linkType!=null){
            String linkTo = getLinkTo(reqMap,linkType);
            reqMap.put("link_to",linkTo);
        }
        saaSModuleServer.updateBanner(reqMap);
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
        int i = saaSModuleServer.updateBanner(reqMap);
        if(i==1){
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
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //获取请求查看的shop_id
        
        //添加状态要求：上架
        reqMap.put("status", "1");
        reqMap.put("app_name", reqEntity.getAppName());
        
        List<Map<String, Object>> bannerList = saaSModuleServer.getShopBannerListForFront(reqMap);
        
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
//        reqMap.put("user_id",userId);
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取缓存jedis
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        //获取分页标识
        String lastSeriesId = reqMap.get("last_series_id").toString(); 
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        
        /*
         * 从缓存中获取店铺信息
         */
        Map<String, String> shopMap = CacheUtils.readShop(shopId, reqEntity, readShopOperation, jedis);
        if(shopMap == null || shopMap.isEmpty()){
        	logger.error("saas店铺-获取店铺系列课程列表>>>>请求的店铺不存在");
        	throw new QNLiveException("190001");
        }
        //获取讲师id
        String lecturerId = shopMap.get("user_id");
        
        //根据讲师id查询缓存中讲师已上架系列课，需要传递分页标识
        Set<String> seriesSet = CacheUtils.readLecturerSeriesUp(lecturerId, lastSeriesId, pageCount, jedis);
        if(seriesSet != null){
        	logger.info("saas店铺-获取店铺系列课程列表>>>>从缓存中获取到讲师的上架系列课");
        	String seriesDetailKey = null;	//获取系列课程详情的key
        	
        	//生成用于缓存不存在时调用数据库的requestEntity
        	Map<String, Object> readSeriesMap = new HashMap<String, Object>();
        	RequestEntity readSeriesReqEntity = this.generateRequestEntity(null, null, "findSeriesBySeriesId", readSeriesMap);
        	
            for(String seriesId : seriesSet){
            	readSeriesMap.put("seried_id", seriesId);
            	//获取系列课程详情
            	Map<String, String> seriesMap = CacheUtils.readSeries(seriesId, readSeriesReqEntity, readSeriesOperation, jedis, true);
            	
            	boolean joinStatus = isJoinSeries(userId, seriesId, jedis);
            	
            	//判断是否加入了该课程
            	if(!joinStatus){	//用户未加入
            		seriesMap.put("is_join", "0");
            	}else{	//用户已购买
            		seriesMap.put("is_join", "1");
            	}
            	
            	seriesInfoList.add(seriesMap);
            }
        }
        
        resultMap.put("series_info_list", seriesInfoList);
        return resultMap;
	}  
    
    /**
     * H5_店铺-获取店铺单品直播课程列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findShopLiveSingleList")
    public Map<String, Object>  findShopLiveSingleList(RequestEntity reqEntity) throws Exception{
    	/*
    	 * 逻辑：
    	 * 1.讲师所有直播课程（同时包括预告和直播结束）没有缓存，所以直接查询数据库
    	 */
    	
    	//返回结果集
    	Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
//        reqMap.put("user_id",userId);
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取缓存jedis
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        //获取前端上一次刷新时的服务器时间
        long lastUpdateTime = (long) reqMap.get("last_update_time"); 
        //获取前端已经加载的数量
        long readedCount = (long) reqMap.get("readed_count"); 
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        //获取当前服务器时间
        Date now = new Date();
        
        /*
         * 从缓存中获取店铺信息
         */
        Map<String, String> shopMap = CacheUtils.readShop(shopId, reqEntity, readShopOperation, jedis);
        if(shopMap == null || shopMap.isEmpty()){
        	logger.error("saas店铺-获取店铺单品直播课程列表>>>>请求的店铺不存在");
        	throw new QNLiveException("190001");
        }
        //获取讲师id
        String lecturerId = shopMap.get("user_id");
        
        /*
         * 从数据库查询讲师的所有直播课程列表（同时包括预告和已完成）
         */
        reqMap.put("lecturer_id", lecturerId);
        reqMap.put("status", "1,2,4");	//用于sql的where `status` in (1：已发布 2:已结束 4直播中)
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
        reqMap.put("app_name", appName);
        
        List<Map<String, Object>> liveCourseList = saaSModuleServer.findLiveCourseListByMap(reqMap);
        List<Map<String, String>> resultLiveCourseList = new ArrayList<>();
        /*
         * 调用方法判断直播状态
         */
        if(liveCourseList != null){
        	Map<String, Object> selectIsStudentMap = new HashMap<String, Object>();	//用于判断是否加入课程
        	selectIsStudentMap.put("user_id", userId);
	        for(Map<String, Object> liveCourseMap : liveCourseList){
	        	Map<String, String> liveInfoMap = new HashMap<String, String>();
	        	MiscUtils.converObjectMapToStringMap(liveCourseMap, liveInfoMap);
	        	//进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
	        	MiscUtils.courseTranferState(now.getTime(), liveInfoMap);
	        	
	        	/*
        		 * 判断是否加入了课程
        		 */
	        	String courseId = liveCourseMap.get("course_id").toString();
        		boolean isStudent = isJoinLiveCourse(userId, courseId, jedis);
        		//加入课程状态 0未加入 1已加入
        		if(isStudent){
        			liveInfoMap.put("is_join", "1");
        		}else {
        			liveInfoMap.put("is_join", "0");
        		}
	        	
	        	resultLiveCourseList.add(liveInfoMap);
	        }
        }
        
        resultMap.put("live_info_list", resultLiveCourseList);
        return resultMap;
	} 

	/**
	 * 店铺-单品-添加视频、音频
	 * @param reqEntity
	 * @throws Exception
	 */
    @FunctionName("addShopSingleVideo")
    public void  addShopSingleVideo(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        reqMap.put("user_id",userId);
        //店铺信息
        Map<String, String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);

        String shopId = shopInfo.get("shop_id");
        reqMap.put("course_id",MiscUtils.getUUId());
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
        reqMap.put("course_updown","1");
        reqMap.put("series_course_updown","0");

        reqMap.put("app_name",reqEntity.getAppName());
        //插入课程
        saaSModuleServer.addCourse(reqMap);
        //缓存加入讲师创建的课程
        jedis.zadd(Constants.CACHED_KEY_COURSE_SAAS,now.getTime(),reqMap.get("course_id").toString());
    }

	/**
	 * 店铺-系列-添加课程
	 * @param reqEntity
	 * @throws Exception
	 */
    @FunctionName("addSeriesSingleCourse")
    public void  addSeriesSingleCourse(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String seriesId = (String) reqMap.get("series_id");
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        reqMap.put("user_id",userId);
        
        /*
         * 获取系列的详情
         */
        Map<String, Object> readSeriesMap = new HashMap<>();
        readSeriesMap.put("series_id", seriesId);
        RequestEntity readSeriesReqEntity = this.generateRequestEntity(null, null, "findSeriesBySeriesId", readSeriesMap);
        Map<String, String> seriesInfoMap = CacheUtils.readSeries(seriesId, readSeriesReqEntity, readSeriesOperation, jedis, true);
        if(MiscUtils.isEmpty(seriesInfoMap)){
        	log.error("saas_店铺-系列-添加课程>>>>系列课程不存在");
        	throw new QNLiveException("210004");
        }
        
        /*
         * 店铺信息
         */
        Map<String, String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);
        if(MiscUtils.isEmpty(shopInfo)){
        	log.error("saas_店铺-系列-添加课程>>>>店铺不存在");
        	throw new QNLiveException("190001");
        }
        
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
        reqMap.put("series_course_updown","1");

        reqMap.put("app_name",reqEntity.getAppName());
        //插入课程
        saaSModuleServer.addCourse(reqMap);
        log.error("saas_店铺-系列-添加课程>>>>数据库插入子课程成功");

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
        jedis.zadd(Constants.CACHED_KEY_COURSE_SAAS, now.getTime(),reqMap.get("course_id").toString());
        
        /*
         * 更新系列课已更新课程数量（数据库、缓存）
         */
        Map<String, Object> updateSeriesMap = new HashMap<>();
        updateSeriesMap.put("series_id", seriesId);
        updateSeriesMap.put("course_num", 
        		Integer.parseInt(seriesInfoMap.get("course_num")) + 1);	//已更新的课程数量，传1用于sql执行+1操作
        updateSeriesMap.put("update_course_time", now);
        saaSModuleServer.updateSeriesByMap(updateSeriesMap);
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
    	

    }

    /**
     * 店铺-单品-编辑视频、音频
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("editShopSingleVideo")
    public void editShopSingleVideo(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        if(reqMap.size()==1){
            throw new QNLiveException("000100");
        }
        Date now = new Date();
        reqMap.put("update_time",now);
        reqMap.put("app_name",reqEntity.getAppName());
        reqMap.put("course_price",reqMap.get("price"));
        //更新缓存前操作
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        Map<String,String> query = new HashMap<>();
        query.clear();
        String courseId = reqMap.get("course_id").toString();

        reqMap.put(Constants.CACHED_KEY_COURSE_FIELD,courseId);
        query.put(Constants.CACHED_KEY_COURSE_FIELD,courseId);
        //更新缓存
        String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, query);
        jedis.del(courseKey);

        RequestEntity entity = new RequestEntity();
        entity.setFunctionName("findSaasCourseByCourseId");
        entity.setParam(reqMap);
        //编辑课程
        saaSModuleServer.updateCourse(reqMap);

        CacheUtils.readCourse(courseId, entity, readCourseOperation,jedis,true);


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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        //获取分页标识
        String lastSingleId = reqMap.get("last_single_id").toString(); 
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        
        /*
         * 从缓存中获取店铺信息
         */
        Map<String, String> shopMap = CacheUtils.readShop(shopId, reqEntity, readShopOperation, jedis);
        if(shopMap == null || shopMap.isEmpty()){
        	logger.error("saas店铺-获取店铺单品课程（直播除外）列表>>>>请求的店铺不存在");
        	throw new QNLiveException("190001");
        }
        //获取讲师id
        String lecturerId = shopMap.get("user_id");
        
        //根据讲师id查询缓存中讲师已上架单品课，需要传递分页标识
        Set<String> singleSet = CacheUtils.readLecturerSingleNotLiveUp(lecturerId, lastSingleId, pageCount,reqEntity,readSingleListOperation,jedis);
        if(singleSet != null){
        	logger.info("saas店铺-获取店铺单品课程（直播除外）列表>>>>从缓存中获取到讲师的上架单品课（直播除外）");
        	String singleDetailKey = null;	//获取单品课程详情的key
        	
        	//生成用于缓存不存在时调用数据库的requestEntity
        	Map<String, Object> readSaasCourseMap = new HashMap<>();
        	RequestEntity readSaasCourseReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readSaasCourseMap);
        	
            for(String singleId : singleSet){
            	readSaasCourseMap.put("course_id", singleId);
            	//获取系列课程详情
            	Map<String, String> singleMap = CacheUtils.readCourse(singleId, readSaasCourseReqEntity, readCourseOperation, jedis, true);
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
     * H5_课程-获取系列课程详情
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("findSeriesCourseDetail")
    public Map<String, Object>  findSeriesCourseDetail(RequestEntity reqEntity) throws Exception{
    	//返回结果集
    	Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        //获取请求查看的series_id
        String seriesId = (String) reqMap.get("series_id");
        //获取缓存jedis
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        
        //生成用于缓存不存在时调用数据库的requestEntity
    	Map<String, Object> readSeriesMap = new HashMap<String, Object>();
    	RequestEntity readSeriesReqEntity = this.generateRequestEntity(null, null, "findSeriesBySeriesId", readSeriesMap);
    	readSeriesMap.put("series_id", seriesId);
		//获取系列课程详情
		Map<String, String> seriesMap = CacheUtils.readSeries(seriesId, readSeriesReqEntity, readSeriesOperation, jedis, true);
		
		/*
		 * 判断是否加入了该系列
		 */
		boolean isJoin = isJoinSeries(userId, seriesId, jedis);
		if(isJoin){	//已经加入
			resultMap.put("is_join", "1");
			resultMap.put("is_bought", "1");
		}else{
			resultMap.put("is_join", "0");
			boolean isBought = isBuySeries(userId, seriesId, jedis);
			if(isBought){
				resultMap.put("is_bought", "1");
			}else{
				resultMap.put("is_bought", "0");
			}
		}
		
        resultMap.put("series_info", seriesMap);
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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
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
        Set<String> courseSet = CacheUtils.readSeriesCourseUp(seriesId, lastCourseId, pageCount, jedis);
        if(courseSet != null){
        	logger.info("saas课程-获取系列课程内容课程列表>>>>从缓存中获取到系列的内容课程列表");
        	String courseDetailKey = null;	//获取内容课程详情的key
        	
        	//生成用于缓存不存在时调用数据库的requestEntity
        	Map<String, Object> readCourseMap = new HashMap<>();
        	RequestEntity readCourseReqEntity = null;
        	if("0".equals(seriesType)){	//直播类型的系列课，从t_course中查询
        		logger.info("saas课程-获取系列课程内容课程列表>>>>请求的系列为直播类型");
        		readCourseReqEntity = this.generateRequestEntity(null, null, "findCourseByCourseId", readCourseMap);
        		
        		for(String courseId : courseSet){
                	readCourseMap.put("course_id", courseId);
                	//获取课程详情
                	Map<String, String> courseMap = CacheUtils.readCourse(courseId, readCourseReqEntity, readCourseOperation, jedis, true);
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
        		logger.info("saas课程-获取系列课程内容课程列表>>>>请求的系列为非直播类型");
        		readCourseReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readCourseMap);
        		
        		for(String courseId : courseSet){
                	readCourseMap.put("course_id", courseId);
                	//获取课程详情
                	Map<String, String> courseMap = CacheUtils.readCourse(courseId, readCourseReqEntity, readCourseOperation, jedis, true);
                	
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
     * 店铺-单品列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getSingleList")
    public Map<String, Object> getSingleList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);//saaSModuleServer.getShopInfo(param);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        return saaSModuleServer.getSingleList(reqMap);

    }
    /**
     * 店铺-单品列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getNoSeriesList")
    public Map<String, Object> getNoSeriesList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);//saaSModuleServer.getShopInfo(param);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        //只查单品
        reqMap.put("noseries","1");
        return saaSModuleServer.getSingleList(reqMap);

    }
    /**
     * 店铺-直播列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getLiveList")
    public Map<String, Object> getLiveList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        return saaSModuleServer.getLiveList(reqMap);

    }

    /**
     * 店铺-用户列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getUserList")
    public Map<String, Object> getUserList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);//saaSModuleServer.getShopInfo(param);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        //TODO 付费用户存入缓存   其他普通和所有放入数据库
        Map<String, Object> userList = saaSModuleServer.getShopUsers(reqMap);
        return userList;
    }

    /**
     * 店铺-消息列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getMessageList")
    public Map<String, Object> getMessageList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);//saaSModuleServer.getShopInfo(param);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        //获取所有课程评论列表
        Map<String, Object> userList = saaSModuleServer.getCourseComment(reqMap);
        return userList;
    }
    /**
     * 店铺-消息列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getFeedbackList")
    public Map<String, Object> getFeedbackList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id",userId);
        Map<String,String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);//saaSModuleServer.getShopInfo(param);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        //获取所有课程评论列表
        Map<String, Object> userList = saaSModuleServer.getUserFeedBack(reqMap);
        return userList;
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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        
        //生成用于缓存不存在时调用数据库的requestEntity
    	Map<String, Object> readSingleMap = new HashMap<String, Object>();
    	//从产品原型上看，调用该接口获取单品课程详情的来源只有非直播课程，所以直接从t_saas_course查找
    	RequestEntity readSingleReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readSingleMap);
    	readSingleMap.put("course_id", singleId);
		//获取课程详情
		Map<String, String> singleMap = CacheUtils.readCourse(singleId, readSingleReqEntity, readCourseOperation, jedis, true);
		if(singleMap == null || singleMap.isEmpty()){
			logger.error("saas_H5_课程-获取单品课程详情>>>>课程不存在");
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
		
		/*
         * Saas课程的浏览量+1，更新缓存和数据库
         */
        /*Map<String, Object> updateCourseMap = new HashMap<>();
        updateCourseMap.put("course_id", singleId);
        updateCourseMap.put("click_num", 1);	//点击次数，置1是用于sql执行加1操作
        updateCourseMap.put("series_id", singleMap.get("series_id"));	//临时解决bug的方法，因为若series_id=null,sql会将series_id置空
        //更新数据库
        saaSModuleServer.updateCourse(updateCourseMap);*/


        //更新缓存
        Map<String, Object> readCourseMap = new HashMap<String, Object>();
        readCourseMap.put(Constants.CACHED_KEY_COURSE_FIELD, singleId);
        String readCourseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, readCourseMap);
        jedis.hincrBy(readCourseKey, "click_num", 1);	//缓存中点击数+1

        resultMap.put("single_info", singleMap);
        return resultMap;
	}

    /**
     * 店铺-单品上下架（未用到）
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("goodsSingleUpdown")
    public void  goodsSingleUpdown(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String type = reqMap.get("course_updown").toString();
        String courseId = reqMap.get("course_id").toString();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
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
        saaSModuleServer.updateCourse(reqMap);
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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        
        /*
         * 判断是否加入了课程
         */
        boolean isStudent = isJoinCourse(userId, articleId, jedis);
        if(!isStudent){	//未购买
        	log.error("Saas_H5_课程-获取图文课程内容>>>>用户未加入该课程(articleId=" + articleId + ")");
        	throw new QNLiveException("120007");
        }
        
        //生成用于缓存不存在时调用数据库的requestEntity
    	Map<String, Object> readArticleMap = new HashMap<String, Object>();
    	//从产品原型上看，调用该接口为图文课程点播，所以直接从t_saas_course查找
    	RequestEntity readArticleReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readArticleMap);
    	readArticleMap.put("course_id", articleId);
		//获取课程详情
		Map<String, String> articleMap = CacheUtils.readCourse(articleId, readArticleReqEntity, readCourseOperation, jedis, true);
		if(articleMap == null){
			logger.error("saas_H5_课程-获取图文课程内容>>>>课程不存在");
			throw new QNLiveException("100004");
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
		String shareUrl = MiscUtils.getConfigByKey("saas_course_share_url_pre_fix", appName) + articleId;
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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        
        /*
         * 判断是否加入了课程
         */
        boolean isStudent = isJoinCourse(userId, courseId, jedis);
        if(!isStudent){	//未购买
        	log.error("Saas_H5_课程-获取课程内容（音频或视频）>>>>用户未加入该课程(courseId=" + courseId + ")");
        	throw new QNLiveException("120007");
        }
        
        //生成用于缓存不存在时调用数据库的requestEntity
    	Map<String, Object> readCourseMap = new HashMap<String, Object>();
    	//从产品原型上看，调用该接口为非直播类课程点播，所以直接从t_saas_course查找
    	RequestEntity readCourseReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readCourseMap);
    	readCourseMap.put("course_id", courseId);
		//获取课程详情
		Map<String, String> courseMap = CacheUtils.readCourse(courseId, readCourseReqEntity, readCourseOperation, jedis, true);
		if(readCourseMap == null){
			logger.error("saas_H5_课程-获取课程内容（音频或视频）>>>>课程不存在");
			throw new QNLiveException("100004");
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
		String shareUrl = MiscUtils.getConfigByKey("saas_course_share_url_pre_fix", appName) + courseId;
		resultMap.put("share_url", shareUrl);
		
        resultMap.put("course_info", courseMap);
        
        return resultMap;
	}


    /**
     * 店铺-系列列表
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
        Map<String, Object> userList = saaSModuleServer.getSeriesList(reqMap);
        return userList;
    }

    /**
     * 店铺-系列-详情
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getSeriesInfo")
    public Map<String, String> getSeriesInfo(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Map<String, String> resultMap = null;
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_SERIES_FIELD, reqMap.get("series_id").toString());
        String seriesKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES, map);
        //1.先从缓存中查询课程详情，如果有则从缓存中读取课程详情
        if (jedis.exists(seriesKey)) {
           resultMap = jedis.hgetAll(seriesKey);
        } else {
            //2.如果缓存中没有课程详情，则读取数据库
            Map<String, Object> seriesDBMap = saaSModuleServer.findSeriesBySeriesId(reqMap.get("series_id").toString());
            if(seriesDBMap==null){
                throw new QNLiveException("210004");
            }
            MiscUtils.converObjectMapToStringMap(seriesDBMap, resultMap);
        }
        return resultMap;
    }

    /**
     * 店铺-系列-详情
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getSeriesCourseList")
    public Map<String, Object> getSeriesCourseList(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.CACHED_KEY_SERIES_FIELD, reqMap.get("series_id").toString());
        String seriesId = reqMap.get("series_id").toString();
        //系列详情
        Map<String,String> seriesInfo = CacheUtils.readSeries(seriesId, reqEntity, readSeriesOperation,jedis,true);
        //系列类型，直播-商品
        reqMap.put("series_course_type",seriesInfo.get("series_course_type"));
        return saaSModuleServer.getSeriesCourseList(reqMap);
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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        //获取分页标识
        String lastMessageId = reqMap.get("last_message_id").toString(); 
        //获取每页数量
        int pageCount = Integer.parseInt(reqMap.get("page_count").toString());
        //课程留言列表
        List<Map<String, String>> messageInfoList = new ArrayList<>();
        
        /*
         * 判断是否加入了课程
         */
        boolean isStudent = isJoinCourse(userId, courseId, jedis);
        if(!isStudent){	//未购买
        	log.error("Saas_H5_课程-获取课程留言列表>>>>用户未加入该课程(courseId=" + courseId + ")");
        	throw new QNLiveException("120007");
        }
        
    	/*
         * 根据课程id查询缓存中课程的留言id列表，需要传递分页标识
         */
        //read课程的留言列表，以创建时间倒序排序
        Set<String> messageSet = CacheUtils.readCourseMessageSet(courseId, lastMessageId, pageCount, jedis);
        if(messageSet != null){
        	logger.info("saas课程-获取课程留言列表>>>>从缓存中获取到课程（course_id=" + courseId + "）的留言列表");
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
            	Map<String, String> messageMap = CacheUtils.readSaasCourseComment(searchKeys, readMessageReqEntity, readSaasCourseMessageOperation, jedis, true);
            	
            	messageInfoList.add(messageMap);
            }
        }
        
        resultMap.put("message_info_list", messageInfoList);
        return resultMap;
    }

	/**
	 * 店铺-单品-详情
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("getShopSingleInfo")
    public Map<String, String> getShopSingleInfo(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);//获取jedis对象
        //从缓存获取课程详情
        RequestEntity readSingleReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", reqMap);
        Map<String,String> map = CacheUtils.readCourse(reqMap.get("course_id").toString(), readSingleReqEntity, readCourseOperation,jedis,true);
        return map;
    }
    /**
     * 店铺-单品-详情
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
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        reqMap.put("user_id",userId);
        //店铺信息
        Map<String, String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);
        reqMap.put("shop_id",shopInfo.get("shop_id"));
        String type = reqMap.get("type").toString();
        Map<String,Object> result = null;
        if("1".equals(type)){
            //单品课程
            //2:图文，3:音频，4:视频
            int typeInt = Integer.valueOf(type)-1;
            reqMap.put("type",typeInt+"");
            result = saaSModuleServer.findUpCourseList(reqMap);
        }else if("2".equals(type)){
            //系列课
            result = saaSModuleServer.findUpLiveCourseList(reqMap);
        }else if("4".equals(type)){
            //语音直播
            result = saaSModuleServer.findUpLiveCourseList(reqMap);
        }
        return result;
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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        Date now = new Date();
        
        /*
         * 判断是否加入了课程
         */
        boolean isStudent = isJoinCourse(userId, courseId, jedis);
        if(!isStudent){	//未购买
        	log.error("Saas_H5_课程-添加课程留言>>>>用户未加入该课程(courseId=" + courseId + ")");
        	throw new QNLiveException("120007");
        }
        
        /*
         * 获得课程信息，主要用于后续获取店铺id进行存储
         */
        Map<String, Object> readCourseMap = new HashMap<String, Object>();
        readCourseMap.put("course_id", courseId);
        RequestEntity readCourseReqEntity = this.generateRequestEntity(null, null, "findSaasCourseByCourseId", readCourseMap);
        
        Map<String, String> courseInfoMap = CacheUtils.readCourse(courseId, readCourseReqEntity, readCourseOperation, jedis, true);
        if(courseInfoMap == null || courseInfoMap.isEmpty()){
        	logger.error("saas课程-添加课程留言>>>>课程不存在");
        	throw new QNLiveException("100004");
        }
        
        /*
         * 获取登录用户信息，主要用于后续获取用户昵称等
         */
        reqMap.put("user_id", userId);
        Map<String, String> loginedUserMap = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
        if(loginedUserMap == null || loginedUserMap.isEmpty()){
        	logger.error("saas课程-添加课程留言>>>>登录用户不存在");
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
        saaSModuleServer.addSaasCourseComment(insertCommentMap, updateCourseMap, updateSaasShopUserMap);
        
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
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        //获取当前服务器时间
        Date now = new Date();
        
        /*
         * 从缓存中获取登录用户信息
         */
        Map<String, String> loginedUserMap = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedis);
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
        saaSModuleServer.addFeedback(newFeedbackMap);
        
        return resultMap;
    }
    /**
     * 系列-获取系列课的消费用户列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getSeriesUserList")
    public Map<String, Object> getSeriesUserList(RequestEntity reqEntity) throws Exception{
    	//返回结果集
    	Map<String, Object> resultMap = new HashMap<>();


        return resultMap;
    }

    /**
     * 用户-余额明细
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("userGains")
    public Map<String, Object> userGains(RequestEntity reqEntity) throws Exception{
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> userGains = saaSModuleServer.findUserGainsByUserId(userId);
        long todayMoney = 0L;
        long todayVisit = 0L;
        long todayPay = 0L;
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
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
        double balance = Double.valueOf(userGains.get("balance").toString());

        balance = DoubleUtil.sub(balance , DoubleUtil.mul(balance,Constants.SYS_WX_RATE));
        userGains.put("balance",balance);

        if(MiscUtils.isEmpty(userGains)){
            throw new QNLiveException("170001");
        }
        return userGains;
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
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //店铺信息
        Map<String, String> shopInfo = CacheUtils.readShop(shopId, reqEntity, readShopOperation, jedis);
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
            saaSModuleServer.userVisitShop(userId,shopId);
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
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //店铺信息
        Map<String, String> shopInfo = CacheUtils.readShop(shopId, reqEntity, readShopOperation, jedis);
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

        //店主ID
        query.put("lecturer_id",shopInfo.get("user_id"));
        query.put("user_id",userId);
        query.put("type","1");
        if(query.get("position") == null||StringUtils.isEmpty(query.get("position").toString())){
            query.remove("position");
        }
        List<Map<String,Object>> records = saaSModuleServer.findUserBuiedRecords(query);

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
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //店铺信息
        Map<String, String> shopInfo = CacheUtils.readShop(shopId, reqEntity, readShopOperation, jedis);
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());

        //店主ID
        query.put("lecturer_id",shopInfo.get("user_id"));
        query.put("user_id",userId);
        query.put("type","2");
        if(query.get("position") == null||StringUtils.isEmpty(query.get("position").toString())){
            query.remove("position");
        }
        List<Map<String,Object>> records = saaSModuleServer.findUserBuiedRecords(query);

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
     * 店铺-订单记录
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("gainsOrdersList")
    public Map<String, Object> gainsOrdersList(RequestEntity reqEntity) throws Exception{
        Map<String,Object> query =  (Map<String, Object>)reqEntity.getParam();
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String, Object> resultMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象
        //店铺信息
        //Map<String, String> shopInfo = CacheUtils.readShopByUserId(userId, reqEntity, readShopOperation, jedis);
        //String shopId = shopInfo.get("shop_id").toString();

        //店主ID
        query.put("lecturer_id",userId);
        //query.put("user_id",userId);
        //query.put("profit_type","2");

        Map<String,Object> records = saaSModuleServer.getOrdersList(query);
        List<Map<String,Object>> list = (List<Map<String,Object>>)records.get("list");

        if(list!=null&&!list.isEmpty()){
                Map<String,Object> cacheQueryMap = new HashMap<>();
                JedisBatchCallback callBack = (JedisBatchCallback)jedis;
                //从缓存中查询讲师的名字
                callBack.invoke(new JedisBatchOperation(){
                    @Override
                    public void batchOperation(Pipeline pipeline, Jedis jedis) {
                        for(Map<String,Object> recordMap : list){
                            cacheQueryMap.put(Constants.CACHED_KEY_LECTURER_FIELD, recordMap.get("user_id"));
                            String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, cacheQueryMap);
                            Response<String> nackName = pipeline.hget(lecturerKey, "nick_name");
                            Response<String> userAvatar = pipeline.hget(lecturerKey, "avatar_address");
                            recordMap.put("cacheLecturerName",nackName);
                            recordMap.put("userAvatar",userAvatar);
                        }
                        for(Map<String,Object> recordMap : list){
                            cacheQueryMap.clear();
                            cacheQueryMap.put(Constants.CACHED_KEY_COURSE_FIELD, recordMap.get("course_id"));
                            String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, cacheQueryMap);
                            Response<String> courseName = pipeline.hget(courseKey, "course_title");
                            Response<String> coursePrice = pipeline.hget(courseKey, "course_price");
                            recordMap.put("courseTitle", courseName);
                            recordMap.put("coursePrice", coursePrice);
                        }
                        pipeline.sync();

                        for(Map<String,Object> recordMap : list){
                            Response<String> cacheLecturerName = (Response)recordMap.get("cacheLecturerName");
                            Response<String> userAvatar = (Response)recordMap.get("userAvatar");

                            Response<String> courseName = (Response)recordMap.get("courseTitle");
                            Response<String> coursePrice = (Response)recordMap.get("coursePrice");
                            recordMap.put("nick_name",cacheLecturerName.get());
                            recordMap.put("user_avatar",userAvatar.get());
                            if(recordMap.get("goods_name")==null) {
                                recordMap.put("goods_name", courseName.get());
                                recordMap.put("price", coursePrice.get());
                            }
                            Date recordTime = (Date)recordMap.get("create_time");
                            recordMap.put("create_time", recordTime);
                        }
                    }
                });

                resultMap.put("list", list);

        }
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
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //获取登录用户user_id
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        //获取请求查看的shop_id
        String shopId = (String) reqMap.get("shop_id");
        //获取缓存jedis
        String appName = reqEntity.getAppName();
        Jedis jedis = jedisUtils.getJedis(appName);
        
        Map<String, String> shopInfoMap = CacheUtils.readShop(shopId, reqEntity, readShopOperation, jedis);
        resultMap.put("shop_info", shopInfoMap);
        
        return resultMap;
	}
    /**
     * 绑定手机号码（校验手机号码）
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("bindPhone")
    public Map<String, Object>  bindPhone(RequestEntity reqEntity) throws Exception{
        Map<String,String> reqMap = (Map<String, String>) reqEntity.getParam();
        String verification_code = reqMap.get("code");
        String phone = reqMap.get("phone");
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());//获取jedis对象

        if(CodeVerifyficationUtil.verifyVerificationCode(reqEntity.getAppName(),userId, verification_code, jedis)){
            //if("0000".equals(verification_code)){
            logger.info("绑定手机号码>>验证码验证通过");
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("user_id", userId);
            userMap.put("phone_number", phone);
            if(saaSModuleServer.updateUserPhone(userMap)>0){
                throw new QNLiveException("130008");
            }
            //清空用户缓存
            Map<String,Object> userQuery = new HashMap<String,Object>();
            userQuery.put(Constants.CACHED_KEY_USER_FIELD, userId);
            String userkey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, userQuery);
            jedis.hset(userkey,"phone_number",phone);
        }else{
            throw new QNLiveException("130002");
        }
        return null;
	}


}