package qingning.mq.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.corba.se.impl.naming.namingutil.CorbalocURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.ImMessage;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.server.ImMsgService;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
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
		log.debug("-----聊天消息------"+JSON.toJSONString(imMessage));
		if(duplicateMessageFilter(imMessage, jedisUtils)){ //判断课程消息是否重复
			return;
		}

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
		if(courseMap.get("status").equals("2") && !information.get("send_type").equals("5")){
			return;
		}

		String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
		double createTime = Double.parseDouble(information.get("create_time").toString());
		String messageId = MiscUtils.getUUId();

		//1.将聊天信息id插入到redis zsort列表中
		jedis.zadd(messageListKey, createTime, messageId);

		//消息回复类型:0:讲师讲解 1：讲师回答 2 用户评论 3 用户提问
		//2.如果该条信息为提问，则存入消息提问列表
		if(information.get("send_type").equals("3")){
			String messageQuestionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_QUESTION, map);
			jedis.zadd(messageQuestionListKey, createTime, messageId);

			//3.如果该条信息为讲师发送的信息，则存入消息-讲师列表
		}else if(information.get("send_type").equals("0")){
			String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
			jedis.zadd(messageLecturerListKey, createTime, messageId);

			//如果为讲师回答，则需要进行极光推送
		}else if(information.get("send_type").equals("1")){
			JSONObject obj = new JSONObject();
			map.put(Constants.CACHED_KEY_LECTURER_FIELD, information.get("creator_id").toString());
			String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
			String lecturerName = jedis.hget(lecturerKey,"nick_name");
			obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_question_answer"), lecturerName, courseMap.get("course_title")));
			obj.put("to", information.get("student_id"));
			obj.put("msg_type","12");
			Map<String,String> extrasMap = new HashMap<>();
			extrasMap.put("msg_type","12");
			extrasMap.put("course_id",courseMap.get("course_id"));
			extrasMap.put("im_course_id",courseMap.get("im_course_id"));
			obj.put("extras_map", extrasMap);
			JPushHelper.push(obj);
		}

		//4.将聊天信息放入redis的map中
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
		log.debug("-----禁言信息------"+JSON.toJSONString(imMessage));
		if(duplicateMessageFilter(imMessage, jedisUtils)){ //判断课程消息是否重复
			return;
		}
		Map<String,Object> body = imMessage.getBody();
		Map<String,Object> information = (Map<String,Object>)body.get("information");
		String banStatus = information.get("ban_status").toString();

		//禁言状态 0未禁言（解除禁言） 1已禁言
		if(banStatus.equals("0")){
			Jedis jedis = jedisUtils.getJedis();
			Map<String, Object> map = new HashMap<>();
			map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
			String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
			jedis.zrem(bandKey, information.get("user_id").toString());

		}else {
			Jedis jedis = jedisUtils.getJedis();
			Map<String, Object> map = new HashMap<>();
			map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
			String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
			double timeDouble = (double) System.currentTimeMillis();
			jedis.zadd(bandKey, timeDouble, information.get("user_id").toString());
		}
	}


	/**
	 * 存储讲师讲课音频到缓存
	 * @param imMessage
	 * @param jedisUtils
	 * @param context
	 */
	private void processCourseAudio(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context) {
		log.debug("-----讲课音频信息------"+JSON.toJSONString(imMessage));
		if(duplicateMessageFilter(imMessage, jedisUtils)){ //判断课程消息是否重复
			return;
		}
		Map<String,Object> body = imMessage.getBody();
		Map<String,Object> information = (Map<String,Object>)body.get("information");

		Jedis jedis = jedisUtils.getJedis();
		Map<String, Object> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
		String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
		Map<String,String> courseMap = jedis.hgetAll(courseKey);
		//先判断是否有实际开播时间，没有则进行进一步判断
		//没有实际开播时间，判断是否为预告中，如果为预告中，且发送者为讲师，且当前时间大于开播时间的前十分钟，则该课程存入实际开播时间
		//并且进行直播超时定时任务检查
		if(courseMap.get("real_start_time") == null && information.get("creator_id") != null){
			if(courseMap.get("lecturer_id").equals(information.get("creator_id"))){
				long now = System.currentTimeMillis();
				long ready_start_time = Long.parseLong(courseMap.get("start_time")) - Long.parseLong(MiscUtils.getConfigByKey("course_ready_start_msec"));
				if(now > ready_start_time){
					//向缓存中增加课程真实开播时间
					jedis.hset(courseKey, "real_start_time", now+"");

					//进行直播超时定时任务检查
					MessagePushServerImpl messagePushServerImpl = (MessagePushServerImpl)context.getBean("MessagePushServer");
					RequestEntity requestEntity = new RequestEntity();
					requestEntity.setServerName("MessagePushServer");
					requestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
					requestEntity.setFunctionName("processCourseLiveOvertime");
					Map<String,Object> timerMap = new HashMap<>();
					timerMap.put("course_id", courseMap.get("course_id"));
					timerMap.put("real_start_time", now+"");
					timerMap.put("im_course_id", courseMap.get("im_course_id"));
					requestEntity.setParam(timerMap);
					messagePushServerImpl.processCourseLiveOvertime(requestEntity,jedisUtils,context);

					//进行超时预先提醒定时任务
					messagePushServerImpl.processLiveCourseOvertimeNotice(requestEntity, jedisUtils, context);

					//发送课程开始消息
					SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
					String str = sdf.format(now);
					String courseStartMessage = "直播开始于"+str;
					String mGroupId = courseMap.get("im_course_id");
					String message = courseStartMessage;
					String sender = "system";
					Map<String,Object> startInformation = new HashMap<>();
					startInformation.put("course_id", information.get("course_id").toString());
					startInformation.put("message", message);
					startInformation.put("message_type", "1");
					startInformation.put("send_type", "5");//5.开始/结束消息
					startInformation.put("create_time", now);//5.开始/结束消息
					Map<String,Object> messageMap = new HashMap<>();
					messageMap.put("msg_type","1");
					messageMap.put("send_time",now);
					messageMap.put("create_time",now);
					messageMap.put("information",startInformation);
					String content = JSON.toJSONString(messageMap);
					IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);//TODO
				}
			}
		}


		String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
		double createTime = Double.parseDouble(information.get("create_time").toString());
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


	//根据course_id和消息id对重复消息进行过滤
	private boolean duplicateMessageFilter(ImMessage imMessage, JedisUtils jedisUtils){
		Jedis jedis = jedisUtils.getJedis();
		Map<String,Object> body = imMessage.getBody();
		Map<String,Object> information = (Map<String,Object>)body.get("information");
		String courseId = information.get("course_id").toString();
		String mid = body.get("mid").toString();
		Map<String, Object> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
		String courseMessageIdInfoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_ID_INFO, map);
		boolean hasMessage = jedis.hexists(courseMessageIdInfoKey, mid);
		if(hasMessage == false){
			jedis.hset(courseMessageIdInfoKey, mid, "1");
		}
		return hasMessage;
	}
}
