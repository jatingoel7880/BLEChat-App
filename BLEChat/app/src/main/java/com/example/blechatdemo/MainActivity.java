package com.example.blechatdemo;

import static com.example.blechatdemo.server.GattService.CHAR_UUID;
import static com.example.blechatdemo.server.GattService.SERVICE_UUID;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.example.blechatdemo.databinding.ActivityMainBinding;
import com.example.blechatdemo.server.GattService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements BleDeviceAdapter.OnSelectItem, EasyPermissions.PermissionCallbacks {
    ActivityMainBinding binding;
    int BLE_PERMISSION_CODE = 201;

    private BleDeviceAdapter adapter;
    private List<BleDevice> deviceList = new ArrayList<>();
    BluetoothLeScanner btScanner;

    @Override
    protected void onStart() {
        super.onStart();
        ChatService.getInstance().currentAddress = UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File file = getFilesDir();
        System.out.println("Files Dir " + file.getPath());
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setUpAdapter();
        checkBlePermission();
        binding.scan.setOnClickListener(v -> {
            Log.e("scan btn click", "init");
            deviceList.clear();
            adapter.reload(deviceList);
            startBleScan();

        });

        initBle();
    }

    @Override
    protected void onResume() {
        super.onResume();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {

            return;
        }

        if (!checkGPSIsOpen()) {

            return;
        }
        if (isServiceRunning(GattService.class)) {
            return;
        }
        Log.d("GattService ...", "init");

        startBLE();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("onStop", "Main");
    }


    private void initBle() {
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setSplitWriteNum(1000)//new code
                .setReConnectCount(1, 5000)//sets the number of reconnection attempts and the time between attempts in milliseconds.
                .setConnectOverTime(20000)// sets the time limit in milliseconds for a connection to be established before it is considered a failure.
                .setOperateTimeout(5000);//sets the timeout in milliseconds for BLE operations such as reading and writing data.

    }

    private void setScanRule() {
        String[] uuids;
        String str_uuid = "0000b81d-0000-1000-8000-00805f9b34fb";
        if (TextUtils.isEmpty(str_uuid)) {
            uuids = null;
        } else {
            uuids = str_uuid.split(",");
        }
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                String name = uuids[i];
                String[] components = name.split("-");
                if (components.length != 5) {
                    serviceUuids[i] = null;
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i]);
                }
            }
        }

        String[] names;
        String str_name = "";
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }

        String mac = "";

        boolean isAutoConnect = false;

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                //.setServiceUuids(serviceUuids)
                .setDeviceName(true, names)
                .setDeviceMac(mac)
                .setAutoConnect(isAutoConnect)
                .setScanTimeOut(10000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);

    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void stopBleService() {
        if (isServiceRunning(GattService.class)) {
            stopService(new Intent(this, GattService.class));
        }
    }

    private void startBleServer() {
        Log.d("startBleServer", "......");
        //Toast.makeText(this,"Ble server started",Toast.LENGTH_SHORT).show();
        stopBleService();
        startService(new Intent(this, GattService.class));
    }

    private void initBleConfig() {


        BleManager.getInstance().init(getApplication());

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setSplitWriteNum(1000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);

        String[] names;
        String str_name = "";
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }

//        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
//                .setDeviceName(true, names)
//                .setAutoConnect(true)
//                .build();

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setDeviceName(true, names)
                .setAutoConnect(true)
                .setScanTimeOut(10000)
                .build();

        BleManager.getInstance().initScanRule(scanRuleConfig);


    }

    private ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.e("Adress: ", result.getDevice().getAddress());
            Log.e("RSSI: ", " rssi: " + result.getRssi());
        }
    };

    //private ScanCallback callback = new ScanCallback() {
    //        @Override
    //        public void onScanResult(int callbackType, ScanResult result) {
    //            Log.d("Adress: ", result.getDevice().getAddress());
    //            Log.d("RSSI: ", " rssi: " + result.getRssi());
    //        }
    //    };

    ActivityResultLauncher<Intent> blePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Here, no request code
                        Log.d("blePermissionLauncher", "Granted");
                        requestPermissions();
                    }
                }
            });

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    private void requestPermissions() {
        Log.d("Request.Permission", "Calling");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            //Toast.makeText(this, "Ble permission disabled", Toast.LENGTH_LONG).show();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            }
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            blePermissionLauncher.launch(enableBtIntent);
            return;
        }

        if (!checkGPSIsOpen()) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            blePermissionLauncher.launch(intent);
            return;
        }
        Log.d("startBLE ...", "...");
        startBLE();
    }

    private void startBLE() {
        setScanRule();
        startBleServer();
    }

    private void checkBlePermission() {

        if (android.os.Build.VERSION.SDK_INT > 30) {
            Log.d("Running on higher set", "....");

            String[] perms = {Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
            if (EasyPermissions.hasPermissions(this, perms)) {
                requestPermissions();
                Log.d("Allpermissions set", "....");
            } else {
                // Do not have permissions, request them now
                EasyPermissions.requestPermissions(this, getString(R.string.ble_permission_required),
                        BLE_PERMISSION_CODE, perms);
            }
        } else {
            Log.d("Running on lower set", "....");
            String[] limitedperms = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN};
            if (EasyPermissions.hasPermissions(this, limitedperms)) {
                //  startBleScan();
                //  Toast.makeText(this, "Allpermissions enabled", Toast.LENGTH_LONG).show();
                requestPermissions();
                Log.d("Allpermissions set", "....");
            } else {
                // Do not have permissions, request them now
                EasyPermissions.requestPermissions(this, getString(R.string.ble_permission_required),
                        BLE_PERMISSION_CODE, limitedperms);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
        requestPermissions();
        // ...
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        // ...
    }

    private void setCurrentUser() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            return;
        }
        ChatService.getInstance().senderName = bluetoothAdapter.getName();

    }

    Boolean isDeviceConnected = false;

    private void startBleScan() {
        Log.e("startBleScan", "init");
//        stopBleService();


        Log.d("startBleScan", "init");
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                Log.d("startBleScan", "onScanStarted" + success);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                Log.d("startBleScan", "onScanning" + bleDevice);
                deviceList.add(bleDevice);
                refresh();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                Log.d("startBleScan", "onScanFinished" + scanResultList.size());
            }


        });
    }

    private void connectDevice(BleDevice device) {
        BleManager.getInstance().connect(device, new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {

            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {

                Log.d("onConnectSuccess", "" + bleDevice.getName());
                Log.d("onConnectSuccess", " " + bleDevice.getRssi());
                BleManager.getInstance().write(
                        bleDevice, SERVICE_UUID.toString(),
                        CHAR_UUID.toString(),
                        "data".getBytes(),
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                Log.d("onWriteSuccess", "" + current);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Data sent to connected devices", Toast.LENGTH_LONG).show();

                                    }
                                });

                            }

                            @Override
                            public void onWriteFailure(BleException exception) {
                                Log.d("exception", exception.getDescription());
                                Log.d("exception", "" + exception);
                                Toast.makeText(MainActivity.this, exception.toString(), Toast.LENGTH_LONG).show();

                            }
                        });


              /*  List<BluetoothGattService> services = BleManager.getInstance().getBluetoothGattServices(bleDevice);
                for (BluetoothGattService item : services) {
                    Log.d("Services", "" + item.getIncludedServices().size());
                    Log.d("Services", "" + item.getUuid());
                    List<BluetoothGattCharacteristic> characteristicsList = BleManager.getInstance().getBluetoothGattCharacteristics(item);
                    for (BluetoothGattCharacteristic chatItem : characteristicsList) {
                        Log.d("chatItem", "" + chatItem);
                        Log.d("chatItem", "" + chatItem.getUuid());
                        Log.d("chatItem", "" + chatItem.getService().getUuid());


                    }
                } */


//                    readRssi(bleDevice);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {

            }
        });
    }

    private void setUpAdapter() {
        adapter = new BleDeviceAdapter(deviceList, this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);
        binding.listview.setLayoutManager(manager);
        binding.listview.setAdapter(adapter);


    }

    private void showLoader() {

    }

    private void refresh() {
        adapter.reload(deviceList);
    }

    @Override
    public void onDeviceSelected(BleDevice device) {
        binding.loader.setVisibility(View.VISIBLE);
        if (!BleManager.getInstance().isConnected(device)) {
            // BleManager.getInstance().cancelScan();

            ChatService.getInstance().connectDevice(device, new BleGattCallback() {
                @Override
                public void onStartConnect() {
                    Log.d("onStartConnect", "" + device.getName());

                }

                @Override
                public void onConnectFail(BleDevice bleDevice, BleException exception) {
                    Log.d("onConnectFail", "" + exception);
                    binding.loader.setVisibility(View.GONE);


                }

                @Override
                public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                    Log.d("onConnectSuccess", "" + bleDevice.getName() + " ");
                    binding.loader.setVisibility(View.GONE);
                    ChatService.getInstance().connectedDevice = bleDevice;
                    goToChat();
                }

                @Override
                public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                    Log.d("onDisConnected", "" + isActiveDisConnected);

                }
            });
        } else {
            goToChat();
        }

    }

    private void goToChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }
}