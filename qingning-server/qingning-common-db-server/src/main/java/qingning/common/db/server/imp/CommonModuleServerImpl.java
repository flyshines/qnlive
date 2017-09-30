
package qingning.common.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.entity.QNLiveException;
import qingning.common.util.*;
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
	private CourseMapper courseMapper;

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
	private LiveRoomMapper liveRoomMapper;

	@Autowired(required = true)
	private LecturerMapper lecturerMapper;

	@Autowired(required = true)
	private FeedbackMapper feedbackMapper;

	@Autowired(required = true)
	private VersionMapper versionMapper;

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
	private ShopMapper shopMapper;
	@Autowired(required = true)
	private ShopUserMapper shopUserMapper;

	@Autowired(required = true)
	private SeriesStudentsMapper seriesStudentsMapper;

	@Autowired(required = true)
	private CourseGuestMapper courseGuestMapper;


	@Autowired(required = true)
	private CourseCommentMapper courseCommentMapper;

	@Autowired(required = true)
	private LecturerDistributionInfoMapper lecturerDistributionInfoMapper;
	@Override
	public List<Map<String, Object>> getServerUrls() {
		return serverFunctionMapper.getServerUrls();
	}



	@Override
	public Map<String, Object> findUserInfoByUserId(String user_id) {
		return userMapper.findByUserId(user_id);
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
	public Map<String, Object> findVersionInfoByOS(Map<String, Object> plateform) {
		return versionMapper.findVersionInfoByOS(plateform);
	}
	@Override
	public Map<String, Object> findForceVersionInfoByOS(String force_version_key) {
		return versionMapper.findForceVersionInfoByOS(force_version_key);
	}



	@Override
	public Map<String,Object> findLectureByLectureId(String lecture_id){
		return lecturerMapper.findLectureByLectureId(lecture_id);
	}


	@Override
	public Map<String, Object> getLoginInfoByLoginId(String unionID) {
		return null;
	}



	@Override
	public List<Map<String, Object>> findRewardConfigurationList() {
		return rewardConfigurationMapper.findRewardConfigurationList();
	}

	@Override
	public List<Map<String, Object>> findShopIdByFans(Map<String, Object> reqMap) {
		return fansMapper.findShopIdByFans(reqMap);
	}


	@Override
	public List<Map<String, Object>> findSystemConfig( ) {
		return systemConfigMapper.findSystemConfig();
	}



	@Override
	public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
		return null;
	}

	@Override
	public Map<String, Object> getShopInfo(String shopId,String userId) {
		return shopMapper.selectByPrimaryKey(shopId,userId);
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




	@Override
	public int updateUserById(Map<String, Object> userMap) {
		//手机号码已经绑定
		if (userMapper.existByPhone(userMap) > 0) {
			return 1;
		}
		userMapper.updateUser(userMap);
		return 0;
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


	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String, String> initializeAccountRegisterUser(Map<String, Object> reqMap) {
		//1.插入t_user
		Date now = new Date();
		Map<String,Object> user = new HashMap<String,Object>();
		String uuid = MiscUtils.getUUId();
		user.put("user_id", uuid);
		user.put("nick_name", reqMap.get("nick_name"));
		user.put("avatar_address", reqMap.get("avatar_address"));
		user.put("gender",  Constants.USER_DEFAULT_GENDER);
		user.put("create_time", now);
		user.put("update_time", now);
		user.put("app_name",reqMap.get("app_name"));
		user.put("user_role", Constants.USER_ROLE_DEFAULT);
		user.put("country", Constants.USER_DEFAULT_COUNTRY);
		user.put("province", Constants.USER_DEFAULT_PROVINCE);
		user.put("city", Constants.USER_DEFAULT_CITY);
		user.put("district", Constants.USER_DEFAULT_DISTRICT);
		//位置信息未插入由消息服务处理
		userMapper.insertUser(user);
		//2.插入login_info
		Map<String,Object> loginInfo = new HashMap<String,Object>();
		loginInfo.put("user_id", uuid);
		String login_type = (String)reqMap.get("login_type");
		//前端第一次加密 后端第二次加密
		String passwd = MD5Util.getMD5(reqMap.get("passwd").toString()+Constants.USER_DEFAULT_MD5);
		loginInfo.put("passwd",passwd);
		loginInfo.put("account",reqMap.get("account"));
		loginInfo.put("app_name",reqMap.get("app_name"));
		loginInfo.put("user_role", Constants.USER_ROLE_DEFAULT);
		//位置信息未插入由消息服务处理
		loginInfo.put("create_time", now);
		loginInfo.put("update_time", now);
		loginInfo.put("country", Constants.USER_DEFAULT_COUNTRY);
		loginInfo.put("province", Constants.USER_DEFAULT_PROVINCE);
		loginInfo.put("city", Constants.USER_DEFAULT_CITY);
		loginInfo.put("district", Constants.USER_DEFAULT_DISTRICT);
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
		if(!MiscUtils.isEmpty(updateMap.get("union_id"))){
			record.put("union_id",updateMap.get("union_id"));
		}
		if(!MiscUtils.isEmpty(updateMap.get("subscribe"))){
			record.put("subscribe",updateMap.get("subscribe"));
		}
		loginInfoMapper.updateLoginInfo(record);
	}

	@Override
	public Map<String,Object> findTradebillByOutTradeNo(String outTradeNo) {
		Map<String,Object> tradeBill = tradeBillMapper.findByOutTradeNo(outTradeNo);
		return tradeBill;
	}

	@Override
	public Map<String,Object> findTradeBillByPrePayNo(String pre_pay_no) {
		Map<String,Object> tradeBill = paymentBillMapper.findTradeBillByPrePayNo(pre_pay_no);
		return tradeBill;
	}

	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String, Object> handleWeixinPayResult(Map<String, Object> requestMapData) throws Exception{

		Date now = new Date();
		Map<String,Object> tradeBill = (Map<String,Object>)requestMapData.get("tradeBillInCache");
		Map<String,Object> updateTradeBill = new HashMap<>();
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
		Map<String,Object> updatePaymentBill = new HashMap<>();
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
		Map<String,Object> profitRecord = new HashMap<>();
		profitRecord.put("profit_id", profitId);
		profitRecord.put("course_id", tradeBill.get("course_id"));
		profitRecord.put("shop_id", tradeBill.get("shop_id"));
		profitRecord.put("user_id", userId);
		profitRecord.put("profit_amount", amount);
		profitRecord.put("profit_type", tradeBill.get("profit_type"));
		profitRecord.put("create_time", now);
		profitRecord.put("create_date", now);
		profitRecord.put("payment_id", paymentBill.get("payment_id"));
		profitRecord.put("payment_type", paymentBill.get("payment_type"));
		profitRecord.put("goods_type", tradeBill.get("goods_type"));

		Map<String,String> courseMap = (Map<String,String>)requestMapData.get("courseInCache");
		String lecturerId = courseMap.get("lecturer_id");
		//分销员分成比例（*10000）
		long distributeRate = 0;
		//分销员ID
		String distributerId = null;

		if(tradeBill.get("guest_id")!=null){
			//嘉宾打赏
			lecturerId = tradeBill.get("guest_id").toString();
			profitRecord.put("lecturer_id", lecturerId);
			profitRecord.put("reward_type","2"); //0:门票收益,1:直播间打赏,2:嘉宾打赏
		}else if(!MiscUtils.isEmpty(courseMap)){
			//讲师收益或打赏
			profitRecord.put("lecturer_id", lecturerId);
			if("1".equals(tradeBill.get("profit_type"))){
				//直播间打赏
				profitRecord.put("reward_type","1");
			}else{
				//门票收益
				profitRecord.put("reward_type","0");
			}
		}
		//收益类型（1:直播课，2：店铺课（非直播课））
		profitRecord.put("course_type",tradeBill.get("course_type"));
		if(tradeBill.get("guest_id")==null) {
			//插入讲师收益
			updateShopUser(lecturerId, userId, amount);
		}
		lecturerCoursesProfitMapper.insertLecturerCoursesProfit(profitRecord);
		//4.如果该用户属于某个分销员的用户，则更新推荐用户信息 t_room_distributer_recommend
		if("0".equals(tradeBill.get("profit_type"))){
			String rqCode = null;
			//t_courses_students
			Map<String,Object> student = new HashMap<>();
			student.put("student_id", MiscUtils.getUUId());
			student.put("user_id", userId);
			student.put("distributer_id", distributerId);
			student.put("lecturer_id", lecturerId);
			student.put("room_id", requestMapData.get("room_id"));
			student.put("course_id", courseMap.get("course_id"));
			student.put("payment_amount", amount);
			student.put("course_password", courseMap.get("course_password"));
			student.put("student_type", distributerId==null? 0 : 1);  //TODO 课程学员待修改
			student.put("rq_code", rqCode);
			student.put("create_time", now);
			student.put("create_date", now);
			coursesStudentsMapper.insertStudent(student);

		}
		countUserGains(distributerId,amount,lecturerId,distributeRate,tradeBill.get("course_type")+"",profitRecord);
		return profitRecord;
	}

	private void updateShopUser(String lecturerId,String userId,long amount){
		//该用户在店铺的所有消费记录
		//根据讲师ID查询店铺ID
		String shopId = shopMapper.selectShopIdByUserId(lecturerId);
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
	private Map<String,Object> initGains(String userId){
		//初始化用户余额信息
		Map<String,Object> gainsMap = new HashMap<>();
		gainsMap.put("user_id",userId);
		gainsMap.put("live_room_total_amount","0");
		gainsMap.put("live_room_real_incomes","0");
		gainsMap.put("distributer_total_amount","0");
		gainsMap.put("distributer_real_incomes","0");
		gainsMap.put("user_total_amount","0");
		gainsMap.put("user_total_real_incomes","0");
		gainsMap.put("balance","0");
		userGainsMapper.insertUserGainsByNewUser(gainsMap);
		return gainsMap;
	}
	/**
	 * @param distributerId		分销ID
	 * @param amount			支付金额
	 * @param lecturerId		讲师ID
	 * @param distributeRate	分销员收益比例
	 * @param courseType	            1:直播收入,2:店铺收入（非直播间收入）
	 */
	private void countUserGains(String distributerId,Long amount,String lecturerId,long distributeRate,String courseType,Map<String,Object> profitRecord){
		double rate = DoubleUtil.divide( (double) distributeRate,10000D);
		//讲师收益
		Map<String,Object> lectureGains = new HashMap<>();
		lectureGains.put("user_id",lecturerId);
		//初始化讲师余额信息
		Map<String,Object> lectureGainsOld = userGainsMapper.findUserGainsByUserId(lecturerId);
		if(lectureGainsOld == null){
			//如果统计未找到做规避
			lectureGainsOld = initGains(lecturerId);
		}
		//讲师收益
		long lectureTotalAmount;
		long lectureRealAmount;

		//分销员收益
		long distTotalAmount = 0L;
		long distRealAmount = 0L;

		//余额统计
			//直播间收益逻辑
		//qnlive逻辑
		//讲师总收益
		lectureTotalAmount = amount;
		//讲师实际收益
		lectureRealAmount = amount;
		//更新t_user_gains 讲师收益统计
		//直播间收入
		if("1".equals(courseType)){
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
				distGainsOld = initGains(distributerId);
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
	public int updateUser(Map<String, Object> parameters) {
		return userMapper.updateUser(parameters);
	}

	@Override
	public Map<String, Object> findLiveRoomByRoomId(String room_id) {
		return liveRoomMapper.findLiveRoomByRoomId(room_id);
	}



	@Override
	public void updateAfterStudentBuyCourse(Map<String, Object> updateCourseMap) {
		courseMapper.updateAfterStudentBuyCourse(updateCourseMap);
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
	public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
		return coursesStudentsMapper.findCourseIdByStudent(reqMap);
	}

	@Override
	public Map<String, Object> findCoursesSumInfo(Map<String, Object> queryMap) {
		return lecturerCoursesProfitMapper.findCoursesSumInfo(queryMap);
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
		courseMapper.updateCourse(queryMap);
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
	public Map<String, Object> findFansByUserIdAndShopId(Map<String, Object> reqMap) {
		Map<String,Object> fansKey = new HashMap<>();
		fansKey.put("user_id",reqMap.get("user_id").toString());
		fansKey.put("shop_id", reqMap.get("shop_id").toString());
		return fansMapper.findFansByUserIdAndShopId(fansKey);
	}


	@Override
	public Map<String,Object> findByPhone(Map<String,Object> record){
		return userMapper.findByPhone(record);
	}

	@Override
	public List<Map<String, Object>> findClassifyInfo() {
		return classifyInfoMapper.findClassifyInfo();
	}

	@Override
	public List<Map<String, Object>> findClassifyInfoByAppName(String appName) {
		return classifyInfoMapper.findClassifyInfo();
	}

	@Override
	public List<Map<String, Object>> findCourseBySearch(Map<String, Object> reqMap) {
		return courseMapper.findCourseBySearch(reqMap);
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
		return bannerInfoMapper.findBannerInfoAll();
	}


	@Override
	public List<Map<String, Object>> findCourseByClassifyId(Map<String, Object> record) {
		return courseMapper.findCourseByClassifyId(record);
	}

	@Override
	public List<Map<String, Object>> findCourseByStatus(Map<String, Object> record) {
		return courseMapper.findCourseByStatus(record);
	}


	@Override
	public Integer insertCourseMessageList(List<Map<String, Object>> messageList) {
		return courseMessageMapper.insertCourseMessageList(messageList);
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
		courseMapper.updateCourse(course);
	}

	@Override
	public List<Map<String, Object>> findLecturerCourseListByStatus(Map<String, Object> queryMap) {
		return courseMapper.findLecturerCourseListByStatus(queryMap);
	}

	@Override
	public Map<String, Object> findUserNumberByCourse(Map<String, Object> map) {
		return tradeBillMapper.findUserNumberByCourse(map);
	}

	@Override
	public List<Map<String, Object>> findLecturerCourseList(Map<String,Object> record) {
		return courseMapper.findLecturerCourseList(record);
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public Map<String,Object> insertClassify(Map<String, Object> record) {
		int i = classifyInfoMapper.insertClassifyInfo(record);
		if(i>0){
			return classifyInfoMapper.selectLastInsert();
		}
		return null;
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
		return courseMapper.findCourseListBySearch(reqMap);
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
	public List<Map<String, Object>> findSeriesByLecturer(String lecturerId) {
		return seriesMapper.findSeriesByLecturer(lecturerId);
	}

	@Override
	public List<Map<String, Object>> findCourseListBySeriesId(String series_id) {
		return courseMapper.findCourseListBySeriesId(series_id);
	}

	@Override
	public void updateSeries(Map<String, Object> map) {
		seriesMapper.updateSeries(map);
	}


	@Override
	public List<Map<String, Object>> findCourseListAllByLecturerId(String lecturerId) {
		return courseMapper.findCourseListAllByLecturerId(lecturerId);
	}

	@Override
	public void updateCourseCmountByCourseId(Map<String, Object> course) {
		courseMapper.updateCourseCountByCourseId(course);
	}

	@Override
	public void updateSeriesCmountByCourseId(Map<String, Object> course) {
		seriesMapper.updateSeriesCmountByCourseId(course);
	}

	@Override
	public boolean isStudentOfTheSeries(Map<String, Object> queryMap) {
		return !MiscUtils.isEmpty( seriesStudentsMapper.isStudentOfTheSeries(queryMap));
	}

	@Override
	public boolean isStudentOfTheCourse(Map<String, Object> studentQueryMap) {
		return !MiscUtils.isEmpty(coursesStudentsMapper.isStudentOfTheCourse(studentQueryMap));
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
		resultMap.put("student_id",student.get("student_id"));
		return resultMap;
	}

	@Override
	public void increaseStudentNumByCourseId(String course_id) {
		courseMapper.increaseStudent(course_id);
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
		resultMap.put("student_id", student.get("student_id"));
		return resultMap;
	}

	@Override
	public void increaseStudentNumBySeriesId(String series_id) {
		seriesMapper.increaseSeriesStudent(series_id);
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
	public void updateAccountUser(Map<String, Object> reqMap) {
		userMapper.updateUser(reqMap);
		Date now = new Date();
		Map<String,Object> loginInfo = new HashMap<String,Object>();
		loginInfo.put("user_id", reqMap.get("user_id").toString());
		//前端第一次加密 后端第二次加密

		if(!MiscUtils.isEmpty(reqMap.get("passwd"))){
			String passwd = MD5Util.getMD5(reqMap.get("passwd").toString()+Constants.USER_DEFAULT_MD5);
			loginInfo.put("passwd",passwd);
		}
		loginInfo.put("account",reqMap.get("account"));
		loginInfo.put("app_name",reqMap.get("app_name"));
		loginInfo.put("update_time",now);
		loginInfoMapper.updateLoginInfo(loginInfo);

		Map<String,Object> shop = new HashMap<String,Object>();
		shop.put("shop_id",reqMap.get("shop_id"));
		shop.put("user_name",reqMap.get("nick_name")+"");
		shop.put("shop_name",reqMap.get("nick_name")+"的知识店铺");
		shop.put("shop_logo",reqMap.get("avatar_address"));
		shop.put("shop_remark",reqMap.get("remark"));
		shop.put("lecturer_title",reqMap.get("lecturer_title"));
		shop.put("lecturer_identity",reqMap.get("lecturer_identity"));
		shopMapper.updateByPrimaryKey(shop);
	}

	@Override
	public List<Map<String, Object>> findLoginInfo() {
		return loginInfoMapper.findLoginInfo();
	}

	@Override
	public Map<String, Object> findCourseGuestByUserAndCourse(String user_id,String course_id) {
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("user_id",user_id);
		map.put("course_id",course_id);
		return courseGuestMapper.findCourseGuestByUserAndCourse(map);
	}

	@Override
	public void updateLoginInfo(Map<String, Object> queryMap) {
		loginInfoMapper.updateLoginInfo(queryMap);
	}

	@Override
	public void updateUserCommonInfo(Map<String, Object> queryMap) {
		userMapper.updateUser(queryMap);
	}



	/**
	 * 根据map中的参数查询banner总数量
	 */
	@Override
	public int findBannerCountByMap(Map<String, Object> reqMap) {
		return bannerInfoMapper.selectBannerCountByMap(reqMap);
	}

	//TODO 实现缓存方法
	@Override
	public List<Map<String, Object>> findSeriesStudentByMap(Map<String, Object> param) {
		return null;
	}
	//TODO 实现缓存方法
	@Override
	public List<Map<String, Object>> findCourseStudentByMap(Map<String, Object> param) {
		return null;
	}
	//TODO 实现缓存方法
	@Override
	public List<Map<String, Object>> getGuestAndCourseInfoByMap(Map<String, Object> reqMap) {
		return null;
	}
}
