package qingning.server.rpc.manager;


import qingning.server.rpc.*;

import java.util.List;
import java.util.Map;

public interface ICommonModuleServer extends VersionModuleServer, CourseModuleServer, UserModuleServer, ShopModuleServer, VersionForceModuleServer, LecturerModuleServer,SeriesModuleServer,ConfigModuleServer {
    List<Map<String, Object>> getServerUrlBy();


    List<Map<String, Object>> findRewardConfigurationList();

    Map<String, Object> createLiveRoom(Map<String, Object> reqMap);


}
