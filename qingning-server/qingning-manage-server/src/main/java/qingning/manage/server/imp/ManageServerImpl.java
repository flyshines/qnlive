package qingning.manage.server.imp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;


import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ICommonModuleServer;
import qingning.server.rpc.manager.IShopModuleServer;
import qingning.server.rpc.manager.IUserUserModuleServer;
import redis.clients.jedis.Jedis;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
public class ManageServerImpl extends AbstractQNLiveServer {
    private static Logger logger = LoggerFactory.getLogger(ManageServerImpl.class);

    private IUserUserModuleServer userModuleServer;
    private ICommonModuleServer commonModuleServer;
    private IShopModuleServer shopModuleServer;

    @Override
    public void initRpcServer() {
        if (userModuleServer == null) {
            userModuleServer = this.getRpcService("userModuleServer");
            boolean init = false;
            if(userModuleServer!=null){
                userServer = userModuleServer;
                courseModuleServer = userModuleServer;
                lecturerModuleServer = userModuleServer;
                seriesModuleServer = userModuleServer;
                configModuleServer = userModuleServer;
                init = true;
            }
            commonModuleServer = this.getRpcService("commonModuleServer");
            if(commonModuleServer!=null&&!init){
                userServer = commonModuleServer;
                courseModuleServer = commonModuleServer;
                lecturerModuleServer = commonModuleServer;
                seriesModuleServer = commonModuleServer;
                configModuleServer = commonModuleServer;
                init = true;
            }
            shopModuleServer = this.getRpcService("shopModuleServer");
            if(shopModuleServer!=null&&!init){
                userServer = this.shopModuleServer;
                courseModuleServer = this.shopModuleServer;
                seriesModuleServer = this.shopModuleServer;
                configModuleServer = this.shopModuleServer;
            }
            initServer();
        }
    }

   /**
     * 获取已购买的单品课程
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("mySingleCourseList")
    public Map<String, Object> mySingleCourseList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, String>> courseInfoList = new ArrayList<>();
        /*
         * 获取请求参数
         */
        Map<String, Object> reqMap = (Map) reqEntity.getParam();
        //获取登录用户id
        String loginedUserId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        reqMap.put("user_id", loginedUserId);
        //获取请求中的分页标识（上一页最后一门课程id）
        String lastCourseId = (String) reqMap.get("last_course_id");
        int pageCount = ((Long) reqMap.get("page_count")).intValue();
        Jedis jedis = jedisUtils.getJedis();
        String shopId = (String) reqMap.get("shop_id");
        long now = System.currentTimeMillis();
        //读取缓存必备参数
        Map<String, Object> readCacheMap = new HashMap<>();
        RequestEntity readCacheReqEntity = this.generateRequestEntity(null, null, null, readCacheMap);

        /*
         * 分页获取用户加入的单品课程id列表
         */
        Set<String> courseIdSet;
        readCacheMap.clear();
        readCacheMap.put("user_id", loginedUserId);
        if (MiscUtils.isEmpty(shopId)) {    //请求中没有传递店铺id，说明获取用户在全平台下的已购单品
            readCacheReqEntity.setFunctionName("getCourseStudentListByMap");
            courseIdSet = readUserCourseIdSet(loginedUserId, lastCourseId, pageCount,
                    readCacheReqEntity, readUserOperation, jedis);
        } else {    //请求中传递店铺id，说明获取用户在该店铺下的已购单品
        	/*
        	 * 根据请求店铺获得该店铺的讲师id
        	 */
            Map<String, Object> readShopMap = new HashMap<>();
            readShopMap.put("shop_id", shopId);
            Map<String, String> shopInfo = readShop(shopId, readShopMap, "getShopInfoByMap", false, jedis);
            if (MiscUtils.isEmpty(shopInfo)) {
                logger.error("获取已购买的单品课程>>>>请求的店铺不存在");
                throw new QNLiveException("190001");
            }
        	/*
        	 * 查找用户在指定店铺加入的单品课程id列表
        	 */
            readCacheMap.put("lecturer_id", shopInfo.get("user_id"));
            readCacheReqEntity.setFunctionName("getCourseStudentListByMap");
            courseIdSet = readUserShopCourseIdSet(loginedUserId, shopId, lastCourseId, pageCount,
                    readCacheReqEntity, readUserOperation, jedis);
        }

        if (!MiscUtils.isEmpty(courseIdSet)) {
            logger.info("获取已购买的单品课程>>>>分页获取到用户加入的单品课程id列表：" + courseIdSet.toString());
        	/*
        	 * 遍历课程id列表获取课程详情
        	 */
            readCacheMap.clear();
            readCacheReqEntity.setFunctionName(null);
            Map<String, String> courseInfo;
            for (String courseId : courseIdSet) {
                readCacheMap.put("course_id", courseId);
                courseInfo = readCourse(courseId, readCacheReqEntity, readCourseOperation, jedis, true);
                if (!MiscUtils.isEmpty(courseInfo)) {
                    //直播课程进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
                    if ("0".equals(courseInfo.get("goods_type"))) {
                        MiscUtils.courseTranferState(now, courseInfo);
                    }
                    courseInfoList.add(courseInfo);
                } else {
                    logger.error("获取已购买的单品课程>>>>遍历课程id列表获取课程详情发现课程不存在，course_id=" + courseId);
                }
            }
        }

        resultMap.put("course_info_list", courseInfoList);
        return resultMap;
    }

    /**
     * 搜索banner列表
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getBannerListBySearch")
    public Map<String, Object> getBannerListBySearch(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        /*
         * 获取请求参数
         */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        //计算页码，用于sql的limit语句
        long pageNum = (long) reqMap.get("page_num");
        long pageCount = (long) reqMap.get("page_count");
        reqMap.put("page_num", (pageNum - 1) * pageCount);

        /*
         * 查询t_banner_info表
         */
        List<Map<String, Object>> bannerList = commonModuleServer.findBannerInfoByMap(reqMap);
        if (bannerList != null) {
            logger.info("后台搜索banner列表>>>>获得" + bannerList.size() + "条banner数据");
        }

        resultMap.put("banner_info_list", bannerList);
        resultMap.put("total_num", commonModuleServer.findBannerCountByMap(reqMap));
        return resultMap;
    }
}
