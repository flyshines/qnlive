package qingning.mq.persistence.entity;

import java.util.Date;

public class LiveRoom {
    private String roomId;

    private String lecturerId;

    private Long courseNum;

    private Long fansNum;

    private Long distributerNum;

    private String roomName;

    private String avatarAddress;

    private String roomRemark;

    private String rqCode;

    private String roomAddress;

    private Long totalAmount;

    private Long lastCourseAmount;

    private Date createTime;

    private Date updateTime;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId == null ? null : roomId.trim();
    }

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

    public Long getFansNum() {
        return fansNum;
    }

    public void setFansNum(Long fansNum) {
        this.fansNum = fansNum;
    }

    public Long getDistributerNum() {
        return distributerNum;
    }

    public void setDistributerNum(Long distributerNum) {
        this.distributerNum = distributerNum;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName == null ? null : roomName.trim();
    }

    public String getAvatarAddress() {
        return avatarAddress;
    }

    public void setAvatarAddress(String avatarAddress) {
        this.avatarAddress = avatarAddress == null ? null : avatarAddress.trim();
    }

    public String getRoomRemark() {
        return roomRemark;
    }

    public void setRoomRemark(String roomRemark) {
        this.roomRemark = roomRemark == null ? null : roomRemark.trim();
    }

    public String getRqCode() {
        return rqCode;
    }

    public void setRqCode(String rqCode) {
        this.rqCode = rqCode == null ? null : rqCode.trim();
    }

    public String getRoomAddress() {
        return roomAddress;
    }

    public void setRoomAddress(String roomAddress) {
        this.roomAddress = roomAddress == null ? null : roomAddress.trim();
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getLastCourseAmount() {
        return lastCourseAmount;
    }

    public void setLastCourseAmount(Long lastCourseAmount) {
        this.lastCourseAmount = lastCourseAmount;
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