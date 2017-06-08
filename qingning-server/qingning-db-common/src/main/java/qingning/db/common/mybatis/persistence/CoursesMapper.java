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

	List<Map<String,Object>> findCourseByClassifyId(Map<String,Object> record);
	List<Map<String, Object>> findCourseByStatus(Map<String,Object> record);
	/**
	 * 后台_搜索课程列表(同时搜索课程名、课程id)
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> findCourseListBySearch(Map<String, Object> reqMap);

}