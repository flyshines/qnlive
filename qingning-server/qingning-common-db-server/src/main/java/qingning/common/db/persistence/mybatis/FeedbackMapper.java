package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.Feedback;

public interface FeedbackMapper {
    int deleteByPrimaryKey(String feedbackId);

    int insert(Feedback record);

    int insertSelective(Feedback record);

    Feedback selectByPrimaryKey(String feedbackId);

    int updateByPrimaryKeySelective(Feedback record);

    int updateByPrimaryKey(Feedback record);
}