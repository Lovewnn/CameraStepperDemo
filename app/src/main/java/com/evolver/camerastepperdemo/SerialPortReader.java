package com.evolver.camerastepperdemo;

import android.hardware.SerialPort;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;


public class SerialPortReader extends Thread {
	private final static String TAG = "SerialPortReader";
	private final static int bufferLength = 1024 * 2;
	private OnSerialPortReaderListener mReaderListener;
	private Collection collection;

	/* 包头 */
	private int DATA_PACKET_START = 0xFD;
	/* 包尾 */
	private int DATA_PACKET_END = 0xF8;

	private volatile boolean mStopped = false;

	private SerialPort mSerialPort;

	private final ByteBuffer mReceiveBuffer;
	private final ByteBuffer mApplicationBuffer;

	private final ByteBuffer mNotifyBuffer;

	private InputStream mInputStream;

	private boolean checkStart = false;

	public SerialPortReader(SerialPort mSerialPort) {
		super(TAG);
		this.mSerialPort = mSerialPort;
		mApplicationBuffer = ByteBuffer.allocate(bufferLength);
		mReceiveBuffer = ByteBuffer.allocate(bufferLength);
		mNotifyBuffer = ByteBuffer.allocate(bufferLength);

		checkStart = false;
	}

	public void quit() {
		this.mStopped = true;
		Log.d(TAG, "quit");
	}

	@Override
	public void run() {
		super.run();
		byte[] buffer = new byte[bufferLength];
		if (mSerialPort == null) {
			Log.e(TAG, "mSerialPort is null ");
			return;
		}

		Log.d(TAG, "SerialPortReader running.");
		this.mApplicationBuffer.clear();

		while (!mStopped) {

			int e = 0;
			try {
                 mReceiveBuffer.clear();
				e = mSerialPort.read(mReceiveBuffer);

				if (e > 0) {
					mReceiveBuffer.get(buffer, 0, e);
					mApplicationBuffer.clear();
					mApplicationBuffer.put(buffer, 0, e);
					mApplicationBuffer.flip();
//					print(mReceiveBuffer, e);

					while (this.consumeData()) {
						;
					}
				} else if (e == -1) {
					Log.d(TAG, "run() : ConnectionLost");

					mStopped = true;
				} else {
					Log.e(TAG, "SerialPortReader read() failed.");
				}

			} catch (Exception e1) {
				String message = e1.getMessage();
				e1.printStackTrace();
			}

		}

		Log.d(TAG, "SerialPortReader reader ended.");

	}



	private boolean consumeData() {

//		printBuffer(mApplicationBuffer);

		for (int i = mApplicationBuffer.position(); i < mApplicationBuffer.limit(); i++) {

			int data = mApplicationBuffer.array()[i] & 255;

			if (data == DATA_PACKET_START) {
				mNotifyBuffer.clear();
			}

			if(checkStart) {
				if (data == 0x7D) {
					mNotifyBuffer.put((byte) 0xFD);
				} else if (data == 0x78) {
					mNotifyBuffer.put((byte) 0xF8);
				} else if (data == 0x7E) {
					mNotifyBuffer.put((byte) 0xFE);
				} else {
					mNotifyBuffer.put((byte)0xFE);
					mNotifyBuffer.put((byte)data);
				}
				checkStart = false;
			}else {
				if (data == 0xFE) {
					if(i == mApplicationBuffer.limit() -1) {
//						Log.w(TAG, "末尾： 0xFE" );
						//尾数据为0xFE， 跳过，下次循环
						checkStart = true;
						return false;
					}else {
						checkStart = false;
					}
					int b = mApplicationBuffer.array()[(i + 1)] & 255;
					if (b == 0x7D) {
						mNotifyBuffer.put((byte) 0xFD);
						i++;
					} else if (b == 0x78) {
						mNotifyBuffer.put((byte) 0xF8);
						i++;
					} else if (b == 0x7E) {
						mNotifyBuffer.put((byte) 0xFE);
						i++;
					} else {
						mNotifyBuffer.put((byte)data);
					}
				} else if (data == DATA_PACKET_END) {
					mNotifyBuffer.put((byte) data);

					mNotifyBuffer.flip();
					try {
						notifyListener(mNotifyBuffer);
					}catch (Exception e){
						e.printStackTrace();
					}
					mNotifyBuffer.clear();
				} else {
					mNotifyBuffer.put((byte) data);
				}
			}
		}

		if(mNotifyBuffer.position() >= 1000) {
			mNotifyBuffer.clear();
		}
		return false;
	}


	/*
  add event
   */
	public void addSerialPortReaderListener(OnSerialPortReaderListener listener) {
		if(collection == null) {
			collection = new HashSet();
		}
		collection.add(listener);
	}

	/*
    remove event
     */
	public void removeSerialPortReaderListenerr(OnSerialPortReaderListener listener) {
		if (collection == null) {
			return;
		}
		collection.remove(listener);
	}

	private void notifyListener(ByteBuffer mData) {
		int start = mData.get() & 255;
//		L.d(TAG, "start :" + start);
		int length = (mData.get() & 255) << 8 | mData.get() & 255;

		if (length != mData.limit()) {
			Log.e(TAG, "data length error:" + length + " != " + mData.limit());
			return;
		}


		int checkByte = 0;

		int residueLength = length - 3;
		mData.position(1);
		for (int i = 0; i < residueLength; i++) {
			checkByte += (mData.get() & 255);
		}


		if (((byte) checkByte & 255) != (mData.get() & 255)) {
			Log.e(TAG, "data check error");
			return;
		}
		mData.position(3);

		mData.limit(mData.limit() - 2);


		byte[] notifyData = new byte[mData.limit() - 3];

		mData.get(notifyData, 0, mData.limit() - 3);


		Iterator iterator = collection.iterator();
		while (iterator.hasNext()) {
			OnSerialPortReaderListener listener = (OnSerialPortReaderListener) iterator.next();
			listener.onReceive(notifyData);
		}
//		if (mReaderListener != null) {
//			mReaderListener.onReceive(notifyData);
//		}

	}
	
//	interface OnSerialPortReaderListener {
////		void onReceiveBefore(byte[] mSendData);
//		void onReceive(byte[] mSendData);
//	}
}
