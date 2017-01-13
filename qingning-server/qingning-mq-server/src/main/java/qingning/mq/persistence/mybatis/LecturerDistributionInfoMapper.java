package qingning.mq.persistence.mybatis;

import java.util.Map;

import qingning.mq.persistence.entity.LecturerDistributionInfo;

public interface LecturerDistributionInfoMapper {
    int deleteByPrimaryKey(String lecturerId);

    int insert(LecturerDistributionInfo record);

    int insertSelective(LecturerDistributionInfo record);

    LecturerDistributionInfo selectByPrimaryKey(String lecturerId);

    int updateByPrimaryKeySelective(LecturerDistributionInfo record);

    int updateByPrimaryKey(LecturerDistributionInfo record);

    Map<String,Object> findLecturerDistributionByLectureId(String user_id);
}