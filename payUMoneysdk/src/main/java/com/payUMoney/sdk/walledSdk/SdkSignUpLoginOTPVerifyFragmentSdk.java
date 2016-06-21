package com.payUMoney.sdk.walledSdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.gsm.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;


public class SdkSignUpLoginOTPVerifyFragmentSdk extends SdkBaseFragment {

    private EditText mobile/*, name*/;

    private ProgressBar pbwaitotp;

    public EditText otp;

    public TextView resend;

    public Pattern p = Pattern.compile("(|^)\\d{6}");  //<--assuming OTP will always be 6 digit.

    private Button proceed, cancelButton/*, back*/;

    private RelativeLayout humble;

    private BroadcastReceiver receiver; // <--BroadCast Receiver dynamic register unregister


    private TextView info;

    private String email, number = null;
    WalletSdkLoginSignUpActivity mSdkLoginSignUpActivity;
    private HashMap<String, String> userParams;

    @Override
    public void onCreate(Bundle save) {
        super.onCreate(save);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View walletPayment = inflater.inflate(R.layout.walletsdk_fragment_otpverify_signup_login, container, false);

        /*
        if OTP is not verified we are not considering that user as logged IN..
        so getting Email and Number value from Bundle , if user verifies OTP then we would save
        email , number and token etc in SharedPreferences
        */

        mSdkLoginSignUpActivity = (WalletSdkLoginSignUpActivity) getActivity();
        userParams = mSdkLoginSignUpActivity.getMapObject();

        if (getArguments() != null) {
            number = getArguments().getString(SdkConstants.PHONE);
            email = getArguments().getString(SdkConstants.EMAIL);
            if (number == null)
                new IllegalStateException("No Mobile number found on for OTP through bundle");
        } else {
            new IllegalStateException("No Mobile number found on for OTP through bundle");
        }

        //Initialize the UI
        resend = (TextView) walletPayment.findViewById(R.id.resend);
        mobile = (EditText) walletPayment.findViewById(R.id.mobile);
        otp = (EditText) walletPayment.findViewById(R.id.otp);
        proceed = (Button) walletPayment.findViewById(R.id.activate);
        info = (TextView) walletPayment.findViewById(R.id.info);
        humble = (RelativeLayout) walletPayment.findViewById(R.id.humble);
        pbwaitotp = (ProgressBar) walletPayment.findViewById(R.id.pbwaitotp);

        /*if (number != null)
            mobile.setText(number.toString());*/

        proceed.setEnabled(false);
        proceed.setText(getString(R.string.activate));
        otp.setVisibility(View.VISIBLE);
        humble.setVisibility(View.VISIBLE);
        pbwaitotp.setVisibility(View.VISIBLE);
        info.setText(getString(R.string.waiting_for_otp));
        mobile.setVisibility(View.GONE);

        cancelButton = (Button) walletPayment.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (SdkHelper.isValidClick())
                    mSdkLoginSignUpActivity.close(mSdkLoginSignUpActivity.PAYMENT_CANCELLED, null);
            }
        });

        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SdkHelper.checkNetwork(mSdkLoginSignUpActivity)) {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.disconnected_from_internet), true);
                    return;
                }

                if(SdkHelper.isValidClick()){
                    setResetOtpButton(false);
                    SdkSession.getInstance(mSdkLoginSignUpActivity).sendMobileVerificationCode(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.EMAIL)
                            , SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.PHONE));
                }
            }
        });

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!SdkHelper.checkNetwork(mSdkLoginSignUpActivity)) {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.disconnected_from_internet), true);
                    return;
                }
                if (proceed.getText().equals(getString(R.string.activate)) && !otp.getText().toString().equals("")) {

                    setResetViews(false);

                    Button proceedButton = (Button) v;

                    proceedButton.setEnabled(false);

                    SdkSession.getInstance(mSdkLoginSignUpActivity).verifyUserCredential(email, number, otp.getText().toString());

                } else if (otp.getText().equals("")) {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.waiting_for_otp), true);
                }
            }
        });

        otp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.toString().trim().length() == 6) {

                    pbwaitotp.setVisibility(View.GONE);
                    info.setText("Verify OTP");
                    proceed.setEnabled(true);

                } else {
                    proceed.setEnabled(false);
                    pbwaitotp.setVisibility(View.VISIBLE);
                    info.setText(getString(R.string.waiting_for_otp));
                }

            }
        });

        mSdkLoginSignUpActivity.showHideLogoutButton(false);
        mSdkLoginSignUpActivity.invalidateActivityOptionsMenu();
        return walletPayment;
    }

    private void setResetOtpButton(boolean visibility) {
        resend.setText(visibility ? mSdkLoginSignUpActivity.getResources().getString(R.string.retry_otp) : mSdkLoginSignUpActivity.getResources().getString(R.string.processing));
        resend.setEnabled(visibility);
    }

    private void setResetViews(boolean visibility){
        info.setText(visibility ? getString(R.string.waiting_for_otp) : getString(R.string.activating));
        pbwaitotp.setVisibility(visibility ? View.GONE : View.VISIBLE);
        mobile.setEnabled(false);
        pbwaitotp.setVisibility(visibility ? View.VISIBLE : View.GONE);
        otp.setEnabled(visibility);
    }
    public void onEventMainThread(final SdkCobbocEvent event) {
        if (event.getType() == SdkCobbocEvent.OPEN_SEND_OTP_VERIFICATION) {
            if (event.getStatus()) {

                SdkHelper.showToastMessage(mSdkLoginSignUpActivity, mSdkLoginSignUpActivity.getResources().getString(R.string.login_successful), false);
                if(userParams != null && userParams.containsKey(SdkConstants.IS_HISTORY_CALL)){
                    mSdkLoginSignUpActivity.startShowingHistory();
                } else {
                    SdkSession.getInstance(mSdkLoginSignUpActivity).getUserVaults();
                }
            } // end if(event.getstatus())
            else {
                setResetViews(true);

                if(event.getValue() != null && event.getValue().toString().trim().toLowerCase().contains(SdkConstants.WALLET_ALREADY_EXIST_STRING)){
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, event.getValue().toString(), true);
                    mSdkLoginSignUpActivity.loadSignUpFragment(true);
                } else if(event.getValue() != null){
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, event.getValue().toString(), true);
                } else {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.something_went_wrong), true);
                }
            }
        } else if (event.getType() == SdkCobbocEvent.USER_VAULT) {

            if (event.getStatus()) {
                checkForWalletOnlyPayment();
            } else {
                if (!SdkHelper.checkNetwork(mSdkLoginSignUpActivity)) {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.disconnected_from_internet), true);
                } else {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.something_went_wrong), true);
                }

            }
        } else if (event.getType() == SdkCobbocEvent.OPEN_REGISTER_USING_OTP_AND_SIGNIN) {
            if (event.getStatus()) {

                if(event != null && event.getValue() != null && event.getValue().toString().equals(SdkConstants.CUSTOMER_REGISTERED)){
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, "OTP sent to Phone Number " + SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.PHONE), false);
                }
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setResetOtpButton(true);
                    }
                }, 1000);

            } else {
                if (event.getValue() != null) {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, event.getValue().toString(), true);
                } else {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.something_went_wrong), true);
                }
                setResetOtpButton(true);
            }
        }
    }

    private void checkForWalletOnlyPayment() {

        double amount = Double.parseDouble(mSdkLoginSignUpActivity.getMapObject().get(SdkConstants.AMOUNT));
        double walletAmount = Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.WALLET_BALANCE));
        if (walletAmount > amount) {
            mSdkLoginSignUpActivity.inflateWalletPaymentFragment(true);
        } else {
            mSdkLoginSignUpActivity.inflateLoadWalletFragment(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mSdkLoginSignUpActivity != null && receiver != null){
            mSdkLoginSignUpActivity.unregisterReceiver(receiver);
        }
    }

    @Override
    public void onResume()   //< -- register and initialize receiver here. What if server gives 401? user will come here.
    {
        super.onResume();

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub

                String msgBody = null;

                if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                    Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
                    SmsMessage[] msgs = null;
                    String msg_from;
                    if (bundle != null && otp != null) {
                        //---retrieve the SMS message received---
                        try {
                            Object[] pdus = (Object[]) bundle.get("pdus");
                            msgs = new SmsMessage[pdus.length];
                            for (int i = 0; i < msgs.length; i++)   //Msg Read
                            {
                                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                                msg_from = msgs[i].getOriginatingAddress();
                                msgBody = msgs[i].getMessageBody();
                            }
                        /*
                        * Now extract the otp*/
                            if (msgBody != null && msgBody.toLowerCase().contains("verification")) {//make sure from payumoney.com
                                Matcher m = p.matcher(msgBody);
                                if (m.find()) {
                                    otp.setText(m.group(0));
                                } else {
                                    SdkHelper.showToastMessage(getActivity(), "Couldn't read sms, please enter OTP manually", true);
                                }
                            }
                        } catch (Exception e) {
                            SdkHelper.showToastMessage(getActivity(), "Couldn't read sms, please enter OTP manually", true);
                        }
                    } else {
                        SdkHelper.showToastMessage(getActivity(), "Couldn't read sms, please enter OTP manually", true);
                    }
                }
            }
        };

        mSdkLoginSignUpActivity.registerReceiver(receiver, filter);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
}
