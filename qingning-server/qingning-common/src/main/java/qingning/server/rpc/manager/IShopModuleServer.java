package qingning.server.rpc.manager;


import qingning.server.rpc.*;

import java.util.List;
import java.util.Map;

public interface IShopModuleServer extends UserModuleServer,ShopModuleServer,CourseModuleServer,SeriesModuleServer,ConfigModuleServer {
	Map<String,Object> createLiveRoom(Map<String, Object> reqMap);
	/**开通店铺
	 * @param shop
	 * @return
	 */
	int insertShop(Map<String, Object> shop);

	/**更新店铺信息
	 * @param param
	 */
	int updateShop(Map<String, Object> param);





	Map<String,Object> createCourse(Map<String, Object> reqMap);
}
