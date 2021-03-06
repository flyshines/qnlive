package qingning.common.util;

import org.apache.solr.common.util.Hash;
import qingning.common.entity.QNLiveException;
import qingning.common.entity.RequestEntity;
import qingning.server.rpc.CommonReadOperation;
import qingning.server.rpc.initcache.ReadCourseOperation;
import qingning.server.rpc.initcache.ReadSeriesOperation;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.*;

public class SortUtils {

    /**
     * 获取课程列表
     *
     * @param redisKey      获取总列表分页
     * @param pageCount 具体分页列表
     * @param pageCount     要多少数
     * @param lpos          分页参数
     * @param jedis         jedis 对象
     * @return
     * @throws Exception
     */
    public static List<String> getList(String redisKey, String pageKey, int pageCount, String lpos, Jedis jedis) throws Exception {
        Map<String,Object> queryMap = new HashMap<>();
        Long page = 0L;
        List<String> goodsIdList = null;
        if (!MiscUtils.isEmpty(lpos)) {//传null
            TreeSet<String> keySet = (TreeSet<String>) jedis.zrangeByScore(redisKey, "(" + lpos, "+inf", 0, 1);//获取位置
            String firstKey = keySet.first();//拿到第一个值
            Long keyRank = jedis.zrank(redisKey, firstKey);//拿到这个key 的值
            page = keyRank - 1;
        } else {
            page = jedis.zcard(redisKey);
        }

        queryMap.put(Constants.PAGING_NUMBER, page);//页码
        String page_key = MiscUtils.getKeyOfCachedData(pageKey, queryMap);
        Set<String> goodsIds = jedis.zrevrangeByScore(page_key, lpos, "-inf", 0, pageCount);
        for (String goodsId : goodsIds) {
            goodsIdList.add(goodsId);
        }
        if (goodsIds.size() < pageCount) {
            page -= 1;
            if (page > 0) {
                goodsIds.clear();
                queryMap.put(Constants.PAGING_NUMBER, page);//页码
                page_key = MiscUtils.getKeyOfCachedData(pageKey, queryMap);
                goodsIds = jedis.zrevrangeByScore(page_key, lpos, "-inf", 0, (pageCount - goodsIdList.size()));
                for (String course_id : goodsIds) {
                    goodsIdList.add(course_id);
                }
            }
        }
        return goodsIdList;
    }


    /**
     * 刷新课程缓存
     *
     * @param shop_id
     * @param jedis
     * @param operation
     * @param requestEntity
     * @return
     * @throws Exception
     */
    public static void refreshCourseListByRedis(String shop_id, String goods_type, Jedis jedis, String page_key, ReadCourseOperation operation, RequestEntity requestEntity) throws Exception {
        String redisKey = "";
        String functionName = "";
        String lposKey = "";
        switch (page_key) {
            case Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP://店铺 非直播 上架
                functionName = Constants.SYS_SHOP_NON_LIVE_COUSE_UP;
                redisKey = Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP_PAGING;
                lposKey = Constants.FIRST_UP_TIME;
                break;

            case Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN://店铺 非直播 下架
                functionName = Constants.SYS_SHOP_NON_LIVE_COUSE_DOWN;
                redisKey = Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN_PAGING;
                lposKey = Constants.CREATE_TIME;
                break;

            case Constants.SYS_SORT_SHOP_GOODS_TYPE_UP://店铺 课程内容 上架
                functionName = Constants.SYS_SHOP_GOODS_TYPE_UP;
                redisKey = Constants.SYS_SORT_SHOP_GOODS_TYPE_UP_PAGING;
                lposKey = Constants.FIRST_UP_TIME;
                break;

            case Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN://店铺 课程内容 下架
                functionName = Constants.SYS_SHOP_GOODS_TYPE_COUSE_DOWN;
                redisKey = Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN_PAGING;
                lposKey = Constants.CREATE_TIME;
                break;

            case Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP://店铺 直播课 预告/正在直播 上架
                functionName = Constants.SYS_SHOP_LIVE_COUSE_PREDICTION_UP;
                redisKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP_PAGING;
                lposKey = Constants.LIVE_START_TIME;
                break;

            case Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN://店铺 直播 预告/正在直播 下架
                functionName = Constants.SYS_SHOP_LIVE_COUSE_PREDICTION_DOWN;
                redisKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN_PAGING;
                lposKey = Constants.LIVE_START_TIME;
                break;

            case Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP://店铺 直播课 结束 上架
                functionName = Constants.SYS_SHOP_LIVE_COUSE_FINISH_UP;
                redisKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP_PAGING;
                lposKey = Constants.LIVE_END_TIME;
                break;

            case Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN://店铺 直播 结束 下架
                functionName = Constants.SYS_SHOP_LIVE_COUSE_FINISH_DOWN;
                redisKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN_PAGING;
                lposKey = Constants.LIVE_END_TIME;
                break;
        }
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(Constants.CACHED_KEY_SHOP_FIELD, shop_id);
        String pageKey = MiscUtils.getKeyOfCachedData(page_key, queryMap);//页码key

        Long page = deleleteLastPageList(pageKey,jedis);
        List<Map<String, String>> courseList = null;
        do {
            queryMap.put("page", page * Constants.SORT_PANGE_NUMBER);
            queryMap.put("lposKey", lposKey);
            requestEntity.setFunctionName(functionName);
            requestEntity.setParam(queryMap);
            courseList = (List<Map<String, String>>) operation.invokeProcess(requestEntity);
            Map<String, Object> map = new HashMap<>();
            map.put(Constants.PAGING_NUMBER, page + 1);
            if (functionName.equals(Constants.SYS_SHOP_GOODS_TYPE_COUSE_DOWN) || functionName.equals(Constants.SYS_SHOP_GOODS_TYPE_UP)) {
                map.put("goods_type", goods_type);
            }
            map.put(Constants.CACHED_KEY_SHOP_FIELD, shop_id);
            String coursePageKey = MiscUtils.getKeyOfCachedData(redisKey, map);//页码key
            for (int i = 0; i <= courseList.size(); i++) {
                Map<String, String> course = courseList.get(i);
                long lpos = MiscUtils.convertInfoToPostion(Long.valueOf(course.get(lposKey)), Long.valueOf(course.get("position")));//算出位置
                if (i == 0) {
                    jedis.zadd(pageKey, lpos, coursePageKey);
                }
                jedis.zadd(coursePageKey, lpos, course.get("course_id"));
            }
            page += 1;
        } while (courseList.size() == 20);
    }


    /**
     * 更改课程缓存
     * 上架或下架
     * @param course
     * @param jedis
     * @throws Exception
     */
    public static void updateCourseListByRedis(Map<String, String> course, Jedis jedis, ReadCourseOperation operation, RequestEntity requestEntity) throws Exception {
        Long position = Long.valueOf(course.get("position"));
        Map<String, String> keysMap = new HashMap<>();
        if (course.get("goods_type").equals("0")) {//直播课
            String live_course_status = course.get("live_course_status");
            if (live_course_status.equals("1")) {
                keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP, course));
                keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN, course));
            }
            if (live_course_status.equals("2")) {
                keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP, course));
                keysMap.put(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN, course));
            }
        } else {//非直播课
            keysMap.put(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP, course));
            keysMap.put(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP, course));
            keysMap.put(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN, course));
            keysMap.put(Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN, course));
        }

        for (String key : keysMap.keySet()) {
            Long score_time = null;
            if (key.equals(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP) || key.equals(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP)) {//非直播课 和 课程类型 上架
                score_time = Long.valueOf(course.get(Constants.FIRST_UP_TIME));//第一次上架时间

            } else if (key.equals(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN) || key.equals(Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN)) {//非直播课 和 课程类型 下架
                score_time = Long.valueOf(course.get(Constants.CREATE_TIME));//创建时间

            } else if (key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP) || key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN)) { //直播课 正在直播和预告 上架 下架
                score_time = Long.valueOf(Constants.LIVE_START_TIME);//课程开始时间

            } else if (key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP) || key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN)) {//直播课 结束 上架 下架
                score_time = Long.valueOf(course.get(Constants.LIVE_END_TIME));//课程结束时间
            }
            long lpos = MiscUtils.convertInfoToPostion(score_time, position);//算出位置
            String redisKey = keysMap.get(key);
            TreeSet<String> keySet = (TreeSet<String>) jedis.zrangeByScore(redisKey, "(" + lpos, "+inf", 0, 1);//获取位置
            String firstKey = keySet.first();//拿到第一个值
            Long keyRank = jedis.zrank(redisKey, firstKey);//拿到这个key 排序
            if (keyRank != 0) {
                keyRank -= 1;
            }
            Long keyAmount = jedis.zcard(redisKey);

            for (long i = keyRank; i <= keyAmount; i++) {
                String constantsPagingKey = null;
                Map map = new HashMap<>();
                map.put(Constants.PAGING_NUMBER, i);//页码
                map.put(Constants.CACHED_KEY_SHOP_FIELD, course.get(Constants.CACHED_KEY_SHOP_FIELD));//shop_id

                if (key.equals(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP)) {//店铺 非直播 上架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_UP_PAGING;//店铺 非直播 上架 分页

                } else if (key.equals(Constants.SYS_SORT_SHOP_GOODS_TYPE_UP)) {//店铺 课程内容 上架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_GOODS_TYPE_UP_PAGING;//店铺 课程内容 上架 分页

                } else if (key.equals(Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN)) {//店铺 非直播 下架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_NON_LIVE_COUSE_DOWN_PAGING;//店铺 非直播 下架 分页

                } else if (key.equals(Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN)) {//店铺 课程内容 下架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_GOODS_TYPE_COUSE_DOWN_PAGING;//店铺 课程内容 下架 分页

                } else if (key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP)) {//店铺 直播课 预告/正在直播 上架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP_PAGING;//店铺 直播 预告/正在直播 上架 分页

                } else if (key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN)) {//店铺 直播 预告/正在直播 下架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_DOWN_PAGING;//店铺 直播 预告/正在直播 下架 分页

                } else if (key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP)) {//店铺 直播课 结束 上架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_UP_PAGING;//店铺 直播 结束 上架 分页

                } else if (key.equals(Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN)) {//店铺 直播 结束 下架
                    constantsPagingKey = Constants.SYS_SORT_SHOP_LIVE_COUSE_FINISH_DOWN_PAGING;//店铺 直播 结束 下架 分页
                }
                String pagingKey = MiscUtils.getKeyOfCachedData(constantsPagingKey, map);

                if (!MiscUtils.isEmpty(pagingKey)) {
                    jedis.zrem(redisKey, pagingKey);
                    jedis.del(pagingKey);
                }
            }
            refreshCourseListByRedis(course.get(Constants.CACHED_KEY_SHOP_FIELD), course.get("goods_type"), jedis, key, operation, requestEntity);
        }

    }

    /**
     * 更改系列
     * 上架或下架
     * @param series
     * @param jedis
     * @throws Exception
     */
    public static void updateSeriesListByRedis(Map<String, String> series, Map<String, String> course, Jedis jedis, CommonReadOperation operation, RequestEntity requestEntity) throws Exception {
        Long position = Long.valueOf(series.get("position"));
        Map<String, String> keysMap = new HashMap<>();
        if (course == null) {//系列上下架
            keysMap.put(Constants.SYS_SORT_SHOP_SERIES_UP, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_SERIES_UP, series));//店铺 系列 上架
            keysMap.put(Constants.SYS_SORT_SHOP_SERIES_DOWN, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_SERIES_DOWN, series));//店铺 系列 下架
        } else {//系列课程上下架
            keysMap.put(Constants.SYS_SORT_SERIES_UP, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SERIES_UP, series));//系列 课程 上架
            keysMap.put(Constants.SYS_SORT_SERIES_DOWN, MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SERIES_DOWN, series));//系列 课程 下架
        }
        for (String key : keysMap.keySet()) {
            Long score_time = null;
            String shop_id = "";
            String series_id = "";
            if (key.equals(Constants.SYS_SORT_SHOP_SERIES_UP) ) {
                score_time = Long.valueOf(series.get("update_course_time"));
                shop_id = series.get(Constants.CACHED_KEY_SHOP_FIELD);
                series_id = series.get(Constants.CACHED_KEY_SERIES_FIELD);

            } else if (key.equals(Constants.SYS_SORT_SERIES_UP) ) {
                score_time = Long.valueOf(course.get(Constants.FIRST_UP_TIME));
                shop_id = course.get(Constants.CACHED_KEY_SHOP_FIELD);
                series_id = course.get(Constants.CACHED_KEY_SERIES_FIELD);
            } else if ( key.equals(Constants.SYS_SORT_SERIES_DOWN)) {
                score_time = Long.valueOf(series.get(Constants.CREATE_TIME));
                shop_id = series.get(Constants.CACHED_KEY_SHOP_FIELD);
                series_id = series.get(Constants.CACHED_KEY_SERIES_FIELD);
            }else if ( key.equals(Constants.SYS_SORT_SHOP_SERIES_DOWN)) {
                score_time = Long.valueOf(course.get(Constants.CREATE_TIME));
                shop_id = course.get(Constants.CACHED_KEY_SHOP_FIELD);
                series_id = course.get(Constants.CACHED_KEY_SERIES_FIELD);
            }
            long lpos = MiscUtils.convertInfoToPostion(score_time, position);//算出位置
            String redisKey = keysMap.get(key);
            TreeSet<String> keySet = (TreeSet<String>) jedis.zrangeByScore(redisKey, "(" + lpos, "+inf", 0, 1);//获取位置
            String firstKey = keySet.first();//拿到第一个值
            Long keyRank = jedis.zrank(redisKey, firstKey);//拿到这个key 排序
            if (keyRank != 0) {
                keyRank -= 1;
            }
            Long keyAmount = jedis.zcard(redisKey);

            for (long i = keyRank; i <= keyAmount; i++) {
                String constantsPagingKey = null;
                Map map = new HashMap<>();
                map.put(Constants.PAGING_NUMBER, i);//页码
                map.put(Constants.CACHED_KEY_SERIES_FIELD,series_id);
                map.put(Constants.CACHED_KEY_SHOP_FIELD,shop_id);

                if (key.equals(Constants.SYS_SORT_SHOP_SERIES_UP)) {
                    constantsPagingKey = Constants.SYS_SORT_SHOP_SERIES_UP_PAGING;

                } else if (key.equals(Constants.SYS_SORT_SERIES_UP)) {
                    constantsPagingKey = Constants.SYS_SORT_SERIES_UP_PAGING;

                }else if (key.equals(Constants.SYS_SORT_SHOP_SERIES_DOWN)) {
                    constantsPagingKey = Constants.SYS_SORT_SHOP_SERIES_DOWN_PAGING;

                }else if (key.equals(Constants.SYS_SORT_SERIES_DOWN)) {
                    constantsPagingKey = Constants.SYS_SORT_SERIES_DOWN_PAGING;
                }

                String pagingKey = MiscUtils.getKeyOfCachedData(constantsPagingKey, map);
                if (!MiscUtils.isEmpty(pagingKey)) {
                    jedis.zrem(redisKey, pagingKey);
                    jedis.del(pagingKey);
                }
            }
            refreshSeriesListByRedis(shop_id,series_id,jedis,key,operation,requestEntity);
        }
    }


    public static List<String> getList(String redisKey, int pageCount, String lpos, Jedis jedis){
        String start_index = "+inf";
        String end_index = "-inf";
        if(lpos != null){
            start_index = "("+lpos;
        }
        Set<String> goodsIds = jedis.zrevrangeByScore(redisKey, start_index, end_index, 0, pageCount);
        return new ArrayList<>(goodsIds);
    }


    public static List<String> getLiveCourseList(Map<String, String> course,String lecture_id,int pageCount,Jedis jedis){

        int offset = 0;//偏移值
        Set<String> courseIdSet;//查询的课程idset
        List<String> courseIdList = new ArrayList<>();//课程id列表
        Integer live_course_status = 4;
        if(!MiscUtils.isEmpty(course)){
            live_course_status = Integer.valueOf(course.get("live_course_status"));
        }

        //<editor-fold desc="正在直播">
        if(live_course_status == 4 ){//如果预告或者是正在直播的课程
            String startIndex ;//坐标起始位
            String endIndex ;//坐标结束位
            String getCourseIdKey;
            //平台的预告中课程列表 预告和正在直播放在一起  按照直播开始时间顺序排序  根据分类获取不同的缓存
            Map<String,Object> map = new HashMap<String,Object>();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
            getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);//讲师

            if(MiscUtils.isEmpty(course)){//如果没有传入courceid 那么就是最开始的查询  进行倒叙查询 查询现在的
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(System.currentTimeMillis(),0L);
                startIndex = "-inf";//设置起始位置 '(' 是要求大于这个参数
                endIndex =courseScoreByRedis+"";//设置结束位置
            }else{//传了courseid
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("live_start_time")),  MiscUtils.convertObjectToLong(course.get("position")));//拿到当前课程在redis中的score
                startIndex = "("+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
                endIndex =MiscUtils.convertInfoToPostion(System.currentTimeMillis(),0L)+"";//设置结束位置
            }
            courseIdSet = jedis.zrangeByScore(getCourseIdKey,startIndex,endIndex,offset,pageCount); //顺序找出couseid  (正在直播或者预告的)
            for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                courseIdList.add(course_id);
            }

            pageCount -= courseIdList.size();//用展示数量减去获取的数量  查看是否获取到了足够的课程数
            if( pageCount > 0){//如果返回的值不够
                course = null;//把课程id设置为null  用来在下面的代码中进行判断
                live_course_status = 1;//设置查询课程状态 为结束课程 因为查找出来的正在直播和预告的课程不够数量
            }
        }
        //</editor-fold>

        //<editor-fold desc="预告">
        if(live_course_status == 1 ){//如果预告或者是正在直播的课程
            String startIndex ;//坐标起始位
            String endIndex ;//坐标结束位
            String getCourseIdKey;

            //平台的预告中课程列表 预告和正在直播放在一起  按照直播开始时间顺序排序  根据分类获取不同的缓存
            Map<String,Object> map = new HashMap<String,Object>();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
            getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_PREDICTION, map);//讲师

            if(MiscUtils.isEmpty(course)){//如果没有传入courceid 那么就是最开始的查询  进行倒叙查询 查询现在的
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(System.currentTimeMillis(),0L);
                startIndex = ""+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
                endIndex ="+inf";//设置结束位置
            }else{//传了courseid
                long courseScoreByRedis = MiscUtils.convertInfoToPostion(MiscUtils.convertObjectToLong(course.get("live_start_time")),  MiscUtils.convertObjectToLong(course.get("position")));//拿到当前课程在redis中的score
                startIndex = "("+courseScoreByRedis;//设置起始位置 '(' 是要求大于这个参数
                endIndex ="+inf";//设置结束位置
            }
            courseIdSet = jedis.zrangeByScore(getCourseIdKey,startIndex,endIndex,offset,pageCount); //顺序找出couseid  (正在直播或者预告的)
            for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                courseIdList.add(course_id);
            }

            pageCount -= courseIdList.size();//用展示数量减去获取的数量  查看是否获取到了足够的课程数
            if( pageCount > 0){//如果返回的值不够
                course = null;//把课程id设置为null  用来在下面的代码中进行判断
                live_course_status = 2;//设置查询课程状态 为结束课程 因为查找出来的正在直播和预告的课程不够数量
            }
        }
        //</editor-fold>

        //<editor-fold desc="结束">
        if(live_course_status == 2 ){//查询结束课程
            boolean key = true;//作为开关 用于下面是否需要接着执行方法
            long startIndex = 0; //开始下标
            long endIndex = -1;   //结束下标
            String getCourseIdKey ;
            Map<String,Object> map = new HashMap<String,Object>();
            map.put(Constants.CACHED_KEY_LECTURER_FIELD, lecture_id);
            getCourseIdKey =  MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_COURSE_FINISH, map);//讲师

            long endCourseSum = jedis.zcard(getCourseIdKey);//获取总共有多少个结束课程
            if(MiscUtils.isEmpty(course)){//如果课程ID没有 那么就从最近结束的课程找起
                endIndex = -1;
                startIndex = endCourseSum - pageCount;//利用总数减去我这边需要获取的数
                if(startIndex < 0){
                    startIndex = 0;
                }
            }else{ //如果有课程id  先获取课程id 在列表中的位置 然后进行获取其他课程id
                long endRank = jedis.zrank(getCourseIdKey, course.get("course_id"));
                endIndex = endRank - 1;
                if(endIndex >= 0){
                    startIndex = endIndex - pageCount + 1;
                    if(startIndex < 0){
                        startIndex = 0;
                    }
                }else{
                    key = false;//因为已经查到最后的课程没有必要往下查了
                }
                if(key){
                    courseIdSet = jedis.zrange(getCourseIdKey, startIndex, endIndex);
                    List<String> transfer = new ArrayList<>();
                    for(String course_id : courseIdSet){//遍历已经查询到的课程在把课程列表加入到课程idlist中
                        transfer.add(course_id);
                    }
                    Collections.reverse(transfer);
                    courseIdList.addAll(transfer);
                }
            }
        }
        //</editor-fold>

        return courseIdList;
    }




    /**
     * 刷新系列缓存
     *
     * @param shop_id
     * @param jedis
     * @param operation
     * @param requestEntity
     * @return
     * @throws Exception
     */
    public static void refreshSeriesListByRedis(String shop_id,String series_id,Jedis jedis, String page_key, CommonReadOperation operation, RequestEntity requestEntity) throws Exception {
        String redisKey = "";
        String functionName = "";
        String lposKey = "";
        String valueKey = "";
        switch (page_key) {
            case Constants.SYS_SORT_SHOP_SERIES_UP:
                functionName = Constants.SYS_SHOP_SERIES_UP;
                redisKey = Constants.SYS_SORT_SHOP_SERIES_UP_PAGING;
                lposKey = Constants.FIRST_UP_TIME;
                valueKey = Constants.CACHED_KEY_SERIES_FIELD;
                break;

            case Constants.SYS_SORT_SERIES_UP:
                functionName = Constants.SYS_SERIES_COURSE_UP;
                redisKey = Constants.SYS_SORT_SERIES_UP_PAGING;
                lposKey = Constants.CREATE_TIME;
                valueKey = Constants.CACHED_KEY_COURSE_FIELD;
                break;

            case Constants.SYS_SORT_SHOP_SERIES_DOWN:
                functionName = Constants.SYS_SHOP_SERIES_DOWN;
                redisKey = Constants.SYS_SORT_SHOP_SERIES_DOWN_PAGING;
                lposKey = Constants.CREATE_TIME;
                valueKey = Constants.CACHED_KEY_SERIES_FIELD;
                break;

            case Constants.SYS_SORT_SERIES_DOWN:
                functionName = Constants.SYS_SERIES_COURSE_DOWN;
                redisKey = Constants.SYS_SORT_SERIES_DOWN_PAGING;
                lposKey = Constants.CREATE_TIME;
                valueKey = Constants.CACHED_KEY_COURSE_FIELD;
                break;
        }


        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(Constants.CACHED_KEY_SHOP_FIELD, shop_id);
        if(series_id != null){
            queryMap.put(Constants.CACHED_KEY_SERIES_FIELD, series_id);
        }
        String pageKey = MiscUtils.getKeyOfCachedData(page_key, queryMap);//页码key
        Long page = deleleteLastPageList(pageKey,jedis);
        List<Map<String, String>> goodsList = null;
        do {
            queryMap.put("page", page * Constants.SORT_PANGE_NUMBER);
            queryMap.put("lposKey", lposKey);
            requestEntity.setFunctionName(functionName);
            requestEntity.setParam(queryMap);
            goodsList = (List<Map<String, String>>) operation.invokeProcess(requestEntity);
            Map<String, Object> map = new HashMap<>();
            map.put(Constants.PAGING_NUMBER, page + 1);
            map.put(Constants.CACHED_KEY_SHOP_FIELD, shop_id);
            if(series_id != null){
                map.put(Constants.CACHED_KEY_SERIES_FIELD, series_id);
            }
            String coursePageKey = MiscUtils.getKeyOfCachedData(redisKey, map);//页码key
            for (int i = 0; i <= goodsList.size(); i++) {
                Map<String, String> goods = goodsList.get(i);
                long lpos = MiscUtils.convertInfoToPostion(Long.valueOf(goods.get(lposKey)), Long.valueOf(goods.get("position")));//算出位置
                if (i == 0) {
                    jedis.zadd(pageKey, lpos, coursePageKey);
                }
                jedis.zadd(coursePageKey, lpos, goods.get(valueKey));
            }
            page += 1;
        } while (goodsList != null && goodsList.size() == 20);



    }


    private static Long deleleteLastPageList(String pageKey,Jedis jedis){
        Long page = jedis.zcard(pageKey);
        if (!MiscUtils.isEmpty(page) && page != 0) {
            Set<String> coursePageKeys = jedis.zrevrangeByScore(pageKey, "+inf", "-inf", 0, 1);
            String coursePageKey = "";
            for (String coursePage : coursePageKeys) {
                coursePageKey = coursePage;
            }
            //判断最后一个key 是否有20条 如果没有 就删掉重新加载
            Long coursePageCount = jedis.zcard(coursePageKey);
            if (coursePageCount < 20) {
                jedis.del(coursePageKey);
                jedis.zrem(pageKey, coursePageKey);
                page -= 1;
            }
        } else {
            page = 0L;
        }
        return page;
    }


    /**无分页缓存
     * 更改系列
     * 上架或下架
     * @param series
     * @param jedis
     * @throws Exception
     */
    public static void updateSeriesListByRedis(Map<String, String> series, Jedis jedis, ReadSeriesOperation operation, RequestEntity requestEntity) throws Exception {
        String lecturerSeriesUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_UP, series);//讲师所有上架系列
        String lecturerSeriesDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_DOWN, series);//讲师所有下架系列
        String seriesCourseTypeUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_UP, series);//讲师在不同的课程内容下架的系列
        String seriesCourseTypeDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_DOWN, series);//讲师在不同的课程内容上架的系列
        String seriesNonLiveUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_NON_LIVE_UP, series);//讲师在不同的课程内容下架的系列
        String seriesNonLiveDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_NON_LIVE_DOWN, series);//讲师在不同的课程内容上架的系列

        jedis.del(lecturerSeriesUpKey);
        jedis.del(lecturerSeriesDownKey);
        jedis.del(seriesCourseTypeUpKey);
        jedis.del(seriesCourseTypeDownKey);
        jedis.del(seriesNonLiveUpKey);
        jedis.del(seriesNonLiveDownKey);
        refreshSeriesListByRedis(series.get(Constants.CACHED_KEY_LECTURER_FIELD),jedis,operation,requestEntity);

    }


    /**无分页
     * 刷新系列缓存
     * @param jedis
     * @param operation
     * @param requestEntity
     * @return
     * @throws Exception
     */
    public static void refreshSeriesListByRedis(String lecturer_id,Jedis jedis,ReadSeriesOperation operation, RequestEntity requestEntity) throws Exception{
        Map<String,Object> queryMap = new HashMap<>();
        queryMap.put(Constants.CACHED_KEY_LECTURER_FIELD,lecturer_id);
        requestEntity.setParam(queryMap);
        requestEntity.setFunctionName(Constants.SYS_SHOP_SERIES_ALL);
        List<Map<String, String>> seriesList = (List<Map<String, String>>) operation.invokeProcess(requestEntity);
        for(Map<String, String> seriesMap : seriesList){
            String seriesId = seriesMap.get(Constants.CACHED_KEY_SERIES_FIELD);
            long lpos = 0L;
           List<String> keyList = new ArrayList<>();
            if(seriesMap.get("updown").equals("1")){
                String lecturerSeriesUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_UP, seriesMap);//讲师所有上架系列
                String seriesCourseTypeUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_UP, seriesMap);//讲师在不同的课程内容下架的系列
                String seriesNonLiveUpKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_NON_LIVE_UP, seriesMap);//讲师 不是直播系列 上架
                lpos = MiscUtils.convertInfoToPostion(Long.valueOf(seriesMap.get("update_course_time")), Long.valueOf(seriesMap.get("position")));//算出位置
                keyList.add(lecturerSeriesUpKey);
                keyList.add(seriesCourseTypeUpKey);
                keyList.add(seriesNonLiveUpKey);
            }else{
                String lecturerSeriesDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_DOWN, seriesMap);//讲师所有下架系列
                String seriesCourseTypeDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_COURSE_DOWN, seriesMap);//讲师在不同的课程内容上架的系列
                String seriesNonLiveDownKey = MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_SERIES_NON_LIVE_DOWN, seriesMap);//讲师 不是直播系列 下架
                lpos = MiscUtils.convertInfoToPostion(Long.valueOf(seriesMap.get("create_time")), Long.valueOf(seriesMap.get("position")));//算出位置
                keyList.add(lecturerSeriesDownKey);
                keyList.add(seriesCourseTypeDownKey);
                keyList.add(seriesNonLiveDownKey);
            }
            for(String key : keyList){
                jedis.zadd(key,lpos,seriesId);
            }
        }
    }



    /**
     * 上课时间 课程之间需要间隔三十分钟
     *
     * @throws Exception
     */
    public static void whetherCreateLiveCourse(String shop_id, Long live_start_time, Jedis jedis) throws Exception {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put(Constants.CACHED_KEY_SHOP_FIELD, shop_id);
        long lpos = MiscUtils.convertInfoToPostion(live_start_time, 0L);//算出位置
        String catalogKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP, queryMap);
        TreeSet<String> keySet = (TreeSet<String>) jedis.zrangeByScore(catalogKey, "(" + lpos, "+inf", 0, 1);//获取位置
        String firstKey = keySet.first();//拿到第一个值
        Long keyRank = jedis.zrank(catalogKey, firstKey);//拿到这个key 的排序
        queryMap.put(Constants.PAGING_NUMBER, keyRank - 1);
        String pageKey = MiscUtils.getKeyOfCachedData(Constants.SYS_SORT_SHOP_LIVE_COUSE_PREDICTION_UP_PAGING, queryMap);
        long startIndex = live_start_time - 30 * 60 * 1000;
        long endIndex = live_start_time + 30 * 60 * 1000;
        long start = MiscUtils.convertInfoToPostion(startIndex, 0L);
        long end = MiscUtils.convertInfoToPostion(endIndex, 0L);
        Set<String> aLong = jedis.zrangeByScore(pageKey, start, end);
        if (!MiscUtils.isEmpty(aLong)) {
            throw new QNLiveException("100029");
        }
    }


}