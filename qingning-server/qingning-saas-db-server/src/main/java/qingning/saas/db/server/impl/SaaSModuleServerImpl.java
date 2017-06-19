
package qingning.saas.db.server.impl;


import org.springframework.beans.factory.annotation.Autowired;
import qingning.db.common.mybatis.persistence.*;
import qingning.server.rpc.manager.ISaaSModuleServer;
import java.util.List;
import java.util.Map;


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
    private SaaSCourseMapper courseMapper;
    @Autowired(required = true)
    private SaaSShopUserMapper shopUserMapper;

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

}
