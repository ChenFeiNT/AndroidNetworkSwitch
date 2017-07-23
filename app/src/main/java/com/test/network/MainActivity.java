package com.test.network;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

    public static String outerUrl2 = "http://www.baidu.com/";
    public String outerUrl = "http://php.weather.sina.com.cn/iframe/index/w_cl.php?code=js&day=0&city=&dfc=1&charset=utf-8";
    public static String intraUrl = "http://10.30.30.88:8888/";

    public static String testOuterHost = "www.baidu.com";
    public static String testIntraHost = "192.168.0.1";

    //定义当前网络状态
    int netWorkStatus;

    long startTime;
    long currentTime;
    long mobileWaitingTime = 29999;
    long wifiWaitingTime = 25000;

    // 权限检测器
    private PermissionsChecker mPermissionsChecker;

    //使用handler时首先要创建一个handler
    Handler handler = new Handler();

    //wifi管理对象
    WifiAdmin wifiAdmin;
    //网络监听实例
    NetworkConnectChangedReceiver networkConnectChangedReceiver;

    //wifiNAME
    public static final String WIFINAME = "1204";
    public static final String WIFIPASSWORD = "852011101";

    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET
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
                //申请读取network_change权限
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

    //权限请求的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionsChecker.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }




    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //点击  使用4G 按钮
            case R.id.button_use_4G:
                choose4G();
                break;
            //点击  使用WIFI  按钮
            case R.id.button_use_WIFI:
                chooseWifi();
                break;
            case R.id.button_app_4G:
               //app切换4G访问
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    NetworkUtil.setAppNetwork(this, NetworkCapabilities.TRANSPORT_CELLULAR);
                }
                break;
            case R.id.button_app_WIFI:
                //app切换WIFI访问
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    NetworkUtil.setAppNetwork(this, NetworkCapabilities.TRANSPORT_WIFI);
                }
                break;
            case R.id.button_request_outerUrl:
                //request 外网
                new Thread(){
                    public void run(){
                        //不用netWork
                        NetworkUtil.requestWithoutNetwork(MainActivity.this,outerUrl);
                    }
                }.start();

                break;
            case R.id.button_request_IntraUrl:
                new Thread(){
                    public void run(){
                        //不用netWork
                        NetworkUtil.requestWithoutNetwork(MainActivity.this,intraUrl);
                    }
                }.start();
                break;
            default:
                break;
        }
    }


    //选择4G的操作==================================================================================
    /**
     * 选择使用4G的方法
     */
    public void choose4G(){
        //判断是否能ping到百度
        netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        if(NetworkUtil.ping(testOuterHost,1)){
            //访问外网
            new Thread(){
                public void run(){
                    //不用netWork
                    NetworkUtil.requestWithoutNetwork(MainActivity.this,outerUrl);
                }
            }.start();
        }else{
            //判断当前网络状态
            if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
                //当前网络为WIFI
                UseMobileButIsWIFI();
            }else if (netWorkStatus == NetworkConstants.NETWORK_CLASS_2_G
                    || netWorkStatus == NetworkConstants.NETWORK_CLASS_3_G
                    || netWorkStatus == NetworkConstants.NETWORK_CLASS_4_G){
                //当前网络为移动
                UseMobileAndIsMobile();
            }else{
                UseMobileButIsUnknow();
            }
        }
    }

    //访问外网，ping不通，网络状态为WIFI
    public void UseMobileButIsWIFI(){
        //判断移动网络是否打开
        if(NetworkUtil.isMobileEnabled(this)){
            //移动网络已打开
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //5.0以下设置移动网络优先
                setPreferredNetworkToMobile();
                //判断当前网络状态
                switch (netWorkStatus){
                    case NetworkConstants.NETWORK_WIFI:
                        showToast("提升网络优先级失败。");
                        break;
                    case NetworkConstants.NETWORK_CLASS_2_G:
                        showToast("当前为2G网络，无法操作");
                        break;
                    case NetworkConstants.NETWORK_CLASS_3_G:
                        showToast("当前为3G网络，无法操作");
                        break;
                    case NetworkConstants.NETWORK_CLASS_4_G:
                        //访问目标网络
                        new Thread(){
                            public void run(){
                                //不用netWork
                                NetworkUtil.requestWithoutNetwork(MainActivity.this,outerUrl);
                            }
                        }.start();
                        //访问完需将优先级还原
                        break;
                    default:
                        showToast("服务器连接失败。");
                        break;
                }
            } else {
                //5.0以上设置应用级网络优先级 （network绑定为移动网络）
                NetworkUtil.setAppNetwork(this,NetworkCapabilities.TRANSPORT_CELLULAR);
                if(NetworkUtil.ping(testOuterHost,1)){
                    //访问目标网络
                    new Thread(){
                        public void run(){
                            //不用netWork
                            NetworkUtil.requestWithoutNetwork(MainActivity.this,outerUrl);
                        }
                    }.start();
                }else {
                    showToast("服务器连接失败。");
                }
            }
        }else {
            //移动网络未打开,准备打开移动网络
            readyTurnOnMobileData();
        }
    }

    //访问外网，ping不通，网络状态为Mobile
    public void UseMobileAndIsMobile(){
        //判断版本
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0以下
            showToast("服务器连接失败");
        }else{
            //5.0以上
            //判断应用优先级
            //设置移动网络为应用级网络优先级
            NetworkUtil.setAppNetwork(this,NetworkCapabilities.TRANSPORT_CELLULAR);
            if(NetworkUtil.ping(testOuterHost,1)){
                //访问目标网络
                new Thread(){
                    public void run(){
                        //不用netWork
                        NetworkUtil.requestWithoutNetwork(MainActivity.this,outerUrl);
                    }
                }.start();
            }else {
                showToast("服务器连接失败。");
            }
        }
    }

    //访问外网，ping不通，网络状态为Unknow
    public void UseMobileButIsUnknow(){
        //判断移动网络是否打开
        if(NetworkUtil.isMobileEnabled(this)){
            showToast("服务器连接失败。");
        }else{
            //移动网络未打开,准备打开移动网络
            readyTurnOnMobileData();
        }
    }

    //移动网络未打开,准备打开移动网络
    public void readyTurnOnMobileData(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0以下打开移动网络
            turnOnMobileData();

            //判断移动网络是否开启
            boolean isMobileEnabled = NetworkUtil.isMobileEnabled(this);
            if(isMobileEnabled){
                //重新走一遍选择内网方法
                choose4G();
            }else {
                showToast("移动网络开启失败");
            }
        } else {
            //andriod 5.0 （API 21）以上  通过代码打开移动网络需要系统权限，所以只能跳转到网络设置页面，手动打开
            moveToNetworkSettings();
        }
    }

    //选择WIFI的操作==================================================================================

    /**
     * 选择使用wifi的方法
     */
    public void chooseWifi(){
        //判断是否能ping到内网服务器
        netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        if(NetworkUtil.ping(testIntraHost,1)){
            //访问内网
            new Thread(){
                public void run(){
                    //不用netWork
                    NetworkUtil.requestWithoutNetwork(MainActivity.this,intraUrl);
                }
            }.start();
        }else{
            //判断当前网络状态
            if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
                //当前网络为WIFI
                UseWIFIAndIsWIFI();
            }else if (netWorkStatus == NetworkConstants.NETWORK_CLASS_2_G
                    || netWorkStatus == NetworkConstants.NETWORK_CLASS_3_G
                    || netWorkStatus == NetworkConstants.NETWORK_CLASS_4_G){
                //当前网络为移动
                UseWIFIButIsMobile();
            }else{
                //当前网络未知
                UseWIFIButIsUnknow();
            }
        }
    }

    //访问内网，ping不通，网络状态为WIFI
    public void  UseWIFIAndIsWIFI(){


        //判断是否连接指定WIFI
        if (NetworkUtil.isConnectedDesignatedWifi(this, WIFINAME)) {
            //判断版本
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //5.0以下，报错
                showToast("连接服务器失败。");
            } else {
                //初始化应用网络优先级（解绑应用network）
                initAppNetwork();
            }
        } else {
            wifiAdmin = new WifiAdmin(this);
            //连接到指定WIFI
            notConnectedDesignatedWifi(wifiAdmin);
        }
    }

    //初始化app network
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void initAppNetwork(){
        //初始化应用网络优先级（解绑应用network）
        NetworkUtil.removeAppNetworkSetting(this);
        //判断是否连接目标服务器（内网）
        if(NetworkUtil.ping(testIntraHost,1)){
            //访问内网
            new Thread(){
                public void run(){
                    //不用netWork
                    NetworkUtil.requestWithoutNetwork(MainActivity.this,intraUrl);
                }
            }.start();
        }else {
            showToast("服务器连接失败。");
        }
    }

    //访问内网，ping不通，网络状态为Mobile
    public void UseWIFIButIsMobile(){
        //判断WIFI是否开启
        wifiAdmin = new WifiAdmin(this);
        if(wifiAdmin.isWifiEnabled()){
            //判断版本
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                //5.0以下，提升WIFI为网络优先
                setPreferredNetworkToWIFI();

                //当前网络状态是否为WIFI
                if (netWorkStatus == NetworkConstants.NETWORK_WIFI){
                    //判断是否连接指定WIFI
                    if(NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME)){
                        //判断是否连接目标服务器（内网）
                        if(NetworkUtil.ping(intraUrl,1)){
                            //访问内网
                            new Thread(){
                                public void run(){
                                    //不用netWork
                                    NetworkUtil.requestWithoutNetwork(MainActivity.this,intraUrl);
                                }
                            }.start();
                        }else {
                            showToast("连接目标服务器失败。");
                        }
                    }else {
                        //连接指定WIFI
                        connectedDesignatedWifi( wifiAdmin);

                        //是否连接指定WIFI
                        if(NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME)){
                            //是否连接目标服务器（内网）
                            if(NetworkUtil.ping(testIntraHost,1)) {
                                //访问内网
                                new Thread(){
                                    public void run(){
                                        //不用netWork
                                        NetworkUtil.requestWithoutNetwork(MainActivity.this,intraUrl);
                                    }
                                }.start();
                            }else {
                                showToast("服务器连接失败。");
                            }
                        }else {
                            showToast("连接指定WIFI发生异常。");
                        }
                    }
                }else{
                    showToast("提升WIFI优先级失败。");
                }
            }else {
                //5.0以上，判断是否连接指定wifi
                if(NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME)){
                    //是否连接目标服务器（内网）
                    if(NetworkUtil.ping(testIntraHost,1)){
                        //为network绑定WIFI网络
                        NetworkUtil.setAppNetwork(this,NetworkCapabilities.TRANSPORT_WIFI);
                        //是否连接目标服务器 （内网）
                        if (NetworkUtil.ping(testIntraHost,1)) {
                            //访问内网
                            new Thread(){
                                public void run(){
                                    //不用netWork
                                    NetworkUtil.requestWithoutNetwork(MainActivity.this,intraUrl);
                                }
                            }.start();
                        }else {
                            showToast("服务器连接失败。");
                        }
                    }else{
                        showToast("服务器连接失败。");
                    }
                }else {
                    showToast("连接指定WIFI发生异常。");
                }
            }
        }else{

            //没有打开WIFI，就先打开WIFI
            notTurnOnWIFI(wifiAdmin);
        }
    }

    //访问内网，ping不通，网络状态为WIFI
    public void  UseWIFIButIsUnknow(){
        wifiAdmin = new WifiAdmin(this);
        //判断WIFI是否开启
        if(wifiAdmin.isWifiEnabled()){
            //判断是否连接指定WIFI
            if (NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME)){
                showToast("服务器连接失败。");
            }else{
                //连接到指定WIFI
                notConnectedDesignatedWifi(wifiAdmin);
            }
        }else {
            //没有打开WIFI，就先打开WIFI
            notTurnOnWIFI(wifiAdmin);
        }
    }

    //没有打开WIFI后的操作
    public void notTurnOnWIFI(WifiAdmin wifiAdmin){
        //打开WIFI
        turnOnWIFI(wifiAdmin);

        //判断WIFI 是否打开
        if(wifiAdmin.isWifiEnabled()){
            //重新走一遍选择外网方法
           chooseWifi();
        }else {
            showToast("打开WIFI失败。");
        }
    }

    //没有连接指定WIFI后的操作
    public void notConnectedDesignatedWifi(WifiAdmin wifiAdmin){
        connectedDesignatedWifi(wifiAdmin);

        //是否连接指定WIFI
        if(NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME)){
            //是否连接目标服务器（内网）
            if(NetworkUtil.ping(testIntraHost,1)){
                //访问内网
                new Thread(){
                    public void run(){
                        //不用netWork
                        NetworkUtil.requestWithoutNetwork(MainActivity.this,intraUrl);
                    }
                }.start();
            }else{
                //判断版本
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    showToast("连接连接服务器失败。");
                }else{
                    //初始化应用网络优先级（解绑应用network）
                    initAppNetwork();
                }
            }
        }else {
            showToast("连接指定WIFI发生异常。");
        }
    }

    //设置网络优先级为Mobile
    public void setPreferredNetworkToMobile(){
        NetworkUtil.setPreferredNetwork(this, ConnectivityManager.TYPE_MOBILE);
        //循环等待
        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
            netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        }while(netWorkStatus != NetworkConstants.NETWORK_CLASS_2_G
                || netWorkStatus != NetworkConstants.NETWORK_CLASS_3_G
                || netWorkStatus != NetworkConstants.NETWORK_CLASS_4_G);
    }

    //设置网络优先级为WIFI
    public void setPreferredNetworkToWIFI() {
        NetworkUtil.setPreferredNetwork(this, ConnectivityManager.TYPE_WIFI);
        //循环等待
        startTime = System.currentTimeMillis();
        do {
            currentTime = System.currentTimeMillis();
            if (currentTime - startTime > mobileWaitingTime) {
                break;
            }
            netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        } while (netWorkStatus != NetworkConstants.NETWORK_WIFI);
    }

    //打开移动网络
    public void turnOnMobileData(){
        NetworkUtil.setMobileData(this, true);

        //消除打开移动网络的延迟
        boolean isMobileEnabled = NetworkUtil.isMobileEnabled(this);
        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
            isMobileEnabled = NetworkUtil.isMobileEnabled(this);
        }while(!isMobileEnabled);
    }

    //打开移动网络
    public void turnOnWIFI(WifiAdmin wifiAdmin){
        wifiAdmin.openWifi();

        //消除开启wifi延迟
        startTime = System.currentTimeMillis();
        while(!wifiAdmin.isWifiEnabled()){
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > wifiWaitingTime) {
                break;
            }
        }
    }

    //连接到指定WIFI
    public void connectedDesignatedWifi(WifiAdmin wifiAdmin) {
        //判断版本
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //android 6 之前连接指定WIFI的方法
            wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(WIFINAME, WIFIPASSWORD, 3));
        }else{
            //android 6 之后连接指定WIFI的方法
            wifiAdmin.addNetWorkAndConnectOnAndroidM(WIFINAME, WIFIPASSWORD,3);
        }
        //循环等待，消除延迟
        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
        }while(!NetworkUtil.isConnectedDesignatedWifi(this,WIFINAME));
    }

    //是否跳转移动网络设置
    public void moveToNetworkSettings() {
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