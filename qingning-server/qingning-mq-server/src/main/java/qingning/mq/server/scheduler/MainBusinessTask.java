package qingning.mq.server.scheduler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import qingning.common.util.JedisUtils;
import qingning.mq.persistence.mybatis.*;
import qingning.mq.server.imp.CacheSyncDatabaseServerImpl;
import qingning.mq.server.imp.LecturerCoursesServerImpl;
import qingning.mq.server.imp.MessagePushServerImpl;
import qingning.mq.server.imp.PlatformCoursesServerImpl;
import qingning.server.AbstractMsgService;
import qingning.server.rabbitmq.MessageServer;

import java.util.ArrayList;
import java.util.List;

public class MainBusinessTask {

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


	public void init(){

		//缓存同步到数据库定时任务
		CacheSyncDatabaseServerImpl cacheSyncDatabaseServerimpl = new CacheSyncDatabaseServerImpl();
		cacheSyncDatabaseServerimpl.setCoursesMapper(coursesMapper);
		cacheSyncDatabaseServerimpl.setLoginInfoMapper(loginInfoMapper);
		cacheSyncDatabaseServerimpl.setLecturerMapper(lecturerMapper);
		cacheSyncDatabaseServerimpl.setLiveRoomMapper(liveRoomMapper);
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

}
