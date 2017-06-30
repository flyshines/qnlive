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
/**
 * 前端SaaSController
 * @author huangyh
 *
 */
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
     * 店铺-获取店铺单品课程（直播除外）列表
     * @param shopId
     * @param lastSingleId
     * @param pageCount
     * @param accessToken
     * @param appName
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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "findShopSingleCourseList", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_single_id", lastSingleId);
        param.put("page_count", pageCount);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 店铺-获取店铺单品直播课程列表
     * @param shopId
     * @param lastLiveId
     * @param pageCount
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/shop/course/live_single/list/{shop_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getShopLiveSingleList(
			@PathVariable("shop_id") String shopId,
			@RequestParam(value="last_update_time", defaultValue="0")long lastUpdateTime,
			@RequestParam(value="readed_count", defaultValue="0")long readedCount,
			@RequestParam(value = "page_count", defaultValue = "20") long pageCount,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "findShopLiveSingleList", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id", shopId);
        param.put("last_update_time", lastUpdateTime);
        param.put("readed_count", readedCount);
        param.put("page_count", pageCount);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 课程-获取系列课程详情
     * @param seriesId
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/course/series/detail/{series_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getSeriesCourseDetail(
			@PathVariable("series_id") String seriesId,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "findSeriesCourseDetail", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("series_id", seriesId);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 课程-获取系列课程内容课程列表
     */
    @RequestMapping(value = "/course/series/course_list/{series_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getSeriesCourseList(
			@PathVariable("series_id") String seriesId,
			@RequestParam(value="last_course_id", defaultValue="0")String lastCourseId,
			@RequestParam(value = "page_count", defaultValue = "20") long pageCount,
			@RequestParam(value = "series_type", defaultValue = "") String seriesType,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "findSeriesCourseList", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("series_id", seriesId);
        param.put("last_course_id", lastCourseId);
        param.put("page_count", pageCount);
        param.put("series_type", seriesType);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 课程-获取单品课程详情
     * @param singleId
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/course/single/detail/{single_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getSingleCourseDetail(
			@PathVariable("single_id") String singleId,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "findSingleCourseDetail", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("single_id", singleId);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    
    /**
     * 课程-获取图文课程内容
     * @param articleId
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/course/article/vod/{article_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity vodArticleCourse(
			@PathVariable("article_id") String articleId,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "vodArticleCourse", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("article_id", articleId);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 课程-获取课程内容（音频或视频）
     * @param courseId
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/course/vod/{course_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity vodCourse(
			@PathVariable("course_id") String courseId,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "vodCourse", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("course_id", courseId);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 课程-获取课程留言列表
     * @param courseId
     * @param lastMessageId
     * @param pageCount
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/course/message/list/{course_id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity getCourseMessageList(
			@PathVariable("course_id") String courseId,
			@RequestParam(value="last_message_id", defaultValue="0") String lastMessageId,
			@RequestParam(value="page_count", defaultValue="20") long pageCount,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "findCourseMessageList", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("course_id", courseId);
        param.put("last_message_id", lastMessageId);
        param.put("page_count", pageCount);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 课程-添加课程留言
     * @param entity
     * @param courseId
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/course/message/new/{course_id}", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity addMessageForCourse(
    		HttpEntity<Object> entity,
			@PathVariable("course_id") String courseId,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "addMessageForCourse", accessToken, version, appName);
        Map<String, Object> param = (Map<String, Object>) entity.getBody();
        param.put("course_id", courseId);
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    
    /**
     * 用户-提交反馈与建议
     * @param entity
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/feedback/new", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity addFeedback(
    		HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "addFeedback", accessToken, version, appName);
        Map<String, Object> param = (Map<String, Object>) entity.getBody();
        
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 用户-访问店铺
     * @param entity
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/vsit", method = RequestMethod.PUT)
    public @ResponseBody ResponseEntity userVisit(
    		HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "userVisit", accessToken, version, appName);
        Map<String, Object> param = (Map<String, Object>) entity.getBody();
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 用户- 单品已购
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/buy/single/{shop_id}/list", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity buiedSingleList(
            @PathVariable("shop_id") String shopId,
            @RequestParam(value = "position", defaultValue = "") String position,
            @RequestParam(value = "page_count", defaultValue = "20") long page_count,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "buiedSingleList", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id",shopId);
        param.put("position",position);
        param.put("page_count",page_count);
        param.put("type","1");
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    /**
     * 用户-系列已购
     * @param accessToken
     * @param appName
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/buy/series/{shop_id}/list", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity buiedSeriesList(
            @PathVariable("shop_id") String shopId,
            @RequestParam(value = "position", defaultValue = "") String position,
            @RequestParam(value = "page_count", defaultValue = "20") long page_count,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "buiedSeriesList", accessToken, version, appName);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id",shopId);
        param.put("page_count",page_count);
        param.put("position",position);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }
    
}
