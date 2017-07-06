
package qingning.common.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.entity.QNLiveException;
import qingning.common.util.Constants;
import qingning.common.util.CountMoneyUtil;
import qingning.common.util.DoubleUtil;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.*;

public class CommonModuleServerImpl implements ICommonModuleServer {

	@Autowired(required = true)
	private ServerFunctionMapper serverFunctionMapper;

	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;

	@Autowired(required = true)
	private UserMapper userMapper;

	@Autowired(required = true)
	private CoursesMapper coursesMapper;

	@Autowired(required = true)
	private RewardConfigurationMapper rewardConfigurationMapper;

	@Autowired(required = true)
	private TradeBillMapper tradeBillMapper;

	@Autowired(required = true)
	private PaymentBillMapper paymentBillMapper;

	@Autowired(required = true)
	private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;

	@Autowired(required = true)
	private CoursesStudentsMapper coursesStudentsMapper;

	@Autowired(required = true)
	private DistributerMapper distributerMapper;

	@Autowired(required = true)
	private RoomDistributerMapper roomDistributerMapper;

	@Autowired(required = true)
	private LiveRoomMapper liveRoomMapper;

	@Autowired(required = true)
	private LecturerMapper lecturerMapper;

	@Autowired(required = true)
	private RoomDistributerRecommendMapper roomDistributerRecommendMapper;

	@Autowired(required = true)
	private FeedbackMapper feedbackMapper;

	@Autowired(required = true)
	private VersionMapper versionMapper;

	@Autowired(required = true)
	private RoomDistributerRecommendDetailMapper roomDistributerRecommendDetailMapper;

	@Autowired(required = true)
	private RoomDistributerDetailsMapper roomDistributerDetailsMapper;

	@Autowired(required = true)
	private RoomDistributerCoursesMapper roomDistributerCoursesMapper;

	@Autowired(required = true)
	private CourseMessageMapper courseMessageMapper;

	@Autowired(required = true)
	private CourseImageMapper courseImageMapper;

	@Autowired(required = true)
	private CourseAudioMapper courseAudioMapper;

	@Autowired(required = true)
	private FansMapper fansMapper;

	@Autowired(required = true)
	private ClassifyInfoMapper classifyInfoMapper;

	@Autowired(required = true)
	private BannerInfoMapper bannerInfoMapper;

	@Autowired(required = true)
	private SystemConfigMapper systemConfigMapper;
	@Autowired(required = true)
	private UserGainsMapper userGainsMapper;
	@Autowired(required = true)
	private WithdrawCashMapper withdrawCashMapper;
	@Autowired(required = true)
	private AdminUserMapper adminUserMapper;
	@Autowired(required = true)
	private SeriesMapper seriesMapper;
	@Autowired(required = true)
	private SaaSShopMapper shopMapper;
	@Autowired(required = true)
	private SaaSShopUserMapper shopUserMapper;

	@Autowired(required = true)
	private SeriesStudentsMapper seriesStudentsMapper;

	@Autowired(required = true)
	private SaaSCourseMapper saaSCourseMapper;

	@Override
	public List<Map<String, Object>> getServerUrls() {
		return serverFunctionMapper.getServerUrls();
	}

	@Override
	public List<Map<String, Object>> getServerUrlByAppName(String appName) {
		return serverFunctionMapper.getServerUrlByAppName(appName);
	}

	@Override
	public Map<String, Object> getLoginInfoByLoginIdAndLoginType(Map<String, Object> reqMap) {
		return loginInfoMapper.getLoginInfoByLoginIdAndLoginType(reqMap);
	}

	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String, String> initializeRegisterUser(Map<String, Object> reqMap) {
		//1.插入t_user		
		Date now = new Date();
		Map<String,Object> user = new HashMap<String,Object>();
		String uuid = MiscUtils.getUUId();
		user.put("user_id", uuid);
		user.put("user_name", reqMap.get("user_name"));
		user.put("nick_name", reqMap.get("nick_name"));
		user.put("avatar_address", reqMap.get("avatar_address"));
		user.put("phone_number", reqMap.get("phone_number"));
		user.put("gender", reqMap.get("gender"));
		user.put("create_time", now);
		user.put("update_time", now);
		user.put("app_name",reqMap.get("app_name"));
		user.put("user_role", Constants.USER_ROLE_LISTENER);
		//位置信息未插入由消息服务处理
		userMapper.insertUser(user);
		//2.插入login_info
		Map<String,Object> loginInfo = new HashMap<String,Object>();
		loginInfo.put("user_id", uuid);
		String login_type = (String)reqMap.get("login_type");
		if("0".equals(login_type)){
			loginInfo.put("union_id", reqMap.get("login_id"));
		} else if("1".equals(login_type)){

		} else if("2".equals(login_type)){

		} else if("3".equals(login_type)){
			loginInfo.put("passwd",reqMap.get("certification"));
		} else if("4".equals(login_type)){
			loginInfo.put("union_id",reqMap.get("unionid"));
			loginInfo.put("web_openid",reqMap.get("web_openid"));
		}

		loginInfo.put("app_name",reqMap.get("app_name"));
		loginInfo.put("phone_number", reqMap.get("phone_number"));
		loginInfo.put("m_user_id", reqMap.get("m_user_id"));
		loginInfo.put("m_pwd", reqMap.get("m_pwd"));
		loginInfo.put("user_role", Constants.USER_ROLE_LISTENER);
		//位置信息未插入由消息服务处理
		loginInfo.put("create_time", now);
		user.put("subscribe",reqMap.get("subscribe"));
		loginInfo.put("update_time", now);
		loginInfoMapper.insertLoginInfo(loginInfo);

		//初始化用户余额信息
		Map<String,Object> gainsMap = new HashMap<>();
		gainsMap.put("user_id",uuid);
		gainsMap.put("live_room_total_amount","0");
		gainsMap.put("live_room_real_incomes","0");
		gainsMap.put("distributer_total_amount","0");
		gainsMap.put("distributer_real_incomes","0");
		gainsMap.put("user_total_amount","0");
		gainsMap.put("user_total_real_incomes","0");
		gainsMap.put("balance","0");
		userGainsMapper.insertUserGainsByNewUser(gainsMap);

		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("user_id", uuid);
		return resultMap;
	}

	@Override
	public Map<String, Object> findLoginInfoByUserId(String user_id) {
		return loginInfoMapper.findLoginInfoByUserId(user_id);
	}

	@Override
	public Map<String, Object> findUserInfoByUserId(String user_id) {
		return userMapper.findByUserId(user_id);
	}

	@Override
	public Map<String, Object> findCourseByCourseId(String courseId) {
		return coursesMapper.findCourseByCourseId(courseId);
	}

	@Override
	public Map<String, Object> findRewardInfoByRewardId(String reward_id) {
		return rewardConfigurationMapper.findRewardInfoByRewardId(MiscUtils.convertObjectToLong(reward_id));
	}

	@Override
	public void insertTradeBill(Map<String, Object> insertMap) {
		//插入t_trade_bill表
		Date now = new Date();
		insertMap.put("create_time", now);
		insertMap.put("update_time", now);

		tradeBillMapper.insertTradeBill(insertMap);
	}

	@Override
	public void closeTradeBill(Map<String, Object> failUpdateMap) {
		Date now = new Date();
		Map<String,Object> record = new HashMap<String,Object>();
		record.put("trade_id", failUpdateMap.get("trade_id"));
		record.put("status", failUpdateMap.get("status"));
		record.put("close_reason", failUpdateMap.get("close_reason"));
		record.put("update_time", now);
		record.put("close_time", now);
		record.put("trade_id", failUpdateMap.get("trade_id"));

		tradeBillMapper.updateTradeBill(record);
	}

	@Override
	public void insertPaymentBill(Map<String, Object> insertPayMap) {
		paymentBillMapper.insertPaymentBill(insertPayMap);
	}

	@Override
	public boolean isTradebillFinish(String outTradeNo) {
		Map<String,Object> value = tradeBillMapper.findByOutTradeNo(outTradeNo);
		//交易状态，0：待付款 1：处理中 2：已完成 3：已关闭
		if(value != null && "2".equals(value.get("status"))){
			return true;
		}else {
			return false;
		}
	}

	@Override
	public void updateUserWebOpenIdByUserId(Map<String, Object> updateMap) {
		Map<String,Object> record = new HashMap<String,Object>();
		record.put("user_id", updateMap.get("user_id"));
		record.put("web_openid",updateMap.get("web_openid"));
		record.put("subscribe",updateMap.get("subscribe").toString());
		loginInfoMapper.updateLoginInfo(record);
	}

	@Override
	public Map<String,Object> findTradebillByOutTradeNo(String outTradeNo) {
		Map<String,Object> tradeBill = tradeBillMapper.findByOutTradeNo(outTradeNo);
		return tradeBill;
	}

	@Override
	public Map<String,Object> findTradeBillByPaymentid(String pre_pay_no) {
		Map<String,Object> tradeBill = paymentBillMapper.findTradeBillByPaymentid(pre_pay_no);
		return tradeBill;
	}

	@SuppressWarnings("unchecked")
	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String, Object> handleWeixinPayResult(Map<String, Object> requestMapData) throws Exception{

		Date now = new Date();
		Map<String,Object> tradeBill = (Map<String,Object>)requestMapData.get("tradeBillInCache");
		Map<String,Object> updateTradeBill = new HashMap<String,Object>();
		//1.更新t_trade_bill 交易信息表
		updateTradeBill.put("trade_id", requestMapData.get("out_trade_no"));
		updateTradeBill.put("status", "2");//交易状态，0：待付款 1：处理中 2：已完成 3：已关闭
		updateTradeBill.put("update_time", now);
		if(tradeBillMapper.updateTradeBill(updateTradeBill) < 1){
			throw new QNLiveException("000105");
		}
		//2.更新t_payment_bill 支付信息表
		Map<String,Object> paymentBill = paymentBillMapper.findPaymentBillByTradeId((String)requestMapData.get("out_trade_no"));
		String status = (String)paymentBill.get("status");
		if("2".equals(status)){
			throw new QNLiveException("000105");
		}
		String profitId = MiscUtils.getUUId();
		Map<String,Object> updatePaymentBill = new HashMap<String,Object>();
		updatePaymentBill.put("status", "2");
		Date realPayTime = new Date(MiscUtils.convertObjectToLong(requestMapData.get("time_end")));
		updatePaymentBill.put("update_time", realPayTime);
		updatePaymentBill.put("payment_id", paymentBill.get("payment_id"));
		updatePaymentBill.put("trade_id", paymentBill.get("trade_id"));
		updatePaymentBill.put("profit_id", profitId);
		if(paymentBillMapper.updatePaymentBill(updatePaymentBill) < 1){
			throw new QNLiveException("000105");
		}
		//支付金额
		long amount = Long.valueOf(tradeBill.get("amount").toString());
		String userId = tradeBill.get("user_id").toString();
		//3.更新 讲师课程收益信息表
		Map<String,Object> profitRecord = new HashMap<String,Object>();
		profitRecord.put("profit_id", profitId);
		profitRecord.put("course_id", tradeBill.get("course_id"));
		profitRecord.put("room_id", tradeBill.get("room_id"));
		profitRecord.put("user_id", userId);
		profitRecord.put("profit_amount", amount);
		profitRecord.put("profit_type", tradeBill.get("profit_type"));
		profitRecord.put("create_time", now);
		profitRecord.put("create_date", now);
		profitRecord.put("payment_id", paymentBill.get("payment_id"));
		profitRecord.put("payment_type", paymentBill.get("payment_type"));

		Map<String,String> courseMap = (Map<String,String>)requestMapData.get("courseInCache");
		Map<String,Object> roomDistributerCache = null;
		String lecturerId = courseMap.get("lecturer_id");
		//分销员分成比例（*10000）
		long distributeRate = 0;
		//分销员ID
		String distributerId = null;

		if(!MiscUtils.isEmpty(courseMap)){
			profitRecord.put("lecturer_id", lecturerId);
		}

		//课程收益
		if("0".equals(tradeBill.get("profit_type"))){
			roomDistributerCache = (Map<String,Object>)requestMapData.get("roomDistributerCache");
			if(!MiscUtils.isEmpty(roomDistributerCache)){
				distributeRate = MiscUtils.convertObjectToLong(roomDistributerCache.get("profit_share_rate"));
				distributerId=(String)roomDistributerCache.get("distributer_id");
				profitRecord.put("rq_code", roomDistributerCache.get("rq_code"));
				long shareAmount= (amount * distributeRate)/10000L;
				profitRecord.put("share_amount", shareAmount);
				profitRecord.put("distributer_id", distributerId);
			}
		}
		//收益类型（1:直播课，2：店铺课（非直播课））
		profitRecord.put("course_type",tradeBill.get("course_type"));
		lecturerCoursesProfitMapper.insertLecturerCoursesProfit(profitRecord);
		//4.如果该用户属于某个分销员的用户，则更新推荐用户信息 t_room_distributer_recommend
		if("0".equals(tradeBill.get("profit_type"))){
			String rqCode = null;
			if(!MiscUtils.isEmpty(roomDistributerCache)){
				//t_room_distributer_recommend更新，done_num+1，course_num+1，update_time更新
				rqCode = (String)roomDistributerCache.get("rq_code");
				Map<String,Object> roomDistributerRecommendUpdateMap = new HashMap<>();
				roomDistributerRecommendUpdateMap.put("done_num",1);
				roomDistributerRecommendUpdateMap.put("course_num",1);
				roomDistributerRecommendUpdateMap.put("update_time",now);
				roomDistributerRecommendUpdateMap.put("rq_code", rqCode);
				roomDistributerRecommendUpdateMap.put("room_id", tradeBill.get("room_id"));
				roomDistributerRecommendUpdateMap.put("user_id", userId);
				roomDistributerRecommendMapper.studentBuyCourseUpdate(roomDistributerRecommendUpdateMap);
				//查询是否有t_room_distributer_courses表，如果没有，则插入数据
				Map<String,Object> roomDistributerCourseMap = new HashMap<>();
				roomDistributerCourseMap.put("rq_code", rqCode);
				roomDistributerCourseMap.put("distributer_id", distributerId);
				roomDistributerCourseMap.put("course_id", tradeBill.get("course_id"));
				Map<String,Object> roomDistributerCourse = roomDistributerCoursesMapper.findRoomDistributerCourse(roomDistributerCourseMap);

				if(MiscUtils.isEmpty(roomDistributerCourse)){
					Map<String,Object> roomDistributerCourseInsertMap = new HashMap<>();
					roomDistributerCourseInsertMap.put("distributer_courses_id", MiscUtils.getUUId());
					roomDistributerCourseInsertMap.put("distributer_id", distributerId);
					roomDistributerCourseInsertMap.put("room_id", tradeBill.get("room_id"));
					roomDistributerCourseInsertMap.put("course_id", tradeBill.get("course_id"));
					roomDistributerCourseInsertMap.put("lecturer_id", lecturerId);
					roomDistributerCourseInsertMap.put("recommend_num", 1);
					roomDistributerCourseInsertMap.put("done_num", 1);
					roomDistributerCourseInsertMap.put("total_amount", profitRecord.get("share_amount"));
					roomDistributerCourseInsertMap.put("effective_time", roomDistributerCache.get("effective_time"));
					roomDistributerCourseInsertMap.put("profit_share_rate", roomDistributerCache.get("profit_share_rate"));
					roomDistributerCourseInsertMap.put("create_time", now);
					roomDistributerCourseInsertMap.put("update_time", now);
					roomDistributerCourseInsertMap.put("rq_code", rqCode);
					roomDistributerCoursesMapper.insertRoomDistributerCourses(roomDistributerCourseInsertMap);
				}else {
					Map<String,Object> roomDistributerCourseInsertMap = new HashMap<>();
					roomDistributerCourseInsertMap.put("distributer_courses_id", roomDistributerCourse.get("distributer_courses_id"));
					roomDistributerCourseInsertMap.put("total_amount", profitRecord.get("share_amount"));
					roomDistributerCoursesMapper.afterStudentBuyCourse(roomDistributerCourseInsertMap);
				}
			}
			//t_courses_students
			Map<String,Object> student = new HashMap<>();
			student.put("student_id", MiscUtils.getUUId());
			student.put("user_id", userId);
			student.put("distributer_id", distributerId);
			student.put("lecturer_id", lecturerId);
			student.put("room_id", courseMap.get("room_id"));
			student.put("course_id", courseMap.get("course_id"));
			student.put("payment_amount", amount);
			student.put("course_password", courseMap.get("course_password"));
			student.put("student_type", distributerId==null? 0 : 1);  //TODO 课程学员待修改
			student.put("rq_code", rqCode);
			student.put("create_time", now);
			student.put("create_date", now);
			coursesStudentsMapper.insertStudent(student);

		}
		countUserGains(distributerId,amount,requestMapData.get("app_name").toString(),lecturerId,distributeRate,tradeBill.get("course_type")+"",profitRecord);
		//插入讲师收益
		updateShopUser(lecturerId,userId,amount);
		return profitRecord;
	}

	private void updateShopUser(String lecturerId,String userId,long amount){
		//该用户在店铺的所有消费记录
		//查询店铺ID
		String shopId = shopMapper.selectShopIdByUserId(userId);
		if(shopId!=null){
			Map<String,Object> param = new HashMap<>();
			param.put("lecturer_id",lecturerId);
			param.put("user_id",userId);
			Map<String,Object> mapSum = lecturerCoursesProfitMapper.findUserSumInfo(param);
			Long totalConsume = amount;
			if(mapSum!=null){
				totalConsume += Long.valueOf(mapSum.get("profit_amount").toString());
			}
			int i = shopUserMapper.updateTypeById(shopId,userId,totalConsume);
			//插入付费用户
			if(i==0){
				Map<String,Object> user = new HashMap<>();
				user.put("shop_id",shopId);
				user.put("user_id",userId);
				user.put("user_type","1");
				user.put("total_consume",totalConsume);
				user.put("create_time",new Date());
				shopUserMapper.insert(user);
			}
		}

	}
	private Map<String,Object> initGains(String userId,String appName){
		//初始化用户收入信息
		List<String> ids = new ArrayList<>();
		ids.add(userId);
		List<Map<String, Object>> userRoomAmountList = liveRoomMapper.selectRoomAmount(ids);
		List<Map<String, Object>> userDistributerAmountList = distributerMapper.selectDistributerAmount(ids);
		List<Map<String, Object>> userWithdrawSumList = withdrawCashMapper.selectUserWithdrawSum(ids);

		List<Map<String, Object>> userIdList = new ArrayList<>();
		Map<String, Object> user = new HashMap<>();
		user.put("app_name",appName);
		user.put("user_id",userId);
		userIdList.add(user);

		List<Map<String, Object>> insertGainsList = CountMoneyUtil.getGaoinsList(userIdList, userRoomAmountList,
				userDistributerAmountList, userWithdrawSumList);
		userGainsMapper.insertUserGains(insertGainsList);
		if(insertGainsList!=null){
			return insertGainsList.get(0);
		}
		return null;
	}
	/**
	 * @param distributerId		分销ID
	 * @param amount			支付金额
	 * @param appName			app名称
	 * @param lecturerId		讲师ID
	 * @param distributeRate	分销员收益比例
	 * @param type	            1:直播收入,2:店铺收入（非直播间收入）
	 */
	private void countUserGains(String distributerId,Long amount,String appName,String lecturerId,long distributeRate,String type,Map<String,Object> profitRecord){
		double rate = DoubleUtil.divide( (double) distributeRate,10000D);
		//讲师收益
		Map<String,Object> lectureGains = new HashMap<>();
		lectureGains.put("user_id",lecturerId);
		//初始化讲师余额信息
		Map<String,Object> lectureGainsOld = userGainsMapper.findUserGainsByUserId(lecturerId);
		if(lectureGainsOld == null){
			//如果统计未找到做规避
			lectureGainsOld = initGains(lecturerId,appName);
		}
		//讲师收益
		long lectureTotalAmount = 0L;
		long lectureRealAmount = 0L;

		//分销员收益
		long distTotalAmount = 0L;
		long distRealAmount = 0L;

		//余额统计
		if(distributerId!=null){
			//分销课程收益逻辑
			if(Constants.HEADER_APP_NAME.equals(appName)){
				//qnlive逻辑
				//分销员总/实际 收益
				distTotalAmount = DoubleUtil.mulForLong(amount,rate);
				distRealAmount = distTotalAmount;
				//讲师总/实际 收益
				lectureTotalAmount = amount - distTotalAmount;
				lectureRealAmount = lectureTotalAmount;
			}else{
				//dlive逻辑
				//分销员总/实际 收益
				distTotalAmount = DoubleUtil.mulForLong(amount,rate);
				distRealAmount = DoubleUtil.mulForLong(DoubleUtil.mulForLong(amount,rate),Constants.USER_RATE);
				//讲师总/实际 收益
				lectureTotalAmount = amount - distTotalAmount;
				lectureRealAmount = DoubleUtil.mulForLong(lectureTotalAmount,Constants.USER_RATE);
			}
		}else{
			//直播间收益逻辑
			if(Constants.HEADER_APP_NAME.equals(appName)){
				//qnlive逻辑
				//讲师总收益
				lectureTotalAmount = amount;
				//讲师实际收益
				lectureRealAmount = amount;
			}else{
				//dlive逻辑
				//讲师总收益
				lectureTotalAmount = amount;
				//讲师实际收益
				lectureRealAmount = DoubleUtil.mulForLong(amount,Constants.USER_RATE);
			}
		}
		//更新t_user_gains 讲师收益统计
		//直播间收入
		if("1".equals(type)){
			long lectureRoomTotalAmountOld = Long.valueOf(lectureGainsOld.get("live_room_total_amount").toString());
			long lectureRoomRealAmountOld = Long.valueOf(lectureGainsOld.get("live_room_real_incomes").toString());
			lectureRoomTotalAmountOld = lectureTotalAmount + lectureRoomTotalAmountOld;
			lectureRoomRealAmountOld = lectureRealAmount + lectureRoomRealAmountOld;
			lectureGains.put("live_room_total_amount",lectureRoomTotalAmountOld);
			lectureGains.put("live_room_real_incomes",lectureRoomRealAmountOld);
		}
		//用户收入
		long lectureTotalAmountOld = Long.valueOf(lectureGainsOld.get("user_total_amount").toString());
		long lectureTotalRealIncomesOld = Long.valueOf(lectureGainsOld.get("user_total_real_incomes").toString());
		long lectureBalanceOld = Long.valueOf(lectureGainsOld.get("balance").toString());
		lectureTotalAmountOld = lectureTotalAmountOld + lectureTotalAmount;
		lectureTotalRealIncomesOld = lectureTotalRealIncomesOld + lectureRealAmount;
		lectureBalanceOld = lectureBalanceOld + lectureRealAmount;
		lectureGains.put("user_total_amount",lectureTotalAmountOld);
		lectureGains.put("user_total_real_incomes",lectureTotalRealIncomesOld);
		lectureGains.put("balance",lectureBalanceOld);
		userGainsMapper.updateUserGains(lectureGains);
		//讲师收益
		profitRecord.put("lecture_amount",lectureRealAmount);
		//更新t_user_gains 分销员收益统计
		if(distributerId!=null){
			Map<String,Object> distGains = new HashMap<>();
			distGains.put("user_id",distributerId);
			//初始化分销员余额信息
			Map<String,Object> distGainsOld = userGainsMapper.findUserGainsByUserId(distributerId);
			if(distGainsOld == null){
				//如果统计未找到做规避
				distGainsOld = initGains(distributerId,appName);
			}
			//分销收入
			long distributerTotalAmountOld = Long.valueOf(distGainsOld.get("distributer_total_amount").toString());
			long distributerRealAmountOld = Long.valueOf(distGainsOld.get("distributer_real_incomes").toString());
			distributerTotalAmountOld = distTotalAmount + distributerTotalAmountOld;
			distributerRealAmountOld = distRealAmount + distributerRealAmountOld;
			distGains.put("distributer_total_amount",distributerTotalAmountOld);
			distGains.put("distributer_real_incomes",distributerRealAmountOld);
			//用户收入
			long distTotalAmountOld = Long.valueOf(distGainsOld.get("user_total_amount").toString());
			long distTotalRealIncomesOld = Long.valueOf(distGainsOld.get("user_total_real_incomes").toString());
			long distBalanceOld = Long.valueOf(distGainsOld.get("balance").toString());
			distTotalAmountOld = distTotalAmountOld + distTotalAmount;
			distTotalRealIncomesOld = distTotalRealIncomesOld + distRealAmount;
			distBalanceOld = distBalanceOld + distRealAmount;
			distGains.put("user_total_amount",distTotalAmountOld);
			distGains.put("user_total_real_incomes",distTotalRealIncomesOld);
			distGains.put("balance",distBalanceOld);
			userGainsMapper.updateUserGains(distGains);
			//分销员收益
			profitRecord.put("dist_amount",distRealAmount);
		}
	}
	@Override
	public Map<String, Object> findByDistributerId(String distributer_id) {
		return distributerMapper.findByDistributerId(distributer_id);
	}

	@Override
	public List<Map<String, Object>> findDistributionInfoByDistributerId(Map<String, Object> parameters) {
		return distributerMapper.findDistributionInfoByDistributerId(parameters);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerRecommendInfo(Map<String, Object> parameters) {
		return distributerMapper.findRoomDistributerRecommendInfo(parameters);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerCourseInfo(Map<String, Object> parameters) {
		return distributerMapper.findRoomDistributerCourseInfo(parameters);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerCourseDetailsInfo(Map<String, Object> parameters) {
		return distributerMapper.findRoomDistributerCourseDetailsInfo(parameters);
	}

	@Override
	public int updateUser(Map<String, Object> parameters) {
		return userMapper.updateUser(parameters);
	}

	@Override
	public Map<String, Object> findRoomDistributerInfoByRqCode(String rqCode) {
		return roomDistributerMapper.findRoomDistributerInfoByRqCode(rqCode);
	}

	@Override
	public Map<String, Object> findLiveRoomByRoomId(String room_id) {
		return liveRoomMapper.findLiveRoomByRoomId(room_id);
	}

	@Override
	public Map<String, Object> findRoomDistributerRecommendAllInfo(Map<String, Object> queryMap) {
		return roomDistributerRecommendMapper.findRoomDistributerRecommendAllInfo(queryMap);
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public void insertRoomDistributerRecommend(Map<String, Object> insertMap) {
		insertMap.put("create_time", insertMap.get("now"));
		insertMap.put("update_time", insertMap.get("now"));
		insertMap.put("end_date", insertMap.get("end_date"));
		roomDistributerRecommendMapper.insertRoomDistributerRecommend(insertMap);

		roomDistributerRecommendDetailMapper.insertRoomDistributerRecommend(insertMap);
	}

	@Override
	public Map<String, Object> findRoomDistributionInfoByDistributerId(Map<String, Object> queryMap) {
		queryMap.put("current_date", MiscUtils.getEndTimeOfToday());
		return roomDistributerMapper.findRoomDistributer(queryMap);
	}

	/*@Override
	public void increteRecommendNumForRoomDistributer(Map<String, Object> updateMap) {
		roomDistributerMapper.increteRecommendNumForRoomDistributer(updateMap);
	}*/

	@Override
	public void updateAfterStudentBuyCourse(Map<String, Object> updateCourseMap) {
		coursesMapper.updateAfterStudentBuyCourse(updateCourseMap);
	}

	@Override
	public void insertFeedback(Map<String, Object> reqMap) {
		Date now = (Date)reqMap.get("now");
		reqMap.put("create_time", now);
		reqMap.put("update_time", now);
		reqMap.put("status", "1"); //处理状态，1：未处理 2：已经处理
		feedbackMapper.insertFeedBack(reqMap);

	}

	@Override
	public Map<String, Object> findRewardByUserIdAndCourseId(Map<String, Object> rewardQueryMap) {
		return lecturerCoursesProfitMapper.findRewardByUserIdAndCourseId(rewardQueryMap);
	}

	@Override
	public Map<String, Object> findVersionInfoByOS(Map<String, Object> plateform) {
		return versionMapper.findVersionInfoByOS(plateform);
	}
	@Override
	public Map<String, Object> findAvailableRoomDistributer(Map<String, Object> record) {
		return roomDistributerMapper.findRoomDistributer(record);
	}
	@Override
	public Map<String, Object> findForceVersionInfoByOS(String force_version_key) {
		return versionMapper.findForceVersionInfoByOS(force_version_key);
	}

	@Override
	public Integer updateIMAccount(Map<String, Object> updateIMAccountMap) {
		Map<String,Object> loginInfo = new HashMap<String,Object>();
		loginInfo.put("user_id", updateIMAccountMap.get("user_id"));
		loginInfo.put("m_user_id", updateIMAccountMap.get("m_user_id"));
		loginInfo.put("m_pwd", updateIMAccountMap.get("m_pwd"));
		return loginInfoMapper.updateLoginInfo(loginInfo);
	}

	@Override
	public Map<String, Object> findUserDistributionInfo(Map<String, Object> queryuserDistribution) {
		Map<String,Object> userDistributionInfoForDoneNum = coursesStudentsMapper.findUserDistributionInfoForDoneNum(queryuserDistribution);
		Map<String,Object> userDistributionInfoForLastDoneNum = lecturerCoursesProfitMapper.findUserDistributionInfoForLastDoneNum(queryuserDistribution);
		Map<String,Object> resultMap = new HashMap<>();
		if(MiscUtils.isEmpty(userDistributionInfoForDoneNum)){
			resultMap.put("userDistributionInfoForDoneNum",false);
		}else {
			resultMap.put("userDistributionInfoForDoneNum",true);
		}

		if(MiscUtils.isEmpty(userDistributionInfoForLastDoneNum)){
			resultMap.put("userDistributionInfoForLastDoneNum",false);
		}else {
			resultMap.put("userDistributionInfoForLastDoneNum",true);
		}

		return resultMap;
	}

	@Override
	public List<Map<String, Object>> findcourseRecommendUsers(Map<String, Object> reqMap) {
		return coursesStudentsMapper.findCourseRecommendUsers(reqMap);
	}

	@Override
	public Map<String, Object> findCourseRecommendUserNum(Map<String, Object> reqMap) {
		return coursesStudentsMapper.findCourseRecommendUserNum(reqMap);
	}

	@Override
	public Map<String, Object> findRoomDistributerRecommendItem(Map<String, Object> queryMap) {
		return roomDistributerRecommendMapper.findRoomDistributerRecommendItem(queryMap);
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public void updateRoomDistributerRecommend(Map<String, Object> insertMap) {
		insertMap.put("create_time", insertMap.get("now"));
		roomDistributerRecommendDetailMapper.insertRoomDistributerRecommend(insertMap);
		long position = roomDistributerRecommendDetailMapper.getLatestPostion((String)insertMap.get("distributer_recommend_detail_id"));
		insertMap.remove("create_time");

		insertMap.put("update_time", insertMap.get("now"));
		insertMap.put("position", position);
		roomDistributerRecommendMapper.updateRoomDistributerRecommend(insertMap);

		insertMap.remove("position");
		roomDistributerRecommendDetailMapper.updateRoomDistributerRecommend(insertMap);

	}

	@Override
	public List<Map<String, Object>> findRoomRecommendUserList(Map<String, Object> reqMap) {
		return roomDistributerRecommendMapper.findRoomRecommendUserList(reqMap);
	}

	@Override
	public List<Map<String, Object>> findDistributionRoomDetailList(Map<String, Object> reqMap) {
		return roomDistributerDetailsMapper.findDistributionRoomDetailList(reqMap);
	}

	@Override
	public Map<String,Object> findLectureByLectureId(String lecture_id){
		return lecturerMapper.findLectureByLectureId(lecture_id);
	}

	@Override
	public Map<String, Object> findDistributionRoomDetail(Map<String, Object> reqMap) {
		return roomDistributerDetailsMapper.findDistributionRoomDetail(reqMap);
	}

	@Override
	public List<Map<String, Object>> findCourseWithRoomDistributerCourseInfo(Map<String, Object> parameters) {
		return distributerMapper.findCourseWithRoomDistributerCourseInfo(parameters);
	}

	@Override
	public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
		return coursesStudentsMapper.findCourseIdByStudent(reqMap);
	}

	@Override
	public Map<String, Object> findCoursesSumInfo(Map<String, Object> queryMap) {
		return lecturerCoursesProfitMapper.findCoursesSumInfo(queryMap);
	}

	@Override
	public List<Map<String, Object>> findRoomRecommendUserListByCode(Map<String, Object> record) {
		return roomDistributerRecommendMapper.findRoomRecommendUserList(record);
	}
	@Override
	public Map<String, Object> findCourseMessageByComm(Map<String, Object> record) {
		return courseMessageMapper.findCourseMessageByComm(record);
	}

	@Override
	public List<Map<String, Object>> findCourseMessageListByComm(Map<String, Object> queryMap) {
		return courseMessageMapper.findCourseMessageListByComm(queryMap);
	}

	@Override
	public int findCourseMessageSum(Map<String, Object> queryMap) {
		return courseMessageMapper.findCourseMessageSum(queryMap);
	}

	@Override
	public void updateCourseByCourseId(Map<String, Object> queryMap) {
		coursesMapper.updateCourse(queryMap);
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
	public Map<String, Object> findFansByUserIdAndRoomId(Map<String, Object> reqMap) {
		Map<String,Object> fansKey = new HashMap<>();
		fansKey.put("user_id",reqMap.get("user_id").toString());
		fansKey.put("room_id", reqMap.get("room_id").toString());
		return fansMapper.findFansByUserIdAndRoomId(fansKey);
	}

	@Override
	public List<Map<String, Object>> findRewardConfigurationList() {
		return rewardConfigurationMapper.findRewardConfigurationList();
	}

	@Override
	public Map<String,Object> findByPhone(Map<String,String> record){
		return userMapper.findByPhone(record);
	}

	@Override
	public List<Map<String, Object>> findRoomIdByFans(Map<String, Object> reqMap) {
		return fansMapper.findRoomIdByFans(reqMap);
	}

	@Override
	public List<Map<String, Object>> findClassifyInfo() {
		return classifyInfoMapper.findClassifyInfo();
	}

	@Override
	public List<Map<String, Object>> findClassifyInfoByAppName(String appName) {
		return classifyInfoMapper.findClassifyInfoByAppName(appName);
	}

	@Override
	public List<Map<String, Object>> findCourseBySearch(Map<String, Object> reqMap) {
		return coursesMapper.findCourseBySearch(reqMap);
	}

	@Override
	public List<Map<String, Object>> findLiveRoomBySearch(Map<String, Object> record) {
		return liveRoomMapper.findLiveRoomBySearch(record);
	}

	@Override
	public List<Map<String, Object>> findBannerInfoAll() {
		return bannerInfoMapper.findBannerInfoAll();
	}

	@Override
	public List<Map<String, Object>> findBannerInfoAllByAppName(String appName) {
		return bannerInfoMapper.findBannerInfoAllByAppName(appName);
	}


	@Override
	public List<Map<String, Object>> findCourseByClassifyId(Map<String, Object> record) {
		return coursesMapper.findCourseByClassifyId(record);
	}

	@Override
	public List<Map<String, Object>> findCourseByStatus(Map<String, Object> record) {
		return coursesMapper.findCourseByStatus(record);
	}


	@Override
	public Integer insertCourseMessageList(List<Map<String, Object>> messageList) {
		return courseMessageMapper.insertCourseMessageList(messageList);
	}

	@Override
	public List<Map<String, Object>> findSystemConfig() {
		return systemConfigMapper.findSystemConfig();
	}

	@Override
	public List<Map<String, Object>> findSystemConfigByAppName(String appName) {
		return systemConfigMapper.findSystemConfigByAppName(appName);
	}

	@Override
	public void updateCourse(Map<String, Object> reqMap) {
		Integer updateCount = null;
		Date now = (Date)reqMap.get("now");
		Map<String,Object> course = new HashMap<String,Object>();
		course.put("course_id", reqMap.get("course_id"));


		if("0".equals(reqMap.get("status"))){
			if(!MiscUtils.isEmpty( reqMap.get("course_amount"))){
				course.put("course_amount", reqMap.get("course_amount"));
			}
			if(!MiscUtils.isEmpty( reqMap.get("student_num"))){
				course.put("student_num", reqMap.get("student_num"));
			}
			if(!MiscUtils.isEmpty( reqMap.get("extra_amount"))){
				course.put("extra_amount", reqMap.get("extra_amount"));
			}
			if(!MiscUtils.isEmpty( reqMap.get("extra_num"))){
				course.put("extra_num", reqMap.get("extra_num"));
			}

		}else if("2".equals(reqMap.get("status"))){
			course.put("end_time", now);
			course.put("status", "2");
		}else if("5".equals(reqMap.get("status"))) {
			course.put("update_time", now);
			course.put("status", "5");
		}else if("1".equals(reqMap.get("status"))){
			course.put("status", "1");
		}else{
			Object course_title = reqMap.get("course_title");
			Object start_time = reqMap.get("start_time");
			if(!MiscUtils.isEmpty(course_title)){
				course.put("course_title", course_title);
			}
			if(!MiscUtils.isEmpty(start_time)){
				course.put("start_time", new Date(MiscUtils.convertObjectToLong(start_time)));
			}
			course.put("course_remark", reqMap.get("course_remark"));
			course.put("course_url", reqMap.get("course_url"));
			course.put("course_password", reqMap.get("course_password"));
			if(!MiscUtils.isEmpty(reqMap.get("update_time"))){
				course.put(Constants.SYS_FIELD_LAST_UPDATE_TIME, new Date(MiscUtils.convertObjectToLong(reqMap.get("update_time"))));
			}
		}
		course.put("update_time", now);
		coursesMapper.updateCourse(course);
	}

	@Override
	public List<Map<String, Object>> findLecturerCourseListByStatus(Map<String, Object> queryMap) {
		return coursesMapper.findLecturerCourseListByStatus(queryMap);
	}

	@Override
	public Map<String, Object> findUserNumberByCourse(Map<String, Object> map) {
		return tradeBillMapper.findUserNumberByCourse(map);
	}

	@Override
	public List<Map<String, Object>> findLecturerCourseList(Map<String,Object> record) {
		return coursesMapper.findLecturerCourseList(record);
	}

	@Override
	public int insertClassify(Map<String, Object> record) {
		return classifyInfoMapper.insertClassifyInfo(record);
	}

	@Override
	public int updateClassify(Map<String, Object> record) {
		return classifyInfoMapper.updateClassifyInfo(record);
	}

	/**
	 * 新增轮播
	 */
	@Override
	public int addBanner(Map<String, Object> insertMap) {
		return bannerInfoMapper.insertBanner(insertMap);
	}

	/**
	 * 根据map中的参数查询banner
	 */
	@Override
	public List<Map<String, Object>> findBannerInfoByMap(Map<String, Object> reqMap) {
		return bannerInfoMapper.selectBannerInfoByMap(reqMap);
	}

	/**
	 * 根据map中的参数查询banner总数量
	 */
	@Override
	public int findBannerCountByMap(Map<String, Object> reqMap) {
		return bannerInfoMapper.selectBannerCountByMap(reqMap);
	}

	/**
	 * 更新banner所有字段
	 */
	@Override
	public int updateBannerByMap(Map<String, Object> reqMap) {
		return bannerInfoMapper.updateBannerInfoByMap(reqMap);
	}

	/**
	 * 移除banner
	 */
	@Override
	public int deleteBannerInfoByMap(Map<String, Object> reqMap) {
		return bannerInfoMapper.delectBannerInfoByMap(reqMap);
	}

	/**
	 * 根据map中非空字段更新banner
	 */
	@Override
	public int updateBannerByMapNotNull(Map<String, Object> reqMap) {
		return bannerInfoMapper.updateBannerInfoByMapNotNull(reqMap);
	}

	/**
	 * 后台_搜索课程列表(同时搜索课程名、课程id)
	 */
	@Override
	public List<Map<String, Object>> findCourseListBySearch(Map<String, Object> reqMap) {
		return coursesMapper.findCourseListBySearch(reqMap);
	}

	/**
	 * 后台_搜索课程列表(同时搜索直播间名、直播间id)
	 */
	@Override
	public List<Map<String, Object>> findLiveRoomListBySearch(Map<String, Object> reqMap) {
		return liveRoomMapper.findLiveRoomListBySearch(reqMap);
	}

	/**
	 * 后台_获取分类列表
	 */
	@Override
	public List<Map<String, Object>> getClassifyList(Map<String, Object> reqMap) {
		return classifyInfoMapper.findClassifyListByMap(reqMap);
	}

	/**
	 * 后台_获取各分类下课程数量
	 */
	@Override
	public List<Map<String, Object>> getCourseNumGroupByClassifyId(Map<String, Object> selectMap) {
		return classifyInfoMapper.findCourseNumGroupByClassifyId(selectMap);
	}

	/**
	 * 后台_根据手机号码查询后台登录帐号
	 */
	@Override
	public Map<String, Object> getAdminUserByMobile(Map<String, Object> reqMap) {
		return adminUserMapper.selectAdminUserByMobile(reqMap);
	}

	/**
	 * 后台_更新后台账户所有字段
	 */
	@Override
	public int updateAdminUserByAllMap(Map<String, Object> adminUserMap) {
		return adminUserMapper.updateAdminUserByAllMap(adminUserMap);
	}

	@Override
	public List<Map<String, Object>> findSeriesBySearch(Map<String, Object> record) {
		return seriesMapper.findSeriesBySearch(record);
	}

	@Override
	public Map<String, Object> getShopInfo(Map<String, Object> reqMap) {
		return shopMapper.selectByPrimaryKey(reqMap);
	}

	@Override
	public void insertShopInfo(Map<String, Object> shop) {
		shopMapper.insert(shop);
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
	public List<Map<String, Object>> findSeriesByLecturer(String lecturerId) {
		return seriesMapper.findSeriesByLecturer(lecturerId);
	}

	@Override
	public List<Map<String, Object>> findCourseListBySeriesId(String series_id) {
		return coursesMapper.findCourseListBySeriesId(series_id);
	}

	@Override
	public void updateSeries(Map<String, Object> map) {
		seriesMapper.updateSeries(map);
	}

	@Override
	public Map<String, Object> findSaaSCourseByCourseId(String course_id) {
		return saaSCourseMapper.selectByPrimaryKey(course_id);
	}
}
