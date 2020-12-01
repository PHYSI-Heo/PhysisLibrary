package com.physis.dev.library.mqtt;

import android.content.Context;
import android.os.Handler;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTClient {

    private static final String TAG = MQTTClient.class.getSimpleName();

    private static final int QOS = 0;

    public static final int MQTT_CONNECTED = 300;
    public static final int MQTT_DISCONNECTED = 301;
    public static final int MQTT_SUB_LISTENER = 302;

    private static MQTTClient mqttClient = null;

    private MqttClient mClient = null;
    private Handler handler = null;


    private MQTTClient(){
    }

    public synchronized static MQTTClient getInstance(){
        if(mqttClient == null)
            mqttClient = new MQTTClient();
        return mqttClient;
    }

    public void setHandler(Handler handler){
        this.handler = handler;
    }

    public void connect(String ip, String port){
        if(mClient.isConnected())
            return;

        try {
            mClient = new MqttClient("tcp://" + ip + ":" + port,
                    MqttClient.generateClientId(),
                    new MemoryPersistence());
            mClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    handler.obtainMessage(MQTT_DISCONNECTED).sendToTarget();
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
            new MQTTConnectThread(mClient, handler).start();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(){
        if(mClient != null && mClient.isConnected()){
            try {
                mClient.disconnect();
                handler.obtainMessage(MQTT_DISCONNECTED).sendToTarget();
                mClient = null;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void subscribe(String topic){
        try {
            if(mClient == null)
                return;
            mClient.subscribeWithResponse(topic, QOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String subTopic, MqttMessage message) throws Exception {
                    handler.obtainMessage(MQTT_SUB_LISTENER,
                            new SubscribeData(subTopic, new String(message.getPayload()))).sendToTarget();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String topic){
        try {
            if(mClient == null)
                return;
            mClient.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message){
        try {
            if(mClient == null)
                return;
            mClient.publish(topic, new MqttMessage(message.getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


}
