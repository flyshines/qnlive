package qingning.common.server.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import qingning.common.entity.AccessToken;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.server.util.ServerUtils;
import qingning.common.util.HttpTookit;
import qingning.common.util.MiscUtils;
import qingning.common.util.WeiXinUtil;
import qingning.server.AbstractController;

import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
public class CommonController extends AbstractController {

    private static final Logger logger   = LoggerFactory.getLogger(CommonController.class);

    /**
     * 获取系统时间
     *
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping(value = "/common/client/information", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity collectClientInformation(HttpEntity<Object> entity,
    		@RequestHeader(value="access_token", defaultValue="") String accessToken) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "logUserInfo", accessToken, null);
		Map inputParameters = (Map)entity.getBody();
        inputParameters.put("ip", ServerUtils.getRequestIP());
        if(inputParameters.containsKey("login_id")){
        	inputParameters.put("union_id", inputParameters.get("login_id"));
        }
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * 获取系统时间
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/time", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getServerTime(
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "serverTime", null, null);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * 用户登录
     *
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/common/login", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity userLogin(
            HttpEntity<Object> entity,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "userLogin", null, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);

        //根据相关条件将server_url列表信息返回
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        Map<String, Object> bodyMap = (Map<String, Object>) entity.getBody();
        if (bodyMap.get("server_url_update_time") == null ||
                !bodyMap.get("server_url_update_time").toString().equals(serverUrlInfoUpdateTime.toString())) {
            resultMap.put("server_url_info_list", serverUrlInfoMap);
            resultMap.put("server_url_info_update_time", serverUrlInfoUpdateTime);
        }
        responseEntity.setReturnData(resultMap);
        return responseEntity;
    }

    /**
     * 微信授权code登录
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/weixin/code/login", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity weixinCodeUserLogin(
            HttpEntity<Object> entity,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "weixinCodeUserLogin", null, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);

        //根据相关条件将server_url列表信息返回
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        Map<String, Object> bodyMap = (Map<String, Object>) entity.getBody();
        if (bodyMap.get("server_url_update_time") == null || !bodyMap.get("server_url_update_time").toString().equals(serverUrlInfoUpdateTime.toString())) {
            resultMap.put("server_url_info_list", serverUrlInfoMap);
            resultMap.put("server_url_info_update_time", serverUrlInfoUpdateTime);
        }
        responseEntity.setReturnData(resultMap);
        return responseEntity;
    }

    /**
     * 测试 微信公众号授权后的回调
     * 前端 默认进行 微信静默授权
     *  授权回调进入后台在后台获取code进行判断时候获取 openid
     *  如果有就进行正常跳转
     *  如果没有就进行手动授权
     *
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/common/weixin/weixinlogin", method = RequestMethod.GET)
    public void weixinLogin(HttpServletRequest request,HttpServletResponse response) throws Exception {

        StringBuffer url = request.getRequestURL();//获取路径
        Map<String, String[]> params = request.getParameterMap();
        String[] codes = params.get("code");//拿到的code的值
        String code = codes[0];
        Map<String,String> map = new HashMap<>();
        map.put("code",code);

        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "weixinLogin", null, "");
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Integer subscribe = Integer.valueOf((String)resultMap.get("subscribe"));
        if(subscribe == 0){//如果没有关注公众号
            logger.info("没有关注我们微信,跳转至关注页面");
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_auth_redirect_url"));
            return ;
        }

        String userWeixinAccessToken = (String) resultMap.get("access_token");
        logger.info("微信Access_token"+userWeixinAccessToken);
        Map<String, String> param = new HashMap<String, String>();
        param.put("token",userWeixinAccessToken);
        response.sendRedirect(MiscUtils.getConfigByKey("web_index")+userWeixinAccessToken);
        return ;
    }

    /**
     * 微信 开放平台 公众号消息与事件接受
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/common/weixin/weiXinSystemMsg", method = RequestMethod.GET)
    public void weiXinSystemMsg(HttpServletRequest request,HttpServletResponse response) throws Exception {

    }

    /**
     * 获得上传到七牛token
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/upload/token", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getQiNiuUploadToken(
            @RequestParam(value = "upload_type", defaultValue = "") String upload_type,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "qiNiuUploadToken", accessToken, version);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("upload_type", upload_type);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 获得微信JS端配置
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/weixin/configuration", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getWeiXinConfiguration(
            @RequestParam(value = "url", defaultValue = "") String url,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "weiXinConfiguration", null, version);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("url", url);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }


    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/common/user", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getUserInfo(
            @RequestParam(value = "query_type", defaultValue = "1") String query_type,
            @RequestParam(value = "server_url_info_update_time", defaultValue = "") String server_url_info_update_time,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "userInfo", accessToken, version);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("query_type", query_type);
        requestEntity.setParam(param);

        //根据相关条件将server_url列表信息返回
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        if("2".equals(query_type)){
	        if (MiscUtils.isEmpty(server_url_info_update_time) ||
	                !server_url_info_update_time.equals(serverUrlInfoUpdateTime.toString())) {
	            resultMap.put("server_url_info_list", serverUrlInfoMap);
	            resultMap.put("server_url_info_update_time", serverUrlInfoUpdateTime);
	        }
        }
        responseEntity.setReturnData(resultMap);
        return responseEntity;
    }

    /**
     * 生成微信支付单
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/payment/weixin/bill", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity generateWeixinPayBill(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version, HttpServletRequest request) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "generateWeixinPayBill", accessToken, version);
        String remote_ip_address = MiscUtils.getIpAddr(request);
        ((Map<String,Object>)entity.getBody()).put("remote_ip_address",remote_ip_address);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 处理微信支付回调
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/common/payment/weixin/result", method = RequestMethod.POST)
    public void handleWeixinPayResult(HttpServletRequest request, HttpServletResponse response) throws Exception{
        logger.info("================> 接收到 weixinNotify 通知。==================");
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = null;
        try{
            out =response.getWriter();
            String resultData = MiscUtils.convertStreamToString(request.getInputStream());
            SortedMap<String,String> requestMapData = MiscUtils.requestToMap(resultData);//notify请求数据
            logger.info("===> weixinNotify 请求数据：" + requestMapData);
            RequestEntity requestEntity = this.createResponseEntity("CommonServer", "handleWeixinPayResult", "", "");
            requestEntity.setParam(requestMapData);
            ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
            String result = responseEntity.getReturnData().toString();
            logger.info("===> result Data: " + result);
            out.println(result);
            out.flush();
            out.close();
        }catch(Exception e){
            logger.error("====================>  weixinNotify 处理异常  ========");
            logger.error(e.getMessage(), e);
            out.flush();
            out.close();
        }
        logger.info("==========================>  weixinNotify 处理完毕。 =======");
    }
	/**
	 * 编辑个人信息
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/common/user", method = RequestMethod.PUT)
    public @ResponseBody ResponseEntity updateUserInfo(
    		HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "updateUserInfo", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
	/**
	 * 查询个人的分销信息
	 * @param page_count 每页记录数（默认10）
	 * @param record_date 用户加入直播间分销的时间（分页时使用）
	 * @param access_token token 验证
	 * @param version 版本号
	 * @return ResponseEntity responseEntity
	 * @throws Exception
	 */
    @RequestMapping(value="/common/distribution",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getCommonDistribution(
    		@RequestParam(value="page_count",defaultValue="10") String page_count,
    		@RequestParam(value="position",defaultValue="") String position,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "commonDistribution", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("page_count", page_count);
        param.put("position", position);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
	/**
	 * 讲师查询分销员的推广用户/用户查询自己的推广用户
	 * @param room_id
	 * @param distributer_id
	 * @param page_count
	 * @param position
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value="/common/distribution/rooms/{room_id}/recommend",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getRoomDistributerRecommendInfo(
    		@PathVariable("room_id") String room_id,
    		@RequestParam(value="distributer_id",defaultValue="") String distributer_id,
    		@RequestParam(value="page_count",defaultValue="20") String page_count,
    		@RequestParam(value="position",defaultValue="") String position,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributerRecommendInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("room_id", room_id);
        param.put("distributer_id", distributer_id);
        param.put("page_count", page_count);
        param.put("position", position);        
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
	/**
	 * 用户查询自己对应指定的直播间的分销信息
	 * @param room_id
	 * @param page_count
	 * @param start_time
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value="/common/distribution/rooms/{room_id}/{rq_code}",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getRoomDistributionInfo(
    		@PathVariable("room_id") String room_id,
    		@PathVariable("rq_code") String rq_code,
    		@RequestParam(value="page_count",defaultValue="20") String page_count,
    		@RequestParam(value="start_time",defaultValue="") String start_time,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributionInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("room_id", room_id);
        param.put("rq_code", rq_code);
        param.put("start_time", start_time);
        param.put("page_count", page_count);        
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
	/**
	 * 用户查询具体课程的分销信息
	 * @param course_id
	 * @param page_count
	 * @param position
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value="/common/distribution/course/{course_id}",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getCourseDistributionInfo(
    		@PathVariable("course_id") String course_id,
    		@RequestParam(value="page_count",defaultValue="20") String page_count,
    		@RequestParam(value="position",defaultValue="") String position,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "courseDistributionInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("course_id", course_id);
        param.put("page_count", page_count);
        param.put("position", position);        
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
	/**
	 * 分销员分销直播间链接
	 * @param room_id
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value="/common/distribution/room/{room_id}/share",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getRoomDistributionShareInfo(
    		@PathVariable("room_id") String room_id,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributionShareInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("room_id", room_id);           
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 查询课程邀请卡信息
     * @param course_id
     * @param access_token
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/common/courses/{course_id}/card",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getCourseInviteCard(
            @PathVariable("course_id") String course_id,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseInviteCard", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("course_id", course_id);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 查询直播间邀请卡信息
     * @param room_id
     * @param access_token
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/common/rooms/{room_id}/card",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getRoomInviteCard(
            @PathVariable("room_id") String room_id,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getRoomInviteCard", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("room_id", room_id);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    @RequestMapping(value="/common/distributors/{recommend_code}/user",method=RequestMethod.PUT)
    public @ResponseBody ResponseEntity distributorsRecommendUser(
            @PathVariable("recommend_code") String recommend_code,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "distributorsRecommendUser", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("recommend_code", recommend_code);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 用户反馈
     * @param entity
     * @param access_token
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/common/feedback",method=RequestMethod.POST)
    public @ResponseBody ResponseEntity createFeedback(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "createFeedback", access_token, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    @RequestMapping(value="/common/weixin/resource/conversion",method=RequestMethod.POST)
    public @ResponseBody ResponseEntity convertWeixinResource(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "convertWeixinResource", access_token, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    @RequestMapping(value="/common/share",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getShareInfo(
            @RequestParam(value="query_type",defaultValue="") String query_type,
            @RequestParam(value="id",defaultValue="") String id,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getShareInfo", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("query_type", query_type);
        param.put("id", id);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 直播间中的分销课程的推荐用户列表
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "common/distribution/rooms/{room_id}/courses/{course_id}/recommend/users", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getCourseRecommendUsers(
            @PathVariable("room_id") String room_id,
            @PathVariable("course_id") String course_id,
            @RequestParam(value="page_count",defaultValue="20") String page_count,
            @RequestParam(value="student_pos",defaultValue="") String student_pos,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseRecommendUsers", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("room_id", room_id);
        param.put("course_id", course_id);
        param.put("page_count", page_count);
        param.put("student_pos", student_pos);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

}
