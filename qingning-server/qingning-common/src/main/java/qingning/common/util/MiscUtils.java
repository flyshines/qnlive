package qingning.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.utils.StringUtils;

import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import qingning.server.rpc.CommonReadOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public final class MiscUtils {
	private MiscUtils(){};
	
	private static Map<String, String> configProperty = null;
	private static String configPropertyPath="classpath:application.properties";
	private static DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void setConfigPropertyPath(String configPropertyPath){
		MiscUtils.configPropertyPath=configPropertyPath;
	}
	public static boolean isEmptyString(String value){
		return value==null?true:(value.trim().length()<1?true:false);
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean isEmpty(Object value){
		boolean ret = value==null?true:false;
		if(!ret){
			if(value instanceof String){
				ret = isEmptyString((String)value);
			} else if(value instanceof Collection){
				Collection list = (Collection)value;
				ret = (list.size() < 1);				
			} else if(value instanceof Map){
				Map map = (Map) value;
				ret = map.isEmpty();
			}
		}
		return ret;
	}
	
	 public static long getUnixTimeStamp(Date date) {
	        return date.getTime() / 1000;
	 }
	 
	public static Object convertStringToObject(Object obj , String type){
		if(!isEmpty(obj) && !isEmpty(type)){
			try{
				if(!(obj instanceof String)){
					if(obj instanceof Date){
						return obj;
					} else if(!(obj instanceof Map) && !(obj instanceof Collection)){
						obj = String.valueOf(obj);
					}
				}
				
				if(Constants.SYSINT.equalsIgnoreCase(type)){
					obj = Integer.parseInt((String)obj);
				} else if(Constants.SYSLONG.equalsIgnoreCase(type)){
					obj = Long.parseLong((String)obj);
				} else if(Constants.SYSDOUBLE.equalsIgnoreCase(type)){
					obj = Double.parseDouble((String)obj);
				} else if(Constants.SYSDATE.equalsIgnoreCase(type)){
					obj = new Date(Long.parseLong((String)obj));
				}
			} catch(Exception e){
				
			}
		}
		return obj;
	}
	
	public static Map<String, String> convertPropertiesFileToMap(String path) throws Exception{
		InputStream input = null;
		Map<String,String> propertiesMap = null;
		try{
			if(path.toLowerCase().startsWith("classpath:")){
				String fileName = path.substring("classpath:".length());
				input = MiscUtils.class.getClassLoader().getResourceAsStream(fileName);
				if(input==null){
					path=MiscUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
					path=path.substring(0, path.lastIndexOf(File.separatorChar)+1)+fileName;
				}
			}
			if(input==null){
				input = new FileInputStream(path);
			}
			
			Properties properties = new Properties();
			properties.load(input);
			propertiesMap = new HashMap<String,String>();			
			for(String name : properties.stringPropertyNames()){
				propertiesMap.put(name, properties.getProperty(name));
			}			
		}finally{
			if(input!=null){
				input.close();
			}
		}
		return propertiesMap;
	}
	
	public static Object convertObject(Object obj){
		if(obj == null){
			return null;
		}
		if(StringUtils.isBlank(obj.toString())){
			return null;
		}else{
			return obj;
		}
	}
	
	public static String convertString(Object object){
		Object obj=object;
		if(obj == null){
			return "";
		} else if(obj instanceof Date){
			obj = ((Date)obj).getTime();
			return obj.toString();
		}
		String result = obj.toString().trim();
		if(StringUtils.isBlank(result)){
			return "";
		}   
		return result;
	}
	
	/**
	 * map中参数的值为null 时返回 false 有值时为true
	 * @param map
	 * @return
	 */
	public static Boolean compareMapIsNotBlank(Map<String, Object> map){
		if (map==null) {
			return false;
		}
		for(Entry<String, Object> entry:map.entrySet()){    
			if (entry.getKey().equals("page_count")||entry.getKey().equals("page_num")) {
				continue;
			} 
			Object obj=entry.getValue();
			if(obj != null){
				if(!StringUtils.isBlank(obj.toString())){
					return true;
				} 
			} 
		}    
		return false;
	}
	
	/**
	 * 将map中的"" 转为null
	 * @param map
	 */
	public static void mapConvertNull(Map<String, Object> map){
		if (map==null) {
		}
		for(Entry<String, Object> entry:map.entrySet()){    
			if (entry.getKey().equals("page_count")||entry.getKey().equals("page_num")) {
				continue;
			} 
			Object obj=entry.getValue();
			if(obj != null){
				if(StringUtils.isBlank(obj.toString())){
					map.put(entry.getKey(), null);
				} 
			} 
		}    
	}
	
	public static Date objectToDate(Object obj){
		if(obj == null){
			return null;
		}
		if(obj instanceof Date){
			return (Date)obj;
		}else{
			return new Date();
		}
		
	}

	public static String getUUId() {
		return UUID.randomUUID().toString().replace("-", "");
	}
		
	/**
	 * Generate the Random No. 
	 * @return
	 */
	public static String generateNormalNo(){
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());		
		long maxLen=10000000000000000L;		
		long currentTime = cal.getTimeInMillis()%maxLen;		
		String tmpStr = String.valueOf(currentTime);
		int len = 16 -tmpStr.length();
		if(len>0){
			maxLen=(long)Math.pow(10, len);
			long rand=0;
			try {
				rand=Math.abs(SecureRandom.getInstanceStrong().nextInt())%maxLen;		
			} catch (NoSuchAlgorithmException e) {			
			}
			tmpStr=tmpStr+String.format("%0"+len+"d", rand);			
		} else {
			long rand=0;
			try {
				rand=Math.abs(SecureRandom.getInstanceStrong().nextInt())%maxLen;		
			} catch (NoSuchAlgorithmException e) {			
			}
			tmpStr=String.format("%0"+16+"d", rand);
			
		}
		tmpStr=tmpStr.substring(0, 16);
		StringBuilder oddBuilder = new StringBuilder();
		StringBuilder evenbuilder = new StringBuilder();
		for(int i=0;i<16;++i){
			if(i%2==0){
				evenbuilder.append(tmpStr.substring(i,i+1));
			} else {
				oddBuilder.append(tmpStr.substring(i,i+1));
			}
		}
		return oddBuilder.reverse().toString()+evenbuilder.toString();
	}
	 
	public static String getConfigByKey(String key) {
		if(isEmptyString(key)){
			return null;
		}
		try{
			if(configProperty==null){
				configProperty= MiscUtils.convertPropertiesFileToMap(configPropertyPath);
			}
		} catch(Exception e){
			//TODO add log info
		}
		return configProperty.get(key);
	}
	
	/**
	 * 
	 * @param inList
	 * @return List<Map<String, Object>>
	 */
	public static List<Map<String, Object>> convertObjectForMap(List<Map<String, String>> inList){
		if(inList == null){
			return null;
		}
		List<Map<String,Object>> resData = new ArrayList<Map<String,Object>>();
		Map<String, Object> resMap = null;
 		for (Map<String, String> map : inList) {
 			resMap = new HashMap<String, Object>();
 			for(Map.Entry<String, String> entry:map.entrySet()){
 				resMap.put(entry.getKey(), entry.getValue());
 			}
 			resData.add(resMap);
		}
 		return resData;
	}
	/**
     * 元转换成分
     * @param amount
     * @return
     */
    public static String yuanToFen(BigDecimal amount){
    	BigDecimal bigDecimal=new BigDecimal(100);
		String totalAmount = String.valueOf(bigDecimal.multiply(amount).longValue());//以分为单位.
		return totalAmount;
    }
    
    /**
     * 格式化string为Date
     *
     * @param datestr
     * @return date
     */
    public static Date parseDate(String datestr) {
       if(StringUtils.isBlank(datestr)){
    	   return null;
       }
       Date date = null;
       try {
    	   date  = dateTimeFormat.parse(datestr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
       return date;
    }
	
    @SuppressWarnings("rawtypes")
	public static String getKeyOfCachedData(String keyTemplate, Map map){
    	if(isEmpty(keyTemplate) || isEmpty(map)){
    		return "";
    	}
    	String[] keySection = keyTemplate.split(":");
    	StringBuilder build = new StringBuilder();
    	for(int i=0; i < keySection.length; ++i){
    		String section=keySection[i].trim();
    		if(section.startsWith("{") && section.endsWith("}")){
    			section = convertString(map.get(section.substring(1, section.length()-1)));
    		}
    		if(i==0){
    			build.append(section);
    		} else {
    			build.append(":").append(section);
    		}
    	}
    	return build.toString();
    }

	public static void converObjectMapToStringMap(Map<String,Object> objectMap, Map<String,String> stringMap){

		for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
			if(entry.getValue() != null){
				if(entry.getValue() instanceof Date){
					stringMap.put(entry.getKey(), ((Date)entry.getValue()).getTime() + "");
				}else {
					stringMap.put(entry.getKey(), entry.getValue().toString());
				}
			}
		}
	}
}
