package qingning.common.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngineManager;

import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;

public class InputParameter {
	
	private String name;	
	private String format;
	private String formatErrorCode;
	
	private String type;
	private String validate;
	private String validateErrorCode;
	
	private boolean require=false;
	private String requireErrorCode;
	
	private List<String> checkConditionList = new ArrayList<String>();
	private String validationFuncName = null;
	Invocable invoke;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	public void setFormat(String format) {
		if(MiscUtils.isEmpty(format)){
			this.format = null;
			this.formatErrorCode=null;
		} else {
			int pos =format.lastIndexOf(",");
			if(pos < 1){
				this.format = format.trim();
				this.formatErrorCode=null;
			} else {
				this.format = format.substring(0, pos).trim();
				this.formatErrorCode=format.substring(pos+1).trim();
			}
		}
	}

	public void setValidate(String validate) {
		if(MiscUtils.isEmpty(validate)){
			this.validate = null;
			this.validateErrorCode=null;
		} else {
			int pos =validate.lastIndexOf(",");
			if(pos < 1){
				this.validate = validate.trim();
				this.validateErrorCode=null;
			} else {
				this.validate = validate.substring(0, pos).trim();
				this.validateErrorCode=validate.substring(pos+1).trim();
			}
		}
		if(!MiscUtils.isEmpty(this.validate)){
			String[] functionStr= this.validate.split("=>");
			if(functionStr.length != 2){
				this.validate=null;
			} else {
				functionStr[1]=MiscUtils.specialCharReplace(functionStr[1]);
				int length = functionStr[0].trim().length();
				String parameters = functionStr[0].substring(1, length-1);
				String[] tmpArr = parameters.split(",");
				for(String key : tmpArr){
					key = key.trim();
					if(!MiscUtils.isEmpty(key)){
						checkConditionList.add(key);	
					}
				}
				if(checkConditionList.isEmpty()){
					this.validate=null;
				} else {
					validationFuncName = this.name.trim()+Constants.VALIDATION;
					this.validate = Constants.FUNCTION + " " + validationFuncName +" "+ functionStr[0] + " " + functionStr[1];
				}
			}
		}
	}

	public void setRequire(String require) {
		if(MiscUtils.isEmpty(require)){
			this.require = false;
			this.requireErrorCode=null;
		} else {
			int pos =require.lastIndexOf(",");
			if(pos < 1){
				this.require = "y".equalsIgnoreCase(require.trim());
				this.requireErrorCode=null;
			} else {
				this.require = "y".equalsIgnoreCase(require.substring(0, pos).trim());
				this.requireErrorCode=require.substring(pos+1).trim();
			}
		}
	}
	
	public boolean isRequire() {
		return require;
	}

	public String getFormat() {
		return format;
	}
	public String getFormatErrorCode() {
		return formatErrorCode;
	}
	
	public String getValidate() {
		return validate;
	}
	
	public String getRequireErrorCode() {
		return requireErrorCode;
	}
	
	public List<String> getCheckCondition(){
		return checkConditionList;
	}
	
	public void checkCondition(Map<String, Object> value) throws Exception{		
		if(MiscUtils.isEmpty(this.validate)){
			return;
		}
		if(invoke==null){
			ScriptEngineManager scriptEngineManager = new ScriptEngineManager();  
			Compilable nashorn = (Compilable) scriptEngineManager.getEngineByName(Constants.JS_ENGINE);
			String scriptStr = String.format("var utils = Java.type('%s');",MiscUtils.class.getName()) + this.validate;
			CompiledScript script= nashorn.compile(scriptStr);
			script.eval();
			invoke = (Invocable)nashorn;
		}
		Object[] valueArray = new Object[checkConditionList.size()];
		for(int i=0; i < valueArray.length; ++i){
			valueArray[i]=value.get(checkConditionList.get(i));
		}

		boolean  ret = (boolean) invoke.invokeFunction(validationFuncName, valueArray);
		if(!ret){
			throw new QNLiveException(this.validateErrorCode);
		}
	}
}
