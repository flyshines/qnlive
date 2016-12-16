package qingning.mq.persistence.mybatis;


import qingning.mq.persistence.entity.Courses;

import java.util.List;
import java.util.Map;

public interface CoursesMapper {
    int deleteByPrimaryKey(String courseId);

    int insert(Courses record);

    int insertSelective(Courses record);

    Courses selectByPrimaryKey(String courseId);

    int updateByPrimaryKeySelective(Courses record);

    int updateByPrimaryKey(Courses record);

    Map<String,Object> findCourseByCourseId(String courseId);

    List<Map<String,Object>> findCourseListForLecturer(Map<String, Object> queryMap);

    List<Map<String,Object>> findLecturerCourseList(Map<String, Object> queryMap);

    List<Map<String,Object>> findPlatformCourseList(Map<String, Object> queryMap);
}