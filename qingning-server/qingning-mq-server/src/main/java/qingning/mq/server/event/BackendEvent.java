package qingning.mq.server.event;

import org.springframework.context.ApplicationEvent;

public class BackendEvent extends ApplicationEvent {
	private static final long serialVersionUID = 1L;
	
	String action = null;
	public BackendEvent(String source) {
		super(source);
		this.action = source;
	}
	public String getAction(){
		return this.action;
	}
}
