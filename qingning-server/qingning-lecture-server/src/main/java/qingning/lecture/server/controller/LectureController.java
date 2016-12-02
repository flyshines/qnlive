package qingning.lecture.server.controller;

import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.server.AbstractController;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;

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


	@RequestMapping(value = "/lecturer/rooms/{room_id}", method = RequestMethod.PUT)
	public
	@ResponseBody ResponseEntity updateLiveRoom(
			HttpEntity<Object> entity,
			@PathVariable("user_id") String user_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("LectureServer", "updateLiveRoom", accessToken, version);
		((Map<String,Object>)entity.getBody()).put("user_id", user_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}
}
