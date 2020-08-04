package com.anecoz.billie;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements IAPIObserver {
    private static class APIEventWrapper {
        public APIEventWrapper(APIEvent event, Response response) {
            _event = event;
            _response = response;
        }
        APIEvent _event;
        Response _response;
    }

    Handler _handler = null;

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_PERMISSIONS = 2;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothAPI _bluetoothApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Check permissions
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
            setupBluetooth();
        }
        else {
            Log.println(Log.INFO, "TAG", "Permissions not set");
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_PERMISSIONS);
        }

        // Handler for API messages, runs on UI thread
        _handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull android.os.Message msg) {
                super.handleMessage(msg);

                APIEventWrapper wrapper = (APIEventWrapper) msg.obj;
                if (wrapper == null) return;

                if (wrapper._event == APIEvent.Connected) {
                    Toast.makeText(MainActivity.this, "Connected to bluetooth API", Toast.LENGTH_SHORT).show();
                }
                else if (wrapper._event == APIEvent.Disconnected) {
                    Toast.makeText(MainActivity.this, "Disconnected from Bluetooth API", Toast.LENGTH_SHORT).show();
                }
                else if (wrapper._event == APIEvent.Response) {
                    Toast.makeText(MainActivity.this, "BT Resp: " + wrapper._response._val, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted! Setting up bluetooth.", Toast.LENGTH_SHORT).show();
                setupBluetooth();
            }
            else {
                Toast.makeText(this, "App cannot work without Location and Bluetooth permissions.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (_bluetoothApi != null) {
            _bluetoothApi.cleanup();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAPIEvent(APIEvent event, Response response) {
        APIEventWrapper wrapper = new APIEventWrapper(event, response);
        android.os.Message msg =  _handler.obtainMessage(0, wrapper);
        msg.sendToTarget();
    }

    private void setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.println(Log.INFO, "TAG", "Bluetooth supported");

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            connectBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.println(Log.INFO, "TAG", "Bluetooth is now enabled");
                connectBluetooth();
            }
            else {
                Toast.makeText(this, "Bluetooth needs to be enabled for the app to work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Message createMessage() {
        return new Message(
                RegMap.MASTER_TO_BATT,
                RegMap.REG_READ,
                RegMap.BATT_CURRENT_REG,
                new byte[]{(byte)(RegMap.BATT_CURRENT_LEN)});
    }

    private void connectBluetooth() {
        /*final BluetoothLeScanner leScanner = bluetoothAdapter.getBluetoothLeScanner();

        leScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                String deviceName = result.getDevice().getName();
                String macAddr = result.getDevice().getAddress();

                Log.println(Log.INFO, "TAG", "Found LE bluetooth device. Name: " + deviceName + ", MAC Addr: " + macAddr);
            }
        });*/

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice("CA:C7:66:66:5D:89");
        _bluetoothApi = new BluetoothAPI(this, device);
        _bluetoothApi.registerObserver(this);

        _bluetoothApi.sendMessage(createMessage());
    }
}