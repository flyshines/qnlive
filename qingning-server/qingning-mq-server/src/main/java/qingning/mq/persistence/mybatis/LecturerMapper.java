package qingning.mq.persistence.mybatis;

import qingning.mq.persistence.entity.Lecturer;

import java.util.List;
import java.util.Map;

public interface LecturerMapper {
    int deleteByPrimaryKey(String lecturerId);

    int insert(Lecturer record);

    int insertSelective(Lecturer record);

    Lecturer selectByPrimaryKey(String lecturerId);

    int updateByPrimaryKeySelective(Lecturer record);

    int updateByPrimaryKey(Lecturer record);

    Map<String,Object> findLectureByLectureId(String user_id);
    List<Map<String,Object>> findLectureId(Map<String,Object> query);
}