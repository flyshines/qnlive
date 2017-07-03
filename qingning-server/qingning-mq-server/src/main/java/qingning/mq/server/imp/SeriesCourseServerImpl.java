package qingning.mq.server.imp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.db.common.mybatis.persistence.ClassifyInfoMapper;
import qingning.db.common.mybatis.persistence.CoursesMapper;
import qingning.db.common.mybatis.persistence.SeriesMapper;
import qingning.server.AbstractMsgService;
import qingning.server.JedisBatchCallback;
import qingning.server.JedisBatchOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by GHS on 2017/6/2.
 */
public class SeriesCourseServerImpl extends AbstractMsgService {


    private static Logger log = LoggerFactory.getLogger(SeriesCourseServerImpl.class);
    private CoursesMapper coursesMapper;
    private SeriesMapper seriesMapper;
    @Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
        processSeriesCourseCache(requestEntity, jedisUtils, context);
    }


    private void processSeriesCourseCache(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {
        String appName = requestEntity.getAppName();
        ((JedisBatchCallback)jedisUtils.getJedis(appName)).invoke(new JedisBatchOperation(){
            private void processCached(Set<String> lecturerSet, Pipeline pipeline, Jedis jedis){
                for(String lecturerId : lecturerSet) {
                    Map<String,Object> map = new HashMap<>();
                    map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecturerId);
                    for(int i = 0;i<4;i++){
                        map.put("series_course_type",i);
                        String upkey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_UP, map);
                        String downkey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_DOWN, map);
                        jedis.del(upkey);
                        jedis.del(downkey);
                    }
                    List<Map<String, Object>> seriesList = seriesMapper.findSeriesByLecturer(lecturerId);
                    for(Map<String, Object> series : seriesList){
                        String series_id = series.get("series_id").toString();
                        long update_course_time = MiscUtils.convertObjectToLong(series.get("update_course_time"));
                        map.put("series_course_type",series.get("series_course_type"));
                        String upkey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_UP, map);
                        String downkey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_DOWN, map);
                        if(series.get("updown").equals("1")){
                            jedis.zadd(upkey, update_course_time,series_id);
                        }else{
                            jedis.zadd(downkey, update_course_time,series_id);
                        }
                        map.clear();
                        map.put("series_id",series_id);
                        String seriesCourseUpKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_UP, map);
                        String seriesCourseDownKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_SERIES_COURSE_DOWN, map);
                        jedis.del(seriesCourseDownKey);
                        jedis.del(seriesCourseUpKey);
                        List<Map<String, Object>> seriesCourseList = coursesMapper.findCourseBySeriesId(series_id);
                        for(Map<String, Object> course : seriesCourseList){
                            long update_time = MiscUtils.convertObjectToLong(course.get("update_time"));
                            if(course.get("series_course_updown").toString().equals("1")){
                                jedis.zadd(seriesCourseUpKey, update_time,course.get("course_id").toString());
                            }else{
                                jedis.zadd(seriesCourseDownKey, update_time,course.get("course_id").toString());
                            }
                        }
                    }
                }
            }
            @Override
            public void batchOperation(Pipeline pipeline, Jedis jedis) {
                Set<String> lecturerSet = jedis.smembers(Constants.CACHED_LECTURER_KEY);
                if(!MiscUtils.isEmpty(lecturerSet)){
                    processCached(lecturerSet, pipeline, jedis);
                }
            }
        });
    }

    public CoursesMapper getCoursesMapper() {
        return coursesMapper;
    }

    public void setCoursesMapper(CoursesMapper coursesMapper) {
        this.coursesMapper = coursesMapper;
    }

    public SeriesMapper getSeriesMapper() {
        return seriesMapper;
    }

    public void setSeriesMapper(SeriesMapper seriesMapper) {
        this.seriesMapper = seriesMapper;
    }
}
