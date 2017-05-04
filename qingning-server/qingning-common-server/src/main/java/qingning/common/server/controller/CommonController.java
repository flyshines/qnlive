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
import sun.misc.BASE64Decoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

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
    		@RequestHeader(value="access_token", defaultValue="") String accessToken,
             @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "logUserInfo", accessToken, null);
		Map inputParameters = (Map)entity.getBody();
        inputParameters.put("ip", ServerUtils.getRequestIP());
        inputParameters.put("version",version);
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
     *  获取所有连接
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/serverUrlInfo", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity serverUrlInfo(
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "serverTime", null, version);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        resultMap.put("server_url_info_list", serverUrlInfoMap.get("qnlive"));
        responseEntity.setReturnData(resultMap);
        return responseEntity;
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
            resultMap.put("server_url_info_list", serverUrlInfoMap.get("qnlive"));
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

    @RequestMapping(value = "/common/weixin/pclogin", method = RequestMethod.GET)
    public void pcLogin(HttpServletRequest request,HttpServletResponse response) throws Exception {
        StringBuffer url = request.getRequestURL();//获取路径
        Map<String, String[]> params = request.getParameterMap();
        String[] codes = params.get("code");//拿到的code的值
        String code = codes[0];
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "pcCodeUserLogin", null, "");
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Integer key = Integer.valueOf(resultMap.get("key").toString());
        String access_token = (String) resultMap.get("access_token");
        String weName = (String) resultMap.get("name");
        if(key == 0){//未绑定
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_binding_phone_url").replace("ACCESSTOKEN", access_token).replace("NAME", URLEncoder.encode(weName, "utf-8")));
        } else if(key == 1) { //登录过 有直播间信息
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_binding_room_url").replace("ACCESSTOKEN", access_token).replace("NAME", URLEncoder.encode(weName, "utf-8")));
        } else { //登录过 没有直播间信息
            //重定向到另一个页面
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_to_creat_room_ur"));
        }
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
        long startTime = System.currentTimeMillis();
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getRoomInviteCard", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("room_id", room_id);
        param.put("png",png);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        long endTime = System.currentTimeMillis();
        logger.debug("==================================================================="+(endTime-startTime)+"==========================================================");
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
        //处理打赏信息
        if(! reward_update_time.equals(rewardConfigurationTime.toString())){
            reqMap.put("reward_info",rewardConfigurationMap);
            responseEntity.setReturnData(reqMap);
        }
        return responseEntity;
    }



    /**
     * 进行分享url解码 然后进行跳转
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/shareUrl", method = RequestMethod.GET)
    public void shareUrl(HttpServletRequest request,
                         HttpServletResponse response,
            @RequestParam(value = "shareUrl") String shareUrl)throws Exception{
        byte[] b = null;
        String result = null;
        if (shareUrl != null) {
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                b = decoder.decodeBuffer(shareUrl);
                shareUrl = new String(b,"UTF-8");
               // String a = new String(shareUrl.getBytes("8859_1"),"UTF-8");
                response.sendRedirect(shareUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 判断手机号是否可以使用
     * @param phone_num 手机号
     * @param accessToken 后台安全证书
     * @param version 版本号
     * @return 如果可以使用不会返回任何异常信息
     *           如果失败 会根据失败的状态返回对应的异常信息
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/isphone", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity isphone(
            @RequestParam("phone_num") String phone_num,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "isphone", accessToken, null);
        Map<String,String> map = new HashMap<>();
        map.put("phone_num",phone_num);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }


    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/delDistribution ", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity delDistribution(
            @RequestParam("lecture_id") String lecture_id,
            @RequestParam("distribution_user_id") String distribution_user_id,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "delDistribution", accessToken, null);
        Map<String,String> map = new HashMap<>();
        map.put("lecture_id",lecture_id);
        map.put("distribution_user_id",distribution_user_id);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }




    /**
     * 搜索
     * @param search_text 搜索文本
     * @param search_type 查询类型 0所有 1直播间 2课程
     * @param page_count 分页
     * @param page_num 分页参数
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/search", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity search(
            @RequestParam(value = "search_text",defaultValue = "") String search_text,
            @RequestParam(value = "search_type",defaultValue = "0") String search_type,
            @RequestParam(value = "page_count", defaultValue = "20") int page_count,
            @RequestParam(value ="page_num",defaultValue = "0") int page_num,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "search", accessToken, null);
        Map<String,Object> map = new HashMap<>();
        map.put("search_text",search_text);
        map.put("search_type",search_type);
        map.put("page_count",page_count);
        map.put("page_num",page_num);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 推荐
     * @param select_type 查询类型  1是推荐课程换一换 2推荐课程下拉
     * @param page_count 分页
     * @param page_num 分页参数
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/recommendCourse", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity recommendCourse(
            @RequestParam(value = "select_type",defaultValue = "0") String select_type,
            @RequestParam(value = "page_count", defaultValue = "20") String page_count,
            @RequestParam(value ="page_num",defaultValue = "0") String page_num,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "recommendCourse", accessToken, null);
        Map<String,String> map = new HashMap<>();
        map.put("select_type",select_type);
        map.put("page_count",page_count);
        map.put("page_num",page_num);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }



    /**
     * 广告
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/banner", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity banner(
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "banner", accessToken, null);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * 获取分类信息
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/classifyInfo", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity classifyInfo(
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "classifyInfo", accessToken, null);
        return this.process(requestEntity, serviceManger, message);
    }

//    /**
//     * 保存消息
//     * @param accessToken
//     * @param version
//     * @return
//     * @throws Exception
//     */
//    @SuppressWarnings("unchecked")
//    @RequestMapping(value = "/common/saveMsg", method = RequestMethod.GET)
//    public @ResponseBody
//    ResponseEntity saveMsg(
//            @RequestHeader("access_token") String accessToken,
//            @RequestHeader("version") String version)throws Exception{
//        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "saveMsg", accessToken, null);
//        return this.process(requestEntity, serviceManger, message);
//    }


}
