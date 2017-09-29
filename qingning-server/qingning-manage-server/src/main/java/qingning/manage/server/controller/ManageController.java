package qingning.manage.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.Constants;
import qingning.server.AbstractController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ManageController extends AbstractController {

    /**
     * 获取已购买的单品课程
     * @param shopId
     * @param lastCourseId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/my_course/single/list", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity mySingleCourseList(
            @RequestParam(value = "shop_id", defaultValue = "") String shopId,
            @RequestParam(value = "last_course_id", defaultValue = "") String lastCourseId,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "mySingleCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id",shopId);
        param.put("last_course_id",lastCourseId);
        param.put("page_count",pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }


    /**
     * 后台_搜索banner列表
     * @param bannerName
     * @param status
     * @param bannerType
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/banner/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getBannerListBySearch(
            @RequestParam(value="banner_name", defaultValue="") String bannerName,
            @RequestParam(value="status", defaultValue="-1") int status,
            @RequestParam(value="banner_type", defaultValue="-1") int bannerType,
            @RequestParam(value="page_count", defaultValue="20") long pageCount,
            @RequestParam(value="page_num", defaultValue="1") long pageNum,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("ManageServer", "getBannerListBySearch", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("banner_name", bannerName);
        param.put("status", status);
        param.put("banner_type", bannerType);
        param.put("page_count", pageCount);
        param.put("page_num", pageNum);
        requestEntity.setParam(param);

        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 后台_更新banner
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/common/banner/update", method=RequestMethod.PUT)
    public @ResponseBody ResponseEntity updateBannerInfo(
            HttpEntity<Object> entity,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "updateBannerInfo", accessToken, version);
        requestEntity.setParam(entity.getBody());
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        reqMap.put("bannerJumpCoursePre", ((Map<String, Object>)systemConfigMap.get("bannerJumpCoursePre")).get("config_value").toString());
        reqMap.put("bannerJumpRoomPre", ((Map<String, Object>)systemConfigMap.get("bannerJumpRoomPre")).get("config_value").toString());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 后台_快速更新banner
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value="/common/banner/fast_update", method=RequestMethod.PUT)
    public @ResponseBody ResponseEntity fastUpdateBannerInfo(
            HttpEntity<Object> entity,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "fastUpdateBannerInfo", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 后台_移除banner
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value="/common/banner/remove", method=RequestMethod.DELETE)
    public @ResponseBody ResponseEntity removeBannerInfo(
            HttpEntity<Object> entity,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "removeBannerInfo", accessToken, version);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 后台_课程搜索查询
     * @param searchParam
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/course/searcher", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getCourseListBySearch(
            @RequestParam(value="search_param", defaultValue="") String searchParam,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getCourseListBySearch", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("search_param", searchParam);
        requestEntity.setParam(param);

        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

    /**
     * 后台_直播间搜索查询
     * @param searchParam
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/common/live_room/searcher", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getLiveRoomListBySearch(
            @RequestParam(value="search_param", defaultValue="") String searchParam,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "getLiveRoomListBySearch", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("search_param", searchParam);
        requestEntity.setParam(param);

        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 管理后台登录
     * @param entity
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/sys/login", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity adminUserLogin(
            HttpEntity<Object> entity) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "adminUserLogin", null, null);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


    /**
     * 后台_新增轮播
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value="/common/banner/new", method=RequestMethod.POST)
    public @ResponseBody ResponseEntity addBanner(
            HttpEntity<Object> entity,
            @RequestHeader(value="access_token", defaultValue="") String accessToken,
            @RequestHeader(value="version", defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("CommonServer", "addBanner", accessToken, version);
        requestEntity.setParam(entity.getBody());
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        reqMap.put("bannerJumpCoursePre", ((Map<String, Object>)systemConfigMap.get("bannerJumpCoursePre")).get("config_value").toString());
        reqMap.put("bannerJumpRoomPre", ((Map<String, Object>)systemConfigMap.get("bannerJumpRoomPre")).get("config_value").toString());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


}
