package com.test.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Parcelable;

import java.lang.reflect.Method;


/**
 * 网络改变监控广播
 * 监听网络的改变状态,只有在用户操作网络连接开关(wifi,mobile)的时候接受广播,
 * Created by ChenFei on 2017/7/16.
 */

public class NetworkConnectChangedReceiver extends BroadcastReceiver {

    //网络连接状态 全局变量
    public static int NET_STATE;
    //WIFI状态 全局变量
    public static int WIFI_STATE;
    //WIFI是否连接有效路由
    public static int WIFI_CONNECTED;
    //WIFI连接有效路由的名字
    public static int WIFI_NAME;

    public static final int TRUE = 1;
    public static final int FALSE = 0;

    public static NetworkInfo activeNetwork;

    private Handler mHandler;

    public NetworkConnectChangedReceiver(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        isMobileEnabled(intent, mHandler, context);
        //wifi开关监听
        isOpenWifi(intent, mHandler);
        //是否连接wifi
        isConnectionWifi(intent, mHandler);
        //监听网络连接设置
        isConnection(intent, mHandler, context);


    }

    /**
     * 监听wifi打开与关闭
     * （与连接与否无关）
     * @author hjy
     * created at 2016/12/12 17:33
     */
    public void isOpenWifi(Intent intent, Handler mHandler){
        // 这个监听wifi的打开与关闭，与wifi的连接无关
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            switch (wifiState) {
                //WIFI已禁用
                case WifiManager.WIFI_STATE_DISABLED:
                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_DISABLED;
                    break;
                //WIFI正在禁用
                case WifiManager.WIFI_STATE_DISABLING:
                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_DISABLING;
                    break;
                //WIFI正在启用
                case WifiManager.WIFI_STATE_ENABLING:
                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_ENABLING;
                    break;
                //WIFI已启用
                case WifiManager.WIFI_STATE_ENABLED:
                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_ENABLED;
                    mHandler.sendEmptyMessage(NetworkConstants.NETWORK_WIFI_STATE_ENABLED);
                    break;
                //WIFI状态未知
                case WifiManager.WIFI_STATE_UNKNOWN:
                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_UNKNOWN;
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 连接有用的wifi（有效无线路由）
     * WifiManager.WIFI_STATE_DISABLING与WIFI_STATE_DISABLED的时候，根本不会接到这个广播
     * @author hjy
     * created at 2016/12/13 9:47
     */
    public void isConnectionWifi(Intent intent, Handler mHandler){
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            Parcelable parcelableExtra = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                NetworkInfo.State state = networkInfo.getState();
                boolean isConnected = state == NetworkInfo.State.CONNECTED;
                //wifi连接
                if (isConnected) {
                    mHandler.sendEmptyMessage(NetworkConstants.NETWORK_WIFI_STATE_ISCONNECTED);
                }else{
                    mHandler.sendEmptyMessage(NetworkConstants.NETWORK_WIFI_STATE_UNCONNECTED);
                }
            }
        }
    }

    /**
     * 监听网络连接的设置，包括wifi和移动数据的打开和关闭。(推荐)
     * 这个广播的最大弊端是比上边两个广播的反应要慢，如果只是要监听wifi，我觉得还是用上边两个配合比较合适
     * @author hjy
     * created at 2016/12/13 9:47
     */
    public void isConnection(Intent intent, Handler mHandler, Context context){
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
            if (activeNetwork != null) {
                // connected to the internet
                if (activeNetwork.isConnected()) {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        // connected to wifi
                       mHandler.sendEmptyMessage(NetworkConstants.NETWORK_WIFI);
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        // connected to the mobile provider's data plan
                        //获取当前移动网络的具体分类
                        mHandler.sendEmptyMessage(NetworkUtil.getNetWorkClass(context));
                    }
                } else {
                    mHandler.sendEmptyMessage( NetworkConstants.NETWORK_CLASS_UNKNOWN);
                }
//                LogUtil.e(TAG, "TypeName：" + activeNetwork.getTypeName());
//                LogUtil.e(TAG, "SubtypeName：" + activeNetwork.getSubtypeName());
//                LogUtil.e(TAG, "State：" + activeNetwork.getState());
//                LogUtil.e(TAG, "DetailedState："
//                        + activeNetwork.getDetailedState().name());
//                LogUtil.e(TAG, "ExtraInfo：" + activeNetwork.getExtraInfo());
//                LogUtil.e(TAG, "Type：" + activeNetwork.getType());

            } else {   // not connected to the internet
                mHandler.sendEmptyMessage( NetworkConstants.NETWORK_CLASS_UNKNOWN);
            }
        }
    }

    public static void isMobileEnabled(Intent intent, Handler mHandler, Context context) {
        try {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method getMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabledMethod.setAccessible(true);
           if( (Boolean) getMobileDataEnabledMethod.invoke(mConnectivityManager)){
               mHandler.sendEmptyMessage( NetworkConstants.NETWORK_MOBILE_STATE_ENABLED);
           }else{
               mHandler.sendEmptyMessage( NetworkConstants.NETWORK_MOBILE_STATE_UNABLED);
           }
        } catch (Exception e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage( NetworkConstants.NETWORK_MOBILE_STATE_UNABLED);
        }
        // 反射失败，默认开启

    }


//    public void onReceive(Context context, Intent intent) {
//        // 这个监听wifi的打开与关闭，与wifi的连接无关
//        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
//            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
//            switch (wifiState) {
//                //WIFI已禁用
//                case WifiManager.WIFI_STATE_DISABLED:
//                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_DISABLED;
//                    break;
//                //WIFI正在禁用
//                case WifiManager.WIFI_STATE_DISABLING:
//                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_DISABLING;
//                    break;
//                //WIFI正在启用
//                case WifiManager.WIFI_STATE_ENABLING:
//                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_ENABLING;
//                    break;
//                //WIFI已启用
//                case WifiManager.WIFI_STATE_ENABLED:
//                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_ENABLED;
//                    break;
//                //WIFI状态未知
//                case WifiManager.WIFI_STATE_UNKNOWN:
//                    WIFI_STATE = NetworkConstants.NETWORK_WIFI_STATE_UNKNOWN;
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        // 这个监听wifi的连接状态即是否连上了一个有效无线路由，当上边广播的状态是WifiManager
//        // .WIFI_STATE_DISABLING，和WIFI_STATE_DISABLED的时候，根本不会接到这个广播。
//        // 在上边广播接到广播是WifiManager.WIFI_STATE_ENABLED状态的同时会接到这个广播，
//        // 当然刚打开wifi肯定还没有连接到有效的无线
//        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
//
//            Toast.makeText(context,"WIFI已连接", Toast.LENGTH_SHORT).show();
//
//            //获得网络状态数据
//            Parcelable parcelableExtra = intent
//                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//            if (null != parcelableExtra) {
//                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
//                //获取网络粗颗粒状态
//                NetworkInfo.State state = networkInfo.getState();
//                boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
//
//                if (isConnected) {
//                    //WIFI已连上有效的无线路由
//                    WIFI_CONNECTED = TRUE;
//                    Toast.makeText(context,"WIFI已连接", Toast.LENGTH_SHORT).show();
//                } else {
//                    //WIFI未连上有效的无线路由
//                    WIFI_CONNECTED = FALSE;
//                }
//            }
//        }
//
//        // 这个监听网络连接的设置，包括wifi和移动数据的打开和关闭。.
//        // 最好用的还是这个监听。wifi如果打开，关闭，以及连接上可用的连接都会接到监听。见log
//        // 这个广播的最大弊端是比上边两个广播的反应要慢，如果只是要监听wifi，我觉得还是用上边两个配合比较合适
//        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
//            ConnectivityManager manager = (ConnectivityManager) context
//                    .getSystemService(Context.CONNECTIVITY_SERVICE);
//
//            activeNetwork = manager.getActiveNetworkInfo();
//            if (activeNetwork != null) { // connected to the internet
//                if (activeNetwork.isConnected()) {
//                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
//                        // connected to wifi // 当前WiFi连接可用
//                        NET_STATE = NetworkConstants.NETWORK_WIFI;
//                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
//                        // connected to the mobile provider's data plan // 当前移动网络连接可用
//                        //获取当前移动网络的具体分类
//                        NET_STATE = NetworkUtil.getNetWorkClass(context);
//                    }
//                } else {
//                    //"当前没有网络连接，请确保你已经打开网络 "
//                    NET_STATE = NetworkConstants.NETWORK_CLASS_UNKNOWN;
//                }
//                //当前网络已连接
//
////                Log.e(TAG1, "info.getTypeName()" + activeNetwork.getTypeName());
////                Log.e(TAG1, "getSubtypeName()" + activeNetwork.getSubtypeName());
////                Log.e(TAG1, "getState()" + activeNetwork.getState());
////                Log.e(TAG1, "getDetailedState()"
////                        + activeNetwork.getDetailedState().name());
////                Log.e(TAG1, "getDetailedState()" + activeNetwork.getExtraInfo());
////                Log.e(TAG1, "getType()" + activeNetwork.getType());
//            } else {   // not connected to the internet
//               //"当前没有网络连接，请确保你已经打开网络 "
//                NET_STATE = NetworkConstants.NETWORK_CLASS_UNKNOWN;
//            }
//        }
//    }
//
//    // 自定义接口
//    public interface NetEvevt {
//        public void onNetChange(int netMobile);
//    }
}
