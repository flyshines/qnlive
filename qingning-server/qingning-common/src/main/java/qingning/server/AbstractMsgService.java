package qingning.server;

import java.lang.reflect.Method;
import java.util.HashMap;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.JedisUtils;
import qingning.server.annotation.FunctionName;

public abstract class AbstractMsgService {
	private HashMap<String, Method> methodMap = new HashMap<String, Method>();
	public void invoke(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception{
		if(requestEntity==null){
			return;
		}
		boolean processed = false;		
		if(methodMap !=null && methodMap.isEmpty()){				
			Method[] methods = this.getClass().getDeclaredMethods();
			if(methods !=null && methods.length>0){
				for(Method curMethod:methods){
					FunctionName functionName = curMethod.getAnnotation(FunctionName.class);
					if(functionName==null){
						continue;
					}
					Class<?>[] types = curMethod.getParameterTypes();
					if(types == null || types.length != 3){
						continue;
					}
					if(RequestEntity.class.isAssignableFrom(types[0]) && JedisUtils.class.isAssignableFrom(types[1]) 
							&& ApplicationContext.class.isAssignableFrom(types[2])){
						curMethod.setAccessible(true);
						methodMap.put(functionName.value(), curMethod);
					}						
				}
			}
			if(methodMap.isEmpty()){
				methodMap=null;

			}			
		}
		if(methodMap!=null){
			Method method = methodMap.get(requestEntity.getFunctionName());
			method.invoke(this, requestEntity, jedisUtils,context);
			processed=true;
		}
		if(!processed){
			process(requestEntity, jedisUtils, context);
		}
	}

	public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception{

	}

	protected RequestEntity generateRequestEntity(String serverName, String method, String functionName, Object param) {
		RequestEntity requestEntity = new RequestEntity();
		requestEntity.setServerName(serverName);
		requestEntity.setMethod(method);
		requestEntity.setFunctionName(functionName);
		requestEntity.setParam(param);
		return requestEntity;
	}
}
