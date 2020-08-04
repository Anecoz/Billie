package com.anecoz.billie;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class BluetoothAPI extends BluetoothGattCallback {
    private final String TAG = "BluetoothAPI";

    private final String WRITE_CHARACTERISTIC_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private final String READ_CHARACTERISTIC_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private final String NORDIC_SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private final String NOTIFY_DESCRIPTOR_UUID ="00002902-0000-1000-8000-00805f9b34fb";

    private Context _context;

    private BluetoothDevice _device = null;
    private BluetoothGatt _gatt = null;

    boolean _running = false;
    private Deque<Message> _requestQueue = new LinkedBlockingDeque<>();
    private Deque<Response> _responseQueue = new LinkedBlockingDeque<>();
    private Message _outgoingMessage = null;
    private long _timeSinceLastReq = 0;

    private ArrayList<IAPIObserver> _observers = new ArrayList<>();

    private BluetoothGattCharacteristic _readChar = null;
    private BluetoothGattCharacteristic _writeChar = null;

    private BluetoothAPIResponseParser _parser = new BluetoothAPIResponseParser();

    private boolean _initialized = false;

    public BluetoothAPI(Context context, BluetoothDevice device) {
        _context = context;
        _device = device;
        _device.connectGatt(_context, false, this);

        _running = true;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (_running) {
                    if (!_initialized) continue;

                    while (!_responseQueue.isEmpty()) {
                        Response response = _responseQueue.remove();
                        notifyObservers(APIEvent.Response, response);
                    }

                    if (_outgoingMessage != null) {
                        long now = System.currentTimeMillis();
                        if (now - _timeSinceLastReq > 500) {
                            // Override and consider this a lost request
                            _outgoingMessage = null;
                        }
                        else {
                            continue;
                        }
                    }

                    if (!_requestQueue.isEmpty()) {
                        _outgoingMessage = _requestQueue.remove();
                        _writeChar.setValue(_outgoingMessage.getData());
                        _gatt.writeCharacteristic(_writeChar);
                        _timeSinceLastReq = System.currentTimeMillis();
                    }
                }
            }
        };

        ExecutorService schTaskEx = Executors.newFixedThreadPool(1);
        schTaskEx.execute(r);
    }

    public void registerObserver(IAPIObserver observer) {
        _observers.add(observer);
    }

    private void notifyObservers(APIEvent event, Response response) {
        for (IAPIObserver observer: _observers) {
            observer.onAPIEvent(event, response);
        }
    }

    public void cleanup() {
        _running = false;
        if (_gatt != null) _gatt.disconnect();
    }

    public void sendMessage(Message message) {
        if (msgTypeInQueue(message._command)) return;
        _requestQueue.add(message);
        Log.i(TAG, "Req queue size: " + _requestQueue.size());
    }

    private boolean msgTypeInQueue(int command) {
        for (Message msg : _requestQueue) {
            if (msg._command == command) return true;
        }
        return false;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Log.i(TAG, "onConnectionStateChange, status: " + status + ", newState: " + newState);

        switch (newState) {
            case STATE_CONNECTED:
                Log.i(TAG, "STATE_CONNECTED");
                break;
            case STATE_DISCONNECTED:
                Log.i(TAG, "STATE_DISCONNECTED");
                break;
        }

        if (status == GATT_SUCCESS && newState == STATE_CONNECTED) {
            if (!_initialized) {
                gatt.discoverServices();
            }
        }
        else {
            Log.e(TAG, "Connection status is not ok");
            _initialized = false;
            notifyObservers(APIEvent.Disconnected, null);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.i(TAG, "onServicesDiscovered, status: " + status);

        if (status == GATT_SUCCESS) {
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service: services) {
                if (service.getUuid().toString().equals(NORDIC_SERVICE_UUID)) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        Log.println(Log.INFO, TAG, "Characteristic uuid: " + characteristic.getUuid().toString());
                        if (characteristic.getUuid().toString().equals(READ_CHARACTERISTIC_UUID)) {
                            _readChar = characteristic;
                        }
                        else if (characteristic.getUuid().toString().equals(WRITE_CHARACTERISTIC_UUID)) {
                           _writeChar = characteristic;
                        }
                    }

                    if (_readChar == null || _writeChar == null) {
                        Log.e(TAG, "Something went wrong with discovering service/characteristics");
                        return;
                    }

                    if (gatt.setCharacteristicNotification(_readChar, true)) {
                        UUID uuid = UUID.fromString(NOTIFY_DESCRIPTOR_UUID);
                        BluetoothGattDescriptor descriptor = _readChar.getDescriptor(uuid);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                    else {
                        Log.i(TAG, "Could not Set notifications to true on read char");
                    }
                }
            }
        }
        else {
            Log.e(TAG, "Could not discover services");
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        Log.i(TAG, "onCharacteristicRead, status: " + status);

    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Log.i(TAG, "onCharacteristicWrite, status: " + status + ", val: " + Arrays.toString(characteristic.getValue()));
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Log.i(TAG, "onCharacteristicChanged, val: " + Arrays.toString(characteristic.getValue()));
        _responseQueue.add(_parser.parse(characteristic.getValue()));
        _outgoingMessage = null;
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.i(TAG, "onDescriptorWrite, status: " + status);

        if (!_initialized && descriptor.getUuid().toString().equals(NOTIFY_DESCRIPTOR_UUID)) {
            Log.i(TAG, "Bluetooth API initialized");
            _initialized  = true;
            _gatt = gatt;
            notifyObservers(APIEvent.Connected, null);
        }
    }
}
