
package qingning.user.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.transaction.annotation.Transactional;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.IUserUserModuleServer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserUserModuleServerImpl implements IUserUserModuleServer {

    @Autowired(required = true)
    private FansMapper fansMapper;
    @Autowired(required = true)
    private UserMapper userMapper;
    @Autowired(required = true)
    private LiveRoomMapper liveRoomMapper;
    @Autowired(required = true)
    private CoursesMapper coursesMapper;
    @Autowired(required = true)
    private CoursesStudentsMapper coursesStudentsMapper;
    @Autowired(required = true)
    private CourseImageMapper courseImageMapper;
    @Autowired(required = true)
    private CourseAudioMapper courseAudioMapper;
    @Autowired(required = true)
    private RewardConfigurationMapper rewardConfigurationMapper;
    @Autowired(required = true)
    private CourseMessageMapper courseMessageMapper;
    @Autowired(required = true)
    private LoginInfoMapper loginInfoMapper;
    @Autowired(required = true)
    private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;
    @Autowired(required = true)
    private LecturerMapper lecturerMapper;
    @Autowired(required = true)
    private SystemConfigMapper systemConfigMapper;
    @Autowired(required = true)
    private UserGainsMapper userGainsMapper;
    @Autowired(required = true)
    private WithdrawCashMapper withdrawCashMapper;
    @Autowired(required = true)
    private SeriesMapper seriesMapper;
    @Autowired(required = true)
    private SeriesStudentsMapper seriesStudentsMapper;
    @Autowired(required = true)
    private ShopMapper shopMapper;
    @Autowired(required = true)
    private TradeBillMapper tradeBillMapper;
    @Autowired(required = true)
    private SaaSCourseMapper saasCourseMapper;
    @Autowired(required = true)
    private AdminUserMapper adminUserMapper;

    @Autowired(required = true)
    private CourseMapper courseMapper;

    @Override
    public Map<String, Object> findUserInfoByUserId(String user_id) {
        return userMapper.findByUserId(user_id);
    }

    @Override
    public Map<String, Object> findCourseByCourseId(String string) {
        return coursesMapper.findCourseByCourseId(string);
    }

    @Override
    public List<Map<String, Object>> findCourse(Map<String, Object> record) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findRewardConfigurationList() {
        return rewardConfigurationMapper.findRewardConfigurationList();
    }

    @Override
    public boolean isStudentOfTheCourse(Map<String, Object> studentQueryMap) {
        return !MiscUtils.isEmpty(coursesStudentsMapper.isStudentOfTheCourse(studentQueryMap));
    }

    @Override
    public Map<String, Object> findLectureByLectureId(String lecture_id) {
        return lecturerMapper.findLectureByLectureId(lecture_id);
    }

    @Override
    public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
        return coursesStudentsMapper.findCourseIdByStudent(reqMap);
    }

    @Override
    public Map<String, Object> getLoginInfoByLoginId(String unionID) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findShopIdByFans(Map<String, Object> reqMap) {
        return fansMapper.findShopIdByFans(reqMap);
    }

    @Override
    public List<Map<String, Object>> findSeriesStudentListBySeriesId(String seriesId) {
        return null;
    }

    @Override
    public Map<String, Object> findSeriesBySeriesId(String series_id) {
        return seriesMapper.findSeriesBySeriesId(series_id);
    }

    @Override
    public List<Map<String, Object>> findSeriesIdByStudent(Map<String, Object> reqMap) {
        return seriesStudentsMapper.findSeriesIdByStudent(reqMap);
    }

    /**
     * 获取用户加入的单品课程列表
     */
	@Override
	public List<Map<String, Object>> findCourseStudentByMap(Map<String, Object> selectCourseStudentMap) {
		return coursesStudentsMapper.selectCourseStudentByMap(selectCourseStudentMap);
	}
	/**
	 * 获取用户加入的系列列表
	 */
	@Override
	public List<Map<String, Object>> findSeriesStudentByMap(Map<String, Object> reqMap) {
		return seriesStudentsMapper.selectSeriesStudentsByMap(reqMap);
	}

    //TODO 实现缓存方法
    @Override
    public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
        return null;
    }
    //TODO 实现缓存方法
    @Override
    public Map<String, Object> getShopInfo(String shopId) {
        return shopMapper.selectByPrimaryKey(shopId);
    }
    //TODO 实现缓存方法
    @Override
    public List<Map<String, Object>> getGuestAndCourseInfoByMap(Map<String, Object> reqMap) {
        return null;
    }

    @Override
    public List<Map<String, Object>> findSystemConfig() {
        return systemConfigMapper.findSystemConfig();
    }

    /**
     * 查看订单
     * @param record
     * Created by DavidGong on 2017/7/4.
     * @return
     */
    @Override
    public boolean findUserWhetherToPay(Map<String, Object> record) {
        return  !MiscUtils.isEmpty(tradeBillMapper.findUserWhetherToPay(record)) ;
    }

    /**
     * 加入课程
     * @param courseMap
     * @return
     */
    @Override
    public Map<String, Object> joinCourse(Map<String, String> courseMap) {
        Date now = new Date();
        Map<String, Object> student = new HashMap<String, Object>();
        student.put("student_id", MiscUtils.getUUId());
        student.put("user_id", courseMap.get("user_id"));
        student.put("distributer_id", courseMap.get("distributer_id"));
        student.put("lecturer_id", courseMap.get("lecturer_id"));
        student.put("shop_id", courseMap.get("shop_id"));
        student.put("course_id", courseMap.get("course_id"));
        student.put("payment_amount", courseMap.get("payment_amount"));
        student.put("course_password", courseMap.get("course_password"));
        student.put("student_type", "0"); //TODO distribution case
        student.put("create_time", now);
        student.put("create_date", now);
        //students.setPaymentAmount();//TODO
        Integer insertCount = coursesStudentsMapper.insertStudent(student);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("insertCount", insertCount);
        return resultMap;
    }

    @Override
    public void increaseStudentNumByCourseId(String course_id) {
        courseMapper.increaseStudent(course_id);
    }


    @Override
    public List<Map<String, Object>> findUserShopRecords(Map<String, Object> queryMap) {
        String shopId = queryMap.get("shop_id").toString();
        String shopUser = shopMapper.selectUserIdByShopId(shopId);	//店主的user_id
        //店铺不存在返回空
        if(shopUser==null){
            return null;
        }
        queryMap.put("lecturer_id",shopUser);
        List<Map<String, Object>> res = lecturerCoursesProfitMapper.findUserConsumeRecords(queryMap);
        res.stream().filter(map -> map.get("series_title") != null).forEach(map -> {
            map.put("course_title", map.get("series_title"));
        });
        return res;
    }

    @Override
    public List<Map<String, Object>> findUserConsumeRecords(Map<String, Object> queryMap) {
        List<Map<String, Object>> res;
        if("2".equals(queryMap.get("type"))){
            //所有收入明细
            res = lecturerCoursesProfitMapper.findUserIncomeRecords(queryMap);
        }else{
            //系列记录转换
            res = lecturerCoursesProfitMapper.findUserConsumeRecords(queryMap);
        }
        res.stream().filter(map -> map.get("series_title") != null).forEach(map -> {
            map.put("course_title", map.get("series_title"));
        });
        return res;
    }

    @Override
    public Map<String, Object> findUserGainsByUserId(String user_id) {
        return userGainsMapper.findUserGainsByUserId(user_id);
    }

    /**
     * 获得符合条件的首条提现记录
     */
    @Override
    public Map<String, Object> findWithdrawCashByMap(Map<String, Object> selectMap) {
        List<Map<String, Object>> resultList = withdrawCashMapper.findWithdrawCashByUser(selectMap);
        if (resultList != null && !resultList.isEmpty()) {
            return resultList.get(0);
        } else {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWithdrawCash(Map<String, Object> record, long balance) {
        int i = withdrawCashMapper.insertWithdrawCashByNewUser(record);
        if(i>0) {
            Map<String ,Object> withdraw = new HashMap<>();
            withdraw.put("user_id",record.get("user_id"));
            withdraw.put("balance",balance);
            if(userGainsMapper.updateUserGains(withdraw)<1){
                throw new RuntimeException();
            }
        }
        return i;
    }

    @Override
    public List<Map<String, Object>> findWithdrawList(Map<String, Object> param) {
        return withdrawCashMapper.selectWithdrawList(param);
    }
    /**
     * 分页查询-后台提现记录
     */
    @Override
    public Map<String, Object> findWithdrawListAll(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()),Integer.valueOf(param.get("page_count").toString()));
        PageList<Map<String,Object>> result = withdrawCashMapper.selectWithdrawListAll(param,page);
        //提现实际提现金额
        for(Map<String,Object> map:result){
            long initial_amount = (Long)map.get("initial_amount");
            long actual_amount = (Long)map.get("actual_amount");
            long filter_amount = initial_amount - actual_amount;
            map.put("filter_amount",filter_amount);

        }
        Map<String,Object> res = new HashMap<>();
        if(param.get("is_sys")!=null&&"1".equals(param.get("is_sys").toString())){
            //查询未处理的条数
            int i = 0;
            if(param.get("finance")!=null&&"1".equals(param.get("finance"))){
                //财务未处理数
                i = withdrawCashMapper.selectWithdrawCountFinance(param);
            }else{
                //运营未处理数
                i = withdrawCashMapper.selectWithdrawCountOperate(param);
            }
            res.put("undo_count",i);
        }
        res.put("user_list",result);
        res.put("total_count",result.getTotal());
        res.put("total_page",result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> selectAdminUserById(String userId) {

        return adminUserMapper.selectAdminUserById(userId);
    }

    @Override
    public Map<String, Object> selectWithdrawSizeById(Map<String, Object> selectMap) {
        return withdrawCashMapper.selectWithdrawSizeById(selectMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWithdraw(String withdrawId, String remark, String userId, String result, Long initial_amount, String adminId, String role, String adminName) {
        //更新提现记录
        Map<String,Object> paramMap = new HashMap<>();
        //通过
        boolean pass = "1".equals(result);
        //财务通过
        boolean allPass = pass&&("3".equals(role)||"1".equals(role));
        if(allPass){
            //财务同意提现
            paramMap.put("state",1);
            paramMap.put("finance_update_time",new Date());
            paramMap.put("finance_admin_id",userId);
            paramMap.put("finance_admin_name",adminName);
        }else if(pass){
            //运营同意提现---进行中
            paramMap.put("state",0);
            paramMap.put("update_time",new Date());
            paramMap.put("handle_id",userId);
            paramMap.put("handle_name",adminName);
        }else{
            //驳回提现
            paramMap.put("state",2);
            //返还用户余额
            Map<String ,Object> user = userGainsMapper.findUserGainsByUserId(userId);
            long balance = Integer.valueOf(user.get("balance").toString());
            Map<String ,Object> reqMap = new HashMap<>();
            reqMap.put("user_id",userId);
            reqMap.put("balance",new BigDecimal(balance).add(new BigDecimal(initial_amount)).longValue());
            userGainsMapper.updateUserGains(reqMap);
        }
        paramMap.put("withdraw_cash_id",withdrawId);
        paramMap.put("remark",remark);
        int i = withdrawCashMapper.updateWithdrawCash(paramMap);
        return i;
    }

    @Override
    public Map<String, Object> findOrderListAll(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()),Integer.valueOf(param.get("page_count").toString()));
        PageList<Map<String,Object>> result = lecturerCoursesProfitMapper.selectOrderListAll(param,page);
        Map<String,Object> res = new HashMap<>();
        for(Map<String,Object> map: result){
            if("1".equals(map.get("is_dist").toString())){
                //分销者
                map.put("profit_type","3");
                map.put("user_amount",map.get("share_amount"));
                map.remove("distributer_id");
            }else if(!"0".equals(map.get("share_amount").toString())){
                //讲师分销收益
                map.put("profit_type","2");
                Long total = Long.valueOf(map.get("amount").toString());
                Long share = Long.valueOf(map.get("share_amount").toString());
                map.put("user_amount",total-share);
            }else if("2".equals(map.get("profit_type").toString())){
                //门票
                map.put("profit_type","0");
                map.put("user_amount",map.get("amount"));
            }else{
                map.put("user_amount",map.get("amount"));
            }
        }
        res.put("list",result);
        res.put("total_count",result.getTotal());
        res.put("total_page",result.getPaginator().getTotalPages());
        return res;
    }
}
