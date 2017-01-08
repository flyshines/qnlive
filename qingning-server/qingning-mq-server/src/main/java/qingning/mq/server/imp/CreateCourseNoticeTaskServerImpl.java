package qingning.mq.server.imp;

import org.springframework.context.ApplicationContext;
import qingning.common.entity.RequestEntity;
import qingning.common.util.Constants;
import qingning.common.util.JedisUtils;
import qingning.common.util.MiscUtils;
import qingning.mq.persistence.mybatis.CoursesMapper;
import qingning.server.AbstractMsgService;

import java.util.List;
import java.util.Map;

/**
 * Created by loovee on 2016/12/16.
 */
public class CreateCourseNoticeTaskServerImpl extends AbstractMsgService {

    private CoursesMapper coursesMapper;

    @Override
    public void process(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) throws Exception {
        processCreateCourseNoticeLongTask(requestEntity, jedisUtils, context);
        processCreateCourseNoticeShortTask(requestEntity, jedisUtils, context);
    }


    private void processCreateCourseNoticeLongTask(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {

        //查找出后天开播的课程，生成提前24小时开课提醒定时任务
        List<Map<String,Object>> courseStartLongNoticeList = coursesMapper.findCourseStartLongNoticeList();
        if(! MiscUtils.isEmpty(courseStartLongNoticeList)){
            MessagePushServerImpl messagePushServerImpl = (MessagePushServerImpl)context.getBean("MessagePushServer");
            for(Map<String,Object> map : courseStartLongNoticeList){
                RequestEntity requestEntityTask = new RequestEntity();
                requestEntityTask.setServerName("MessagePushServer");
                requestEntityTask.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                requestEntityTask.setFunctionName("processCourseStartLongNotice");
                requestEntityTask.setParam(map);
                messagePushServerImpl.processCourseStartLongNotice(requestEntityTask, jedisUtils, context);
            }
        }
    }

    private void processCreateCourseNoticeShortTask(RequestEntity requestEntity, JedisUtils jedisUtils, ApplicationContext context) {

        //查找出明天开播的课程，生成提前5分钟开课提醒定时任务和开课时间到了未在直播间通知
        List<Map<String,Object>> courseStartShortNoticeList = coursesMapper.findCourseStartShortNoticeList();
        if(! MiscUtils.isEmpty(courseStartShortNoticeList)){
            MessagePushServerImpl messagePushServerImpl = (MessagePushServerImpl)context.getBean("MessagePushServer");
            for(Map<String,Object> map : courseStartShortNoticeList){
                RequestEntity requestEntityTask = new RequestEntity();
                requestEntityTask.setServerName("MessagePushServer");
                requestEntityTask.setMethod(Constants.MQ_METHOD_ASYNCHRONIZED);
                requestEntityTask.setFunctionName("processCourseStartShortNotice");
                requestEntityTask.setParam(map);

                //提前五分钟开课提醒
                messagePushServerImpl.processCourseStartShortNotice(requestEntityTask, jedisUtils, context);

                //开课时间到但是讲师未出现提醒
                requestEntityTask.setFunctionName("processCourseStartLecturerNotShow");
                messagePushServerImpl.processCourseStartLecturerNotShow(requestEntityTask, jedisUtils, context);

                //提醒学生参加课程定时任务
                requestEntityTask.setFunctionName("processCourseStartStudentStudyNotice");
                messagePushServerImpl.processCourseStartStudentStudyNotice(requestEntityTask, jedisUtils, context);
            }
        }
    }

    public CoursesMapper getCoursesMapper() {
        return coursesMapper;
    }

    public void setCoursesMapper(CoursesMapper coursesMapper) {
        this.coursesMapper = coursesMapper;
    }
}
