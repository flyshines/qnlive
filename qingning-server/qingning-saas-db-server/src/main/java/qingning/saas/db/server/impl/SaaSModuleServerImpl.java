
package qingning.saas.db.server.impl;


import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import qingning.db.common.mybatis.pageinterceptor.domain.PageBounds;
import qingning.db.common.mybatis.pageinterceptor.domain.PageList;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.ISaaSModuleServer;

import java.util.*;


public class SaaSModuleServerImpl implements ISaaSModuleServer {
    @Autowired(required = true)
    private UserMapper userMapper;
    @Autowired(required = true)
    private CoursesStudentsMapper coursesStudentsMapper;
    @Autowired(required = true)
    private FansMapper fansMapper;
    @Autowired(required = true)
    private SaaSShopMapper shopMapper;
    @Autowired(required = true)
    private SaaSBannerMapper bannerMapper;
    @Autowired(required = true)
    private SaaSCourseMapper saasCourseMapper;
    @Autowired(required = true)
    private SaaSShopUserMapper shopUserMapper;
    @Autowired(required = true)
    private SeriesMapper seriesMapper;
    @Autowired(required = true)
    private SeriesStudentsMapper seriesStudentsMapper;
    @Autowired(required = true)
    private CoursesMapper coursesMapper;
    @Autowired(required = true)
    private SaaSFeedBackMapper feedBackMapper;
    @Autowired(required = true)
    private SaaSCourseCommentMapper courseCommentMapper;

    @Override
    public List<Map<String, Object>> findCourseIdByStudent(Map<String, Object> reqMap) {
        return coursesStudentsMapper.findCourseIdByStudent(reqMap);
    }


    @Override
    public List<Map<String, Object>> findRoomIdByFans(Map<String, Object> reqMap) {
        return fansMapper.findRoomIdByFans(reqMap);
    }


    @Override
    public Map<String, Object> findUserInfoByUserId(String user_id) {
        return userMapper.findByUserId(user_id);
    }

    @Override
    public void updateShop(Map<String, Object> param) {
        shopMapper.updateByPrimaryKey(param);
    }

    @Override
    public Map<String, Object> getShopBannerList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()),Integer.valueOf(param.get("page_count").toString()));
        PageList<Map<String,Object>> result = bannerMapper.selectListByUserId(param,page);
        //根据优先级排序
        Collections.sort(result, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                int position1;
                int position2 = 0;
                Map<String, Object> map1 = (Map) o1;
                Map<String, Object> map2 = (Map) o2;
                if (map1.get("position") != null && StringUtils.isNotEmpty(map1.get("position") + "")) {
                    position1 = Integer.valueOf(map1.get("position").toString());
                }else{
                    return 1;
                }
                if (map2.get("position") != null && StringUtils.isNotEmpty(map2.get("position") + "")) {
                    position2 = Integer.valueOf(map2.get("position").toString());
                }
                if (position1 > position2) return 1;
                else if (position1 < position2) return -1;
                else return 1;
            }
        });
        Map<String,Object> res = new HashMap<>();
        res.put("list",result);
        res.put("total_count",result.getTotal());
        res.put("total_page",result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> addShopBanner(Map<String, Object> param) {
        bannerMapper.insert(param);
        return null;
    }

    /**
     * 前端获取店铺轮播列表
     */
	@Override
	public List<Map<String, Object>> getShopBannerListForFront(Map<String, Object> paramMap) {
		return bannerMapper.selectBannerListByMap(paramMap);
	}

    @Override
    public Map<String, Object> getShopInfo(Map<String, Object> param) {
        return shopMapper.selectByPrimaryKey(param);
    }
    
    /**
     * 根据系列id获取系列详情
     */
    @Override
    public Map<String, Object> findSeriesBySeriesId(String seriesId){
		return seriesMapper.findSeriesBySeriesId(seriesId);
    }

    /**
     * 根据条件查询系列id
     */
	@Override
	public List<Map<String, Object>> findSeriesStudentsByMap(Map<String, Object> selectSeriesStudentsMap) {
		return seriesStudentsMapper.selectSeriesStudentsByMap(selectSeriesStudentsMap);
	}

    @Override
    public void updateBanner(Map<String, Object> param) {
        bannerMapper.updateByPrimaryKey(param);
    }

    @Override
    public void addCourse(Map<String, Object> param) {
    	saasCourseMapper.insert(param);
    }

    @Override
    public void updateCourse(Map<String, Object> param) {
    	saasCourseMapper.updateByPrimaryKey(param);
    }

    /**
     * 根据课程id获取saas课程信息，从t_saas_course查询
     */
	@Override
	public Map<String, Object> findSaasCourseByCourseId(String courseId) {
		return saasCourseMapper.selectByPrimaryKey(courseId);
	}
	
	/**
	 * 根据课程id获取直播课程信息，从t_course查询
	 */
	@Override
	public Map<String, Object> findCourseByCourseId(String courseId) {
		return coursesMapper.findCourseByCourseId(courseId);
	}

	@Override
    public Map<String, Object> getSingleList(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()),Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String,Object>> result = saasCourseMapper.selectByShop(param,page);
        Map<String,Object> res = new HashMap<>();
        res.put("list",result);
        res.put("total_count",result.getTotal());
        res.put("total_page",result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> getShopUsers(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()),Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String,Object>> result = shopUserMapper.selectUsersByShop(param,page);
        Map<String,Object> res = new HashMap<>();
        res.put("list",result);
        res.put("total_count",result.getTotal());
        res.put("total_page",result.getPaginator().getTotalPages());
        return res;
    }

    @Override
    public Map<String, Object> getCourseComment(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()),Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String,Object>> result = courseCommentMapper.selectCommentByShop(param,page);
        Map<String,Object> res = new HashMap<>();
        res.put("list",result);
        res.put("total_count",result.getTotal());
        res.put("total_page",result.getPaginator().getTotalPages());
        return res;
    }
    @Override
    public Map<String, Object> getUserFeedBack(Map<String, Object> param) {
        PageBounds page = new PageBounds(Integer.valueOf(param.get("page_num").toString()),Integer.valueOf(param.get("page_size").toString()));
        PageList<Map<String,Object>> result = feedBackMapper.selectFeedBackByShop(param,page);
        Map<String,Object> res = new HashMap<>();
        res.put("list",result);
        res.put("total_count",result.getTotal());
        res.put("total_page",result.getPaginator().getTotalPages());
        return res;
    }
}