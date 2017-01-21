package qingning.mq.server.entyity;

import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qingning.common.util.MiscUtils;

public abstract class ScheduleTask implements Runnable{
	private static Logger log = LoggerFactory.getLogger(ScheduleTask.class);
	private ScheduledFuture<?> scheduledFuture = null; 
	private String courseId=null;
	private String lecturerId=null;
	private long startTime=0;
	private String taskName = null;
	private String id;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setTaskName(String taskName){
		this.taskName=taskName;
	}
	
	public String getTaskName(){
		return this.taskName;
	}
	
	public String getCourseId() {
		return courseId;
	}
	public void setCourseId(String courseId) {
		this.courseId = courseId;
	}	
	public void setLecturerId(String lecturerId) {
		this.lecturerId = lecturerId;
	}
	public String getLecturerId() {
		return lecturerId;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public void setScheduledFuture(ScheduledFuture<?> scheduledFuture){
		this.scheduledFuture=scheduledFuture;
	}
	
	public void cancel(){
		if(scheduledFuture!=null && !scheduledFuture.isDone()){
			scheduledFuture.cancel(false);
		}
	}
	
	@Override
	public void run() {
		try{
			if(!MiscUtils.isEmpty(taskName) && scheduledFuture !=null){
				process();
			}
		} catch(Exception e){
			log.warn(e.getMessage());
		} finally{			
		}
	}	
	public abstract void process();
}
