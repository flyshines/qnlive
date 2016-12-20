package qingning.mq.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.context.ApplicationContext;

import qingning.common.entity.ImMessage;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.server.ImMsgService;
import redis.clients.jedis.Jedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ImMsgServiceImp implements ImMsgService {

	private static Logger log = LoggerFactory.getLogger(ImMsgServiceImp.class);


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
			case "3"://讲师讲课音频
				processCourseAudio(imMessage, jedisUtils, context);
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
		//课程id为空，则该条消息为无效消息
		if(information.get("course_id") == null){
			log.info("msgType"+body.get("msg_type").toString() + "消息course_id为空" + JSON.toJSONString(imMessage));
			return;
		}

		//判断课程状态
		//如果课程为已经结束，则不能发送消息，将该条消息抛弃
		map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
		String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
		Map<String,String> courseMap = jedis.hgetAll(courseKey);
		//课程为已结束
		if(courseMap.get("status").equals("2")){
			return;
		}

		//课程为预告中
		//如果课程状态为预告中，则继续判断当前时间是否大于课程开播时间，如果大于，则将课程变更为直播中状态
		if(courseMap.get("status").equals("1")){
			String courseStartTimeString = courseMap.get("start_time");
			long courseStartTime = Long.parseLong(courseStartTimeString);
			long currentTime = System.currentTimeMillis();
			if(currentTime > courseStartTime){
				//课程变更为直播中状态操作
				//1.更新课程缓存中的状态为直播中,更新课程实际开始时间
				jedis.hset(courseKey, "status", "4");
				jedis.hset(courseKey, "real_start_time", currentTime+"");

				//2.将该课程移动到讲师的预告列表第一条
				map.put(Constants.CACHED_KEY_LECTURER_FIELD, courseMap.get("lecturer_id"));
				String lecturerPredictionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);
				String oppositeCurrentTime = "-"+currentTime;
				jedis.zadd(lecturerPredictionListKey, Double.parseDouble(oppositeCurrentTime), courseMap.get("course_id"));//讲师预告课程列表为升序排序，将分数值设置开播时间绝对值取负

				//3.操作平台的课程列表，将其从预告列表中删除，按照实际开播时间放入直播中列表
				jedis.zrem(Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION, courseMap.get("course_id"));
				jedis.zadd(Constants.CACHED_KEY_PLATFORM_COURSE_LIVE, (double) currentTime, courseMap.get("course_id"));
			}
		}

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


	/**
	 * 存储讲师讲课音频到缓存
	 * @param imMessage
	 * @param jedisUtils
	 * @param context
	 */
	private void processCourseAudio(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context) {
		Map<String,Object> body = imMessage.getBody();
		Map<String,Object> information = (Map<String,Object>)body.get("information");

		Jedis jedis = jedisUtils.getJedis();
		Map<String, Object> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
		String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
		double createTime = Double.parseDouble(body.get("send_time").toString());
		String audioId = MiscUtils.getUUId();

		//1.将讲课音频信息id插入到redis zsort列表中
		jedis.zadd(audioListKey, createTime, audioId);

		//2.将讲课音频信息放入redis的map中
		map.put(Constants.FIELD_AUDIO_ID, audioId);
		String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
		Map<String,String> stringMap = new HashMap<>();
		MiscUtils.converObjectMapToStringMap(information, stringMap);
		stringMap.put("audio_id", audioId);
		jedis.hmset(messageKey, stringMap);
	}
}
