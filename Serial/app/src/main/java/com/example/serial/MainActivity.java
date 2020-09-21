package com.example.serial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.companion.BluetoothLeDeviceFilter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 10;   // 블루투스 활성화 상태
    private BluetoothAdapter bluetoothAdapter;      // 블루투스 어댑터
    private Set<BluetoothDevice> devices;           // 블루투스 디바이스 데이터 셋
    private BluetoothDevice bluetoothDevice;        // 블루투스 디바이스
    private BluetoothSocket bluetoothSocket= null;  // 블루투스 소켓

    private OutputStream outputStream = null;       // 블루투스에 데이터 출력
    private InputStream inputStream = null;         // 블루투스에 데이터 입력

    private Thread workerThread = null;             // 문자열 수신에 사용하는 쓰레드
    private byte[] readBuffer;                      // 수신된 문자열 저장 버퍼
    private int readBufferPosition;                 // 버퍼 내 문자 저장 위치

    private TextView textViewReceive;               // 수신된 데이터 표시
    private EditText editTextSend;                  // 송신할 데이터 작성
    private Button buttonSend;                      // 송신 버튼

    private Button[] buttonList = new Button[5];    // 버튼 리스트 저장
    private int[] buttonId = {R.id.stage0, R.id.stage1, R.id.stage2, R.id.stage3, R.id.stage4};

    private class ButtonClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            byte[] data = null;
            int stage = 0;
            switch (view.getId()) {
                case (R.id.stage0) :
                    stage = 0;
                    break;
                case (R.id.stage1) :
                    stage = 1;
                    break;
                case (R.id.stage2) :
                    stage = 2;
                    break;
                case (R.id.stage3) :
                    stage = 3;
                    break;
                case (R.id.stage4) :
                    stage = 4;
                    break;
            }
            System.out.println(stage);
            data = makeProtocol(stage);
            System.out.println(new String(data));
            sendData(data);
        }
    }

    /* 생성 시에 실행되는 함수 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 컨테이너 id와 메인 xml 매칭
        textViewReceive = (TextView)findViewById(R.id.textView_receive);
        editTextSend    = (EditText)findViewById(R.id.editText_send);
        buttonSend      = (Button)findViewById(R.id.button_send);
        for(int i=0; i<buttonList.length; i++) {
            buttonList[i] = (Button)findViewById(buttonId[i]);
            buttonList[i].setOnClickListener(new ButtonClick());
        }

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = editTextSend.getText().toString();
                sendData(hexStringToByteArray(data));
            }
        });

        //불루투스 활성화
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 디포트 어댑터로 어댑터 지정
        if(bluetoothAdapter == null) { // 장치가 블루투스를 지원하지 않을 떄
            //처리 코드
            finishAffinity();
            System.runFinalization();
            System.exit(0);
        }
        else{ // 장치가 블루투스를 지원할 떄
            if(bluetoothAdapter.isEnabled()) { // 장치의 블루투스가 켜져있음
                selectBluetoothDevice();
            }
            else { // 장치의 불루투스가 꺼져있음
                // 블루투스를 활성화하기 위한 다이얼로그 출력
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // 선택한 값이 onActivityResult 함수에 콜백
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }
    }

    /* Activity에 저장된 결과값을 가져오는 함수 (startActivityForResult에 의해 호출) */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(requestCode == RESULT_OK) { // 사용을 눌렀을 때
                    selectBluetoothDevice();
                }
                else { // 취소를 눌렀을 때
                    // 처리 코드 작성: 종료
                    finishAffinity();
                    System.runFinalization();
                    System.exit(0);
                }
                break;
        }
    }

    /* 블루투스 장치 선택 */
    public void selectBluetoothDevice() {
        devices = bluetoothAdapter.getBondedDevices();      // 이미 페어링된 창치 찾기
        int pariedDeviceCount = devices.size();             // 페어링된 장치 크기 저장
        if(pariedDeviceCount == 0) {// 페어링된 장치가 없는 경우
            // 페어링을 위한 함수 호출
        }
        else { // 페어링된 장치가 있는 경우
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("페어링 되어있는 블루투스 디바이스 목록");
            List<String> list = new ArrayList<>(); // 디사이스의 이름과 주소를 저장할 리스트

            for(BluetoothDevice bluetoothDevice : devices) { // 모든 장치의 이름 추가
                list.add(bluetoothDevice.getName());
            }
            list.add("취소");

            // List를 CharSequence 배열 변경
            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
            list.toArray(new CharSequence[list.size()]);

            // 해당 아이템을 눌렀을 때 호출되는 이벤트 리스너
            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //해당 디바이스와 연결하는 함수 호출
                    connectDevice(charSequences[which].toString());
                }
            });

            builder.setCancelable(false); // 뒤로가기 못하게 함
            // 다이얼로그 생성
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    /* 장치 연결 함수 */
    public void connectDevice(String deviceName) {

        // 페어링 된 장치 모두 탐색
        for(BluetoothDevice tempDevice : devices) {
            // 사용자가 선택한 이름과 같은 장치 설정
            if(deviceName.equals((tempDevice.getName()))) {
                bluetoothDevice = tempDevice;
                break;
            }
        }

        //UUID 설정
        UUID uuid = java.util.UUID.fromString("0001101-0000-1000-8000-00805f9b34fb");
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();

            // 데이터 송,수신 스티림 얻기
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            receiveDate(); // 데이터 수신 함수 호출
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* 데이터 수신 함수 */
    public void receiveDate() {
        final Handler handler = new Handler();

        // 데이터 수신을 위한 버퍼 생성
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        // 데이터 송신을 위한 버퍼 생성
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(Thread.currentThread().isInterrupted()) {
                    try {
                        // 데이터를 수신했는지 확인
                        int byteAvailable = inputStream.available();
                        if(byteAvailable > 0) { // 데이터가 수신된 경우
                            byte[] bytes = new byte[byteAvailable]; // 입력 스트림에서 바이트 단위로 읽기
                            inputStream.read(bytes);
                            // 한 바이트씩 읽어 옴
                            for(int i=0; i<byteAvailable; i++){
                                byte tempByte = bytes[i];

                                if(tempByte == '\n') { // 개행 기준
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length); // readBuffer 배열 복사

                                    final String text = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            textViewReceive.append(text + "\n"); // 텍스트 뷰에 출력
                                        }
                                    });
                                }
                                else { // 개행 문자가 아니라면
                                    readBuffer[readBufferPosition++] = tempByte;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    byte[] makeProtocol(int stage) {
        byte[] bytes = new byte[9];
        int sum = 0;
        // TODO: makeProtocol
        bytes[0] = Byte.parseByte("02", 16);
        bytes[1] = Byte.parseByte("07", 16);
        bytes[2] = Byte.parseByte(Integer.toString((int)'U', 16), 16);
        bytes[3] = Byte.parseByte(Integer.toString((int)'M', 16), 16);
        bytes[4] = Byte.parseByte("00", 16);
        bytes[5] = Byte.parseByte("01", 16);
        bytes[6] = Byte.parseByte(Integer.toString(stage, 16), 16);
        bytes[7] = Byte.parseByte("03", 16);

        for(int i=0; i<7; i++) {
            sum += bytes[i];
        }
        bytes[8] = (byte) (Integer.parseInt("100",16) - sum);

        return bytes;
    }

    /* 데이터 송신 함수 */
    void sendData(String text) { // 버튼 이벤트에서 사용하는 메서드
        text += "\n";
        try {
            outputStream.write(text.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendData(byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    byte[] hexStringToByteArray(String str) {
        int len = str.length();
        byte[] data = new byte[len/2];
        for(int i=0; i<len; i+=2) {
            data[i/2] = (byte) ((Character.digit(str.charAt(i), 16)<<4)  + Character.digit(str.charAt(i+1), 16));
        }
        return data;
    }
}
