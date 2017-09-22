package qingning.server.rpc;

import java.util.Map;

/**
 * Created by Administrator on 2017/9/22.
 */
public interface VersionModuleServer {
    Map<String,Object> findVersionInfoByOS(Map<String, Object> plateform);
}
