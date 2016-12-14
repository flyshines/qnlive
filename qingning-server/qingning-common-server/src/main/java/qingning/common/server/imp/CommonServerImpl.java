package qingning.common.server.imp;

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
					Map<String,String> imResultMap = IMMsgUtil.createIMAccount(reqMap.get("device_id").toString());
					if(imResultMap == null || imResultMap.get("uid") == null || imResultMap.get("password") == null){
						throw new QNLiveException("120003");
					}else {
						//初始化数据库相关表
						reqMap.put("m_user_id", imResultMap.get("uid"));
						reqMap.put("m_pwd", imResultMap.get("password"));
						//设置默认用户头像
						if(reqMap.get("avatar_address") == null || StringUtils.isBlank(reqMap.get("avatar_address").toString())){
							reqMap.put("avatar_address","http://7xt3lm.com1.z0.glb.clouddn.com/images/1467E3DB24D3F6EA884511BF6B24D2D9.png");//TODO
						}
						if(reqMap.get("nick_name") == null || StringUtils.isBlank(reqMap.get("nick_name").toString())){
							reqMap.put("avatar_address","用户" + jedis.incrBy(Constants.CACHED_KEY_USER_NICK_NAME_INCREMENT_NUM, 1));//TODO
						}
						Map<String,String> dbResultMap = commonModuleServer.initializeRegisterUser(reqMap);

						//生成access_token，将相关信息放入缓存，构造返回参数
						processLoginSuccess(1, dbResultMap, null, resultMap);
					}

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
}
