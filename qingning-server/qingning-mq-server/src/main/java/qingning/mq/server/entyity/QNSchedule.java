package qingning.mq.server.entyity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import qingning.common.util.MiscUtils;

public class QNSchedule {
	public static final String TASK_END_COURSE="task_end_course";//结束课程
	public static final String TASK_COURSE_OVER_TIME_NOTICE="course_over_time_notice";//课程超时
	public static final String TASK_COURSE_24H_NOTICE="course_24h_notice";//24小时提醒
	public static final String TASK_COURSE_5MIN_NOTICE="course_5min_notice";//5分钟提醒
	public static final String TASK_COURSE_15MIN_NOTICE="course_15min_notice";//15分钟提醒
	public static final String TASK_LECTURER_NOTICE="lecturer_notice";//讲师通知
	public static final String TASK_COURSE_START="courase_start";//课程开课提醒
	
	private Map<String, ScheduleTask>  taskEndCourse = new HashMap<String, ScheduleTask>();
	private Map<String, ScheduleTask>  courseOverTimeNotice = new HashMap<String, ScheduleTask>();
	private Map<String, ScheduleTask>  course24hNotice = new HashMap<String, ScheduleTask>();
	private Map<String, ScheduleTask>  course5MinNotice = new HashMap<String, ScheduleTask>();
	private Map<String, ScheduleTask>  course15MinNotice = new HashMap<String, ScheduleTask>();
	private Map<String, ScheduleTask>  lecturerNotice = new HashMap<String, ScheduleTask>();
	private Map<String, ScheduleTask>  couraseStart = new HashMap<String, ScheduleTask>();

	private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = null;//new ScheduledThreadPoolExecutor(100);
	
	public QNSchedule(){
		int processors = Math.max(Runtime.getRuntime().availableProcessors(),4);	
		scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(processors*10);
	}
	
	private Map<String, ScheduleTask> getScheduleTaskMap(String taskName){
		Map<String, ScheduleTask> map = null;
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
		Map<String, ScheduleTask> map = getScheduleTaskMap(task.getTaskName());
		if(map.containsKey(task.getId())){
			return;
		}
		map.put(task.getId(), task);
		task.setScheduledFuture(scheduledThreadPoolExecutor.schedule(task, task.getStartTime()-System.currentTimeMillis(), TimeUnit.MILLISECONDS));		
	}
	public void cancelTask(String id, String taskName){
		Map<String, ScheduleTask> map = getScheduleTaskMap(taskName);
		if(map!=null && map.containsKey(id)){
			ScheduleTask task = map.remove(id);
			if(task != null){
				task.cancel();
				scheduledThreadPoolExecutor.purge();
			}
		}		
	}
	
	public void cancelTask(ScheduleTask task){
		this.cancelTask(task.getId(), task.getTaskName());
	}
	
	public boolean containTask(String id, String taskName){
		boolean result = false;
		Map<String, ScheduleTask> map = getScheduleTaskMap(taskName);
		if(map!=null && map.containsKey(id)){
			result=true;
		}
		return result;
	}
}
