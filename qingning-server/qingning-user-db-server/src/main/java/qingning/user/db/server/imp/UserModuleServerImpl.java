
package qingning.user.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import qingning.server.rpc.manager.IUserModuleServer;
import qingning.user.db.persistence.mybatis.FansMapper;
import qingning.user.db.persistence.mybatis.UserMapper;
import qingning.user.db.persistence.mybatis.entity.Fans;
import qingning.user.db.persistence.mybatis.entity.FansKey;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserModuleServerImpl implements IUserModuleServer {

	@Autowired(required = true)
	private FansMapper fansMapper;

	@Autowired(required = true)
	private UserMapper userMapper;


	@Override
	public Map<String,Object> userFollowRoom(Map<String, Object> reqMap) {
		Map<String,Object> dbResultMap = new HashMap<String,Object>();

		//follow_type 关注操作类型 0关注 1不关注
		if(reqMap.get("follow_type").toString().equals("0")){
			Date now = new Date();
			Fans fans = new Fans();
			fans.setFansId(reqMap.get("user_id").toString());
			fans.setLecturerId(reqMap.get("lecturer_id").toString());
			fans.setRoomId(reqMap.get("room_id").toString());
			fans.setCreateTime(now);
			fans.setCreateDate(now);
			Integer updateCount = fansMapper.insert(fans);
			dbResultMap.put("update_count", updateCount);
			return dbResultMap;
		}else {
			FansKey fansKey = new FansKey();
			fansKey.setRoomId(reqMap.get("room_id").toString());
			fansKey.setFansId(reqMap.get("user_id").toString());
			fansKey.setLecturerId(reqMap.get("lecturer_id").toString());
			Integer updateCount = fansMapper.deleteByPrimaryKey(fansKey);
			dbResultMap.put("update_count", updateCount);
			return dbResultMap;
		}
	}

	@Override
	public void updateLiveRoomNumForUser(Map<String, Object> reqMap) {
		userMapper.updateLiveRoomNumForUser(reqMap);
	}
}
