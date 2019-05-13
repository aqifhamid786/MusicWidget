package com.test.widget;

import android.Manifest;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {


    private Button permissionButton;
    private TextView messageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        initUI();
        checkAndRequestPermission();
    }

    private void initUI() {
        permissionButton = (Button) findViewById(R.id.permissionbtn);
        messageTextView = (TextView) findViewById(R.id.messagetv);
        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermission();
            }
        });
    }

    private void updateUi(boolean permissionGranted){
        if(permissionGranted) {
            messageTextView.setText("Enjoy your music!");
            permissionButton.setVisibility(View.INVISIBLE);
        } else {
            messageTextView.setText("Please grant permission!");
            permissionButton.setVisibility(View.VISIBLE);
        }
    }

    private void checkAndRequestPermission() {
        if (isExplicitPermissionRequired() && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            updateUi(false);
            requestPermission();
        } else {
            updateUi(true);
            broadcastPermissionSuccess();
        }
    }

    private boolean isExplicitPermissionRequired(){
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestPermission() {
        requestPermissions(new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.sPermissionStorageRequestId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (Constants.sPermissionStorageRequestId == requestCode && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateUi(true);
            broadcastPermissionSuccess();
        }
    }

    private void broadcastPermissionSuccess(){
        Intent intent = new Intent(this, AppWidgetProvider.class);
        intent.setComponent(new ComponentName(MusicPlayerWidget.class.getPackage().getName(), MusicPlayerWidget.class.getCanonicalName()));
        intent.putExtra(Constants.sPermissionEventKey, Constants.sPermissionGranted);
        sendBroadcast(intent);
    }
}


