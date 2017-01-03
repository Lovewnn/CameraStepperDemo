package com.evolver.camerastepperdemo;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.SerialManager;
import android.hardware.SerialPort;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Created by gjf on 1/3/17.
 */

public class SlaveFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "SlaveFragment";
    private static final int MSG_SET_DISTANCE = 0;
    private static final int MSG_SET_ANGLE = 1;
    private static final int MSG_HEART_BEAT =4;
    private SerialManager mSerialManager;
    private SerialPort mSerialPort;
    private ByteBuffer mOutputBuffer;
    private ByteBuffer mInputBuffer;
    private SerialPortReader mSerialPortReader;
    private EditText angleView;
    private EditText distanceView;
    private EditText headAngleView;
    private EditText cusDistance;
    private EditText cusSpeed;
    private float mLocationX;
    private float mLocationY;
    private float mLocationAngle;
    private float mBeforeX;
    private float mBeforeY;
    private float mBeforeAngle;
    private ViewHandler mHandler;
    private byte[] mBeatData;
    private int countNum = 0;

    private Button slaveForward;
    private Button slaveBack;
    private Button turnLeft;
    private Button turnRight;
    private Button slaveStop;
    private Button slaveReset;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.slave_frag, null);
        angleView = (EditText) v.findViewById(R.id.angleview);
        distanceView =(EditText) v.findViewById(R.id.distance);
        cusDistance = (EditText) v.findViewById(R.id.cus_distance);
        cusSpeed = (EditText) v.findViewById(R.id.cus_speed);
        mHandler = new ViewHandler();

        slaveForward = (Button) v.findViewById(R.id.slave_forward);
        slaveBack = (Button) v.findViewById(R.id.slave_back);
        turnLeft = (Button) v.findViewById(R.id.turn_left);
        turnRight = (Button) v.findViewById(R.id.turn_right);
        slaveStop = (Button) v.findViewById(R.id.slave_stop);
        slaveReset = (Button) v.findViewById(R.id.slave_reset);

        slaveForward.setOnClickListener(this);
        slaveBack.setOnClickListener(this);
        turnLeft.setOnClickListener(this);
        turnRight.setOnClickListener(this);
        slaveStop.setOnClickListener(this);
        slaveReset.setOnClickListener(this);

        init();
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSerialPort = ((MainActivity)activity).getSerialPort();
        mSerialPortReader = ((MainActivity)activity).getSerialPortReader();
        mSerialPortReader.addSerialPortReaderListener(new OnSerialPortReaderListener() {
            @Override
            public void onReceive(byte[] mSendData) {
                Log.d(TAG, "receive: " + Arrays.toString(mSendData));
                parseData(mSendData);
            }
        });
    }
    private void init() {

        mOutputBuffer = ByteBuffer.allocate(1024);
        mInputBuffer = ByteBuffer.allocate(1024);
    }

    private int getDistance() {

        if(!cusDistance.getText().toString().equals("")) {
            return Integer.parseInt(cusDistance.getText().toString());
        }
        return 1000;
    }

    private int getSpeed() {
        if(!cusSpeed.getText().toString().equals("")) {
            return Integer.parseInt(cusSpeed.getText().toString());
        }
        return 200;
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            byte[] data = new byte[]{(byte) 0xfd, 00, (byte) 0x0e, (byte) 0x0c, 05, 06, 03, 02, 00, 00, 00, 00, 0x2a, (byte) 0xf8};
            mOutputBuffer.clear();
            mOutputBuffer.put(data);
            mSerialPort.write(mOutputBuffer, data.length);
        }catch (IOException e) {
            Log.e(TAG, "IOexception:"+e);
        }
    }

    private byte[] getMoveData(int upback) {

        byte[] data = new byte[]{(byte) 0xfd,00, 0x11, 0x0c, 05, 01, 00, 00, 00, 00, 00, 01 ,00,00,00, 00, (byte) 0xf8,};
        int sum = 0;
        int speed = getSpeed();
        int distance = getDistance();
        switch(upback) {
            case 1:
                data[6] = 0x09;
                break;
            case 2:
                data[6] = 0x0a;
                break;
        }
        data[7] = (byte) ((speed & 0xff00) >> 8);
        data[8] = (byte) (speed & 0xff);
        data[9] = (byte) ((distance & 0xff00) >> 8);
        data[10] = (byte) (distance & 0xff);
        for(int i = 1; i <15; i++) {
            sum += data[i];
        }
        data[15] = (byte) (sum&0xff);
        return data;
    }
    private void move(int howmove) {
        try {
            byte[] data =null;
            byte[] data2 = null;
            switch(howmove) {
                case 1:
                    data = getMoveData(1);
                    heartbeat(data);
                    break;
                case 2:
                    data = getMoveData(2);
                    heartbeat(data);
                    break;
                case 3:
                    data = new byte[]{(byte) 0xfd,00, 0x11, 0x0c, 05, 01, 07, 00, (byte) 0xc8, 00, (byte)0x5a, 01 ,00, 00,00, (byte) 0x4d, (byte) 0xf8};
                    break;
                case 4:
                    data = new byte[]{(byte) 0xfd,00, 0x11, 0x0c, 05, 01, 0x08, 00, (byte) 0xc8, 00, (byte)0x5a, 01 ,00, 00,00, (byte) 0x4e, (byte) 0xf8};
                    break;
                case 5:
                    data = new byte[]{(byte)0xfd, 00, (byte)0x0d, (byte)0x0c, 05, 01, 05, 01, 00, 00, 00, (byte)0x25, (byte)0xf8};
                    data2 = new byte[]{(byte)0xfd, 00, 0x0d, 0x0c, 05, 01, 0x0d, 01, 00, 00, 00, 0x2d, (byte)0xf8};
                    break;
            }

            Log.d(TAG, "write: " + data);
            for (int i=0; i < data.length ; i++) {
                Log.d(TAG, "write: " + data[i]);
            }
            Log.d(TAG, "write: " + data.toString());
            mOutputBuffer.clear();
            mOutputBuffer.put(data);
            mSerialPort.write(mOutputBuffer, data.length);
            if(data2 != null) {
                mOutputBuffer.clear();
                mOutputBuffer.put(data2);
                mSerialPort.write(mOutputBuffer, data2.length);
            }
        } catch (Exception e) {
            Log.e(TAG, "write failed", e);
        }
    }

    private void heartbeat(byte[] data) {
        mBeatData = data.clone();
        mBeatData[11] = 0;
        mBeatData[15] = (byte) (mBeatData[15] - 1);
        mHandler.sendEmptyMessageDelayed(MSG_HEART_BEAT, 500);
    }
    private void stopBeat() {
        mHandler.removeMessages(MSG_HEART_BEAT);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.slave_forward:
                //forward
                move(1);
              //  Toast.makeText(getActivity(),"forward",Toast.LENGTH_SHORT).show();
                break;
            case R.id.slave_back:
                //backward
                move(2);
              //  Toast.makeText(getActivity(),"backward",Toast.LENGTH_SHORT).show();
                break;
            case R.id.turn_left:
                //turn left
                move(3);
              //  Toast.makeText(getActivity(),"turn_left",Toast.LENGTH_SHORT).show();
                break;
            case R.id.turn_right:
                //turn right
                move(4);
              //  Toast.makeText(getActivity(),"turn_right",Toast.LENGTH_SHORT).show();
                break;
            case R.id.slave_stop:
                move(5);
              //  Toast.makeText(getActivity(),"slave_stop",Toast.LENGTH_SHORT).show();
                break;
            case R.id.slave_reset:
                reset();
             //   Toast.makeText(getActivity(),"slave_reset",Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void reset() {
        mLocationX = mBeforeX;
        mLocationY = mBeforeY;
        mLocationAngle = mBeforeAngle;
    }

    private boolean isRunning(float x, float y, float angle) {

        if(isChange(x,mBeforeX) || isChange(y, mBeforeY) || isChange(angle,mBeforeAngle))
            return true;
        return false;
    }

    private boolean isChange(float x ,float y) {
        float i = Math.abs(x - y);
        if(i >=0 && i <=0.01)
            return false;
        return true;
    }

    private void showResult(float x, float y, float angle) {

        if(mLocationX == 0 && mLocationY == 0 && mLocationAngle == 0) {
            mLocationX = x;
            mLocationY = y;
            mLocationAngle =angle;
            Log.i(TAG, "init data");
            return;
        }
        float diffX = Math.abs(x - mLocationX);
        float diffY = Math.abs(y - mLocationY);
        float diffAngle = Math.abs(angle - mLocationAngle);
        if( diffX != 0 || diffY !=0) {
            double distance = Math.sqrt(diffX * diffX + diffY * diffY);
            sendMess(MSG_SET_DISTANCE, format(distance));
            Log.i(TAG, " go " + distance);
            Log.i(TAG, " go2 " + format(distance));
        }
        if(diffAngle != 0) {
            sendMess(MSG_SET_ANGLE, format(diffAngle));
        }
        if(!isRunning(x, y, angle)) {
            Log.i(TAG, "stop");
            //mLocationX = x;
            //mLocationY = y;
            // mLocationAngle =angle;
            countNum ++;
            if(countNum == 3) {
                stopBeat();
                countNum = 0;
            }
        }
        mBeforeX =x;
        mBeforeY = y;
        mBeforeAngle = angle;
    }

    private String format(float angle) {

        DecimalFormat decimalFormat =new DecimalFormat("0.0");
        String angleString = decimalFormat.format(angle);
        return angleString + "度";
        //return angleString;
    }

    private String format(double distance) {

        DecimalFormat decimalFormat =new DecimalFormat("0.00");
        String distanceString = decimalFormat.format(distance);
        return distanceString + "mm";
    }

    private void parseData(byte[] data) {
        int cmdStart = (data[0] & 255) << 8 | data[1] & 255;
        int cmdOrder = (data[2] & 255) << 8 | data[3] & 255;
        if (cmdStart == 0xC05) {
            if (cmdOrder == 0x0506) {
                try {

                    int locationIntX = (data[4] & 255) << 24 | (data[5] & 255) << 16 | (data[6] & 255) << 8 | data[7] & 255;
                    float locationX = Float.intBitsToFloat(locationIntX);

                    int locationIntY = (data[8] & 255) << 24 | (data[9] & 255) << 16 | (data[10] & 255) << 8 | data[11] & 255;
                    float locationY = Float.intBitsToFloat(locationIntY);

                    int locationIntAngle = (data[12] & 255) << 24 | (data[13] & 255) << 16 | (data[14] & 255) << 8 | data[15] & 255;
                    float locationAngle = Float.intBitsToFloat(locationIntAngle);
                    showResult(locationX, locationY, locationAngle);
                    //debug begin
                    //sendMess(MSG_SET_DISTANCE, "X = " + format(locationX) + "   Y = " + format(locationY));
                    //sendMess(MSG_SET_ANGLE, " Angle = " + format(locationAngle));
                    Log.i(TAG, " locationX = " + locationX + "  locationY = " + locationY + "  locationAngle = " + locationAngle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
//    @Override
//    public void onReceive(byte[] mSendData) {
//        Log.d(TAG, "receive: " + Arrays.toString(mSendData));
//        parseData(mSendData);
//    }

    private void sendMess(int what, String data) {
        Message message = mHandler.obtainMessage(what, data);
        mHandler.sendMessage(message);
    }

    class ViewHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch(msg.what) {
                case MSG_SET_DISTANCE:
                    distanceView.setText(msg.obj.toString());
                    break;
                case MSG_SET_ANGLE:
                    angleView.setText(msg.obj.toString());
                    break;
                case MSG_HEART_BEAT:
                    Log.i(TAG , " heart beat");
                    mHandler.sendEmptyMessageDelayed(MSG_HEART_BEAT, 500);
                    mOutputBuffer.clear();
                    mOutputBuffer.put(mBeatData);
                    try {
                        mSerialPort.write(mOutputBuffer, mBeatData.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }


}
