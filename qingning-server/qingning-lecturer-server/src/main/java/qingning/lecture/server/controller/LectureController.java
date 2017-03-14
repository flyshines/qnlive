package qingning.lecture.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createLiveRoom", accessToken, version);
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "updateLiveRoom", accessToken, version);
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
			@RequestHeader("version") String version
	) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "liveRoom", accessToken, version);
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createCourse", accessToken, version);
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseDetail", accessToken, version);
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "updateCourse", accessToken, version);
		((Map<String,Object>)entity.getBody()).put("course_id", course_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 查询课程列表
	 * @param page_count
	 * @param room_id
	 * @param start_time
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
			@RequestParam(value = "query_time", defaultValue = "") String start_time,
			@RequestParam(value = "course_id", defaultValue = "") String course_id,
			@RequestParam(value = "position", defaultValue = "") String position,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseList", accessToken, version);
		Map<String, Object> parMap = new HashMap<String, Object>();
		parMap.put("page_count", page_count);
		parMap.put("room_id", room_id);
		parMap.put("query_time", start_time);
		parMap.put("course_id", course_id);
		parMap.put("position", position);
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "processCoursePPTs", accessToken, version);
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseInfo", accessToken, version);
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "messageList", accessToken, version);
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseStudents", accessToken, version);
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
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "roomProfitList", access_token, version);
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
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseProfitList", access_token, version);
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
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "distributionInfo", access_token, version);		
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
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "roomDistributerInfo", access_token, version);
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
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "roomDistributerCoursesInfo", access_token, version);
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
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createRoomShareCode", access_token, version);
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
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "createRoomDistributer", access_token, version);
		Map<String, Object> parMap = (Map<String, Object>)entity.getBody();		
		requestEntity.setParam(parMap);		
		return this.process(requestEntity, serviceManger, message);
	}
	/**
	 * 创建直播间分销用户
	 * @param page_count
	 * @param start_time
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/courses/statistics",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getCourseStatistics(			
			@RequestParam(value ="page_count", defaultValue ="10") String page_count,
			@RequestParam(value ="query_time", defaultValue ="") String start_time,
			@RequestParam(value ="course_id", defaultValue ="") String course_id,
			@RequestParam(value ="position", defaultValue ="") String position,
			@RequestHeader("access_token") String access_token,
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseStatistics", access_token, version);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("query_time", start_time);
		parMap.put("course_id", course_id);
		parMap.put("position", position);
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
			@RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "fanList", access_token, version);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("room_id", room_id);
		parMap.put("page_count", page_count);
		parMap.put("position", position);
		requestEntity.setParam(parMap);		
		return this.process(requestEntity, serviceManger, message);
	}
	/**
	 * 微信服务器每隔10分钟定时推送component_verify_ticket
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/auth", method = RequestMethod.POST)
	public String wechatAuth(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		//解析XML文件
		BufferedReader br = req.getReader();
		String temp = null;
		StringBuilder xmlStrB = new StringBuilder();
		while((temp = br.readLine()) != null){
			xmlStrB.append(temp);
		}
		InputStream in = new ByteArrayInputStream(xmlStrB.toString().getBytes ("UTF-8"));
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(in);

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
		in.close();

		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "wechatTicketNotify", null, null);
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
	@RequestMapping(value = "/auth/redirectUrl/{user_id}", method = RequestMethod.GET)
	public void wechatAuthRedirect(
			@PathVariable("user_id") String user_id,
			@RequestParam(value = "auth_code") String auth_code,
			HttpServletRequest req,
			HttpServletResponse resp) throws Exception {

		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "wechatAuthRedirect", null, null);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("auth_code", auth_code);
		parMap.put("user_id", user_id);
		requestEntity.setParam(parMap);

		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
		Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

		String redirectUrl = (String) resultMap.get("redirectUrl");
		resp.sendRedirect(redirectUrl);
	}
	/**
	 * 跳转微信服务号授权页面
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/lecturer/wechat/binding",method=RequestMethod.GET)
	public void bindServiceNo (
			@RequestHeader("access_token") String access_token,
			@RequestHeader("version") String version,
			HttpServletRequest req,
			HttpServletResponse resp) throws Exception{

		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "bindServiceNo", access_token, null);

		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
		Map<String, Object> resultMap = (Map<String, Object>) responseEntity.getReturnData();

		String redirectUrl = (String) resultMap.get("redirectUrl");
		resp.sendRedirect(redirectUrl);
	}

//	/**
//	 * 关联微信号二维码号
//	 * @param type
//	 * @param url
//	 * @param access_token
//	 * @param version
//	 * @return
//	 * @throws Exception
//	 */
//	@RequestMapping(value="/lecturer/wechat/associate",method=RequestMethod.GET)
//	public @ResponseBody ResponseEntity associateWechatCode (
//			@RequestParam(value = "type", defaultValue = "") String type,
//			@RequestParam(value = "url", defaultValue = "") String url,
//			@RequestHeader("access_token") String access_token,
//			@RequestHeader("version") String version) throws Exception{
//		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "associateWechatCode", access_token, version);
//		return this.process(requestEntity, serviceManger, message);
//	}
}
