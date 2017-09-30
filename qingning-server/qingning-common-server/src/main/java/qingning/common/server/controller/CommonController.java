package qingning.common.server.controller;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.server.util.ServerUtils;
import qingning.common.util.Constants;
import qingning.common.util.HttpTookit;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

@RestController
public class CommonController extends AbstractController {

    private static final Logger logger   = LoggerFactory.getLogger(CommonController.class);


    /**
     * 绑定手机号码（校验验证码）
     * @param accessToken 用户安全证书
     * @param version 版本
     * @throws Exception
     */
    @RequestMapping(value = "/bind/phone", method = RequestMethod.PUT)
    public @ResponseBody ResponseEntity  verifyVerificationCode(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version)throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "bindPhone", accessToken, version);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 获取系统时间
     *
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @RequestMapping(value = "/common/client/information", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity collectClientInformation(HttpEntity<Object> entity,
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
    public @ResponseBody ResponseEntity getVersion(
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
    @RequestMapping(value = "/common/client/control", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity control(
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
    @RequestMapping(value = "/common/serverUrlInfo", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity serverUrlInfo(
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "serverTime", null, version);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        resultMap.put("server_url_info_list", serverUrlInfoMap);
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
     * 查询店铺邀请卡信息
     * @param shop_id
     * @param access_token
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/common/shop/{shop_id}/card",method=RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopCard(
            @RequestParam(value="png",defaultValue="N") String png,
            @PathVariable("shop_id") String shop_id,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception{
        long startTime = System.currentTimeMillis();
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getShopCard", access_token, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shop_id);
        param.put("png",png);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        long endTime = System.currentTimeMillis();
        logger.debug("==================================================================="+(endTime-startTime)+"==========================================================");
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
     * 发送手机验证码
     *
     * @param phone       电话号码
     * @param accessToken 用户安全证书
     * @param version     版本
     * @throws Exception
     */
    @RequestMapping(value = "/common/sendVerificationCode", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity sendVerificationCode(HttpServletRequest request,
                                        @RequestParam(value = "phone") String phone,
                                        @RequestHeader("access_token") String accessToken,
                                        @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "sendVerificationCode", accessToken, version);
        String ipAdress = HttpTookit.getIpAdress(request);//获取请求地址
        Map<String, String> map = new HashMap<>();
        map.put("phone", phone);
        map.put("ipAdress", ipAdress);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 发送手机验证码
     * @param phone 电话号码

     * @throws Exception
     */
    @RequestMapping(value = "/common/sendIMError", method = RequestMethod.GET)
    public
    @ResponseBody ResponseEntity
    sendIMError(HttpServletRequest request, @RequestParam(value = "phone") String phone
    )throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "sendIMError", "", "");
        String ipAdress = request.getRemoteAddr();// HttpTookit.getIpAdress(request);//获取请求地址
        Map<String,String> map = new HashMap<>();
        map.put("phone",phone);
        map.put("ipAdress",ipAdress);
        logger.debug("IMERROR ipAdress : "+ipAdress);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * 七牛音视频处理
     * @param fileUrl 文件地址

     * @throws Exception
     */
    @RequestMapping(value = "/common/qiniu/cutMedia", method = RequestMethod.POST)
    public
    @ResponseBody ResponseEntity cutMedia(
            HttpEntity<Object> entity,
            @RequestParam(value = "file_url", defaultValue="") String fileUrl,
            @RequestParam(value = "type", defaultValue="") String type,
            @RequestParam(value="time_second", defaultValue="") String timeSecond
    )throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "cutMedia", "", "");
        Map<String,String> map = (Map<String,String>)entity.getBody();
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 七牛回调处理

     * @throws Exception
     */
    @RequestMapping(value = "/common/qiniu/callback", method = RequestMethod.GET)
    public
    @ResponseBody void callback(HttpServletRequest request,HttpServletResponse response)throws Exception {
        logger.info("================> 接收到 qiniuNotify 通知。==================");
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        PrintWriter out = null;
        try{
            out =response.getWriter();
            String resultData = MiscUtils.convertStreamToString(request.getInputStream());
            SortedMap<String,String> requestMapData = MiscUtils.requestToMap(resultData);//notify请求数据
            logger.info("===> weixinNotify 请求数据：" + requestMapData);
            JSONObject result = new JSONObject();
            result.put("success",true);
            result.put("name","");
            logger.info("===> result Data: " + result.toJSONString());
            out.println(result.toJSONString());
            out.flush();
            out.close();
        }catch(Exception e){
            logger.error("====================>  qiniuNotify 处理异常  ========");
            logger.error(e.getMessage(), e);
            out.flush();
            out.close();
        }
        logger.info("==========================>  qiniuNotify 处理完毕。 =======");
    }


    /**
     * 发送手机验证码(已有电话号码用户)
     * @param accessToken 用户安全证书
     * @param version 版本
     * @throws Exception
     */
    @RequestMapping(value = "/common/vip/sendVerificationCode", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity sendVipVerificationCode(HttpServletRequest request,
                                                                @RequestHeader("access_token") String accessToken,
                                                                @RequestHeader("version") String version)throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "sendVipVerificationCode", accessToken, version);
        String ipAdress = HttpTookit.getIpAdress(request);//获取请求地址
        Map<String,String> map = new HashMap<>();
        map.put("ipAdress",ipAdress);
        requestEntity.setParam(map);
        return this.process(requestEntity, serviceManger, message);
    }

    @RequestMapping(value = "/common/user/userloginbyuserid", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity userlogin(
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "userLoginByUserId", accessToken, version);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
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
}
