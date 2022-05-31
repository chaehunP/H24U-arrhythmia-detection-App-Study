package de.lme.heartnhealth4u;


import static de.lme.heartnhealth4u.BluetoothSPP.DATA;
import static de.lme.heartnhealth4u.BluetoothSPP.readMessage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import app.akexorcist.bluetotohspp.library.BluetoothState;

public class Realtimedata extends AppCompatActivity {
    private final Activity activity = this;
    private BluetoothSPP bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.realtimedata);

        // save CSV
        ((Button) findViewById(R.id.csvSave)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCsv(((CheckBox) findViewById(R.id.csvDate)).isChecked());
            }
        });

        bt = new BluetoothSPP(this); //Initializing
        TextView rtd = findViewById(R.id.RealTimeData);

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() { //데이터 수신
            public void onDataReceived(byte[] data, String message) {
                rtd.setText(message);
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() { //연결됐을 때
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext(), "Connected to " + name + "\n" + address, Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() { //연결해제
                Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() { //연결실패
                Toast.makeText(getApplicationContext(), "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnConnect = findViewById(R.id.btnConnect); //연결시도
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                    bt.disconnect();
                } else {
                    Intent intent = new Intent(getApplicationContext(), DeviceListActivity.class);  // spp 라이브리에 있는 DeviceList.class를 intent를 이용하여 구현
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);  //  구현한 intent를 결과 화면으로 표시
                }
            }
        });

    }

    // request write permissions
    private void requestPerm() {
        // request write permission for Ver>22
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                activity.getClass().getMethod("requestPermissions", new Class[]{String[].class, int.class})
                        .invoke(activity, new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, 200);
            } catch (Exception e) {
                Log.w("btcsv", "perm.request: " + e.toString());
                Toast.makeText(this, "Permission request error: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // save chart data to CSV
    private static final String CSV_NAME = "data";

    @SuppressLint("NewApi")
    private void saveCsv(boolean addDate) {
        if (readMessage == null) {
            Toast.makeText(this, "No data to write...", Toast.LENGTH_SHORT).show();
            return;
        }
        requestPerm();
        @SuppressLint("SimpleDateFormat")
        String fileName = !addDate ? (CSV_NAME + ".csv") : (CSV_NAME + (new SimpleDateFormat("_yyyy.MM.dd_HH.mm")).format(Calendar.getInstance().getTime()) + ".csv");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            try {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                BufferedOutputStream fo = new BufferedOutputStream(new FileOutputStream(file, false));  // BufferedOutputStream: byte단위로 파일을 기록할 때 사용하는 버퍼 스트림
                StringBuilder sb = new StringBuilder();  // 긴 문자열을 더하는 상황이 발생할 때 StringBuilder 사용
                                                         // String은 불변값, StringBuilder, StringBuffer은 가변값
                                                         // StringBuilder은 가변값이기 때문에 get(), add() 함수를 이용하여 값을 변경할 수 있음
                for(int i=0; i<DATA.size(); i++) {
                    sb.append(DATA.get(i)).append(',');  // 리스트의 값 꺼내기 get함수
                    sb.append('\n');
                }

                fo.write(sb.toString().getBytes("UTF-8"));  // 문자열을 바이트 배열로 변환해서 파일에 저장
                fo.flush();
                fo.close();  // 사용이 끝나면 파일 스트림 닫기
                sb.setLength(0); // 데이터 저장 후 다시 연결하여 데이터를 저장하면 새로운 파일에 "기존 데이터 + 현재 데이터"가 저장


                Toast.makeText(this, "CSV written to Downloads/" + fileName, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to save file Downloads/" + fileName + "\n" + e.toString(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "External media not available", Toast.LENGTH_SHORT).show();
        }
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) { //
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER); //DEVICE_ANDROID는 안드로이드 기기 끼리
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth was not enabled.", Toast.LENGTH_SHORT).show();
                finish();
            }

        }
    }

    @Override
    protected void onDestroy() {
        bt.stopService();
        super.onDestroy();

    }
}