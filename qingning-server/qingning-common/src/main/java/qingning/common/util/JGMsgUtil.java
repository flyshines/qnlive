package qingning.common.util;

import cn.jpush.api.JPushClient;
import cn.jpush.api.common.resp.APIConnectionException;
import cn.jpush.api.common.resp.APIRequestException;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Message;
import cn.jpush.api.push.model.Options;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JGMsgUtil {
	private static final Logger logger = LoggerFactory.getLogger(JGMsgUtil.class);
	private static String masterSecret = null;
	private static String appKey = null;
	private static boolean apnsProduction = false;
	static {
		masterSecret = MiscUtils.getConfigByKey("jg_master_secret");
		appKey = MiscUtils.getConfigByKey("jg_app_key");
		apnsProduction = Boolean.valueOf(MiscUtils.getConfigByKey("apns_production"));
	}
	
	public static void sendMsg(String plat, List<String> audiences, String contents,  Integer count,
			String msgType, String recipient, Map<String,String> extrasMap) {

		JPushClient jpushClient = new JPushClient(masterSecret, appKey);
		if (plat == null) {
			plat = Platforms.all.getName();
		}
		Platform platform = null;
		PushPayload pushPayload = null;
		Audience audience = null;
		Notification notification = null;
		if (plat.equals(Platforms.all.getName())) {
			platform = Platform.all();
			// 消息推送
			//不做累加角标的消息
			if(MiscUtils.isEmpty(extrasMap)){
				notification = Notification.newBuilder()
						.addPlatformNotification(IosNotification.newBuilder().setAlert(contents).disableBadge().setSound("default").build())
						.addPlatformNotification(AndroidNotification.newBuilder().setAlert(contents).build()).build();
			}else {
				notification = Notification.newBuilder()
						.addPlatformNotification(IosNotification.newBuilder().setAlert(contents).addExtras(extrasMap).disableBadge().setSound("default").build())
						.addPlatformNotification(AndroidNotification.newBuilder().setAlert(contents).addExtras(extrasMap).build()).build();
			}
		}
		if (plat.equals(Platforms.ios.getName())) {
			platform = Platform.ios();
			notification = Notification.newBuilder()
					.addPlatformNotification(IosNotification.newBuilder().setAlert("新消息").disableBadge().setSound("default").build())
					.build();
		}
		if (plat.equals(Platforms.android.getName())) {
			platform = Platform.android();
			notification = Notification.newBuilder()
					.addPlatformNotification(AndroidNotification.newBuilder().setAlert("新消息").build()).build();
		}
		if(StringUtils.isBlank(recipient) || recipient.equals(Constants.JPUSH_SEND_TYPE_ALIAS)){
			if (audiences == null) {
				audience = Audience.all();
			} else {
				audience = Audience.alias(audiences);
			}
		}else {
			audience = Audience.tag(audiences);
		}

		Options options = Options.newBuilder().setApnsProduction(apnsProduction).build();
		Message message = null;
		if(MiscUtils.isEmpty(extrasMap)){
			message = Message.newBuilder().setMsgContent(contents).build();
		}else {
			message = Message.newBuilder().setMsgContent(contents).addExtras(extrasMap).build();
		}
		pushPayload = build(platform, audience, options, notification, contents, message);
		try {
			logger.info("开始远程消息推送");
			PushResult result = jpushClient.sendPush(pushPayload);
			logger.info("Got result - " + result);
		} catch (APIConnectionException e) {
			logger.error("Connection error, should retry later", e);
		} catch (APIRequestException e) {
			logger.error("Should review the error, and fix the request", "推送错误" + e.getStatus() + e.getErrorCode());
			logger.info("HTTP Status: " + e.getStatus());
			logger.info("Error Code: " + e.getErrorCode());
			logger.error("Error Message: " + e.getErrorMessage());
		}
	}

	private static PushPayload build(Platform platform, Audience audience, Options options, Notification notification,
			String content,Message message) {
		return PushPayload.newBuilder().setPlatform(platform).setAudience(audience).setOptions(options)
				.setNotification(notification).setMessage(message).build();
	}

	public enum Platforms {
		all(1, "all"), ios(2, "ios"), android(3, "android");

		private int plat;
		private String name;

		private Platforms(int plat, String name) {
			this.plat = plat;
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public int getPlat() {
			return this.plat;
		}
	}

	public static JSONObject parseContent(String content) {
		if (!content.endsWith("\"}}")) {
			content = content + "\"}}";
		}
		JSONObject object = JSONObject.parseObject(content);
		return object;
	}

	public static void main(String[] args) {
		System.out.println("发送推送"+"  执行时间"+System.currentTimeMillis());
		JSONObject obj = new JSONObject();
		obj.put("body","单次直播最长1440分钟，您已直播1430分钟，还剩10分钟");
		obj.put("to","000054dea56b56d34535a8f5e529ad53327b");//courseMap.get("lecturer_id")
		obj.put("msg_type","4");
		Map<String,String> extrasMap = new HashMap<>();
		extrasMap.put("msg_type","4");
		extrasMap.put("course_id","0000df343ef902404d74b4b8a62394e9f69c");//courseId
		extrasMap.put("im_course_id","424");//im_course_id
		obj.put("extras_map", extrasMap);
		JPushHelper.push(obj);
	}
}
