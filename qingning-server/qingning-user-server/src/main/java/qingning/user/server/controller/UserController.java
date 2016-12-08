package qingning.user.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.server.AbstractController;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController extends AbstractController{
	
	private static final Logger logger = Logger.getLogger(UserController.class);

	/**
	 * 用户关注直播间
	 * @param entity
	 * @param room_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/live_rooms/{room_id}/notice", method = RequestMethod.PUT)
	public
	@ResponseBody
	ResponseEntity userFollowRoom(
			HttpEntity<Object> entity,
			@PathVariable("room_id") String room_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userFollowRoom", accessToken, version);
		((Map<String,Object>)entity.getBody()).put("room_id", room_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 用户-查询课程列表（我加入的课程列表、正在直播（用户查看））
	 * @param page_count
	 * @param course_id
	 * @param query_type
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
			@RequestParam(value = "course_id", defaultValue = "") String   course_id,
			@RequestParam(value = "query_type", defaultValue = "") String  query_type,
			@RequestParam(value = "status", defaultValue = "") String  status,
			@RequestParam(value = "data_source", defaultValue = "") String  data_source,
			@RequestParam(value = "start_time", defaultValue = "") String  start_time,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userCourses", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("course_id", course_id);
		param.put("query_type", query_type);
		param.put("status", status);
		param.put("data_source", data_source);
		param.put("start_time", start_time);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 用户查询直播间信息
	 * @param room_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/rooms/{room_id}", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getRoomInfo(
			@PathVariable("room_id") String room_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "roomInfo", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("room_id", room_id);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 用户查询课程详情
	 * @param course_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/courses/{course_id}", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getCourseDetailInfo(
			@PathVariable("course_id") String course_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "courseDetailInfo", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("course_id", course_id);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 用户查询直播间课程列表
	 * @param room_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/live_rooms/{room_id}/courses", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getRoomCourses(
			@PathVariable("room_id") String room_id,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "course_id", defaultValue = "") String course_id,
			@RequestParam(value = "start_time", defaultValue = "") String start_time,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "roomCourses", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("room_id", room_id);
		param.put("page_count", page_count);
		param.put("course_id", course_id);
		param.put("start_time", start_time);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 用户加入课程
	 * @param course_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/study/courses/{course_id}", method = RequestMethod.PUT)
	public
	@ResponseBody
	ResponseEntity joinCourse(
			@PathVariable("course_id") String course_id,
			HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "joinCourse", accessToken, version);
		((Map<String,Object>)entity.getBody()).put("course_id", course_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}


//	@RequestMapping(value = "/user/live_rooms/{room_id}/courses", method = RequestMethod.GET)
//	public
//	@ResponseBody
//	ResponseEntity getRoomCourses(
//			@PathVariable("room_id") String room_id,
//			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
//			@RequestParam(value = "course_id", defaultValue = "") String course_id,
//			@RequestParam(value = "start_time", defaultValue = "") String start_time,
//			@RequestHeader("access_token") String accessToken,
//			@RequestHeader("version") String version) throws Exception {
//		RequestEntity requestEntity = this.createResponseEntity("UserServer", "roomCourses", accessToken, version);
//		Map<String, Object> param = new HashMap<String, Object>();
//		param.put("room_id", room_id);
//		param.put("page_count", page_count);
//		param.put("course_id", course_id);
//		param.put("start_time", start_time);
//		requestEntity.setParam(param);
//		return this.process(requestEntity, serviceManger, message);
//	}

}
