
package qingning.lecturer.db.server.imp;


import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.IShopModuleServer;

import java.util.*;

public class ShopModuleServerImpl implements IShopModuleServer {

    @Autowired(required = true)
    private FansMapper fansMapper;

    @Autowired(required = true)
    private LecturerMapper lecturerMapper;

    @Autowired(required = true)
    private LiveRoomMapper liveRoomMapper;

    @Autowired(required = true)
    private UserMapper userMapper;

    @Autowired(required = true)
    private LoginInfoMapper loginInfoMapper;

    @Autowired(required = true)
    private CourseMapper courseMapper;

    @Autowired(required = true)
    private ShopUserMapper shopUserMapper;
    @Autowired(required = true)
    private CourseCommentMapper courseCommentMapper;
    @Autowired(required = true)
    private UserGainsMapper userGainsMapper;
    @Autowired(required = true)
    private WithdrawCashMapper withdrawCashMapper;
    @Autowired(required = true)
    private ShopBannerMapper shopBannerMapper;

    @Autowired(required = true)
    private CoursesStudentsMapper coursesStudentsMapper;

    @Autowired(required = true)
    private LecturerDistributionInfoMapper lecturerDistributionInfoMapper;

    @Autowired(required = true)
    private SystemConfigMapper systemConfigMapper;

    @Autowired(required = true)
    private SeriesMapper seriesMapper;

    @Autowired(required = true)
    private SeriesStudentsMapper seriesStudentsMapper;

    @Autowired(required = true)
    private ShopMapper shopMapper;

    @Autowired(required = true)
    private CourseGuestMapper courseGuestMapper;

    @Autowired(required = true)
    private ShopFeedBackMapper shopFeedbackMapper;
    @Autowired(required = true)
    private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;

    /**
     * 创建直播间
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> createLiveRoom(Map<String, Object> reqMap) {
        Date now = new Date();
        //1.插入直播间表
        Map<String, Object> liveRoom = new HashMap<String, Object>();
        liveRoom.put("room_id", reqMap.get("room_id"));
        liveRoom.put("", reqMap.get(""));
        liveRoom.put("user_id", reqMap.get("user_id"));
        liveRoom.put("rq_code", reqMap.get("room_id"));
        liveRoom.put("room_address", reqMap.get("room_address"));
        liveRoom.put("room_name", reqMap.get("room_name"));
        liveRoom.put("avatar_address", reqMap.get("avatar_address"));
        liveRoom.put("room_remark", reqMap.get("room_remark"));
        liveRoom.put("lecturer_id", reqMap.get("user_id"));
        liveRoom.put("create_time", now);
        liveRoom.put("update_time", now);
        liveRoomMapper.insertLiveRoom(liveRoom);

        //2.如果该用户为普通用户，则需要插入讲师表，并且修改登录信息表中的身份，
        // 同时插入t_lecturer_distribution_info讲师分销信息表(统计冗余表)
        boolean isLecturer = (Boolean) reqMap.get("isLecturer");
        if (isLecturer == false) {
            //2.1插入讲师表
            Map<String, Object> lecturer = new HashMap<String, Object>();
            lecturer.put("lecturer_id", reqMap.get("user_id"));
            lecturer.put("live_room_num", 1L);
            lecturer.put("create_time", now);
            lecturer.put("update_time", now);
            lecturerMapper.insertLecture(lecturer);

            //2.2修改登录信息表 身份
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("user_id", reqMap.get("user_id").toString());
            updateMap.put("add_role", "," + Constants.USER_ROLE_LECTURER);
            loginInfoMapper.updateUserRole(updateMap);

            //2.3插入讲师分销信息表(统计冗余表)
            Map<String, Object> lecturerDistributionInfo = new HashMap<String, Object>();
            lecturerDistributionInfo.put("lecturer_id", reqMap.get("user_id"));
            lecturerDistributionInfo.put("create_time", now);
            lecturerDistributionInfo.put("update_time", now);
            lecturerDistributionInfoMapper.insertLecturerDistributionInfo(lecturerDistributionInfo);
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("room_id", reqMap.get("room_id"));
        return resultMap;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int insertShop(Map<String, Object> shop) {
        //店铺插入
        if(shopMapper.insert(shop)>0){
            //讲师信息插入
            Map<String,Object> lecturerInfo = new HashMap<>();
            lecturerInfo.put("lecturer_id",shop.get("lecturer_id"));
            lecturerInfo.put("create_time",new Date());
            lecturerInfo.put("shop_id",shop.get("shop_id"));
            return(lecturerMapper.insertLecture(lecturerInfo));
        }
        return 0;
    }

    @Override
    public int updateShop(Map<String, Object> param) {
        return shopMapper.updateByPrimaryKey(param);
    }

    @Override
    public Map<String, Object> findLectureByLectureId(String lecture_id) {
        return lecturerMapper.findLectureByLectureId(lecture_id);
    }

    @Override
    public Map<String, Object> getShopBannerList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_count").toString()));
        PageList<Map<String, Object>> result = shopBannerMapper.selectListByUserId(param, page);
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

        int upSize = shopBannerMapper.selectUpCount(param.get("lecturer_id").toString());

        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        res.put("up_count", upSize);
        return res;
    }

    @Override
    public int addShopBanner(Map<String, Object> param) {
        return shopBannerMapper.insert(param);
    }

    @Override
    public Map<String, Object> getShopBannerInfo(String bannerId) {
        return shopBannerMapper.selectByPrimaryKey(bannerId);
    }

    @Override
    public int updateBanner(Map<String, Object> param) {
        int i = shopBannerMapper.selectUpCount(param.get("user_id").toString());
        int size = (int)param.get("shopBannerSize");
        if (i >= size&& "1".equals(param.get("status")+"")) {
            return 0;
        }
        return shopBannerMapper.updateByPrimaryKey(param);
    }
    @Override
    public List<Map<String, Object>> getShopBannerListForFront(Map<String, Object> paramMap) {
        return shopBannerMapper.selectBannerListByMap(paramMap);
    }

    @Override
    public int addSingleCourse(Map<String, Object> reqMap) {
        return courseMapper.insertCourse(reqMap);
    }
    /**
     * 根据非空字段更新系列课详情
     */
    @Override
    public int updateSeriesByMap(Map<String, Object> updateSeriesMap) {
        return seriesMapper.updateSeries(updateSeriesMap);
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
        PageList<Map<String, Object>> result = shopFeedbackMapper.selectFeedBackByShop(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> getSeriesListByLecturerId(Map<String, Object> reqMap) {
        PageBounds page = new PageBounds(Integer.valueOf(reqMap.get("page_num").toString()), Integer.valueOf(reqMap.get("page_size").toString()));
        PageList<Map<String, Object>> result = seriesMapper.selectSeriesListByLecturerId(reqMap, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> getSeriesChildCourseList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result;
        if ("0".equals(param.get("series_course_type").toString())) {
            //直播课程
            param.put("goods_type","0");
        }
        result = courseMapper.findCourseBySeriesId(param, page);
        //是否是单卖判断
        result.stream().filter(map -> !"0".equals(map.get("is_single").toString())).forEach(map -> {
            //非单卖
            map.put("is_single", "1");
        });
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }


    @Override
    public Map<String, Object> findBannerUpCourseList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = courseMapper.selectUpCourseListByShopId(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> findBannerUpSeriesList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String, Object>> result = null;
        result = seriesMapper.findSeriesListByLiturere(param, page);
        Map<String, Object> res = new HashMap<>();
        res.put("list", result);
        res.put("total_count", result.getTotal());
        res.put("total_page", result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> findUserGainsByUserId(String userId) {
        Map<String, Object> result = userGainsMapper.findUserGainsByUserId(userId);
        //返回提现信息
        int count = withdrawCashMapper.selectWithdrawCountUser(userId);
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
        result.put("withdrawing_count",count);
        return result;
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

    @Override
    public List<Map<String,Object>> findCourseBySeriesId(String seriesId){
        return courseMapper.findAllCourseBySeriesId(seriesId);
    }

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
    public Map<String, Object> getLecturerImcome(String userId) {
        Map<String,Object> gainsInfo = userGainsMapper.findUserGainsByUserId(userId);
        if(gainsInfo!=null){
            int courseCount = courseMapper.selectCountByUserId(userId);
            int seriesCount = seriesMapper.selectCountByUserId(userId);
            Map<String,Object> resultMap = new HashMap<>();
            resultMap.put("course_count",courseCount);
            resultMap.put("series_count",seriesCount);
            resultMap.put("real_incomes",gainsInfo.get("user_total_real_incomes"));
            resultMap.put("total_incomes",gainsInfo.get("user_total_real_incomes"));
            return resultMap;
        }
        return null;
    }

    @Override
    public int deleteBanner(Map<String, Object> record) {
        return shopBannerMapper.deleteBanner(record);
    }

    @Override
    public Map<String, Object> getSingleList(Map<String, Object> param) {
        if ("0".equals(param.get("type"))) {
            //直播
            PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
            PageList<Map<String, Object>> result = courseMapper.findLiveListByLiturere(param, page);
            Map<String, Object> res = new HashMap<>();
            res.put("list", result);
            res.put("total_count", result.getTotal());
            res.put("total_page", result.getPaginator().getTotalPages());
            return res;
        } else {
            PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()), Integer.valueOf(param.get("page_size").toString()));
            PageList<Map<String, Object>> result = courseMapper.selectCourseByShopId(param, page);
            Map<String, Object> res = new HashMap<>();
            res.put("list", result);
            res.put("total_count", result.getTotal());
            res.put("total_page", result.getPaginator().getTotalPages());
            return res;
        }
    }

    @Override
    public Map<String, Object> findCourseByCourseId(String courseId) {
        return courseMapper.findCourseByCourseId(courseId);
    }

    @Override
    public List<Map<String, Object>> findCourse(Map<String, Object> record) {
        return null;
    }

    @Override
    public Map<String, Object> createCourse(Map<String, Object> reqMap) {
        reqMap.put(Constants.CACHED_KEY_COURSE_FIELD, MiscUtils.getUUId());
        if(reqMap.get("goods_type").toString().equals("0")){
            reqMap.put("status", "1");
        }
        courseMapper.insertCourse(reqMap);
        return null;
    }

    @Override
    public Map<String,Object> updateCourse(Map<String, Object> reqMap) {
        Integer updateCount = 0;
        Date now = (Date)reqMap.get("now");
        Map<String,Object> course = new HashMap<String,Object>();
        course.put("course_id", reqMap.get("course_id"));
        Object start_time = reqMap.get("live_start_time");
        if(!MiscUtils.isEmpty(start_time)){
            course.put("live_start_time", new Date(MiscUtils.convertObjectToLong(start_time)));
        }
        course.put("classify_id", reqMap.get("classify_id"));
        course.put("series_id", reqMap.get("series_id"));
        course.put("course_title", reqMap.get("course_title"));
        course.put("course_image", reqMap.get("course_image"));
        course.put("course_remark", reqMap.get("course_remark"));
        course.put("course_abstract", reqMap.get("course_abstract"));
        course.put("buy_tips", reqMap.get("buy_tips"));
        course.put("target_user", reqMap.get("target_user"));
        course.put("share_url", reqMap.get("share_url"));
        course.put("course_url", reqMap.get("course_url"));
        course.put("live_start_time", reqMap.get("live_start_time"));
        course.put("live_end_time", reqMap.get("live_end_time"));
        course.put("course_duration", reqMap.get("course_duration"));
        course.put("course_type", reqMap.get("course_type"));
        course.put("course_password", reqMap.get("course_password"));
        course.put("course_price", reqMap.get("course_price"));
        course.put("series_course_updown", reqMap.get("series_course_updown"));
        course.put("course_updown", reqMap.get("course_updown"));
        course.put("updown_time", reqMap.get("updown_time"));
        course.put("shelves_sharing", reqMap.get("shelves_sharing"));
        course.put("details", reqMap.get("details"));
        course.put("update_time", now);
        updateCount=courseMapper.updateCourse(course);

        Map<String, Object> dbResultMap = new HashMap<String, Object>();
        dbResultMap.put("update_count", updateCount);
        dbResultMap.put("update_time", now);
        return dbResultMap;
    }

    @Override
    public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
        return liveRoomMapper.findLiveRoomByLectureId(lecture_id);
    }

    @Override
    public Map<String, Object> findUserInfoByUserId(String userId) {
        return userMapper.findByUserId(userId);
    }

    @Override
    public List<Map<String, Object>> findSeriesStudentByMap(Map<String, Object> param) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findCourseStudentByMap(Map<String, Object> param) {
        return coursesStudentsMapper.selectCourseStudentByMap(param);
    }

    //根据union查找登录信息
    @Override
    public Map<String, Object> getLoginInfoByLoginId(String unionID) {
        return loginInfoMapper.getLoginInfoByLoginId(unionID);
    }


    @Override
    public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
        return coursesStudentsMapper.findCourseIdByStudent(reqMap);
    }

    @Override
    public List<Map<String, Object>> findShopIdByFans(Map<String, Object> reqMap) {
        return fansMapper.findShopIdByFans(reqMap);
    }

    @Override
    public Map<String, Object> findSeriesBySeriesId(String series_id) {
        return seriesMapper.findSeriesBySeriesId(series_id);
    }

    @Override
    public List<Map<String, Object>> findSeriesIdByStudent(Map<String, Object> reqMap) {
        return seriesStudentsMapper.findSeriesIdByStudent(reqMap);
    }

    @Override
    public Map<String, Object> getShopInfo(String id) {
        return shopMapper.selectByPrimaryKey(id);
    }

    /**
     * 根据系列id获得该系列的所有学员列表
     */
    @Override
    public List<Map<String, Object>> findSeriesStudentListBySeriesId(String seriesId) {
        Map<String, Object> selectMap = new HashMap<>();
        selectMap.put("series_id", seriesId);
        return seriesStudentsMapper.selectSeriesStudentsByMap(selectMap);
    }

    /**
     * 根据条件获取嘉宾课程列表，并关联查询出课程详情
     */
    @Override
    public List<Map<String, Object>> getGuestAndCourseInfoByMap(Map<String, Object> reqMap) {
        return courseGuestMapper.findGuestAndCourseInfoByMap(reqMap);
    }

    @Override
    public List<Map<String, Object>> findSystemConfig() {
        return systemConfigMapper.findSystemConfig();
    }
}
