
package qingning.saas.db.server.impl;


import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.ISaaSModuleServer;
import redis.clients.jedis.Jedis;

import java.util.*;


public class SaaSModuleServerImpl implements ISaaSModuleServer {
    @Autowired(required = true)
    private UserMapper userMapper;
    @Autowired(required = true)
    private CoursesStudentsMapper coursesStudentsMapper;
    @Autowired(required = true)
    private FansMapper fansMapper;
    @Autowired(required = true)
    private SaaSShopMapper shopMapper;
    @Autowired(required = true)
    private SaaSBannerMapper bannerMapper;
    @Autowired(required = true)
    private SaaSCourseMapper saasCourseMapper;
    @Autowired(required = true)
    private SaaSShopUserMapper shopUserMapper;
    @Autowired(required = true)
    private SeriesMapper seriesMapper;
    @Autowired(required = true)
    private SeriesStudentsMapper seriesStudentsMapper;
    @Autowired(required = true)
    private CoursesMapper coursesMapper;
    @Autowired(required = true)
    private SaaSFeedBackMapper saasFeedbackMapper;
    @Autowired(required = true)
    private SaaSCourseCommentMapper courseCommentMapper;
    @Autowired(required = true)
    private UserGainsMapper userGainsMapper;
    @Autowired(required = true)
    private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;
    @Autowired(required = true)
    private SystemConfigMapper systemConfigMapper;
    @Autowired(required = true)
    private TradeBillMapper tradeBillMapper;
    @Autowired(required = true)
    private WithdrawCashMapper withdrawCashMapper;

    @Override
    public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
        return coursesStudentsMapper.findCourseIdByStudent(reqMap);
    }


    @Override
    public List<Map<String, Object>> findRoomIdByFans(Map<String, Object> reqMap) {
        return fansMapper.findRoomIdByFans(reqMap);
    }


    @Override
    public Map<String, Object> findUserInfoByUserId(String user_id) {
        return userMapper.findByUserId(user_id);
    }

    @Override
    public void updateShop(Map<String, Object> param) {
        shopMapper.updateByPrimaryKey(param);
    }

    @Override
    public Map<String, Object> getShopBannerList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_count").toString()));
        PageList<Map<String, Object>> result = bannerMapper.selectListByUserId(param, page);
        //根据优先级排序
        Collections.sort(result, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                int position1;
                int position2 = 0;
                Map<String, Object> map1 = (Map) o1;
                Map<String, Object> map2 = (Map) o2;
                if (map1.get("position") != null && StringUtils.isNotEmpty(map1.get("position") + "")) {
                    position1 = Integer.valueOf(map1.get("position").toString());
                } else {
                    return 1;
                }
                if (map2.get("position") != null && StringUtils.isNotEmpty(map2.get("position") + "")) {
                    position2 = Integer.valueOf(map2.get("position").toString());
                }
                if (position1 > position2) return 1;
                else if (position1 < position2) return -1;
                else return 1;
            }
        });

        int upSize = bannerMapper.selectUpCount(param.get("user_id").toString());

        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        res.put("up_count", upSize);
        return res;
    }

    @Override
    public Map<String, Object> addShopBanner(Map<String, Object> param) {
        bannerMapper.insert(param);
        return null;
    }

    /**
     * 前端获取店铺轮播列表
     */
    @Override
    public List<Map<String, Object>> getShopBannerListForFront(Map<String, Object> paramMap) {
        return bannerMapper.selectBannerListByMap(paramMap);
    }

    @Override
    public Map<String, Object> getShopInfo(Map<String, Object> param) {
        return shopMapper.selectByPrimaryKey(param);
    }

    /**
     * 根据系列id获取系列详情
     */
    @Override
    public Map<String, Object> findSeriesBySeriesId(String seriesId) {
        return seriesMapper.findSeriesBySeriesId(seriesId);
    }

    /**
     * 根据条件查询系列id
     */
    @Override
    public List<Map<String, Object>> findSeriesStudentsByMap(Map<String, Object> selectSeriesStudentsMap) {
        return seriesStudentsMapper.selectSeriesStudentsByMap(selectSeriesStudentsMap);
    }

    @Override
    public int updateBanner(Map<String, Object> param) {
        int i = bannerMapper.selectUpCount(param.get("user_id").toString());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("config_key", "saasBannerSize");
        paramMap.put("appName", Constants.HEADER_APP_NAME);
        Map<String, Object> sysConfig = systemConfigMapper.findCustomerServiceBySystemConfig(paramMap);
        int size = 3;
        if (sysConfig != null && sysConfig.get("config_key") != null) {
            size = Integer.valueOf(sysConfig.get("config_value").toString());
        }
        if (i >= size) {
            return 1;
        }
        bannerMapper.updateByPrimaryKey(param);
        return 0;
    }

    @Override
    public void addCourse(Map<String, Object> param) {
        saasCourseMapper.insert(param);
    }

    @Override
    public void updateCourse(Map<String, Object> param) {
        saasCourseMapper.updateByPrimaryKey(param);
    }

    /**
     * 根据课程id获取saas课程信息，从t_saas_course查询
     */
    @Override
    public Map<String, Object> findSaasCourseByCourseId(String courseId) {
        return saasCourseMapper.selectByPrimaryKey(courseId);
    }

    /**
     * 根据课程id获取直播课程信息，从t_course查询
     */
    @Override
    public Map<String, Object> findCourseByCourseId(String courseId) {
        return coursesMapper.findCourseByCourseId(courseId);
    }

    @Override
    public Map<String, Object> getSingleList(Map<String, Object> param) {
        if ("0".equals(param.get("type"))) {
            //直播
            PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
            PageList<Map<String, Object>> result = coursesMapper.findAllListByLiturere(param, page);
            Map<String, Object> res = new HashMap<>();
            res.put("list", result);
            res.put("total_count", result.getTotal());
            res.put("total_page", result.getPaginator().getTotalPages());
            return res;
        } else {
            PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
            PageList<Map<String, Object>> result = saasCourseMapper.selectByShop(param, page);
            Map<String, Object> res = new HashMap<>();
            res.put("list", result);
            res.put("total_count", result.getTotal());
            res.put("total_page", result.getPaginator().getTotalPages());
            return res;
        }
    }

    @Override
    public Map<String, Object> getShopUsers(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = shopUserMapper.selectUsersByShop(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        //付费用户，免费用户
        int free = shopUserMapper.selectCountByShopId(param.get("shop_id").toString(),"0");
        int paied = shopUserMapper.selectCountByShopId(param.get("shop_id").toString(),"1");
        res.put("paied_count",paied);
        res.put("free_count",free);
        return res;
    }

    @Override
    public Map<String, Object> getCourseComment(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = courseCommentMapper.selectCommentByShop(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> getUserFeedBack(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = saasFeedbackMapper.selectFeedBackByShop(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public List<String> findShopUpList(String shop_id) {
        return saasCourseMapper.selectUpListByShopId(shop_id);
    }

    @Override
    public Map<String, Object> getSeriesList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = seriesMapper.selectSeriesListByLecturerId(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    /**
     * 根据留言id获取留言信息
     */
    @Override
    public Map<String, Object> findSaasCourseCommentByCommentId(String commentId) {
        return courseCommentMapper.selectByPrimaryKey(commentId);
    }

    @Override
    public Map<String, Object> getShopBannerInfo(String bannerId) {

        return bannerMapper.selectByPrimaryKey(bannerId);
    }

    @Override
    public Map<String, Object> findUpCourseList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = saasCourseMapper.selectUpCourseListByShop(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> findUpLiveCourseList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = null;
        if ("2".equals(param.get("type").toString())) {
            //系列课
            result = seriesMapper.findSeriesListByLiturere(param, page);
        } else {
            //直播课程
            result = coursesMapper.findCourseListByLiturere(param, page);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> getSeriesCourseList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = null;
        if ("0".equals(param.get("series_course_type").toString())) {
            //直播课程
            result = coursesMapper.findCourseBySeriesId(param, page);
        } else {
            //sass课程
            result = saasCourseMapper.findCourseBySeriesId(param, page);
        }
        //是否是单卖判断
        for (Map<String, Object> map : result) {
            if (!"0".equals(map.get("is_single").toString())) {
                //非单卖
                map.put("is_single", "1");
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public void openShop(Map<String, Object> shop) {
        shopMapper.insert(shop);
    }

    /**
     * 新增saas课程的留言，同时更新课程的评论次数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addSaasCourseComment(Map<String, Object> insertCommentMap, Map<String, Object> updateCourseMap, Map<String, Object> updateSaasShopUserMap) {

        //新增数据库saas课程的留言
        courseCommentMapper.insert(insertCommentMap);
        //更新数据库课程的评论次数
        saasCourseMapper.updateByPrimaryKey(updateCourseMap);
        //更新数据库用户留言数
        shopUserMapper.updateByPrimaryKey(updateSaasShopUserMap);
        return 1;
    }

    /**
     * 根据条件获取直播课程列表
     */
    @Override
    public List<Map<String, Object>> findLiveCourseListByMap(Map<String, Object> reqMap) {
        return coursesMapper.findCourseByMap(reqMap);
    }

    @Override
    public Map<String, Object> getLiveList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = coursesMapper.findAllListByLiturere(param, page);
        for (Map<String, Object> map : result) {
            if ("0".equals(map.get("is_start"))) {
                map.put("type", "0");
            } else if ("1".equals(map.get("is_start")) || map.get("end_time") == null) {
                map.put("type", "1");
            } else {
                map.put("type", "2");
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    /**
     * 新增反馈和建议
     */
    @Override
    public int addFeedback(Map<String, Object> newFeedbackMap) {
        return saasFeedbackMapper.insert(newFeedbackMap);
    }


    @Override
    public List<Map<String, Object>> findSeriesIdByStudent(Map<String, Object> reqMap) {
        return seriesStudentsMapper.findSeriesIdByStudent(reqMap);
    }

    @Override
    public Map<String, Object> findUserGainsByUserId(String userId) {
        Map<String, Object> result = userGainsMapper.findUserGainsByUserId(userId);
        if (result == null) {
            //初始化用户余额信息
            Map<String, Object> gainsMap = new HashMap<>();
            gainsMap.put("user_id", userId);
            gainsMap.put("live_room_total_amount", "0");
            gainsMap.put("live_room_real_incomes", "0");
            gainsMap.put("distributer_total_amount", "0");
            gainsMap.put("distributer_real_incomes", "0");
            gainsMap.put("user_total_amount", "0");
            gainsMap.put("user_total_real_incomes", "0");
            gainsMap.put("balance", "0");
            userGainsMapper.insertUserGainsByNewUser(gainsMap);
            return gainsMap;
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> findUserBuiedRecords(Map<String, Object> query) {
        List<Map<String, Object>> res;
        if ("1".equals(query.get("type"))) {
            //单品已购
            query.put("profit_type", "0");
            res = lecturerCoursesProfitMapper.findUserBuiedSingleRecords(query);
        } else {
            //系列已购
            query.put("profit_type", "2");
            res = lecturerCoursesProfitMapper.findUserBuiedRecords(query);
            res.stream().filter(map -> map.get("series_title") != null).forEach(map -> {
                map.put("title", map.get("series_title"));
            });

        }
        return res;
    }

    @Override
    public Map<String, Object> getOrdersList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result;
        if (param.get("order_type") != null) {
            if ("1".equals(param.get("order_type").toString())) {
                param.put("dist", "1");
            } else if ("2".equals(param.get("order_type").toString())) {
                param.put("auto", "1");
            }
            result = lecturerCoursesProfitMapper.searchOrdersListByUserId(param, page);
        } else if (param.get("nick_name") != null || param.get("goods_name") != null) {
            result = lecturerCoursesProfitMapper.searchOrdersListByUserId(param, page);
        } else {
            result = lecturerCoursesProfitMapper.selectOrdersListByUserId(param, page);

        }
        for (Map<String, Object> map : result) {

            //订单类型（1:分销订单，2:普通订单）
            if (map.get("distributer_id") != null) {
                //分销订单
                map.put("order_type", "1");

                Long profitAmount = Long.valueOf(map.get("profit_amount").toString());
                Long shareAmount = Long.valueOf(map.get("share_amount").toString());
                map.put("income",profitAmount - shareAmount);

            } else {
                //普通订单
                map.put("order_type", "2");
                map.put("income",map.get("profit_amount"));
            }
            map.remove("distributer_id");

            //商品类型（1:小圈子，2:系列，3:单品，4:打赏）
            if ("1".equals(map.get("profit_type").toString())) {
                map.put("goods_type", "1");
            } else if ("2".equals(map.get("profit_type").toString())) {
                //系列课程
                map.put("goods_type", "2");
            } else {
                //单品
                map.put("goods_type", "0");
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    /**
     * 判断用户是否是指定课程的学员
     */
    @Override
    public boolean isStudentOfTheCourse(Map<String, Object> selectIsStudentMap) {
        String isCourseStudent = coursesStudentsMapper.isStudentOfTheCourse(selectIsStudentMap);
        if ("1".equals(isCourseStudent)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 根据key查询系统配置表
     */
    @Override
    public List<Map<String, Object>> findSystemConfigByInKey(Map<String, Object> selectSysConfigMap) {
        return systemConfigMapper.selectSysConfigByInKey(selectSysConfigMap);
    }

    @Override
    public int updateUserPhone(Map<String, Object> userMap) {
        //手机号码已经绑定
        if (userMapper.existByPhone(userMap) > 0) {
            return 1;
        }
        userMapper.updateUser(userMap);
        return 0;
    }

    /**
     * 根据条件查询订单
     */
	@Override
	public Map<String, Object> findTradeBillByMap(Map<String, Object> selectTradeBillMap) {
		return tradeBillMapper.findTradeBillByMap(selectTradeBillMap);
	}

    @Override
    public void userVisitShop(String userId, String shopId) {
        if (shopUserMapper.selectExistUser(userId, shopId) == 0) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("shop_id", shopId);
            userMap.put("user_id", userId);
            userMap.put("user_type", "0");
            userMap.put("total_consume", 0);
            userMap.put("total_consume", 0);
            userMap.put("msg_num", 0);
            userMap.put("create_time", new Date());
            shopUserMapper.insert(userMap);
        }
    }

    /**
     * 根据非空字段更新系列课详情
     */
	@Override
	public int updateSeriesByMap(Map<String, Object> updateSeriesMap) {
		return seriesMapper.updateSeries(updateSeriesMap);
	}

    /**提现记录
     * @param param
     * @return
     */
 /*   @Override
    public Map<String, Object> getUserWithdrawList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = withdrawCashMapper.selectSaaSWithdrawList(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }*/

    @Override
    public Map<String,Object> getShopInfoList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = shopMapper.getShopInfoList(param, page);

        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }


    @Override
    public List<Map<String,Object>> findCourseBySeriesId(String series_id){
        return saasCourseMapper.findCourseBySeries(series_id);
    }

}
