package qingning.common.util;

import org.apache.solr.common.util.Hash;
import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import redis.clients.jedis.Jedis;

import java.util.*;

public class SortUtils {

    /**
     * 获取非直播课程 列表id
     * @param redisKey
     * @param pageCount
     * @param lpos
     * @param jedis
     * @param operation
     * @param requestEntity
     * @return
     * @throws Exception
     */
    private List<String> getNonLiveCourses(String redisKey, int pageCount, String lpos, Jedis jedis, CommonReadOperation operation,RequestEntity requestEntity) throws Exception{





        int offset = 0;//偏移值
        Set<String> courseIdSet;//查询的课程idset
        List<Map<String,String>> courseList = new LinkedList<>();//课程对象列表
        Map<String, Object> resultMap = new HashMap<String, Object>();//最后返回的结果对象
        //判断传过来的课程状态
        String startIndex = "+inf";//坐标起始位
        String endIndex = "-inf";//坐标结束位
        if(!MiscUtils.isEmpty(lpos)){
            startIndex = "("+lpos;
        }


        courseIdSet = jedis.zrangeByScore(redisKey,startIndex,endIndex,offset,pageCount); //顺序找出couseid  (正在直播或者预告的)
        return new ArrayList(courseIdSet);
    }

    /**
     * 更改课程缓存
     * 上架或下架
     * @param course
     * @param jedis
     * @throws Exception
     */
    private void updateCourseListByRedis(Map<String,String> course,Jedis jedis,boolean is_live) throws Exception{
        Long position = Long.valueOf(course.get("position"));
        Map<String,String> keysMap = new HashMap<>();
        if(is_live){//直播课
            MiscUtils.courseTranferState(System.currentTimeMillis(), course);
            String live_course_status = course.get("live_course_status");
            if(live_course_status.equals("4") || live_course_status.equals("1")){
                keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP,course));
                keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN,course));
            }
            if(live_course_status.equals("2")){
                keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP,course));
                keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN  ,course));
            }
        }else{//非直播课
            keysMap.put(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP,course));
            keysMap.put(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP,course));
            keysMap.put(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN,course));
            keysMap.put(Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN,course));
        }

        for (String key : keysMap.keySet()) {
            Long score_time = null;
            if(key.equals(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP) || key.equals(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP)){//非直播课 和 课程类型 上架
                score_time = Long.valueOf(course.get("first_up_time"));//第一次上架时间

            }else  if(key.equals(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN) || key.equals(Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN)){//非直播课 和 课程类型 下架
                score_time = Long.valueOf(course.get("last_down_time"));//最后一次下架时间

            }else if(key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP) || key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN)){ //直播课 正在直播和预告 上架 下架
                score_time = Long.valueOf(course.get("start_time"));//课程开始时间

            }else if(key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP) || key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN)){//直播课 结束 上架 下架
                score_time = Long.valueOf(course.get("end_time"));//课程结束时间
            }
            long lpos = MiscUtils.convertInfoToPostion(score_time,position);//算出位置
            String redisKey = keysMap.get(key);
            TreeSet<String> keySet = (TreeSet<String>) jedis.zrangeByScore(redisKey, "(" + lpos, "+inf", 0, 1);//获取位置
            String firstKey =  keySet.first();//拿到第一个值
            Long keyRank = jedis.zrank(redisKey,firstKey);//拿到这个key 的值
            Long keyAmount = jedis.zcard(redisKey);

            for(long i=keyRank;i<=keyAmount;i++){
                String constantsPagingKey = null;
                Map map = new HashMap<>();
                map.put(Constants.PAGING_NUMBER,i);//页码
                map.put(Constants.CACHED_KEY_SHOP_FIELD,course.get(Constants.CACHED_KEY_SHOP_FIELD));//shop_id


                if(key.equals(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP)){//店铺 非直播 上架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP_PAGING;//店铺 非直播 上架 分页

                }else if(key.equals(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP)){//店铺 课程内容 上架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_GOODS_TYPE_UP_PAGING;//店铺 课程内容 上架 分页

                }else if(key.equals(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN)){//店铺 非直播 下架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN_PAGING;//店铺 非直播 下架 分页

                }else if(key.equals(Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN)){//店铺 课程内容 下架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN_PAGING;//店铺 课程内容 下架 分页

                }else if(key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP)){//店铺 直播课 预告/正在直播 上架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP_PAGING;//店铺 直播 预告/正在直播 上架 分页

                }else if(key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN)){//店铺 直播 预告/正在直播 下架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN_PAGING;//店铺 直播 预告/正在直播 下架 分页

                }else if(key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP)){//店铺 直播课 结束 上架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP_PAGING;//店铺 直播 结束 上架 分页

                }else if(key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN)){//店铺 直播 结束 下架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN_PAGING;//店铺 直播 结束 下架 分页
                }
                String pagingKey = MiscUtils.getKeyOfCachedData(constantsPagingKey,map);

                if(!MiscUtils.isEmpty(pagingKey)){
                    jedis.zrem(redisKey,pagingKey);
                    jedis.del(pagingKey);
                }
            }
        }
    }

    /**
     * 更改直播课程缓存
     * 上架或下架
     * @param course
     * @param jedis
     * @throws Exception
     */
    private void updateLiveCourseListByRedis(Map<String,String> course,Jedis jedis) throws Exception{
//        MiscUtils.courseTranferState(System.currentTimeMillis(), course);
//        String live_course_status = course.get("live_course_status");
//        Map<String,String> keysMap = new HashMap<>();
//
//        if(live_course_status.equals("4") || live_course_status.equals("1")){
//            keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP,course));
//            keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN,course));
//        }
//
//        if(live_course_status.equals("2")){
//            keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP,course));
//            keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN,MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN  ,course));
//        }
//






    }


//        /**
//         * 预告
//         */
//        if(courceStatus == 1 ){//如果预告或者是正在直播的课程
//            String startIndex ;//坐标起始位
//            String endIndex ;//坐标结束位
//            String getCourseIdKey;
//
//            //平台的预告中课程列表 预告和正在直播放在一起  按照直播开始时间顺序排序  根据分类获取不同的缓存
//            if(!MiscUtils.isEmpty(classify_id)){//有分类
//                Map<String,Object> map = new HashMap<String,Object>();
//                map.put(Constants.CACHED_KEY_CLASSIFY,classify_id);
//                getCourseIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_PREDICTION, map);//分类
//            }else if(lecture_id != null){
//                Map<String,Object> map = new HashMap<String,Object>();
//                map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
//                getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);//讲师
//            }else{ //首页
//                getCourseIdKey = Constants.CACHED_KEY_PLATFORM_COURSE_PREDICTION;
//            }
//            if(MiscUtils.isEmpty(courseId)){//如果没有传入courceid 那么就是最开始的查询  进行倒叙查询 查询现在的
//                long courseScoreByRedis = MiscUtils.convertInfoToPostion(System.currentTimeMillis(),0L);
//                startIndex = ""+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
//                endIndex ="+inf";//设置结束位置
//            }else{//传了courseid
//                Map<String,String> queryParam = new HashMap<String,String>();
//                queryParam.put("course_id", courseId);
//                RequestEntity requestParam = this.generateRequestEntity(null, null, null, queryParam);
//                Map<String, String> course = CacheUtils.readCourse(courseId, requestParam, readCourseOperation, jedis, true);//获取当前课程参数
//                long courseScoreByRedis = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("start_time")),  MiscUtils.convertObjectToLong(course.get("position")));//拿到当前课程在redis中的score
//                startIndex = "("+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
//                endIndex ="+inf";//设置结束位置
//            }
//            courseIdSet = jedis.zrangeByScore(getCourseIdKey,startIndex,endIndex,offset,pageCount); //顺序找出couseid  (正在直播或者预告的)
//            for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
//                courseIdList.add(course_id);
//            }
//
//            pageCount =  pageConts - courseIdList.size();//用展示数量减去获取的数量  查看是否获取到了足够的课程数
//            if( pageCount > 0){//如果返回的值不够
//                courseId = null;//把课程id设置为null  用来在下面的代码中进行判断
//                courceStatus = 2;//设置查询课程状态 为结束课程 因为查找出来的正在直播和预告的课程不够数量
//            }
//        }
//
//
//        //=========================下面的缓存使用另外一种方式获取====================================
//        /**
//         * 结束
//         */
//        if(courceStatus == 2 ){//查询结束课程
//            boolean key = true;//作为开关 用于下面是否需要接着执行方法
//            long startIndex = 0; //开始下标
//            long endIndex = -1;   //结束下标
//            String getCourseIdKey ;
//            //平台的已结束课程列表
//            if(!MiscUtils.isEmpty(classify_id)){//有分类
//                Map<String,Object> map = new HashMap<String,Object>();
//                map.put(Constants.CACHED_KEY_CLASSIFY,classify_id);
//                getCourseIdKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_PLATFORM_COURSE_CLASSIFY_FINISH, map);//分类
//            }else if(lecture_id != null){
//                Map<String,Object> map = new HashMap<String,Object>();
//                map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
//                getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);//讲师
//            }else{ //首页
//                getCourseIdKey = Constants.CACHED_KEY_PLATFORM_COURSE_FINISH;
//            }
//
//            long endCourseSum = jedis.zcard(getCourseIdKey);//获取总共有多少个结束课程
//            if(MiscUtils.isEmpty(courseId)){//如果课程ID没有 那么就从最近结束的课程找起
//                endIndex = -1;
//                startIndex = endCourseSum - pageCount;//利用总数减去我这边需要获取的数
//                if(startIndex < 0){
//                    startIndex = 0;
//                }
//            }else{ //如果有课程id  先获取课程id 在列表中的位置 然后进行获取其他课程id
//                long endRank = jedis.zrank(getCourseIdKey, courseId);
//                endIndex = endRank - 1;
//                if(endIndex >= 0){
//                    startIndex = endIndex - pageCount + 1;
//                    if(startIndex < 0){
//                        startIndex = 0;
//                    }
//                }else{
//                    key = false;//因为已经查到最后的课程没有必要往下查了
//                }
//            }
//            if(key){
//                courseIdSet = jedis.zrange(getCourseIdKey, startIndex, endIndex);
//                List<String> transfer = new ArrayList<>();
//                for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
//                    transfer.add(course_id);
//                }
//                Collections.reverse(transfer);
//                courseIdList.addAll(transfer);
//                if(!MiscUtils.isEmpty(lecture_id) && !MiscUtils.isEmpty(userId) && lecture_id.equals(userId)){
//                    courseId = null;
//                }
//            }
//        }
//    }
//
//    //</editor-fold>
//        if(!MiscUtils.isEmpty(lecture_id) && !MiscUtils.isEmpty(userId) && lecture_id.equals(userId)){
//        pageCount =  pageConts - courseIdList.size();
//        if(pageCount > 0 || updown == 2){//是讲师
//            boolean key = true;//作为开关 用于下面是否需要接着执行方法
//            long startIndex = 0; //开始下标
//            long endIndex = -1;   //结束下标
//            String getCourseIdKey ;
//            Map<String,Object> map = new HashMap<String,Object>();
//            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
//            getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_DOWN, map);//讲师 下架的课
//
//            long endCourseSum = jedis.zcard(getCourseIdKey);//获取总共有多少个结束课程
//            if(MiscUtils.isEmpty(courseId)){//如果课程ID没有 那么就从最近结束的课程找起
//                endIndex = -1;
//                startIndex = endCourseSum - pageCount;//利用总数减去我这边需要获取的数
//                if(startIndex < 0){
//                    startIndex = 0;
//                }
//            }else{ //如果有课程id  先获取课程id 在列表中的位置 然后进行获取其他课程id
//                if(jedis.exists(getCourseIdKey)){
//                    long endRank = jedis.zrank(getCourseIdKey, courseId);
//                    endIndex = endRank - 1;
//                    if(endIndex >= 0){
//                        startIndex = endIndex - pageCount + 1;
//                        if(startIndex < 0){
//                            startIndex = 0;
//                        }
//                    }else{
//                        key = false;//因为已经查到最后的课程没有必要往下查了
//                    }
//                }else{
//                    key = false;
//                }
//            }
//            if(key){
//                courseIdSet = jedis.zrange(getCourseIdKey, startIndex, endIndex);
//                List<String> transfer = new ArrayList<>();
//                for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
//                    transfer.add(course_id);
//                }
//                Collections.reverse(transfer);
//                courseIdList.addAll(transfer);
//            }
//        }
//    }
//    //====================================================上面是获取课程id集合======================================================================
//    //====================================================下面是针对用户 等其他操作=================================================================
//    //<editor-fold desc="根据课程id 获取课程对象并判断当前用户是否有加入课程">
//        if(courseIdList.size() > 0){
//        Map<String,String> queryParam = new HashMap<String,String>();
//        Map<String,Object> query = new HashMap<String,Object>();
//        query.put(Constants.CACHED_KEY_USER_FIELD, userId);
//        String key = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_USER_COURSES, query);//用来查询当前用户加入了那些课程
//
//        for(String course_id : courseIdList){
//            queryParam.put("course_id", course_id);
//            Map<String, String> courseInfoMap = CacheUtils.readCourse(course_id, this.generateRequestEntity(null, null, null, queryParam), readCourseOperation, jedis, true);//从缓存中读取课程信息
//            if(!MiscUtils.isEmpty(courseInfoMap)){
//                MiscUtils.courseTranferState(currentTime, courseInfoMap);//进行课程时间判断,如果课程开始时间大于当前时间 并不是已结束的课程  那么就更改课程的状态 改为正在直播
//                if(jedis.exists(key)){
//                    if(!MiscUtils.isEmpty(jedis.zrank(key, course_id))){//判断当前用户是否有加入这个课程
//                        courseInfoMap.put("student", "Y");
//                    } else {
//                        courseInfoMap.put("student", "N");
//                    }
//                }else{
//                    courseInfoMap.put("student", "N");
//                }
//                courseList.add(courseInfoMap);
//            }
//        }
//    }
//    //</editor-fold>
//        resultMap.put("course_list", courseList);
//        resultMap.put("course_list_size", courseList.size());
//        return resultMap;
}
