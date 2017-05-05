package qingning.common.entity;

import java.util.LinkedList;
import java.util.List;

public class FunctionInfo {
	private String functionName;
	
	private boolean accessTokenRequire;
	private boolean appNameRequire;
	private long millisecond;
	private List<InputParameter> inputParameterList = new LinkedList<InputParameter>();
	private List<OutputParameter> outputParameterList = new LinkedList<OutputParameter>();
				
	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public void setAccessTokenRequire(boolean accessTokenRequire){
		this.accessTokenRequire=accessTokenRequire;
	}
	
	public boolean isAccessTokenRequire(){
		return this.accessTokenRequire;
	}
	
	public void setTimesLimits(long millisecond){
		this.millisecond=millisecond;
	}
	
	public long getTimesLimits(){
		return this.millisecond;
	}
	
	public boolean isTimesLimitsRequire(){
		return this.millisecond > 0;
	}
	
	public void addInputParameter(InputParameter inputParameter){
		inputParameterList.add(inputParameter);
	}
	
	public List<InputParameter> getInputParameterList(){
		return inputParameterList;
	}
	
	public void addOutputParameter(OutputParameter outputParameter){
		outputParameterList.add(outputParameter);
	}
	public List<OutputParameter> getOutputParameterList(){
		return outputParameterList;
	}

	public void setAppNameRequire(boolean appNameRequire){this.appNameRequire = appNameRequire;}
	public boolean isAppNameRequire(){return appNameRequire;}




}
