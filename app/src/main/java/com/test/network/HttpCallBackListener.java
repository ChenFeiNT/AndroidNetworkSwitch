package com.test.network;

/**
 * Created by pengkv on 17/07/26.
 * 网络请求回调接口
 */

public interface  HttpCallBackListener {
    void onSuccess(String respose);

    void onError(Exception e);
}
