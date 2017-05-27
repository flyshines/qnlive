package qingning.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static double getCashInAmount(String total_amount){
        BigDecimal totalAmount = new BigDecimal(total_amount);
        totalAmount = totalAmount.multiply(OneHundred);
        BigDecimal withdrawalProportion = ONE.subtract(new BigDecimal(Constants.DIVIDED_PROPORTION));
        BigDecimal withdrawalAmount = totalAmount.multiply(withdrawalProportion);
        withdrawalAmount = withdrawalAmount.divide(OneHundred, 0, BigDecimal.ROUND_HALF_DOWN );
        NumberFormat number = NumberFormat.getNumberInstance();
        double ret =withdrawalAmount.doubleValue();// number.format(withdrawalAmount);//轉成string 確保不變
        return ret;
    }
    
    /**
     * 计算并返回要插入t_user_gains表的数据列表
     * @param userIdList 需要新增的用户id列表
     * @param userRoomAmountList 直播间总收入列表，用于获取直播间收入进行计算
     * @param userDistributerAmountList 分销总收入列表，用于获取分销总收入进行计算
     * @param userWithdrawSumList 用户提现成功总金额列表，用于计算余额
     * @return 计算后需要插入t_user_gains表的数据列表
     */
    public static List<Map<String, Object>> getGaoinsList(List<String> userIdList, 
    		List<Map<String, Object>> userRoomAmountList, 
    		List<Map<String, Object>> userDistributerAmountList, 
    		List<Map<String, Object>> userWithdrawSumList){
    	Map<String, Object> userRoomAmountMap = new HashMap<>();	//userId:roonAmount映射
		for(Map<String, Object> map : userRoomAmountList){
			userRoomAmountMap.put(map.get("lecturer_id").toString(), map.get("total_amount"));
		}
		Map<String, Object> userDistributerAmountMap = new HashMap<>();	//userId:distributerAmount映射
		for(Map<String, Object> map : userDistributerAmountList){
			userDistributerAmountMap.put(map.get("distributer_id").toString(), map.get("total_amount"));
		}
		Map<String, Object> userWithdrawSumMap = new HashMap<>();	//userId:withdrawSum映射
		for(Map<String, Object> map : userWithdrawSumList){
			userWithdrawSumMap.put(map.get("user_id").toString(), map.get("sum"));
		}
		
		List<Map<String, Object>> insertList = new ArrayList<Map<String, Object>>();
		try{
			for(String userId : userIdList){
				Map<String, Object> gainsMap = new HashMap<>();
				gainsMap.put("user_id", userId);
				/*
				 * 计算直播间总收入
				 */
				String live_room_total_amount = String.valueOf(userRoomAmountMap.get(userId));
				if(MiscUtils.isEmptyString(live_room_total_amount) || "null".equals(live_room_total_amount)){
					live_room_total_amount = "0";
				}
				gainsMap.put("live_room_total_amount", live_room_total_amount);
				/*
				 * 计算直播间实际收益
				 */
				double roomReal = Double.parseDouble(live_room_total_amount)/100;
				double live_room_real_incomes = getCashInAmount(String.valueOf(roomReal));
				gainsMap.put("live_room_real_incomes", live_room_real_incomes*100);
				
				/*
				 * 分销总收入
				 */
				String distributer_total_amount = String.valueOf(userDistributerAmountMap.get(userId));
				if(MiscUtils.isEmptyString(distributer_total_amount) || "null".equals(distributer_total_amount)){
					distributer_total_amount = "0";
				}
				gainsMap.put("distributer_total_amount", distributer_total_amount);
				/*
				 * 计算分销实际收益
				 */
				double distributerReal = Double.parseDouble(distributer_total_amount)/100;
				double distributer_real_incomes = getCashInAmount(String.valueOf(distributerReal));
				gainsMap.put("distributer_real_incomes", distributer_real_incomes*100);
				
				/*
				 * 计算用户总收入
				 */
				gainsMap.put("user_total_amount", 
						Double.parseDouble(live_room_total_amount) 
						+ Double.parseDouble(distributer_total_amount));
				/*
				 * 计算用户实际收入
				 */
				double user_total_real_incomes = live_room_real_incomes*100 + distributer_real_incomes*100;
				gainsMap.put("user_total_real_incomes", user_total_real_incomes);
				/*
				 * 计算余额
				 */
				String user_withdraw_sum = String.valueOf(userWithdrawSumMap.get(userId));
				if(MiscUtils.isEmptyString(user_withdraw_sum) || "null".equals(user_withdraw_sum)){
					user_withdraw_sum = "0";
				}
				double balance = user_total_real_incomes-Double.parseDouble(user_withdraw_sum);
				gainsMap.put("balance", balance);
				
				insertList.add(gainsMap);
			}
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}
		return insertList;
    }

    public static void main(String[] args) {
        System.out.println(getCashInAmount("1.11"));
    }



}
