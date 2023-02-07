package com.example.sg_app;

import android.app.Application;

import java.net.Socket;

public class MySocket extends Application {
    Socket socket = null;

    public Socket getSocket() {
        System.out.println(socket);
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}

