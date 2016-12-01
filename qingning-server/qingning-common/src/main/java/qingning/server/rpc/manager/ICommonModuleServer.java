package qingning.server.rpc.manager;


import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public interface ICommonModuleServer {
	public List<Map<String, Object>> getServerUrls();

	Map<String,Object> getLoginInfoByLoginIdAndLoginType(Map<String, Object> reqMap);

	Map<String,String> initializeRegisterUser(Map<String, Object> reqMap);


	Map<String,Object> findLoginInfoByUserId(String user_id);
}
