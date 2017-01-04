package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.CoursesStudents;

import java.util.List;
import java.util.Map;

public interface CoursesStudentsMapper {
    int deleteByPrimaryKey(String studentId);

    int insert(CoursesStudents record);

    int insertSelective(CoursesStudents record);

    CoursesStudents selectByPrimaryKey(String studentId);

    int updateByPrimaryKeySelective(CoursesStudents record);

    int updateByPrimaryKey(CoursesStudents record);

    List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);

    List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap);

    List<Map<String,Object>> findStudentCourseList(Map<String, Object> queryMap);

    Map<String,Object> findStudentByCourseIdAndUserId(Map<String, Object> studentQueryMap);
}