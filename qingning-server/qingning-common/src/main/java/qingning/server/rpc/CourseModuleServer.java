package qingning.server.rpc;

import java.util.List;
import java.util.Map;

/**
 * Created by Rouse on 2017/9/22.
 */
public interface CourseModuleServer {
    Map<String,Object> findLastestFinishCourse(Map<String,Object> record);
    /**
     * 根据条件获取嘉宾课程列表，并关联查询出课程详情
     * @param reqMap
     * @return
     */
    List<Map<String, Object>> getGuestAndCourseInfoByMap(Map<String, Object> reqMap);
    Map<String, Object> findSaasCourseByCourseId(String courseId);
    /**
     * 根据条件查询课程列表
     * @param reqMap
     * @return
     */
    List<Map<String, Object>> getCourseListByMap(Map<String, Object> reqMap);

    Map<String,Object> findCourseByCourseId(String courseId);












}
