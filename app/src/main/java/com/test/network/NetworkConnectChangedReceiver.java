package com.test.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;


/**
 * 网络改变监控广播
 * 监听网络的改变状态,只有在用户操作网络连接开关(wifi,mobile)的时候接受广播,
 * Created by ChenFei on 2017/7/16.
 */

public class NetworkConnectChangedReceiver extends BroadcastReceiver {

    //网络连接状态 全局变量
    public static int NETSTATE;
    //WIFI状态 全局变量
    public static int WIFISTATE;
    //WIFI是否连接有效路由
    public static int WIFICONNECTED;
    public static final int TRUE = 1;
    public static final int FALSE = 0;

    public static NetworkInfo activeNetwork;

    @Override
    public void onReceive(Context context, Intent intent) {
        // 这个监听wifi的打开与关闭，与wifi的连接无关
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            switch (wifiState) {
                //WIFI已禁用
                case WifiManager.WIFI_STATE_DISABLED:
                    WIFISTATE = NetworkConstants.NETWORK_WIFI_STATE_DISABLED;
                    break;
                //WIFI正在禁用
                case WifiManager.WIFI_STATE_DISABLING:
                    WIFISTATE = NetworkConstants.NETWORK_WIFI_STATE_DISABLING;
                    break;
                //WIFI正在启用
                case WifiManager.WIFI_STATE_ENABLING:
                    WIFISTATE = NetworkConstants.NETWORK_WIFI_STATE_ENABLING;
                    break;
                //WIFI已启用
                case WifiManager.WIFI_STATE_ENABLED:
                    WIFISTATE = NetworkConstants.NETWORK_WIFI_STATE_ENABLED;
                    break;
                //WIFI状态未知
                case WifiManager.WIFI_STATE_UNKNOWN:
                    WIFISTATE = NetworkConstants.NETWORK_WIFI_STATE_UNKNOWN;
                    break;
                default:
                    break;
            }
        }

        // 这个监听wifi的连接状态即是否连上了一个有效无线路由，当上边广播的状态是WifiManager
        // .WIFI_STATE_DISABLING，和WIFI_STATE_DISABLED的时候，根本不会接到这个广播。
        // 在上边广播接到广播是WifiManager.WIFI_STATE_ENABLED状态的同时会接到这个广播，
        // 当然刚打开wifi肯定还没有连接到有效的无线
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            //获得网络状态数据
            Parcelable parcelableExtra = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                //获取网络粗颗粒状态
                NetworkInfo.State state = networkInfo.getState();
                boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态

                if (isConnected) {
                    //WIFI已连上有效的无线路由
                    WIFICONNECTED = TRUE;
                } else {
                    //WIFI未连上有效的无线路由
                    WIFICONNECTED = FALSE;
                }
            }
        }

        // 这个监听网络连接的设置，包括wifi和移动数据的打开和关闭。.
        // 最好用的还是这个监听。wifi如果打开，关闭，以及连接上可用的连接都会接到监听。见log
        // 这个广播的最大弊端是比上边两个广播的反应要慢，如果只是要监听wifi，我觉得还是用上边两个配合比较合适
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            activeNetwork = manager.getActiveNetworkInfo();
            if (activeNetwork != null) { // connected to the internet
                if (activeNetwork.isConnected()) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        // connected to wifi // 当前WiFi连接可用
                        NETSTATE = NetworkConstants.NETWORK_WIFI;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        // connected to the mobile provider's data plan // 当前移动网络连接可用
                        //获取当前移动网络的具体分类
                        NETSTATE = NetworkUtil.getNetWorkClass(context);
                    }
                } else {
                    //"当前没有网络连接，请确保你已经打开网络 "
                    NETSTATE = NetworkConstants.NETWORK_CLASS_UNKNOWN;
                }
                //当前网络已连接

//                Log.e(TAG1, "info.getTypeName()" + activeNetwork.getTypeName());
//                Log.e(TAG1, "getSubtypeName()" + activeNetwork.getSubtypeName());
//                Log.e(TAG1, "getState()" + activeNetwork.getState());
//                Log.e(TAG1, "getDetailedState()"
//                        + activeNetwork.getDetailedState().name());
//                Log.e(TAG1, "getDetailedState()" + activeNetwork.getExtraInfo());
//                Log.e(TAG1, "getType()" + activeNetwork.getType());
            } else {   // not connected to the internet
               //"当前没有网络连接，请确保你已经打开网络 "
                NETSTATE = NetworkConstants.NETWORK_CLASS_UNKNOWN;
            }
        }
    }

    // 自定义接口
    public interface NetEvevt {
        public void onNetChange(int netMobile);
    }
}
