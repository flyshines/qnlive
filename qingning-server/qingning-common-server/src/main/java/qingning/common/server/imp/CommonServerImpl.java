package qingning.common.server.imp;

import com.alibaba.fastjson.JSONObject;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang.StringUtils;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ICommonModuleServer;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CommonServerImpl extends AbstractQNLiveServer {
	
	private ICommonModuleServer commonModuleServer;

	@Override
	public void initRpcServer() {
		if(commonModuleServer == null){
			commonModuleServer = this.getRpcService("commonModuleServer");
		}
	}

	private static Auth auth;
	static {
		auth = Auth.create (IMMsgUtil.configMap.get("AK"), IMMsgUtil.configMap.get("SK"));
	}

	@FunctionName("serverTime")
	public Map<String,Object> getServerTime (RequestEntity reqEntity) throws Exception{

		Map<String,Object> resultMap = new HashMap<String, Object>();
		resultMap.put("server_time", System.currentTimeMillis());
		return resultMap;
	}

	@SuppressWarnings("unchecked")
	@FunctionName("userLogin")
	public Map<String,Object> userLogin (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();
		Map<String,Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(reqMap);
		

		int login_type_input = Integer.parseInt(reqMap.get("login_type").toString());

		switch (login_type_input){

			case 0 : //微信登录
				//如果登录信息为空，则进行注册
				if(loginInfoMap == null){
					//注册IM
					Jedis jedis = jedisUtils.getJedis();
					Map<String,String> imResultMap = null;
					try {
						imResultMap =	IMMsgUtil.createIMAccount(reqMap.get("device_id").toString());
					}catch (Exception e){
						//TODO 暂不处理
					}
					//if(imResultMap == null || imResultMap.get("uid") == null || imResultMap.get("password") == null){
						//throw new QNLiveException("120003");
					//}else {
						//初始化数据库相关表
						reqMap.put("m_user_id", imResultMap.get("uid"));
						reqMap.put("m_pwd", imResultMap.get("password"));
						//设置默认用户头像
						if(reqMap.get("avatar_address") == null || StringUtils.isBlank(reqMap.get("avatar_address").toString())){
							reqMap.put("avatar_address",IMMsgUtil.configMap.get("default_avatar_address"));//TODO
						}
						if(reqMap.get("nick_name") == null || StringUtils.isBlank(reqMap.get("nick_name").toString())){
							reqMap.put("avatar_address","用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
						}
						Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);

						//生成access_token，将相关信息放入缓存，构造返回参数
						processLoginSuccess(1, dbResultMap, null, resultMap);
					//}

				}else{
					//构造相关返回参数 TODO
					processLoginSuccess(2, null, loginInfoMap, resultMap);
				}
				break;

			case 1 : //QQ登录
				//TODO
				break;

			case 2 : //手机号登录
				if(loginInfoMap == null){
					//抛出用户不存在
					throw new QNLiveException("120002");
				}else {
					//校验用户名和密码
					//登录成功
					if(reqMap.get("certification").toString().equals(loginInfoMap.get("passwd").toString())){
						//构造相关返回参数 TODO
						processLoginSuccess(2, null, loginInfoMap, resultMap);
					}else{
						//抛出用户名或者密码错误
						throw new QNLiveException("120001");
					}
				}
				break;

		}

		return resultMap;
	}

	/**
	 * 微信授权code登录
	 * @param reqEntity
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@FunctionName("weixinCodeUserLogin")
	public Map<String,Object> weixinCodeUserLogin (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();
		String code = reqMap.get("login_id").toString();
		//1.传递授权code及相关参数，调用微信验证code接口
		JSONObject getCodeResultJson = WeiXinUtil.getUserInfoByCode(code);
		if(getCodeResultJson == null || getCodeResultJson.getInteger("errcode") != null || getCodeResultJson.getString("openid") == null){
			throw new QNLiveException("120008");
		}

		//1.2如果验证成功，则得到用户的union_id和用户的access_token。
		//1.2.1根据 union_id查询数据库
		String openid = getCodeResultJson.getString("openid");
		Map<String,Object> queryMap = new HashMap<>();
		queryMap.put("login_type","4");//4.微信code方式登录
		queryMap.put("web_openid",openid);
		Map<String,Object> loginInfoMap = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);

		//1.2.1.1如果用户存在则进行登录流程
		if(loginInfoMap != null){
			processLoginSuccess(2, null, loginInfoMap, resultMap);
		}else {
			//1.2.1.2如果用户不存在，则根据用户的open_id和用户的access_token调用微信查询用户信息接口，得到用户的头像、昵称等相关信息
			String userWeixinAccessToken = getCodeResultJson.getString("access_token");
			JSONObject userJson = WeiXinUtil.getUserInfoByAccessToken(userWeixinAccessToken, openid);
			// 根据得到的相关用户信息注册用户，并且进行登录流程。
			if(userJson == null || userJson.getInteger("errcode") != null || userJson.getString("unionid") == null){
				throw new QNLiveException("120008");
			}

			String nickname = userJson.getString("nickname");
			String sex = userJson.getString("sex");
			String headimgurl = userJson.getString("headimgurl");

			Jedis jedis = jedisUtils.getJedis();
			Map<String,String> imResultMap = null;
			try {
				imResultMap = IMMsgUtil.createIMAccount("weixinCodeLogin");
			}catch (Exception e){
				//TODO 暂不处理
			}
			//if(imResultMap == null || imResultMap.get("uid") == null || imResultMap.get("password") == null){
			//throw new QNLiveException("120003");
			//}else {
			//初始化数据库相关表
			reqMap.put("m_user_id", imResultMap.get("uid"));
			reqMap.put("m_pwd", imResultMap.get("password"));
			//设置默认用户头像
			if(MiscUtils.isEmpty(headimgurl)){
				reqMap.put("avatar_address",IMMsgUtil.configMap.get("default_avatar_address"));//TODO
			}else {
				reqMap.put("avatar_address", headimgurl);//TODO
			}

			if(MiscUtils.isEmpty(nickname)){
				reqMap.put("nick_name","用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
			}else {
				reqMap.put("nick_name", nickname);
			}

			if(MiscUtils.isEmpty(sex)){
				reqMap.put("gender","2");//TODO
			}

			//微信性别与本系统性别转换
			//微信用户性别 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
			if(sex.equals("1")){
				reqMap.put("gender","1");//TODO
			}
			if(sex.equals("2")){
				reqMap.put("gender","0");//TODO
			}
			if(sex.equals("0")){
				reqMap.put("gender","2");//TODO
			}

			String unionid =  userJson.getString("unionid");
			reqMap.put("unionid",unionid);
			reqMap.put("web_openid",openid);
			Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);

			//生成access_token，将相关信息放入缓存，构造返回参数
			processLoginSuccess(1, dbResultMap, null, resultMap);
			//}
		}

		return resultMap;
	}

	/**
	 *
	 * @param type 1新注册用户处理方式，2老用户处理方式
	 * @param dbResultMap 当type为1时，传入该值，该值为新用户注册后返回的信息；当type为2时该值传入null
	 * @param loginInfoMap
	 * @param resultMap service要返回的map
	 */
	private void processLoginSuccess(Integer type, Map<String,String> dbResultMap, Map<String,Object> loginInfoMap,
									 Map<String,Object> resultMap) {

		Jedis jedis = jedisUtils.getJedis();
		String last_login_date = (new Date()).getTime() + "";
		String user_id = null;
		String m_user_id = null;
		String m_pwd = null;
		Map<String,String> cacheMap = new HashMap<String,String>();

		//新注册用户,重新查询loginInfo
		if(type == 1){
			user_id = dbResultMap.get("user_id");
			loginInfoMap = commonModuleServer.findLoginInfoByUserId(user_id);

			//老用户
		}else if(type == 2){
			user_id = loginInfoMap.get("user_id").toString();
		}

		//1.将objectMap转为StringMap
		m_user_id = loginInfoMap.get("m_user_id") == null ? null : loginInfoMap.get("m_user_id").toString();
		m_pwd = loginInfoMap.get("m_pwd") == null ? null : loginInfoMap.get("m_pwd").toString();
		MiscUtils.converObjectMapToStringMap(loginInfoMap, cacheMap);

		//2.根据相关信息生成access_token
		String access_token = AccessTokenUtil.generateAccessToken(user_id, last_login_date);
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("access_token", access_token);
		String process_access_token = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_ACCESS_TOKEN, map);
		jedis.hmset(process_access_token, cacheMap);
		jedis.expire(process_access_token, 10800);

		//3.增加相关返回参数
		resultMap.put("access_token", access_token);
		resultMap.put("m_user_id", m_user_id);
		resultMap.put("m_pwd", m_pwd);
		resultMap.put("user_id", user_id);
		Map<String,Object> userMap = commonModuleServer.findUserInfoByUserId(user_id);
		resultMap.put("avatar_address", userMap.get("avatar_address"));
		resultMap.put("nick_name", userMap.get("nick_name"));
	}

	@SuppressWarnings("unchecked")
	@FunctionName("qiNiuUploadToken")
	public Map<String,Object> getQiNiuUploadToken (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();
		long expiredTime = 3600;
		String token = null;
		String url = null;
		if("1".equals (reqMap.get("upload_type"))){ //图片
			url = IMMsgUtil.configMap.get("images_space_domain_name");

			token = auth.uploadToken(IMMsgUtil.configMap.get("image_space"), null, expiredTime, new StringMap()
					.putNotEmpty("returnBody", "{\"key\": $(key), \"hash\": $(etag), \"width\": $(imageInfo.width), \"height\": $(imageInfo.height)}"));

		} else if("2".equals (reqMap.get("upload_type"))){ //音频
			url = IMMsgUtil.configMap.get("audio_space_domain_name");

			token = auth.uploadToken(IMMsgUtil.configMap.get("audio_space"), null, expiredTime, (StringMap)null);

		}

		resultMap.put("upload_token", token);
		resultMap.put("access_prefix_url", url);
		return resultMap;
	}

	@SuppressWarnings("unchecked")
	@FunctionName("weiXinConfiguration")
	public Map<String,String> getWeiXinConfiguration (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		String JSApiTIcket = WeiXinUtil.getJSApiTIcket(jedisUtils.getJedis());
		return WeiXinUtil.sign(JSApiTIcket, reqMap.get("url").toString());
	}


	@SuppressWarnings("unchecked")
	@FunctionName("userInfo")
	public Map<String,Object> getUserInfo (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();

		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		//1:个人中心信息 2：个人基本信息
		String queryType = reqMap.get("query_type").toString();
		if(queryType.equals("1")){
			//TODO
		}else if(queryType.equals("2")){
			Map<String,Object> userMap = commonModuleServer.findUserInfoByUserId(userId);
			if(MiscUtils.isEmpty(userMap)){
				throw new QNLiveException("120002");
			}

			Map<String,Object> loginInfoMap = commonModuleServer.findLoginInfoByUserId(userId);
			if(MiscUtils.isEmpty(loginInfoMap)){
				throw new QNLiveException("120002");
			}

			resultMap.put("access_token", reqEntity.getAccessToken());
			resultMap.put("m_user_id", loginInfoMap.get("m_user_id"));
			resultMap.put("m_pwd", loginInfoMap.get("m_pwd"));
			resultMap.put("user_id", userId);
			resultMap.put("avatar_address", userMap.get("avatar_address"));
			resultMap.put("nick_name", userMap.get("nick_name"));
			return resultMap;
		}

		return resultMap;
	}

	@SuppressWarnings("unchecked")
	@FunctionName("generateWeixinPayBill")
	public Map<String,Object> generateWeixinPayBill (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<String, Object>();

		//1.检测课程是否存在，课程不存在则给出提示（ 课程不存在，120009）
		String courseId = reqMap.get("course_id").toString();
		Map<String, Object> map = new HashMap<>();
		map.put(Constants.CACHED_KEY_COURSE_FIELD, courseId);
		String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);
		Jedis jedis = jedisUtils.getJedis();
		Map<String,String> courseMap = jedis.hgetAll(courseKey);

		//1.1如果课程不在缓存中，则查询数据库
		if(MiscUtils.isEmpty(courseMap)){
			Map<String,Object> courseObjectMap = commonModuleServer.findCourseByCourseId(courseId);
			if(courseMap == null){
				throw new QNLiveException("120009");
			}
			courseMap = new HashMap<>();
			MiscUtils.converObjectMapToStringMap(courseObjectMap,courseMap);
		}

		//2.如果支付类型为打赏，则检测内存中的打赏类型是否存在，如果不存在则给出提示（120010，打赏类型不存在）
		String profit_type = reqMap.get("profit_type").toString();
		String reward_id = null;
		Map<String,Object> rewardInfoMap = null;
		//0:课程收益 1:打赏
		if(profit_type.equals("1")){
			if(reqMap.get("reward_id") == null || StringUtils.isBlank(reqMap.get("reward_id").toString())){
				throw new QNLiveException("000100");
			}
			reward_id = reqMap.get("reward_id").toString();
			rewardInfoMap = commonModuleServer.findRewardInfoByRewardId(reward_id);
			if(MiscUtils.isEmpty(rewardInfoMap)){
				throw new QNLiveException("120010");
			}
		}

		//3.插入t_trade_bill表 交易信息表
		String goodName = null;
		Integer totalFee = 0;
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		Map<String,Object> insertMap = new HashMap<>();
		insertMap.put("user_id",userId);
		insertMap.put("room_id",courseMap.get("room_id"));
		insertMap.put("course_id",courseMap.get("course_id"));
		//判断类型为 0:课程收益 1:打赏
		if(profit_type.equals("1")){
			insertMap.put("amount", rewardInfoMap.get("amount").toString());
			BigDecimal rewardBigDecimal = new BigDecimal(rewardInfoMap.get("amount").toString());
			totalFee = (rewardBigDecimal.multiply(new BigDecimal(100))).intValue();
			goodName = new String(MiscUtils.getConfigByKey("weixin_pay_reward_course_good_name").getBytes(), "UTF-8")+courseMap.get("course_id");
		}else if(profit_type.equals("0")){
			insertMap.put("amount", courseMap.get("course_price"));
			BigDecimal coursePriceBigDecimal = new BigDecimal(courseMap.get("course_price"));
			totalFee = (coursePriceBigDecimal.multiply(new BigDecimal(100))).intValue();
			goodName = new String(MiscUtils.getConfigByKey("weixin_pay_buy_course_good_name").getBytes(), "UTF-8")+courseMap.get("course_id");
		}
		insertMap.put("status","0");
		String tradeId = MiscUtils.getUUId();
		insertMap.put("trade_id",tradeId);
		commonModuleServer.insertTradeBill(insertMap);

		Map<String,Object> userMap = commonModuleServer.findLoginInfoByUserId(userId);

		//4.调用微信生成预付单接口
		String terminalIp = reqMap.get("remote_ip_address").toString();
		String tradeType = "JSAPI";
		String outTradeNo = tradeId;
		String openid = userMap.get("web_openid").toString();
		Map<String, String> payResultMap = TenPayUtils.sendPrePay(goodName, totalFee, terminalIp, tradeType, outTradeNo, openid);

		//5.处理生成微信预付单接口
		if (payResultMap.get ("return_code").equals ("FAIL")) {
			//更新交易表
			Map<String,Object> failUpdateMap = new HashMap<>();
			failUpdateMap.put("status","3");
			failUpdateMap.put("close_reason","生成微信预付单失败 "+payResultMap.get("return_msg"));
			failUpdateMap.put("trade_id",tradeId);
			commonModuleServer.updateTradeBill(failUpdateMap);

			throw new QNLiveException("120012");
		} else if (payResultMap.get ("result_code").equals ("FAIL")) {
			//更新交易表
			Map<String,Object> failUpdateMap = new HashMap<>();
			failUpdateMap.put("status","3");
			failUpdateMap.put("close_reason","生成微信预付单失败 "+ payResultMap.get ("err_code_des"));
			failUpdateMap.put("trade_id",tradeId);
			commonModuleServer.updateTradeBill(failUpdateMap);

			throw new QNLiveException("120012");
		} else {
			//成功，则需要插入支付表
			Map<String,Object> insertPayMap = new HashMap<>();
			insertPayMap.put("trade_id",tradeId);
			insertPayMap.put("payment_id",userId);
			insertPayMap.put("payment_type","0");
			insertPayMap.put("status","1");
			insertPayMap.put("pre_pay_no",payResultMap.get("prepay_id"));
			commonModuleServer.insertPaymentBill(insertPayMap);

			//返回相关参数给前端
			resultMap.put("app_id",MiscUtils.getConfigByKey("appid"));
			resultMap.put("prepay_id", payResultMap.get("prepay_id"));
			resultMap.put("pay_sign", payResultMap.get("sign"));
			resultMap.put("sign_type", "MD5");
			resultMap.put("nonce_str", payResultMap.get("nonce_str"));
			return resultMap;
		}
	}

}
