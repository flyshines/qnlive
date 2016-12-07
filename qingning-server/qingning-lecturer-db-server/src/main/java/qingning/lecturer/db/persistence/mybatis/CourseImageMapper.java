package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.CourseImage;

import java.util.Map;

public interface CourseImageMapper {
    int deleteByPrimaryKey(String imageId);

    int insert(CourseImage record);

    int insertSelective(CourseImage record);

    CourseImage selectByPrimaryKey(String imageId);

    int updateByPrimaryKeySelective(CourseImage record);

    int updateByPrimaryKey(CourseImage record);

    void batchInsertPPT(Map<String, Object> reqMap);
}