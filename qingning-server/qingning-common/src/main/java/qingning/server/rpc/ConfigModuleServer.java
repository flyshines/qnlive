package qingning.server.rpc;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/9/25.
 */
public interface ConfigModuleServer {

    List<Map<String, Object>> findSystemConfig();

}
