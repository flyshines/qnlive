package qingning.server;

import org.springframework.context.ApplicationContext;

import qingning.common.entity.FunctionInfo;
import qingning.common.entity.RequestEntity;
import qingning.common.util.JedisUtils;
import qingning.common.util.MqUtils;

public interface GCWServer {
/*	void setAccessTokenRequire(boolean accessTokenRequire);
	void setTimesLimits(long millisecond);	
	void addInputParameter(InputParameter inputParameter);
	void addOutputParameter(OutputParameter outputParameter);*/
	
	void addFunctionInfo(FunctionInfo functionInfo);
	void setJedisUtils(JedisUtils jedisUtils);
	void validateRequestParamters(RequestEntity reqEntity) throws Exception;
	void setApplicationContext(ApplicationContext context);
	void setMqUtils(MqUtils mqUtils);
	void initRpcServer();
	Object invoke(RequestEntity reqEntity) throws Exception;
	Object process(RequestEntity reqEntity) throws Exception;
	Object processReturnValue(RequestEntity reqEntity, Object value) throws Exception;
}
