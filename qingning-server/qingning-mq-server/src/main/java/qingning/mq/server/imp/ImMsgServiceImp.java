package qingning.mq.server.imp;

import org.springframework.context.ApplicationContext;

import qingning.common.entity.ImMessage;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.server.ImMsgService;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

public class ImMsgServiceImp implements ImMsgService {

	@Override
	public void process(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context) {
		Map<String,Object> body = imMessage.getBody();
		String msgType = body.get("msg_type").toString();

		switch (msgType){
			case "1"://存储聊天消息
				processSaveCourseMessages(imMessage, jedisUtils, context);
				break;
			case "2"://禁言
				processCourseBanUser(imMessage, jedisUtils, context);
				break;
		}
	}

	/**
	 * 存储课程聊天消息到缓存中
	 * @param imMessage
	 * @param jedisUtils
	 * @param context
	 */
	@SuppressWarnings("unchecked")
	private void processSaveCourseMessages(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context) {
		Map<String,Object> body = imMessage.getBody();
		Map<String,Object> information = (Map<String,Object>)body.get("information");

		Jedis jedis = jedisUtils.getJedis();
		Map<String, Object> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
		String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
		double createTime = Double.parseDouble(body.get("send_time").toString());
		String messageId = MiscUtils.getUUId();

		//1.将聊天信息id插入到redis zsort列表中
		jedis.zadd(messageListKey, createTime, messageId);

		//2.将聊天信息放入redis的map中
		map.put(Constants.FIELD_MESSAGE_ID, messageId);
		String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
		Map<String,String> stringMap = new HashMap<>();
		MiscUtils.converObjectMapToStringMap(information, stringMap);
		stringMap.put("message_id", messageId);
		jedis.hmset(messageKey, stringMap);
	}

	/**
	 * 处理课程禁言
	 * @param imMessage
	 * @param jedisUtils
	 * @param context
	 */
	@SuppressWarnings("unchecked")
	private void processCourseBanUser(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context) {
		Map<String,Object> body = imMessage.getBody();
		Map<String,Object> information = (Map<String,Object>)body.get("information");
		String banStatus = information.get("ban_status").toString();

		//禁言状态 0未禁言（解除禁言） 1已禁言
		if(banStatus.equals("0")){
			Jedis jedis = jedisUtils.getJedis();
			Map<String, Object> map = new HashMap<>();
			map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
			String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
			jedis.zrem(bandKey, information.get("creator_id").toString());

		}else {
			Jedis jedis = jedisUtils.getJedis();
			Map<String, Object> map = new HashMap<>();
			map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
			String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
			double timeDouble = (double) System.currentTimeMillis();
			jedis.zadd(bandKey, timeDouble, body.get("creator_id").toString());
		}
	}


}
