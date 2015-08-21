package com.payUMoney.sdk;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.support.v4.view.ViewPager;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.payUMoney.sdk.adapter.CouponListAdapter;
import com.payUMoney.sdk.adapter.PaymentModeAdapter;
import com.payUMoney.sdk.adapter.StoredCardAdapter;
import com.payUMoney.sdk.database.Cards;
import com.payUMoney.sdk.database.Users;
import com.payUMoney.sdk.dialog.QustomDialogBuilder;
import com.payUMoney.sdk.fragment.Credit;
import com.payUMoney.sdk.fragment.Debit;
import com.payUMoney.sdk.fragment.NetBankingFragment;
import com.payUMoney.sdk.fragment.PayUMoneyPointsFragment;
import com.payUMoney.sdk.interfaces.FragmentLifecycle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

public class HomeActivity extends FragmentActivity implements Credit.MakePaymentListener, Debit.MakePaymentListener, NetBankingFragment.MakePaymentListener, StoredCardAdapter.MakePaymentListener {

    private String device_id,currentVersion;
    String mid;
    String amt;
    final int LOGIN = 1;
    public final int WEB_VIEW = 2;
    public final int SIGN_UP = 7;
    public final int RESULT_BACK = 8;
    private boolean show = false;
    public TextView mAmount, savings, mAmoutDetails;
    private AccountManager mAccountManager;
    Session session;
    int count = 0;
    private Account mAccount;
    Button login, register;
    private ViewPager mViewPager;
    PaymentModeAdapter adapter;
    private ProgressDialog mProgressDialog;
    private Timer timer;
    private HashMap<String, String> map;
    public double walletUsage = 0.0, walletAmount = 0.0, cashback_amt_total = 0.0,
            amt_convenience = 0.0,
            amt_net = 0.0,
            amount = 0.0,
            walletBal;
    public static double coupan_amt = 0.0, choosedItem = 0.0;//Undo


    private JSONObject walletJason;
    private JSONObject details;
    private CheckBox walletCheck;
    private LinearLayout walletBoxLayout;
    private RelativeLayout pagerContainerLayout, couponLayout;
    private TextView walletText, walletBalance, applyCoupon;
    private Button payByWalletButton;

    private FragmentLifecycle fragmentLifecycle;


    private PagerSlidingTabStripCustomised tabs;
    private double amt_discount = 0.0;
    private boolean walletFlag;
    private HashMap<String, Object> data = new HashMap<String, Object>();
    private ArrayList<String> availableModes;
    String mode = "WALLET";

    private double discountCashback = 0.0;
    private CouponListAdapter coupanAdapter;
    private ListView couponList;
    private JSONObject couponListItem;
    public static String choosedCoupan;
    private JSONObject points;
    public JSONArray storedCardList;
    private boolean mProgress = false;
    private Double modifiedDiscount = 0.0;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);


        map = (HashMap<String, String>) getIntent().getSerializableExtra(Constants.PARAMS);

        check_login();

        //Called every time activity starts
    }

    public String getAndroidID(Context context) {
        if (context == null)
            return "";
        device_id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return device_id;
    }
    public String getAppVersion(){

        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            currentVersion = pInfo.versionName;
            return currentVersion;
        }catch (Exception e)
        {
//Start the next activity
                return "";
        }

    }

    private void initHomeLayout() {

        getAndroidID(this);
        setContentView(R.layout.activity_home);

        mAmount = ((TextView) findViewById(R.id.amountText));
        savings = (TextView) findViewById(R.id.savings);
        mAmoutDetails = (TextView) findViewById(R.id.amountDetails);
        walletBoxLayout = (LinearLayout) findViewById(R.id.walletLayout);
        walletCheck = (CheckBox) walletBoxLayout.findViewById(R.id.walletcheck);
        walletText = (TextView) walletBoxLayout.findViewById(R.id.wallettext);
        walletBalance = (TextView) walletBoxLayout.findViewById(R.id.walletbalance);
        couponLayout = (RelativeLayout) findViewById(R.id.couponSection);
        applyCoupon = (TextView) couponLayout.findViewById(R.id.selectCoupon);
        pagerContainerLayout = (RelativeLayout) findViewById(R.id.pagerContainer);
        mViewPager = (ViewPager) pagerContainerLayout.findViewById(R.id.pager);
        tabs = (PagerSlidingTabStripCustomised) pagerContainerLayout.findViewById(R.id.tabs);
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

                   /* if(payByWalletButton.isShown())//bug fixes
                    {
                        try {
                            amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }*/
                    try {
                        if (amount + new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT") - cashback_amt_total - amt_discount <= walletAmount) //Wallet is fatter so pay from it.
                        {

                            amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT");

                            walletBal = (walletAmount - (amt_convenience + amount - amt_discount - cashback_amt_total));

                            walletBalance.setText("Remaining bal: " + round(walletBal, 2));
                            walletUsage = walletAmount - walletBal;
                            updateDetails("WALLET");
                            /*amt_net = 0.0;*/
                            /*mAmount.setText(" " + round(((amt_net)), 2));*/


                            //wallet is fatter hence pay by wallet

                                walletFlag = true;
                                pagerContainerLayout.setVisibility(View.GONE);
                                payByWalletButton.setVisibility(View.VISIBLE);



                        } else //Wallet is smaller, remove wallet amount from net discounted amount
                        {

                            walletFlag = false;
                            walletUsage = walletAmount;
                            walletBal = 0.0;
                            updateDetails(mode);
                            walletBalance.setText("Remaining bal: " + 0.0);
                            //amt_net = amount - amt_discount - cashback_amt_total + amt_convenience - walletUsage;

                            /*if (amt_net < 0) //When convinience fee of other cards is in play
                            {
                                mAmount.setText(" " + 0.0);
                                amt_net = 0.0;
                            } else*/
                             //   mAmount.setText(" " + round((amt_net), 2));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        walletText.setText("Initial bal: " + String.valueOf(walletJason.getJSONObject("wallet").getDouble("availableAmount")));
                    } catch (Exception e) {
                    }

                    walletBalance.setVisibility(View.VISIBLE);
                    walletText.setVisibility(View.VISIBLE);
                } else if (!b) //NOT TICKED
                {
                    unchecked();

                }
            }
        });

        mAmoutDetails.setOnClickListener(new View.OnClickListener()
        {
            //@Override if (amtafterCoupanDiscount - cashback_amt_total <= 0) {
            public void onClick(View v) {
                handleViewDetails();
            }
        });
    }



    public void handleViewDetails(){
        if (coupan_amt == 0.0) {
            if (walletCheck.isShown())
                new QustomDialogBuilder(HomeActivity.this, R.style.PauseDialog).
                        setTitleColor(Constants.greenPayU).
                        setDividerColor(Constants.greenPayU)
                        .setTitle("Payment Details")
                        .setMessage("**Bill Break Down**\n\n" + "Order Amount : Rs." + round((float) (amount / 100) * 100, 2) + "\nConvenience Fee : Rs." + round((float) (amt_convenience / 100) * 100, 2) + "\nTotal : Rs." + round((float) ((amt_convenience + amount) / 100) * 100, 2) + "\n\n**Payment Break Down**\n" + "\nDiscount : Rs." + round((float) (amt_discount / 100) * 100, 2) + "\nAvailable PayUMoney points : Rs." + round((float) (cashback_amt_total / 100) * 100, 2) + "\nNet Amount : Rs." + round((float) (amt_net * 100) / 100, 2) + "\nWallet:Rs. " + round(walletUsage, 2))

                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })

                        .show();
            else
                new QustomDialogBuilder(HomeActivity.this, R.style.PauseDialog).
                        setTitleColor(Constants.greenPayU).
                        setDividerColor(Constants.greenPayU)
                        .setTitle("Payment Details")
                        .setMessage("**Bill Break Down**\n\n" + "Order Amount : Rs." + round((float) (amount / 100) * 100, 2) + "\nConvenience Fee : Rs." + round((float) (amt_convenience / 100) * 100, 2) + "\nTotal : Rs." + round((float) ((amt_convenience + amount) / 100) * 100, 2) + "\n\n**Payment Break Down**\n" + "\nDiscount : Rs." + round((float) (amt_discount / 100) * 100, 2) + "\nAvailable PayUMoney points : Rs." + round((float) (cashback_amt_total / 100) * 100, 2) + "\nNet Amount : Rs." + round((float) (amt_net * 100) / 100, 2))

                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Your code
                            }
                        })

                        .show();
        } else {
            if (walletCheck.isShown())
                new QustomDialogBuilder(HomeActivity.this, R.style.PauseDialog).
                        setTitleColor(Constants.greenPayU).
                        setDividerColor(Constants.greenPayU)
                        .setTitle("Payment Details")
                        .setMessage("\n**Bill Break Down**\n\n" + "Order Amount : Rs." + round((float) (amount / 100) * 100, 2) + "\nConvenience Fee : Rs." + round((float) (amt_convenience / 100) * 100, 2) + "\nTotal : Rs." + round((float) ((amt_convenience + amount) / 100) * 100, 2) + "\n\n**Payment Break Down**\n" + "\nAvailable PayUMoney points : Rs." + round((float) (cashback_amt_total / 100) * 100, 2) + "\nCoupon Discount :" + round((float) (coupan_amt / 100) * 100, 2) + "\nNet Amount : Rs." + round((float) (amt_net * 100) / 100, 2) + "\nWallet :Rs. " + round(walletUsage, 2))

                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Your code
                            }
                        })

                        .show();
            else
                new QustomDialogBuilder(HomeActivity.this, R.style.PauseDialog).
                        setTitleColor(Constants.greenPayU).
                        setDividerColor(Constants.greenPayU)
                        .setTitle("Payment Details")
                        .setMessage("\n**Bill Break Down**\n\n" + "Order Amount : Rs." + round((float) (amount / 100) * 100, 2) + "\nConvenience Fee : Rs." + round((float) (amt_convenience / 100) * 100, 2) + "\nTotal : Rs." + round((float) ((amount + amt_convenience) / 100) * 100, 2) + "\n\n**Payment Break Down**\n" + "\nAvailable PayUMoney points : Rs." + round((float) (cashback_amt_total / 100) * 100, 2) + "\nCoupon Discount :" + round((float) (coupan_amt / 100) * 100, 2) + "\nNet Amount : Rs." + round((float) (amt_net * 100) / 100, 2))

                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Your code
                            }
                        })

                        .show();
        }
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
        Log.d("Sagar", "entered in check login()");
        getAndroidID(this);
        getAppVersion();
        map.put("deviceId", device_id);
        map.put("appVersion", currentVersion);
        session = Session.getInstance(getApplicationContext()); //get attached object of Session
        if (!session.isLoggedIn())  //Not logged in
        {
            setContentView(R.layout.chooser);
            show = false;
            invalidateOptionsMenu();
            login = (Button) findViewById(R.id.login);
            register = (Button) findViewById(R.id.signup);

            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (!session.isLoggedIn()) {
                        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                        intent.putExtra(Constants.AMOUNT, getIntent().getStringExtra(Constants.AMOUNT));
                        intent.putExtra(Constants.MERCHANTID, getIntent().getStringExtra(Constants.MERCHANTID));
                        intent.putExtra(Constants.PARAMS, getIntent().getSerializableExtra(Constants.PARAMS));
                        intent.putExtra(Constants.USER_EMAIL, getIntent().getStringExtra(Constants.USER_EMAIL));
                        intent.putExtra(Constants.USER_PHONE, getIntent().getStringExtra(Constants.USER_PHONE));
                        startActivityForResult(intent, LOGIN);
                    }

                }
            });

            register.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!session.isLoggedIn()) {
                        Intent intent = new Intent(HomeActivity.this, SignUpActivity.class);
                        intent.putExtra(Constants.AMOUNT, getIntent().getStringExtra(Constants.AMOUNT));
                        intent.putExtra(Constants.MERCHANTID, getIntent().getStringExtra(Constants.MERCHANTID));
                        intent.putExtra(Constants.PARAMS, getIntent().getSerializableExtra(Constants.PARAMS));
                        intent.putExtra(Constants.USER_EMAIL, getIntent().getStringExtra(Constants.USER_EMAIL));
                        intent.putExtra(Constants.USER_PHONE, getIntent().getStringExtra(Constants.USER_PHONE));
                        startActivityForResult(intent, SIGN_UP);
                    }
                }
            });
        } else  //logged in already
        {
            initHomeLayout();
            show = true;
            invalidateOptionsMenu();
            mProgressDialog = showProgress(this);
            Log.d("Sagar", "entered in check login1()");
            Session.getInstance(this).getUserPoints();
            Session.getInstance(this).createPayment(map); //Create  event fired when Parent Activity is created

        }
    }

    public JSONArray getStoredCardList() {
        return storedCardList;
    }

    public void setStoredCardList(JSONArray list) {
        storedCardList = list;
    }

    public void startPayment(final HashMap<String, String> params)  //Intiate payment
    {
        Log.d("Sagar", "Entered in Start Payment");
        availableModes = new ArrayList<String>();
        mid = params.get("MerchantId");
        amt = params.get("Amount");
        mAmount.setText(amt);



        try {
            JSONObject paymentOption = details.getJSONObject(Constants.PAYMENT_OPTION);
            amount = details.getJSONObject(Constants.PAYMENT).getDouble("totalAmount");
           /* if (!details.getString("cashbackAccumulated").toString().equals("null"))
                discountCashback = details.getJSONObject("cashbackAccumulated").getDouble(Constants.AMOUNT);*/
            if (!(details.getString("storeCardDTOList").toString().equals("null"))) {

                availableModes.add("STORED_CARDS");
                storedCardList = details.getJSONArray("storeCardDTOList");
                setStoredCardList(storedCardList);

            }
            if (paymentOption.has("db")) //Add debitcard to list
            {
                availableModes.add("DC");
            }
            if (paymentOption.has("cc")) {  //Add creditcard to list
                availableModes.add("CC");
            }
            if (paymentOption.has("nb")) {     //Add NetBanking to list
                availableModes.add("NB");
            }

            cashback_amt_total = this.getPoints(); //Get points user has
            //Get amount user need to pay

            if (coupan_amt > 0.0)
                amt_discount = coupan_amt;
            else //If No coupon
                amt_discount = discountCashback;//Get discount offered

            if (amt_discount != 0.0) {
                savings.setText("Savings : Rs." + (round((float) (amt_discount / 100) * 100, 2)));
                savings.setVisibility(View.VISIBLE);
            } else {
                savings.setVisibility(View.GONE);
            }

            if (availableModes.get(0).equals("STORED_CARDS"))
                mode = "WALLET";
            else if (availableModes.get(0).equals("DC"))
                mode = "DC";
            else if (availableModes.get(0).equals("CC"))
                mode = "CC";
            else if (availableModes.get(0).equals("NB"))
                mode = "NB";





            if(mode == "WALLET")
                amt_convenience = 0.0;
            else
                amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject(mode).getDouble("DEFAULT");
            //setMincon();
        } catch (JSONException e) {
        }

        try {
            if(amount + new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT") - amt_discount - cashback_amt_total <= 0.0){

                pointDialog();
            } else {
                amt_net = amount - amt_discount - cashback_amt_total + amt_convenience;
                if (amt_net < 0.0) {
                    amt_net = 0.0;
                }
                try {
                    mAmount.setText(" " + round((amt_net), 2));

                } catch (Exception e) {
                    e.printStackTrace();
                }

                adapter = new PaymentModeAdapter(getSupportFragmentManager(), this, availableModes);
                mViewPager.setAdapter(adapter);
                mViewPager.setOffscreenPageLimit(availableModes.size() + 1);
                tabs.setViewPager(mViewPager);
                dismissProgress();
                tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {

                        if (adapter.getPageTitle(position).toString().equals("Saved Cards"))
                            mode = "";
                        if (adapter.getPageTitle(position).toString().equals("Debit Card")){

                            mode = "DC";
                            fragmentLifecycle = (FragmentLifecycle) adapter.getItem(position);
                            fragmentLifecycle.onResumeFragment(HomeActivity.this);

                        }
                        if (adapter.getPageTitle(position).toString().equals("Credit Card")){

                            mode = "CC";
                            fragmentLifecycle = (FragmentLifecycle) adapter.getItem(position);
                            fragmentLifecycle.onResumeFragment(HomeActivity.this);

                        }
                        if (adapter.getPageTitle(position).toString().equals("Net Banking"))
                            mode = "NB";
                        updateDetails(mode);
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });

                /*****COUPONS******/
                try {
                    if (details.getJSONArray("userCouponsAvailable").length() != 0 && amount >= 1) //if coupons are available
                    {
                        for (int i = 0; i < details.getJSONArray("userCouponsAvailable").length(); i++) {
                            if (details.getJSONArray("userCouponsAvailable").getJSONObject(i).getBoolean("enabled")) {
                                findViewById(R.id.couponSection).setVisibility(View.VISIBLE);
                                findViewById(R.id.selectCoupon).setVisibility(View.VISIBLE);
                                //findViewById(R.id.selectCoupon1)).setText(R.string.select_coupon_option);
                                ((TextView) findViewById(R.id.selectCoupon)).setText(R.string.view_coupon);

                                break;
                            }
                        }
                        final JSONArray coupansArray = new JSONArray();
                        if (details.getJSONArray("userCouponsAvailable") != null) {
                            for (int i = 0; i < details.getJSONArray("userCouponsAvailable").length(); i++) {
                                if (details.getJSONArray("userCouponsAvailable").getJSONObject(i).getBoolean("enabled"))
                                    coupansArray.put(details.getJSONArray("userCouponsAvailable").getJSONObject(i));
                            }
                        }
                        coupanAdapter = new CouponListAdapter(this, coupansArray);
                        findViewById(R.id.selectCoupon).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if ((((TextView) findViewById(R.id.selectCoupon)).getText().toString()).equals("Remove")) {

                                    coupan_amt = 0.0;
                                    amt_discount = discountCashback;

                                    if (walletCheck.isChecked()) {

                                        try {
                                            if( (amount + new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT") - cashback_amt_total -amt_discount) > walletAmount) {
                                                walletUsage = walletAmount;
                                                walletBal = 0.0;
                                                updateDetails(mode);
                                                if(payByWalletButton.isShown()) {
                                                    payByWalletButton.setVisibility(View.GONE);
                                                    pagerContainerLayout.setVisibility(View.VISIBLE);
                                                }
                                                walletFlag = false;
                                            }
                                            else {
                                                amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT");
                                                walletUsage = amount + amt_convenience - cashback_amt_total - amt_discount;
                                                walletBal = walletAmount - walletUsage;
                                                updateDetails("WALLET");
                                                if(!payByWalletButton.isShown()) {
                                                    payByWalletButton.setVisibility(View.VISIBLE);
                                                    pagerContainerLayout.setVisibility(View.GONE);
                                                }
                                                walletFlag = true;
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        walletBalance.setText("Remaining bal: " + round(walletBal, 2));

                                    }
                                    else {
                                        updateDetails(mode);
                                    }


                                    ((TextView) findViewById(R.id.selectCoupon)).setText(R.string.view_coupon);
                                    (findViewById(R.id.selectCoupon1)).setVisibility(View.GONE);


                                    savings.setText("Savings : Rs." + round((float) (amt_discount / 100) * 100, 2));
                                    savings.setVisibility(View.VISIBLE);


                                } else //If remove is not in text i.e. u are adding some coupons
                                {
                                    QustomDialogBuilder alertDialog = new QustomDialogBuilder(HomeActivity.this, R.style.PauseDialog);

                                    View convertView = (View) getLayoutInflater().inflate(R.layout.coupon_list, null);

                                    alertDialog.setView(convertView);

                                    couponList = (ListView) convertView.findViewById(R.id.lv);
                                    couponList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                                    couponList.setAdapter(coupanAdapter);
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
                                                    /******Apply the coupons*********/
                                                    couponListItem = (JSONObject) couponList.getAdapter().getItem(j);
                                                    try {
                                                        choosedCoupan = couponListItem.getString("couponString");
                                                        choosedItem = j;
                                                        coupan_amt = couponListItem.getDouble("amount");
                                                        Log.i("Choosed coupan", choosedCoupan);
                                                        amt_discount = coupan_amt;
                                                        if (walletCheck.isChecked()) {

                                                               /* walletUsage = walletUsage + amt_discount - coupan_amt;
                                                            walletBal = walletAmount - walletUsage;
                                                            if(walletBal < 0.0)
                                                            {
                                                                walletBal = 0.0;
                                                                walletUsage = walletAmount;
                                                            }
                                                            walletBalance.setText("Remaining bal: " + round(walletBal, 2));*/
                                                            if (amount + new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT") - cashback_amt_total - amt_discount <= walletAmount) {

                                                                amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT");

                                                                walletUsage = amount + amt_convenience - cashback_amt_total - amt_discount;
                                                                walletBal = walletAmount - walletUsage;
                                                                updateDetails("WALLET");
                                                                payByWalletButton.setVisibility(View.VISIBLE);//bugfix
                                                                pagerContainerLayout.setVisibility(View.GONE);
                                                                walletFlag = true;

                                                            } else if ((amount + amt_convenience - cashback_amt_total - amt_discount) > walletAmount) {

                                                                walletUsage = walletAmount;
                                                                walletBal = 0.0;

                                                                updateDetails(mode);
                                                                walletFlag = false;
                                                                payByWalletButton.setVisibility(View.GONE);//bugfix
                                                                pagerContainerLayout.setVisibility(View.VISIBLE);
                                                            } else {//wallet is fat enough to pay

                                                                amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT");

                                                                walletUsage = amount + amt_convenience - cashback_amt_total - amt_discount;
                                                                walletBal = walletAmount - walletUsage;
                                                                updateDetails("WALLET");
                                                                payByWalletButton.setVisibility(View.VISIBLE);//bugfix
                                                                pagerContainerLayout.setVisibility(View.GONE);
                                                                walletFlag = true;
                                                            }


                                                            walletBalance.setText("Remaining bal: " + round(walletBal, 2));

                                                        } else {

                                                            updateDetails(mode);
                                                        }


                                                        if (amount + new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT") - amt_discount - cashback_amt_total <= 0.0) {

                                                            pointDialog();
                                                        }


                                                        /*amt_discount = discountCashback;
                                                        if (amount + new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT") - amt_discount - cashback_amt_total > walletAmount) {
                                                            walletFlag = false;
                                                            amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT");

                                                            walletUsage = walletAmount;
                                                            walletBal = 0.0;
                                                            updateDetails("WALLET");

                                                            pagerContainerLayout.setVisibility(View.VISIBLE);
                                                            payByWalletButton.setVisibility(View.GONE);
                                                            walletBalance.setText("Remaining bal: " + round (walletBal ,2));

                                                        } else {

                                                        amt_convenience



                                                        }*/

                                                        if (amount - amt_discount == 0.0) //100% Coupon discount
                                                        {
                                                            if (payByWalletButton.isShown()) {
                                                                mAmount.setText(" " + round((amt_convenience), 2));
                                                                walletUsage = (walletAmount - amt_net);
                                                                walletBal = walletAmount - walletUsage;
                                                                walletBalance.setText("Remaining bal: " + round((float) (walletBal / 100) * 100, 2));
                                                            } else {
                                                                mAmount.setText(" " + 0.0);
                                                            }
                                                        }






                                                        /*if (payByWalletButton.isShown() && amt_net > 0.0 && walletBal == 0.0) {

                                                            pagerContainerLayout.setVisibility(View.VISIBLE);
                                                            payByWalletButton.setVisibility(View.GONE);

                                                        }*///bugfix

                                                        //Not enough but has some
                                                        String coupon_string = couponListItem.getString("couponStringForUser") + " Applied";
                                                        ((TextView) findViewById(R.id.selectCoupon1)).setText(coupon_string);
                                                        (findViewById(R.id.selectCoupon1)).setVisibility(View.VISIBLE);
                                                        ((TextView) findViewById(R.id.selectCoupon)).setText(R.string.remove);
                                                        (findViewById(R.id.selectCoupon)).setVisibility(View.VISIBLE);

                                                        savings.setText("Savings : Rs." + round((float) (amt_discount / 100) * 100, 2));
                                                        savings.setVisibility(View.VISIBLE);
                                                        break;
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                } else {

                                                }


                                            }

                                        }

                                    });

                                    alertDialog.show();

                                    couponList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                            adapter.notifyDataSetChanged();

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
                } catch (JSONException e) {

                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateDetails(String m) {


        try {
            if(m.equals("") && walletCheck.isChecked() )
                amt_convenience =new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT");
            else if(m.equals(""))
                amt_convenience = 0.0;
            else
            amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject(m).getDouble("DEFAULT");
            if (coupan_amt > 0.0)
                amt_discount = coupan_amt;
            else /*if (!details.getString("cashbackAccumulated").toString().equals("null"))*/
                amt_discount = discountCashback;//details.getJSONObject("cashbackAccumulated").getDouble(Constants.AMOUNT);//can be commented?
            amt_net = amount + amt_convenience - amt_discount - walletUsage - cashback_amt_total;
            if(amt_net < 0.0)
                amt_net = 0.0;//bugfix
            mAmount.setText(" " + round((amt_net), 2));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public String getMid() {
        return mid;
    }

    public String getAmt() {
        return amt;
    }

    public JSONObject getBankObject() {
        return details;
    }

    public Double getPoints() {
        try {
            if (points != null && points.has("cashback") && !points.isNull("cashback")) {
                JSONObject tempCashback = points.getJSONObject("cashback");
                if(tempCashback.has("availableAmount") && !tempCashback.isNull("availableAmount")) {
                    Double tempPoints = tempCashback.optDouble("availableAmount", 0.0);
                    return tempPoints;
                }
                else
                    return 0.0;
            }
            else
                return 0.0; //No points
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) //When HomeActivity resumes/starts
    {
        if (requestCode == LOGIN) {
            if (resultCode == RESULT_OK) {
                check_login();
            } else if (resultCode == LoginActivity.RESULT_QUIT) {
                check_login();
            } else if (resultCode == RESULT_CANCELED) {
                check_login();
                //Write your code if there's no result
            }
        } else if (requestCode == WEB_VIEW) //Coming back from making a payment
        {
            if (resultCode == RESULT_OK) //Success
            {
                Log.i("payment_status", "success");
                setResult(RESULT_OK, data);
                finish();
            } else if (resultCode == RESULT_CANCELED) //Fail
            {
                Log.i("payment_status", "failure");
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
                Log.i("login_status", "success");
                check_login();
            } else if (resultCode == RESULT_CANCELED) {
                Log.i("payment_status", "failure");

                check_login();
            }
        }
    }//onActivityResult

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            logout();
        } else if (item.getItemId() == R.id.add_account) {
            mAccountManager = AccountManager.get(getApplicationContext());
            addnewaccount();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Function to add new account
     *
     * @param menu
     * @return
     */

    public void logout() {
        Session.getInstance(getApplicationContext()).logout("");
        SharedPreferences.Editor edit = getSharedPreferences(Constants.SP_SP_NAME, Activity.MODE_PRIVATE).edit();
        edit.clear();
        edit.commit();
        Cards.getInstance(getApplicationContext()).deleteAll();
        Users.getInstance(getApplicationContext()).deleteAll();


       finish();//bug fixes
    }

    public void addnewaccount() {
        // TODO: add account
        try {
            mAccountManager.addAccount(
                    "com.payUMoney.sdk.auth.account",
                    "bearer",
                    null,
                    new Bundle(),
                    this,
                    new OnAccountAddComplete(),
                    null);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error adding account", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void goToPayment(String mode, HashMap<String, Object> data) throws JSONException {

        hideKeyboardIfShown();
        setContentView(R.layout.layout);
        mProgressDialog = showProgress(this);
        Session.getInstance(this).sendToPayUWithWallet(details, mode, data, cashback_amt_total, walletUsage);

    }

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

    /**
     * Call back handler for when account is added
     *
     * @param menu
     * @return
     */
    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error adding account", Toast.LENGTH_LONG).show();
                return;
            } catch (AuthenticatorException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error adding account", Toast.LENGTH_LONG).show();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            mAccount = new Account(
                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
            );
            Log.d("main", "Added account " + mAccount.name + ", fetching");
            Toast.makeText(getApplicationContext(), mAccount.name.toString() + " Added", Toast.LENGTH_LONG).show();
            logout();

            //Start fetch of the new account
            // startAuthTokenFetch();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (show)
            menu.add(Menu.NONE, R.id.logout, menu.size(), R.string.logout).setIcon(R.drawable.logout).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        //    menu.add(Menu.NONE, R.id.add_account, menu.size(), "Add Account").setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }

    public void onEventMainThread(final CobbocEvent event) //Bus Function
    {
        if (event != null) {
            if (event.getType() == CobbocEvent.LOGOUT) {
                if (event.getValue() != null) {
                    if (event.getValue().equals("force")) {
                        Toast.makeText(this, R.string.inactivity, Toast.LENGTH_LONG).show();
                        SharedPreferences.Editor edit = getSharedPreferences(Constants.SP_SP_NAME, Activity.MODE_PRIVATE).edit();
                        edit.clear();
                        edit.commit();
                        Cards.getInstance(getApplicationContext()).deleteAll();
                        Users.getInstance(getApplicationContext()).deleteAll();
                        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                        intent.putExtra(Constants.AMOUNT, getIntent().getStringExtra(Constants.AMOUNT));
                        intent.putExtra(Constants.PARAMS, getIntent().getSerializableExtra(Constants.PARAMS));
                        intent.putExtra(Constants.USER_EMAIL, getIntent().getStringExtra(Constants.USER_EMAIL));
                        intent.putExtra(Constants.USER_PHONE, getIntent().getStringExtra(Constants.USER_PHONE));
                        intent.putExtra("force", "force");
                        startActivityForResult(intent, LOGIN);
                    }
                }
                // clear the token stored in SharedPreferences
            } else if (event.getType() == CobbocEvent.USER_POINTS) //Add wallet points
            {
                Log.d("Sagar", "Entered in User Points");
                if (event.getStatus()) {
                    try {
                        walletJason = (JSONObject) event.getValue();
                        points = walletJason;
                        walletAmount = walletJason.getJSONObject("wallet").getDouble("availableAmount");

                        if (walletAmount > 0.0) {
                            //   wallettext.setVisibility(View.VISIBLE);
                            walletCheck.setVisibility(View.VISIBLE);
                            walletBoxLayout.setVisibility(View.VISIBLE);
                            walletText.setText("Initial bal: " + walletAmount);
                            Log.d("Sagar", "Exited from  User Points");
                        }
                    } catch (Exception e) {
                        Log.d("Exception while getting wallet detials", e.toString());
                    }
                } else {
                   
                }
            } else if (event.getType() == CobbocEvent.CREATE_PAYMENT)  //New Payment
            {
                Log.d("Sagar", "Entered in Create Payment");
                if (event.getStatus()) {
                    try {
                        JSONObject result = (JSONObject) event.getValue();
                        JSONObject paymentOfferDTO = result.getJSONObject("paymentOfferDTO");

                        if (!paymentOfferDTO.getString("newCashBackAmount").equals("-1.0")) {
                            // modifiedDiscount = paymentOfferDTO.getJSONObject("amount").getDouble(Constants.AMOUNT);
                            String s =  paymentOfferDTO.getString("newCashBackAmount");
                            discountCashback = Double.parseDouble(s);
                        }
                        else if(!paymentOfferDTO.getString("amount").equals("null")) {
                            // modifiedDiscount = paymentOfferDTO.getJSONObject("amount").getDouble(Constants.AMOUNT);
                            String s =  paymentOfferDTO.getString("amount");
                            discountCashback = Double.parseDouble(s);
                        }

                        Session.getInstance(this).getPaymentDetails(result.getString(Constants.PAYMENT_ID)); //Fire getpaymentdetails of session
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                 //   Session.getInstance(this).getPaymentDetails((String)event.getValue());
                    Log.d("Sagar", "exited from User Points");
                } else {
                    dismissProgress();
                    Toast.makeText(this, "Some error occurred! Try again", Toast.LENGTH_LONG).show();
                }
            } else if (event.getType() == CobbocEvent.PAYMENT_DETAILS)  // Payment details (CC DC PayuP inquired) at session
            {
                if (event.getStatus()) {
                    // so we got payment details.
                    // now we'll ask the user about his choice of payment mode.
                    details = (JSONObject) event.getValue();
                    Log.d("Sagar", "Entered in Payment details");
                    startPayment(map);//Get the Json Object attached with the post of event Payment_details

                } else {
                    dismissProgress();
                    Toast.makeText(this, "Some error occurred! Try again", Toast.LENGTH_LONG).show();
                }
            } else if (event.getType() == CobbocEvent.PAYMENT_POINTS) {
                if (event.getStatus()) {

                    Intent intent = new Intent(this, WebViewActivityPoints.class);
                    intent.putExtra(Constants.RESULT, event.getValue().toString());
                    this.startActivityForResult(intent, this.WEB_VIEW);


                } else {

                }
            }//sagar_start
            else if (event.getType() == CobbocEvent.PAYMENT) {
                dismissProgress();
                if (event.getStatus()) {
                    Log.i("reached", "credit");
                    Intent intent = new Intent(this, WebViewActivity.class);
                    intent.putExtra(Constants.RESULT, event.getValue().toString());
                    this.startActivityForResult(intent, this.WEB_VIEW);
                } else {
                    Log.i("reached", "failed");
                    //If not status do nothing
                    Toast.makeText(this, "Payment Failed", Toast.LENGTH_LONG).show();
                }
            }

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
        } else {
            Toast.makeText(getApplicationContext(), "Press Back again to cancel transaction", Toast.LENGTH_LONG).show();
        }

    }


    public void close() {
        Intent intent = new Intent();
        intent.putExtra(Constants.RESULT, "cancel");
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onDestroy() {
        coupan_amt = 0.0;
        super.onDestroy();

    }

    public void unchecked() {
        try {
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
        }
        walletBalance.setVisibility(View.GONE);
        walletText.setVisibility(View.GONE);
        walletUsage = 0.0;
        walletBal = walletAmount;
        updateDetails(mode);
        if (walletFlag) //Wallet is fatter
        {
            if (payByWalletButton.isShown()) {
                // availableModes.remove("wallet");



                payByWalletButton.setVisibility(View.GONE);
                pagerContainerLayout.setVisibility(View.VISIBLE);
                // amt_convenience=0.0;//sagar
            }
        }
    }

    public void walletDialog() {
        amt_net = walletUsage;
        if (coupan_amt > 0.0)
            amt_discount = coupan_amt;
        if (cashback_amt_total > 0.0)
            showWalletwithPayu(cashback_amt_total, amt_discount, amt_net);
        else if (cashback_amt_total == 0.0)
            showWallet(amt_discount, amt_net);
        else
            Toast.makeText(this, "Something went Wrong", Toast.LENGTH_LONG).show();

    }

    public void showWallet(double dsc, final double net) {
        new QustomDialogBuilder(this, R.style.PauseDialog).
                setTitleColor(Constants.greenPayU).
                setDividerColor(Constants.greenPayU)
                .setTitle("Payment using Wallet")
                .setMessage("Yoo-hoo!\n" +
                        "\n" +
                        "You have enough money in PayUMoney Wallet for this transaction. All you need to do is confirm the payment by clicking on the OK button below and that's it.\n\nOrder Amount : Rs." + amount + "\nConvenient Fees: " + amt_convenience + "\nCashback/PayUPoints used : Rs." + 0 + "\nDiscount : Rs." + round(dsc, 2) + "\nWallet Money Used : Rs." + round(net, 2) + "\nRemaining Money in Wallet : Rs." + round(walletBal , 2))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Your code
                        try {

                            mProgressDialog.show();
                            Session.getInstance(HomeActivity.this).sendToPayU(details, "wallet", data, net); //PURE WALLEt
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).setCancelable(false)
                .show();
    }

    public void showWalletwithPayu(final double pnts, double dsc, final double net) {
        new QustomDialogBuilder(this, R.style.PauseDialog).
                setTitleColor(Constants.greenPayU).
                setDividerColor(Constants.greenPayU)
                .setTitle("Payment using Wallet")
                .setMessage("Yoo-hoo!\n" +
                        "\n" +
                        "You have enough money in PayUMoney Wallet for this transaction. All you need to do is confirm the payment by clicking on the OK button below and that's it.\n\nOrder Amount : Rs." + amount + "\nConvenient Fees: " + amt_convenience + "\nCashback/PayUPoints used : Rs." + pnts + "\nDiscount : Rs." + round(dsc, 2) + "\nWallet Money Used : Rs." + round(net, 2) + "\nRemaining Money in Wallet : Rs." + round(walletBal , 2))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Your code
                        try {

                            Session.getInstance(HomeActivity.this).sendToPayUWithWallet(details, "wallet", data, net, pnts); //wallet +pnts
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).setCancelable(false)
                .show();
    }

    public void pointDialog() //If user has PayUpoints
    {

        dismissProgress();
        mAmount.setText("0.0");
        savings.setText("Sufficient PayUPoints");
        walletBoxLayout.setVisibility(View.GONE);
        mAmoutDetails.setVisibility(View.GONE);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Payment using PayUMoney points")
                .setMessage("Yoo-hoo!\n" +
                        "\n" +
                        "You have enough PayUMoney points for this transaction. All you need to do is confirm the payment by clicking on the OK button below and that's it")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // Your code
                        PayUMoneyPointsFragment fragment = new PayUMoneyPointsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("details", details.toString());
                        bundle.putDouble("cashback_amt_total", cashback_amt_total);
                        bundle.putDouble("discount", amt_discount);

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
                .show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    FragmentManager fragmentManager = getFragmentManager();
                    android.app.Fragment tempFragment = fragmentManager.findFragmentByTag("paymentOptions");
                    if (tempFragment != null) {
                        Intent intent = new Intent();
                        intent.putExtra(Constants.RESULT, "cancel");
                        setResult(RESULT_CANCELED, intent);
                        finish();
                    }

                }
                return true;

            }
        });


    }

/*    //Two round off ;)
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public static float round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }*/

    //Two round off ;)
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
        final Drawable[] drawables = {getResources().getDrawable(R.drawable.nopoint),
                getResources().getDrawable(R.drawable.onepoint),
                getResources().getDrawable(R.drawable.twopoint),
                getResources().getDrawable(R.drawable.threepoint)
        };

        View layout = mInflater.inflate(R.layout.prog_dialog, null);
        final ImageView imageView;
        imageView = (ImageView) layout.findViewById(R.id.imageView);
        ProgressDialog progDialog = new ProgressDialog(context, R.style.ProgressDialog);
        timer = new Timer();

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
           // mProgressDialog.cancel();
            mProgressDialog.dismiss();
            mProgress = false;
        }

    }


}
