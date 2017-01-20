package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.Version;

import java.util.Map;

public interface VersionMapper {
    int deleteByPrimaryKey(Integer versionId);

    int insert(Version record);

    int insertSelective(Version record);

    Version selectByPrimaryKey(Integer versionId);

    int updateByPrimaryKeySelective(Version record);

    int updateByPrimaryKey(Version record);

    Map<String,Object> findVersionInfoByOS(String plateform);

    Map<String,Object> findForceVersionInfoByOS(String force_version_key);
}