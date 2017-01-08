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

import java.util.List;

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
			String msgType, String recipient) {

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
			notification = Notification.alert(contents);

		}
		if (plat.equals(Platforms.ios.getName())) {
			platform = Platform.ios();
			notification = Notification.newBuilder()
					.addPlatformNotification(IosNotification.newBuilder().setAlert("新消息").disableBadge().build())
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
		pushPayload = build(platform, audience, options, notification, contents);
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
			String content) {
		return PushPayload.newBuilder().setPlatform(platform).setAudience(audience).setOptions(options)
				.setNotification(notification).setMessage(Message.content(content)).build();
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
		// sendMsg(null,null,"推送测试数据");
	}

}
