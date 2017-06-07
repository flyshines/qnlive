package qingning.common.util;

import qingning.common.dj.DjSendMsg;
import qingning.common.entity.QNLiveException;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

/**校验验证码工具类
 * Created by Administrator on 2017/6/7.
 */
public class CodeVerifyficationUtil {


    /**效验手机验证码
     * @param appName
     * @param userId
     * @param verification_code
     * @param jedis
     * @return
     * @throws Exception
     */
    public static boolean verifyVerificationCode(String appName,String userId,String verification_code,Jedis jedis) throws Exception {
        Map<String, String> phoneMap = new HashMap();
        phoneMap.put("user_id", userId);
        phoneMap.put("code", verification_code);
        MiscUtils.getKeyOfCachedData(Constants.CACHED_KEY_LECTURER_ROOMS, phoneMap);


        String codeKey = MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_CODE, phoneMap);//根据userId 拿到 key
        if (!jedis.exists(codeKey)) {
            throw new QNLiveException("130009");
        }
        String code = jedis.get(codeKey);//拿到值
        if (appName.equals(Constants.HEADER_APP_NAME)) {//如果是qnlive就进行判断
            if (!code.equals(verification_code)) {//进行判断
                throw new QNLiveException("130002");
            }
        }

        String phoneKey = MiscUtils.getKeyOfCachedData(Constants.CAPTCHA_KEY_PHONE, phoneMap);//根据userId 拿到 key
        String phone = jedis.get(phoneKey);//拿到电话
        if (!appName.equals(Constants.HEADER_APP_NAME)) {//不是qnlive
            boolean key = DjSendMsg.checkVerificationCode(phone, code, verification_code, jedis);
            if (!key) {
                throw new QNLiveException("130002");
            }
        }
        return true;
    }

}
