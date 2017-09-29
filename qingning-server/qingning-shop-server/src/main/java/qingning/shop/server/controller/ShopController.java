package qingning.shop.server.controller;


import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.Constants;
import qingning.common.util.wxEncrypt.WXBizMsgCrypt;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ShopController extends AbstractController {

    private WXBizMsgCrypt cryptUtil = null;



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
    /**
     * PC_店铺-轮播图列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity shopBannerList(
            @RequestHeader("access_token") String accessToken,
            @RequestParam(value = "page_size", defaultValue = "10") long pageSize,
            @RequestParam(value = "page_num", defaultValue = "1") long pageNum,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerList", accessToken, version);
        Map<String, Object> paramCode = new HashMap<>();
        paramCode.put("page_count", pageSize);
        paramCode.put("page_num", pageNum);
        requestEntity.setParam(paramCode);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * PC_店铺-添加轮播图
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
            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerAdd", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-轮播图编辑
     *
     * @param accessToken
     * @param entity
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/{banner_id}/edit", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity shopBannerEdit(
            HttpEntity<Object> entity,
            @PathVariable("banner_id") String bannerId,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerEdit", accessToken, version);
        Map<String,String> param = (Map)entity.getBody();
        param.put("banner_id",bannerId);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-轮播图详情
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/{banner_id}/info", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity shopBannerInfo(
            @PathVariable(value = "banner_id") String bannerId,
            @RequestHeader(value = "access_token") String accessToken,
            @RequestHeader(value = "version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerInfo", accessToken, version);
        Map<String,String> param = new HashMap<>();
        param.put("banner_id",bannerId);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

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
     * 编辑课程
     * @param entity
     * @param course_id
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/shop/lecturer/courses/{course_id}", method = RequestMethod.PUT)
    public
    @ResponseBody ResponseEntity updateCourse(
            HttpEntity<Object> entity,
            @PathVariable("course_id") String course_id,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "updateCourse", accessToken, version);
        ((Map<String,Object>)entity.getBody()).put("course_id", course_id);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }
  /**
     * PC_店铺-轮播图上下架
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/{banner_id}/updown/{type}", method = RequestMethod.PUT)
    public @ResponseBody ResponseEntity shopBannerUpdown(
            @PathVariable("banner_id") String bannerId,
            @PathVariable("type") String type,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "shopBannerUpdown", accessToken, version);
        Map<String,String> param = new HashMap<>();
        param.put("banner_id",bannerId);
        param.put("type",type);
        requestEntity.setParam(param);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * PC_店铺-添加课程（音频，视频，图文）
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/add", method = RequestMethod.POST)
    public  @ResponseBody ResponseEntity addShopSingleVideo(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "addSingleGoods", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * PC_店铺-系列-添加子课程
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/series/course/add", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity addSeriesCourseChild(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "addSeriesCourseChild", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * PC_店铺-单品-编辑
     *
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/goods/single/edit", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity editShopSingleVideo(
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "editSingleGoods", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
    /**
     * H5_店铺-获取店铺轮播列表
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopBannerList(
            @PathVariable("shop_id") String shopId,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "queryShopBannerList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        requestEntity.setParam(param);

        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * H5_店铺-获取店铺系列课程列表
     * @param shopId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/course/series/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopSeriesList(
            @PathVariable("shop_id") String shopId,
            @RequestParam(value="last_series_id", defaultValue="0")String lastSeriesId,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "findShopSeriesList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_series_id", lastSeriesId);
        param.put("page_count", pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * H5_店铺-获取店铺单品直播课程列表
     * @param shopId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    //TODO 待完善
    @RequestMapping(value = "/shop/course/live_single/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopLiveSingleList(
            @PathVariable("shop_id") String shopId,
            @RequestParam(value="last_update_time", defaultValue="0")long lastUpdateTime,
            @RequestParam(value="readed_count", defaultValue="0")long readedCount,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "findShopLiveSingleList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_update_time", lastUpdateTime);
        param.put("readed_count", readedCount);
        param.put("page_count", pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * H5_店铺-获取店铺单品课程（直播除外）列表
     * @param shopId
     * @param lastSingleId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/course/single/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopSingleList(
            @PathVariable("shop_id") String shopId,
            @RequestParam(value="last_single_id", defaultValue="0")String lastSingleId,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ShopServer", "findShopSingleCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_single_id", lastSingleId);
        param.put("page_count", pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

}
