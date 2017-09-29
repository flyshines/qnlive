package qingning.user.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.Constants;
import qingning.server.AbstractController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController extends AbstractController {
    /***************************** V2.0.0 ********************************/
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
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "mySingleCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id",shopId);
        param.put("last_course_id",lastCourseId);
        param.put("page_count",pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 获取已购买的系列课程
     * @param shopId
     * @param lastSeriesId
     * @param pageCount
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/user/my_course/series/list", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity mySeriesCourseList(
            @RequestParam(value = "shop_id", defaultValue = "") String shopId,
            @RequestParam(value = "last_series_id", defaultValue = "") String lastSeriesId,
            @RequestParam(value = "page_count", defaultValue = "20") long pageCount,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "mySeriesCourseList", accessToken, version);
        Map<String, Object> param = new HashMap<>();
        param.put("shop_id",shopId);
        param.put("last_series_id",lastSeriesId);
        param.put("page_count",pageCount);
        requestEntity.setParam(param);
        return this.process(requestEntity, serviceManger, message);
    }

    /**
     * 用户加入课程
     * @param course_id
     * @param entity
     * @param accessToken
     * @param version
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/user/study/courses/{course_id}", method = RequestMethod.PUT)
    public
    @ResponseBody
    ResponseEntity joinCourse(
            @PathVariable("course_id") String course_id,
            HttpEntity<Object> entity,
            @RequestHeader("access_token") String accessToken,
            @RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "joinCourse", accessToken, version);
        ((Map<String,Object>)entity.getBody()).put("course_id", course_id);
        requestEntity.setParam(entity.getBody());
        return this.process(requestEntity, serviceManger, message);
    }

}
