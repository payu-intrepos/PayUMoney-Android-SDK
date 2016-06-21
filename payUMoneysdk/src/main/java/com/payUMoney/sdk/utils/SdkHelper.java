package com.payUMoney.sdk.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SdkHelper {

    private static long mLastClickTime = 0;
    private static ProgressDialog progressDialog;

    public static boolean checkNetwork(Context c) {
        ConnectivityManager cm = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(cm!=null){
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            return isConnected;
        }
        return false;
    }


    public static void dismissProgressDialog() {
        if (progressDialog != null) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
        }
        progressDialog = null;
    }

    public static void showProgressDialog(Context mActivity, String strMessage) {
        if (progressDialog != null) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
        } else {
            progressDialog = new ProgressDialog(mActivity);
        }
        progressDialog.setMessage((strMessage.equals(null) ? "Loading..." : strMessage));
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        try {
            progressDialog.show();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }


    public static boolean setStringPreference(Context context, String key, String value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null && !TextUtils.isEmpty(key)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, value);
            return editor.commit();
        }
        return false;
    }

    public static String getStringPreference(Context context, String key) {
        String value = null;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            value = preferences.getString(key, null);
        }
        return value;
    }

    public static void showProgressDialog(Context mActivity, String strMessage, boolean isCancellable) {
        if (progressDialog != null) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
        } else {
            progressDialog = new ProgressDialog(mActivity);
        }
        progressDialog = new ProgressDialog(mActivity);
        progressDialog.setMessage((strMessage.equals(null) ? "Loading..." : strMessage));
        progressDialog.setCancelable(isCancellable);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    public static boolean isValidClick(){

        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return false;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        return true;
    }

    public static void showToastMessage(Activity mActivity, String strMessage, boolean warningMessage) {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.sdk_toast_layout,
                (ViewGroup) mActivity.findViewById(R.id.toast_layout_root));

        ((TextView) layout.findViewById(R.id.toast_textView)).setText(strMessage);
        layout.findViewById(R.id.toast_layout_root).setBackgroundColor(mActivity.getApplicationContext().getResources().getColor(warningMessage ? (android.R.color.holo_red_light) : (R.color.primary_green)));
        Toast toast = new Toast(mActivity.getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, 30);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    public static String getAndroidID(Context context) {

        if (context == null)
            return "";
        String device_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return device_id;
    }

    public static String getAppVersion(Context mContext) {

        try {
            PackageInfo pInfo = mContext.getApplicationContext().getPackageManager().getPackageInfo(mContext.getApplicationContext().getPackageName(), 0);
            String currentVersion = pInfo.versionName;
            return currentVersion;
        } catch (Exception e) {
//Start the next activity
            return "";
        }

    }


    public static String getUserCookieSessionId(Context c){
        if(c == null){
            return "";
        }

        long lastUsedSessionTimeStamp = getLongPreference(c, "LAST_USED_SESSION_TIMESTAMP");
        long currentSessionTimeStamp ;
        if(lastUsedSessionTimeStamp + SdkConstants.DEFAULT_SESSION_UPDATE_TIME < System.currentTimeMillis())
        {
            currentSessionTimeStamp = System.currentTimeMillis();
            setLongPreference(c, "LAST_USED_SESSION_TIMESTAMP" , currentSessionTimeStamp);
        }else{
            currentSessionTimeStamp = lastUsedSessionTimeStamp;
        }
        return "UserSessionCookie = "+getAndroidID(c) + "_" +c.getPackageName()+"_"+ currentSessionTimeStamp;
    }


    public static long getLongPreference(Context context, String key) {
        long value = 0;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            value = preferences.getLong(key, 0);
        }
        return value;
    }

    public static boolean setLongPreference(Context context, String key, long value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null && !TextUtils.isEmpty(key)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(key, value);
            return editor.commit();
        }
        return false;
    }

    public static boolean checkForValidString(String inputString) {
        if(inputString != null && !inputString.isEmpty() && !SdkConstants.NULL_STRING.equals(inputString)){
            return true;
        }
        return false;
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        return metrics.widthPixels;
    }
    public static int getScreenHeight(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        return metrics.heightPixels;
    }

    public static boolean hasNFC(Context context) {
        if(context!=null)
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
        else
            return false;
    }

    public static boolean hasTelephony(Context context) {
        if(context!=null)
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        else
            return false;
    }

    public static String getDeviceCustomPropertyJsonString(Context c ){
        JSONObject j = new JSONObject();
        try {
            j.put(SdkConstants.WIDTH,getScreenWidth(c));
            j.put(SdkConstants.HEIGHT,getScreenHeight(c));
            j.put(SdkConstants.IS_WIFI,isConnectedWifi(c));
            j.put(SdkConstants.NFC,hasNFC(c));
            j.put(SdkConstants.TELEPHONE, hasTelephony(c));
            j.put(SdkConstants.DEVICE_ID, getAndroidID(c));
            j.put(SdkConstants.DEVICE_NAME, getDeviceName());
            j.put(SdkConstants.OS_NAME, SdkConstants.OS_NAME_VALUE);
            j.put(SdkConstants.OS_VERSION, getAndroidOSVersion());
            j.put(SdkConstants.BR_VERSION, SdkConstants.BR_VERSION_VALUE);

            return j.toString();

        } catch (JSONException e) {
            return "";
        }
    }

    private static String getAndroidOSVersion() {
        return Build.VERSION.RELEASE + "";
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model != null && model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + ":" + model;
        }
    }


    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static boolean isUpdateSessionRequired(Context c) {

        if (c == null) {
            return false;
        }
        long lastSessionSendTimeStamp = getLongPreference(c, SdkConstants.LAST_USED_SESSION_SEND_TIMESTAMP);

        if (lastSessionSendTimeStamp + SdkConstants.DEFAULT_SESSION_SEND_MAX_TIME < System.currentTimeMillis()) {
            setLongPreference(c, SdkConstants.LAST_USED_SESSION_SEND_TIMESTAMP, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public static void resetSessionUpdateTimeStamp(Context c){
        if(c!=null)
            setLongPreference(c, SdkConstants.LAST_USED_SESSION_SEND_TIMESTAMP, 0);
    }

    public static synchronized String getUserSessionId(Context c){

        if(c == null){
            return "";
        }
        long lastUsedSessionTimeStamp = getLongPreference(c, SdkConstants.LAST_USED_SESSION_TIMESTAMP);

        String sessionId="";
        if(lastUsedSessionTimeStamp + SdkConstants.DEFAULT_SESSION_UPDATE_TIME < System.currentTimeMillis())
        {
            sessionId = getAndroidID(c) + "_" + c.getPackageName() + "_" + System.currentTimeMillis();
            setStringPreference(c, SdkConstants.LAST_SESSION_ID, sessionId);
        }else{
            sessionId = getStringPreference(c,SdkConstants.LAST_SESSION_ID);
            if(TextUtils.isEmpty(sessionId)){
                sessionId = getAndroidID(c) + "_" + c.getPackageName() + "_" + System.currentTimeMillis();
                setStringPreference(c,SdkConstants.LAST_SESSION_ID,sessionId);
            }
        }

        setLongPreference(c, SdkConstants.LAST_USED_SESSION_TIMESTAMP , System.currentTimeMillis());

        return sessionId;

    }
    public static NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    /**
     * Check if there is any connectivity to a Wifi network
     *
     * @param context
     * @return
     * @paramtype
     */
    public static boolean isConnectedWifi(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }
}