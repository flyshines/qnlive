package qingning.common.entity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
public class ResponseEntity {
	@JsonProperty("code")
	private String code="";
	@JsonProperty("msg")
	private String msg="";
	@JsonProperty("res_data")
	private Object returnData = new HashMap<String, Object>();
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public Object getReturnData() {
		return returnData;
	}
	public void setReturnData(Object returnData) {
		if(returnData != null){
			if(returnData instanceof Map || returnData instanceof List){
				this.returnData = returnData;
			} else {
				this.returnData=returnData;
			}
		}
	}
}
