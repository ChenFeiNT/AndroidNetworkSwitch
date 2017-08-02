package com.test.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

/**
 * Created by softwise on 2017/8/2.
 */

public class NetworkSwitchUtil implements HttpCallBackListener {
    
    private Context context;
    private Handler mHandler;
    private Activity activity;

    public NetworkSwitchUtil(Activity activity, Handler mHandler){
        this.context = activity;
        this.mHandler = mHandler;
        this.activity = activity;
    }

    //最终返回的结果
    private static int FLAG = 0;
    private static final int TRUE = 1;
    private static final int FALSE = 0;
    private static String NEED = "" ;

    //使用request请求外网百度返回 为“  ” ，改为 天气端口测试外网
    public String outerUrl3 = "http://10.30.100.67:8088/api/index.jsp";
    public static String outerUrl2 = "http://www.baidu.com/";
    public String outerUrl = "http://php.weather.sina.com.cn/iframe/index/w_cl.php?code=js&day=0&city=&dfc=1&charset=utf-8";

    public static String intraUrl = "http://10.30.30.88:8888/";
    //连接FIND测试时内网指定为天气端口，Find7开关移动网络进行区分
    public static String intraUrl2 = "http://php.weather.sina.com.cn/iframe/index/w_cl.php?code=js&day=0&city=&dfc=1&charset=utf-8";

    //测试ping通时使用的ip
    public static String testOuterHost = "www.baidu.com";
    public static String testIntraHost = "10.30.30.88";
    //连接FIND7 开移动网络PING百度
    //public static String testIntraHost = "www.baidu.com";

    //指定WIFI
    //wifiNAME
    public static final String WIFINAME = "PROJECT-SOFTWISE-NT";
    //public static final String WIFINAME = "FIND7";
    public static final String WIFIPASSWORD = "Project0818wise";


    //定义当前网络状态
    int netWorkStatus;
    //设置跳转到网络设置页面的 requestCode，用于onActivityResult回调时的判断
    public static final int ACTION_DATA_ROAMING_SETTINGS = 0;

    //判断超时使用
    long startTime;
    long currentTime;
    long mobileWaitingTime = 29999;


    Object lock = new Object();
    //子线程
    public Thread childThread ;
    //wifi管理对象
    WifiAdmin wifiAdmin;
    //网络监听实例
    NetworkConnectChangedReceiver networkConnectChangedReceiver;

    //在onResume()方法注册网络监听
    protected void onResume() {
        if (networkConnectChangedReceiver == null) {
            networkConnectChangedReceiver = new NetworkConnectChangedReceiver(netWorkHandler);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(networkConnectChangedReceiver, filter);
        System.out.println("注册");
      
    }


    //onPause()方法注销网络监听
    protected void onPause() {
        context.unregisterReceiver(networkConnectChangedReceiver);
        System.out.println("注销");
       
    }

    //网络监听Handler
    private Handler netWorkHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case NetworkConstants.NETWORK_MOBILE_STATE_ENABLED:
                    if(NEED.equals("turnOnMobileData")
                            && childThread.getState().equals(Thread.State.WAITING) ) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    //在这里写，防止系统设置后返回，在设置画面睡2.5秒，造成假死现象，影响用户体验
                    if(NEED.equals("moveToNetworkSettings")
                            && childThread.getState().equals(Thread.State.WAITING) ) {
                        synchronized (lock) {
                            lock.notify();
                            if(NetworkUtil.isMobileEnabled(context)){
                                try {
                                    childThread.sleep(2500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                UseMobileButNot();
                            }
                        }
                    }
                    break;
                case NetworkConstants.NETWORK_WIFI_STATE_ENABLED:
                    if((NEED.equals("turnOnWIFI"))
                            && childThread.getState().equals(Thread.State.WAITING) ) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    break;
                case NetworkConstants.NETWORK_WIFI_STATE_ISCONNECTED:
                    if((NEED.equals("connectedDesignatedWifi")||NEED.equals("setPreferredNetworkToWIFI") )
                            && childThread.getState().equals(Thread.State.WAITING) ) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    break;
            }
        }
    };

    //抽出的总入口，0为外网，1为内网
    public void netIsConneted(final int type){
        //新建操作线程
        childThread = new Thread(){
            public void run() {
                Looper.prepare();//给当前线程初始化Looper
                switch (type) {
                    case 0:
                        FLAG = FALSE;
                        choose4G();
                        mHandler.sendEmptyMessage(FLAG);
                        //showToast(FLAG + "");
                        break;
                    case 1:
                        FLAG = FALSE;
                        chooseWifi();
                        mHandler.sendEmptyMessage(FLAG);
                        //showToast(FLAG + "");
                        break;
                    default:
                }
                Looper.loop();//开始消息轮询
            }
        };
        childThread.start();
    }


    //选择4G的操作==================================================================================
    /**
     * 选择使用4G的方法
     */
    public void choose4G(){
        //判断是否能ping到百度
        netWorkStatus = NetworkUtil.getNetWorkStatus(context);
        if(NetworkUtil.ping(testOuterHost,1)){
            //访问外网
            //不用netWork
            //判断版本
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //5.0以下  访问
                request(outerUrl);
            }else{
                //5.0以上  解绑应用network;防止与系统不一致，造成无法访问
                NetworkUtil.removeAppNetworkSetting(context);
                NetworkUtil.setAppNetwork(context, NetworkCapabilities.TRANSPORT_CELLULAR);
                //请求目标网络
                request(outerUrl);
            }

        }else{

            //判断当前网络状态
            if (netWorkStatus == NetworkConstants.NETWORK_CLASS_2_G
                    || netWorkStatus == NetworkConstants.NETWORK_CLASS_3_G
                    || netWorkStatus == NetworkConstants.NETWORK_CLASS_4_G){
                //当前网络为移动
                UseMobileAndIsMobile();
            }else{
                //非移动网络
                UseMobileButNot();
            }
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
            //先将network解绑，不然会导致设置移动网络延迟
            //设置移动网络为应用级网络优先级
            NetworkUtil.removeAppNetworkSetting(context);
            NetworkUtil.setAppNetwork(context,NetworkCapabilities.TRANSPORT_CELLULAR);
            //请求目标网络
            request(outerUrl);
        }
    }

    //访问外网，ping不通，网络状态不为Mobile
    public void UseMobileButNot(){
        //判断移动网络是否打开
        if(NetworkUtil.isMobileEnabled(context)){
            mobileTurnedOn();
        }else {
            //移动网络未打开,准备打开移动网络
            readyTurnOnMobileData();
        }
    }




    //访问外网，ping不通，网络状态不为Mobile,mobile已打开状态
    public void mobileTurnedOn(){
        //移动网络已打开
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0以下设置移动网络优先
            setPreferredNetworkToMobile();
            netWorkStatus = NetworkUtil.getNetWorkStatus(context);
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
                    //请求目标网络
                    request(outerUrl);
                    break;
                default:
                    showToast("服务器连接失败。");
                    break;
            }
        } else {
            //5.0以上设置应用级网络优先级 （network绑定为移动网络）
            //先将network解绑，不然会导致设置移动网络延迟
            NetworkUtil.removeAppNetworkSetting(context);
            NetworkUtil.setAppNetwork(context,NetworkCapabilities.TRANSPORT_CELLULAR);
            try {
                childThread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //请求目标网络
            request(outerUrl);
        }
    }

    //访问外网，ping不通，网络状态不为Mobile,mobile未打开状态，准备打开移动网络
    public void readyTurnOnMobileData(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0以下打开移动网络
            turnOnMobileData();

            //判断移动网络是否开启
            boolean isMobileEnabled = NetworkUtil.isMobileEnabled(context);
            if(isMobileEnabled){
                //重新走一遍上一层 mobile已打开方法
                mobileTurnedOn();
            }else {
                showToast("移动网络开启失败");
            }
        } else {
            //andriod 5.0 （API 21）以上  通过代码打开移动网络需要系统权限，所以只能跳转到网络设置页面，手动打开
            moveToNetworkSettings();
        }
    }

    //是否跳转移动网络设置
    public void moveToNetworkSettings() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                Looper.prepare();//给当前线程初始化Looper
                new AlertDialog.Builder(context).setTitle("是否前往打开移动网络？")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 点击“确认”后的操作，跳转移动网络设置
                                activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS),ACTION_DATA_ROAMING_SETTINGS);
                                dialog.dismiss();

                            }
                        })
                        .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 点击“返回”后的操作,这里不设置没有任何操作
                                if(NEED.equals("moveToNetworkSettings")
                                        && childThread.getState().equals(Thread.State.WAITING) ) {
                                    synchronized (lock) {
                                        lock.notify();
                                    }
                                }
                            }
                        }).show();
                Looper.loop();//开始消息轮询
            }
        }).start();

        //线程等待点击结果
        synchronized (lock) {
            try {
                NEED = "moveToNetworkSettings";
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //使用网络监听 给handler发送消息来notify()
        }
    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ACTION_DATA_ROAMING_SETTINGS ){
            //在Handler里设置了
        }
    }



    //选择WIFI的操作==================================================================================

    /**
     * 选择使用wifi的方法
     */
    public void chooseWifi(){
        //判断是否能ping到内网服务器
        netWorkStatus = NetworkUtil.getNetWorkStatus(context);
        if(NetworkUtil.ping(testIntraHost,1)){
            //访问内网，不用netWork
            //判断版本
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //5.0以下  访问
                request(intraUrl);
            }else{
                //5.0以上  解绑应用network;防止与系统不一致，造成无法访问
                initAppNetwork();
            }
        }else{
            //判断当前网络状态
            if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
                //当前网络为WIFI
                UseWIFIAndIsWIFI();
            }else {
                //当前网络不为WIFI
                UseWIFIButNot();
            }
        }
    }

    //访问内网，ping不通，网络状态为WIFI
    public void  UseWIFIAndIsWIFI(){

        //判断是否连接指定WIFI
        if (NetworkUtil.isConnectedDesignatedWifi(context, WIFINAME)) {
            //判断版本
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //5.0以下，报错
                showToast("连接服务器失败。");
            } else {
                //初始化应用网络优先级（解绑应用network）
                initAppNetwork();
            }
        } else {
            if (wifiAdmin == null) {
                wifiAdmin = new WifiAdmin(context);
            }
            //连接到指定WIFI
            readyConnectedDesignatedWifi(wifiAdmin);
        }
    }

    //访问内网，ping不通，网络状态不为WIFI
    public void UseWIFIButNot(){
        //判断WIFI是否开启
        if (wifiAdmin == null) {
            wifiAdmin = new WifiAdmin(context);
        }
        if(wifiAdmin.isWifiEnabled()){
            WIFITurnedOn(wifiAdmin);
        }else{
            //没有打开WIFI，就先打开WIFI
            readyTurnOnWIFIData(wifiAdmin);
        }
    }




    //访问内网，ping不通，网络状态不为WIFI,WIFI已打开状态
    public void WIFITurnedOn(WifiAdmin wifiAdmin){
        //判断版本
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            //5.0以下，提升WIFI为网络优先
            setPreferredNetworkToWIFI();

            netWorkStatus = NetworkUtil.getNetWorkStatus(context);
            //当前网络状态是否为WIFI
            if (netWorkStatus == NetworkConstants.NETWORK_WIFI){
                //判断是否连接指定WIFI
                if(NetworkUtil.isConnectedDesignatedWifi(context,WIFINAME)){
                    //5.0以前的手机反应较慢，再睡2秒，消除延迟
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    request(intraUrl);
                }else {
                    //连接指定WIFI
                    connectedDesignatedWifi( wifiAdmin);

                    //是否连接指定WIFI
                    if(NetworkUtil.isConnectedDesignatedWifi(context,WIFINAME)){
                        request(intraUrl);
                    }else {
                        showToast("连接指定WIFI发生异常。");
                    }
                }
            }else{
                showToast("提升WIFI优先级失败。");
            }
        }else {
            //5.0以上，判断是否连接指定wifi
            //先将network解绑，不然会导致设置WIFI延迟
            //为network绑定WIFI网络
            NetworkUtil.removeAppNetworkSetting(context);
            NetworkUtil.setAppNetwork(context,NetworkCapabilities.TRANSPORT_WIFI);

            //判断是否连接指定WIFI
            if(NetworkUtil.isConnectedDesignatedWifi(context,WIFINAME)){
                //请求目标网络
                request(intraUrl);
            }else{
                //连接指定WIFI
                connectedDesignatedWifi( wifiAdmin);

                //是否连接指定WIFI
                if(NetworkUtil.isConnectedDesignatedWifi(context,WIFINAME)){
                    request(intraUrl);
                }else {
                    showToast("连接指定WIFI发生异常。");
                }
            }
        }
    }

    //WIFI未打开,准备打开WIFI的操作
    public void readyTurnOnWIFIData(WifiAdmin wifiAdmin){
        //打开WIFI
        turnOnWIFI(wifiAdmin);

        //判断WIFI 是否打开
        if(wifiAdmin.isWifiEnabled()){
            //重新走一遍上一层WIFI已打开的方法
            WIFITurnedOn(wifiAdmin);
        }else {
            showToast("打开WIFI失败。");
        }
    }

    //没有连接指定WIFI后的操作
    public void readyConnectedDesignatedWifi(WifiAdmin wifiAdmin){
        //连接到指定WIFI
        connectedDesignatedWifi(wifiAdmin);

        //是否连接指定WIFI
        if(NetworkUtil.isConnectedDesignatedWifi(context,WIFINAME)){
            //waitWIFIConnected();
            //是否连接目标服务器（内网）
            if(NetworkUtil.ping(testIntraHost,1)){
                //访问内网，不用netWork
                //请求目标网
                request(intraUrl);
            }else{
                //判断版本
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    showToast("连接连接服务器失败。");
                }else{
                    //初始化应用网络优先级（解绑应用network）
                    NetworkUtil.removeAppNetworkSetting(context);
                    //请求目标网络
                    if(NetworkUtil.isWiFiActive(context)){
                        //请求目标网
                        request(intraUrl);
                    }else{
                        showToast("连接指定WIFI发生异常。");
                    }
                }
            }
        }else {
            showToast("连接指定WIFI发生异常。");
        }
    }

    //初始化app network及后续操作
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void initAppNetwork(){
        //初始化应用网络优先级（解绑应用的network）
        NetworkUtil.removeAppNetworkSetting(context);
        //请求目标网络
        request(intraUrl);
    }


    //耗时操作========================================================================================

    //打开移动网络 5.0以下使用的方法
    public void turnOnMobileData(){
        NetworkUtil.setMobileData(context, true);

        //打开移动网络后线程等待
        synchronized (lock) {
            try {
                NEED = "turnOnMobileData";
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //使用网络监听 给handler发送消息来notify()
        }
        //打开WIFI后有延迟，线程睡2秒
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //打开WIFI
    public void turnOnWIFI(WifiAdmin wifiAdmin) {
        wifiAdmin.openWifi();

        //打开WIFI后线程等待
        synchronized (lock) {
            try {
                NEED = "turnOnWIFI";
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //使用网络监听 给handler发送消息来notify()
        }
        //打开WIFI后有延迟，线程睡2秒
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
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

        synchronized (lock)
        {
            try {
                NEED = "connectedDesignatedWifi";
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //使用网络监听 给handler发送消息来notify()
        }

        //连接到指定WIFI，线程睡1秒
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //设置系统网络优先级为Mobile
    public void setPreferredNetworkToMobile(){
        NetworkUtil.setPreferredNetwork(context, ConnectivityManager.TYPE_MOBILE);
        //循环等待，
        // 不用监听的原因是，监听发送的消息无法控制顺序，因为此方法过后，还需其他监听消息作为判断依据
        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
            netWorkStatus = NetworkUtil.getNetWorkStatus(context);
        }while(netWorkStatus != NetworkConstants.NETWORK_CLASS_2_G
                || netWorkStatus != NetworkConstants.NETWORK_CLASS_3_G
                || netWorkStatus != NetworkConstants.NETWORK_CLASS_4_G);

    }

    //设置系统网络优先级为WIFI
    public void setPreferredNetworkToWIFI() {
        NetworkUtil.setPreferredNetwork(context, ConnectivityManager.TYPE_WIFI);
        //循环等待
        // 不用监听的原因是，监听发送的消息无法控制顺序，因为此方法过后，还需其他监听消息作为判断依据
        startTime = System.currentTimeMillis();
        do {
            currentTime = System.currentTimeMillis();
            if (currentTime - startTime > mobileWaitingTime) {
                break;
            }
            netWorkStatus = NetworkUtil.getNetWorkStatus(context);
        } while (netWorkStatus != NetworkConstants.NETWORK_WIFI);
    }


    //共通========================================================================================

    //请求目标网络
    public void request( String Url){
        //访问目标网络
        //不用netWork
        FLAG = TRUE;
        NetworkUtil.requestWithoutNetwork( Url, this);
    }

    //显示Toast
    public void showToast(final String msg){
        new Thread(){
            public void run(){
                Looper.prepare();//给当前线程初始化Looper
                Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();;//Toast初始化的时候会new Handler();无参构造默认获取当前线程的Looper，如果没有prepare过，则抛出题主描述的异常。上一句代码初始化过了，就不会出错。
                Looper.loop();//这句执行，Toast排队show所依赖的Handler发出的消息就有人处理了，Toast就可以吐出来了。但是，这个Thread也阻塞这里了，因为loop()是个for (;;) ...
            }
        }.start();
    }

    //HTTP请求回调处理
    @Override
    public void onSuccess(String respose) {
        showToast(respose);
    }
    //HTTP请求回调处理
    @Override
    public void onError(Exception e) {
        showToast(e.toString());
    }

}
