package qingning.mq.persistence.mybatis;


import qingning.mq.persistence.entity.CourseImage;

import java.util.List;
import java.util.Map;

public interface CourseImageMapper {
    int deleteByPrimaryKey(String imageId);

    int insert(CourseImage record);

    int insertSelective(CourseImage record);

    CourseImage selectByPrimaryKey(String imageId);

    int updateByPrimaryKeySelective(CourseImage record);

    int updateByPrimaryKey(CourseImage record);

    void batchInsertPPT(List<Map<String, Object>> list);

    Map<String,Object> findOnePPTByCourseId(String course_id);

    void deletePPTByCourseId(String course_id);

    List<Map<String,Object>> findPPTListByCourseId(String course_id);
}