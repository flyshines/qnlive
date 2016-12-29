package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.TradeBill;

import java.util.Map;

public interface TradeBillMapper {
    int deleteByPrimaryKey(String tradeId);

    int insert(TradeBill record);

    int insertSelective(TradeBill record);

    TradeBill selectByPrimaryKey(String tradeId);

    int updateByPrimaryKeySelective(TradeBill record);

    int updateByPrimaryKey(TradeBill record);

    TradeBill findByOutTradeNo(String outTradeNo);

    Map<String,Object> findMapByOutTradeNo(String outTradeNo);
}