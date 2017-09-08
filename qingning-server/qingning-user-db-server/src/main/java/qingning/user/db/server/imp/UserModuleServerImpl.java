
package qingning.user.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import org.springframework.transaction.annotation.Transactional;
import qingning.common.entity.QNLiveException;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.IUserModuleServer;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserModuleServerImpl implements IUserModuleServer {

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
    private DistributerMapper distributerMapper;
    @Autowired(required = true)
    private RoomDistributerMapper roomDistributerMapper;

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
    private SaaSShopMapper shopMapper;

    @Autowired(required = true)
    private TradeBillMapper tradeBillMapper;

    @Autowired(required = true)
    private SaaSCourseMapper saasCourseMapper;

    @Override
    public Map<String, Object> userFollowRoom(Map<String, Object> reqMap) throws Exception {
        Map<String, Object> dbResultMap = new HashMap<>();
        //follow_type 关注操作类型 1关注 0不关注
        Date now = new Date();
        if ("1".equals(reqMap.get("follow_type"))) {
            try {
                Map<String, Object> fans = new HashMap<String, Object>();
                fans.put("fans_id", MiscUtils.getUUId());
                fans.put("user_id", reqMap.get("user_id"));
                fans.put("lecturer_id", reqMap.get("lecturer_id"));
                fans.put("room_id", reqMap.get("room_id"));
                fans.put("create_time", now);
                fans.put("create_date", now);
                Integer updateCount = fansMapper.insertFans(fans);
                dbResultMap.put("update_count", updateCount);
                return dbResultMap;
            } catch (Exception e) {
                if (e instanceof DuplicateKeyException) {
                    throw new QNLiveException("110005");
                } else {
                    throw e;
                }
            }
        } else {
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("user_id", reqMap.get("user_id").toString());
            updateMap.put("room_id", reqMap.get("room_id").toString());
            Integer updateCount = fansMapper.deleteFans(updateMap);
            dbResultMap.put("update_count", updateCount);
            return dbResultMap;
        }
    }

    @Override
    public void updateLiveRoomNumForUser(Map<String, Object> reqMap) {
        userMapper.updateLiveRoomNumForUser(reqMap);
    }

    @Override
    public Map<String, Object> findUserInfoByUserId(String user_id) {
        return userMapper.findByUserId(user_id);
    }

    @Override
    public Map<String, Object> findLiveRoomByRoomId(String room_id) {
        return liveRoomMapper.findLiveRoomByRoomId(room_id);
    }

    @Override
    public Map<String, Object> findFansByUserIdAndRoomId(Map<String, Object> reqMap) {
        Map<String, Object> fansKey = new HashMap<>();
        fansKey.put("user_id", reqMap.get("user_id").toString());
        fansKey.put("room_id", reqMap.get("room_id").toString());
        return fansMapper.findFansByUserIdAndRoomId(fansKey);
    }

    @Override
    public Map<String, Object> findCourseByCourseId(String string) {
        return coursesMapper.findCourseByCourseId(string);
    }

    @Override
    public List<Map<String, Object>> findCourseListForLecturer(Map<String, Object> queryMap) {
        return coursesMapper.findCourseListForLecturer(queryMap);
    }

    @Override
    public Map<String, Object> joinCourse(Map<String, String> courseMap) {
        Date now = new Date();
        Map<String, Object> student = new HashMap<String, Object>();
        student.put("student_id", MiscUtils.getUUId());
        student.put("user_id", courseMap.get("user_id"));
        student.put("lecturer_id", courseMap.get("lecturer_id"));
        student.put("room_id", courseMap.get("room_id"));
        student.put("course_id", courseMap.get("course_id"));
        student.put("value_from", courseMap.get("value_from"));
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
        coursesMapper.increaseStudent(course_id);
    }

    @Override
    public List<Map<String, Object>> findPPTListByCourseId(String course_id) {
        return courseImageMapper.findPPTListByCourseId(course_id);
    }

    @Override
    public List<Map<String, Object>> findAudioListByCourseId(String course_id) {
        return courseAudioMapper.findAudioListByCourseId(course_id);
    }

    @Override
    public List<Map<String, Object>> findRewardConfigurationList() {
        return rewardConfigurationMapper.findRewardConfigurationList();
    }

    @Override
    public List<Map<String, Object>> findCourseMessageList(Map<String, Object> queryMap) {
        return courseMessageMapper.findCourseMessageList(queryMap);
    }

    @Override
    public List<Map<String, Object>> findCourseStudentList(Map<String, Object> queryMap) {
        return coursesStudentsMapper.findCourseStudentList(queryMap);
    }

    @Override
    public Map<String, Object> findLoginInfoByUserId(String user_id) {
        return loginInfoMapper.findLoginInfoByUserId(user_id);
    }

    @Override
    public Map<String, Object> findCourseMessageMaxPos(String course_id) {
        return courseMessageMapper.findCourseMessageMaxPos(course_id);
    }

    @Override
    public List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap) {
        return coursesStudentsMapper.findLatestStudentAvatarAddList(queryMap);
    }

    @Override
    public List<Map<String, Object>> findFanInfoByUserId(Map<String, Object> queryMap) {
        return fansMapper.findFanInfoByUserId(queryMap);
    }

    @Override
    public List<Map<String, Object>> findCourseListOfStudent(Map<String, Object> queryMap) {
        return coursesStudentsMapper.findCourseListOfStudent(queryMap);
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
    public boolean findUserWhetherToPay(Map<String, Object> record) {
        return  !MiscUtils.isEmpty(tradeBillMapper.findUserWhetherToPay(record)) ;
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


    @Override
    public List<Map<String, Object>> findDistributionInfoByDistributerId(Map<String, Object> queryMap) {
        return distributerMapper.findDistributionInfoByDistributerId(queryMap);
    }

    @Override
    public Map<String, Object> findAvailableRoomDistributer(Map<String, Object> queryMap) {
        return roomDistributerMapper.findRoomDistributer(queryMap);
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
    public List<Map<String, Object>> findFinishCourseListForLecturer(Map<String, Object> record) {
        return coursesMapper.findFinishCourseListForLecturer(record);
    }

    @Override
    public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
        return coursesStudentsMapper.findCourseIdByStudent(reqMap);
    }

    @Override
    public Map<String, Object> findCourseRecommendUserNum(Map<String, Object> reqMap) {
        return coursesStudentsMapper.findCourseRecommendUserNum(reqMap);
    }

    @Override
    public Map<String, Object> findCustomerServiceBySystemConfig(Map<String, Object> reqMap) {
        return systemConfigMapper.findCustomerServiceBySystemConfig(reqMap);
    }

    @Override
    public List<Map<String, Object>> findRoomIdByFans(Map<String, Object> reqMap) {
        return fansMapper.findRoomIdByFans(reqMap);
    }


    /**
     * 获取没有t_user_gains记录的user_id
     */
    @Override
    public List<Map<String,Object>> findNotGainsUserId(int page_num, int page_count) {
        List<Map<String,Object>> result = userMapper.selectNotGainsUserId(page_num,page_count);
        return result;
    }

    /**
     * 获得userId列表里用户的直播间收益
     */
    @Override
    public List<Map<String, Object>> findRoomAmount(List<String> userIdList) {
        return liveRoomMapper.selectRoomAmount(userIdList);
    }

    /**
     * 获得userId列表里分销员总收益
     */
    @Override
    public List<Map<String, Object>> findDistributerAmount(List<String> userIdList) {
        return distributerMapper.selectDistributerAmount(userIdList);
    }

    /**
     * 获取用户提现成功总金额
     */
    @Override
    public List<Map<String, Object>> findUserWithdrawSum(List<String> userIdList) {
        List<Map<String, Object>> result = withdrawCashMapper.selectUserWithdrawSum(userIdList);
        return result;
    }

    @Override
    public void insertUserGains(List<Map<String, Object>> list) {
        userGainsMapper.insertUserGains(list);
    }

    @Override
    public void insertUserGainsByNewUser(Map<String, Object> reqMap) {
        userGainsMapper.insertUserGainsByNewUser(reqMap);
    }

    @Override
    public void updateUserGains(Map<String, Object> reqMap) {
        userGainsMapper.updateUserGains(reqMap);
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
    public int insertWithdrawCash(Map<String, Object> record, int balance) {
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

    @Override
    public Map<String, Object> selectWithdrawSizeById(Map<String, Object> selectMap) {
        return withdrawCashMapper.selectWithdrawSizeById(selectMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWithdraw(String withdrawId,String remark,String userId,String result,Long initial_amount) {
        //更新提现记录
        Map<String,Object> paramMap = new HashMap<>();
        if("1".equals(result)){
            //同意提现
            paramMap.put("state",1);
        }else{
            //驳回提现
            paramMap.put("state",2);
            //返还用户余额
            Map<String ,Object> user = userGainsMapper.findUserGainsByUserId(userId);
            long balance = Integer.valueOf(user.get("balance").toString());
            balance = balance + initial_amount;
            Map<String ,Object> reqMap = new HashMap<>();
            reqMap.put("user_id",userId);
            reqMap.put("balance",balance);
            userGainsMapper.updateUserGains(reqMap);
        }
        paramMap.put("withdraw_cash_id",withdrawId);
        paramMap.put("update_time",new Date());
        paramMap.put("remark",remark);
        
        paramMap.put("handle_id",userId);
        int i = withdrawCashMapper.updateWithdrawCash(paramMap);
        return i;
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
        res.put("user_list",result);
        res.put("total_count",result.getTotal());
        res.put("total_page",result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> findSeriesBySeriesId(String series_id) {
        return seriesMapper.findSeriesBySeriesId(series_id);
    }

    @Override
    public boolean isStudentOfTheSeries(Map<String, Object> queryMap) {
        return !MiscUtils.isEmpty( seriesStudentsMapper.isStudentOfTheSeries(queryMap));
    }

    @Override
    public Map<String, Object> joinSeries(Map<String, String> seriesMap) {
        Date now = new Date();
        Map<String, Object> student = new HashMap<String, Object>();
        student.put("student_id", MiscUtils.getUUId());
        student.put("user_id", seriesMap.get("user_id"));
        student.put("lecturer_id", seriesMap.get("lecturer_id"));
        student.put("series_id", seriesMap.get("series_id"));
        student.put("student_type", "0"); //TODO distribution case
        student.put("value_from", seriesMap.get("value_from"));
        student.put("create_time", now);
        student.put("create_date", now);
        //students.setPaymentAmount();//TODO
        Integer insertCount = seriesStudentsMapper.insertStudent(student);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("insertCount", insertCount);
        return resultMap;
    }

    @Override
    public Map<String, Object> findSeriesRecommendUserNum(Map<String, Object> reqMap) {
        return seriesStudentsMapper.findSeriesRecommendUserNum(reqMap);
    }

    @Override
    public void increaseStudentNumBySeriesId(String series_id) {
        seriesMapper.increaseSeriesStudent(series_id);
    }


    @Override
    public List<Map<String, Object>> findSeriesIdByStudent(Map<String, Object> reqMap) {
        return seriesStudentsMapper.findSeriesIdByStudent(reqMap);
    }

    @Override
    public Map<String, Object> findUserOrderByCourse(Map<String, Object> queryTradeMap) {
        return tradeBillMapper.findUserOrderByCourse(queryTradeMap);
    }

    @Override
    public Map<String, Object> getShopInfo(Map<String, Object> param) {
        return shopMapper.selectByPrimaryKey(param);
    }

    @Override
    public Map<String, Object> findSaasCourseByCourseId(String courseId) {
        return saasCourseMapper.selectByPrimaryKey(courseId);
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
}
