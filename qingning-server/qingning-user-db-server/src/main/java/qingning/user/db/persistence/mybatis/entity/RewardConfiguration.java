package qingning.user.db.persistence.mybatis.entity;

import java.util.Date;

public class RewardConfiguration {
    private Long rewardId;

    private String description;

    private Double amount;

    private Long rewardPos;

    private String status;

    private Date updateTime;

    public Long getRewardId() {
        return rewardId;
    }

    public void setRewardId(Long rewardId) {
        this.rewardId = rewardId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Long getRewardPos() {
        return rewardPos;
    }

    public void setRewardPos(Long rewardPos) {
        this.rewardPos = rewardPos;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}