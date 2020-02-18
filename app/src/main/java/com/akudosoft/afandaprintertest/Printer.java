package com.akudosoft.afandaprintertest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.serialport.SerialPort;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

//打印
public class Printer {
	private Socket mSocket = null; // 网口打印
	private SerialPort mSerialPort = null; // 串口打印
	private OutputStream mOutputStream = null; // 输出�?
	private InputStream mInputStream = null;
	private int type = 0;

	private UsbManager usbManager;
	private UsbDevice usbDevice;
	private static final String ACTION_USB_PERMISSION = "com.scpos.hddpos.USB_PERMISSION";
	private PendingIntent pendingIntent;
	private UsbInterface usbInterface;
	private UsbEndpoint outEndpoint, inEndpoint;
	private UsbDeviceConnection connection;
	private CallBack callBack;

	private int cacheSize = 16;

	private String PRINT_ERR1 = Constant.M_CONTEXT
			.getString(R.string.PRINT_ERR1);
	private String PRINT_ERR2 = Constant.M_CONTEXT
			.getString(R.string.PRINT_ERR2);
	private String PRINT_ERR3 = Constant.M_CONTEXT
			.getString(R.string.PRINT_ERR3);
	private String PRINT_ERR4 = Constant.M_CONTEXT
			.getString(R.string.PRINT_ERR4);
	private boolean printErrFlag = false;

	public interface CallBack {
		public void onSuccess(Printer printer);

		public void onFailure(String err);
	}

	private byte[] data;

	boolean isFirst = true;

	// 网口打印机
	public Printer(String printerIp, CallBack callBack) {
		type = 1;
		this.callBack = callBack;
		try {
			mSocket = new Socket();
			InetSocketAddress addr = new InetSocketAddress(printerIp, 9100);
			mSocket.connect(addr, 2000);
			mSocket.setSoTimeout(2000);
			mOutputStream = mSocket.getOutputStream();
			callBack.onSuccess(this);
			commit();
		} catch (Exception e) {
			e.printStackTrace();
			callBack.onFailure(e.getMessage());
		}
	}

	// usb打印
	public Printer(Context context, CallBack callBack) {
		type = 2;
		this.callBack = callBack;
		usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		HashMap<String, UsbDevice> map = usbManager.getDeviceList();
		for (UsbDevice device : map.values()) {
			if (1155 == device.getVendorId() && 33054 == device.getProductId()) {
				usbDevice = device;
			}
		}

		if (usbDevice == null) {
			callBack.onFailure("USB_PRINT_NOT_FIND");
		} else {
			if (usbManager.hasPermission(usbDevice)) {
				usbInterface = usbDevice.getInterface(0);
				outEndpoint = usbInterface.getEndpoint(1);
				inEndpoint = usbInterface.getEndpoint(0);
				connection = usbManager.openDevice(usbDevice);
				connection.claimInterface(usbInterface, true);
				startReading();
//				callBack.onSuccess(this);
				
//				commit();
			} else {
				context.registerReceiver(mUsbReceiver, filter);
				usbManager.requestPermission(usbDevice, pendingIntent); // 申请usb设备访问权限
			}
		}
	}

	private Thread mReadingthread = null;
	private boolean isReading = false;

	// 开线程读取数据
	private void startReading() {

		isReading = true;

		final StringBuffer qr = new StringBuffer();

		mReadingthread = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.e("mReadingthread", "isReading:"+isReading);
				while (isReading) {
					synchronized (this) {
						Log.e("接受数据", "MaxPacketSize:"+inEndpoint.getMaxPacketSize());
						byte[] bytes = new byte[inEndpoint.getMaxPacketSize()];
						int ret = connection.bulkTransfer(inEndpoint, bytes,
								bytes.length, 100);
						Log.e("接受数据", "ret:"+ret);
						if (ret > 0) {
							StringBuilder stringbuilder = new StringBuilder(
									bytes.length);
							for (byte b : bytes) {
								if (b != 0) {
									if (b == 2) {
										stringbuilder.append("da");
									}
									stringbuilder.append(Integer.toHexString(b));

								}
							}
							// 最终处理数据
							Log.e("接受数据", stringbuilder.toString());
						}
					}

				}
				connection.close();
			}

		});
		mReadingthread.start();
	}

	// 串口打印
	public Printer(int com, String baudrate, CallBack callBack) {
		type = 3;
		this.callBack = callBack;
		Log.e("Printer", "com:" + com + ",baudrate:" + baudrate);
		try {
			int baud = integer(baudrate);
			openSerialPort(com, baud);
			callBack.onSuccess(this);
			commit();
		} catch (Exception e) {
			e.printStackTrace();
			callBack.onFailure("SerialPortOpenFailure!");
		}
	}

	public void write(String s) {
		try {
			data = combineTowTytes(data, s.getBytes("gbk"));

		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	}

	public void writeHex(String hexStr) {
		data = combineTowTytes(data, ConversionNumber.HexString2Bytes(hexStr));
	}

	public void writeDec(String decStr) {

		data = combineTowTytes(data,
				ConversionNumber.HexString2Bytes(ConversionNumber
						.int2Hex(decStr)));

	}
	
	public void writeMapHex(byte[] map) throws Exception {

		data = combineTowTytes(data, map);

	}
	
	// Picture
		/*
		 * 函数名：BmpToArray 参 数：Bitmap bm 作 用：实现图像转换为打印数据
		 */
		public byte[] getPrintPictureCmd(Bitmap bm) {
			// 获得图像的宽和高
			int width = bm.getWidth();
			int height = bm.getHeight();

			// 获得原图的像素
			int pixR = 0;
			int pixG = 0;
			int pixB = 0;

			// 定义像素数组
			int[] pixels = new int[width * height];

			int widArray = ((width - 1) / 8) + 1;// 横向字节数
			int lenArray = widArray * height;// 纵向点数
			byte[] dataArray = new byte[lenArray + 8];// 定义一个变换后的数据数组

			dataArray[0] = 0x1D;
			dataArray[1] = 0x76;
			dataArray[2] = 0x30;
			dataArray[3] = 0x00;

			dataArray[4] = (byte) widArray;// xL
			dataArray[5] = (byte) (widArray / 256);// xH
			dataArray[6] = (byte) height;
			dataArray[7] = (byte) (height / 256);

			// 获得原图像素
			bm.getPixels(pixels, 0, width, 0, 0, width, height);

			int indexByte = 8;
			dataArray[indexByte] = 0;
			int indexBit = 0;
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {// 每一行进行转换，转换完成后，可能最后一个字节需要将数据移到高位
					// 获取当前像素值的r部分
					pixR = Color.red(pixels[i * width + j]);
					// 获取当前像素值的g部分
					pixG = Color.green(pixels[i * width + j]);
					// 获取当前像素值的b部分
					pixB = Color.blue(pixels[i * width + j]);
					// 一个临时的变量，保存变换后的值
					// int temp = (int)(0.299*pixR + 0.587*pixG + 0.114*pixB + 0.5);
					if ((pixR + pixG + pixB) < 384) {
						dataArray[indexByte] += 0x01;
					}

					++indexBit;

					if (indexBit < 8) {
						dataArray[indexByte] *= 2;// 相当于左移一位
					} else {
						indexBit = 0;
						++indexByte;
						if (indexByte < lenArray) {
							dataArray[indexByte] = 0;
						}
					}
				}

				if (indexBit != 0)// 存在不完整字节，1－7有效位
				{
					while (indexBit < 8) {
						dataArray[indexByte] *= 2;// 相当于左移一位
						++indexBit;
					}

					
					indexBit = 0;
					++indexByte;
					if (indexByte < lenArray) {
						dataArray[indexByte] = 0;
					}
				}
			}
			return dataArray;
		}


	// public abstract void reOver();

	// public void print() {
	// reOver();
	// commit();
	// }

	private void commit() {// 执行
		try {

			// checkPrintState();
			Log.e("commit", "type:" + type);
			if (type == 1) {
				mOutputStream.write(data);
				mOutputStream.flush();
				if (mSocket != null)
					mSocket.close();
			} else if (type == 2) {
				connection.bulkTransfer(outEndpoint, data, data.length, 600000);
				if (connection != null)
					connection.close();
			} else if (type == 3) {

				/*
				 * 前提，打印机开启流控后，每发一次数据必定有回读，回读数据 若空，继续读
				 * 若是0x13，继续读，当低于设定的低位时，还会发送0x11， 若非0x11，继续写 数据回读至少一个char
				 * 地址：http
				 * ://blog.csdn.net/robertsong2004/article/details/38540507
				 * 
				 * 当接收端的输入缓冲区内数据量超过设定的高位时,就向数据发送端发出XOFF字符（十进制的19或Control-S,
				 * 设备编程说明书应该有详细阐述）
				 */

				int allSize = data.length;
				int size;

				mOutputStream.write(data);
				mOutputStream.flush();
				/*
				 * for (int i = 0; i < allSize; i = i + cacheSize) {
				 * 
				 * size = allSize - i;
				 * 
				 * if (size >= cacheSize) { mOutputStream.write(data, i,
				 * cacheSize); mOutputStream.flush();
				 * 
				 * } else { mOutputStream.write(data, i, size);
				 * mOutputStream.flush(); }
				 * 
				 * Boolean printFlag = false; while (!printFlag) { int count =
				 * 0; while (count == 0) { count = mInputStream.available(); }
				 * byte[] b = new byte[count]; mInputStream.read(b);
				 * 
				 * String[] str = bytesToHexStrings(b); printFlag = false;
				 * 
				 * int TNum = -1; int FNum = -1;
				 * 
				 * for (int m = 0; m < str.length; m++) {
				 * 
				 * Log.e("LOG", "re:" + str[m]);
				 * 
				 * if ("11".equals(str[m])) TNum = m; if ("13".equals(str[m]))
				 * FNum = m; }
				 * 
				 * if ((TNum > FNum) && TNum >= 0) printFlag = true;
				 * 
				 * }
				 * 
				 * }
				 */
				closeSerialPort();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		data = null;
		System.gc();
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			try {
				String action = intent.getAction();

				if (ACTION_USB_PERMISSION.equals(action)) {
					synchronized (this) {
						usbDevice = (UsbDevice) intent
								.getParcelableExtra(UsbManager.EXTRA_DEVICE);
						if (intent.getBooleanExtra(
								UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
							if (usbDevice != null && isFirst) {
								isFirst = false;
								context.unregisterReceiver(mUsbReceiver);
								usbInterface = usbDevice.getInterface(0);
								outEndpoint = usbInterface.getEndpoint(1);
								connection = usbManager.openDevice(usbDevice);
								connection.claimInterface(usbInterface, true);
								callBack.onSuccess(Printer.this);
								commit();
							} else
								throw new Exception("USB DEVICE NOT FIND!");
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private byte[] combineTowTytes(byte[] bytes1, byte[] bytes2) {

		try {
			byte[] bytes3;
			if (bytes1 == null && bytes2 != null) {
				bytes3 = bytes2;
			} else if (bytes1 != null && bytes2 == null) {
				bytes3 = bytes1;
			} else if (bytes1 == null && bytes2 == null) {
				bytes3 = null;
			} else {
				bytes3 = new byte[bytes1.length + bytes2.length];
				System.arraycopy(bytes1, 0, bytes3, 0, bytes1.length);
				System.arraycopy(bytes2, 0, bytes3, bytes1.length,
						bytes2.length);
			}
			return bytes3;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private void openSerialPort(int com, int baudrate) throws Exception {

		if (mSerialPort == null) {
			/* Open the serial port */
			if (com <= 0)
				throw new Exception("front_printer_port com number <=0!");
			String path = "/dev/ttyS" + (com - 1);
			// String path = "/dev/ttyMT" + (com - 1);
			System.err.println("com:" + path);
			Log.e("openSerialPort", "com:" + path);
			mSerialPort = new SerialPort(new File(path), baudrate);
			Log.e("openSerialPort", "mSerialPort:" + mSerialPort);
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();
		}
	}

	private void closeSerialPort() {
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
			mOutputStream = null;
		}
	}

	private int integer(String str) {
		try {
			return Integer.valueOf(str);
		} catch (Exception e) {
			return 1;
		}
	}

	/**
	 * byte数组转换成16进制字符串
	 * 
	 * @param src
	 * @return
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder();
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/**
	 * byte数组转换成16进制字符数组
	 * 
	 * @param src
	 * @return
	 */
	public static String[] bytesToHexStrings(byte[] src) {
		if (src == null || src.length <= 0) {
			return null;
		}
		String[] str = new String[src.length];

		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				str[i] = "0";
			}
			str[i] = hv;
		}
		return str;
	}

	private void checkPrintState() throws Exception {

		String returnValue = null;
		printErrFlag = false;

		mOutputStream.write(new byte[] { 0x10, 0x04, 0x01 });
		mOutputStream.flush();

		Log.e("checkPrintState", "<--11-->" + mInputStream);
		returnValue = Integer.toBinaryString(mInputStream.read() & 0xff);
		Log.e("checkPrintState", "<--22-->");
		while (returnValue.length() < 8) {
			returnValue = "0" + returnValue;
		}
		Log.e("checkPrintState", "returnValue:" + returnValue);
		returnValue = returnValue.substring(4, 5);
		Log.e("checkPrintState", "returnValue11:" + returnValue);

		if ("1".equals(returnValue)) {
			// 打印机离线 打印机状态错误原因
			mOutputStream.write(new byte[] { 0x10, 0x04, 0x02 });
			mOutputStream.flush();

			returnValue = Integer.toBinaryString(mInputStream.read() & 0xff);
			while (returnValue.length() < 8) {
				returnValue = "0" + returnValue;
			}
			Log.e("checkPrintState", "returnValue22:" + returnValue);
			if ("1".equals(returnValue.substring(1, 2))) {
				printErrFlag = true;
				// 有错误
				throw new Exception(PRINT_ERR1);
			}

			if ("1".equals(returnValue.substring(2, 3))) {
				printErrFlag = true;
				// 打印停止
				throw new Exception(PRINT_ERR2);
			}
			if ("1".equals(returnValue.substring(4, 5))) {
				printErrFlag = true;
				// 正在通过使用FEED键出纸
				throw new Exception(PRINT_ERR3);
			}

			if ("1".equals(returnValue.substring(6, 7))) {
				printErrFlag = true;
				// 打印机盖开启
				throw new Exception(PRINT_ERR4);
			}
		}
	}
}
