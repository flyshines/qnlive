package qingning.user.server.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.common.util.CSVUtils;
import qingning.common.util.Constants;
import qingning.server.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userFollowRoom", accessToken, version,appName);
		((Map<String,Object>)entity.getBody()).put("room_id", room_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 用户-查询课程列表（正在直播（用户查看））
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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "roomInfo", accessToken, version,appName);
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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "courseDetailInfo", accessToken, version,appName);
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
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "roomCourses", accessToken, version,appName);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("room_id", room_id);
		param.put("page_count", page_count);
		param.put("course_id", course_id);
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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "joinCourse", accessToken, version,appName);
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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "courseInfo", accessToken, version,appName);
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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "courseStudents", accessToken, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("course_id", course_id);
		parMap.put("page_count", page_count);
		parMap.put("student_pos", student_pos);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}


	/**消费记录
	 * @param page_count
	 * @param position
	 * @param accessToken
	 * @param appName
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/record/consume", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getUserConsumeRecords(
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "position", defaultValue = "") String position,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getUserConsumeRecords", accessToken, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("position", position);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
	/**消费记录-saas后台(本店铺所有消费记录)
	 * @param page_count
	 * @param position
	 * @param accessToken
	 * @param appName
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/record/consume/saas", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getUserConsumeRecordsForSaaS(
			@RequestParam(value = "shop_id", defaultValue = "") String shopId,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "position", defaultValue = "") String position,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getUserConsumeRecords", accessToken, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("position", position);
		if(shopId==null||StringUtils.isEmpty(shopId)){
			throw new QNLiveException("000100");
		}
		parMap.put("shop_id", shopId);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
	/**消费记录-saas收入
	 * @param page_count
	 * @param position
	 * @param accessToken
	 * @param appName
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/record/income/details", method = RequestMethod.GET)
	public
	@ResponseBody ResponseEntity getUserIncomeRecords(
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "position", defaultValue = "") String position,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getUserIncomeRecords", accessToken, version,appName);
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
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "noticeRooms", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("notice_create_time", notice_create_time);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
	
	/**
	 * 查询用户加入的课程列表
	 * @param page_count
	 * @param course_id
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/user/course/list",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity  getStudyCourseList(
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "record_time", defaultValue = "") String course_id,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "studyCourses", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("course_id", course_id);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 查询课程或者是直播间
	 * @param id 根据type查询 0是课程id 1是直播间id
	 * @param type 0是课程 1是直播间
	 * @param access_token 后台证书
	 * @param version 版本
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/user/find",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity  getCourseOrLiveRoom(
			@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "type", defaultValue = "") String type,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getCourseOrLiveRoom", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 判断新增所有用户的gains
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/user/insertAllUserGains",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity  updateAllUserGains(
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "insertAllUserGains", null, null, null);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 获取用户可提现金额
	 * @param access_token 后台证书
	 * @param version 版本
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/user/gains",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity  userGains(
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userGains", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 发起提现申请
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/user/withdraw", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity addWithdraw(
    		HttpEntity<Object> entity,
    		@RequestHeader(value="access_token") String accessToken,
    		@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader(value="version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "createWithdraw", accessToken, version, appName);
		Map<String,String> param = (Map)entity.getBody();
		param.put("app_name",appName);
		requestEntity.setParam(entity.getBody());
		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
	
	/**
	 * 获取提现记录-客户度
	 * @param page_count
	 * @param createTime
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/user/withdraw/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWithdrawList(
    		@RequestParam(value="page_count", defaultValue="10") String page_count,
    		@RequestParam(value="create_time", defaultValue="0") String createTime,
    		@RequestHeader("access_token") String accessToken,
    		@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader("version") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getWithdrawList", accessToken, version, appName);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("page_count", page_count);
        paramMap.put("create_time", createTime);
        
        requestEntity.setParam(paramMap);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

	/**
	 * 获取提现记录-后台
	 * @param page_count
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/sys/withdraw/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWithdrawListAll(
    		@RequestParam(value="page_count", defaultValue="20") String page_count,
    		@RequestParam(value="page_num", defaultValue="1") String page_num,
    		@RequestParam(value="user_name",defaultValue="") String user_name,
    		@RequestParam(value="user_id",defaultValue="") String user_id,
    		@RequestParam(value="status",defaultValue="") String status,
    		@RequestHeader(value="access_token", defaultValue = "") String accessToken,
    		@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader(value="version",defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getWithdrawListAll", accessToken, version, appName);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("page_count", page_count);
        paramMap.put("page_num", page_num);
		if(StringUtils.isNotEmpty(user_name))
			paramMap.put("user_name", user_name);
		if(StringUtils.isNotEmpty(user_id))
			paramMap.put("user_id", user_id);
		if(StringUtils.isNotEmpty(status))
        	paramMap.put("status", status);
        requestEntity.setParam(paramMap);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }
	/**
	 * 获取提现记录-saas端
	 * @param page_count
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/saas/withdraw/list", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getWithdrawListSaaS(
    		@RequestParam(value="page_count", defaultValue="10") String page_count,
    		@RequestParam(value="page_num", defaultValue="1") String page_num,
    		@RequestParam(value="user_name",defaultValue="") String user_name,
    		@RequestParam(value="user_id",defaultValue="") String user_id,
    		@RequestParam(value="status",defaultValue="") String status,
    		@RequestHeader(value="access_token", defaultValue = "") String accessToken,
    		@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader(value="version",defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "getWithdrawListSaaS", accessToken, version, appName);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("page_count", page_count);
        paramMap.put("page_num", page_num);
		if(StringUtils.isNotEmpty(user_name))
			paramMap.put("user_name", user_name);
		if(StringUtils.isNotEmpty(user_id))
			paramMap.put("user_id", user_id);
		if(StringUtils.isNotEmpty(status))
        	paramMap.put("status", status);
        requestEntity.setParam(paramMap);
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }

	/**
	 * 后台_处理提现申请
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/user/withdraw/result", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity getWithdrawList(
			HttpEntity<Object> entity,
    		@RequestHeader(value="access_token", defaultValue="") String accessToken,
    		@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
    		@RequestHeader(value="version",defaultValue="") String version) throws Exception {
        RequestEntity requestEntity = this.createResponseEntity("UserServer", "handleWithDrawResult", accessToken, version, appName);
        requestEntity.setParam(entity.getBody());
        ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
        return responseEntity;
    }


	/**
	 * 查询系列列表（正在直播（用户查看））
	 * @param page_count 分页数
	 * @param series_id  系列id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/series", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getSeries(
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "series_id", defaultValue = "") String series_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "userSeries", accessToken, version,appName);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("series_id", series_id);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 查询直播间系列
	 * @param page_count 分页数
	 * @param room_id 直播间id
	 * @param series_id  系列id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/{room_id}/series", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getRoomSeries(
			@PathVariable("room_id") String room_id,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "series_id", defaultValue = "") String series_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getRoomSeries", accessToken, version,appName);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("room_id", room_id);
		param.put("series_id", series_id);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 用户查询系列详情
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/user/series/{series_id}", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getSeriesDetailInfo(
			@PathVariable("series_id") String series_id,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getSeriesDetailInfo", accessToken, version,appName);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("series_id", series_id);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}




	/**
	 * 用户查询系列课程列表
	 * @param series_id 系列id
	 * @param course_id 课程id 分页必传
	 * @param page_count 分页参数
	 * @param accessToken
	 * @param appName
	 * @param version
	 * @return
	 * @throws Exception
	 * 判断当前用户是否是讲师 如果是展示所有 如果不是展示已上架的
	 */
	@RequestMapping(value = "/user/series/course", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getSerieCourse(
			@RequestParam("series_id") String series_id,
			@RequestParam(value = "course_id", defaultValue = "") String course_id,
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getSerieCourse", accessToken, version,appName);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("series_id", series_id);
		param.put("course_id", course_id);
		param.put("page_count", page_count);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 用户订阅系列
	 * @param series_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/user/study/series/{series_id}", method = RequestMethod.PUT)
	public
	@ResponseBody
	ResponseEntity joinSeries(
			@PathVariable("series_id") String series_id,
			HttpEntity<Object> entity,
			@RequestHeader("access_token") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "joinSeries", accessToken, version,appName);
		((Map<String,Object>)entity.getBody()).put("series_id", series_id);
		requestEntity.setParam(entity.getBody());
		return this.process(requestEntity, serviceManger, message);
	}

	/**
	 * 查询用户加入的系列列表
	 * @param page_count 分页参数
	 * @param series_id 系列id
	 * @param access_token
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/user/series/list",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity  getStudySeriesList(
			@RequestParam(value = "page_count", defaultValue = "20") String page_count,
			@RequestParam(value = "series_id", defaultValue = "") String series_id,
			@RequestHeader("access_token") String access_token,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "studySeries", access_token, version,appName);
		Map<String, Object> parMap = new HashMap<>();
		parMap.put("page_count", page_count);
		parMap.put("series_id", series_id);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}


	/**
	 * 获取订单记录-后台
	 * @param page_count
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/sys/order/list", method = RequestMethod.GET)
	public
	@ResponseBody
	ResponseEntity getOrderListAll(
			@RequestParam(value="page_count", defaultValue="20") String page_count,
			@RequestParam(value="page_num", defaultValue="1") String page_num,
			@RequestParam(value="user_name",defaultValue="") String user_name,
			@RequestParam(value="user_id",defaultValue="") String user_id,
			@RequestParam(value="order_id",defaultValue="") String order_id,
			@RequestParam(value="pre_pay_no",defaultValue="") String pre_pay_no,
			@RequestParam(value="start_time",defaultValue="") Long start_time,
			@RequestParam(value="end_time",defaultValue="") Long end_time,
			@RequestHeader(value="access_token", defaultValue = "") String accessToken,
			@RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
			@RequestHeader(value="version",defaultValue="") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "getOrderListAll", accessToken, version, appName);
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("page_count", page_count);
		paramMap.put("page_num", page_num);
		if(StringUtils.isNotEmpty(user_name)) {
			paramMap.put("user_name", user_name);
		}
		if(StringUtils.isNotEmpty(user_id)) {
			paramMap.put("user_id", user_id);
		}
		if(StringUtils.isNotEmpty(order_id)) {
			paramMap.put("order_id", order_id);
		}
		if(StringUtils.isNotEmpty(pre_pay_no)) {
			paramMap.put("pre_pay_no", pre_pay_no);
		}
		if(start_time!=null)
			paramMap.put("start_time", new Date(Long.valueOf(start_time)));
		if(end_time!=null)
			paramMap.put("end_time", new Date(Long.valueOf(end_time)));
		requestEntity.setParam(paramMap);
		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
		return responseEntity;
	}

	/******************************* 导出数据 	***************************************/

	/**
	 * 导出订单记录-后台
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/sys/order/export",method=RequestMethod.GET)
	public void exportOrderListAll(HttpServletResponse resp, HttpServletRequest req,
							 @RequestParam(value="page_count", defaultValue="2000") String page_count,
							 @RequestParam(value="page_num", defaultValue="1") String page_num,
							 @RequestParam(value="user_name",defaultValue="") String user_name,
							 @RequestParam(value="user_id",defaultValue="") String user_id,
							 @RequestParam(value="order_id",defaultValue="") String order_id,
							 @RequestParam(value="pre_pay_no",defaultValue="") String pre_pay_no,
							 @RequestParam(value="start_time",defaultValue="") Long start_time,
							 @RequestParam(value="end_time",defaultValue="") Long end_time,
							 @RequestParam(value="access_token", defaultValue = "") String accessToken,
							 @RequestHeader(value = "app_name",defaultValue = Constants.HEADER_APP_NAME) String appName,
							 @RequestHeader(value="version",defaultValue="") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("UserServer", "exportOrderListAll", accessToken, version, appName);
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("page_count", page_count);
		paramMap.put("page_num", page_num);
		if(StringUtils.isNotEmpty(user_name)) {
			paramMap.put("user_name", user_name);
		}
		if(StringUtils.isNotEmpty(user_id)) {
			paramMap.put("user_id", user_id);
		}
		if(StringUtils.isNotEmpty(order_id)) {
			paramMap.put("order_id", order_id);
		}
		if(StringUtils.isNotEmpty(pre_pay_no)) {
			paramMap.put("pre_pay_no", pre_pay_no);
		}
		if(start_time!=null)
			paramMap.put("start_time", new Date(Long.valueOf(start_time)));
		if(end_time!=null)
			paramMap.put("end_time", new Date(Long.valueOf(end_time)));
		requestEntity.setParam(paramMap);
		ResponseEntity responseEntity = this.process(requestEntity, serviceManger, message);
		List<Map<String, Object>> exportCourseList = new ArrayList<>();
		Map<String,Object> res = (Map<String,Object>)responseEntity.getReturnData();
		if(res.get("list") instanceof List){
			exportCourseList = (List<Map<String, Object>>)res.get("list");
		}

		LinkedHashMap<String,String> headMap = new LinkedHashMap<>();
		headMap.put("order_id", "订单ID");
		headMap.put("pre_pay_no", "微信订单ID");
		headMap.put("user_id", "收益人ID");
		headMap.put("resume_id", "消费者ID");
		headMap.put("distributer_id", "分销者ID");
		headMap.put("user_amount", "收益");
		headMap.put("amount", "订单总额");
		headMap.put("nick_name", "收益人昵称");
		headMap.put("profit_type", "收益来源");
		headMap.put("resume_user", "消费者昵称");
		headMap.put("distributer_user", "分销者昵称");
		headMap.put("create_time", "创建时间");

		File file = CSVUtils.createCSVFile(exportCourseList, headMap, null, "订单记录");
		CSVUtils.exportFile(resp, file.getName(), file);
	}
}
