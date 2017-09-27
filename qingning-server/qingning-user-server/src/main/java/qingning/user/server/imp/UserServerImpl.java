package qingning.user.server.imp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.IUserUserModuleServer;
import redis.clients.jedis.Jedis;

import java.util.*;

public class UserServerImpl extends AbstractQNLiveServer {

    private IUserUserModuleServer userModuleServer;

    private static Logger logger = LoggerFactory.getLogger(UserServerImpl.class);

    @Override
    public void initRpcServer() {
        if (userModuleServer == null) {
            userModuleServer = this.getRpcService("userModuleServer");

            readCourseOperation = new ReadCourseOperation(userModuleServer);
            readUserOperation = new ReadUserOperation(userModuleServer);
            readLecturerOperation = new ReadLecturerOperation(userModuleServer);
            readSeriesOperation = new ReadSeriesOperation(userModuleServer);
            readShopOperation = new ReadShopOperation(userModuleServer);
            readConfigOperation = new ReadConfigOperation(userModuleServer);

        }
    }

    /***************************** V2.0.0 ********************************/
    /**
     * 获取已购买的单品课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
//    @FunctionName("mySingleCourseList")
//    public Map<String, Object> mySingleCourseList(RequestEntity reqEntity) throws Exception{
//        Map<String, Object> resultMap = new HashMap<>();
//        List<Map<String, String>> courseInfoList = new ArrayList<>();
//        /*
//         * 获取请求参数
//         */
//        Map<String, Object> reqMap = (Map)reqEntity.getParam();
//        String loginedUserId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
//        reqMap.put("user_id", loginedUserId);
//        String lastCourseId = (String) reqMap.get("last_course_id");
//        int pageCount = ((Long)reqMap.get("page_count")).intValue();
//        Jedis jedis = jedisUtils.getJedis();
//        String shopId = (String) reqMap.get("shop_id");
//        long now = System.currentTimeMillis();
//        Map<String, Object> readCacheMap = new HashMap<>();
//        RequestEntity readCacheReqEntity = this.generateRequestEntity(null, null, null, readCacheMap);
//
//        /*
//         * 分页获取用户加入的单品课程id列表
//         */
//        //生成分页索引key，详细分页key
//        String courseIndexKey, coursePageKey;
//        readCacheMap.clear();
//        readCacheMap.put(Constants.CACHED_KEY_USER_FIELD, loginedUserId);
//        if (MiscUtils.isEmpty(shopId)) {    //请求中没有传递店铺id，说明获取用户在全平台下的已购单品
//            courseIndexKey = MiscUtils.getKeyOfCachedData(, readCacheMap);
//        } else {    //请求中传递店铺id，说明获取用户在该店铺下的已购单品
//
//        }
//
//        List<String> courseIdList = SortUtils.getCourses();
//
//        /*
//         * 根据课程id列表获取课程详情
//         */
//        if (!MiscUtils.isEmpty(courseIdList)) {
//            logger.info("获取已购买的单品课程>>>>分页获取到用户加入的单品课程id列表：" + courseIdList.toString());
//            /*
//             * 遍历课程id列表获取课程详情
//             */
//            readCacheMap.clear();
//            readCacheReqEntity.setFunctionName("getCourseByCourseId");
//            Map<String, String> courseInfo;
//            for (String courseId : courseIdList) {
//                courseInfo = readCourse(courseId, readCacheReqEntity, readCourseOperation, jedis, true);
//                if (!MiscUtils.isEmpty(courseInfo)) {
//                    if ("0".equals(courseInfo.get("goods_type"))) { //课程是直播课，需要判断直播状态
//                        //进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
//                        MiscUtils.courseTranferState(now, courseInfo);
//                    }
//                    courseInfoList.add(courseInfo);
//                } else {
//                     logger.error("获取已购买的单品课程>>>>遍历课程id列表获取课程详情发现课程不存在，course_id=" + courseId);
//                }
//            }
//        }
//
//        resultMap.put("course_info_list", courseInfoList);
//        return resultMap;
//    }

    /**
     * 获取已购买的系列课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
//    @FunctionName("mySeriesCourseList")
//    public Map<String, Object> mySeriesCourseList(RequestEntity reqEntity) throws Exception{
//        Map<String, Object> resultMap = new HashMap<>();
//        List<Map<String, String>> seriesInfoList = new ArrayList<>();
//        /*
//         * 获取请求参数
//         */
//        Map<String, Object> reqMap = (Map)reqEntity.getParam();
//        String loginedUserId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
//        reqMap.put("user_id", loginedUserId);
//        String lastSeriesId = (String) reqMap.get("last_series_id");
//        int pageCount = ((Long)reqMap.get("page_count")).intValue();
//        Jedis jedis = jedisUtils.getJedis(reqEntity.getAppName());
//        String shopId = (String) reqMap.get("shop_id");
//
//        /*
//         * 分页获取用户加入的系列课程id列表
//         */
//        Set<String> seriesIdSet = null;
//        Map<String, Object> readUserSeriesMap = new HashMap<>();
//        readUserSeriesMap.put("user_id", loginedUserId);
//        if (MiscUtils.isEmpty(shopId)) {    //请求中没有传递店铺id，说明获取用户在全平台下的已购系列
//            RequestEntity readUserSeriesReqEntity = this.generateRequestEntity(null, null, "getSeriesStudentListByMap", readUserSeriesMap);
//            seriesIdSet = CacheUtils.readUserSeriesIdSet(loginedUserId, lastSeriesId, pageCount,
//                    readUserSeriesReqEntity, readUserOperation, jedis);
//        } else {    //请求中传递店铺id，说明获取用户在该店铺下的已购系列
//            /*
//             * 根据请求店铺获得该店铺的讲师id
//             */
//            Map<String, Object> readShopMap = new HashMap<>();
//            readShopMap.put("shop_id", shopId);
//            RequestEntity readShopReqEntity = this.generateRequestEntity(null, null, "getShopInfoByMap", readShopMap);
//            Map<String, String> shopInfo = CacheUtils.readShop(shopId, readShopReqEntity, readShopOperation, jedis);
//            if (MiscUtils.isEmpty(shopInfo)) {
//                logger.error("获取已购买的系列课程>>>>请求的店铺不存在");
//                throw new QNLiveException("190001");
//            }
//            readUserSeriesMap.put("lecturer_id", shopInfo.get("user_id"));
//            RequestEntity readUserShopSeriesReqEntity = this.generateRequestEntity(null, null, "getSeriesStudentListByMap", readUserSeriesMap);
//            seriesIdSet = CacheUtils.readUserShopSeriesIdSet(loginedUserId, shopId, lastSeriesId, pageCount,
//                    readUserShopSeriesReqEntity, readUserOperation, jedis);
//        }
//
//        if (!MiscUtils.isEmpty(seriesIdSet)) {
//            logger.info("获取已购买的系列课程>>>>分页获取到用户加入的系列课程id列表：" + seriesIdSet.toString());
//            /*
//             * 遍历课程id列表获取课程详情
//             */
//            Map<String, Object> readSeriesMap = new HashMap<>();
//            RequestEntity readSeriesReqEntity = this.generateRequestEntity(null, null, "getSeriesBySeriesId", readSeriesMap);
//            Map<String, String> seriesInfo = new HashMap<>();
//            for (String seriesId : seriesIdSet) {
//                readSeriesMap.put("series_id", seriesId);
//                seriesInfo = CacheUtils.readSeries(seriesId, readSeriesReqEntity, readSeriesOperation, jedis, true);
//                if (!MiscUtils.isEmpty(seriesInfo)) {
//                    seriesInfoList.add(seriesInfo);
//                } else {
//                    logger.error("获取已购买的系列课程>>>>遍历课程id列表获取课程详情发现课程不存在，series_id=" + seriesId);
//                }
//            }
//        }
//
//        resultMap.put("series_info_list", seriesInfoList);
//        return resultMap;
//    }

}
