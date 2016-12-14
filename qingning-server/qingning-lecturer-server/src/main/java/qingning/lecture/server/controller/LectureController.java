package qingning.lecture.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.server.AbstractController;

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
			@RequestParam(value = "start_time", defaultValue = "") String start_time,
			@RequestParam(value = "course_id", defaultValue = "") String course_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "courseList", accessToken, version);
		Map<String, Object> parMap = new HashMap<String, Object>();
		parMap.put("page_count", page_count);
		parMap.put("room_id", room_id);
		parMap.put("start_time", start_time);
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
	@RequestMapping(value = "/lecturer/courses/{course_id}/ppts", method = RequestMethod.PUT)
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
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "messageList", accessToken, version);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("course_id", course_id);
		parMap.put("page_count", page_count);
		parMap.put("message_pos", message_pos);
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

}
