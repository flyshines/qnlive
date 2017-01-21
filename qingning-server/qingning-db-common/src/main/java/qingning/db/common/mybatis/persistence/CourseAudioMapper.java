package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface CourseAudioMapper {
    List<Map<String,Object>> findAudioListByCourseId(String course_id);
    int saveCourseAudio(List<Map<String,Object>> list);
}