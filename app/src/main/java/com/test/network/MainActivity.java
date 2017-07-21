package com.test.network;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity  implements View.OnClickListener {
    Button button_use_4G, button_use_WIFI, button_request_outerUrl, button_request_IntraUrl,button_app_4G,button_app_WIFI;
    private ProgressBar pgb;

    Uri uri;
    public static String outerUrl = "http://www.baidu.com/";
    public static String IntraUrl = "http://10.30.30.88:8888/";
    long startTime;
    long currentTime;
    long mobileWaitingTime = 29999;
    long wifiWaitingTime = 25000;

    // 权限检测器
    private PermissionsChecker mPermissionsChecker;

    //wifi管理对象
    WifiAdmin wifiAdmin;
    //网络监听实例
    NetworkConnectChangedReceiver networkConnectChangedReceiver;

    //wifiNAME
    public static final String WIFINAME = "FIND7";
    public static final String WIFIPASSWORD = "85860201";

    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET

            //Manifest.permission.WRITE_SETTINGS,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button_use_4G = (Button) findViewById(R.id.button_use_4G);
        button_use_WIFI = (Button) findViewById(R.id.button_use_WIFI);
        button_app_4G = (Button) findViewById(R.id.button_app_4G);
        button_app_WIFI = (Button) findViewById(R.id.button_app_WIFI);
        button_request_outerUrl = (Button) findViewById(R.id.button_request_outerUrl);
        button_request_IntraUrl = (Button) findViewById(R.id.button_request_IntraUrl);

        button_use_4G.setOnClickListener(this);
        button_use_WIFI.setOnClickListener(this);
        button_app_4G.setOnClickListener(this);
        button_app_WIFI.setOnClickListener(this);
        button_request_outerUrl.setOnClickListener(this);
        button_request_IntraUrl.setOnClickListener(this);

        this.pgb = (ProgressBar)findViewById(R.id.pgb);

        // 5.0以下不可验证
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            button_app_4G.setVisibility(View.GONE);
            button_app_WIFI.setVisibility(View.GONE);
            button_request_outerUrl.setVisibility(View.GONE);
            //button_request_IntraUrl.setVisibility(View.GONE);
        }

        //权限检测器
        mPermissionsChecker = new PermissionsChecker(this);
        mPermissionsChecker.activity = this;
        mPermissionsChecker.permissions = PERMISSIONS;
        mPermissionsChecker.judgePermission();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // 第一次请求权限时，用户如果拒绝，下一次请求shouldShowRequestPermissionRationale()返回true
            // 向用户解释为什么需要这个权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CHANGE_NETWORK_STATE)) {
                new android.support.v7.app.AlertDialog.Builder(this)
                        .setMessage("申请 CHANGE_NETWORK_STATE 权限")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //申请CHANGE_NETWORK_STATE权限
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.CHANGE_NETWORK_STATE}, 1);
                            }
                        })
                        .show();
            } else {
                //申请读取图库权限
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CHANGE_NETWORK_STATE},1);
            }
        } else {

        }
    }

    //在onResume()方法注册网络监听
    @Override
    protected void onResume() {
        if (networkConnectChangedReceiver == null) {
            networkConnectChangedReceiver = new NetworkConnectChangedReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkConnectChangedReceiver, filter);
        System.out.println("注册");
        super.onResume();
    }


    //onPause()方法注销网络监听
    @Override
    protected void onPause() {
        unregisterReceiver(networkConnectChangedReceiver);
        System.out.println("注销");
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionsChecker.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            //点击  使用4G 按钮
            case R.id.button_use_4G:
                choose4G(v);
                break;
            //点击  使用WIFI  按钮
            case R.id.button_use_WIFI:
                chooseWifi(v);
                break;
            case R.id.button_app_4G:
               //app切换4G访问
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    NetworkUtil.appUseMobileNetwork(this,outerUrl);
                }
                break;
            case R.id.button_app_WIFI:
                //app切换WIFI访问
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    NetworkUtil.appUseWifiNetwork(this,IntraUrl);
                }
                break;
            case R.id.button_request_outerUrl:
                //request 外网
                new Thread(){
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void run(){
                        //用netWork
                        //NetworkUtil.requestWithNetwork(MainActivity.this);
                        //不用netWork
                        NetworkUtil.executeHttpGet(MainActivity.this);
                    }
                }.start();

                break;
            case R.id.button_request_IntraUrl:
                if(NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME)){
                    showToast("已连接指定WIFI");
                }else{
                    showToast("未连接指定WIFI");
                }

                //request 内网，暂时访问的是外网网址
//                new Thread(){
//                    @RequiresApi(api = Build.VERSION_CODES.M)
//                    public void run(){
//                        //用netWork
//                        NetworkUtil.requestWithNetwork(MainActivity.this);
//                        //不用netWork
//                        //NetworkUtil.executeHttpGet(MainActivity.this);
//                    }
//                }.start();

                break;
            default:
                break;
        }
    }


    //选择4G的操作==================================================================================
    /**
     * 选择使用4G的方法
     * @param v
     */
    public void choose4G(View v){
        int netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        //如果当前优先级为WIFI
        if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
            //判断移动网络是否开启
            if(NetworkUtil.isMobileEnabled(this)){
                UseMobileAndMobileEnabled(v);
            }else{
                UseMobileAndMobileUnabled(v);
            }
        }else{
            UseMobileAndIsMobile(v,netWorkStatus);
        }
    }
    //当需使用4G且网络优先级为Mobile
    public void UseMobileAndIsMobile(View v, int netWorkStatus){
        switch (netWorkStatus){
            case NetworkConstants.NETWORK_CLASS_2_G:
                showToast("当前为2G网络，无法操作");
                break;
            case NetworkConstants.NETWORK_CLASS_3_G:
                showToast("当前为3G网络，无法操作");
                break;
            case NetworkConstants.NETWORK_CLASS_4_G:
                //直接跳转4G目标网络
                uri = Uri.parse(outerUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;
            default:
                UseMobileAndMobileUnabled(v);

                break;
        }
    }

    //当需使用4G,网络优先级为WIFI且移动网络未打开
    public void UseMobileAndMobileUnabled(View v){
        //判断当前版本
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            //andriod 5.0 （API 21）以下  可以通过代码打开移动网络
            showToast("正在打开移动网络");
            NetworkUtil.setMobileData(v.getContext(), true);

            //消除打开移动网络的延迟
            boolean isMobileEnabled = NetworkUtil.isMobileEnabled(this);
            startTime = System.currentTimeMillis();
            do{
                pgb.setVisibility(View.VISIBLE);
                currentTime = System.currentTimeMillis();
                if(currentTime - startTime > mobileWaitingTime) {
                    break;
                }
               isMobileEnabled = NetworkUtil.isMobileEnabled(this);
            }while(!isMobileEnabled);
            pgb.setVisibility(View.INVISIBLE);

            //判断移动网络是否开启
            if(isMobileEnabled){
                UseMobileAndMobileEnabled(v);
            }else {
                showToast("移动网络开启失败");
            }
        }else{
            //andriod 5.0 （API 21）以上  通过代码打开移动网络需要系统权限，所以只能跳转到网络设置页面，手动打开
            moveToNetworkSettings(v);
        }
    }

    //当需使用4G,网络优先级为WIFI且移动网络已打开
    public void UseMobileAndMobileEnabled(View v){
        //判断当前版本
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            //andriod 5.0 （API 21）以下  使用切换网络优先级
            showToast("网络切换中...");
            NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_MOBILE);

            //循环消除延迟
            int netWorkStatus = NetworkUtil.getNetWorkStatus(this);
            startTime = System.currentTimeMillis();
            do{
                pgb.setVisibility(View.VISIBLE);
                currentTime = System.currentTimeMillis();
                if(currentTime - startTime > mobileWaitingTime) {
                    break;
                }
                netWorkStatus = NetworkUtil.getNetWorkStatus(this);
            }while(netWorkStatus != NetworkConstants.NETWORK_CLASS_2_G
                    || netWorkStatus != NetworkConstants.NETWORK_CLASS_3_G
                    || netWorkStatus != NetworkConstants.NETWORK_CLASS_4_G);

            pgb.setVisibility(View.INVISIBLE);
            //showToast(NetworkConnectChangedReceiver.activeNetwork.getExtraInfo());
            //优先级为移动网络后的操作

            UseMobileAndIsMobile(v,netWorkStatus);
            //方法后需要重置网络优先级
        }else{
            //andriod 5.0 （API 21）以上  使用特殊方法访问外网
            NetworkUtil.useMobileNetwork(this);
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
//                        //判断 是否打开了移动网络，是就进行后续操作，不是就跳出，防止openMobile(v)里的耗时操作产生
//                        if(NetworkUtil.getMobileDataState(v.getContext(), null)){
//                            useMobile(v);
//                        }

                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“返回”后的操作,这里不设置没有任何操作

                    }
                }).show();
    }

    //选择WIFI的操作==================================================================================

    /**
     * 选择使用wifi的方法
     * @param v
     */
    public void chooseWifi(View v){
        int netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        //如果当前优先级为WIFI
        showToast(netWorkStatus+"");
        if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
            UseWifiAndIsWifi(v);
        }else{
            //判断WIFI是否开启
            if(wifiAdmin ==null){
                wifiAdmin = new WifiAdmin(this);
            }
            if(wifiAdmin.isWifiEnabled()){
                wifiIsEnabled(v,wifiAdmin);
            }else{
                wifiIsUnabled(v,wifiAdmin);
            }
        }

    }

    //当需使用WIFI,且网络优先级为WIFI
    public void UseWifiAndIsWifi(View v){
        //判断是否连接指定WIFI
        if(NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME)){
            isWiFiActive();
        }else{
            //连接指定WIFI
            ConnectToDesignatedWifi(v);
        }
    }
    //wifi已打开
    public void wifiIsEnabled(View v, WifiAdmin wifiAdmin){
        //判断当前版本
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            //andriod 5.0 （API 21）以下  提升WIFI 优先级
            showToast("网络切换中");
            NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_WIFI);

            //循环消除延迟
            int netWorkStatus = NetworkUtil.getNetWorkStatus(this);
            startTime = System.currentTimeMillis();
            do{
                pgb.setVisibility(View.VISIBLE);
                currentTime = System.currentTimeMillis();
                if(currentTime - startTime > mobileWaitingTime) {
                    break;
                }
                netWorkStatus = NetworkUtil.getNetWorkStatus(this);
            }while(netWorkStatus != NetworkConstants.NETWORK_WIFI);
            //showToast(NetworkConnectChangedReceiver.activeNetwork.getExtraInfo());
            pgb.setVisibility(View.INVISIBLE);


            //判断提升后的网络优先级
            if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
                UseWifiAndIsWifi(v);
            }else{
                showToast("网络切换失败，请检查网络。。。");
            }
        }else{
            //andriod 5.0 （API 21）以上  判断是否连接只当wifi
            UseWifiAndIsWifi(v);
        }
    }

    //wifi未打开
    public void wifiIsUnabled(View v,WifiAdmin wifiAdmin){
        //打开wifi
        wifiAdmin.openWifi();

        //消除开启wifi延迟
        startTime = System.currentTimeMillis();
        while(!wifiAdmin.isWifiEnabled()){
            //等待
            pgb.setVisibility(View.VISIBLE);
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > wifiWaitingTime) {
                break;
            }
        }
        pgb.setVisibility(View.INVISIBLE);
        //showToast(NetworkConnectChangedReceiver.activeNetwork.getExtraInfo());
        //判断WIFI是否被打开
        if(wifiAdmin.isWifiEnabled()){
            wifiIsEnabled(v,wifiAdmin);
        }else{
           showToast("WIFI打开失败。。。");
        }
    }


    //连接指定WIFI的方法
    public void ConnectToDesignatedWifi(View v){
        if(wifiAdmin == null){
            WifiAdmin wifiAdmin = new WifiAdmin(this);
            wifiIsUnabled(v,wifiAdmin);
        }
        wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(WIFINAME, WIFIPASSWORD, 3));

        startTime = System.currentTimeMillis();
        do{
            pgb.setVisibility(View.VISIBLE);
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > wifiWaitingTime) {
                break;
            }
        }while(NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME));
        pgb.setVisibility(View.INVISIBLE);

        if(NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME)){
            isWiFiActive();
        }else{
            showToast("连接指定WIFI失败。。。");
        }
    }


    //判断是wifi否联通~
    public void isWiFiActive(){
        if (NetworkUtil.isWiFiActive(this)) {
            //直接跳转wifi目标网络
            uri = Uri.parse(IntraUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else {
            //连接到指定WIFI
            showToast("WIFI未连通。。。，请检查WIFI网络。。。");
        }
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
}