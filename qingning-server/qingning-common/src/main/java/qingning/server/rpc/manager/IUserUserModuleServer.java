package qingning.server.rpc.manager;


import qingning.server.rpc.*;

import java.util.List;
import java.util.Map;

public interface IUserUserModuleServer extends UserModuleServer,CourseModuleServer,LecturerModuleServer,ShopModuleServer,SeriesModuleServer,ConfigModuleServer {


	List<Map<String,Object>> findRewardConfigurationList();



}
