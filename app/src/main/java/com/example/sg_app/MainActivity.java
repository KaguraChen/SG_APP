package com.example.sg_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private Button button_1;
    private EditText ip;
    private EditText port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ip = findViewById(R.id.et_1);
        port = findViewById(R.id.et_2);
        button_1 = findViewById(R.id.openCamera);
        button_1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                Thread myThread=new Thread(){//创建子线程
                    @Override
                    public void run() {
                        try{
                            Socket socket = new Socket(ip.getText().toString(),Integer.parseInt(port.getText().toString()));
                            ((MySocket)getApplication()).setSocket(socket);//初始化MySocket
//                            System.out.println("ok");

                            Intent intent = new Intent();
                            intent.setClass(MainActivity.this, CameraGetActivity.class);
                            MainActivity.this.startActivity(intent);//启动新的Intent
                        }catch (Exception e){
                            e.printStackTrace();
                            Looper.prepare();
                            Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                            Looper.loop();

                        }

                    }
                };
                myThread.start();//启动线程
            }
        });
    }
}