package qingning.lecture.server.controller;

import qingning.common.entity.RequestEntity;
import qingning.common.entity.ResponseEntity;
import qingning.server.AbstractController;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.common.utils.StringUtils;

@RestController
public class CommonController extends AbstractController{
	
	private static final Logger logger = Logger.getLogger(CommonController.class);
	
/*	*//**
	 * 课程列表查询
	 * @param page_count
	 * @param page_num
	 * @param search_type
	 * @param search_name
	 * @param course_type_id
	 * @param start_time
	 * @param end_time
	 * @param course_status
	 * @param course_class
	 * @param choreographer_id	
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/courses",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getCourseList(
	    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
	    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
	    @RequestParam(value = "search_type", defaultValue = "") String search_type,
	    @RequestParam(value = "search_name", defaultValue = "") String search_name,
	    @RequestParam(value = "course_type_id", defaultValue = "") String course_type_id,  
	    @RequestParam(value = "time_type", defaultValue = "") String time_type,  
	    @RequestParam(value = "start_time", defaultValue = "") String start_time,
	    @RequestParam(value = "end_time", defaultValue = "") String end_time,
		@RequestParam(value = "status", defaultValue = "") String status,
		@RequestParam(value = "course_class", defaultValue = "") String course_class,
		@RequestParam(value = "choreographer_id", defaultValue = "") String choreographer_id,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception{
		
		RequestEntity requestEntity = this.createResponseEntity("CourseManger", "courseList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		if(StringUtils.isBlank(search_name)){
			search_type = null;
		}
		param.put("search_name", search_name);
		param.put("search_type", search_type);
		param.put("course_type_id", course_type_id);
		if(StringUtils.isBlank(start_time)){
			time_type = null;
		}
		param.put("time_type", time_type);
		param.put("start_time", start_time);
		param.put("end_time", end_time);
		param.put("status", status);
		param.put("course_class", course_class);
		param.put("choreographer_id", choreographer_id);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	
	*//**
	 * 影藏课程
	 * @param course_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/courses/{course_id}",method=RequestMethod.PUT)
	public @ResponseBody ResponseEntity updateCourse(@PathVariable("course_id") String course_id, 	
		HttpEntity<Object> entity,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("CourseManger", "updateCourse", accessToken, version);
		Map<String, Object> parMap = (Map<String, Object>) entity.getBody();
		parMap.put("course_id", course_id);
		requestEntity.setParam(parMap);
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	@RequestMapping(value="/manager/courses/{course_id}/students",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getStudentList(
	    @PathVariable("course_id") String course_id,
	    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
	    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception{
		
		Map<String, Object> param = new HashMap<String, Object>();
		RequestEntity requestEntity = this.createResponseEntity("CourseManger", "getCourseStudentList", accessToken, version);
		param.put("course_id", course_id);
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}

	@RequestMapping(value="/manager/distributor/{distributor_id}/card",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getDistributorCardList(
		    @PathVariable("distributor_id") String distributor_id,
		    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
		    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
	        @RequestHeader("accessToken") String accessToken,
	        @RequestHeader("version") String version) throws Exception { 
		
		Map<String, Object> param = new HashMap<String, Object>();
		RequestEntity requestEntity = this.createResponseEntity("DistributorManger", "distributorCardList", accessToken, version);
		param.put("distributor_id", distributor_id);
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}

	
	
	@RequestMapping(value="/manager/courses/type",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getCourseTypeList(
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception{
		
		RequestEntity requestEntity = this.createResponseEntity("CourseManger", "getCourseTypeList", accessToken, version);
		
		return this.process(requestEntity, serviceManger, message);
	}

	    
	@RequestMapping(value="/manager/income/wallet",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getIncomeWalletList(
	    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
	    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
	    @RequestParam(value = "search_type", defaultValue = "") String search_type,
		@RequestParam(value = "search_name", defaultValue = "") String search_name,
		@RequestParam(value = "start_time", defaultValue = "") String start_time,
		@RequestParam(value = "end_time", defaultValue = "") String end_time,
		@RequestParam(value = "profit_sharing_status", defaultValue = "") String profit_sharing_status,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		//TODO
		RequestEntity requestEntity = this.createResponseEntity("IncomeManger", "incomeWalletList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		param.put("profit_sharing_status", profit_sharing_status);
		param.put("search_type", search_type);
		param.put("search_name", search_name);
		param.put("start_time", start_time);
		param.put("end_time", end_time);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
		
		
	}
	
	
	@RequestMapping(value="/manager/distributor/add",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity distributorAdd(   
		    HttpEntity<Object> entity,
		    @RequestHeader("accessToken") String accessToken,
		    @RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("DistributorManger", "distributorAdd", accessToken, version);
		requestEntity.setParam(entity.getBody()); 
		return this.process(requestEntity, serviceManger, message);
	}
	
	

	@RequestMapping(value="/manager/distributor",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getDistributorList( 
	    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
	    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
		@RequestParam(value = "search_type", defaultValue = "") String search_type,
		@RequestParam(value = "search_name", defaultValue = "") String search_name,
		@RequestParam(value = "province_id", defaultValue = "") String province_id,
		@RequestParam(value = "city_id", defaultValue = "") String city_id,
		@RequestParam(value = "start_time", defaultValue = "") String start_time,
		@RequestParam(value = "end_time", defaultValue = "") String end_time,
		@RequestParam(value = "distributor_id", defaultValue = "") String distributor_id,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("DistributorManger", "distributorList", accessToken, version);
		Map<String, Object> parMap = new HashMap<String, Object>();
		parMap.put("page_count", page_count);
		parMap.put("page_num", page_num);
		parMap.put("search_type", search_type);
		parMap.put("search_name", search_name);
		parMap.put("province_id", province_id);
		parMap.put("city_id", city_id);
		parMap.put("start_time", start_time);
		parMap.put("end_time", end_time);
		parMap.put("distributor_id", distributor_id);
		
		requestEntity.setParam(parMap); 
		return this.process(requestEntity, serviceManger, message);
	}
 
	
	*//**
	 * 用户管理（嗒嗒详情）
	 * @param user_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/user/{user_id}/wallet",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getUserWallet( 
		@PathVariable("user_id") String user_id,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("UserManger", "userWallet", accessToken, version);
		Map<String, Object> parMap = new HashMap<String, Object>();
		parMap.put("user_id", user_id);
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
	}
	
	
	*//**
	 * 收益管理（提现单据更新）
	 * @param user_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/income/transfer",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getTransferList( 
	    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
	    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
		@RequestParam(value = "search_type", defaultValue = "") String search_type,
		@RequestParam(value = "search_name", defaultValue = "") String search_name,
		@RequestParam(value = "start_time", defaultValue = "") String start_time,
		@RequestParam(value = "end_time", defaultValue = "") String end_time,
		@RequestParam(value = "status", defaultValue = "") String status,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		//TODO
		RequestEntity requestEntity = this.createResponseEntity("IncomeManger", "updateTransfer", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		param.put("search_type", search_type);
		param.put("search_name", search_name);
		param.put("status", status);
		param.put("start_time", start_time);
		param.put("end_time", end_time);
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
	}
	 
	
	*//**
	 * 收益管理（提现设置获取）
	 * @param page_count
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/income/transfer/setting",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getTransferSetting(
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		RequestEntity requestEntity = this.createResponseEntity("IncomeManger", "transferSetting", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		requestEntity.setParam(param);
		return this.process(requestEntity, serviceManger, message);
		
	}
	*//**
	 *收益管理（提现设置）
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/income/transfer/setting",method=RequestMethod.PUT)
	public @ResponseBody ResponseEntity updateTransfer1(   
		HttpEntity<Object> entity,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception { 
		RequestEntity requestEntity = this.createResponseEntity("IncomeManger", "updateTransfer", accessToken, version);
		Map<String, Object> parMap = (Map<String, Object>) entity.getBody();
		requestEntity.setParam(parMap);
		return this.process(requestEntity, serviceManger, message);
		
	}
	
	 
	
	*//**
	 * 用户管理列表
	 * @param page_count
	 * @param page_num
	 * @param search_type
	 * @param search_name
	 * @param course_type_id
	 * @param start_time
	 * @param end_time
	 * @param course_status
	 * @param course_class
	 * @param choreographer_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/user",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getUserList( 
	    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
	    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
		@RequestParam(value = "nick_name", defaultValue = "") String nick_name,
		@RequestParam(value = "province_id", defaultValue = "") String province_id,
		@RequestParam(value = "city_id", defaultValue = "") String city_id,
		@RequestParam(value = "start_time", defaultValue = "") String start_time,
		@RequestParam(value = "end_time", defaultValue = "") String end_time,
		@RequestParam(value = "user_id", defaultValue = "") String user_id,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception{
		
		RequestEntity requestEntity = this.createResponseEntity("UserManger", "userList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		param.put("nick_name", nick_name);
		param.put("province_id", province_id);
		param.put("city_id", city_id);
		param.put("start_time", start_time);
		param.put("end_time", end_time);
		param.put("user_id", user_id);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	
	
	*//**
	 * 订单列表查询
	 * @param page_count
	 * @param page_num
	 * @param search_type
	 * @param search_name
	 * @param course_type_id
	 * @param start_time
	 * @param end_time
	 * @param course_status
	 * @param course_class
	 * @param choreographer_id
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/bills",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getBillList(
	    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
	    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
	    @RequestParam(value = "search_type", defaultValue = "") String search_type,
	    @RequestParam(value = "search_name", defaultValue = "") String search_name,
	    @RequestParam(value = "choreographer_id", defaultValue = "") String choreographer_id,
	    @RequestParam(value = "time_type", defaultValue = "") String time_type,
	    @RequestParam(value = "start_time", defaultValue = "") String start_time,
	    @RequestParam(value = "end_time", defaultValue = "") String end_time,
		@RequestParam(value = "status", defaultValue = "") String status,
		@RequestParam(value = "bill_id", defaultValue = "") String bill_id,
		@RequestParam(value = "course_type_id", defaultValue = "") String course_type_id,
		@RequestParam(value = "user_id", defaultValue = "") String user_id,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception{
		
		RequestEntity requestEntity = this.createResponseEntity("BillsManger", "billsList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		param.put("search_type", search_type);
		param.put("search_name", search_name);
		param.put("choreographer_id", choreographer_id);
		param.put("time_type", time_type);
		param.put("start_time", start_time);
		param.put("end_time", end_time);
		param.put("status", status);
		param.put("course_type_id", course_type_id);
		param.put("bill_id", bill_id);
		param.put("user_id", user_id);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	
	
	*//**
	 * 教练管理-列表查询
	 * @param page_count
	 * @param page_num
	 * @param search_type
	 * @param search_name
	 * @param choreographer_id
	 * @param time_type
	 * @param start_time
	 * @param end_time
	 * @param status
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/choreographer/single",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getChoreographerSingleList(
		@RequestParam(value = "page_count", defaultValue = "20") String page_count,	  
		@RequestParam(value = "page_num", defaultValue = "1") String page_num,
		@RequestParam(value = "search_type", defaultValue = "") String search_type,
		@RequestParam(value = "search_name", defaultValue = "") String search_name,
	    @RequestParam(value = "start_time", defaultValue = "") String start_time,
	    @RequestParam(value = "end_time", defaultValue = "") String end_time,
	    @RequestParam(value = "city_id", defaultValue = "") String city_id,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		logger.info("---〉查询教练列表信息. --- ");

		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "getChoreographerSingleList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		param.put("search_name", search_name);
		if(StringUtils.isBlank(search_name)){
			search_type = null;
		}
		param.put("search_type", search_type);
		param.put("start_time", start_time);
		param.put("end_time", end_time);
		param.put("city_id", city_id);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}

	*//**
	 * 教练编辑
	 * @param choreographer_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/choreographer/single/{choreographer_id}",method=RequestMethod.PUT)
	public @ResponseBody ResponseEntity updateChoreographerSingle(
			@PathVariable("choreographer_id") String choreographer_id, 
			HttpEntity<Object> entity,
			@RequestHeader("accessToken") String accessToken,
			@RequestHeader("version") String version) throws Exception {
			logger.info("---〉更新教练信息. --- ");
			RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "updateChoreographerSingle", accessToken, version);
			Map<String, Object> parMap = (Map<String, Object>) entity.getBody();
			parMap.put("choreographer_id", choreographer_id);
			requestEntity.setParam(parMap);
			
		return this.process(requestEntity, serviceManger, message);
	}
	
	*//**
	 * 新增教练
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/choreographer/single",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity createChoreographerSingle(   
	    HttpEntity<Object> entity,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		logger.info("---〉新增教练. --- ");
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "createChoreographerSingle", accessToken, version);
		requestEntity.setParam(entity.getBody());
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	*//**
	 * 机构列表查询
	 * @param page_count
	 * @param page_num
	 * @param search_type
	 * @param search_name
	 * @param start_time
	 * @param end_time
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/choreographer/corp", method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getChoreographerCorpList(
		@RequestParam(value = "page_count", defaultValue = "20") String page_count,	  
		@RequestParam(value = "page_num", defaultValue = "1") String page_num,
		@RequestParam(value = "search_type", defaultValue = "") String search_type,
		@RequestParam(value = "search_name", defaultValue = "") String search_name,
	    @RequestParam(value = "start_time", defaultValue = "") String start_time,
	    @RequestParam(value = "end_time", defaultValue = "") String end_time,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "getChoreographerCorpList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		param.put("search_name", search_name);
		if(StringUtils.isBlank(search_name)){
			search_type = null;
		}
		param.put("search_type", search_type);
		param.put("start_time", start_time);
		param.put("end_time", end_time);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
		
	}
	
	*//**
	 * 编辑机构信息
	 * @param choreographer_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/choreographer/corp/{choreographer_id}",method=RequestMethod.PUT)
	public @ResponseBody ResponseEntity updateChoreographerCorpSingle(
	    @PathVariable("choreographer_id") String choreographer_id, 
	    HttpEntity<Object> entity,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		
		logger.info("---〉更新机构信息. --- ");
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "updateChoreographerCorp", accessToken, version);
		Map<String, Object> parMap = (Map<String, Object>) entity.getBody();
		parMap.put("choreographer_id", choreographer_id);
		requestEntity.setParam(parMap);
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	*//**
	 * 新增机构
	 * @param choreographer_id
	 * @param entity
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/choreographer/corp",method=RequestMethod.POST)
	public @ResponseBody ResponseEntity createChoreographerCorp(
	    HttpEntity<Object> entity,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		logger.info("---〉新增机构. --- ");
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "createChoreographerCorp", accessToken, version);
		requestEntity.setParam(entity.getBody());
		
		return this.process(requestEntity, serviceManger, message);
	}



	
	*//**
	 * 教练机构账户详情
	 * @param choreographer_id
	 * @param page_count
	 * @param page_num
	 * @param type
	 * @param start_time
	 * @param end_time
	 * @param accessToken
	 * @param version
	 * @return
	 * @throws Exception
	 *//*
	@RequestMapping(value="/manager/choreographer/{choreographer_id}/wallet",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getChoreographerWalletList(
	    @PathVariable("choreographer_id") String choreographer_id, 
	    @RequestParam(value = "page_count", defaultValue = "20") String page_count, 	
	    @RequestParam(value = "page_num", defaultValue = "1") String page_num,
	    @RequestParam(value = "type", defaultValue = "") String type,
	    @RequestParam(value = "start_time", defaultValue = "") String start_time,    
	    @RequestParam(value = "end_time", defaultValue = "") String end_time,    
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception {
		
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "getChoreographerWalletList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("page_count", page_count);
		param.put("page_num", page_num);
		param.put("type", type);
		param.put("start_time", start_time);
		param.put("end_time", end_time);
		param.put("choreographer_id", choreographer_id);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
			
	}
	
	@RequestMapping(value="/manager/choreographer/recommend",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getRecommendList(
	    @RequestParam(value = "start", defaultValue = "0") String start,
	    @RequestParam(value = "end", defaultValue = "10") String end,
	    @RequestParam(value = "city_id", defaultValue = "") String city_id,
	    @RequestParam(value = "search_name", defaultValue = "") String search_name,
	    @RequestParam(value = "longitude", defaultValue = "") String longitude,
	    @RequestParam(value = "latitude", defaultValue = "") String latitude,
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception{
		
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "getRecommendList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("start", start);
		param.put("end", end);
		param.put("city_id", city_id);
		param.put("search_name", search_name);
		param.put("longitude", longitude);
		param.put("latitude", latitude);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	@RequestMapping(value="/manager/getAreaList",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getAreaList(
			@RequestParam(value="type", defaultValue = "") String type,
			@RequestHeader("accessToken") String accessToken,
			@RequestHeader("version") String version) throws Exception{
		
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "getAreaList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("type", type);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	
	@RequestMapping(value="/manager/choreographer/{choreographer_id}/special",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getSpecialList(
	    @PathVariable("choreographer_id") String choreographer_id, 
	    @RequestHeader("accessToken") String accessToken,
	    @RequestHeader("version") String version) throws Exception{
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "getSpecialList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("choreographer_id", choreographer_id);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}
	
	@RequestMapping(value="/manager/choreographer/{choreographer_id}/courses",method=RequestMethod.GET)
	public @ResponseBody ResponseEntity getCoursesList(
		    @PathVariable("choreographer_id") String choreographer_id, 
		    @RequestParam(value = "record_count", defaultValue = "10") String record_count, 
		    @RequestParam(value = "row_num", defaultValue = "") String row_num,     
		    @RequestHeader("accessToken") String accessToken,
		    @RequestHeader("version") String version) throws Exception{
		
		RequestEntity requestEntity = this.createResponseEntity("ChoreographerManger", "getCoursesList", accessToken, version);
		Map<String, Object> param = new HashMap<String, Object>();
		
		param.put("record_count", record_count);
		param.put("row_num", row_num);
		param.put("choreographer_id", choreographer_id);
		requestEntity.setParam(param);
		
		return this.process(requestEntity, serviceManger, message);
	}
*/
	
	
}
