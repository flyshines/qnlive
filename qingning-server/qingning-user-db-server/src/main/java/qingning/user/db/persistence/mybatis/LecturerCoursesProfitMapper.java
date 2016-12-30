package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.LecturerCoursesProfit;

import java.util.List;
import java.util.Map;

public interface LecturerCoursesProfitMapper {
    int deleteByPrimaryKey(String profitId);

    int insert(LecturerCoursesProfit record);

    int insertSelective(LecturerCoursesProfit record);

    LecturerCoursesProfit selectByPrimaryKey(String profitId);

    int updateByPrimaryKeySelective(LecturerCoursesProfit record);

    int updateByPrimaryKey(LecturerCoursesProfit record);

    List<Map<String,Object>> findUserConsumeRecords(Map<String, Object> queryMap);
}