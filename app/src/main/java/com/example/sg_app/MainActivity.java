package com.example.sg_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private Button button_1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread myThread=new Thread(){//创建子线程
            @Override
            public void run() {
                try{
                    Socket socket = new Socket("127.0.0.1",Integer.parseInt("5050"));
                    ((MySocket)getApplication()).setSocket(socket);//初始化MySocket
                    System.out.println("ok");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        myThread.start();//启动线程

        button_1 = findViewById(R.id.openCamera);
        button_1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CameraGetActivity.class);
                MainActivity.this.startActivity(intent);//启动新的Intent

            }
        });
    }
}