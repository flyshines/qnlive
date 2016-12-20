package qingning.mq.persistence.mybatis;


import qingning.mq.persistence.entity.LiveRoom;

import java.util.Map;

public interface LiveRoomMapper {
    int deleteByPrimaryKey(String roomId);

    int insert(LiveRoom record);

    int insertSelective(LiveRoom record);

    LiveRoom selectByPrimaryKey(String roomId);

    int updateByPrimaryKeySelective(LiveRoom record);

    int updateByPrimaryKey(LiveRoom record);

    Map<String,Object> findLiveRoomByRoomId(String room_id);
}