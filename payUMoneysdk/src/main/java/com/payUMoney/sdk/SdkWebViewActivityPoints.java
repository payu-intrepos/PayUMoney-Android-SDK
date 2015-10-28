package com.payUMoney.sdk;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;


import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by amit on 27/11/13.
 */
public class SdkWebViewActivityPoints extends FragmentActivity {

    WebView mWebView = null;
    private BroadcastReceiver mReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sdk_activity_web_view);
        setTitle(R.string.pay);

        mWebView = (WebView) findViewById(R.id.webview);

        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {

            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.addJavascriptInterface(new PayUJavaScriptInterface(), "PayUMoney");
            mWebView.getSettings().setDomStorageEnabled(true);
            RequestParams p = new RequestParams();
            try {
                JSONObject object = new JSONObject(getIntent().getStringExtra(SdkConstants.RESULT));
                p.put("paymentId", object.getJSONObject(SdkConstants.PAYMENT).getString("paymentId"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (SdkConstants.DEBUG.booleanValue())
                mWebView.loadUrl("https://mobiletest.payumoney.com/payment/postBackParam.do?" + p.toString());
            else
                mWebView.loadUrl("https://www.payumoney.com/payment/postBackParam.do?" + p.toString());

            mWebView.setWebChromeClient(new WebChromeClient());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (mWebView != null) {
                mWebView.removeAllViews();
                mWebView.destroy();
            }
        } catch (NullPointerException ignored) {

        }

    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            String paymentId = getIntent().getStringExtra(SdkConstants.PAYMENT_ID);
            if(paymentId != null)
                SdkSession.getInstance(this).notifyUserCancelledTransaction(paymentId,"1");
            Intent intent = new Intent();
            intent.putExtra(SdkConstants.RESULT, "cancel");
            //mWebView.destroy();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.cancel) {
            cancelPayment();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    private void cancelPayment() {
        Intent intent = new Intent();
        intent.putExtra(SdkConstants.RESULT, "cancel");
        //mWebView.destroy();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    protected void onSaveInstanceState(Bundle outState) {
        mWebView.saveState(outState);
    }


    private final class PayUJavaScriptInterface {
        PayUJavaScriptInterface() {
        }

        @JavascriptInterface
        public void success(long id, final String paymentId) {
            runOnUiThread(new Runnable() {
                public void run() {

                    Intent intent = new Intent();
                    intent.putExtra(SdkConstants.RESULT, "success");
                    intent.putExtra(SdkConstants.PAYMENT_ID, paymentId);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }

        @JavascriptInterface
        public void failure(final String id, String error) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    failPayment();
                }
            });
        }

        @JavascriptInterface
        public void failure() {
            failure("");
        }

        @JavascriptInterface
        public void failure(final String params) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Intent intent = new Intent();
                    intent.putExtra(SdkConstants.RESULT, params);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
            });
        }

    }
    private void failPayment() {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(SdkConstants.RESULT, "failure");
//        mWebView.destroy();
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
