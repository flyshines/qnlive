package qingning.mq.persistence.entity;

import java.util.Date;

public class RoomDistributer {
    private String roomDistributerId;

    private String distributerId;

    private String lecturerId;

    private String roomId;

    private Long recommendNum;

    private Long courseNum;

    private Long doneNum;

    private Long profitShareRate;

    private Long totalAmount;

    private String effectiveTime;

    private Date endDate;

    private String rqCode;

    private Date createTime;

    private Date createDate;

    private Date updateTime;

    private Date doneTime;

    public String getRoomDistributerId() {
        return roomDistributerId;
    }

    public void setRoomDistributerId(String roomDistributerId) {
        this.roomDistributerId = roomDistributerId == null ? null : roomDistributerId.trim();
    }

    public String getDistributerId() {
        return distributerId;
    }

    public void setDistributerId(String distributerId) {
        this.distributerId = distributerId == null ? null : distributerId.trim();
    }

    public String getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId == null ? null : lecturerId.trim();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId == null ? null : roomId.trim();
    }

    public Long getRecommendNum() {
        return recommendNum;
    }

    public void setRecommendNum(Long recommendNum) {
        this.recommendNum = recommendNum;
    }

    public Long getCourseNum() {
        return courseNum;
    }

    public void setCourseNum(Long courseNum) {
        this.courseNum = courseNum;
    }

    public Long getDoneNum() {
        return doneNum;
    }

    public void setDoneNum(Long doneNum) {
        this.doneNum = doneNum;
    }

    public Long getProfitShareRate() {
        return profitShareRate;
    }

    public void setProfitShareRate(Long profitShareRate) {
        this.profitShareRate = profitShareRate;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(String effectiveTime) {
        this.effectiveTime = effectiveTime == null ? null : effectiveTime.trim();
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getRqCode() {
        return rqCode;
    }

    public void setRqCode(String rqCode) {
        this.rqCode = rqCode == null ? null : rqCode.trim();
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

    public Date getDoneTime() {
        return doneTime;
    }

    public void setDoneTime(Date doneTime) {
        this.doneTime = doneTime;
    }
}