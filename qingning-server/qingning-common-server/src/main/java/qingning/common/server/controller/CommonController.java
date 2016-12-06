package qingning.common.server.controller;

import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.server.AbstractController;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.common.utils.StringUtils;

@RestController
public class CommonController extends AbstractController {

    private static final Logger logger = Logger.getLogger(CommonController.class);


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
    @RequestMapping(value = "/user/login", method = RequestMethod.POST)
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
}
