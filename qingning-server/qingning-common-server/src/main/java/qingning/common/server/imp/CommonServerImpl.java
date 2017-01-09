package qingning.common.server.imp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang.StringUtils;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.server.other.ReadCourseOperation;
import qingning.common.server.other.ReadDistributerOperation;
import qingning.common.server.other.ReadLiveRoomOperation;
import qingning.common.server.other.ReadUserOperation;
import qingning.common.util.*;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.manager.ICommonModuleServer;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.*;

public class CommonServerImpl extends AbstractQNLiveServer {
	
	private ICommonModuleServer commonModuleServer;
	private ReadDistributerOperation readDistributerOperation;
	private ReadUserOperation readUserOperation;
	private ReadCourseOperation readCourseOperation;
	private ReadLiveRoomOperation readLiveRoomOperation;

	@Override
	public void initRpcServer() {
		if(commonModuleServer == null){
			commonModuleServer = this.getRpcService("commonModuleServer");
			readDistributerOperation = new ReadDistributerOperation(commonModuleServer);
			readUserOperation = new ReadUserOperation(commonModuleServer);
			readCourseOperation = new ReadCourseOperation(commonModuleServer);
			readLiveRoomOperation = new ReadLiveRoomOperation(commonModuleServer);
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
		Map<String,Object> resultMap = new HashMap<>();
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
			return resultMap;
		}else {
			//1.2.1.2如果用户不存在，则根据用户的open_id和用户的access_token调用微信查询用户信息接口，得到用户的头像、昵称等相关信息
			String userWeixinAccessToken = getCodeResultJson.getString("access_token");
			JSONObject userJson = WeiXinUtil.getUserInfoByAccessToken(userWeixinAccessToken, openid);
			// 根据得到的相关用户信息注册用户，并且进行登录流程。
			if(userJson == null || userJson.getInteger("errcode") != null || userJson.getString("unionid") == null){
				throw new QNLiveException("120008");
			}

			queryMap.clear();
			queryMap.put("login_type","0");//0.微信方式登录
			queryMap.put("login_id",userJson.getString("unionid"));
			Map<String,Object> loginInfoMapFromUnionid = commonModuleServer.getLoginInfoByLoginIdAndLoginType(queryMap);
			if(loginInfoMapFromUnionid != null){
				//将open_id更新到login_info表中
				Map<String,Object> updateMap = new HashMap<>();
				updateMap.put("user_id", loginInfoMapFromUnionid.get("user_id").toString());
				updateMap.put("web_openid", openid);
				commonModuleServer.updateUserWebOpenIdByUserId(updateMap);
				processLoginSuccess(2, null, loginInfoMapFromUnionid, resultMap);
				return resultMap;
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
			reqMap.put("login_type","4");
			Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);

			//生成access_token，将相关信息放入缓存，构造返回参数
			processLoginSuccess(1, dbResultMap, null, resultMap);
			//}

			return resultMap;
		}
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
			StringMap putPolicy = new StringMap()
					.put("persistentPipeline","qnlive-audio-convert")//设置私有队列处理
					.put("persistentOps", "avthumb/mp3/ab/64k")
					.put("persistentNotifyUrl",MiscUtils.getConfigByKey("qiniu-audio-transfer-persistent-notify-url"));//转码策略

			token = auth.uploadToken(IMMsgUtil.configMap.get("audio_space"), null, expiredTime, putPolicy);

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
			reqMap.put("user_id", userId);
			Map<String,String> values = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedisUtils);
			resultMap.put("avatar_address", values.get("avatar_address"));
			resultMap.put("nick_name", values.get("nick_name"));			
			resultMap.put("course_num", MiscUtils.convertObjToObject(values.get("course_num"), Constants.SYSLONG, "course_num", 0l));
			resultMap.put("live_room_num", MiscUtils.convertObjToObject(values.get("live_room_num"), Constants.SYSLONG, "live_room_num", 0l));
			resultMap.put("today_distributer_amount",MiscUtils.convertObjToObject(values.get("today_distributer_amount"), Constants.SYSLONG, "today_distributer_amount", 0l));
			resultMap.put("update_time", MiscUtils.convertObjToObject(values.get("update_time"),Constants.SYSLONG,"update_time", 0l));			
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
	public Map<String,String> generateWeixinPayBill (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();

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
			if(courseObjectMap == null){
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
			insertMap.put("amount", reqMap.get("reward_amount"));
			totalFee = ((Long)reqMap.get("reward_amount")).intValue();
			goodName = new String(MiscUtils.getConfigByKey("weixin_pay_reward_course_good_name").getBytes(), "UTF-8")+courseMap.get("course_id");
		}else if(profit_type.equals("0")){
			insertMap.put("amount", courseMap.get("course_price"));
			totalFee = Integer.parseInt(courseMap.get("course_price"));
			goodName = new String(MiscUtils.getConfigByKey("weixin_pay_buy_course_good_name").getBytes(), "UTF-8")+courseMap.get("course_id");
		}
		insertMap.put("status","0");
		String tradeId = MiscUtils.getUUId();
		insertMap.put("trade_id",tradeId);
		insertMap.put("profit_type",profit_type);
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

			throw new QNLiveException("120015");
		} else if (payResultMap.get ("result_code").equals ("FAIL")) {
			//更新交易表
			Map<String,Object> failUpdateMap = new HashMap<>();
			failUpdateMap.put("status","3");
			failUpdateMap.put("close_reason","生成微信预付单失败 "+ payResultMap.get ("err_code_des"));
			failUpdateMap.put("trade_id",tradeId);
			commonModuleServer.updateTradeBill(failUpdateMap);

			throw new QNLiveException("120015");
		} else {
			//成功，则需要插入支付表
			Map<String,Object> insertPayMap = new HashMap<>();
			insertPayMap.put("trade_id",tradeId);
			insertPayMap.put("payment_id",MiscUtils.getUUId());
			insertPayMap.put("payment_type","0");
			insertPayMap.put("status","1");
			insertPayMap.put("pre_pay_no",payResultMap.get("prepay_id"));
			insertPayMap.put("create_time",new Date());
			commonModuleServer.insertPaymentBill(insertPayMap);

			//返回相关参数给前端.
			SortedMap<String,String> resultMap = new TreeMap<>();
			resultMap.put("appId",MiscUtils.getConfigByKey("appid"));
			resultMap.put("nonceStr", payResultMap.get("random_char"));
			//resultMap.put("nonceStr", "5K8264ILTKCH16CQ2502SI8ZNMTM67VS");
			resultMap.put("package", "prepay_id="+payResultMap.get("prepay_id"));
			resultMap.put("signType", "MD5");
			resultMap.put("timeStamp",System.currentTimeMillis()/1000 + "");
			String paySign  = TenPayUtils.getSign(resultMap);
			resultMap.put ("paySign", paySign);

			return resultMap;
		}
	}

	@SuppressWarnings("unchecked")
	@FunctionName("handleWeixinPayResult")
	public String handleWeixinPayResult (RequestEntity reqEntity) throws Exception{
		String resultStr = TenPayConstant.FAIL;
		SortedMap<String,String> requestMapData = (SortedMap<String,String>)reqEntity.getParam();
		String outTradeNo = requestMapData.get("out_trade_no");

		Map<String,Object> billMap = commonModuleServer.findTradebillByOutTradeNo(outTradeNo);
		if(billMap != null && billMap.get("status").equals("2")){
			System.out.println("====>　已经处理完成, 不需要继续。流水号是: "+outTradeNo );
			return TenPayConstant.SUCCESS;
		}

		//if (TenPayUtils.isValidSign(requestMapData)){// MD5签名成功，处理课程打赏\购买课程等相关业务
		if(true){
			System.out.println(" ===> 微信notify Md5 验签成功 <=== ");

			if("SUCCESS".equals(requestMapData.get("return_code")) &&
					"SUCCESS".equals(requestMapData.get("result_code"))){
				//更新交易表信息
				//更新支付表信息
				//更新收益表信息
				//如果为购买课程，则插入学员表
				Map<String,Object> handleResultMap = commonModuleServer.handleWeixinPayResult(requestMapData);

				//如果为打赏，则需要发送推送
				String profit_type = handleResultMap.get("profit_type").toString();
				//0:课程收益 1:打赏
				Jedis jedis = jedisUtils.getJedis();
				Map<String,Object> map = new HashMap<>();
				map.put(Constants.CACHED_KEY_LECTURER_FIELD, handleResultMap.get("lecturer_id").toString());
				String lecturerKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER, map);

				map.put(Constants.CACHED_KEY_COURSE_FIELD, handleResultMap.get("course_id").toString());
				String courseKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE, map);

				//处理缓存中的收益
				//讲师缓存中的收益
				Long amountLong = (Long)handleResultMap.get("pay_amount");
				jedis.hincrBy(lecturerKey, "total_amount", amountLong.longValue());

				if(profit_type.equals("1")){
					String mGroupId = handleResultMap.get("im_course_id").toString();
					Map<String,Object> payUserMap = commonModuleServer.findUserInfoByUserId(handleResultMap.get("pay_user_id").toString());
					Map<String,Object> lecturerMap = commonModuleServer.findUserInfoByUserId(handleResultMap.get("lecturer_id").toString());
					String message = payUserMap.get("nick_name") + "打赏了" + lecturerMap.get("nick_name") + handleResultMap.get("pay_amount") + "元";
					long currentTime = System.currentTimeMillis();
					String sender = "system";
					Map<String,Object> infomation = new HashMap<>();
					infomation.put("course_id", handleResultMap.get("course_id"));
					infomation.put("creator_id", lecturerMap.get(handleResultMap.get("lecturer_id").toString()));
					infomation.put("message", message);
					infomation.put("send_type", "4");//4.打赏信息
					infomation.put("message_type", "1");
					infomation.put("create_time", currentTime);
					Map<String,Object> messageMap = new HashMap<>();
					messageMap.put("msg_type","1");
					messageMap.put("send_time",currentTime);
					messageMap.put("information",infomation);
					String content = JSON.toJSONString(messageMap);
					IMMsgUtil.sendMessageInIM(mGroupId, content, "", sender);

					if(jedis.exists(courseKey)) {
						jedis.hincrBy(courseKey, "extra_num", 1);
						jedis.hincrBy(courseKey, "extra_amount", amountLong.longValue());
					}

				}else if(profit_type.equals("0")){
					//增加课程人数
					jedis.hincrBy(lecturerKey, "total_student_num", 1);
					if(jedis.exists(courseKey)) {
						jedis.hincrBy(courseKey, "student_num", 1);
						jedis.hincrBy(courseKey, "course_amount", amountLong.longValue());
					}
				}

				//直播间缓存中的收益
				map.put(Constants.CACHED_KEY_LECTURER_FIELD, handleResultMap.get("lecturer_id").toString());
				map.put(Constants.FIELD_ROOM_ID, handleResultMap.get("room_id").toString());
				String liveRoomKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SPECIAL_LECTURER_ROOM, map);
				jedis.hincrBy(liveRoomKey, "total_amount", amountLong.longValue());


				resultStr = TenPayConstant.SUCCESS;
				System.out.println("====> 微信支付流水: "+outTradeNo+" 更新成功, return success === ");

			}else{
				System.out.println("==> 微信支付失败 ,流水 ：" + outTradeNo);
				resultStr = TenPayConstant.FAIL;
			}

		}else {// MD5签名失败
			System.out.println("==> fail -Md5 failed");
			resultStr = TenPayConstant.FAIL;
		}
		return resultStr;
	}
	
	@FunctionName("commonDistribution")
	public Map<String,Object> getCommonDistribution(RequestEntity reqEntity) throws Exception{
		@SuppressWarnings("unchecked")
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();		
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		//int page_count = (Integer)reqMap.get("page_count");
		Date record_date = (Date)reqMap.get("record_date");
		reqMap.put("create_time", record_date);
		reqMap.put("distributer_id", userId);
		Map<String,String> distributer = CacheUtils.readDistributer(userId, reqEntity, readDistributerOperation, jedisUtils, true);
		if(MiscUtils.isEmpty(distributer)){
			throw new QNLiveException("120012");
		}
		Map<String,Object> resultMap = new HashMap<String, Object>();
		resultMap.put("total_amount", distributer.get("total_amount"));
		List<Map<String,Object>> rooom_list = commonModuleServer.findDistributionInfoByDistributerId(reqMap);
		if(!MiscUtils.isEmpty(rooom_list)){
			Date currentDate = new Date(System.currentTimeMillis());
			for(Map<String,Object> values:rooom_list){
				Date endDate = (Date)values.get("end_date");
				if(!MiscUtils.isEmpty(endDate) && endDate.before(currentDate)){
					values.put("effective_time", null);
				}
			}
		}
		resultMap.put("room_list", rooom_list);
		return resultMap;
	}
	
	@FunctionName("roomDistributerRecommendInfo")
	public Map<String,Object> getRoomDistributerRecommendInfo(RequestEntity reqEntity) throws Exception{
		@SuppressWarnings("unchecked")
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();		
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		String room_id = (String)reqMap.get("room_id");
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("room_id", room_id);
		String distributer_id = (String)reqMap.get("distributer_id");
		boolean isLecturer = false;
		if(MiscUtils.isEmpty(distributer_id)){
			distributer_id=userId;					
		} else {
			parameters.put("lecturer_id", userId);
			isLecturer=true;
		}
		parameters.put("distributer_id", distributer_id);
		List<Map<String,Object>> rooom_list = commonModuleServer.findDistributionInfoByDistributerId(parameters);
		Map<String,Object> result = new HashMap<String,Object>();
		long recommend_num = 0l;
		if(MiscUtils.isEmpty(rooom_list)){
			if(isLecturer){
				throw new QNLiveException("100028");
			}
			result.put("recommend_num", 0l);
		} else {
			Object recommend_num_tmp = rooom_list.get(0).get("recommend_num");
			if(recommend_num_tmp!=null){
				recommend_num = Long.parseLong(recommend_num_tmp.toString());
			}
			result.put("recommend_num", recommend_num);
		}
		if(recommend_num>0){
			parameters.clear();
			parameters.put("room_id", room_id);
			parameters.put("distributer_id", distributer_id);
			parameters.put("page_count", reqMap.get("page_count"));
			parameters.put("position", reqMap.get("position"));
			List<Map<String,Object>> recommend_list = commonModuleServer.findRoomDistributerRecommendInfo(parameters);
			result.put("recommend_list", recommend_list);
		}
		return result;
	}
	
	@FunctionName("roomDistributionInfo")
	public Map<String,Object> getRoomDistributionInfo(RequestEntity reqEntity) throws Exception{
		@SuppressWarnings("unchecked")
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();		
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		String room_id = (String)reqMap.get("room_id");				
		reqMap.put("distributer_id", userId);
		Map<String,String> distributer = CacheUtils.readDistributer(userId, reqEntity, readDistributerOperation, jedisUtils, true);
		if(MiscUtils.isEmpty(distributer)){
			throw new QNLiveException("120012");
		}
		Map<String,Object> result = new HashMap<String,Object>();
		result.put("total_amount", distributer.get("total_amount"));
		
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("room_id", room_id);
		parameters.put("distributer_id", userId);
		parameters.put("page_count", reqMap.get("page_count"));
		parameters.put("start_time", reqMap.get("start_time"));
		
		List<Map<String,Object>> course_list = commonModuleServer.findRoomDistributerCourseInfo(parameters);
		if(!MiscUtils.isEmpty(course_list)){
			Date currentDate = new Date(System.currentTimeMillis());
			for(Map<String,Object> values:course_list){
				Date endDate = (Date)values.get("end_date");
				if(!MiscUtils.isEmpty(endDate) && endDate.before(currentDate)){
					values.put("effective_time", null);
				}
			}
		}
		result.put("course_list", course_list);
		return result;
	}
	
	@FunctionName("courseDistributionInfo")
	public Map<String,Object> getCourseDistributionInfo(RequestEntity reqEntity) throws Exception{
		@SuppressWarnings("unchecked")
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();		
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		String course_id = (String)reqMap.get("course_id");
		Long position = (Long)reqMap.get("position");
		
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("course_id", course_id);
		parameters.put("distributer_id", userId);		
		List<Map<String,Object>> course_list = commonModuleServer.findRoomDistributerCourseInfo(parameters);
		if(MiscUtils.isEmpty(course_list)){
			throw new QNLiveException("120013");
		}
		Map<String,Object> result = course_list.get(0);
		
		parameters.put("position", position);
		parameters.put("page_count", reqMap.get("page_count"));
		
		List<Map<String,Object>> list = commonModuleServer.findRoomDistributerCourseDetailsInfo(parameters);
		result.put("profit_list", list);
		return result;
	}
	
	@FunctionName("roomDistributionShareInfo")
	public Map<String,Object> getRoomDistributionShareInfo(RequestEntity reqEntity) throws Exception{
		@SuppressWarnings("unchecked")
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();		
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		reqMap.put("distributer_id", userId);		
		List<Map<String,Object>> list = commonModuleServer.findDistributionInfoByDistributerId(reqMap);
		if(MiscUtils.isEmpty(list)){
			throw new QNLiveException("120014");
		}
		return list.get(0);
	}
	
	@FunctionName("updateUserInfo")
	public Map<String,Object> updateUserInfo(RequestEntity reqEntity) throws Exception{
		@SuppressWarnings("unchecked")
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();		
		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		
		reqMap.put("user_id", userId);
		Map<String,String> values = CacheUtils.readUser(userId, reqEntity, readUserOperation, jedisUtils);
		String update_time_str = values.get("update_time");
		Long update_time = (Long)reqMap.get("update_time");
		if(!update_time.toString().equals(update_time_str)){
			throw new QNLiveException("000104");
		}
		String nick_name = (String)reqMap.get("nick_name");
		String avatar_address = (String)reqMap.get("avatar_address");
		if(!MiscUtils.isEmpty(nick_name) || !MiscUtils.isEmpty(avatar_address)){
			Map<String,Object> parameters = new HashMap<String,Object>();
			parameters.put("nick_name", nick_name);
			parameters.put("avatar_address", avatar_address);
			parameters.put("updateTime", new Date(update_time));
			parameters.put("userId", userId);
			int count = commonModuleServer.updateUser(parameters);
			if(count <1){
				throw new QNLiveException("000104");
			} else {
				Map<String,Object> parameter = new HashMap<String,Object>();
				parameter.put(Constants.CACHED_KEY_USER_FIELD, userId);
				String cachedKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER, parameter);
				jedisUtils.getJedis().del(cachedKey);
			}
		}
		return new HashMap<String,Object>();
	}


	@SuppressWarnings("unchecked")
	@FunctionName("getCourseInviteCard")
	public Map<String,Object> getCourseInviteCard (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<>();

		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		String courseId = reqMap.get("course_id").toString();
		Map<String,Object> userMap = commonModuleServer.findUserInfoByUserId(userId);
		resultMap.put("avatar_address",userMap.get("userMap"));
		resultMap.put("nick_name",userMap.get("nick_name"));

		Map<String,String> courseMap =  CacheUtils.readCourse(courseId, reqEntity, readCourseOperation, jedisUtils, false);
		resultMap.put("course_title",courseMap.get("course_title"));
		resultMap.put("start_time",courseMap.get("start_time"));
		resultMap.put("share_url",MiscUtils.getConfigByKey("course_share_url_pre_fix")+courseId);

		return resultMap;
	}

	@SuppressWarnings("unchecked")
	@FunctionName("getRoomInviteCard")
	public Map<String,Object> getRoomInviteCard (RequestEntity reqEntity) throws Exception{//TODO
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<>();

		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		String roomId = reqMap.get("room_id").toString();
		Map<String,Object> userMap = commonModuleServer.findUserInfoByUserId(userId);
		resultMap.put("avatar_address",userMap.get("avatar_address"));
		resultMap.put("nick_name",userMap.get("nick_name"));

		Map<String,String> liveRoomMap = CacheUtils.readLiveRoom(roomId,reqEntity,readLiveRoomOperation,jedisUtils,true);
		resultMap.put("room_name",liveRoomMap.get("room_name"));

		//查询该用户是否为该直播间的分销员
		Map<String,Object> queryMap = new HashMap<>();
		queryMap.put("distributer_id", userId);
		queryMap.put("room_id", roomId);
		List<Map<String,Object>> roomDistributerList = commonModuleServer.findRoomDistributionInfoByDistributerId(queryMap);

		boolean isDistributer = false;
		String recommend_code = null;
		long now = MiscUtils.getEndDateOfToday().getTime();
		if (! MiscUtils.isEmpty(roomDistributerList)) {
			for(Map<String,Object> map : roomDistributerList){
				//0:永久有效 1: 1个月内有效 2: 3个月内有效 3: 6个月内有效 4: 9个月内有效 5:一年有效 6: 两年有效
				if(map.get("effective_time").toString().equals("0")){
					isDistributer = true;
					recommend_code = map.get("rq_code").toString();
					break;
				}else {
					if(map.get("end_date") != null){
						Date end_date = (Date)map.get("end_date");
						if(end_date.getTime() >= now){
							isDistributer = true;
							recommend_code = map.get("rq_code").toString();
							break;
						}
					}
				}
			}
		}
		String share_url = null;
		//是分销员
		if(isDistributer == true){
			share_url = MiscUtils.getConfigByKey("live_room_share_url_pre_fix")+roomId+"&recommend_code="+recommend_code;
		}else {
			//不是分销员
			share_url = MiscUtils.getConfigByKey("live_room_share_url_pre_fix")+roomId;
		}
		resultMap.put("share_url",share_url);
		return resultMap;
	}

	@SuppressWarnings("unchecked")
	@FunctionName("distributorsRecommendUser")
	public Map<String,Object> distributersRecommendUser (RequestEntity reqEntity) throws Exception{
		Map<String, Object> reqMap = (Map<String, Object>)reqEntity.getParam();
		Map<String,Object> resultMap = new HashMap<>();
		Date now = new Date();

		String userId = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());
		String rqCode = reqMap.get("recommend_code").toString();
		//0.读取直播间分销员信息
		Map<String,Object> roomDistributerMap = commonModuleServer.findRoomDistributerInfoByRqCode(rqCode);
		if(! MiscUtils.isEmpty(roomDistributerMap)){
			boolean isValidate =false;
			Date todayEnd = MiscUtils.getEndDateOfToday();
			Date end_date = null;
			if(roomDistributerMap.get("effective_time").equals("0")){
				isValidate = true;
			}else {
				end_date = (Date)roomDistributerMap.get("end_date");

				//1.判断该分销员是否有效，有效则进行下一步验证
				if(end_date.getTime() >= todayEnd.getTime()){
					isValidate = true;
				}
			}

			if(isValidate == true){
				String room_id = roomDistributerMap.get("room_id").toString();
				Map<String,String> roomMap = CacheUtils.readLiveRoom(room_id,reqEntity,readLiveRoomOperation,jedisUtils,true);
				String lecturerId = roomMap.get("lecturer_id");
				//2.如果访问该推广链接的用户不是讲师,不是分销员，则进行下一步验证，验证用户目前是否已经属于了有效分销员
				if((! userId.equals(lecturerId)) && (! userId.equals( roomDistributerMap.get("distributer_id").toString()))){
					Map<String,Object> queryMap = new HashMap<>();
					queryMap.put("room_id", room_id);
					queryMap.put("user_id", userId);
					queryMap.put("today_end_date", todayEnd.getTime());
					Map<String,Object> roomDistributerRecommendMap = commonModuleServer.findRoomDistributerRecommendAllInfo(queryMap);

					//3.如果该用户没有成为有效分销员的推荐用户，则该用户成为该分销员的推荐用户
					if(MiscUtils.isEmpty(roomDistributerRecommendMap)){
						Map<String,Object> insertMap = new HashMap<>();
						insertMap.put("distributer_recommend_id", MiscUtils.getUUId());
						insertMap.put("distributer_id", roomDistributerMap.get("distributer_id").toString());
						insertMap.put("room_id", room_id);
						insertMap.put("user_id", userId);
						insertMap.put("end_date", end_date);
						insertMap.put("rq_code", rqCode);
						insertMap.put("now", now);
						commonModuleServer.insertRoomDistributerRecommend(insertMap);

						//4.直播间分销员的推荐人数增加一
						Map<String,Object> updateMap = new HashMap<>();
						updateMap.put("distributer_id",roomDistributerMap.get("distributer_id").toString());
						updateMap.put("room_id",room_id);
						commonModuleServer.increteRecommendNumForRoomDistributer(updateMap);
					}
				}
			}
		}


		return resultMap;
	}



}
