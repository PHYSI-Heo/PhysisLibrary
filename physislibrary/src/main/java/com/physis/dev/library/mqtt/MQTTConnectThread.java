package com.physis.dev.library.mqtt;

import android.os.Handler;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MQTTConnectThread extends Thread {

    private final static int CONNECT_TIMEOUT = 2;

    private MqttClient mClient = null;
    private Handler handler = null;

    public MQTTConnectThread(MqttClient mClient, Handler handler){
        this.mClient = mClient;
        this.handler = handler;
    }

    @Override
    public void run() {
//        super.run();
        try {
            if(mClient != null && mClient.isConnected())
                return;
            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(CONNECT_TIMEOUT);
            mClient.connect(options);
        } catch (MqttException e ) {
            e.printStackTrace();
        }finally {
            if (mClient != null) {
                handler.obtainMessage(MQTTClient.MQTT_CONNECTED, mClient.isConnected()).sendToTarget();
            }
        }
    }
}
