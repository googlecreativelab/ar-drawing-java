package com.googlecreativelab.drawar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

// ARCore requires camera permissions to operate. We should request the permission before DrawAR because the ARCore's JNI_OnLoad need
// the permission. If don't request it before DrawAR, the app will crash on the first start.
public class FakeMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.requestCameraPermission(this);
        }else{
            startRealActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            finish();
        }else{
            startRealActivity();
        }
    }

    private void startRealActivity(){
        Intent intent = new Intent(this,DrawAR.class);
        startActivity(intent);
        finish();
    }
}
