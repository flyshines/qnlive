package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.CoursesStudents;
import qingning.user.db.persistence.mybatis.entity.CoursesStudentsKey;

import java.util.Map;

public interface CoursesStudentsMapper {
    int deleteByPrimaryKey(CoursesStudentsKey key);

    int insert(CoursesStudents record);

    int insertSelective(CoursesStudents record);

    CoursesStudents selectByPrimaryKey(CoursesStudentsKey key);

    int updateByPrimaryKeySelective(CoursesStudents record);

    int updateByPrimaryKey(CoursesStudents record);

    Map<String,Object> findStudentByKey(CoursesStudentsKey key);
}