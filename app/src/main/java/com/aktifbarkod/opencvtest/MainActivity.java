package com.aktifbarkod.opencvtest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    // Mat objects to use in globally
    public Mat ResimOrj;
    public Mat ResimNotModified;
    public Mat ResimGray;
    public Mat ResimMask;
    public Mat RoiMat;
    public Mat RoiMatTmp;

    // Contour opeartions
    public Mat hierarchy;
    public List<MatOfPoint> contours;

    // For masking operation
    public Scalar lower = new Scalar(90, 90, 90);
    public Scalar upper = new Scalar(255, 255, 255);

    // For color classification
    public CameraBridgeViewBase cameraView;
    public BaseLoaderCallback mLoaderCallback;

    // Button
    public Button cekButton;

    // Globals
    public boolean processCurrentFrame = false;
    public boolean ilkDefa = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkCameraPermission();

        cekButton = (Button) findViewById(R.id.cekButton);
        cekButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processCurrentFrame = true;
            }
        });
    }

    private void checkCameraPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        0);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            cameraView = (JavaCameraView) findViewById(R.id.camera);
            cameraView.setVisibility(SurfaceView.VISIBLE);
            cameraView.setCvCameraViewListener(this);

            mLoaderCallback = new BaseLoaderCallback(this) {

                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS: {
                            Log.i("", "Opencv loading success.");
                            cameraView.enableView();
                            break;
                        }
                        default: {
                            Log.i("OPENCV", "Opencv loading failed!");
                            super.onManagerConnected(status);
                        }
                        break;
                    }
                }
            };
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        ResimOrj = new Mat(width, height, CvType.CV_8UC1);
        ResimGray = new Mat(width, height, CvType.CV_8UC1);
        ResimMask = new Mat(width, height, CvType.CV_8UC1);
        ResimNotModified = new Mat(width, height, CvType.CV_8UC1);

        RoiMat = new Mat();
        RoiMatTmp = new Mat();
        hierarchy = new Mat();
        contours = new ArrayList<MatOfPoint>();

    }

    @Override
    public void onCameraViewStopped() {
        ResimOrj.release();
        ResimGray.release();
        ResimMask.release();
        RoiMat.release();
        RoiMatTmp.release();
    }

    private void assignMatObject(Mat frame) {
        Imgproc.cvtColor(frame, ResimNotModified, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(frame, ResimOrj, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(ResimOrj, ResimGray, Imgproc.COLOR_BGR2GRAY);
    }

    private void applyColorFilter() {
        //Imgproc.cvtColor(ResimOrj, ResimOrj, Imgproc.COLOR_BGR2HSV);
        Core.inRange(ResimOrj, lower, upper, ResimMask);
        //Imgproc.threshold(ResimMask, ResimMask, 127, 255, Imgproc.THRESH_OTSU);
    }

    private void findContours() {
        contours.clear();
        Imgproc.findContours(ResimMask, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
    }

    private void processCountours() {
        StaticContour.ContourBoxesArrayList.clear();
        ContourBox contourBox;
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = new MatOfPoint(contours.get(i).toArray());
            Rect rect = Imgproc.boundingRect(contour);
            double area = Imgproc.contourArea(contour);
            double ratio = rect.height * 1.0 / rect.width;
            if (rect.height > rect.width && area > 305 && area < 1050 && ratio > 1.05 && ratio < 4) {
                    /*
                    RoiMat = ResimGray.submat(rect).clone();
                    Imgproc.threshold(RoiMat, RoiMat, 127, 255, Imgproc.THRESH_OTSU);
                    Imgproc.cvtColor(RoiMat, RoiMat, Imgproc.COLOR_GRAY2RGB);
                    Imgproc.resize(RoiMat, RoiMat, new Size(128, 128));
                    RoiMat.convertTo(RoiMatTmp, CvType.CV_32FC(1));
                    */

                contourBox = new ContourBox();
                contourBox.img= ResimOrj.submat(rect).clone();
                contourBox.x = rect.x;
                contourBox.y = rect.y;
                contourBox.w = rect.width;
                contourBox.h = rect.height;

                StaticContour.ContourBoxesArrayList.add(contourBox);
                // convert to bitmap the roi positions
                String cevap = "";

                //cevap = cls.classifyFrame(RoiMatTmp);
                Log.println(Log.DEBUG, "CLS : ", cevap);

                Imgproc.rectangle(ResimOrj, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 3);
                //Log.println(descriptors.size());
                System.gc();
            }
        }
        if (processCurrentFrame) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    processCurrentFrame = false;
                    Intent intent = new Intent(cekButton.getContext(), RummikubResult.class);
                    startActivity(intent);
                }
            });
        }

    }



    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        assignMatObject(frame);
        applyColorFilter();
        findContours();
        processCountours();
        returnToOriginalFrame();
        return ResimOrj;
    }

    private void returnToOriginalFrame() {
        Imgproc.cvtColor(ResimOrj,ResimOrj,Imgproc.COLOR_BGR2RGBA);
    }
}

