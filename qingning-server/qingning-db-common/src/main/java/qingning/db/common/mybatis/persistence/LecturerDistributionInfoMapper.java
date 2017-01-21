package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface LecturerDistributionInfoMapper {
    Map<String,Object> findLecturerDistributionByLectureId(String user_id);
    int insertLecturerDistributionInfo(Map<String,Object> record);
    int updateLecturerDistributionInfo(Map<String,Object> record);
}