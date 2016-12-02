package qingning.lecture.server.controller;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.server.AbstractController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LectureController extends AbstractController {

	private static final Logger logger = Logger.getLogger(LectureController.class);

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


	//查询直播间基本信息
	@RequestMapping(value = "/lecturer/rooms", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity queryLiveRoomDetail(
			@RequestParam(value = "query_type") String query_type,
			@RequestParam(value = "room_id") String room_id,
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
}
