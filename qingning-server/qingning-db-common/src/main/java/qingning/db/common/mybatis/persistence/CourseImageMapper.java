package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface CourseImageMapper {
	int deletePPTByCourseId(String course_id);
	int createCoursePPTs(Map<String, Object> reqMap);
	Map<String,Object> findOnePPTByCourseId(String course_id);
	List<Map<String,Object>> findPPTListByCourseId(String course_id);
}