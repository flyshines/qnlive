package qingning.shop.server.controller;


import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.wxEncrypt.WXBizMsgCrypt;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class ShopController extends AbstractController {

    private WXBizMsgCrypt cryptUtil = null;


    /**
     * 创建课程
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/shop/lecturer/courses", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity createCourse(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "createCourse", accessToken, version);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 开通店铺
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/open", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity openShop(@RequestHeader(value = "access_token") String accessToken,

                            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopOpen", accessToken, version);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * 微信扫码 获取扫码链接
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/wechat/login/rqcode", method = RequestMethod.GET)
    public void wechatLogin(HttpServletResponse resp) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "wechatLogin", null, null);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Object redirectUrl = resultMap.get("redirectUrl");
        if (redirectUrl != null) {
            resp.sendRedirect(redirectUrl.toString());
        }
    }

    /**
     * 店铺-店铺信息
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/info", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity shopInfo(@RequestHeader(value = "access_token") String accessToken,
                            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopInfo", accessToken, version);
        return this.process(requestEntity, serviceManger, message);
    }

}
