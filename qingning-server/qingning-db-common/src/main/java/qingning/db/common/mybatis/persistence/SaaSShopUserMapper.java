package qingning.db.common.mybatis.persistence;


import java.util.Map;

public interface SaaSShopUserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Map<String,Object> record);

    int updateByPrimaryKey(Map<String,Object> record);

}