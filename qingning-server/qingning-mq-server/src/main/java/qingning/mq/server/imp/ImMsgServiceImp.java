package qingning.mq.server.imp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import qingning.common.entity.ImMessage;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.util.CacheUtils;
import qingning.common.util.Constants;
import qingning.common.util.IMMsgUtil;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.common.util.WeiXinUtil;
import qingning.db.common.mybatis.persistence.CourseMessageMapper;
import qingning.db.common.mybatis.persistence.CoursesMapper;
import qingning.db.common.mybatis.persistence.CoursesStudentsMapper;
import qingning.db.common.mybatis.persistence.LoginInfoMapper;
import qingning.server.ImMsgService;
import qingning.server.rpc.CommonReadOperation;
import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;

public class ImMsgServiceImp implements ImMsgService {

	private static Logger log = LoggerFactory.getLogger(ImMsgServiceImp.class);

	@Autowired(required = true)
	private CoursesStudentsMapper coursesStudentsMapper;
	
	@Autowired(required = true)
	private CourseMessageMapper courseMessageMapper;
	
	@Autowired(required = true)
	private CoursesMapper coursesMapper;
	
	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;

	private static Hashtable<String,Object> messageLockMap = new Hashtable<>();
	private static Hashtable<String,Object> maxLockMap = new Hashtable<>();
	
    private SaveCourseMessageService saveCourseMessageService;
    private SaveCourseAudioService saveCourseAudioService;
	
    private SaveCourseMessageService getSaveCourseMessageService(ApplicationContext context){
    	if(saveCourseMessageService == null){
    		saveCourseMessageService =  (SaveCourseMessageService)context.getBean("SaveCourseMessageServer");
    	}
    	return saveCourseMessageService;
    }
    
    private SaveCourseAudioService getSaveCourseAudioService(ApplicationContext context){
    	if(saveCourseAudioService == null){
    		saveCourseAudioService =  (SaveCourseAudioService)context.getBean("SaveAudioMessageServer");
    	}
    	return saveCourseAudioService;
    }
	
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
		final Map<String,Object> information = (Map<String,Object>)body.get("information");

		Jedis jedis = jedisUtils.getJedis();
		Map<String, Object> map = new HashMap<>();
		//课程id为空，则该条消息为无效消息
		if(information.get("course_id") == null){
			log.info("msgType"+body.get("msg_type").toString() + "消息course_id为空" + JSON.toJSONString(imMessage));
			return;
		}
		if(MiscUtils.isEmpty(information.get("creator_id"))){
			information.set("creator_id","SYS");
		}
		//判断课程状态
		//如果课程为已经结束，则不能发送消息，将该条消息抛弃
		map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
		//String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
		Map<String,String> courseMap = null;
		try{
			courseMap = CacheUtils.readCourse((String)information.get("course_id"), null, new CommonReadOperation(){
				@Override
				public Object invokeProcess(RequestEntity requestEntity) throws Exception {				
					return coursesMapper.findCourseByCourseId((String)information.get("course_id"));
				}
			}, jedisUtils, false);//jedis.hgetAll(courseKey);
		} catch(Exception e){
			log.error("read course["+(String)information.get("course_id")+"]:"+e.getMessage());
		}
		if(MiscUtils.isEmpty(courseMap)){
			log.info("Course["+(String)information.get("course_id")+"] can't be readed.");
			return;
		}
		
		String messageId = MiscUtils.getUUId();
		Map<String,String> stringMap = new HashMap<>();
		MiscUtils.converObjectMapToStringMap(information, stringMap);
		String message = stringMap.get("message");
		if(!MiscUtils.isEmpty(message)){
			stringMap.put("message", MiscUtils.emojiConvertToNormalString(message));
		}
		String message_question = stringMap.get("message_question");
		if(!MiscUtils.isEmpty(message_question)){
			stringMap.put("message_question", MiscUtils.emojiConvertToNormalString(message_question));
		}
		stringMap.put("message_id", messageId);
		//课程为已结束
		if(courseMap.get("status").equals("2") && !information.get("send_type").equals("6")){
			if("4".equals(information.get("send_type"))){				
				Map<String,Object> messageObjectMap = new HashMap<>();				
				messageObjectMap.put("message_id", stringMap.get("message_id"));
				messageObjectMap.put("course_id", stringMap.get("course_id"));
				messageObjectMap.put("message_url", stringMap.get("message_url"));
				messageObjectMap.put("message", stringMap.get("message"));
				messageObjectMap.put("message_question", stringMap.get("message_question"));
				if(!MiscUtils.isEmpty(stringMap.get("audio_time"))){
					messageObjectMap.put("audio_time", Long.parseLong(stringMap.get("audio_time")));
				}else {
					messageObjectMap.put("audio_time", 0);
				}				
				messageObjectMap.put("message_type", stringMap.get("message_type"));
				messageObjectMap.put("send_type", stringMap.get("send_type"));
				messageObjectMap.put("creator_id", stringMap.get("creator_id"));
				if(!MiscUtils.isEmpty(stringMap.get("create_time"))){
					Date createTime = new Date(Long.parseLong(stringMap.get("create_time")));
					messageObjectMap.put("create_time", createTime);
				}
				Object lockObject;
				if(maxLockMap.containsKey(stringMap.get("course_id"))){
					lockObject = maxLockMap.get(stringMap.get("course_id"));
				}else {
					maxLockMap.put(stringMap.get("course_id"),stringMap.get("course_id"));
					lockObject = stringMap.get("course_id");
				}
				List<Map<String,Object>> list = new LinkedList<Map<String,Object>>();
				list.add(messageObjectMap);
				synchronized (lockObject){
					Map<String,Object> maxPosMessage = courseMessageMapper.findCourseMessageMaxPos(stringMap.get("course_id"));
					long messagePos = 0;
					if(!MiscUtils.isEmpty(maxPosMessage)){
						messagePos = MiscUtils.convertObjectToLong(maxPosMessage.get("message_pos"))+1;
					}
					messageObjectMap.put("message_pos", messagePos);					
					courseMessageMapper.insertCourseMessageList(list);
				}
			}
			return;
		}

		String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
		double createTime = Double.parseDouble(information.get("create_time").toString());
		
		//1.将聊天信息id插入到redis zsort列表中
		jedis.zadd(messageListKey, createTime, messageId);

		//消息回复类型:0:讲师讲解 1：讲师回答 2 用户评论 3 用户提问
		//2.如果该条信息为提问，则存入消息提问列表
		if(information.get("send_type").equals("3")){
			String messageQuestionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_QUESTION, map);
			jedis.zadd(messageQuestionListKey, createTime, messageId);

			//3.如果该条信息为讲师发送的信息，则存入消息-讲师列表
		}else if(information.get("send_type").equals("0") || information.get("send_type").equals("1")){
			String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
			jedis.zadd(messageLecturerListKey, createTime, messageId);

			//如果为讲师回答，则需要进行极光推送//TODO
		}
// else if(information.get("send_type").equals("1")){
//			JSONObject obj = new JSONObject();
//			map.put(Constants.CACHED_KEY_LECTURER_FIELD, information.get("creator_id").toString());
//			String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);
//			String lecturerName = jedis.hget(lecturerKey,"nick_name");
//			obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_question_answer"), lecturerName, courseMap.get("course_title")));
//			obj.put("to", information.get("student_id"));
//			obj.put("msg_type","12");
//			Map<String,String> extrasMap = new HashMap<>();
//			extrasMap.put("msg_type","12");
//			extrasMap.put("course_id",courseMap.get("course_id"));
//			extrasMap.put("im_course_id",courseMap.get("im_course_id"));
//			obj.put("extras_map", extrasMap);
//			JPushHelper.push(obj);
//		}

		//4.将聊天信息放入redis的map中
		map.put(Constants.FIELD_MESSAGE_ID, messageId);
		String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
		jedis.hmset(messageKey, stringMap);
		if("6".equals(information.get("send_type"))){
			//1.7如果存在课程聊天信息
            RequestEntity messageRequestEntity = new RequestEntity();
            Map<String,Object> processMap = new HashMap<>();
            processMap.put("course_id", (String)information.get("course_id"));
            messageRequestEntity.setParam(processMap);
            try {
            	SaveCourseMessageService saveCourseMessageService = this.getSaveCourseMessageService(context);
            	if(saveCourseMessageService != null){
            		saveCourseMessageService.process(messageRequestEntity, jedisUtils, null);
            	}
            } catch (Exception e) {
            	log.error("SaveCourseMessageService["+information.get("course_id")+"] error:"+e.getMessage());
            }

            //1.8如果存在课程音频信息
            RequestEntity audioRequestEntity = new RequestEntity();
            audioRequestEntity.setParam(processMap);
            try {            	
            	SaveCourseAudioService saveCourseAudioService=this.getSaveCourseAudioService(context);
            	if(saveCourseAudioService!=null){
            		saveCourseAudioService.process(audioRequestEntity, jedisUtils, null);
            	}
            } catch (Exception e) {
            	log.error("save SaveCourseAudioService["+information.get("course_id")+"] error:"+e.getMessage());
            }
		}
	}

	/**
	 * 处理课程禁言
	 * @param imMessage
	 * @param jedisUtils
	 * @param context
	 */

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
		//没有实际开播时间，判断是否为预告中，如果为预告中，且发送者为讲师，
		// 且当前时间大于开播时间的前十分钟，如果开课前15分钟讲课，则该课程存入实际开播时间，
		//并且进行直播超时定时任务检查
		if(courseMap.get("real_start_time") == null && information.get("creator_id") != null){
			if(courseMap.get("lecturer_id").equals(information.get("creator_id"))){
				long now = System.currentTimeMillis();
				long ready_start_time = Long.parseLong(courseMap.get("start_time")) - Long.parseLong(MiscUtils.getConfigByKey("course_ready_start_msec"));
				long dealine =  Long.parseLong(courseMap.get("start_time")) + 15*60*1000;
				if(now > ready_start_time &&  now <= dealine){
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
					messagePushServerImpl.processCourseNotStartCancel(requestEntity, jedisUtils, context);
					messagePushServerImpl.processCourseLiveOvertime(requestEntity,jedisUtils,context);

					//进行超时预先提醒定时任务
					messagePushServerImpl.processLiveCourseOvertimeNotice(requestEntity, jedisUtils, context);

					//取消15分钟未开始定时任务
					messagePushServerImpl.processCourseNotStartCancel(requestEntity, jedisUtils, context);

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
					messageMap.put("mid",MiscUtils.getUUId());
					String content = JSON.toJSONString(messageMap);
					IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);//TODO
					
				   	
		    		Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
		    		TemplateData first = new TemplateData();
		    		first.setColor("#000000");
		    		first.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_first"));
		    		templateMap.put("first", first);
		    		
		    		TemplateData orderNo = new TemplateData();
		    		orderNo.setColor("#000000");
		    		orderNo.setValue(MiscUtils.RecoveryEmoji(courseMap.get("course_title")));
		    		templateMap.put("keyword1", orderNo);
		    		
		    		TemplateData wuliu = new TemplateData();
		    		wuliu.setColor("#000000");
		    		wuliu.setValue(str);
		    		templateMap.put("keyword2", wuliu);	


		    		TemplateData remark = new TemplateData();
		    		remark.setColor("#000000");
		    		remark.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_remark"));
		    		templateMap.put("remark", remark);
		    		//查询报名了的用户id 
		    		List<String> findFollowUserIds =  coursesStudentsMapper.findUserIdsByCourseId(courseMap.get("room_id"));
		    		
		    		String url = MiscUtils.getConfigByKey("live_room_url_pre_fix");
		    		url=String.format(url,  courseMap.get("course_id"),courseMap.get("room_id"));
		    		if (findFollowUserIds!=null && findFollowUserIds.size()>0) {
		    			weiPush(findFollowUserIds, MiscUtils.getConfigByKey("wpush_start_lesson"),url,templateMap, jedis);
					}
			
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


    /**
     * 微信推送
     * @param findFollowUserIds
     * @param templateId
     * @param templateMap
     */
    public void weiPush(List<String> findFollowUserIds,String templateId,String url,Map<String, TemplateData> templateMap,Jedis jedis){
    	// 推送   关注的直播间有创建新的课程
        	Map<String, Object> map = new HashMap<String, Object>();
        	map.put("list", findFollowUserIds);
			List<String> findOpenIds = loginInfoMapper.findLoginInfoByUserIds(map);
			if (findOpenIds!=null && findOpenIds.size()>0) {
				for (String openId : findOpenIds) {
					//TODO
					WeiXinUtil.send_template_message(openId, templateId,url, templateMap, jedis);
				} 
			} 
    	
    }

	//根据course_id和消息id对重复消息进行过滤
	private boolean duplicateMessageFilter(ImMessage imMessage, JedisUtils jedisUtils){
		Jedis jedis = jedisUtils.getJedis();
		Map<String,Object> body = imMessage.getBody();
		Map<String,Object> information = (Map<String,Object>)body.get("information");
		String courseId = information.get("course_id").toString();
		Object lockObject;
		if(messageLockMap.containsKey(courseId)){
			lockObject = messageLockMap.get(courseId);
		}else {
			messageLockMap.put(courseId,courseId);
			lockObject = courseId;
		}

		synchronized (lockObject){
			String mid = body.get("mid").toString();
			Map<String, Object> map = new HashMap<>();
			map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
			String courseMessageIdInfoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_ID_INFO, map);

			String hasMessage = jedis.hget(courseMessageIdInfoKey, mid);
			if(MiscUtils.isEmpty(hasMessage)){
				jedis.hset(courseMessageIdInfoKey, mid, System.currentTimeMillis()+"");
			}
			if(MiscUtils.isEmpty(hasMessage) == true){
				return false;
			}else {
				return true;
			}
		}
	}

	public static void clearMessageLockMap(){
		messageLockMap.clear();
		maxLockMap.clear();
	}
}
