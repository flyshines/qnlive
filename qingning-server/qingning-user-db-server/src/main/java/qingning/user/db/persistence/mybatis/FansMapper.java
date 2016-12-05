package qingning.user.db.persistence.mybatis;


import qingning.user.db.persistence.mybatis.entity.Fans;
import qingning.user.db.persistence.mybatis.entity.FansKey;

public interface FansMapper {
    int deleteByPrimaryKey(FansKey key);

    int insert(Fans record);

    int insertSelective(Fans record);

    Fans selectByPrimaryKey(FansKey key);

    int updateByPrimaryKeySelective(Fans record);

    int updateByPrimaryKey(Fans record);
}