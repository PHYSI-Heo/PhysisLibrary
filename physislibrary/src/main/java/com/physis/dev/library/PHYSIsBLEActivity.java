package com.physis.dev.library;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.physis.dev.library.ble.Beacon;
import com.physis.dev.library.ble.BluetoothLEManager;

import java.lang.ref.WeakReference;

public class PHYSIsBLEActivity extends AppCompatActivity {

    private static final String ADDRESS_REGEX = ":";
    private static final String ADDRESS_REPLACE = "";
    public static final String HM_10_CONF = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String HM_RX_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";

    public static final int CONNECTED = 200;
    public static final int DISCONNECTED = 201;
    public static final int NO_DISCOVERY = 202;

    private BluetoothLEManager bleManager = null;
    private String serialNumber = null;

    private BluetoothDevice targetDevice = null;
    private boolean isDiscovery = false;
    private boolean isConnecting = false;
    private boolean isDirectConnect = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bleManager = BluetoothLEManager.getInstance(getApplicationContext());

        if(!bleManager.checkBleSupport()){
            Toast.makeText(getApplicationContext(),
                    "해당 디바이스는 블루투스 서비스를 제공하지 않습니다.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        if(!bleManager.isEnabled()){
            Toast.makeText(getApplicationContext(),
                    "해당 디바이스의 블루투스가 비활성화 상태입니다.\\n 블루투스 활성화 후 다시 시도해주세요.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        bleManager.bindService();
        bleManager.setHandler(bleHandler);
    }


    @Override
    protected void onStart() {
        super.onStart();
        bleManager.registerReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bleManager.unregisterReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.disconnectDevice();
        bleManager.unBindService();
    }

    protected void connectDevice(String serialNumber){
        if(isConnecting)
            return;
        this.serialNumber = serialNumber;
        this.isDiscovery = false;
        isConnecting = isDirectConnect = true;
        bleManager.scan(true, true);
    }

    protected void connectDevice(BluetoothDevice device){
        isConnecting = true;
        isDirectConnect = false;
        bleManager.connectDevice(device);
    }

    protected void disconnectDevice(){
        bleManager.disconnectDevice();
    }

    protected void onBLEConnectedStatus(int result){
        isConnecting = false;
    }

    protected void onBLEReceiveMsg(String msg){

    }

    protected void sendMessage(String msg){
        bleManager.writeCharacteristic(HM_10_CONF, HM_RX_TX, msg);
    }

    protected void sendMessage(String serviceUUID, String charUUID, String msg){
        bleManager.writeCharacteristic(serviceUUID, charUUID, msg);
    }

    private void checkTargetDevice(BluetoothDevice device){
        if(device.getAddress()
                .replaceAll(ADDRESS_REGEX, ADDRESS_REPLACE).equals(serialNumber)){
            bleManager.scan(false, false);
            isDiscovery = true;
            targetDevice = device;
        }
    }

    private static class BluetoothLHandle extends Handler {
        private final WeakReference<PHYSIsBLEActivity> mActivity;
        BluetoothLHandle(PHYSIsBLEActivity activity) {
            mActivity = new WeakReference<PHYSIsBLEActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PHYSIsBLEActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    protected final BluetoothLHandle bleHandler = new BluetoothLHandle(this);

    protected void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case BluetoothLEManager.BLE_SCAN_START:
                break;
            case BluetoothLEManager.BLE_SCAN_STOP:
                if(isDirectConnect){
                    if(!isDiscovery)
                        onBLEConnectedStatus(NO_DISCOVERY);
                    else
                        bleManager.connectDevice(targetDevice);
                }
                break;
            case BluetoothLEManager.BLE_SCAN_DEVICE:
                if(isDirectConnect)
                    checkTargetDevice((BluetoothDevice) msg.obj);
                break;
            case BluetoothLEManager.BLE_SCAN_BEACON:
                Beacon beacon = (Beacon) msg.obj;
                break;
            case BluetoothLEManager.BLE_CONNECT_DEVICE:
                break;
            case BluetoothLEManager.BLE_SERVICES_DISCOVERED:
                if (!bleManager.notifyCharacteristic(HM_10_CONF, HM_RX_TX)) {
                    bleManager.disconnectDevice();
                }
                break;
            case BluetoothLEManager.BLE_DATA_AVAILABLE:
                onBLEReceiveMsg((String) msg.obj);
                break;
            case BluetoothLEManager.BLE_READ_CHARACTERISTIC:
                onBLEConnectedStatus(CONNECTED);
                break;
            case BluetoothLEManager.BLE_DISCONNECT_DEVICE:
                onBLEConnectedStatus(DISCONNECTED);
                break;
        }
    }
}
