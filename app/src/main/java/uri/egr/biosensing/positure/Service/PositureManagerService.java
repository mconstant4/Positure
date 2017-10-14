package uri.egr.biosensing.positure.Service;

import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import uri.egr.biosensing.positure.GattAttributes.GattCharacteristics;
import uri.egr.biosensing.positure.GattAttributes.GattServices;
import uri.egr.biosensing.positure.Receivers.PositureReceiverService;

/**
 * Created by mcons on 3/11/2017.
 */

public class PositureManagerService extends Service {
    public static final String ACTION_CONNECT = "uri.egr.vapegate.connect";
    public static final String ACTION_DISCONNECT = "uri.egr.vapegate.disconnect";
    public static final String ACTION_REQUEST_READ = "uri.egr.vapegate.read";

    private boolean mServiceBound;
    private BluetoothLeService mService;
    private ServiceConnection mServiceConnection;

    private BroadcastReceiver mBLEUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BLE", "Received Update");
            String action = intent.getStringExtra(BluetoothLeService.INTENT_EXTRA);
            switch (action) {
                case BluetoothLeService.GATT_STATE_CONNECTED:
                    mService.discoverServices();
                    break;
                case BluetoothLeService.GATT_STATE_DISCONNECTED:
                    Intent updateIntent2 = new Intent(PositureReceiverService.POSITURE_INTENT_FILTER.getAction(0));
                    updateIntent2.putExtra(PositureReceiverService.EXTRA_CONNECTION_UPDATE, false);
                    sendBroadcast(updateIntent2);
                    break;
                case BluetoothLeService.GATT_DISCOVERED_SERVICES:
                    BluetoothGattCharacteristic characteristic = mService.getCharacteristic(GattServices.POSITURE_SERVICE, GattCharacteristics.BACK_LEFT_CHARACTERISTIC);
                    if (characteristic != null) {
                        mService.enableNotifications(characteristic);
                    }
                    Intent updateIntent1 = new Intent(PositureReceiverService.POSITURE_INTENT_FILTER.getAction(0));
                    updateIntent1.putExtra(PositureReceiverService.EXTRA_CONNECTION_UPDATE, true);
                    sendBroadcast(updateIntent1);
                    break;
                case BluetoothLeService.GATT_CHARACTERISTIC_READ:
                    byte[] data = intent.getByteArrayExtra(BluetoothLeService.INTENT_DATA);
                    Intent updateIntent = new Intent(PositureReceiverService.POSITURE_INTENT_FILTER.getAction(0));
                    updateIntent.putExtra(PositureReceiverService.EXTRA_READ_UPDATE, data);
                    sendBroadcast(updateIntent);

                    //Update daily hits


                    //Send to Watson

                    break;
                case BluetoothLeService.GATT_DESCRIPTOR_WRITE:
                    break;
                case BluetoothLeService.GATT_NOTIFICATION_TOGGLED:
                    break;
                case BluetoothLeService.GATT_DEVICE_INFO_READ:
                    break;
            }
        }
    };

    public static void connect(Context context) {
        Intent intent = new Intent(context, PositureManagerService.class);
        intent.setAction(ACTION_CONNECT);
        context.startService(intent);
    }

    public static void disconnect(Context context) {
        Intent intent = new Intent(context, PositureManagerService.class);
        intent.setAction(ACTION_DISCONNECT);
        context.startService(intent);
    }

    public static void requestRead(Context context) {
        Intent intent = new Intent(context, PositureManagerService.class);
        intent.setAction(ACTION_REQUEST_READ);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mServiceConnection = new BleServiceConnection();

        registerReceiver(mBLEUpdateReceiver, new IntentFilter(BluetoothLeService.INTENT_FILTER_STRING));
        bindService(new Intent(this, BluetoothLeService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        if (intent == null) {
            return START_REDELIVER_INTENT;
        }

        switch (intent.getAction()) {
            case ACTION_CONNECT:
                connect();
                break;
            case ACTION_DISCONNECT:
                disconnect();
                break;
            case ACTION_REQUEST_READ:
                requestRead();
                break;
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        unbindService(mServiceConnection);
        unregisterReceiver(mBLEUpdateReceiver);

        super.onDestroy();
    }

    private void connect() {
        if (mServiceBound) {
            mService.connect();
        } else {
            log("Cannot Connect - BLE Connection Service is not bound yet!");
        }
    }

    private void disconnect() {
        if (mServiceBound) {
            mService.disconnect();
        } else {
            log("Could not Disconnect - BLE Connection Service is not bound!");
        }
    }

    private void requestRead() {
        if (mServiceBound) {
            BluetoothGattCharacteristic characteristic = mService.getCharacteristic(GattServices.POSITURE_SERVICE, GattCharacteristics.BACK_LEFT_CHARACTERISTIC);
            byte[] readValue = characteristic.getValue();
            Intent intent = new Intent(PositureReceiverService.POSITURE_INTENT_FILTER.getAction(0));
            intent.putExtra(PositureReceiverService.EXTRA_READ_UPDATE, readValue);
            sendBroadcast(intent);
        } else {
            log("Could not read from Vape - BLE Connection Service is not bound!");
        }
    }

    private void log(String message) {
        Log.d("VapeGateManager", message);
    }

    private class BleServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mServiceBound = true;
            mService = ((BluetoothLeService.BLEConnectionBinder) iBinder).getService();
            mService.connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mServiceBound = false;
        }
    }
}