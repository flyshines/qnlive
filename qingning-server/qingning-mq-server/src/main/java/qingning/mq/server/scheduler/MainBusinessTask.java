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
import qingning.mq.persistence.mybatis.*;
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
	public void backstageMethod(){
		init();
		logger.info("=====> 主业务定时任务驱动开始  ====");
		for(AbstractMsgService server : list){
			logger.info("===> 执行任务 【"+server.getClass().getName()+"】 === ");
			try {
				server.process(null, jedisUtils, context);
			} catch (Exception e) {
				logger.error("---- 主业务定时任务执行失败!: "+ server.getClass().getName() +" ---- ", e);
			}
		}
	}

	public void loadLecturerID(){
		Jedis jedis = jedisUtils.getJedis();
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
							for(Map<String,Object> value:valueList){
								pipeline.sadd(Constants.CACHED_LECTURER_KEY, (String)value.get("lecturer_id"));
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
		loadLecturerID();
		backstageMethod();
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
			backstageMethod();
			loadLecturerID();
		}
	}

	private void clearMessageLock() {
		ImMsgServiceImp imMsgServiceImp = (ImMsgServiceImp)context.getBean("ImMsgServiceImp");
		imMsgServiceImp.clearMessageLockMap();
	}

}
