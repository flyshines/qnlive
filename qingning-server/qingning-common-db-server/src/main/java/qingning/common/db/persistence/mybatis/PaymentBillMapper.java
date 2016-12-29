package qingning.common.db.persistence.mybatis;

import qingning.common.db.persistence.mybatis.entity.PaymentBill;

public interface PaymentBillMapper {
    int deleteByPrimaryKey(String paymentId);

    int insert(PaymentBill record);

    int insertSelective(PaymentBill record);

    PaymentBill selectByPrimaryKey(String paymentId);

    int updateByPrimaryKeySelective(PaymentBill record);

    int updateByPrimaryKey(PaymentBill record);

    int updateByTradeIdKeySelective(PaymentBill updatePayBill);

    PaymentBill selectByTradeId(String tradeId);
}