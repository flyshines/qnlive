
package qingning.common.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.util.Constants;
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
	
	@Override
	public List<Map<String, Object>> getServerUrls() {
		return serverFunctionMapper.getServerUrls();
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
		loginInfo.put("phone_number", reqMap.get("phone_number"));
		loginInfo.put("m_user_id", reqMap.get("m_user_id"));
		loginInfo.put("m_pwd", reqMap.get("m_pwd"));
		loginInfo.put("user_role", Constants.USER_ROLE_LISTENER);
		//位置信息未插入由消息服务处理
		loginInfo.put("create_time", now);
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
		loginInfoMapper.updateLoginInfo(record);
	}

	@Override
	public Map<String,Object> findTradebillByOutTradeNo(String outTradeNo) {
		Map<String,Object> tradeBill = tradeBillMapper.findByOutTradeNo(outTradeNo);
		return tradeBill;
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String, Object> handleWeixinPayResult(Map<String, Object> requestMapData) {
		Date now = new Date();
		Map<String,Object> tradeBill = (Map<String,Object>)requestMapData.get("tradeBillInCache");
		Map<String,Object> updateTradeBill = new HashMap<String,Object>();
		//1.更新t_trade_bill 交易信息表
		updateTradeBill.put("trade_id", requestMapData.get("out_trade_no"));
		updateTradeBill.put("status", "2");//交易状态，0：待付款 1：处理中 2：已完成 3：已关闭
		updateTradeBill.put("update_time", now);
		tradeBillMapper.updateTradeBill(updateTradeBill);

		//2.更新t_payment_bill 支付信息表
		Map<String,Object> paymentBill = paymentBillMapper.findPaymentBillByTradeId((String)requestMapData.get("out_trade_no"));
		Map<String,Object> updatePaymentBill = new HashMap<String,Object>();
		updatePaymentBill.put("status", "2");
		Date realPayTime = new Date(MiscUtils.convertObjectToLong(requestMapData.get("time_end")));
		updatePaymentBill.put("update_time", realPayTime);
		updatePaymentBill.put("payment_id", paymentBill.get("payment_id"));
		paymentBillMapper.updatePaymentBill(updatePaymentBill);

		//3.更新 讲师课程收益信息表
		Map<String,Object> profitRecord = new HashMap<String,Object>();
		profitRecord.put("profit_id", MiscUtils.getUUId());
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

		//4.如果该用户属于某个分销员的用户，则更新推荐用户信息 t_room_distributer_recommend
		if("0".equals(tradeBill.get("profit_type"))){
			String distributer_id = null;
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
			student.put("create_time", now);
			student.put("create_date", now);
			coursesStudentsMapper.insertStudent(student);			
		}
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
		insertMap.put("update_time", insertMap.get("now"));
		roomDistributerRecommendMapper.updateRoomDistributerRecommend(insertMap);

		insertMap.put("create_time", insertMap.get("now"));
		roomDistributerRecommendDetailMapper.insertRoomDistributerRecommend(insertMap);
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
}
