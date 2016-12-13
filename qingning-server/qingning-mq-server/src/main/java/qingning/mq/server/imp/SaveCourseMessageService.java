package qingning.mq.server.imp;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.*;

public class SaveCourseMessageService extends AbstractMsgService{

	@Override
	public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context)
			throws Exception {
		Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
		Jedis jedis = jedisUtils.getJedis();
		//批量读取缓存中的内容
		Map<String, Object> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
		String messageListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE_LIST, map);
		Set<String> messageIdList = jedis.zrange(messageListKey, 0 , -1);

		List<Map<String,Object>> messageList = new ArrayList<>();
		JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
		callBack.invoke(new JedisBatchOperation(){
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {
				Long messagePos = 0L;
				for(String messageId : messageIdList){
					map.put(Constants.FIELD_MESSAGE_ID, messageId);
					String messageKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_MESSAGE, map);
					Response<Map<String, String>> redisResponse = pipeline.hgetAll(messageKey);
					Map<String,String> messageStringMap = redisResponse.get();
					Map<String,Object> messageObjectMap = new HashMap<>();
					messageObjectMap.put("message_id", messageStringMap.get("message_id"));
					messageObjectMap.put("course_id", messageStringMap.get("course_id"));
					messageObjectMap.put("message_url", messageStringMap.get("message_url"));
					messageObjectMap.put("message_question", messageStringMap.get("message_question"));
					if(StringUtils.isNotBlank(messageStringMap.get("audio_time"))){
						messageObjectMap.put("audio_time", Long.parseLong(messageStringMap.get("audio_time")));
					}
					messageObjectMap.put("message_pos", messagePos);
					messageObjectMap.put("message_type", messageStringMap.get("message_type"));
					messageObjectMap.put("send_type", messageStringMap.get("send_type"));
					messageObjectMap.put("creator_id", messageStringMap.get("creator_id"));
					if(StringUtils.isNotBlank(messageStringMap.get("create_time"))){
						Date createTime = new Date(Long.parseLong(messageStringMap.get("create_time")));
						messageObjectMap.put("create_time", createTime);
					}
					messageList.add(messageObjectMap);
				}

				pipeline.sync();
			}
		});

		//批量插入到数据库中


		//判断插入结果，插入结果正常则批量删除缓存中的内容

		System.out.println("test");
	}
}
