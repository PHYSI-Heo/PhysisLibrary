package com.physis.dev.library.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.Objects;

public class BluetoothLEManager {

    private static final String TAG = BluetoothLEManager.class.getSimpleName();

    public static final int BLE_SCAN_START = 101;
    public static final int BLE_SCAN_STOP = 102;
    public static final int BLE_SCAN_DEVICE = 103;
    public static final int BLE_SCAN_BEACON = 104;
    public static final int BLE_CONNECT_DEVICE = 105;
    public static final int BLE_DISCONNECT_DEVICE = 106;
    public static final int BLE_SERVICES_DISCOVERED = 107;
    public static final int BLE_DATA_AVAILABLE = 108;
    public static final int BLE_READ_CHARACTERISTIC = 109;

    private static BluetoothLEManager bluetoothLEManager = null;

    private Context context;
    private Handler bleHandler = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;

    private BluetoothLEService bluetoothLEService = null;

    private long scanTime = 5000;
    private boolean isScanning = false;
    private boolean isBinding = false;
    private boolean isBroadcastRegister = false;


    private BluetoothLEManager(Context context){
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public synchronized static BluetoothLEManager getInstance(Context context){
        if(bluetoothLEManager == null)
            bluetoothLEManager = new BluetoothLEManager(context);
        return bluetoothLEManager;
    }

    public boolean checkBleSupport() {
        return bluetoothAdapter != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    public void setScanTime(long time){
        scanTime = time;
    }

    public void setHandler(Handler handler){
        bleHandler = handler;
    }

    private void startBLEScan(){
        isScanning = true;
        bluetoothLeScanner.startScan(scanCallback);
        bleHandler.obtainMessage(BLE_SCAN_START).sendToTarget();
        Log.e(TAG, "# Start Bluetooth LE Scan..");
    }

    private void stopBLEScan(){
        isScanning = false;
        bluetoothLeScanner.stopScan(scanCallback);
        bleHandler.removeCallbacks(scanStopRunnable);
        bleHandler.obtainMessage(BLE_SCAN_STOP).sendToTarget();
        Log.e(TAG, "# Stop Bluetooth LE Scan..");
    }

    @SuppressLint("NewApi")
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Beacon beacon = Beacon.fromScanData(Objects.requireNonNull(result.getScanRecord()).getBytes(), result.getRssi(), result.getDevice());
            if(beacon !=null) {
                bleHandler.obtainMessage(BLE_SCAN_BEACON, beacon).sendToTarget();
            }else{
                bleHandler.obtainMessage(BLE_SCAN_DEVICE, result.getDevice()).sendToTarget();
            }
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {

        }
        @Override
        public void onScanFailed(int errorCode) {

        }
    };

    public void scan(boolean isScan, boolean isTimer){
        if(isScan){
            if(isScanning)
                return;
            startBLEScan();
            if(isTimer)
                bleHandler.postDelayed(scanStopRunnable, scanTime);
        }else{
            if(isScanning) stopBLEScan();
        }
    }

    private Runnable scanStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopBLEScan();
        }
    };

    public void bindService(){
        if(isBinding)
            return;
        Intent gattServiceIntent = new Intent(context, BluetoothLEService.class);
        context.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unBindService(){
        if(!isBinding)
            return;
        try{
            isBinding = false;
            context.unbindService(serviceConnection);
        }catch (Exception e){
            e.getStackTrace();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "Bluetooth Service Connected..");
            bluetoothLEService = ((BluetoothLEService.ServiceBinder) iBinder).getBLEService();
            bluetoothLEService.initialize();
            isBinding = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "Bluetooth Service DisConnected..");
            bluetoothLEService = null;
            isBinding = false;
        }
    };

    public void connectDevice(BluetoothDevice device){
        if(!isBinding || bluetoothLEService == null)
            return;
        bluetoothLEService.connectGatt(device);
    }

    public void disconnectDevice(){
        if(!isBinding || bluetoothLEService == null)
            return;
        bluetoothLEService.disconnectGatt();
    }

    public List<String> getGattServices(){
        if(!isBinding || bluetoothLEService == null)
            return null;
        return bluetoothLEService.getServiceUUID();
    }

    public List<String> getGattCharacteristics(String serviceUUID){
        if(!isBinding || bluetoothLEService == null)
            return null;
        return bluetoothLEService.getCharacteristicUUID(serviceUUID);
    }

    public boolean notifyCharacteristic(String serviceUUID, String charUUID){
        return bluetoothLEService.setNotifyCharacteristic(serviceUUID, charUUID);
    }

    public void writeCharacteristic(String serviceUUID, String charUUID, String writeData){
        bluetoothLEService.writeCharacteristic(serviceUUID, charUUID, writeData);
    }

    public void registerReceiver(){
        if(isBroadcastRegister)
            return;
        context.registerReceiver(gattReceiver, BluetoothLEService.gattIntentFilter());
        isBroadcastRegister = true;
    }

    public void unregisterReceiver(){
        if(!isBroadcastRegister)
            return;
        context.unregisterReceiver(gattReceiver);
        isBroadcastRegister = false;
    }

    private BroadcastReceiver gattReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e(TAG, "Broadcast Action : " + action);

            if(action == null)
                return;

            switch (action) {
                case BluetoothLEService.ACTION_GATT_CONNECTED:
                    bleHandler.obtainMessage(BLE_CONNECT_DEVICE).sendToTarget();
                    break;
                case BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED:
                    bleHandler.obtainMessage(BLE_SERVICES_DISCOVERED).sendToTarget();
                    break;
                case BluetoothLEService.ACTION_CHARACTERISTIC_READ:
                    bleHandler.obtainMessage(BLE_READ_CHARACTERISTIC).sendToTarget();
                    break;
                case BluetoothLEService.ACTION_DATA_AVAILABLE:
                    bleHandler.obtainMessage(BLE_DATA_AVAILABLE,
                            intent.getStringExtra(BluetoothLEService.EXTRA_KEY_CHANGE_VALUE)).sendToTarget();
                    break;
                case BluetoothLEService.ACTION_GATT_DISCONNECTED:
                    bleHandler.obtainMessage(BLE_DISCONNECT_DEVICE).sendToTarget();
                    break;
            }
        }
    };
}
