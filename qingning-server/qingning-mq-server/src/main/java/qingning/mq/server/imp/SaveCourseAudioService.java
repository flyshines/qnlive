package qingning.mq.server.imp;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.CourseAudioMapper;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.*;

public class SaveCourseAudioService extends AbstractMsgService {

	@Autowired(required = true)
	private CourseAudioMapper courseAudioMapper;

	@SuppressWarnings("unchecked")
	@Override
	public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context)
			throws Exception {

		Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

		//批量读取缓存中的内容
		Map<String, Object> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
		String audioListKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS, map);

		Jedis jedisObject = jedisUtils.getJedis();
		//1.从缓存中查询该课程的消息列表
		Set<String> audioIdList = jedisObject.zrange(audioListKey, 0 , -1);
		if(audioIdList == null || audioIdList.size() == 0){
			return;
		}

		//2.批量从缓存中读取消息详细信息
		List<Map<String,Object>> audioList = new ArrayList<>();

		JedisBatchCallback callBack = (JedisBatchCallback)jedisUtils.getJedis();
		callBack.invoke(new JedisBatchOperation(){
			@Override
			public void batchOperation(Pipeline pipeline, Jedis jedis) {

				Long messagePos = 0L;
				List<Response<Map<String, String>>> redisResponseList = new ArrayList<>();
				for(String audio : audioIdList){
					map.put(Constants.FIELD_AUDIO_ID, audio);
					String audioKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIO, map);
					redisResponseList.add(pipeline.hgetAll(audioKey));
					pipeline.del(audioKey);
				}
				pipeline.sync();

				int audioPos = 0;
				for(Response<Map<String, String>> redisResponse : redisResponseList){
					Map<String,String> messageStringMap = redisResponse.get();
					Map<String,Object> messageObjectMap = new HashMap<>();
					if(messageStringMap.get("audio_id") == null){
						return;
					}
					messageObjectMap.put("audio_id", messageStringMap.get("audio_id"));
					messageObjectMap.put("course_id", messageStringMap.get("course_id"));
					messageObjectMap.put("audio_url", messageStringMap.get("audio_url"));
					if(StringUtils.isNotBlank(messageStringMap.get("audio_time"))){
						messageObjectMap.put("audio_time", Long.parseLong(messageStringMap.get("audio_time")));
					}else {
						messageObjectMap.put("audio_time", 0);
					}
					messageObjectMap.put("audio_image", messageStringMap.get("audio_image"));
					if(StringUtils.isNotBlank(messageStringMap.get("create_time"))){
						Date createTime = new Date(Long.parseLong(messageStringMap.get("create_time")));
						messageObjectMap.put("create_time", createTime);
					}
					messageObjectMap.put("audio_image", messageStringMap.get("audio_image"));
					messageObjectMap.put("audioPos", audioPos);
					audioList.add(messageObjectMap);
					audioPos++;
				}
			}
		});

		//3.批量插入到数据库中
		Integer insertResult = courseAudioMapper.saveCourseAudio(audioList);

		map.clear();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, reqMap.get("course_id").toString());
		String audioJsonStringKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_AUDIOS_JSON_STRING, map);
		jedisObject.set(audioJsonStringKey, JSONObject.toJSONString(audioList));
		jedisObject.del(audioListKey);
	}
}
