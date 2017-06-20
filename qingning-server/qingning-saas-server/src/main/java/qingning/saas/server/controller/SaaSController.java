package qingning.saas.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.Constants;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SaaSController extends AbstractController {

    /**
     * 微信扫码 1 登录入口
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/wechat/login/rqcode", method = RequestMethod.GET)
    public void wechatLogin(HttpServletResponse resp) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "wechatLogin", null, null, null);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Object redirectUrl = resultMap.get("redirectUrl");
        if (redirectUrl != null) {
            resp.sendRedirect(redirectUrl.toString());
        }
    }


    /**
     * 微信扫码 2 微信扫码登录
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/wechat/check_login", method = RequestMethod.GET)
    public void wechatCheckLogin(HttpServletResponse resp,
                                 @RequestParam(value = "code", defaultValue = "") String code) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "wechatCheckLogin", null, null, null);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("code", code);
        requestEntity.setParam(paramCode);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Object redirectUrl = resultMap.get("redirectUrl");
        if (redirectUrl != null) {
            resp.sendRedirect(redirectUrl.toString());
        }
    }

    /**
     * 店铺-轮播图列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity wechatCheckLogin(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "shopBannerList", accessToken, version, appName);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_count", pageCount);
        paramCode.put("page_num", pageNum);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * 店铺-添加轮播图
     *
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/shop/banner/add", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity shopBannerAdd(
            HttpEntity<Object> entity,
            @RequestHeader(value = "access_token") String accessToken,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "shopBannerAdd", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


}
