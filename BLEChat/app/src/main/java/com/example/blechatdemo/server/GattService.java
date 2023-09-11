package com.example.blechatdemo.server;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.clj.fastble.BleManager;
import com.example.blechatdemo.ChatService;
import com.example.blechatdemo.file.FileHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GattService extends Service {
    private static int NOTIFICATION_ID = 1000;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer server;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private boolean start;
    private String channelName;


    public static final ParcelUuid UUID_DEV = ParcelUuid.fromString("00001111-0000-1000-8000-00805F9B34fb");
    public static final java.util.UUID SERVICE_UUID = java.util.UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb");
    public static final java.util.UUID CHAR_UUID = java.util.UUID.fromString("36d4dc5c-814b-4097-a5a6-b93b39085928");



    @Override
    public void onCreate() {
        super.onCreate();
        setupBluetooth();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return Service.START_STICKY;
    }

    private void setupBluetooth() {
        try {
            // localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
            BluetoothManager bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this.getApplicationContext(), "setupBluetooth,Unable to connect, Permission error", Toast.LENGTH_SHORT).show();

                //  return;
            }
            Log.e("setupBluetooth", "onGattServer");
            server = bluetoothManager.openGattServer(this, serverCallback);
            initServer();
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothAdapter.setName(bluetoothAdapter.getName());
            advertise();
        } catch (Exception e) {
            Toast.makeText(this.getApplicationContext(), "Unable to connect, please try again", Toast.LENGTH_SHORT).show();
        }

    }

    private void initServer() {
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(CHAR_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristic);
        if (server.getServices() != null && server.getServices().size() > 0) {
            server.getServices().clear();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this.getApplicationContext(), "initServer, Permission error", Toast.LENGTH_SHORT).show();

           // return;
        }
        server.addService(service);

    }

    private void advertise() {

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseData advertisementData = getAdvertisementData();
        AdvertiseSettings advertiseSettings = getAdvertiseSettings();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this.getApplicationContext(), "advertise, Permission error", Toast.LENGTH_SHORT).show();

            //return;
        }
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertisementData, advertiseCallback);
        start = true;
        Log.d("GattServer", "startedAdvertise");
    }

    private byte[] getDeviceToken() {
        try {
            String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            Log.d("GATT", "DeviceToken : " + getDeviceToken());
            return "DEV".getBytes("UTF-8");

        } catch (Exception e) {
            Log.d("GATT", "Failed to complete token refresh", e);
        }
        return null;
    }

    private AdvertiseData getAdvertisementData() {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeTxPowerLevel(true);
        builder.addServiceUuid(UUID_DEV);
        builder.addServiceData(UUID_DEV,"D".getBytes()); //recently undoubleslashed
//                    .addServiceData( pServiceDataUuid, "D".getBytes() )

        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
            }
      }

//        bluetoothAdapter.setName("BLE_CHAT");

//      bluetoothAdapter.setName(com.tjouin.staytouch.Controllers.Main.Bluetooth.BleManager.getInstance().unique_android_channel_name);
        builder.setIncludeDeviceName(true);
        return builder.build();
    }
    public String getLocalBluetoothName() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this.getApplicationContext(), "getLocalBluetoothName, Permission error", Toast.LENGTH_SHORT).show();

            return "";
        }
        channelName = bluetoothAdapter.getName();
        //if(channelName == null){
        System.out.println("Name is null!");
        channelName = "BLE-test " + (int) (Math.random() * 1000);


        sendSignal("CHANNEL_CREATED");


        return channelName;
    }

    private AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        builder.setConnectable(true);
        return builder.build();
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @SuppressLint("Override")
        @Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {

            final String message = "BLE Advertisement successful";
            showmessagedata(message);
            sendNotification(message);
            Log.d("GATTSERVICE", "AdvertiseCallback onStartSuccess " + advertiseSettings.toString());
            sendSignal("SERVICE_STARTED");
        }

        @SuppressLint("Override")
        @Override
        public void onStartFailure(int i) {
            final String message = "BLE Advertisement failed error code: " + i;
            showmessagedata(message);
            sendNotification(message);
            Log.d("GATTSERVICE", "AdvertiseCallback onStartFailure " + i);
//            setupBluetooth();

        }

    };

    private BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d("GATTSERVICE", "onConnectionStateChange" + status + " device "+device + " newState "+newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                sendNotification("Client connected");

            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("GATTSERVICE", "BluetoothProfile.STATE_DISCONNECTED");
            }
            if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.d("GATTSERVICE", "BluetoothProfile.STATE_CONNECTING");
            }

        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.d("GATTSERVICE", "onServiceAdded "+ service.getUuid());

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.v("CharacteristicReadReq", "called");
        }

        List<byte[]> receivedBytes = new ArrayList<>();//new code

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.v("onWriteRequest", "called");
            //The value of the characteristic write request is retrieved and stored in the bytes array.

            byte[] bytes = value;
            Log.d("Server.received.bytes","" + bytes.length);

            //new code

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

               // return;
            }
            server.sendResponse(device, requestId, 0, offset, value);
            // server.sendResponse to send a response to the write request. It acknowledges the write request and indicates success.

            // handling the received message
            String message = new String(bytes);
            Log.d("jsonObject:message","" + message);
            Boolean isTextMsg = false;
            int fileTotalSize = 0;
            String textContent = "";
            //The received bytes are converted to a String message.
            try {
                //attempts to parse the received message as a JSON object.
                // If it succeeds, it assumes the message is a text message and extracts the "txt" field from the
                // JSON object.
                JSONObject jsonObject = new JSONObject(message);
                Log.d("jsonObject:read","" + jsonObject);
                isTextMsg = true;
                textContent = jsonObject.getString("txt");
            } catch (JSONException e) {
                e.printStackTrace();
                isTextMsg = false;
                Log.d("File.first.msg","" + message);


            }
            Log.d("jsonObject:isTextMsg","" + isTextMsg);

            if(isTextMsg) {
                if (ChatService.getInstance().getChatCallbacks() != null){
                    ChatService.getInstance().getChatCallbacks().onMessageReceived(textContent);
                }
            }else{
                if(message.equals("#FILE_INIT")) {
                    receivedBytes.clear();
                    //If the parsing fails or it is not a text message, the code checks if the received message
                    // is "#FILE_INIT". If it is, it clears the receivedBytes list.
                }
                if(message.equals("#FILE_END")) {
                    //If the received message is "#FILE_END", it calculates the total size of the received bytes,
                    // combines them into a single byte array
                    //When the #FILE_END command is received, the code calculates the total size of the received
                    // bytes by iterating over the receivedBytes list and summing the lengths of each byte array
                    int totalBytes = 0;
                    for (int i = 0; i < receivedBytes.size(); ++i)
                    {
                        byte[] receivedItem = receivedBytes.get(i);
                        totalBytes += receivedItem.length;

                    }
                    Log.d("totalBytes:received","" + totalBytes);

                    byte[] allByteArray = new byte[totalBytes];
                    ByteBuffer buff = ByteBuffer.wrap(allByteArray);
                    for (int i = 0; i < receivedBytes.size(); ++i)
                    {
                        buff.put(receivedBytes.get(i));
                    }
                    byte[] combined = buff.array();
                    String path = FileHelper.saveFile(combined,GattService.this);
                    if (path != null){
                        if (ChatService.getInstance().getChatCallbacks() != null){
                            ChatService.getInstance().getChatCallbacks().onFileReceived(path);
                            //Finally, the combined byte array is passed to the FileHelper.saveFile method to save
                        }
                    }
                    receivedBytes.clear();
                }
                else {
                    receivedBytes.add(bytes);
                }
                Log.d("receivedBytes:size","" + receivedBytes.size());
                //The received chunks of the file are stored in the receivedBytes list (ArrayList<byte[]>),
                // which is cleared when the #FILE_INIT command is received. Each received chunk is added to the
                // list using the receivedBytes.add(bytes) statement.


            }

// Check if all file bytes have been received
            //message.equals("#FILE_END") is placed after adding the bytes to the receivedBytes list to ensure that the file chunks are processed correctly.
            if (message.equals("#FILE_END")) {
                int totalBytes = 0;
                for (byte[] receivedItem : receivedBytes) {
                    totalBytes += receivedItem.length;
                }
                Log.d("totalBytes:received", "" + totalBytes);

                // allByteArray is created with the exact length of totalBytes. The individual byte arrays from receivedBytes are
                // copied into allByteArray using System.arraycopy, ensuring that the file bytes are combined in the correct order.
                byte[] allByteArray = new byte[totalBytes];
                int currentIndex = 0;
                for (byte[] receivedItem : receivedBytes) {
                    System.arraycopy(receivedItem, 0, allByteArray, currentIndex, receivedItem.length);
                    currentIndex += receivedItem.length;
                }
                //Finally, the allByteArray is passed to FileHelper.saveFile to save the file, and if the file is successfully saved,
                // the ChatCallbacks are notified about the received file using onFileReceived. The receivedBytes list is cleared after
                // processing the file.
                String path = FileHelper.saveFile(allByteArray, GattService.this);
                if (path != null) {
                    if (ChatService.getInstance().getChatCallbacks() != null) {
                        ChatService.getInstance().getChatCallbacks().onFileReceived(path);
                    }
                }
                receivedBytes.clear();
            }



        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.v("onDescriptorReadRequest", "called");
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            Log.v("DescriptorWriteRequest", "called");
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            Log.d("GATTSERVICE", "onExecuteWrite");

        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.d("GATTSERVICE", "onNotificationSent");
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.v("onMtuChanged", "called");
        }


    };


    @Override
    public void onDestroy() {
        Log.d("GATTSERVICE", "onDestroy"); //new code
        if (start) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this.getApplicationContext(), "onDestroy, Permission error", Toast.LENGTH_SHORT).show();

                return;
            }
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            server.clearServices();
            server.close();
            BleManager.getInstance().disconnectAllDevice();
            BleManager.getInstance().destroy();

            sendNotification("Close");
        }
        super.onDestroy();
    }

    private void sendNotification(String message) {
//        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
//
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this, "ble")
//                        .setSmallIcon(R.drawable.ic_launcher_background)
//                        .setContentTitle(getString(R.string.app_name))
//                        .setAutoCancel(true)
//                        .setContentText(message)
//                        .setChannelId(channelName);
//
//        Notification notification = mBuilder.build();
////        notification.defaults |= Notification.DEFAULT_VIBRATE;
////        notification.defaults |= Notification.DEFAULT_SOUND;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(channelName, "ble_master", NotificationManager.IMPORTANCE_DEFAULT);
//            // Register the channel with the system; you can't change the importance
//            // or other notification behaviors after this
//            NotificationManager notificationManager = (NotificationManager)
//                    this.getSystemService(Context.NOTIFICATION_SERVICE);
//            notificationManager.createNotificationChannel(channel);
//            notificationManager.notify((int) (NOTIFICATION_ID * Math.random()), notification);
//        } else {
//
//            mNotificationManager.notify((int) (NOTIFICATION_ID * Math.random()), notification);
//        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendSignal(String msg){
        Intent localIntent = new Intent("service");
        localIntent.putExtra("msg", msg);
      /*  try{
            localBroadcastManager.sendBroadcast(localIntent);
        }catch (Exception er){
            Log.e("GATTSERVICE",er.getLocalizedMessage());
        } */


    }

    private void sendBroadCast(String msg) {
        Intent localIntent = new Intent("update");
        localIntent.putExtra("msg", msg);
       // localBroadcastManager.sendBroadcast(localIntent);
    }

    private void showmessagedata(String message) {
    }
}

