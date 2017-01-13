package qingning.mq.server.imp;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.context.ApplicationContext;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.mq.persistence.mongo.MongoDB;
import qingning.mq.persistence.mybatis.RegisterInfoInfoMapper;
import qingning.server.AbstractMsgService;

public class RegisterInfoServerImpl extends AbstractMsgService {
	private MongoDB mongoDB;
	private RegisterInfoInfoMapper registerInfoInfoMapper;
	private final static String[] keyFieldNames={
			 "country","province","city","district","plateform","device_model","device_oem",
			 "screen_size","os_version","download_channel", "version","net_status","network_operators",
			 "subscribe","gender"
	};
	public void setMongoDB(MongoDB mongoDB){
		this.mongoDB = mongoDB;
	}
	
	public void setRegisterInfoInfoMapper(RegisterInfoInfoMapper registerInfoInfoMapper){
		this.registerInfoInfoMapper = registerInfoInfoMapper;
	}
	
    @Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
    	MongoClient mongoClient = mongoDB.getMongoClient();
    	MongoDatabase dataBase = mongoClient.getDatabase(Constants.MONGODB_USER_REGISTRY_DATABASE);    	    	
    	long time = MiscUtils.getDate(System.currentTimeMillis());    	
    	Calendar cal = Calendar.getInstance();
    	Date currentDate = cal.getTime();
    	cal.setTimeInMillis(time);    	
    	cal.add(Calendar.DAY_OF_MONTH, -1);
    	Date recordDate = cal.getTime();
		String year_month = cal.get(Calendar.YEAR)+"_"+(cal.get(Calendar.MONTH)+1);			
		MongoCollection<Document> collection =dataBase.getCollection(String.format(Constants.MONGODB_USER_REGISTRY_COLLECTION_FORMAT, year_month));
		
		List<Map<String,Object>> list = MongoDB.queryValue(collection, new BasicDBObject("create_date",cal.getTimeInMillis()), -1);
		
		if(MiscUtils.isEmpty(list)){
			return;
		}
    	Map<String,Map<String,Object>> map = new HashMap<String,Map<String,Object>>();
    	for(Map<String,Object> value:list){
    		StringBuilder keyBuilder = new StringBuilder(); 
    		for(int i=0;i<keyFieldNames.length;++i){
    			keyBuilder.append(MiscUtils.convertString(value.get(keyFieldNames[i]))).append(":");
    		}
    		String key = keyBuilder.toString();
    		long count = 1;
    		Map<String,Object> calValue = value;
    		if(!map.containsKey(key)){
    			map.put(key, value);
    			value.put("create_time", currentDate);
    			value.put("record_date", recordDate);
    			value.put("info_id", MiscUtils.getUUId());
    		} else {
    			calValue=map.get(key);
    			count = (Long)calValue.get("info_num") +1;
    		}
    		calValue.put("info_num", count);
    	}
    	
    	//TODO change to list
    	for(Map<String,Object>value : map.values()){
    		registerInfoInfoMapper.insert(value);
    	}
    }
}
