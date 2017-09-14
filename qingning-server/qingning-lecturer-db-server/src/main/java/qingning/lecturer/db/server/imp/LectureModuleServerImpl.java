
package qingning.lecturer.db.server.imp;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import qingning.common.entity.QNLiveException;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.ILectureModuleServer;

import java.util.*;

public class LectureModuleServerImpl implements ILectureModuleServer {

	@Autowired(required = true)
	private FansMapper fansMapper;

	@Autowired(required = true)
	private LecturerMapper lecturerMapper;

	@Autowired(required = true)
	private LiveRoomMapper liveRoomMapper;

	@Autowired(required = true)
	private UserMapper userMapper;

	@Autowired(required = true)
	private LoginInfoMapper loginInfoMapper;

	@Autowired(required = true)
	private CoursesMapper coursesMapper;

	@Autowired(required = true)
	private CourseImageMapper courseImageMapper;

	@Autowired(required = true)
	private CourseAudioMapper courseAudioMapper;

	@Autowired(required = true)
	private CourseMessageMapper courseMessageMapper;

	@Autowired(required = true)
	private CoursesStudentsMapper coursesStudentsMapper;
	
	@Autowired(required = true)
	private DistributerMapper distributerMapper;

	@Autowired(required = true)
	private LecturerDistributionInfoMapper lecturerDistributionInfoMapper;
	
	@Autowired(required = true)
	private LecturerCoursesProfitMapper lecturerCoursesProfitMapper;
	
	@Autowired(required = true)
	private RoomDistributerMapper roomDistributerMapper;
	
	@Autowired(required = true)
	private RoomDistributerCoursesMapper roomDistributerCoursesMapper;
	
	@Autowired(required = true)
	private RoomDistributerDetailsMapper roomDistributerDetailsMapper;
	
	@Autowired(required = true)
	private LecturerDistributionLinkMapper lecturerDistributionLinkMapper;

	@Autowired(required = true)
	private SystemConfigMapper systemConfigMapper;

	@Autowired(required = true)
	private SeriesMapper seriesMapper;

    @Autowired(required = true)
    private SeriesStudentsMapper seriesStudentsMapper;

	@Autowired(required = true)
	private SaaSCourseMapper saaSCourseMapper;

    @Autowired(required = true)
    private SaaSShopMapper shopMapper;

	@Autowired(required = true)
	private SaaSCourseMapper saasCourseMapper;

	@Autowired(required = true)
	private CourseGuestMapper courseGuestMapper;
	/**
	 * 创建直播间
	 */
	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String,Object> createLiveRoom(Map<String, Object> reqMap) {
		Date now = new Date();
		//1.插入直播间表
		Map<String,Object> liveRoom = new HashMap<String,Object>();
		liveRoom.put("room_id", reqMap.get("room_id"));
		liveRoom.put("appName",reqMap.get("appName"));
		liveRoom.put("user_id", reqMap.get("user_id"));
		liveRoom.put("rq_code", reqMap.get("room_id"));
		liveRoom.put("room_address", reqMap.get("room_address"));
		liveRoom.put("room_name", reqMap.get("room_name"));
		liveRoom.put("avatar_address", reqMap.get("avatar_address"));
		liveRoom.put("room_remark", reqMap.get("room_remark"));
		liveRoom.put("lecturer_id", reqMap.get("user_id"));
		liveRoom.put("create_time", now);		
		liveRoom.put("update_time", now);
		liveRoomMapper.insertLiveRoom(liveRoom);

		//2.如果该用户为普通用户，则需要插入讲师表，并且修改登录信息表中的身份，
		// 同时插入t_lecturer_distribution_info讲师分销信息表(统计冗余表)
		boolean isLecturer = (Boolean)reqMap.get("isLecturer");
		if(isLecturer == false){
			//2.1插入讲师表
			Map<String,Object> lecturer = new HashMap<String,Object>();
			lecturer.put("lecturer_id", reqMap.get("user_id"));
			lecturer.put("live_room_num", 1L);
			lecturer.put("create_time", now);
			lecturer.put("update_time", now);
			lecturerMapper.insertLecture(lecturer);

			//2.2修改登录信息表 身份
			Map<String,Object> updateMap = new HashMap<>();
			updateMap.put("user_id",reqMap.get("user_id").toString());
			updateMap.put("add_role",","+ Constants.USER_ROLE_LECTURER);
			loginInfoMapper.updateUserRole(updateMap);

			//2.3插入讲师分销信息表(统计冗余表)
			Map<String,Object> lecturerDistributionInfo = new HashMap<String,Object>();
			lecturerDistributionInfo.put("lecturer_id", reqMap.get("user_id"));
			lecturerDistributionInfo.put("create_time", now);
			lecturerDistributionInfo.put("update_time", now);			
			lecturerDistributionInfoMapper.insertLecturerDistributionInfo(lecturerDistributionInfo);
		}

		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("room_id", reqMap.get("room_id"));		
		return resultMap;
	}

	@Override
	public Map<String, Object> findLectureByLectureId(String lecture_id) {
		return lecturerMapper.findLectureByLectureId(lecture_id);
	}

	@Override
	public Map<String, Object> findLiveRoomByRoomId(String room_id) {
		return liveRoomMapper.findLiveRoomByRoomId(room_id);
	}

	@Override
	/**
	 * 更新直播间
	 */
	public Map<String, Object> updateLiveRoom(Map<String, Object> reqMap) {
		Date date = new Date(System.currentTimeMillis());
		Date last_update_time = null;
		if(!MiscUtils.isEmpty(reqMap.get("update_time"))){
			last_update_time = new Date(MiscUtils.convertObjectToLong(reqMap.get("update_time")));
		}
		reqMap.put("update_time", date);
		reqMap.put(Constants.SYS_FIELD_LAST_UPDATE_TIME, last_update_time);
		
		Integer updateCount = liveRoomMapper.updateLiveRoom(reqMap);
		
		Map<String,Object> dbResultMap = new HashMap<String,Object>();
		dbResultMap.put("updateCount", updateCount);
		dbResultMap.put("update_time", date);
		return dbResultMap;
	}

	@Override
	public Map<String,Object> createCourse(Map<String, Object> reqMap) {
		Map<String,Object> course = new HashMap<String,Object>();
		course.put("course_id", MiscUtils.getUUId());
		course.put("appName",reqMap.get("appName"));
		course.put("room_id", reqMap.get("room_id"));
		course.put("lecturer_id", reqMap.get("user_id"));
		course.put("course_title", reqMap.get("course_title"));
		course.put("course_url", reqMap.get("course_url"));
		Date startTime = new Date(MiscUtils.convertObjectToLong(reqMap.get("start_time")));
		course.put("start_time", startTime);
		course.put("real_start_time", startTime);
		String course_type = (String)reqMap.get("course_type");
		course.put("course_type", reqMap.get("course_type"));
		course.put("status", "1");
		course.put("rq_code", course.get("course_id"));
		course.put("room_id", reqMap.get("room_id"));
		if("1".equals(course_type)){
			course.put("course_password",reqMap.get("course_password").toString());
		}else if("2".equals(course_type)){
			course.put("course_price", (Long)reqMap.get("course_price"));
		}
		if(!MiscUtils.isEmpty(reqMap.get("series_id"))){//是系列课
			course.put("series_id", reqMap.get("series_id"));
			course.put("course_updown","0");
			course.put("series_course_updown",reqMap.get("updown"));
		}else{//不是系列课
			course.put("series_course_updown","0");
			course.put("course_updown",reqMap.get("updown"));
		}
		Date now = new Date();
		course.put("create_time", now);
		course.put("create_date", now);
		course.put("update_time", now);
		course.put("im_course_id", reqMap.get("im_course_id"));
		course.put("classify_id",reqMap.get("classify_id"));


		coursesMapper.insertCourse(course);

		Map<String ,Object> dbResultMap = new HashMap<String,Object>();
		dbResultMap.put("course_id",course.get("course_id"));
		return dbResultMap;
	}

	@Override
	public Map<String, Object> findCourseByCourseId(String courseId) {
		return coursesMapper.findCourseByCourseId(courseId);
	}

	@Override
	public Map<String, Object> updateCourse(Map<String, Object> reqMap) {
		Integer updateCount = 0;
		Date now = (Date)reqMap.get("now");
		Map<String,Object> course = new HashMap<String,Object>();
		course.put("course_id", reqMap.get("course_id"));
		
		if("2".equals(reqMap.get("status"))){
			course.put("end_time", now);
			course.put("status", "2");			
		}else if("5".equals(reqMap.get("status"))) {
			course.put("update_time", now);
			course.put("status", "5");
		}else if("1".equals(reqMap.get("status"))){
			course.put("status", "1");
		}else{
			Object course_title = reqMap.get("course_title");
			Object start_time = reqMap.get("start_time");
			if(!MiscUtils.isEmpty(course_title)){
				course.put("course_title", course_title);
			}
			if(!MiscUtils.isEmpty(start_time)){
				course.put("start_time", new Date(MiscUtils.convertObjectToLong(start_time)));
				course.put("real_start_time", new Date(MiscUtils.convertObjectToLong(start_time)));
			}
			course.put("course_remark", reqMap.get("course_remark"));
			course.put("course_url", reqMap.get("course_url"));
			course.put("course_password", reqMap.get("course_password"));
			if(!MiscUtils.isEmpty(reqMap.get("update_time"))){
				course.put(Constants.SYS_FIELD_LAST_UPDATE_TIME, new Date(MiscUtils.convertObjectToLong(reqMap.get("update_time"))));
			}
		}
		course.put("update_time", now);		
		updateCount=coursesMapper.updateCourse(course);
		
		Map<String, Object> dbResultMap = new HashMap<String, Object>();
		dbResultMap.put("update_count", updateCount);
		dbResultMap.put("update_time", now);
		return dbResultMap;		
	}

	@Override
	public List<Map<String, Object>> findCourseListForLecturer(Map<String, Object> queryMap) {
		queryMap.put("status","2");
		queryMap.put("orderType","2");
		return coursesMapper.findCourseListForLecturer(queryMap);
	}

	@Override
	public void createCoursePPTs(Map<String, Object> reqMap) {
		courseImageMapper.createCoursePPTs(reqMap);
	}

	@Override
	public void deletePPTByCourseId(String course_id) {
		courseImageMapper.deletePPTByCourseId(course_id);
	}

	@Override
	public List<Map<String, Object>> findPPTListByCourseId(String course_id) {
		return courseImageMapper.findPPTListByCourseId(course_id);
	}

	@Override
	public List<Map<String, Object>> findAudioListByCourseId(String course_id) {
		return courseAudioMapper.findAudioListByCourseId(course_id);
	}

	@Override
	public List<Map<String, Object>> findCourseMessageList(Map<String, Object> queryMap) {
		return courseMessageMapper.findCourseMessageList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findCourseStudentList(Map<String, Object> queryMap) {
		return coursesStudentsMapper.findCourseStudentList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findCourseAllStudentList(String course_id) {
		return coursesStudentsMapper.findCourseAllStudentList(course_id);
	}

	@Override
	public Map<String, Object> findLoginInfoByUserId(String userId) {
		return loginInfoMapper.findLoginInfoByUserId(userId);
	}

	@Override
	public List<Map<String, Object>> findBanUserListInfo(Map<String,Object> banUserIdList) {
		return coursesStudentsMapper.findBanUserListInfo(banUserIdList);
	}

	@Override
	public Map<String,Object> findCourseMessageMaxPos(String course_id) {
		return courseMessageMapper.findCourseMessageMaxPos(course_id);
	}

	@Override
	public List<Map<String, Object>> findCourseProfitList(Map<String, Object> queryMap) {
		return lecturerCoursesProfitMapper.findCourseProfitList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerInfo(Map<String, Object> paramters) {
		return roomDistributerMapper.findRoomDistributerInfo(paramters);
	}

	@Override
	public List<Map<String, Object>> findRoomDistributerCourseInfo(Map<String, Object> paramters) {
		return roomDistributerCoursesMapper.findRoomDistributerCourseInfo(paramters);
	}
	
	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String,Object> createRoomDistributer(Map<String, String> reqMap) throws Exception {
		Map<String,Object> record = new HashMap<String,Object>();		
		record.put("room_id", reqMap.get("room_id"));
		record.put("distributer_id", reqMap.get("distributer_id"));			
		Date date = new Date(System.currentTimeMillis());
		record.put("current_time", date);
		//1.插入t_distributer
		if("1".equals(reqMap.get(Constants.SYS_INSERT_DISTRIBUTER))){
			distributerMapper.insertDistributer(record);
		}
		//2.插入t_room_distributer    
		Map<String,Object> distributer = roomDistributerMapper.findRoomDistributer(record);
		Map<String,Object> roomDistributer = new HashMap<String,Object>();
		roomDistributer.put("done_time", date);
		roomDistributer.put("click_num", 0l);
		String room_distributer_details_id = (String)reqMap.get("newRqCode");
		roomDistributer.put("lecturer_distribution_id", reqMap.get("lecturer_distribution_id"));
		roomDistributer.put("room_distributer_details_id", room_distributer_details_id);
		roomDistributer.put("profit_share_rate", MiscUtils.convertObjectToLong(reqMap.get("profit_share_rate")));		
        String effective_time = reqMap.get("effective_time");
        roomDistributer.put("effective_time", reqMap.get("effective_time"));
        roomDistributer.put("rq_code", room_distributer_details_id);
        Calendar calEndDate = Calendar.getInstance();
        calEndDate.setTime(date);
        if("1".equals(effective_time)){
        	calEndDate.add(Calendar.MONTH, 1);
        } else if("2".equals(effective_time)){
        	calEndDate.add(Calendar.MONTH, 3);
        } else if("3".equals(effective_time)){
        	calEndDate.add(Calendar.MONTH, 6);
        } else if("4".equals(effective_time)){
        	calEndDate.add(Calendar.MONTH, 9);
        } else if("5".equals(effective_time)){
        	calEndDate.add(Calendar.YEAR, 1);
        } else if("6".equals(effective_time)){
        	calEndDate.add(Calendar.YEAR, 2);
        } else {
        	calEndDate = null;
        }
        if(calEndDate!=null){
        	roomDistributer.put("end_date",calEndDate.getTime());
        } else {
        	roomDistributer.put("end_date",null);
        } 
		if(!MiscUtils.isEmpty(distributer)){
			roomDistributer.put("room_distributer_id", distributer.get("room_distributer_id"));
			roomDistributer.put("update_time", date);
			roomDistributer.put("last_update_time", distributer.get("update_time"));
			roomDistributer.put("last_recommend_num", 0l);
			roomDistributer.put("last_course_num", 0l);
			roomDistributer.put("last_done_num", 0l);
			roomDistributer.put("last_total_amount", 0l);
			roomDistributer.put("recommend_num", MiscUtils.convertObjectToLong("new_recommend_num"));
			roomDistributer.put("course_num", MiscUtils.convertObjectToLong("new_course_num"));
			roomDistributer.put("done_num", MiscUtils.convertObjectToLong("new_done_num"));
			roomDistributer.put("total_amount", MiscUtils.convertObjectToLong("new_total_amount"));	
			
			int count = roomDistributerMapper.updateRoomDistributer(roomDistributer);
			if(count<1){
				throw new QNLiveException("100027");
			}
			for(String key:roomDistributer.keySet()){
				distributer.put(key, roomDistributer.get(key));
			}
		} else {
			roomDistributer.put("room_distributer_id", MiscUtils.getUUId());
			roomDistributer.put("room_id", reqMap.get("room_id"));
			roomDistributer.put("lecturer_id", reqMap.get("lecturer_id"));
			roomDistributer.put("distributer_id", reqMap.get("distributer_id"));
			roomDistributer.put("update_time", date);
			roomDistributer.put("create_time", date);
			roomDistributerMapper.insertRoomDistributer(roomDistributer);
			distributer=roomDistributer;
		}
		Map<String,Object> roomDistributerDetails = new HashMap<String,Object>();
		roomDistributerDetails.put("lecturer_distribution_id", distributer.get("lecturer_distribution_id"));
		roomDistributerDetails.put("room_distributer_details_id", room_distributer_details_id);
		roomDistributerDetails.put("distributer_id", distributer.get("distributer_id"));
		roomDistributerDetails.put("lecturer_id", distributer.get("lecturer_id"));
		roomDistributerDetails.put("room_id", distributer.get("room_id"));
		roomDistributerDetails.put("effective_time", distributer.get("effective_time"));
		roomDistributerDetails.put("end_date", distributer.get("end_date"));
		roomDistributerDetails.put("create_date", date);
		roomDistributerDetails.put("update_time", date);
		roomDistributerDetails.put("done_time", date);
		roomDistributerDetails.put("profit_share_rate", MiscUtils.convertObjectToLong(reqMap.get("profit_share_rate")));
		roomDistributerDetails.put("rq_code", room_distributer_details_id);
		roomDistributerDetails.put("create_time", date);
		roomDistributerDetails.put("status", "0");
		roomDistributerDetailsMapper.insertRoomDistributerDetails(roomDistributerDetails);
		
		String lastRoomDistributerDetailsId = reqMap.get("last_room_distributer_details_id");
		String lastRecommendNum = reqMap.get("last_recommend_num");
		String lastDoneNum = reqMap.get("last_done_num");
		String lastTotalAmount = reqMap.get("last_total_amount");
		String lastCourseNum = reqMap.get("last_course_num");
		Date doneTime = null;
		if(!MiscUtils.isEmpty(reqMap.get("last_done_time"))){
			doneTime = new Date(MiscUtils.convertObjectToLong(reqMap.get("last_done_time")));
		}
		Date endTime =  null;
		if(!MiscUtils.isEmpty(reqMap.get("last_end_date"))){
			endTime = new Date(MiscUtils.convertObjectToLong(reqMap.get("last_end_date")));
		}
		long clickNum = MiscUtils.convertObjectToLong(reqMap.get("last_click_num"));
		String rqCode = reqMap.get("rq_code");
		if(!MiscUtils.isEmpty(lastRoomDistributerDetailsId)){
			roomDistributerDetails = new HashMap<String,Object>();
			if(!MiscUtils.isEmpty(lastRecommendNum)){
				roomDistributerDetails.put("recommend_num", lastRecommendNum);
			}
			if(!MiscUtils.isEmpty(lastDoneNum)){
				roomDistributerDetails.put("done_num", lastDoneNum);
			}
			if(!MiscUtils.isEmpty(lastTotalAmount)){
				roomDistributerDetails.put("total_amount", lastTotalAmount);
			}
			if(!MiscUtils.isEmpty(lastCourseNum)){
				roomDistributerDetails.put("course_num", lastCourseNum);
			}
			if(doneTime!=null){
				roomDistributerDetails.put("done_time", doneTime);
			}
			if(endTime!=null){
				roomDistributerDetails.put("end_date", endTime);
			}			
			roomDistributerDetails.put("click_num", clickNum);
			if(!MiscUtils.isEmpty(rqCode)){
				roomDistributerDetails.put("rq_code", rqCode);
			}
			if(!MiscUtils.isEmpty(roomDistributerDetails)){
				roomDistributerDetails.put("room_distributer_details_id", lastRoomDistributerDetailsId);
				roomDistributerDetails.put("status", "1");
				roomDistributerDetailsMapper.updateRoomDistributerDetails(roomDistributerDetails);
			}
		}
		
		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("now", date);
		return resultMap;
	}

	@Override
	public List<String> findLatestStudentAvatarAddList(Map<String, Object> queryMap) {
		return coursesStudentsMapper.findLatestStudentAvatarAddList(queryMap);
	}

	@Override
	public List<Map<String, Object>> findLiveRoomByLectureId(String lecture_id) {
		return liveRoomMapper.findLiveRoomByLectureId(lecture_id);
	}
    //TODO
	@Override
	public Map<String, Object> findDistributerInfo(Map<String, Object> paramters) {
		return null;
	}

	@Override
	public List<Map<String, Object>> findRoomFanList(Map<String, Object> paramters) {
		return fansMapper.findRoomFanList(paramters);
	}

	@Override
	public Map<String, Object> findLecturerDistributionByLectureId(String user_id) {
		return lecturerDistributionInfoMapper.findLecturerDistributionByLectureId(user_id);
	}

	@Override
	public Map<String, Object> findUserInfoByUserId(String userId) {
		return userMapper.findByUserId(userId);
	}

	@Override
	public List<Map<String, Object>> findCourseStudentListWithLoginInfo(String course_id){
		Map<String,Object> query = new HashMap<String,Object>();
		query.put("course_id", course_id);
		return coursesStudentsMapper.findCourseStudentListWithLoginInfo(query);
	}

	@Override
	public List<Map<String,Object>> findRoomFanListWithLoginInfo(String roomId) {
		Map<String,Object> query = new HashMap<String,Object>();
		query.put("room_id", roomId);
		return fansMapper.findRoomFanListWithLoginInfo(query);
	}

	@Override
	public List<String> findLoginInfoByUserIds(Map<String, Object> map) {
		return loginInfoMapper.findLoginInfoByUserIds(map);
	}

	//根据union查找登录信息
	@Override
	public Map<String, Object> getLoginInfoByLoginId(String unionID) {
		return loginInfoMapper.getLoginInfoByLoginId(unionID);
	}

	@Override
	public int insertLecturerDistributionLink(Map<String, Object> map) {		
		return lecturerDistributionLinkMapper.insertLecturerDistributionLink(map);
	}

	@Override
	public Map<String, Object> findAvailableRoomDistributer(Map<String, Object> record) {
		return roomDistributerMapper.findRoomDistributer(record);
	}

	@Override
	public Map<String, Object> findByDistributerId(String distributer_id) {		
		return distributerMapper.findByDistributerId(distributer_id);
	}

	@Override
	public List<Map<String, Object>> findFinishCourseListForLecturer(Map<String, Object> record) {
		return coursesMapper.findFinishCourseListForLecturer(record);
	}

	@Override
	public Map<String, Object> findLastestFinishCourse(Map<String, Object> record) {		
		return coursesMapper.findLastestFinishCourse(record);
	}

	@Override
	public List<Map<String, Object>> findDistributionRoomByLectureInfo(Map<String, Object> record) {		
		return roomDistributerDetailsMapper.findDistributionRoomByLectureInfo(record);
	}
	
	@Override
	public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {		
		return coursesStudentsMapper.findCourseIdByStudent(reqMap);
	}
	@Override
	public Map<String, Object> findCustomerServiceBySystemConfig(Map<String, Object> reqMap) {
		return systemConfigMapper.findCustomerServiceBySystemConfig(reqMap);
	}
	@Override
	public int insertServiceNoInfo(Map<String, String> map) {
		return lecturerMapper.insertServiceNoInfo(map);
	}
    @Override
    public int updateServiceNoInfo(Map<String, String> map) {
        return lecturerMapper.updateServiceNoInfo(map);
    }
	@Override
	public int updateServiceNoLecturerId(Map<String, String> map) {
		return lecturerMapper.updateServiceNoLecturerId(map);
	}

	@Override
	public Map<String, Object> findServiceNoInfoByAppid(String authorizer_appid) {
		return lecturerMapper.findServiceNoInfoByAppid(authorizer_appid);
	}

	@Override
	public Map<String, Object> findServiceNoInfoByLecturerId(String lectureId) {
		return lecturerMapper.findServiceNoInfoByLecturerId(lectureId);
	}

	@Override
	public int updateUser(Map<String, Object> parameters) {
		return userMapper.updateUser(parameters);
	}

	@Override
	public void updateLoginInfo(Map<String, Object> updateMap){loginInfoMapper.updateLoginInfo(updateMap);}

	@Override
	public List<Map<String, Object>> findRoomIdByFans(Map<String, Object> reqMap) {
		return fansMapper.findRoomIdByFans(reqMap);
	}

	@Override
	public Map<String, Object> findServiceTemplateInfoByLecturerId(Map<String, String> paramMap) {
		return lecturerMapper.findServiceTemplateInfoByLecturerId(paramMap);
	}

	@Override
	public void insertServiceTemplateInfo(Map<String, String> paramMap) {
		lecturerMapper.insertServiceTemplateInfo(paramMap);
	}


	/**
	 * 创建系列
	 * @param reqMap
	 * @return
	 */
	@Override
	public Map<String, Object> createSeries(Map<String, Object> reqMap) {
		Map<String,Object> series = new HashMap<String,Object>();
		series.put("series_id", MiscUtils.getUUId());
		series.put("lecturer_id", reqMap.get("user_id"));
		series.put("series_title", reqMap.get("series_title"));
		series.put("series_img", reqMap.get("series_img"));
		series.put("series_remark", reqMap.get("series_remark"));
		series.put("update_plan", reqMap.get("update_plan"));

		series.put("series_pay_remark", String.format(MiscUtils.getConfigKey("series_pay_remark"),  reqMap.get("update_plan").toString()));
		series.put("series_type", reqMap.get("series_type"));
		series.put("series_status", reqMap.get("series_status"));
		series.put("series_price", reqMap.get("series_price"));
		series.put("updown", reqMap.get("updown"));
		Date now = new Date();
		series.put("create_time",now);
		series.put("update_time", now);
		series.put("update_course_time", now);
		series.put("classify_id", reqMap.get("classify_id"));
		series.put("rq_code",  series.get("series_id"));
		series.put("series_course_type", reqMap.get("series_course_type"));
		series.put("appName",reqMap.get("appName"));

		if(!MiscUtils.isEmpty( reqMap.get("target_user"))){
			series.put("target_user",reqMap.get("target_user"));
		}
		seriesMapper.insertSeries(series);
		return series;
	}

	@Override
	public Map<String, Object> findSeriesBySeriesId(String series_id) {
		return seriesMapper.findSeriesBySeriesId(series_id);
	}

	@Override
	public Map<String, Object> updateSeries(Map<String, Object> record) {
		Date now = new Date();
		record.put("update_time",now);
		int updateCount = seriesMapper.updateSeries(record);
		Map<String, Object> dbResultMap = new HashMap<String, Object>();
		dbResultMap.put("update_count", updateCount);
		dbResultMap.put("update_time", now);
		return dbResultMap;
	}

	@Override
	public Map<String, Object> updateSeriesCourse(Map<String, Object> course) {
		Integer updateCount = null;
		Date now = new Date();
		course.put("update_time",now);
		updateCount=coursesMapper.updateSeriesCourse(course);
		Map<String, Object> dbResultMap = new HashMap<String, Object>();
		dbResultMap.put("update_count", updateCount);
		dbResultMap.put("update_time", now);
		return dbResultMap;
	}

	@Override
	public Map<String, Object> updateSaasSeriesCourse(Map<String, Object> course) {
		Integer updateCount = null;
		Date now = new Date();
		course.put("update_time",now);
		updateCount=saaSCourseMapper.updateSeriesCourse(course);
		Map<String, Object> dbResultMap = new HashMap<String, Object>();
		dbResultMap.put("update_count", updateCount);
		dbResultMap.put("update_time", now);
		return dbResultMap;
	}

	@Override
	public Map<String, Object> increaseSeriesCourse(String series_id) {
        Integer updateCount = null;
		Date now = new Date();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("update_course_time",now);
		map.put("series_id",series_id);
		updateCount=seriesMapper.increaseSeriesCourse(map);

		Map<String, Object> dbResultMap = new HashMap<String, Object>();
		dbResultMap.put("update_count", updateCount);
		dbResultMap.put("update_time", now);
		return dbResultMap;
	}

	@Override
	public Map<String, Object> delSeriesCourse(String series_id) {
		Integer updateCount = null;
		Date now = new Date();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("update_course_time",now);
		map.put("series_id",series_id);
		updateCount=seriesMapper.delSeriesCourse(map);
		Map<String, Object> dbResultMap = new HashMap<String, Object>();
		dbResultMap.put("update_count", updateCount);
		dbResultMap.put("update_time", now);
		return dbResultMap;
	}


    @Override
    public Map<String, Object> updateUpdown(Map<String,Object> record) {
		record.put("update_time",new Date());
        Integer updateCount = 0;
	    String query_from = record.get("query_from").toString();
        if(!MiscUtils.isEmpty(record.get("series_id"))){	//系列id不为null
            updateCount += seriesMapper.updateSeries(record);
        }else if(!MiscUtils.isEmpty(record.get("course_id"))){	//课程id不为null
            if(query_from.equals("0")){
                updateCount += coursesMapper.updateCourse(record);
            }else{
                updateCount += saaSCourseMapper.updateByPrimaryKey(record);
            }
        }

        Map<String, Object> dbResultMap = new HashMap<String, Object>();
        dbResultMap.put("update_count", updateCount);
        dbResultMap.put("update_time", new Date());
        return dbResultMap;
    }


    @Override
    public List<Map<String, Object>> findSeriesIdByStudent(Map<String, Object> reqMap) {
        return seriesStudentsMapper.findSeriesIdByStudent(reqMap);
    }


    @Override
    public Map<String, Object> getShopInfo(Map<String, Object> param) {
        return shopMapper.selectByPrimaryKey(param);
    }

    @Override
    public Map<String, Object> updateCourseLonely(Map<String, Object> course) {

        Integer updateCount = 0;
        if(course.get("series_course_type").equals("0")){
            updateCount = coursesMapper.updateCourse(course);
        }else{
            updateCount = saaSCourseMapper.updateByPrimaryKey(course);
        }
        Map<String, Object> dbResultMap = new HashMap<String, Object>();
        dbResultMap.put("update_count", updateCount);
        dbResultMap.put("update_time", new Date());
        return dbResultMap;
    }

    /**
     * 根据系列id获得该系列的所有学员列表
     */
	@Override
	public List<Map<String, Object>> findSeriesStudentListBySeriesId(String seriesId) {
		Map<String, Object> selectMap = new HashMap<>();
		selectMap.put("series_id", seriesId);
		return seriesStudentsMapper.selectSeriesStudentsByMap(selectMap);
	}

	/**
	 * 获取系列课收益明细列表
	 */
	@Override
	public List<Map<String, Object>> findSeriesProfitListByMap(Map<String, Object> reqMap) {
		return lecturerCoursesProfitMapper.selectSeriesProfitListByMap(reqMap);
	}

	/**
	 * 获取系列课收益统计（门票总收入，总收入）
	 */
	@Override
	public List<Map<String, Object>> findSeriesProfitStatistics(Map<String, Object> reqMap) {
		return lecturerCoursesProfitMapper.selectSeriesProfitStatistics(reqMap);
	}

	@Override
	public Map<String, Object> findSaasCourseByCourseId(String courseId) {
		return saasCourseMapper.selectByPrimaryKey(courseId);
	}

	@Transactional(rollbackFor=Exception.class)
	@Override
	public Map<String, Object> courseGurest(Map<String, Object> reqMap) {
		Integer updateCount = 0;
		Date now = new Date();
		String user_id = reqMap.get("user_id").toString();
		reqMap.put("create_time",now);
		reqMap.put("update_time",now);
		Map<String, Object> courseGuest = courseGuestMapper.findCourseGuestByUserAndCourse(reqMap);
		if(MiscUtils.isEmpty(courseGuest)){
			updateCount = courseGuestMapper.insertCourseGurest(reqMap);
		}else{
			updateCount = courseGuestMapper.updateCourseGuest(reqMap);
		}
		coursesStudentsMapper.updateStudent(reqMap);
		Map<String, Object> dbResultMap = new HashMap<String, Object>();
		dbResultMap.put("update_count", updateCount);
		dbResultMap.put("update_time", new Date());
		return dbResultMap;
	}

	@Override
	public List<Map<String, Object>> findGuestCourses(Map<String, Object> reqMap) {
		return courseGuestMapper.findGuestCourses(reqMap);
	}

	@Override
	public List<Map<String, Object>> findCourseGuestByCourseId(String course_id) {
		return courseGuestMapper.findCourseGuestByCourseId(course_id);
	}

	@Override
	public Map<String, Object> findByPhone(Map<String, Object> record) {
		return userMapper.findByPhone(record);
	}

	@Transactional(rollbackFor=Exception.class)
	@Override
	public void deleteUserAndLogin(String user_id) {
		userMapper.deleteUserByUserId(user_id);
		loginInfoMapper.delectLoginByUserId(user_id);
	}
	/**
	 * 根据条件查询课程列表
	 */
	@Override
	public List<Map<String, Object>> getCourseListByMap(Map<String, Object> reqMap) {
		return coursesMapper.findCourseByMap(reqMap);

	}
	/**
	 * 根据条件获取嘉宾课程列表，并关联查询出课程详情
	 */
	@Override
	public List<Map<String, Object>> getGuestAndCourseInfoByMap(Map<String, Object> reqMap) {
		return courseGuestMapper.findGuestAndCourseInfoByMap(reqMap);
	}
	/**
	 * 根据条件获取课程嘉宾记录
	 */
	@Override
	public List<Map<String, Object>> getCourseGuestByMap(Map<String, Object> selectCourseGuestMap) {
		return courseGuestMapper.findGuestCourseByMap(selectCourseGuestMap);
	}
}
