package com.example.blechatdemo;

import static com.example.blechatdemo.server.GattService.CHAR_UUID;
import static com.example.blechatdemo.server.GattService.SERVICE_UUID;

import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ChatService {

    private static ChatService INSTANCE;
    public String currentAddress;
    public String senderName;
    public static ChatService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ChatService();
        }
        return INSTANCE;
    }

    public BleDevice connectedDevice;
    private ChatListener listener;

    public void disconnect() {
        Log.e("ChatService","disconnect...");//newly added
        if (connectedDevice == null){
            return;
        }
        BleManager.getInstance().disconnect(connectedDevice);
    }

    public interface ChatListener {
        void onMessageReceived(String text);
        void onFileReceived(String path);//newly added code
    }

    public void setChatCallback(ChatListener listener){
        this.listener = listener;
    }
    public ChatListener getChatCallbacks(){
        return listener;
    }

    public void connectDevice(BleDevice device,BleGattCallback callback){
        BleManager.getInstance().connect(device,callback);
    }
    public void sendMessage(String text,BleWriteCallback callback){
        if (connectedDevice == null){
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("txt",text);
            // jsonObject.put("typ","1");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        byte[] data = jsonObject.toString().getBytes();
        BleManager.getInstance().write(
                connectedDevice,SERVICE_UUID.toString(),
                CHAR_UUID.toString(),
                data,
                callback);
      //  if (callback == null) {
        //            Log.e("ChatService", "BleWriteCallback is null");
        //            return;
        //        }
        //        // here convert file to bytes
        //        byte[] data = text.getBytes();
        //        BleManager.getInstance().write(
        //                connectedDevice,SERVICE_UUID.toString(),
        //                CHAR_UUID.toString(),
        //                data,
        //              callback);
    }
    //new code
    public void sendFileNotificationMessage(byte[] data,BleWriteCallback callback){
        if (connectedDevice == null){
            return;
        }

        BleManager.getInstance().write(
                connectedDevice,SERVICE_UUID.toString(),
                CHAR_UUID.toString(),
                data,true,
                callback);
    }

    public void sendFileMessage(
           byte[] data , BleWriteCallback callback){ //byte[] data instead of File file
        if (connectedDevice == null){
            return;
        }

        //if (callback == null) {
        //            Log.e("ChatService", "BleWriteCallback is null");
        //            return;
        //        }
        // here convert file to bytes
        // after
        //byte[] data = "text".getBytes(); // pass bytes data here
        BleManager.getInstance().write(
                connectedDevice,SERVICE_UUID.toString(),
                CHAR_UUID.toString(),
                data,true,
                callback);
    }
    }
