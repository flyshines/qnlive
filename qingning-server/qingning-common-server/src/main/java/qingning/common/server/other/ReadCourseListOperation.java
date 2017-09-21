package qingning.common.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadCourseListOperation implements CommonReadOperation {
    private ICommonModuleServer iCommonModuleServer;

    public ReadCourseListOperation(ICommonModuleServer iCommonModuleServer) {
        this.iCommonModuleServer = iCommonModuleServer;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        if(Constants.SYS_READ_NON_LIVE_COURSE_LIST.equals(requestEntity.getFunctionName())){//非直播课程

        }else if(Constants.SYS_READ_FINISH_LIVE_COURSE_LIST.equals(requestEntity.getFunctionName())){

        }
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();

        return iCommonModuleServer.findVersionInfoByOS(reqMap);
    }
}
