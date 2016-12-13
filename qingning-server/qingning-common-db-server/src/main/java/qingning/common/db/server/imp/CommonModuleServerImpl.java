
package qingning.common.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import qingning.common.db.persistence.mybatis.LoginInfoMapper;
import qingning.common.db.persistence.mybatis.ServerFunctionMapper;
import qingning.common.db.persistence.mybatis.UserMapper;
import qingning.common.db.persistence.mybatis.entity.LoginInfo;
import qingning.common.db.persistence.mybatis.entity.User;
import qingning.common.util.MiscUtils;
import qingning.server.rpc.manager.ICommonModuleServer;

import java.util.*;

public class CommonModuleServerImpl implements ICommonModuleServer {

	@Autowired(required = true)
	private ServerFunctionMapper serverFunctionMapper;

	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;

	@Autowired(required = true)
	private UserMapper userMapper;


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
		loginInfo.setmUserId(reqMap.get("m_user_id").toString());
		loginInfo.setmPwd(reqMap.get("m_pwd").toString());
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


}
