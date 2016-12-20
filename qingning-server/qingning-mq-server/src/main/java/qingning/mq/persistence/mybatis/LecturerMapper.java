package qingning.mq.persistence.mybatis;

import java.util.Map;

public interface LecturerMapper {
    int deleteByPrimaryKey(String lecturerId);

    int insert(Lecturer record);

    int insertSelective(Lecturer record);

    Lecturer selectByPrimaryKey(String lecturerId);

    int updateByPrimaryKeySelective(Lecturer record);

    int updateByPrimaryKey(Lecturer record);

    Map<String,Object> findLectureByLectureId(String user_id);
}