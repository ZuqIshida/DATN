package com.DATN.vuonthongminh;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private FloatingActionButton fabAdd;
    private DatabaseReference mDatabase;
    private ArrayList<String> gardenList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listGardens);
        fabAdd = findViewById(R.id.fabAdd);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        gardenList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, gardenList);
        listView.setAdapter(adapter);

        // Load danh sách các vườn từ Firebase
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                gardenList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    gardenList.add(child.getKey());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });

        // Khi bấm vào 1 vườn → sang màn chi tiết
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String gardenName = gardenList.get(position);
            Intent intent = new Intent(MainActivity.this, GardenDetail.class);
            intent.putExtra("gardenName", gardenName);
            startActivity(intent);
        });

        // Nút thêm vườn
        fabAdd.setOnClickListener(v -> showAddGardenDialog());
    }

    private void showAddGardenDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm Vườn Mới");

        final android.widget.EditText input = new android.widget.EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                addGarden(name);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addGarden(String name) {
        Map<String, Object> garden = new HashMap<>();
        garden.put("Temp", 0);   // int
        garden.put("Humi", 0);   // int
        garden.put("Soil_moisture", 0);
        garden.put("Ambient_light", 0);

        garden.put("Limit_temp", Arrays.asList(0, 0));   // list
        garden.put("Limit_humi", Arrays.asList(0, 0));
        garden.put("Limit_soil", Arrays.asList(0, 0));
        garden.put("Limit_light", Arrays.asList(0, 0));

        garden.put("Bom", false);   // bool
        garden.put("Den", false);
        garden.put("Quat", false);
        garden.put("Suong", false);
        garden.put("Changing_response", false);
        garden.put("Online_status", false);

        mDatabase.child(name).setValue(garden);
    }
}
