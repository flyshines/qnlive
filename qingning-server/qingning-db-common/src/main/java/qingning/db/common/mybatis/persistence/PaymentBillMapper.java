package qingning.db.common.mybatis.persistence;

import java.util.Map;

public interface PaymentBillMapper {
/*    int deleteByPrimaryKey(String paymentId);

    int insert(PaymentBill record);

    int insertSelective(PaymentBill record);

    PaymentBill selectByPrimaryKey(String paymentId);

    int updateByPrimaryKeySelective(PaymentBill record);

    int updateByPrimaryKey(PaymentBill record);

    int updateByTradeIdKeySelective(PaymentBill updatePayBill);

    PaymentBill selectByTradeId(String tradeId);*/
	int insertPaymentBill(Map<String,Object> record);
	int updatePaymentBill(Map<String,Object> record);
	Map<String,Object> findPaymentBillByTradeId(String tradeId);

	Map<String,Object> findTradeIdByPamentid(String tradeId);
}