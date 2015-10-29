package com.payUMoney.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.loopj.android.http.RequestParams;
import com.payUMoney.sdk.adapter.SdkStoredCardAdapter;
import com.payu.custombrowser.Bank;
import com.payu.custombrowser.PayUWebChromeClient;
import com.payu.custombrowser.PayUWebViewClient;

/*import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.MapBuilder;*/

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class SdkWebViewActivityNew extends FragmentActivity {

    Bundle bundle = null;
    String url = null;
    boolean cancelTransaction = false;
    private BroadcastReceiver mReceiver = null;
    private String UTF = "UTF-8";
    private boolean viewPortWide = false;
    private WebView mWebView = null;
    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            super.onCreate(null);
            finish();//call activity u want to as activity is being destroyed it is restarted
        } else {
            super.onCreate(savedInstanceState);
        }
        setContentView(R.layout.sdk_activity_web_view);
        mWebView = (WebView) findViewById(R.id.webview);
        SharedPreferences mPref = getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        bundle = getIntent().getExtras();

        JSONObject object;
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
                    if (mReceiver != null) {
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
            args.putInt(Bank.WEBVIEW, R.id.webview);
            args.putInt(Bank.TRANS_LAYOUT, R.id.trans_overlay);
            args.putInt(Bank.MAIN_LAYOUT, R.id.r_layout);
            if (getIntent().getStringExtra(SdkConstants.PAYMENT_MODE).equals("NB"))
                viewPortWide = true;

            args.putBoolean(Bank.VIEWPORTWIDE, viewPortWide);
            JSONObject temp = new JSONObject(getIntent().getStringExtra(SdkConstants.RESULT));
            String txnId = temp.getString("txnid");

            txnId = txnId == null ? String.valueOf(System.currentTimeMillis()) : txnId;
            args.putString(Bank.TXN_ID, txnId);

            String merchantKey = getIntent().getExtras().getString(SdkConstants.MERCHANT_KEY);
            args.putString(Bank.MERCHANT_KEY, null != merchantKey ? merchantKey : "could not find");
            /*PayUSdkDetails payUSdkDetails = new PayUSdkDetails();
            args.putString(Bank.SDK_DETAILS, payUSdkDetails.getSdkVersionName());*/
            if (getIntent().getExtras().containsKey("showCustom")) {
                args.putBoolean(Bank.SHOW_CUSTOMROWSER, getIntent().getBooleanExtra("showCustom", false));
            }
            args.putBoolean(Bank.SHOW_CUSTOMROWSER, true);
            if(mPref.getBoolean(SdkConstants.ONE_TAP_PAYMENT, false)) {
                args.putBoolean(Bank.AUTO_APPROVE, true);
                args.putBoolean(Bank.AUTO_SELECT_OTP, true);
            }
            bank.setArguments(args);
            findViewById(R.id.parent).bringToFront();
            try {
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.cb_fade_in, R.anim.cb_face_out).add(R.id.parent, bank).commit();

            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }

            mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            mWebView.getSettings().setSupportMultipleWindows(true);
            mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            // Setting view port for NB
            if (viewPortWide) {
                mWebView.getSettings().setUseWideViewPort(viewPortWide);
            }
            // Hiding the overlay
            /*View transOverlay = findViewById(R.id.trans_overlay);
            transOverlay.setVisibility(View.GONE);*/

            mWebView.addJavascriptInterface(new PayUJavaScriptInterface(), "PayUMoney");
            mWebView.setWebChromeClient(new PayUWebChromeClient(bank) {
                /*public void onProgressChanged(WebView view, int progress) {
                    super.onProgressChanged(view, progress);
                    getWindow().setSoftInputMode(WindowManager.
                            LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    if(progress == 100)
                        progressBarVisibilityPayuChrome(View.GONE);
                    else
                        progressBarVisibilityPayuChrome(View.VISIBLE);
                }
*/

            });
            mWebView.setWebViewClient(new PayUWebViewClient(bank));
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.getSettings().setDomStorageEnabled(true);
            RequestParams p = new RequestParams();
            try {
                object = new JSONObject(getIntent().getStringExtra(SdkConstants.RESULT));
                Iterator keys = object.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    p.put(key, object.getString(key));
                }
            } catch (JSONException exc) {
                exc.printStackTrace();
            }
            String paymentMode = getIntent().getExtras().getString(SdkConstants.PAYMENT_MODE);


            if (paymentMode != null && mPref != null && mPref.getBoolean(SdkConstants.ONE_TAP_PAYMENT, false)) {
                if (paymentMode.equals("")) {
                    if (getIntent().getExtras().getString("proceedForCvvLessTransaction").equals("0"))
                        p.put(SdkConstants.ONE_CLICK_CHECKOUT, "1");
                    else
                        p.put(SdkConstants.CARD_MERCHANT_PARAM, getIntent().getExtras().getString("proceedForCvvLessTransaction"));
                } else if (paymentMode.equals("DC") || paymentMode.equals("CC")) {
                    p.put(SdkConstants.ONE_CLICK_CHECKOUT, "1");
                }
            }
            mWebView.postUrl("https://" + (SdkConstants.DEBUG.booleanValue() ? "mobiletest" : "secure") + ".payu.in/_seamless_payment", p.toString().getBytes());

        } catch (ClassNotFoundException e) {
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_payments, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (cancelTransaction) {
            cancelTransaction = false;
            String paymentId = getIntent().getStringExtra(SdkConstants.PAYMENT_ID);
            if (paymentId != null)
                SdkSession.getInstance(this).notifyUserCancelledTransaction(paymentId, "1");
            Intent intent = new Intent();
            intent.putExtra(SdkConstants.RESULT, "cancel"/*"Transaction canceled due to back pressed!"*/);
            setResult(RESULT_CANCELED, intent);
            super.onBackPressed();
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setCancelable(false);
        alertDialog.setMessage("Do you really want to cancel the transaction ?");
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancelTransaction = true;
                dialog.dismiss();
                onBackPressed();
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
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

        View layout = mInflater.inflate(R.layout.cb_prog_dialog, null);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
                mReceiver = null;
            }
            if (mWebView != null) {
                mWebView.clearCache(true);
                mWebView.removeAllViews();
                mWebView.destroy();
            }
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }
}
