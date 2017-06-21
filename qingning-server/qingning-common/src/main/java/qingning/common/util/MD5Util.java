package qingning.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class MD5Util {

    /**
     * MD5转换
     * 
     * @param plainText
     * @return MD5字符串
     */
    public static String toMD5(String plainText){

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance ("MD5");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException ("MD5 error:",e);
        }
        messageDigest.update (plainText.getBytes ());
        byte by[] = messageDigest.digest ();

        StringBuffer buf = new StringBuffer ();
        int val;
        for ( int i = 0 ; i < by.length ; i++ ) {
            val = by[i];
            if (val < 0) {
                val += 256;
            } else if (val < 16) {
                buf.append ("0");
            }
            buf.append (Integer.toHexString (val));
        }
        return buf.toString ().toUpperCase ();
    }

    /**
     * MD5转换
     * 
     * @param plainText
     * @return MD5字符串
     */
    public static String getMD5(String plainText){

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance ("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException ("MD5 error:",e);
        }
        messageDigest.update (plainText.getBytes ());
        byte by[] = messageDigest.digest ();

        StringBuffer buf = new StringBuffer ();
        int val;
        for ( int i = 0 ; i < by.length ; i++ ) {
            val = by[i];
            if (val < 0) {
                val += 256;
            } else if (val < 16) {
                buf.append ("0");
            }
            buf.append (Integer.toHexString (val));
        }
        return buf.toString ().toUpperCase ();
    }

    /**
     * @Title: Md5Lenth16
     * @Description: 取长度16位
     * @Author: Administrator
     * @Since: 2012-3-20上午10:16:21
     * @param plainText
     * @return
     */
    public static String Md5Lenth16(String plainText){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance ("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException ("MD5 error:",e);
        }
        md.update (plainText.getBytes ());
        byte b[] = md.digest ();
        int i;
        StringBuffer buf = new StringBuffer ("");
        for ( int offset = 0 ; offset < b.length ; offset++ ) {
            i = b[offset];
            if (i < 0) i += 256;
            if (i < 16) buf.append ("0");
            buf.append (Integer.toHexString (i));
        }
        return buf.toString ().substring (8, 24);// 16位的加密
    }

    public static void main(String[] s) throws Exception {
    	//System.out.println("md5值："+getMD5("appid=testappid1deviceid=testwan").toLowerCase());
        /*Map<String,String> a = new HashMap<>();

        a.put("login_name","W44G9WRB");
        a.put("login_pw","3a01dda81e8cada809b70f33677b43e3");
        a.put("country","86");
        a.put("sys_area_code","440100");


        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(a);
        System.out.println(json);
        String b = test(json,"securecode",1);
        System.out.println(b);*/
    	
    	/*
    	 * 生成t_admin_user帐号密码
    	 */
    	String userPw = "88888888";	//用户密码（未加密）
    	String md5Pw = MD5Util.getMD5(userPw);
    	System.out.println("对原始密码进行第一次加密：" + md5Pw);
    	md5Pw = md5Pw + "_qnlive";
    	md5Pw = MD5Util.getMD5(md5Pw);
    	//输出加密密码
    	System.out.println("对原始密码进行第二次加密：" + md5Pw);
    	//输出uuid，用于当作user_id
    	System.out.println("UUID:" + MiscUtils.getUUId());
    }




//    /**
//     * 根据私盐加密
//     * @param source
//     * @return
//     */
//    public static String getMd5Salt(String source,String salt)
//    {
//        Md5Hash md5Hash = new Md5Hash(source, salt, 1);
//        return md5Hash.toString();
//    }
    /**
     * 加密工具类
     * @param sour 待加密字符串
     * @param salt 私盐
     * @param hashIterations 加密次数
     * @return 加密后字符串
     */
    public static String test(String sour, String salt, int hashIterations) throws Exception {

        //若带加密字符串为空，直接返回，或抛异常，可根据自己系统处理逻辑来定义
        if(StringUtils.isBlank(sour)){
            return null;
        }

        //获取md5实例
        MessageDigest digest = getDigest("MD5");

        //加私盐
        if (salt != null) {
            digest.reset();
            digest.update(getBytes(salt));
        }

        //第一次加密
        byte[] hashed = digest.digest(getBytes(sour));
        int iterations = hashIterations - 1;

        //超过第一次的加密
        for (int i = 0; i < iterations; ++i) {
            digest.reset();
            hashed = digest.digest(hashed);
        }

        //返回加密结果
        return encodeToString(hashed);
    }

    /**
     * 获取实例
     * @param algorithmName
     * @return
     * @throws Exception
     */
    protected static MessageDigest getDigest(String algorithmName) throws Exception {
        try {
            return MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            String msg = "No native '" + algorithmName + "' MessageDigest instance available on the current JVM.";
            throw new Exception(msg, e);
        }
    }


    /**
     * 将string转换成byte数组
     * @param str 带转换字符串
     * @return
     */
    public static byte[] getBytes(String str){

        //若待转换字符串为空，直接返回，或抛异常，可根据自己系统处理逻辑来定义
        if(StringUtils.isBlank(str)){
            return null;
        }

        try {
            return str.getBytes("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e','f' };

    /**
     * 对byte数组重新生成字符串
     * @param bytes bytes数组
     * @return
     */
    public static String encodeToString(byte[] bytes) {
        char[] encodedChars = encode(bytes);
        return new String(encodedChars);
    }


    /**
     * byte数组转字符串处理方法
     * @param data bytes数组
     * @return
     */
    public static char[] encode(byte[] data) {
        int l = data.length;

        char[] out = new char[l << 1];

        int i = 0;
        for (int j = 0; i < l; ++i) {
            out[(j++)] = DIGITS[((0xF0 & data[i]) >>> 4)];
            out[(j++)] = DIGITS[(0xF & data[i])];
        }

        return out;
    }

}
