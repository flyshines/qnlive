package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface FeedbackMapper {
	int insertFeedBack(Map<String,Object> record);
	int updateFeedBack(Map<String,Object> record);
	Map<String,Object> findFeedbackById(String feedback_id);
}