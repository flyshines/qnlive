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

public class LbsAmpsLocation implements LocationUtils {
	private String key = "d0cde1c287406074e0cb57a4a9d14d4c";
	private static Logger log = LoggerFactory.getLogger(LbsAmpsLocation.class);	
	private String ipFormat = "http://restapi.amap.com/v3/ip?key=%s&ip=%s&output=json";
	private String geographicFormat = "http://restapi.amap.com/v3/geocode/regeo?key=%s&location=%f,%f&output=json&pois=0";
	private static final long maxRequest = 4000000;
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
				if(!MiscUtils.isEmpty(map) && "1".equals(String.valueOf(map.get("status")))){
					String rectangle = (String)map.get("rectangle");
					if(!MiscUtils.isEmpty(rectangle)){
						String[] rectangleArr = rectangle.split(";");
						if(rectangleArr.length>1){
							String[] topStr = rectangleArr[0].split(",");
							String[] buttonStr = rectangleArr[1].split(",");
							double longitude = (Double.parseDouble(topStr[0]) + Double.parseDouble(buttonStr[0]))/2;
							double latitude = (Double.parseDouble(topStr[1]) + Double.parseDouble(buttonStr[1]))/2;
							result = this.getLocationByGeographic(longitude, latitude);
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
		Map<String,String> result = null;
		if(!requestLimit(System.currentTimeMillis())){
			return result;
		}
		try{
			String ipPosition =  String.format(geographicFormat,key, longitude,latitude);

			String resultStr = MiscUtils.convertUnicodeToChinese(HttpTookit.doGet(ipPosition));
			if(!MiscUtils.isEmpty(resultStr)){
				Map<String, Object> map = JSON.parseObject(resultStr, HashMap.class);
				if(!MiscUtils.isEmpty(map) && "1".equals(String.valueOf(map.get("status")))){
					map = (Map<String, Object>) map.get("regeocode");
					if(!MiscUtils.isEmpty(map)){
						map = (Map<String, Object>) map.get("addressComponent");
						if(!MiscUtils.isEmpty(map)){
							result = new HashMap<String,String>();
							MiscUtils.addValueToMap(Constants.SYS_FIELD_COUNTRY, map.get("country"), result);
							MiscUtils.addValueToMap(Constants.SYS_FIELD_PROVINCE, map.get("province"), result);
							if(MiscUtils.isEmpty(map.get("city")) && !MiscUtils.isEmpty(map.get("province"))){
								MiscUtils.addValueToMap(Constants.SYS_FIELD_CITY, map.get("province"), result);
							} else {
								MiscUtils.addValueToMap(Constants.SYS_FIELD_CITY, map.get("city"), result);
							}							
							MiscUtils.addValueToMap(Constants.SYS_FIELD_DISTRICT, map.get("district"), result);							
						}
					}
				}
				
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
