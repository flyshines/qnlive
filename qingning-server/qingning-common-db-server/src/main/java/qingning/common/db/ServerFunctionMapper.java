package qingning.common.db;

import qingning.common.db.persistence.mybatis.entity.ServerFunction;

public interface ServerFunctionMapper {
    int deleteByPrimaryKey(String serverName);

    int insert(ServerFunction record);

    int insertSelective(ServerFunction record);

    ServerFunction selectByPrimaryKey(String serverName);

    int updateByPrimaryKeySelective(ServerFunction record);

    int updateByPrimaryKey(ServerFunction record);
}