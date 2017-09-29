package qingning.server.rpc.manager;


import qingning.server.rpc.*;

import java.util.List;
import java.util.Map;

public interface ICommonModuleServer extends VersionModuleServer, CourseModuleServer, UserModuleServer, ShopModuleServer, VersionForceModuleServer, LecturerModuleServer,SeriesModuleServer,ConfigModuleServer {
    List<Map<String, Object>> getServerUrls();


    List<Map<String, Object>> findRewardConfigurationList();

    Map<String, Object> createLiveRoom(Map<String, Object> reqMap);


    /**更新用户信息
     * @param userMap
     * @return
     */
    int updateUserById(Map<String, Object> userMap);

    /**
     * 根据map中的参数查询banner
     * @param reqMap
     * @return
     */
    List<Map<String, Object>> findBannerInfoByMap(Map<String, Object> reqMap);
    /**
     * 根据map中的参数查询banner总数量
     * @param reqMap
     * @return
     */
    int findBannerCountByMap(Map<String, Object> reqMap);
}
