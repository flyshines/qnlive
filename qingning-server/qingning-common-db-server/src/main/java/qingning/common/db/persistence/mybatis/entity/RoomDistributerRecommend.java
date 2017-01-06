package qingning.common.db.persistence.mybatis.entity;

import java.util.Date;

public class RoomDistributerRecommend {
    private String distributerRecommendId;

    private String distributerId;

    private String roomId;

    private String userId;

    private Long recommendNum;

    private Long doneNum;

    private Long courseNum;

    private Long position;

    private Date createTime;

    private Date updateTime;

    private Date endDate;

    private String rqCode;

    public String getDistributerRecommendId() {
        return distributerRecommendId;
    }

    public void setDistributerRecommendId(String distributerRecommendId) {
        this.distributerRecommendId = distributerRecommendId == null ? null : distributerRecommendId.trim();
    }

    public String getDistributerId() {
        return distributerId;
    }

    public void setDistributerId(String distributerId) {
        this.distributerId = distributerId == null ? null : distributerId.trim();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId == null ? null : roomId.trim();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public Long getRecommendNum() {
        return recommendNum;
    }

    public void setRecommendNum(Long recommendNum) {
        this.recommendNum = recommendNum;
    }

    public Long getDoneNum() {
        return doneNum;
    }

    public void setDoneNum(Long doneNum) {
        this.doneNum = doneNum;
    }

    public Long getCourseNum() {
        return courseNum;
    }

    public void setCourseNum(Long courseNum) {
        this.courseNum = courseNum;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
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
}