package com.guyang.admin;
import android.app.Activity; import android.app.AlertDialog; import android.os.Bundle;
import android.view.*; import android.widget.*;
import org.json.JSONArray; import org.json.JSONObject;
import java.util.*;

public class BannersActivity extends Activity {
    private ListView listInfo;
    private TextView tvSelected;
    private Button btnSave;
    private List<JSONObject> availInfos = new ArrayList<>();
    private Set<String> selectedIds = new HashSet<>();
    private static final int MAX = 5;

    protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_banners);
        listInfo = findViewById(R.id.list_info);
        tvSelected = findViewById(R.id.tv_selected);
        btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> saveCarousel());
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            try {
                String r = ApiClient.get("/admin/banners.php");
                if (r == null || r.startsWith("ERROR:")) return;
                JSONObject d = new JSONObject(r);
                if (d.optInt("code", -1) != 0) return;
                JSONObject data = d.optJSONObject("data");
                if (data == null) return;

                JSONArray ids = data.optJSONArray("carousel_ids");
                if (ids != null) {
                    for (int i = 0; i < ids.length(); i++) selectedIds.add(ids.optString(i));
                }

                JSONArray infos = data.optJSONArray("available_infos");
                if (infos != null) {
                    availInfos.clear();
                    for (int i = 0; i < infos.length(); i++) availInfos.add(infos.optJSONObject(i));
                }

                runOnUiThread(this::updateUI);
            } catch (Exception ignored) {}
        }).start();
    }

    private void updateUI() {
        String[] items = new String[availInfos.size()];
        boolean[] checked = new boolean[availInfos.size()];
        for (int i = 0; i < availInfos.size(); i++) {
            JSONObject info = availInfos.get(i);
            String id = info.optString("id", "");
            items[i] = info.optString("title", "") + " [" + info.optString("category", "") + "]";
            checked[i] = selectedIds.contains(id);
        }

        listInfo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, items));
        listInfo.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for (int i = 0; i < checked.length; i++) listInfo.setItemChecked(i, checked[i]);

        listInfo.setOnItemClickListener((p, v, pos, id) -> {
            JSONObject info = availInfos.get(pos);
            String infoId = info.optString("id", "");
            if (listInfo.isItemChecked(pos)) {
                if (selectedIds.size() >= MAX) {
                    listInfo.setItemChecked(pos, false);
                    Toast.makeText(this, "最多选择" + MAX + "条资讯作为轮播", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedIds.add(infoId);
            } else {
                selectedIds.remove(infoId);
            }
            updateSelectedText();
        });
        updateSelectedText();
    }

    private void updateSelectedText() {
        tvSelected.setText("已选择: " + selectedIds.size() + "/" + MAX);
    }

    private void saveCarousel() {
        new Thread(() -> {
            try {
                JSONObject b = new JSONObject();
                JSONArray arr = new JSONArray();
                for (String id : selectedIds) arr.put(id);
                b.put("carousel_ids", arr);
                String r = ApiClient.post("/admin/banners.php", b);
                JSONObject d = new JSONObject(r);
                runOnUiThread(() -> {
                    if (d.optInt("code", -1) == 0) {
                        Toast.makeText(this, "轮播设置成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, d.optString("message", "保存失败"), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "保存异常", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
