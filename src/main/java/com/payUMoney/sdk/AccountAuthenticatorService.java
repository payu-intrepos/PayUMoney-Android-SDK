package com.payUMoney.sdk;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.payUMoney.sdk.AuthActivity.AuthenticatorActivity;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


public class AccountAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        //   if (intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT))
        //     return null;

        AbstractAccountAuthenticator authenticator =
                new FussyLogicAuthenticator(this);
        return authenticator.getIBinder();
    }

    // --------------------------------

    public class FussyLogicAuthenticator extends AbstractAccountAuthenticator {
        protected Context mContext;
        String authToken = "";
        private final AsyncHttpClient mHttpClient;
        private final Handler handler;

        public FussyLogicAuthenticator(Context context) {
            super(context);
            this.mContext = context;
            mHttpClient = new AsyncHttpClient();
            mHttpClient.setTimeout(60000);
            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
            return null;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] strings, Bundle options) throws NetworkErrorException {
            // We absolutely cannot add an account without some information
            // from the user; so we're definitely going to return an Intent
            // via KEY_INTENT
            final Bundle bundle = new Bundle();

            // We're going to use a LoginActivity to talk to the user (mContext
            // we'll have noted on construction).
            final Intent intent = new Intent(mContext, AuthenticatorActivity.class);

            // We can configure that activity however we wish via the
            // Intent.  We'll set ARG_IS_ADDING_NEW_ACCOUNT so the Activity
            // knows to ask for the account name as well
            intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
            intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
            intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);

            // It will also need to know how to send its response to the
            // account manager; LoginActivity must derive from
            // AccountAuthenticatorActivity, which will want this key set
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                    response);

            // Wrap up this intent, and return it, which will cause the
            // intent to be run
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
                throws NetworkErrorException {

            // We can add rejection of a request for a token type we
            // don't support here

            // Get the instance of the AccountManager that's making the
            // request
            final AccountManager am = AccountManager.get(mContext);

            // See if there is already an authentication token stored
            authToken = am.peekAuthToken(account, authTokenType);

            // If we have no token, use the account credentials to fetch
            // a new one, effectively another logon
            if (authToken != null) {
                if (TextUtils.isEmpty(authToken)) {
                    Log.d("Token is empty", "token is empty");
                    final String password = am.getPassword(account);
                    if (password != null) {
                        Log.d("ID", account.name.toString());
                        Log.d("pass", password.toString());
                        create(account.name, password);

                        long startTime = System.currentTimeMillis();

                        //Wait until create function is completed

                        while (TextUtils.isEmpty(authToken)) {
                            long nowtim = System.currentTimeMillis();
                            if (startTime - nowtim > 60000) break;
                        }
                    }
                }

                // If we either got a cached token, or fetched a new one, hand
                // it back to the client that called us.
                if (!TextUtils.isEmpty(authToken)) {
                    Log.d("Token is NOT empty", "token is NOT empty");
                    final Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                    result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                    return result;
                }
            }
            // If we get here, then we don't have a token, and we don't have
            // a password that will let us get a new one (or we weren't able
            // to use the password we do have).  We need to fetch
            // information from the user, we do that by creating an Intent
            // to an Activity child class.
            final Intent intent = new Intent(mContext, AuthenticatorActivity.class);

            // We want to give the Activity the information we want it to
            // return to the AccountManager.  We'll cover that with the
            // KEY_ACCOUNT_AUTHENTICATOR_RESPONSE parameter.
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                    response);
            // We'll also give it the parameters we've already looked up, or
            // were given.
            intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, false);
            intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_NAME, account.name);
            intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type);
            intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);

            // Remember that we have to return a Bundle, not an Intent, but
            // we can tell the caller to run our intent to get its
            // information with the KEY_INTENT parameter in the returned
            // Bundle
            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }

        @Override
        public String getAuthTokenLabel(String s) {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) throws NetworkErrorException {
            return null;
        }

        public void create(final String username, String password) {
            RequestParams p = new RequestParams();
            p.put("username", username);
            p.put("password", password);
            p.put("scope", "trust");
            p.put("response_type", "token");
            p.put("client_id", "10182"); //10182 -> Remember Me
            p.put("redirect_uri", Constants.BASE_URL);
            Log.d("Cr8ed paramsbfr device", "yes");
            addDeviceParams(p);
            Log.d("Cr8ed params", "yes");

            postFetch("/auth/authorize", p, new Session.Task() {
                @Override
                public void onSuccess(final JSONObject jsonObject) {
                    try {
                        authToken = jsonObject.getString("access_token");

                        Log.d("Suc", jsonObject.toString());

                    } catch (Throwable e) {
                        if (Constants.DEBUG) {
                            Log.d(Constants.TAG, e.getMessage());
                        }
                        onError(e);
                    }
                }

                @Override
                public void onSuccess(String response) {
                    Log.d("Suc", response.toString());
                }

                public void onError(Throwable e) {
                    Log.d("Error", e.toString());
                }

                @Override
                public void onProgress(int percent) {

                }
            }, "POST");
            //   Log.d("return",authToken);

        }

        protected void addDeviceParams(RequestParams p) {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            String imeiNumber = telephonyManager.getDeviceId();

            WifiInfo wifiInf = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
            String macAddr = wifiInf.getMacAddress();

            String UID = null;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                if (imeiNumber != null) {
                    UID = new BigInteger(1, md.digest(imeiNumber.getBytes("UTF-8"))).toString(16);
                } else if (macAddr != null) {
                    UID = new BigInteger(1, md.digest(macAddr.getBytes("UTF-8"))).toString(16);
                } else {
                    UID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
                }
            } catch (NoSuchAlgorithmException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            p.put("deviceName", android.os.Build.MODEL);

            p.put("deviceUDID", UID);

        }

        public void postFetch(final String url, final RequestParams params, final Session.Task task, final String method) {
            if (Constants.DEBUG) {
                Log.d(Constants.TAG, "Session.postFetch: " + url + " " + params + " " + method);
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    List<Header> headers = new ArrayList<Header>();
                    if (authToken != null) {
                        headers.add(new BasicHeader("Authorization", "Bearer " + authToken));
                    } else {
                        //headers.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
                        headers.add(new BasicHeader("Accept", "*/*;"));
                    }
                    Header[] headersArray = headers.toArray(new Header[headers.size()]);
                    TextHttpResponseHandler responseHandler = new TextHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String res) {

                            if (Constants.DEBUG) {
                                Log.d(Constants.TAG, "Session.postFetch.onSuccess: " + url + " " + params + " " + method + ": " + res);
                            }
                            try {
                                JSONObject object = new JSONObject(res);
                                try {
                                    if (object.has("error")) {
                                        onFailure(statusCode, headers, object.getString("error"), new Throwable(object.getString("error")));
                                    } else {
                                        runSuccessOnHandlerThread(task, object);
                                    }
                                } catch (JSONException e) {
                                    onFailure(statusCode, headers, e.getMessage(), e);
                                }
                            } catch (JSONException e) {
                                // maybe this is a string?
                                runSuccessOnHandlerThread(task, res);
                            }

                        }

                        @Override
                        public void onProgress(int bytesWritten, int totalSize) {
                            task.onProgress(bytesWritten * 100 / totalSize);
                        }

                        @Override
                        public void onFinish() {
                            Log.d(Constants.TAG, "Done!");
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String msg, Throwable e) {
                            if (Constants.DEBUG) {
                                Log.e(Constants.TAG, "Session...new JsonHttpResponseHandler() {...}.onFailure: " + e.getMessage() + " " + msg);
                            }

                        }
                    };


                    if (method.equals("POST"))
                        mHttpClient.post(mContext, getAbsoluteUrl(url), headersArray, params, null, responseHandler);

                }
            });
        }

        private void runSuccessOnHandlerThread(final Session.Task task, final JSONObject jsonObject) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    task.onSuccess(jsonObject);
                }
            });
        }

        private void runSuccessOnHandlerThread(final Session.Task task, final String response) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    task.onSuccess(response);
                }
            });
        }

        private String getAbsoluteUrl(String relativeUrl) {
            if (relativeUrl.equals("/payuPaisa/up.php"))
                return Constants.BASE_URL_IMAGE + relativeUrl;
            else
                return Constants.BASE_URL + relativeUrl;
        }


    }

    ;


}