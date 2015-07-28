package com.payUMoney.sdk;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.ImageView;

import com.loopj.android.http.RequestParams;
import com.payu.custombrowser.Bank;
import com.payu.custombrowser.PayUWebChromeClient;
import com.payu.custombrowser.PayUWebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/*import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.MapBuilder;*/

/**
 * Created by amit on 27/11/13.
 */
public class WebViewActivity extends ActionBarActivity {

    private Handler mHandler = null;
    private View transOverlay;
    private ProgressDialog progressDialog;
    private int checkUnavailable = 0;
    private WebView webView;
    private BroadcastReceiver mReceiver = null;
    boolean cancelTransaction = false;
    private JSONObject object;
//    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        webView = (WebView) findViewById(R.id.webview);
        mHandler = new Handler();
        try {
            Class.forName("com.payu.custombrowser.Bank");
            final Bank bank = new Bank() {
                @Override
                public void registerBroadcast(BroadcastReceiver broadcastReceiver, IntentFilter filter) {
                    mReceiver = broadcastReceiver;
                    registerReceiver(broadcastReceiver, filter);
                }

                @Override
                public void unregisterBroadcast(BroadcastReceiver broadcastReceiver) {
                    if(mReceiver != null){
                        unregisterReceiver(mReceiver);
                        mReceiver = null;
                    }
                }

                @Override
                public void onHelpUnavailable() {
                    findViewById(R.id.parent).setVisibility(View.GONE);
                    findViewById(R.id.trans_overlay).setVisibility(View.GONE);
                }

                @Override
                public void onBankError() {
                    findViewById(R.id.parent).setVisibility(View.GONE);
                    findViewById(R.id.trans_overlay).setVisibility(View.GONE);
                }

                @Override
                public void onHelpAvailable() {
                    findViewById(R.id.parent).setVisibility(View.VISIBLE);
                }
            };
            Bundle args = new Bundle();
            args.putInt("webView", R.id.webview);
            args.putInt("tranLayout",R.id.trans_overlay);
            args.putInt("mainLayout",R.id.r_layout);

            /*String [] list =  getIntent().getExtras().getString("postData").split("&");
            String txnId = null;
            for (String item : list) {
                if(item.contains("txnid")){
                    txnId = item.split("=")[1];
                    break;
                }
            }*/

            JSONObject temp = new JSONObject(getIntent().getStringExtra(Constants.RESULT));
            String txnId = temp.getString("txnid");

            txnId = txnId == null ? String.valueOf(System.currentTimeMillis()) : txnId;
            args.putString(Bank.TXN_ID, txnId);
            if(getIntent().getExtras().containsKey("showCustom")) {
                args.putBoolean("showCustom", getIntent().getBooleanExtra("showCustom", false));
            }
            args.putBoolean("showCustom", true);
            bank.setArguments(args);
            findViewById(R.id.parent).bringToFront();
            try {
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.cb_fade_in, R.anim.cb_face_out).add(R.id.parent, bank).commit();
            }catch(Exception e)
            {
                e.printStackTrace();
                finish();
            }
            webView.setWebChromeClient(new PayUWebChromeClient(bank) {});
            webView.setWebViewClient(new PayUWebViewClient(bank) {
            });
            webView.addJavascriptInterface(new PayUJavaScriptInterface(), "PayUMoney");
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

            RequestParams p = new RequestParams();
            try {
                object = new JSONObject(getIntent().getStringExtra(Constants.RESULT));
                Iterator keys = object.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    p.put(key, object.getString(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        webView.postUrl("https://" + (Constants.DEBUG ? "test" : "secure") + ".payu.in/_seamless_payment", p.toString().getBytes());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (webView != null) {
                webView.clearCache(true);
                webView.removeAllViews();
                webView.destroy();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().setSoftInputMode(WindowManager.
                LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // progressBarVisibility(View.VISIBLE);
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
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    private void failPayment() {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(Constants.RESULT, "failure");
//        mWebView.destroy();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
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
        webView.saveState(outState);
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
        public void failure(final String id, String error) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    cancelPayment();
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

    public void progressBarVisibility(int visibility) {
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
        } else if (progressDialog == null || !progressDialog.isShowing()) {
            progressDialog = showProgress(this);
        }
    }

    public void progressBarVisibilityPayuChrome(int visibility) {
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
        } else if (progressDialog == null) {
            progressDialog = showProgress(this);
        }
    }

    public ProgressDialog showProgress(Context context) {
        LayoutInflater mInflater = LayoutInflater.from(context);
        final Drawable[] drawables = {getResources().getDrawable(R.drawable.nopoint),
                getResources().getDrawable(R.drawable.onepoint),
                getResources().getDrawable(R.drawable.twopoint),
                getResources().getDrawable(R.drawable.threepoint)
        };

        View layout = mInflater.inflate(R.layout.prog_dialog, null);
        final ImageView imageView;
        imageView = (ImageView) layout.findViewById(R.id.imageView);
        ProgressDialog progDialog = new ProgressDialog(context, R.style.ProgressDialog);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int i = -1;

            @Override
            synchronized public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        i++;
                        if (i >= drawables.length) {
                            i = 0;
                        }
                        imageView.setImageBitmap(null);
                        imageView.destroyDrawingCache();
                        imageView.refreshDrawableState();
                        imageView.setImageDrawable(drawables[i]);
                    }
                });

            }
        }, 0, 500);

        progDialog.show();
        progDialog.setContentView(layout);
        progDialog.setCancelable(true);
        progDialog.setCanceledOnTouchOutside(false);
        return progDialog;
    }


}
