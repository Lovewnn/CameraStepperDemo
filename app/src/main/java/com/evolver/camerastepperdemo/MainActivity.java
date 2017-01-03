package com.evolver.camerastepperdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageInfo;
import android.graphics.Camera;
import android.graphics.Color;
import android.hardware.CameraStepperManager;
import android.hardware.SerialManager;
import android.hardware.SerialPort;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    String TAG = "MainActivity";

    private LinearLayout mSlaveBtn;
    private LinearLayout mCameraBtn;

    private TextView mSlaveText;
    private TextView mCameraText;

    private ButtonFragment buttonFragment;
    private SlaveFragment slaveFragment;

    private FragmentManager fragmentManager;

    private SerialManager mSerialManager;
    private SerialPort mSerialPort;
    private SerialPortReader mSerialPortReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSlaveBtn = (LinearLayout) findViewById(R.id.slave);
        mCameraBtn = (LinearLayout) findViewById(R.id.camera);
        mSlaveText = (TextView) mSlaveBtn.findViewById(R.id.slave_text);
        mCameraText = (TextView) mCameraBtn.findViewById(R.id.camera_text);

        mSlaveBtn.setOnClickListener(this);
        mCameraBtn.setOnClickListener(this);

        fragmentManager = getFragmentManager();
        setTabSelection(0);



        mSerialManager = (SerialManager) getSystemService("serial");
        String[] ports = mSerialManager.getSerialPorts();
        Log.i(TAG, "ports list : ");
        for(String i:ports){
            Log.i(TAG, " "+i);
        }

        try {
            mSerialPort = mSerialManager.openSerialPort("/dev/ttyS3",115200);
            if (mSerialPort != null) {
                mSerialPortReader = new SerialPortReader(mSerialPort);
                Log.i(TAG, "startRead");
                mSerialPortReader.start();
                //  mHandler.sendEmptyMessageDelayed(MSG_GET_HEAD_ANGLE, 500);
            }
        } catch (IOException e) {
            Log.e(TAG,"IOexception" +e);
        }
    }

    public SerialPort getSerialPort() {
        return mSerialPort;
    }
    public SerialPortReader getSerialPortReader() {
        return mSerialPortReader;
    }
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.slave:
                setTabSelection(0);
                break;
            case R.id.camera:
                setTabSelection(1);
                break;
            default:
                break;
        }
    }

    @SuppressLint("NewApi")
    private void setTabSelection(int index) {

        resetBtn();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        hideFragments(transaction);

        switch (index) {
            case 0:
                mSlaveText.setTextColor(Color.parseColor("#00ff00"));
                if (slaveFragment == null) {
                    slaveFragment = new SlaveFragment();
                    transaction.add(R.id.ctl_frag, slaveFragment);
                } else {
                    transaction.show(slaveFragment);
                }
                break;
            case 1:
                mCameraText.setTextColor(Color.parseColor("#00ff00"));
                if (buttonFragment == null) {
                    buttonFragment = new ButtonFragment();
                    transaction.add(R.id.ctl_frag, buttonFragment);
                } else {
                    transaction.show(buttonFragment);
                }
                break;
            default:
                break;
        }
        transaction.commit();
    }

    private void resetBtn() {
        mSlaveText.setTextColor(Color.parseColor("#000000"));
        mCameraText.setTextColor(Color.parseColor("#000000"));
    }

    @SuppressLint("NewApi")
    private void hideFragments(FragmentTransaction transaction) {
        if (buttonFragment != null) {
            transaction.hide(buttonFragment);
        }
        if (slaveFragment != null) {
            transaction.hide(slaveFragment);
        }
    }
}
