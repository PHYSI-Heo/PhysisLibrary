package com.physis.dev.library;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.physis.dev.library.mqtt.MQTTClient;
import com.physis.dev.library.mqtt.SubscribeData;

import java.lang.ref.WeakReference;

public class PHYSIsMQTTActivity extends AppCompatActivity {

    private static final String PHYSI_MQTT_IP = "physicomtech.com";
    private static final String PHYSI_MQTT_PORT = "1883";

    private MQTTClient mqttClient = null;
    private boolean isCustom = false;

    private static class MQTTHandle extends Handler {
        private final WeakReference<PHYSIsMQTTActivity> mActivity;
        MQTTHandle(PHYSIsMQTTActivity activity) {
            mActivity = new WeakReference<PHYSIsMQTTActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PHYSIsMQTTActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }
    protected final MQTTHandle mqttHandler = new MQTTHandle(this);

    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MQTTClient.MQTT_CONNECTED:
                onMQTTConnectedStatus((boolean) msg.obj);
                break;
            case MQTTClient.MQTT_DISCONNECTED:
                onMQTTDisconnected();
                break;
            case MQTTClient.MQTT_SUB_LISTENER:
                SubscribeData subData = (SubscribeData) msg.obj;
                if(isCustom){
                    onSubscribeListener(subData.getTopic(), subData.getMessage());
                }else{
                    String topic = subData.getTopic();
                    int sepIndex = topic.indexOf("/");
                    onSubscribeListener(topic.substring(0, sepIndex),
                            topic.substring(sepIndex + 1), subData.getMessage());
                }
                break;
        }
    }

    protected void setCustomMode(boolean isCustom){
        this.isCustom = isCustom;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mqttClient = MQTTClient.getInstance();
        mqttClient.setHandler(mqttHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttClient.disconnect();
    }

    protected void connectMQTT(){
        mqttClient.connect(PHYSI_MQTT_IP, PHYSI_MQTT_PORT);
    }

    protected void connectMQTT(String ip, String port){
        mqttClient.connect(ip, port);
    }

    protected void disconnectMQTT(){
        mqttClient.disconnect();
    }

    protected void startSubscribe(String serialNum, String topic){
        mqttClient.subscribe(serialNum + "/" + topic);
    }

    protected void stopSubscribe(String serialNum, String topic){
        mqttClient.unsubscribe(serialNum + "/" + topic);
    }

    protected void publish(String serialNum, String topic, String message){
        mqttClient.publish(serialNum + "/" + topic, message);
    }

    protected void startSubscribe(String topic){
        mqttClient.subscribe(topic);
    }

    protected void stopSubscribe(String topic){
        mqttClient.unsubscribe(topic);
    }

    protected void publish(String topic, String message){
        mqttClient.publish(topic, message);
    }

    protected void onMQTTConnectedStatus(boolean result){

    }

    protected void onMQTTDisconnected(){

    }

    protected void onSubscribeListener(String serialNum, String topic, String data){

    }

    protected void onSubscribeListener(String topic, String data){

    }
}

