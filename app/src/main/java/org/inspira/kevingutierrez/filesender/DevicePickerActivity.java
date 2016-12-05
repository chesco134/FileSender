package org.inspira.kevingutierrez.filesender;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by jcapiz on 19/09/15.
 */
public class DevicePickerActivity extends Activity {

    private ArrayAdapter<String> arrAdapter;
    private BluetoothManager manager;
    private ListView devicesList;

    public void addDevice(String device){
        arrAdapter.add(device);
        arrAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
