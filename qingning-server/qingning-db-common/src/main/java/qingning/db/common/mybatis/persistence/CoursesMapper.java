package qingning.db.common.mybatis.persistence;
import java.util.List;
import java.util.Map;

public interface CoursesMapper {
	Map<String,Object> findCourseByCourseId(String courseId);
	int insertCourse(Map<String,Object> record);
	int updateCourse(Map<String,Object> record);
	int updateAfterStudentBuyCourse(Map<String,Object> record);	
	int updateAfterStudentRewardCourse(Map<String,Object> record);
	int increaseStudent(String course_id);
	List<Map<String,Object>> findCourseListForLecturer(Map<String,Object> record);
	List<Map<String,Object>> findLecturerCourseList(Map<String,Object> record);
	List<Map<String,Object>> findPlatformCourseList(Map<String,Object> record);
}