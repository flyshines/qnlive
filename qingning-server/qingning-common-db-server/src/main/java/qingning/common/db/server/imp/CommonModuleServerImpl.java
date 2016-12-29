
package qingning.common.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import qingning.common.db.persistence.mybatis.*;
import qingning.common.db.persistence.mybatis.entity.*;
import qingning.common.util.MiscUtils;
import qingning.common.util.TenPayUtils;
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

	@Override
	public List<Map<String, Object>> getServerUrls() {
		return serverFunctionMapper.getServerUrls();
	}

	@Override
	public Map<String, Object> getLoginInfoByLoginIdAndLoginType(Map<String, Object> reqMap) {
		return loginInfoMapper.getLoginInfoByLoginIdAndLoginType(reqMap);
	}

	@Override
	public Map<String,String> initializeRegisterUser(Map<String, Object> reqMap) {
		//1.插入t_user
		User user = new User();
		user.setUserId(MiscUtils.getUUId());
		user.setNickName(reqMap.get("nick_name") == null ? null : reqMap.get("nick_name").toString());
		user.setAvatarAddress(reqMap.get("avatar_address") == null ? null : reqMap.get("avatar_address").toString());
		user.setPhoneNumber(reqMap.get("phone_number") == null ? null : reqMap.get("phone_number").toString());
		user.setGender(reqMap.get("gender") == null ? null : reqMap.get("gender").toString());
		//TODO 位置信息未插入
		user.setCourseNum(0L);
		user.setLiveRoomNum(0L);
		Date now = new Date();
		user.setCreateTime(now);
		user.setUpdateTime(now);
		userMapper.insert(user);

		//2.插入login_info
		LoginInfo loginInfo = new LoginInfo();
		loginInfo.setUserId(user.getUserId());
		if(reqMap.get("login_type") != null && reqMap.get("login_type").toString().equals("0")){
			loginInfo.setUnionId(reqMap.get("login_id").toString());
		}
		//loginInfo.setQqId(reqMap.get("qq_id").toString()); //TODO
		loginInfo.setPhoneNumber(reqMap.get("phone_number") == null ? null : reqMap.get("phone_number").toString());
		if(reqMap.get("login_type") != null && reqMap.get("login_type").toString().equals("3")){
			loginInfo.setPasswd(reqMap.get("certification") == null ? null : reqMap.get("certification").toString());
		}
		if(reqMap.get("login_type") != null && reqMap.get("login_type").toString().equals("4")){
			loginInfo.setUnionId(reqMap.get("unionid").toString());
			loginInfo.setWebOpenid(reqMap.get("web_openid").toString());
		}
		if(reqMap.get("m_user_id") != null){
			loginInfo.setmUserId(reqMap.get("m_user_id").toString());
		}
		if(reqMap.get("m_pwd") != null){
			loginInfo.setmPwd(reqMap.get("m_pwd").toString());
		}
		
		loginInfo.setUserRole("normal_user");
		//loginInfo.setSystemRole("normal_user"); //TODO
		loginInfo.setStatus("1");
		//TODO 位置信息未插入
		loginInfo.setCreateTime(now);
		loginInfo.setUpdateTime(now);
		loginInfoMapper.insert(loginInfo);

		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("user_id", loginInfo.getUserId());
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
		return rewardConfigurationMapper.findRewardInfoByRewardId(Long.parseLong(reward_id));
	}

	@Override
	public void insertTradeBill(Map<String, Object> insertMap) {
		//插入t_trade_bill表
		Date now = new Date();
		TradeBill tradeBill = new TradeBill();
		tradeBill.setTradeId(insertMap.get("trade_id").toString());
		tradeBill.setUserId(insertMap.get("user_id").toString());
		tradeBill.setRoomId(insertMap.get("room_id").toString());
		tradeBill.setCourseId(insertMap.get("course_id").toString());
		tradeBill.setAmount(Double.valueOf(insertMap.get("amount").toString()));
		tradeBill.setStatus(insertMap.get("status").toString());
		tradeBill.setCreateTime(now);
		tradeBill.setUpdateTime(now);
		tradeBill.setProfitType(insertMap.get("profit_type").toString());
		tradeBillMapper.insert(tradeBill);
	}

	@Override
	public void updateTradeBill(Map<String, Object> failUpdateMap) {
		Date now = new Date();
		TradeBill tradeBill = new TradeBill();
		tradeBill.setTradeId(failUpdateMap.get("trade_id").toString());
		tradeBill.setStatus(failUpdateMap.get("status").toString());
		tradeBill.setCloseReason(failUpdateMap.get("close_reason").toString());
		tradeBill.setCloseTime(now);
		tradeBill.setUpdateTime(now);
		tradeBillMapper.updateByPrimaryKeySelective(tradeBill);
	}

	@Override
	public void insertPaymentBill(Map<String, Object> insertPayMap) {
		PaymentBill p = new PaymentBill();
		p.setTradeId(insertPayMap.get("trade_id").toString());
		p.setPaymentId(insertPayMap.get("payment_id").toString());
		p.setPaymentType(insertPayMap.get("payment_type").toString());
		p.setStatus(insertPayMap.get("status").toString());
		p.setPrePayNo(insertPayMap.get("pre_pay_no").toString());
		paymentBillMapper.insert(p);
	}

	@Override
	public boolean findTradebillStatus(String outTradeNo) {
		TradeBill t = tradeBillMapper.findByOutTradeNo(outTradeNo);
		//交易状态，0：待付款 1：处理中 2：已完成 3：已关闭
		if(t != null && t.getStatus().equals("2")){
			return true;
		}else {
			return false;
		}
	}

	@Override
	public void updateUserWebOpenIdByUserId(Map<String, Object> updateMap) {
		LoginInfo updateLoginInfo = new LoginInfo();
		updateLoginInfo.setUserId(updateMap.get("user_id").toString());
		updateLoginInfo.setWebOpenid(updateMap.get("web_openid").toString());
		loginInfoMapper.updateByPrimaryKeySelective(updateLoginInfo);
	}


	@Override
	public Map<String,Object> findTradebillByOutTradeNo(String outTradeNo) {
		Map<String,Object> t = tradeBillMapper.findMapByOutTradeNo(outTradeNo);
		return t;
	}

	@Override
	public Map<String,Object> handleWeixinPayResult(SortedMap<String, String> requestMapData) {
		Map<String,Object> resultMap = new HashMap<>();
		Date now = new Date();

		TradeBill tradeBill = tradeBillMapper.findByOutTradeNo(requestMapData.get("out_trade_no"));
		//更新交易表信息
		TradeBill updateBill = new TradeBill();
		updateBill.setTradeId(requestMapData.get("out_trade_no"));
		updateBill.setStatus("2");//交易状态，0：待付款 1：处理中 2：已完成 3：已关闭
		updateBill.setUpdateTime(now);
		tradeBillMapper.updateByPrimaryKeySelective(updateBill);

		//更新支付表信息
		PaymentBill updatePayBill = new PaymentBill();
		updatePayBill.setStatus("2");//支付状态，1：付款中 2：成功 3：失败
		String realPayTimeString = requestMapData.get("time_end");
		Date realPayTime = MiscUtils.parseDateWinxin(realPayTimeString);
		updatePayBill.setUpdateTime(realPayTime);
		paymentBillMapper.updateByTradeIdKeySelective(updatePayBill);

		//更新收益表信息
		Map<String,Object> courses = coursesMapper.findCourseByCourseId(tradeBill.getCourseId());
		PaymentBill paymentBill = paymentBillMapper.selectByTradeId(tradeBill.getTradeId());
		LecturerCoursesProfit lcp = new LecturerCoursesProfit();
		lcp.setProfitId(MiscUtils.getUUId());
		lcp.setCourseId(tradeBill.getCourseId());
		lcp.setRoomId(tradeBill.getRoomId());
		lcp.setLecturerId(courses.get("lecturer_id").toString());
		lcp.setUserId(tradeBill.getUserId());
		lcp.setProfitAmount(tradeBill.getAmount());
		lcp.setProfitType(tradeBill.getProfitType());
		lcp.setCourseTitle(courses.get("course_title").toString());
		lcp.setCreateTime(now);
		lcp.setCreateDate(now);
		lcp.setPaymentId(paymentBill.getPaymentId());
		lcp.setProfitType(paymentBill.getPaymentType());
		lcp.setLecturerName("");//todo
		lecturerCoursesProfitMapper.insert(lcp);

		//0:课程收益 1:打赏
		if(tradeBill.getProfitType().equals("0")){
           //如果为购买课程，则插入学员表
			CoursesStudents students = new CoursesStudents();
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
		}

		resultMap.put("profit_type",tradeBill.getProfitType());
		resultMap.put("pay_user_id",tradeBill.getUserId());
		resultMap.put("pay_amount",tradeBill.getAmount());
		resultMap.putAll(courses);
		return resultMap;
	}
}
