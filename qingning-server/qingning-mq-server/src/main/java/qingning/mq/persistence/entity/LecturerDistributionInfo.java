package qingning.mq.persistence.entity;

import java.util.Date;

public class LecturerDistributionInfo {
    private String lecturerId;

    private Long liveRoomNum;

    private Long roomDistributerNum;

    private Long roomRecommendNum;

    private Long roomDoneNum;

    private Long courseDistributionNum;

    private Long courseDistributerNum;

    private Long courseRecommendNum;

    private Long courseDoneNum;

    private Date createTime;

    private Date updateTime;

    public String getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId == null ? null : lecturerId.trim();
    }

    public Long getLiveRoomNum() {
        return liveRoomNum;
    }

    public void setLiveRoomNum(Long liveRoomNum) {
        this.liveRoomNum = liveRoomNum;
    }

    public Long getRoomDistributerNum() {
        return roomDistributerNum;
    }

    public void setRoomDistributerNum(Long roomDistributerNum) {
        this.roomDistributerNum = roomDistributerNum;
    }

    public Long getRoomRecommendNum() {
        return roomRecommendNum;
    }

    public void setRoomRecommendNum(Long roomRecommendNum) {
        this.roomRecommendNum = roomRecommendNum;
    }

    public Long getRoomDoneNum() {
        return roomDoneNum;
    }

    public void setRoomDoneNum(Long roomDoneNum) {
        this.roomDoneNum = roomDoneNum;
    }

    public Long getCourseDistributionNum() {
        return courseDistributionNum;
    }

    public void setCourseDistributionNum(Long courseDistributionNum) {
        this.courseDistributionNum = courseDistributionNum;
    }

    public Long getCourseDistributerNum() {
        return courseDistributerNum;
    }

    public void setCourseDistributerNum(Long courseDistributerNum) {
        this.courseDistributerNum = courseDistributerNum;
    }

    public Long getCourseRecommendNum() {
        return courseRecommendNum;
    }

    public void setCourseRecommendNum(Long courseRecommendNum) {
        this.courseRecommendNum = courseRecommendNum;
    }

    public Long getCourseDoneNum() {
        return courseDoneNum;
    }

    public void setCourseDoneNum(Long courseDoneNum) {
        this.courseDoneNum = courseDoneNum;
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