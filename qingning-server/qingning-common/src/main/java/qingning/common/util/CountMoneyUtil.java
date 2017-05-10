package qingning.common.util;

import java.text.NumberFormat;

/**
 * Created by GHS on 2017/5/10.
 * 計算金額
 */
public class CountMoneyUtil {

    public static final double ONE = 1.0;
    /**
     * 可提現金額
     * (1 - x) * 直播總收入/分銷總收入
     *  ( ONE - Constants.DIVIDED_PROPORTION )* total_amount
     * @param total_amount 總額
     * @return
     */
    public static String getCashInAmount(String total_amount){
        double totalAmount = Double.valueOf(total_amount);//總額
        double withdrawalProportion = DoubleUtil.sub(ONE,  Constants.DIVIDED_PROPORTION);//可提現比例
        double withdrawalAmount = DoubleUtil.mul(withdrawalProportion, totalAmount);//提現總數
        NumberFormat number = NumberFormat.getNumberInstance();
        String str = number.format(withdrawalAmount);//轉成string 確保不變
        return str;
    }





}
