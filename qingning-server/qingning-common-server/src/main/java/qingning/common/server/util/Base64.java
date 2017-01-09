package qingning.common.server.util;


import java.io.ByteArrayOutputStream;

public class Base64 {

	static final char[] charTab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
			.toCharArray();

	/**
	 * Create a ALBase64 encoded string.
	 * 
	 * @param data
	 * @return encoded
	 */
	public static String encode(byte[] data) {
		return encode(data, 0, data.length, null).toString();
	}

	/**
	 * Encodes the part of the given byte array denoted by start and len to the
	 * ALBase64 format. The encoded data is appended to the given StringBuffer. If
	 * no StringBuffer is given, a new one is created automatically. The
	 * StringBuffer is the return value of this method.
	 * 
	 * @param data
	 * @param start
	 * @param len
	 * @param buf
	 * @return Encoded string
	 */
	public static StringBuffer encode(byte[] data, int start, int len,
			StringBuffer buf) {

		if (buf == null)
			buf = new StringBuffer(data.length * 3 / 2);

		int end = len - 3;
		int i = start;
		int n = 0;

		while (i <= end) {
			int d = (((data[i]) & 0x0ff) << 16)
					| (((data[i + 1]) & 0x0ff) << 8) | ((data[i + 2]) & 0x0ff);

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append(charTab[(d >> 6) & 63]);
			buf.append(charTab[d & 63]);

			i += 3;

			if (n++ >= 14) {
				n = 0;
				buf.append("\r\n");
			}
		}

		if (i == start + len - 2) {
			int d = (((data[i]) & 0x0ff) << 16) | (((data[i + 1]) & 255) << 8);

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append(charTab[(d >> 6) & 63]);
			buf.append("=");
		} else if (i == start + len - 1) {
			int d = ((data[i]) & 0x0ff) << 16;

			buf.append(charTab[(d >> 18) & 63]);
			buf.append(charTab[(d >> 12) & 63]);
			buf.append("==");
		}

		return buf;
	}

	static int decode(char c) {
		if (c >= 'A' && c <= 'Z')
			return (c) - 65;
		else if (c >= 'a' && c <= 'z')
			return (c) - 97 + 26;
		else if (c >= '0' && c <= '9')
			return (c) - 48 + 26 + 26;
		else
			switch (c) {
			case '+':
				return 62;
			case '/':
				return 63;
			case '=':
				return 0;
			default:
				throw new RuntimeException(
						new StringBuffer("unexpected code: ").append(c)
								.toString());
			}
	}

	/**
	 * Decodes the given ALBase64 encoded String to a new byte array. The byte
	 * array holding the decoded data is returned.
	 * 
	 * @param s
	 * @return The real thingi
	 */
	public static byte[] decode(String s) {

		int i = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int len = s.length();

		while (true) {
			while (i < len && s.charAt(i) <= ' ')
				i++;

			if (i == len)
				break;

			int tri = (decode(s.charAt(i)) << 18)
					+ (decode(s.charAt(i + 1)) << 12)
					+ (decode(s.charAt(i + 2)) << 6)
					+ (decode(s.charAt(i + 3)));

			bos.write((tri >> 16) & 255);
			if (s.charAt(i + 2) == '=')
				break;
			bos.write((tri >> 8) & 255);
			if (s.charAt(i + 3) == '=')
				break;
			bos.write(tri & 255);

			i += 4;
		}
		return bos.toByteArray();
	}
}
