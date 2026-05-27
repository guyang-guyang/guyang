package com.guyang.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AppDetailActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ImageView ivAppIcon;
    private TextView tvAppName;
    private TextView tvAppInfo;
    private TextView tvAppDesc;
    private Button btnDownload;
    private Button btnBrowser;

    private String appName;
    private String appDescription;
    private String appId;
    private String downloadUrl;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        initViews();
        parseIntent();
        loadDefaultData();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.btn_back1);
        ivAppIcon = findViewById(R.id.app_icon2);
        tvAppName = findViewById(R.id.app_name2);
        tvAppInfo = findViewById(R.id.app_info2);
        tvAppDesc = findViewById(R.id.app_desc2);
        btnDownload = findViewById(R.id.btn_download);
        btnBrowser = findViewById(R.id.btn_browser);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void parseIntent() {
        appName = getIntent().getStringExtra("appName");
        appDescription = getIntent().getStringExtra("appDescription");
        appId = getIntent().getStringExtra("appId");
        downloadUrl = getIntent().getStringExtra("downloadUrl");
        fileName = getIntent().getStringExtra("fileName");

        if (appName == null) appName = "未知应用";
        if (appDescription == null) appDescription = "";
        if (fileName == null) fileName = appName + ".apk";
    }

    private void loadDefaultData() {
        tvAppName.setText(appName);
        tvAppDesc.setText(appDescription.isEmpty() ? "暂无描述" : appDescription);
        tvAppInfo.setText("版本 1.0 大小未知");
        btnDownload.setText("下载");
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnDownload.setOnClickListener(v -> {
            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                InAppDownloadActivity.start(AppDetailActivity.this,
                        appName, appId, fileName, downloadUrl);
            }
        });

        btnBrowser.setVisibility(View.GONE);
    }
}