package com.DATN.vuonthongminh;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

public class GardenDetail extends AppCompatActivity {
    TextView txt_temp, txt_humi, txt_soil, txt_light, txt_id, txt_onlinestatus, txt_limittemp1, txt_limittemp2, txt_limithumi1, txt_limithumi2, txt_limitsoil1, txt_limitsoil2, txt_limitlight1, txt_limitlight2;
    Switch sw_automode, sw_bom, sw_quat, sw_den, sw_suong;
    Button btn_deletegarden;
    DatabaseReference garden, mDatabase;
    String gardenName;
    FloatingActionButton fab_setrange;
    CardView cad_swauto, cad_swdevice, cad_sensorsvalue, cad_config;

    private List<ValueEventListener> sensorListeners = new ArrayList<>();
    private List<ValueEventListener> deviceListeners = new ArrayList<>();
    private List<ValueEventListener> limitListeners = new ArrayList<>();
    private List<Integer> limitTemp;
    private List<Integer> limitHumi;
    private List<Integer> limitSoil;
    private List<Integer> limitLight;
    private Handler handler = new Handler();
    private Runnable checkTask;

    private Boolean onlineStatus = false;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_garden_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        txt_temp = findViewById(R.id.txt_Temp);
        txt_humi = findViewById(R.id.txt_Humi);
        txt_soil = findViewById(R.id.txt_Soil);
        txt_light = findViewById(R.id.txt_Light);
        txt_id = findViewById(R.id.txt_ID);
        txt_limittemp1 = findViewById(R.id.txt_LimitTemp1);
        txt_limittemp2 = findViewById(R.id.txt_LimitTemp2);
        txt_limithumi1 = findViewById(R.id.txt_LimitHumi1);
        txt_limithumi2 = findViewById(R.id.txt_LimitHumi2);
        txt_limitsoil1 = findViewById(R.id.txt_LimitSoil1);
        txt_limitsoil2 = findViewById(R.id.txt_LimitSoil2);
        txt_limitlight1 = findViewById(R.id.txt_LimitLight1);
        txt_limitlight2 = findViewById(R.id.txt_LimitLight2);
        txt_onlinestatus = findViewById(R.id.txt_OnlineStatus);
        sw_automode = findViewById(R.id.sw_AutoMode);
        sw_bom = findViewById(R.id.sw_Bom);
        sw_den = findViewById(R.id.sw_Den);
        sw_quat = findViewById(R.id.sw_Quat);
        sw_suong = findViewById(R.id.sw_Suong);
        cad_swauto = findViewById(R.id.cad_SwAuto);
        cad_swdevice = findViewById(R.id.cad_SwDevice);
        cad_sensorsvalue = findViewById(R.id.cad_SensorsValue);
        cad_config = findViewById(R.id.cad_Config);
        btn_deletegarden = findViewById(R.id.btn_DeleteGarden);
        fab_setrange = findViewById(R.id.fab_settingRange);
        Intent get_id = getIntent();
        gardenName = get_id.getStringExtra("gardenName");
        txt_id.setText(String.format("ID: %s", gardenName));
        mDatabase = FirebaseDatabase.getInstance().getReference();
        garden = mDatabase.child(gardenName);
        checkOnlineStatus();
        readLimit();
        setTextConfig();
        getSensorData();
        getStateDevice();
        On_Off_Device();
        Auto_Mode();
        btn_deletegarden.setOnClickListener(v -> createDialogDeleteGarden(gardenName));
        fab_setrange.setOnClickListener(v -> createDialogSettingRange());
    }

    private void removeAllListeners() {
        if (garden != null) {
            for (ValueEventListener listener : sensorListeners) {
                garden.removeEventListener(listener);
            }
            for (ValueEventListener listener : deviceListeners) {
                garden.removeEventListener(listener);
            }
            for (ValueEventListener listener : limitListeners){
                garden.removeEventListener(listener);
            }
        }
    }

    private void getSensorData(){
        ValueEventListener tempListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    txt_temp.setText(snapshot.getValue().toString()+"°C");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txt_temp.setText("Không thể truy cập đến Database");
            }
        };
        garden.child("Temp").addValueEventListener(tempListener);
        sensorListeners.add(tempListener);

        ValueEventListener humiListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    txt_humi.setText(snapshot.getValue().toString()+" %");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txt_humi.setText("Không thể truy cập đến Database");
            }
        };
        garden.child("Humi").addValueEventListener(humiListener);
        sensorListeners.add(humiListener);

        ValueEventListener soilListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    txt_soil.setText(snapshot.getValue().toString()+" %");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txt_soil.setText("Không thể truy cập đến Database");
            }
        };
        garden.child("Soil_moisture").addValueEventListener(soilListener);
        sensorListeners.add(soilListener);

        ValueEventListener lightListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    txt_light.setText(snapshot.getValue().toString()+" %");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                txt_light.setText("Không thể truy cập đến Database");
            }
        };
        garden.child("Ambient_light").addValueEventListener(lightListener);
        sensorListeners.add(lightListener);
    }
    private void getStateDevice(){
        ValueEventListener bomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean bomState = snapshot.getValue(Boolean.class);
                    sw_bom.setChecked(Boolean.TRUE.equals(bomState));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GardenDetail.this, "Không thể truy cập đến Database", Toast.LENGTH_SHORT).show();
            }
        };
        garden.child("Bom").addValueEventListener(bomListener);
        deviceListeners.add(bomListener);

        ValueEventListener denListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean bomState = snapshot.getValue(Boolean.class);
                    sw_den.setChecked(Boolean.TRUE.equals(bomState));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GardenDetail.this, "Không thể truy cập đến Database", Toast.LENGTH_SHORT).show();
            }
        };
        garden.child("Den").addValueEventListener(denListener);
        deviceListeners.add(denListener);

        ValueEventListener quatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean bomState = snapshot.getValue(Boolean.class);
                    sw_quat.setChecked(Boolean.TRUE.equals(bomState));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GardenDetail.this, "Không thể truy cập đến Database", Toast.LENGTH_SHORT).show();
            }
        };
        garden.child("Quat").addValueEventListener(quatListener);
        deviceListeners.add(quatListener);

        ValueEventListener suongListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean bomState = snapshot.getValue(Boolean.class);
                    sw_suong.setChecked(Boolean.TRUE.equals(bomState));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GardenDetail.this, "Không thể truy cập đến Database", Toast.LENGTH_SHORT).show();
            }
        };
        garden.child("Suong").addValueEventListener(suongListener);
        deviceListeners.add(suongListener);

        ValueEventListener autoModeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean bomState = snapshot.getValue(Boolean.class);
                    sw_automode.setChecked(Boolean.TRUE.equals(bomState));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GardenDetail.this, "Không thể truy cập đến Database", Toast.LENGTH_SHORT).show();
            }
        };
        garden.child("Auto_mode").addValueEventListener(autoModeListener);
        deviceListeners.add(autoModeListener);

    }
    private void On_Off_Device(){
        sw_bom.setOnCheckedChangeListener((buttonView, isChecked) -> garden.child("Bom").setValue(isChecked));
        sw_den.setOnCheckedChangeListener((buttonView, isChecked) -> garden.child("Den").setValue(isChecked));
        sw_quat.setOnCheckedChangeListener((buttonView, isChecked) -> garden.child("Quat").setValue(isChecked));
        sw_suong.setOnCheckedChangeListener((buttonView, isChecked) -> garden.child("Suong").setValue(isChecked));
    }
    private void Auto_Mode() {
        sw_automode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            garden.child("Auto_mode").setValue(isChecked);
            if (isChecked) {
                cad_swdevice.setVisibility(View.GONE);
            } else {
                cad_swdevice.setVisibility(View.VISIBLE);
            }
        });
    }
    private void readLimit(){
        ValueEventListener limitTempListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<List<Integer>> t = new GenericTypeIndicator<List<Integer>>() {};
                limitTemp = snapshot.getValue(t);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        garden.child("Limit_temp").addValueEventListener(limitTempListener);
        limitListeners.add(limitTempListener);
        ValueEventListener limithumiListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<List<Integer>> t = new GenericTypeIndicator<List<Integer>>() {};
                limitHumi = snapshot.getValue(t);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        garden.child("Limit_humi").addValueEventListener(limithumiListener);
        limitListeners.add(limithumiListener);
        ValueEventListener limitsoilListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<List<Integer>> t = new GenericTypeIndicator<List<Integer>>() {};
                limitSoil = snapshot.getValue(t);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        garden.child("Limit_soil").addValueEventListener(limitsoilListener);
        limitListeners.add(limitsoilListener);
        ValueEventListener limitLightListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<List<Integer>> t = new GenericTypeIndicator<List<Integer>>() {};
                limitLight = snapshot.getValue(t);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        garden.child("Limit_light").addValueEventListener(limitLightListener);
        limitListeners.add(limitLightListener);
    }
    private boolean checkLimit(String A, String B, Integer Max, boolean State_check){
        Integer a = Integer.parseInt(A);
        Integer b = Integer.parseInt(B);
        if(a > Max || b > Max) return false;
        if(State_check){
            if(a < b) return true;
            else return false;
        }
        else{
            if(a > b) return true;
            else return false;
        }
    }
    private void createDialogDeleteGarden(String garden){
        AlertDialog.Builder delBuilder = new AlertDialog.Builder(GardenDetail.this);
        delBuilder.setTitle("Xoá vườn");
        delBuilder.setMessage("Bạn có chắc muốn xoá Vườn với ID: " + garden);
        delBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        delBuilder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeAllListeners();
                mDatabase.child(garden).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(GardenDetail.this, "Đã xóa " + gardenName, Toast.LENGTH_SHORT).show();
                            finish(); // Quay lại MainActivity sau khi xóa
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(GardenDetail.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }
        });
        delBuilder.setNegativeButton("Không", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(GardenDetail.this, "Bạn đã chọn không xoá vườn với ID: "+ garden, Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog alertDelGarden = delBuilder.create();
        alertDelGarden.setCanceledOnTouchOutside(true);
        alertDelGarden.show();
    }
    private void createDialogCheckandSendLimit(boolean[] stateCheck, Dialog customDialog,
                                               EditText edt_temp1, EditText edt_temp2,
                                               EditText edt_humi1, EditText edt_humi2,
                                               EditText edt_soil1, EditText edt_soil2,
                                               EditText edt_light1, EditText edt_light2) {

        String[] str_AlertResponse = {
                "► Bạn đã cấu hình sai NHIỆT ĐỘ! Yêu cầu ngưỡng bật CAO hơn ngưỡng tắt và KHÔNG vượt quá 50°C.",
                "► Bạn đã cấu hình sai ĐỘ ẨM KHÔNG KHÍ! Yêu cầu ngưỡng bật THẤP hơn ngưỡng tắt và KHÔNG vượt quá 100%.",
                "► Bạn đã cấu hình sai ĐỘ ẨM ĐẤT! Yêu cầu ngưỡng bật THẤP hơn ngưỡng tắt và KHÔNG vượt quá 100%.",
                "► Bạn đã cấu hình sai CƯỜNG ĐỘ ÁNH SÁNG! Yêu cầu ngưỡng bật THẤP hơn ngưỡng tắt và KHÔNG vượt quá 100%."
        };

        boolean passState = true;
        StringBuilder alertMessage = new StringBuilder();

        for (int i = 0; i < stateCheck.length; i++) {
            if (!stateCheck[i]) {
                alertMessage.append(str_AlertResponse[i]).append("\n");
                passState = false;
            }
        }

        if (passState) {
            // ✅ Cập nhật giá trị vào List trong code
            limitTemp = Arrays.asList(
                    Integer.parseInt(edt_temp1.getText().toString()),
                    Integer.parseInt(edt_temp2.getText().toString())
            );

            limitHumi = Arrays.asList(
                    Integer.parseInt(edt_humi1.getText().toString()),
                    Integer.parseInt(edt_humi2.getText().toString())
            );

            limitSoil = Arrays.asList(
                    Integer.parseInt(edt_soil1.getText().toString()),
                    Integer.parseInt(edt_soil2.getText().toString())
            );

            limitLight = Arrays.asList(
                    Integer.parseInt(edt_light1.getText().toString()),
                    Integer.parseInt(edt_light2.getText().toString())
            );


            garden.child("Limit_temp").setValue(limitTemp);
            garden.child("Limit_humi").setValue(limitHumi);
            garden.child("Limit_soil").setValue(limitSoil);
            garden.child("Limit_light").setValue(limitLight);
            garden.child("Changing_response").setValue(false);

            Toast.makeText(this, "Cấu hình thành công", Toast.LENGTH_SHORT).show();


            if (customDialog != null && customDialog.isShowing()) {
                customDialog.dismiss();
            }

        } else {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(GardenDetail.this);
            alertBuilder.setTitle("Cấu hình sai!");
            alertBuilder.setIcon(android.R.drawable.ic_dialog_alert);
            alertBuilder.setMessage(alertMessage.toString().trim());
            alertBuilder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

            AlertDialog altDialog = alertBuilder.create();
            altDialog.setCanceledOnTouchOutside(true);
            altDialog.show();
        }
    }


    private void createDialogSettingRange(){
        Dialog customDialog = new Dialog(GardenDetail.this);
        customDialog.setContentView(R.layout.dialog_set_range);
        ImageView img_tempinfo = (ImageView) customDialog.findViewById(R.id.img_TempInfo);
        ImageView img_humiinfo = (ImageView) customDialog.findViewById(R.id.img_HumiInfo);
        ImageView img_soilinfo = (ImageView) customDialog.findViewById(R.id.img_SoilInfo);
        ImageView img_lightinfo = (ImageView) customDialog.findViewById(R.id.img_LightInfo);

        EditText edt_temp1 = (EditText) customDialog.findViewById(R.id.edt_Temp1);
        EditText edt_temp2 = (EditText) customDialog.findViewById(R.id.edt_Temp2);
        EditText edt_humi1 = (EditText) customDialog.findViewById(R.id.edt_Humi1);
        EditText edt_humi2 = (EditText) customDialog.findViewById(R.id.edt_Humi2);
        EditText edt_soil1 = (EditText) customDialog.findViewById(R.id.edt_Soil1);
        EditText edt_soil2 = (EditText) customDialog.findViewById(R.id.edt_Soil2);
        EditText edt_light1 = (EditText) customDialog.findViewById(R.id.edt_Light1);
        EditText edt_light2 = (EditText) customDialog.findViewById(R.id.edt_Light2);

        Button btn_cancel = (Button) customDialog.findViewById(R.id.btn_cancel);
        Button btn_set = (Button) customDialog.findViewById(R.id.btn_Set);

        edt_temp1.setText(String.valueOf(limitTemp.get(0)));
        edt_temp2.setText(String.valueOf(limitTemp.get(1)));

        edt_humi1.setText(String.valueOf(limitHumi.get(0)));
        edt_humi2.setText(String.valueOf(limitHumi.get(1)));

        edt_soil1.setText(String.valueOf(limitSoil.get(0)));
        edt_soil2.setText(String.valueOf(limitSoil.get(1)));

        edt_light1.setText(String.valueOf(limitLight.get(0)));
        edt_light2.setText(String.valueOf(limitLight.get(1)));

        img_tempinfo.setOnClickListener(v -> createDialogInfo(0));
        img_humiinfo.setOnClickListener(v -> createDialogInfo(1));
        img_soilinfo.setOnClickListener(v -> createDialogInfo(2));
        img_lightinfo.setOnClickListener(v -> createDialogInfo(3));
        btn_cancel.setOnClickListener(v -> customDialog.dismiss());
        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean[] stateCheck = new boolean[4];
                stateCheck[0] = checkLimit(edt_temp1.getText().toString() , edt_temp2.getText().toString(), 50, false);
                stateCheck[1] = checkLimit(edt_humi1.getText().toString() , edt_humi2.getText().toString(), 100, true);
                stateCheck[2] = checkLimit(edt_soil1.getText().toString() , edt_soil2.getText().toString(), 100, true);
                stateCheck[3] = checkLimit(edt_light1.getText().toString() , edt_light2.getText().toString(), 100, true);
                createDialogCheckandSendLimit(stateCheck, customDialog,
                        edt_temp1, edt_temp2,
                        edt_humi1, edt_humi2,
                        edt_soil1, edt_soil2,
                        edt_light1, edt_light2);
                setTextConfig();
            }
        });

        customDialog.setCanceledOnTouchOutside(false);
        customDialog.show();

    }
    private void createDialogInfo(int number) {
        AlertDialog.Builder infoBuilder = new AlertDialog.Builder(GardenDetail.this);

        String[] info = {
                "Đối với NHIỆT ĐỘ: nếu nhiệt độ >= ngưỡng BẬT, quạt sẽ BẬT. Nếu nhiệt độ <= ngưỡng TẮT, quạt sẽ TẮT.",
                "Đối với ĐỘ ẨM KHÔNG KHÍ: nếu độ ẩm <= ngưỡng BẬT, phun sương sẽ BẬT. Nếu độ ẩm >= ngưỡng TẮT, phun sương sẽ TẮT.",
                "Đối với ĐỘ ẨM ĐẤT: nếu độ ẩm đất <= ngưỡng BẬT, bơm sẽ BẬT. Nếu độ ẩm đất >= ngưỡng TẮT, bơm sẽ TẮT.",
                "Đối với CƯỜNG ĐỘ ÁNH SÁNG: nếu ánh sáng <= ngưỡng BẬT, đèn sẽ BẬT. Nếu ánh sáng >= ngưỡng TẮT, đèn sẽ TẮT."
        };

        if (number < 0 || number >= info.length) number = 0;

        infoBuilder.setTitle("Thông tin");
        infoBuilder.setMessage(info[number]);
        infoBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog infoAlert = infoBuilder.create();
        infoAlert.setCanceledOnTouchOutside(true);
        infoAlert.show();
    }
    private void updateStatus(TextView txtStatus, boolean isOnline) {
        String prefix = "Trạng thái: ";
        String statusText = isOnline ? "ONLINE" : "OFFLINE";
        int color = isOnline ? Color.GREEN : Color.RED;

        SpannableString spannable = new SpannableString(prefix + statusText);

        // Chỉ đổi màu phần "Online"/"Offline"
        spannable.setSpan(
                new ForegroundColorSpan(color),
                prefix.length(),
                prefix.length() + statusText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        txtStatus.setText(spannable);
    }
    private void checkOnlineStatus() {
        // Hiển thị loading trong 3s đầu
        txt_onlinestatus.setText("Trạng thái: Đang tải...");
        hideUI(!onlineStatus);
        garden.child("Online_status").setValue(false);
        handler.postDelayed(() -> {
            checkTask = new Runnable() {
                @Override
                public void run() {
                    garden.child("Online_status").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Boolean value = task.getResult().getValue(Boolean.class);
                            if (value != null && value) {
                                // Nếu true thì Online và reset về false
                                onlineStatus = true;
                                updateStatus(txt_onlinestatus, true);
                                hideUI(!onlineStatus);
                                setTextConfig();
                                garden.child("Online_status").setValue(false);
                            } else {
                                // Nếu false thì Offline
                                onlineStatus = false;
                                updateStatus(txt_onlinestatus, false);
                                hideUI(!onlineStatus);
                                setTextConfig();
                            }
                        }
                    });

                    // Lặp lại sau 3 giây
                    handler.postDelayed(this, 4000);
                }
            };
            // Bắt đầu chạy task
            handler.post(checkTask);

        }, 3000); // Delay 3 giây để hiển thị "Đang tải..."
    }
    private void setTextConfig() {
        if (onlineStatus) {
            if (limitTemp != null && limitTemp.size() >= 2) {
                txt_limittemp1.setText(String.format("Ngưỡng BẬT: >= %d°C", limitTemp.get(0)));
                txt_limittemp2.setText(String.format("Ngưỡng TẮT: <= %d°C", limitTemp.get(1)));
            }

            if (limitHumi != null && limitHumi.size() >= 2) {
                txt_limithumi1.setText(String.format("Ngưỡng BẬT: <= %d%%", limitHumi.get(0)));
                txt_limithumi2.setText(String.format("Ngưỡng TẮT: >= %d%%", limitHumi.get(1)));
            }

            if (limitSoil != null && limitSoil.size() >= 2) {
                txt_limitsoil1.setText(String.format("Ngưỡng BẬT: <= %d%%", limitSoil.get(0)));
                txt_limitsoil2.setText(String.format("Ngưỡng TẮT: >= %d%%", limitSoil.get(1)));
            }

            if (limitLight != null && limitLight.size() >= 2) {
                txt_limitlight1.setText(String.format("Ngưỡng BẬT: <= %d%%", limitLight.get(0)));
                txt_limitlight2.setText(String.format("Ngưỡng TẮT: >= %d%%", limitLight.get(1)));
            }
        } else {
            txt_limittemp1.setText("Ngưỡng BẬT: >= --°C");
            txt_limittemp2.setText("Ngưỡng TẮT: <= --°C");
            txt_limithumi1.setText("Ngưỡng BẬT: <= --%");
            txt_limithumi2.setText("Ngưỡng TẮT: >= --%");
            txt_limitsoil1.setText("Ngưỡng BẬT: <= --%");
            txt_limitsoil2.setText("Ngưỡng TẮT: >= --%");
            txt_limitlight1.setText("Ngưỡng BẬT: <= --%");
            txt_limitlight2.setText("Ngưỡng TẮT: >= --%");
        }
    }
    private void hideUI(boolean state){
        if(state){
            cad_swauto.setVisibility(View.GONE);
            cad_swdevice.setVisibility(View.GONE);
            cad_sensorsvalue.setVisibility(View.GONE);
            cad_config.setVisibility(View.GONE);
            fab_setrange.setVisibility(View.GONE);
        }
        else{
            cad_swauto.setVisibility(View.VISIBLE);
            if(sw_automode.isChecked()) cad_swdevice.setVisibility(View.GONE);
            else cad_swdevice.setVisibility(View.VISIBLE);
            cad_sensorsvalue.setVisibility(View.VISIBLE);
            cad_config.setVisibility(View.VISIBLE);
            fab_setrange.setVisibility(View.VISIBLE);
        }
    }

    private void stopCheckOnlineStatus() {
        if (handler != null && checkTask != null) {
            handler.removeCallbacks(checkTask);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeAllListeners();
        stopCheckOnlineStatus();
    }
}