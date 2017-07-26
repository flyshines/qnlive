package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface TradeBillMapper {
    int insertTradeBill(Map<String,Object> record);
    int updateTradeBill(Map<String,Object> record);
    Map<String,Object> findByOutTradeNo(String outTradeNo);


    String findUserWhetherToPay(Map<String,Object> record);


    Map<String,Object> findUserNumberByCourse(Map<String,Object> record);
    /**
     * 根据条件查询订单
     * @param selectTradeBillMap
     * @return
     */
	Map<String, Object> findTradeBillByMap(Map<String, Object> selectTradeBillMap);

    Map<String,Object> findUserOrderByCourse(Map<String,Object> record);
}