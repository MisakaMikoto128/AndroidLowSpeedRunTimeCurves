package com.example.mycurves;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.DashPathEffect;
import android.graphics.PathDashPathEffect;
import android.os.Bundle;
import android.util.Log;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Tets CurvesView
        CurvesView curvesView = findViewById(R.id.mCurve);
//  配置坐标系
        curvesView.setupCoordinator("日", "人",0f, 5f, 10f, 15f, 20f, 25f, 30f);
        // 添加曲线, 确保纵坐标的数值位数相等
        curvesView.addWave(ContextCompat.getColor(this, R.color.red), false,
                0f, 10f, 30f, 54f, 30f, 100f, 10f);

//  Test RectView
//        RectView rectView = findViewById(R.id.mCurve);


        RunTimeCurvesView RTCurvesView = findViewById(R.id.mRTCurve);
//  配置坐标系
        RTCurvesView.setCoordinator("日", "人",10,15,"int","int");
        int curve1 = RTCurvesView.createCurve(0xFFFF00FF,false);


        new Thread() {
            int i = 0;
            Random random = new Random();
            @Override
            public void run() {
                while (true){
                    i++;
//                    Log.e("LCTest",""+i);
                    RTCurvesView.push2Curve(curve1,random.nextFloat()*999%100);
                    if (i== 4){
                        RTCurvesView.gridOn(false);
                    }
                    if (i == 8){
                        RTCurvesView.gridOn(true);
                        RTCurvesView.setXDivNum(30);

                    }
                    if (i==12){
                        RTCurvesView.setPathEffect(new DashPathEffect(new float[] {20, 10}, 1));
                    }
                    try {
                        Thread.sleep(10);


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();
    }
}