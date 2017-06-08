
package qingning.common.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.entity.QNLiveException;
import qingning.common.util.Constants;
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

		//用户这次产生的实际收益
		long userIncome = DoubleUtil.mulForLong(Long.valueOf(tradeBill.get("amount").toString()),Constants.USER_RATE);
		long userTotalIncome = Long.valueOf(tradeBill.get("amount").toString());

		//3.更新 讲师课程收益信息表
		Map<String,Object> profitRecord = new HashMap<String,Object>();
		profitRecord.put("profit_id", profitId);
		profitRecord.put("course_id", tradeBill.get("course_id"));
		profitRecord.put("room_id", tradeBill.get("room_id"));
		profitRecord.put("user_id", tradeBill.get("user_id"));
		profitRecord.put("profit_amount", tradeBill.get("amount"));
		profitRecord.put("profit_type", tradeBill.get("profit_type"));
		profitRecord.put("create_time", now);
		profitRecord.put("create_date", now);
		profitRecord.put("payment_id", paymentBill.get("payment_id"));
		profitRecord.put("payment_type", paymentBill.get("payment_type"));
		
		Map<String,String> courseMap = (Map<String,String>)requestMapData.get("courseInCache");
		if(!MiscUtils.isEmpty(courseMap)){
			profitRecord.put("lecturer_id", courseMap.get("lecturer_id"));
		}
		Map<String,Object> roomDistributerCache = null;		
		if("0".equals(tradeBill.get("profit_type"))){
			roomDistributerCache = (Map<String,Object>)requestMapData.get("roomDistributerCache");
			if(!MiscUtils.isEmpty(roomDistributerCache)){
				profitRecord.put("rq_code", roomDistributerCache.get("rq_code"));
				long shareAmount= (MiscUtils.convertObjectToLong(tradeBill.get("amount")) * MiscUtils.convertObjectToLong(roomDistributerCache.get("profit_share_rate")))/10000L;
				profitRecord.put("share_amount", shareAmount);
				profitRecord.put("distributer_id", roomDistributerCache.get("distributer_id"));
			}
		}
		
		lecturerCoursesProfitMapper.insertLecturerCoursesProfit(profitRecord);
		boolean isDist = false;
		//4.如果该用户属于某个分销员的用户，则更新推荐用户信息 t_room_distributer_recommend
		if("0".equals(tradeBill.get("profit_type"))){
			String distributer_id = null;
			String rqCode = null;
			if(!MiscUtils.isEmpty(roomDistributerCache)){
				//t_room_distributer_recommend更新，done_num+1，course_num+1，update_time更新
				Map<String,Object> roomDistributerRecommendUpdateMap = new HashMap<>();
				roomDistributerRecommendUpdateMap.put("done_num",1);
				roomDistributerRecommendUpdateMap.put("course_num",1);
				roomDistributerRecommendUpdateMap.put("update_time",now);
				roomDistributerRecommendUpdateMap.put("rq_code", (String)roomDistributerCache.get("rq_code"));
				roomDistributerRecommendUpdateMap.put("room_id", tradeBill.get("room_id"));
				roomDistributerRecommendUpdateMap.put("user_id", tradeBill.get("user_id"));
				roomDistributerRecommendMapper.studentBuyCourseUpdate(roomDistributerRecommendUpdateMap);
				distributer_id=(String)roomDistributerCache.get("distributer_id");
				rqCode = (String)roomDistributerCache.get("rq_code");
				//查询是否有t_room_distributer_courses表，如果没有，则插入数据
				Map<String,Object> roomDistributerCourseMap = new HashMap<>();
				roomDistributerCourseMap.put("rq_code", (String)roomDistributerCache.get("rq_code"));
				roomDistributerCourseMap.put("distributer_id", roomDistributerCache.get("distributer_id"));
				roomDistributerCourseMap.put("course_id", tradeBill.get("course_id"));
				Map<String,Object> roomDistributerCourse = roomDistributerCoursesMapper.findRoomDistributerCourse(roomDistributerCourseMap);

				if(MiscUtils.isEmpty(roomDistributerCourse)){
					Map<String,Object> roomDistributerCourseInsertMap = new HashMap<>();
					roomDistributerCourseInsertMap.put("distributer_courses_id", MiscUtils.getUUId());
					roomDistributerCourseInsertMap.put("distributer_id", roomDistributerCache.get("distributer_id"));
					roomDistributerCourseInsertMap.put("room_id", tradeBill.get("room_id"));
					roomDistributerCourseInsertMap.put("course_id", tradeBill.get("course_id"));
					roomDistributerCourseInsertMap.put("lecturer_id", courseMap.get("lecturer_id"));
					roomDistributerCourseInsertMap.put("recommend_num", 1);
					roomDistributerCourseInsertMap.put("done_num", 1);
					roomDistributerCourseInsertMap.put("total_amount", (Long)profitRecord.get("share_amount"));
					roomDistributerCourseInsertMap.put("effective_time", roomDistributerCache.get("effective_time"));
					roomDistributerCourseInsertMap.put("profit_share_rate", roomDistributerCache.get("profit_share_rate"));
					roomDistributerCourseInsertMap.put("create_time", now);
					roomDistributerCourseInsertMap.put("update_time", now);
					roomDistributerCourseInsertMap.put("rq_code", (String)roomDistributerCache.get("rq_code"));
					roomDistributerCoursesMapper.insertRoomDistributerCourses(roomDistributerCourseInsertMap);
				}else {
					Map<String,Object> roomDistributerCourseInsertMap = new HashMap<>();
					roomDistributerCourseInsertMap.put("distributer_courses_id", roomDistributerCourse.get("distributer_courses_id"));
					roomDistributerCourseInsertMap.put("total_amount", (Long)profitRecord.get("share_amount"));
					roomDistributerCoursesMapper.afterStudentBuyCourse(roomDistributerCourseInsertMap);
				}
				isDist = true;
			}
			//t_courses_students
			Map<String,Object> student = new HashMap<String,Object>();
			student.put("student_id", MiscUtils.getUUId());
			student.put("user_id", tradeBill.get("user_id"));
			student.put("distributer_id", distributer_id);
			student.put("lecturer_id", courseMap.get("lecturer_id"));
			student.put("room_id", courseMap.get("room_id"));
			student.put("course_id", courseMap.get("course_id"));
			student.put("payment_amount", tradeBill.get("amount"));
			student.put("course_password", courseMap.get("course_password"));
			student.put("student_type", distributer_id==null? 0 : 1);  //TODO 课程学员待修改
			student.put("rq_code", rqCode);
			student.put("create_time", now);
			student.put("create_date", now);
			coursesStudentsMapper.insertStudent(student);			
		}
		Map<String,Object> userGains = new HashMap<>();
		if(isDist){
			userGains.put("user_id",roomDistributerCache.get("distributer_id"));
		}else{
			userGains.put("user_id",courseMap.get("lecturer_id"));
		}
		Map<String,Object> userGainsOld = userGainsMapper.findUserGainsByUserId(userGains.get("user_id").toString());
		if(userGainsOld == null){
			//如果统计未找到做规避
			userGains.put("live_room_total_amount",0);
			userGains.put("live_room_real_incomes",0);
			userGains.put("distributer_total_amount",0);
			userGains.put("distributer_real_incomes",0);
			userGains.put("user_total_amount",0);
			userGains.put("user_total_real_incomes",0);
			userGains.put("balance",0);
		}
		if(isDist){
			//用户分销收益
			userGains.put("distributer_total_amount",userTotalIncome + Long.valueOf(userGainsOld.get("distributer_total_amount").toString()));
			userGains.put("distributer_real_incomes", userIncome + Long.valueOf(userGainsOld.get("distributer_real_incomes").toString()));
		}else{
			//用户直播间收益
			userGains.put("live_room_total_amount",userTotalIncome + Long.valueOf(userGainsOld.get("distributer_total_amount").toString()));
			userGains.put("live_room_real_incomes", userIncome + Long.valueOf(userGainsOld.get("distributer_real_incomes").toString()));
		}
		//更新t_user_gains 用户收益统计表
		long user_total_amount = Long.valueOf(userGainsOld.get("user_total_amount").toString());
		long user_total_real_incomes = Long.valueOf(userGainsOld.get("user_total_real_incomes").toString());
		long balance = Long.valueOf(userGainsOld.get("balance").toString());
		if(userGains!=null){
			user_total_amount = user_total_amount + Long.valueOf(tradeBill.get("amount").toString());
			user_total_real_incomes = user_total_real_incomes + userIncome;
			balance = balance + userIncome;
			userGains.put("user_total_amount",user_total_amount);
			userGains.put("user_total_real_incomes",user_total_real_incomes);
			userGains.put("balance",balance);
			userGainsMapper.updateUserGains(userGains);
		}else{
			userGains.put("user_total_amount",user_total_amount);
			userGains.put("user_total_real_incomes",user_total_real_incomes);
			userGains.put("balance",balance);
			userGainsMapper.insertUserGainsByNewUser(userGains);
		}
		//<editor-fold desc="Description">
		//TODO 定时任务处理，需更新缓存
/*		//更新收益表信息
		Map<String,Object> courses = coursesMapper.findCourseByCourseId(tradeBill.getCourseId());
		PaymentBill paymentBill = paymentBillMapper.selectByTradeId(tradeBill.getTradeId());
		LecturerCoursesProfit lcp = new LecturerCoursesProfit();
		//如果为购买课程，则查询该学员是否属于某个分销员
		if(tradeBill.getProfitType().equals("0")){
			Map<String,Object> queryMap = new HashMap<>();
			queryMap.put("room_id", tradeBill.getRoomId());
			queryMap.put("user_id", tradeBill.getUserId());
			queryMap.put("today_end_date", MiscUtils.getEndDateOfToday().getTime());
			Map<String,Object> recommendMap = roomDistributerRecommendMapper.findRoomDistributerRecommendAllInfo(queryMap);

			//设置相关收益
			if(! MiscUtils.isEmpty(recommendMap)){
				lcp.setDistributerId(recommendMap.get("distributer_id").toString());
				Map<String,Object> distributerMap = roomDistributerMapper.findRoomDistributerInfoByRqCode(recommendMap.get("rq_code").toString());
				long profit_share_rate = (Long)distributerMap.get("profit_share_rate");
				long totalProfit = tradeBill.getAmount();
				long shareAmount = profit_share_rate *  totalProfit / 10000;
				lcp.setShareAmount(shareAmount);

				//t_room_distributer更新 成交人数+1，分销总收益(分)增加，最后一次成交时间修改
				Map<String,Object> roomDistributerUpdateMap = new HashMap<>();
				roomDistributerUpdateMap.put("done_num",1);
				roomDistributerUpdateMap.put("total_amount",shareAmount);
				roomDistributerUpdateMap.put("done_time",now);
				roomDistributerUpdateMap.put("rq_code",recommendMap.get("rq_code").toString());
				roomDistributerMapper.studentBuyCourseUpdate(roomDistributerUpdateMap);

				//t_room_distributer_recommend更新，done_num+1，course_num+1，update_time更新
				Map<String,Object> roomDistributerRecommendUpdateMap = new HashMap<>();
				roomDistributerRecommendUpdateMap.put("done_num",1);
				roomDistributerRecommendUpdateMap.put("course_num",1);
				roomDistributerRecommendUpdateMap.put("update_time",now);
				roomDistributerRecommendUpdateMap.put("rq_code",recommendMap.get("rq_code").toString());
				roomDistributerRecommendMapper.studentBuyCourseUpdate(roomDistributerRecommendUpdateMap);

			}
		}*/
//TODO 定时任务处理，需更新缓存
/*		//0:课程收益 1:打赏
		if(tradeBill.getProfitType().equals("0")){
			//如果为购买课程，则插入学员表
			CoursesStudents students = new CoursesStudents();
			students.setStudentId(MiscUtils.getUUId());
			students.setUserId(tradeBill.getUserId());
			students.setLecturerId(courses.get("lecturer_id").toString());
			students.setRoomId(courses.get("room_id").toString());
			students.setCourseId(courses.get("course_id").toString());
			//students.setPaymentAmount();//todo
			if(courses.get("course_password") != null){
				students.setCoursePassword(courses.get("course_password").toString());
			}
			//		if(StringUtils.isNotBlank(courseMap.get("course_password"))){
			//			students.setCoursePassword(courseMap.get("course_password"));
			//		}
			students.setStudentType("0");//TODO
			students.setCreateTime(now);
			students.setCreateDate(now);
			coursesStudentsMapper.insert(students);

			//如果缓存中没有课程，则直接更新数据库中的课程信息
			//1 课程在缓存中  2课程不在缓存中
			if(requestMapData.get("courseInCache").toString().equals("2")){
				Map<String,Object> updateCourseMap = new HashMap<>();
				updateCourseMap.put("course_amount", tradeBill.getAmount());
				updateCourseMap.put("student_num", 1);
				updateCourseMap.put("course_id", courses.get("course_id").toString());
				coursesMapper.updateAfterStudentBuyCourse(updateCourseMap);
			}
		}else {
			//1 课程在缓存中  2课程不在缓存中
			if(requestMapData.get("courseInCache").toString().equals("2")){
				//查询该用户是否打赏了该课程
				Map<String,Object> rewardQueryMap = new HashMap<>();
				rewardQueryMap.put("course_id",courses.get("course_id").toString());
				rewardQueryMap.put("user_id",tradeBill.getUserId());
				Map<String,Object> rewardMap = lecturerCoursesProfitMapper.findRewardByUserIdAndCourseId(rewardQueryMap);
				if(MiscUtils.isEmpty(rewardMap)){
					Map<String,Object> updateCourseMap = new HashMap<>();
					updateCourseMap.put("extra_amount", tradeBill.getAmount());
					updateCourseMap.put("extra_num", 1);
					updateCourseMap.put("course_id", courses.get("course_id").toString());
					coursesMapper.updateAfterStudentRewardCourse(updateCourseMap);
				}
			}
		}*/
		//</editor-fold>
		return profitRecord;
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
	public Map<String, Object> findVersionInfoByOS(String plateform) {
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
}
