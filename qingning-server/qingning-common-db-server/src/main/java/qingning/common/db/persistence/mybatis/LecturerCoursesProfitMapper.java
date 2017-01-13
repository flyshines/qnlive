package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.LecturerCoursesProfit;

import java.util.Map;

public interface LecturerCoursesProfitMapper {
    int deleteByPrimaryKey(String profitId);

    int insert(LecturerCoursesProfit record);

    int insertSelective(LecturerCoursesProfit record);

    LecturerCoursesProfit selectByPrimaryKey(String profitId);

    int updateByPrimaryKeySelective(LecturerCoursesProfit record);

    int updateByPrimaryKey(LecturerCoursesProfit record);

    Map<String,Object> findRewardByUserIdAndCourseId(Map<String, Object> rewardQueryMap);
}