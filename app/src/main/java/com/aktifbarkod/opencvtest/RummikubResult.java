package com.aktifbarkod.opencvtest;

import android.content.Intent;
import android.os.UserHandle;
import android.print.PrintJob;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AndroidException;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.content.res.Configuration;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

public class RummikubResult extends AppCompatActivity {

    public ListView listView;
    public ArrayList<String> cevaplar;
    public ImageClassifier cls;

    public  Scalar redl;
    public  Scalar redH;
    public  Scalar blackL;
    public  Scalar blackH;
    public  Scalar blueL;
    public  Scalar blueH;
    public  Scalar  orangeL;
    public  Scalar orangeH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rummikub_result);

        redl  = new Scalar(20,15,76);
        redH = new Scalar(68,45,121);
        blackL = new Scalar(0,0,0);
        blackH = new Scalar(66,67,0);
        blueL = new Scalar(66,67,0);
        blueH = new Scalar(113,200,200);
        orangeL = new Scalar(0,48,90);
        orangeH = new Scalar(42,78,141);

        listView = (ListView) findViewById(R.id.listView);
        try {
            cls = new ImageClassifier(this);
            cevaplar = new ArrayList<String>();
            processCapturedContours();

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cevaplar);
            listView.setAdapter(arrayAdapter);
        } catch (Exception ex) {
            String err = ex.getMessage();
            int ttt = 4;
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_rummikub_result);

        if (cevaplar.size() > 0) {
        } else {
            processCapturedContours();
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cevaplar);
        listView.setAdapter(arrayAdapter);
    }

    public int getColorValue(Mat img) {
        Mat popImg= new Mat(img.width(),img.height(),CvType.CV_8UC3);
        Mat hsvImg = new Mat();
        popImg= img.clone();
        Imgproc.cvtColor(popImg, hsvImg, Imgproc.COLOR_RGB2HSV);

        Mat redRange = new Mat();
        Core.inRange(hsvImg, redl, redH, redRange);
        //Imgproc.threshold(redRange,redRange,127,255,Imgproc.THRESH_BINARY);

        Mat blackRange= new Mat();
        Core.inRange(hsvImg, blackL, blackH, blackRange);
        //Imgproc.threshold(blackRange,blackRange,127,255,Imgproc.THRESH_BINARY);

        Mat blueRange = new Mat();
        Core.inRange(hsvImg, blueL, blueH, blueRange);
        //Imgproc.threshold(blueRange,blueRange,127,255,Imgproc.THRESH_BINARY);

        Mat orangeRange = new Mat();
        Core.inRange(hsvImg, orangeL, orangeH, orangeRange);
        //Imgproc.threshold(orangeRange,orangeRange,127,255,Imgproc.THRESH_BINARY);

        int redcount =Core.countNonZero(redRange);
        int blackcount=Core.countNonZero(blackRange);
        int bluecount=Core.countNonZero(blueRange);
        int orangecount=Core.countNonZero(orangeRange);

        int[] sonuclar = new int[4];
        sonuclar[0] = redcount;
        sonuclar[1] = blackcount;
        sonuclar[2] = blackcount;
        sonuclar[3] = orangecount;

        int largest = 0;
        for ( int i = 1; i < sonuclar.length; i++ )
        {
            if ( sonuclar[i] > sonuclar[largest] ) largest = i;
        }

        hsvImg.release();
        redRange.release();
        blackRange.release();
        blueRange.release();
        orangeRange.release();

        return largest; // position of the first largest found
    }

    public void processCapturedContours() {
        for (int i = StaticContour.ContourBoxesArrayList.size() - 1; i >= 0; i--) {
            ContourBox a_bolge = StaticContour.ContourBoxesArrayList.get(i);
            // Clone image here MORE IMPORTANTLY RELEASE AT END OF LOOP
            Mat contourImg= a_bolge.img.clone();

            // Color of image
            a_bolge.renk = getColorValue(contourImg);

            Imgproc.cvtColor(contourImg, contourImg, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(contourImg, contourImg, 127, 255, Imgproc.THRESH_OTSU);
            Imgproc.resize(contourImg, contourImg, new Size(128, 128));
            contourImg.convertTo(contourImg, CvType.CV_32FC(1));
            Imgproc.cvtColor(contourImg, contourImg, Imgproc.COLOR_GRAY2BGR);
            String cevap = cls.classifyFrame(contourImg);
            cevap += " //**" + a_bolge.renk;
            String sayiChar = String.valueOf(cevap.charAt(1));
            Integer sayi = Integer.parseInt(sayiChar);
            a_bolge.sayi = sayi;

            contourImg.release();
            cevaplar.add(cevap);
        }
        int bb = 5;
        int a = bb;
        //StaticContour.ContourBoxesArrayList.clear();

        cevaplar.clear();
        for (int i=0;i<StaticContour.ContourBoxesArrayList.size();i++)
        {
            boolean ciftiVar = false;
            ContourBox a_bolge = StaticContour.ContourBoxesArrayList.get(i);
            for (int j=0;j<StaticContour.ContourBoxesArrayList.size();j++)
            {
                ContourBox kutu = new ContourBox();
                ContourBox t_bolge = StaticContour.ContourBoxesArrayList.get(j);

                int farkx = Math.abs( a_bolge.x - t_bolge.x);
                int farky = Math.abs( a_bolge.y - t_bolge.y);

                if (farkx < 50 && farkx > 5 && farky < 20)
                {
                    ciftiVar = true;
                    int sayi = -1;
                    int ilkDigit = 1;
                    int sonDigit = -1;
                    int renk = -1;
                    if (a_bolge.sayi == 1)
                    {
                        ilkDigit = 1;
                        sonDigit = t_bolge.sayi;
                    }
                    else
                    {
                        ilkDigit = 1;
                        sonDigit = a_bolge.sayi;
                    }
                    sayi = ilkDigit * 10 + sonDigit;
                    renk = a_bolge.renk;
                    if (j > i)
                    {
                        kutu.sayi = sayi;
                        kutu.renk = renk;
                        String sonuc = sayi + " sayısının rengi " + renk;
                        cevaplar.add(sonuc);
                    }
                }
            }
            if (!ciftiVar)
            {
                String sonuc = a_bolge.sayi + " " + " sayısının rengi " + a_bolge.renk;
                cevaplar.add(sonuc);
            }
        }
    }
}
