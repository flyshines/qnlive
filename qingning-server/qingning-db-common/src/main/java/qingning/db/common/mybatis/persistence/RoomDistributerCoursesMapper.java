package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface RoomDistributerCoursesMapper {
	List<Map<String, Object>> findRoomDistributerCourseInfo(Map<String, Object> paramters);
	int insertRoomDistributerCourses(Map<String, Object> paramters);
}
