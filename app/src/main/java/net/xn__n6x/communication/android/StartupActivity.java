package net.xn__n6x.communication.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import net.xn__n6x.communication.Assertions;
import net.xn__n6x.communication.R;

import java.util.Optional;

public class StartupActivity extends AppCompatActivity {
    /** ID of the request for enabling Bluetooth. */
    protected static final int REQUEST_ENABLE_BT = 1;

    /** Maximum number of tasks in the task stack. */
    protected static final int TASK_STACK_LENGTH = 3;
    /** Tasks we need to perform are executed in FILO order from this stack. */
    protected Runnable[] taskStack;
    /** Number of the current task being executed. */
    protected int taskNumber;

    protected WifiP2pManager wifiManager;
    protected BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        /* Make sure we have access to Wifi P2P. */
        wifiManager = (WifiP2pManager) this.getSystemService(Context.WIFI_P2P_SERVICE);
        if(wifiManager == null) {
            Toast.makeText(this, R.string.wifip2p_unavailable, Toast.LENGTH_LONG).show();
            this.finish();

            return;
        }
        /* Make sure we have access to Bluetooth. */
        bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        if(bluetoothManager == null) {
            Toast.makeText(this, R.string.bluetoothle_unavailable, Toast.LENGTH_LONG).show();
            this.finish();

            return;
        }

        /* Initialize the task stack with the task to run once both Bluetooth and Wifi P2P are available. */
        this.taskStack  = new Runnable[TASK_STACK_LENGTH];
        this.taskNumber = 0;
        taskStack[this.taskNumber++] = this::start;

        /* Add the new user check to the stack. */
        taskStack[this.taskNumber++] = () -> {
            Optional<DeviceIdentity> identity = DeviceIdentity.load(this);
            if(!identity.isPresent()) {
                /* Create a new identity for this device. */

            } else taskStack[--this.taskNumber].run();
        };

        /* Add the Bluetooth check to the task stack. */
        taskStack[this.taskNumber++] = () -> {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothManager.getAdapter() == null || !bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else taskStack[--this.taskNumber].run();
        };

        /* Run the first task. */
        taskStack[--this.taskNumber].run();
    }

    /** Runs the startup code once all requirements have been fulfilled. */
    protected void start() {
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        Assertions.debugAssertDiffers(adapter, null);

        Log.d("StartupActivity", "Bluetooth LE capabilities:");
        Log.d("StartupActivity", "    * isLe2MPhySupported()               " + adapter.isLe2MPhySupported());
        Log.d("StartupActivity", "    * isLeCodedPhySupported()            " + adapter.isLeCodedPhySupported());
        Log.d("StartupActivity", "    * isLeExtendedAdvertisingSupported() " + adapter.isLeExtendedAdvertisingSupported());
        Log.d("StartupActivity", "    * isLePeriodicAdvertisingSupported() " + adapter.isLePeriodicAdvertisingSupported());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK)
                    taskStack[--this.taskNumber].run();
                else {
                    Toast.makeText(this, R.string.bluetoothle_unavailable, Toast.LENGTH_LONG).show();
                    this.finish();
                }
        }
    }
}
