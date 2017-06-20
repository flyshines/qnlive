package qingning.db.common.mybatis.persistence;


import java.util.Map;

public interface SaaSCourseMapper {
    int deleteByPrimaryKey(String courseId);

    int insert(Map<String,Object> record);

    Map<String,Object> selectByPrimaryKey(String courseId);

    int updateByPrimaryKey(Map<String,Object> record);
}