package com.payUMoney.sdk;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.payUMoney.sdk.entity.Card;
import com.payUMoney.sdk.entity.User;

import org.apache.http.Header;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import de.greenrobot.event.EventBus;

/*import static android.support.v4.app.ActivityCompat.startActivity;*/

/**
 * This class handles the tokens for logging in, and in case the tokens have
 * expired this class is the one that handles getting new ones. This class also
 * maintains the time you last used the app, and broadcasts the logout event, if
 * an access is made after an expired session. Also it exposes the server API to
 * the client with asynchronous {@link EventBus} events on server responses.
 */
public class Session extends Activity {

    public double wallet_points = 0.0;
    private double discount = 0.0;

    public static enum PaymentMode {
        CC, DC, NB, EMI, PAYU_MONEY, STORED_CARDS, CASH
    }

    public static final int PAYMENT_SUCCESS = 3;

    static final Map<PaymentMode, String> PAYMENT_MODE_TITLES;

    static {
        PAYMENT_MODE_TITLES = new HashMap<PaymentMode, String>();
        PAYMENT_MODE_TITLES.put(PaymentMode.CC, "Credit Card");
        PAYMENT_MODE_TITLES.put(PaymentMode.DC, "Debit Card");
        PAYMENT_MODE_TITLES.put(PaymentMode.NB, "Net Banking");
        PAYMENT_MODE_TITLES.put(PaymentMode.EMI, "EMI");
        PAYMENT_MODE_TITLES.put(PaymentMode.PAYU_MONEY, "PayUMoney");
        PAYMENT_MODE_TITLES.put(PaymentMode.STORED_CARDS, "Stored Cards");
        PAYMENT_MODE_TITLES.put(PaymentMode.CASH, "Cash Card");
    }

    private static Session INSTANCE;
    Long start, end, diff;
    private final SessionData mSessionData = new SessionData();

    private final Context mContext;

    private final AsyncHttpClient mHttpClient;

    private final Handler handler;
    public static final String AMOUNT = "amount";
    public static final int RESULT = 100;

    private final EventBus eventBus;

    public JSONArray cards;

    private Session(Context context) //Set Token and User from SharedPrefs in constructor, very clever actually :P
    {
        eventBus = EventBus.getDefault();

        // the handler ensures that all operations happen in the ui thread and
        // not in the background thread. This may very have changed after the
        // removal of dependence on Tasks Class and usage of EventBus, but it
        // hasn't been verified.
        handler = new Handler(Looper.getMainLooper());
        mContext = context;
        mHttpClient = new AsyncHttpClient();
        mHttpClient.setTimeout(60000);

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(Constants.SP_SP_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getString(Constants.TOKEN, null) != null) {
            mSessionData.setToken(sharedPreferences.getString(Constants.TOKEN, null));
        }

        if (sharedPreferences.getString(Constants.EMAIL, null) != null) {
            User user = new User();
            user.setEmail(sharedPreferences.getString(Constants.EMAIL, null));
            mSessionData.setUser(user);
        }

    }


    private static String getAbsoluteUrl(String relativeUrl) {
        if (relativeUrl.equals("/payuPaisa/up.php"))
            return Constants.BASE_URL_IMAGE + relativeUrl;
        else
            return Constants.BASE_URL + relativeUrl;
    }

    public static synchronized Session getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Session(context);
        }
        return INSTANCE;
    }

    public static synchronized Session getInstanceForService() {

        return INSTANCE;
    }

    public static synchronized Session createNewInstance(Context context) {
        INSTANCE = null; //refresh
        INSTANCE = new Session(context);

        return INSTANCE;
    }

    /**
     * ************ENTRY POINT IN SDK********
     */
    public static void startPaymentProcess(Activity mActivity, HashMap<String, String> userParams)  //HashMap has all required params
    {
        //check for all compulsory params
        Intent intent = new Intent(mActivity, HomeActivity.class);//HomeActivity


        if (userParams.get("key").equals("") || userParams.get("key") == null)
            throw new RuntimeException("Merchant Key missing");
        else
            intent.putExtra(Constants.KEY, userParams.get("key"));
        if (userParams.get("hash").equals("") || userParams.get("hash") == null)
            throw new RuntimeException("Hash is  missing");
        else
            intent.putExtra(Constants.HASH, userParams.get("hash"));
        if (userParams.get("TxnId").equals("") || userParams.get("TxnId") == null)
            throw new RuntimeException("TxnId Id missing");
        else
            intent.putExtra(Constants.TXNID, userParams.get("TxnId"));

        if (userParams.get("Amount").equals("") || userParams.get("Amount") == null)
            throw new RuntimeException("Amount is missing");
        else
            intent.putExtra(Constants.AMOUNT, userParams.get("Amount"));
        if (userParams.get("SURL").equals("") || userParams.get("SURL") == null)
            throw new RuntimeException("Surl is missing");
        else
            intent.putExtra(Constants.SURL, userParams.get("SURL"));
        if (userParams.get("FURL").equals("") || userParams.get("FURL") == null)
            throw new RuntimeException("Furl is missing");
        else
            intent.putExtra(Constants.FURL, userParams.get("FURL"));
        if (userParams.get("ProductInfo").equals("") || userParams.get("ProductInfo") == null)
            throw new RuntimeException("Product info is missing");
        else
            intent.putExtra(Constants.PRODUCT_INFO, userParams.get("ProductInfo"));
        if (userParams.get("firstName").equals("") || userParams.get("firstName") == null)
            throw new RuntimeException("Firstname is missing");
        else
            intent.putExtra(Constants.FIRSTNAME, userParams.get("firstName"));
        if (userParams.get("Email").equals("") || userParams.get("Email") == null)
            throw new RuntimeException("Email is missing");
        else
            intent.putExtra(Constants.USER_EMAIL, userParams.get("Email"));
        if (userParams.get("Phone").equals("") || userParams.get("Phone") == null)
            throw new RuntimeException("Phone is missing");
        else
            intent.putExtra(Constants.USER_PHONE, userParams.get("Phone"));

        intent.putExtra(Constants.PARAMS, userParams);

        //Step 2
        mActivity.startActivityForResult(intent, 3); //Start the Home Activity


    }

    public static PublicKey getPublicKey(String key) throws Exception {
        key = key.replaceAll("(-+BEGIN PUBLIC KEY-+\\r?\\n|-+END PUBLIC KEY-+\\r?\\n?)", "").trim();
        // generate public key
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(key, Base64.DEFAULT));
        Log.d("SSS", new String(spec.getEncoded()));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    public void cancelRequests() {
        mHttpClient.cancelRequests(mContext, true);
    }

    public void reset() {
        mSessionData.reset();
    }

    public void postFetch(String url, Task task, Method method) {
        postFetch(url, new RequestParams(), task, method);
    }

    public void postFetch(final String url, final RequestParams params, final Task task, final Method method) {
        if (Constants.DEBUG) {
            Log.d(Constants.TAG, "Session.postFetch: " + url + " " + params + " " + method);
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                List<Header> headers = new ArrayList<Header>();
                headers.add(new BasicHeader("User-Agent", "PayUMoneyAPP"));
                if (mSessionData.getToken() != null) {
                    headers.add(new BasicHeader("Authorization", "Bearer " + mSessionData.getToken()));
                } else {
                    //headers.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
                    headers.add(new BasicHeader("Accept", "*/*;"));
                }
                Header[] headersArray = headers.toArray(new Header[headers.size()]);
                TextHttpResponseHandler responseHandler = new TextHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String res) {
                        diff = System.currentTimeMillis() - start;

                        Log.i("Difference ", "URL=" + url + "Time=" + diff);

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
                        if (statusCode == 401) {
                            logout("force");
                        }
                        runErrorOnHandlerThread(task, e);
                    }
                };

                start = System.currentTimeMillis();

                switch (method) {
                    case POST:
                        mHttpClient.post(mContext, getAbsoluteUrl(url), headersArray, params, null, responseHandler);
                        break;
                    case GET:
                        mHttpClient.get(mContext, getAbsoluteUrl(url), headersArray, params, responseHandler);
                        break;
                    case DELETE:
                        mHttpClient.delete(mContext, getAbsoluteUrl(url), headersArray, responseHandler);
                        break;
                    default:
                        if (Constants.DEBUG) {
                            Log.d(Constants.TAG, "Session.postFetch(...).new Task() {...}.onSuccess: UNKNOWN Method " + method);
                        }
                }
            }
        });
    }

    private void runErrorOnHandlerThread(final Task task, final Throwable e) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            final Throwable x = new Throwable("time out error");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    task.onError(x);
                }
            });
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    task.onError(e);
                }
            });
        }
    }

    private void runSuccessOnHandlerThread(final Task task, final JSONObject jsonObject) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                task.onSuccess(jsonObject);
            }
        });
    }

    private void runSuccessOnHandlerThread(final Task task, final String response) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                task.onSuccess(response);
            }
        });
    }

    /**
     * Get the cached login state
     */
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public User getUser() {
        return mSessionData.getUser();
    }

    /**
     * get the user id
     */


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


    /**
     * Set token func*
     */
    public void setokennow(String mAuthToken) {
        mSessionData.setToken(mAuthToken);
    }

    /**
     * log in on the server
     * <p/>
     * <ul>
     * <li>onSuccess a {@link com.payu.payumoney.CobbocEvent} is dispatched</li>
     * <li>onError a {@link com.payu.payumoney.CobbocEvent} is dispatched</li>
     * </ul>
     */
    // This function is called when we don't know the appId
    public void create(final String username, String password) {

        RequestParams p = new RequestParams();
        p.put("username", username);
        p.put("password", password);
        p.put("scope", "trust");
        p.put("response_type", "token");
        p.put("client_id", "10182"); //10182 -> Remember Me
        p.put("redirect_uri", Constants.BASE_URL);

        addDeviceParams(p);

        postFetch("/auth/authorize", p, new Task() {
            @Override
            public void onSuccess(final JSONObject jsonObject) {
                try {
//                    mSessionData = new SessionData();
                    mSessionData.setToken(jsonObject.getString("access_token")); //Set the token received
                    final User user = new User();
                    user.setEmail(username);
                    mSessionData.setUser(user);
                    postFetch("/auth/user", new Task() {
                        @Override
                        public void onSuccess(JSONObject object) {
                            try {
                                user.setAvatar(new JSONArray(object.getJSONObject(Constants.RESULT).getString(Constants.PROFILE_PICTURE)).getJSONObject(0).getString(Constants.FILE_PATH));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            //					mSessionData.setUser(makeUserFromJSONObject(jsonObject.getJSONObject("user")));
                            eventBus.post(new CobbocEvent(CobbocEvent.LOGIN, true, jsonObject));
                        }

                        @Override
                        public void onSuccess(String response) {

                        }

                        @Override
                        public void onError(Throwable throwable) {
                            eventBus.post(new CobbocEvent(CobbocEvent.LOGIN, false));
                        }

                        @Override
                        public void onProgress(int percent) {

                        }
                    }, Method.GET);
                } catch (Throwable e) {
                    if (Constants.DEBUG) {
                        Log.d(Constants.TAG, e.getMessage());
                    }
                    onError(e);
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            public void onError(Throwable e) {
                eventBus.post(new CobbocEvent(CobbocEvent.LOGIN, false, e.getMessage()));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.POST);

    }

    public void quicklogin(String data) {

        RequestParams p = new RequestParams();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Iterator<?> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            try {

                if (jsonObject.getString(key) == null || jsonObject.getString(key).equals("null")) {
                    jsonObject.put(key, "");
                }
                p.put(key, jsonObject.getString(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        addDeviceParams(p);

        postFetch("/auth/authorize", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
//                    mSessionData = new SessionData();
                    mSessionData.setToken(jsonObject.getString("access_token"));
                    final User user = new User();
                    user.setEmail(jsonObject.getString("email"));
                    mSessionData.setUser(user);
                    postFetch("/auth/user", new Task() {
                        @Override
                        public void onSuccess(JSONObject object) {
                            try {
                                user.setAvatar(new JSONArray(object.getJSONObject(Constants.RESULT).getString(Constants.PROFILE_PICTURE)).getJSONObject(0).getString(Constants.FILE_PATH));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            //					mSessionData.setUser(makeUserFromJSONObject(jsonObject.getJSONObject("user")));
                            eventBus.post(new CobbocEvent(CobbocEvent.LOGIN, true));
                        }

                        @Override
                        public void onSuccess(String response) {

                        }

                        @Override
                        public void onError(Throwable throwable) {
                            eventBus.post(new CobbocEvent(CobbocEvent.LOGIN, true));
                        }

                        @Override
                        public void onProgress(int percent) {

                        }
                    }, Method.GET);
                } catch (Throwable e) {
                    if (Constants.DEBUG) {
                        Log.d(Constants.TAG, e.getMessage());
                    }
                    onError(e);
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            public void onError(Throwable e) {
                eventBus.post(new CobbocEvent(CobbocEvent.LOGIN, false, e.getMessage()));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.POST);
    }

    public void getMyCards() {
        RequestParams p = new RequestParams();

        postFetch("/payment/card/getCardDetails", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    // process merchants
                    cards = jsonObject.getJSONArray(Constants.RESULT);
                    eventBus.post(new CobbocEvent(CobbocEvent.CARDS, true, cards)); //cards JsonArray sent as argument in post
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.CARDS, false));
                }
            }


            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.CARDS, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }

    public void sendFeedback(String message, String email) {
        RequestParams p = new RequestParams();
        p.put("name", "random");
        p.put("email", email.equals("") ? "randomuser@payumoney.com" : email);
        p.put("mobileNo", "9999999999");
        p.put("reason", "PayU Money Feedback response");
        p.put("message", message);
        p.put("userType", "buyer");
        p.put("callbackRequest", "1");
        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.getInt("status") == 0) {
                        eventBus.post(new CobbocEvent(CobbocEvent.FEEDBACK, true));
                    } else {
                        onError(new Throwable("Error!"));
                    }
                } catch (JSONException e) {
                    onError(e);
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.FEEDBACK, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/auth/utils/contactEmail", p, task, Method.POST);
    }


    public void getMyTransactions() {
        RequestParams p = new RequestParams();
        postFetch("/payment/user/getPayments", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {

                    jsonObject = jsonObject.getJSONObject(Constants.RESULT);

                    eventBus.post(new CobbocEvent(CobbocEvent.TRANS_HISTORY, true, jsonObject));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.TRANS_HISTORY, false));
                }
            }


            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.TRANS_HISTORY, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }


    /*
    * get payupoints
    * and wallet*/
    public void getUserPoints() {
        RequestParams p = new RequestParams();
        postFetch("/vault/getAvailableAmount", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    jsonObject = jsonObject.getJSONObject(Constants.RESULT);
                    eventBus.post(new CobbocEvent(CobbocEvent.USER_POINTS, true, jsonObject));
                    Log.d("payumoneypoints", jsonObject.toString());
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.USER_POINTS, false));
                }
            }


            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.USER_POINTS, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }


    public void resetPassword(String old_password, String new_password) {
        RequestParams p = new RequestParams();
        p.put("oldPassword", old_password);
        p.put("newPassword", new_password);

        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {
                    eventBus.post(new CobbocEvent(CobbocEvent.RESET_PASSWORD, true, object.getJSONObject(Constants.RESULT)));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.RESET_PASSWORD, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.RESET_PASSWORD, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/auth/user/update/password", p, task, Method.POST);
    }

    public void VerifyMobilecode(String otp, String num) {
        RequestParams p = new RequestParams();
        p.put("mobile", num);
        p.put("code", otp);

        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {
                    eventBus.post(new CobbocEvent(CobbocEvent.VERIFY_OTP, true, object.getString(Constants.MESSAGE)));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.VERIFY_OTP, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.VERIFY_OTP, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/auth/userVerification/mobile", p, task, Method.POST);
    }

    public void generateMobilecode(String num) {
        RequestParams p = new RequestParams();
        p.put("mobile", num);

        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {
                    eventBus.post(new CobbocEvent(CobbocEvent.GENERATE_OTP, true, object.getString(Constants.MESSAGE)));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.GENERATE_OTP, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.GENERATE_OTP, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/auth/userVerification/generateMobileCode", p, task, Method.POST);
    }

    public void generateEmailcode(String email) {
        RequestParams p = new RequestParams();
        p.put("email", email);

        Task task = new Task() {


            @Override
            public void onSuccess(JSONObject object) {
                try {
                    eventBus.post(new CobbocEvent(CobbocEvent.GENERATE_EMAIL_CODE, true, object.getString(Constants.MESSAGE)));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.GENERATE_EMAIL_CODE, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.GENERATE_EMAIL_CODE, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/auth/userVerification/generateEmailCode", p, task, Method.POST);
    }

    public void updateUserDetails(String name, String phone, JSONObject j, JSONObject img) {
        RequestParams p = new RequestParams();
        try {
            Iterator<?> keys = j.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (j.getString(key) == null || j.getString(key).equals("null")) {
                    j.put(key, "");
                }
                p.put(key, j.getString(key));
            }
            p.put("name", name);
            p.put("phone", phone);
            //if(!img.isNull("filePath"))
            if (img != null) {
                JSONArray array = new JSONArray();
                array.put(img);
                p.put("profilePicture", array.toString());
            }

        } catch (JSONException jx) {
        }
        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {
                    eventBus.post(new CobbocEvent(CobbocEvent.CONTACT_INFO_UPDATE, true, object.getJSONObject(Constants.RESULT)));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.CONTACT_INFO_UPDATE, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.CONTACT_INFO_UPDATE, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/auth/user/update", p, task, Method.POST);
    }

    public void getUserContactDetail() {
        RequestParams p = new RequestParams();
        postFetch("/auth/user", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {

                    jsonObject = jsonObject.getJSONObject(Constants.RESULT);

                    eventBus.post(new CobbocEvent(CobbocEvent.CONTACT_INFO, true, jsonObject));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.CONTACT_INFO, false));
                }
            }


            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.CONTACT_INFO, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }

    public void getUserAddressDetail() {
        RequestParams p = new RequestParams();
        postFetch("/auth/user", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {

                    jsonObject = jsonObject.getJSONObject(Constants.RESULT);

                    eventBus.post(new CobbocEvent(CobbocEvent.ADDRESS_INFO, true, jsonObject));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.ADDRESS_INFO, false));
                }
            }


            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.ADDRESS_INFO, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }

    public void updateUserAddress(JSONObject add, String type) {
        RequestParams p = new RequestParams();

        if (type.equals("shipAddress")) {
            p.put("addressType", type);
            p.put("address", add);
        } else {
            try {
                Iterator<?> keys = add.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    p.put(key, add.getString(key));

                }
            } catch (Exception e) {
            }
        }
        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (jsonObject.has(Constants.RESULT)) {
                    eventBus.post(new CobbocEvent(CobbocEvent.ADDRESS_INFO_UPDATE, true));
                } else {
                    eventBus.post(new CobbocEvent(CobbocEvent.ADDRESS_INFO_UPDATE, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.ADDRESS_INFO_UPDATE, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/auth/user/updateAddress", p, task, Method.POST);
    }

    public void uploadPhoto(String path) {
        RequestParams p = new RequestParams();

        File myFile = new File(path);
        myFile.getName();
        try {
            p.put("Filedata", myFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Task task = new Task() {


            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    jsonObject = jsonObject.getJSONObject(Constants.RESULT);

                    eventBus.post(new CobbocEvent(CobbocEvent.UPLOAD_IMAGE, true, jsonObject));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.UPLOAD_IMAGE, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.UPLOAD_IMAGE, false));
            }

            @Override
            public void onProgress(int percent) {
                eventBus.post(new CobbocEvent(CobbocEvent.UPLOAD_IMAGE_PROGRESS, true, percent));
            }
        };
        postFetch("/payuPaisa/up.php", p, task, Method.POST);
    }

    public void getMoreTransactions(final String s, int co, int type) {
        RequestParams p = new RequestParams();
        String url;
        if (s.equals("filter")) {
            if (co != 0)
                url = "/payment/user/getPayments?count=12&days=" + co + "&offset=0&status=";
            else
                url = "/payment/user/getPayments?count=12&days=&offset=0&status=";
        } else {
            if (type != 0)
                url = "/payment/user/getPayments?count=12&days=" + type + "&offset=" + co + "&status=";
            else
                url = "/payment/user/getPayments?count=12&days=&offset=" + co + "&status=";
        }
        postFetch(url, p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {

                    jsonObject = jsonObject.getJSONObject(Constants.RESULT);
                    if (s.equals("filter"))
                        eventBus.post(new CobbocEvent(CobbocEvent.TRANS_HISTORY, true, jsonObject));
                    else
                        eventBus.post(new CobbocEvent(CobbocEvent.TRANS_HISTORY, false, jsonObject));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.TRANS_HISTORY, false));
                }
            }


            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.TRANS_HISTORY, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }

    public void checkAppVersionAvailable() {
        RequestParams p = new RequestParams();
        postFetch("/appUpdatesAvailable/5", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    jsonObject = jsonObject.getJSONObject(Constants.RESULT);
                    eventBus.post(new CobbocEvent(CobbocEvent.APP_VERSION_CHECK, true, jsonObject));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.APP_VERSION_CHECK, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.APP_VERSION_CHECK, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }

    public void searchForMerchant(String query, final int off) {
        RequestParams p = new RequestParams();
        p.put("param", query);
        p.put("count", 10);
        p.put("offset", off);
        postFetch("/auth/searchMerchant", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    JSONArray result = jsonObject.getJSONArray(Constants.RESULT);
                    // get the payment id, and send it in the event. The activity will then take over there and send us to the webview.
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject object = result.getJSONObject(i);
                        if (object.get(Constants.ADDRESS) == null || object.getString(Constants.ADDRESS).equals("null")) {
                            object.remove(Constants.ADDRESS);
                            result.put(i, object);
                        }
                        if (object.get(Constants.PHONE) == null || object.getString(Constants.PHONE).equals("null")) {
                            object.remove(Constants.PHONE);
                            result.put(i, object);
                        }
                    }
                    eventBus.post(new CobbocEvent(CobbocEvent.MERCHANT_SEARCH, true, result));
                } catch (JSONException e) {
                    onError(e);
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.MERCHANT_SEARCH, false, off));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }


    /**
     * Delete the credit {@link Card} with the id provided
     * <p/>
     * <ul>
     * <li>A {@link com.payu.payumoney.CobbocEvent} is dispatched</li>
     * </ul>
     */
    public void deleteCard(final int id) {
        RequestParams p = new RequestParams();
        p.put("cardId", id);
        postFetch("/payment/card/removeCard", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.getBoolean(Constants.RESULT)) {
                        eventBus.post(new CobbocEvent(CobbocEvent.CARD_DELETED, true, id));
                    } else {
                        throw new Throwable();
                    }
                } catch (Throwable e) {
                    reportBack(e);
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable e) {
                reportBack(e);
            }

            @Override
            public void onProgress(int percent) {

            }

            private void reportBack(Throwable e) {
                if (Constants.DEBUG) {
                    Log.e(Constants.TAG, "Session.deleteCard(...).new Task() {...}.onError: " + e.getMessage());
                }
                eventBus.post(new CobbocEvent(CobbocEvent.CARD_DELETED, false, id));
            }
        }, Method.POST);
    }

    /**
     * Create an Payment on the server
     * <p/>
     * <ul>
     * <li>A {@link com.payu.payumoney.CobbocEvent} is dispatched</li>
     * </ul>
     *
     * @param mid         Mid of merchant
     * @param amount      the amount
     * @param description text describing the product or service
     */
    public void createPayment(HashMap<String, String> params) {
        RequestParams p = new RequestParams();
        p.put("key", params.get("key"));
        p.put("amount", params.get("Amount"));
        p.put("txnid", params.get("TxnId"));
        p.put("productinfo", params.get("ProductInfo"));
        p.put("firstname", params.get("firstName"));
        p.put("email", params.get("Email"));
        p.put("udf1", params.get("udf1"));
        p.put("udf2", params.get("udf2"));
        p.put("udf3", params.get("udf3"));
        p.put("udf4", params.get("udf4"));
        p.put("udf5", params.get("udf5"));
        p.put("hash", params.get("hash"));
        p.put("paymentIdentifiers", "[]");
       p.put("purchaseFrom", "merchant-app");
      // p.put("purchaseFrom", "applongtail");
        // p.put("txnDetails", "{\"surl\": \"" + Constants.BASE_URL + "/mobileapp/payumoney/success.php\", \"furl\": \"" + Constants.BASE_URL + "/mobileapp/payumoney/failure.php\",\"confirmSMSPhone\": \""+mob+"\"\n" +
        //      "        }");
        p.put("txnDetails", "{\"surl\": \"" + params.get("SURL") + "\", \"furl\": \"" + params.get("FURL") + "\"}");
        // p.put("txnDetails", "{\"surl\":\"https://www.payumoney.com/mobileapp/payumoney/success.php\", \"furl\": \"https://www.payumoney.com/mobileapp/payumoney/failure.php\"}");
        p.put("paymentParts", "[]");
        p.put("deviceId",params.get("deviceId"));
        p.put("appVersion", params.get("appVersion"));
        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {

                    JSONObject result =  object.getJSONObject(Constants.RESULT);
                    JSONObject paymentOfferDTO = result.getJSONObject("paymentOfferDTO");

                    if (!paymentOfferDTO.getString("newCashBackAmount").equals("-1.0")) {

                        /*Fitty Farty*/
                        String s = paymentOfferDTO.getString("newCashBackAmount");
                        setrevisedCashbackReceivedStatus("1");
                        discount = Double.parseDouble(s);

                    }
                    else if(!paymentOfferDTO.getString("amount").equals("null")) {

                         /*Normal*/
                    // modifiedDiscount = paymentOfferDTO.getJSONObject("amount").getDouble(Constants.AMOUNT);
                        String s = paymentOfferDTO.getString("amount");
                        setrevisedCashbackReceivedStatus("0");
                        discount = Double.parseDouble(s);
                    }

                  //  eventBus.post(new CobbocEvent(CobbocEvent.CREATE_PAYMENT, true, object.getJSONObject(Constants.RESULT).getJSONObject("PaymentDTO")/*.getString(Constants.PAYMENT_ID)*/));
                 eventBus.post(new CobbocEvent(CobbocEvent.CREATE_PAYMENT, true, object.getJSONObject(Constants.RESULT)/*.getString(Constants.PAYMENT_ID)*/));
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.CREATE_PAYMENT, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.CREATE_PAYMENT, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };

        postFetch("/payment/payment/addPaymentBySDK", p, task, Method.POST );
    }

    public void forgotPassword(String email) {
        RequestParams p = new RequestParams();
        p.put("userName", email);
        postFetch("/auth/forgot/password", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                eventBus.post(new CobbocEvent(CobbocEvent.FORGOT_PASSWORD, true, jsonObject));
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.FORGOT_PASSWORD, false, throwable.getMessage()));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.POST);
    }

    /**
     * Create a credit {@link Card} on the server
     * <p/>
     * <ul>
     * <li>A {@link com.payu.payumoney.CobbocEvent} is dispatched</li>
     * </ul>
     *
     * @param number      the number
     * @param expiryMonth the 2 digit month of mCardExpiry
     * @param expiryYear  the 4 digit year of mCardExpiry
     */
    public void createCard(String number, String label, final String mode, JSONObject hash, int expiryMonth, int expiryYear) {
        final RequestParams p = new RequestParams();
        // if(expiryMonth==null)
        try {
            p.put("key", hash.getString("key"));
            p.put("hash", hash.getString("hash"));
            p.put("command", hash.getString("command"));
            p.put("var1", hash.getString("var1"));
            p.put("var2", label);
            p.put("var3", mode);
            p.put("var4", mode.equals("DC") ? "VISA" : mode);
            p.put("var5", "PayUMoney");
            p.put("var6", number);
            p.put("var7", expiryMonth);
            p.put("var8", expiryYear);
        } catch (JSONException e) {

        }
        postFetch("/payu/webservice/postservice.php", p, new Task() {
            @Override
            public void onSuccess(String response) {
                // now actually send this to the other url

                RequestParams p = new RequestParams();
                p.put("cardResponse", response);
                p.put("cardMode", mode);
                p.put("cardType", mode.equals("DC") ? "VISA" : mode);
                p.put("nameOnCard", "payumoney app");

                postFetch("/payment/card/pareseAndAddCard", p, new Task() {
                    @Override
                    public void onSuccess(String response) {
                    }

                    @Override
                    public void onSuccess(JSONObject object) {

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        eventBus.post(new CobbocEvent(CobbocEvent.CREATE_CARD, false));
                    }

                    @Override
                    public void onProgress(int percent) {

                    }
                }, Method.POST);
            }

            @Override
            public void onSuccess(JSONObject object) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.CREATE_CARD, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.POST);
    }

    /**
     * Sign up the user
     * <p/>
     * <ul>
     * <li>A {@link CobbocEvent} is dispatched</li>
     * </ul>
     *
     * @param email    This is the username of the user
     * @param phone    This is the phone of the user
     * @param password This is the password of the user
     */
    public void sign_up(String email, String phone, String password) {
        RequestParams p = new RequestParams();
        p.put("userType", "customer");
        p.put("username", email);
        p.put("phone", phone);
        p.put("source", "payumoney app");
        p.put("password", password);
        p.put("pageSource", "sign up");
        p.put("name", email);
        postFetch("/auth/register", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.get(Constants.RESULT) == null || jsonObject.getString(Constants.RESULT).equals("null")) {
                        // fuck!
                        eventBus.post(new CobbocEvent(CobbocEvent.SIGN_UP, false, jsonObject.getString("msg")));
                    } else {
                        eventBus.post(new CobbocEvent(CobbocEvent.SIGN_UP, true, jsonObject.getJSONObject(Constants.RESULT)));
                    }
                } catch (JSONException e) {
                    eventBus.post(new CobbocEvent(CobbocEvent.SIGN_UP, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.SIGN_UP, false, "An error occurred while trying to sign you up. Please try again later."));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.POST);
    }

    /**
     * Get the card hash to be sent to PayU on the server and dispatch a {@link com.payu.payumoney.CobbocEvent}
     */

    public void getCardHash() {
        RequestParams p = new RequestParams();
        postFetch("/payment/card/getStoreCardHash", p, new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                if (Constants.DEBUG) {
                    Log.d(Constants.TAG, "Session.getCardHash().new Task() {...}.onSuccess: ");
                }
                try {
                    eventBus.post(new CobbocEvent(CobbocEvent.GET_CARD_HASH, true, object.getJSONObject(Constants.RESULT)));
                } catch (JSONException e) {
                    //final Throwable x = new Throwable("Card number seems to be Invalid");
                    onError(new Throwable());
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.GET_CARD_HASH, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }

    /**
     * Logout on the server and dispatch a {@link com.payu.payumoney.CobbocEvent}
     */

    public void logout(String message) {
        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                if (Constants.DEBUG) {
                    Log.d(Constants.TAG, "Session.logout().new Task() {...}.onSuccess: ");
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        RequestParams p = new RequestParams();
        p.put("_method", "delete");
        postFetch("/logout", p, task, Method.POST);
        eventBus.post(new CobbocEvent(CobbocEvent.LOGOUT, false, message));
        if (!message.equals("force"))
            mSessionData.reset();
    }

    public void logout() {
        logout(null);
    }

    public void getPaymentDetails(String paymentId) {
        RequestParams p = new RequestParams();
        postFetch("/payment/customer/getPaymentMerchant/" + paymentId, p, new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {
                    eventBus.post(new CobbocEvent(CobbocEvent.PAYMENT_DETAILS, true, object.getJSONObject(Constants.RESULT)));
                } catch (JSONException e) {
                    onError(e);
                }

            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.PAYMENT_DETAILS, false, throwable.getMessage()));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }


    /*
    Send to payU with Wallet
    params: Details  Json --> fetch all payment-Merchant details from here
    params: mode - NB CC DC wallet
    params: Hashmap data - bankobject key etc.
    params: cashback -> points/wallet depending on the call
    params: vault -> points/wallet depending on the call
     */
    public void sendToPayUWithWallet(JSONObject details, final String mode, final HashMap<String, Object> data, Double cashback, Double vault) throws JSONException {
        wallet_points = vault; //Set points or wallet depending on the call
        sendToPayU(details, mode, data, cashback); //cashback is point/wallet to be payed depending on the call

    }

    /*
       Send to payU with Wallet
       params: Details  Json --> fetch all payment-Merchant details from here
       params: mode - NB CC DC wallet
       params: Hashmap data - bankobject key etc.
       params: cashback is wallet or points
        */
    public void sendToPayU(JSONObject details, final String mode, final HashMap<String, Object> data, Double cashback1) throws JSONException {
        Float cashback = round(cashback1, 2);
        // first get the transaction keys
        RequestParams p = new RequestParams();
        Double Dis_amt = 0.0;

        if (HomeActivity.coupan_amt != 0) {
            p.put("couponUsed", HomeActivity.choosedCoupan);
            Dis_amt = HomeActivity.coupan_amt;
        }else {
            //DecimalFormat format = new DecimalFormat("0.#");
            Dis_amt = discount;
        }


        /*no payupoints
          * this will be fired in most cases*/
        if (cashback != null && cashback == 0) {
            Double payUAmt = (details.getJSONObject(Constants.PAYMENT).getDouble("totalAmount") + new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject(mode).getDouble("DEFAULT") - Dis_amt);
            if (wallet_points > 0.0) {
                payUAmt -= wallet_points; //substract wallet
                p.put("sourceAmountMap", "{\"PAYU\":" + payUAmt + ",\"DISCOUNT\":" + Dis_amt  + ",\"WALLET\":" + wallet_points + "}"); //here wallet_point is wallet money
            } else {
                p.put("sourceAmountMap", "{\"PAYU\":" + payUAmt + ",\"DISCOUNT\":" + Dis_amt + "}");
            }
        } else if (mode.equals("wallet")) //Points+Wallet OR Just Wallet Payments
        {
            if (wallet_points == 0.0) //Pure wallet
            {
                p.put("sourceAmountMap", "{\"WALLET\":" + cashback + ",\"PAYU\":" + 0 + ",\"DISCOUNT\":" + Dis_amt + "}");
            } else //wallet+points
                p.put("sourceAmountMap", "{\"WALLET\":" + cashback + ",\"CASHBACK\":" + wallet_points + ",\"PAYU\":" + 0 + ",\"DISCOUNT\":" + Dis_amt + "}"); //here wallet_point is PayUPoints (depends on call)

        } else if (mode.equals("points")) //Have PayUPoints and has selected to pay via payupoints
        {
            p.put("sourceAmountMap", "{\"CASHBACK\":" + cashback + ",\"PAYU\":" + 0 + ",\"DISCOUNT\":" + Dis_amt + "}");
        } else  //Has PayUPoints but opted not to pay via payupoints
        {
            Double payUAmt = (details.getJSONObject(Constants.PAYMENT).getDouble("totalAmount") + new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject(mode).getDouble("DEFAULT") - Dis_amt - cashback);
            if (wallet_points > 0.0) payUAmt -= wallet_points;
            p.put("sourceAmountMap", "{\"CASHBACK\":" + cashback + ",\"PAYU\":" + payUAmt + ",\"DISCOUNT\":" + Dis_amt + ",\"WALLET\":" + wallet_points + "}");
        }

        wallet_points = 0.0; //reinitialize to 0 for future use on same instance

        if (mode.equals("points") || mode.equals("wallet"))
            p.put("PG", "WALLET");
        else
            p.put("PG", mode); // Pure points/wallet - > CC, Pure Credit Card -> CC Debit Card -> DC Net banking -> NB

        if (!mode.equals("points") && !mode.equals("wallet")) {
            if (data.containsKey("bankcode")) {
                p.put("bankCode", data.get("bankcode"));
            } else {
                p.put("bankCode", mode);
            }
        }

        if (data.containsKey("storeCardId")) {
            p.put("storeCardId", String.valueOf(data.get("storeCardId")));
        }
        p.put("revisedCashbackReceivedStatus", getrevisedCashbackReceivedStatus());

        Log.d("Params posted-->", p.toString());

        postFetch("/payment/customer/getPaymentMerchant/" + details.getJSONObject(Constants.PAYMENT).getString(Constants.PAYMENT_ID), p, new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {

                    Log.d("Success on --->", object.toString());

                    object = object.getJSONObject(Constants.RESULT);
                    JSONObject p = new JSONObject();

                    if (!mode.equals("points") && !mode.equals("wallet")) //For NB CC AND DC
                    {

                        String key = (String) data.get("key");
                        data.remove("key");
                      //  if(!object.getString(Constants.TRANSACTION_DTO).equals("null")) {
                            object = object.getJSONObject(Constants.TRANSACTION_DTO).getJSONObject("hash");
                      //  }
                        if (!mode.equals("NB")) {
                            // we'll need to encrypt
                            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                            // encrypt the plain text using the public key
                            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(key));
                            String text = data.get("ccnum") + "|payu|" + data.get("ccvv") + "|" + data.get("ccexpmon") + "|" + data.get("ccexpyr") + "|";
                            p.put("encrypted_payment_data", URLEncoder.encode(Base64.encodeToString(cipher.doFinal(text.getBytes()), Base64.DEFAULT)));
                        }
                        Iterator keys = object.keys();
                        while (keys.hasNext()) {
                            key = (String) keys.next();
                            p.put(key, object.getString(key));
                        }
                        p.put("pg", mode);
                        if (data.containsKey("bankcode")) {
                            p.put("bankcode", data.get("bankcode"));
                        } else {
                            p.put("bankcode", mode);
                        }
                        if (!mode.equals("NB")) {
                            if (data.containsKey(Constants.LABEL)) {
                                p.put(Constants.LABEL, data.get(Constants.LABEL));
                            }
                            if (data.containsKey(Constants.STORE)) {
                                p.put(Constants.STORE, data.get(Constants.STORE));
                            }
                            if (data.containsKey("store_card_token")) {
                                p.put("store_card_token", data.get("store_card_token"));
                            }
                        }
                        eventBus.post(new CobbocEvent(CobbocEvent.PAYMENT, true, p));
                    } else //For wallet and PayuPoints
                    {
                        eventBus.post(new CobbocEvent(CobbocEvent.PAYMENT_POINTS, true, object));
                    }


                } catch (JSONException e) {
                    onError(e);
                } catch (IllegalBlockSizeException e) {
                    onError(e);
                } catch (BadPaddingException e) {
                    onError(e);
                } catch (NoSuchAlgorithmException e) {
                    onError(e);
                } catch (NoSuchPaddingException e) {
                    onError(e);
                } catch (InvalidKeyException e) {
                    onError(e);
                } catch (Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onSuccess(String response) {
                Log.d("Success on String --->", response.toString());
            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new CobbocEvent(CobbocEvent.PAYMENT, false, throwable.getMessage()));
                //   Toast.makeText(getApplicationContext(),throwable.toString(),Toast.LENGTH_LONG).show();


                Log.d("failure on --->", throwable.toString());
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Method.GET);
    }

    public String getToken() {
        return mSessionData.getToken();
    }

    public void setrevisedCashbackReceivedStatus(String s){mSessionData.setrevisedCashbackReceivedStatus(s);}

    public String getrevisedCashbackReceivedStatus(){return mSessionData.revisedCashbackReceivedStatus;}


    public enum Method {
        POST, GET, DELETE
    }

    public interface Task {
        public void onSuccess(JSONObject object);

        public void onSuccess(String response);

        public void onError(Throwable throwable);

        void onProgress(int percent);
    }

    private class SessionData {
        private String token;
        private User user;
        private String revisedCashbackReceivedStatus="0";

        public SessionData() {
            reset();
        }

        public void setrevisedCashbackReceivedStatus(String s){

            revisedCashbackReceivedStatus=s;

        }

        public String revisedCashbackReceivedStatus(){
            return revisedCashbackReceivedStatus;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public void reset() {
            token = null;
            user = null;
        }
    }

    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public static float round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }


}
