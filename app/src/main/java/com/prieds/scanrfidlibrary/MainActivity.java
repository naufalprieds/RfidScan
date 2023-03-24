package com.prieds.scanrfidlibrary;

import static com.prieds.rfidlibrary.BaseApplication.REQUEST_ENABLE_BT;
import static com.prieds.rfidlibrary.BaseApplication.REQUEST_SELECT_DEVICE;
import static com.prieds.rfidlibrary.BaseApplication.showToast;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.prieds.rfidlibrary.BaseApplication;

public class MainActivity extends AppCompatActivity {
    Button btnClickBt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnClickBt = findViewById(R.id.btnClickBt);
        BaseApplication.init(this);

        btnClickBt.setOnClickListener(v->{
            BaseApplication.requestBlePermissions();
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    BaseApplication.connectAfterSuccessBt(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    showToast("Bluetooth has turned on ");
                } else {
                    showToast("Problem in BT Turning ON ");
                }
                break;
            default:
                break;
        }
    }
}