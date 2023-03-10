package com.example.sg_app;



import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class CameraGetActivity extends CameraActivity {

    private static final String TAG = "OpencvCam";

    private JavaCamera2View javaCameraView;
//    private int cameraId = JavaCamera2View.CAMERA_ID_ANY;

    private Socket socket;
    private OutputStreamWriter osw;
    private BufferedOutputStream bos;
    private BufferedReader br;
    private BufferedInputStream bis;

    private Mat img;
    private Mat receiveImg;
    private boolean exitFlag;
    private boolean flag;



    private CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener2 = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
//            Log.i(TAG, "onCameraViewStarted width=" + width + ", height=" + height);
        }

        @Override
        public void onCameraViewStopped() {
//            Log.i(TAG, "onCameraViewStopped");
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            img = inputFrame.rgba();
            flag = !flag;
            if (flag)
                return receiveImg;
            Mat img0 =  img.clone();//??????????????????????????????
            Imgproc.resize(img0,img0, new Size(540, 540));//?????????????????????????????????
            Bitmap bitmap = Bitmap.createBitmap(img0.width(), img0.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img0, bitmap,true);//??????Mat???Bitmap
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bout);
            byte[] imgBytes = bout.toByteArray();
//            String sender = Base64.encodeToString(imgBytes,Base64.DEFAULT);//Base64??????
            String len_str = String.format("%-16d", imgBytes.length);

            try {
                osw.write(len_str);//?????????????????????
                osw.flush();
//                System.out.println(imgBytes);
                bos.write(imgBytes);//?????????????????????
                bos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            receiveImg = Imgcodecs.imdecode(new MatOfByte(imgBytes), Imgcodecs.IMREAD_COLOR);
            return receiveImg;
        }
    };




private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
//            Log.i(TAG, "onManagerConnected status=" + status + ", javaCameraView=" + javaCameraView);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    if (javaCameraView != null) {
                        javaCameraView.setCvCameraViewListener(cvCameraViewListener2);
                        // ??????????????????
                        javaCameraView.disableFpsMeter();
                        javaCameraView.enableView();
                    }
                }
                break;
                default:
                    super.onManagerConnected(status);break;
            }
        }
    };

    //??????????????? getCameraViewList ???????????? javaCameraView ????????? Activity?????????????????????????????????javaCameraView ??? setCameraPermissionGranted ????????????????????????
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
//        Log.i(TAG, "getCameraViewList");
        List<CameraBridgeViewBase> list = new ArrayList<>();
        list.add(javaCameraView);
        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_get);
        findView();
        exitFlag = false;
        socket = ((MySocket)getApplication()).getSocket();
        try {
            osw = new OutputStreamWriter(socket.getOutputStream());
            bos = new BufferedOutputStream(socket.getOutputStream());
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bis = new BufferedInputStream(socket.getInputStream());
        }catch (IOException e) {
            e.printStackTrace();

        }

        Thread myThread=new Thread(){//???????????????
            @Override
            public void run() {
                byte[] imgLen_int = new byte[16];
                byte[] array;
                Bitmap returnBmp;
                Integer leaveLen, imgLen;
                int cnt = 1;
                while (true) {
                    if (exitFlag)
                        break;
                    try {
                        while (bis.read(imgLen_int) == 0);
                        imgLen = Integer.parseInt((new String(imgLen_int, 0, 16)).trim());
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] b = new byte[1024];
                        int len;
                        leaveLen = imgLen;
                        while (leaveLen != 0) {
                            if (leaveLen <= 1024) {
                                b = new byte[leaveLen];
                            }
                            len = bis.read(b);
                            bos.write(b, 0, len);
                            leaveLen -= len;
                        }
                        array = bos.toByteArray();
                        bos.close();
                        returnBmp = BitmapFactory.decodeByteArray(array, 0, imgLen);
                        Mat recImg = new Mat();
//                        System.out.println("????????????"+cnt+"???");
                        ++cnt;
                        Utils.bitmapToMat(returnBmp, recImg);
                        if (recImg != null && img != null) {
                            Mat dest = new Mat();
                            Imgproc.resize(recImg,dest, new Size(1080, 1080));//?????????????????????????????????
                            receiveImg = dest.clone();
                        }
//                         reciveImg = Imgcodecs.imdecode(new MatOfByte(array), Imgcodecs.IMREAD_COLOR);
//                        Thread.sleep(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        myThread.start();//????????????
        }

    private void findView() {
        javaCameraView = findViewById(R.id.javaCameraView);
    }


    @Override
    public void onPause() {
//        Log.i(TAG, "onPause");
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
//        Log.i(TAG, "onResume");
        super.onResume();
        if (OpenCVLoader.initDebug()) {
//            Log.i(TAG, "initDebug true");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
//            Log.i(TAG, "initDebug false");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            exitFlag = true;
            socket.close();
            osw.close();
            bos.close();
            br.close();
            bis.close();
            CameraGetActivity.this.finish();
//            System.out.println("??????");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            CameraGetActivity.this.finish();
        }
        return false;
    }

}