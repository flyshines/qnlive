package qingning.common.entity;

import java.util.Date;

public class OutputParameter {
	private String name;
	private String fieldName;
	private String defaultValue="";
	private String type;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDefault() {
		return defaultValue;
	}
	public void setDefault(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}	
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public Object convertValue(Object value){
		if(value==null){
			value=defaultValue;
		}else if(value instanceof Date){
			value=((Date)value).getTime();
		}
		return value;
	}
}
