package qingning.user.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.server.AbstractController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController extends AbstractController{
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
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userFollowRoom", accessToken, version);
		((Map<String,Object>)entity.getBody()).put("room_id", room_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 用户-查询课程列表（正在直播（用户查看））
	 * @param page_count
	 * @param course_id
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
			@RequestParam(value = "status", defaultValue = "") String  status,
			@RequestParam(value = "data_source", defaultValue = "") String  data_source,
			@RequestParam(value = "query_time", defaultValue = "") String  start_time,
			@RequestParam(value = "position", defaultValue = "") String  position,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userCourses", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("course_id", course_id);
		param.put("status", status);
		param.put("data_source", data_source);
		param.put("query_time", start_time);
		param.put("position", position);
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
			@RequestParam(value = "query_time", defaultValue = "") String start_time,
			@RequestParam(value = "position", defaultValue = "") String position,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "roomCourses", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("room_id", room_id);
		param.put("page_count", page_count);
		param.put("course_id", course_id);
		param.put("query_time", start_time);
		param.put("position", position);
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
	@SuppressWarnings("unchecked")
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


	/**
	 * 查询课程综合信息，包括PPT信息，讲课音频信息，打赏列表信息
	 * @param course_id
	 * @param reward_update_time
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/user/courses/{course_id}/info", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getCoursesInfo(
			@PathVariable("course_id") String course_id,
			@RequestParam(value = "reward_update_time", defaultValue = "") String reward_update_time,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "courseInfo", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("course_id", course_id);
		param.put("reward_update_time", reward_update_time);
		requestEntity.setParam(param);
		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);

		//处理打赏信息
		Map<String, Object> resultMap = null;
		if(! reward_update_time.equals(rewardConfigurationTime.toString())){
			resultMap = (Map<String, Object>) responseEntity.getReturnData();
			resultMap.put("reward_info",rewardConfigurationMap);
			responseEntity.setReturnData(resultMap);
		}
		return responseEntity;
	}

	/**
	 * 查询课程消息列表
	 * @param course_id
	 * @param page_count
	 * @param message_pos
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/courses/{course_id}/messages", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getMessageList(
			@PathVariable("course_id") String course_id,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "message_pos", defaultValue = "") String message_pos,
			@RequestParam(value = "query_type", defaultValue = "0") String query_type,
			@RequestParam(value = "message_id", defaultValue = "") String message_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "messageList", accessToken, version);
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
	@RequestMapping(value = "/user/courses/{course_id}/students", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getCourseStudentList(
			@PathVariable("course_id") String course_id,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "student_pos", defaultValue = "") String student_pos,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "courseStudents", accessToken, version);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("course_id", course_id);
		parMap.put("page_count", page_count);
		parMap.put("student_pos", student_pos);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}



	@RequestMapping(value = "/user/record/consume", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getUserConsumeRecords(
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "position", defaultValue = "") String position,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getUserConsumeRecords", accessToken, version);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("position", position);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
	
	 /**
	 * 用户查询关注的直播间列表
	 * @param page_count
	 * @param notice_create_time
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/user/notice/live_rooms",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity  getNoticeRooms(
			@RequestParam(value = " page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "notice_create_time", defaultValue = "") String notice_create_time,
			@RequestHeader("access_token") String access_token,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "noticeRooms", access_token, version);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("notice_create_time", notice_create_time);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
	
	/**
	 * 查询用户加入的课程列表
	 * @param page_count
	 * @param record_time
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/user/course/list",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity  getStudyCourseList(
			@RequestParam(value = " page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "record_time", defaultValue = "") String record_time,
			@RequestHeader("access_token") String access_token,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "studyCourses", access_token, version);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("record_time", record_time);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
}
