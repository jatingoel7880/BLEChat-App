package com.example.blechatdemo;

import android.bluetooth.BluetoothGatt;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.blechatdemo.databinding.BleDeviceItemBinding;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceAdapter.Bleholder> {
    List<BleDevice> deviceList;
    public BleDeviceAdapter(List<BleDevice> data,OnSelectItem listener) {
        this.deviceList = data;
        this.callback = listener;
    }
    OnSelectItem callback;
    interface OnSelectItem {
        void onDeviceSelected(BleDevice device);
    }

    @NonNull
    @Override
    public Bleholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BleDeviceItemBinding binding = BleDeviceItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new Bleholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Bleholder holder, int position) {
        BleDevice device = deviceList.get(position);
        if (!TextUtils.isEmpty(device.getName())) {
            holder.binding.deviceName.setText(device.getName());
        } else if (!TextUtils.isEmpty(device.getDevice().getAddress())) {
            holder.binding.deviceName.setText(device.getDevice().getAddress());
        }
        String RSSI = String.valueOf(device.getRssi());
        holder.binding.address.setText("RSSI: ".concat(RSSI));//printing rssi on device
        int rssi= Integer.parseInt(String.valueOf(device.getRssi()));
        double distance = calculateDistance(rssi);
        holder.binding.distance.setText("Distance: ".concat(String.valueOf(distance)).concat("m"));
        holder.binding.rootView.setOnClickListener(v->{
            callback.onDeviceSelected(deviceList.get(position));
        });
    }
    double calculateDistance(int rssi) {
        double signalPropagationConstant = 2.7;
        int measuredPower = -70;
        double distance = Math.pow(10, ((measuredPower - rssi) / (10 * signalPropagationConstant)));
        return Math.round(distance * 100.0) / 100.0; // round to two decimal places
    }

    private void connect(BleDevice device){

        BleManager.getInstance().connect(device, new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Log.d("BleDevice onConnectFail",bleDevice.getName());
                Log.d("BleDevice onConnectFail","" + bleDevice.getRssi());
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                Log.d("BleDevice onConnectSuccess",bleDevice.getName());
                Log.d("BleDevice onConnectSuccess","" + bleDevice.getRssi());
                readDevice(bleDevice);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {

            }
        });
    }
    private void readDevice(BleDevice device){
        BleManager.getInstance().readRssi(device, new BleRssiCallback() {
            @Override
            public void onRssiFailure(BleException onRssiFailure) {
                Log.d("onRssiFailure",""+onRssiFailure);

            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.d("onRssiSuccess",""+rssi);
                //String RSSI = String.valueOf(rssi);
                //                int position = deviceList.indexOf(device);
                //                Bleholder holder = (Bleholder) recyclerView.findViewHolderForAdapterPosition(position);
                //                if (holder != null) {
                //                    holder.binding.address.setText("RSSI: ".concat(RSSI));
                //                    int rssiInt = Integer.parseInt(RSSI);
                //                    double distance = calculateDistance(rssiInt);
                //                    holder.binding.distance.setText("Distance: ".concat(String.valueOf(distance)).concat("m"));
                }
        });
    }


    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void reload(List<BleDevice> deviceList) {
        this.deviceList = deviceList;
        notifyDataSetChanged();
    }

    public class Bleholder extends RecyclerView.ViewHolder {
        private BleDeviceItemBinding binding;
        public Bleholder(@NonNull BleDeviceItemBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;


        }
    }

}
