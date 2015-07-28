package com.payUMoney.sdk;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
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
public class WebViewActivityPoints extends ActionBarActivity {

    WebView mWebView;
    private Handler mHandler = null;
    private BroadcastReceiver mReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        setTitle(R.string.pay);


        mHandler = new Handler();

        mWebView = (WebView) findViewById(R.id.webview);

        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {

            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.addJavascriptInterface(new PayUJavaScriptInterface(), "PayUMoney");
            mWebView.getSettings().setDomStorageEnabled(true);

            RequestParams p = new RequestParams();
            try {
                JSONObject object = new JSONObject(getIntent().getStringExtra(Constants.RESULT));
                p.put("paymentId", object.getJSONObject(Constants.PAYMENT).getString("paymentId"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (Constants.DEBUG)
                mWebView.loadUrl("https://test.payumoney.com/payment/postBackParam.do?" + p.toString());
                //https:\/\/test.payumoney.com\/payment\/postBackParam.do
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
                Intent intent = new Intent();
                intent.putExtra(Constants.RESULT, "cancel");
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        } catch (NullPointerException ex) {

        }

    }

    @Override
    public void onResume() {
        super.onResume();

//        mWebView.loadUrl("javascript:localStorage.setItem(\"access_token\", \"" + Session.getInstance(this).getSessionData().getLoginResponse() + "\");");
//        mWebView.loadUrl("javascript:localStorage.setItem(\"lastActivity\", \"" + (System.currentTimeMillis() / 1000) + "\");");
    }

    @Override
    public void onPause() {
        super.onPause();
        // mWebView.setWebChromeClient(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            Intent intent = new Intent();
            intent.putExtra(Constants.RESULT, "cancel");
            //mWebView.destroy();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    private void failPayment() {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(Constants.RESULT, "failure");
        setResult(RESULT_CANCELED, intent);
        finish();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_web_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent intent = new Intent();
            intent.putExtra(Constants.RESULT, "cancel");
            //mWebView.destroy();
            setResult(RESULT_CANCELED, intent);
            finish();
            return true;
        } else if (id == R.id.cancel) {
            cancelPayment();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    private void cancelPayment() {
        Intent intent = new Intent();
        intent.putExtra(Constants.RESULT, "cancel");
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

        /**
         * This is not called on the UI thread. Post a runnable to invoke
         * loadUrl on the UI thread.
         */
        @JavascriptInterface
        public void success(long id, final String paymentId) {
            mHandler.post(new Runnable() {
                public void run() {
                    mHandler = null;
                    Intent intent = new Intent();
                    intent.putExtra(Constants.RESULT, "success");
                    intent.putExtra(Constants.PAYMENT_ID, paymentId);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }

        @JavascriptInterface
        public void failure(String id, String error) {
            Log.i("error", error);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    failPayment();
                }
            });
        }

        @JavascriptInterface
        public void success() {
            success("");
        }

        @JavascriptInterface
        public void success(final String params) {
            mHandler.post(new Runnable() {
                public void run() {
                    mHandler = null;

                    Intent intent = new Intent();
                    intent.putExtra(Constants.RESULT, params);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }

        @JavascriptInterface
        public void failure() {
            failure("");
        }

        @JavascriptInterface
        public void failure(final String params) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    Intent intent = new Intent();
                    intent.putExtra(Constants.RESULT, params);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
            });
        }
    }
}
