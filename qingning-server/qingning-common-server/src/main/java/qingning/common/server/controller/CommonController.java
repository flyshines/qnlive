package qingning.common.server.controller;

import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractController;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class CommonController extends AbstractController {

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
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "generateWeixinPayBill", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    @RequestMapping(value = "/common/user", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getUserInfo(
            @RequestParam(value = "query_type", defaultValue = "") String query_type,
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
        if (MiscUtils.isEmpty(server_url_info_update_time) ||
                !server_url_info_update_time.equals(serverUrlInfoUpdateTime.toString())) {
            resultMap.put("server_url_info_list", serverUrlInfoMap);
            resultMap.put("server_url_info_update_time", serverUrlInfoUpdateTime);
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

}
