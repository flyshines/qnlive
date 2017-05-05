package qingning.common.entity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestEntity {
	@JsonProperty("accessToken")
	private String accessToken;
	@JsonProperty("serverName")
	private String serverName;
	@JsonProperty("functionName")
	private String functionName;
	@JsonProperty("version")
	private String version;
	@JsonProperty("method")
	private String method;
	@JsonProperty("param")
	private Object param  = null;	
	@JsonProperty("timeStamp")
	private long timeStamp;
	@JsonProperty("appName")
	private String appName;
	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	public String getServerName() {
		return serverName;
	}
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}	
	public String getFunctionName() {
		return functionName;
	}
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public Object getParam() {
		return param;
	}
	public void setParam(Object param) {
		if(param != null){
			if(param instanceof Map || param instanceof List){
				this.param = param;
			} else {
				//throw Exception
			}
		}
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}

}
