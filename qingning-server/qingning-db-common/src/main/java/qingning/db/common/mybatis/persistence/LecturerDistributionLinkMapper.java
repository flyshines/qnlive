package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface LecturerDistributionLinkMapper {
	int insertLecturerDistributionLink(Map<String,Object> record);
	int updateLecturerDistributionLink(Map<String,Object> record);
}
