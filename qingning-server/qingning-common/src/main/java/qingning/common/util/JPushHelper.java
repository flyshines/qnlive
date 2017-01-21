package qingning.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JPushHelper {
	private static final Logger logger = LoggerFactory.getLogger(JPushHelper.class);
	
	/**
	 * APP角标数
	 */
	public static Map<String, Integer> msgCount = new HashMap<String, Integer>();

	public static void push(JSONObject obj) {
		logger.debug(obj.toJSONString());

		JSONObject user = JSON.parseObject(obj.getString("user"));
		// 消息内容
		String content = obj.getString("body");
		// 推送人MID（个人）
		String m_user_id = obj.getString("to");
		// 推送人MID（多人）
		List<String> userIds = (List<String>)obj.get("user_ids");
		// 推送类型
		String msgType = obj.getString("msg_type");
		// 推送人
		String sendName = null;
		if(user != null){
			sendName = user.getString("nick");
		}

		Map<String,String> extras_map = (Map<String,String>)obj.get("extras_map");

		//是标签推送还是别名推送
		String sendType = obj.getString("send_type");
		
		//	推送文本
		String contents = null;
		if (sendName == null || sendName == null) {
			contents = content;
		} else {
			contents = sendName + ":" + content;
		}
		
		
		try {
			logger.info("====开始远程推送====content=" + obj.toJSONString() + ",m_user_id=" + m_user_id);
			//推送人MID结合，适合多人推送
			List<String> audiences = new ArrayList<String>();
			if (m_user_id != null) {
				audiences.add(m_user_id);
			}
			if(userIds!=null&&!userIds.isEmpty()){
				audiences.addAll(userIds);
			}
			
			Integer count = null;
//			if (msgCount.get(m_user_id) != null) {
//				count = msgCount.get(m_user_id);
//				msgCount.remove(m_user_id);
//			}
			
			JGMsgUtil.sendMsg("all", audiences, contents, count, msgType,sendType,extras_map);
			logger.info("====远程推送结束====");
		} catch (Exception e) {
			logger.error("远程推送发生错误：", e);
		}
	}

}
