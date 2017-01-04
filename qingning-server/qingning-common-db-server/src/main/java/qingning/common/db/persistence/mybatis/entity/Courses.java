package qingning.common.db.persistence.mybatis.entity;

import java.util.Date;

public class Courses {
    private String courseId;

    private String roomId;

    private String imCourseId;

    private String lecturerId;

    private String courseTitle;

    private String courseUrl;

    private String courseRemark;

    private Date startTime;

    private Date endTime;

    private String courseType;

    private String status;

    private String rqCode;

    private Long coursePrice;

    private String coursePassword;

    private Long studentNum;

    private Long courseAmount;

    private Long extraNum;

    private Long extraAmount;

    private Date createTime;

    private Date createDate;

    private Date updateTime;

    private Date realStartTime;

    private Long realStudentNum;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId == null ? null : courseId.trim();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId == null ? null : roomId.trim();
    }

    public String getImCourseId() {
        return imCourseId;
    }

    public void setImCourseId(String imCourseId) {
        this.imCourseId = imCourseId == null ? null : imCourseId.trim();
    }

    public String getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId == null ? null : lecturerId.trim();
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle == null ? null : courseTitle.trim();
    }

    public String getCourseUrl() {
        return courseUrl;
    }

    public void setCourseUrl(String courseUrl) {
        this.courseUrl = courseUrl == null ? null : courseUrl.trim();
    }

    public String getCourseRemark() {
        return courseRemark;
    }

    public void setCourseRemark(String courseRemark) {
        this.courseRemark = courseRemark == null ? null : courseRemark.trim();
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType == null ? null : courseType.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public String getRqCode() {
        return rqCode;
    }

    public void setRqCode(String rqCode) {
        this.rqCode = rqCode == null ? null : rqCode.trim();
    }

    public Long getCoursePrice() {
        return coursePrice;
    }

    public void setCoursePrice(Long coursePrice) {
        this.coursePrice = coursePrice;
    }

    public String getCoursePassword() {
        return coursePassword;
    }

    public void setCoursePassword(String coursePassword) {
        this.coursePassword = coursePassword == null ? null : coursePassword.trim();
    }

    public Long getStudentNum() {
        return studentNum;
    }

    public void setStudentNum(Long studentNum) {
        this.studentNum = studentNum;
    }

    public Long getCourseAmount() {
        return courseAmount;
    }

    public void setCourseAmount(Long courseAmount) {
        this.courseAmount = courseAmount;
    }

    public Long getExtraNum() {
        return extraNum;
    }

    public void setExtraNum(Long extraNum) {
        this.extraNum = extraNum;
    }

    public Long getExtraAmount() {
        return extraAmount;
    }

    public void setExtraAmount(Long extraAmount) {
        this.extraAmount = extraAmount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getRealStartTime() {
        return realStartTime;
    }

    public void setRealStartTime(Date realStartTime) {
        this.realStartTime = realStartTime;
    }

    public Long getRealStudentNum() {
        return realStudentNum;
    }

    public void setRealStudentNum(Long realStudentNum) {
        this.realStudentNum = realStudentNum;
    }
}