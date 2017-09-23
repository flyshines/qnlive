package qingning.shop.server.imp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.common.util.AccessTokenUtil;
import qingning.common.util.Constants;
import qingning.common.util.MiscUtils;
import qingning.server.AbstractQNLiveServer;
import qingning.server.annotation.FunctionName;
import qingning.server.rpc.initcache.*;
import qingning.server.rpc.manager.IShopModuleServer;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;

public class ShopServerImpl extends AbstractQNLiveServer {
    private static Logger log = LoggerFactory.getLogger(ShopServerImpl.class);

    private IShopModuleServer shopModuleServer;

    @Override
    public void initRpcServer() {
        if (shopModuleServer == null) {
            shopModuleServer = this.getRpcService("shopModuleServer");
            readCourseOperation = new ReadCourseOperation(shopModuleServer);
            readShopOperation = new ReadShopOperation(shopModuleServer);
            readUserOperation = new ReadUserOperation(shopModuleServer);
            readSeriesOperation = new ReadSeriesOperation(shopModuleServer);
            readLecturerOperation =  new ReadLecturerOperation(shopModuleServer);
        }
    }



    @SuppressWarnings("unchecked")
    @FunctionName("createCourse")
    public Map<String, Object> createCourse(RequestEntity reqEntity) throws Exception {
        Map<String, Object> reqMap = (Map<String, Object>) reqEntity.getParam();
        String lecturer_id = AccessTokenUtil.getUserIdFromAccessToken(reqEntity.getAccessToken());//通过token 获取讲师id
        Jedis jedis = jedisUtils.getJedis();
        Map query = new HashMap();
        query.put(Constants.CACHED_KEY_LECTURER_FIELD,lecturer_id);
        Map<String,String> lecturerMap = readLecturer(lecturer_id,this.generateRequestEntity(null, null, null, query),readLecturerOperation,jedis);
        //判断当前用户是否是讲师
        if(MiscUtils.isEmpty(lecturerMap)){
            throw new QNLiveException("100001");
        }


        if(reqMap.get("good_type").toString().equals("0")){
            //课程之间需要间隔三十分钟
            Long startTime = (Long)reqMap.get("start_time");
            String lecturerCoursesAllKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_ALL, query);
            long startIndex = startTime-30*60*1000;
            long endIndex = startTime+30*60*1000;
            long start = MiscUtils.convertInfoToPostion(startIndex , 0L);
            long end = MiscUtils.convertInfoToPostion(endIndex , 0L);
            Set<String> aLong = jedis.zrangeByScore(lecturerCoursesAllKey, start, end);
            for(String course_id : aLong){
                Map<String,Object> map = new HashMap<>();
                map.put("course_id",course_id);
                Map<String, String> course = readCourse(course_id, generateRequestEntity(null, null, null, map), readCourseOperation, jedis, true);
                if(course.get("status").equals("1")){
                    throw new QNLiveException("100029");
                }
            }
        }




        return null;
    }

}
