package qingning.common.server.controller;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractController;

import java.io.PrintWriter;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class CommonController extends AbstractController {

    private static final Logger logger   = LoggerFactory.getLogger(CommonController.class);

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
        if (bodyMap.get("server_url_update_time") == null ||
                !bodyMap.get("server_url_update_time").toString().equals(serverUrlInfoUpdateTime.toString())) {
            resultMap.put("server_url_info_list", serverUrlInfoMap);
            resultMap.put("server_url_info_update_time", serverUrlInfoUpdateTime);
        }
        responseEntity.setReturnData(resultMap);
        return responseEntity;
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

//    @SuppressWarnings("unchecked")
//    @RequestMapping(value = "/common/weixin/payment/result", method = RequestMethod.POST)
//    public
//    @ResponseBody
//    String handleWeixinPayResult(
//            HttpEntity<Object> entity) throws Exception {
//        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "handleWeixinPayResult", null, null);
//        requestEntity.setParam(entity.getBody());
//        Object responseString = this.processWithObjectReturn(requestEntity, serviceManger, message);
//        return (String)responseString;
//    }

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
	 * @param page_count
	 * @param record_date
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value="/common/distribution",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getCommonDistribution(
    		@RequestParam(value="page_count",defaultValue="10") String page_count,
    		@RequestParam(value="record_date",defaultValue="") String record_date,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "commonDistribution", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("page_count", page_count);
        param.put("record_date", record_date);
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
	 * @param accessToken
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
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @RequestMapping(value="/common/distribution/rooms/{room_id}",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getRoomDistributionInfo(
    		@PathVariable("room_id") String room_id,
    		@RequestParam(value="page_count",defaultValue="20") String page_count,
    		@RequestParam(value="start_time",defaultValue="") String start_time,
    		@RequestHeader("access_token") String access_token,
    		@RequestHeader("version") String version) throws Exception{
    	RequestEntity requestEntity = this.createResponseEntity("CommonServer", "roomDistributionInfo", access_token, version);
    	Map<String, Object> param = new HashMap<String, Object>();
        param.put("room_id", room_id);
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



}
