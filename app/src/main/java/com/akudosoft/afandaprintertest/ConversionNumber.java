package com.akudosoft.afandaprintertest;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public class ConversionNumber {

	private static String data2 = "";
	private static String data1 = "";
	private static int count = 1, index = 1;
	public static boolean is = false, begin = false;
	private static String hexString = "0123456789ABCDEF";

	public static void resetKey() {
		data2 = "";
		count = 1;
		index = 1;
		is = false;
	}

	// �ַ�ת16����
	public static String encode(String str) {

		// ���Ĭ�ϱ����ȡ�ֽ�����
		byte[] bytes = null;
		try {
			bytes = str.getBytes("gbk");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		// ���ֽ�������ÿ���ֽڲ���2λ16��������
		for (int i = 0; i < bytes.length; i++) {
			sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
			sb.append(hexString.charAt((bytes[i] & 0x0f) >> 0));
		}
		return sb.toString();
	}

	// 16����ת����
	public static int hex2Int(String s) {

		return Integer.valueOf(s, 16);
	}

	// ����ת16����
	public static String int2Hex(String s) {

		String tmp = Integer.toHexString(Integer.parseInt(s));
		if (tmp.length() < 2) {
			tmp = "0" + tmp;
		}
		return tmp;
	}

	// 255-HEX
	public static String odd255(String hex) {
		String tmp = Integer.toHexString(255 - Integer.valueOf(hex, 16));
		if (tmp.length() < 2) {
			tmp = "0" + tmp;
		}
		return tmp;
	}

	// ��ȡ��ݳ���?
	public static String getDateLenght(String data) {
		String tmp = String.valueOf(data.length() / 2 + 2);
		tmp = int2Hex(tmp);
		if (tmp.length() < 2) {
			tmp = "0" + tmp;
		}
		return tmp;
	}

	// У���?
	public static String xiaoyan(String allData) {
		long value = 0;
		int count = allData.length() / 2;
		for (int i = 0; i < count; i++) {
			value += Integer.valueOf(allData.substring(i * 2, i * 2 + 2), 16);
			value = value & Long.valueOf("8000007F", 16);
			if (value < 0) {
				value++;
				value = value | Long.valueOf("FFFFFF80", 16);
			}
		}
		value++;

		String newStr = Integer.toHexString((int) value);
		if (newStr.length() > 2)
			newStr = newStr.substring(newStr.length() - 2);
		if (newStr.length() < 2)
			newStr = "0" + newStr;
		return newStr;
	}

	public static String Bytes2HexString(byte[] b) {
		String ret = "";
		String hex;
		for (int i = 0; i < b.length; i++) {
			hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() < 2) {
				hex = '0' + hex;
			}
			ret += hex;
		}
		return ret.toUpperCase();
	}

	public static String Bytes2HexString2(byte[] b, int size) {
		String ret = "";
		String hex;
		for (int i = 0; i < size; i++) {
			hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() < 2) {
				hex = '0' + hex;
			}
			ret += hex;
		}
		return ret.toUpperCase();
	}

	public static boolean istongbu(byte[] b, int size) {
		String ret = "";
		String hex;
		if (size != 4)
			return false;
		for (int i = 0; i < size; i++) {
			hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() < 2) {
				hex = '0' + hex;
			}
			ret += hex;
		}
		return ret.toUpperCase().equals("0002FDFF") ? true : false;
	}

	/**
	 * ��ָ���ַ�src����ÿ�����ַ�ָ�ת���?6������ʽ �磺"2B44EFD9" --> byte[]{0x2B,
	 * 0x44, 0xEF, 0xD9}
	 * 
	 * @param src
	 *            String
	 * @return byte[]
	 */

	private static byte uniteBytes(byte src0, byte src1) {
		byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 }))
				.byteValue();
		_b0 = (byte) (_b0 << 4);
		byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 }))
				.byteValue();
		byte ret = (byte) (_b0 ^ _b1);
		return ret;
	}

	public static byte[] HexString2Bytes(String src) {
		byte[] ret = new byte[src.length() / 2];
		byte[] tmp;

		try {
			tmp = src.getBytes("GBK");
			for (int i = 0; i < src.length() / 2; i++) {
				ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public static String HexString2String(String bytes) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(
				bytes.length() / 2);
		// ��ÿ2λ16����������װ��һ���ֽ�
		for (int i = 0; i < bytes.length(); i += 2)
			baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString
					.indexOf(bytes.charAt(i + 1))));
		try {
			return new String(baos.toByteArray(), "gbk");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	// ��ȡ��Ч���?
	public static String guolv1(byte[] buffer, int size) {
		String data = Bytes2HexString2(buffer, size);
		for (int i = 0; i < data.length(); i += 2) {
			if (data.charAt(i) == '0' && data.charAt(i + 1) == '0') {
				data1 = "";
				begin = true;
			}

			if (begin)
				data1 += data.substring(i, i + 2);

			if (data.charAt(i) == 'F' && data.charAt(i + 1) == 'F') {
				begin = false;
				if (data1.length() >= 8) {
					String tmp = data1;
					data1 = "";
					return tmp;
				}
				data1 = "";
			}
		}
		return null;
	}

	// ���ƴ�ӡ�������������?
	public static String guolv2(String bf) {
		if (bf == null || bf.length() <= 8)
			return null;
		System.out.println("bf=" + bf);

		if (is)
			data2 += bf.substring(16, bf.length() - 4);
		else
			data2 = bf.substring(16, bf.length() - 4);

		count = hex2Int(bf.substring(12, 14));
		index = hex2Int(bf.substring(14, 16));

		if (count == index) {
			is = false;
			return HexString2String(data2);
		} else if (index % 4 == 0) {
			is = true;
			return "goon";
		} else {
			is = true;
			return null;
		}

	}

	// // ��Ʒ���ش���
	// public static String CPDownloadguolv(String bf) {
	// System.out.println("downloadbf=" + bf);
	//
	// if (bf.matches("00[0-9A-Z]{4}04FBFF"))
	// return "over";
	// if(bf.length()==0)
	// return null;
	//
	// if (is)
	// data2 += "\r\n"
	// + HexString2String(bf.substring(20, bf.length() - 4));
	// else
	// data2 = HexString2String(bf.substring(20, bf.length() - 4));
	//
	// count = (hex2Int(bf.substring(12, 14)) - 1)
	// + (hex2Int(bf.substring(14, 16)) - 1) * 200;
	// index = (hex2Int(bf.substring(16, 18)) - 1)
	// + (hex2Int(bf.substring(18, 20)) - 1) * 200;
	//
	// System.out.println("count="+count);
	// System.out.println("index="+index);
	//
	// if (count == index) {
	//
	// // ���?������������д�뵽�ļ�
	//
	// PopupMenuMan.setDownLoadjindu(bf.substring(6, 8), index + "/"
	// + count);
	// MainDataMan.g_mainDataMan.Write2SDCard(data2, bf.substring(6, 8));
	// is = false;
	// return data2;
	// } else {
	// is = true;
	// PopupMenuMan.setDownLoadjindu(bf.substring(6, 8), index + "/"
	// + count);
	// return null;
	// }
	//
	// }
	//
	// public static ArrayList<String> addZD(String bfs) {
	// byte[] bf = null;
	// ArrayList<String> list = new ArrayList<String>();
	// try {
	// bf = bfs.getBytes(Constant.ENCODING);
	// list.add(new String(bf, 0, 20, Constant.ENCODING));
	// int i =20;
	// for ( i = 20; i < (bf.length) - 39; i = i + 40) {
	// list.add(new String(bf, i,40, Constant.ENCODING));
	// }
	// list.add(new String(bf, i,18, Constant.ENCODING));
	// return list;
	// } catch(StringIndexOutOfBoundsException s){
	// return list;
	// }catch (UnsupportedEncodingException e) {
	// }
	// return null;
	// }
}
