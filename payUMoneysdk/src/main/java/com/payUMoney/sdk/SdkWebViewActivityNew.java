package com.payUMoney.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.payUMoney.sdk.utils.SdkLogger;
import com.payUMoney.sdk.walledSdk.SharedPrefsUtils;
import com.payu.custombrowser.Bank;
import com.payu.custombrowser.PayUWebChromeClient;
import com.payu.custombrowser.PayUWebViewClient;
import com.payu.magicretry.Helpers.Util;
import com.payu.magicretry.MagicRetryFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;



public class SdkWebViewActivityNew extends FragmentActivity implements MagicRetryFragment.ActivityCallback {

    Bundle bundle = null;
    String url = null;
    boolean cancelTransaction = false;
    private BroadcastReceiver mReceiver = null;
    private String UTF = "UTF-8";
    private boolean viewPortWide = false;
    private WebView mWebView = null;
    private ProgressDialog progressDialog = null;
    MagicRetryFragment magicRetryFragment;
    private HashMap<String,String> p;
    public final int RESULT_FAILED = 90;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        JSONObject object;
        JSONObject userConfigDto = null;
        String userToken = null;
        Boolean oneClickPayment = false, oneTapFeature = false, mOTPAutoRead = false;

        try {
            if (savedInstanceState != null) {
                super.onCreate(null);
                finish();//call activity u want to as activity is being destroyed it is restarted
            } else {
                super.onCreate(savedInstanceState);
            }

            bundle = getIntent().getExtras();

            setContentView(R.layout.sdk_activity_web_view);
            mWebView = (WebView) findViewById(R.id.webview);
            SharedPreferences mPref = getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);

            String paymentMode = null;
            if(bundle != null) {
                paymentMode = bundle.getString(SdkConstants.PAYMENT_MODE);
            }

            if (mPref.contains(SdkConstants.CONFIG_DTO) && mPref.contains(SdkConstants.ONE_TAP_FEATURE) && mPref.getBoolean(SdkConstants.ONE_TAP_FEATURE,false))
                userConfigDto = new JSONObject(mPref.getString(SdkConstants.CONFIG_DTO, SdkConstants.XYZ_STRING));

            if (userConfigDto != null) {
                if (userConfigDto.has(SdkConstants.ONE_CLICK_PAYMENT) && !userConfigDto.isNull(SdkConstants.ONE_CLICK_PAYMENT)) {
                    oneClickPayment = userConfigDto.optBoolean(SdkConstants.ONE_CLICK_PAYMENT);
                    if (oneClickPayment && userConfigDto.has(SdkConstants.ONE_TAP_FEATURE) && !userConfigDto.isNull(SdkConstants.ONE_TAP_FEATURE)) {
                        oneTapFeature = userConfigDto.optBoolean(SdkConstants.ONE_TAP_FEATURE);
                    }
                }
            }
            if (userConfigDto != null && userConfigDto.has(SdkConstants.USER_TOKEN) && !userConfigDto.isNull(SdkConstants.USER_TOKEN)) {
                userToken = userConfigDto.getString(SdkConstants.USER_TOKEN);
            }

            Class.forName(SdkConstants.CUSTOM_BROWSER_PACKAGE);
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

            if (bundle != null && bundle.getString(SdkConstants.PAYMENT_MODE).equals(SdkConstants.PAYMENT_MODE_NB)) {
                viewPortWide = true;
            }

            args.putBoolean(Bank.VIEWPORTWIDE, viewPortWide);

            String txnId = null, merchantKey = null;
            if(bundle != null) {
                JSONObject temp = new JSONObject(bundle.getString(SdkConstants.RESULT));
                if(temp != null && temp.has(SdkConstants.TXNID) && !temp.isNull(SdkConstants.TXNID)) {
                    txnId = temp.getString(SdkConstants.TXNID);
                }

                merchantKey = bundle.getString(SdkConstants.MERCHANT_KEY);
            }

            txnId = txnId == null ? String.valueOf(System.currentTimeMillis()) : txnId;
            args.putString(Bank.TXN_ID, txnId);

            args.putString(Bank.MERCHANT_KEY, null != merchantKey ? merchantKey : SdkConstants.COULD_NOT_FOUND);

            args.putBoolean(Bank.SHOW_CUSTOMROWSER, true);

            if(bundle != null && bundle.getBoolean(SdkConstants.OTP_AUTO_READ, false)) {
                args.putBoolean(Bank.MERCHANT_SMS_PERMISSION, true);
            }

            if (oneTapFeature) {
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
            p = new HashMap<>();

            if(bundle != null){
                object = new JSONObject(bundle.getString(SdkConstants.RESULT));
                if(object != null) {
                    Iterator keys = object.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        p.put(key, object.getString(key));
                    }
                }
            }

            if (paymentMode != null && oneClickPayment) {
                if (paymentMode.isEmpty()) {
                    if (bundle != null && bundle.getString(SdkConstants.CARD_HASH_FOR_ONE_CLICK_TXN).equals("0")) {
                        p.put(SdkConstants.ONE_CLICK_CHECKOUT, "1");
                        if (userToken != null && !userToken.isEmpty())
                            p.put(SdkConstants.USER_TOKEN, userToken);
                    } else
                        p.put(SdkConstants.CARD_MERCHANT_PARAM, bundle.getString(SdkConstants.CARD_HASH_FOR_ONE_CLICK_TXN));
                } else if (paymentMode.equals(SdkConstants.PAYMENT_MODE_DC) || paymentMode.equals(SdkConstants.PAYMENT_MODE_CC)) {
                    p.put(SdkConstants.ONE_CLICK_CHECKOUT, "1");
                    if (userToken != null && !userToken.isEmpty())
                        p.put(SdkConstants.USER_TOKEN, userToken);
                }
            }

            initMagicRetry(txnId);
            mWebView.setWebViewClient(new PayUWebViewClient(bank, magicRetryFragment,merchantKey));
            //mWebView is the WebView Object
            magicRetryFragment.setWebView(mWebView);
            // MR Integration - initMRSettingsFromSharedPreference
            magicRetryFragment.initMRSettingsFromSharedPreference(this);
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.getSettings().setDomStorageEnabled(true);

            // Remove these
            /*p.put(SdkConstants.ccname_KEY, SdkConstants.ccname_VALUE);
            p.put(SdkConstants.ccvv_KEY, SdkConstants.ccvv_VALUE);
            p.put(SdkConstants.ccexpmon_KEY, SdkConstants.ccexpmon_VALUE);
            p.put(SdkConstants.ccexpyr_KEY, SdkConstants.ccexpyr_VALUE);*/

            mWebView.postUrl(PayUmoneySdkInitilizer.getWebviewRedirectionUrl(), getParameters1(p).getBytes());



        } catch (ClassNotFoundException e) {
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getParameters1(Map<String, String> params) {
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

    private void initMagicRetry(String txnId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        magicRetryFragment = new MagicRetryFragment();
        Bundle newInformationBundle = new Bundle();
        newInformationBundle.putString(MagicRetryFragment.KEY_TXNID, txnId);
        magicRetryFragment.setArguments(newInformationBundle);

        Map<String, String> urlList = new HashMap<String, String>();
        urlList.put(url, p.toString());
        magicRetryFragment.setUrlListWithPostData(urlList);

        fragmentManager.beginTransaction().add(R.id.magic_retry_container, magicRetryFragment, "magicRetry").commit();
        // magicRetryFragment = (MagicRetryFragment) fragmentManager.findFragmentBy(R.id.magicretry_fragment);

        toggleFragmentVisibility(Util.HIDE_FRAGMENT);

        magicRetryFragment.isWhiteListingEnabled(true);
    }


    public void toggleFragmentVisibility(int flag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (!isFinishing()) {
            if (flag == Util.SHOW_FRAGMENT) {
                // Show fragment
                ft.show(magicRetryFragment).commitAllowingStateLoss();
            } else if (flag == Util.HIDE_FRAGMENT) {
                // Hide fragment
                ft.hide(magicRetryFragment).commitAllowingStateLoss();
                // ft.hide(magicRetryFragment);
                // SdkLogger.v("#### PAYU", "hidhing magic retry");
            }
        }
    }

    @Override
    public void showMagicRetry() {
        toggleFragmentVisibility(Util.SHOW_FRAGMENT);
    }

    @Override
    public void hideMagicRetry() {
        toggleFragmentVisibility(Util.HIDE_FRAGMENT);
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
        public void success(long id, final String paymentId, String amount) {
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
        public void failure(final String id, final String paymentId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(SdkConstants.RESULT, "failure");
                    intent.putExtra(SdkConstants.PAYMENT_ID, paymentId);
                    setResult(RESULT_FAILED, intent);
                    finish();
                }
            });
        }

        /*@JavascriptInterface
        public void failure() {
            failure("");
        }

        @JavascriptInterface
        public void failure(final String params) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Intent intent = new Intent();
                    intent.putExtra(SdkConstants.RESULT, "");
                    setResult(RESULT_FAILED, intent);
                    finish();
                }
            });
        }*/

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_payments, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPrefsUtils.setStringPreference(this, SdkConstants.USER_SESSION_COOKIE_PAGE_URL, this.getClass().getSimpleName());
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
            String paymentId = null;
            if(bundle != null) {
                paymentId = bundle.getString(SdkConstants.PAYMENT_ID);
            }
            if (paymentId != null)
                SdkSession.getInstance(this).notifyUserCancelledTransaction(paymentId, null);
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

    /*public void progressBarVisibility(int visibility) {
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
    }*/

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
        /*Error handling:No solution to check if the reciever is registered or not by android*/
        catch (IllegalArgumentException e){
            SdkLogger.d("Error during unregistering reciever");
        }

    }
}
