package com.payUMoney.sdk.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.payUMoney.sdk.PayUmoneySdkInitilizer;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkHomeActivityNew;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.SdkSetupCardDetails;
import com.payUMoney.sdk.adapter.SdkStoredCardAdapter;
import com.payUMoney.sdk.entity.SdkIssuer;
import com.payUMoney.sdk.utils.SdkHelper;
import com.payUMoney.sdk.utils.SdkLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 */

public class SdkStoredCardFragment extends View {


    String mode = null;
    ListView listView = null;
    Context mContext;
    private String cardCvvHash;
    MakePaymentListener mCallback;
    private String encryptedUserId, userToken, authorizationSalt;
    private String device_id;
    private RequestQueue mRequestQueue;
    private final Handler handler;
    private SdkStoredCardAdapter adapter;
    private int selectedCardPosition = -1;
    private JSONObject selectedCard;
    private ProgressDialog mProgressDialog = null;
    private JSONObject userConfigDto = null;

    public SdkStoredCardFragment(Context context) {
        super(context);
        mContext = context;
        mCallback = (MakePaymentListener) context;
        handler = new Handler(Looper.getMainLooper());
    }

    public RequestQueue getRequestQueue(Context context) {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context/*, new ProxyHurlStack()*/);
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue(mContext).add(req);
    }

    public interface MakePaymentListener {
        void setCardHashForOneClickTxn(String b);

        void goToPayment(String mode, HashMap<String, Object> data) throws JSONException;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container) {

        SdkLogger.d(SdkConstants.TAG, "StoredCardFragment" + "onCreateView");
        View storedCardFragment = inflater.inflate(R.layout.sdk_fragment_stored_card, container, false);
        listView = (ListView) (storedCardFragment.findViewById(R.id.storedCardListView));  //Initialize ListView
        mContext = container.getContext();
        JSONArray arr = ((SdkHomeActivityNew) mContext).getStoredCardList();
        adapter = new SdkStoredCardAdapter(mContext, arr); //Initialize the adapter
        listView.setAdapter(adapter);
        listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        listView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // disallow the onTouch for your scrollable parent view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        if (adapter.getCount() > 1) {
            int totalHeight = listView.getPaddingTop() + listView.getPaddingBottom();
            for (int i = 0; i < 2; i++) {
                View listItem = adapter.getView(i, null, listView);
                if (listItem instanceof ViewGroup) {
                    listItem.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight + (listView.getDividerHeight());
            listView.setLayoutParams(params);

        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                mProgressDialog = ((SdkHomeActivityNew) mContext).showProgress(mContext);
                selectedCard = (JSONObject) adapterView.getAdapter().getItem(i);
                selectedCardPosition = i;
                try {
                    SharedPreferences mPref = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
                    if (mPref.contains(SdkConstants.ONE_TAP_FEATURE) && mPref.getBoolean(SdkConstants.ONE_TAP_FEATURE,false) && mPref.contains(SdkConstants.CONFIG_DTO))
                        userConfigDto = new JSONObject(mPref.getString(SdkConstants.CONFIG_DTO, SdkConstants.XYZ_STRING));

                    mode = selectedCard.getString("pg");
                    ((SdkHomeActivityNew) mContext).updateDetails(mode);

                    if (userConfigDto != null && userConfigDto.has(SdkConstants.ONE_CLICK_PAYMENT)
                            && userConfigDto.optBoolean(SdkConstants.ONE_CLICK_PAYMENT, false)
                            && selectedCard.has(SdkConstants.ONE_CLICK_CHECK_OUT) && !selectedCard.isNull(SdkConstants.ONE_CLICK_CHECK_OUT)
                            && selectedCard.optBoolean(SdkConstants.ONE_CLICK_CHECK_OUT, false)
                            && selectedCard.has(SdkConstants.CARD_TOKEN) && !selectedCard.isNull(SdkConstants.CARD_TOKEN)) {
                            /*One Click is true Henc ConfigDto must not be null*/
                        calculateCardHash(selectedCard.getString(SdkConstants.CARD_TOKEN), userConfigDto);
                    }
                    else{
                        askForCvvDialog(adapter, selectedCardPosition);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        return storedCardFragment;
    }

    private void askForCvvDialog(SdkStoredCardAdapter adapter, int i) {

        dismissProgress();
        adapter.setSelectedCard(i);
        adapter.notifyDataSetInvalidated();
    }

    private void calculateCardHash(String cardToken, JSONObject userConfigDto) {

        //JSONObject userConfigDto = SdkSession.getInstance(mContext).getUserConfigDto();
        try {
            if (userConfigDto != null && userConfigDto.has(SdkConstants.AUTHORIZATION_SALT) && !userConfigDto.isNull(SdkConstants.AUTHORIZATION_SALT))
                authorizationSalt = userConfigDto.getString(SdkConstants.AUTHORIZATION_SALT);

            if (userConfigDto != null && userConfigDto.has(SdkConstants.USER_TOKEN) && !userConfigDto.isNull(SdkConstants.USER_TOKEN))
                userToken = userConfigDto.getString(SdkConstants.USER_TOKEN);

            if (userConfigDto != null && userConfigDto.has("userId") && !userConfigDto.isNull("userId"))
                encryptedUserId = userConfigDto.getString("userId");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String userId = ((SdkHomeActivityNew) mContext).getUserId();
        String hashSequence = userToken + "|" + userId + "|" + cardToken + "|" + authorizationSalt;
        String hash = hashCal(hashSequence);
        String paymentId = ((SdkHomeActivityNew) mContext).getPaymentId();
        String deviceId = getAndroidID(mContext);
        if (!(hash.isEmpty() || paymentId.isEmpty() || deviceId.isEmpty())) {
            final Map<String, String> p = new HashMap<>();
            p.put("hash", hash);
            p.put("uid", encryptedUserId);
            p.put("pid", paymentId);
            p.put("did", deviceId);

            postFetch(PayUmoneySdkInitilizer.IsDebugMode()? SdkConstants.KVAULT_TEST_URL : SdkConstants.KVAULT_PROD_URL, p, Request.Method.POST);
        }

    }

    public String getAndroidID(Context context) {

        if (context == null)
            return "";
        device_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return device_id;
    }

    public static String hashCal(String str) {
        byte[] hashseq = str.getBytes();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("SHA-256");
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

    private void proceedToPay(JSONObject jsonObject) {
        try {


            //String tempcard

            final HashMap<String, Object> data = new HashMap<>();
            data.put("storeCardId", jsonObject.getString("cardId"));
            data.put("store_card_token", jsonObject.getString(SdkConstants.CARD_TOKEN));
            data.put(SdkConstants.LABEL, jsonObject.getString("cardName"));
            data.put(SdkConstants.NUMBER, "");
            /*data.put(SdkConstants.CARD_CVV_MERCHANT, jsonObject.getString("cvvToken"));*/

            /*if (jsonObject.getString("cardType").equals(SdkConstants.PAYMENT_MODE_CC))
                mode = SdkConstants.PAYMENT_MODE_CC;
            else
                mode = SdkConstants.PAYMENT_MODE_DC;
*/
            data.put("key", ((SdkHomeActivityNew) mContext).getPublicKey());
            data.put(SdkConstants.BANK_CODE, SdkSetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode));

            if (!SdkHelper.checkNetwork(mContext)) {
                Toast.makeText(mContext, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
            } else {
                //  Toast.makeText(mContext,String.valueOf(position),Toast.LENGTH_LONG).show();
                // data.put(SdkConstants.CVV, cvv.getText().toString());
                data.put(SdkConstants.EXPIRY_MONTH, "");
                data.put(SdkConstants.EXPIRY_YEAR, "");
                /*if (card_store_check) {
                    data.put(SdkConstants.STORE_CARD_WITH_CVV, "1");
                }*/
                dismissProgress();
                mCallback.setCardHashForOneClickTxn(cardCvvHash);
                mCallback.goToPayment(mode, data);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static SdkIssuer getIssuer(String mNumber, String cardMode) {

        String tempIssuer = SdkSetupCardDetails.findIssuer(mNumber, cardMode);

        switch (tempIssuer) {
            case "AMEX":
                return SdkIssuer.AMEX;

            case "DINR":
                return SdkIssuer.DINER;

            case "JCB":
                return SdkIssuer.JCB;

            case "LASER":
                return SdkIssuer.LASER;

            case "VISA":
                return SdkIssuer.VISA;

            case "MAST":
                return SdkIssuer.MASTERCARD;

            case "RUPAY":
                return SdkIssuer.RUPAY;

            case "MAES":
                return SdkIssuer.MAESTRO;

            default:
                if (cardMode.contentEquals(SdkConstants.PAYMENT_MODE_CC))
                    return SdkIssuer.UNKNOWN;
                else if (cardMode.contentEquals(SdkConstants.PAYMENT_MODE_DC))
                    return SdkIssuer.MASTERCARD;
        }

        return SdkIssuer.UNKNOWN;
    }

    /*public class ProxyHurlStack extends HurlStack {

        @Override
        protected HttpURLConnection createConnection(URL url) throws IOException {
            final HttpURLConnection urlConnection;
            Proxy proxy = null;
            try {
                proxy = ProxySelector.getDefault().select(url.toURI()).get(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (proxy == null) {
                urlConnection = (HttpURLConnection) url.openConnection();
            } else {
                urlConnection = (HttpURLConnection) url.openConnection(proxy);
            }
            return urlConnection;
        }
    }*/

    public void postFetch(final String url, final Map<String, String> params, final int method) {
        if (PayUmoneySdkInitilizer.IsDebugMode()) {
            SdkLogger.d(SdkConstants.TAG, "SdkSession.postFetch: " + url + " " + params + " " + method);
        }

        StringRequest myRequest = new StringRequest(method, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject object = new JSONObject(response);
                    if (object.has("error")) {
                        onFailure(object.getString("error"), new Throwable(object.getString("error")));
                    } else {
                        if (object != null && !object.getString("status").equals("-1") && object.has(SdkConstants.RESULT) && !object.isNull(SdkConstants.RESULT)) {
                            cardCvvHash = object.getString(SdkConstants.RESULT);
                            proceedToPay(selectedCard);
                        } else {
                            askForCvvDialog(adapter, selectedCardPosition);
                        }
                        //runSuccessOnHandlerThread(task, object);
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
                askForCvvDialog(adapter, selectedCardPosition);
                //runErrorOnHandlerThread(task, e);
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (PayUmoneySdkInitilizer.IsDebugMode()) {
                    Log.e(SdkConstants.TAG, "Session...new JsonHttpResponseHandler() {...}.onFailure: " + error.getMessage());
                }
                if (error != null && error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    //logout("force");
                }
                askForCvvDialog(adapter, selectedCardPosition);
                //runErrorOnHandlerThread(task, error);
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
                if (SdkSession.getInstance(mContext).getToken() != null) {
                    params.put("Authorization", "Bearer " + SdkSession.getInstance(mContext).getToken());
                } else {
                    params.put("Accept", "*/*;");
                }
                params.put("Cookie", SdkHelper.getUserCookieSessionId(mContext));
                return params;
            }

            @Override
            public String getBodyContentType() {
                if (SdkSession.getInstance(mContext).getToken() == null) {
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

    }

    private void dismissProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            //mProgress = false;
        }

    }
}
