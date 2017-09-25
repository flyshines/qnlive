package qingning.server.rpc.initcache;

import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.CourseModuleServer;
import qingning.server.rpc.manager.IShopModuleServer;

import java.util.Map;

/**
 * 包名: qingning.shop.server.other
 * 描 述:
 * 创建日期: 2016/12/4
 */
public class ReadCourseOperation implements CommonReadOperation {
    private CourseModuleServer courseModuleServer;

    public ReadCourseOperation(CourseModuleServer courseModuleServer) {
        this.courseModuleServer = courseModuleServer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invokeProcess(RequestEntity requestEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) requestEntity.getParam();
        String functionName = requestEntity.getFunctionName();
        if(Constants.SYS_SHOP_NON_LIVE_COUSE_UP.equals(functionName)){//店铺 非直播 上架

        }else if(Constants.SYS_SHOP_NON_LIVE_COUSE_DOWN.equals(functionName)){//店铺 非直播 下架

        }else if(Constants.SYS_SHOP_GOODS_TYPE_UP.equals(functionName)){//店铺 课程内容 上架

        }else if(Constants.SYS_SHOP_GOODS_TYPE_COUSE_DOWN.equals(functionName)){//店铺 课程内容 下架

        }else if(Constants.SYS_SHOP_LIVE_COUSE_PREDICTION_UP.equals(functionName)){//店铺 直播课 预告/正在直播 上架

        }else if(Constants.SYS_SHOP_LIVE_COUSE_PREDICTION_DOWN.equals(functionName)){//店铺 直播 预告/正在直播 下架

        }else if(Constants.SYS_SHOP_LIVE_COUSE_FINISH_UP.equals(functionName)){//店铺 直播课 结束 上架

        }else if(Constants.SYS_SHOP_LIVE_COUSE_FINISH_DOWN.equals(functionName)){//店铺 直播 结束 下架

        }




        if (SYS_READ_LAST_COURSE.equals(functionName)) {
            return courseModuleServer.findLastestFinishCourse(reqMap);
        } else if (SYS_READ_SAAS_COURSE.equals(functionName)) {    //根据课程id数据库查询saas课程
            return courseModuleServer.findSaasCourseByCourseId(reqMap.get("course_id").toString());
        } else if ("getCourseByMap".equals(functionName)) {	//根据条件获取直播课程详情
        	return courseModuleServer.getCourseListByMap(reqMap);
        } else if ("getGuestAndCourseInfoByMap".equals(functionName)) {	//根据条件获取嘉宾课程列表，并关联查询出课程详情
        	return courseModuleServer.getGuestAndCourseInfoByMap(reqMap);
        } else {
            return courseModuleServer.findCourseByCourseId(reqMap.get("course_id").toString());
        }

        /*if(course==null){
            course = commonModuleServer.findSaaSCourseByCourseId(reqMap.get("course_id").toString());
        }*/
    }

    @Override
    public Object invokeProcessByFunction(Map<String, Object> reqMap, String functionName) throws Exception {
        return null;
    }
}
