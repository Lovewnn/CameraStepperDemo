package com.evolver.camerastepperdemo;

import java.util.EventListener;

/**
 * Created by gjf on 1/3/17.
 */

public interface OnSerialPortReaderListener extends EventListener{
    void onReceive(byte[] mSendData);
}
