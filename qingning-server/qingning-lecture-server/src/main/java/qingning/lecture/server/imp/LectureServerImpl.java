package qingning.lecture.server.imp;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.Constants;
import qingning.common.util.IMMsgUtil;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ILectureModuleServer;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LectureServerImpl extends AbstractQNLiveServer {
	
	private ILectureModuleServer lectureModuleServer;

	@Override
	public void initRpcServer() {
		if(lectureModuleServer == null){
			lectureModuleServer = this.getRpcService("lectureModuleServer");
		}
	}


	@SuppressWarnings("unchecked")
	@FunctionName("createLiveRoom")
	public Map<String,Object> createLiveRoom (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		reqMap.put("room_address","");//TODO
		//0.目前校验每个讲师仅能创建一个直播间
		//1.缓存中读取直播间信息
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("lecturer_id", userId);
		String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);

		Jedis jedis = jedisUtils.getJedis();
		if(jedis.exists(lectureLiveRoomKey)){
			throw new QNLiveException("100006");
		}

		//2.数据库修改
		//2.如果为新讲师用户，插入讲师表。插入直播间表。更新登录信息表中的用户身份
		map.clear();
		map.put("access_token", reqEntity.getAccessToken());
		String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
		String user_role = jedis.hget(accessTokenKey, "user_role");

		reqMap.put("user_role",user_role);
		reqMap.put("user_id", userId);
		Map<String,Object> createResultMap = lectureModuleServer.createLiveRoom(reqMap);

		//3.缓存修改
		//如果是刚刚成为讲师，则增加讲师信息缓存，并且修改access_token缓存中的身份
		if(reqMap.get("user_role") == null || reqMap.get("user_role").toString().split(",").length == 1){
			Map<String,Object> lectureObjectMap = lectureModuleServer.findLectureByLectureId(reqMap.get("user_id").toString());
			Map<String,String> lectureStringMap = new HashMap<String,String>();
			MiscUtils.converObjectMapToStringMap(lectureObjectMap, lectureStringMap);
			map.clear();
			map.put("lecturer_id", reqMap.get("user_id").toString());
			String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
			jedis.hmset(lectureKey, lectureStringMap);

			jedis.hset(accessTokenKey, "user_role", "normal_user,lecture");
		}

		//增加讲师直播间信息缓存
		String room_id = createResultMap.get("room_id").toString();
		Map<String,Object> liveRoomObjectMap = lectureModuleServer.findLiveRoomByRoomId(room_id);
		Map<String,String> liveRoomStringMap = new HashMap<String,String>();
		MiscUtils.converObjectMapToStringMap(liveRoomObjectMap, liveRoomStringMap);
		map.clear();
		map.put("room_id", createResultMap.get("room_id").toString());
		String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
		jedis.hmset(liveRoomKey, liveRoomStringMap);

		//增加讲师直播间对应关系缓存(一对多关系)
		jedis.hset(lectureLiveRoomKey, createResultMap.get("room_id").toString(), "1");

		return resultMap;
	}


	@SuppressWarnings("unchecked")
	@FunctionName("updateLiveRoom")
	public Map<String,Object> updateLiveRoom (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		Jedis jedis = jedisUtils.getJedis();
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("room_id", reqMap.get("room_id").toString());
		String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
		String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");

		if(! liveRoomOwner.equals(userId)){
			throw new QNLiveException("100002");
		}

		String liveRoomUpdateTime = jedis.hget(liveRoomKey, "lecturer_id");





		reqMap.put("room_address","");//TODO
		//0.目前校验每个讲师仅能创建一个直播间
		//1.缓存中读取直播间信息
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("lecturer_id", userId);
		String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, map);

		Jedis jedis = jedisUtils.getJedis();
		if(jedis.exists(lectureLiveRoomKey)){
			throw new QNLiveException("100006");
		}

		//2.数据库修改
		//2.如果为新讲师用户，插入讲师表。插入直播间表。更新登录信息表中的用户身份
		map.clear();
		map.put("access_token", reqEntity.getAccessToken());
		String accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
		String user_role = jedis.hget(accessTokenKey, "user_role");

		reqMap.put("user_role",user_role);
		reqMap.put("user_id", userId);
		Map<String,Object> createResultMap = lectureModuleServer.createLiveRoom(reqMap);

		//3.缓存修改
		//如果是刚刚成为讲师，则增加讲师信息缓存，并且修改access_token缓存中的身份
		if(reqMap.get("user_role") == null || reqMap.get("user_role").toString().split(",").length == 1){
			Map<String,Object> lectureObjectMap = lectureModuleServer.findLectureByLectureId(reqMap.get("user_id").toString());
			Map<String,String> lectureStringMap = new HashMap<String,String>();
			MiscUtils.converObjectMapToStringMap(lectureObjectMap, lectureStringMap);
			map.clear();
			map.put("lecturer_id", reqMap.get("user_id").toString());
			String lectureKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
			jedis.hmset(lectureKey, lectureStringMap);

			jedis.hset(accessTokenKey, "user_role", "normal_user,lecture");
		}

		//增加讲师直播间信息缓存
		String room_id = createResultMap.get("room_id").toString();
		Map<String,Object> liveRoomObjectMap = lectureModuleServer.findLiveRoomByRoomId(room_id);
		Map<String,String> liveRoomStringMap = new HashMap<String,String>();
		MiscUtils.converObjectMapToStringMap(liveRoomObjectMap, liveRoomStringMap);
		map.clear();
		map.put("room_id", createResultMap.get("room_id").toString());
		String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
		jedis.hmset(liveRoomKey, liveRoomStringMap);

		//增加讲师直播间对应关系缓存(一对多关系)
		jedis.hset(lectureLiveRoomKey, createResultMap.get("room_id").toString(), "1");

		return resultMap;
	}
}
