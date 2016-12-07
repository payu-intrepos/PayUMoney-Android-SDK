package com.payUMoney.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;

import com.payu.custombrowser.Bank;
import com.payu.custombrowser.CustomBrowser;
import com.payu.custombrowser.PayUCustomBrowserCallback;
import com.payu.custombrowser.PayUWebChromeClient;
import com.payu.custombrowser.PayUWebViewClient;
import com.payu.custombrowser.bean.CustomBrowserConfig;
import com.payu.magicretry.MagicRetryFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.greenrobot.event.EventBus;


public class SdkWebViewActivityNew extends AppCompatActivity {

    private boolean viewPortWide;
    public final int RESULT_FAILED = 90;
    private Map<String, String> pMap;
    String userToken = null;
    private static final String TAG = SdkWebViewActivityNew.class.getName();



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sdk_activity_web_view_dummy);

        final String merchantKey = (null==getIntent().getExtras().getString(SdkConstants.MERCHANT_KEY))?"could not find"
                :getIntent().getExtras().getString(SdkConstants.MERCHANT_KEY);


        // Callback for CB
        PayUCustomBrowserCallback payUCustomBrowserCallback = new PayUCustomBrowserCallback() {
            private boolean isPostBackParamFallBackRequired;

            @Override
            public void onPaymentFailure(String payuResponse,String merchantResponse) {
                if (PayUmoneySdkInitilizer.IsDebugMode()) {
                    Log.i(SdkConstants.TAG, "Failure -- payuResponse" + payuResponse );
                    Log.i(SdkConstants.TAG, "Failure -- merchantResponse" + merchantResponse );
                }
            }

            @Override
            public void onPaymentTerminate() {
                if(!isPostBackParamFallBackRequired){
                    finish();
                }
            }

            @Override
            public void onPaymentSuccess(String payuResponse,String merchantResponse) {
                if (PayUmoneySdkInitilizer.IsDebugMode()) {
                    Log.i(SdkConstants.TAG, "Success -- payuResponse" + payuResponse );
                    Log.i(SdkConstants.TAG, "Success -- merchantResponse" + merchantResponse );
                }
            }


            @Override
            public void onCBErrorReceived(int code, String errormsg) {
            }



            @Override
            public void setCBProperties(WebView webview, Bank payUCustomBrowser) {
                webview.setWebChromeClient(new PayUWebChromeClient(payUCustomBrowser));
                webview.setWebViewClient(new PayUWebViewClient(payUCustomBrowser,merchantKey));
            }

            @Override
            public void onBackApprove() {
                notifyUserCancelledTransaction();
                Intent intent = new Intent();
                intent.putExtra(SdkConstants.RESULT, "cancel");
                setResult(RESULT_CANCELED, intent);
                SdkWebViewActivityNew.this.finish();
            }

            @Override
            public void onBackDismiss() {
                super.onBackDismiss();
            }

            @Override
            public void onBackButton(AlertDialog.Builder alertDialogBuilder) {
                super.onBackButton(alertDialogBuilder);
            }

            @Override
            public void initializeMagicRetry(Bank payUCustomBrowser,WebView webview,MagicRetryFragment magicRetryFragment){
                webview.setWebViewClient(new PayUWebViewClient(payUCustomBrowser, magicRetryFragment,merchantKey));
                Map<String, String> urlList = new HashMap<String, String>();
                //urlList.put(PayUmoneySdkInitilizer.getWebviewRedirectionUrl(), getParameters1(pMap));
                payUCustomBrowser.setMagicRetry(urlList);
            }

        };

        // Set configuration for CB

        try {
            JSONObject temp = new JSONObject(getIntent().getStringExtra(SdkConstants.RESULT));
            pMap = new HashMap<>();
            Iterator keys = temp.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                pMap.put(key, temp.getString(key));
            }
            pMap.put("device_type","1");

            String txnId = temp.getString(SdkConstants.TXNID);

            txnId = txnId == null ? String.valueOf(System.currentTimeMillis()) : txnId;
            CustomBrowserConfig customBrowserConfig = new CustomBrowserConfig(merchantKey,txnId);
            if (getIntent().getStringExtra(SdkConstants.PAYMENT_MODE).equals("NB"))
                viewPortWide = true;
            customBrowserConfig.setViewPortWideEnable(viewPortWide);

            SharedPreferences mPref = getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
            String paymentMode = getIntent().getExtras().getString(SdkConstants.PAYMENT_MODE);
            JSONObject userConfigDto = null;
            Boolean oneClickPayment = false, oneTapFeature = false;
            if (mPref.contains(SdkConstants.CONFIG_DTO) && mPref.contains(SdkConstants.ONE_TAP_FEATURE) && mPref.getBoolean(SdkConstants.ONE_TAP_FEATURE,false))
                userConfigDto = new JSONObject(mPref.getString(SdkConstants.CONFIG_DTO, "XYZ"));

            if (userConfigDto != null) {
                if (userConfigDto.has(SdkConstants.ONE_CLICK_PAYMENT) && !userConfigDto.isNull(SdkConstants.ONE_CLICK_PAYMENT)) {
                    oneClickPayment = userConfigDto.optBoolean(SdkConstants.ONE_CLICK_PAYMENT);
                    if (oneClickPayment && userConfigDto.has(SdkConstants.ONE_TAP_FEATURE) && !userConfigDto.isNull(SdkConstants.ONE_TAP_FEATURE)) {
                        oneTapFeature = userConfigDto.optBoolean(SdkConstants.ONE_TAP_FEATURE);
                    }
                }
            }

            if (userConfigDto != null && userConfigDto.has("userToken") && !userConfigDto.isNull("userToken")) {
                userToken = userConfigDto.getString("userToken");
            }

            if (paymentMode != null && oneClickPayment) {
                if (paymentMode.equals("")) {
                    if (getIntent().getExtras().getString("cardHashForOneClickTxn").equals("0")) {
                        pMap.put(SdkConstants.ONE_CLICK_CHECKOUT, "1");
                        if (userToken != null && !userToken.isEmpty())
                            pMap.put("userToken", userToken);
                    } else
                        pMap.put(SdkConstants.CARD_MERCHANT_PARAM, getIntent().getExtras().getString("cardHashForOneClickTxn"));
                } else if (paymentMode.equals("DC") || paymentMode.equals("CC")) {
                    pMap.put(SdkConstants.ONE_CLICK_CHECKOUT, "1");
                    if (userToken != null && !userToken.isEmpty())
                        pMap.put("userToken", userToken);
                }
            }


            if(oneTapFeature){
                customBrowserConfig.setAutoApprove(true);
                customBrowserConfig.setAutoSelectOTP(true);
            }else{
                customBrowserConfig.setAutoApprove(false);
                customBrowserConfig.setAutoSelectOTP(false);
            }

            customBrowserConfig.setDisableBackButtonDialog(false);
            customBrowserConfig.setStoreOneClickHash(CustomBrowserConfig.STOREONECLICKHASH_MODE_SERVER);
            customBrowserConfig.setMerchantSMSPermission(true);
            customBrowserConfig.setmagicRetry(true);
            customBrowserConfig.setPostURL(PayUmoneySdkInitilizer.getWebviewRedirectionUrl());
            String s = getParameters1(pMap);
            customBrowserConfig.setPayuPostData(s);
            new CustomBrowser().addCustomBrowser(SdkWebViewActivityNew.this,customBrowserConfig , payUCustomBrowserCallback);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void notifyUserCancelledTransaction() {
        String paymentId = getIntent().getStringExtra(SdkConstants.PAYMENT_ID);
        if (paymentId != null)
            SdkSession.getInstance(SdkWebViewActivityNew.this).notifyUserCancelledTransaction(paymentId, "1");
    }
    private synchronized  String getParameters1(Map<String, String> params) {
        String parameters = "";
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

    public void onEventMainThread(SdkCobbocEvent event) {
        if (event.getType() == SdkCobbocEvent.POST_BACK_PARAM) {
            if(event.getStatus()){
                Log.d(TAG, "POST_BACK_PARAM getStatus is true");
            }else {
                Log.d(TAG, "POST_BACK_PARAM getStatus is false");
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
        super.onDestroy();
    }

}