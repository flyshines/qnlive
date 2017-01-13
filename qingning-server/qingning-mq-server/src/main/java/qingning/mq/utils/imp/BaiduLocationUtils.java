package qingning.mq.utils.imp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import qingning.common.util.Constants;
import qingning.common.util.HttpTookit;
import qingning.common.util.MiscUtils;
import qingning.mq.utils.LocationUtils;

public class BaiduLocationUtils implements LocationUtils {
	private static Logger log = LoggerFactory.getLogger(BaiduLocationUtils.class);
	private String key = "UsRSw3nTcduUNpdN1I59kZwD2ZxB5QKf";
	private String ipFormat = "https://api.map.baidu.com/location/ip?ak=%s&coor=bd09ll&ip=%s";
	private String geographicFormat = "http://api.map.baidu.com/geocoder/v2/?ak=%s&location=%f,%f&output=json&pois=0";	
		
	private static final long maxRequest = 3000000;
	private static long curRequest = 0;
	private static final long maxRequestPerMinuter = 10000;
	private static long curRequestPerMinuter = 0;
	private long lastRequestTime = 0;
	private long lastRequestDate = 0;
	
	private void restTime(Calendar cal, int type){
		if(type == 0 || type ==2){
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.SECOND,0);
			lastRequestTime=cal.getTimeInMillis();
			curRequestPerMinuter = 1;
		} 
		if(type == 1 || type ==2){
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.SECOND,0);
			cal.set(Calendar.MILLISECOND,0);
			lastRequestDate=cal.getTimeInMillis();
			curRequest = 1;
		}
	}
	
	private synchronized boolean requestLimit(long time){
		boolean result = true;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		if(lastRequestTime == 0){
			restTime(cal, 2);
		} else {
			if(time-lastRequestTime < 60*1000 && curRequestPerMinuter> maxRequestPerMinuter){
				return false;
			}
			
			if(time-lastRequestDate <24* 60* 60*1000 && curRequest > maxRequest){
				return false;
			}
			++curRequestPerMinuter;
			++curRequest;
			if(time-lastRequestTime>=60*1000){
				restTime(cal, 0);
			}
			if(time-lastRequestDate <24* 60* 60*1000){
				restTime(cal, 1);
			}
		}
		
		return result;
	}
	
	public void setAccessKey(String accessKey){
		if(!MiscUtils.isEmpty(accessKey)){
			key=accessKey;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getLocationByIp(String ip) {
		Map<String,String> result = null;
		if(MiscUtils.isEmpty(ip) || !requestLimit(System.currentTimeMillis())){
			return result;
		}
		try{
			String ipPosition =  String.format(ipFormat,key, ip);
			String resultStr = MiscUtils.convertUnicodeToChinese(HttpTookit.doGet(ipPosition));
			if(!MiscUtils.isEmpty(resultStr)){
				Map<String, Object> map = JSON.parseObject(resultStr, HashMap.class);	
				if(!MiscUtils.isEmpty(map) && "0".equals(String.valueOf(map.get("status")))){
					map = (Map<String, Object>)map.get("content");
					if(!MiscUtils.isEmpty(map)){
						map = (Map<String, Object>)map.get("point");
						if(!MiscUtils.isEmpty(map)){
							return getLocationByGeographic(Double.parseDouble(map.get("x").toString()),Double.parseDouble(map.get("y").toString()));
						}						
					}
				}
			}
		}catch(Exception e){
			log.info(e.getMessage());
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getLocationByGeographic(double longitude, double latitude) {
		String geographic = String.format(geographicFormat, key, latitude, longitude);
		Map<String,String> result = null;
		if(!requestLimit(System.currentTimeMillis())){
			return result;
		}
		try{
			String resultStr = MiscUtils.convertUnicodeToChinese(HttpTookit.doGet(geographic));			
			if(!MiscUtils.isEmpty(resultStr)){
				Map<String, Object> map = JSON.parseObject(resultStr, HashMap.class);				
								
				if(!MiscUtils.isEmpty(map) && "0".equals(String.valueOf(map.get("status")))){
					map = (Map<String, Object>) map.get("result");
					if(!MiscUtils.isEmpty(map)){
						map = (Map<String, Object>) map.get("addressComponent");
						if(!MiscUtils.isEmpty(map)){
							result = new HashMap<String,String>();
							MiscUtils.addValueToMap(Constants.SYS_FIELD_COUNTRY, map.get("country"), result);
							MiscUtils.addValueToMap(Constants.SYS_FIELD_PROVINCE, map.get("province"), result);
							MiscUtils.addValueToMap(Constants.SYS_FIELD_CITY, map.get("city"), result);
							MiscUtils.addValueToMap(Constants.SYS_FIELD_DISTRICT, map.get("district"), result);							
						}
					}
				}
			}
			if(MiscUtils.isEmpty(result)){
				result=null;
			}
		}catch(Exception e){
			log.info(e.getMessage());
		}
		return result;
	}

	@Override
	public Map<String, String> getLocationByPhoneNumber(String phoneNumber) {		
		return null;
	}
}
