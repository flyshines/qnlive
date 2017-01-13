package qingning.mq.persistence.mongo;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import qingning.common.util.MiscUtils;

public class MongoDB {
	private String ip;
	private int port;
	private int maxConnection;
	private int outTime;
	private int maxWait;
	private int maxBlockThreads;
	private int maxConnIdleTime;
	private int maxConnLifeTime;
	private int socketOutTime;
	private boolean socketKeepAlive;
	
	private MongoClient mongoClient = null;
	
	private int convertStringToInt(String value){
		int ret = 0;
		try{
			ret = Integer.parseInt(value);
		}catch(Exception e){
			ret = 0;
		}
		return ret;
	}
	
	public static List<Map<String,Object>> queryValue(MongoCollection<Document> collection, Bson bson, int limit){
		FindIterable<Document> documentIterator = collection.find(bson);
		MongoCursor<Document> cursor = documentIterator.iterator();
		int count = 0;
		List<Map<String,Object>> list = new LinkedList<Map<String,Object>>();
		while(cursor.hasNext()){
			if(limit >0 && count> limit){
				break;
			} else {
				++count;
			}
			Document document = cursor.next();
			if(!document.isEmpty()){
				Map<String, Object> values = new HashMap<String,Object>();
				list.add(values);
				for(String key:document.keySet()){
					values.put(key, document.get(key));
				}
			}
		}
		return list;
	}
	
	public static  void insert(MongoCollection<Document> collection, Map<String,String> value){
		if(!MiscUtils.isEmpty(value)){
			Document doc=new Document();
			doc.putAll(value);
			collection.insertOne(doc);
		}
	}
	
	public MongoClient getMongoClient(){
		if(mongoClient==null){
			MongoClientOptions.Builder buide = new MongoClientOptions.Builder();
	        buide.connectionsPerHost(this.maxConnection);
	        buide.connectTimeout(this.outTime);
	        buide.maxWaitTime(this.maxWait);
	        buide.threadsAllowedToBlockForConnectionMultiplier(this.maxBlockThreads);
	        buide.maxConnectionIdleTime(this.maxConnection);
	        buide.maxConnectionLifeTime(this.maxConnLifeTime);
	        buide.socketTimeout(this.socketOutTime);
	        buide.socketKeepAlive(this.socketKeepAlive);
	        MongoClientOptions options = buide.build();
	        mongoClient = new MongoClient(new ServerAddress(this.ip, this.port), options);
		}
		return mongoClient;
	}
	
	public int getMaxConnIdleTime() {
		return maxConnIdleTime;
	}
	public void setMaxConnIdleTime(String maxConnIdleTime) {
		this.maxConnIdleTime = convertStringToInt(maxConnIdleTime);
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public void setPort(String port) {
		this.port = convertStringToInt(port);
	}
	public void setMaxConnection(String maxConnection) {
		this.maxConnection = convertStringToInt(maxConnection);
	}
	public void setOutTime(String outTime) {
		this.outTime = convertStringToInt(outTime);
	}
	public void setMaxWait(String maxWait) {
		this.maxWait = convertStringToInt(maxWait);
	}
	public void setMaxBlockThreads(String maxBlockThreads) {
		this.maxBlockThreads = convertStringToInt(maxBlockThreads);
	}
	public void setMaxConnLifeTime(String maxConnLifeTime) {
		this.maxConnLifeTime = convertStringToInt(maxConnLifeTime);
	}
	public void setSocketOutTime(String socketOutTime) {
		this.socketOutTime = convertStringToInt(socketOutTime);
	}
	public void setSocketKeepAlive(String socketKeepAlive) {
		this.socketKeepAlive = "Y".equals(socketKeepAlive);
	}

}
