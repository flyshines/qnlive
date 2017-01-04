package qingning.user.db.persistence.mybatis;



import qingning.user.db.persistence.mybatis.entity.LiveRoom;

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