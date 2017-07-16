package com.test.network;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity  implements View.OnClickListener {
    Button button_use_4G, button_use_WIFI;

    Uri uri;
    String outerUrl = "http://www.baidu.com";
    String IntraUrl = "http://10.30.30.88:8888/";
    long startTime;
    long currentTime;
    long mobileWaitingTime = 29999;
    long wifiWaitingTime = 25000;

    //wifi管理对象
    WifiAdmin wifiAdmin;
    //网络监听实例
    NetworkConnectChangedReceiver networkConnectChangedReceiver;

    //wifiNAME
    public static final String WIFINAME = "1204";
    public static final String WIFIPASSWORD = "852011101";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_use_4G = (Button) findViewById(R.id.button_use_4G);
        button_use_WIFI = (Button) findViewById(R.id.button_use_WIFI);

        button_use_4G.setOnClickListener(this);
        button_use_WIFI.setOnClickListener(this);

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
            default:
                break;
        }
    }

    /**
     * 选择使用4G的方法
     * @param v
     */
    public void choose4G(View v){
        int netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        //如果当前优先级为WIFI
        switch (netWorkStatus){
            //如果网络连接状态为WIFI
            case NetworkConstants.NETWORK_WIFI:
                mobileMethod(v);
                break;
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
                mobileMethod(v);
                break;
        }
    }

    public void mobileMethod(View v){
        showToast("网络切换中...");
        //判断移动网络是否打开
        boolean isMobileEnabled = NetworkUtil.isMobileEnabled(this);
        //判断移动网络是否已被打开,已打开就提升优先级，没有就打开移动网络
        if(isMobileEnabled){
            useMobile(v);
        }else{
            //打开Mobile
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //andriod 5.0 （API 21）以下  可以通过代码打开移动网络
                showToast("正在打开移动网络");
                NetworkUtil.setMobileData(v.getContext(), true);
                //打开移动网络后的操作
                useMobile(v);
            }else{
                //andriod 5.0 （API 21）以上  通过代码打开移动网络需要系统权限，所以只能跳转到网络设置页面，手动打开
                moveToNetworkSettings(v);
            }
        }
    }

    public void useMobile(View v){
        int netWorkStatus = NetworkUtil.getNetWorkStatus(this);
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

        if(!isMobileEnabled){
            showToast("打开移动网络超时...");
            return;
        }

        //如果打开移动网络以后，wifi依旧是优先，就提升移动网络优先级
        if(netWorkStatus == NetworkConstants.NETWORK_WIFI){

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //andriod 5.0 （API 21）以下  可以通过代码移动网络优先级
                NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_MOBILE);
            }else{
                //andriod 5.0 （API 21）以上  关闭WIFI，使用4G
                //wifiAdmin.closeWifi(); 会崩溃
                showToast("当前为WIFI网络，请先关闭WIFI");
                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                return;
            }

        }

        //消除因提升优先级造成的延迟
        startTime = System.currentTimeMillis();
        do{
            currentTime = System.currentTimeMillis();
            if(currentTime - startTime > mobileWaitingTime) {
                break;
            }
            netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        }while(netWorkStatus == NetworkConstants.NETWORK_WIFI
                || netWorkStatus == NetworkConstants.NETWORK_CLASS_UNKNOWN);



        //如果当目前网络状态不是 WIFI网络
        if(netWorkStatus!= NetworkConstants.NETWORK_WIFI) {

            switch (netWorkStatus) {
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

                    //执行完毕，将wifi级别升回来  （此方法要根据WIFI是否开启调用，或在后续的回调函数中调用）
                    //警告：因为访问网址是用时间上的延迟，所以在WIFI和4G同时开启，4G优先级被提升，访问网址后立即使用此方法，
                    //      会在访问延迟的时间段内切换回WIFI，有可能会导致访问无法完成（如WIFI未连接外网等）。
                    //NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_WIFI);
                    break;
                default:
                    showToast("网络错误，请检查网络");
                    break;
            }
        }else{
            showToast("网络切换超时超时，请检查网络");
        }
    }

    /**
     * 选择使用wifi的方法
     * @param v
     */
    public void chooseWifi(View v){
        int netWorkStatus = NetworkUtil.getNetWorkStatus(this);
        //如果当前优先级为WIFI
        if(netWorkStatus == NetworkConstants.NETWORK_WIFI){
            //直接跳转wifi目标网络
            uri = Uri.parse(IntraUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }else{
            showToast("网络切换中...");
            wifiMethod(v);
        }

    }

    public void wifiMethod(View v){
        //判断WIFI是否打开
        if (wifiAdmin == null) {
            wifiAdmin = new WifiAdmin(this);
        }
        boolean isWifiEnabled =  wifiAdmin.isWifiEnabled();
        //如果WIFI打开，
        if(isWifiEnabled){
            int netWorkStatus = NetworkUtil.getNetWorkStatus(this);
            //当前网络状态不是wifi
            if(netWorkStatus != NetworkConstants.NETWORK_WIFI){
                //如果WIFI连上了有效路由
                //if(NetworkConnectChangedReceiver.WIFICONNECTED == NetworkConnectChangedReceiver.TRUE) {
                    //提升wifi优先级
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        //andriod 5.0 （API 21）以下  可以通过代码wifi优先级
                        NetworkUtil.setPreferredNetwork(v.getContext(), ConnectivityManager.TYPE_WIFI);
                        //消除提升wifi优先级的延迟
                        startTime = System.currentTimeMillis();
                        while(netWorkStatus != NetworkConstants.NETWORK_WIFI){
                            //等待
                            currentTime = System.currentTimeMillis();
                            if(currentTime - startTime > wifiWaitingTime) {
                                break;
                            }
                            netWorkStatus = NetworkUtil.getNetWorkStatus(this);
                        }
                    }else{
                        //andriod 5.0 （API 21）以上  自动判断是WIFI否可用
//                        showToast("WIFI无连接，请先设置WIFI");
//                        return;
                    }
                //}else{
                //  showToast("WIFI未能正常连接，请检查网络...");
                //}
            }

            //判断是否联入指定WIFI
//            if(wifiAdmin.getBSSID() != WIFINAME) {
//                wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(WIFINAME, WIFIPASSWORD, 3));
//            }
//            //消除联入指定WIFI产生的延迟
//            startTime = System.currentTimeMillis();
//            while(NetworkConnectChangedReceiver.WIFISTATE != NetworkConstants.NETWORK_WIFI_STATE_ENABLED){
//                //等待
//                currentTime = System.currentTimeMillis();
//                if(currentTime - startTime > wifiWaitingTime) {
//                    break;
//                }
//            }
//            //连接未完成
//            if(NetworkConnectChangedReceiver.WIFISTATE != NetworkConstants.NETWORK_WIFI_STATE_ENABLED
//                    && wifiAdmin.getBSSID() != WIFINAME){
//                showToast("WIFI连接超时，请设置WIFI...");
//                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
//                return;
//            }

            //判断是否连通
            startTime = System.currentTimeMillis();
            while(!NetworkUtil.isWiFiActive(this)){
                //等待
                currentTime = System.currentTimeMillis();
                if(currentTime - startTime > wifiWaitingTime) {
                    break;
                }
            }

            if(!NetworkUtil.isWiFiActive(this)){
                showToast("WIFI连接超时，请设置WIFI...");
                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            }else{
                uri = Uri.parse(IntraUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        }else{
            //打开WIFI
            if(NetworkConnectChangedReceiver.WIFISTATE != NetworkConstants.NETWORK_WIFI_STATE_ENABLED
                    || NetworkConnectChangedReceiver.WIFISTATE != NetworkConstants.NETWORK_WIFI_STATE_ENABLING) {

                //WIFI打开方法
                wifiAdmin.openWifi();

                //消除开启wifi延迟
                startTime = System.currentTimeMillis();
                while(!wifiAdmin.isWifiEnabled()){
                    //等待
                    currentTime = System.currentTimeMillis();
                    if(currentTime - startTime > wifiWaitingTime) {
                        break;
                    }
                }
                //打开wifi失败
                if(!wifiAdmin.isWifiEnabled()){
                    showToast("WIFI打开失败，请设置WIFI...");
                    startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
                }else{
                    wifiMethod(v);
                }
            }
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
                            useMobile(v);
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



}