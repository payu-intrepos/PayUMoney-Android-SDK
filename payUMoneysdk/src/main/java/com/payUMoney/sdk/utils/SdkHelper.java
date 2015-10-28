package com.payUMoney.sdk.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;


public class SdkHelper {

    public static boolean checkNetwork(Context c)
    {
        ConnectivityManager conMgr = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        if ( conMgr.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
                || conMgr.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTING
                || conMgr.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED
                || conMgr.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING ) {

            return true;
            // notify user you are online

        }
        else if ( conMgr.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED
                  && conMgr.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED) {


            return false;
            // notify user you are not online
        }
        else
        {
            return false;
        }
    }
}
