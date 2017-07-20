package qingning.common.util;
 
import com.alibaba.dubbo.common.utils.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import qingning.common.entity.QNLiveException;
 
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
public final class MiscUtils {
    private MiscUtils(){};
    
    private static Map<String, String> configProperty = null;
    private static String configPropertyPath="classpath:application.properties";
    private static DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static DateFormat dateTimeFormatWinxin = new SimpleDateFormat("yyyyMMddHHmmss");
 
    private static Pattern emojiPattern = Pattern.compile("([\\x{10000}-\\x{10ffff}\ud800-\udfff])");
    private static Pattern emojiRecoverPattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
    
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
    
    public static boolean isEqual(Object value1, Object value2){
        boolean ret =false;
        if(value1==value2){
            ret = true;
        } else if(value1!= null && value2 !=null ){
            if(value1.equals(value2)){
                ret = true;
            }
        }
        
        return ret;
    }
    
     public static long getUnixTimeStamp(Date date) {
            return date.getTime() / 1000;
     }
     
    public static Object convertStringToObject(Object obj , String type,String fieldName, boolean adjust) throws Exception{
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
                    if(adjust){
                        BigDecimal bigDecimal = new BigDecimal((String)obj);
                        bigDecimal = bigDecimal.multiply(BigDecimal.valueOf(100));
                        bigDecimal.setScale(0, BigDecimal.ROUND_HALF_UP);
                        obj = bigDecimal.longValue();
                    } else {
                        obj = Double.parseDouble((String)obj);
                    }                    
                } else if(Constants.SYSDATE.equalsIgnoreCase(type)){
                    obj = new Date(Long.parseLong((String)obj));
                }
            } catch(Exception e){
                throw new QNLiveException("000101",fieldName);
            }
        } else if(isEmpty(obj) && !isEmpty(type)){
            if(!Constants.SYSSTR.equalsIgnoreCase(type)){
                obj=null;
            }
        }
        return obj;
    }
 
    public static long convertObjectToLong(Object value){
        long ret = 0l;
        try{
            if(MiscUtils.isEmpty(value)){
                ret = 0l;
            } else {
                if(value instanceof Long){
                    ret = (Long)value;                    
                } else {
                    if(value instanceof Date){
                        value = ((Date)value).getTime();
                    }
                    BigDecimal bigDecimal = new BigDecimal(value.toString());
                    bigDecimal.setScale(BigDecimal.ROUND_HALF_UP);
                    ret = bigDecimal.longValue();
                }
            }
            
        }catch(Exception e){
            ret= 0l;
        }
        return ret;
    }
    
    public static double convertObjectToDouble(Object value){
        return convertObjectToDouble(value, false);
    }
 
    public static double convertObjectToDouble(Object value, boolean adjust){
        double ret = 0d;        
        try{
            if(MiscUtils.isEmpty(value)){
                ret = 0d;
            } else {
                if(value instanceof Double){
                    ret = (Double)value;                    
                } else {
                    if(value instanceof Date){
                        value = ((Date)value).getTime();
                    }
                    if(adjust){
                        BigDecimal bigDecimal = new BigDecimal(value.toString());                        
                        bigDecimal = bigDecimal.divide(BigDecimal.valueOf(100L), 2, BigDecimal.ROUND_HALF_UP);
                        ret = bigDecimal.doubleValue();
                    } else {
                        ret = Double.parseDouble(value.toString());
                    }
                }
            }
            
        }catch(Exception e){
            ret= 0d;
        }
        return ret;
    }
    
    public static Object convertObjToObject(Object obj , String type,String fieldName, Object defaultValue) throws Exception{
        return convertObjToObject(obj , type, fieldName, defaultValue, false);
    }
    
    public static Object convertObjToObject(Object obj , String type,String fieldName, Object defaultValue, boolean adjust) throws Exception{
        try{
            if(MiscUtils.isEmpty(obj)){
                obj = defaultValue;
            }
            
            if(!MiscUtils.isEmpty(obj)){
                if(Constants.SYSINT.equalsIgnoreCase(type)){
                    if(!(obj instanceof Integer)){
                        obj = Integer.parseInt(obj.toString());
                    }            
                } else if(Constants.SYSLONG.equalsIgnoreCase(type)){
                    if(!(obj instanceof Long)){
                        if(obj instanceof Date){
                            obj = ((Date)obj).getTime();
                        } else {
                            obj = Long.parseLong(obj.toString());
                        }                
                    }
                } else if(Constants.SYSDOUBLE.equalsIgnoreCase(type)){
                    if(!(obj instanceof Double)){
                        if(obj instanceof Date){
                            Long timeStamp = ((Date)obj).getTime();
                            obj = timeStamp.toString();
                        }
                    }                    
                    if(adjust){
                        BigDecimal bigDecimal = new BigDecimal(obj.toString());                        
                        bigDecimal = bigDecimal.divide(BigDecimal.valueOf(100l), 2, BigDecimal.ROUND_HALF_UP);
                        obj = bigDecimal.doubleValue();
                    } else {
                        obj = Double.parseDouble(obj.toString());
                    }
                } else if(!(obj instanceof Map) && !(obj instanceof Collection)){
                    if(!(obj instanceof String)){
                        obj=obj.toString();
                    }
                }    
            }
            
            if(MiscUtils.isEmpty(obj)){
                if(Constants.SYSMAP.equals(type)){
                    obj=new HashMap<Object,Object>();
                } else if(Constants.SYSLIST.equals(type)){
                    obj=new ArrayList<Object>();
                } else {
                    obj="";
                }
            }
        } catch (Exception e){
            throw new QNLiveException("000102",fieldName);
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
        }catch (IOException e){

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
        return getConfigKey("server.number")+UUID.randomUUID().toString().replace("-", "");
        //return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getWeiXinId() {
        //TODO
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
 
    public static String getConfigKey(String key) {
        String value="";
        if(isEmptyString(key)){
            return null;
        }
        try{
            if(configProperty==null){
                configProperty= MiscUtils.convertPropertiesFileToMap(configPropertyPath);
            }
            value = configProperty.get(key);
        } catch(Exception e){
            //TODO add log info
        }
        return value;
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
    
    public static String convertDateToString(Date date){
    	if(date != null){
    		return dateTimeFormat.format(date);
    	} else {
    		return "";
    	}
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
        if(MiscUtils.isEmpty(objectMap)){
            return;
        }
        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
            if(entry.getValue() != null){
                if(entry.getValue() instanceof Date){
                    stringMap.put(entry.getKey(), ((Date)entry.getValue()).getTime() + "");
/*                }else if(entry.getValue() instanceof Double){
                    BigDecimal doubleNum = new BigDecimal((Double)entry.getValue());
                    stringMap.put(entry.getKey(), doubleNum.multiply(new BigDecimal(100)).longValue()+"");*/
                } else {
                    stringMap.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
    }
    
    public static String specialCharReplace(String value){
        if(isEmpty(value)){
            return value;
        }
        String[] specailChar={"<",   "&",    "\"",    ">",   " "};
        String[] specailCode={"&lt;","&amp;","&quot;","&gt;","&nbsp;"};
        for(int i=0; i < specailChar.length; ++i){
            value=value.replaceAll(specailCode[i], specailChar[i]);
        }
        return value;
    }
 
 
    /**
     * 转换课程状态
     * @param currentTime
     * @param courseInfoMap
     */
    public static void courseTranferState(long currentTime, Map<String, String> courseInfoMap) {
        //如果课程状不为结束，则判断其开始时间是否大于当前时间，如果大于当前时间，则修改其状态为直播中
        if(! courseInfoMap.get("status").equals("2")&& !courseInfoMap.get("status").equals("5")){
            long courseStartTime = Long.parseLong(courseInfoMap.get("start_time"));
            if(currentTime > courseStartTime){
                courseInfoMap.put("status", "4");
            }
        }
    }

    /**
     * 转换课程状态
     * @param currentTime
     * @param courseInfoMap
     */
    public static void courseTranferState(long currentTime,Map<String, Object> courseInfoMap,Long start_time) {
        //如果课程状不为结束，则判断其开始时间是否大于当前时间，如果大于当前时间，则修改其状态为直播中
        if(! courseInfoMap.get("status").equals("2")&& !courseInfoMap.get("status").equals("5")){
            if(currentTime > start_time){
                courseInfoMap.put("status", "4");
            }
        }
    }

 
    public static Date getEndTimeOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date end = calendar.getTime();
        return end;
    }
 
    public static Date getEndDateOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date end = calendar.getTime();
        return end;
    }
 
    public static String getIpAddr(HttpServletRequest request){
        String ip = request.getHeader ("X-Real-IP");
        if (!org.apache.commons.lang.StringUtils.isBlank(ip) && !"unknown".equalsIgnoreCase (ip)) { return ip; }
        ip = request.getHeader ("X-Forwarded-For");
        if (!org.apache.commons.lang.StringUtils.isBlank(ip) && !"unknown".equalsIgnoreCase (ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP。
            int index = ip.indexOf (',');
            if (index != -1) {
                return ip.substring (0, index);
            } else {
                return ip;
            }
        } else {
            return request.getRemoteAddr ();
        }
    }
 
    /**
     * 从inputstream 取数据。
     * @param is
     * @return
     * @throws Exception
     */
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            is.close();
            reader.close();
        }
        return sb.toString();
    }
 
 
    /**
     * Xml To Map
     * @param resultData
     * @return
     * @throws org.dom4j.DocumentException
     */
    public static SortedMap<String,String> requestToMap(String resultData) throws DocumentException {
        SortedMap<String,String> map = new TreeMap<String, String>();
 
        Document document = DocumentHelper.parseText(resultData);
        // 得到xml根元素
        Element root = document.getRootElement();
        // 得到根元素的所有子节点
        List<Element> elementList = root.elements();
        // 遍历所有子节点
        for (Element e : elementList){
            map.put(e.getName().toLowerCase(), e.getText());
        }
        return map;
    }
 
    public static Date parseDateWinxin(String realPayTimeString) {
        if(com.alibaba.dubbo.common.utils.StringUtils.isBlank(realPayTimeString)){
            return null;
        }
        Date date = null;
        try {
            date  = dateTimeFormatWinxin.parse(realPayTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
 
    public static String parseDateToFotmatString(Date date, String fotmatString) {
        if(date == null || MiscUtils.isEmpty(fotmatString)){
            return null;
        }else {
            SimpleDateFormat sdf = new SimpleDateFormat(fotmatString);
            return sdf.format(date);
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void addValueToMap(Object key, Object value, Map map){
        if(map ==null) return;
        if(MiscUtils.isEmpty(value)){
            return;
        }
        map.put(key, value);
    }
    
    public static String convertUnicodeToChinese(String value){
        if(isEmpty(value) || value.indexOf("\\u") == -1){
            return value;
        }
        
        StringBuilder chinese = new StringBuilder();  
        int i = -1;  
        int pos = 0;  
          
        while((i=value.indexOf("\\u", pos)) != -1){  
            chinese.append(value.substring(pos, i));  
            if(i+5 < value.length()){  
                pos = i+6;  
                chinese.append((char)Integer.parseInt(value.substring(i+2, i+6), 16));  
            }  
        }
        if(pos>0 && pos < value.length()){
            chinese.append(value.substring(pos));
        }
        return chinese.toString();  
    }
    
    public static int convertStringToDBNo(String value, int number){
        if(isEmpty(value) || number<1){
            return 0;
        }
        int length = value.length();
        int sum = 0;
        for(int i = 0; i<length;++i){
            sum+= value.charAt(i);
        }
        return sum % number;
    }
    
    public static long getDate(long time){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal.getTimeInMillis();
    }
    
    //0:公开课程 1:加密课程 2:收费课程
    public static String convertCourseTypeToContent(String course_type) {
        if(course_type.equals("0")){
            return "公开课程";
        }else if(course_type.equals("1")){
            return "加密课程";
        }else if(course_type.equals("2")){
            return "收费课程";
        }else {
            return null;
        }
    }

    /**
     * 生成[min, max]之间的随机数
     * @param min
     * @param max
     * @return
     */
    public static int getRandomIntNum(int min, int max){
        Random random = new Random();
        int resultNum = random.nextInt(max)%(max-min+1) + min;
        return resultNum;
    }
    public static long convertInfoToPostion(long timeInMillis,long pos){
    	long postion = (timeInMillis-1487034415000L)/1000;
    	return postion*Constants.SEQUENCE+pos;
    }
    
    /**
     * 未来时间-指定时间：实现指定时间越大，返回值越小
     * @param timeInMillis 指定时间
     * @return
     */
    public static long convertLongByDesc(long timeInMillis){
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	long futureTime = 0;
		try {
			futureTime = sdf.parse("2100-01-01 00:00:00").getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}	
		//设定一个未来时间，用来做减法实现倒序排列
    	long postion = futureTime - timeInMillis;	//未来时间-指定时间：实现指定时间越大，返回值越小
    	return postion;
    }
    
	/** 
	 * @Description 将字符串中的emoji表情转换成可以在utf-8字符集数据库中保存的格式（表情占4个字节，需要utf8mb4字符集） 
	 * @param str 待转换字符串 
	 * @return 转换后字符串  
	 */  
	public static String emojiConvertToNormalString(String str) {
		if(MiscUtils.isEmpty(str)){
			return "";
		}
	    Matcher matcher = emojiPattern.matcher(str);  
	    StringBuffer sb = new StringBuffer();  
	    while(matcher.find()) {  
	        try {  
	            matcher.appendReplacement(sb,"[["+ URLEncoder.encode(matcher.group(1),"UTF-8") + "]]");  
	        } catch(UnsupportedEncodingException e) {
	        	matcher.appendReplacement(sb,matcher.group(1));
	        }  
	    }  
	    matcher.appendTail(sb);
	    return sb.toString();  
	}  
	  
	/** 
	 * @Description 还原utf8数据库中保存的含转换后emoji表情的字符串 
	 * @param str 转换后的字符串 
	 * @return 转换前的字符串 
	 */  
	public static String RecoveryEmoji(String str) {  
		if(MiscUtils.isEmpty(str)){
			return "";
		}
	    String patternString = "\\[\\[(.*?)\\]\\]";  
	  
	    Pattern pattern = Pattern.compile(patternString);  
	    Matcher matcher = pattern.matcher(str);  
	  
	    StringBuffer sb = new StringBuffer();  
	    while(matcher.find()) {  
	        try {  
	        	String emoji = URLDecoder.decode(matcher.group(1), "UTF-8");
	        	
	        	Matcher detailsMatch = emojiPattern.matcher(emoji);
	        	boolean isEmoji = detailsMatch.find();
	        	if(isEmoji && detailsMatch.find()){
	        		isEmoji=false;
	        	}	        	
	        	if(isEmoji){
	        		matcher.appendReplacement(sb, emoji);
	        	} else {
	        		matcher.appendReplacement(sb, matcher.group(1));
	        	}
	              
	        } catch(UnsupportedEncodingException e) {  
	        	matcher.appendReplacement(sb,matcher.group(1));
	        }  
	    }  
	    matcher.appendTail(sb);
	    return sb.toString();  
	}
	
	public static boolean isTheSameDate(Date date1, Date date2){		
		boolean result = false;
		if(date1 != null && date2 != null){
			Calendar cal1 = Calendar.getInstance();			
			Calendar cal2 = Calendar.getInstance();
			if(cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && 
					cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) &&
					cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)){
				result = true;
			}
			
		}
		return result;
	}

    private static String CONFIG_PROPERTY_PATH="classpath:application.properties";//共有的配置文件路径
    private static String CONFIG_PROPERTY_APP_PATH="classpath:appservice.properties";//接入几个app
    private static  Map<String, Map<String, String>> APP_SERVICE_MAP = null;//存入的map
    private static String[] APP_NAME = null;

    public static String[] getAppName() throws Exception {
        if(isEmpty(APP_NAME)){
            Map<String, String> appMap = convertPropertiesFileToMap(CONFIG_PROPERTY_APP_PATH);//先读取app配置 介入几个app
            String app_service = appMap.get("app_service").toString();//获取总共接入几个app
            APP_NAME = app_service.split(",");//切分
        }
        return APP_NAME;
    }

    /**
     * 解析接入app 的配置文件 读取app相关的配置文件和共有配置文件
     * @return
     * @throws Exception
     */
    private static Map<String, Map<String, String>> convertAppPropertiesFileToMap() throws Exception {
        Map<String, String> appMap = convertPropertiesFileToMap(CONFIG_PROPERTY_APP_PATH);//先读取app配置 介入几个app
        String app_service = appMap.get("app_service").toString();//获取总共接入几个app
        APP_NAME = app_service.split(",");//切分

        Map<String, String> configPropertyMap =  convertPropertiesFileToMap(CONFIG_PROPERTY_PATH);//共有配置
        Map<String, Map<String, String>> propertiesMap = new HashMap<>();//返回的参数
        for(String appName : APP_NAME){
            String path = appMap.get(appName+"_application").toString();//app配置文件
            Map<String, String> appConfigMap = convertPropertiesFileToMap(path);//读取app的配置
            //规避配置文件不存在
            if(appConfigMap!=null)
                appConfigMap.putAll(configPropertyMap);//加入共有配置
            propertiesMap.put(appName,appConfigMap);
        }
        return propertiesMap;
    }

    public static String getConfigByKey(String key,String appName) {
        String value="";
        if(isEmptyString(key) || isEmptyString(appName) ){
            return null;
        }
        try{
            if(APP_SERVICE_MAP==null){
                APP_SERVICE_MAP= MiscUtils.convertAppPropertiesFileToMap();
            }
            value = APP_SERVICE_MAP.get(appName).get(key);
        } catch(Exception e){
        }
        return value;
    }

    /**
     * 根据appid 获取appname
     * @param appId
     * @return
     */
    public static String getAppNameByAppid(String appId){
        for (Map.Entry<String,Map<String,String>> entry : APP_SERVICE_MAP.entrySet()) {
            if(entry.getValue().get(Constants.APPID).equals(appId) || entry.getValue().get(Constants.APP_APP_ID).equals(appId)){
                return entry.getKey();
            }
        }
        return null;
    }


    /**
     * 判断是否存在appname
     * @param appName
     * @return
     */
    public static boolean isAppName(String appName){
        return APP_SERVICE_MAP.containsKey(appName);
    }


}
