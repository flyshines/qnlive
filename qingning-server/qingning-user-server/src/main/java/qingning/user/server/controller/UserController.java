package qingning.user.server.controller;

import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.server.AbstractController;

import org.apache.log4j.Logger;

import java.util.Map;

@RestController
public class UserController extends AbstractController{
	
	private static final Logger logger = Logger.getLogger(UserController.class);

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
	
}
