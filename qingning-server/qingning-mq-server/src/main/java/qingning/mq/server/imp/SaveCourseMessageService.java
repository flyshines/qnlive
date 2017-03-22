package qingning.mq.server.imp;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.CourseMessageMapper;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.*;

public class SaveCourseMessageService extends AbstractMsgService{
	private static Logger log = LoggerFactory.getLogger(SaveCourseMessageService.class);
	@Autowired(required = true)
	private CourseMessageMapper courseMessageMapper;

	@SuppressWarnings("unchecked")
	@Override
	public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
		log.info("=============================================================执行数据落地方法===========================================================");
		Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

		//批量读取缓存中的内容
		Map<String, Object> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
		String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);//COURSE:{course_id}:MESSAGE_LIST


		Jedis jedisObject = jedisUtils.getJedis();


		//1.从缓存中查询该课程的消息列表
		Set<String> messageIdList = jedisObject.zrange(messageListKey, 0, -1);//jedisObject.zrevrange(messageListKey, 0 , -1);
		log.info(messageListKey+"=============================================================消息列表==========================================================="+messageIdList.size());
		if(messageIdList == null || messageIdList.size() == 0){
			log.error("没有拿到值messageListKey:"+messageListKey);
			return;
		}

		//2.批量从缓存中读取消息详细信息
		List<Map<String,Object>> messageList = new ArrayList<>();
		List<String> messageKeyList = new ArrayList<>();
		JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
		log.info("=============================================================落地===========================================================");
		callBack.invoke(new JedisBatchOperation(){
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {

				long messagePos = 0L;
				List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
				for(String messageId : messageIdList){
					map.put(Constants.FIELD_MESSAGE_ID, messageId);
					String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
					redisResponseList.add(pipeline.hgetAll(messageKey));
					messageKeyList.add(messageKey);
				}
				pipeline.sync();

				for(Response<Map<String, String>> redisResponse : redisResponseList){
					Map<String,String> messageStringMap = redisResponse.get();
					Map<String,Object> messageObjectMap = new HashMap<>();
					if(messageStringMap.get("message_imid") == null){
						return;
					}
					if(!MiscUtils.isEmpty(messageStringMap.get("message_id"))){
						messageObjectMap.put("message_id", messageStringMap.get("message_id"));
					}

					messageObjectMap.put("course_id", messageStringMap.get("course_id"));
					messageObjectMap.put("message_url", messageStringMap.get("message_url"));
					messageObjectMap.put("message", messageStringMap.get("message"));
					messageObjectMap.put("message_question", messageStringMap.get("message_question"));
					if(!MiscUtils.isEmpty(messageStringMap.get("audio_time"))){
						messageObjectMap.put("audio_time", Long.parseLong(messageStringMap.get("audio_time")));
					}else {
						messageObjectMap.put("audio_time", 0);
					}
					messageObjectMap.put("message_pos", messagePos++);
					messageObjectMap.put("message_type", messageStringMap.get("message_type"));
					messageObjectMap.put("send_type", messageStringMap.get("send_type"));
					messageObjectMap.put("creator_id", messageStringMap.get("creator_id"));
					if(!MiscUtils.isEmpty(messageStringMap.get("create_time"))){
						Date createTime = new Date(Long.parseLong(messageStringMap.get("create_time")));
						messageObjectMap.put("create_time", createTime);
					}
					if(!MiscUtils.isEmpty(messageStringMap.get("audio_image"))){
						messageObjectMap.put("audio_image", messageStringMap.get("audio_image"));
					}
					if(!MiscUtils.isEmpty(messageStringMap.get("status"))){
						messageObjectMap.put("status",messageStringMap.get("status"));
					}else{
						messageObjectMap.put("status",0);
					}

					messageObjectMap.put("message_imid", messageStringMap.get("message_imid"));

					messageList.add(messageObjectMap);
				}
			}
		});
		log.debug("课程信息插入数据库"+messageList.size());
		//3.批量插入到数据库中
		Integer insertResult = courseMessageMapper.insertCourseMessageList(messageList);

		//4.如果插入数据库正常，则删除缓存中的内容
		if(insertResult != null && insertResult > 0){
			//删除redis中的key
			String[] messageKeyArray = new String[messageKeyList.size()];
			messageKeyList.toArray(messageKeyArray);
			jedisObject.del(messageKeyArray);
			jedisObject.del(messageListKey);
			String messageUserListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_USER, map);
			String messageLecturerListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER, map);
			String messageLecturerVoiceListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST_LECTURER_VOICE, map);
			jedisObject.del(messageUserListKey);
			jedisObject.del(messageLecturerListKey);
			jedisObject.del(messageLecturerVoiceListKey);
		}

	}
}
