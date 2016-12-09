package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.CoursesStudents;
import qingning.lecturer.db.persistence.mybatis.entity.CoursesStudentsKey;

import java.util.List;
import java.util.Map;

public interface CoursesStudentsMapper {
    int deleteByPrimaryKey(CoursesStudentsKey key);

    int insert(CoursesStudents record);

    int insertSelective(CoursesStudents record);

    CoursesStudents selectByPrimaryKey(CoursesStudentsKey key);

    int updateByPrimaryKeySelective(CoursesStudents record);

    int updateByPrimaryKey(CoursesStudents record);

    List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);
}