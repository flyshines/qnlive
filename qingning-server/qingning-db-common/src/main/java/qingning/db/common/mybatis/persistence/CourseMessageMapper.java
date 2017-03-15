package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface CourseMessageMapper {
	List<Map<String,Object>> findCourseMessageList(Map<String, Object> queryMap);
	int insertCourseMessageList(List<Map<String,Object>> list);
	Map<String,Object> findCourseMessageMaxPos(String course_id);
	List<Map<String,Object>> findCourseMessageListByComm(Map<String, Object> queryMap);
	int findCourseMessageSum(Map<String, Object> queryMa);
}