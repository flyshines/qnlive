package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface LecturerCoursesProfitMapper {
	int insertLecturerCoursesProfit(Map<String,Object> record);
	Map<String,Object> findRewardByUserIdAndCourseId(Map<String, Object> rewardQueryMap);
	List<Map<String, Object>> findCourseProfitList(Map<String, Object> queryMap);
	List<Map<String, Object>> findUserConsumeRecords(Map<String, Object> queryMap);

	Map<String,Object> findUserDistributionInfoForLastDoneNum(Map<String, Object> queryuserDistribution);
	
	Map<String,Object> findCoursesSumInfo(Map<String, Object> queryMap);
}