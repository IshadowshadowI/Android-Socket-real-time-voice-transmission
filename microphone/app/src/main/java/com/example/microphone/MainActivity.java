package com.example.microphone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    // 音频参数
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private boolean isConnected = false;  // 是否连接服务器
    private TextView statusTextView;

    // 服务器配置
    private static final String SERVER_IP = "192.168.137.1"; // 替换为服务器 IP
    private static final int SERVER_PORT = 7000; // 替换为服务器端口

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);

        // 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            startConnectionLoop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startConnectionLoop();
        } else {
            Toast.makeText(this, "需要麦克风权限才能运行应用程序！", Toast.LENGTH_SHORT).show();
            statusTextView.setText("权限被拒绝！");
        }
    }

    // 启动连接循环
    private void startConnectionLoop() {
        new Thread(() -> {
            while (true) { // 无限循环尝试连接
                if (!isConnected) {
                    try {
                        connectAndStream();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(5000); // 重试间隔 5 秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // 连接服务器并开始录音传输
    private void connectAndStream() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             OutputStream outputStream = socket.getOutputStream()) {

            // 连接成功，更新状态
            isConnected = true;
            runOnUiThread(() -> statusTextView.setText("已连接到服务器！"));

            // 初始化录音器
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            audioRecord.startRecording();
            isRecording = true;

            // 开始录音并发送数据
            byte[] buffer = new byte[BUFFER_SIZE];
            while (isConnected) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

            // 连接断开，更新状态并尝试重新连接
            isConnected = false;
            runOnUiThread(() -> statusTextView.setText("连接断开，正在尝试重新连接..."));
        } finally {
            // 释放录音资源
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        }
    }
}
