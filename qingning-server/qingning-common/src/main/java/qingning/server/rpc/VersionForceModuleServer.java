package qingning.server.rpc;

import java.util.Map;

/**
 * Created by Administrator on 2017/9/22.
 */
public interface VersionForceModuleServer {
    Map<String,Object> findForceVersionInfoByOS(String force_version_key);
}
