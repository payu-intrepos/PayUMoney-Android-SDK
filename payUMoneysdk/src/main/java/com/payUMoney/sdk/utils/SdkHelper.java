package com.payUMoney.sdk.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.entity.SdkUser;

import org.json.JSONObject;

public class SdkHelper {

    private static long mLastClickTime = 0;
    public static final String COL_NAME = "name";
    public static final String COL_PHONE = "phone";
    public static final String COL_EMAIL = "email";
    public static final String COL_AVATAR = "avatar";
    public static final String COL_USER_ID = "userId";
    public static final String COL_PASSWORD_CHANGED = "password_changed";
    private static ProgressDialog progressDialog;

    public static boolean checkNetwork(Context c) {
        ConnectivityManager conMgr = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(conMgr != null){
            NetworkInfo resultTypeMobile = conMgr.getNetworkInfo(0);
            NetworkInfo resultTypeWifi = conMgr.getNetworkInfo(1);
            if(((resultTypeMobile != null && resultTypeMobile.isConnectedOrConnecting())) || (resultTypeWifi != null && resultTypeWifi.isConnectedOrConnecting())){
                return true;
            }
            else
                return false;

        }

        else {
            return false;
        }
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
}