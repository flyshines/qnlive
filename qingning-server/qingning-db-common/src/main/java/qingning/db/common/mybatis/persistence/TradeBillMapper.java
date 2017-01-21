package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface TradeBillMapper {
    int insertTradeBill(Map<String,Object> record);
    int updateTradeBill(Map<String,Object> record);
    Map<String,Object> findByOutTradeNo(String outTradeNo);
}