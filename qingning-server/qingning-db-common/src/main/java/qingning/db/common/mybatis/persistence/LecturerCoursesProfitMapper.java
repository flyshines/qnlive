package qingning.db.common.mybatis.persistence;

import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;

import java.util.List;
import java.util.Map;

public interface LecturerCoursesProfitMapper {
	int insertLecturerCoursesProfit(Map<String,Object> record);
	Map<String,Object> findRewardByUserIdAndCourseId(Map<String, Object> rewardQueryMap);
	List<Map<String, Object>> findCourseProfitList(Map<String, Object> queryMap);
	List<Map<String, Object>> findUserConsumeRecords(Map<String, Object> queryMap);

	Map<String,Object> findUserDistributionInfoForLastDoneNum(Map<String, Object> queryuserDistribution);
	
	Map<String,Object> findCoursesSumInfo(Map<String, Object> queryMap);

	List<Map<String,Object>> findUserIncomeRecords(Map<String, Object> queryMap);

	List<Map<String,Object>> findUserBuiedRecords(Map<String, Object> query);

	/**查询用户的收益订单记录
	 * @return
	 * @param param
	 * @param page
	 */
	PageList<Map<String,Object>> selectOrdersListByUserId(Map<String, Object> param, PageBounds page);
}