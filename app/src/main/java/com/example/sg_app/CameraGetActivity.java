package com.example.sg_app;


import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class CameraGetActivity extends CameraActivity {

    private static final String TAG = "OpencvCam";

    private JavaCamera2View javaCameraView;
    private int cameraId = JavaCamera2View.CAMERA_ID_ANY;

    private Socket socket;
    private PrintWriter pw,ps;
    private InputStream is;
    private OutputStream os;

    private Mat img;
    private BufferedReader br;
    private String recevied;


    private CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener2 = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
            Log.i(TAG, "onCameraViewStarted width=" + width + ", height=" + height);
        }

        @Override
        public void onCameraViewStopped() {
            Log.i(TAG, "onCameraViewStopped");
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            img = inputFrame.rgba();
            Mat img0 =  img.clone();//复制图像矩阵用于处理
            Mat dst = new  Mat();
            br=new BufferedReader(new InputStreamReader(is));
            Imgproc.resize(img0,dst, new Size(256, 256));//放缩图像降低传输的延时
            Bitmap bitmap = Bitmap.createBitmap(dst.width(), dst.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(dst, bitmap,true);//添加透明度，转变Mat为Bitmap
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bout);
            byte[] imgBytes = bout.toByteArray();
            String sender = Base64.encodeToString(imgBytes,Base64.DEFAULT);//Base64编码
            String len_str = new String(String.format("%-16d", sender.length()).getBytes());
            pw.write(len_str);//第一次发送编码的长度
            pw.flush();
            try {
                recevied = br.readLine();//由于读取每一行信息，故服务器发送的数据也应做处理
                System.out.println(recevied);
            } catch (IOException e) {
                System.out.println(e);
            }
            ps.write(sender);//第二次发送编码信息
            ps.flush();
            return img;
        }
    };




private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.i(TAG, "onManagerConnected status=" + status + ", javaCameraView=" + javaCameraView);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    if (javaCameraView != null) {
                        javaCameraView.setCvCameraViewListener(cvCameraViewListener2);
                        // 禁用帧率显示
                        javaCameraView.disableFpsMeter();
                        javaCameraView.enableView();
                    }
                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    //复写父类的 getCameraViewList 方法，把 javaCameraView 送到父 Activity，一旦权限被授予之后，javaCameraView 的 setCameraPermissionGranted 就会自动被调用。
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        Log.i(TAG, "getCameraViewList");
        List<CameraBridgeViewBase> list = new ArrayList<>();
        list.add(javaCameraView);
        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_get);
        findView();
        socket = ((MySocket)getApplication()).getSocket();
        try {
            os = socket.getOutputStream();
            pw = new PrintWriter(os);
            ps = new PrintWriter(os);
            is = socket.getInputStream();
        }catch (IOException e) {
            e.printStackTrace();}
        }

    private void findView() {
        javaCameraView = findViewById(R.id.javaCameraView);
    }


    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "initDebug true");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "initDebug false");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {

            Intent resultIntent=new Intent(CameraGetActivity.this, MainActivity.class);
            CameraGetActivity.this.finish();
            startActivity(resultIntent);
        }
        return false;
    }

}


