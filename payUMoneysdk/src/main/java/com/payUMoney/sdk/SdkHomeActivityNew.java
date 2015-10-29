package com.payUMoney.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.payUMoney.sdk.adapter.SdkCouponListAdapter;
import com.payUMoney.sdk.adapter.SdkExpandableListAdapter;
import com.payUMoney.sdk.adapter.SdkStoredCardAdapter;
import com.payUMoney.sdk.dialog.SdkQustomDialogBuilder;
import com.payUMoney.sdk.fragment.SdkDebit;
import com.payUMoney.sdk.fragment.SdkNetBankingFragment;
import com.payUMoney.sdk.fragment.SdkPayUMoneyPointsFragment;
import com.payUMoney.sdk.fragment.SdkStoredCardFragment;
import com.payUMoney.sdk.utils.SdkHelper;
import com.payUMoney.sdk.utils.SdkLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

public class SdkHomeActivityNew extends FragmentActivity implements SdkDebit.MakePaymentListener, SdkNetBankingFragment.MakePaymentListener, SdkStoredCardAdapter.MakePaymentListener, SdkStoredCardFragment.MakePaymentListener {

    private String device_id = null, currentVersion = null;
    final int LOGIN = 1;
    public final int WEB_VIEW = 2;
    public final int SIGN_UP = 7;
    public final int RESULT_BACK = 8;
    public TextView mAmount = null, savings = null, mAmoutDetails = null;
    SdkSession sdkSession = null;
    int count = 0;
    private ProgressDialog mProgressDialog = null;
    private HashMap<String, String> map = null;
    public double walletUsage = 0.0, walletAmount = 0.0, userPoints = 0.0,
            amt_convenience = 0.0,
            amt_net = 0.0,
            amount = 0.0,
            walletBal = 0.0;
    public static double coupan_amt = 0.0, choosedItem = 0.0;//Undo
    private JSONObject walletJason = null;
    private JSONObject details = null;
    private CheckBox walletCheck = null;
    private LinearLayout walletBoxLayout = null;
    private LinearLayout couponLayout = null;
    private TextView walletBalance = null, applyCoupon = null;
    private Button payByWalletButton = null;
    private double amt_discount = 0.0;
    private boolean walletFlag = false;
    private HashMap<String, Object> data = new HashMap<>();
    private ArrayList<String> availableModes, availableDebitCards, availableCreditCards = null;
    String mode = "WALLET";
    private double cashback = 0.0, discount = 0.0;
    private SdkCouponListAdapter coupanAdapter = null;
    private ListView couponList = null;
    private JSONObject couponListItem = null;
    public static String choosedCoupan = null;
    public JSONArray storedCardList = null, mCouponsArray = null;
    private boolean mProgress = false;
    private double amt_convenience_wallet = 0.0;
    private boolean chooseOtherMode = false;
    private boolean guestCheckOut = false;
    private String quickLogin = "", allowGuestCheckout = "";
    private JSONObject paymentOption = null;
    private boolean fromPayUMoneyApp, fromPayUBizzApp = false;
    private JSONObject appResponse = null;
    private JSONObject user = null;
    private String key = null;
    private ExpandableListView paymentModesList = null;
    private boolean isAnotherGroupExpanding = false;
    private SdkExpandableListAdapter listAdapter = null;
    private String paymentId = "";
    private String proceedForCvvLessTransaction = null;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mProgressDialog = showProgress(this);
        getAndroidID(this);
        getAppVersion();
        map = (HashMap<String, String>) getIntent().getSerializableExtra(SdkConstants.PARAMS);
        map.put(SdkConstants.DEVICE_ID, device_id);
        map.put(SdkConstants.APP_VERSION, currentVersion);
        if (map.containsKey(SdkConstants.PAYUMONEY_APP)) {
            try {
                fromPayUMoneyApp = true;
                appResponse = new JSONObject(map.get(SdkConstants.PAYUMONEY_APP));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (map.containsKey(SdkConstants.PAYUBIZZ_APP)) {
            fromPayUBizzApp = true;
            try {
                appResponse = new JSONObject(map.get(SdkConstants.PAYUBIZZ_APP));

                if (!SdkSession.getInstance(getApplicationContext()).isLoggedIn()) {
                    check_login();
                } else if (appResponse != null) {

                    initLayout();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        if (fromPayUMoneyApp) {
            if (appResponse != null) {

                initLayout();
            }
        } else {
            /*if (!SdkSession.getInstance(getApplicationContext()).isLoggedIn()) {
                SdkSession.getInstance(getApplicationContext()).fetchMechantParams(map.get("MerchantId"));
            } else {
                check_login();
            }*/
            check_login();
        }
    }

    public String getAndroidID(Context context) {

        if (context == null)
            return "";
        device_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return device_id;
    }

    public String getAppVersion() {

        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            currentVersion = pInfo.versionName;
            return currentVersion;
        } catch (Exception e) {
//Start the next activity
            return "";
        }

    }

    /*@Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        if(ev.getAction()==MotionEvent.ACTION_DOWN)
            return true;
        Toast.makeText(SdkHomeActivityNew.this,"touched :" ,Toast.LENGTH_SHORT).show();
        return super.dispatchTouchEvent(ev);
    }*/
    private void createPaymentModesList() {

        listAdapter = new SdkExpandableListAdapter(this, availableModes);
        paymentModesList.setAdapter(listAdapter);
        paymentModesList.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                if (!isAnotherGroupExpanding) {
                    //  paymentModesList.expandGroup(groupPosition);
                    mode = "";
                    updateDetails(mode);
                }
            }
        });
        paymentModesList.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            // Keep track of previous expanded parent
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                // Collapse previous parent if expanded.
                if ((previousGroup != -1) && (groupPosition != previousGroup)) {
                    isAnotherGroupExpanding = true;
                    paymentModesList.collapseGroup(previousGroup);
                    isAnotherGroupExpanding = false;
                }
                previousGroup = groupPosition;
                String currentGroup = listAdapter.getGroup(groupPosition).toString();
                if (currentGroup.equals("STORED_CARDS"))
                    mode = "";
                if (currentGroup.equals("DC")) {

                    mode = "DC";
                    /*sdkFragmentLifecycleNew = (SdkFragmentLifecycleNew) adapter.getItem(groupPosition);
                    sdkFragmentLifecycleNew.onResumeFragment(SdkHomeActivityNew.this);*/

                }
                if (currentGroup.equals("CC")) {

                    mode = "CC";
                    /*sdkFragmentLifecycleNew = (SdkFragmentLifecycleNew) adapter.getItem(groupPosition);
                    sdkFragmentLifecycleNew.onResumeFragment(SdkHomeActivityNew.this);*/

                }
                if (currentGroup.equals("NB"))
                    mode = "NB";
                updateDetails(mode);
            }
        });
        //if(listAdapter.getGroup(0).toString().equals("STORED_CARDS"))
        paymentModesList.expandGroup(0);

    }

    public void handleViewDetails() {
        SdkQustomDialogBuilder qb = new SdkQustomDialogBuilder(SdkHomeActivityNew.this, R.style.PauseDialog);

        qb.setTitleColor(SdkConstants.active_black).
                setDividerColor(SdkConstants.active_yellow).
                setTitle("Payment Breakdown").
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).
                show();
        StringBuffer message = new StringBuffer("Order Amount : Rs." + round((float) (amount / 100) * 100, 2));

        if (amt_convenience > 0.0) {
            message.append("\nConvenience Fee : Rs.").append(round((float) (amt_convenience / 100) * 100, 2)).append("\nTotal : Rs.").append(round((float) ((amt_convenience + amount) / 100) * 100, 2));
        } else
            message.append("\nTotal : Rs.").append(round((float) ((amount) / 100) * 100, 2));


        if (amt_discount > 0.0) {
            if (coupan_amt > 0.0) {
                message.append("\nCoupon Discount : Rs.").append(round((float) (amt_discount / 100) * 100, 2));
            } else {
                message.append("\nDiscount : Rs.").append(round((float) (amt_discount / 100) * 100, 2));
            }
        } else if (cashback > 0.0 && !(coupan_amt > 0.0)) {
            message.append("\nCashback : Rs.").append(round((float) (cashback / 100) * 100, 2));
        }
        if (userPoints > 0.0) {
            message.append("\nAvailable PayUMoney points : Rs.").append(round((float) (userPoints / 100) * 100, 2));
        }

        message.append("\nNet Amount : Rs.").append(round((float) (amt_net * 100) / 100, 2));

        if (walletUsage > 0.0) {
            message.append("\nWallet Usage: Rs.").append(round(walletUsage, 2));
        }

        qb.setMessage(message);


    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);


    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void check_login()       // Function to check login and if yes then initiate the payment
    {
        SdkLogger.d(SdkConstants.TAG, "entered in check login()");
        sdkSession = SdkSession.getInstance(getApplicationContext()); //get attached object of SdkSession
        if (!sdkSession.isLoggedIn() && !guestCheckOut)  //Not logged in
        {

            if (!sdkSession.isLoggedIn()) {
                dismissProgress();
                Intent intent = new Intent(SdkHomeActivityNew.this, SdkLoginSignUpActivity.class);
                intent.putExtra(SdkConstants.AMOUNT, getIntent().getStringExtra(SdkConstants.AMOUNT));
                intent.putExtra(SdkConstants.MERCHANTID, getIntent().getStringExtra(SdkConstants.MERCHANTID));
                intent.putExtra(SdkConstants.PARAMS, getIntent().getSerializableExtra(SdkConstants.PARAMS));
                intent.putExtra(SdkConstants.USER_EMAIL, getIntent().getStringExtra(SdkConstants.USER_EMAIL));
                intent.putExtra(SdkConstants.USER_PHONE, getIntent().getStringExtra(SdkConstants.USER_PHONE));
                intent.putExtra(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_VALUE, allowGuestCheckout);
                intent.putExtra(SdkConstants.OTP_LOGIN, quickLogin);
                startActivityForResult(intent, LOGIN);
            }
        } else if (fromPayUBizzApp) {

            if (appResponse != null && appResponse.has(SdkConstants.PAYMENT_ID) && !appResponse.isNull(SdkConstants.PAYMENT_ID))
                try {
                    paymentId = appResponse.getString(SdkConstants.PAYMENT_ID);
                    sdkSession.fetchUserParams(paymentId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        } else  //logged in already
        {
            initLayout();
        }
    }

    public void initLayout() {

        initHomeLayout();
        invalidateOptionsMenu();
        if (mProgressDialog != null && !mProgressDialog.isShowing())
            mProgressDialog = showProgress(this);
        /*if (guestCheckOut && !fromPayUMoneyApp && !fromPayUBizzApp) {
           SdkSession.getInstance(getApplicationContext()).updateTransactionDetails();
        }*/
        if (fromPayUMoneyApp || fromPayUBizzApp)
            startPayment(appResponse);
        else
            SdkSession.getInstance(this).createPayment(map);//Create  event fired when Parent Activity is created
    }

    private void initHomeLayout() {
        getAndroidID(this);
        setContentView(R.layout.sdk_activity_home_new);
        mAmount = ((TextView) findViewById(R.id.sdkAmountText));
        savings = (TextView) findViewById(R.id.savings);
        mAmoutDetails = (TextView) findViewById(R.id.amountDetails);
        walletBoxLayout = (LinearLayout) findViewById(R.id.walletLayout);
        walletCheck = (CheckBox) walletBoxLayout.findViewById(R.id.walletcheck);
        //walletText = (TextView) walletBoxLayout.findViewById(R.id.wallettext);
        walletBalance = (TextView) walletBoxLayout.findViewById(R.id.walletbalance);
        couponLayout = (LinearLayout) findViewById(R.id.couponSection);
        applyCoupon = (TextView) couponLayout.findViewById(R.id.selectCoupon);
        /*pagerContainerLayout = (RelativeLayout) findViewById(R.id.pagerContainer);
        mViewPager = (ViewPager) pagerContainerLayout.findViewById(R.id.pager);
        tabs = (SdkPagerSlidingTabStripCustomised) pagerContainerLayout.findViewById(R.id.tabs);*/
        paymentModesList = (ExpandableListView) findViewById(R.id.lvExp);
        payByWalletButton = (Button) findViewById(R.id.PayByWallet);
        payByWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                walletDialog();
            }
        });
        walletCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {

                    if (amount + amt_convenience_wallet - userPoints - amt_discount <= walletAmount) //Wallet is fatter so pay from it.
                    {
                        amt_convenience = amt_convenience_wallet;
                        walletBal = (walletAmount - (amt_convenience + amount - amt_discount - userPoints));
                        walletBalance.setText("Wallet balance: " + round(walletBal, 2));
                        walletUsage = walletAmount - walletBal;
                        updateDetails("WALLET");
                        //wallet is fatter hence pay by wallet
                        walletFlag = true;
                        paymentModesList.setVisibility(View.GONE);
                        payByWalletButton.setVisibility(View.VISIBLE);

                    } else //Wallet is smaller, remove wallet amount from net discounted amount
                    {
                        walletFlag = false;
                        walletUsage = walletAmount;
                        walletBal = 0.0;
                        updateDetails(mode);
                        walletBalance.setText("Wallet balance: " + 0.0);
                    }

                } else //NOT TICKED
                {
                    unchecked();

                }
            }
        });

        mAmoutDetails.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleViewDetails();
            }
        });
    }

    public void startPayment(JSONObject params)  //Intiate payment
    {
        SdkLogger.d(SdkConstants.TAG, "Entered in Start Payment");
        availableModes = new ArrayList<>();
        availableCreditCards = new ArrayList<>();
        availableDebitCards = new ArrayList<>();
        if ((fromPayUMoneyApp || fromPayUBizzApp) && !chooseOtherMode) {
            details = params;
            calculateOffersAndCashback();
        }

        try {
            if (user != null && user.has("savedCards") && !user.isNull("savedCards")) {
                availableModes.add("STORED_CARDS");
                JSONArray tempArr = user.getJSONArray("savedCards");
                setStoredCardList(tempArr);
            }
            if (details.has(SdkConstants.PAYMENT_OPTION) && !details.isNull(SdkConstants.PAYMENT_OPTION)) {
                JSONObject tempPaymentOption = details.getJSONObject(SdkConstants.PAYMENT_OPTION);
                if (tempPaymentOption != null && tempPaymentOption.has("options") && !tempPaymentOption.isNull("options")) {
                    paymentOption = tempPaymentOption.getJSONObject("options");
                }
                if (paymentOption != null) {
                    if (paymentOption.has("dc")) {
                        availableModes.add("DC");
                        if (!paymentOption.isNull("dc")) {

                            JSONObject tempDC = new JSONObject(paymentOption.getString("dc"));
                            Iterator keys = tempDC.keys();
                            while (keys.hasNext()) {
                                String tempCardType = (String) keys.next();
                                availableDebitCards.add(tempCardType);
                            }
                        }
                    }
                    if (paymentOption.has("cc")) {
                        availableModes.add("CC");
                        if (!paymentOption.isNull("cc")) {

                            JSONObject tempCC = new JSONObject(paymentOption.getString("cc"));
                            Iterator keys = tempCC.keys();
                            while (keys.hasNext()) {
                                String tempCardType = (String) keys.next();
                                availableCreditCards.add(tempCardType);
                            }
                        }
                    }
                    if (paymentOption.has("nb")) {
                        availableModes.add("NB");
                    }
                }
                if (tempPaymentOption != null && tempPaymentOption.has("config") && !tempPaymentOption.isNull("config")) {

                    JSONObject tempConfig = tempPaymentOption.getJSONObject("config");
                    if (tempConfig != null && tempConfig.has("publicKey") && !tempConfig.isNull("publicKey")) {
                        key = tempConfig.getString("publicKey").replaceAll("\\r", "");
                    }
                }
            }

            if (availableModes.get(0).equals("STORED_CARDS"))
                mode = "";//BugFixWalletConvenienceIssue
            else if (availableModes.get(0).equals("DC"))
                mode = "DC";
            else if (availableModes.get(0).equals("CC"))
                mode = "CC";
            else if (availableModes.get(0).equals("NB"))
                mode = "NB";

            if (mode.equals("WALLET") || mode.equals(""))
                amt_convenience = 0.0;
            else
                amt_convenience = new JSONObject(details.getString("convenienceCharges")).getJSONObject(mode).getDouble("DEFAULT");

            amount = details.getJSONObject(SdkConstants.PAYMENT).getDouble("orderAmount");
            amt_convenience_wallet = new JSONObject(details.getString("convenienceCharges")).getJSONObject("WALLET").getDouble("DEFAULT");

            if (!chooseOtherMode && !guestCheckOut)
                userPoints = this.getPoints().doubleValue(); //Get points user has

            if (coupan_amt > 0.0)
                amt_discount = coupan_amt;
            else //If No coupon
                amt_discount = discount;//Get discount offered

            if (amount + amt_convenience_wallet - amt_discount - userPoints <= 0.0 && !chooseOtherMode) {
                pointDialog();
            } else {
                if (walletCheck.isChecked()) {
                    amt_net = amount + amt_convenience - amt_discount - walletUsage - userPoints;
                } else {
                    amt_net = amount + amt_convenience - amt_discount - userPoints;
                }
                if (amt_net < 0.0) {
                    amt_net = 0.0;
                }

                mAmount.setText(" " + round((amt_net), 2));
                if (amt_discount > 0.0 || userPoints > 0.0) {
                    savings.setText("Savings : Rs." + (round((float) ((amt_discount + userPoints) / 100) * 100, 2)));
                    savings.setVisibility(View.VISIBLE);
                } else {
                    savings.setVisibility(View.INVISIBLE);
                }

                createPaymentModesList();
                /*if(!chooseOtherMode)
                walletCheck.setChecked(true);*/
                /****COUPONS*****/
                if (user.has("coupons") && !user.isNull("coupons")) {
                    updateCouponsVisibility();
                    if (coupan_amt <= 0.0) {
                        ((TextView) findViewById(R.id.selectCoupon)).setText(R.string.view_coupon);
                        handleCoupon();
                    }
                }
                dismissProgress();
            }
        } catch (JSONException ignored) {
        }
    }

    public JSONArray getStoredCardList() {
        return storedCardList;
    }

    public void setStoredCardList(JSONArray list) {
        storedCardList = list;
    }

    public void handleCoupon() {

        findViewById(R.id.selectCoupon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((((TextView) findViewById(R.id.selectCoupon)).getText().toString()).equals("Remove")) {

                    coupan_amt = 0.0;
                    amt_discount = discount;

                    if (walletCheck.isChecked()) {
                        updateWalletDetails();

                    } else {
                        updateDetails(mode);
                    }

                    ((TextView) findViewById(R.id.selectCoupon)).setText(R.string.view_coupon);
                    (findViewById(R.id.selectCoupon1)).setVisibility(View.INVISIBLE);
                    if (amt_discount > 0.0 || userPoints > 0.0) {
                        savings.setText("Savings : Rs." + (round((float) ((amt_discount + userPoints) / 100) * 100, 2)));
                        savings.setVisibility(View.VISIBLE);
                    } else
                        savings.setVisibility(View.INVISIBLE);

                } else //If remove is not in text i.e. u are adding some coupons
                {
                    final SdkQustomDialogBuilder alertDialog = new SdkQustomDialogBuilder(SdkHomeActivityNew.this, R.style.PauseDialog);
                    View convertView = getLayoutInflater().inflate(R.layout.sdk_coupon_list, null);
                    couponList = (ListView) convertView.findViewById(R.id.lv);
                    couponList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    try {
                        if (user.has("coupons") && !user.isNull("coupons") && mCouponsArray != null) {
                            coupanAdapter = new SdkCouponListAdapter(SdkHomeActivityNew.this, mCouponsArray);
                            couponList.setAdapter(coupanAdapter);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // coupan_amt = 0.0;
                            choosedItem = -1;
                            for (int j = 0; j < couponList.getCount(); j++) {

                                if (((RadioButton) couponList.getChildAt(j).findViewById(R.id.coupanSelect)).isChecked()) {
                                    /*****Apply the coupons ********/

                                    couponListItem = (JSONObject) coupanAdapter.getItem(j);
                                    try {
                                        choosedCoupan = couponListItem.getString("couponString");
                                        choosedItem = j;
                                        coupan_amt = couponListItem.getDouble("couponAmount");
                                        SdkLogger.i("Choosed coupan", choosedCoupan);
                                        amt_discount = coupan_amt;
                                        if (walletCheck.isChecked()) {
                                            updateWalletDetails();
                                        } else {
                                            updateDetails(mode);
                                        }
                                        if (amount + amt_convenience_wallet - amt_discount - userPoints <= 0.0 && !chooseOtherMode) {

                                            pointDialog();
                                        }
                                        if (amount - amt_discount == 0.0) //100% Coupon discount
                                        {
                                            if (payByWalletButton.isShown()) {
                                                mAmount.setText(" " + round((amt_convenience), 2));
                                                walletUsage = (walletAmount - amt_net);
                                                walletBal = walletAmount - walletUsage;
                                                walletBalance.setText("Wallet balance: " + round((float) (walletBal / 100) * 100, 2));
                                            } else {
                                                mAmount.setText(" " + 0.0);
                                            }
                                        }
                                        //Not enough but has some
                                        String coupon_string = couponListItem.getString("couponString") + " Applied";
                                        ((TextView) findViewById(R.id.selectCoupon1)).setText(coupon_string);
                                        (findViewById(R.id.selectCoupon1)).setVisibility(View.VISIBLE);
                                        ((TextView) findViewById(R.id.selectCoupon)).setText(R.string.remove);
                                        (findViewById(R.id.selectCoupon)).setVisibility(View.VISIBLE);
                                        if (amt_discount > 0.0 || userPoints > 0.0) {
                                            savings.setText("Savings : Rs." + (round((float) ((amt_discount + userPoints) / 100) * 100, 2)));
                                            savings.setVisibility(View.VISIBLE);
                                        } else
                                            savings.setVisibility(View.INVISIBLE);
                                        break;
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                    }).setView(convertView).show();

                    /*AlertDialog dialog = alertDialog.create();
                    dialog.setView(convertView, 0, 0, 0, 0);
                    dialog.setInverseBackgroundForced(true);
                    dialog.show();*/

                    couponList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            /*adapter.notifyDataSetChanged();*/

                            if (((RadioButton) view.findViewById(R.id.coupanSelect)).isChecked()) {
                                ((RadioButton) view.findViewById(R.id.coupanSelect)).setChecked(false);
                            } else {
                                ((RadioButton) view.findViewById(R.id.coupanSelect)).setChecked(true);
                            }

                            for (int j = 0; j < couponList.getCount(); j++) {
                                if (j != i)
                                    ((RadioButton) couponList.getChildAt(j).findViewById(R.id.coupanSelect)).setChecked(false);
                            }

                        }
                    });

                }
            }
        });


    }

    private void updateCouponsVisibility() {
        mCouponsArray = new JSONArray();
        try {
            if (user.getJSONArray("coupons") != null) {
                JSONArray tempArrayCoupons = user.getJSONArray("coupons");
                for (int i = 0; i < tempArrayCoupons.length(); i++) {
                    if (tempArrayCoupons.getJSONObject(i).getBoolean("enabled"))
                        mCouponsArray.put(tempArrayCoupons.getJSONObject(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (coupan_amt > 0.0)
            return;
        else {
            if (mCouponsArray.length() > 0) {
                applyCoupon.setText(R.string.view_coupon);
                applyCoupon.setVisibility(View.VISIBLE);
                couponLayout.setVisibility(View.VISIBLE);
            } else {
                applyCoupon.setVisibility(View.GONE);
                couponLayout.setVisibility(View.GONE);
            }
        }
    }

    public void updateDetails(String m) {
        try {
            if (m.equals("") && walletCheck.isChecked())
                amt_convenience = amt_convenience_wallet;
            else if (m.equals(""))
                amt_convenience = 0.0;
            else
                amt_convenience = new JSONObject(details.getString("convenienceCharges")).getJSONObject(m).getDouble("DEFAULT");
            if (coupan_amt > 0.0)
                amt_discount = coupan_amt;
            else
                amt_discount = discount;//details.getJSONObject("cashbackAccumulated").getDouble(Constants.AMOUNT);//can be commented?
            amt_net = amount + amt_convenience - amt_discount - walletUsage - userPoints;
            if (amt_net < 0.0)
                amt_net = 0.0;//bugfix
            mAmount.setText(" " + round((amt_net), 2));
            walletBalance.setText("Wallet balance: " + round((walletBal), 2));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getPublicKey() {
        return key;
    }

    public JSONObject getBankObject() {
        return details;
    }

    public Double getPoints() {
        return Double.valueOf(userPoints);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) //When HomeActivity resumes/starts
    {
        if (requestCode == LOGIN) {
            if (resultCode == RESULT_OK) {
                if (SdkSession.getInstance(getApplicationContext()).getLoginMode().equals("guestLogin")) {
                    guestCheckOut = true;
                }
                if (fromPayUBizzApp) {
                    setResult(RESULT_OK, data);
                    fromPayUBizzApp = false;
                } else
                    check_login();
            } else if (resultCode == SdkLoginSignUpActivity.RESULT_QUIT) {
                // check_login();
                close();
            } else if (resultCode == RESULT_CANCELED) {
                //check_login(); //commented by Viswash
                close();
                //Write your code if there's no result
            }
        } else if (requestCode == WEB_VIEW) //Coming back from making a payment
        {
            if (resultCode == RESULT_OK) //Success
            {
                SdkLogger.i("payment_status", "success");
                setResult(RESULT_OK, data);
                finish();
            } else if (resultCode == RESULT_CANCELED) //Fail
            {
                SdkLogger.i("payment_status", "failure");
                setResult(RESULT_CANCELED, data);
                finish();
                //Write your code if there's no result
            } else if (resultCode == RESULT_BACK) {
                //Write your code if there's no result
            } else {
                Toast.makeText(getApplicationContext(), "Something went wrong. please retry", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == SIGN_UP) {
            if (resultCode == RESULT_OK) {
                SdkLogger.i("login_status", "success");
                check_login();
            } else if (resultCode == RESULT_CANCELED) {
                SdkLogger.i("payment_status", "failure");

                check_login();
            }
        }
    }//onActivityResult

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            logout();
        }
        return super.onOptionsItemSelected(item);
    }

    public void logout() {
        SdkSession.getInstance(getApplicationContext()).logout("");
        SharedPreferences.Editor edit = getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE).edit();
        edit.clear();
        edit.commit();
        /*SdkCards.getInstance(getApplicationContext()).deleteAll();
        SdkUsers.getInstance(getApplicationContext()).deleteAll();*/
        SdkSession.getInstance(getApplicationContext()).startPaymentProcess(this, map);//LoginLogoutFlowIssue
        this.finish();
    }

    @Override
    public void proceedForCvvLessTransaction(String s) {
        proceedForCvvLessTransaction = s;
    }

    @Override
    public void goToPayment(String mode, HashMap<String, Object> data) throws JSONException {

        hideKeyboardIfShown();
        /*if (!Helper.checkNetwork(this)) {*/
        if (mode.equals("NB") && data.get("bankcode").equals("CITNB")) {
            Toast.makeText(this, "City Bank doesn't provide Net Banking!", Toast.LENGTH_LONG).show();
            int i = 0;
            if (availableModes.contains("DC")) {
                while (!availableModes.get(i).equals("DC")) {
                    i++;
                }
                Toast.makeText(this, "City Bank doesn't provide Net Banking!", Toast.LENGTH_LONG).show();
                paymentModesList.expandGroup(i);
            }
        } else if (mode.equals("DC") && !availableDebitCards.contains(data.get("bankcode"))) {

            Toast.makeText(this, "The merchant doesn't support: " + data.get("bankcode").toString() + " Debit Cards", Toast.LENGTH_SHORT).show();
        } else if (mode.equals("CC") && (data.get("bankcode").toString().equals("AMEX") || data.get("bankcode").toString().equals("DINR")) && !availableCreditCards.contains(data.get("bankcode"))) {

            Toast.makeText(this, "The merchant doesn't support: " + data.get("bankcode").toString() + " Credit Cards", Toast.LENGTH_SHORT).show();
        } else {
            //  handleCvvLessTransaction();
            mProgressDialog = showProgress(this);
            SdkSession.getInstance(this).sendToPayUWithWallet(details, mode, data, Double.valueOf(userPoints), Double.valueOf(walletUsage), Double.valueOf(discount));
        }
    }

  /*  private void handleCvvLessTransaction() {

        *//*if (cvvDialogIsShowing)
            return;*//*
        final SdkQustomDialogBuilder saveCvvDialog = new SdkQustomDialogBuilder(SdkHomeActivityNew.this, R.style.PauseDialog);
        saveCvvDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgressDialog = showProgress(SdkHomeActivityNew.this);
                        try {
                            SdkSession.getInstance(SdkHomeActivityNew.this).sendToPayUWithWallet(details, mode, data, Double.valueOf(userPoints), Double.valueOf(walletUsage), Double.valueOf(discount));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });
        saveCvvDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });


        saveCvvDialog.show();
    }*/

    private void hideKeyboardIfShown() {

        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = this.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*Not required in case of SDK in the app*/
        menu.add(Menu.NONE, R.id.logout, menu.size(), R.string.logout).setIcon(R.drawable.logout).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }

    public void onEventMainThread(final SdkCobbocEvent event) //Bus Function
    {
        if (event != null) {
            if (event.getType() == SdkCobbocEvent.FETCH_USER_PARAMS) {
                if (event.getStatus()) {
                    user = (JSONObject) event.getValue();
                    initLayout();
                } else {
                    SdkLogger.d(SdkConstants.TAG, "Error fetching User Params");
                }

            }
            if (event.getType() == SdkCobbocEvent.FETCH_MERCHANT_PARAMS) {
                if (event.getStatus()) {
                    try {
                        JSONObject jsonObject = (JSONObject) event.getValue();
                        if (jsonObject.has(SdkConstants.RESULT) && !jsonObject.isNull(SdkConstants.RESULT)) {
                            JSONArray result = jsonObject.getJSONArray(SdkConstants.RESULT);
                            for (int i = 0; i < result.length(); i++) {
                                JSONObject object = result.getJSONObject(i);

                                String paramKey = object.optString(SdkConstants.PARAM_KEY, "");
                                String paramValue = object.optString(SdkConstants.PARAM_VALUE, "");

                                if (paramKey.equals(SdkConstants.OTP_LOGIN))
                                    quickLogin = paramValue;
                                else if (paramKey.equals(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_VALUE))
                                    allowGuestCheckout = paramValue;

                            }
                        } else {
                            SdkLogger.d(SdkConstants.TAG, "Error fetching Merchant Login Params");

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                dismissProgress();
                check_login();

            } else if (event.getType() == SdkCobbocEvent.LOGOUT) {
                if (event.getValue() != null) {
                    if (event.getValue().equals(SdkConstants.LOGOUT_FORCE)) {
                        Toast.makeText(this, R.string.inactivity, Toast.LENGTH_LONG).show();
                        SharedPreferences.Editor edit = getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE).edit();
                        edit.clear();
                        edit.commit();
                        Intent intent = new Intent(SdkHomeActivityNew.this, SdkLoginSignUpActivity.class);
                        intent.putExtra(SdkConstants.AMOUNT, getIntent().getStringExtra(SdkConstants.AMOUNT));
                        intent.putExtra(SdkConstants.PARAMS, getIntent().getSerializableExtra(SdkConstants.PARAMS));
                        intent.putExtra(SdkConstants.USER_EMAIL, getIntent().getStringExtra(SdkConstants.USER_EMAIL));
                        intent.putExtra(SdkConstants.USER_PHONE, getIntent().getStringExtra(SdkConstants.USER_PHONE));
                        intent.putExtra(SdkConstants.LOGOUT_FORCE, SdkConstants.LOGOUT_FORCE);
                        startActivityForResult(intent, LOGIN);
                    }
                }
                // clear the token stored in SharedPreferences
            } else if (event.getType() == SdkCobbocEvent.USER_POINTS) //Add wallet points
            {
                SdkLogger.d(SdkConstants.TAG, "Entered in User Points");
                if (event.getStatus()) {
                    try {
                        walletJason = (JSONObject) event.getValue();

                        if ((walletJason.has(SdkConstants.WALLET)) && !walletJason.isNull(SdkConstants.WALLET))
                            walletAmount = walletJason.getJSONObject(SdkConstants.WALLET).optDouble(SdkConstants.AVAILABLE_AMOUNT, 0.0);

                        if (walletAmount > 0.0) {
                            //   wallettext.setVisibility(View.VISIBLE);
                            walletCheck.setVisibility(View.VISIBLE);
                            walletBoxLayout.setVisibility(View.VISIBLE);
                            walletBalance.setText("Wallet balance: " + round(walletBal, 2));
                            //walletText.setText("Initial bal: " + walletAmount);
                            SdkLogger.d(SdkConstants.TAG, "Exited from  User Points");
                        }
                    } catch (Exception e) {
                        SdkLogger.d(SdkConstants.TAG, e.toString());
                    }
                } else {
                    dismissProgress();
                    Toast.makeText(this, "Some error occurred! Try again", Toast.LENGTH_LONG).show();
                }
            } else if (event.getType() == SdkCobbocEvent.CREATE_PAYMENT)  //New Payment
            {
                SdkLogger.d(SdkConstants.TAG, "Entered in Create Payment");
                if (event.getStatus()) {
                    try {
                        details = (JSONObject) event.getValue();
                        if (details != null && details.has(SdkConstants.PAYMENT_ID) && !details.isNull(SdkConstants.PAYMENT_ID))
                            paymentId = details.getString(SdkConstants.PAYMENT_ID);
                        calculateOffersAndCashback();
                        // SdkSession.getInstance(this).getPaymentDetails(details.getJSONObject("payment").getString(Constants.PAYMENT_ID));//merge
                        //SdkSession.getInstance(this).getPaymentDetails(result.getString(Constants.PAYMENT_ID)); //Fire getpaymentdetails of sdkSession
                    } catch (Exception e) {
                        dismissProgress();
                        e.printStackTrace();
                    }
                    //   SdkSession.getInstance(this).getPaymentDetails((String)event.getValue());
                    SdkLogger.d(SdkConstants.TAG, "exited from Create Payment");
                } else {
                    dismissProgress();
                    Toast.makeText(this, "Some error occurred! Try again", Toast.LENGTH_LONG).show();
                }
            } else if (event.getType() == SdkCobbocEvent.PAYMENT_POINTS) {
                if (event.getStatus()) {

                    Intent intent = new Intent(this, SdkWebViewActivityPoints.class);
                    intent.putExtra(SdkConstants.RESULT, event.getValue().toString());
                    intent.putExtra(SdkConstants.PAYMENT_ID, paymentId);
                    this.startActivityForResult(intent, this.WEB_VIEW);

                }
            } else if (event.getType() == SdkCobbocEvent.PAYMENT) {
                dismissProgress();
                if (event.getStatus()) {
                    SdkLogger.i("reached", "cred" +
                            "it");
                    Intent intent = new Intent(this, SdkWebViewActivityNew.class);
                    intent.putExtra(SdkConstants.RESULT, event.getValue().toString());
                    intent.putExtra(SdkConstants.PAYMENT_MODE, mode);
                    if (mode.equals("")) {
                        if (proceedForCvvLessTransaction != null)
                            intent.putExtra("proceedForCvvLessTransaction", proceedForCvvLessTransaction);
                        else
                            intent.putExtra("proceedForCvvLessTransaction", "0");
                    }
                    intent.putExtra(SdkConstants.MERCHANT_KEY, getIntent().getExtras().getString("key"));
                    intent.putExtra(SdkConstants.PAYMENT_ID, paymentId);
                    this.startActivityForResult(intent, this.WEB_VIEW);
                } else {
                    SdkLogger.i("reached", "failed");
                    //If not status do nothing
                    Toast.makeText(this, "Payment Failed", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void calculateOffersAndCashback() {
        if (details != null) {
            try {
                if (details.has(SdkConstants.MERCHANT) && !details.isNull(SdkConstants.MERCHANT)) {
                    JSONObject merchant = details.getJSONObject(SdkConstants.MERCHANT);
                    if (merchant.has(SdkConstants.OFFER) && !merchant.isNull(SdkConstants.OFFER)) {
                        JSONObject tempDiscount = merchant.getJSONObject(SdkConstants.OFFER);
                        if (tempDiscount.has(SdkConstants.OFFER_TYPE) && !tempDiscount.isNull(SdkConstants.OFFER_TYPE)) {
                            String tempOfferType = tempDiscount.optString(SdkConstants.OFFER_TYPE, "Blank");
                            String temp = tempDiscount.optString(SdkConstants.OFFER_AMOUNT, "0.0");
                            if (tempOfferType.equals(SdkConstants.DISCOUNT)) {
                                discount = Double.parseDouble(temp);
                                cashback = 0.0;
                            } else if (tempOfferType.equals(SdkConstants.CASHBACK)) {
                                cashback = Double.parseDouble(temp);
                                discount = 0.0;
                            }
                        }
                    }
                }
                if (details.has(SdkConstants.USER) && !details.isNull(SdkConstants.USER)) {
                    user = details.getJSONObject(SdkConstants.USER);

                    if (user.has(SdkConstants.POINTS) && !user.isNull(SdkConstants.POINTS)) {
                        JSONObject tempPoints = user.getJSONObject(SdkConstants.POINTS);
                        userPoints = tempPoints.optDouble(SdkConstants.AMOUNT, 0.0);
                        /*if (chooseOtherMode)
                            userPoints = 0.0;*/ //CHECKED
                    }
                    if (user.has(SdkConstants.WALLET) && !user.isNull(SdkConstants.WALLET)) {
                        walletJason = user.getJSONObject(SdkConstants.WALLET);
                        if ((walletJason.has(SdkConstants.AMOUNT)) && !walletJason.isNull(SdkConstants.AMOUNT))
                            walletAmount = walletJason.optDouble(SdkConstants.AMOUNT, 0.0);

                        walletBal = walletAmount;
                        if (walletAmount > 0.0) {
                            walletCheck.setVisibility(View.VISIBLE);//CHECKED
                            walletBoxLayout.setVisibility(View.VISIBLE);
                            walletBalance.setText("Wallet balance: " + round(walletAmount, 2));
                        }
                    }
                } else if (fromPayUBizzApp && !SdkSession.getInstance(getApplicationContext()).isLoggedIn()) {

                    /*make the call*/
                }
                if (!fromPayUMoneyApp && !fromPayUBizzApp) {
                    startPayment(null);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(getApplicationContext(), "Something Went Wrong", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (mProgress) {
            mProgress = false;
            return;
        }
        count++;
        if (count % 2 == 0) {
            count = 0;
            close();
            SdkSession.getInstance(this).notifyUserCancelledTransaction(paymentId, "1");

        } else {
            Toast.makeText(getApplicationContext(), "Press Back again to cancel transaction", Toast.LENGTH_SHORT).show();
        }

    }


    public void close() {
        Intent intent = new Intent();
        intent.putExtra(SdkConstants.RESULT, "cancel");
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onDestroy() {
        coupan_amt = 0.0;
        super.onDestroy();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

    }

    public void unchecked() {
        /*try {
            if (details.getJSONArray("userCouponsAvailable").length() != 0) {
                if (coupan_amt > 0.0) {

                    applyCoupon.setText(R.string.remove);
                    couponLayout.setVisibility(View.VISIBLE);
                } else
                    for (int i = 0; i < details.getJSONArray("userCouponsAvailable").length(); i++) {
                        if (details.getJSONArray("userCouponsAvailable").getJSONObject(i).getBoolean("enabled")) {
                            applyCoupon.setText(R.string.view_coupon);
                            couponLayout.setVisibility(View.VISIBLE);
                            break;
                        }
                    }
            }
        } catch (Exception e) {
        }*/


        // walletText.setVisibility(View.GONE);
        walletUsage = 0.0;
        walletBal = walletAmount;
        updateDetails(mode);
        if (walletFlag) //Wallet is fatter
        {
            if (payByWalletButton.isShown()) {
                // availableModes.remove("wallet");
                payByWalletButton.setVisibility(View.GONE);
                paymentModesList.setVisibility(View.VISIBLE);
                // amt_convenience=0.0;
            }
        }
    }

    public void walletDialog() {
        amt_net = walletUsage;
        if (coupan_amt > 0.0)
            amt_discount = coupan_amt;
        if (userPoints > 0.0)
            showWalletwithPayu(userPoints, amt_discount, amt_net);
        else if (userPoints == 0.0)
            showWallet(amt_discount, amt_net);
        else
            Toast.makeText(this, "Something went Wrong", Toast.LENGTH_LONG).show();

    }

    public void showWallet(double dsc, final double net) {
        new SdkQustomDialogBuilder(this, R.style.PauseDialog).
                setTitleColor(SdkConstants.WHITE).
                setDividerColor(SdkConstants.greenPayU)
                .setTitle("Payment using Wallet")
                .setMessage("Yoo-hoo!\n" +
                        "\n" +
                        "You have enough money in PayUMoney Wallet for this transaction. All you need to do is confirm the payment by clicking on the OK button below and that's it." + "\n\nWallet Money Used : Rs." + round(net, 2) + "\nRemaining Money in Wallet : Rs." + round(walletBal, 2))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (!SdkHelper.checkNetwork(SdkHomeActivityNew.this)) {
                                    Toast.makeText(SdkHomeActivityNew.this, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                                } else {
                                    // Your code
                                    try {
                                        mProgressDialog.show();
                                        SdkSession.getInstance(SdkHomeActivityNew.this).sendToPayU(details, "wallet", data, Double.valueOf(net), Double.valueOf(discount)); //PURE WALLEt
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                ).setCancelable(false).show();

    }

    public void showWalletwithPayu(final double pnts, double dsc, final double net) {
        new SdkQustomDialogBuilder(this, R.style.PauseDialog).
                setTitleColor(SdkConstants.WHITE).
                setDividerColor(SdkConstants.greenPayU)
                .setTitle("Payment using Wallet")
                .setMessage("Yoo-hoo!\n" +
                        "\n" +
                        "You have enough money in PayUMoney Wallet for this transaction. All you need to do is confirm the payment by clicking on the OK button below and that's it." + "\n\nWallet Money Used : Rs." + round(net, 2) + "\nRemaining Money in Wallet : Rs." + round(walletBal, 2))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (!SdkHelper.checkNetwork(SdkHomeActivityNew.this)) {
                            Toast.makeText(SdkHomeActivityNew.this, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                        } else {
                            // Your code
                            try {
                                mProgressDialog.show();
                                SdkSession.getInstance(SdkHomeActivityNew.this).sendToPayUWithWallet(details, "wallet", data, Double.valueOf(net), Double.valueOf(pnts), Double.valueOf(discount)); //wallet +pnts
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).setCancelable(false)
                .show();
    }

    public void pointDialog() //If user has PayUpoints
    {
        dismissProgress();
        paymentModesList.setVisibility(View.INVISIBLE);
        /*mAmount.setText("0.0");
        savings.setText("Sufficient PayUPoints");
        walletBoxLayout.setVisibility(View.GONE);
        mAmoutDetails.setVisibility(View.GONE);*/

        new SdkQustomDialogBuilder(this, R.style.PauseDialog)
                .setTitle("Payment using PayUMoney points")
                .setTitleColor(SdkConstants.WHITE)
                .setDividerColor(SdkConstants.greenPayU)
                .setMessage("Yoo-hoo!\n" +
                        "\n" +
                        "You have enough PayUMoney points for this transaction. All you need to do is confirm the payment by clicking on the OK button below and that's it")
                .setNegativeButton("Choose Other Mode", new DialogInterface.OnClickListener() {//giving user the option to pay without points

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseOtherMode = true;
                        userPoints = 0.0;
                        updateWalletDetails();
                        if (!payByWalletButton.isShown())
                            paymentModesList.setVisibility(View.VISIBLE);
                        if (fromPayUMoneyApp || fromPayUBizzApp)
                            startPayment(appResponse);
                        /*else if(coupan_amt > 0.0)
                            paymentModesList.setVisibility(View.VISIBLE);*/
                        else
                            startPayment(null);


                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAmount.setText("0.0");
                        savings.setText("Sufficient PayUPoints");
                        walletBoxLayout.setVisibility(View.GONE);
                        mAmoutDetails.setVisibility(View.GONE);
                        payByWalletButton.setVisibility(View.GONE);
                        /*mViewPager.setVisibility(View.GONE);
                        pagerContainerLayout.setVisibility(View.VISIBLE);*/
                        couponLayout.setVisibility(View.GONE);
                        //tabs.setVisibility(View.GONE);
                        // Your code
                        SdkPayUMoneyPointsFragment fragment = new SdkPayUMoneyPointsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("details", details.toString());
                        bundle.putDouble("userPoints", userPoints);
                        bundle.putDouble("discount", discount);
                        bundle.putDouble("cashback", cashback);
                        bundle.putDouble("couponAmount", coupan_amt);

                        fragment.setArguments(bundle);
                        FragmentTransaction transaction;
                        transaction = getFragmentManager().beginTransaction().setCustomAnimations(
                                R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                                R.animator.card_flip_left_in, R.animator.card_flip_left_out);
                        //((HomeActivity) getActivity()).onPaymentOptionSelected(details);
                        transaction.replace(R.id.pagerContainer, fragment, "payumoneypoints");
                        transaction.addToBackStack("a");
                        transaction.commit();
                        getFragmentManager().executePendingTransactions();
                    }
                })
                        //  dialog.setCanceledOnTouchOutside(false);
                .setOnKeyListener(new Dialog.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {

                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            FragmentManager fragmentManager = getFragmentManager();
                            android.app.Fragment tempFragment = fragmentManager.findFragmentByTag("paymentOptions");
                            if (tempFragment != null) {
                                Intent intent = new Intent();
                                intent.putExtra(SdkConstants.RESULT, "cancel");
                                setResult(RESULT_CANCELED, intent);
                                finish();
                            }
                        }
                        return true;

                    }
                }).setCancelable(false).show();

    }

    public static BigDecimal round(float d, int decimalPlace) {

        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    public static BigDecimal round(double d, int decimalPlace) {

        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    public ProgressDialog showProgress(Context context) {
        mProgress = true;
        LayoutInflater mInflater = LayoutInflater.from(context);
        final Drawable[] drawables = {getResources().getDrawable(R.drawable.nopoint_green),
                getResources().getDrawable(R.drawable.onepoint_green),
                getResources().getDrawable(R.drawable.twopoint_green),
                getResources().getDrawable(R.drawable.threepoint_green)
        };

        View layout = mInflater.inflate(R.layout.sdk_prog_dialog, null);
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
        progDialog.setCancelable(false);
        progDialog.setCanceledOnTouchOutside(false);
        return progDialog;
    }

    private void dismissProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgress = false;
        }

    }

    private void updateWalletDetails() {
        if (walletCheck.isChecked()) {

            if (amount + amt_convenience_wallet - userPoints - amt_discount <= walletAmount && (amount + amt_convenience_wallet - userPoints - amt_discount) >= 0.0) {
                amt_convenience = amt_convenience_wallet;
                walletUsage = amount + amt_convenience - userPoints - amt_discount;
                walletBal = walletAmount - walletUsage;
                updateDetails("WALLET");
                payByWalletButton.setVisibility(View.VISIBLE);//bugfix
                paymentModesList.setVisibility(View.GONE);
                walletFlag = true;

            } else if ((amount + amt_convenience - userPoints - amt_discount) > walletAmount && (amount + amt_convenience_wallet - userPoints - amt_discount) > 0) {

                walletUsage = walletAmount;
                walletBal = 0.0;
                updateDetails(mode);
                walletFlag = false;
                payByWalletButton.setVisibility(View.GONE);//bugfix
                paymentModesList.setVisibility(View.VISIBLE);
            } else if ((amount + amt_convenience_wallet - userPoints - amt_discount) > 0) {//wallet is fat enough to pay

                amt_convenience = amt_convenience_wallet;
                walletUsage = amount + amt_convenience - userPoints - amt_discount;
                walletBal = walletAmount - walletUsage;
                updateDetails("WALLET");
                payByWalletButton.setVisibility(View.VISIBLE);//bugfix
                paymentModesList.setVisibility(View.GONE);
                walletFlag = true;
            } else if ((amount + amt_convenience_wallet - userPoints - amt_discount) <= 0) {
                walletUsage = 0;
                walletBal = walletAmount - walletUsage;
                updateDetails("WALLET");
                walletFlag = false;
            }

            walletBalance.setText("Wallet balance: " + round(walletBal, 2));
        }
    }
}
