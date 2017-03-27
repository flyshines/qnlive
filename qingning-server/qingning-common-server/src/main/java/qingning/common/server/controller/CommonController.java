package qingning.common.server.controller;

//import org.apache.commons.fileupload.FileItem;
//import org.apache.commons.fileupload.FileUploadBase;
//import org.apache.commons.fileupload.FileUploadException;
//import org.apache.commons.fileupload.disk.DiskFileItemFactory;
//import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.server.util.ServerUtils;
import qingning.common.util.HttpTookit;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

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
     * 获取版本信息
     *
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @RequestMapping(value = "/common/client/version", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getVersion(
            @RequestParam(value = "plateform" ,defaultValue = "0") String plateform,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getVersion", null, null);
        Map<String,String> map = new HashMap<>();
        map.put("plateform",plateform);
        map.put("version",version);
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 总控开关
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @RequestMapping(value = "/common/client/control", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity control(
            @RequestParam(value = "plateform" ,defaultValue = "0") String plateform,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "control", null, null);
        Map<String,String> map = new HashMap<>();
        map.put("plateform",plateform);
        requestEntity.setParam(map);
        return  this.process(requestEntity, serviceManger, message);
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




    @RequestMapping(value = "/common/weixin/weCatLogin", method = RequestMethod.GET)
    public void weCatLogin(HttpServletRequest request,HttpServletResponse response) throws Exception {
        String authorization_url = MiscUtils.getConfigByKey("authorization_base_url");//静默授权url
        String authorizationUrl = authorization_url.replace("APPID", MiscUtils.getConfigByKey("appid")).replace("REDIRECTURL", MiscUtils.getConfigByKey("redirect_url"));//修改参数
        response.sendRedirect(authorizationUrl);
        return;
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
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "weixinCodeUserLogin", null, "");
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Integer key = Integer.valueOf(resultMap.get("key").toString());
        if(key == 1){
            //正常跳转到首页
            String userWeixinAccessToken = (String) resultMap.get("access_token");
            response.sendRedirect(MiscUtils.getConfigByKey("web_index")+userWeixinAccessToken);
            return ;
        }
        //如果没有拿到
        logger.info("没有拿到openId 或者 unionid 跳到手动授权页面");
        String authorization_url = MiscUtils.getConfigByKey("authorization_userinfo_url");//手动授权url
        String authorizationUrl = authorization_url.replace("APPID", MiscUtils.getConfigByKey("appid")).replace("REDIRECTURL", MiscUtils.getConfigByKey("redirect_url"));//修改参数
        response.sendRedirect(authorizationUrl);
        return ;
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

    /**
     * 查询用户信息
     * @param query_type 1:个人中心信息 2：个人基本信息
     * @param server_url_info_update_time 服务与对应url信息更新时间
     * @param accessToken 后台安全证书
     * @param version 版本号
     * @return
     * @throws Exception
     */
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
     * @param accessToken 安全证书
     * @param entity post参数
     * @param version 版本号
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/payment/weixin/bill", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity generateWeixinPayBill(
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


    @RequestMapping(value = "/common/payment/weixin/check", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity checkWeixinPayBill(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version, HttpServletRequest request) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "checkWeixinPayBill", accessToken, version);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
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
	 * @param position 用户加入直播间分销的时间（分页时使用）
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
    		@RequestParam(value="rq_code",defaultValue="") String rq_code,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributerRecommendInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("room_id", room_id);
        param.put("distributer_id", distributer_id);
        param.put("page_count", page_count);
        param.put("rq_code", rq_code);
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
    		@RequestParam(value="distributer_id", defaultValue="") String distributer_id,
    		@RequestParam(value="page_count",defaultValue="20") String page_count,
    		@RequestParam(value="start_time",defaultValue="") String start_time,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributionInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("room_id", room_id);
        param.put("rq_code", rq_code);
        param.put("distributer_id", distributer_id);
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
    		@RequestParam(value="rq_code",defaultValue="") String rq_code,
    		@RequestParam(value="page_count",defaultValue="20") String page_count,
    		@RequestParam(value="position",defaultValue="") String position,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "courseDistributionInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("course_id", course_id);
        param.put("rq_code", rq_code);
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
            @RequestParam(value="rq_code") String rq_code,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{

    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributionShareInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("room_id", room_id);
        param.put("rq_code",rq_code);
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
            @RequestParam(value="png",defaultValue="N") String png,
            @PathVariable("course_id") String course_id,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseInviteCard", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("course_id", course_id);
        param.put("png",png);
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
            @RequestParam(value="png",defaultValue="N") String png,
            @PathVariable("room_id") String room_id,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getRoomInviteCard", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("room_id", room_id);
        param.put("png",png);
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
            @RequestParam(value="png",defaultValue="N") String png,
            @RequestParam(value="id",defaultValue="") String id,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getShareInfo", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("query_type", query_type);
        param.put("id", id);
        param.put("png",png);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 获取qr_code
     * @param response servlet请求
     * @param request 返回
     * @param query_type 请求那种状态 如果是0 那么就是请求课程 如果是 1 那么就请求直播间
     * @param id 具体的id
     * @param access_token 安全证书
     * @param version 版本号
     * @return 返回ResponseEntity
     * @throws Exception
     */
    @RequestMapping(value="/common/getqr_code",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getQr_Code(
            HttpServletResponse response,HttpServletRequest request,
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
            @RequestParam(value="rq_code",defaultValue="") String rq_code,
            @RequestParam(value="distributer_id",defaultValue="") String distributer_id,
            @RequestParam(value="page_count",defaultValue="20") String page_count,
            @RequestParam(value="student_pos",defaultValue="") String student_pos,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseRecommendUsers", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("room_id", room_id);
        param.put("distributer_id", distributer_id);
        param.put("rq_code", rq_code);
        param.put("course_id", course_id);
        param.put("page_count", page_count);
        if(!student_pos.equals("")){
            param.put("student_pos", student_pos);
        }
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 发送手机验证码
     * @param phone 电话号码
     * @param accessToken 用户安全证书
     * @param version 版本
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/sendVerificationCode", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity sendVerificationCode(HttpServletRequest request,
            @RequestParam(value = "phone") String phone,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "sendVerificationCode", accessToken, null);
        String ipAdress = HttpTookit.getIpAdress(request);//获取请求地址
        Map<String,String> map = new HashMap<>();
        map.put("phone",phone);
        map.put("ipAdress",ipAdress);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 查询课程消息列表
     * @param course_id 课程id
     * @param page_count 分页
     * @param user_type  用户类型 0老师/顾问  1用户
     * @param message_type 消息类型:0:音频 1：文字 3：图片 4 附件
     * @param message_imid 消息id
     * @param direction  获取那种信息  0旧  1新 默认为1
     * @param accessToken 后台安全证书
     * @param version 版本号
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/courses/{course_id}/messages", method = RequestMethod.GET)
    public
    @ResponseBody ResponseEntity getMessageList(
            @PathVariable("course_id") String course_id,
            @RequestParam(value = "page_count", defaultValue = "20") String page_count,
            @RequestParam(value = "user_type", defaultValue = "0") String user_type,
            @RequestParam(value = "message_type",defaultValue = "") String message_type,
            @RequestParam(value = "message_imid", defaultValue = "") String message_imid,
            @RequestParam(value = "direction", defaultValue = "1") String direction,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "messageList", accessToken, version);
        Map<String, Object> parMap = new HashMap<>();
        parMap.put("course_id", course_id);
        parMap.put("page_count", page_count);
        parMap.put("user_type", user_type);
        parMap.put("message_type", message_type);
        parMap.put("message_imid", message_imid);
        parMap.put("direction",direction);
        requestEntity.setParam(parMap);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * 查询课程综合信息，包括PPT信息，讲课音频信息，打赏列表信息
     * @param course_id
     * @param reward_update_time
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/courses/{course_id}/info", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getCoursesInfo(
            @PathVariable("course_id") String course_id,
            @RequestParam(value = "reward_update_time", defaultValue = "") String reward_update_time,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "courseInfo", accessToken, version);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("course_id", course_id);
        param.put("reward_update_time", reward_update_time);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> reqMap = (Map<String, Object>) responseEntity.getReturnData();

        if(reqMap.get("user_type").equals("1")){
            //处理打赏信息
            if(! reward_update_time.equals(rewardConfigurationTime.toString())){
                reqMap.put("reward_info",rewardConfigurationMap);
                responseEntity.setReturnData(reqMap);
            }
        }
        return responseEntity;
    }

    /**
     * 设置课程状态
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/courses/{course_id}/courseStatus", method = RequestMethod.GET)
    public void setCourseStatus(
            @PathVariable("course_id") String course_id,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "courseStatus", accessToken, version);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("course_id", course_id);
        requestEntity.setParam(param);
        this.process(requestEntity, serviceManger, message);
    }

//    /**
//     * 上传机器人数据
//     * @param req
//     * @param resp
//     * @throws Exception
//     */
//    @RequestMapping(value = "/common/uploadusers", method = RequestMethod.PUT)
//    public void setCourseStatus(HttpServletRequest req, HttpServletResponse resp)throws Exception{
//        //得到上传文件的保存目录，将上传的文件存放于WEB-INF目录下，不允许外界直接访问，保证上传文件的安全
//        String savePath = req.getServletContext().getRealPath("/WEB-INF/upload");
//        //上传时生成的临时文件保存目录
//        String tempPath = req.getServletContext().getRealPath("/WEB-INF/temp");
//
//        File file = new File(tempPath);
//        if(!file.exists()&&!file.isDirectory()){
//            System.out.println("目录或文件不存在！");
//            file.mkdir();
//        }
//        //消息提示
//        String result = "0"; //成功
//        //保存路径
//        String savePathStr = null;
//        try {
//            //使用Apache文件上传组件处理文件上传步骤：
//            //1、创建一个DiskFileItemFactory工厂
//            DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
//            //设置工厂的缓冲区的大小，当上传的文件大小超过缓冲区的大小时，就会生成一个临时文件存放到指定的临时目录当中。
//            diskFileItemFactory.setSizeThreshold(1024*100);
//            //设置上传时生成的临时文件的保存目录
//            diskFileItemFactory.setRepository(file);
//            //2、创建一个文件上传解析器
//            ServletFileUpload fileUpload = new ServletFileUpload(diskFileItemFactory);
//            //解决上传文件名的中文乱码
//            fileUpload.setHeaderEncoding("UTF-8");
//            //监听文件上传进度
////            fileUpload.setProgressListener(new ProgressListener(){
////                public void update(long pBytesRead, long pContentLength, int arg2) {
////                    System.out.println("文件大小为：" + pContentLength + ",当前已处理：" + pBytesRead);
////                }
////            });
//            //3、判断提交上来的数据是否是上传表单的数据
//            if(!fileUpload.isMultipartContent(req)){
//                //按照传统方式获取数据
//                return;
//            }
//            //设置上传单个文件的大小的最大值，目前是设置为1024*1024字节，也就是1MB
//            fileUpload.setFileSizeMax(1024*1024);
//            //设置上传文件总量的最大值，最大值=同时上传的多个文件的大小的最大值的和，目前设置为10MB
//            fileUpload.setSizeMax(1024*1024*10);
//            //4、使用ServletFileUpload解析器解析上传数据，解析结果返回的是一个List<FileItem>集合，每一个FileItem对应一个Form表单的输入项
//            List<FileItem> list = fileUpload.parseRequest(req);
//            for (FileItem item : list) {
//                //如果fileitem中封装的是普通输入项的数据
//                if(item.isFormField()){
////                    String name = item.getFieldName();
////                    //解决普通输入项的数据的中文乱码问题
////                    String value = item.getString("UTF-8");
////                    String value1 = new String(name.getBytes("iso8859-1"),"UTF-8");
////                    System.out.println(name+"  "+value);
////                    System.out.println(name+"  "+value1);
//                }else{
//                    //如果fileitem中封装的是上传文件，得到上传的文件名称，
//                    String fileName = item.getName();
//                    System.out.println(fileName);
//                    if(fileName==null||fileName.trim().equals("")){
//                        continue;
//                    }
//                    //注意：不同的浏览器提交的文件名是不一样的，有些浏览器提交上来的文件名是带有路径的，如：  c:\a\b\1.txt，而有些只是单纯的文件名，如：1.txt
//                    //处理获取到的上传文件的文件名的路径部分，只保留文件名部分
//                    fileName = fileName.substring(fileName.lastIndexOf(File.separator)+1);
//                    //得到上传文件的扩展名
//                    String fileExtName = fileName.substring(fileName.lastIndexOf(".")+1);
//                    if (!fileExtName.equals("xls") && !fileExtName.equals("xlsx")) {
//                        result = "1";
//                    }
//                    //如果需要限制上传的文件类型，那么可以通过文件的扩展名来判断上传的文件类型是否合法
//                    System.out.println("上传文件的扩展名为:"+fileExtName);
//                    //获取item中的上传文件的输入流
//                    InputStream fis = item.getInputStream();
//                    //得到文件保存的名称
//                    fileName = UUID.randomUUID().toString()+"_"+fileName;;
//                    //得到文件保存的路径
//                    savePathStr = mkFilePath(savePath, fileName);
//                    System.out.println("保存路径为:"+savePathStr);
//                    //创建一个文件输出流
//                    FileOutputStream fos = new FileOutputStream(savePathStr+File.separator+fileName);
//                    //获取读通道
//                    FileChannel readChannel = ((FileInputStream)fis).getChannel();
//                    //获取读通道
//                    FileChannel writeChannel = fos.getChannel();
//                    //创建一个缓冲区
//                    ByteBuffer buffer = ByteBuffer.allocate(1024);
//                    //判断输入流中的数据是否已经读完的标识
//                    int length = 0;
//                    //循环将输入流读入到缓冲区当中，(len=in.read(buffer))>0就表示in里面还有数据
//                    while(true){
//                        buffer.clear();
//                        int len = readChannel.read(buffer);//读入数据
//                        if(len < 0){
//                            break;//读取完毕
//                        }
//                        buffer.flip();
//                        writeChannel.write(buffer);//写入数据
//                    }
//                    //关闭输入流
//                    fis.close();
//                    //关闭输出流
//                    fos.close();
//                    //删除处理文件上传时生成的临时文件
//                    item.delete();
//                }
//            }
//        } catch (FileUploadBase.FileSizeLimitExceededException e) {
//            e.printStackTrace();
//            result = "2";
//        } catch (FileUploadBase.SizeLimitExceededException e) {
//            e.printStackTrace();
//            result = "3";
//        } catch (FileUploadException e) {
//            e.printStackTrace();
//            result = "3";
//        }
//
//        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "uploadusers", null, null);
//        Map<String, Object> param = new HashMap<String, Object>();
//        param.put("result", result);
//        param.put("path", savePathStr);
//        requestEntity.setParam(param);
//        this.process(requestEntity, serviceManger, message);
//    }
//
//    public String mkFilePath(String savePath,String fileName){
//        //得到文件名的hashCode的值，得到的就是filename这个字符串对象在内存中的地址
//        int hashcode = fileName.hashCode();
//        int dir1 = hashcode&0xf;
//        int dir2 = (hashcode&0xf0)>>4;
//        //构造新的保存目录
//        String dir = savePath + "\\" + dir1 + "\\" + dir2;
//        //File既可以代表文件也可以代表目录
//        File file = new File(dir);
//        if(!file.exists()){
//            file.mkdirs();
//        }
//        return dir;
//    }
}
