package qingning.mq.utils;

import java.util.Map;

public interface LocationUtils {
	Map<String,String> getLocationByIp(String ip);
	Map<String,String> getLocationByGeographic(double longitude, double latitude);
	Map<String,String> getLocationByPhoneNumber(String phoneNumber);
}
