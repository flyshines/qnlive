package qingning.mq.persistence.entity;

import java.util.Date;

public class Lecturer {
    private String lecturerId;

    private Long courseNum;

    private Long totalStudentNum;

    private Long payStudentNum;

    private Long totalTime;

    private Long liveRoomNum;

    private Long fansNum;

    private Long payCourseNum;

    private Long privateCourseNum;

    private Long totalAmount;

    private Date createTime;

    private Date updateTime;

    public String getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId == null ? null : lecturerId.trim();
    }

    public Long getCourseNum() {
        return courseNum;
    }

    public void setCourseNum(Long courseNum) {
        this.courseNum = courseNum;
    }

    public Long getTotalStudentNum() {
        return totalStudentNum;
    }

    public void setTotalStudentNum(Long totalStudentNum) {
        this.totalStudentNum = totalStudentNum;
    }

    public Long getPayStudentNum() {
        return payStudentNum;
    }

    public void setPayStudentNum(Long payStudentNum) {
        this.payStudentNum = payStudentNum;
    }

    public Long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Long totalTime) {
        this.totalTime = totalTime;
    }

    public Long getLiveRoomNum() {
        return liveRoomNum;
    }

    public void setLiveRoomNum(Long liveRoomNum) {
        this.liveRoomNum = liveRoomNum;
    }

    public Long getFansNum() {
        return fansNum;
    }

    public void setFansNum(Long fansNum) {
        this.fansNum = fansNum;
    }

    public Long getPayCourseNum() {
        return payCourseNum;
    }

    public void setPayCourseNum(Long payCourseNum) {
        this.payCourseNum = payCourseNum;
    }

    public Long getPrivateCourseNum() {
        return privateCourseNum;
    }

    public void setPrivateCourseNum(Long privateCourseNum) {
        this.privateCourseNum = privateCourseNum;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}