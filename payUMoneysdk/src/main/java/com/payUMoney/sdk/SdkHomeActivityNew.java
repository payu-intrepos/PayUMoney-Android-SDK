package com.payUMoney.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.telephony.gsm.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
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
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.payUMoney.sdk.adapter.SdkCouponListAdapter;
import com.payUMoney.sdk.adapter.SdkExpandableListAdapter;
import com.payUMoney.sdk.adapter.SdkStoredCardAdapter;
import com.payUMoney.sdk.dialog.SdkOtpProgressDialog;
import com.payUMoney.sdk.dialog.SdkQustomDialogBuilder;
import com.payUMoney.sdk.fragment.SdkDebit;
import com.payUMoney.sdk.fragment.SdkNetBankingFragment;
import com.payUMoney.sdk.fragment.SdkPayUMoneyPointsFragment;
import com.payUMoney.sdk.fragment.SdkStoredCardFragment;
import com.payUMoney.sdk.utils.SdkHelper;
import com.payUMoney.sdk.utils.SdkLogger;
import com.payUMoney.sdk.walledSdk.SharedPrefsUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

public class SdkHomeActivityNew extends FragmentActivity implements SdkDebit.MakePaymentListener, SdkNetBankingFragment.MakePaymentListener, SdkStoredCardAdapter.MakePaymentListener, SdkStoredCardFragment.MakePaymentListener {

    private String device_id = null, currentVersion = null;
    final int LOGIN = 1;
    public final int WEB_VIEW = 2;
    public final int SIGN_UP = 7;
    public final int LOAD_AND_PAY_USING_WALLET = 9;
    //    public final int RESULT_BACK = 8;
    private final int PAYMNET_CANCELLED = 21;
    private final int PAYMNET_LOGOUT = 22;
    //    public final int RESULT_FAILED = 90;
    public TextView mAmount = null, savings = null, mOrderSummary = null, mCvvTnCLink, resend, info;
    private SdkSession sdkSession = null;
    int count = 0;
    private ProgressDialog mProgressDialog = null;
    private HashMap<String, String> map = null;
    public double walletUsage = 0.0, walletAmount = 0.0, userPoints = 0.0,
            amt_convenience = 0.0,
            amt_net = 0.0,
            amount = 0.0,
            cashback = 0.0,
            discount = 0.0,
            amt_discount = 0.0,
            amt_convenience_wallet = 0.0,
            walletBal = 0.0,
            loadWalletMinLimit = 0.00,
            loadWalletMaxLimit = 10000.00;
    public static double coupan_amt = 0.0/*,choosedItem = 0.0*/;//Undo
    private CheckBox walletCheck = null;
    private LinearLayout walletBoxLayout = null;
    private LinearLayout couponLayout = null;
    private TextView walletBalance = null, applyCoupon = null;
    private Button payByWalletButton = null;
    private boolean walletFlag = false;
    private HashMap<String, Object> data = new HashMap<>();
    private ArrayList<String> availableModes, availableDebitCards, availableCreditCards = null;
    String mode = SdkConstants.WALLET_STRING;
    private SdkCouponListAdapter coupanAdapter = null;
    private ListView couponList = null;
    public static String choosedCoupan = null;
    public JSONArray storedCardList = null, mCouponsArray = null;
    private boolean mProgress = false;
    private boolean chooseOtherMode = false;
    private boolean guestCheckOut = false;
    private String quickLogin = "", allowGuestCheckout = "";
    private boolean fromPayUMoneyApp, mInternalLoadWalletCall = false, fromPayUBizzApp = false;
    private JSONObject appResponse, couponListItem, walletJason, details, paymentOption, convenienceChargesObject, user, mNetBankingStatusObject;
    private String key = null;
    private ExpandableListView paymentModesList = null;
    private boolean isAnotherGroupExpanding = false;
    private SdkExpandableListAdapter listAdapter = null;
    private String paymentId = "";
    private String cardHashForOneClickTxn = null;
    private boolean userParamsFetchedExplicitely = false;
    private Button verifyCouponBtn = null, proceed, anotherAccountButton;
    private EditText mannualCouponEditText = null, mobileEditText, OTPEditText;
    private RelativeLayout verifyCouponProgress = null, humble;
    private boolean manualCouponEntered = false;
    private LinearLayout whenHideChooseCouponLayoutRequired;
    private AlertDialog splashDialog;
    private AlertDialog.Builder alertDialogBuilder;
    private boolean loadWalletCall = false, mOnlyWalletPaymentModeActive = false, walletActive = false, pointsActive = false, firstTimeFetchingOneClickFlag = false, mFrontPaymentModesEnabled = true, mNeedToShowOneTapCheckBox = false, mWalletRecentlyVerified = false, mOTPAutoRead = false, mIsUserWalletBlocked = false, mIsLoginInitiated = false;
    private String manualCouopnNameString;
    private CheckBox mOneTap;
    private ProgressBar progressBarWaitOTP;
    private AlertDialog OTPVerificationdialog;
    private BroadcastReceiver receiver = null;
    private Pattern otpPattern = Pattern.compile("(|^)\\d{6}");

    public String getUserId() {
        return userId;
    }

    private String userId;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        sdkSession = SdkSession.getInstance(getApplicationContext());
        initiateProgressDialog();
        device_id = SdkHelper.getAndroidID(this);
        currentVersion = SdkHelper.getAppVersion(this);

        availableModes = new ArrayList<>();
        availableCreditCards = new ArrayList<>();
        availableDebitCards = new ArrayList<>();

        try {
            if (SdkConstants.WALLET_SDK) {
                details = new JSONObject(getIntent().getStringExtra(SdkConstants.PAYMENT_DETAILS_OBJECT));
                initLayout();
            } else {
                map = (HashMap<String, String>) getIntent().getSerializableExtra(SdkConstants.PARAMS);
                map.put(SdkConstants.DEVICE_ID, device_id);
                map.put(SdkConstants.APP_VERSION, currentVersion);

                if (map.containsKey(SdkConstants.INTERNAL_LOAD_WALLET_CALL)) {
                    mInternalLoadWalletCall = true;
                    appResponse = new JSONObject(map.get(SdkConstants.INTERNAL_LOAD_WALLET_CALL));
                    loadWalletCall = true;
                    if (map.containsKey(SdkConstants.NEED_TO_SHOW_ONE_TAP_CHECK_BOX)) {
                        mNeedToShowOneTapCheckBox = false;
                    }
                } else if (map.containsKey(SdkConstants.PAYUMONEY_APP)) {
                    fromPayUMoneyApp = true;
                    mNeedToShowOneTapCheckBox = true;
                    appResponse = new JSONObject(map.get(SdkConstants.PAYUMONEY_APP));
                    if (map.containsKey(SdkConstants.PAYMENT_TYPE)) {
                        if (map.get(SdkConstants.PAYMENT_TYPE).equals(SdkConstants.LOAD_WALLET))
                            loadWalletCall = true;
                    }
                } else if (map.containsKey(SdkConstants.PAYUBIZZ_APP)) {
                    fromPayUBizzApp = true;
                    JSONObject tmp = new JSONObject(map.get(SdkConstants.PAYUBIZZ_APP));
                    appResponse = tmp.getJSONObject(SdkConstants.RESULT);
                    if (!sdkSession.isLoggedIn()) {
                        if (appResponse.has(SdkConstants.CONFIG_DATA) && !appResponse.isNull(SdkConstants.CONFIG_DATA)) {
                            JSONObject merchantLoginParams = appResponse.getJSONObject(SdkConstants.CONFIG_DATA);
                            allowGuestCheckout = merchantLoginParams.optString(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_VALUE, "");
                            String temp = merchantLoginParams.optString("quickLoginEnabled", "");
                            if (!temp.isEmpty() && temp.equals("true"))
                                quickLogin = "1";
                            else
                                quickLogin = "0";
                        }
                        check_login();
                        return;
                    } else if (appResponse != null) {
                        initLayout();
                        return;
                    }
                }
                if (fromPayUMoneyApp) {
                    if (appResponse != null) {
                        initLayout();
                    }
                } else if (mInternalLoadWalletCall) {
                    if (appResponse != null) {
                        initLayout();
                    }
                } else {
                    if (!sdkSession.isLoggedIn()) {
                        sdkSession.fetchMechantParams(map.get(SdkConstants.MERCHANT_ID));
                    } else {
                        check_login();
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getPaymentId() {
        return paymentId;
    }

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
                if (mOneTap.getVisibility() == View.GONE) {
                    mOneTap.setVisibility(View.VISIBLE);
                    mCvvTnCLink.setVisibility(View.VISIBLE);

                    if (payByWalletButton.getVisibility() == View.VISIBLE) {
                        mOneTap.setVisibility(View.GONE);
                        mCvvTnCLink.setVisibility(View.GONE);
                    }
                }
                // Collapse previous parent if expanded.
                if ((previousGroup != -1) && (groupPosition != previousGroup)) {
                    isAnotherGroupExpanding = true;
                    paymentModesList.collapseGroup(previousGroup);
                    isAnotherGroupExpanding = false;
                }
                previousGroup = groupPosition;
                Object object = listAdapter.getGroup(groupPosition);
                if (object != null) {
                    String currentGroup = object.toString();
                    if (currentGroup != null) {
                        if (currentGroup.equals(SdkConstants.PAYMENT_MODE_STORE_CARDS))
                            mode = "";
                        if (currentGroup.equals(SdkConstants.PAYMENT_MODE_DC)) {

                            mode = SdkConstants.PAYMENT_MODE_DC;
                    /*sdkFragmentLifecycleNew = (SdkFragmentLifecycleNew) adapter.getItem(groupPosition);
                    sdkFragmentLifecycleNew.onResumeFragment(SdkHomeActivityNew.this);*/

                        }
                        if (currentGroup.equals(SdkConstants.PAYMENT_MODE_CC)) {

                            mode = SdkConstants.PAYMENT_MODE_CC;
                    /*sdkFragmentLifecycleNew = (SdkFragmentLifecycleNew) adapter.getItem(groupPosition);
                    sdkFragmentLifecycleNew.onResumeFragment(SdkHomeActivityNew.this);*/

                        }
                        if (currentGroup.equals(SdkConstants.PAYMENT_MODE_NB)) {
                            mode = SdkConstants.PAYMENT_MODE_NB;
                            mOneTap.setVisibility(View.GONE);
                            mCvvTnCLink.setVisibility(View.GONE);
                        }
                    }
                }

                if (guestCheckOut) {
                    mOneTap.setVisibility(View.GONE);
                    mCvvTnCLink.setVisibility(View.GONE);
                }

                updateDetails(mode);
            }
        });
        //if(listAdapter.getGroup(0).toString().equals(SdkConstants.STORE_CARDS))
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
        StringBuffer message = new StringBuffer("Order Amount : Rs." + round((float) (amount / 100) * 100));

        if (amt_convenience > 0.0) {
            message.append("\nConvenience Fee : Rs.").append(round((float) (amt_convenience / 100) * 100)).append("\nTotal : Rs.").append(round((float) ((amt_convenience + amount) / 100) * 100));
        } else
            message.append("\nTotal : Rs.").append(round((float) ((amount) / 100) * 100));


        if (amt_discount > 0.0) {
            if (coupan_amt > 0.0) {
                message.append("\nCoupon Discount : Rs.").append(round((float) (amt_discount / 100) * 100));
            } else {
                message.append("\nDiscount : Rs.").append(round((float) (amt_discount / 100) * 100));
            }
        } else if (cashback > 0.0 && !(coupan_amt > 0.0)) {
            message.append("\nCashback : Rs.").append(round((float) (cashback / 100) * 100));
        }
        if (userPoints > 0.0) {
            message.append("\nAvailable PayUMoney points : Rs.").append(round((float) (userPoints / 100) * 100));
        }

        message.append("\nNet Amount : Rs.").append(round((float) (amt_net * 100) / 100));

        if (walletUsage > 0.0) {
            message.append("\nWallet Usage: Rs.").append(round(walletUsage));
        }
        qb.setMessage(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (EventBus.getDefault() != null && !EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        SharedPrefsUtils.setStringPreference(this, SdkConstants.USER_SESSION_COOKIE_PAGE_URL, this.getClass().getSimpleName());
    }

    @Override
    public void onStop() {
        super.onStop();

        try {
            if (receiver != null) {
                unregisterReceiver(receiver);
                receiver = null;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void check_login() {     // Function to check login and if yes then initiate the payment

        SdkLogger.d(SdkConstants.TAG, "entered in check login()");
        if (!sdkSession.isLoggedIn() && !guestCheckOut)  //Not logged in
        {

            if (!sdkSession.isLoggedIn() && !mIsLoginInitiated) {
                dismissProgress();
                mIsLoginInitiated = true;
                Intent intent = new Intent(SdkHomeActivityNew.this, SdkLoginSignUpActivity.class);
                intent.putExtra(SdkConstants.AMOUNT, getIntent().getStringExtra(SdkConstants.AMOUNT));
                intent.putExtra(SdkConstants.MERCHANTID, getIntent().getStringExtra(SdkConstants.MERCHANTID));
                intent.putExtra(SdkConstants.PARAMS, getIntent().getSerializableExtra(SdkConstants.PARAMS));
                intent.putExtra(SdkConstants.EMAIL, getIntent().getStringExtra(SdkConstants.EMAIL));
                intent.putExtra(SdkConstants.PHONE, getIntent().getStringExtra(SdkConstants.PHONE));
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
        } else { //logged in already
            initLayout();
        }
    }

    public void initLayout() {

        initHomeLayout();
        invalidateOptionsMenu();

        //  handleCvvLessTransaction();
        initiateProgressDialog();

        //Fetch and save netBanking status for all banks
        SdkSession.getInstance(this).getNetBankingStatus();

        if (fromPayUMoneyApp || fromPayUBizzApp || SdkConstants.WALLET_SDK || mInternalLoadWalletCall)
            startPayment(appResponse);
        else
            sdkSession.createPayment(map);//Create  event fired when Parent Activity is created
    }

    private void initHomeLayout() {
        setContentView(R.layout.sdk_activity_home_new);
        mAmount = ((TextView) findViewById(R.id.sdkAmountText));
        savings = (TextView) findViewById(R.id.savings);
        mOrderSummary = (TextView) findViewById(R.id.orderSummary);
        walletBoxLayout = (LinearLayout) findViewById(R.id.walletLayout);
        walletCheck = (CheckBox) walletBoxLayout.findViewById(R.id.walletcheck);
        //walletText = (TextView) walletBoxLayout.findViewById(R.id.wallettext);
        walletBalance = (TextView) walletBoxLayout.findViewById(R.id.walletbalance);
        couponLayout = (LinearLayout) findViewById(R.id.couponSection);
        applyCoupon = (TextView) couponLayout.findViewById(R.id.selectCoupon);

        if (mNeedToShowOneTapCheckBox) {
            findViewById(R.id.user_profile_is_cvv_less_layout).setVisibility(View.GONE);
        }

        mOneTap = (CheckBox) findViewById(R.id.user_profile_is_cvv_less_checkbox);
        mCvvTnCLink = (TextView) findViewById(R.id.cvv_tnc_link);
        mCvvTnCLink.setMovementMethod(LinkMovementMethod.getInstance());

        mOneTap.setOnClickListener((new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckedChanged(mOneTap.isChecked());
            }

            public void onCheckedChanged(boolean isChecked) {
                if (isChecked) {
                    sdkSession.enableOneClickTransaction("1");

                    //item.setTitle("Disable One Tap Payment");
                } else {
                    sdkSession.enableOneClickTransaction("0");
                    // item.setTitle("Enable One Tap Payment");
                }
            }
        }));

        paymentModesList = (ExpandableListView) findViewById(R.id.lvExp);
        payByWalletButton = (Button) findViewById(R.id.PayByWallet);
        payByWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SdkHelper.isValidClick()
                        && payByWalletButton != null
                        && payByWalletButton.getText() != null
                        && payByWalletButton.getText().toString() != null
                        && getString(R.string.pay_using_wallet).equals(payByWalletButton.getText().toString())) {
                    walletDialog();
                } else {
                    loadWalletDialog();
                }
            }
        });
        walletCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {

                    if (amount + amt_convenience_wallet - userPoints - amt_discount <= walletAmount) { //Wallet is fatter so pay from it.

                        amt_convenience = amt_convenience_wallet;
                        walletBal = (walletAmount - (amt_convenience + amount - amt_discount - userPoints));
                        walletBalance.setText(getString(R.string.remaining_wallet_bal) + " " + round(walletBal));
                        walletUsage = walletAmount - walletBal;
                        updateDetails(SdkConstants.WALLET_STRING);
                        //wallet is fatter hence pay by wallet
                        walletFlag = true;
                        paymentModesList.setVisibility(View.GONE);
                        payByWalletButton.setVisibility(View.VISIBLE);
                        payByWalletButton.setText(R.string.pay_using_wallet);
                        if (mOneTap != null && mOneTap.getVisibility() == View.VISIBLE) {
                            mOneTap.setVisibility(View.GONE);
                            mCvvTnCLink.setVisibility(View.GONE);
                        }
                    } else {//Wallet is smaller, remove wallet amount from net discounted amount

                        walletFlag = false;
                        walletUsage = walletAmount;
                        walletBal = 0.0;
                        updateDetails(mode);
                        walletBalance.setText(getString(R.string.remaining_wallet_bal) + " " + 0.0);

                        //Eanbling LoadWallet Flow
                        if (!mFrontPaymentModesEnabled) {
                            walletFlag = true;
                            if (mOneTap != null && mOneTap.getVisibility() == View.VISIBLE) {
                                mOneTap.setVisibility(View.GONE);
                                mCvvTnCLink.setVisibility(View.GONE);
                            }
                            paymentModesList.setVisibility(View.GONE);
                            payByWalletButton.setVisibility(View.VISIBLE);
                            payByWalletButton.setText(R.string.load_and_pay);
                            updateDetailsForLoadWallet();
                        }
                    }

                    // Preventing UnCheck Operation once checked
                    walletCheck.setEnabled(false);
                } else {//NOT TICKED

                    if (mOneTap != null && mOneTap.getVisibility() != View.VISIBLE && (mode.equals(SdkConstants.PAYMENT_MODE_CC) || mode.equals(SdkConstants.PAYMENT_MODE_DC) || mode.isEmpty())) {
                        mOneTap.setVisibility(View.VISIBLE);
                        mCvvTnCLink.setVisibility(View.VISIBLE);
                    }
                    unchecked();
                }
            }
        });
        mOrderSummary.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleViewDetails();
            }
        });
        if (sdkSession.isLoggedIn()) {
            sdkSession.enableOneClickTransaction("-1");
            firstTimeFetchingOneClickFlag = true;
        }

    }

    public double getAmount() {
        return amount;
    }

    private void inflateSufficientDiscountLayout() {

        dismissProgress();
        paymentModesList.setVisibility(View.INVISIBLE);

        mAmount.setText("0.0");
        savings.setText("Sufficient Discount");
        walletBoxLayout.setVisibility(View.GONE);
        mOrderSummary.setVisibility(View.GONE);
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
        bundle.putBoolean("enoughDiscount", true);

        fragment.setArguments(bundle);
        FragmentTransaction transaction;
        transaction = getFragmentManager().beginTransaction().setCustomAnimations(
                R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                R.animator.card_flip_left_in, R.animator.card_flip_left_out);
        //((HomeActivity) SdkHomeActivityNew.this).onPaymentOptionSelected(details);
        transaction.replace(R.id.pagerContainer, fragment, "payumoneypoints");
        transaction.addToBackStack("a");
        transaction.commit();
        getFragmentManager().executePendingTransactions();
    }

    public void startPayment(JSONObject params) {//Intiate payment

        SdkLogger.d(SdkConstants.TAG, "Entered in Start Payment");

        try {
        /*Will be skipped in case of Wallet Sdk*/
            if (SdkConstants.WALLET_SDK) {

                applyCoupon.setVisibility(View.GONE);
                couponLayout.setVisibility(View.GONE);
                walletCheck.setVisibility(View.GONE);//CHECKED
                walletBoxLayout.setVisibility(View.GONE);
                savings.setVisibility(View.GONE);
                if (details.has(SdkConstants.USER) && !details.isNull(SdkConstants.USER)) {
                    if (!userParamsFetchedExplicitely)
                        user = details.getJSONObject(SdkConstants.USER);
                    if (details != null && details.has(SdkConstants.PAYMENT_ID) && !details.isNull(SdkConstants.PAYMENT_ID))
                        paymentId = details.getString(SdkConstants.PAYMENT_ID);
                    if (user.has(SdkConstants.USER_ID) && !user.isNull(SdkConstants.USER_ID))
                        userId = user.getString(SdkConstants.USER_ID);
                }
            }
            if ((fromPayUMoneyApp || fromPayUBizzApp || mInternalLoadWalletCall) && !chooseOtherMode) {
                details = params;
            }

            if (fromPayUMoneyApp) {
                mOTPAutoRead = true;
            }

            if (!mWalletRecentlyVerified) {
                checkForAvailablePaymentOptions();
                checkForWalletOnlyPaymentMode();
                checkForUserWalletActive();
            }

            if (details != null && details.has(SdkConstants.CONVENIENCE_CHARGES) && !details.isNull(SdkConstants.CONVENIENCE_CHARGES)) {
                convenienceChargesObject = new JSONObject(details.getString(SdkConstants.CONVENIENCE_CHARGES));
            }

            JSONObject tempPaymentOption = null;
            if (paymentOption == null && details.has(SdkConstants.PAYMENT_OPTION) && !details.isNull(SdkConstants.PAYMENT_OPTION)) {
                tempPaymentOption = details.getJSONObject(SdkConstants.PAYMENT_OPTION);
                if (tempPaymentOption != null && tempPaymentOption.has(SdkConstants.OPTIONS) && !tempPaymentOption.isNull(SdkConstants.OPTIONS)) {
                    paymentOption = tempPaymentOption.getJSONObject(SdkConstants.OPTIONS);
                }
            }

            if (availableModes != null && availableModes.size() == 0) {
                checkForAvailablePaymentOptions();
            }

            if (availableModes != null && availableModes.size() > 0 && !SdkConstants.PAYMENT_MODE_STORE_CARDS.equals(availableModes.get(0))
                    && user != null && user.has("savedCards") && !user.isNull("savedCards")) {
                availableModes.add(0, SdkConstants.PAYMENT_MODE_STORE_CARDS);
                JSONArray tempArr = user.getJSONArray("savedCards");
                setStoredCardList(tempArr);
            }

            if (tempPaymentOption != null && tempPaymentOption.has(SdkConstants.CONFIG) && !tempPaymentOption.isNull(SdkConstants.CONFIG)) {

                JSONObject tempConfig = tempPaymentOption.getJSONObject(SdkConstants.CONFIG);
                if (tempConfig != null && tempConfig.has("publicKey") && !tempConfig.isNull("publicKey")) {
                    key = tempConfig.getString("publicKey").replaceAll("\\r", "");
                }
            }

            if (mOnlyWalletPaymentModeActive) {
                availableModes.clear();
                coupan_amt = 0.0;
                applyCoupon.setVisibility(View.GONE);
                couponLayout.setVisibility(View.GONE);
            }

            if (availableModes != null && availableModes.size() > 0) {
                if (availableModes.get(0).equals(SdkConstants.PAYMENT_MODE_STORE_CARDS))
                    mode = "";//BugFixWalletConvenienceIssue
                else if (availableModes.get(0).equals(SdkConstants.PAYMENT_MODE_DC))
                    mode = SdkConstants.PAYMENT_MODE_DC;
                else if (availableModes.get(0).equals(SdkConstants.PAYMENT_MODE_CC))
                    mode = SdkConstants.PAYMENT_MODE_CC;
                else if (availableModes.get(0).equals(SdkConstants.PAYMENT_MODE_NB))
                    mode = SdkConstants.PAYMENT_MODE_NB;
            } else {
                mFrontPaymentModesEnabled = false;
            }

            setAmountConvenience(mode);

            amount = details.getJSONObject(SdkConstants.PAYMENT).getDouble(SdkConstants.ORDER_AMOUNT);

            if (!chooseOtherMode && !guestCheckOut)
                userPoints = this.getPoints().doubleValue(); //Get points user has

            if (coupan_amt > 0.0)
                amt_discount = coupan_amt;
            else //If No coupon
                amt_discount = discount;//Get discount offered

            if (amount + amt_convenience_wallet - amt_discount <= 0.0 && !chooseOtherMode) {
                inflateSufficientDiscountLayout();
            } else if (amount + amt_convenience_wallet - amt_discount - userPoints <= 0.0 && !chooseOtherMode) {
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

                if (fromPayUBizzApp) {
                    amount = getIntent().getDoubleExtra(SdkConstants.AMOUNT, 0.0);
                }
                mAmount.setText(" " + round((amt_net)));
                if (amt_discount > 0.0 || userPoints > 0.0) {
                    savings.setText("Savings : Rs." + (round((float) ((amt_discount + userPoints) / 100) * 100)));
                    savings.setVisibility(View.VISIBLE);
                } else {
                    savings.setVisibility(View.INVISIBLE);
                }

                createPaymentModesList();
                /*if(!chooseOtherMode)
                walletCheck.setChecked(true);*/
                /****COUPONS*****/
                //if (user.has(SdkConstants.COUPONS_STRING) && !user.isNull(SdkConstants.COUPONS_STRING)) {
                if (!SdkConstants.WALLET_SDK) {
                    updateCouponsVisibility();
                    if (coupan_amt <= 0.0) {
                        ((TextView) findViewById(R.id.selectCoupon)).setText(R.string.view_coupon);
                        handleCoupon();
                    }
                }
                // }
                if (guestCheckOut) {
                    if (mOneTap != null)
                        mCvvTnCLink.setVisibility(View.GONE);
                    mOneTap.setVisibility(View.GONE);
                }
                dismissProgress();
            }
        } catch (JSONException ignored) {
        }

        // Apply AutoCheck on Use Wallet In Case of CC DC NB Disabled
        if (!mFrontPaymentModesEnabled) {
            showWalletCheckBox();
            walletCheck.setChecked(true);
        } else if (walletCheck != null && walletCheck.getVisibility() == View.VISIBLE && walletBal > 0.0) {
            walletCheck.setChecked(true);
        }

    }

    private void checkForAvailablePaymentOptions() {
        try {

            JSONObject tempPaymentOption = null;
            if (paymentOption == null && details.has(SdkConstants.PAYMENT_OPTION) && !details.isNull(SdkConstants.PAYMENT_OPTION)) {
                tempPaymentOption = details.getJSONObject(SdkConstants.PAYMENT_OPTION);
                if (tempPaymentOption != null && tempPaymentOption.has(SdkConstants.OPTIONS) && !tempPaymentOption.isNull(SdkConstants.OPTIONS)) {
                    paymentOption = tempPaymentOption.getJSONObject(SdkConstants.OPTIONS);
                }
            }

            if (tempPaymentOption != null && tempPaymentOption.has(SdkConstants.CONFIG) && !tempPaymentOption.isNull(SdkConstants.CONFIG)) {
                JSONObject tempConfig = tempPaymentOption.getJSONObject(SdkConstants.CONFIG);
                if (tempConfig != null && tempConfig.has("publicKey") && !tempConfig.isNull("publicKey")) {
                    key = tempConfig.getString("publicKey").replaceAll("\\r", "");
                }
            }

            if (paymentOption != null) {
                if (checkForPaymentModeActive("dc") && !availableModes.contains(SdkConstants.PAYMENT_MODE_DC)) {
                    availableModes.add(SdkConstants.PAYMENT_MODE_DC);
                    if (!paymentOption.isNull("dc")) {

                        JSONObject tempDC = new JSONObject(paymentOption.getString("dc"));
                        Iterator keys = tempDC.keys();
                        while (keys.hasNext()) {
                            String tempCardType = (String) keys.next();
                            availableDebitCards.add(tempCardType);
                        }
                    }
                }
                if (checkForPaymentModeActive("cc") && !availableModes.contains(SdkConstants.PAYMENT_MODE_CC)) {
                    availableModes.add(SdkConstants.PAYMENT_MODE_CC);
                    if (!paymentOption.isNull("cc")) {

                        JSONObject tempCC = new JSONObject(paymentOption.getString("cc"));
                        Iterator keys = tempCC.keys();
                        while (keys.hasNext()) {
                            String tempCardType = (String) keys.next();
                            availableCreditCards.add(tempCardType);
                        }
                    }
                }
                if (checkForPaymentModeActive("nb") && !availableModes.contains(SdkConstants.PAYMENT_MODE_NB)) {
                    JSONObject nbJSONObject = new JSONObject(paymentOption.getString("nb"));
                    if (nbJSONObject != null && nbJSONObject.keys().hasNext()) {
                        availableModes.add(SdkConstants.PAYMENT_MODE_NB);
                    }
                }

                if (checkForPaymentModeActive(SdkConstants.WALLET) && !paymentOption.getString(SdkConstants.WALLET).equals("0.0")) {
                    walletActive = true;
                }
                if (!mOnlyWalletPaymentModeActive && checkForPaymentModeActive(SdkConstants.POINTS) && !paymentOption.getString(SdkConstants.POINTS).equals("0.0")) {
                    pointsActive = true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (availableModes != null && availableModes.size() == 0) {
            mFrontPaymentModesEnabled = false;
        }
    }

    private boolean checkForPaymentModeActive(String paymentModeString) {
        try {
            if (paymentOption.has(paymentModeString)
                    && !paymentOption.isNull(paymentModeString)
                    && !("-1").equals(paymentOption.getString(paymentModeString))
                    && !(SdkConstants.FALSE_STRING).equals(paymentOption.getString(paymentModeString))
                    && !(SdkConstants.NULL_STRING).equals(paymentOption.getString(paymentModeString))) {
                return true;
            }

            return false;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
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

                    choosedCoupan = null;
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
                        savings.setText("Savings : Rs." + (round((float) ((amt_discount + userPoints) / 100) * 100)));
                        savings.setVisibility(View.VISIBLE);
                    } else
                        savings.setVisibility(View.INVISIBLE);

                } else //If remove is not in text i.e. u are adding some coupons
                {
                    final SdkQustomDialogBuilder alertDialog = new SdkQustomDialogBuilder(SdkHomeActivityNew.this, R.style.PauseDialog);
                    View convertView = getLayoutInflater().inflate(R.layout.sdk_coupon_list, null);
                    couponList = (ListView) convertView.findViewById(R.id.lv);
                    couponList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    whenHideChooseCouponLayoutRequired = (LinearLayout) convertView.findViewById(R.id.whenHideChooseCouponLayoutRequired);
                    verifyCouponBtn = (Button) convertView.findViewById(R.id.verifyCoupon);
                    mannualCouponEditText = (EditText) convertView.findViewById(R.id.mannualCouponEditText);
                    verifyCouponProgress = (RelativeLayout) convertView.findViewById(R.id.verifyCouponProgress);
                    verifyCouponProgress.setVisibility(View.INVISIBLE);
                    verifyCouponBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideKeyboardIfShown();
                            String manualCoupon = "";
                            if (mannualCouponEditText.getText() != null) {
                                manualCoupon = mannualCouponEditText.getText().toString();
                            }
                            if (!manualCoupon.isEmpty()) {
                                SdkSession.getInstance(getApplicationContext()).verifyManualCoupon(manualCoupon, paymentId, device_id, "0");
                                for (int j = 0; j < couponList.getCount(); j++) {
                                    if (((RadioButton) couponList.getChildAt(j).findViewById(R.id.coupanSelect)).isChecked()) {
                                        ((RadioButton) couponList.getChildAt(j).findViewById(R.id.coupanSelect)).setChecked(false);
                                    }
                                }
                                verifyCouponProgress.setVisibility(View.VISIBLE);
                                mannualCouponEditText.clearFocus();
                                mannualCouponEditText.setText("");
                                mannualCouponEditText.setHint("");
                                SdkOtpProgressDialog.showDialog(getApplicationContext(), verifyCouponProgress);
                            } else
                                Toast.makeText(SdkHomeActivityNew.this, "Coupon Field is empty!", Toast.LENGTH_SHORT).show();

                        }
                    });

                    try {
                        if (user.has(SdkConstants.COUPONS_STRING) && !user.isNull(SdkConstants.COUPONS_STRING) && mCouponsArray != null) {
                            coupanAdapter = new SdkCouponListAdapter(SdkHomeActivityNew.this, mCouponsArray);
                            couponList.setAdapter(coupanAdapter);
                        } else {
                            whenHideChooseCouponLayoutRequired.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            manualCouponEntered = false;
                            choosedCoupan = null;
                            coupan_amt = 0.0;
                            manualCouopnNameString = null;
                        }
                    });
                    alertDialog.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // coupan_amt = 0.0;
                            //choosedItem = -1;
                            if (!manualCouponEntered) {
                                Boolean couponSelectedFromList = false;
                                for (int j = 0; j < couponList.getCount(); j++) {

                                    if (((RadioButton) couponList.getChildAt(j).findViewById(R.id.coupanSelect)).isChecked()) {

                                        /*****Apply the coupons ********/
                                        try {
                                            couponListItem = (JSONObject) coupanAdapter.getItem(j);
                                            choosedCoupan = couponListItem.getString("couponString");
                                            //choosedItem = j;
                                            coupan_amt = couponListItem.getDouble("couponAmount");
                                            couponSelectedFromList = true;
                                            SdkLogger.i("Choosed coupan", choosedCoupan);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                if (mannualCouponEditText != null && !mannualCouponEditText.getText().toString().isEmpty()) {

                                    Toast.makeText(SdkHomeActivityNew.this, "Invalid Coupon entered", Toast.LENGTH_SHORT).show();
                                } else if (!couponSelectedFromList) {
                                    Toast.makeText(SdkHomeActivityNew.this, "No Coupon Selected", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                            }
                            amt_discount = coupan_amt;
                            if (walletCheck.isChecked()) {
                                updateWalletDetails();
                            } else {
                                updateDetails(mode);
                            }
                            if (amount + amt_convenience_wallet - amt_discount <= 0.0 && !chooseOtherMode) {
                                inflateSufficientDiscountLayout();
                            } else if (amount + amt_convenience_wallet - amt_discount - userPoints <= 0.0 && !chooseOtherMode) {

                                pointDialog();
                            }
                            if (amount - amt_discount == 0.0) //100% Coupon discount
                            {
                                if (payByWalletButton.isShown()) {
                                    mAmount.setText(" " + round((amt_convenience)));
                                    walletUsage = (walletAmount - amt_net);
                                    walletBal = walletAmount - walletUsage;
                                    walletBalance.setText(getString(R.string.remaining_wallet_bal) + " " + round((float) (walletBal / 100) * 100));
                                } else {
                                    mAmount.setText(" " + 0.0);
                                }
                            }
                            //Not enough but has some
                            if (coupan_amt > 0.0) {
                                String coupon_string;
                                if (manualCouponEntered) {
                                    coupon_string = /*couponListItem.getString("couponString")*/manualCouopnNameString + " Applied";
                                } else {
                                    coupon_string = /*couponListItem.getString("couponString")*/ choosedCoupan + " Applied";
                                }
                                ((TextView) findViewById(R.id.selectCoupon1)).setText(coupon_string);
                                (findViewById(R.id.selectCoupon1)).setVisibility(View.VISIBLE);
                                ((TextView) findViewById(R.id.selectCoupon)).setText(R.string.remove);
                                (findViewById(R.id.selectCoupon)).setVisibility(View.VISIBLE);
                                /*reset manualCouponEntered*/
                                if (manualCouponEntered)
                                    manualCouponEntered = false;
                            } else {

                            }
                            if (amt_discount > 0.0 || userPoints > 0.0) {
                                savings.setText("Savings : Rs." + (round((float) ((amt_discount + userPoints) / 100) * 100)));
                                savings.setVisibility(View.VISIBLE);
                            } else
                                savings.setVisibility(View.INVISIBLE);
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
                            mannualCouponEditText.setHint(R.string.enter_coupon);
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
        if (user.has(SdkConstants.COUPONS_STRING) && !user.isNull(SdkConstants.COUPONS_STRING)) {
            mCouponsArray = new JSONArray();
            try {
                if (user.getJSONArray(SdkConstants.COUPONS_STRING) != null) {
                    JSONArray tempArrayCoupons = user.getJSONArray(SdkConstants.COUPONS_STRING);
                    for (int i = 0; i < tempArrayCoupons.length(); i++) {
                        if (tempArrayCoupons.getJSONObject(i).getBoolean(SdkConstants.ENABLED_STRING))
                            mCouponsArray.put(tempArrayCoupons.getJSONObject(i));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (coupan_amt > 0.0) {
            return;
        } else {
            /*if ( mCouponsArray != null && mCouponsArray.length() > 0) {
                applyCoupon.setText(R.string.view_coupon);
                applyCoupon.setVisibility(View.VISIBLE);
                couponLayout.setVisibility(View.VISIBLE);
            } else {
                applyCoupon.setVisibility(View.GONE);
                couponLayout.setVisibility(View.GONE);

            }*/
            /*FLow changed after manual coupon*/
            if (!(loadWalletCall || guestCheckOut || mOnlyWalletPaymentModeActive)) {
                applyCoupon.setText(R.string.view_coupon);
                applyCoupon.setVisibility(View.VISIBLE);
                couponLayout.setVisibility(View.VISIBLE);
            } else {
                applyCoupon.setVisibility(View.GONE);
                couponLayout.setVisibility(View.GONE);
            }

        }
    }

    public void updateDetails(String currentPaymentMode) {
        try {
            if (walletCheck.isChecked() || userPoints > 0.0) {
                if (currentPaymentMode.isEmpty()) {
                    amt_convenience = amt_convenience_wallet;
                } else if (convenienceChargesObject != null && convenienceChargesObject.has(currentPaymentMode) && !convenienceChargesObject.isNull(currentPaymentMode)) {
                    amt_convenience = Math.max(convenienceChargesObject.getJSONObject(currentPaymentMode).getDouble(SdkConstants.DEFAULT), amt_convenience_wallet);
                } else {
                    amt_convenience = 0.0;
                }
            } else {
                if (currentPaymentMode.isEmpty()) {
                    amt_convenience = 0;
                } else if (convenienceChargesObject != null && convenienceChargesObject.has(currentPaymentMode) && !convenienceChargesObject.isNull(currentPaymentMode)) {
                    amt_convenience = convenienceChargesObject.getJSONObject(currentPaymentMode).getDouble(SdkConstants.DEFAULT);
                } else {
                    amt_convenience = 0.0;
                }
            }

            if (coupan_amt > 0.0)
                amt_discount = coupan_amt;
            else
                amt_discount = discount;//details.getJSONObject("cashbackAccumulated").getDouble(Constants.AMOUNT);//can be commented?
            amt_net = amount + amt_convenience - amt_discount - walletUsage - userPoints;
            if (amt_net < 0.0)
                amt_net = 0.0;//bugfix
            mAmount.setText(" " + round((amt_net)));
            walletBalance.setText(getString(R.string.remaining_wallet_bal) + " " + round((walletBal)));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateDetailsForLoadWallet() {
        try {

            amt_convenience = new JSONObject(details.getString(SdkConstants.CONVENIENCE_CHARGES)).getJSONObject(SdkConstants.WALLET_STRING).getDouble(SdkConstants.DEFAULT);

            if (coupan_amt > 0.0)
                amt_discount = coupan_amt;
            else
                amt_discount = discount;//details.getJSONObject("cashbackAccumulated").getDouble(Constants.AMOUNT);//can be commented?
            amt_net = amount + amt_convenience - amt_discount - walletUsage - userPoints;
            if (amt_net < 0.0)
                amt_net = 0.0;//bugfix
            mAmount.setText(" " + round((amt_net)));
            walletBalance.setText(getString(R.string.remaining_wallet_bal) + " " + round((walletBal)));

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

    public void onActivityResult(int requestCode, int resultCode, Intent data) { //When HomeActivity resumes/starts

        if (requestCode == LOAD_AND_PAY_USING_WALLET) {
            // Fetch DTO for merchant for this payment
            //SdkSession.getInstance(this).fetchPaymentStatus(paymentId);
            //SdkSession.getInstance(this).fetchPaymentResponse(paymentId);
            SdkSession.getInstance(this).verifyPaymentDetails(paymentId);
        } else if (requestCode == LOGIN) {
            if (resultCode == RESULT_OK) {
                if (sdkSession.getLoginMode().equals("guestLogin")) {
                    guestCheckOut = true;
                }
                mIsLoginInitiated = false;
                check_login();
            } else {
                close();
            }
        } else if (requestCode == WEB_VIEW) { //Coming back from making a payment

            if(guestCheckOut){
                SdkSession.getInstance(this).fetchPaymentStatus(paymentId);
            } else if (!mInternalLoadWalletCall) {
                // Fetch DTO for merchant for this payment
                //SdkSession.getInstance(this).fetchPaymentStatus(paymentId);
                //SdkSession.getInstance(this).fetchPaymentResponse(paymentId);
                SdkSession.getInstance(this).verifyPaymentDetails(paymentId);
            } else {
                finish();
            }
            /*if (resultCode == RESULT_OK) //Success
            {
                SdkLogger.i("payment_status", "success");
                setResult(RESULT_OK, data);
                finish();
            } else if (resultCode == RESULT_CANCELED) //Fail
            {
                SdkLogger.i("payment_status", "cancelled");
                setResult(RESULT_CANCELED, data);
                finish();
                //Write your code if there's no result
            } else if (resultCode == PayUmoneySdkInitilizer.RESULT_FAILED) {

                SdkLogger.i("payment_status", "failure");
                setResult(PayUmoneySdkInitilizer.RESULT_FAILED, data);
                finish();

            } else {
                Toast.makeText(getApplicationContext(), "Something went wrong. please retry", Toast.LENGTH_LONG).show();
            }*/
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
        int id = item.getItemId();
        if (id == R.id.logout) {
            if (SdkConstants.WALLET_SDK) {
                sdkSession.logout();
            } else {
                logout();
            }
        }
        return true;
    }

        /*if(item.getTitle().equals("Enable One Tap Payment")){
            SdkSession.getInstance(getApplicationContext()).enableOneClickTransaction("1");
            //item.setTitle("Disable One Tap Payment");
        } else if (item.getTitle().equals("Disable One Tap Payment")) {
            sdkSession.enableOneClickTransaction("0");
            // item.setTitle("Enable One Tap Payment");
        }
        return super.onOptionsItemSelected(item);
    }*/

    public void logout() {

        sdkSession.logout("");
        SharedPreferences.Editor edit = getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE).edit();
        edit.clear();
        edit.commit();
        /*reset login mode as well*/
        sdkSession.setLoginMode("");
        resetValuesOnLogout();
        //SdkSession.startPaymentProcess(SdkSession.merchantContext, map);//LoginLogoutFlowIssue
        sdkSession.fetchMechantParams(map.get(SdkConstants.MERCHANT_ID));
    }

    public void resetValuesOnLogout() {

        walletUsage = 0.0;
        walletAmount = 0.0;
        userPoints = 0.0;
        amt_convenience = 0.0;
        amt_net = 0.0;
        amount = 0.0;
        cashback = 0.0;
        discount = 0.0;
        amt_discount = 0.0;
        amt_convenience_wallet = 0.0;
        walletBal = 0.0;
        coupan_amt = 0.0;
        choosedCoupan = null;
        /*set it to false if it ws set to true earlier*/
        guestCheckOut = false;
        chooseOtherMode = false;
        paymentOption = null;
        pointsActive = false;
        walletActive = false;

        // Clear payment options
        availableModes.clear();
        availableDebitCards.clear();
        availableCreditCards.clear();
    }

    @Override
    public void setCardHashForOneClickTxn(String s) {
        cardHashForOneClickTxn = s;
    }

    @Override
    public void goToPayment(String mode, HashMap<String, Object> data) throws JSONException {

        hideKeyboardIfShown();

        //check if selected bank services are Up
        if (mode.equals(SdkConstants.PAYMENT_MODE_NB)) {

            String mBankCode = (String) data.get(SdkConstants.BANK_CODE);
            if (mNetBankingStatusObject != null && mNetBankingStatusObject.has(mBankCode)
                    && !mNetBankingStatusObject.isNull(mBankCode)) {
                JSONObject mBankStatusObject = mNetBankingStatusObject.getJSONObject(mBankCode);
                if (mBankStatusObject != null && mBankStatusObject.has(SdkConstants.UP_STATUS) && !mBankStatusObject.isNull(SdkConstants.UP_STATUS)) {
                    int mIsBankStatusUp = Integer.parseInt(mBankStatusObject.getString(SdkConstants.UP_STATUS));
                    if (mIsBankStatusUp == 0) {
                        Toast.makeText(this, mBankStatusObject.getString(SdkConstants.BANK_TITLE_STRING) + " seems to be down temporarily.\n" +
                                "Please select another bank or pay using other payment options.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
        }

        if (mode.equals(SdkConstants.PAYMENT_MODE_NB) && data.get(SdkConstants.BANK_CODE).equals("CITNB")) {
            Toast.makeText(this, "City Bank doesn't provide Net Banking!", Toast.LENGTH_LONG).show();
            int i = 0;
            if (availableModes != null && availableModes.contains(SdkConstants.PAYMENT_MODE_DC)) {
                while (!availableModes.get(i).equals(SdkConstants.PAYMENT_MODE_DC)) {
                    i++;
                }
                Toast.makeText(this, "City Bank doesn't provide Net Banking!", Toast.LENGTH_LONG).show();
                paymentModesList.expandGroup(i);
            }
        } else if (mode.equals(SdkConstants.PAYMENT_MODE_DC) && availableDebitCards != null && !availableDebitCards.contains(data.get(SdkConstants.BANK_CODE))) {

            Toast.makeText(this, "The merchant doesn't support: " + data.get(SdkConstants.BANK_CODE).toString() + " Debit Cards", Toast.LENGTH_SHORT).show();
        } else if (mode.equals(SdkConstants.PAYMENT_MODE_CC) && (data.get(SdkConstants.BANK_CODE).toString().equals("AMEX") || data.get(SdkConstants.BANK_CODE).toString().equals("DINR")) && !availableCreditCards.contains(data.get("bankcode"))) {

            Toast.makeText(this, "The merchant doesn't support: " + data.get(SdkConstants.BANK_CODE).toString() + " Credit Cards", Toast.LENGTH_SHORT).show();
        } else {
            //  handleCvvLessTransaction();
            initiateProgressDialog();
            sdkSession.sendToPayUWithWallet(details, mode, data, Double.valueOf(userPoints), Double.valueOf(walletUsage), Double.valueOf(discount), amt_convenience);
        }
    }

    @Override
    public void modifyConvenienceCharges(String cardBankType) {

        if(cardBankType == null) {
            updateDetails(mode);
            return;
        }

        String currentPaymentMode = mode;
        try {
            if (walletCheck.isChecked() || userPoints > 0.0) {
                if (currentPaymentMode.isEmpty()) {
                    amt_convenience = amt_convenience_wallet;
                } else if (convenienceChargesObject != null && convenienceChargesObject.has(currentPaymentMode) && !convenienceChargesObject.isNull(currentPaymentMode)) {

                    JSONObject modeConvenienceChargesObject = convenienceChargesObject.getJSONObject(currentPaymentMode);
                    if (modeConvenienceChargesObject != null && modeConvenienceChargesObject.has(cardBankType) && !modeConvenienceChargesObject.isNull(cardBankType)) {

                        amt_convenience = convenienceChargesObject.getJSONObject(currentPaymentMode).getDouble(cardBankType);
                    } else {
                        amt_convenience = convenienceChargesObject.getJSONObject(currentPaymentMode).getDouble(SdkConstants.DEFAULT);
                    }
                    amt_convenience = Math.max(amt_convenience, amt_convenience_wallet);
                } else {
                    amt_convenience = 0.0;
                }
            } else {
                if (currentPaymentMode.isEmpty()) {
                    amt_convenience = 0;
                } else if (convenienceChargesObject != null && convenienceChargesObject.has(currentPaymentMode) && !convenienceChargesObject.isNull(currentPaymentMode)) {

                    JSONObject modeConvenienceChargesObject = convenienceChargesObject.getJSONObject(currentPaymentMode);
                    if (modeConvenienceChargesObject != null && modeConvenienceChargesObject.has(cardBankType) && !modeConvenienceChargesObject.isNull(cardBankType)) {

                        amt_convenience = convenienceChargesObject.getJSONObject(currentPaymentMode).getDouble(cardBankType);
                    } else {
                        amt_convenience = convenienceChargesObject.getJSONObject(currentPaymentMode).getDouble(SdkConstants.DEFAULT);
                    }
                } else {
                    amt_convenience = 0.0;
                }
            }

            amt_net = amount + amt_convenience - amt_discount - walletUsage - userPoints;

            if (amt_net < 0.0) {
                amt_net = 0.0;//bugfix
            }

            mAmount.setText(" " + round((amt_net)));
            walletBalance.setText(getString(R.string.remaining_wallet_bal) + " " + round((walletBal)));
        } catch(JSONException e) {
            e.printStackTrace();
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

    public void hideKeyboardIfShown() {

        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = this.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void hideKeyboardIfShown(View view) {

        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = this.getCurrentFocus();
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        SharedPreferences mPref = this.getSharedPreferences(SdkConstants.SP_SP_NAME, Context.MODE_PRIVATE);
        /*Not required in case of SDK in the app*/
        //if(!guestCheckOut)
        if (!mInternalLoadWalletCall) {
            getMenuInflater().inflate(R.menu.sdk_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }


    public void onEventMainThread(final SdkCobbocEvent event) {
        if (event != null) {

            if (event.getType() == SdkCobbocEvent.CREATE_WALLET) {

                if (event.getStatus()) {
                    String status = "-1";
                    //Success - OTP verified
                    JSONObject eventObject = (JSONObject) event.getValue();
                    try {
                        status = eventObject.getString(SdkConstants.STATUS);
                    } catch (Exception e) {
                        e.printStackTrace();
                        status = "-1";
                    }

                    if (status.equals("0")) {
                        if (eventObject != null && eventObject.has(SdkConstants.RESULT) && !eventObject.isNull(SdkConstants.RESULT)) {
                            try {
                                JSONObject resultObject = eventObject.getJSONObject(SdkConstants.RESULT);

                                if (resultObject.has(SdkConstants.AVAILABLE_AMOUNT) && !resultObject.isNull(SdkConstants.AVAILABLE_AMOUNT)) {
                                    mWalletRecentlyVerified = true;
                                    loadWalletMinLimit = resultObject.optDouble(SdkConstants.MIN_LOAD_LIMIT, 10.00);
                                    loadWalletMaxLimit = resultObject.optDouble(SdkConstants.MAX_LOAD_LIMIT, 10000.00);
                                    calculateOffersAndCashback();
                                    if (OTPVerificationdialog != null && OTPVerificationdialog.isShowing()) {
                                        OTPVerificationdialog.dismiss();
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        progressBarWaitOTP.setVisibility(View.GONE);
                        OTPEditText.setEnabled(true);
                        proceed.setEnabled(true);

                        if (event.getValue() != null) {
                            try {
                                Toast.makeText(SdkHomeActivityNew.this, ((JSONObject) event.getValue()).getString(SdkConstants.MESSAGE), Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                            }
                        } else {

                            Toast.makeText(SdkHomeActivityNew.this, getString(R.string.something_went_wrong), Toast.LENGTH_LONG).show();
                        }
                    }
                } else { // end if(event.getstatus())
                    OTPEditText.setEnabled(true);
                    proceed.setEnabled(true);
                    if (event.getValue() != null) {

                        try {

                            Toast.makeText(SdkHomeActivityNew.this, ((JSONObject) event.getValue()).getString(SdkConstants.MESSAGE), Toast.LENGTH_LONG).show();

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    } else {

                        Toast.makeText(SdkHomeActivityNew.this, getString(R.string.something_went_wrong), Toast.LENGTH_LONG).show();
                    }
                    progressBarWaitOTP.setVisibility(View.GONE);
                }
            } else if (event.getType() == SdkCobbocEvent.LOAD_WALLET) {
                dismissProgress();
                if (event.getStatus()) {
                    JSONObject paymentDetailsObject = (JSONObject) event.getValue();
                    Intent intent = new Intent(this, SdkHomeActivityNew.class);
                    map.put(SdkConstants.PAYMENT_TYPE, SdkConstants.LOAD_WALLET);
                    map.put(SdkConstants.INTERNAL_LOAD_WALLET_CALL, paymentDetailsObject.toString());
                    if (!fromPayUMoneyApp) {
                        map.put(SdkConstants.NEED_TO_SHOW_ONE_TAP_CHECK_BOX, true + "");
                    }
                    intent.putExtra(SdkConstants.PARAMS, map);

                    // unRegister Event for HomeActivity current Instance to prevent the posting of multiple events in LoadWallet Instance of HomeActivity
                    if (EventBus.getDefault() != null && EventBus.getDefault().isRegistered(this)) {
                        EventBus.getDefault().unregister(this);
                    }

                    startActivityForResult(intent, LOAD_AND_PAY_USING_WALLET); //Start the HomeActivity for loadWallet
                } else {
                    SdkHelper.showToastMessage(this, this.getString(R.string.something_went_wrong), true);
                }
            } else if (event.getType() == SdkCobbocEvent.ONE_TAP_OPTION_ALTERED) {
                if (event.getStatus()) {
                    JSONObject result = (JSONObject) event.getValue();
                    handleOneClickAndOneTapFeature(result);
                    /*SdkDebit.mCardStore.setText("");
                    SdkDebit.sdkTnc.setVisibility(View.VISIBLE);*/
                    //SdkHelper.showToastMessage(this,"Can't opt for this feature now",true);// STOPSHIP: 12/21/15  ;
                    // invalidateOptionsMenu();
                } else {
                    /*call failed stick with the older choice*/
                    SharedPreferences mPref = getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
                    Boolean oneClickPayment = false, oneTapFeature = false;
                    if (mPref.contains(SdkConstants.CONFIG_DTO))
                        try {
                            JSONObject userConfigDto = new JSONObject(mPref.getString(SdkConstants.CONFIG_DTO, SdkConstants.XYZ_STRING));
                            if (userConfigDto != null) {
                                if (userConfigDto.has(SdkConstants.ONE_CLICK_PAYMENT) && !userConfigDto.isNull(SdkConstants.ONE_CLICK_PAYMENT)) {
                                    oneClickPayment = userConfigDto.optBoolean(SdkConstants.ONE_CLICK_PAYMENT);
                                    /*if (oneClickPayment && userConfigDto.has(SdkConstants.ONE_TAP_FEATURE) && !userConfigDto.isNull(SdkConstants.ONE_TAP_FEATURE)) {
                                        oneTapFeature = userConfigDto.optBoolean(SdkConstants.ONE_TAP_FEATURE);
                                    }*/
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    if (!oneClickPayment) {
                        /*SdkDebit.mCardStore.setText("Save this card");
                        SdkDebit.sdkTnc.setVisibility(View.GONE);*/
                        mOneTap.setChecked(false);
                    } else {
                        /*SdkDebit.mCardStore.setText("");
                        SdkDebit.sdkTnc.setVisibility(View.VISIBLE);*/
                        mOneTap.setChecked(true);
                    }
                    if (!firstTimeFetchingOneClickFlag)
                        SdkHelper.showToastMessage(this, this.getString(R.string.something_went_wrong), true);

                    firstTimeFetchingOneClickFlag = false;

                }
            } else if (event.getType() == SdkCobbocEvent.VERIFY_MANUAL_COUPON) {
                verifyCouponProgress.setVisibility(View.INVISIBLE);
                if (event.getStatus()) {
                    try {
                        JSONObject manualCoupon = (JSONObject) event.getValue();
                        if (manualCoupon.has("couponStringForUser") && !manualCoupon.isNull("couponStringForUser")) {
                            manualCouopnNameString = manualCoupon.getString("couponStringForUser");
                            mannualCouponEditText.setText(manualCouopnNameString + " Added");
                        } else
                            mannualCouponEditText.setText("Coupon Added");

                        if (manualCoupon.has("couponString") && !manualCoupon.isNull("couponString")) {
                            choosedCoupan = manualCoupon.getString("couponString");
                        }
                        if (manualCoupon.has("amount") && !manualCoupon.isNull("amount")) {
                            coupan_amt = manualCoupon.getDouble("amount");
                        } else
                            coupan_amt = 0.0;

                        manualCouponEntered = true;

                    } catch (JSONException e) {
                        e.printStackTrace();
                        manualCouponEntered = false;
                        mannualCouponEditText.setText("Invalid Coupon");
                        SdkLogger.d(SdkConstants.TAG, "Invalid Coupon Entered");
                    }

                } else {
                    manualCouponEntered = false;
                    mannualCouponEditText.setText("Invalid Coupon");
                    SdkLogger.d(SdkConstants.TAG, "Invalid Coupon Entered");
                }

            }
            if (event.getType() == SdkCobbocEvent.FETCH_USER_PARAMS) {
                if (event.getStatus()) {
                    userParamsFetchedExplicitely = true;
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

                                if (paramKey.equals(SdkConstants.OTP_LOGIN)) {
                                    quickLogin = paramValue;
                                } else if (paramKey.equals(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_VALUE)) {
                                    if((SdkConstants.MERCHANT_PARAM_ALLOW_QUICK_GUEST_CHECKOUT).equals(paramValue)) {
                                        paramValue = SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_ONLY;
                                    }
                                    allowGuestCheckout = paramValue;
                                }
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
                        sdkSession.reset();
                        if (!mIsLoginInitiated) {
                            sdkSession.fetchMechantParams(map.get(SdkConstants.MERCHANT_ID));
                        }
                    } else if (SdkConstants.WALLET_SDK) {

                        SdkHelper.dismissProgressDialog();
                        if (event.getStatus()) {
                            SdkHelper.showToastMessage(this, getString(R.string.logout_success), false);
                            close(PAYMNET_LOGOUT);
                        } else {

                            if (!SdkHelper.checkNetwork(this)) {
                                SdkHelper.showToastMessage(this, getString(R.string.disconnected_from_internet), true);
                            } else {
                                SdkHelper.showToastMessage(this, getString(R.string.something_went_wrong), true);
                            }
                        }
                    }
                }
                // clear the token stored in SharedPreferences
            } else if (event.getType() == SdkCobbocEvent.USER_POINTS) { //Add wallet points

                SdkLogger.d(SdkConstants.TAG, "Entered in User Points");
                /*if (event.getStatus()) {
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
                }*/
            } else if (event.getType() == SdkCobbocEvent.CREATE_PAYMENT) {  //New Payment
                SdkLogger.d(SdkConstants.TAG, "Entered in Create Payment");
                if (event.getStatus()) {

                    try {
                        details = (JSONObject) event.getValue();
                        if (details != null && details.has(SdkConstants.PAYMENT_ID) && !details.isNull(SdkConstants.PAYMENT_ID))
                            paymentId = details.getString(SdkConstants.PAYMENT_ID);
                        if (guestCheckOut && !fromPayUMoneyApp && !fromPayUBizzApp && !mInternalLoadWalletCall) {
                            String guestEmail = sdkSession.getGuestEmail();
                            sdkSession.updateTransactionDetails(paymentId, guestEmail);
                        }
                        startPayment(null);
                        // sdkSession.getPaymentDetails(details.getJSONObject("payment").getString(Constants.PAYMENT_ID));//merge
                        //sdkSession.getPaymentDetails(result.getString(Constants.PAYMENT_ID)); //Fire getpaymentdetails of sdkSession
                    } catch (Exception e) {
                        dismissProgress();
                        e.printStackTrace();
                    }
                    SdkLogger.d(SdkConstants.TAG, "exited from Create Payment");
                } else {
                    dismissProgress();
                    try {
                        String responseString = (String) (event.getValue());
                        if (responseString != null && !responseString.isEmpty() && !responseString.equals(SdkConstants.NULL_STRING)) {
                            JSONObject responseObject = new JSONObject(responseString);
                            if (responseObject != null && responseObject.has(SdkConstants.MESSAGE) && !responseObject.isNull(SdkConstants.MESSAGE)) {
                                String messageString = responseObject.getString(SdkConstants.MESSAGE);
                                if (messageString.contains(SdkConstants.PAYMENT_NOT_VALID)) {
                                    Toast.makeText(this, messageString, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "Some error occurred! Try again", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(this, "Some error occurred! Try again", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Some error occurred! Try again", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (event.getType() == SdkCobbocEvent.PAYMENT_POINTS) {

                //sending paymentDTO to merchant
                /*Intent intent = new Intent();
                try {
                    JSONObject jsonObject = new JSONObject(event.getValue().toString()).getJSONObject(SdkConstants.RESULT);
                    intent.putExtra(SdkConstants.RESULT, jsonObject.toString());
                    setResult(RESULT_OK, intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                    intent.putExtra(SdkConstants.RESULT, SdkConstants.NULL_STRING);
                    setResult(PayUmoneySdkInitilizer.RESULT_FAILED, intent);
                }
                finish();*/

                if (event.getStatus()) {
                    Intent intent = new Intent();

                    String status = null;
                    String paymentId = null;
                    try {
                        JSONObject jsonObject = new JSONObject(event.getValue().toString()).getJSONObject(SdkConstants.RESULT);
                        if (jsonObject != null && jsonObject.has(SdkConstants.PAYMENT_ID) && !jsonObject.isNull(SdkConstants.PAYMENT_ID)) {
                            paymentId = jsonObject.getString(SdkConstants.PAYMENT_ID);
                        }
                        if (jsonObject != null && jsonObject.has(SdkConstants.STATUS) && !jsonObject.isNull(SdkConstants.STATUS)) {
                            status = jsonObject.getString(SdkConstants.STATUS);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (intent != null) {
                        intent.putExtra(SdkConstants.PAYMENT_ID, paymentId);
                        intent.putExtra(SdkConstants.RESULT, status);
                    }

                    if (status != null && status.equalsIgnoreCase(SdkConstants.SUCCESS_STRING)) {
                        setResult(RESULT_OK, intent);
                    } else {
                        setResult(PayUmoneySdkInitilizer.RESULT_FAILED, intent);
                    }

                    finish();

                } else if (event.getValue() != null && event.getValue().toString().equals(SdkConstants.INVALID_APP_VERSION)) {
                    //showAlertDialog();
                } else {

                    Intent intent = new Intent();
                    intent.putExtra(SdkConstants.RESULT, event.getValue().toString());
                    setResult(PayUmoneySdkInitilizer.RESULT_FAILED, intent);
                    finish();
                    //onActivityResult(PAYMENT_SUCCESS, RESULT_FAILED, intent);
                }


            } else if (event.getType() == SdkCobbocEvent.PAYMENT) {
                dismissProgress();
                if (event.getStatus()) {
                    SdkLogger.i("reached", "cred" +
                            "it");
                    Intent intent = new Intent(this, SdkWebViewActivityNew.class);
                    intent.putExtra(SdkConstants.RESULT, event.getValue().toString());
                    intent.putExtra(SdkConstants.PAYMENT_MODE, mode);
                    if (mode.isEmpty()) {
                        if (cardHashForOneClickTxn != null)
                            intent.putExtra(SdkConstants.CARD_HASH_FOR_ONE_CLICK_TXN, cardHashForOneClickTxn);
                        else
                            intent.putExtra(SdkConstants.CARD_HASH_FOR_ONE_CLICK_TXN, "0");
                    }

                    // Adding merchant Permission for OTP Auto Read
                    if (mOTPAutoRead) {
                        intent.putExtra(SdkConstants.OTP_AUTO_READ, true);
                    }

                    intent.putExtra(SdkConstants.MERCHANT_KEY, getIntent().getExtras().getString("key"));
                    intent.putExtra(SdkConstants.PAYMENT_ID, paymentId);
                    this.startActivityForResult(intent, this.WEB_VIEW);
                } else if (event.getValue().toString().equals(SdkConstants.INVALID_APP_VERSION)) {
                    //showAlertDialog();
                } else {
                    SdkLogger.i("reached", "failed");
                    //If not status do nothing
                    Toast.makeText(this, "Payment Failed", Toast.LENGTH_LONG).show();
                }
            } else if (event.getType() == SdkCobbocEvent.SEND_OTP_TO_USER) {
                if (event.getStatus()) {
                    JSONObject response = (JSONObject) event.getValue();
                    try {
                        if (response.getString("status").equals("0")) {

                            //register receiver for otp reading
                            autoFillOTPForWalletCreation();

                            proceed.setEnabled(false);
                            proceed.setText("Activate");
                            resend.setVisibility(View.VISIBLE);
                            OTPEditText.setVisibility(View.VISIBLE);
                            humble.setVisibility(View.VISIBLE);
                            progressBarWaitOTP.setVisibility(View.VISIBLE);
                            info.setText("Waiting for OTP..");
                            Toast.makeText(SdkHomeActivityNew.this, ((JSONObject) event.getValue()).getString("message"), Toast.LENGTH_LONG).show();
                        } else {
                            if (event.getValue() != null) {
                                try {
                                    Toast.makeText(SdkHomeActivityNew.this, ((JSONObject) event.getValue()).getString("message"), Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                }
                            } else {
                                Toast.makeText(SdkHomeActivityNew.this, "Something went wrong", Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        if (event.getValue() != null) {
                            try {
                                Toast.makeText(SdkHomeActivityNew.this, ((JSONObject) event.getValue()).getString("message"), Toast.LENGTH_LONG).show();
                            } catch (Exception e1) {
                            }
                        } else {
                            Toast.makeText(SdkHomeActivityNew.this, "Something went wrong", Toast.LENGTH_LONG).show();
                        }
                    } //end catch

                } // end if(event.getstatus())
                else {
                    if (event.getValue() != null) {
                        try {
                            Toast.makeText(SdkHomeActivityNew.this, ((JSONObject) event.getValue()).getString("message"), Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                        }
                    } else {
                        Toast.makeText(SdkHomeActivityNew.this, "Something went wrong", Toast.LENGTH_LONG).show();
                    }
                }
            } else if (event.getType() == SdkCobbocEvent.NET_BANKING_STATUS) {
                if (event.getStatus()) {
                    mNetBankingStatusObject = (JSONObject) event.getValue();
                }
            }
        }
    }


    private void showOtpVerifyDialog(String phoneNumber) {
        final SdkQustomDialogBuilder alertDialog = new SdkQustomDialogBuilder(SdkHomeActivityNew.this, R.style.PauseDialog);

        View convertView = getLayoutInflater().inflate(R.layout.sdk_otp_verify_layout, null);

        resend = (TextView) convertView.findViewById(R.id.resend);
        info = (TextView) convertView.findViewById(R.id.info);
        mobileEditText = (EditText) convertView.findViewById(R.id.mobile);
        OTPEditText = (EditText) convertView.findViewById(R.id.otp);
        proceed = (Button) convertView.findViewById(R.id.activate);
        anotherAccountButton = (Button) convertView.findViewById(R.id.logout);
        humble = (RelativeLayout) convertView.findViewById(R.id.humble);
        progressBarWaitOTP = (ProgressBar) convertView.findViewById(R.id.pbwaitotp);

        if (phoneNumber != null)
            mobileEditText.setText(phoneNumber);

        humble.setVisibility(View.GONE);
        proceed.setEnabled(true);
        proceed.setText(getString(R.string.proceed));
        OTPEditText.setVisibility(View.GONE);
        humble.setVisibility(View.GONE);
        progressBarWaitOTP.setVisibility(View.GONE);
        resend.setVisibility(View.GONE);
        info.setText(getString(R.string.please_verify_number));

        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SdkHelper.checkNetwork(SdkHomeActivityNew.this)) {
                    SdkHelper.showToastMessage(SdkHomeActivityNew.this, getString(R.string.connect_to_internet), true);
                    return;
                }
                sdkSession.sendMobileVerificationCodeForWalletCreation(mobileEditText.getText().toString());
            }
        });

        anotherAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (OTPVerificationdialog != null && OTPVerificationdialog.isShowing()) {
                    OTPVerificationdialog.dismiss();
                }
                //Logout this user
                logout();
            }
        });

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!SdkHelper.checkNetwork(SdkHomeActivityNew.this)) {
                    SdkHelper.showToastMessage(SdkHomeActivityNew.this, getString(R.string.connect_to_internet), true);
                    return;
                }

                if (mobileEditText.getText().toString().equalsIgnoreCase("") || mobileEditText.getText().toString().charAt(0) < '6') {

                    Toast.makeText(SdkHomeActivityNew.this, "Please enter valid number", Toast.LENGTH_LONG).show();

                } else {
                    if (proceed.getText().toString().equalsIgnoreCase(getString(R.string.proceed))) {
                        sdkSession.sendMobileVerificationCodeForWalletCreation(mobileEditText.getText().toString());
                    } else {
                        if (proceed.getText().toString().equalsIgnoreCase(getString(R.string.activate)) && !OTPEditText.getText().toString().equalsIgnoreCase("") /*&& !name.getText().toString().equalsIgnoreCase("")*/ && !mobileEditText.getText().toString().equalsIgnoreCase("")) {

                            info.setText(getString(R.string.activating));
                            progressBarWaitOTP.setVisibility(View.VISIBLE);
                            mobileEditText.setEnabled(false);
                            OTPEditText.setEnabled(false);

                            Button proceedButton = (Button) v;
                            proceedButton.setEnabled(false);

                            sdkSession.createWallet(SharedPrefsUtils.getStringPreference(SdkHomeActivityNew.this, SdkConstants.EMAIL), mobileEditText.getText().toString(), OTPEditText.getText().toString());
                        } else if (OTPEditText.getText().toString().equalsIgnoreCase("")) {
                            Toast.makeText(SdkHomeActivityNew.this, getString(R.string.waiting_for_otp), Toast.LENGTH_LONG).show();

                        }
                    }
                }
            }
        });

        OTPEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.toString().trim().length() == 6) {

                    progressBarWaitOTP.setVisibility(View.GONE);
                    info.setText("Verify OTP");
                    proceed.setEnabled(true);
                    hideKeyboardIfShown((View) OTPEditText);
                } else {
                    progressBarWaitOTP.setVisibility(View.VISIBLE);
                    info.setText("Waiting for OTP..");
                    proceed.setEnabled(false);
                }

            }
        });

        //alertDialog.setCancelable(false);
        alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    count++;
                    if (count % 2 == 0) {
                        count = 0;
                        close(PAYMNET_CANCELLED);
                        sdkSession.notifyUserCancelledTransaction(paymentId, "1");

                    } else {
                        Toast.makeText(getApplicationContext(), "This merchant supports only wallet as payment mode and your wallet is not active. Press Back again to cancel transaction.", Toast.LENGTH_LONG).show();
                        return true;
                    }
                }
                return false;
            }
        });

        OTPVerificationdialog = alertDialog.setView(convertView).show();
        OTPVerificationdialog.setCanceledOnTouchOutside(false);
    }

    public void autoFillOTPForWalletCreation() {
         /*
        * Start broadcast receiverx
        * for sms listen*/
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                String msgBody = null;
                if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                    Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
                    SmsMessage[] msgs;
                    if (bundle != null && OTPEditText != null) {
                        //---retrieve the SMS message received---
                        try {
                            Object[] pdus = (Object[]) bundle.get("pdus");
                            msgs = new SmsMessage[pdus.length];
                            for (int i = 0; i < msgs.length; i++)   //Msg Read
                            {
                                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                                msgBody = msgs[i].getMessageBody();
                            }
                        /*
                        * Now extract the otp*/
                            if (msgBody != null && msgBody.toLowerCase().contains("verification")) {//make sure from payumoney.com
                                Matcher m = otpPattern.matcher(msgBody);
                                if (m.find()) {
                                    OTPEditText.setText(m.group(0));
                                } else {
                                    Toast.makeText(SdkHomeActivityNew.this, "Couldn't read sms, please enter OTP manually", Toast.LENGTH_LONG).show();
                                    OTPEditText.requestFocus();
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(SdkHomeActivityNew.this, "Couldn't read sms, please enter OTP manually", Toast.LENGTH_LONG).show();
                            OTPEditText.requestFocus();
                        }
                    } else {
                        Toast.makeText(SdkHomeActivityNew.this, "Couldn't read sms, please enter OTP manually", Toast.LENGTH_LONG).show();
                        OTPEditText.requestFocus();
                    }
                }
            }
        };
        registerReceiver(receiver, filter);
    }

    private void checkForUserWalletActive() {

        try {
            if (mOnlyWalletPaymentModeActive || !mFrontPaymentModesEnabled) {

                if (details.has(SdkConstants.USER) && !details.isNull(SdkConstants.USER)) {
                    user = details.getJSONObject(SdkConstants.USER);

                    if (user.has(SdkConstants.WALLET) && user.isNull(SdkConstants.WALLET)) {
                        if (user.has(SdkConstants.PHONE) && !user.isNull(SdkConstants.PHONE)) {
                            showOtpVerifyDialog(user.getString(SdkConstants.PHONE));
                        } else {
                            userWalletNotRegisteredDialog();
                        }
                    } else {
                        calculateOffersAndCashback();
                    }
                }
            } else {
                calculateOffersAndCashback();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkForWalletOnlyPaymentMode() {
        try {
            if (details != null && details.has(SdkConstants.CONFIG_DATA) && !details.isNull(SdkConstants.CONFIG_DATA)) {
                JSONObject configDataJsonObject = details.getJSONObject(SdkConstants.CONFIG_DATA);

                if (configDataJsonObject.has(SdkConstants.MERCHANT_CATEGORY_TYPE)
                        && !configDataJsonObject.isNull(SdkConstants.MERCHANT_CATEGORY_TYPE)
                        && (SdkConstants.ONLY_WALLET_PAYMENT).equals(configDataJsonObject.getString(SdkConstants.MERCHANT_CATEGORY_TYPE))) {
                    mOnlyWalletPaymentModeActive = true;
                }

                //Checking for OTPAutoRead falg
                if (configDataJsonObject.has(SdkConstants.OTP_AUTO_READ)
                        && !configDataJsonObject.isNull(SdkConstants.OTP_AUTO_READ)) {
                    mOTPAutoRead = configDataJsonObject.optBoolean(SdkConstants.OTP_AUTO_READ, false);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            mOnlyWalletPaymentModeActive = false;
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
                    if (!userParamsFetchedExplicitely)
                        user = details.getJSONObject(SdkConstants.USER);
                    if (details != null && details.has(SdkConstants.PAYMENT_ID) && !details.isNull(SdkConstants.PAYMENT_ID))
                        paymentId = details.getString(SdkConstants.PAYMENT_ID);
                    if (user.has("userId") && !user.isNull("userId"))
                        userId = user.getString("userId");

                    if (paymentOption == null && details.has(SdkConstants.PAYMENT_OPTION) && !details.isNull(SdkConstants.PAYMENT_OPTION)) {
                        JSONObject tempPaymentOption = details.getJSONObject(SdkConstants.PAYMENT_OPTION);
                        if (tempPaymentOption != null && tempPaymentOption.has(SdkConstants.OPTIONS) && !tempPaymentOption.isNull(SdkConstants.OPTIONS)) {
                            paymentOption = tempPaymentOption.getJSONObject(SdkConstants.OPTIONS);
                        }
                    }

                    if (!mOnlyWalletPaymentModeActive && paymentOption != null && checkForPaymentModeActive(SdkConstants.POINTS)) {
                        pointsActive = true;
                    }
                    if (paymentOption != null && checkForPaymentModeActive(SdkConstants.WALLET)) {
                        walletActive = true;
                    }

                    if (pointsActive) {
                        if (user.has(SdkConstants.POINTS) && !user.isNull(SdkConstants.POINTS)) {
                            JSONObject tempPoints = user.getJSONObject(SdkConstants.POINTS);
                            userPoints = tempPoints.optDouble(SdkConstants.AMOUNT, 0.0);
                        /*if (chooseOtherMode)
                            userPoints = 0.0;*/ //CHECKED
                        }
                    } else {
                        userPoints = 0.0;
                    }
                    if (walletActive) {
                        if (user.has(SdkConstants.WALLET) && !user.isNull(SdkConstants.WALLET)) {
                            walletJason = user.getJSONObject(SdkConstants.WALLET);
                            if ((walletJason.has(SdkConstants.AMOUNT)) && !walletJason.isNull(SdkConstants.AMOUNT)) {
                                walletAmount = walletJason.optDouble(SdkConstants.AMOUNT, 0.0);
                                mIsUserWalletBlocked = walletJason.optInt(SdkConstants.STATUS, 2) == 0;
                            }

                            walletBal = walletAmount;
                            if (walletAmount > 0.0) {
                                showWalletCheckBox();
                            }
                        }
                    }
                } else if (fromPayUBizzApp && !sdkSession.isLoggedIn()) {
                    /*make the call*/
                }
                if (mWalletRecentlyVerified) {
                    startPayment(null);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(getApplicationContext(), "Something Went Wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void showWalletCheckBox() {
        walletCheck.setVisibility(View.VISIBLE);//CHECKED
        walletBoxLayout.setVisibility(View.VISIBLE);
        walletBalance.setText(getString(R.string.remaining_wallet_bal) + " " + round(walletAmount));
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
            close(PAYMNET_CANCELLED);
            sdkSession.notifyUserCancelledTransaction(paymentId, "1");

        } else {
            Toast.makeText(getApplicationContext(), "Press Back again to cancel transaction.", Toast.LENGTH_SHORT).show();
        }

    }


    public void close() {
         /*do nothing user cancelled without loggin*/
        Intent intent = new Intent();
        intent.putExtra(SdkConstants.RESULT, SdkConstants.CANCEL_STRING);
        setResult(PayUmoneySdkInitilizer.RESULT_BACK, intent);
        finish();
    }


    public void close(int resultCode) {

        Intent intent = new Intent();
        if (resultCode == PAYMNET_LOGOUT) {
            intent.putExtra(SdkConstants.IS_LOGOUT_CALL, true);
            setResult(RESULT_CANCELED, intent);
        } else if (resultCode == PAYMNET_CANCELLED) {
            intent.putExtra(SdkConstants.RESULT, SdkConstants.CANCEL_STRING);
            setResult(RESULT_CANCELED, intent);
        }
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
        //sdkSession.cancelPendingRequests();
        if (EventBus.getDefault() != null && EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
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

    public void loadWalletDialog() {

        if (!mWalletRecentlyVerified && walletJason != null && (walletJason.has(SdkConstants.MIN_LIMIT)) && !walletJason.isNull(SdkConstants.MIN_LIMIT)) {
            loadWalletMinLimit = walletJason.optDouble(SdkConstants.MIN_LIMIT, 0.0);
        }

        loadWalletMinLimit = Math.max(loadWalletMinLimit, amt_net);

        if (!mWalletRecentlyVerified && walletJason != null && (walletJason.has(SdkConstants.MAX_LIMIT)) && !walletJason.isNull(SdkConstants.MAX_LIMIT)) {
            loadWalletMaxLimit = walletJason.optDouble(SdkConstants.MAX_LIMIT, 10000.00);
        }

        String walletBalanceInfoForPaymentString = "You can load a minimum of ₹ " + round(loadWalletMinLimit)
                + " and maximum of ₹ " + round(loadWalletMaxLimit)
                + " to your wallet. ₹ " + round(amt_net) + " would be auto-debited from your wallet balance";

        final SdkQustomDialogBuilder alertDialog = new SdkQustomDialogBuilder(SdkHomeActivityNew.this, R.style.PauseDialog);

        View convertView = getLayoutInflater().inflate(R.layout.sdk_load_wallet_and_pay_layout, null);

        ((TextView) convertView.findViewById(R.id.wallet_balance_info_for_payment)).setText(walletBalanceInfoForPaymentString);

        final EditText loadWalletAmountEditText = (EditText) convertView.findViewById(R.id.load_wallet_amount_editText);
        Button loadWalletButton = (Button) convertView.findViewById(R.id.load_wallet_button);
        Button backToHomeButton = (Button) convertView.findViewById(R.id.back_to_home_button);

        if (amt_net > loadWalletMaxLimit) {

            ((TextView) convertView.findViewById(R.id.insufficient_wallet_balance_message_textView)).setText("We are Sorry");
            walletBalanceInfoForPaymentString = "The transaction amount is greater than the maximum load amount (₹ " + loadWalletMaxLimit + ") for this mode of payment.";
            if (mIsUserWalletBlocked) {
                walletBalanceInfoForPaymentString = "Your PayUmoney Wallet is blocked.";
            }
            convertView.findViewById(R.id.load_wallet_container_layout).setVisibility(View.GONE);
            ((TextView) convertView.findViewById(R.id.wallet_balance_info_for_payment)).setVisibility(View.GONE);
            ((TextView) convertView.findViewById(R.id.transaction_amount_greater_message)).setVisibility(View.VISIBLE);
            ((TextView) convertView.findViewById(R.id.transaction_amount_greater_message)).setText(walletBalanceInfoForPaymentString);
            backToHomeButton.setVisibility(View.VISIBLE);
        }

        loadWalletAmountEditText.setText(round(loadWalletMinLimit) + "");
        loadWalletAmountEditText.setSelection(loadWalletAmountEditText.getText().toString().length());
        loadWalletAmountEditText.addTextChangedListener(new LoadWalletAmountEditTextTextWatcher(loadWalletAmountEditText, loadWalletButton, loadWalletMinLimit));

        final AlertDialog dialog = alertDialog.setView(convertView).show();

        loadWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateProgressDialog();
                if (fromPayUMoneyApp) {
                    sdkSession.loadWallet(null, loadWalletAmountEditText.getText().toString(), paymentId, "PayUmoney App");
                } else if (fromPayUBizzApp) {
                    sdkSession.loadWallet(null, loadWalletAmountEditText.getText().toString(), paymentId, "PayUBizz App");
                } else {
                    sdkSession.loadWallet(null, loadWalletAmountEditText.getText().toString(), paymentId, map.get(SdkConstants.PRODUCT_INFO));
                }
                dialog.dismiss();
            }
        });

        backToHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                close(PAYMNET_CANCELLED);
                sdkSession.notifyUserCancelledTransaction(paymentId, "1");
            }
        });
    }

    public void userWalletNotRegisteredDialog() {

        String walletBalanceInfoForPaymentString = "User wallet is not active and registered phone number not found. Please contact PayUmoney Customer care.";

        final SdkQustomDialogBuilder alertDialog = new SdkQustomDialogBuilder(SdkHomeActivityNew.this, R.style.PauseDialog);

        View convertView = getLayoutInflater().inflate(R.layout.sdk_load_wallet_and_pay_layout, null);

        ((TextView) convertView.findViewById(R.id.wallet_balance_info_for_payment)).setText(walletBalanceInfoForPaymentString);

        Button backToHomeButton = (Button) convertView.findViewById(R.id.back_to_home_button);

        ((TextView) convertView.findViewById(R.id.insufficient_wallet_balance_message_textView)).setText("We are Sorry");
        convertView.findViewById(R.id.load_wallet_container_layout).setVisibility(View.GONE);
        ((TextView) convertView.findViewById(R.id.wallet_balance_info_for_payment)).setVisibility(View.GONE);
        ((TextView) convertView.findViewById(R.id.transaction_amount_greater_message)).setVisibility(View.VISIBLE);
        ((TextView) convertView.findViewById(R.id.transaction_amount_greater_message)).setText(walletBalanceInfoForPaymentString);
        backToHomeButton.setVisibility(View.VISIBLE);

        final AlertDialog dialog = alertDialog.setView(convertView).show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        backToHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                close(PAYMNET_CANCELLED);
                sdkSession.notifyUserCancelledTransaction(paymentId, "1");
            }
        });
    }

    public void setAmountConvenience(String currentPaymentMode) throws JSONException {

        if (currentPaymentMode.equals(SdkConstants.WALLET_STRING) || currentPaymentMode.isEmpty()) {
            amt_convenience = 0.0;
        } else if (convenienceChargesObject != null && convenienceChargesObject.has(mode) && !convenienceChargesObject.isNull(mode)) {
            amt_convenience = convenienceChargesObject.getJSONObject(mode).getDouble(SdkConstants.DEFAULT);
        } else {
            amt_convenience = 0.0;
        }

        if (convenienceChargesObject != null && convenienceChargesObject.has(SdkConstants.WALLET_STRING)
                && !convenienceChargesObject.isNull(SdkConstants.WALLET_STRING)){
            amt_convenience_wallet = convenienceChargesObject.getJSONObject(SdkConstants.WALLET_STRING).getDouble(SdkConstants.DEFAULT);
        }

        /*if (amt_convenience_wallet <= 0.0) {
            amt_convenience_wallet = Math.max(convenienceChargesObject.getJSONObject(SdkConstants.PAYMENT_MODE_DC).getDouble(SdkConstants.DEFAULT), (amount * SdkConstants.FIXED_CONVENIENCE_CHARGES_COMPONENT));
        }*/
    }

    private class LoadWalletAmountEditTextTextWatcher implements TextWatcher {

        EditText loadAmountEditText;
        Button loadWalletButton;
        double loadWalletMinLimit;

        LoadWalletAmountEditTextTextWatcher(EditText editText, Button button, double minLimit) {
            loadAmountEditText = editText;
            loadWalletButton = button;
            loadWalletMinLimit = minLimit;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (loadWalletButton != null && loadAmountEditText != null
                    && loadAmountEditText.getText() != null
                    && loadAmountEditText.getText().toString() != null
                    && isDouble(loadAmountEditText.getText().toString())
                    && Double.parseDouble(loadAmountEditText.getText().toString()) >= loadWalletMinLimit
                    && Double.parseDouble(loadAmountEditText.getText().toString()) <= loadWalletMaxLimit) {
                loadWalletButton.setEnabled(true);
            } else {
                loadWalletButton.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }

    }

    boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void initiateProgressDialog() {
        if (mProgressDialog != null) {
            if (!mProgressDialog.isShowing()) {
                mProgressDialog.show();
            }
        } else {
            mProgressDialog = showProgress(this);
        }
    }

    public void showWallet(double dsc, final double net) {
        new SdkQustomDialogBuilder(this, R.style.PauseDialog).
                setTitleColor(SdkConstants.WHITE).
                setDividerColor(SdkConstants.greenPayU)
                .setTitle("Payment using Wallet")
                .setMessage("Yoo-hoo!\n" +
                        "\n" +
                        "You have enough money in PayUMoney Wallet for this transaction. All you need to do is confirm the payment by clicking on the OK button below and that's it." + "\n\nWallet Money Used : Rs." + round(net) + "\nRemaining Money in Wallet : Rs." + round(walletBal))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (!SdkHelper.checkNetwork(SdkHomeActivityNew.this)) {
                                    Toast.makeText(SdkHomeActivityNew.this, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                                } else {
                                    // Your code
                                    try {
                                        mProgressDialog.show();
                                        SdkSession.getInstance(SdkHomeActivityNew.this).sendToPayU(details, SdkConstants.WALLET, data, Double.valueOf(net), Double.valueOf(discount), amt_convenience); //PURE WALLEt
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
                        "You have enough money in PayUMoney Wallet for this transaction. All you need to do is confirm the payment by clicking on the OK button below and that's it." + "\n\nWallet Money Used : Rs." + round(net) + "\nRemaining Money in Wallet : Rs." + round(walletBal))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (!SdkHelper.checkNetwork(SdkHomeActivityNew.this)) {
                            Toast.makeText(SdkHomeActivityNew.this, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                        } else {
                            // Your code
                            try {
                                mProgressDialog.show();
                                SdkSession.getInstance(SdkHomeActivityNew.this).sendToPayUWithWallet(details, SdkConstants.WALLET, data, Double.valueOf(net), Double.valueOf(pnts), Double.valueOf(discount), amt_convenience); //wallet +pnts
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
                /*.setNegativeButton("Choose Other Mode", new DialogInterface.OnClickListener() {//giving user the option to pay without points

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseOtherMode = true;
                        userPoints = 0.0;
                        updateWalletDetails();
                        if (!payByWalletButton.isShown()) {
                            paymentModesList.setVisibility(View.VISIBLE);
                        }
                        if (fromPayUMoneyApp || fromPayUBizzApp)
                            startPayment(appResponse);
                        *//*else if(coupan_amt > 0.0)
                            paymentModesList.setVisibility(View.VISIBLE);*//*
                        else
                            startPayment(null);


                    }
                })*/
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAmount.setText("0.0");
                        savings.setText("Sufficient PayUPoints");
                        walletBoxLayout.setVisibility(View.GONE);
                        mOrderSummary.setVisibility(View.GONE);
                        payByWalletButton.setVisibility(View.GONE);
                        /*mViewPager.setVisibility(View.GONE);
                        pagerContainerLayout.setVisibility(View.VISIBLE);*/
                        couponLayout.setVisibility(View.GONE);
                        //tabs.setVisibility(View.GONE);
                        // Your code
                        /*hiding checkbox for one tap feature*/
                        mOneTap.setVisibility(View.GONE);
                        mCvvTnCLink.setVisibility(View.GONE);


                        SdkPayUMoneyPointsFragment fragment = new SdkPayUMoneyPointsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("details", details.toString());
                        bundle.putDouble("userPoints", userPoints);
                        bundle.putDouble("discount", discount);
                        bundle.putDouble("cashback", cashback);
                        bundle.putDouble("couponAmount", coupan_amt);
                        bundle.putDouble("convenienceChargesAmount", amt_convenience_wallet);

                        fragment.setArguments(bundle);
                        FragmentTransaction transaction;
                        transaction = getFragmentManager().beginTransaction().setCustomAnimations(
                                R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                                R.animator.card_flip_left_in, R.animator.card_flip_left_out);
                        //((HomeActivity) SdkHomeActivityNew.this).onPaymentOptionSelected(details);
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

    public static BigDecimal round(float d) {

        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    public static BigDecimal round(double d) {

        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
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
                updateDetails(SdkConstants.WALLET_STRING);
                payByWalletButton.setVisibility(View.VISIBLE);//bugfix
                mOneTap.setVisibility(View.GONE);
                mCvvTnCLink.setVisibility(View.GONE);
                paymentModesList.setVisibility(View.GONE);
                walletFlag = true;

            } else if ((amount + amt_convenience - userPoints - amt_discount) > walletAmount && (amount + amt_convenience_wallet - userPoints - amt_discount) > 0) {

                walletUsage = walletAmount;
                walletBal = 0.0;
                updateDetails(mode);
                walletFlag = false;
                payByWalletButton.setVisibility(View.GONE);//bugfix
                if (mOneTap != null && mOneTap.getVisibility() != View.VISIBLE && (mode.equals(SdkConstants.PAYMENT_MODE_CC) || mode.equals(SdkConstants.PAYMENT_MODE_DC) || mode.isEmpty())) {
                    mOneTap.setVisibility(View.VISIBLE);
                    mCvvTnCLink.setVisibility(View.VISIBLE);
                }
                paymentModesList.setVisibility(View.VISIBLE);
            } else if ((amount + amt_convenience_wallet - userPoints - amt_discount) > 0) {//wallet is fat enough to pay

                amt_convenience = amt_convenience_wallet;
                walletUsage = amount + amt_convenience - userPoints - amt_discount;
                walletBal = walletAmount - walletUsage;
                updateDetails(SdkConstants.WALLET_STRING);
                payByWalletButton.setVisibility(View.VISIBLE);//bugfix
                mOneTap.setVisibility(View.GONE);
                mCvvTnCLink.setVisibility(View.GONE);
                paymentModesList.setVisibility(View.GONE);
                walletFlag = true;
            } else if ((amount + amt_convenience_wallet - userPoints - amt_discount) <= 0) {
                walletUsage = 0;
                walletBal = walletAmount - walletUsage;
                updateDetails(SdkConstants.WALLET_STRING);
                walletFlag = false;
            }

            walletBalance.setText(getString(R.string.remaining_wallet_bal) + " " + round(walletBal));
        }
    }

    private void handleOneClickAndOneTapFeature(JSONObject userDto) {
        SharedPreferences.Editor editor = this.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE).edit();
        try {
            if (userDto.has(SdkConstants.CONFIG_DTO) && !userDto.isNull(SdkConstants.CONFIG_DTO)) {
                JSONObject userConfigDtoTmp = userDto.getJSONObject(SdkConstants.CONFIG_DTO);
                String salt = PayUmoneySdkInitilizer.IsDebugMode() ? SdkConstants.AUTHORIZATION_SALT_TEST : SdkConstants.AUTHORIZATION_SALT_PROD;
                if (userConfigDtoTmp.has(SdkConstants.AUTHORIZATION_SALT) && !userConfigDtoTmp.isNull(SdkConstants.AUTHORIZATION_SALT)) {
                    if (salt.equals(userConfigDtoTmp.optString(SdkConstants.AUTHORIZATION_SALT, SdkConstants.XYZ_STRING))) {
                        editor.putBoolean(SdkConstants.ONE_CLICK_PAYMENT, false);
                        editor.putBoolean(SdkConstants.ONE_TAP_FEATURE, false);
                    } else {
                        editor.putBoolean(SdkConstants.ONE_CLICK_PAYMENT, userConfigDtoTmp.optBoolean(SdkConstants.ONE_CLICK_PAYMENT, false));
                        editor.putString(SdkConstants.CONFIG_DTO, userConfigDtoTmp.toString());
                        if (userConfigDtoTmp.has(SdkConstants.ONE_TAP_FEATURE) && !userConfigDtoTmp.isNull(SdkConstants.ONE_TAP_FEATURE)) {
                            boolean temp = userConfigDtoTmp.optBoolean(SdkConstants.ONE_TAP_FEATURE, false);
                            editor.putBoolean(SdkConstants.ONE_TAP_FEATURE, temp);

                            changeDebitCardCheckBoxLable(temp);

                            if (temp) {
                                mOneTap.setChecked(true);

                            } else {
                                mOneTap.setChecked(false);
                            }

                            if (firstTimeFetchingOneClickFlag)
                                firstTimeFetchingOneClickFlag = false;

                        }
                    }
                }

            }
            editor.commit();
            editor.apply();
        } catch (Exception e) {

            editor.putBoolean(SdkConstants.ONE_TAP_FEATURE, false);
            editor.putBoolean(SdkConstants.ONE_CLICK_PAYMENT, false);
            mOneTap.setChecked(false);

            editor.commit();
            editor.apply();
            e.printStackTrace();
        }

    }

    private void changeDebitCardCheckBoxLable(Boolean oneTap) {

        View v = findViewById(R.id.pagerContainer);
        if (v != null) {
            CheckBox c = (CheckBox) v.findViewById(R.id.store_card);
            TextView t = (TextView) v.findViewById(R.id.sdk_tnc);
            if (c != null && t != null) {
                if (oneTap) {
                    c.setText("");
                    t.setVisibility(View.VISIBLE);
                } else {
                    c.setText("Save this card");
                    t.setVisibility(View.GONE);
                }
            }
        }

    }

    /*private void showLessThanMinimumWalletLoadAmountAlertDialog(final double minimumLoadWalletAmount) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SdkHomeActivityNew.this, AlertDialog.THEME_HOLO_LIGHT);
        dialogBuilder.setTitle("Add More Money")
                .setMessage("Minimum load amount for wallet is ₹" + minimumLoadWalletAmount + " ,  ₹" + (minimumLoadWalletAmount - amt_net) + " will be credited to your wallet.")
                .setPositiveButton("Load&Pay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        initiateProgressDialog();
                        sdkSession.loadWallet(null, minimumLoadWalletAmount + "", paymentId, map.get(SdkConstants.PRODUCT_INFO));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }


    private void showAlertDialog() {

        dismissProgress();
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SdkConstants.SP_SP_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(SdkConstants.STORED_DATE).commit();

        if (splashDialog == null) {
            alertDialogBuilder = new AlertDialog.Builder(SdkHomeActivityNew.this, AlertDialog.THEME_HOLO_LIGHT);
            alertDialogBuilder.setTitle("Great News!")
                    .setMessage("We have brand new app waiting for you. Your current version of app is not supported.")
                    .setNeutralButton("Update", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Update
                            final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        }
                    });

            splashDialog = alertDialogBuilder.create();
            splashDialog.setCanceledOnTouchOutside(false);
            splashDialog.setCancelable(false);
            splashDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    splashDialog = null;
                }
            });
            splashDialog.show();
        }

        if (splashDialog != null && !splashDialog.isShowing() && !isFinishing()) {
            splashDialog.show();
        }

    }*/
}
