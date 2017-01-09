package qingning.common.server.util;


import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 本类封装DES加密、解密方法。
 * 
 * @author Administrator
 * 
 */
public class DES {

	private static String pricateKey;

	public static void setKey(String key) {
		pricateKey = key;
	}

	/**
	 * 对指定字符串用指定密码做加密处理后再输出用ALBase64编码后的字符串
	 * 
	 * @param encryptString
	 *            待加密字符串
	 * @param encryptKey
	 *            密码
	 * @return ALBase64编码后的加密字符串
	 * @throws Exception
	 */
	public static String encryptDES(String encryptString, String encryptKey) {
		byte[] encryptedData = null;
		try {
			IvParameterSpec zeroIv = new IvParameterSpec(encryptKey.getBytes());
			SecretKeySpec key = new SecretKeySpec(encryptKey.getBytes(), "DES");
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
			encryptedData = cipher.doFinal(encryptString.getBytes());
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
		return Base64.encode(encryptedData);
	}

	/**
	 * 对指定字符串用指定密码做解密处理后再输出解密后的字符串
	 * 
	 * @param decryptString
	 *            待解密字符串
	 * @param decryptKey
	 *            解密密码
	 * @return 解密后的字符串
	 * @throws Exception
	 */
	public static String decryptDES(String decryptString, String decryptKey) {

		byte decryptedData[] = null;
		try {
			byte[] byteMi = Base64.decode(decryptString);
			IvParameterSpec zeroIv = new IvParameterSpec(decryptKey.getBytes());
			SecretKeySpec key = new SecretKeySpec(decryptKey.getBytes(), "DES");
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
			decryptedData = cipher.doFinal(byteMi);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}

		return new String(decryptedData);
	}

	/**
	 * 对指定字符串用指定密码做加密处理后再输出用ALBase64编码后的字符串
	 * 
	 * @param encryptString
	 *            待加密字符串
	 * @param encryptKey
	 *            密码
	 * @return ALBase64编码后的加密字符串
	 * @throws Exception
	 */
	public static String encryptDES(String encryptString) throws Exception {
		IvParameterSpec zeroIv = new IvParameterSpec(pricateKey.getBytes());
		SecretKeySpec key = new SecretKeySpec(pricateKey.getBytes(), "DES");
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
		byte[] encryptedData = cipher.doFinal(encryptString.getBytes());
		return Base64.encode(encryptedData);
	}

	/**
	 * 对指定字符串用指定密码做解密处理后再输出解密后的字符串
	 * 
	 * @param decryptString
	 *            待解密字符串
	 * @param decryptKey
	 *            解密密码
	 * @return 解密后的字符串
	 * @throws Exception
	 */
	public static String decryptDES(String decryptString) throws Exception {

		byte[] byteMi = Base64.decode(decryptString);
		IvParameterSpec zeroIv = new IvParameterSpec(pricateKey.getBytes());
		SecretKeySpec key = new SecretKeySpec(pricateKey.getBytes(), "DES");
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
		byte decryptedData[] = cipher.doFinal(byteMi);

		return new String(decryptedData);
	}

}
