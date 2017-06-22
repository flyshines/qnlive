package qingning.saas.server.imp;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.saas.server.other.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ISaaSModuleServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.net.URLEncoder;
import java.util.*;

public class SaaSServerImpl extends AbstractQNLiveServer {
    private static Logger log = LoggerFactory.getLogger(SaaSServerImpl.class);

    private ISaaSModuleServer saaSModuleServer;

    private ReadUserOperation readUserOperation;

    private ReadShopOperation readShopOperation;
    private ReadSeriesOperation readSeriesOperation;
    private ReadCourseOperation readCourseOperation;
    private static Logger logger = LoggerFactory.getLogger(SaaSServerImpl.class);

    @Override
    public void initRpcServer() {
        if (saaSModuleServer == null) {
            saaSModuleServer = this.getRpcService("saaSModuleServer");
            readUserOperation = new ReadUserOperation(saaSModuleServer);
            readShopOperation = new ReadShopOperation(saaSModuleServer);
            readSeriesOperation = new ReadSeriesOperation(saaSModuleServer);
            readCourseOperation = new ReadCourseOperation(saaSModuleServer);

        }
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
	 * 扫码登录 2 登录
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
    @FunctionName("wechatCheckLogin")
    public Map<String, Object>  wechatCheckLogin(RequestEntity reqEntity) throws Exception{
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String code = (String) reqMap.get("code");

        log.debug("------微信SaaS登录授权回调------"+reqMap);

        //根据微信回调的URL的参数去获取公众号的接口调用凭据和授权信息
        Jedis jedis = jedisUtils.getJedis(Constants.HEADER_APP_NAME);//获取jedis对象

        String access_token = jedis.hget(Constants.SERVICE_NO_ACCESS_TOKEN, "component_access_token");

        JSONObject authJsonObj = WeiXinUtil.getServiceAuthInfo(access_token, code,Constants.HEADER_APP_NAME );
        Object errCode = authJsonObj.get("errcode");
        if (errCode != null ) {
            log.error("微信授权回调 获取授权信息失败-----------"+authJsonObj);
            throw new QNLiveException("150001");
        }

        JSONObject authauthorizer_info = authJsonObj.getJSONObject("authorization_info");

        String authorizer_appid = authauthorizer_info.getString("authorizer_appid");

        //获取公众号的头像 昵称 QRCode等相关信息
        JSONObject serviceNoJsonObj = WeiXinUtil.getServiceAuthAccountInfo(access_token, authorizer_appid,Constants.HEADER_APP_NAME);
        errCode = serviceNoJsonObj.get("errcode");
        if (errCode != null ) {
            log.error("微信授权回调 获取服务号相关信息失败-----------"+authJsonObj);
            throw new QNLiveException("150001");
        }
        Map<String,Object> result = new HashMap<String,Object>();//返回重定向的url
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
        if(shop == null){
            //店铺不存在
            throw new QNLiveException("190001");
        }
        shop.put("user_id",userId);
        shop.put("nick_name",userMap.get("nick_name"));
        shop.put("phone_num",userMap.get("phone_number"));
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
        String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        Map<String,Object> param = (Map<String, Object>) reqEntity.getParam();
        param.put("user_id",userId);
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
        reqMap.put("link_to",linkTo);

        reqMap.put("create_time",new Date());
        reqMap.put("app_name",reqEntity.getAppName());
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
        String linkTo = null;
        if("1".equals(linkType)){
            //外部链接
            if(reqMap.get("link_id")==null){
                //链接为空判断
                throw new QNLiveException("000004");
            }
            linkTo = reqMap.get("link_type").toString();
        }else {
            if(reqMap.get("link_id")==null){
                //课程ID为空判断
                throw new QNLiveException("000004");
            }
            //课程，系列ID
            String linkId = reqMap.get("link_id").toString();
            if("2".equals(linkType)){
                //单品ID
                linkTo = "/course/single/detail/"+linkId;
            }else if("3".equals(linkType)){
                //系列ID
                linkTo = "/course/series/detail/"+linkId;
            }else if("3".equals(linkType)){
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
        saaSModuleServer.updateBanner(reqMap);
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
        reqMap.put("user_id",userId);
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
        	logger.error("saas店铺-获取店铺系列课程列表>>>>从缓存中获取到讲师的上架系列课");
        	String seriesDetailKey = null;	//获取系列课程详情的key
        	
        	/*
        	 * 获取用户购买的所有系列课程
        	 * 返回 -> 键series_id；值1
        	 */
        	Map<String, Object> selectSeriesStudentsMap = new HashMap<String, Object>();
        	selectSeriesStudentsMap.put("user_id", userId);
        	selectSeriesStudentsMap.put("lecturer_id", lecturerId);
        	List<Map<String, Object>> userSeriesList = saaSModuleServer.findSeriesStudentsByMap(selectSeriesStudentsMap);
        	
        	/*
        	 * 对用户购买的所有系列课程进行格式化成map，方便后期用seriesId进行查询
        	 */
        	Map<String, String> seriesIdKeyMap = new HashMap<>();	//以seriesId做key，存储用户购买过的所有系列课
        	for(Map<String, Object> userSeriesMap : userSeriesList){
        		seriesIdKeyMap.put(userSeriesMap.get("series_id").toString(), "1");
        	}
        	
            for(String seriesId : seriesSet){
            	reqMap.put("seried_id", seriesId);
            	//获取系列课程详情
            	Map<String, String> seriesMap = CacheUtils.readSeries(seriesId, reqEntity, readSeriesOperation, jedis, true);
            	
            	//判断是否购买了该课程
            	if(seriesIdKeyMap == null || seriesIdKeyMap.get(seriesId) == null){	//用户未购买
            		seriesMap.put("buy_status", "0");
            	}else{	//用户已购买
            		seriesMap.put("buy_status", "1");
            	}
            	
            	seriesInfoList.add(seriesMap);
            }
        }
        
        resultMap.put("series_info_list", seriesInfoList);
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
        //收益初始化
        reqMap.put("extra_amount",0);
        reqMap.put("extra_num",0);
        reqMap.put("sale_num",0);
        reqMap.put("goods_type",reqMap.get("type"));
        reqMap.put("course_amount",0);

        //默认下架
        reqMap.put("course_updown","2");
        reqMap.put("series_course_updown","2");

        reqMap.put("app_name",reqEntity.getAppName());
        //插入课程
        saaSModuleServer.addCourse(reqMap);
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
        //插入课程
        saaSModuleServer.updateCourse(reqMap);
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
        Set<String> singleSet = CacheUtils.readLecturerSingleNotLiveUp(lecturerId, lastSingleId, pageCount, jedis);
        if(singleSet != null){
        	logger.error("saas店铺-获取店铺单品课程（直播除外）列表>>>>从缓存中获取到讲师的上架单品课（直播除外）");
        	String singleDetailKey = null;	//获取单品课程详情的key
        	
        	/*
        	 * 获取用户购买的所有单品课程
        	 * 返回 -> 键course_id；值1
        	 */
        /*	Map<String, Object> selectSeriesStudentsMap = new HashMap<String, Object>();
        	selectSeriesStudentsMap.put("user_id", userId);
        	selectSeriesStudentsMap.put("lecturer_id", lecturerId);
        	List<Map<String, Object>> userSeriesList = saaSModuleServer.findSeriesStudentsByMap(selectSeriesStudentsMap);
        	
        	
        	 * 对用户购买的所有系列课程进行格式化成map，方便后期用seriesId进行查询
        	 
        	Map<String, String> seriesIdKeyMap = new HashMap<>();	//以seriesId做key，存储用户购买过的所有系列课
        	for(Map<String, Object> userSeriesMap : userSeriesList){
        		seriesIdKeyMap.put(userSeriesMap.get("series_id").toString(), "1");
        	}
        */	
            for(String singleId : singleSet){
            	reqMap.put("course_id", singleId);
            	//获取系列课程详情
            	Map<String, String> singleMap = CacheUtils.readCourse(singleId, reqEntity, readCourseOperation, jedis, true);
            	
            	//判断是否购买了该课程
            /*	if(seriesIdKeyMap == null || seriesIdKeyMap.get(seriesId) == null){	//用户未购买
            		seriesMap.put("buy_status", "0");
            	}else{	//用户已购买
            		seriesMap.put("buy_status", "1");
            	}
            */	
            	singleInfoList.add(singleMap);
            }
        }
        
        resultMap.put("single_info_list", singleInfoList);
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
        Map<String, Object> resultMap = (Map)reqEntity.getParam();
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
     * 店铺-直播列表
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getLiveList")
    public Map<String, Object> getLiveList(RequestEntity reqEntity) throws Exception{
        return null;
    }
}