package qingning.common.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngineManager;

import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;

public class OutputParameter {
	private String name;
	private String fieldName;
	private String defaultValue="";
	private String type;
	private List<OutputParameter> outputParameterList = new LinkedList<OutputParameter>();
	private Map<String, OutputParameter> outputParameterMap = new HashMap<String,OutputParameter>();
	
	private List<String> convertConditionList = new ArrayList<String>();
	private String convertFuncName = null;
	private String convertFunc = null;	
	Invocable invoke;
	
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
	
	public void setConvertFunction(String convertFunc){
		if(MiscUtils.isEmpty(convertFunc)){
			this.convertFunc = null;
			return;
		} else {
			this.convertFunc = convertFunc;
		}
		if(!MiscUtils.isEmpty(this.convertFunc)){
			String[] functionStr= this.convertFunc.split("=>");
			if(functionStr.length != 2){
				this.convertFunc=null;
			} else {
				functionStr[1]=MiscUtils.specialCharReplace(functionStr[1]);
				int length = functionStr[0].trim().length();
				String parameters = functionStr[0].substring(1, length-1);
				String[] tmpArr = parameters.split(",");
				for(String key : tmpArr){
					key = key.trim();
					if(!MiscUtils.isEmpty(key)){
						convertConditionList.add(key);	
					}
				}
				if(convertConditionList.isEmpty()){
					this.convertFunc=null;
				} else {
					convertFuncName = this.name.trim()+Constants.CONVERTVALUE;
					this.convertFunc = Constants.FUNCTION + " " + convertFuncName +" "+ functionStr[0] + " " + functionStr[1];
				}
			}
		}
	}
	
	public void addOutputParameterForList(OutputParameter outputParameter){
		this.outputParameterList.add(outputParameter);
	}
	
	public void addOutputParameterForMap(OutputParameter outputParameter){
		this.outputParameterMap.put(outputParameter.getName(), outputParameter);
	}
	public List<OutputParameter> getOutputParameterList(){
		return outputParameterList;
	}
	public Map<String, OutputParameter> getOutputParameterMap(){
		return outputParameterMap;
	}
	
	public Object execConvertFunction(Object value, Map<String, Object> outputMap) throws Exception{		
		if(MiscUtils.isEmpty(this.convertFunc)){
			return value;
		}
		if(invoke==null){
			ScriptEngineManager scriptEngineManager = new ScriptEngineManager();			
			Compilable nashorn = (Compilable) scriptEngineManager.getEngineByName(Constants.JS_ENGINE);
			String scriptStr = String.format("var utils = Java.type('%s');",MiscUtils.class.getName()) + this.convertFunc;
			CompiledScript script= nashorn.compile(scriptStr);
			script.eval();
			invoke = (Invocable)nashorn;
		}
		Object[] valueArray = new Object[convertConditionList.size()];
		for(int i=0; i < valueArray.length; ++i){
			valueArray[i]=outputMap.get(convertConditionList.get(i));
		}

		return invoke.invokeFunction(convertFuncName, valueArray);		
	}
	
	public boolean canExecConvertFunction(){
		return !MiscUtils.isEmpty(convertFunc);
	}
	
	public Object convertValue(Object value) throws Exception{
		Object result = defaultValue;
		if(!Constants.SYSRICHSTR.equals(type)){
			result= MiscUtils.convertObjToObject(value, type, fieldName, defaultValue, Constants.SYSDOUBLE.equalsIgnoreCase(type));
		} else if(!MiscUtils.isEmpty(value)){
			result = MiscUtils.RecoveryEmoji((String)value);
		}
		return result;
	}
}
