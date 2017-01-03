package com.evolver.camerastepperdemo;

import android.app.Activity;
import android.content.Context;
import android.hardware.CameraStepperManager;
import android.hardware.SerialManager;
import android.hardware.SerialPort;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * Created by gjf on 12/9/16.
 */

public class ButtonFragment extends Fragment implements View.OnClickListener , View.OnLongClickListener {//

    private static final String TAG = "ButtonFragment";
    private static final int MSG_SET_DISTANCE = 0;
    private static final int MSG_SET_ANGLE = 1;
    private static final int MSG_GET_HEAD_ANGLE =2;
    private static final int MSG_SET_HEAD_ANGLE = 3;

    private ByteBuffer mOutputBuffer;
    private SerialPort mSerialPort;
    private TextView mTextView;
    private SerialPortReader mSerialPortReader;
    private ViewHandler mHandler;

    private Button speedUp0;
    private Button speedDown0;
    private Button speedUp1;
    private Button speedDown1;
    private Button stepUp;
    private Button stepDown;
    private Button stepLeft;
    private Button stepRight;
    private Button reset;
    double angle0;
    double speed0;
    int angle1;
    int speed1;
    private CameraStepperManager cameraStepperManager;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.button_frag, null);

        speedUp0 = (Button) v.findViewById(R.id.camera_up);
        speedDown0 = (Button) v.findViewById(R.id.camera_down);
        speedUp1 = (Button) v.findViewById(R.id.camera_left);
        speedDown1 = (Button) v.findViewById(R.id.camera_right);
        stepUp = (Button) v.findViewById(R.id.step_up);
        stepDown = (Button) v.findViewById(R.id.step_down);
        stepLeft = (Button) v.findViewById(R.id.step_left);
        stepRight = (Button) v.findViewById(R.id.step_right);
        reset = (Button) v.findViewById(R.id.camera_reset);
        mTextView = (TextView) v.findViewById(R.id.text_view);

        speedUp0.setOnClickListener(this);
        speedDown0.setOnClickListener(this);
        speedUp1.setOnClickListener(this);
        speedDown1.setOnClickListener(this);
        stepUp.setOnClickListener(this);
        stepDown.setOnClickListener(this);
        stepLeft.setOnClickListener(this);
        stepRight.setOnClickListener(this);
        reset.setOnClickListener(this);

        stepUp.setOnLongClickListener(this);
        stepDown.setOnLongClickListener(this);
        stepLeft.setOnLongClickListener(this);
        stepRight.setOnLongClickListener(this);

        mHandler = new ViewHandler();

        cameraStepperManager = (CameraStepperManager) getActivity().getSystemService(Context.CAMERA_STEPPER_SERVICE);
        return v;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mHandler.sendEmptyMessageDelayed(MSG_GET_HEAD_ANGLE, 0);
        cameraStepperManager.setSpeed(20);
        cameraStepperManager.setPosition(30);
        cameraStepperManager.reset();
        mOutputBuffer = ByteBuffer.allocate(1024);
        speed1 = 50;
        angle1 = 120;
        move(3,(byte)0);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.camera_up:
                speed0 = cameraStepperManager.getSpeed();
                if (speed0 >= 100) {
                    Toast.makeText(getActivity(),"到达最快速度",Toast.LENGTH_SHORT).show();
                    break;
                }
                cameraStepperManager.setSpeed(speed0+10);
                break;
            case R.id.camera_down:
                speed0 = cameraStepperManager.getSpeed();
                if (speed0 <= 20) {
                    Toast.makeText(getActivity(),"到达最慢速度",Toast.LENGTH_SHORT).show();
                    break;
                }
                cameraStepperManager.setSpeed(speed0-10);
                break;
            case R.id.camera_left:
                if (speed1 > 90) {
                    Toast.makeText(getActivity(),"到达最快速度",Toast.LENGTH_SHORT).show();
                    break;
                }
                speed1 += 10;
                move(2,speed1);
                break;
            case R.id.camera_right:
                if (speed1 < 10) {
                    Toast.makeText(getActivity(),"到达最慢速度",Toast.LENGTH_SHORT).show();
                    break;
                }
                speed1 -= 10;
                move(2,speed1);
                break;
            case R.id.step_up:
                while (cameraStepperManager.getState() != 0)
                    ;
                angle0 = cameraStepperManager.getPosition();
                if(angle0 > 55.0) {
                    Toast.makeText(getActivity(),"到达最大位置",Toast.LENGTH_SHORT).show();
                    break;
                }
                cameraStepperManager.setPosition(angle0+5);
                cameraStepperManager.setState(1);
                break;
            case R.id.step_down:
                while (cameraStepperManager.getState() != 0)
                    ;
                angle0 = cameraStepperManager.getPosition();
                if(angle0 < 5) {
                    Toast.makeText(getActivity(),"到达最小位置",Toast.LENGTH_SHORT).show();
                    break;
                }
                cameraStepperManager.setPosition(angle0-5);
                cameraStepperManager.setState(1);
                break;
            case R.id.step_left:
                //190 10
                if (angle1 > 220) {
                    Toast.makeText(getActivity(),"到达最左位置",Toast.LENGTH_SHORT).show();
                    break;
                }
                angle1 += 20;
                move(1,angle1);
                break;
            case R.id.step_right:
                if (angle1 < 20) {
                    Toast.makeText(getActivity(),"到达最右位置",Toast.LENGTH_SHORT).show();
                    break;
                }
                angle1 -= 20;
                move(1,angle1);
                break;
            case R.id.camera_reset:
                cameraStepperManager.setPosition(30);
                cameraStepperManager.setState(1);
                cameraStepperManager.reset();
                cameraStepperManager.setPosition(30);
                cameraStepperManager.setState(1);
                move(1,120);
                break;
            default:
                break;
        }

    }
    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.step_up:
                //190 10
                while (cameraStepperManager.getState() != 0)
                    ;
                angle0 = cameraStepperManager.getPosition();
                if (angle0 >= 60.0) {
                    Toast.makeText(getActivity(), "到达最大位置", Toast.LENGTH_SHORT).show();
                    break;
                }
                cameraStepperManager.setPosition(60);
                cameraStepperManager.setState(1);
                break;
            case R.id.step_down:
                while (cameraStepperManager.getState() != 0)
                    ;
                angle0 = cameraStepperManager.getPosition();
                if (angle0 <= 0.001) {
                    Toast.makeText(getActivity(), "到达最小位置", Toast.LENGTH_SHORT).show();
                    break;
                }
                cameraStepperManager.setPosition(0);
                cameraStepperManager.setState(1);
                break;
            case R.id.step_left:
                if (angle1 == 240) {
                    Toast.makeText(getActivity(),"到达最左位置",Toast.LENGTH_SHORT).show();
                    break;
                }
                move(1,240);
                break;
            case R.id.step_right:
                if (angle1 == 0) {
                    Toast.makeText(getActivity(),"到达最右位置",Toast.LENGTH_SHORT).show();
                    break;
                }
                move(1,0);
                break;
        }
        return true;
    }

    private void move(int howmove,int data0) {
        try {
            byte[] data =null;

            switch(howmove) {
                case 1://angle//
                     {
                    int sum = (0x0f + 0x0c + 0x05 + 0x01+ 0x0c + speed1 + data0 + 0x01) & 0xff;
                    switch (sum) {
                        case 0xf8: {
                            data = new byte[]{(byte) 0xfd, 00, (byte) 0x0f, (byte) 0x0c, 05, 01, 0x0c, (byte) speed1, (byte) data0, 01, 00, 00, 00, (byte) 0xfe, 0x78, (byte) 0xf8};
                            break;
                        }
                        case 0xfd: {
                            data = new byte[]{(byte) 0xfd, 00, (byte) 0x0f, (byte) 0x0c, 05, 01, 0x0c, (byte) speed1, (byte) data0, 01, 00, 00, 00, (byte) 0xfe, 0x7d, (byte) 0xf8};
                            break;
                        }
                        case 0xfe:{
                            data = new byte[]{(byte) 0xfd, 00, (byte) 0x0f, (byte) 0x0c, 05, 01, 0x0c, (byte) speed1, (byte) data0, 01, 00, 00, 00, (byte) 0xfe, 0x7e, (byte) 0xf8};
                            break;
                        }
                        default:{
                            data = new byte[]{(byte) 0xfd, 00, (byte) 0x0f, (byte) 0x0c, 05, 01, 0x0c, (byte) speed1, (byte) data0, 01, 00, 00, 00, (byte) sum, (byte) 0xf8};
                            break;
                        }
                    }
                }
                    break;
                case 2://speed

                    int sum = (0x0f + 0x0c + 0x05 + 0x01 + 0x0c + angle1 + data0 + 0x01)&0xff;
                    switch (sum) {
                        case 0xf8: {
                            data = new byte[]{(byte)0xfd, 00, (byte)0x0f, (byte)0x0c, 05, 01, 0x0c, (byte) data0, (byte) angle1, 01, 00, 00, 00, (byte)0xfe,0x78, (byte)0xf8};
                            break;
                        }
                        case 0xfd: {
                            data = new byte[]{(byte)0xfd, 00, (byte)0x0f, (byte)0x0c, 05, 01, 0x0c, (byte) data0, (byte) angle1, 01, 00, 00, 00, (byte)0xfe,0x7d, (byte)0xf8};
                            break;
                        }
                        case 0xfe:{
                            data = new byte[]{(byte)0xfd, 00, (byte)0x0f, (byte)0x0c, 05, 01, 0x0c, (byte) data0, (byte) angle1, 01, 00, 00, 00, (byte)0xfe,0x7e, (byte)0xf8};
                            break;
                        }
                        default:{
                            data = new byte[]{(byte)0xfd, 00, (byte)0x0f, (byte)0x0c, 05, 01, 0x0c, (byte) data0, (byte) angle1, 01, 00, 00, 00, (byte)sum, (byte)0xf8};
                            break;
                        }
                    }

                    break;
                case 3:
                    data = new byte[]{(byte)0xfd, 00, (byte)0x0f, (byte)0x0c, 05, 01, 0x0c, 50, 120, 01, 00, 00, 00, (byte)0x38, (byte)0xf8};
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
        } catch (Exception e) {
            Log.e(TAG, "write failed", e);
        }
    }
    private void sendMess(int what, int data) {
        Message message = mHandler.obtainMessage(what, data);
        mHandler.sendMessage(message);
    }
    private void parseData(byte[] data) {
        int cmdStart = (data[0] & 255) << 8 | data[1] & 255;
        int cmdOrder = (data[2] & 255) << 8 | data[3] & 255;
        if (cmdStart == 0xC05) {
            if(cmdOrder == 0x0401) {
                angle1 = data[4] & 255;
                Log.i(TAG, "angle1 =" + angle1);
                sendMess(MSG_SET_HEAD_ANGLE , angle1);
            }
        }
    }
//    @Override
//    public void onReceive(byte[] mSendData) {
//        Log.d(TAG, "receive: " + Arrays.toString(mSendData));
//        parseData(mSendData);
//    }
    private void getHeadAngle() {
        byte[] data = new byte[]{(byte)0xfd, 00, 0x0d, 0x0c, 05, 04, 01, 00, 00, 00, 00, 0x23, (byte)0xf8};
        mOutputBuffer.clear();
        mOutputBuffer.put(data);
        try {
            mSerialPort.write(mOutputBuffer, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class ViewHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch(msg.what) {
                case MSG_SET_HEAD_ANGLE:
                    mTextView.setText("head: " + msg.obj.toString() + "度  " + speed1 + "度/秒\n" +
                                        "camera:" + (int)cameraStepperManager.getPosition()+ "度  " + (int)cameraStepperManager.getSpeed() + "度/秒"
                    );
                    break;
                case MSG_GET_HEAD_ANGLE:
                    mHandler.sendEmptyMessageDelayed(MSG_GET_HEAD_ANGLE, 500);
                    getHeadAngle();
                    break;
            }
        }
    }
}
