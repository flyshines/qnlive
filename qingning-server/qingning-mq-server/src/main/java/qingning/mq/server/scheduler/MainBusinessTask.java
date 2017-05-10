package qingning.mq.server.scheduler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.scheduling.annotation.Scheduled;

import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.mq.server.event.BackendEvent;
import qingning.mq.server.imp.*;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainBusinessTask implements Lifecycle, ApplicationListener<BackendEvent>{

	private static final Logger logger = LoggerFactory.getLogger(MainBusinessTask.class);
	private List<AbstractMsgService> list = new ArrayList<>();


	@Autowired
	private ApplicationContext context;

	@Autowired(required=true)
	private JedisUtils jedisUtils;

	@Autowired(required=true)
	private CoursesMapper coursesMapper;

	@Autowired(required=true)
	private LoginInfoMapper loginInfoMapper;

	@Autowired(required=true)
	private CourseAudioMapper courseAudioMapper;

	@Autowired(required=true)
	private CourseImageMapper courseImageMapper;

	@Autowired(required=true)
	private LecturerMapper lecturerMapper;

	@Autowired(required=true)
	private LiveRoomMapper liveRoomMapper;
	
	@Autowired(required=true)
	private DistributerMapper distributerMapper;
	
	@Autowired(required=true)
	private LecturerDistributionInfoMapper lecturerDistributionInfoMapper;
	
	@Autowired(required=true)
	private RoomDistributerMapper roomDistributerMapper;
	
	@Autowired(required=true)
	private LecturerDistributionLinkMapper lecturerDistributionLinkMapper;
	
	@Autowired(required=true)
	private RoomDistributerDetailsMapper roomDistributerDetailsMapper;	
	@Autowired(required=true)
	private UserMapper userMapper;
	@Autowired(required=true)
	private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;
	
	public void init(){
		if(list.isEmpty()){
			//缓存同步到数据库定时任务
			CacheSyncDatabaseServerImpl cacheSyncDatabaseServerimpl = new CacheSyncDatabaseServerImpl();
			cacheSyncDatabaseServerimpl.setCoursesMapper(coursesMapper);
			cacheSyncDatabaseServerimpl.setLoginInfoMapper(loginInfoMapper);
			cacheSyncDatabaseServerimpl.setLecturerMapper(lecturerMapper);
			cacheSyncDatabaseServerimpl.setLiveRoomMapper(liveRoomMapper);
			cacheSyncDatabaseServerimpl.setDistributerMapper(distributerMapper);
			cacheSyncDatabaseServerimpl.setLecturerDistributionInfoMapper(lecturerDistributionInfoMapper);
			cacheSyncDatabaseServerimpl.setRoomDistributerMapper(roomDistributerMapper);
			cacheSyncDatabaseServerimpl.setCourseImageMapper(courseImageMapper);
			cacheSyncDatabaseServerimpl.setLecturerDistributionLinkMapper(lecturerDistributionLinkMapper);
			cacheSyncDatabaseServerimpl.setRoomDistributerDetailsMapper(roomDistributerDetailsMapper);
			cacheSyncDatabaseServerimpl.setUserMapper(userMapper);
			cacheSyncDatabaseServerimpl.setLecturerCoursesProfitMapper(lecturerCoursesProfitMapper);
			list.add(cacheSyncDatabaseServerimpl);

			//讲师课程列表定时任务
			LecturerCoursesServerImpl lecturerCoursesServerimpl = new LecturerCoursesServerImpl();
			lecturerCoursesServerimpl.setCoursesMapper(coursesMapper);
			lecturerCoursesServerimpl.setLoginInfoMapper(loginInfoMapper);
			lecturerCoursesServerimpl.setCourseAudioMapper(courseAudioMapper);
			lecturerCoursesServerimpl.setCourseImageMapper(courseImageMapper);
			MessagePushServerImpl messagePushServerImpl = (MessagePushServerImpl)context.getBean("MessagePushServer");
			lecturerCoursesServerimpl.setMessagePushServerimpl(messagePushServerImpl);
			list.add(lecturerCoursesServerimpl);

			//平台课程列表定时任务
			PlatformCoursesServerImpl platformCoursesServerimpl = new PlatformCoursesServerImpl();
			platformCoursesServerimpl.setCoursesMapper(coursesMapper);
			platformCoursesServerimpl.setCourseImageMapper(courseImageMapper);
			platformCoursesServerimpl.setCourseAudioMapper(courseAudioMapper);
			list.add(platformCoursesServerimpl);

			//课程极光定时推送
			CreateCourseNoticeTaskServerImpl createCourseNoticeTaskServerImpl = new CreateCourseNoticeTaskServerImpl();
			createCourseNoticeTaskServerImpl.setCoursesMapper(coursesMapper);
			list.add(createCourseNoticeTaskServerImpl);

			clearMessageLock();
		}
	}

	//本地测试 5秒执行一次，开发服30分钟执行一次，正式每天凌晨1点执行
	//@Scheduled(cron = "*/5 * * * * ? ")
	//@Scheduled(cron = "0 */30 * * * ? ")
	@Scheduled(cron = "0 0 1 * * ?")
	public void backstageMethod() {
		init();
		String[] appNames = new String[0];
		try {
			appNames = MiscUtils.getAppName();
		} catch (Exception e) {
			e.printStackTrace();
		}
		for(String appName : appNames){
			backstageMethod(appName);
		}
//		logger.info("=====> 主业务定时任务驱动开始  ====");
//		for(AbstractMsgService server : list){
//			logger.info("===> 执行任务 【"+server.getClass().getName()+"】 === ");
//			try {
//				server.process(null, jedisUtils, context);
//			} catch (Exception e) {
//				logger.error("---- 主业务定时任务执行失败!: "+ server.getClass().getName() +" ---- ", e);
//			}
//		}
	}
	public void backstageMethod(String appName){
		for(AbstractMsgService server : list){
			logger.info("===> 执行任务 【"+server.getClass().getName()+"】 === ");
			try {
				server.process(null, jedisUtils, context,appName);
			} catch (Exception e) {
				logger.error("---- 主业务定时任务执行失败!: "+ server.getClass().getName() +" ---- ", e);
			}
		}
	}

	public void loadLecturerID(String appName){
		Jedis jedis = jedisUtils.getJedis(appName);
		if(!jedis.exists(Constants.CACHED_LECTURER_KEY)){
			int start_pos = 0;
			int page_count =50000;
			Map<String,Object> query = new HashMap<String,Object>();
			query.put("start_pos", start_pos);
			query.put("page_count", page_count);
			
			List<Map<String,Object>> list = null;
			do{
				list = lecturerMapper.findLectureId(query);
				if(!MiscUtils.isEmpty(list)){
					final List<Map<String,Object>> valueList  = list;
					((JedisBatchCallback)jedis).invoke(new JedisBatchOperation(){
						@Override
						public void batchOperation(Pipeline pipeline, Jedis jedis) {
							Map<String,String> query = new HashMap<String,String>();
							for(Map<String,Object> value:valueList){
								String lecture_id = (String)value.get("lecturer_id");
								pipeline.sadd(Constants.CACHED_LECTURER_KEY, (String)value.get("lecturer_id"));
								
								Map<String,String> lectureValue = new HashMap<String,String>();
								MiscUtils.converObjectMapToStringMap(lecturerMapper.findLectureByLectureId(lecture_id), lectureValue);
								query.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
								String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, query);
								pipeline.hmset(key, lectureValue);
								String lectureLiveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, query);
								List<Map<String,Object>> roomList = liveRoomMapper.findLiveRoomByLectureId("lecture_id");
								if(!MiscUtils.isEmpty(roomList)){
									for(Map<String,Object> room : roomList){
										String roomid = (String) room.get(Constants.FIELD_ROOM_ID);
										Map<String,String> roomValue = new HashMap<String,String>();
										MiscUtils.converObjectMapToStringMap(room, roomValue);
										query.put(Constants.FIELD_ROOM_ID, roomid);
										key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ROOM, query);
										pipeline.hmset(key, roomValue);
										pipeline.hset(lectureLiveRoomKey, roomid, "1");
									}
								}
							}
							pipeline.sync();
						}					
					});
					start_pos=start_pos+page_count;
					query.put("start_pos", start_pos);
				}
			} while (!MiscUtils.isEmpty(list) && list.size()>=page_count);
		}
	}
	
	@Override
	public void start() {
		try {
            String[] appNames = MiscUtils.getAppName();
            for(String appName : appNames){
                backstageMethod();
                loadLecturerID(appName);
            }
        } catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		list.clear();
	}

	@Override
	public boolean isRunning() {
		if(!list.isEmpty()){
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onApplicationEvent(BackendEvent event) {
		if(event == null){
			return;
		}
		if(Constants.REFRESH.equals(event.getAction())){
            this.start();
		}
	}

	private void clearMessageLock() {
		//ImMsgServiceImp imMsgServiceImp = (ImMsgServiceImp)context.getBean("ImMsgServiceImp");
		ImMsgServiceImp.clearMessageLockMap();
	}

}
