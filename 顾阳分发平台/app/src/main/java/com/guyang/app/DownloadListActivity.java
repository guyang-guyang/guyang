package com.guyang.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DownloadListActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ListView lvDownloads;
    private TextView tvEmpty;
    private DownloadListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);

        initViews();
        setupListeners();
        loadDownloadRecords();
    }

    private void initViews() {
        ivBack = findViewById(R.id.iv_back);
        lvDownloads = findViewById(R.id.lv_downloads);
        tvEmpty = findViewById(R.id.tv_empty);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void loadDownloadRecords() {
        List<InAppDownloadManager.DownloadTask> tasks = new ArrayList<>();
        InAppDownloadManager manager = InAppDownloadManager.getInstance(this);

        if (tasks.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvDownloads.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvDownloads.setVisibility(View.VISIBLE);
            adapter = new DownloadListAdapter(this, tasks, manager);
            lvDownloads.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDownloadRecords();
    }
}