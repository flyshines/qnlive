package qingning.server;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public interface JedisBatchOperation {
	void  batchOperation(Pipeline pipeline,Jedis jedis);
}
