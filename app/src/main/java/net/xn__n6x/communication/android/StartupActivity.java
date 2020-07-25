package net.xn__n6x.communication.android;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import net.xn__n6x.communication.Assertions;
import net.xn__n6x.communication.R;
import net.xn__n6x.communication.identity.Profile;

import java.util.Optional;

public class StartupActivity extends AppCompatActivity {
    /** ID of the request for enabling Bluetooth. */
    protected static final int REQUEST_ENABLE_BT = 1;
    /** ID of the request for enabling fine device location. */
    protected static final int REQUEST_ENABLE_FINE_LOCATION = 2;

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
                DeviceIdentity.createWith(this, new Profile("Test"));
                Log.d("StartupActivity", "Created new identity for this device");

                taskStack[--this.taskNumber].run();
            } else taskStack[--this.taskNumber].run();
        };

        /* Add the permission check to the task stack. */
        taskStack[this.taskNumber++] = () -> {
            switch(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                case PackageManager.PERMISSION_GRANTED:
                    /* Move on. */
                    taskStack[--this.taskNumber].run();
                    break;
                case PackageManager.PERMISSION_DENIED:
                    /* Request the permission. */
                    this.requestPermissions(
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                        REQUEST_ENABLE_FINE_LOCATION);
            }
        };

        /* Run the first task. */
        taskStack[--this.taskNumber].run();
    }

    /** Runs the startup code once all requirements have been fulfilled. */
    protected void start() {
        Log.d("StartupActivity", "All seems right. Starting up the main activity");
        Intent intent = new Intent(this, PeerSelectionActivity.class);
        this.startActivity(intent);
        this.finish();
    }

    /** Oh, Android. */
    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        @NonNull String[] permissions,
        @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_ENABLE_FINE_LOCATION) {
            if(grantResults.length < 1)
                Assertions.fail("onRequestPermissionResult got called with an invalid result list");
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permissions_unavailable, Toast.LENGTH_LONG).show();
                this.finish();
            } else {
                /* Move on to the next task. */
                this.taskStack[--this.taskNumber].run();
            }
        }
    }
}
