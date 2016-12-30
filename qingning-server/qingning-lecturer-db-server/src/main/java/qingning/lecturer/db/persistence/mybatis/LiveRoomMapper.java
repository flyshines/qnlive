package qingning.lecturer.db.persistence.mybatis;


import qingning.lecturer.db.persistence.mybatis.entity.LiveRoom;

import java.util.List;
import java.util.Map;

public interface LiveRoomMapper {
    int deleteByPrimaryKey(String roomId);

    int insert(LiveRoom record);

    int insertSelective(LiveRoom record);

    LiveRoom selectByPrimaryKey(String roomId);

    int updateByPrimaryKeySelective(LiveRoom record);

    int updateByPrimaryKey(LiveRoom record);

    Map<String,Object> findLiveRoomByRoomId(String room_id);
    
    List<Map<String,Object>> findRoomDistributerInfo(Map<String,Object> paramters);
    List<Map<String,Object>> findRoomDistributerCourseInfo(Map<String,Object> paramters);
    List<Map<String,Object>> findLiveRoomByLectureId(String lecture_id); 
    List<Map<String,Object>> findRoomFanList(Map<String,Object> paramters);
}