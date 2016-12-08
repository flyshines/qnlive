package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.CourseAudio;

import java.util.List;
import java.util.Map;

public interface CourseAudioMapper {
    int deleteByPrimaryKey(String audioId);

    int insert(CourseAudio record);

    int insertSelective(CourseAudio record);

    CourseAudio selectByPrimaryKey(String audioId);

    int updateByPrimaryKeySelective(CourseAudio record);

    int updateByPrimaryKey(CourseAudio record);

    List<Map<String,Object>> findAudioListByCourseId(String course_id);
}