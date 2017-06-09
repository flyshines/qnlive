package qingning.mq.server.entyity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QNQuartzSchedule {
	private static Logger log = LoggerFactory.getLogger(QNQuartzSchedule.class);
	public static final String TASK_END_COURSE="task_end_course";
	public static final String TASK_COURSE_OVER_TIME_NOTICE="course_over_time_notice";
	public static final String TASK_COURSE_24H_NOTICE="course_24h_notice";
	public static final String TASK_COURSE_5MIN_NOTICE="course_5min_notice";
	public static final String TASK_COURSE_15MIN_NOTICE="course_15min_notice";
	public static final String TASK_LECTURER_NOTICE="lecturer_notice";
	public static final String TASK_COURSE_START="courase_start";//课程开课提醒


    private Scheduler scheduler = null;
    private SchedulerFactory factory = null;
	private Map<String, SimpleTrigger>  taskEndCourse = new HashMap<String, SimpleTrigger>();
	private Map<String, SimpleTrigger>  courseOverTimeNotice = new HashMap<String, SimpleTrigger>();
	private Map<String, SimpleTrigger>  course24hNotice = new HashMap<String, SimpleTrigger>();
	private Map<String, SimpleTrigger>  course5MinNotice = new HashMap<String, SimpleTrigger>();
	private Map<String, SimpleTrigger>  course15MinNotice = new HashMap<String, SimpleTrigger>();
	private Map<String, SimpleTrigger>  lecturerNotice = new HashMap<String, SimpleTrigger>();
	private Map<String, SimpleTrigger>  couraseStart = new HashMap<String, SimpleTrigger>();

    public static class QNSchedulerJob implements Job{
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			try{
				JobDataMap map = context.getTrigger().getJobDataMap();
				ScheduleTask task = (ScheduleTask)map.get("Task");
				QNQuartzSchedule schedule = (QNQuartzSchedule)map.get("Schedule");				
				task.SetCheckScheduledFuture(false);
				log.debug("Run Task ("+task.getId()+"---"+task.getTaskName());
				task.run();
				schedule.cancelTask(task.getId(), task.getTaskName(), true);
			}catch(Exception e){
				log.error("Run Task Exception:"+e.getMessage());
			}
			
		}    	
    }
	
    public QNQuartzSchedule(){
    	factory = new StdSchedulerFactory();
		try {
			scheduler = factory.getScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			log.error("create Scheduler error");
		}
    }
	

    
    
	private Map<String, SimpleTrigger> getScheduleTaskMap(String taskName){
		Map<String, SimpleTrigger> map = null;
		if(TASK_END_COURSE.equals(taskName)){
			map = taskEndCourse;
		} else if(TASK_COURSE_OVER_TIME_NOTICE.equals(taskName)){
			map = courseOverTimeNotice;
		} else if(TASK_COURSE_24H_NOTICE.equals(taskName)){
			map = course24hNotice;
		} else if(TASK_COURSE_5MIN_NOTICE.equals(taskName)){
			map = course5MinNotice;
		} else if(TASK_COURSE_15MIN_NOTICE.equals(taskName)){
			map = course15MinNotice;
		} else if(TASK_LECTURER_NOTICE.equals(taskName)){
			map = lecturerNotice;
		}else if(TASK_COURSE_START.equals(taskName)){
			map = couraseStart;
		}
		return map;
	}
	
	public void add(ScheduleTask task){
		if(task == null) return;
		Map<String, SimpleTrigger> map = getScheduleTaskMap(task.getTaskName());
		if(map.containsKey(task.getId())){
			return;
		}

		JobKey jobKey = new JobKey(task.getId(),task.getTaskName());
		TriggerKey triggerKey = new TriggerKey(task.getId(),task.getTaskName());
		
		JobDetailImpl details = new JobDetailImpl();
        details.setKey(jobKey);
        details.setJobClass(QNSchedulerJob.class);
        
        SimpleTriggerImpl trigger = new SimpleTriggerImpl();
        trigger.setKey(triggerKey);
        trigger.setJobKey(jobKey);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("Task", task);
        jobDataMap.put("Schedule", this);
        trigger.setJobDataMap(jobDataMap);        
        trigger.setStartTime(new Date(task.getStartTime()));
        try {
        	map.put(task.getId(), trigger);
			scheduler.scheduleJob(details, trigger);			
		} catch (SchedulerException e) {
			map.remove(task.getId());
			log.error("create scheduleJob error["+task.getId()+"]");
		} 
	}
	
	public void cancelTask(String id, String taskName){
		cancelTask(id, taskName, false);
	}
	
	public void cancelTask(String id, String taskName, boolean onlyRemove){
		Map<String, SimpleTrigger> map = getScheduleTaskMap(taskName);
		if(map!=null && map.containsKey(id)){
			SimpleTrigger trigger = map.get(id);
			if(trigger != null){				
				try {
					if(!onlyRemove){
						scheduler.pauseTrigger(trigger.getKey());
					}
					scheduler.unscheduleJob(trigger.getKey());	
					scheduler.deleteJob(trigger.getJobKey());
				} catch (SchedulerException e) {
					log.error("cancel Job["+id+"] error");
				}
				map.remove(id);
			}
		}	
	}
	
	public boolean containTask(String id, String taskName){
		boolean result = false;
		Map<String, SimpleTrigger> map = getScheduleTaskMap(taskName);
		if(map!=null && map.containsKey(id)){
			result=true;
		}
		return result;
	}
}
