
package qingning.lecturer.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import qingning.common.util.MiscUtils;
import qingning.lecturer.db.persistence.mybatis.LecturerMapper;
import qingning.lecturer.db.persistence.mybatis.LiveRoomMapper;
import qingning.lecturer.db.persistence.mybatis.LoginInfoMapper;
import qingning.lecturer.db.persistence.mybatis.UserMapper;
import qingning.lecturer.db.persistence.mybatis.entity.Lecturer;
import qingning.lecturer.db.persistence.mybatis.entity.LiveRoom;
import qingning.lecturer.db.persistence.mybatis.entity.LoginInfo;
import qingning.server.rpc.manager.ILectureModuleServer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LectureModuleServerImpl implements ILectureModuleServer {

	@Autowired(required = true)
	private LecturerMapper lecturerMapper;

	@Autowired(required = true)
	private LiveRoomMapper liveRoomMapper;

	@Autowired(required = true)
	private UserMapper userMapper;

	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;

	@Override
	/**
	 * 创建直播间
	 */
	public Map<String,Object> createLiveRoom(Map<String, Object> reqMap) {
		Map<String,Object> userMap = null;
		Date now = new Date();
		//1.插入直播间表
		LiveRoom liveRoom = new LiveRoom();
		liveRoom.setRoomId(MiscUtils.getUUId());
		liveRoom.setLecturerId(reqMap.get("user_id").toString());
		liveRoom.setCourseNum(0L);
		liveRoom.setFansNum(0L);
		liveRoom.setDistributerNum(0L);

		liveRoom.setRqCode(liveRoom.getRoomId());
		liveRoom.setRoomAddress(reqMap.get("room_address").toString() + liveRoom.getRoomId());//TODO
		liveRoom.setTotalAmount(0.0);
		liveRoom.setLastCourseAmount(0.0);
		liveRoom.setCreateTime(now);
		liveRoom.setUpdateTime(now);

		//直播间名字、直播间头像地址、直播间简介的相关设置。
		//如果有输入的参数则使用输入的参数，否则使用t_user表中的数据
		if(reqMap.get("room_name") == null || reqMap.get("avatar_address") == null
				|| reqMap.get("room_remark") == null){
			userMap = userMapper.findByUserId(reqMap.get("user_id").toString());
		}
		if(reqMap.get("room_name") == null){
			liveRoom.setRoomName(userMap.get("nick_name").toString() + "的直播间");
		}else {
			liveRoom.setRoomName(reqMap.get("room_name").toString() + "的直播间");
		}

		if(reqMap.get("avatar_address") == null){
			liveRoom.setAvatarAddress((userMap.get("avatar_address").toString()));
		}else {
			liveRoom.setAvatarAddress((reqMap.get("avatar_address").toString()));
		}

		if(reqMap.get("room_remark") != null){
			liveRoom.setRoomRemark(reqMap.get("room_remark").toString());
		}
		liveRoomMapper.insert(liveRoom);

		//2.如果该用户为普通用户，则需要插入讲师表，并且修改登录信息表中的身份
		if(reqMap.get("user_role") == null || reqMap.get("user_role").toString().split(",").length == 1){
			//2.1插入讲师表
			Lecturer lecturer = new Lecturer();
			lecturer.setLecturerId(reqMap.get("user_id").toString());
			lecturer.setCourseNum(0L);
			lecturer.setTotalStudentNum(0L);
			lecturer.setLiveRoomNum(0L);
			lecturer.setFansNum(0L);
			lecturer.setTotalAmount(0.0);
			lecturer.setCreateTime(now);
			lecturer.setUpdateTime(now);
			lecturerMapper.insert(lecturer);

			//2.2修改登录信息表 身份
			LoginInfo updateLoginInfo = new LoginInfo();
			updateLoginInfo.setUserId(reqMap.get("user_id").toString());
			updateLoginInfo.setUserRole("normal_user,lecture");
			updateLoginInfo.setUpdateTime(now);
			loginInfoMapper.updateByPrimaryKeySelective(updateLoginInfo);
		}

		Map<String,Object> resultMap = new HashMap<String,Object>();
		resultMap.put("room_id", liveRoom.getRoomId());
		return resultMap;
	}

	@Override
	public Map<String, Object> findLectureByLectureId(String user_id) {
		return lecturerMapper.findLectureByLectureId(user_id);
	}

	@Override
	public Map<String, Object> findLiveRoomByRoomId(String room_id) {
		return liveRoomMapper.findLiveRoomByRoomId(room_id);
	}

	@Override
	/**
	 * 更新直播间
	 */
	public Map<String, Object> updateLiveRoom(Map<String, Object> reqMap) {

		LiveRoom updateLiveRoom = new LiveRoom();
		updateLiveRoom.setRoomId(reqMap.get("room_id").toString());
		updateLiveRoom.setUpdateTime(new Date());

		if(reqMap.get("avatar_address") != null ){
			updateLiveRoom.setAvatarAddress(reqMap.get("avatar_address").toString());
		}
		if(reqMap.get("room_name") != null ){
			updateLiveRoom.setRoomName(reqMap.get("room_name").toString());
		}
		if(reqMap.get("room_remark") != null ){
			updateLiveRoom.setRoomRemark(reqMap.get("room_remark").toString());
		}

		Integer updateCount = liveRoomMapper.updateByPrimaryKeySelective(updateLiveRoom);
		Map<String,Object> dbResultMap = new HashMap<String,Object>();
		dbResultMap.put("updateCount", updateCount);
		dbResultMap.put("update_time", updateLiveRoom.getUpdateTime());
		return dbResultMap;
	}
}
