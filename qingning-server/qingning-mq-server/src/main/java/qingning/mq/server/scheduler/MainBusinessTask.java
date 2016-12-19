package qingning.mq.server.scheduler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import qingning.common.util.JedisUtils;
import qingning.mq.persistence.mybatis.CoursesMapper;
import qingning.mq.persistence.mybatis.LoginInfoMapper;
import qingning.mq.server.imp.LecturerCoursesServerImpl;
import qingning.mq.server.imp.PlatformCoursesServerImpl;
import qingning.server.rabbitmq.MessageServer;

import java.util.ArrayList;
import java.util.List;

public class MainBusinessTask {
	
	private static final Logger logger = LoggerFactory.getLogger(MainBusinessTask.class);
	private List<MessageServer> list = new ArrayList<>();
	
	@Autowired(required=true)
	private JedisUtils jedisUtils;
	
    @Autowired(required=true)
    private CoursesMapper coursesMapper;

	@Autowired(required=true)
	private LoginInfoMapper loginInfoMapper;

	public void init(){

		LecturerCoursesServerImpl lecturerCoursesServerimpl = new LecturerCoursesServerImpl();
		lecturerCoursesServerimpl.setCoursesMapper(coursesMapper);
		lecturerCoursesServerimpl.setLoginInfoMapper(loginInfoMapper);
		lecturerCoursesServerimpl.setJedisUtils(jedisUtils);
		list.add(lecturerCoursesServerimpl);

		PlatformCoursesServerImpl platformCoursesServerimpl = new PlatformCoursesServerImpl();
		platformCoursesServerimpl.setCoursesMapper(coursesMapper);
		platformCoursesServerimpl.setJedisUtils(jedisUtils);
		list.add(platformCoursesServerimpl);
	}
	
	//本地测试 5秒执行一次，开发服30分钟执行一次，正式每天凌晨1点执行
    //@Scheduled(cron = "*/5 * * * * ? ")
    //@Scheduled(cron = "0 */30 * * * ? ")
	@Scheduled(cron = "0 0 1 * * ?")
	public void backstageMethod(){
		init();
		logger.info("=====> 主业务定时任务驱动开始  ====");
		for(MessageServer server : list){
			logger.info("===> 执行任务 【"+server.getClass().getName()+"】 === ");
			try {
				 server.process(null);
			} catch (Exception e) {
				logger.error("---- 主业务定时任务执行失败!: "+ server.getClass().getName() +" ---- ", e);
			}
		}
	} 
	
}
