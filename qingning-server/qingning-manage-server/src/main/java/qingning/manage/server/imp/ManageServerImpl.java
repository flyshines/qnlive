package qingning.manage.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qingning.common.dj.HttpClientUtil;
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

    /**
     * 获取提现记录列表-后台
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getWithdrawListFinance")
    public Map<String, Object> getWithdrawListFinance(RequestEntity reqEntity) throws Exception{
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> param = (Map)reqEntity.getParam();
        /*
         * 查询提现记录列表-财务
         */
        //记录数查询标识
        param.put("is_sys","1");
        param.put("finance","1");
        return userModuleServer.findWithdrawListAll(param);
    }
    /**
     * 获取提现记录列表-后台
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getWithdrawListAll")
    public Map<String, Object> getWithdrawListAll(RequestEntity reqEntity) throws Exception{
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> param = (Map)reqEntity.getParam();
        /*
         * TODO 判断后台是否登录
         */

        /*
         * 查询提现记录列表-运营
         */
        //记录数查询标识
        param.put("is_sys","1");
        Map<String,Object> result = userModuleServer.findWithdrawListAll(param);
        return result;
    }

    /**
     * 后台_处理提现
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("handleWithDrawResult")
    public Map<String, Object> handleWithDrawResult(RequestEntity reqEntity) throws Exception{
        Map<String, Object> resultMap = new HashMap<>();
    	/*
    	 * 获取请求参数
    	 */
        Map<String, Object> param = (Map)reqEntity.getParam();
        String withdrawId = param.get("withdraw_cash_id").toString();
        String remark = "";
        if(param.get("remark") != null){
            remark = param.get("remark").toString();
        }
        String result = param.get("result").toString();

        //审核人员ID
        String adminId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
        if(adminId == null){
            throw new QNLiveException("000005","系统用户不存在");
        }
        //验证审核人员
        Map<String,Object> adminInfo = userModuleServer.selectAdminUserById(adminId);
        if(adminInfo == null){
            throw new QNLiveException("000005","系统用户不存在");
        }
        //角色
        String role = adminInfo.get("role_id").toString();
        String adminName = adminInfo.get("username").toString();
        /*
         * 查询提现记录
         */
        Map<String, Object> selectMap = new HashMap<>();
        selectMap.put("withdraw_cash_id", withdrawId);
        Map<String, Object> withdraw = userModuleServer.selectWithdrawSizeById(selectMap);

        if("3".equals(role)&&withdraw.get("handle_id")==null){
            //未经过运营审核
            throw new QNLiveException("170005","未经过运营审核");
        }

        if(withdraw==null||!"0".equals(withdraw.get("state"))||(("2").equals(role)&&withdraw.get("handle_id")!=null)){
            //未找到提现记录或重复提现
            throw new QNLiveException("170004");
        }else {
            //同意提现，更新提现记录，用户余额
            long initial_amount = Long.valueOf(withdraw.get("initial_amount").toString());
            userModuleServer.updateWithdraw(withdrawId, remark, withdraw.get("user_id").toString(), result, initial_amount,adminId,role,adminName);
        }
        return resultMap;
    }

    /**
     * 获取订单记录列表-后台
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getOrderListAll")
    public Map<String, Object> getOrderListAll(RequestEntity reqEntity) throws Exception{

        //获取请求参数
        Map<String, Object> param = (Map)reqEntity.getParam();

        //查询提现记录列表

        return userModuleServer.findOrderListAll(param);
    }
    /**
     * 导出订单记录列表-后台
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("exportOrderListAll")
    public Map<String, Object> exportOrderListAll(RequestEntity reqEntity) throws Exception{

        //获取请求参数
        Map<String, Object> param = (Map)reqEntity.getParam();

        //查询提现记录列表
        return userModuleServer.findOrderListAll(param);
    }
    /**
     * 后台_课程搜索查询
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getCourseListBySearch")
    public Map<String, Object> getCourseListBySearch(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        /*
         * 获取请求参数
         */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();
        /*
         * 查询数据库
         */
        List<Map<String, Object>> courseList = commonModuleServer.findCourseListBySearch(reqMap);
        //从缓存中获取直播间名称和讲师名称

        resultMap.put("course_info_list", courseList);

        return resultMap;
    }
    /**
     * 后台登录
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("adminUserLogin")
    public Map<String, Object> adminUserLogin(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        /*
         * 获取请求参数
         */
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        Jedis jedis = jedisUtils.getJedis();

        String mobile = (String) reqMap.get("mobile");
        String password = ((String) reqMap.get("password")).toUpperCase();

        /*
         * 根据号码查询数据库
         */
        Map<String, Object> adminUserMap = commonModuleServer.getAdminUserByMobile(reqMap);
        if (adminUserMap == null) {
            logger.info("后台登录>>>>手机号没有关联账户");
            throw new QNLiveException("000005");
        }

        /*
         * 验证密码:
         * 	前端传递的MD5加密字符串后追加appName，在进行MD5加密
         */
        String md5Pw = MD5Util.getMD5(password + "_" + "qnlive");
        if (!md5Pw.equals(adminUserMap.get("password").toString())) {
            logger.info("后台登录>>>>登录密码错误");
            throw new QNLiveException("120001");
        }

        /*
         * 更新后台登录用户统计数据
         */
        Date now = new Date();
        adminUserMap.put("last_login_time", now);
        adminUserMap.put("last_login_ip", "");
        adminUserMap.put("login_num", 1);    //用于sql执行+1

        /*
         * 判断是否需要生成token
         */
        String userId = (String) adminUserMap.get("user_id");
        String accessToken = (String) adminUserMap.get("token");
        Map<String, Object> map = new HashMap<String, Object>();
        String accessTokenKey = null;
        if (!StringUtils.isBlank(accessToken)) {    //数据库中的token不为空
            map.put("access_token", accessToken);
            accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN,
                    map);
            if (!jedis.exists(accessTokenKey)) {    //不在redis中
            	/*
            	 * 生成新的accessToken
            	 */
                accessToken = AccessTokenUtil.generateAccessToken(userId, String.valueOf(now.getTime()));
                map.put("access_token", accessToken);
                accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN,
                        map);
            }
        } else {
        	/*
        	 * 生成新的accessToken
        	 */
            accessToken = AccessTokenUtil.generateAccessToken(userId, String.valueOf(now.getTime()));
            map.put("access_token", accessToken);
            accessTokenKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN,
                    map);
        }

        adminUserMap.put("token", accessToken);
        commonModuleServer.updateAdminUserByAllMap(adminUserMap);

        /*
         * 将token写入缓存
         */
        Map<String, String> tokenCacheMap = new HashMap<>();
        MiscUtils.converObjectMapToStringMap(adminUserMap, tokenCacheMap);

        if (!tokenCacheMap.isEmpty()) {
            jedis.hmset(accessTokenKey, tokenCacheMap);
        }
        jedis.expire(accessTokenKey, Integer.parseInt(MiscUtils.getConfigByKey("access_token_expired_time")));

        /*
         * 返回数据
         */
        adminUserMap.put("access_token", accessToken);
        adminUserMap.put("version", "1.2.0");    //暂时写死
        resultMap.putAll(adminUserMap);

        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("version", "1.2.0");
        headerMap.put("Content-Type", "application/json;charset=UTF-8");
        headerMap.put("access_token",accessToken);
        //获取知享课程数
        String getUrl = MiscUtils.getConfigByKey("sharing_api_url")
                +SharingConstants.SHARING_SERVER_USER_COMMON
                +SharingConstants.SHARING_USER_COMMON_GENERATE_TOKEN+"?type=admin";
        String result = HttpClientUtil.doGet(getUrl, headerMap, null, "UTF-8");
        resultMap.put("synchronization_token",result);
        return resultMap;
    }
    /**
     * 后台_获取分类列表
     *
     * @param reqEntity
     * @return
     * @throws Exception
     */
    @FunctionName("getClassifyList")
    public Map<String, Object> getClassifyList(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> reqMap = new HashMap();

        //获取数据库所有的分类信息
        List<Map<String, Object>> classifyList = commonModuleServer.getClassifyList(reqMap);

        /*
         * 循环所有分类信息，拼接分类id，用于后面查询课程表统计类目下的课程数量
         */
        StringBuilder classSB = new StringBuilder();
        if (classifyList != null) {
            for (Map<String, Object> classify : classifyList) {
                classSB.append(classify.get("classify_id")).append(",");
            }
            //去除最后对于的逗号
            classSB.deleteCharAt(classSB.length() - 1);
        }

        /*
         * 查询获得分类对应的课程数量
         */
        Map<String, Object> selectMap = new HashMap<>();
        selectMap.put("classify_ids", classSB.toString());
        List<Map<String, Object>> classCourseNumList = commonModuleServer.getCourseNumGroupByClassifyId(selectMap);

        //循环将其转换成map（例：classify_id:course_num），方便后期查询映射
        Map<String, Object> classifyIdCourseNumMap = new HashMap<>();
        for (Map<String, Object> classCourseNum : classCourseNumList) {
            classifyIdCourseNumMap.put(classCourseNum.get("classify_id").toString(),
                    classCourseNum.get("course_num"));
        }
        JSONArray array = new JSONArray();
        /*
         * 循环分类列表，进行一一赋值课程数量
         */
        for (Map<String, Object> classifyMap : classifyList) {
            String classId = classifyMap.get("classify_id").toString();
            classifyMap.put("course_num", classifyIdCourseNumMap.get(classId));
            array.add(classId);
        }

        //参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("qn_classify_ids", array);
        //请求头
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("version", "1.0");
        headerMap.put("Content-Type", "application/json;charset=UTF-8");

        //获取知享课程数
        String getUrl = MiscUtils.getConfigByKey("sharing_api_url")
                +SharingConstants.SHARING_SERVER_COURSE
                +SharingConstants.SHARING_COURSE_CLASSIFY_COURSE_NUM;
        String result = HttpClientUtil.doPostUrl(getUrl, headerMap, paramMap, "UTF-8");
        try {
            JSONObject obj = JSON.parseObject(result);
            JSONObject resData = obj.getJSONObject("res_data");
            JSONObject classfy = resData.getJSONObject("classify_course_num");

            //插入知享数据
            for (Map<String, Object> classifyMap : classifyList) {
                String classId = classifyMap.get("classify_id").toString();
                classifyMap.put("zx_course_num", classfy.getString(classId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        resultMap.put("classify_info_list", classifyList);
        return resultMap;
    }
    /**
     * 后台_编辑分类信息
     *
     * @param reqEntity
     * @throws Exception
     */
    @FunctionName("editClassify")
    public Map<String, Object> editClassify(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> param = (Map) reqEntity.getParam();
        commonModuleServer.updateClassify(param);
        //清除缓存
        Set<String> classKeys = jedis.keys(Constants.CACHED_KEY_CLASSIFY_PATTERN);
        for (String classKey : classKeys) {
            jedis.del(classKey);
        }

        //更新知享分类列表
        //参数
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("qn_classify_id", param.get("classify_id").toString());
        if (param.get("classify_name") != null)
            paramMap.put("classify_name", param.get("classify_name").toString());
        if (param.get("position") != null)
            paramMap.put("position", param.get("position").toString());
        if (param.get("is_use") != null)
            paramMap.put("is_use", param.get("is_use").toString());
        //请求头
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("version", "1.0");
        headerMap.put("Content-Type", "application/json;charset=UTF-8");
        String updateClassfyUrl = MiscUtils.getConfigByKey("sharing_api_url")
                +SharingConstants.SHARING_SERVER_COURSE
                +SharingConstants.SHARING_COURSE_CLASSIFY_UPDATE;
        //获取请求
        String res = HttpClientUtil.doPutUrl(updateClassfyUrl, headerMap, paramMap, "UTF-8");
        logger.debug(res);
        return resultMap;
    }
    /**
     * 后台_添加分类信息
     *
     * @param reqEntity
     * @throws Exception
     */
    @FunctionName("addClassify")
    public Map<String, Object> addClassify(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        Jedis jedis = jedisUtils.getJedis();
        Map<String, Object> param = (Map) reqEntity.getParam();
        Date now = new Date();
        param.put("create_date", now);
        param.put("create_time", now.getTime());
        Map<String, Object> result = commonModuleServer.insertClassify(param);
        //清除缓存
        Set<String> classKeys = jedis.keys(Constants.CACHED_KEY_CLASSIFY_PATTERN);
        classKeys.forEach(jedis::del);

        //更新知享分类列表

        //参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("qn_classify_id", result.get("classify_id").toString());
        paramMap.put("classify_name", param.get("classify_name").toString());
        paramMap.put("is_use", result.get("is_use").toString());
        if (result.get("position") != null)
            paramMap.put("position", result.get("position").toString());
        else
            paramMap.put("position", "0");
        //请求头
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("version", "1.0");
        headerMap.put("Content-Type", "application/json;charset=UTF-8");

        String addClassfyUrl = MiscUtils.getConfigByKey("sharing_api_url")
                +SharingConstants.SHARING_SERVER_COURSE
                +SharingConstants.SHARING_COURSE_CLASSIFY_NEW;
        //获取请求
        String res = HttpClientUtil.doPostUrl(addClassfyUrl, headerMap, paramMap, "UTF-8");
        logger.debug(res);
        return resultMap;
    }
    /**
     * 获取分类
     *
     * @param reqEntity
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @FunctionName("classifyInfo")
    public Map<String, Object> classifyInfo(RequestEntity reqEntity) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Jedis jedis = jedisUtils.getJedis();
        List<Map<String, Object>> classifyList = new ArrayList<>();
        if (jedis.exists(Constants.CACHED_KEY_CLASSIFY_ALL)) { //判断当前是否有缓存key 存在
            Set<String> classifyIdSet = jedis.zrange(Constants.CACHED_KEY_CLASSIFY_ALL, 0, -1);//获取所有值
            Map<String, Object> map = new HashMap<>();
            for (String classify_id : classifyIdSet) {
                map.put(Constants.CACHED_KEY_CLASSIFY, classify_id);
                String classifyInfoKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_CLASSIFY_INFO, map);//生成查找分类的key
                if (jedis.exists(classifyInfoKey)) {//获取分类
                    Map<String, String> classifyInfo = jedis.hgetAll(classifyInfoKey);
                    Map<String, Object> classify_info = new HashMap<>();
                    classify_info.put("classify_id", classifyInfo.get("classify_id"));
                    classify_info.put("classify_name", classifyInfo.get("classify_name"));
                    classify_info.put("is_use", classifyInfo.get("is_use"));
                    classify_info.put("create_time", classifyInfo.get("create_time"));
                    classifyList.add(classify_info);
                }
            }
        } else {
            //没有
            Map<String, Object> param = new HashMap<>();
            param.put("is_use", "1");

            classifyList = commonModuleServer.getClassifyList(param);//读数据库
            Map<String, String> classify_info = new HashMap<>();
            for (Map<String, Object> classify : classifyList) {
                jedis.zadd(Constants.CACHED_KEY_CLASSIFY_ALL, System.currentTimeMillis(), classify.get("classify_id").toString());
                classify_info.put("classify_id", classify.get("classify_id").toString());
                classify_info.put("classify_name", classify.get("classify_name").toString());
                classify_info.put("is_use", classify.get("is_use").toString());
                classify_info.put("create_time", classify.get("create_time").toString());
                Map<String, Object> map = new HashMap<>();
                map.put("classify_id", classify.get("classify_id").toString());
                jedis.hmset(MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_CLASSIFY_INFO, map), classify_info);
            }

        }

        List<Map<String, Object>> resultClassifyList = new ArrayList<>();//返回结果
        Map<String, Object> ortherClassify = new HashMap<>();//其他
        for (Map<String, Object> classify : classifyList) {
            if (!classify.get("classify_id").toString().equals("9")) {//不是其他
                resultClassifyList.add(classify);
            } else if (classify.get("classify_id").toString().equals("9")) {
                ortherClassify.putAll(classify);//其他
            }
        }

        if (!MiscUtils.isEmpty(ortherClassify)) {
            resultClassifyList.add(ortherClassify);
        }


        if (!MiscUtils.isEmpty(resultClassifyList)) {
            resultMap.put("classify_info", resultClassifyList);
        }
        return resultMap;
    }
}
