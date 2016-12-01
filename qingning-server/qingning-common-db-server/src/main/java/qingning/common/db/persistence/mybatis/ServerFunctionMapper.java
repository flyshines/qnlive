package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.ServerFunction;

import java.util.List;
import java.util.Map;

public interface ServerFunctionMapper {
    int deleteByPrimaryKey(String serverName);

    int insert(ServerFunction record);

    int insertSelective(ServerFunction record);

    ServerFunction selectByPrimaryKey(String serverName);

    int updateByPrimaryKeySelective(ServerFunction record);

    int updateByPrimaryKey(ServerFunction record);

    List<Map<String,Object>> getServerUrls();

}