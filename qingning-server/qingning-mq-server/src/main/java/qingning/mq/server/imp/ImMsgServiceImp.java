package qingning.mq.server.imp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import qingning.common.entity.ImMessage;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.TemplateData;
import qingning.common.util.*;
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
		String appName = body.get("app_name").toString();
		if(appName == null || appName.equals("")){
			appName = Constants.HEADER_APP_NAME;
		}
		switch (msgType){
			case "1"://存储聊天消息
				processSaveCourseMessages(imMessage, jedisUtils, context,appName);
				break;
			case "2"://禁言
				processCourseBanUser(imMessage, jedisUtils, context,appName);
				break;
//			case "3"://讲师讲课音频
//				processCourseAudio(imMessage, jedisUtils, context);
//				break;
		}
	}



	/**
	 * 存储课程聊天消息到缓存中
	 * @param imMessage
	 * @param jedisUtils
	 * @param context
	 */
	@SuppressWarnings("unchecked")
	private void processSaveCourseMessages(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context,String appName) {
		log.debug("-----聊天消息------"+JSON.toJSONString(imMessage));
		if(duplicateMessageFilter(imMessage, jedisUtils,appName)){ //判断课程消息是否重复
			return;
		}
		Map<String,Object> body = imMessage.getBody();
		final Map<String,Object> information = (Map<String,Object>)body.get("information");//获取信息

		Jedis jedis = jedisUtils.getJedis(appName);//缓存
		Map<String, Object> map = new HashMap<>();
		String imid = body.get("mid").toString();

		//<editor-fold desc="课程id为空，则该条消息为无效消息">
		if(information.get("course_id") == null){
			log.info("msgType"+body.get("msg_type").toString() + "消息course_id为空" + JSON.toJSONString(imMessage));
			return;
		}
		if(MiscUtils.isEmpty(information.get("creator_id"))){
			if(information.get("send_type").equals("5")){
				return;
			}
			information.put("creator_id","SYS");
		}
		//</editor-fold>


		map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());//在map中增加课程id course_id : xxxx
		String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
		Map<String,String> courseMap = jedis.hgetAll(courseKey);

		//<editor-fold desc="先判断是否有实际开播时间，没有则进行进一步判断  2.没有实际开播时间，判断是否为预告中，
		// 如果为预告中，且发送者为讲师，且当前时间大于开播时间的前十分钟，如果开课前15分钟讲课，
		// 则该课程存入实际开播时间，并且进行直播超时定时任务检查">
		if(courseMap.get("real_start_time") == null && information.get("creator_id") != null){
			if(courseMap.get("lecturer_id").equals(information.get("creator_id"))){
				long now = System.currentTimeMillis();
				long ready_start_time = Long.parseLong(courseMap.get("start_time")) - Long.parseLong(MiscUtils.getConfigKey("course_ready_start_msec"));
				long dealine =  Long.parseLong(courseMap.get("start_time")) + 15*60*1000;
				if(now > ready_start_time &&  now <= dealine){
					//向缓存中增加课程真实开播时间
					jedis.hset(courseKey, "real_start_time", now+"");

					//进行直播超时定时任务检查
					MessagePushServerImpl messagePushServerImpl = (MessagePushServerImpl)context.getBean("MessagePushServer");
					RequestEntity requestEntity = new RequestEntity();
					requestEntity.setServerName("MessagePushServer");
					requestEntity.setAppName(appName);
					requestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
					requestEntity.setFunctionName("processCourseLiveOvertime");
					Map<String,Object> timerMap = new HashMap<>();
					timerMap.put("course_id", courseMap.get("course_id"));
					timerMap.put("real_start_time",  now+"");
					timerMap.put("im_course_id", courseMap.get("im_course_id"));
					requestEntity.setParam(timerMap);
					log.debug("取消15分钟未开始定时任务 course_id:"+courseMap.get("course_id"));
					//取消15分钟未开始定时任务
					messagePushServerImpl.processCourseNotStartCancel(requestEntity, jedisUtils, context);
					log.debug("课程未开播处理定时任务取消 course_id:"+courseMap.get("course_id"));
					//课程未开播处理定时任务取消
					messagePushServerImpl.processCourseNotStartCancel(requestEntity, jedisUtils, context);
					log.debug("课程直播超时处理 服务端逻辑 定时任务 course_id:"+courseMap.get("course_id"));
					//课程直播超时处理 服务端逻辑 定时任务
					messagePushServerImpl.processCourseLiveOvertime(requestEntity,jedisUtils,context);
					log.debug("进行超时预先提醒定时任务 提前30分钟 提醒课程结束 course_id:"+courseMap.get("course_id"));
					//进行超时预先提醒定时任务 提前30分钟 提醒课程结束
					timerMap.put(Constants.OVERTIME_NOTICE_TYPE_30, Constants.OVERTIME_NOTICE_TYPE_30);
					requestEntity.setParam(timerMap);
					messagePushServerImpl.processLiveCourseOvertimeNotice(requestEntity, jedisUtils, context);

					log.debug("提前10分钟 提醒课程结束 course_id:"+courseMap.get("course_id"));
					//提前10分钟 提醒课程结束
					timerMap.remove(Constants.OVERTIME_NOTICE_TYPE_30);
					requestEntity.setParam(timerMap);
					messagePushServerImpl.processLiveCourseOvertimeNotice(requestEntity, jedisUtils, context);


					//发送课程开始消息
					SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
					String str = sdf.format( System.currentTimeMillis());
//					String courseStartMessage = "直播开始于"+str;
//					String mGroupId = courseMap.get("im_course_id");
//					String message = courseStartMessage;
//					String sender = "system";
//					Map<String,Object> startInformation = new HashMap<>();
//					startInformation.put("course_id", information.get("course_id").toString());
//					startInformation.put("message", message);
//					startInformation.put("message_type", "1");
//					startInformation.put("send_type", "5");//5.开始/结束消息
//					startInformation.put("message_imid",MiscUtils.getUUId());
//					startInformation.put("message_id",startInformation.get("message_imid"));
//					startInformation.put("create_time",  System.currentTimeMillis());//5.开始/结束消息
//					Map<String,Object> messageMap = new HashMap<>();
//					messageMap.put("msg_type","1");
//					messageMap.put("send_time", System.currentTimeMillis());
//					messageMap.put("create_time", System.currentTimeMillis());
//					messageMap.put("information",startInformation);
//					messageMap.put("mid",startInformation.get("message_imid"));
//					String content = JSON.toJSONString(messageMap);
//					IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);//发送信息
//
//
//					String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, startInformation);
////					//1.将聊天信息id插入到redis zsort列表中
//					jedis.zadd(messageListKey,  System.currentTimeMillis(), (String)startInformation.get("message_imid"));
////					//添加到老师发送的集合中
//					String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
//					jedis.zadd(messageLecturerListKey,  System.currentTimeMillis(),startInformation.get("message_imid").toString());
//
//					String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, startInformation);//直播间开始于
//					Map<String,String> result = new HashMap<String,String>();
//					MiscUtils.converObjectMapToStringMap(startInformation, result);
//					jedis.hmset(messageKey, result);

					Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
					TemplateData first = new TemplateData();
					first.setColor(Constants.WE_CHAT_PUSH_COLOR);
					first.setValue(MiscUtils.getConfigKey("wpush_start_lesson_first"));
					templateMap.put("first", first);

					TemplateData orderNo = new TemplateData();
					orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
					orderNo.setValue(MiscUtils.RecoveryEmoji(courseMap.get("course_title")));
					templateMap.put("keyword1", orderNo);

					TemplateData wuliu = new TemplateData();
					wuliu.setColor(Constants.WE_CHAT_PUSH_COLOR);
					wuliu.setValue(str);
					templateMap.put("keyword2", wuliu);


					TemplateData remark = new TemplateData();
					remark.setColor(Constants.WE_CHAT_PUSH_COLOR);
					remark.setValue(MiscUtils.getConfigKey("wpush_start_lesson_remark"));
					templateMap.put("remark", remark);
					//查询报名了的用户id
					List<String> findFollowUserIds =  coursesStudentsMapper.findUserIdsByCourseId(courseMap.get("course_id"));

					String url = MiscUtils.getConfigByKey("course_live_room_url",appName);
					url=String.format(url,  courseMap.get("course_id"),courseMap.get("room_id"));
					if (findFollowUserIds!=null && findFollowUserIds.size()>0) {
						weiPush(findFollowUserIds, MiscUtils.getConfigByKey("wpush_start_lesson",appName),url,templateMap, jedis,appName);
					}

				}
			}
		}
		//</editor-fold>

		//判断课程状态
		//如果课程为已经结束，则不能发送消息，将该条消息抛弃
		courseMap = null;
		try{
			courseMap = CacheUtils.readCourse((String)information.get("course_id"), null, new CommonReadOperation(){
				@Override
				public Object invokeProcess(RequestEntity requestEntity) throws Exception {
					return coursesMapper.findCourseByCourseId((String)information.get("course_id"));
				}
			}, jedis, false);//jedis.hgetAll(courseKey);
		} catch(Exception e){
			log.error("read course["+(String)information.get("course_id")+"]:"+e.getMessage());
		}
		if(MiscUtils.isEmpty(courseMap)){
			log.info("Course["+(String)information.get("course_id")+"] can't be readed.");
			return;
		}

		String messageId = MiscUtils.getUUId();//设置messageid
		Map<String,String> stringMap = new HashMap<>();
		MiscUtils.converObjectMapToStringMap(information, stringMap);
		String message = stringMap.get("message");
		if(!MiscUtils.isEmpty(message)){
			stringMap.put("message", MiscUtils.emojiConvertToNormalString(message));
		}
		//增加回复
		String message_question = stringMap.get("message_question");
		if(!MiscUtils.isEmpty(message_question)){
			stringMap.put("message_question", MiscUtils.emojiConvertToNormalString(message_question));
		}
		stringMap.put("message_id", messageId);
		stringMap.put("message_imid",imid);
		//<editor-fold desc="课程为已结束">
		if(courseMap.get("status").equals("2") && !information.get("send_type").equals("6")){ //如果课程状态是2结束 消息类型不是6 结束信息
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
				if(!MiscUtils.isEmpty(stringMap.get("audio_image"))){
					messageObjectMap.put("audio_image", stringMap.get("audio_image"));
				}
				if(!MiscUtils.isEmpty(stringMap.get("status"))){
					messageObjectMap.put("status",stringMap.get("status"));
				}else{
					messageObjectMap.put("status",0);
				}

				messageObjectMap.put("message_imid", stringMap.get("message_imid"));

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
		//</editor-fold>

		String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
		double createTime = Double.parseDouble(information.get("create_time").toString());

		//1.将聊天信息id插入到redis zsort列表中
		jedis.zadd(messageListKey, createTime, imid);

		//消息回复类型:0:讲师讲解 1：讲师回答 2 用户互动 3 用户提问
		//4.打赏信息 5.课程开始 6结束消息 7讲师互动
		//2.如果该条信息为提问，则存入消息提问列表
		if(information.get("send_type").equals("3") || information.get("send_type").equals("2")){//用户消息
			String messageQuestionListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_USER, map);
			jedis.zadd(messageQuestionListKey, createTime, imid);
			//3.如果该条信息为讲师发送的信息，则存入消息-讲师列表
		}else if(information.get("send_type").equals("0") ||	//老师讲解
				information.get("send_type").equals("1")  ||	//老师回答
				information.get("send_type").equals("4")  ||	//用户互动
				information.get("send_type").equals("5")  ||	//课程开始
				information.get("send_type").equals("6")  ||	//结束消息
				information.get("send_type").equals("7")){		//老师回复
			String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
			jedis.zadd(messageLecturerListKey, createTime, imid);
			if(information.get("send_type").equals("0") && information.get("message_type").equals("0")){//老师的语音消息
				String messageLecturerVoiceListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER_VOICE, map);
				jedis.zadd(messageLecturerVoiceListKey, createTime, imid);
			}
			if(information.get("send_type").equals("1") || information.get("send_type").equals("7")){//讲师回答 和 讲师回复
  				Map<String, Object> map1 = JSON.parseObject(information.get("message_question").toString(), HashMap.class);
				map1.put("course_id",information.get("course_id"));
				String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map1);
				jedis.hset(key,"message_status","1");

				//<editor-fold desc="暂时还没测试">
				//给学生发送推送消息
//				Map<String,Object> courseInfo = coursesMapper.findCourseByCourseId(information.get("course_id").toString());
//				Map<String,Object> userInfo = null;
//				noticeToStudent(courseInfo, userInfo, jedisUtils);
				//</editor-fold>
			}


		}
		//4.将聊天信息放入redis的map中
		map.put(Constants.FIELD_MESSAGE_ID, imid);
		String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
		jedis.hmset(messageKey, stringMap);
		if("6".equals(information.get("send_type"))){//结束消息
			//1.7如果存在课程聊天信息
            RequestEntity messageRequestEntity = new RequestEntity();
            Map<String,Object> processMap = new HashMap<>();
            processMap.put("course_id", (String)information.get("course_id"));
            messageRequestEntity.setParam(processMap);
			messageRequestEntity.setAppName(appName);
            try {
            	SaveCourseMessageService saveCourseMessageService = this.getSaveCourseMessageService(context);

            	if(saveCourseMessageService != null){
            		saveCourseMessageService.process(messageRequestEntity, jedisUtils, null);
            	}
            } catch (Exception e) {
            	log.error("SaveCourseMessageService["+information.get("course_id")+"] error:"+e.getMessage());
            }

            //1.8如果存在课程音频信息
//            RequestEntity audioRequestEntity = new RequestEntity();
//            audioRequestEntity.setParam(processMap);
//			messageRequestEntity.setAppName(appName);
//            try {
//            	SaveCourseAudioService saveCourseAudioService=this.getSaveCourseAudioService(context);
//            	if(saveCourseAudioService!=null){
//            		saveCourseAudioService.process(audioRequestEntity, jedisUtils, null);
//            	}
//            } catch (Exception e) {
//            	log.error("save SaveCourseAudioService["+information.get("course_id")+"] error:"+e.getMessage());
//            }
		}
	}



//<editor-fold desc="暂时还没测试">
	/**
	 * 讲师回复学生或者和学生互动之后 推送消息给学生
	 * @param courseInfo
	 * @param userInfo
	 * @param jedisUtils
	 */

//	private void noticeToStudent(Map<String,Object> courseInfo, Map<String,Object> userInfo, JedisUtils jedisUtils) {
//		Map<String, TemplateData> templateMap = (Map<String, TemplateData>) reqMap.get("templateParam");//模板消息
//		String url = MiscUtils.getConfigByKey("course_share_url_pre_fix")+ courseInfo.get("courseId");//推送url
//		String templateId = MiscUtils.getConfigByKey("wpush_update_course");//更新课程的模板id;
//		String openId = userInfo.get("web_openid").toString();
//		if(!MiscUtils.isEmpty(openId)){//推送微信模板消息给微信用户
//			WeiXinUtil.send_template_message(openId, templateId, url, templateMap, jedis);//推送消息
//		}
//
//		Jedis jedis = jedisUtils.getJedis();
//		JSONObject obj = new JSONObject();
//		Map<String,String> extrasMap = new HashMap<>();
//		obj.put("body",String.format(MiscUtils.getConfigByKey("jpush_course_question_answer"), "讲师xxx",  MiscUtils.RecoveryEmoji(courseInfo.get("course_title").toString())));
//		obj.put("msg_type","12");//发布新课程
//		extrasMap.put("msg_type","12");
//		extrasMap.put("course_id",courseInfo.get("courseId").toString());
//		extrasMap.put("im_course_id",courseInfo.get("im_course_id").toString());
//		obj.put("extras_map", extrasMap);
//
//		String mUserId = userInfo.get("m_user_id").toString();
//		if (!MiscUtils.isEmpty(mUserId)) {//极光推送给app用户
//			obj.put("to", mUserId);
//			JPushHelper.push(obj);
//		}
//	}
	//</editor-fold>

	/**
	 * 处理课程禁言
	 * @param imMessage
	 * @param jedisUtils
	 * @param context
	 */

	private void processCourseBanUser(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context,String appName) {
		log.debug("-----禁言信息------"+JSON.toJSONString(imMessage));
		if(duplicateMessageFilter(imMessage, jedisUtils,appName)){ //判断课程消息是否重复
			return;
		}
		Jedis jedis = jedisUtils.getJedis(appName);
		Map<String,Object> body = imMessage.getBody();
		Map<String,Object> information = (Map<String,Object>)body.get("information");
		String banStatus = information.get("ban_status").toString();

		//禁言状态 0未禁言（解除禁言） 1已禁言
		if(banStatus.equals("0")){

			Map<String, Object> map = new HashMap<>();
			map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
			String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
			jedis.zrem(bandKey, information.get("user_id").toString());

		}else {
			Map<String, Object> map = new HashMap<>();
			map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
			String bandKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_BAN_USER_LIST, map);
			double timeDouble = (double) System.currentTimeMillis();
			jedis.zadd(bandKey, timeDouble, information.get("user_id").toString());
		}
	}


	//<editor-fold desc="Description">
	/**
	 * 存储讲师讲课音频到缓存
	 * @param imMessage
	 * @param jedisUtils
	 * @param context
	 */
	//<editor-fold desc="存储讲师讲课音频到缓存">
	//	private void processCourseAudio(ImMessage imMessage, JedisUtils jedisUtils, ApplicationContext context) {
//		log.debug("-----讲课音频信息------"+JSON.toJSONString(imMessage));
//		if(duplicateMessageFilter(imMessage, jedisUtils)){ //判断课程消息是否重复
//			return;
//		}
//		Map<String,Object> body = imMessage.getBody();
//		Map<String,Object> information = (Map<String,Object>)body.get("information");
//
//		Jedis jedis = jedisUtils.getJedis();
//		Map<String, Object> map = new HashMap<>();
//		map.put(Constants.CACHED_KEY_COURSE_FIELD, information.get("course_id").toString());
//		String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
//		Map<String,String> courseMap = jedis.hgetAll(courseKey);
//
//		//<editor-fold desc="先判断是否有实际开播时间，没有则进行进一步判断  2.没有实际开播时间，判断是否为预告中，如果为预告中，且发送者为讲师，且当前时间大于开播时间的前十分钟，如果开课前15分钟讲课，则该课程存入实际开播时间，并且进行直播超时定时任务检查">
//		if(courseMap.get("real_start_time") == null && information.get("creator_id") != null){
//			if(courseMap.get("lecturer_id").equals(information.get("creator_id"))){
//				long now = System.currentTimeMillis();
//				long ready_start_time = Long.parseLong(courseMap.get("start_time")) - Long.parseLong(MiscUtils.getConfigByKey("course_ready_start_msec"));
//				long dealine =  Long.parseLong(courseMap.get("start_time")) + 15*60*1000;
//				if(now > ready_start_time &&  now <= dealine){
//					//向缓存中增加课程真实开播时间
//					jedis.hset(courseKey, "real_start_time", now+"");
//
//					//进行直播超时定时任务检查
//					MessagePushServerImpl messagePushServerImpl = (MessagePushServerImpl)context.getBean("MessagePushServer");
//					RequestEntity requestEntity = new RequestEntity();
//					requestEntity.setServerName("MessagePushServer");
//					requestEntity.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
//					requestEntity.setFunctionName("processCourseLiveOvertime");
//					Map<String,Object> timerMap = new HashMap<>();
//					timerMap.put("course_id", courseMap.get("course_id"));
//					timerMap.put("real_start_time", now+"");
//					timerMap.put("im_course_id", courseMap.get("im_course_id"));
//					requestEntity.setParam(timerMap);
//					messagePushServerImpl.processCourseNotStartCancel(requestEntity, jedisUtils, context);
//					messagePushServerImpl.processCourseLiveOvertime(requestEntity,jedisUtils,context);
//
//					//进行超时预先提醒定时任务
//					timerMap.put(Constants.OVERTIME_NOTICE_TYPE_30, Constants.OVERTIME_NOTICE_TYPE_30);
//					messagePushServerImpl.processLiveCourseOvertimeNotice(requestEntity, jedisUtils, context);
//					timerMap.remove(Constants.OVERTIME_NOTICE_TYPE_30);
//					messagePushServerImpl.processLiveCourseOvertimeNotice(requestEntity, jedisUtils, context);
//
//					//取消15分钟未开始定时任务
//					messagePushServerImpl.processCourseNotStartCancel(requestEntity, jedisUtils, context);
//
//					//发送课程开始消息
//					SimpleDateFormat sdf =   new SimpleDateFormat("yyyy年MM月dd日HH:mm");
//					String str = sdf.format(now);
//					String courseStartMessage = "直播开始于"+str;
//					String mGroupId = courseMap.get("im_course_id");
//					String message = courseStartMessage;
//					String sender = "system";
//					Map<String,Object> startInformation = new HashMap<>();
//					startInformation.put("course_id", information.get("course_id").toString());
//					startInformation.put("message", message);
//					startInformation.put("message_type", "1");
//					startInformation.put("send_type", "5");//5.开始/结束消息
//					startInformation.put("create_time", now);//5.开始/结束消息
//					Map<String,Object> messageMap = new HashMap<>();
//					messageMap.put("msg_type","1");
//					messageMap.put("send_time",now);
//					messageMap.put("create_time",now);
//					messageMap.put("information",startInformation);
//					messageMap.put("mid",MiscUtils.getUUId());
//					String content = JSON.toJSONString(messageMap);
//					IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);
//
//					startInformation.put("creator_id",courseMap.get("lecturer_id"));
//					startInformation.put("message_id",messageMap.get("mid"));
//					String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, startInformation);
//					//1.将聊天信息id插入到redis zsort列表中
//					jedis.zadd(messageListKey, now, (String)startInformation.get("message_id"));
//					String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, startInformation);//直播间开始于
//
//					Map<String,String> result = new HashMap<String,String>();
//					MiscUtils.converObjectMapToStringMap(startInformation, result);
//					jedis.hmset(messageKey, result);
//
//		    		Map<String, TemplateData> templateMap = new HashMap<String, TemplateData>();
//		    		TemplateData first = new TemplateData();
//		    		first.setColor(Constants.WE_CHAT_PUSH_COLOR);
//		    		first.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_first"));
//		    		templateMap.put("first", first);
//
//		    		TemplateData orderNo = new TemplateData();
//		    		orderNo.setColor(Constants.WE_CHAT_PUSH_COLOR);
//		    		orderNo.setValue(MiscUtils.RecoveryEmoji(courseMap.get("course_title")));
//		    		templateMap.put("keyword1", orderNo);
//
//		    		TemplateData wuliu = new TemplateData();
//		    		wuliu.setColor(Constants.WE_CHAT_PUSH_COLOR);
//		    		wuliu.setValue(str);
//		    		templateMap.put("keyword2", wuliu);
//
//
//		    		TemplateData remark = new TemplateData();
//		    		remark.setColor(Constants.WE_CHAT_PUSH_COLOR);
//		    		remark.setValue(MiscUtils.getConfigByKey("wpush_start_lesson_remark"));
//		    		templateMap.put("remark", remark);
//		    		//查询报名了的用户id
//		    		List<String> findFollowUserIds =  coursesStudentsMapper.findUserIdsByCourseId(courseMap.get("room_id"));
//
//		    		String url = MiscUtils.getConfigByKey("live_room_url_pre_fix");
//		    		url=String.format(url,  courseMap.get("course_id"),courseMap.get("room_id"));
//		    		if (findFollowUserIds!=null && findFollowUserIds.size()>0) {
//		    			weiPush(findFollowUserIds, MiscUtils.getConfigByKey("wpush_start_lesson"),url,templateMap, jedis);
//					}
//
//				}
//			}
//		}
//		//</editor-fold>
//		String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);
//		double createTime = Double.parseDouble(information.get("create_time").toString());
//		String audioId = MiscUtils.getUUId();
//
//		//1.将讲课音频信息id插入到redis zsort列表中
//		jedis.zadd(audioListKey, createTime, audioId);
//
//		//2.将讲课音频信息放入redis的map中
//		map.put(Constants.FIELD_AUDIO_ID, audioId);
//		String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
//		Map<String,String> stringMap = new HashMap<>();
//		MiscUtils.converObjectMapToStringMap(information, stringMap);
//		stringMap.put("audio_id", audioId);
//		jedis.hmset(messageKey, stringMap);
//	}
	//</editor-fold>
	//</editor-fold>


    /**
     * 微信推送
     * @param findFollowUserIds
     * @param templateId
     * @param templateMap
     */
    public void weiPush(List<String> findFollowUserIds,String templateId,String url,Map<String, TemplateData> templateMap,Jedis jedis,String appName){
    	// 推送   关注的直播间有创建新的课程
        	Map<String, Object> map = new HashMap<String, Object>();
        	map.put("list", findFollowUserIds);
			List<String> findOpenIds = loginInfoMapper.findLoginInfoByUserIds(map);
			if (findOpenIds!=null && findOpenIds.size()>0) {
				for (String openId : findOpenIds) {
					//TODO
					WeiXinUtil.send_template_message(openId, templateId,url, templateMap, jedis,appName);
				} 
			} 
    }

	//根据course_id和消息id对重复消息进行过滤
	private boolean duplicateMessageFilter(ImMessage imMessage, JedisUtils jedisUtils,String appName){
		Jedis jedis = jedisUtils.getJedis(appName);
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


	public static void main(String[] args){

	}

}
