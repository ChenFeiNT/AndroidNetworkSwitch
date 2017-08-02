package com.test.network;

import android.Manifest;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity  implements View.OnClickListener  {

    Button button_use_4G, button_use_WIFI, button_request_outerUrl, button_request_IntraUrl,button_app_4G,button_app_WIFI;


    // 权限检测器
    private PermissionsChecker mPermissionsChecker;
    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET
    };

    //网络监听Handler
    private Handler netWorkHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            showToast(msg.what+"");
        }
    };

    //实例化NetworkSwitchUtil
    NetworkSwitchUtil networkSwitchUtil = new NetworkSwitchUtil(this, netWorkHandler);

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



        // 5.0以下不可验证
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            button_app_4G.setVisibility(View.GONE);
            button_app_WIFI.setVisibility(View.GONE);
            //button_request_outerUrl.setVisibility(View.GONE);
            //button_request_IntraUrl.setVisibility(View.GONE);
        }

        //权限检测器
        mPermissionsChecker = new PermissionsChecker(this);
        mPermissionsChecker.activity = this;
        mPermissionsChecker.permissions = PERMISSIONS;
        mPermissionsChecker.judgePermission();


//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE)
//                != PackageManager.PERMISSION_GRANTED) {
//            // 第一次请求权限时，用户如果拒绝，下一次请求shouldShowRequestPermissionRationale()返回true
//            // 向用户解释为什么需要这个权限
//            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CHANGE_NETWORK_STATE)) {
//                new android.support.v7.app.AlertDialog.Builder(this)
//                        .setMessage("申请 CHANGE_NETWORK_STATE 权限")
//                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                //申请CHANGE_NETWORK_STATE权限
//                                ActivityCompat.requestPermissions(MainActivity.this,
//                                        new String[]{Manifest.permission.CHANGE_NETWORK_STATE}, 1);
//                            }
//                        })
//                        .show();
//            } else {
//                //申请读取network_change权限
//                ActivityCompat.requestPermissions(MainActivity.this,
//                        new String[]{Manifest.permission.CHANGE_NETWORK_STATE},1);
//            }
//        } else {
//
//        }

    }



    @Override
    protected void onResume() {
        //在onResume()方法注册网络监听
        networkSwitchUtil.onResume();
        super.onResume();
    }



    @Override
    protected void onPause() {
        //onPause()方法注销网络监听
        networkSwitchUtil.onPause();
        super.onPause();
    }

    //权限请求的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionsChecker.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       // super.onActivityResult(requestCode, resultCode, data);
        networkSwitchUtil.onActivityResult(requestCode,resultCode,data);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //点击  使用4G 按钮
            case R.id.button_use_4G:
               // netIsConneted(0);
                networkSwitchUtil.netIsConneted(0);
                break;
            //点击  使用WIFI  按钮
            case R.id.button_use_WIFI:
                //netIsConneted(1);
                networkSwitchUtil.netIsConneted(1);
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
//                //不用netWork，访问外网
                // request(outerUrl);
                break;
            case R.id.button_request_IntraUrl:
               // request(intraUrl);
                break;
            default:
                break;
        }
    }


    //显示Toast
    public void showToast(final String msg){
        new Thread(){
            public void run(){
                Looper.prepare();//给当前线程初始化Looper
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();;//Toast初始化的时候会new Handler();无参构造默认获取当前线程的Looper，如果没有prepare过，则抛出题主描述的异常。上一句代码初始化过了，就不会出错。
                Looper.loop();//这句执行，Toast排队show所依赖的Handler发出的消息就有人处理了，Toast就可以吐出来了。但是，这个Thread也阻塞这里了，因为loop()是个for (;;) ...
            }
        }.start();
    }

}