package qingning.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class CryptAES {

    //private static final String AESTYPE ="AES/ECB/PKCS5Padding";
    private static final String AESTYPE ="AES/CBC/PKCS5Padding";

    public static String AES_Encrypt(String plainText,String keyStr, String iv) {
        byte[] encrypt = null;
        try{
            Key key = generateKey(keyStr);
            Cipher cipher = Cipher.getInstance(AESTYPE);
            //cipher.init(Cipher.ENCRYPT_MODE, key);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(getIV(iv)));//使用加密模式初始化 密钥
            encrypt = cipher.doFinal(plainText.getBytes());
        }catch(Exception e){
            e.printStackTrace();
        }
        //return new String(Base64.encodeBase64(encrypt));
       // return new BASE64Encoder().encode(encrypt);
        return ParseSystemUtil.parseByte2HexStr(encrypt);
    }

    public static String AES_Decrypt(String keyStr, String encryptData,String iv) {
        byte[] decrypt = null;
        try{
            Key key = generateKey(keyStr);
            Cipher cipher = Cipher.getInstance(AESTYPE);
            //cipher.init(Cipher.DECRYPT_MODE, key);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(getIV(iv)));//使用解密模式初始化 密钥
            //decrypt = cipher.doFinal(new BASE64Decoder().decodeBuffer(encryptData));
            decrypt = cipher.doFinal(ParseSystemUtil.parseHexStr2Byte(encryptData));

        }catch(Exception e){
            e.printStackTrace();
        }
        return new String(decrypt).trim();
    }
    public static String AES_Decrypt2(String keyStr, byte[] encryptData,String iv) {
        byte[] decrypt = null;
        try{
            Key key = generateKey(keyStr);
            Cipher cipher = Cipher.getInstance(AESTYPE);
            //cipher.init(Cipher.DECRYPT_MODE, key);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(getIV(iv)));//使用解密模式初始化 密钥
            //decrypt = cipher.doFinal(new BASE64Decoder().decodeBuffer(encryptData));
            decrypt = cipher.doFinal(encryptData);

        }catch(Exception e){
            e.printStackTrace();
        }
        return new String(decrypt).trim();
    }

    private static Key generateKey(String key)throws Exception{
        try{
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            return keySpec;
        }catch(Exception e){
            e.printStackTrace();
            throw e;
        }

    }

    private static byte[] getIV(String originalIV) {
        String iv = originalIV; //IV length: must be 16 bytes long
        return iv.getBytes();
    }
    

    public static void main(String[] args) {

        String keyStr = "sfjIud2thf56p2kK";
        String iv = "weifsaOAoS804ty5";

        String plainText = "this is a string will be AES_Encrypt";

       // String encText = AES_Encrypt(plainText,keyStr,iv);
        String decString = AES_Decrypt(keyStr, "3334518C69DFA9524A6AE9A6934EDD58",iv);

        //System.out.println(encText);
        System.out.println(decString);

    }
}