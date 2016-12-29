package qingning.common.db.persistence.mybatis.entity;

import java.util.Date;

public class CoursesStudents extends CoursesStudentsKey {
    private Double paymentAmount;

    private String coursePassword;

    private String studentType;

    private Date createTime;

    private Date createDate;

    private Long studentPos;

    public Double getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Double paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getCoursePassword() {
        return coursePassword;
    }

    public void setCoursePassword(String coursePassword) {
        this.coursePassword = coursePassword == null ? null : coursePassword.trim();
    }

    public String getStudentType() {
        return studentType;
    }

    public void setStudentType(String studentType) {
        this.studentType = studentType == null ? null : studentType.trim();
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

    public Long getStudentPos() {
        return studentPos;
    }

    public void setStudentPos(Long studentPos) {
        this.studentPos = studentPos;
    }
}