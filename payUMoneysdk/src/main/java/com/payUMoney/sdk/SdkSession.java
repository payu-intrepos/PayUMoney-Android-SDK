package com.payUMoney.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.payUMoney.sdk.utils.SdkHelper;
import com.payUMoney.sdk.utils.SdkLogger;
import com.payUMoney.sdk.walledSdk.SharedPrefsUtils;
import com.payUMoney.sdk.walledSdk.WalletSdkLoginSignUpActivity;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Cipher;

import de.greenrobot.event.EventBus;


/**
 * This class handles the tokens for logging in, and in case the tokens have
 * expired this class is the one that handles getting new ones. This class also
 * maintains the time you last used the app, and broadcasts the logout event, if
 * an access is made after an expired sdkSession. Also it exposes the server API to
 * the client with asynchronous {@link EventBus} events on server responses.
 */
public class SdkSession {

    //public static Activity merchantContext = null;
    private boolean mIsLogOutCall = false;
    public double wallet_points = 0.0;

//    public static final int PAYMENT_SUCCESS = PayUmoneySdkInitilizer.PAYU_SDK_PAYMENT_REQUEST_CODE;

    public enum PaymentMode {
        CC, DC, NB, EMI, PAYU_MONEY, STORED_CARDS, CASH
    }

    public enum Method {
        POST, GET, DELETE
    }

    public interface Task {
        void onSuccess(JSONObject object);

        void onSuccess(String response);

        void onError(Throwable throwable);

        void onProgress(int percent);
    }

    private class SessionData {
        private String token = null;
        private String revisedCashbackReceivedStatus = "0";

        public SessionData() {
            reset();
        }

        public void setrevisedCashbackReceivedStatus(String s) {

            revisedCashbackReceivedStatus = s;

        }

       /* public String revisedCashbackReceivedStatus() {
            return revisedCashbackReceivedStatus;
        }*/

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public void reset() {
            token = null;
        }
    }

    static final Map<PaymentMode, String> PAYMENT_MODE_TITLES;

    static {
        PAYMENT_MODE_TITLES = new HashMap<>();
        PAYMENT_MODE_TITLES.put(PaymentMode.CC, "Credit Card");
        PAYMENT_MODE_TITLES.put(PaymentMode.DC, "Debit Card");
        PAYMENT_MODE_TITLES.put(PaymentMode.NB, "Net Banking");
        PAYMENT_MODE_TITLES.put(PaymentMode.EMI, "EMI");
        PAYMENT_MODE_TITLES.put(PaymentMode.PAYU_MONEY, "PayUMoney");
        PAYMENT_MODE_TITLES.put(PaymentMode.STORED_CARDS, "Stored Cards");
        PAYMENT_MODE_TITLES.put(PaymentMode.CASH, "Cash Card");
    }

    private static SdkSession INSTANCE = null;
    public static final String TAG = SdkSession.class.getSimpleName();
    Long start = null, end = null, diff = null;
    private final SessionData mSessionData = new SessionData();

    private final Context mContext;

    //private final AsyncHttpClient mHttpClient;
    private RequestQueue mRequestQueue;

    private final Handler handler;

    private final EventBus eventBus;

    private String loginMode = "";
    private String guestEmail = "";
    private static String clientId;
    private static String merchantKey;
    private static String merchantSalt;
    private static String merchantTxnId;

    public String getLoginMode() {
        return loginMode;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setLoginMode(String loginMode) {
        this.loginMode = loginMode;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    private SdkSession(Context context) //Set Token and User from SharedPrefs in constructor, very clever actually :P
    {
        eventBus = EventBus.getDefault();

        // the handler ensures that all operations happen in the ui thread and
        // not in the background thread. This may very have changed after the
        // removal of dependence on Tasks Class and usage of EventBus, but it
        // hasn't been verified.
        handler = new Handler(Looper.getMainLooper());
        mContext = context;
        clientId = null;
        mIsLogOutCall = false;

        // SharedPreferences sharedPreferences = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Context.MODE_PRIVATE);
        String mToken = SharedPrefsUtils.getStringPreference(mContext, SdkConstants.ACCESS_TOKEN);
        if (mToken != null) {
            mSessionData.setToken(mToken);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        if (relativeUrl.equals("/payuPaisa/up.php"))
            return SdkConstants.BASE_URL_IMAGE + relativeUrl;
        else
            return SdkConstants.BASE_URL + relativeUrl;
    }

    public static synchronized SdkSession getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SdkSession(context);
        }
        return INSTANCE;
    }

    public RequestQueue getRequestQueue(Context context) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TextUtils.isEmpty(SdkSession.TAG) ? TAG : SdkSession.TAG);
        getRequestQueue(mContext).add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    public static synchronized SdkSession getInstanceForService() {

        return INSTANCE;
    }

    public static synchronized SdkSession createNewInstance(Context context) {
        INSTANCE = null; //refresh
        INSTANCE = new SdkSession(context);

        return INSTANCE;
    }

    /**
     * ************ENTRY POINT IN SDK********
     */
    public static void startPaymentProcess(Activity mActivity, HashMap<String, String> userParams)  //HashMap has all required params
    {
        //merchantContext = mActivity;
        if (SdkConstants.WALLET_SDK) {
            merchantKey = userParams.get(SdkConstants.KEY);
            merchantSalt = userParams.get(SdkConstants.SALT);
            merchantTxnId = userParams.get(SdkConstants.MERCHANT_TXNID);
            clientId = userParams.get(SdkConstants.CLIENT_ID);

            Intent intent = new Intent(mActivity, WalletSdkLoginSignUpActivity.class);
            intent.putExtra(SdkConstants.PARAMS, userParams);
            mActivity.startActivityForResult(intent, PayUmoneySdkInitilizer.PAYU_SDK_PAYMENT_REQUEST_CODE);
        } else {
        /*SDK specific,Handling logout from SDK in case of Merchant's App with SDK*/
        /*if(merchantContext == null)
        merchantContext = mActivity;
        Intent intent = new Intent(merchantContext, SdkHomeActivityNew.class);*/

            Intent intent = new Intent(mActivity, SdkHomeActivityNew.class);
            if (!userParams.containsKey(SdkConstants.PAYUMONEY_APP) && !userParams.containsKey(SdkConstants.PAYUBIZZ_APP)) {
                //check for all compulsory params
                /*if (userParams.get(SdkConstants.KEY) == null || userParams.get(SdkConstants.KEY).equals(""))
                    throw new RuntimeException("Merchant Key missing");
                else
                    intent.putExtra(SdkConstants.KEY, userParams.get(SdkConstants.KEY));*/

                if (userParams.get(SdkConstants.HASH) == null || userParams.get(SdkConstants.HASH).equals(""))
                    throw new RuntimeException("Hash is  missing");
                else
                    intent.putExtra(SdkConstants.HASH, userParams.get(SdkConstants.HASH));

                if (SdkConstants.WALLET_SDK) {
                    if (userParams.get(SdkConstants.MERCHANT_TXNID) == null || userParams.get(SdkConstants.MERCHANT_TXNID).equals(""))
                        throw new RuntimeException("TxnId Id missing");
                    else
                        intent.putExtra(SdkConstants.MERCHANT_TXNID, userParams.get(SdkConstants.MERCHANT_TXNID));
                } else {
                    if (userParams.get(SdkConstants.TXNID) == null || userParams.get(SdkConstants.TXNID).equals(""))
                        throw new RuntimeException("TxnId Id missing");
                    else
                        intent.putExtra(SdkConstants.TXNID, userParams.get(SdkConstants.TXNID));
                }

                if (userParams.get(SdkConstants.AMOUNT) == null || userParams.get(SdkConstants.AMOUNT).equals(""))
                    throw new RuntimeException("Amount is missing");
                else
                    intent.putExtra(SdkConstants.AMOUNT, userParams.get(SdkConstants.AMOUNT));

                if (userParams.get(SdkConstants.SURL) == null || userParams.get(SdkConstants.SURL).equals(""))
                    throw new RuntimeException("Surl is missing");
                else
                    intent.putExtra(SdkConstants.SURL, userParams.get(SdkConstants.SURL));

                if (userParams.get(SdkConstants.FURL) == null || userParams.get(SdkConstants.FURL).equals(""))
                    throw new RuntimeException("Furl is missing");
                else
                    intent.putExtra(SdkConstants.FURL, userParams.get(SdkConstants.FURL));

                if (userParams.get(SdkConstants.PRODUCT_INFO) == null || userParams.get(SdkConstants.PRODUCT_INFO).equals(""))
                    throw new RuntimeException("Product info is missing");
                else
                    intent.putExtra(SdkConstants.PRODUCT_INFO, userParams.get(SdkConstants.PRODUCT_INFO));

                if (userParams.get(SdkConstants.FIRSTNAME) == null || userParams.get(SdkConstants.FIRSTNAME).equals(""))
                    throw new RuntimeException("Firstname is missing");
                else
                    intent.putExtra(SdkConstants.FIRSTNAME, userParams.get(SdkConstants.FIRSTNAME));

                if (userParams.get(SdkConstants.EMAIL) == null || userParams.get(SdkConstants.EMAIL).equals(""))
                    throw new RuntimeException("Email is missing");
                else
                    intent.putExtra(SdkConstants.EMAIL, userParams.get(SdkConstants.EMAIL));

                if (userParams.get(SdkConstants.PHONE).equals("") || userParams.get(SdkConstants.PHONE) == null)
                    throw new RuntimeException("Phone is missing");
                else
                    intent.putExtra(SdkConstants.PHONE, userParams.get(SdkConstants.PHONE));
            }

            intent.putExtra(SdkConstants.PARAMS, userParams);

            //Step 2
            //merchantContext.startActivityForResult(intent, PAYMENT_SUCCESS);
            mActivity.startActivityForResult(intent, PayUmoneySdkInitilizer.PAYU_SDK_PAYMENT_REQUEST_CODE);//Start the Home Activity


        }
    }


    public void fetchMechantParams(String merchantId) {

        /*allowGuestCheckout can have three values: guestcheckout, guestcheckoutonly, quickGuestCheckout
        quickLogin will have 0 or 1*/
        final Map<String, String> p = new HashMap<>();
        p.put(SdkConstants.MERCHANT_ID, merchantId);
        String uri = String.format("/auth/app/op/merchant/LoginParams" + getParameters(p),
                merchantId);

        postFetch(uri, null, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FETCH_MERCHANT_PARAMS, true, jsonObject));
                    /*if(result.get)
                    String quickLogin = result.getString("quickLogin");
                    String*/

            }

            @Override
            public void onSuccess(String response) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FETCH_MERCHANT_PARAMS, false));
            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FETCH_MERCHANT_PARAMS, false, null));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.GET);
    }

    private Map<String, String> getUserSessionInfo() {
        Map<String, String> customHeaders = new HashMap<String, String>();
        customHeaders.put(SdkConstants.USER_SESSION_COOKIE, SdkHelper.getUserSessionId(mContext));
        customHeaders.put(SdkConstants.CUSTOM_BROWSER_PROPERTY, SdkHelper.getDeviceCustomPropertyJsonString(mContext));
        customHeaders.put(SdkConstants.USER_SESSION_COOKIE_PAGE_URL, SharedPrefsUtils.getStringPreference(mContext, SdkConstants.USER_SESSION_COOKIE_PAGE_URL));
        if (SdkHelper.isUpdateSessionRequired(mContext)) {
            customHeaders.put(SdkConstants.USER_SESSION_UPDATE, "1");
        }
        return customHeaders;
    }

    public void createWallet(String email, String phone, String OTP) {
        final Map<String, String> p = new HashMap<>();

        p.put("userName", email);
        p.put("name", email);
        p.put("mobile", phone);
        p.put("otp", OTP);

        postFetch("/vault/app/createWallet", p, new Task() {
            //Override Task interface
            //**//*//**//**//**//****CHECK WITH SHOBHIT FOR JSON FORMAT

            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.get(SdkConstants.STATUS) == null || jsonObject.getString(SdkConstants.STATUS).equals("null")) {
                        // No response from server :/ -- Internet off/Server Down/Device internal issue
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_WALLET, false, jsonObject));
                    } else {
                        //Server response {errorCode,GUID, sessionId, message, status }
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_WALLET, true, jsonObject));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_WALLET, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_WALLET, false, "An error occurred while verifying your OTP. Please generate again."));
            }

            @Override
            public void onProgress(int percent) {

            }

        }, Request.Method.POST);

    }

    public void sendMobileVerificationCodeForWalletCreation(String phone) {
        final Map<String, String> p = new HashMap<>();

        p.put("mobile", phone);
        p.put("otpType", "R");

        //Call postFetch
        postFetch("/auth/app/generateWalletCode", p, new Task() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.get(SdkConstants.STATUS) == null || jsonObject.getString(SdkConstants.STATUS).equals("null")) {
                        // No response from server :/ -- Internet off/Server Down/Device internal issue
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.SEND_OTP_TO_USER, false, jsonObject.getString("message")));
                    } else {
                        //Server response {errorCode,GUID, sessionId, message, status }
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.SEND_OTP_TO_USER, true, jsonObject));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.SEND_OTP_TO_USER, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.SEND_OTP_TO_USER, false, "An error occurred while trying to sign you up. Please try again later."));
            }

            @Override
            public void onProgress(int percent) {
            }

        }, Request.Method.POST);

    }

    public void fetchUserParams(String paymentId) {

        final Map<String, String> p = new HashMap<>();
        p.put("paymentId", paymentId);
        postFetch("/payment/app/fetchPaymentUserData", p, new Task() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.has(SdkConstants.RESULT) && !jsonObject.isNull(SdkConstants.RESULT))
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FETCH_USER_PARAMS, true, jsonObject.getJSONObject(SdkConstants.RESULT)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(String response) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FETCH_USER_PARAMS, false));
            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FETCH_USER_PARAMS, false, null));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);

    }

    public static PublicKey getPublicKey(String key) throws Exception {
        key = key.replaceAll("(-+BEGIN PUBLIC KEY-+\\r?\\n|-+END PUBLIC KEY-+\\r?\\n?)", "").trim();
        // generate public key
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(key, Base64.DEFAULT));
        SdkLogger.d(SdkConstants.TAG, new String(spec.getEncoded()));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    /*public void cancelRequests() {
        mHttpClient.cancelRequests(mContext, true);
    }*/

    public void reset() {
        mSessionData.reset();
    }

    public void postFetch(final String url, final Map<String, String> params, final Task task, final int method) {
        postFetch(url, params, null, task, method);
    }

    public void postFetch(final String url, final Map<String, String> params, final Map<String, String> customHeader, final Task task, final int method) {
        if (PayUmoneySdkInitilizer.IsDebugMode()) {
            SdkLogger.d(SdkConstants.TAG, "SdkSession.postFetch: " + url + " " + params + " " + method);
        }

        StringRequest myRequest = new StringRequest(method, getAbsoluteUrl(url), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                diff = Long.valueOf(System.currentTimeMillis() - start.longValue());

                SdkLogger.i("Difference ", "URL=" + url + "Time=" + diff);

                if (PayUmoneySdkInitilizer.IsDebugMode()) {
                    SdkLogger.d(SdkConstants.TAG, "SdkSession.postFetch.onSuccess: " + url + " " + params + " " + method + ": " + response);
                }

                try {
                    JSONObject object = new JSONObject(response);
                    if (object.has("error")) {
                        onFailure(object.getString("error"), new Throwable(object.getString("error")));
                    } else {
                        runSuccessOnHandlerThread(task, object);
                    }
                } catch (JSONException e) {
                    // maybe this is a string?
                    onFailure(e.getMessage(), e);
                }
            }

            public void onFailure(String msg, Throwable e) {
                if (PayUmoneySdkInitilizer.IsDebugMode()) {
                    Log.e(SdkConstants.TAG, "Session...new JsonHttpResponseHandler() {...}.onFailure: " + e.getMessage() + " " + msg);
                }
                if (msg.contains("401")) {
                    /*not required in app*/
                    if (SdkConstants.WALLET_SDK) {
                        if (!mIsLogOutCall) {
                            logout();
                        } else {
                            mIsLogOutCall = false;
                        }
                    } else {
                        logout("force");
                        cancelPendingRequests(TAG);
                    }
                }
                runErrorOnHandlerThread(task, e);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (PayUmoneySdkInitilizer.IsDebugMode()) {
                    Log.e(SdkConstants.TAG, "Session...new JsonHttpResponseHandler() {...}.onFailure: " + error.getMessage());
                }
                if (error != null && error.networkResponse != null && error.networkResponse.statusCode == 401) {
                   /*not required in app*/
                    if (SdkConstants.WALLET_SDK) {
                        if (!mIsLogOutCall) {
                            logout();
                        } else {
                            mIsLogOutCall = false;
                        }
                    } else {
                        logout("force");
                    }
                }
                runErrorOnHandlerThread(task, error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                if (SdkConstants.WALLET_SDK) {
                    params.put(SdkConstants.CLIENT_ID, clientId);
                    params.put(SdkConstants.IS_MOBILE, "1");
                }
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();

                if (customHeader != null && !customHeader.isEmpty()) {
                    params.putAll(customHeader);
                }

                params.putAll(getUserSessionInfo());

                params.put("User-Agent", "PayUMoneyAPP");
                if (getToken() != null) {
                    params.put("Authorization", "Bearer " + getToken());
                } else {
                    params.put("Accept", "*/*;");
                }
                return params;
            }

            @Override
            public String getBodyContentType() {
                if (getToken() == null) {
                    return "application/x-www-form-urlencoded";
                } else {
                    return super.getBodyContentType();
                }
            }
        };
        myRequest.setShouldCache(false);
        myRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        addToRequestQueue(myRequest);
        start = Long.valueOf(System.currentTimeMillis());

    }

    private String getParameters(Map<String, String> params) {
        String parameters = "?";
        Iterator it = params.entrySet().iterator();
        boolean isFirst = true;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (isFirst) {
                parameters = parameters.concat(pair.getKey() + "=" + pair.getValue());
            } else {
                parameters = parameters.concat("&" + pair.getKey() + "=" + pair.getValue());
            }
            isFirst = false;
            it.remove();
        }
        return parameters;
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

    public void setToken(String token) {
        mSessionData.setToken(token);
    }

    /*private void handleOneClickAndOneTapFeature(JSONObject userDto) {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE).edit();
        try {

            if (userDto.has(SdkConstants.CONFIG_DTO) && !userDto.isNull(SdkConstants.CONFIG_DTO)) {
                JSONObject userConfigDtoTmp = userDto.getJSONObject(SdkConstants.CONFIG_DTO);
                String salt = SdkConstants.DEBUG ? SdkConstants.AUTHORIZATION_SALT_TEST : SdkConstants.AUTHORIZATION_SALT_PROD;
                if (userConfigDtoTmp.has(SdkConstants.AUTHORIZATION_SALT) && !userConfigDtoTmp.isNull(SdkConstants.AUTHORIZATION_SALT)) {
                    if (salt.equals(userConfigDtoTmp.optString(SdkConstants.AUTHORIZATION_SALT, SdkConstants.XYZ_STRING))) {
                        editor.putBoolean(SdkConstants.ONE_CLICK_PAYMENT, false);
                    } else {
                        editor.putBoolean(SdkConstants.ONE_CLICK_PAYMENT, userConfigDtoTmp.optBoolean(SdkConstants.ONE_CLICK_PAYMENT, false));
                        editor.putString(SdkConstants.CONFIG_DTO, userConfigDtoTmp.toString());
                        if (userConfigDtoTmp.has(SdkConstants.ONE_TAP_FEATURE) && !userConfigDtoTmp.isNull(SdkConstants.ONE_TAP_FEATURE)) {
                            editor.putBoolean(SdkConstants.ONE_TAP_FEATURE, userConfigDtoTmp.optBoolean(SdkConstants.ONE_TAP_FEATURE, false));
                        }
                    }
                }

            }
            editor.commit();
            editor.apply();
        } catch (JSONException e) {

            editor.putBoolean(SdkConstants.ONE_TAP_FEATURE, false);
            editor.putBoolean(SdkConstants.ONE_CLICK_PAYMENT, false);
            editor.commit();
            editor.apply();
            e.printStackTrace();
        }

    }*/

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

        final Map<String, String> p = new HashMap<>();
        p.put("grant_type", "password");
        //p.put("client_id", "180551"); // Always Merchant KEY
        p.put(SdkConstants.CLIENT_ID, "10182");
        p.put(SdkConstants.USER_NAME, username);
        p.put(SdkConstants.PASSWORD, password);  //Password --> Device Master Token , OTP , Password

        postFetch("/auth/oauth/token", p, new Task() {
            @Override
            public void onSuccess(final JSONObject jsonObject) {
                try {
//                    mSessionData = new SessionData();
                    if (jsonObject.has(SdkConstants.ACCESS_TOKEN) && !jsonObject.isNull(SdkConstants.ACCESS_TOKEN)) {

                        String token = jsonObject.getString(SdkConstants.ACCESS_TOKEN);
                        SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.ACCESS_TOKEN, token);
                        SdkSession.getInstance(mContext).setToken(token);
                        SdkHelper.resetSessionUpdateTimeStamp(mContext);

                        SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.EMAIL, username);

/*
                        postFetch("/auth/app/user", null, new Task() {
                            @Override
                            public void onSuccess(JSONObject object) {
                                try {

                                    JSONObject result = object.getJSONObject(SdkConstants.RESULT);
                                    SharedPreferences.Editor editor = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE).edit();
                                    if (result != null) {

                                        if (result.has(SdkConstants.USER_NAME) && !result.isNull(SdkConstants.USER_NAME)) {
                                            String username = result.getString(SdkConstants.USER_NAME);
                                            editor.putString(SdkConstants.USER_NAME, username);
                                            editor.putString(SdkConstants.EMAIL, username);
                                        }
                                        if (result.has(SdkConstants.PHONE) && !result.isNull(SdkConstants.PHONE)) {
                                            editor.putString(SdkConstants.PHONE, result.getString(SdkConstants.PHONE));
                                        }
                                        if (result.has(SdkConstants.NAME) && !result.isNull(SdkConstants.NAME)) {
                                            editor.putString(SdkConstants.NAME, result.getString(SdkConstants.NAME));
                                        }
                                        handleOneClickAndOneTapFeature(result);
                                    }
                                    editor.commit();
                                    editor.apply();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGIN, true, object));
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
                        }, Request.Method.GET);
*/
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGIN, true, jsonObject));
                    } else {

                        SdkLogger.d(SdkConstants.TAG, "Token Not Found");
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGIN, false, "Error"));
                    }
                } catch (Throwable e) {
                    if (PayUmoneySdkInitilizer.IsDebugMode()) {
                        SdkLogger.d(SdkConstants.TAG, e.getMessage());
                    }
                    onError(e);
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            public void onError(Throwable e) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGIN, false, e.getMessage()));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);

    }


    public void generateAndSendOtp(String userEmail) {

        final Map<String, String> p = new HashMap<>();
        p.put("requestType", "login");
        p.put("userName", userEmail);
        postFetch("/auth/app/op/generateAndSendOTP", p, new Task() {

            @Override
            public void onSuccess(JSONObject object) {
                // JSONObject jsonObject = new JSONObject(object);
                // Toast.makeText(SdkSession.getInstance(getApplicationContext()), object.getString("message"), Toast.LENGTH_LONG).show();
                //return result=object.getString("message");
                if (object.has(SdkConstants.STATUS)) {
                    String status = object.optString(SdkConstants.STATUS);
                    if (status == null || status.equals("-1"))
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.GENERATE_AND_SEND_OTP, false, object));
                    else
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.GENERATE_AND_SEND_OTP, true, object));

                }

            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {

                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.GENERATE_AND_SEND_OTP, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);

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
        final Map p = new HashMap<>();
        p.put(SdkConstants.KEY, params.get(SdkConstants.KEY));
        p.put(SdkConstants.AMOUNT, params.get(SdkConstants.AMOUNT));
        p.put(SdkConstants.TXNID, params.get(SdkConstants.TXNID));
        p.put(SdkConstants.PRODUCT_INFO_STRING, params.get(SdkConstants.PRODUCT_INFO));
        p.put(SdkConstants.FIRST_NAME_STRING, params.get(SdkConstants.FIRSTNAME));
        p.put(SdkConstants.EMAIL, params.get(SdkConstants.EMAIL));
        p.put(SdkConstants.UDF1, params.get(SdkConstants.UDF1));
        p.put(SdkConstants.UDF2, params.get(SdkConstants.UDF2));
        p.put(SdkConstants.UDF3, params.get(SdkConstants.UDF3));
        p.put(SdkConstants.UDF4, params.get(SdkConstants.UDF4));
        p.put(SdkConstants.UDF5, params.get(SdkConstants.UDF5));
        p.put(SdkConstants.HASH, params.get(SdkConstants.HASH));
        p.put(SdkConstants.PAYMENT_IDENTIFIERS_STRING, "[]");
        p.put("purchaseFrom", "merchant-app");
        // p.put("purchaseFrom", "applongtail");
        // p.put("txnDetails", "{\"surl\": \"" + Constants.BASE_URL + "/mobileapp/payumoney/success.php\", \"furl\": \"" + Constants.BASE_URL + "/mobileapp/payumoney/failure.php\",\"confirmSMSPhone\": \""+mob+"\"\n" +
        //      "        }");
        p.put("txnDetails", "{\"surl\": \"" + params.get(SdkConstants.SURL) + "\", \"furl\": \"" + params.get(SdkConstants.FURL) + "\"}");
        // p.put("txnDetails", "{\"surl\":\"https://www.payumoney.com/mobileapp/payumoney/success.php\", \"furl\": \"https://www.payumoney.com/mobileapp/payumoney/failure.php\"}");
        p.put(SdkConstants.PAYMENT_PARTS_STRING, "[]");
        p.put("deviceId", params.get("deviceId"));
        // p.put("appVersion", params.get("appVersion"));
        p.put("isMobile", "1");
        if (loginMode.equals("guestLogin"))
            p.put("guestCheckout", "true");
        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {

                    if (object.has(SdkConstants.RESULT) && !object.isNull(SdkConstants.RESULT)) {

                        JSONObject result = object.getJSONObject(SdkConstants.RESULT);

                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_PAYMENT, true, result /*object.getJSONObject(Constants.RESULT).getString(Constants.PAYMENT_ID)*/));
                    } else if (object.has(SdkConstants.MESSAGE) && !object.isNull(SdkConstants.MESSAGE)) {

                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_PAYMENT, false, object.toString()));
                    } else {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_PAYMENT, false));
                        SdkLogger.e(object.toString());
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_PAYMENT, false));
                }
            }

            @Override
            public void onSuccess(String response) {
                /*String Response for this call Signifies something went wrong*/
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_PAYMENT, false));

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_PAYMENT, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };

        postFetch("/payment/app/payment/addSdkPayment", p, task, Request.Method.POST);
    }

    public void updateTransactionDetails(String paymentId, String guestEmail) {

        final Map p = new HashMap<>();
        p.put(SdkConstants.PAYMENT_ID, paymentId);
        JSONObject tmp = new JSONObject();
        try {

            tmp.put("guestemail", guestEmail);
            tmp.put("state", "null");
            tmp.put("country", "null");
            tmp.put("addressId", "null");
            tmp.put("addressLine", "null");
            tmp.put("entityId", "null");
            tmp.put("city", "null");
            tmp.put("zipcode", "null");
            tmp.put("entityType", "null");
            tmp.put("city", "null");
            tmp.put("city", "null");
            tmp.put("city", "null");
        } catch (JSONException e) {

        }
        p.put("txnDetails", tmp.toString());
        Task task1 = new Task() {
            @Override
            public void onSuccess(JSONObject object) {

                SdkLogger.d("ss", "entered success json");

            }

            @Override
            public void onSuccess(String response) {
                SdkLogger.d(SdkConstants.TAG, "entered success");
            }

            @Override
            public void onError(Throwable throwable) {
                SdkLogger.d(SdkConstants.TAG, "entered error");
            }

            @Override
            public void onProgress(int percent) {
                SdkLogger.d(SdkConstants.TAG, "entered progress");
            }
        };
        postFetch("/payment/app/op/payment/updateTxnDetails", p, task1, Request.Method.POST);

    }

    public void forgotPassword(String email) {
        final Map<String, String> p = new HashMap<>();
        p.put("userName", email);
        postFetch("/auth/app/forgot/password", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FORGOT_PASSWORD, true, jsonObject));
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FORGOT_PASSWORD, false, throwable.getMessage()));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);
    }

    public void getNetBankingStatus() {

        final Map<String, String> p = new HashMap<>();

        postFetch("/payment/op/getNetBankingStatus", null, new Task() {
            @Override
            public void onSuccess(final JSONObject jsonObject) {
                try {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.NET_BANKING_STATUS, true, (new JSONObject((new JSONArray(jsonObject.getString(SdkConstants.RESULT))).getString(0)))));
                } catch (JSONException e) {
                    e.printStackTrace();
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.NET_BANKING_STATUS, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.NET_BANKING_STATUS, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.GET);

    }

    /**
     * Sign up the user
     * <p/>
     * <ul>
     * <li>A {@link SdkCobbocEvent} is dispatched</li>
     * </ul>
     *
     * @param email    This is the username of the user
     * @param phone    This is the phone of the user
     * @param password This is the password of the user
     */
    public void sign_up(String email, String phone, String password) {
        final Map<String, String> p = new HashMap<>();
        p.put("userType", "customer");
        p.put("username", email);
        p.put("phone", phone);
        p.put("source", "payumoney app");
        p.put("password", password);
        p.put("pageSource", "sign up");
        p.put("name", email);
        /**/
        postFetch("/auth/app/register", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.get(SdkConstants.RESULT) == null || jsonObject.getString(SdkConstants.RESULT).equals("null")) {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.SIGN_UP, false, jsonObject.getString("msg")));
                    } else {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.SIGN_UP, true, jsonObject.getJSONObject(SdkConstants.RESULT)));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.SIGN_UP, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.SIGN_UP, false, "An error occurred while trying to sign you up. Please try again later."));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);
    }

    /**
     * Logout on the server and dispatch a {@link com.payu.payumoney.CobbocEvent}
     */

    public void logout(String message) {

        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGOUT, false, message));
        if (message != null && !message.equals("force"))
            mSessionData.reset();
    }

    public void logout() {
        if (SdkConstants.WALLET_SDK) {
            logoutWalletSdk();
        } else {
            logout(null);
        }
    }

    private void logoutWalletSdk() {

        mIsLogOutCall = true;
        final Map<String, String> p = new HashMap<>();
        p.put(SdkConstants.TOKEN, SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.ACCESS_TOKEN));
        p.put(SdkConstants.EMAIL, SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.EMAIL));
        p.put(SdkConstants.MOBILE, SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.PHONE));

        p.put(SdkConstants.HASH, getHashForThisCall(SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.PHONE)
                , SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.EMAIL), null, null, null));

        postFetch("/auth/ext/wallet/deleteToken", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    int status = jsonObject.getInt(SdkConstants.STATUS);
                    if (status < 0) {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGOUT, false, jsonObject.getString(SdkConstants.MESSAGE)));
                    } else {

                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.ACCESS_TOKEN);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.REFRESH_TOKEN);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.EMAIL);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.PHONE);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.USER_ID);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.USER_TYPE);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.DISPLAY_NAME);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.AVATAR);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.WALLET_BALANCE);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.P2P_PENDING_COUNT);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.P2P_PENDING_AMOUNT);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.MY_BILLS_BADGE_COUNT);
                        SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.ADDED_ON);
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGOUT, true, jsonObject.getString(SdkConstants.MESSAGE)));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGOUT, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGOUT, false, "An error occurred while trying to logging you out. Please try again later."));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);
    }

    public void updatePostBackParamDetails(Map postBackParamMap) {

        final Map p = postBackParamMap;

        Task task1 = new Task() {
            @Override
            public void onSuccess(JSONObject object) {

                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.POST_BACK_PARAM, true));

            }

            @Override
            public void onSuccess(String response) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.POST_BACK_PARAM, true));
            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.POST_BACK_PARAM, false));
            }

            @Override
            public void onProgress(int percent) {
                SdkLogger.d(SdkConstants.TAG, "entered progress");
            }
        };
        postFetch("/payment/app/processP2Response", p, task1, Request.Method.POST);

    }
    public void notifyUserCancelledTransaction(String paymentId, String userCancelled) {

        final Map<String, String> p = new HashMap<>();
        p.put(SdkConstants.PAYMENT_ID, paymentId);

        if(userCancelled != null) {
            p.put(SdkConstants.USER_CANCELLED_TRANSACTION, userCancelled);
        }


        postFetch("/payment/postBackParam.do" + getParameters(p), null, new Task() {
            @Override
            public void onSuccess(final JSONObject jsonObject) {

                if (PayUmoneySdkInitilizer.IsDebugMode()) {
                    SdkLogger.d(SdkConstants.TAG, "Successfully Cancelled the transaction");
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
        }, Request.Method.GET);

    }

    /*
    Send to payU with Wallet
    params: Details  Json --> fetch all payment-Merchant details from here
    params: mode - NB CC DC wallet
    params: Hashmap data - bankobject key etc.
    params: cashback -> points/wallet depending on the call
    params: vault -> points/wallet depending on the call
     */
    public void sendToPayUWithWallet(JSONObject details, final String mode, final HashMap<String, Object> data, Double cashback, Double vault, Double discount, Double convenienceChargesAmount) throws JSONException {
        wallet_points = vault.doubleValue(); //Set points or wallet depending on the call
        sendToPayU(details, mode, data, cashback, discount, convenienceChargesAmount); //cashback is point/wallet to be payed depending on the call
    }

    /*private boolean checkForValidObject(JSONObject jsonObject, String paymentModeString) {
        try {
            if (jsonObject.has(paymentModeString)
                    && !jsonObject.isNull(paymentModeString)
                    && !("-1").equals(jsonObject.getString(paymentModeString))
                    && !(SdkConstants.FALSE_STRING).equals(jsonObject.getString(paymentModeString))
                    && !(SdkConstants.NULL_STRING).equals(jsonObject.getString(paymentModeString))) {
                return true;
            }

            return false;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }*/

    /*
       Send to payU with Wallet
       params: Details  Json --> fetch all payment-Merchant details from here
       params: mode - NB CC DC wallet
       params: Hashmap data - bankobject key etc.
       params: cashback is wallet or points
        */
    /*private double getConvenienceChargesAmount(JSONObject details, String mode, boolean mWalletOrPointPayment) {

        double convenienceChargesAmount = 0.0;

        try {
            if (checkForValidObject(details, SdkConstants.CONVENIENCE_CHARGES)) {
                JSONObject convenienceChargesJsonObject = new JSONObject(details.getString(SdkConstants.CONVENIENCE_CHARGES));
                String currentMode = ((SdkConstants.POINTS).equals(mode) || SdkConstants.WALLET.equals(mode)) ? SdkConstants.WALLET_STRING : mode;

                if (checkForValidObject(convenienceChargesJsonObject, currentMode)) {
                    if (SdkConstants.WALLET_STRING.equals(mode)) {
                        convenienceChargesAmount = Math.max(convenienceChargesJsonObject.getJSONObject(mode).getDouble(SdkConstants.DEFAULT), 0.0);
                        if (convenienceChargesAmount <= 0.0 && checkForValidObject(convenienceChargesJsonObject, SdkConstants.PAYMENT_MODE_DC)) {
                            convenienceChargesAmount = Math.max(convenienceChargesJsonObject.getJSONObject(SdkConstants.PAYMENT_MODE_DC).getDouble(SdkConstants.DEFAULT),
                                    Double.valueOf(details.getJSONObject(SdkConstants.PAYMENT).getDouble(SdkConstants.ORDER_AMOUNT)) * SdkConstants.FIXED_CONVENIENCE_CHARGES_COMPONENT);
                        }
                    } else if (mWalletOrPointPayment) {
                        convenienceChargesAmount = Math.max(convenienceChargesJsonObject.getJSONObject(mode).getDouble(SdkConstants.DEFAULT),
                                convenienceChargesJsonObject.getJSONObject(SdkConstants.WALLET_STRING).getDouble(SdkConstants.DEFAULT));
                    } else {
                        convenienceChargesAmount = convenienceChargesJsonObject.getJSONObject(mode).getDouble(SdkConstants.DEFAULT);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return convenienceChargesAmount;
    }*/

    public void sendToPayU(JSONObject details, final String mode, final HashMap<String, Object> data, Double cashback1, Double discount, Double convenienceChargesAmount) throws JSONException {
        Float cashback = Float.valueOf(round(cashback1.doubleValue()));
        // first get the transaction keys
        final Map<String, String> p = new HashMap<>();
        Double Dis_amt;


        // Remove these things:
        /*SdkConstants.ccvv_VALUE = (String)data.get("ccvv");
        SdkConstants.ccexpmon_VALUE = (Integer)data.get("ccexpmon")+"";
        SdkConstants.ccexpyr_VALUE = (Integer)data.get("ccexpyr")+"";*/

        final String paymentID = details.optString(SdkConstants.PAYMENT_ID);

        if (SdkHomeActivityNew.coupan_amt != 0) {
            p.put(SdkConstants.COUPON_USED, SdkHomeActivityNew.choosedCoupan);
            Dis_amt = Double.valueOf(SdkHomeActivityNew.coupan_amt);
        } else {
            //DecimalFormat format = new DecimalFormat("0.#");
            Dis_amt = discount;
        }
        /*no payupoints
          * this will be fired in most cases*/
        //double convenienceChargesAmount = getConvenienceChargesAmount(details, mode, (wallet_points > 0.0 || cashback > 0.0));

        if (cashback != null && cashback.floatValue() == 0) {

            Double payUAmt = Double.valueOf(details.getJSONObject(SdkConstants.PAYMENT).getDouble(SdkConstants.ORDER_AMOUNT) + convenienceChargesAmount - Dis_amt.doubleValue());
            if (wallet_points > 0.0) {
                payUAmt = Double.valueOf(payUAmt.doubleValue() - wallet_points); //substract wallet
                p.put("sourceAmountMap", "{\"PAYU\":" + payUAmt + ",\"DISCOUNT\":" + Dis_amt + ",\"WALLET\":" + wallet_points + "}"); //here wallet_point is wallet money
            } else {
                p.put("sourceAmountMap", "{\"PAYU\":" + payUAmt + ",\"DISCOUNT\":" + Dis_amt + "}");
            }
        } else if (mode.equals(SdkConstants.WALLET)) {//Points+Wallet OR Just Wallet Payments

            if (wallet_points == 0.0) {//Pure wallet

                p.put("sourceAmountMap", "{\"WALLET\":" + cashback + ",\"PAYU\":" + 0 + ",\"DISCOUNT\":" + Dis_amt + "}");
            } else //wallet+points
                p.put("sourceAmountMap", "{\"WALLET\":" + cashback + ",\"CASHBACK\":" + wallet_points + ",\"PAYU\":" + 0 + ",\"DISCOUNT\":" + Dis_amt + "}"); //here wallet_point is PayUPoints (depends on call)

        } else if (mode.equals(SdkConstants.POINTS)) {//Have PayUPoints and has selected to pay via payupoints

            p.put("sourceAmountMap", "{\"CASHBACK\":" + cashback + ",\"PAYU\":" + 0 + ",\"DISCOUNT\":" + Dis_amt + "}");
        } else {//Has PayUPoints but opted not to pay via payupoints

            Double payUAmt = Double.valueOf(details.getJSONObject(SdkConstants.PAYMENT).getDouble(SdkConstants.ORDER_AMOUNT) + convenienceChargesAmount - Dis_amt.doubleValue() - cashback.doubleValue());
            if (wallet_points > 0.0) payUAmt = Double.valueOf(payUAmt - wallet_points);
            p.put("sourceAmountMap", "{\"CASHBACK\":" + cashback + ",\"PAYU\":" + payUAmt + ",\"DISCOUNT\":" + Dis_amt + ",\"WALLET\":" + wallet_points + "}");
        }

        wallet_points = 0.0; //reinitialize to 0 for future use on same instance

        if (mode.equals(SdkConstants.POINTS) || mode.equals(SdkConstants.WALLET))
            p.put("PG", SdkConstants.WALLET_STRING);
        else
            p.put("PG", mode); // Pure points/wallet - > CC, Pure Credit Card -> CC Debit Card -> DC Net banking -> NB

        if (!mode.equals(SdkConstants.POINTS) && !mode.equals(SdkConstants.WALLET)) {
            if (data.containsKey(SdkConstants.BANK_CODE)) {
                p.put(SdkConstants.BANK_CODE_STRING, data.get(SdkConstants.BANK_CODE).toString());
            } else {
                p.put(SdkConstants.BANK_CODE_STRING, mode);
            }
        }

        if (data.containsKey("storeCardId")) {
            p.put("storeCardId", String.valueOf(data.get("storeCardId")));
        }
        p.put("revisedCashbackReceivedStatus", getrevisedCashbackReceivedStatus());
        p.put("isMobile", "1");
        p.put(SdkConstants.CALLING_PLATFORM_NAME, SdkConstants.CALLING_PLATFORM_VALUE);
        //p.put(SdkConstants.APP_VERSION_CODE, ""+SdkConstants.getVersionCode(mContext));

        if (loginMode.equals("guestLogin"))
            p.put("guestCheckout", "true");//Guest Checkout

        SdkLogger.d(SdkConstants.TAG + ":Params -->", p.toString());

        postFetch("/payment/app/customer/getPaymentMerchant/" + paymentID + getParameters(p), null, new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {
                    if (object.has(SdkConstants.MESSAGE) && object.optString(SdkConstants.MESSAGE, SdkConstants.XYZ_STRING).contains(SdkConstants.INVALID_APP_VERSION)) {
                        if (!mode.equals(SdkConstants.POINTS) && !mode.equals(SdkConstants.WALLET))
                            eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT, false, SdkConstants.INVALID_APP_VERSION));
                        else
                            eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, false, SdkConstants.INVALID_APP_VERSION));

                        return;
                    }
                    SdkLogger.d(SdkConstants.TAG + ":Success-->", object.toString());
                    object = object.getJSONObject(SdkConstants.RESULT);
                    JSONObject p = new JSONObject();

                    if (mode.equals(SdkConstants.POINTS) || mode.equals(SdkConstants.WALLET)) {

                        fetchPaymentStatus(paymentID);
                        //fetchPaymentResponse(paymentID);
                        //verifyPaymentDetails(paymentID);

                    } else//For NB CC AND DC
                    {
                        String key = (String) data.get("key");
                        data.remove("key");
                        //  if(!object.getString(Constants.TRANSACTION_DTO).equals("null")) {
                        object = object.getJSONObject(SdkConstants.TRANSACTION_DTO).getJSONObject("hash");
                        //  }
                        if (!mode.equals(SdkConstants.PAYMENT_MODE_NB)) {
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
                        if (data.containsKey(SdkConstants.BANK_CODE)) {
                            p.put(SdkConstants.BANK_CODE, data.get(SdkConstants.BANK_CODE));
                        } else {
                            p.put(SdkConstants.BANK_CODE, mode);
                        }
                        if (!mode.equals(SdkConstants.PAYMENT_MODE_NB)) {
                            if (data.containsKey(SdkConstants.LABEL)) {
                                p.put(SdkConstants.LABEL, data.get(SdkConstants.LABEL));
                            }
                            if (data.containsKey(SdkConstants.STORE)) {
                                p.put(SdkConstants.STORE, data.get(SdkConstants.STORE));
                            }
                            if (data.containsKey("store_card_token")) {
                                p.put("store_card_token", data.get("store_card_token"));
                            }
                        }
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT, true, p));
                    }
                } catch (Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onSuccess(String response) {
                SdkLogger.d(SdkConstants.TAG + ":Success-->", response);
            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT, false, throwable.getMessage()));
                //   Toast.makeText(getApplicationContext(),throwable.toString(),Toast.LENGTH_LONG).show();
                SdkLogger.d(SdkConstants.TAG + ":failure-->", throwable.toString());
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.GET);
    }

    public void fetchPaymentStatus(String paymentID) {

        final Map<String, String> p = new HashMap<>();
        p.put(SdkConstants.PAYMENT_ID, paymentID);
        postFetch("/payment/app/postPayment", p, new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                String status = object.optString(SdkConstants.STATUS);
                if (status == null || status.equals("-1"))
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, false, object));
                else
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, true, object));
            }

            @Override
            public void onSuccess(String response) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, true, response));

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, false, throwable.getMessage()));

            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);

    }

    public void fetchPaymentResponse(String paymentID) {

        final Map<String, String> p = new HashMap<>();
        p.put(SdkConstants.PAYMENT_ID_STRING, paymentID);

        postFetch("/payment/app/payment/verifyPaymentStatus", p, new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                String status = object.optString(SdkConstants.STATUS);
                if (status == null || status.equals("-1"))
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, false, object));
                else
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, true, object));
            }

            @Override
            public void onSuccess(String response) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, true, response));

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, false, throwable.getMessage()));

            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);

    }

    public void verifyPaymentDetails(String paymentID) {

        final Map<String, String> p = new HashMap<>();
        p.put(SdkConstants.PAYMENT_ID_STRING, paymentID);

        postFetch("/payment/app/payment/verifyPaymentDetails", p, new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                String status = object.optString(SdkConstants.STATUS);
                if (status == null || status.equals("-1"))
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, false, object));
                else
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, true, object));
            }

            @Override
            public void onSuccess(String response) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, true, response));

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, false, throwable.getMessage()));

            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);

    }

    /**
     * set One click transactions
     */
    public void enableOneClickTransaction(String enable) {

        final Map<String, String> p = new HashMap<>();

        SharedPreferences mPref = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);

        if (mPref != null) {
            p.put("oneClickTxn", enable);
            postFetch("/auth/app/setUserPaymentOption", p, new Task() {
                @Override
                public void onSuccess(JSONObject object) {
                    try {
                        JSONObject result = object.getJSONObject("result");
                        if (result != null)
                            eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.ONE_TAP_OPTION_ALTERED, true, result));
                        else
                            eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.ONE_TAP_OPTION_ALTERED, false));

                    } catch (JSONException e) {
                        e.printStackTrace();
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.ONE_TAP_OPTION_ALTERED, false));
                    }
                }

                @Override
                public void onSuccess(String response) {

                }

                @Override
                public void onError(Throwable throwable) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.ONE_TAP_OPTION_ALTERED, false));
                }

                @Override
                public void onProgress(int percent) {

                }
            }, Request.Method.POST);

        }

    }

    public void verifyManualCoupon(String manualCoupon, String paymentId, String device_id, String mobileStatus) {

        final Map<String, String> p = new HashMap<>();
        p.put("userCouponString", manualCoupon);
        p.put("visitId", paymentId);
        p.put("reqId", device_id);
        p.put("mobileStatus", mobileStatus);
        postFetch("/payment/app/validateUserCouponString", p, new Task() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.has(SdkConstants.RESULT) && !jsonObject.isNull(SdkConstants.RESULT))
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.VERIFY_MANUAL_COUPON, true, jsonObject.getJSONObject(SdkConstants.RESULT)));
                    else
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.VERIFY_MANUAL_COUPON, false));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(String response) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.VERIFY_MANUAL_COUPON, false));
            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.VERIFY_MANUAL_COUPON, false, null));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);
    }

    public void getUserVaults() {

        final Map p = new HashMap<>();

        p.put(SdkConstants.HASH, getHashForThisCall(SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.PHONE),
                SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.EMAIL), null, null, null));

        postFetch("/auth/ext/wallet/getWalletLimit", p, new Task() {
            //Override Task interface
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.get(SdkConstants.STATUS) == null || jsonObject.getString(SdkConstants.STATUS).equals(SdkConstants.NULL_STRING)) {
                        // No response from server :/ -- Internet off/Server Down/Device internal issue
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.USER_VAULT, false, jsonObject.getString(SdkConstants.MESSAGE)));
                    } else {

                        if (jsonObject.getJSONObject(SdkConstants.RESULT).has(SdkConstants.AVAILABLE_BALANCE)) {
                            double walletBalance = jsonObject.getJSONObject(SdkConstants.RESULT).optDouble(SdkConstants.AVAILABLE_BALANCE, 0.0);
                            SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.WALLET_BALANCE);
                            SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.WALLET_BALANCE, walletBalance + "");
                        }

                        if (jsonObject.getJSONObject(SdkConstants.RESULT).has(SdkConstants.MAX_LIMIT)) {
                            double maxWalletBalance = jsonObject.getJSONObject(SdkConstants.RESULT).optDouble(SdkConstants.MAX_LIMIT, 0.0);
                            SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.MAX_WALLET_BALANCE);
                            SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.MAX_WALLET_BALANCE, maxWalletBalance + "");
                        }

                        if (jsonObject.getJSONObject(SdkConstants.RESULT).has(SdkConstants.MIN_LIMIT)) {
                            double minWalletBalance = jsonObject.getJSONObject(SdkConstants.RESULT).optDouble(SdkConstants.MIN_LIMIT, 0.0);
                            SharedPrefsUtils.removePreferenceByKey(mContext, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE);
                            SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE, minWalletBalance + "");
                        }

                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.USER_VAULT, true));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.USER_VAULT, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {

                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.USER_VAULT, false, "An error occurred while verifying your OTP. Please generate again."));
            }

            @Override
            public void onProgress(int percent) {

            }

        }, Request.Method.POST);

    }

    public static String hashCal(String str) {
        byte[] hashseq = str.getBytes();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("SHA-512");
            algorithm.reset();
            algorithm.update(hashseq);
            byte messageDigest[] = algorithm.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
        } catch (NoSuchAlgorithmException nsae) {
        }
        return hexString.toString();
    }

    public String getHashForThisCall(String mobile, String email, String amount, String merchantTxnId, String productInfo) {

        String hashSequence = merchantKey;

        if (null != mobile)
            hashSequence += "|" + mobile;
        if (null != email)
            hashSequence += "|" + email;
        if (null != amount)
            hashSequence += "|" + amount;

        if (null != productInfo && !SdkConstants.PRODUCT_INFO.equals(productInfo))
            hashSequence += "|" + productInfo;
        else if (null != productInfo)
            hashSequence += "|";

        if (null != merchantTxnId)
            hashSequence += "|" + merchantTxnId;

        hashSequence += "|" + merchantSalt;

        return hashCal(hashSequence);
    }

    public void getTransactionHistory(int offset) {

        final Map p = new HashMap<>();
        int walletLimit = 12;

        p.put(SdkConstants.WALLET_HISTORY_PARAM_OFFSET, offset);
        p.put(SdkConstants.WALLET_HISTORY_PARAM_LIMIT, walletLimit);
        p.put(SdkConstants.KEY, merchantKey);
        p.put(SdkConstants.HASH, getHashForThisCall(offset + "", walletLimit + "", null, null, null));

        postFetch("/vault/ext/getVaultTransactionDetails" + getParameters(p), null, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    int status = jsonObject.getInt(SdkConstants.STATUS);
                    if (status < 0) {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.USER_HISTORY, false, jsonObject.getString(SdkConstants.MESSAGE)));
                    } else {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.USER_HISTORY, true, jsonObject.getJSONObject(SdkConstants.RESULT)));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.USER_HISTORY, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.USER_HISTORY, false, "An error occurred while trying to sign you up. Please try again later."));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.GET);
    }

    public void verifyUserCredential(final String email, final String mobileNo, String otp) {

        final Map p = new HashMap<>();
        p.put(SdkConstants.OTP_STRING, otp);
        p.put(SdkConstants.EMAIL, email);
        p.put(SdkConstants.MOBILE, mobileNo);

        p.put(SdkConstants.HASH, getHashForThisCall(mobileNo, email, null, null, null));

        postFetch("/auth/ext/wallet/verify", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    int status = jsonObject.getInt(SdkConstants.STATUS);
                    if (status < 0) {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.OPEN_SEND_OTP_VERIFICATION, false, jsonObject.getString(SdkConstants.MESSAGE)));
                    } else {
                        SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.ACCESS_TOKEN,
                                jsonObject.getJSONObject(SdkConstants.RESULT).getJSONObject(SdkConstants.BODY).getString(SdkConstants.ACCESS_TOKEN));
                        SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.REFRESH_TOKEN,
                                jsonObject.getJSONObject(SdkConstants.RESULT).getJSONObject(SdkConstants.BODY).getString(SdkConstants.REFRESH_TOKEN));

                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.OPEN_SEND_OTP_VERIFICATION, true, jsonObject.getString(SdkConstants.MESSAGE)));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.OPEN_SEND_OTP_VERIFICATION, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.OPEN_SEND_OTP_VERIFICATION, false, "An error occurred while trying to sign you up. Please try again later."));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);
    }

    public void sendMobileVerificationCode(String email, String mobileNo) {
        final Map p = new HashMap<>();
        p.put(SdkConstants.EMAIL, email);
        p.put(SdkConstants.MOBILE, mobileNo);

        p.put(SdkConstants.HASH, getHashForThisCall(mobileNo, email, null, null, null));

        SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.EMAIL, email);
        SharedPrefsUtils.setStringPreference(mContext, SharedPrefsUtils.Keys.PHONE, mobileNo);

        postFetch("/auth/ext/wallet/register", p, new Task() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    int status = jsonObject.getInt(SdkConstants.STATUS);
                    if (status < 0) {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.OPEN_REGISTER_USING_OTP_AND_SIGNIN, false, jsonObject.getString(SdkConstants.MESSAGE)));
                    } else {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.OPEN_REGISTER_USING_OTP_AND_SIGNIN, true, jsonObject.getString(SdkConstants.MESSAGE)));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.OPEN_REGISTER_USING_OTP_AND_SIGNIN, false));
                }
            }

            @Override
            public void onSuccess(String response) {

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.OPEN_REGISTER_USING_OTP_AND_SIGNIN, false, "An error occurred while trying to sign you up. Please try again later."));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);
    }

    public void loadWallet(String otp, String amount, String paymentId, String productInfo) {

        final Map<String, String> p = new HashMap<>();
        p.put(SdkConstants.TOTAL_AMOUNT, amount);
        if (otp != null) {
            p.put("otp", otp);
        }
        p.put(SdkConstants.PAYMENT_IDENTIFIERS_STRING, "[]");
        p.put(SdkConstants.PAYMENT_PARTS_STRING, "[]");
        p.put(SdkConstants.PRODUCT_INFO, productInfo);
        p.put("paymentDescription", "loadWallet");
        p.put("sourceReferenceId", paymentId);
        p.put("isMobile", "1");
        p.put(SdkConstants.DEVICE_ID, SdkHelper.getAndroidID(mContext));
        /* deliberatley set to higher value to skip the version check*/
        p.put(SdkConstants.APP_VERSION_CODE, "5000");
        postFetch("/payment/app/wallet/loadWalletPayment", p, new Task() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    if (jsonObject.has(SdkConstants.RESULT) && !jsonObject.isNull(SdkConstants.RESULT))
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, true, jsonObject.getJSONObject(SdkConstants.RESULT)));
                    else if (jsonObject.has(SdkConstants.MESSAGE) && !jsonObject.isNull(SdkConstants.MESSAGE)) {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, false, jsonObject.getString(SdkConstants.MESSAGE)));
                    } else
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, false));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(String response) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, false));
            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, false, null));
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.POST);
    }

    public void loadWallet(HashMap<String, String> params, String amt_net) {

        Map p = new HashMap<>();
        p.put(SdkConstants.KEY, params.get(SdkConstants.KEY));
        p.put(SdkConstants.TRANSACTION_DETAILS, params.get(SdkConstants.TRANSACTION_DETAILS));
        p.put(SdkConstants.TOTAL_AMOUNT, amt_net);
        p.put(SdkConstants.TRANSACTION_DETAILS, "{\"surl\": \"" + params.get(SdkConstants.SURL) + "\", \"furl\": \"" + params.get(SdkConstants.FURL) + "\", \"email\": \"" + SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.EMAIL) + "\"}");
        p.put(SdkConstants.HASH, getHashForThisCall(null, null, amt_net, null, params.get(SdkConstants.PRODUCT_INFO)));


        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {

                    if (object.has(SdkConstants.RESULT) && !object.isNull(SdkConstants.RESULT)) {

                        JSONObject result = object.getJSONObject(SdkConstants.RESULT);

                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, true, result));
                    } else {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, false));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, false));
                }
            }

            @Override
            public void onSuccess(String response) {
                /*String Response for this call Signifies something went wrong*/
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, false));

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOAD_WALLET, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/payment/app/wallet/loadWalletPayment", p, task, Request.Method.POST);
    }

    public void debitFromWallet(HashMap<String, String> params) {

        Map p = new HashMap<>();
        p.put(SdkConstants.KEY, params.get(SdkConstants.KEY));
        p.put(SdkConstants.TOTAL_AMOUNT, params.get(SdkConstants.AMOUNT));
        p.put(SdkConstants.MERCHANT_TXNID, params.get(SdkConstants.MERCHANT_TXNID));
        p.put(SdkConstants.HASH, getHashForThisCall(null, null, params.get(SdkConstants.AMOUNT) + "", merchantTxnId, params.get(SdkConstants.PRODUCT_INFO)));
        p.put(SdkConstants.DEVICE_ID, SdkHelper.getAndroidID(mContext));

        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {

                    if (object.has(SdkConstants.RESULT) && !object.isNull(SdkConstants.RESULT)) {

                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.DEBIT_WALLET, true, object.getString(SdkConstants.RESULT)));
                    } else {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.DEBIT_WALLET, false, object.getString(SdkConstants.MESSAGE)));
                    }
                } catch (JSONException e) {
                    eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.DEBIT_WALLET, false));
                }
            }

            @Override
            public void onSuccess(String response) {
                /*String Response for this call Signifies something went wrong*/
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.DEBIT_WALLET, false));

            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.DEBIT_WALLET, false));
            }

            @Override
            public void onProgress(int percent) {

            }
        };
        postFetch("/payment/ext/wallet/useWallet", p, task, Request.Method.POST);
    }


    public String getToken() {

        if (SdkConstants.WALLET_SDK) {
            return SharedPrefsUtils.getStringPreference(mContext, SharedPrefsUtils.Keys.ACCESS_TOKEN);
        }
        return mSessionData.getToken();
    }

    public void setrevisedCashbackReceivedStatus(String s) {
        mSessionData.setrevisedCashbackReceivedStatus(s);
    }

    public String getrevisedCashbackReceivedStatus() {
        return mSessionData.revisedCashbackReceivedStatus;
    }

    public static float round(double d) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

}
