package qingning.mq.server.imp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import qingning.common.entity.RequestEntity;
import qingning.common.util.CacheUtils;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.common.util.WeiXinUtil;
import qingning.db.common.mybatis.persistence.*;
import qingning.mq.persistence.mongo.MongoDB;
import qingning.mq.utils.CommonLocationUtils;
import qingning.server.AbstractMsgService;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.CommonReadOperation;
import redis.clients.jedis.Jedis;
import com.alibaba.fastjson.JSONObject;

public class LogServiceImpl extends AbstractMsgService {
	private static Logger log = LoggerFactory.getLogger(LogServiceImpl.class);
	
	@Autowired(required = true)
	private MongoDB mongoDB;
	@Autowired(required = true)
	private CommonLocationUtils  commonLocationUtils;
	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;
	@Autowired(required = true)
	private UserMapper userMapper;
		
    @SuppressWarnings("unchecked")
	@FunctionName("logLectureInfo")
    public void processLogLectureInfo(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
    	try{//输入：lecture_id,distributer_id,,user_id,room_id,course_id,record_time,record_date
    		Map<String,Object> lectureInfo = (Map<String,Object>)requestEntity.getParam();
    		if(MiscUtils.isEmpty(lectureInfo)) return;
    		String id = null;
    		String databaseName = null;
    		String collectionName = null;
    		if("0".equals(lectureInfo.get("status"))){//课程直播漏斗,课程详情页打开数量 user_id_course_id
    			int num = MiscUtils.convertStringToDBNo((String)lectureInfo.get("lecturer_id"),20);
    			databaseName = String.format(Constants.MONGODB_LECTURER_COURSE_DATABASE_FORMAT, num+"");
    			num = MiscUtils.convertStringToDBNo((String)lectureInfo.get("course_id"),100);
    			collectionName = String.format(Constants.MONGODB_LECTURER_COURSE_COLLECTION_FORMAT, num+"");
    			id=(String)lectureInfo.get("course_id")+"_"+(String)lectureInfo.get("user_id");
    		} else if("1".equals(lectureInfo.get("status"))){//直播分销漏斗,直播间分销链接点击数 distributer_id_user_id,room_id,rq_code
    			int num = MiscUtils.convertStringToDBNo((String)lectureInfo.get("distributer_id"),20);
    			databaseName = String.format(Constants.MONGODB_ROOM_DISTRIBUTER_DATABASE_FORMAT, num+"");
    			num = MiscUtils.convertStringToDBNo((String)lectureInfo.get("room_id"),100);
    			collectionName = String.format(Constants.MONGODB_ROOM_DISTRIBUTER_COLLECTION_FORMAT, num+"");
    			id=lectureInfo.get("distributer_id")+"_"+(String)lectureInfo.get("room_id")+"_"+(String)lectureInfo.get("user_id");
    		} else if("2".equals(lectureInfo.get("status"))){//课程分销分销漏斗,课程分销分销链接点击数 key：distributer_id_user_id_course_id
    			int num = MiscUtils.convertStringToDBNo((String)lectureInfo.get("distributer_id"),20);
    			databaseName = String.format(Constants.MONGODB_LECTURER_COURSE_DATABASE_FORMAT, num+"");
    			num = MiscUtils.convertStringToDBNo((String)lectureInfo.get("course_id"),100);
    			collectionName = String.format(Constants.MONGODB_COURSE_DISTRIBUTER_COLLECTION_FORMAT, num+"");
    			id=lectureInfo.get("distributer_id")+"_"+(String)lectureInfo.get("course_id")+"_"+(String)lectureInfo.get("user_id");
    		}
    		MongoClient mongoClient = mongoDB.getMongoClient();
    		MongoDatabase dataBase = mongoClient.getDatabase(databaseName);
    		MongoCollection<Document> collection = dataBase.getCollection(collectionName);
    		BasicDBObject basicDBObject = new BasicDBObject("_id",id);
    		List<Map<String,Object>> list =MongoDB.queryValue(collection, basicDBObject, 1);
    		if(MiscUtils.isEmpty(list)){
    			lectureInfo.put("_id", id);
    			Map<String,String> map = new HashMap<String,String>();
    			MiscUtils.converObjectMapToStringMap(lectureInfo, map);
    			MongoDB.insert(collection, map);
    		}
    	}catch(Exception e){
    		log.error(e.getMessage());
    	}
    }	
	
    @SuppressWarnings("unchecked")
	@FunctionName("logUserInfo")
    public void processLogUserInfo(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
    	try{    		
    		Map<String,Object> clientSideInfo = (Map<String,Object>)requestEntity.getParam();
    		if(MiscUtils.isEmpty(clientSideInfo)) return;
    		
    		preparedLoginUserInfoData(clientSideInfo, jedisUtils);
    		
    		Map<String,String> clientSideInfoStrMap = new HashMap<String,String>();
    		MiscUtils.converObjectMapToStringMap(clientSideInfo, clientSideInfoStrMap);
    		insertOrgLogDB(clientSideInfoStrMap); 
    		if("0".equals(clientSideInfo.get("status"))){
    			insertActiveDeviceDB(clientSideInfoStrMap);
    		} else if(!"2".equals(clientSideInfo.get("status"))){
    			insertUserDB(clientSideInfoStrMap,jedisUtils);
    		}
    		
    	}catch(Exception e){
    		log.error(e.getMessage());
    	}
    }
    
    private void preparedLoginUserInfoData(Map<String,Object> map, JedisUtils jedisUtils){
    	if("2".equals(map.get("status"))){
    		return;
    	}
    	
    	Jedis jedis = jedisUtils.getJedis();
    	String web_country=null;
    	String web_province=null;
    	String web_city=null;
    	if(!MiscUtils.isEmpty(map.get("web_openid"))){
    		int count = 0;
    		do{
    			try{
    				JSONObject jsonObject = WeiXinUtil.getUserByOpenid(
    						WeiXinUtil.getAccessToken(null, null, jedis).getToken(), (String) map.get("web_openid"));
    				map.put("subscribe", jsonObject.getString("subscribe"));
    				web_country = (String)map.get("country");
    				web_province = (String)map.get("province");
    				web_city = (String)map.get("city");
    				break;
    			} catch (Exception e){
    				++count;
    				try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {						
					}
    			}
    		} while(count<3);
    	}
    	
    	String longitude_str = (String)map.get("longitude");
    	String latitude_str = (String)map.get("latitude");
    	
    	double longitude = 0d;
    	double latitude = 0d;
    	try{
    		longitude = Double.parseDouble(longitude_str);
    		latitude = Double.parseDouble(latitude_str);
    	} catch(Exception e){
    		longitude_str = null;
    		latitude_str = null;
    	}
    	
    	String ip = (String)map.get("ip");

    	Map<String,String> loaction = commonLocationUtils.getLocationByIpFromMongoDB(ip);   
    	if(MiscUtils.isEmpty(loaction) && !MiscUtils.isEmpty(longitude) && !MiscUtils.isEmpty(latitude)){
    		loaction = commonLocationUtils.getLocationByGeographic(longitude,latitude);
    		if(!MiscUtils.isEmpty(loaction)){ 
    			if("1".equals(loaction.get(Constants.SYS_FIELD_COUNTRY_SHORT))){    		
	    			String coutry = jedisUtils.getConfigByKeyValue(loaction.get(Constants.SYS_FIELD_COUNTRY));
	    			if(MiscUtils.isEmpty(coutry)){
	    				loaction.put(Constants.SYS_FIELD_COUNTRY, coutry);
	    			} 
    			}
    			commonLocationUtils.saveLocationByIpIntoMongoDB(ip,loaction);
    		}
    	}
    	
    	if(MiscUtils.isEmpty(loaction) && !MiscUtils.isEmpty(ip)){
    		loaction = commonLocationUtils.getLocationByIp(ip);
    		if(!MiscUtils.isEmpty(loaction) && "1".equals(loaction.get(Constants.SYS_FIELD_COUNTRY_SHORT))){
    			String coutry = jedisUtils.getConfigByKeyValue(loaction.get(Constants.SYS_FIELD_COUNTRY));
    			if(MiscUtils.isEmpty(coutry)){
    				loaction.put(Constants.SYS_FIELD_COUNTRY, coutry);
    			}    			
    		}
    	}
    	
    	String phone_num = (String)map.get("phone_num");
    	if(MiscUtils.isEmpty(loaction) && !MiscUtils.isEmpty(phone_num)){    		
    		loaction = commonLocationUtils.getLocationByPhoneNumber(phone_num);
    		if(!MiscUtils.isEmpty(loaction) && "1".equals(loaction.get(Constants.SYS_FIELD_COUNTRY_SHORT))){
    			String coutry = jedisUtils.getConfigByKeyValue(loaction.get(Constants.SYS_FIELD_COUNTRY));
    			if(MiscUtils.isEmpty(coutry)){
    				loaction.put(Constants.SYS_FIELD_COUNTRY, coutry);
    			}    			
    		}
    	}
    	if(!MiscUtils.isEmpty(loaction)){
    		loaction.remove(Constants.SYS_FIELD_COUNTRY_SHORT);
    		for(String key: loaction.keySet()){
    			map.put(key, loaction.get(key));
    		}
		}
    }
    
    private void insertUserDB(Map<String,String> values,JedisUtils jedisUtils){
    	Jedis jedis = jedisUtils.getJedis();
    	String user_id = (String)values.get("user_id");
    	if(MiscUtils.isEmpty(user_id)){
    		return;
    	}
    	long record_time = 0;
    	try{
    		record_time = Long.parseLong(values.get("record_time").toString());
    	} catch(Exception e){
    		return;
    	}
    	Calendar cal = Calendar.getInstance();
    	cal.setTimeInMillis(record_time);
    	int date = cal.get(Calendar.DATE);
    	String year_month = cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1);
    	MongoClient mongoClient = mongoDB.getMongoClient();
    	MongoDatabase dataBase = mongoClient.getDatabase(Constants.MONGODB_USER_REGISTRY_DATABASE);
    	MongoCollection<Document> collection = dataBase.getCollection(String.format(Constants.MONGODB_USER_REGISTRY_COLLECTION_FORMAT,year_month));
    	BasicDBObject basicDBObject = new BasicDBObject("_id",user_id);
    	values.put("_id",user_id);
    	List<Map<String,Object>> list =MongoDB.queryValue(collection, basicDBObject, 1);
    	if(MiscUtils.isEmpty(list)){
    		MongoDB.insert(collection, values);
    		Map<String,Object> info = new HashMap<String,Object>();
    		info.put("user_id", user_id);
    		info.put("country", values.get("country"));
    		info.put("province", values.get("province"));
    		info.put("city", values.get("city"));
    		info.put("district", values.get("district"));			
    		loginInfoMapper.updateLoginInfo(info);

    		userMapper.updateUser(info);
    	} else if(!MiscUtils.isEmpty(values.get("subscribe")) && !MiscUtils.isEqual(values.get("old_subscribe"), values.get("subscribe"))){    		
    		Document doc=new Document();
    		Map<String,Object> lastValues = list.get(0);
    		for(String key:lastValues.keySet()){
    			doc.put(key, lastValues.get(key));
    		}
    		doc.put("subscribe", values.get("subscribe"));
    		collection.replaceOne(basicDBObject, doc);
    		Map<String,Object> info = new HashMap<String,Object>();
    		info.put("user_id", user_id);
    		info.put("subscribe", values.get("subscribe"));			
    		loginInfoMapper.updateLoginInfo(info);			
    	}
    	try {
    		Map<String,String> info = new HashMap<String,String>();
    		String country = values.get("country");
    		String province = values.get("province");
    		String city = values.get("city");
    		String district = values.get("district");
    		if(!MiscUtils.isEmpty(country)){
    			info.put("country", country);
    		}
    		if(!MiscUtils.isEmpty(province)){
    			info.put("province", province);
    		}
    		if(!MiscUtils.isEmpty(city)){
    			info.put("city", city);
    		}
    		if(!MiscUtils.isEmpty(district)){
    			info.put("district", district);
    		}
    		readUser(user_id, null, new CommonReadOperation(){
    			public Object invokeProcess(RequestEntity requestEntity) throws Exception {
    				Object result = null;
    				if(requestEntity != null){
    					result = null;
    				} else {
    					result = userMapper.findByUserId(user_id);
    				}
    				return result;
    			}

				@Override
				public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
					return null;
				}
			}, jedis);
    		Map<String,Object> query = new HashMap<String,Object>();
    		query.put("user_id", user_id);
    		String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, query);
    		log.debug("user_map "+info.toString());
			jedis.hmset(key, info);

    	} catch (Exception e) {
    		log.error("read user["+user_id+"]:"+e.getMessage());
    	}
    }
    
    private void insertActiveDeviceDB(Map<String,String> values){ 
    	if(MiscUtils.isEmpty(values.get("device_id"))){
    		return;
    	}
    	String device_id = (String)values.get("device_id");
    	int sum = MiscUtils.convertStringToDBNo(device_id,20);
		MongoClient mongoClient = mongoDB.getMongoClient();
		MongoDatabase dataBase = mongoClient.getDatabase(Constants.MONGODB_DEVICE_ACTIVE_DATABASE);
		
		MongoCollection<Document> collection = dataBase.getCollection(String.format(Constants.MONGODB_DEVICE_ACTIVE_COLLECTION_FORMAT, sum));
		
		BasicDBObject basicDBObject = new BasicDBObject("_id",device_id);
		List<Map<String,Object>> list =MongoDB.queryValue(collection, basicDBObject, 1);
		if(MiscUtils.isEmpty(list)){
			Map<String,String> curDeviceID  = new HashMap<String,String>();
			curDeviceID.put("_id",device_id);
			curDeviceID.put("create_time",String.valueOf(values.get("create_time")));
			MongoDB.insert(collection, curDeviceID);
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(System.currentTimeMillis());
			String year_month = cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1);			
			collection = dataBase.getCollection(String.format(Constants.MONGODB_DEVICE_ACTIVE_COLLECTION_DETAILS_FORMAT, year_month));
			values.put("_id",device_id);
			MongoDB.insert(collection, values);
		}		
    }
    
    private void insertOrgLogDB(Map<String,String> values){ 
		MongoClient mongoClient = mongoDB.getMongoClient();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		int date = cal.get(Calendar.DATE);

		String year_month = cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1) + "_" + (date >15 ? 0:1); 
		MongoDatabase dataBase = mongoClient.getDatabase(String.format(Constants.MONGODB_ORG_LOG_DB_FORMAT, year_month));
		MongoCollection<Document> collection = dataBase.getCollection(String.format(Constants.MONGODB_ORG_LOG_COLLECTION_FORMAT, date+""));
		MongoDB.insert(collection, values);
    }    
}
