package qingning.server.rpc.manager;


import qingning.common.util.MiscUtils;
import qingning.server.rpc.*;

import java.util.List;
import java.util.Map;

public interface IUserUserModuleServer extends UserModuleServer,CourseModuleServer,LecturerModuleServer,ShopModuleServer,SeriesModuleServer,ConfigModuleServer {


	List<Map<String,Object>> findRewardConfigurationList();

	boolean isStudentOfTheCourse(Map<String, Object> studentQueryMap);

	/**
	 * 判断订单
	 * @param record
	 * Created by DavidGong on 2017/7/4.
	 * @return
	 */
	boolean findUserWhetherToPay(Map<String,Object> record);

	/**
	 * 加入课程
	 * @param courseMap
	 * @return
	 */
	Map<String,Object> joinCourse(Map<String, String> courseMap);

	/**
	 * 增加课程人数
	 * @param course_id
	 */
	void increaseStudentNumByCourseId(String course_id);

}
