package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface CourseGuestMapper {
	/**
	 * 查询课程嘉宾
	 * @param user_id  根据嘉宾用户id
	 * @return
	 */
	List<Map<String, Object>> findCourseGuestByUserId(String user_id);

	/**
	 * 查询课程嘉宾
	 * @param course_id 课程id
	 * @return
	 */
	List<Map<String, Object>> findCourseGuestByCourseId(String course_id);

	/**
	 * 根据课程id 和用户id 查找这个人
	 * @param map
	 * @return
	 */
	Map<String, Object> findCourseGuestByUserAndCourse(Map<String, Object> map);


	/**
	 * 增加课程嘉宾
	 * @param courseGuestMap
	 * @return
	 */
	int insertCourseGurest(Map<String, Object> courseGuestMap);

	/**
	 * 修改课程嘉宾
	 * @param courseGuestMap
	 * @return
	 */
	int updateCourseGuest(Map<String, Object> courseGuestMap);

	/**
	 * 获取课程列表
	 * @param map
	 * @return
	 */
	List<Map<String, Object>> findGuestCourses(Map<String, Object> map);
	
	/**
	 * 根据条件获取嘉宾课程列表，并关联查询出课程详情
	 * @param reqMap
	 * @return
	 */
	List<Map<String, Object>> findGuestAndCourseInfoByMap(Map<String, Object> reqMap);
	/**
	 * 根据条件获取课程嘉宾记录
	 * @param selectCourseGuestMap
	 * @return
	 */
	List<Map<String, Object>> findGuestCourseByMap(Map<String, Object> selectCourseGuestMap);


}
