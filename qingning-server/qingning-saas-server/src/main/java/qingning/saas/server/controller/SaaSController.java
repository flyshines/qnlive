package qingning.saas.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.Constants;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SaaSController extends AbstractController{

	/**
	 * 微信扫码 1 登录入口
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/wechat/login/address", method = RequestMethod.GET)
	public void wechatLogin(HttpServletResponse resp) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "wechatLogin", null, null,null);
		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
		Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

		Object redirectUrl =  resultMap.get("redirectUrl");
		if (redirectUrl != null) {
			resp.sendRedirect(redirectUrl.toString());
		}
	}

	/**
	 * 微信扫码登录回调地址
	 * @param page_count 分页数
	 * @param course_id  课程id
	 * @param classify_id 分类id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/courses", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getCourse(
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "course_id", defaultValue = "") String course_id,
			@RequestParam(value = "status", defaultValue = "4") String  status,
			@RequestParam(value="classify_id",defaultValue = "") String  classify_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userCourses", accessToken, version,appName);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("course_id", course_id);
		param.put("classify_id",classify_id);
		param.put("status", status);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}



	/**
	 * 用户关注直播间
	 * @param entity
	 * @param room_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/user/live_rooms/{room_id}/notice", method = RequestMethod.PUT)
	public
	@ResponseBody
	ResponseEntity userFollowRoom(
			HttpEntity<Object> entity,
			@PathVariable("room_id") String room_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userFollowRoom", accessToken, version,appName);
		((Map<String,Object>)entity.getBody()).put("room_id", room_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 店铺设置
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/shop/edit", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity addWithdraw(
    		HttpEntity<Object> entity,
    		@RequestHeader(value="access_token") String accessToken,
    		@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader(value="version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("SaaSServer", "shopEdit", accessToken, version, appName);
		Map<String,String> param = (Map)entity.getBody();
		param.put("app_name",appName);
		requestEntity.setParam(entity.getBody());
		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
	

}
