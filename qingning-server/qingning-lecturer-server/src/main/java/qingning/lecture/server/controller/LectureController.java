package qingning.lecture.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.common.util.wxEncrypt.WXBizMsgCrypt;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
public class LectureController extends AbstractController {

	private WXBizMsgCrypt cryptUtil = null;

	/**
	 * 创建直播间
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/rooms", method = RequestMethod.POST)
	public
	@ResponseBody ResponseEntity createLiveRoom(
			HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createLiveRoom", accessToken, version,appName);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 编辑直播间
	 * @param entity
	 * @param room_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/rooms/{room_id}", method = RequestMethod.PUT)
	public
	@ResponseBody ResponseEntity updateLiveRoom(
			HttpEntity<Object> entity,
			@PathVariable("room_id") String room_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "updateLiveRoom", accessToken, version,appName);
		((Map<String,Object>)entity.getBody()).put("room_id", room_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 查询直播间基本信息
	 * @param query_type
	 * @param room_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/rooms", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity queryLiveRoomDetail(
			@RequestParam(value = "query_type",defaultValue = "") String query_type,
			@RequestParam(value = "room_id",defaultValue = "") String room_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version
	) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "liveRoom", accessToken, version,appName);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("query_type", query_type);
		param.put("room_id", room_id);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 发布课程
	 * @param entity
	 * @param room_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/rooms/{room_id}/courses", method = RequestMethod.POST)
	public
	@ResponseBody ResponseEntity createCourse(
			HttpEntity<Object> entity,
			@PathVariable("room_id") String room_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createCourse", accessToken, version,appName);
		((Map<String,Object>)entity.getBody()).put("room_id", room_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 查询课程详情
	 * @param course_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/courses/{course_id}", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getCourseDetail(
			@PathVariable("course_id") String course_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseDetail", accessToken, version,appName);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("course_id", course_id);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 编辑课程
	 * @param entity
	 * @param course_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/courses/{course_id}", method = RequestMethod.PUT)
	public
	@ResponseBody ResponseEntity updateCourse(
			HttpEntity<Object> entity,
			@PathVariable("course_id") String course_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "updateCourse", accessToken, version,appName);
		((Map<String,Object>)entity.getBody()).put("course_id", course_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 查询课程列表
	 * @param page_count
	 * @param room_id
	 * @param course_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/courses", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getCourseList(
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "room_id", defaultValue = "") String room_id,
			@RequestParam(value = "course_id", defaultValue = "") String course_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseList", accessToken, version,appName);
		Map<String, Object> parMap = new HashMap<String, Object>();
		parMap.put("page_count", page_count);
		parMap.put("room_id", room_id);
		parMap.put("course_id", course_id);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 处理课程PPT信息
	 * @param course_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/courses/{course_id}/ppts", method = {RequestMethod.PUT,RequestMethod.POST})
	public
	@ResponseBody ResponseEntity processCoursePPTs(
			@PathVariable("course_id") String course_id,
			HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "processCoursePPTs", accessToken, version,appName);
		((Map<String,Object>)entity.getBody()).put("course_id", course_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 查询课程综合信息（PPT信息和音频信息）
	 * @param course_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/courses/{course_id}/info", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getCourseInfo(
			@PathVariable("course_id") String course_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseInfo", accessToken, version,appName);
		Map<String, Object> parMap = new HashMap<String, Object>();
		parMap.put("course_id", course_id);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 查询某个课程消息列表
	 * @param course_id
	 * @param page_count
	 * @param message_pos
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/courses/{course_id}/messages", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getMessageList(
			@PathVariable("course_id") String course_id,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "message_pos", defaultValue = "") String message_pos,
			@RequestParam(value = "query_type", defaultValue = "0") String query_type,
			@RequestParam(value = "message_id", defaultValue = "") String message_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "messageList", accessToken, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("course_id", course_id);
		parMap.put("page_count", page_count);
		parMap.put("message_pos", message_pos);
		parMap.put("query_type", query_type);
		parMap.put("message_id", message_id);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 查询课程学员列表
	 * @param course_id
	 * @param page_count
	 * @param student_pos
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/courses/{course_id}/students", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getCourseStudentList(
			@PathVariable("course_id") String course_id,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "student_pos", defaultValue = "") String student_pos,
			@RequestParam(value = "data_source", defaultValue = "") String data_source,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseStudents", accessToken, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("course_id", course_id);
		parMap.put("page_count", page_count);
		parMap.put("student_pos", student_pos);
		parMap.put("data_source", data_source);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 讲师查询自己的课程收益明细
	 * @param room_id
	 * @param page_count
	 * @param course_id
	 * @param start_time
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/profits/{room_id}",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getRoomProfitList(
			@PathVariable("room_id") String room_id,
			@RequestParam(value ="page_count", defaultValue ="20") String page_count, 	
			@RequestParam(value = "course_id", defaultValue = "") String course_id,
			@RequestParam(value = "query_time", defaultValue = "") String start_time,
			@RequestParam(value = "position", defaultValue = "") String position,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "roomProfitList", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("room_id", room_id);
		parMap.put("page_count", page_count);
		parMap.put("course_id", course_id);
		parMap.put("query_time", start_time);
		parMap.put("position", position);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
	
	/**
	 * 获取直播间的课程收益
	 * @param room_id
	 * @param course_id
	 * @param page_count
	 * @param position
	 * @param profit_type 
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/profits/{room_id}/{course_id}",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getCourseProfitList(
			@PathVariable("room_id") String room_id,
			@PathVariable("course_id") String course_id,
			@RequestParam(value ="page_count", defaultValue ="40") String page_count,
			@RequestParam(value ="position", defaultValue ="") String position,
			@RequestParam(value ="profit_type", defaultValue ="2") String profit_type,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseProfitList", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("room_id", room_id);
		parMap.put("course_id", course_id);
		parMap.put("page_count", page_count);
		parMap.put("position", position);
		parMap.put("profit_type", profit_type);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
	
	/**
	 * 获取讲师的分销信息
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/distribution",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getDistributionInfo(			
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "distributionInfo", access_token, version,appName);
		return this.process(requestEntity, serviceManger, message);
	}
	
	/**
	 * 获取直播间分销员信息
	 * @param room_id
	 * @param page_count
	 * @param position
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/distribution/rooms/{room_id}",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getRoomDistributerInfo(	
			@PathVariable("room_id") String room_id,
			@RequestParam(value ="page_count", defaultValue ="10") String page_count,
			@RequestParam(value ="position", defaultValue ="") String position,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "roomDistributerInfo", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("room_id", room_id);
		parMap.put("page_count", page_count);
		parMap.put("position", position);
		requestEntity.setParam(parMap);		
		return this.process(requestEntity, serviceManger, message);
	}
	/**
	 * 获取直播间分销员的课程分销信息
	 * @param room_id
	 * @param distributer_id 
	 * @param page_count
	 * @param start_time
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/distribution/rooms/{room_id}/{distributer_id}/courses",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getRoomDistributerInfo(	
			@PathVariable("room_id") String room_id,
			@PathVariable("distributer_id") String distributer_id,
			@RequestParam(value ="page_count", defaultValue ="10") String page_count,
			@RequestParam(value ="start_time", defaultValue ="") String start_time,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "roomDistributerCoursesInfo", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("room_id", room_id);
		parMap.put("distributer_id", distributer_id);
		parMap.put("page_count", page_count);
		parMap.put("start_time", start_time);
		requestEntity.setParam(parMap);		
		return this.process(requestEntity, serviceManger, message);
	}
	/**
	 * 生成直播间分享链接的分享码
	 * @param room_id
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/lecturer/distribution/rooms/{room_id}/distributer/share",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity createRoomShareCode(	
			@PathVariable("room_id") String room_id,
			HttpEntity<Object> entity,			
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createRoomShareCode", access_token, version,appName);
		Map<String, Object> parMap = (Map<String, Object>)entity.getBody();
		parMap.put("room_id", room_id);
		requestEntity.setParam(parMap);		
		return this.process(requestEntity, serviceManger, message);
	}
	
	/**
	 * 创建直播间分销用户
	 * @param entity
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/lecturer/distribution/rooms/distributer/new",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity createRoomDistributer(
			HttpEntity<Object> entity,			
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createRoomDistributer", access_token, version,appName);
		Map<String, Object> parMap = (Map<String, Object>)entity.getBody();		
		requestEntity.setParam(parMap);		
		return this.process(requestEntity, serviceManger, message);
	}
	/**
	 * 创建直播间分销用户
	 * @param page_count
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/courses/statistics",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getCourseStatistics(			
			@RequestParam(value ="page_count", defaultValue ="10") String page_count,
			@RequestParam(value ="course_id", defaultValue ="") String course_id,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseStatistics", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("course_id", course_id);
		requestEntity.setParam(parMap);		
		return this.process(requestEntity, serviceManger, message);
	}
	/**
	 * 获取直播间粉丝列表
	 * @param page_count
	 * @param room_id
	 * @param position
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/rooms/{room_id}/fans",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getFanList(
			@PathVariable("room_id") String room_id,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "position", defaultValue = "") String position,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "fanList", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("room_id", room_id);
		parMap.put("page_count", page_count);
		parMap.put("position", position);
		requestEntity.setParam(parMap);		
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 获取客服信息
	 * @param access_token 安全证书
	 * @param version 版本信息
	 * @return 返回课程
 	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/customerService",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getCustomerService(
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "getCustomerService", access_token, version,appName);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 验证手机验证码
	 * @param verification_code 验证码
	 * @param accessToken 用户安全证书
	 * @param version 版本
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/verifyVerificationCode", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity  verifyVerificationCode(
			@RequestParam(value = "verification_code") String verification_code,
			@RequestParam(value = "room_id", defaultValue = "") String room_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version)throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "verifyVerificationCode", accessToken, null,appName);
		Map<String,String> map = new HashMap<>();
		map.put("verification_code",verification_code);
		map.put("room_id",room_id);
		requestEntity.setParam(map);
		return this.process(requestEntity, serviceManger, message);
	}
	
	/**
     * 微信平台回调第三方平台的ticker回调
     * @throws Exception
     */
	@RequestMapping(value = "/auth", method = RequestMethod.POST)
	public String wechatAuth(
			@RequestParam(value = "msg_signature") String msg_signature,
			@RequestParam(value = "timestamp") String timestamp,
			@RequestParam(value = "nonce") String nonce,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			HttpServletRequest req,
			HttpServletResponse resp) throws Exception {
		//解析XML文件
		BufferedReader br = req.getReader();
		String temp = null;
		StringBuilder xmlStrB = new StringBuilder();
		while((temp = br.readLine()) != null){
			xmlStrB.append(temp);
		}
		if (cryptUtil == null) {
			cryptUtil = new WXBizMsgCrypt(
					MiscUtils.getConfigByKey("weixin_service_no_token",appName),
					MiscUtils.getConfigByKey("weixin_service_no_aeskey",appName),
					MiscUtils.getConfigByKey("weixin_service_no_appid",appName));
		}
		//微信消息解密

		InputStream in = new ByteArrayInputStream(xmlStrB.toString().getBytes ());
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document doc = null;
		String decryptMsg = null;
		try {
			doc = builder.parse(in);
			NodeList infoTypeNodeL = doc.getElementsByTagName ("Encrypt");
			String encrypt = infoTypeNodeL.item(0).getFirstChild().getNodeValue();
			String format = "<xml><ToUserName><![CDATA[toUser]]></ToUserName><Encrypt><![CDATA[%1$s]]></Encrypt></xml>";
			String fromXML = String.format(format, encrypt);
			decryptMsg = cryptUtil.decryptMsg(msg_signature, timestamp, nonce, fromXML);
		} catch (Exception e) {
			e.printStackTrace();
			return "success";
		} finally {
			in.close();
		}

		in = new ByteArrayInputStream(decryptMsg.getBytes ());
		try {
			doc = builder.parse(in);
		} catch (Exception e) {
			e.printStackTrace();
			return "success";
		} finally {
			in.close();
		}

		//InfoType
		NodeList infoTypeNodeL = doc.getElementsByTagName ("InfoType");
		String infoType = infoTypeNodeL.item(0).getFirstChild().getNodeValue();
		String type = null;
		String appidOrTicket = null;

		if (infoType.equals("component_verify_ticket")) { //微信通知ticket
            type = "1";
            NodeList appidNodeL = doc.getElementsByTagName ("ComponentVerifyTicket");
            appidOrTicket = appidNodeL.item(0).getFirstChild().getNodeValue();
		} else if (infoType.equals("authorized")) { //授权成功
            type = "2";
            NodeList appidNodeL = doc.getElementsByTagName ("AuthorizerAppid");
            appidOrTicket = appidNodeL.item(0).getFirstChild().getNodeValue();
		} else if (infoType.equals("updateauthorized")) { //授权更新
            type = "3";
            NodeList appidNodeL = doc.getElementsByTagName ("AuthorizerAppid");
            appidOrTicket = appidNodeL.item(0).getFirstChild().getNodeValue();
		} else if (infoType.equals("unauthorized")) { //取消授权
            type = "4";
            NodeList appidNodeL = doc.getElementsByTagName ("AuthorizerAppid");
            appidOrTicket = appidNodeL.item(0).getFirstChild().getNodeValue();
		}

		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "wechatTicketNotify", null, null,appName);
		Map<String, Object> parMap = new HashMap<>();
        parMap.put("type", type);
		parMap.put("appidOrTicket", appidOrTicket);
		requestEntity.setParam(parMap);

		this.process(requestEntity, serviceManger, message);

		return "success";
	}
	/**
	 * 公众号授权回调URL
	 * access_token的存储至少要保留512个字符空间。access_token的有效期目前为2个小时，需定时刷新
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/auth/redirectUrl", method = RequestMethod.GET)
	public void wechatAuthRedirect(
			@RequestParam(value = "auth_code") String auth_code,
			@RequestParam(value = "expires_in") String expires_in,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			HttpServletRequest req,
			HttpServletResponse resp) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "wechatAuthRedirect", null, null,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("auth_code", auth_code);
		parMap.put("expires_in", expires_in);
		requestEntity.setParam(parMap);

		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
		Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

		Object redirectUrl =  resultMap.get("redirectUrl");
		if (redirectUrl != null) {
			resp.sendRedirect(redirectUrl.toString());
		}
	}

	/**
	 * 重定向到微信平台授权页面
	 * @param req
	 * @param resp
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/wechat/tobinding",method=RequestMethod.GET)
	public void tobindServiceNo (
			HttpServletRequest req,
			HttpServletResponse resp,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName) throws Exception{

		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "bindServiceNo", null, null,appName);

		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
		Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

		Object redirectUrl =  resultMap.get("redirectUrl");
		if (redirectUrl != null) {
			resp.sendRedirect(redirectUrl.toString());
		}
	}

	//<editor-fold desc="暂时用不上先注释掉">
//	/**
//	 * PC微信登录授权回调URL
//	 * @param  code　url带code
//	 * @return
//	 * @throws Exception
//	 */
//	@RequestMapping(value = "/pcauth/redirectUrl", method = RequestMethod.GET)
//	public String pcAuthRedirect(
//			@RequestParam(value = "code") String code,
//			HttpServletRequest req,
//			HttpServletResponse resp) throws Exception {
//		return "success";
//	}
//
//	/**
//	 * PC端登录之后 进行绑定 查询直播间信息 只有accessToken
//	 * @param access_token
//	 * @return
//	 * @throws Exception
//	 */
//	@RequestMapping(value = "/pcauth/queryRoomBrief", method = RequestMethod.GET)
//	public @ResponseBody ResponseEntity queryRoomBrief (
//			@RequestHeader("access_token") String access_token) throws Exception {
//		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "queryRoomBrief", access_token, null);
//		return this.process(requestEntity, serviceManger, message);
//	}
	//</editor-fold>

	/**
	 * PC端关联直播间与服务号
	 * @param appid 服务号的appid
	 * @param req
	 * @param resp
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/pcauth/bindingRoom", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity bindingRoom(
			@RequestParam(value = "appid") String appid,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			HttpServletRequest req,
			HttpServletResponse resp) throws Exception {

		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "bindingRoom", access_token, null,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("appid", appid);
		requestEntity.setParam(parMap);

		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 删除课程
	 * @param course_id 课程id
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/lecturer/course", method = RequestMethod.DELETE)
	public @ResponseBody ResponseEntity delCourse(
			@RequestParam(value = "course_id") String course_id,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "delCourse", access_token, null,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("course_id", course_id);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}



	/**
	 * 发布系列
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/rooms/series", method = RequestMethod.POST)
	public
	@ResponseBody ResponseEntity createSeries(
			HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createSeries", accessToken, version,appName);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 选择系列
	 * @param lecturer_id 讲师id
	 * @param classify_id 分类id
	 * @param series_id 系列id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/series", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getLecturerSeries(
			@RequestParam("lecturer_id") String lecturer_id,
			@RequestParam(value = "classify_id",defaultValue = "") String classify_id,
			@RequestParam(value = "series_id",defaultValue = "") String series_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "getLecturerSeries", accessToken, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("lecturer_id", lecturer_id);
		parMap.put("classify_id", classify_id);
		parMap.put("series_id", series_id);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 编辑系列
	 * @param entity
	 * @param series_id 系列id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/series/{series_id}", method = RequestMethod.PUT)
	public
	@ResponseBody ResponseEntity updateSeries(
			HttpEntity<Object> entity,
			@PathVariable("series_id") String series_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "updateSeries", accessToken, version,appName);
		((Map<String,Object>)entity.getBody()).put("series_id", series_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 上下架
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/updown", method = RequestMethod.PUT)
	public
	@ResponseBody ResponseEntity updown(
			HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "updown", accessToken, version,appName);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 编辑系列课程
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/lecturer/series/course", method = RequestMethod.PUT)
	public
	@ResponseBody ResponseEntity updateSeriesCourse(
			HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "updateSeriesCourse", accessToken, version,appName);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}


}
