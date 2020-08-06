package com.anecoz.billie;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;


public class MainActivity extends AppCompatActivity implements IAPIObserver, OnMapReadyCallback,
        ControlSwitchListener, LocationEngineCallback<LocationEngineResult> {
    private static class APIEventWrapper {
        public APIEventWrapper(APIEvent event, Response response) {
            _event = event;
            _response = response;
        }
        APIEvent _event;
        Response _response;
    }

    Handler _handler = null;
    Handler _requestHandler = null;
    HandlerThread _requestThread = null;
    boolean _running = false;

    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_PERMISSIONS = 2;

    private LocationComponent _locationComponent;
    private LocationEngine _locationEngine;
    MapboxMap _mapboxMap;
    Style _mapStyle;

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
                    //Toast.makeText(MainActivity.this, "BT Resp: " + wrapper._response._val, Toast.LENGTH_SHORT).show();
                    FragmentManager fm = getSupportFragmentManager();
                    Fragment navHost = fm.getPrimaryNavigationFragment();
                    FirstFragment fragment = (FirstFragment)navHost.getChildFragmentManager().getFragments().get(0);

                    if (fragment == null) {
                        Log.e("TAG", "Fragment is null");
                        return;
                    }

                    if (wrapper._response._command == (byte)RegMap.M365_SPEED_REG) {
                        fragment.setSpeed(wrapper._response._val);
                        Statistics.addSpeedEntry(wrapper._response._val);
                    }
                    else if (wrapper._response._command == (byte)RegMap.M365_ODOMETER_REG) {
                        fragment.setOdometer(wrapper._response._val);
                    }
                    else if (wrapper._response._command == (byte)RegMap.M365_BATT_REG) {
                        fragment.setBatteryCharge(wrapper._response._val);
                        Statistics.addBattEntry(wrapper._response._val);
                    }
                    else if (wrapper._response._command == (byte)RegMap.M365_TRIP_KM_REG) {
                        fragment.setTripKm(wrapper._response._val);
                    }
                    else if (wrapper._response._command == (byte)RegMap.BATT_VOLTAGE_REG) {
                        fragment.setBatteryVoltage(wrapper._response._val);
                    }
                    else if (wrapper._response._command == (byte)RegMap.M365_TRIPTIME_REG) {
                        fragment.setTripTime(wrapper._response._val);
                    }
                    else if (wrapper._response._command == (byte)RegMap.M365_FRAMETEMP_REG) {
                        fragment.setTemp(wrapper._response._val);
                    }
                    else if (wrapper._response._command == (byte)RegMap.M365_KM_REMAIN_REG) {
                        fragment.setRange(wrapper._response._val);
                    }
                    else if (wrapper._response._command == (byte)RegMap.M365_CHECK_TAILLIGHT_REG) {
                        fragment.setTailLight(wrapper._response._bVal);
                    }
                    else if (wrapper._response._command == (byte)RegMap.M365_CHECK_LOCK_REG) {
                        fragment.setLock(wrapper._response._bVal);
                    }
                }
            }
        };

        // Setup MapBox view
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        SupportMapFragment mapFragment;
        if (savedInstanceState == null) {
            final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            MapboxMapOptions options = MapboxMapOptions.createFromAttributes(this, null);
            options.camera(new CameraPosition.Builder()
                    .target(new LatLng(58.0, 15.0))
                    .zoom(15)
                    .build());

            mapFragment = SupportMapFragment.newInstance(options);

            transaction.add(R.id.container, mapFragment, "com.mapbox.map");
            transaction.commit();
        }
        else {
            mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentByTag("com.mapbox.map");
        }

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        _mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                _mapStyle = style;
                enableLocationComponent();
                initLocationEngine();
            }
        });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create and customize the LocationComponent's options
            LocationComponentOptions customLocationComponentOptions = LocationComponentOptions.builder(this)
                    .elevation(5)
                    .accuracyAlpha(.3f)
                    .accuracyColor(Color.RED)
                    .trackingGesturesManagement(true)
                    //.foregroundDrawable(R.drawable.android_custom_location_icon)
                    .build();

            // Get an instance of the component
            _locationComponent = _mapboxMap.getLocationComponent();

            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, _mapStyle)
                            .locationComponentOptions(customLocationComponentOptions)
                            .build();

            // Activate with options
            _locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            _locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            _locationComponent.setCameraMode(CameraMode.TRACKING_GPS);

            // Set the component's render mode
            _locationComponent.setRenderMode(RenderMode.GPS);

            // Add the location icon click listener
            //_locationComponent.addOnLocationClickListener(this);

            // Add the camera tracking listener. Fires if the map camera is manually moved.
            //_locationComponent.addOnCameraTrackingChangedListener(this);
        }
    }

    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;

    @SuppressWarnings( {"MissingPermission"})
    private void initLocationEngine() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            _locationEngine = LocationEngineProvider.getBestLocationEngine(this);

            LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

            _locationEngine.requestLocationUpdates(request, this, getMainLooper());
            _locationEngine.getLastLocation(this);
        }
    }

    @Override
    public void onTaillight(boolean value) {
        _bluetoothApi.sendMessage(createMessage(
                RegMap.MASTER_TO_M365,
                RegMap.REG_WRITE,
                RegMap.M365_SET_TAILLIGHT_REG,
                value ? RegMap.M365_SET_TAILLIGHT_ON : RegMap.M365_SET_TAILLIGHT_OFF));
    }

    @Override
    public void onLock(boolean value) {
        _bluetoothApi.sendMessage(createMessage(
                RegMap.MASTER_TO_M365,
                RegMap.REG_WRITE,
                value ? RegMap.M365_SET_LOCK_ON_REG : RegMap.M365_SET_LOCK_OFF_REG,
                RegMap.M365_SET_LOCK));
    }

    @Override
    public void onRecenter() {
        _locationComponent.setCameraMode(CameraMode.TRACKING_GPS);
    }

    @Override
    public void onLog(boolean value) {
        Statistics.setEnabled(value);
    }

    private Message createMessage(int direction, int rw, int command, int len) {
        return new Message(
                direction,
                rw,
                command,
                new byte[]{(byte)len});
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted! Setting up bluetooth.", Toast.LENGTH_SHORT).show();
                setupBluetooth();
                if (_mapboxMap != null && _mapStyle != null) {
                    enableLocationComponent();
                    initLocationEngine();
                }
            }
            else {
                Toast.makeText(this, "App cannot work without Location and Bluetooth permissions.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Statistics.cleanup();

        _running = false;
        try {
            _requestThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

        _requestThread = new HandlerThread("RequestThread");
        _requestThread.start();
        _requestHandler = new Handler(_requestThread.getLooper());

        _running = true;

        _requestHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean first = true;
                while (_running) {
                    _bluetoothApi.sendMessage(createMessage(
                            RegMap.MASTER_TO_M365,
                            RegMap.REG_READ,
                            RegMap.M365_SPEED_REG,
                            RegMap.M365_SPEED_LEN));
                    _bluetoothApi.sendMessage(createMessage(
                            RegMap.MASTER_TO_M365,
                            RegMap.REG_READ,
                            RegMap.M365_ODOMETER_REG,
                            RegMap.M365_ODOMETER_LEN));
                    _bluetoothApi.sendMessage(createMessage(
                            RegMap.MASTER_TO_M365,
                            RegMap.REG_READ,
                            RegMap.M365_TRIP_KM_REG,
                            RegMap.M365_TRIP_KM_LEN));
                    _bluetoothApi.sendMessage(createMessage(
                            RegMap.MASTER_TO_M365,
                            RegMap.REG_READ,
                            RegMap.M365_BATT_REG,
                            RegMap.M365_BATT_LEN));
                    _bluetoothApi.sendMessage(createMessage(
                            RegMap.MASTER_TO_BATT,
                            RegMap.REG_READ,
                            RegMap.BATT_VOLTAGE_REG,
                            RegMap.BATT_VOLTAGE_LEN));
                    _bluetoothApi.sendMessage(createMessage(
                            RegMap.MASTER_TO_M365,
                            RegMap.REG_READ,
                            RegMap.M365_FRAMETEMP_REG,
                            RegMap.M365_FRAMETEMP_LEN));
                    _bluetoothApi.sendMessage(createMessage(
                            RegMap.MASTER_TO_M365,
                            RegMap.REG_READ,
                            RegMap.M365_TRIPTIME_REG,
                            RegMap.M365_TRIPTIME_LEN));
                    _bluetoothApi.sendMessage(createMessage(
                            RegMap.MASTER_TO_M365,
                            RegMap.REG_READ,
                            RegMap.M365_KM_REMAIN_REG,
                            RegMap.M365_KM_REMAIN_LEN));

                    if (first) {
                        _bluetoothApi.sendMessage(createMessage(
                                RegMap.MASTER_TO_M365,
                                RegMap.REG_READ,
                                RegMap.M365_CHECK_TAILLIGHT_REG,
                                RegMap.M365_CHECK_TAILLIGHT_LEN));
                        _bluetoothApi.sendMessage(createMessage(
                                RegMap.MASTER_TO_M365,
                                RegMap.REG_READ,
                                RegMap.M365_CHECK_LOCK_REG,
                                RegMap.M365_CHECK_LOCK_LEN));
                        first = false;
                    }

                    Statistics.writeToDb();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
        Location location = result.getLastLocation();

        if (location == null) {
            return;
        }

        Statistics.addLatLngEntry(
                result.getLastLocation().getLatitude(),
                result.getLastLocation().getLongitude());
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
    }
}