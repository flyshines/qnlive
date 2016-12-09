package qingning.lecturer.db.persistence.mybatis.entity;

import java.util.Date;

public class CourseMessage {
    private String messageId;

    private String courseId;

    private String message;

    private String messageUrl;

    private String messageQuestion;

    private Long audioTime;

    private Long messagePos;

    private String messageType;

    private String sendType;

    private String creatorId;

    private String creatorAvatarAddress;

    private Date createTime;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId == null ? null : messageId.trim();
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId == null ? null : courseId.trim();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? null : message.trim();
    }

    public String getMessageUrl() {
        return messageUrl;
    }

    public void setMessageUrl(String messageUrl) {
        this.messageUrl = messageUrl == null ? null : messageUrl.trim();
    }

    public String getMessageQuestion() {
        return messageQuestion;
    }

    public void setMessageQuestion(String messageQuestion) {
        this.messageQuestion = messageQuestion == null ? null : messageQuestion.trim();
    }

    public Long getAudioTime() {
        return audioTime;
    }

    public void setAudioTime(Long audioTime) {
        this.audioTime = audioTime;
    }

    public Long getMessagePos() {
        return messagePos;
    }

    public void setMessagePos(Long messagePos) {
        this.messagePos = messagePos;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType == null ? null : messageType.trim();
    }

    public String getSendType() {
        return sendType;
    }

    public void setSendType(String sendType) {
        this.sendType = sendType == null ? null : sendType.trim();
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId == null ? null : creatorId.trim();
    }

    public String getCreatorAvatarAddress() {
        return creatorAvatarAddress;
    }

    public void setCreatorAvatarAddress(String creatorAvatarAddress) {
        this.creatorAvatarAddress = creatorAvatarAddress == null ? null : creatorAvatarAddress.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}