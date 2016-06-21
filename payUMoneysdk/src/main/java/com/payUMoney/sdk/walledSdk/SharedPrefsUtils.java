package com.payUMoney.sdk.walledSdk;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.payUMoney.sdk.SdkConstants;

/**
 * Created by viswash Singh on 28/7/15.
 */
public class SharedPrefsUtils {
    private SharedPrefsUtils() {
    }

    public static class Keys {
        public static final String DISPLAY_NAME = "display_name";
        public static final String PHONE = "phone";
        public static final String EMAIL = "email";
        public final static String ACCESS_TOKEN = "access_token";
        public final static String REFRESH_TOKEN = "refresh_token";
        public static final String USER_ID = "userId";
        public static final String USER_TYPE = "userType";
		public static final String AVATAR = "AVATAR";
        public static final String ADDED_ON = "LAST_LOGIN";
        public static final String WALLET_BALANCE = "wallet_balance";
        public static final String MAX_WALLET_BALANCE = "maxLimit";
        public static final String MIN_WALLET_BALANCE = "minLimit";

        public static final String P2P_PENDING_COUNT = "p2p_pending_count";
        public static final String P2P_PENDING_AMOUNT = "p2p_pending_amount";


        public static final String MY_BILLS_BADGE_COUNT = "my_bills_badge_count";
    }

    public static String getStringPreference(Context context, String key) {
        String value = null;
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
            if (preferences != null) {
                value = preferences.getString(key, null);
            }
        }
        return value;
    }

    public static boolean getBooleanPreference(Context context, String key) {
        boolean value = false;
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
            if (preferences != null) {
                value = preferences.getBoolean(key, false);
            }
        }
        return value;
    }

    public static float getFloatPreference(Context context, String key) {
        float value = 0;
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
            if (preferences != null) {
                value = preferences.getFloat(key, 0);
            }
        }
        return value;
    }

    public static float getLongPreference(Context context, String key) {
        long value = 0;
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
            if (preferences != null) {
                value = preferences.getLong(key, 0);
            }
        }
        return value;
    }

    public static int getIntPreference(Context context, String key) {
        int value = 0;
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
            if (preferences != null) {
                value = preferences.getInt(key, 0);
            }
        }
        return value;
    }

    public static boolean setStringPreference(Context context, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        if (context != null) {
            if (preferences != null && !TextUtils.isEmpty(key)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(key, value);
                return editor.commit();
            }
        }
        return false;
    }

    public static boolean setBooleanPreference(Context context, String key, boolean value) {
        SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        if (context != null) {
            if (preferences != null && !TextUtils.isEmpty(key)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(key, value);
                return editor.commit();
            }
        }
        return false;
    }

    public static boolean setFloatPreference(Context context, String key, float value) {
        SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        if (context != null) {
            if (preferences != null && !TextUtils.isEmpty(key)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putFloat(key, value);
                return editor.commit();
            }
        }
        return false;
    }

    public static boolean setIntPreference(Context context, String key, int value) {
        SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        if (context != null) {
            if (preferences != null && !TextUtils.isEmpty(key)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(key, value);
                return editor.commit();
            }
        }
        return false;
    }

    public static boolean setLongPreference(Context context, String key, long value) {
        SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        if (context != null) {
            if (preferences != null && !TextUtils.isEmpty(key)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(key, value);
                return editor.commit();
            }
        }
        return false;
    }

    public static boolean removePreferenceByKey(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        if (context != null) {
            if (preferences != null && !TextUtils.isEmpty(key)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove(key);
                editor.apply();
                return editor.commit();
            }
        }
        return false;
    }

    public static boolean removeAllPreference(Context context) {
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
            if (preferences != null) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();
                return editor.commit();
            }
        }
        return false;
    }

    public static boolean hasKey(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        if (context != null) {
            if (preferences != null && !TextUtils.isEmpty(key)) {
                return preferences.contains(key);
            }
        }
        return false;
    }

}
