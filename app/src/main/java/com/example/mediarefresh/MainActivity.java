package com.example.mediarefresh;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import android.app.ProgressDialog;

import androidx.annotation.NonNull;
import androidx.activity.ComponentActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MainActivity extends ComponentActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button refreshButton = findViewById(R.id.btn_refresh_media);
        refreshButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                refreshMediaLibrary();
            } else {
                requestPermissions();
            }
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要管理外部存储权限
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ 需要读写外部存储权限
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 请求管理外部存储权限
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ 请求读写权限
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void refreshMediaLibrary() {
        Toast.makeText(this, "开始刷新媒体库...", Toast.LENGTH_SHORT).show();

        // 创建并显示加载对话框
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("正在刷新");
        progressDialog.setMessage("正在扫描媒体文件，请稍候...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 直接扫描整个SD卡目录
        String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        MediaScannerConnection.scanFile(this, new String[]{sdcardPath}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss(); // 关闭对话框
                            Toast.makeText(MainActivity.this,
                                    "媒体库刷新完成", Toast.LENGTH_SHORT).show();
                        });
                    }
                });

        // 方法2: 发送广播通知媒体扫描器
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(Environment.getExternalStorageDirectory())));

        // 方法3: 通知MediaStore刷新
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File file = Environment.getExternalStorageDirectory();
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);
        }

        // 方法4: 针对Android 10+的媒体库刷新
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // 使用ContentResolver通知媒体库更新
                getContentResolver().notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null);
                getContentResolver().notifyChange(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null);
                getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                refreshMediaLibrary();
            } else {
                Toast.makeText(this, "需要存储权限才能刷新媒体库", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    refreshMediaLibrary();
                } else {
                    Toast.makeText(this, "需要管理外部存储权限才能刷新媒体库", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}