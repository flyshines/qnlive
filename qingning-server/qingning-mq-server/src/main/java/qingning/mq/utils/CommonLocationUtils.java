package qingning.mq.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.mq.persistence.mongo.MongoDB;

public class CommonLocationUtils implements LocationUtils {
	private List<LocationUtils> locationUtilsList = null;
	@Autowired(required = true)
	private MongoDB mongoDB;
	private MongoDatabase dataBase=null;
	
	private MongoDatabase getDataBase(){
		if(dataBase==null){
			MongoClient mongoClient = mongoDB.getMongoClient();			
			dataBase = mongoClient.getDatabase(Constants.MONGODB_ADDRESS_DATABASE);
		}
		return dataBase;
	}
	
	public void setLocationUtilsList(List<LocationUtils> locationUtilsList) {
		this.locationUtilsList = locationUtilsList;
	}
	
	public Map<String, String> getLocationByIpFromMongoDB(String ip){
		Map<String, String > location = null;
		MongoDatabase dataBase = this.getDataBase();
		int sum = MiscUtils.convertStringToDBNo(ip,20);
		String collName = String.format(Constants.MONGODB_CITY_IP_COLL_FORMAT,sum);
		MongoCollection<Document> collection = dataBase.getCollection(collName);
		List<Map<String,Object>> list =MongoDB.queryValue(collection,new BasicDBObject("_id",ip),1);
		if(!MiscUtils.isEmpty(list)){			
			location = new HashMap<String,String>();
			MiscUtils.converObjectMapToStringMap(list.get(0), location);
			location.remove("_id");
		}
		return location;
	}
	
	public void saveLocationByIpIntoMongoDB(String ip, Map<String, String> location){
		MongoDatabase dataBase = this.getDataBase();
		int sum = MiscUtils.convertStringToDBNo(ip,20);
		String collName = String.format(Constants.MONGODB_CITY_IP_COLL_FORMAT,sum);
		MongoCollection<Document> collection = dataBase.getCollection(collName);
		List<Map<String,Object>> list =MongoDB.queryValue(collection,new BasicDBObject("_id",ip),1);
		if(MiscUtils.isEmpty(list)){	
			location.put("_id", ip);
			MongoDB.insert(collection,location);
			location.remove("_id");
		}
	}
	
	@Override
	public Map<String, String> getLocationByIp(String ip) {
		Map<String, String > location = getLocationByIpFromMongoDB(ip);
		if(MiscUtils.isEmpty(location)){
			MongoDatabase dataBase = this.getDataBase();
			int sum = MiscUtils.convertStringToDBNo(ip,20);
			String collName = String.format(Constants.MONGODB_CITY_IP_COLL_FORMAT,sum);
			MongoCollection<Document> collection = dataBase.getCollection(collName);
			for(LocationUtils utils:locationUtilsList){
				location = utils.getLocationByIp(ip);
				if(!MiscUtils.isEmpty(location)){
					break;
				}
			}
			if(!MiscUtils.isEmpty(location)){
				location.put("_id", ip);
				MongoDB.insert(collection,location);
			}
		}
		return location;
	}

	@Override
	public Map<String, String> getLocationByGeographic(double longitude, double latitude) {
		Map<String, String > location = null;
		//TODO		
		if(MiscUtils.isEmpty(location)){
			for(LocationUtils utils:locationUtilsList){
				location = utils.getLocationByGeographic(longitude,latitude);
				if(!MiscUtils.isEmpty(location)){
					break;
				}
			}
		}		
		return location;
	}

	@Override
	public Map<String, String> getLocationByPhoneNumber(String phoneNumber) {
		Map<String, String > location = null;
		//TODO		
		if(MiscUtils.isEmpty(location)){
			for(LocationUtils utils:locationUtilsList){
				location = utils.getLocationByPhoneNumber(phoneNumber);
				if(!MiscUtils.isEmpty(location)){
					break;
				}
			}
		}		
		return location;
	}

}
