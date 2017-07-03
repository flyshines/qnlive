package qingning.lecture.server.other;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.manager.ILectureModuleServer;

import java.util.Map;

/**
 * 包名: qingning.lecture.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadCourseOperation implements CommonReadOperation {
    private ILectureModuleServer lectureModuleServer;

    public ReadCourseOperation(ILectureModuleServer lectureModuleServer) {
        this.lectureModuleServer = lectureModuleServer;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        if (Constants.SYS_READ_LAST_COURSE.equals(functionName)) {
            return lectureModuleServer.findLastestFinishCourse(reqMap);
        } else if (Constants.SYS_READ_SAAS_COURSE.equals(functionName)) {    //根据课程id数据库查询saas课程
            return lectureModuleServer.findSaasCourseByCourseId(reqMap.get("course_id").toString());
        } else {
            return lectureModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
        }
    }
}
