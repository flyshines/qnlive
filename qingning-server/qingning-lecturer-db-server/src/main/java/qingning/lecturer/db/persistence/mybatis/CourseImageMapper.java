package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.CourseImage;

import java.util.List;
import java.util.Map;

public interface CourseImageMapper {
    int deleteByPrimaryKey(String imageId);

    int insert(CourseImage record);

    int insertSelective(CourseImage record);

    CourseImage selectByPrimaryKey(String imageId);

    int updateByPrimaryKeySelective(CourseImage record);

    int updateByPrimaryKey(CourseImage record);

    void batchInsertPPT(Map<String, Object> reqMap);

    Map<String,Object> findOnePPTByCourseId(String course_id);

    void deletePPTByCourseId(String course_id);

    List<Map<String,Object>> findPPTListByCourseId(String course_id);
}