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
	List<Map<String,Object>> findFinishCourseListForLecturer(Map<String,Object> record);	
	List<Map<String,Object>> findLecturerCourseListByStatus(Map<String,Object> record);
	Map<String,Object> findLastestFinishCourse(Map<String,Object> record);
	List<Map<String,Object>> findCourseBySearch(Map<String,Object> record);
	List<Map<String,Object>> findCourseByRecommend(String appName);
	List<Map<String,Object>> findCourseByClassifyId(Map<String,Object> record);
	List<Map<String, Object>> findCourseByStatus(Map<String,Object> record);

}