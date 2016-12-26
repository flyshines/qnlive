package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.CourseMessage;

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

    Map<String,Object> findCourseMessageMaxPos(String course_id);
}