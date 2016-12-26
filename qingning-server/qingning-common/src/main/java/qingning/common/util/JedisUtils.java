package qingning.common.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

public final class JedisUtils {
	private JedisPool jedisPool = null;
	private Map<String, String> config = null;
	private JedisProxy jedisProxy = null;
	
	public JedisUtils(String path){
		Map<String, String> config = null;
		try {
			config = MiscUtils.convertPropertiesFileToMap(path);
		} catch (Exception e) {
		}
		if(config==null){
			config = new HashMap<String,String>();
		}
		this.config=config;
	}
	
	public JedisUtils(Map<String, String> config){
		if(config==null){
			config = new HashMap<String,String>();
		}
		this.config=config;
	}

	public Jedis getJedis(){
		Jedis jedis = null;
		JedisPool pool = getJedisPool();
		if(pool!=null){
			if(jedisProxy==null){
				jedisProxy = new JedisProxy(this);
			}
			jedis= jedisProxy.getJedis();
		}
		return jedis;
	}
	
	public String getConfigKeyValue(String key){
		String value=null;
		if(!MiscUtils.isEmpty(key) && !MiscUtils.isEmpty(config)){
			value = MiscUtils.convertString(config.get(key));
		}
		return value;
	}
	
	private JedisPool getJedisPool(){
		if(jedisPool!=null){
			if(!jedisPool.isClosed()){
				return jedisPool;
			}
			try{
				jedisPool.destroy();
			}catch(Exception e){
			}
		}
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

		String maxActive = config.get(Constants.REDIS_POOL_MAXACTIVE);
		String maxIdle = config.get(Constants.REDIS_POOL_MAXIDLE);
		String maxWait = config.get(Constants.REDIS_POOL_MAXWAIT);        
		String testOnBorrow = config.get(Constants.REDIS_POOL_TESTONBORROW);
		String testOnReturn = config.get(Constants.REDIS_POOL_TESTONRETURN);        
		String ip = config.get(Constants.REDIS_IP);
		String port = config.get(Constants.REDIS_PORT);       
		String pass = config.get(Constants.REDIS_PASS);  
		Integer outTime = Integer.valueOf(config.get(Constants.REDIS_POOL_OUTTIME).trim());      
		if (outTime == null) {
			outTime=20000;
		}
		
		if(!MiscUtils.isEmpty(maxActive)){
			try{        		
				jedisPoolConfig.setMaxTotal(Integer.valueOf(maxActive));
			}catch(Exception e){
			}
		}
		if(!MiscUtils.isEmpty(maxIdle)){
			try{        		
				jedisPoolConfig.setMaxIdle(Integer.valueOf(maxIdle));
			}catch(Exception e){
			}
		}
		if(!MiscUtils.isEmpty(maxWait)){
			try{        		
				jedisPoolConfig.setMaxWaitMillis(Long.valueOf(maxWait));
			}catch(Exception e){
			}
		}
		try{        		
			jedisPoolConfig.setTestOnBorrow(Boolean.valueOf(testOnBorrow));
		}catch(Exception e){
			jedisPoolConfig.setTestOnBorrow(false);
		}
		try{        		
			jedisPoolConfig.setTestOnReturn(Boolean.valueOf(testOnReturn));
		}catch(Exception e){
			jedisPoolConfig.setTestOnReturn(false);
		}
		try{
			jedisPool = new JedisPool(jedisPoolConfig, ip, Integer.valueOf(port), outTime, pass);
			return jedisPool;
		} catch(Exception e){
			return null;
		}
	}

	private static class JedisProxy implements MethodInterceptor{
		
		private Jedis jedis = null;
		private JedisUtils jedisUtils = null;
		private Enhancer enhancer = new Enhancer();	
		
		public JedisProxy(JedisUtils jedisUtils){
			enhancer.setSuperclass(Jedis.class);
			enhancer.setClassLoader(JedisBatchCallback.class.getClassLoader());
			enhancer.setInterfaces(new Class[]{JedisBatchCallback.class});
			enhancer.setCallback(this);
			jedis = (Jedis)enhancer.create();
			this.jedisUtils=jedisUtils;			
		}
		
		public Jedis getJedis(){
			return jedis;
		}
		
		@Override
		public Object intercept(Object arg0, Method method, Object[] parameters, MethodProxy proxy) throws Throwable {
			JedisPool pool = jedisUtils.getJedisPool();
			if(pool == null){
				return null;
			}			
			Jedis realJedis = null;
			Object retValue = null;
			try{
				realJedis = pool.getResource();
				if(method.getDeclaringClass()==JedisBatchCallback.class){
					if(parameters !=null && parameters.length ==1){
						JedisBatchOperation jedisBatchOperation = (JedisBatchOperation)parameters[0];
						if(jedisBatchOperation!=null){
							Pipeline pipeline = realJedis.pipelined();
							jedisBatchOperation.batchOperation(pipeline, realJedis);
						}
					}
				} else {					
					retValue = proxy.invoke(realJedis, parameters);
				}
			}finally{
				if(realJedis!=null){
					try{
						realJedis.close();
					}catch(Exception e){						
					}
				}
			}
			return retValue;
		}
	}
}
