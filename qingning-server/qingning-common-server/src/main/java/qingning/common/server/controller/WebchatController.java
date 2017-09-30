package qingning.common.server.controller;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import qingning.common.dj.HttpClientUtil;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.server.util.ServerUtils;
import qingning.common.util.Base64;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

@Controller
public class WebchatController extends AbstractController {
    private static final Logger logger = LoggerFactory.getLogger(WebchatController.class);

    /**
     * 微信公众号授权
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/common/weixin/weixinlogin", method = RequestMethod.GET)
    public void weChatLogin(
            @RequestParam(value = "code", defaultValue = "") String code,
            @RequestParam(value = "state", defaultValue = "") String state,
            @RequestParam(value = "key", defaultValue = "KEY") String paramKey,
            @RequestParam(value = "user_id", defaultValue = "USER_ID") String userId,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        //第一次进行跳

        if (MiscUtils.isEmpty(state)) {
            String authorization_url = MiscUtils.getConfigByKey("base_authorization_url");//静默授权
            String appid = MiscUtils.getConfigByKey("old_appid");
            String redireceUrl = MiscUtils.getConfigByKey("new_redirect_url");
            if (paramKey != null) {
                redireceUrl = redireceUrl.replace("KEY", paramKey);
            }
            String encodeUrl = URLEncoder.encode(redireceUrl, "utf-8");
            String authorizationUrl = authorization_url.replace("APPID", appid).replace("REDIRECTURL", encodeUrl).replace("STATE", "old");//修改参数
            response.sendRedirect(authorizationUrl);
            return;
        }

        if (state.equals("old")) {
            RequestEntity requestEntity = this.createResponseEntity("CommonServer", "newWeixinCodeUserLogin", null, "");
            Map<String, String> map = new HashMap<>();
            map.put("code", code);
            map.put("state", state);
            requestEntity.setParam(map);
            map.put("code", code);
            map.put("user_id", userId);
            map.put("state", state);

            requestEntity.setParam(map);
            ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
            Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
            Integer key = Integer.valueOf(resultMap.get("key").toString());
            if (key == 1) {
                String authorization_url = MiscUtils.getConfigByKey("base_authorization_url");//静默授权url
                String appid = MiscUtils.getConfigByKey("appid");
                String redireceUrl = MiscUtils.getConfigByKey("new_redirect_url");
                redireceUrl = redireceUrl.replace("KEY", paramKey);
                if (!MiscUtils.isEmpty(resultMap.get("user_id"))) {
                    String user_id = resultMap.get("user_id").toString();
                    redireceUrl = redireceUrl.replace("USER_ID", user_id);
                }
                String encodeUrl = URLEncoder.encode(redireceUrl, "utf-8");

                String authorizationUrl = authorization_url.replace("APPID", appid).replace("REDIRECTURL", encodeUrl).replace("STATE", "new");//修改参数
                response.sendRedirect(authorizationUrl);
                return;
            }

            //没有拿到旧账号的openid 重新登录
            logger.info("没有拿到openId 或者 unionid 跳到手动授权页面");
            String authorization_url = MiscUtils.getConfigByKey("authorization_url");//手动授权url
            String appid = MiscUtils.getConfigByKey("old_appid");
            String redireceUrl = MiscUtils.getConfigByKey("new_redirect_url");
            redireceUrl = redireceUrl.replace("KEY", paramKey);
            if (!MiscUtils.isEmpty(resultMap.get("user_id"))) {
                String user_id = resultMap.get("user_id").toString();
                redireceUrl = redireceUrl.replace("USER_ID", user_id);
            }
            String encodeUrl = URLEncoder.encode(redireceUrl, "utf-8");
            String authorizationUrl = authorization_url.replace("APPID", appid).replace("REDIRECTURL", encodeUrl).replace("STATE", "old");
            ;//修改参数
            response.sendRedirect(authorizationUrl);
            return;
        }

        //新登路
        if (state.equals("new")) {
            RequestEntity requestEntity = this.createResponseEntity("CommonServer", "newWeixinCodeUserLogin", null, "");
            Map<String, String> map = new HashMap<>();
            map.put("code", code);
            map.put("user_id", userId);
            map.put("state", state);
            requestEntity.setParam(map);
            ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
            Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
            Integer key = Integer.valueOf(resultMap.get("key").toString());
            if (key == 1) {
                String userWeixinAccessToken = (String) resultMap.get("access_token");
                if (StringUtils.isNotEmpty(paramKey) && !paramKey.equals("KEY")) {
                    String newUrl = new String(Base64.decode(paramKey));
                    response.sendRedirect(newUrl + "token=" + userWeixinAccessToken);
                } else {
                    response.sendRedirect(MiscUtils.getConfigByKey("web_index") + userWeixinAccessToken);
                }
                return;
            }
            String authorization_url = MiscUtils.getConfigByKey("authorization_url");//手动授权url
            String appid = MiscUtils.getConfigByKey("appid");
            String redireceUrl = MiscUtils.getConfigByKey("new_redirect_url");
            redireceUrl = redireceUrl.replace("KEY", paramKey);
            if (!MiscUtils.isEmpty(resultMap.get("user_id"))) {
                String user_id = resultMap.get("user_id").toString();
                redireceUrl = redireceUrl.replace("USER_ID", user_id);
            }
            String encodeUrl = URLEncoder.encode(redireceUrl, "utf-8");

            String authorizationUrl = authorization_url.replace("APPID", appid).replace("REDIRECTURL", encodeUrl).replace("STATE", "new");//修改参数
            response.sendRedirect(authorizationUrl);
            return;
        }

    }

    /**
     * 用户登录
     *
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/login", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity userLogin(
            HttpEntity<Object> entity,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "userLogin", null, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 账号用户登录
     *
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/account/login", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity accountLogin(
            HttpEntity<Object> entity,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "accountLogin", null, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 账号用户注册
     * 1.用户注册
     * 2.开通店铺
     * 3,开通直播间
     *
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/account/register", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity accountRegister(
            HttpEntity<Object> entity,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "accountRegister", null, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 微信消息接收和token验证
     *
     * @param model
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/webchat/validation")
    public void tokenValidation(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean isGet = request.getMethod().toLowerCase().equals("get");
        PrintWriter print;
        if (isGet) {
            // 微信加密签名
            String signature = request.getParameter("signature");
            // 时间戳
            String timestamp = request.getParameter("timestamp");
            // 随机数
            String nonce = request.getParameter("nonce");
            // 随机字符串
            String echostr = request.getParameter("echostr");
            // 通过检验signature对请求进行校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败
            if (signature != null && ServerUtils.checkSignature(signature, timestamp, nonce)) {
                try {
                    print = response.getWriter();
                    print.write(echostr);
                    print.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 微信公众号授权后的回调
     * 前端 默认进行 微信静默授权
     * 授权回调进入后台在后台获取code进行判断时候获取 openid
     * 如果有就进行正常跳转
     * 如果没有就进行手动授权
     * 1.3 修改：新增SaaS登录回调逻辑17-06-24
     *旧的微信登录
     * @param response
     * @throws Exception
     */
//    @RequestMapping(value = "/common/weixin/weixinlogin", method = RequestMethod.GET)
//    public void weixinLogin(
//            @RequestParam("code") String code,
//            @RequestParam(value = "state", defaultValue = "qnlive") String state,
//            @RequestParam(value = "key", defaultValue = "") String paramKey, HttpServletResponse response) throws Exception {
//        Map<String, String> map = new HashMap<>();
//        String appName = state;
//        map.put("code", code);
//        if (state.equals("qnsaas")) {
//            appName = "qnlive";
//        }
//        map.put("state", appName);
//        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "weixinCodeUserLogin", null, "");
//        requestEntity.setParam(map);
//        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
//        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
//        Integer key = Integer.valueOf(resultMap.get("key").toString());
//        if (key == 1) {
//            String userWeixinAccessToken = (String) resultMap.get("access_token");
//            if (state.equals("qnsaas")) {//跳转h5店铺
//                logger.debug("key:" + paramKey);
//                if (StringUtils.isNotEmpty(paramKey)) {
//                    String newUrl = new String(Base64.decode(paramKey));
//                    logger.debug("url:" + newUrl);
//                    response.sendRedirect(newUrl + "token=" + userWeixinAccessToken);
//                } else {
//                    response.sendRedirect(MiscUtils.getConfigByKey("qnsaas_index") + userWeixinAccessToken);
//                }
//            } else {//跳转直播
//                response.sendRedirect(MiscUtils.getConfigByKey("web_index") + userWeixinAccessToken);
//            }
//            return;
//        }
//        //如果没有拿到
//        logger.info("没有拿到openId 或者 unionid 跳到手动授权页面");
//        String authorization_url = MiscUtils.getConfigByKey("authorization_userinfo_url");//手动授权url
//        String authorizationUrl = authorization_url.replace("APPID", MiscUtils.getConfigByKey("appid")).replace("REDIRECTURL", MiscUtils.getConfigByKey("redirect_url")).replace("STATE", state);//修改参数
//        response.sendRedirect(authorizationUrl);
//        return;
//    }


    /**
     * PC店铺扫码登录
     *
     * @param code
     * @param paramKey
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/common/saas/login", method = RequestMethod.GET)
    public void pcLoginForSaaS(@RequestParam("code") String code,
                               @RequestParam(value = "key", defaultValue = "") String paramKey, HttpServletResponse response) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "pcCodeUserLogin", null, "");
        map.put("is_saas", "1");
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();
        String appName = "qnlive";
        if (resultMap.get("app_name") != null) {
            appName = resultMap.get("app_name").toString();
        }
        String access_token = (String) resultMap.get("access_token");
        String weName = (String) resultMap.get("name");
        String shopId = (String) resultMap.get("shop_id");
        String roomId = (String) resultMap.get("room_id");
        logger.debug("key:" + paramKey);
        if (StringUtils.isNotEmpty(paramKey)) {
            String newUrl = new String(Base64.decode(paramKey));
            response.sendRedirect(newUrl + "token=" + access_token);
        } else {
            response.sendRedirect(MiscUtils.getConfigByKey("wx_url_shop_index").replace("ACCESSTOKEN", access_token).replace("NAME", URLEncoder.encode(weName, "utf-8")).replace("APPFROME", appName).replace("ROOMID", roomId).replace("SHOPID", shopId));
        }
    }

    /**
     * PC登录
     *
     * @param code
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/common/weixin/pclogin", method = RequestMethod.GET)
    public void pcLogin(@RequestParam("code") String code, HttpServletResponse response) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "pcCodeUserLogin", null, "");
        requestEntity.setParam(map);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

        Integer key = Integer.valueOf(resultMap.get("key").toString());
        String access_token = (String) resultMap.get("access_token");
        String weName = (String) resultMap.get("name");
        if (key == 0) {//未绑定
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_binding_phone_url").replace("ACCESSTOKEN", access_token).replace("NAME", URLEncoder.encode(weName, "utf-8")));
        } else if (key == 1) { //登录过 有直播间信息
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_binding_room_url").replace("ACCESSTOKEN", access_token).replace("NAME", URLEncoder.encode(weName, "utf-8")));
        } else { //登录过 没有直播间信息
            //重定向到另一个页面
            response.sendRedirect(MiscUtils.getConfigByKey("weixin_pc_no_to_creat_room_ur"));
        }
    }

    /**
     * 获得微信JS端配置
     *
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/weixin/configuration", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWeiXinConfiguration(
            @RequestParam(value = "url", defaultValue = "") String url,
            @RequestHeader("version") String version
    ) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "weiXinConfiguration", null, version);
        Map<String, Object> param = new HashMap<>();
        param.put("url", url);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 生成微信支付单
     *
     * @param accessToken 安全证书
     * @param entity      post参数
     * @param version     版本号
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
            @RequestHeader("version") String version,
            HttpServletRequest request) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "generateWeixinPayBill", accessToken, version);
        String remote_ip_address = MiscUtils.getIpAddr(request);
        ((Map<String, Object>) entity.getBody()).put("remote_ip_address", remote_ip_address);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 微信订单校验
     *
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/payment/weixin/check", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity checkWeixinPayBill(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "checkWeixinPayBill", accessToken, version);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 处理微信支付回调
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/common/payment/weixin/result", method = RequestMethod.POST)
    public void handleWeixinPayResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.info("================> 接收到 weixinNotify 通知。==================");
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            String resultData = MiscUtils.convertStreamToString(request.getInputStream());
            SortedMap<String, String> requestMapData = MiscUtils.requestToMap(resultData);//notify请求数据
            logger.info("===> weixinNotify 请求数据：" + requestMapData);
            RequestEntity requestEntity = this.createResponseEntity("CommonServer", "handleWeixinPayResult", "", "");
            requestEntity.setParam(requestMapData);
            ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
            String result = responseEntity.getReturnData().toString();
            logger.info("===> result Data: " + result);
            out.println(result);
            out.flush();
            out.close();
        } catch (Exception e) {
            logger.error("====================>  weixinNotify 处理异常  ========");
            logger.error(e.getMessage(), e);
            out.flush();
            out.close();
        }
        logger.info("==========================>  weixinNotify 处理完毕。 =======");
    }


    /**
     * @param entity
     * @param access_token
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/common/weixin/resource/conversion", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity convertWeixinResource(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String access_token,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "convertWeixinResource", access_token, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 查询订单
     * @param user_id 用户id
     * @param pre_pay_no wx 预付订单号
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/order", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity  queryOrder (
            @RequestParam(value="user_id", defaultValue="") String user_id,
            @RequestParam(value="pre_pay_no", defaultValue="") String pre_pay_no,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "queryOrder", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("user_id", user_id);
        param.put("pre_pay_no", pre_pay_no);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }



    /**
     * 解析二维码
     * 前端传入公众号文章链接 进行解析 返回二维码链接
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/weChatUrl", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWeChatQrCode(@RequestParam(value="we_chat_url", defaultValue="") String we_chat_url) throws Exception {
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

}
