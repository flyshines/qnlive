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
import qingning.common.dj.HttpClientUtil;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.server.util.ServerUtils;
import qingning.common.util.Constants;
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
             @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
             @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "logUserInfo", accessToken, null,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getVersion", null, null,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "control", null, null,appName);
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
            @RequestHeader("version") String version,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "userLogin", null, version,appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
//        if (bodyMap.get("server_url_update_time") == null ||
//                !bodyMap.get("server_url_update_time").toString().equals(serverUrlInfoUpdateTime.toString())) {
//            resultMap.put("server_url_info_list", serverUrlInfoMap);
//            resultMap.put("server_url_info_update_time", serverUrlInfoUpdateTime);
//        }

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
            @RequestHeader("version") String version,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "serverTime", null, version,appName);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        resultMap.put("server_url_info_list", serverUrlInfoMap.get(appName));
        responseEntity.setReturnData(resultMap);
        return responseEntity;
    }




    /**
     *  微信公众号授权后的回调
     * 前端 默认进行 微信静默授权
     *  授权回调进入后台在后台获取code进行判断时候获取 openid
     *  如果有就进行正常跳转
     *  如果没有就进行手动授权
     *
     *  1.3 修改：新增SaaS登录回调逻辑17-06-24
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/common/weixin/weixinlogin", method = RequestMethod.GET)
    public void weixinLogin(
            @RequestParam("code") String code,
            @RequestParam(value="state",defaultValue = Constants.HEADER_APP_NAME) String state,
            HttpServletRequest request,HttpServletResponse response) throws Exception {
        Map<String,String> map = new HashMap<>();
        String appName = state;
        map.put("code",code);
        if(state.equals("qnsaas")){
            appName = "qnlive";
        }
        map.put("state",appName);
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "weixinCodeUserLogin", null, "",appName);
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Integer key = Integer.valueOf(resultMap.get("key").toString());
        if(key == 1){
            String userWeixinAccessToken = (String) resultMap.get("access_token");
            if(state.equals("qnsaas")){//跳转h5店铺
                response.sendRedirect("http://www.qnlive.com/qnsaas?token="+userWeixinAccessToken);
            }else{//跳转直播
                response.sendRedirect(MiscUtils.getConfigByKey("web_index",appName)+userWeixinAccessToken);
            }
             return ;
        }
        //如果没有拿到
        logger.info("没有拿到openId 或者 unionid 跳到手动授权页面");
        String authorization_url = MiscUtils.getConfigByKey("authorization_userinfo_url",appName);//手动授权url
        String authorizationUrl = authorization_url.replace("APPID", MiscUtils.getConfigByKey("appid",appName)).replace("REDIRECTURL", MiscUtils.getConfigByKey("redirect_url",appName)).replace("STATE", state);//修改参数
        response.sendRedirect(authorizationUrl);
        return ;
    }

    @RequestMapping(value = "/common/saas/login", method = RequestMethod.GET)
    public void pcLoginForSaaS(     @RequestParam("code") String code,
                             @RequestParam(value="state",defaultValue = Constants.HEADER_APP_NAME) String state,
                             HttpServletRequest request,HttpServletResponse response) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "pcCodeUserLogin", null, "",state);
        map.put("is_saas","1");
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        String appName = Constants.HEADER_APP_NAME;
        if(resultMap.get("app_name")!=null){
            appName = resultMap.get("app_name").toString();
        }
        String access_token = (String) resultMap.get("access_token");
        String weName = (String) resultMap.get("name");
        String shopId = (String) resultMap.get("shop_id");
        String roomId = (String) resultMap.get("room_id");

        response.sendRedirect(MiscUtils.getConfigByKey("wx_url_shop_index",state).replace("ACCESSTOKEN", access_token).replace("NAME", URLEncoder.encode(weName, "utf-8")).replace("APPFROME",appName).replace("ROOMID",roomId).replace("SHOPID",shopId));
    }

    @RequestMapping(value = "/common/weixin/pclogin", method = RequestMethod.GET)
    public void pcLogin(     @RequestParam("code") String code,
                             @RequestParam(value="state",defaultValue = Constants.HEADER_APP_NAME) String state,
            HttpServletRequest request,HttpServletResponse response) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "pcCodeUserLogin", null, "",state);
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Integer key = Integer.valueOf(resultMap.get("key").toString());
        String access_token = (String) resultMap.get("access_token");
        String weName = (String) resultMap.get("name");
        if(key == 0){//未绑定
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_binding_phone_url",state).replace("ACCESSTOKEN", access_token).replace("NAME", URLEncoder.encode(weName, "utf-8")));
        } else if(key == 1) { //登录过 有直播间信息
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_binding_room_url",state).replace("ACCESSTOKEN", access_token).replace("NAME", URLEncoder.encode(weName, "utf-8")));
        } else { //登录过 没有直播间信息
            //重定向到另一个页面
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_to_creat_room_ur",state));
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "qiNiuUploadToken", accessToken, version,appName);
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
            @RequestHeader("version") String version,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "weiXinConfiguration", null, version,appName);
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
            @RequestHeader("version") String version,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "userInfo", accessToken, version,appName);
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
            @RequestHeader("version") String version,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            HttpServletRequest request) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "generateWeixinPayBill", accessToken, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version, HttpServletRequest request) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "checkWeixinPayBill", accessToken, version,appName);
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
            RequestEntity requestEntity = this.createResponseEntity("CommonServer", "handleWeixinPayResult", "", "","");
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "updateUserInfo", accessToken, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "commonDistribution", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributerRecommendInfo", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributionInfo", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "courseDistributionInfo", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributionShareInfo", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseInviteCard", access_token, version,appName);
        Map<String, Object> param = new HashMap<>();
        param.put("course_id", course_id);
        param.put("png",png);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 查询系列邀请卡信息
     * @param series_id
     * @param access_token
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/common/series/{series_id}/card",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getSeriesInviteCard(
            @PathVariable("series_id") String series_id,
            @RequestParam(value="png",defaultValue="N") String png,
            @RequestHeader("access_token") String access_token,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getSeriesInviteCard", access_token, version,appName);
        Map<String, Object> param = new HashMap<>();
        param.put("series_id", series_id);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception{
        long startTime = System.currentTimeMillis();
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getRoomInviteCard", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "distributorsRecommendUser", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "createFeedback", access_token, version,appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    @RequestMapping(value="/common/weixin/resource/conversion",method=RequestMethod.POST)
    public @ResponseBody ResponseEntity convertWeixinResource(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String access_token,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "convertWeixinResource", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getShareInfo", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getShareInfo", access_token, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseRecommendUsers", accessToken, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version)throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "sendVerificationCode", accessToken, null,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "messageList", accessToken, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "courseInfo", accessToken, version,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "isphone", accessToken, null,appName);
        Map<String,String> map = new HashMap<>();
        map.put("phone_num",phone_num);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }




    /**
     * 搜索
     * @param search_text 搜索文本
     * @param search_type 查询类型 0所有 1直播间 2课程 3系列
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "search", accessToken, null,appName);
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
     * @param page_count 分页
     * @param course_id 课程id
     * @param status 课程状态
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/recommendCourse", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity recommendCourse(
            @RequestParam(value = "page_count", defaultValue = "20") String page_count,
            @RequestParam(value ="course_id",defaultValue = "") String course_id,
            @RequestParam(value ="status",defaultValue = "4") String status,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "recommendCourse", accessToken, null,appName);
        Map<String,String> map = new HashMap<>();
        map.put("page_count",page_count);
        map.put("course_id",course_id);
        map.put("status",status);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "banner", accessToken, null,appName);
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
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "classifyInfo", accessToken, null,appName);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 后台_添加分类信息
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/classify/add", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity addClassify(
            @RequestHeader(value = "access_token", defaultValue = "") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            HttpEntity<Object> entity,
            @RequestHeader(value="version", defaultValue="") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "addClassify", accessToken, version,appName);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 后台_编辑分类信息
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/classify/edit", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity editClassify(
            @RequestHeader(value = "access_token", defaultValue = "") String accessToken,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            HttpEntity<Object> entity,
            @RequestHeader(value="version", defaultValue="") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "editClassify", accessToken, version,appName);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 后台_获取所有分类列表
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/classify/list", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity getClassifyList(
            @RequestHeader(value = "access_token", defaultValue = "") String accessToken,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader(value="version", defaultValue="") String version)throws Exception{
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getClassifyList", accessToken, version,appName);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     *  获取systemConfig
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/systemConfig", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity systemConfig(
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "serverTime", null, version,appName);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        resultMap.put("system_config", systemConfigMap.get(appName));
        responseEntity.setReturnData(resultMap);
        return responseEntity;
    }

    /**
     *  saveCourseMsgList
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/saveCourseMsgList", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity saveCourseMsgList(
            @RequestParam(value = "course_id",defaultValue = "0") String course_id,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "saveCourseMsgList", null, version,appName);
        Map<String,String> map = new HashMap<>();
        map.put("course_id",course_id);
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     *  updateCourse
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/updateCourse", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity userGains(
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "userGains", null, version,appName);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
    
    /**
     * 后台_新增轮播
     * @param entity
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value="/common/banner/new", method=RequestMethod.POST)
    public @ResponseBody ResponseEntity addBanner( 
		    HttpEntity<Object> entity,
		    @RequestHeader(value="access_token", defaultValue="") String accessToken,
		    @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
		    @RequestHeader(value="version", defaultValue="") String version) throws Exception {
    	/*
    	 * 先执行获取systemConfig
    	 */
    	RequestEntity sysConfigReqEntity = this.createResponseEntity("CommonServer", "serverTime", null, version,appName);
        ResponseEntity sysConfigResponseEntity = this.process(sysConfigReqEntity, serviceManger, message);
    	
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "addBanner", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        /*
         * 获取轮播跳转前缀
         */
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        Map<String, Object> sysConfig = (Map<String, Object>) systemConfigMap.get(appName);
        reqMap.put("bannerJumpCoursePre", ((Map<String, Object>)sysConfig.get("bannerJumpCoursePre")).get("config_value").toString());
        reqMap.put("bannerJumpRoomPre", ((Map<String, Object>)sysConfig.get("bannerJumpRoomPre")).get("config_value").toString());
        
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
    /**
     * 后台_搜索banner列表
     * @param bannerName
     * @param status
     * @param bannerType
     * @param pageCount
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/banner/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getBannerListBySearch(
    		@RequestParam(value="banner_name", defaultValue="") String bannerName,
    		@RequestParam(value="status", defaultValue="-1") int status,
    		@RequestParam(value="banner_type", defaultValue="-1") int bannerType,
    		@RequestParam(value="page_count", defaultValue="20") long pageCount,
    		@RequestParam(value="page_num", defaultValue="1") long pageNum,
    		@RequestHeader(value="access_token", defaultValue="") String accessToken,
    		@RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getBannerListBySearch", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("banner_name", bannerName);
        param.put("status", status);
        param.put("banner_type", bannerType);
        param.put("page_count", pageCount);
        param.put("page_num", pageNum);
        requestEntity.setParam(param);
        
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
    /**
     * 后台_更新banner
     * @param entity
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value="/common/banner/update", method=RequestMethod.PUT)
    public @ResponseBody ResponseEntity updateBannerInfo( 
		    HttpEntity<Object> entity,
		    @RequestHeader(value="access_token", defaultValue="") String accessToken,
		    @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
		    @RequestHeader(value="version", defaultValue="") String version) throws Exception {
    	/*
    	 * 先执行获取systemConfig
    	 */
    	RequestEntity sysConfigReqEntity = this.createResponseEntity("CommonServer", "serverTime", null, version,appName);
        ResponseEntity sysConfigResponseEntity = this.process(sysConfigReqEntity, serviceManger, message);
    	
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "updateBannerInfo", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        /*
         * 获取轮播跳转前缀
         */
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        Map<String, Object> sysConfig = (Map<String, Object>) systemConfigMap.get(appName);
        reqMap.put("bannerJumpCoursePre", ((Map<String, Object>)sysConfig.get("bannerJumpCoursePre")).get("config_value").toString());
        reqMap.put("bannerJumpRoomPre", ((Map<String, Object>)sysConfig.get("bannerJumpRoomPre")).get("config_value").toString());
        
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
    /**
     * 后台_快速更新banner
     * @param entity
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value="/common/banner/fast_update", method=RequestMethod.PUT)
    public @ResponseBody ResponseEntity fastUpdateBannerInfo( 
		    HttpEntity<Object> entity,
		    @RequestHeader(value="access_token", defaultValue="") String accessToken,
		    @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
		    @RequestHeader(value="version", defaultValue="") String version) throws Exception {
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "fastUpdateBannerInfo", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
    /**
     * 后台_移除banner
     * @param entity
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value="/common/banner/remove", method=RequestMethod.DELETE)
    public @ResponseBody ResponseEntity removeBannerInfo( 
		    HttpEntity<Object> entity,
		    @RequestHeader(value="access_token", defaultValue="") String accessToken,
		    @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
		    @RequestHeader(value="version", defaultValue="") String version) throws Exception {
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "removeBannerInfo", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
    /**
     * 后台_课程搜索查询
     * @param searchParam
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/course/searcher", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getCourseListBySearch(
    		@RequestParam(value="search_param", defaultValue="") String searchParam,
    		@RequestHeader(value="access_token", defaultValue="") String accessToken,
    		@RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseListBySearch", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("search_param", searchParam);
        requestEntity.setParam(param);
        
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    
    /**
     * 后台_直播间搜索查询
     * @param searchParam
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/live_room/searcher", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getLiveRoomListBySearch(
    		@RequestParam(value="search_param", defaultValue="") String searchParam,
    		@RequestHeader(value="access_token", defaultValue="") String accessToken,
    		@RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader(value="version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getLiveRoomListBySearch", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("search_param", searchParam);
        requestEntity.setParam(param);
        
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 解析二维码
     * 前端传入公众号文章链接 进行解析 返回二维码链接
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/weChatUrl", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWeChatQrCode(
            @RequestParam(value="we_chat_url", defaultValue="") String we_chat_url,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        ResponseEntity responseEntity = new ResponseEntity();
        HttpClientUtil httpClientUtil = new HttpClientUtil();
        //"http://mp.weixin.qq.com/s/AZ56baCsh049jQ4C9xF2Ig"
        String str = httpClientUtil.doPost(we_chat_url, null, null, "UTF-8");
        String username = " user_name = \"";
        int user_name = str.indexOf(" user_name = \"");
        int i = str.indexOf("\"", user_name+username.length()+1);
        String substring = str.substring(user_name+username.length(), i);
        String url = "http://open.weixin.qq.com/qr/code/?username="+substring;

        String nickename = " nickname = \"";
        int nicke_name = str.indexOf(" nickname = \"");
        int a = str.indexOf("\"", nicke_name+nickename.length()+1);
        String weChatNickeName = str.substring(nicke_name+nickename.length(), a);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("url",url);
        resultMap.put("nickname",weChatNickeName);
        responseEntity.setReturnData(resultMap);
        return responseEntity;
    }
    
    /**
     * 管理后台登录
     * @param entity
     * @param appName
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/sys/login", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity adminUserLogin(
            HttpEntity<Object> entity,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "adminUserLogin", null, null, appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 发送手机验证码
     * @param phone 电话号码

     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/sendIMError", method = RequestMethod.GET)
    public
    @ResponseBody ResponseEntity
    sendIMError(HttpServletRequest request, @RequestParam(value = "phone") String phone
                )throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "sendIMError", "", "","qnlive");
        String ipAdress = request.getRemoteAddr();// HttpTookit.getIpAdress(request);//获取请求地址
        Map<String,String> map = new HashMap<>();
        map.put("phone",phone);
        map.put("ipAdress",ipAdress);
        logger.debug("IMERROR ipAdress : "+ipAdress);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }


}
