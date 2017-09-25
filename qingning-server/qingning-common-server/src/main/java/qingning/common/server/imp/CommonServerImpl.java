package qingning.common.server.imp;

import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractQNLiveServer;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.ICommonModuleServer;

public class CommonServerImpl  extends AbstractQNLiveServer {
    private static Logger logger = LoggerFactory.getLogger(CommonServerImpl.class);
    private ICommonModuleServer commonModuleServer;

    @Override
    public void initRpcServer() {
        if (commonModuleServer == null) {
            commonModuleServer = this.getRpcService("commonModuleServer");
            readShopOperation = new ReadShopOperation(commonModuleServer);
            readUserOperation = new ReadUserOperation(commonModuleServer);
            readCourseOperation = new ReadCourseOperation(commonModuleServer);
            readAPPVersionOperation = new ReadVersionOperation(commonModuleServer);
            readForceVersionOperation = new ReadVersionForceOperation(commonModuleServer);
            readLecturerOperation = new ReadLecturerOperation(commonModuleServer);
            readSeriesOperation = new ReadSeriesOperation(commonModuleServer);
            readConfigOperation = new ReadConfigOperation(commonModuleServer);
        }
    }

    private static Auth auth;

    static {
        auth = Auth.create(MiscUtils.getConfigByKey("qiniu_AK"), MiscUtils.getConfigByKey("qiniu_SK"));
    }

}
