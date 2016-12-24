package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.CourseMessage;

import java.util.List;
import java.util.Map;

public interface CourseMessageMapper {
    int deleteByPrimaryKey(String messageId);

    int insert(CourseMessage record);

    int insertSelective(CourseMessage record);

    CourseMessage selectByPrimaryKey(String messageId);

    int updateByPrimaryKeySelective(CourseMessage record);

    int updateByPrimaryKey(CourseMessage record);

    List<Map<String,Object>> findCourseMessageList(Map<String, Object> queryMap);

    long findCourseMessageMaxPos(String course_id);
}