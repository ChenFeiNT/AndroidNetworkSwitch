package com.test.network;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener{
    Button btn1, btn2;
    Toast tst;

    Uri uri;
    String outerUrl = "http://www.baidu.com";
    String IntraUrl = "http://10.30.30.88:8888/";
    long startTime;
    long currentTime;
    long mobileWaitingTime = 10000;
    long wifiWaitingTime = 20000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn1 = (Button) findViewById(R.id.button1);
        btn2 = (Button) findViewById(R.id.button2);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        int netWorkStatus =  NetworkUtil.getNetWorkStatus(this);


        switch (v.getId()) {
            case R.id.button1:
                if(netWorkStatus == NetworkConstants.NETWORK_CLASS_4_G){
                    uri = Uri.parse(outerUrl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }else{
                    tst = Toast.makeText(this, "当前非4G网络，网络切换中...", Toast.LENGTH_SHORT);
                    tst.show();

                    switch (netWorkStatus) {
                         case NetworkConstants.NETWORK_WIFI:
                             openMobile(v,netWorkStatus);
                             break;
                         case NetworkConstants.NETWORK_CLASS_2_G:
                             tst = Toast.makeText(this, "当前为2G网络，无法操作。", Toast.LENGTH_SHORT);
                             tst.show();
                             break;
                         case NetworkConstants.NETWORK_CLASS_3_G:
                             tst = Toast.makeText(this, "当前为3G网络，无法操作。", Toast.LENGTH_SHORT);
                             tst.show();
                             break;
                         case NetworkConstants.NETWORK_CLASS_4_G:
                             //永远不会跑进来
                             break;
                         default:
                             openMobile(v,netWorkStatus);
                             break;
                    }
                }
                break;
            case R.id.button2:
                if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
                    uri = Uri.parse(IntraUrl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }else{
                    tst = Toast.makeText(this, "当前非WIFI网络，网络切换中...", Toast.LENGTH_SHORT);
                    tst.show();

                    //判断当前是否是未开网络状态
                    if(netWorkStatus == NetworkConstants.NETWORK_CLASS_UNKNOWN) {
                        //添加WIFI打开方法
                        new WifiAdmin(v.getContext()).openWifi();
                    }else{
                        NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_WIFI);
                    }

                    //循环等待wifi连通，或连接超时
                    startTime = System.currentTimeMillis();
                    do{
                        currentTime = System.currentTimeMillis();
                        if(currentTime - startTime > wifiWaitingTime) {
                            break;
                        }
                    }while(!NetworkUtil.isWiFiActive(this));

                    //如果wifi已连通
                    if(NetworkUtil.isWiFiActive(this)){
                        uri = Uri.parse(IntraUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }else{
                        tst = Toast.makeText(this, "WIFI未连接，请设置WIFI。", Toast.LENGTH_SHORT);
                        tst.show();
                        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                    }
                }
                break;
            default:
                break;
        }
    }

    //打开移动网络后的判断及跳转方法
    public void openMobile(View v, int netWorkStatus){
        //判断4G是否已被打开,如果没打开4G，就先把4G打开
        if(!NetworkUtil.getMobileDataState(v.getContext(), null)){
            tst = Toast.makeText(this, "正在打开移动网络", Toast.LENGTH_SHORT);
            tst.show();
            NetworkUtil.setMobileData(v.getContext(),true);
        }

        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
        }while(!NetworkUtil.getMobileDataState(v.getContext(), null));

        //判断打开移动网络是否超时，如果不超时，就提升MOBILE优先级
        if(NetworkUtil.getMobileDataState(v.getContext(), null)){
            //如果在WIFI开启的状态下就提升MBILE的优先级
            if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
                NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_MOBILE);
            }
        }else{
            tst = Toast.makeText(this, "打开移动网络超时，请确认网络...", Toast.LENGTH_SHORT);
            tst.show();
            return;
        }

        //循环等待网络状态改变
        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
            netWorkStatus =  NetworkUtil.getNetWorkStatus(this);
        }while(netWorkStatus != NetworkConstants.NETWORK_CLASS_2_G
                && netWorkStatus != NetworkConstants.NETWORK_CLASS_3_G
                && netWorkStatus != NetworkConstants.NETWORK_CLASS_4_G);

        switch (netWorkStatus){
            case NetworkConstants.NETWORK_CLASS_2_G:
                tst = Toast.makeText(this, "当前为2G网络，无法操作。", Toast.LENGTH_SHORT);
                tst.show();
                break;
            case NetworkConstants.NETWORK_CLASS_3_G:
                tst = Toast.makeText(this, "当前为3G网络，无法操作。", Toast.LENGTH_SHORT);
                tst.show();
                break;
            case NetworkConstants.NETWORK_CLASS_4_G:
                uri = Uri.parse(outerUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

                //执行完毕，将wifi级别升回来
               // NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_WIFI);
                break;
            default:
                tst = Toast.makeText(this, "打开移动网络超时，请确认网络...", Toast.LENGTH_SHORT);
                tst.show();
                break;
        }
    }
}