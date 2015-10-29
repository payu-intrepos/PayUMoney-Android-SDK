package com.payUMoney.sdk;


import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.payUMoney.sdk.entity.SdkUser;
import com.payUMoney.sdk.utils.SdkLogger;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.KeyFactory;
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
public class SdkSession extends Activity {

    private static Activity merchantContext = null;
    public double wallet_points = 0.0;



    public static enum PaymentMode {
        CC, DC, NB, EMI, PAYU_MONEY, STORED_CARDS, CASH
    }

    public static final int PAYMENT_SUCCESS = 3;

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

    public String getLoginMode() {
        return loginMode;
    }

    public void setLoginMode(String loginMode) {
        this.loginMode = loginMode;
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
       // mHttpClient = new AsyncHttpClient();
        //mHttpClient.setTimeout(30000);

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getString(SdkConstants.TOKEN, null) != null) {
            mSessionData.setToken(sharedPreferences.getString(SdkConstants.TOKEN, null));
        }

        if (sharedPreferences.getString(SdkConstants.EMAIL, null) != null) {
            SdkUser sdkUser = new SdkUser();
            sdkUser.setEmail(sharedPreferences.getString(SdkConstants.EMAIL, null));
            mSessionData.setSdkUser(sdkUser);
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

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
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
        /*SDK specific,Handling logout from SDK in case of Merchant's App with SDK*/
        if(merchantContext == null)
        merchantContext = mActivity;
        Intent intent = new Intent(merchantContext, SdkHomeActivityNew.class);

        //Intent intent = new Intent(mActivity, SdkHomeActivityNew.class);
        if(!userParams.containsKey("PayUMoneyApp")) {
            //check for all compulsory params
            if (userParams.get("key").equals("") || userParams.get("key") == null)
                throw new RuntimeException("Merchant Key missing");
            else
                intent.putExtra(SdkConstants.KEY, userParams.get("key"));
            if (userParams.get("hash").equals("") || userParams.get("hash") == null)
                throw new RuntimeException("Hash is  missing");
            else
                intent.putExtra(SdkConstants.HASH, userParams.get("hash"));
            if (userParams.get("TxnId").equals("") || userParams.get("TxnId") == null)
                throw new RuntimeException("TxnId Id missing");
            else
                intent.putExtra(SdkConstants.TXNID, userParams.get("TxnId"));

            if (userParams.get("Amount").equals("") || userParams.get("Amount") == null)
                throw new RuntimeException("Amount is missing");
            /*else if(Double.parseDouble(userParams.get("Amount")) > 1000000.00)
                throw new RuntimeException("Invalid Amount");*/
            else
                intent.putExtra(SdkConstants.AMOUNT, userParams.get("Amount"));
            if (userParams.get("SURL").equals("") || userParams.get("SURL") == null)
                throw new RuntimeException("Surl is missing");
            else
                intent.putExtra(SdkConstants.SURL, userParams.get("SURL"));
            if (userParams.get("FURL").equals("") || userParams.get("FURL") == null)
                throw new RuntimeException("Furl is missing");
            else
                intent.putExtra(SdkConstants.FURL, userParams.get("FURL"));
            if (userParams.get("ProductInfo").equals("") || userParams.get("ProductInfo") == null)
                throw new RuntimeException("Product info is missing");
            else
                intent.putExtra(SdkConstants.PRODUCT_INFO, userParams.get("ProductInfo"));
            if (userParams.get("firstName").equals("") || userParams.get("firstName") == null)
                throw new RuntimeException("Firstname is missing");
            else
                intent.putExtra(SdkConstants.FIRSTNAME, userParams.get("firstName"));
            if (userParams.get("Email").equals("") || userParams.get("Email") == null)
                throw new RuntimeException("Email is missing");
            else
                intent.putExtra(SdkConstants.USER_EMAIL, userParams.get("Email"));
            if (userParams.get("Phone").equals("") || userParams.get("Phone") == null)
                throw new RuntimeException("Phone is missing");
            else
                intent.putExtra(SdkConstants.USER_PHONE, userParams.get("Phone"));
        }

        intent.putExtra(SdkConstants.PARAMS, userParams);

        //Step 2
        merchantContext.startActivityForResult(intent, PAYMENT_SUCCESS);
        //mActivity.startActivityForResult(intent, PAYMENT_SUCCESS);//Start the Home Activity


    }


    public void fetchMechantParams(String merchantId) {

        /*allowGuestCheckout can have three values: guestcheckout, guestcheckoutonly, quickGuestCheckout
        quickLogin will have 0 or 1*/
        final Map<String,String> p = new HashMap<>();
        p.put("merchantId", merchantId);
        String uri = String.format("/auth/app/op/merchant/LoginParams"+getParameters(p),
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

    public void fetchUserParams(String paymentId) {

        final Map<String, String> p = new HashMap<>();
        p.put("paymentId", paymentId);
        postFetch("/payment/app/fetchPaymentUserData", p, new Task() {

            @Override
            public void onSuccess(JSONObject jsonObject) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.FETCH_USER_PARAMS, true, jsonObject));

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

    public void postFetch(final String url, final Map<String,String> params, final Task task, final int method) {
        if (SdkConstants.DEBUG.booleanValue()) {
            SdkLogger.d(SdkConstants.TAG, "SdkSession.postFetch: " + url + " " + params + " " + method);
        }

        StringRequest myRequest = new StringRequest(method, getAbsoluteUrl(url), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                diff = Long.valueOf(System.currentTimeMillis() - start.longValue());

                SdkLogger.i("Difference ", "URL=" + url + "Time=" + diff);

                if (SdkConstants.DEBUG.booleanValue()) {
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
                if (SdkConstants.DEBUG) {
                    Log.e(SdkConstants.TAG, "Session...new JsonHttpResponseHandler() {...}.onFailure: " + e.getMessage() + " " + msg);
                }
                if (msg.contains("401")) {
                    logout("force");
                }
                runErrorOnHandlerThread(task, e);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (SdkConstants.DEBUG) {
                    Log.e(SdkConstants.TAG, "Session...new JsonHttpResponseHandler() {...}.onFailure: " + error.getMessage());
                }
                if (error != null && error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    logout("force");
                }
                runErrorOnHandlerThread(task, error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("User-Agent", "PayUMoneyAPP");
                if (mSessionData.getToken() != null) {
                    params.put("Authorization", "Bearer " + mSessionData.getToken());
                } else {
                    params.put("Accept", "*/*;");
                }
                return params;
            }

            @Override
            public String getBodyContentType() {
                if (mSessionData.getToken() == null) {
                    return "application/x-www-form-urlencoded";
                } else {
                    return super.getBodyContentType();
                }
            }
        };
        myRequest.setShouldCache(false);
        myRequest.setRetryPolicy(new DefaultRetryPolicy(
                8000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        addToRequestQueue(myRequest, TAG);
        start = Long.valueOf(System.currentTimeMillis());

    }

    private String getParameters(Map<String,String> params) {
        String parameters = "?";
        Iterator it = params.entrySet().iterator();
        boolean isFirst = true;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(isFirst) {
                parameters = parameters.concat(pair.getKey()+"="+pair.getValue());
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

    public SdkUser getUser() {
        return mSessionData.getSdkUser();
    }

    public void setToken(String token) {
        mSessionData.setToken(token);
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

        final Map<String,String> p = new HashMap<>();
        p.put("grant_type", "password");
        p.put("client_id", "180551"); // Always Merchant KEY
        // p.put("client_id", "10182");
        p.put("username", username);
        p.put("password", password);  //Password --> Device Master Token , OTP , Password

        postFetch("/auth/oauth/token", p, new Task() {
            @Override
            public void onSuccess(final JSONObject jsonObject) {
                try {
//                    mSessionData = new SessionData();
                    if (jsonObject.has(SdkConstants.ACCESS_TOKEN) && !jsonObject.isNull(SdkConstants.ACCESS_TOKEN)) {
                        mSessionData.setToken(jsonObject.getString(SdkConstants.ACCESS_TOKEN)); //Set the token received
                        final SdkUser sdkUser = new SdkUser();
                        sdkUser.setEmail(username);
                        mSessionData.setSdkUser(sdkUser);
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGIN, true, jsonObject));
                    }
                    else{

                        SdkLogger.d(SdkConstants.TAG,"Token Not Found");
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGIN, false,"Error"));
                    }
                }catch (Throwable e) {
                    if (SdkConstants.DEBUG.booleanValue()) {
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

        final Map<String,String> p = new HashMap<>();
        p.put("requestType", "login");
        p.put("userName", userEmail);
        postFetch("/auth/app/op/generateAndSendOTP", p, new Task() {

            @Override
            public void onSuccess(JSONObject object) {
                // JSONObject jsonObject = new JSONObject(object);
                // Toast.makeText(SdkSession.getInstance(getApplicationContext()), object.getString("message"), Toast.LENGTH_LONG).show();
                //return result=object.getString("message");
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.GENERATE_AND_SEND_OTP, true, object));

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
        final Map<String,String> p = new HashMap<>();
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
        p.put("deviceId", params.get("deviceId"));
       // p.put("appVersion", params.get("appVersion"));
        p.put("isMobile", "1");
        if (loginMode.equals("guestLogin"))
            p.put("guestCheckout", "true");
        Task task = new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {

                    if(object.has(SdkConstants.RESULT) && !object.isNull(SdkConstants.RESULT)) {

                        JSONObject result = object.getJSONObject(SdkConstants.RESULT);

                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_PAYMENT, true, result /*object.getJSONObject(Constants.RESULT).getString(Constants.PAYMENT_ID)*/));
                    }
                    else{
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.CREATE_PAYMENT, false));
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

    public void updateTransactionDetails() {

        final Map<String,String> p = new HashMap<>();
       // p.put("guest")
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
        final Map<String,String> p = new HashMap<>();
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

    /**
     * Create a credit {@link com.payUMoney.sdk.entity.SdkCard} on the server
     * <p/>
     * <ul>
     * <li>A {@link com.payu.payumoney.CobbocEvent} is dispatched</li>
     * </ul>
     *
     * @param number      the number
     * @param expiryMonth the 2 digit month of mCardExpiry
     * @param expiryYear  the 4 digit year of mCardExpiry
     */

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
        final Map<String,String> p = new HashMap<>();
        p.put("userType", "customer");
        p.put("username", email);
        p.put("phone", phone);
        p.put("source", "payumoney app");
        p.put("password", password);
        p.put("pageSource", "sign up");
        p.put("name", email);
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
        Map<String,String> p = new HashMap<String,String>();
        p.put("_method", "delete");
        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.LOGOUT, false, message));
        if (message != null && !message.equals("force"))
            mSessionData.reset();
    }

    public void logout() {
        logout(null);
    }

    public void notifyUserCancelledTransaction(String paymentId , String userCancelled) {

        final Map<String,String> p = new HashMap<>();
        p.put(SdkConstants.PAYMENT_ID, paymentId);
        p.put(SdkConstants.USER_CANCELLED_TRANSACTION, userCancelled);


        postFetch("/payment/postBackParam.do"+getParameters(p), null, new Task() {
            @Override
            public void onSuccess(final JSONObject jsonObject) {

                    if (SdkConstants.DEBUG.booleanValue()) {
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
    public void sendToPayUWithWallet(JSONObject details, final String mode, final HashMap<String, Object> data, Double cashback, Double vault,Double discount) throws JSONException {
        wallet_points = vault.doubleValue(); //Set points or wallet depending on the call
        sendToPayU(details, mode, data, cashback,discount); //cashback is point/wallet to be payed depending on the call

    }

    /*
       Send to payU with Wallet
       params: Details  Json --> fetch all payment-Merchant details from here
       params: mode - NB CC DC wallet
       params: Hashmap data - bankobject key etc.
       params: cashback is wallet or points
        */
    public void sendToPayU(JSONObject details, final String mode, final HashMap<String, Object> data, Double cashback1,Double discount) throws JSONException {
        Float cashback = Float.valueOf(round(cashback1.doubleValue(), 2));
        // first get the transaction keys
        final Map<String,String> p = new HashMap<>();
        Double Dis_amt;

        if (SdkHomeActivityNew.coupan_amt != 0) {
            p.put("couponUsed", SdkHomeActivityNew.choosedCoupan);
            Dis_amt = Double.valueOf(SdkHomeActivityNew.coupan_amt);
        } else {
            //DecimalFormat format = new DecimalFormat("0.#");
            Dis_amt = discount;
        }


        /*no payupoints
          * this will be fired in most cases*/
        if (cashback != null && cashback.floatValue() == 0) {
            Double payUAmt = Double.valueOf(details.getJSONObject(SdkConstants.PAYMENT).getDouble("orderAmount") + new JSONObject(details.getString("convenienceCharges")).getJSONObject(mode).getDouble("DEFAULT") - Dis_amt.doubleValue());
            if (wallet_points > 0.0) {
                payUAmt = Double.valueOf(payUAmt.doubleValue() - wallet_points); //substract wallet
                p.put("sourceAmountMap", "{\"PAYU\":" + payUAmt + ",\"DISCOUNT\":" + Dis_amt + ",\"WALLET\":" + wallet_points + "}"); //here wallet_point is wallet money
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
            Double payUAmt = Double.valueOf(details.getJSONObject(SdkConstants.PAYMENT).getDouble("orderAmount") + new JSONObject(details.getString("convenienceCharges")).getJSONObject(mode).getDouble("DEFAULT") - Dis_amt.doubleValue() - cashback.doubleValue());
            if (wallet_points > 0.0) payUAmt = Double.valueOf(payUAmt - wallet_points);
            p.put("sourceAmountMap", "{\"CASHBACK\":" + cashback + ",\"PAYU\":" + payUAmt + ",\"DISCOUNT\":" + Dis_amt + ",\"WALLET\":" + wallet_points + "}");
        }

        wallet_points = 0.0; //reinitialize to 0 for future use on same instance

        if (mode.equals("points") || mode.equals("wallet"))
            p.put("PG", "WALLET");
        else
            p.put("PG", mode); // Pure points/wallet - > CC, Pure Credit Card -> CC Debit Card -> DC Net banking -> NB

        if (!mode.equals("points") && !mode.equals("wallet")) {
            if (data.containsKey("bankcode")) {
                p.put("bankCode", data.get("bankcode").toString());
            } else {
                p.put("bankCode", mode);
            }
        }

        if (data.containsKey("storeCardId")) {
            p.put("storeCardId", String.valueOf(data.get("storeCardId")));
        }
        p.put("revisedCashbackReceivedStatus", getrevisedCashbackReceivedStatus());

        if (loginMode.equals("guestLogin"))
            p.put("guestCheckout", "true");//Guest Checkout

        SdkLogger.d(SdkConstants.TAG +":Params -->",p.toString());

        postFetch("/payment/app/customer/getPaymentMerchant/" + details.getJSONObject(SdkConstants.PAYMENT).getString(SdkConstants.PAYMENT_ID)+getParameters(p), null, new Task() {
            @Override
            public void onSuccess(JSONObject object) {
                try {

                    SdkLogger.d(SdkConstants.TAG +":Success-->", object.toString());
                    object = object.getJSONObject(SdkConstants.RESULT);
                    JSONObject p = new JSONObject();

                    if (!mode.equals("points") && !mode.equals("wallet")) //For NB CC AND DC
                    {
                        String key = (String) data.get("key");
                        data.remove("key");
                        //  if(!object.getString(Constants.TRANSACTION_DTO).equals("null")) {
                        object = object.getJSONObject(SdkConstants.TRANSACTION_DTO).getJSONObject("hash");
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
                    } else //For wallet and PayuPoints
                    {
                        eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT_POINTS, true, object));
                    }


                } catch (Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onSuccess(String response) {
                SdkLogger.d(SdkConstants.TAG +":Success-->",response);
            }

            @Override
            public void onError(Throwable throwable) {
                eventBus.post(new SdkCobbocEvent(SdkCobbocEvent.PAYMENT, false, throwable.getMessage()));
                //   Toast.makeText(getApplicationContext(),throwable.toString(),Toast.LENGTH_LONG).show();
                SdkLogger.d(SdkConstants.TAG +":failure-->",throwable.toString());
            }

            @Override
            public void onProgress(int percent) {

            }
        }, Request.Method.GET);
    }

    public String getToken() {
        return mSessionData.getToken();
    }

    public void setrevisedCashbackReceivedStatus(String s) {
        mSessionData.setrevisedCashbackReceivedStatus(s);
    }

    public String getrevisedCashbackReceivedStatus() {
        return mSessionData.revisedCashbackReceivedStatus;
    }

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
        private String token = null;
        private SdkUser sdkUser = null;
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

        public SdkUser getSdkUser() {
            return sdkUser;
        }

        public void setSdkUser(SdkUser sdkUser) {
            this.sdkUser = sdkUser;
        }

        public void reset() {
            token = null;
            sdkUser = null;
        }
    }

    public static float round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

}
