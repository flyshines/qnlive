package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.LecturerDistributionInfo;

import java.util.Map;

public interface LecturerDistributionInfoMapper {
    int deleteByPrimaryKey(String lecturerId);

    int insert(LecturerDistributionInfo record);

    int insertSelective(LecturerDistributionInfo record);

    LecturerDistributionInfo selectByPrimaryKey(String lecturerId);

    int updateByPrimaryKeySelective(LecturerDistributionInfo record);

    int updateByPrimaryKey(LecturerDistributionInfo record);

    Map<String,Object> findLecturerDistributionByLectureId(String user_id);
}