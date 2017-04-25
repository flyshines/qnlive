package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface CoursesStudentsMapper {
    List<Map<String,Object>> findCourseStudentList(Map<String, Object> queryMap);
    List<Map<String,Object>> findCourseAllStudentList(String course_id);
    List<Map<String,Object>> findCourseListOfStudent(Map<String, Object> queryMap);
    List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap);
    int insertStudent(Map<String, Object> queryMap);
    List<Map<String, Object>> findBanUserListInfo(Map<String, Object> banUserIdList);
    List<Map<String, Object>> findCourseStudentListWithLoginInfo(Map<String, Object> queryMap);
    List<String> findUserIdsByCourseId(String course_id);
    String isStudentOfTheCourse(Map<String, Object> queryMap);

    Map<String,Object> findUserDistributionInfoForDoneNum(Map<String, Object> queryuserDistribution);

    List<Map<String,Object>> findCourseRecommendUsers(Map<String, Object> reqMap);

    Map<String,Object> findCourseRecommendUserNum(Map<String, Object> reqMap);
    
    List<Map<String,Object>> findCourseIdByStudent(Map<String, Object> reqMap);
}