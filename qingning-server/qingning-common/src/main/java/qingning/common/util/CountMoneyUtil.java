package qingning.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * Created by GHS on 2017/5/10.
 * 計算金額
 */
public class CountMoneyUtil {

    public static final BigDecimal ONE = new BigDecimal(1);
    public static final BigDecimal OneHundred = new BigDecimal(100);
    public static final int SCALE = 2;//保留2位小数

    /**
     * 可提現金額
     * (1 - x) * 直播總收入/分銷總收入
     *  ( ONE - Constants.DIVIDED_PROPORTION )* total_amount
     * @param total_amount 總額
     * @return
     */
    public static String getCashInAmount(String total_amount){
        BigDecimal totalAmount = new BigDecimal(total_amount);
        totalAmount = totalAmount.multiply(OneHundred);
        BigDecimal withdrawalProportion = ONE.subtract(new BigDecimal(Constants.DIVIDED_PROPORTION));
        BigDecimal withdrawalAmount = totalAmount.multiply(withdrawalProportion);
        withdrawalAmount = withdrawalAmount.divide(OneHundred, 0, BigDecimal.ROUND_HALF_DOWN );
        NumberFormat number = NumberFormat.getNumberInstance();
        String str = number.format(withdrawalAmount);//轉成string 確保不變
        return str;
    }

    public static void main(String[] args) {
        System.out.println(getCashInAmount("1.11"));
    }



}
