package qingning.common.entity;

import java.util.Map;

public class MessageEntity {
	private Map<String, String> messages;
	public void setMessages(Map<String, String> messages){
		this.messages=messages;
	}
	public String getMessages(String code){
		return this.messages==null?"":this.messages.get(code);
	}
}
