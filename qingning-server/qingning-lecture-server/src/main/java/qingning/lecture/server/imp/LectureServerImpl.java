package qingning.lecture.server.imp;

import org.springframework.util.CollectionUtils;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ILectureModuleServer;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
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

		resultMap.put("room_id", room_id);
		return resultMap;
	}


	@SuppressWarnings("unchecked")
	@FunctionName("updateLiveRoom")
	public Map<String,Object> updateLiveRoom (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();

		//0.检测是否包含修改信息
		if(reqMap.get("avatar_address") == null && reqMap.get("room_name") == null
				&& reqMap.get("room_remark") == null){
			throw new QNLiveException("100007");
		}

		//1.检测该直播间是否属于修改人
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		Jedis jedis = jedisUtils.getJedis();
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("room_id", reqMap.get("room_id").toString());
		String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
		String liveRoomOwner = jedis.hget(liveRoomKey, "lecturer_id");
		if(liveRoomOwner == null || !liveRoomOwner.equals(userId)){
			throw new QNLiveException("100002");
		}

		//2.检测更新时间是否与系统一致
		String liveRoomUpdateTime = jedis.hget(liveRoomKey, "update_time");
		if(! reqMap.get("update_time").toString().equals(liveRoomUpdateTime)){
			throw new QNLiveException("100003");
		}

		//3.修改数据库
		reqMap.put("user_id",userId);
		Map<String,Object> dbResultMap = lectureModuleServer.updateLiveRoom(reqMap);
		if(dbResultMap == null || dbResultMap.get("updateCount") == null ||
				((Integer)dbResultMap.get("updateCount")) == 0 ){
			throw new QNLiveException("100003");
		}

		//4.修改缓存
		Map<String,String> updateCacheMap = new HashMap<String,String>();
		if(reqMap.get("avatar_address") != null ){
			updateCacheMap.put("avatar_address", reqMap.get("avatar_address").toString());
		}
		if(reqMap.get("room_name") != null ){
			updateCacheMap.put("room_name", reqMap.get("room_name").toString());
		}
		if(reqMap.get("room_remark") != null ){
			updateCacheMap.put("room_remark", reqMap.get("room_remark").toString());
		}
		updateCacheMap.put("update_time", ((Date) dbResultMap.get("update_time")).getTime() + "");
		jedis.hmset(liveRoomKey, updateCacheMap);

		return resultMap;
	}

	@SuppressWarnings("unchecked")
	@FunctionName("liveRoom")
	public Map<String,Object> queryLiveRoomDetail (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();

		//0.查询缓存中是否存在key，存在则将缓存中的信息返回
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("room_id", reqMap.get("room_id").toString());
		String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, map);
		Jedis jedis = jedisUtils.getJedis();
		Map<String,String> liveRoomMap = null;

		//1.如果缓存中不存在则查询数据库
		if(! jedis.exists(liveRoomKey)){
			Map<String,Object> dbResultMap = lectureModuleServer.findLiveRoomByRoomId( reqMap.get("room_id").toString());

			//2.如果缓存和数据库中均不存在，则返回直播间不存在
			if(CollectionUtils.isEmpty(dbResultMap)){
				throw new QNLiveException("100002");
			}else {
				//1.1如果数据库中查询出数据，将查询出的结果放入缓存
				Map<String,String> liveRoomStringMap = new HashMap<String,String>();
				MiscUtils.converObjectMapToStringMap(dbResultMap,liveRoomStringMap);
				jedis.hmset(liveRoomKey, liveRoomStringMap);

				liveRoomMap = liveRoomStringMap;
			}

		}else {
			//缓存中有数据
			liveRoomMap = jedis.hgetAll(liveRoomKey);
		}


		String queryType = reqMap.get("query_type").toString();

		switch (queryType){
			case "0":
				resultMap.put("avatar_address",liveRoomMap.get("avatar_address"));
				resultMap.put("room_name",liveRoomMap.get("room_name"));
				resultMap.put("last_course_amount",new BigDecimal(liveRoomMap.get("last_course_amount").toString()));
				resultMap.put("fans_num",Long.valueOf(liveRoomMap.get("fans_num").toString()));
				break;
			case "1":
				resultMap.put("avatar_address",liveRoomMap.get("avatar_address"));
				resultMap.put("room_name",liveRoomMap.get("room_name"));
				resultMap.put("room_remark",liveRoomMap.get("room_remark"));
				resultMap.put("rq_code",liveRoomMap.get("rq_code"));
				resultMap.put("room_address",liveRoomMap.get("room_address"));
				break;
			case "2":
				resultMap.put("avatar_address",liveRoomMap.get("avatar_address"));
				resultMap.put("room_name",liveRoomMap.get("room_name"));
				resultMap.put("room_remark",liveRoomMap.get("room_remark"));
				resultMap.put("fans_num",Long.valueOf(liveRoomMap.get("fans_num").toString()));
				break;
		}

		return resultMap;
	}
}
