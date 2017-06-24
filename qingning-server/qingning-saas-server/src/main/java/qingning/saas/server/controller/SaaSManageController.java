package qingning.saas.server.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.Constants;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/manage")
public class SaaSManageController extends AbstractController {

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
     * 店铺-店铺信息
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/info", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity shopInfo(@RequestHeader(value = "access_token") String accessToken,
                             @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
                             @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "shopInfo", accessToken, version, appName);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 店铺-店铺设置
     *
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/edit", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity shopEdit(
            HttpEntity<Object> entity,
            @RequestHeader(value = "access_token") String accessToken,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "shopEdit", accessToken, version, appName);
        if(((Map)entity.getBody()).isEmpty()){
            throw new QNLiveException("000100");
        }
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
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
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "shopBannerList", accessToken, version, appName);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_count", pageSize);
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

    /**
     * 店铺-轮播图编辑
     *
     * @param accessToken
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/{banner_id}/edit", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity shopBannerEdit(
            HttpEntity<Object> entity,
            @PathVariable("banner_id") String bannerId,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "shopBannerEdit", accessToken, version, appName);
        Map<String,String> param = (Map)entity.getBody();
        param.put("banner_id",bannerId);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 店铺-轮播图上下架
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/{banner_id}/updown/{type}", method = RequestMethod.PUT)
    public
    @ResponseBody
    ResponseEntity shopBannerUpdown(
            @PathVariable("banner_id") String bannerId,
            @PathVariable("type") String type,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "shopBannerUpdown", accessToken, version, appName);
        Map<String,String> param = new HashMap<>();
        param.put("banner_id",bannerId);
        param.put("type",type);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 店铺-单品-添加视频、音频
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/video/add", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity addShopSingleVideo(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "addShopSingleVideo", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * 店铺-单品-编辑视频、音频
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/video/edit", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity editShopSingleVideo(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "editShopSingleVideo", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 店铺-单品-新增图文
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/images/add", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity addShopSingleImages(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "addShopSingleVideo", accessToken, version, appName);
        Map<String,String> param = (Map)entity.getBody();
        param.put("type","1");
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * 店铺-单品-编辑图文
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/images/edit", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity editShopSingleImages(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "editShopSingleVideo", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 店铺-单品列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getSingleList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "status",defaultValue = "") String status,
            @RequestParam(value = "type",defaultValue = "") String type,
            @RequestParam(value = "keyword",defaultValue = "") String keyword,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "getSingleList", accessToken, version, appName);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(status))
            paramCode.put("status", status);
        if(StringUtils.isNotEmpty(type))
            paramCode.put("type", type);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("keyword", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 店铺-用户列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getUserList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "type",defaultValue = "") String type,
            @RequestParam(value = "nick_name",defaultValue = "") String keyword,
            @RequestParam(value = "phone",defaultValue = "") String phone,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "getUserList", accessToken, version, appName);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(phone))
            paramCode.put("phone", phone);
        if(StringUtils.isNotEmpty(type))
            paramCode.put("type", type);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("nick_name", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * 店铺-评论列表（用户留言）
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/message/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getMessageList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "nick_name",defaultValue = "") String keyword,
            @RequestParam(value = "course_name",defaultValue = "") String courseName,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "getMessageList", accessToken, version, appName);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(courseName))
            paramCode.put("course_name", courseName);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("nick_name", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 店铺-用户反馈
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/feedback/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getFeedbackList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "nick_name",defaultValue = "") String keyword,
            @RequestParam(value = "comment",defaultValue = "") String comment,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "getFeedbackList", accessToken, version, appName);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(comment))
            paramCode.put("comment", comment);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("nick_name", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * 店铺-单品上下架
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/{course_id}/updown/{type}", method = RequestMethod.PUT)
    public
    @ResponseBody
    ResponseEntity goodsSingleUpdown(
            @PathVariable("course_id") String courseId,
            @PathVariable("type") String type,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "goodsSingleUpdown", accessToken, version, appName);
        Map<String,String> param = new HashMap<>();
        param.put("course_id",courseId);
        param.put("course_updown",type);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 商品-系列-列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/series/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getSeriesList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestParam(value = "status",defaultValue = "") String status,
            @RequestParam(value = "keyword",defaultValue = "") String keyword,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "getSeriesList", accessToken, version, appName);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_size", pageSize);
        paramCode.put("page_num", pageNum);
        if(StringUtils.isNotEmpty(status))
            paramCode.put("status", status);
        if(StringUtils.isNotEmpty(keyword))
            paramCode.put("keyword", keyword);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 商品-系列-详情
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/series/{series_id}/info", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getSeriesDetailInfo(
            @PathVariable("series_id") String series_id,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "getSeriesInfo", accessToken, version,appName);
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("series_id", series_id);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 商品-系列-子课程列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/series/{series_id}/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getSeriesCourseList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "20") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestHeader(value = "app_name", defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "getSeriesCourseList", accessToken, version, appName);
        return this.process(requestEntity, serviceManger, message);
    }

}
