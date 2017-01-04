package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.CoursesStudents;

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
}