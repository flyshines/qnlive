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
     * 店铺-获取店铺轮播列表
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/banner/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopBannerList(
    		@PathVariable("shop_id") String shopId,
    	    @RequestHeader("access_token") String accessToken,
    	    @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    	    @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "queryShopBannerList", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        requestEntity.setParam(param);
        
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 店铺-获取店铺系列课程列表
     * @param shopId
     * @param lastUpdateTime
     * @param readedCount
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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "findShopSeriesList", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_series_id", lastSeriesId);
        param.put("page_count", pageCount);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
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
