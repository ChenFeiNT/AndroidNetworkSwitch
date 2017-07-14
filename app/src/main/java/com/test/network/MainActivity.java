package com.test.network;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener{
    Button button_use_4G, button_use_WIFI, testButton;
    Toast tst;

    Uri uri;
    String outerUrl = "http://www.baidu.com";
    String IntraUrl = "http://10.30.30.88:8888/";
    long startTime;
    long currentTime;
    long mobileWaitingTime = 9999;
    long wifiWaitingTime = 15000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_use_4G = (Button) findViewById(R.id.button_use_4G);
        button_use_WIFI = (Button) findViewById(R.id.button_use_WIFI);
        testButton = (Button) findViewById(R.id.testButton);
        button_use_4G.setOnClickListener(this);
        button_use_WIFI.setOnClickListener(this);
        testButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        //获取当前网络状态：0: 未知网络,  1: WIFI, 2: 2G, 3: 3G, 4: 4G
        int netWorkStatus =  NetworkUtil.getNetWorkStatus(this);


        switch (v.getId()) {
            //点击  使用4G 按钮
            case R.id.button_use_4G:
                //判断当前网络是否为 4G
                if(netWorkStatus == NetworkConstants.NETWORK_CLASS_4_G){
                    //如果是4G跳转外网
                    uri = Uri.parse(outerUrl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }else{
                    //如果网络状态不是4G，进行切换
                    tst = Toast.makeText(this, "当前非4G网络，网络切换中...", Toast.LENGTH_SHORT);
                    tst.show();

                    //网络状态不是4G，根据不同状态进行相应操作
                    switch (netWorkStatus) {
                        //当网络状态是WIFI时
                         case NetworkConstants.NETWORK_WIFI:
                             //判断4G是否已被打开,如果没打开4G，就先把4G打开
                             if(!NetworkUtil.getMobileDataState(v.getContext(), null)){
                                 if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                     //andriod 5.0 （API 21）以下  可以通过代码打开移动网络
                                     tst = Toast.makeText(this, "正在打开移动网络", Toast.LENGTH_SHORT);
                                     tst.show();
                                     NetworkUtil.setMobileData(v.getContext(), true);
                                     //打开移动网络后的操作
                                     openMobile(v);
                                 }else{
                                     //andriod 5.0 （API 21）以上  通过代码打开移动网络需要系统权限，所以只能跳转到网络设置页面，手动打开
                                     moveToNetworkSettings(v);
                                 }
                             }else{
                                 //移动网络开启状态后的操作
                                 openMobile(v);
                             }

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
                             //判断4G是否已被打开,如果没打开4G，就先把4G打开
                             if(!NetworkUtil.getMobileDataState(v.getContext(), null)){
                                 if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                     //andriod 5.0 （API 21）以下  可以通过代码打开移动网络
                                     tst = Toast.makeText(this, "正在打开移动网络", Toast.LENGTH_SHORT);
                                     tst.show();
                                     NetworkUtil.setMobileData(v.getContext(), true);
                                     //打开移动网络后的操作
                                     openMobile(v);
                                 }else{
                                     //andriod 5.0 （API 21）以上  通过代码打开移动网络需要系统权限，所以只能跳转到网络设置页面，手动打开
                                     moveToNetworkSettings(v);
                                 }
                             }
                             //openMobile(v,netWorkStatus);
                             break;
                    }
                }
                break;
            //点击  使用WIFI  按钮
            case R.id.button_use_WIFI:
                //判断当前网络是否为WIFI
                if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
                    uri = Uri.parse(IntraUrl);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }else{
                    //如果不为WIFI,进行切换
                    showToast("当前非WIFI网络，网络切换中...");

                    int wifiState = new WifiAdmin(v.getContext()).checkState();
                    //如果wifi状态不是正在打开或已打开的情况
                    if(wifiState!= WifiManager.WIFI_STATE_ENABLED || wifiState!=WifiManager.WIFI_STATE_ENABLING) {
                        //添加WIFI打开方法
                        new WifiAdmin(v.getContext()).openWifi();

                        //消除开启wifi延迟
                        startTime = System.currentTimeMillis();
                        do{
                            currentTime = System.currentTimeMillis();
                            if(currentTime - startTime > wifiWaitingTime) {
                                break;
                            }
                            netWorkStatus =  NetworkUtil.getNetWorkStatus(this);
                        }while(netWorkStatus != NetworkConstants.NETWORK_WIFI);

                        //如果kaiqiWIFI后优先级不是wifi，就提升WIFI优先级
                        if(netWorkStatus != NetworkConstants.NETWORK_WIFI){
                            NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_WIFI);
                        }
                    }else{
                        //如果不是未知网络（即2G/3G/4G）,就把WIFI优先级提升回来。
                        NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_WIFI);
                    }

                    //此处后续可能添加连接指定WIFI的方法

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
                        showToast("WIFI未连接，请设置WIFI。");
                        startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                    }
                }
                break;
            case R.id.testButton:
                showToast(getLocalIpAddress());
                break;
            default:
                break;
        }
    }

    //打开移动网络后的判断及跳转方法
    public void openMobile(View v){

        //循环等待移动网络开关状态变更（打开移动网络会产生延迟）
        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
        }while(!NetworkUtil.getMobileDataState(v.getContext(), null));

        //获取当前网络优先级状态：0: 未知网络,  1: WIFI, 2: 2G, 3: 3G, 4: 4G
        int netWorkStatus =  NetworkUtil.getNetWorkStatus(this);


        //判断打开移动网络是否超时，如果不超时，移动网络被打开
        if(NetworkUtil.getMobileDataState(v.getContext(), null)){
            //在移动网络被打开的状态下，如果网络优先级是WIFI就提升MOBILE的优先级
            if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
                showToast("提升");
                NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_MOBILE);
            }

            //如果WIFI没开启，就保持默认优先级（即移动网络优先）
        }else{
            //如果超时，跳出警告
            tst = Toast.makeText(this, "打开移动网络超时，请确认网络...", Toast.LENGTH_SHORT);
            tst.show();
            return;
        }

        //循环等待网络状态改变，消除延迟
        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
            netWorkStatus =  NetworkUtil.getNetWorkStatus(this);
            //当网络状态变更为2G或3G或4G（即移动数据），或超时（未知网络或WIFI）跳出
        }while(netWorkStatus != NetworkConstants.NETWORK_CLASS_2_G
                && netWorkStatus != NetworkConstants.NETWORK_CLASS_3_G
                && netWorkStatus != NetworkConstants.NETWORK_CLASS_4_G);

        showToast("当前为优先级状态提升并循环后"+netWorkStatus+"");
        //根据不同网络状态，进行操作
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

                //执行完毕，将wifi级别升回来  （此方法要根据WIFI是否开启调用，或在后续的回调函数中调用）
                //警告：因为访问网址是用时间上的延迟，所以在WIFI和4G同时开启，4G优先级被提升，访问网址后立即使用此方法，
                //      会在访问延迟的时间段内切换回WIFI，有可能会导致访问无法完成（如WIFI未连接外网等）。
                //NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_WIFI);
                break;
            default:
                tst = Toast.makeText(this, "网络错误，请确认网络...", Toast.LENGTH_SHORT);
                tst.show();
                break;
        }
    }

    //是否跳转移动网络设置
    public void moveToNetworkSettings(final View v) {
          new AlertDialog.Builder(this).setTitle("是否前往打开移动网络？")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“确认”后的操作，跳转移动网络设置
                        startActivity(new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS));
                        dialog.dismiss();

                        //判断 是否打开了移动网络，是就进行后续操作，不是就跳出，防止openMobile(v)里的耗时操作产生
                        if(NetworkUtil.getMobileDataState(v.getContext(), null)){
                            openMobile(v);
                        }

                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“返回”后的操作,这里不设置没有任何操作

                    }
                }).show();
    }

    //显示Toast
    public void showToast(final String msg){
        new Thread(){
            public void run(){
                Looper.prepare();//给当前线程初始化Looper
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();;//Toast初始化的时候会new Handler();无参构造默认获取当前线程的Looper，如果没有prepare过，则抛出题主描述的异常。上一句代码初始化过了，就不会出错。
                Looper.loop();//这句执行，Toast排队show所依赖的Handler发出的消息就有人处理了，Toast就可以吐出来了。但是，这个Thread也阻塞这里了，因为loop()是个for (;;) ...
            }
        }.start();
    }

    public List<NetworkInterface> getNetworkInterfaceList() {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            String msg ="";
            for (NetworkInterface networkInterface : networkInterfaces)
            {
                String displayName = networkInterface.getDisplayName();
                msg += displayName+" / ";
            }
            showToast(msg);
            return networkInterfaces;
        } catch (SocketException e) {
            showToast(e.toString());
            e.printStackTrace();
        }
        return null;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {

           // Log.e("WifiPreference IpAddress", ex.toString());
        }

        return null;
    }
}