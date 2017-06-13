package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface VersionMapper {
    Map<String,Object> findVersionInfoByOS(Map<String, Object> plateform);
    Map<String,Object> findForceVersionInfoByOS(String force_version_key);
}