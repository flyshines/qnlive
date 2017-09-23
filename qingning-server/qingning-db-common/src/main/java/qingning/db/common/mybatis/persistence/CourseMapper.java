package qingning.db.common.mybatis.persistence;

import java.util.List;
import java.util.Map;

public interface CourseMapper {
    /**
     * 根据各种参数 获取课程list
     * @param record
     *  course_id = 课程id
     *  shop_id = 店铺id
     *  classify_id = 分类id
     *  lecturer_id = 讲师id
     *  series_id = 系列id
     *  course_title LIKE 课程标题
     *  course_type = 课程收费类型 0:公开课程 1:加密课程 2:收费课程
     *  goods_type = 课程内容类型 0直播 1语音 2视频 3图文
     *  series_course_updown = 系列课程上下架 系列上下架 1上架  2下架 针对系列里面这一个课程
     *  course_updown = 课程上下架 1:已上架，2:已下架
     *  shelves_sharing = 有没有上架知享 0是没有上架知享 1上架知享
     *
     * @return
     */
    List<Map<String,Object>> findCourse(Map<String, Object> record);

    /**
     * 根据课程id 查找课程信息
     * @param course_id 课程id
     * @return
     */
    Map<String,Object> findCourseByCourseId(String course_id);

    /**
     * 创建课程
     * @param record
     *  course_id = 课程id
     *  shop_id = 店铺id
     *  classify_id = 分类id
     *  lecturer_id = 讲师id
     *  series_id = 系列id
     *  course_title = 课程标题
     *  course_image = 课程封面
     *  course_remark = 课程简介
     *  course_abstract = 摘要
     *  buy_tips = 购买须知
     *  target_user = 适宜人群
     *  share_url = 分享连接
     *  course_url = 音视频url 课程内容
     *  live_start_time = 直播课程开始时间
     *  course_duration = 课程时长（秒）
     *  live_course_status = 直播课程状态 0:草稿 1：已发布 2:已结束 3:已撤销 4直播中（4状态不会出现在数据库中，仅出现在缓存中）
     *  course_type = 课程收费类型 0:公开课程 1:加密课程 2:收费课程
     *  goods_type = 课程内容类型 0直播 1语音 2视频 3图文
     *  rq_code = 课程二维码
     *  course_password = 课程密码
     *  course_price = 课程价格(分)
     *  series_course_updown = 系列课程上下架 系列上下架 1上架  2下架 针对系列里面这一个课程
     *  course_updown = 课程上下架 1:已上架，2:已下架
     *  updown_time = 上下架时间
     *  shelves_sharing = 有没有上架知享 0是没有上架知享 1上架知享
     *  create_time = 创建时间
     *  update_time = 更新时间
     *  details = 课程详情
     * @return
     */
    int insertCourse(Map<String, Object> record);

    /**
     * 更改课程
     * @param record
     *  classify_id = 分类id
     *  series_id = 系列id
     *  course_title = 课程标题
     *  course_image = 课程封面
     *  course_remark = 课程简介
     *  course_abstract = 摘要
     *  buy_tips = 购买须知
     *  target_user = 适宜人群
     *  share_url = 分享连接
     *  course_url = 音视频url 课程内容
     *  live_start_time = 直播课程开始时间
     *  live_end_time = 直播课程 结束时间
     *  live_course_status = 直播课程状态 0:草稿 1：已发布 2:已结束 3:已撤销 4直播中（4状态不会出现在数据库中，仅出现在缓存中）
     *  course_duration = 课程时长（秒）
     *  course_type = 课程收费类型 0:公开课程 1:加密课程 2:收费课程
     *  goods_type = 课程内容类型 0直播 1语音 2视频 3图文
     *  rq_code = 课程二维码
     *  course_password = 课程密码
     *  course_price = 课程价格(分)
     *  series_course_updown = 系列课程上下架 系列上下架 1上架  2下架 针对系列里面这一个课程
     *  course_updown = 课程上下架 1:已上架，2:已下架
     *  updown_time = 上下架时间
     *  shelves_sharing = 有没有上架知享 0是没有上架知享 1上架知享
     *  create_time = 创建时间
     *  update_time = 更新时间
     *  details = 课程详情
     * @return
     */
    int updateCourse(Map<String, Object> record);




}