package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.CoursesStudents;
import qingning.lecturer.db.persistence.mybatis.entity.CoursesStudentsKey;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CoursesStudentsMapper {
    int deleteByPrimaryKey(CoursesStudentsKey key);

    int insert(CoursesStudents record);

    int insertSelective(CoursesStudents record);

    CoursesStudents selectByPrimaryKey(CoursesStudentsKey key);

    int updateByPrimaryKeySelective(CoursesStudents record);

    int updateByPrimaryKey(CoursesStudents record);

    List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);

    List<Map<String,Object>> findBanUserListInfo(Set<String> banUserIdList);

    List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap);
}