package qingning.mq.persistence.entity;

import java.util.Date;

public class CourseAudio {
    private String audioId;

    private String courseId;

    private String audioUrl;

    private Long audioPos;

    private Long audioTime;

    private String audioImage;

    private Date createTime;

    public String getAudioId() {
        return audioId;
    }

    public void setAudioId(String audioId) {
        this.audioId = audioId == null ? null : audioId.trim();
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId == null ? null : courseId.trim();
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl == null ? null : audioUrl.trim();
    }

    public Long getAudioPos() {
        return audioPos;
    }

    public void setAudioPos(Long audioPos) {
        this.audioPos = audioPos;
    }

    public Long getAudioTime() {
        return audioTime;
    }

    public void setAudioTime(Long audioTime) {
        this.audioTime = audioTime;
    }

    public String getAudioImage() {
        return audioImage;
    }

    public void setAudioImage(String audioImage) {
        this.audioImage = audioImage == null ? null : audioImage.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}