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

    List<Map<String,Object>> findUserShopRecords(Map<String, Object> queryMap);

	List<Map<String,Object>> findUserConsumeRecords(Map<String, Object> queryMap);

	Map<String,Object> findUserGainsByUserId(String userId);

	Map<String,Object> findWithdrawCashByMap(Map<String, Object> selectMap);

	/**
	 * 插入提现申请表
	 * @param record
	 * @param balance
	 * @return
	 */
	int insertWithdrawCash(Map<String, Object> record, long balance);

	List<Map<String, Object>> findWithdrawList(Map<String, Object> param);

	Map<String,Object> findWithdrawListAll(Map<String, Object> param);

	Map<String,Object> selectAdminUserById(String adminId);

	Map<String,Object> selectWithdrawSizeById(Map<String, Object> selectMap);

	int updateWithdraw(String withdrawId, String remark, String userId, String result, Long initial_amount, String adminId, String role, String adminName);

	Map<String,Object> findOrderListAll(Map<String, Object> param);
}
