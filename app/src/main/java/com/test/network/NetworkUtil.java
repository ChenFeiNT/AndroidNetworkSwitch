package com.test.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by softwise on 2017/7/11.
 */

public class NetworkUtil{

    /*
     * 获取手机网络类型（2G/3G/4G）：
     * 4G为LTE，
     * 联通的3G为UMTS或HSDPA，
     * 电信的3G为EVDO，
     * 移动和联通的2G为GPRS或EGDE，电信的2G为CDMA。
     */
    public static int getNetWorkClass(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        switch (telephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NetworkConstants.NETWORK_CLASS_2_G;

            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NetworkConstants.NETWORK_CLASS_3_G;

            case TelephonyManager.NETWORK_TYPE_LTE:
                return NetworkConstants.NETWORK_CLASS_4_G;

            default:
                return NetworkConstants.NETWORK_CLASS_UNKNOWN;
        }
    }

    /*
     * 获取手机连接的网络类型（是WIFI还是手机网络[2G/3G/4G]）
     */
    public static int getNetWorkStatus(Context context) {
        int netWorkType = NetworkConstants.NETWORK_CLASS_UNKNOWN;

        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            int type = networkInfo.getType();

            if (type == ConnectivityManager.TYPE_WIFI) {
                netWorkType = NetworkConstants.NETWORK_WIFI;
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                netWorkType = getNetWorkClass(context);
            }
        }
        return netWorkType;
    }

    /**
     * 设置网络优先级，api 21 之前有效
     * @param context
     * @param networkType  ConnectivityManager.TYPE_MOBILE (移动网络)，ConnectivityManager.TYPE_MOBILE
     */
    public static void setPreferredNetwork(Context context, int networkType) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.setNetworkPreference(networkType);
    }

    /**
     * 返回手机移动数据的状态
     * @param pContext
     * @param arg
     *            默认填null
     * @return true 连接 false 未连接
     */
    public static boolean getMobileDataState(Context pContext, Object[] arg) {
        Boolean isOpen = false;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //andriod 5.0 （API 21）以下  通过此反射方法获取移动网络状态
                ConnectivityManager mConnectivityManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                Class ownerClass = mConnectivityManager.getClass();
                Class[] argsClass = null;
                if (arg != null) {
                    argsClass = new Class[1];
                    argsClass[0] = arg.getClass();
                }
                Method method = ownerClass.getMethod("getMobileDataEnabled", argsClass);
                isOpen = (Boolean) method.invoke(mConnectivityManager, arg);
                return isOpen;
            } else {
                //andriod 5.0 （API 21）以上  通过此反射方法获取移动网络状态
                TelephonyManager telephonyService = (TelephonyManager) pContext.getSystemService(Context.TELEPHONY_SERVICE);
                Method getMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
                if (null != getMobileDataEnabledMethod){
                    isOpen = (Boolean) getMobileDataEnabledMethod.invoke(telephonyService);
                }
                return isOpen;
            }
        } catch (Exception e) {
            System.out.println("得到移动数据状态出错");
            return false;
        }
    }

    /**
     * 打开或关闭手机的移动数据
     * 此方法只使用于 andriod 5.0（API 21）以下
     * andriod 5.0（API 21）以上需要系统权限才可打开移动网络
     */
    public static void setMobileData(Context pContext, boolean pBoolean) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //andriod 5.0 （API 21）以下  通过此方法打开或关闭移动网络
                ConnectivityManager mConnectivityManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                Class ownerClass = mConnectivityManager.getClass();
                Class[] argsClass = new Class[1];
                argsClass[0] = boolean.class;
                Method method = ownerClass.getMethod("setMobileDataEnabled", argsClass);
                method.invoke(mConnectivityManager, pBoolean);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("移动数据设置错误: " + e.toString());
        }
    }

    //判断wifi是否连通（不是是否开启）
    public static boolean isWiFiActive(Context inContext) {
        Context context = inContext.getApplicationContext();
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        //getAllNetworkInfo 在23被舍弃，getNetworkInfo 在21被加入
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = connectivity.getAllNetworks();
            NetworkInfo networkInfo;
            for (Network mNetwork : networks) {
                networkInfo = connectivity.getNetworkInfo(mNetwork);
                if (networkInfo.getTypeName().equals("WIFI") && networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                    return true;
                }
            }
        }else {
            if (connectivity != null) {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null) {
                    for (int i = 0; i < info.length; i++) {
                        if (info[i].getTypeName().equals("WIFI") && info[i].isConnected()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }



    //判断当前网络是否连接
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    /**
     * 判断能够连到目标主机
     * @param host          主机
     * @param pingCount     ping的次数
     * @return
     */
    public static boolean ping(String host, int pingCount) {
        Process process = null;
        String command = "ping -c " + pingCount + " -w 10 " + host;
        boolean isSuccess = false;
        try {
            process = Runtime.getRuntime().exec(command);
            if (process == null) {
                return false;
            }
            int status = process.waitFor();
            if (status == 0) {
                isSuccess = true;
            } else {
                isSuccess = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return isSuccess;
    }




    /**
     * 判断 移动网络 是否被打开
     * @param context
     * @return false 未打开 true 打开
     */
    public static boolean isMobileEnabled(Context context) {
        try {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method getMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabledMethod.setAccessible(true);
            return (Boolean) getMobileDataEnabledMethod.invoke(mConnectivityManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 反射失败，默认开启
        return true;
    }

    //      判断是否连接指定WIFI
    public static boolean isConnectedDesignatedWifi(Context inContext,String designatedWifi){
        WifiManager mWifiManager = (WifiManager) inContext.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        if (wifiInfo.getSSID().toString().equals("\""+designatedWifi+"\"")){
            return true;
        }else{
            return false;
        }
    }



    /**
     * 5.0以上WIFI和4G同开，设置network并访问（请求级别）
     * @param context
     * @param strURL   请求的url
     * @param transport_type  设置network的连接种类（NetworkCapabilities.TRANSPORT_CELLULAR：移动网络，NetworkCapabilities.TRANSPORT_WIFI：WIFI）
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void   useMobileNetwork(final Context context, final String strURL,final int transport_type){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(transport_type)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback(){
            /**
             * Called when the framework connects and has declared a new network ready for use.
             * This callback may be called more than once if the {@link Network} that is
             * satisfying the request changes.
             *
             * This method will be called on non-UI thread, so beware not to use any UI updates directly.
             *
             * @param network The {@link Network} of the satisfying network.
             */
            @Override
            public void onAvailable(final Network network){
                try {
                    URL url = new URL(strURL);
                    URLConnection uRLConnection = network.openConnection(url);          //openURLConnection();
                    InputStream in = uRLConnection.getInputStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    String a = new String(out.toByteArray());
                    out.close();
                    in.close();
                    showToast(a,context);
                }catch (Exception e){
                    showToast(e.toString(),context);
                    e.toString();
                }
            }
        });
    }

    /**
     * 5.0以上WIFI和4G同开，设置network（应用级别）
     * @param context
     * @param transport_type  设置network的连接种类（NetworkCapabilities.TRANSPORT_CELLULAR：移动网络，NetworkCapabilities.TRANSPORT_WIFI：WIFI）
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void   setAppNetwork(final Context context, final int transport_type){

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(transport_type)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback(){

            @Override
            public void onAvailable(final Network network) {
                // 可以通过下面代码将app接下来的请求都绑定到这个网络下请求
                if (Build.VERSION.SDK_INT >= 23) {
                    connectivityManager.bindProcessToNetwork(network);
                } else {
                    // 23后这个方法舍弃了
                    ConnectivityManager.setProcessDefaultNetwork(network);
                }
                // 也可以在将来某个时间取消这个绑定网络的设置
                // if (Build.VERSION.SDK_INT >= 23) {
                //      onnectivityManager.bindProcessToNetwork(null);
                //} else {
                //     ConnectivityManager.setProcessDefaultNetwork(null);
                //}
                // 只要一找到符合条件的网络就注销本callback
                // 你也可以自己进行定义注销的条件
                //connectivityManager.unregisterNetworkCallback(this);
            }
        });
        showToast("未知错误",context);
    }

    //5.0以上取消app级network绑定网络设置
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void    removeAppNetworkSetting(final Context context){
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        // 可以通过下面代码取消app绑定网络的设置
         if (Build.VERSION.SDK_INT >= 23) {
             connectivityManager.bindProcessToNetwork(null);
        } else {
             ConnectivityManager.setProcessDefaultNetwork(null);
        }
        // 只要一找到符合条件的网络就注销本callback
        // 你也可以自己进行定义注销的条件
        //connectivityManager.unregisterNetworkCallback(this);
    }

    //显示Toast
    public static void showToast(final String msg, final Context context){
        new Thread(){
            public void run(){
                Looper.prepare();//给当前线程初始化Looper
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();;//Toast初始化的时候会new Handler();无参构造默认获取当前线程的Looper，如果没有prepare过，则抛出题主描述的异常。上一句代码初始化过了，就不会出错。
                Looper.loop();//这句执行，Toast排队show所依赖的Handler发出的消息就有人处理了，Toast就可以吐出来了。但是，这个Thread也阻塞这里了，因为loop()是个for (;;) ...
            }
        }.start();
    }

    //不使用network访问网络
    public static void requestWithoutNetwork(Context context,String strUrl) {
        String result = null;
        URL url = null;
        HttpURLConnection connection = null;
        InputStreamReader in = null;
        try {
            url = new URL(strUrl);
            connection = (HttpURLConnection) url.openConnection();
            in = new InputStreamReader(connection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(in);
            StringBuffer strBuffer = new StringBuffer();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                strBuffer.append(line);
            }
            result = strBuffer.toString();
            showToast(result,context);

        } catch (Exception e) {
            showToast(e.toString(),context);
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast(e.toString(),context);
                }
            }
        }
    }

    //使用network访问网络
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestWithNetwork(Context context,String strUrl){
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(MainActivity.CONNECTIVITY_SERVICE);
        Network network = (Network)connectivityManager.getBoundNetworkForProcess();
        try {
            URL url = new URL(strUrl);
            URLConnection uRLConnection = network.openConnection(url);          //openURLConnection();
            InputStream in = uRLConnection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            String a = new String(out.toByteArray());
            out.close();
            in.close();
            showToast(a,context);
        }catch (Exception e){
            showToast(e.toString(),context);
            e.toString();
        }
    }
}
