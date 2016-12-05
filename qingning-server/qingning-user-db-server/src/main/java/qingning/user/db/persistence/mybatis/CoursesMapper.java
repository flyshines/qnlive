package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.Courses;

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
}