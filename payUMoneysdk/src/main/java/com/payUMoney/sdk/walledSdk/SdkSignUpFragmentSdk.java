package com.payUMoney.sdk.walledSdk;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

public class SdkSignUpFragmentSdk extends SdkBaseFragment {

    private WalletSdkLoginSignUpActivity mSdkLoginSignUpActivity;
    private Button mSignUp, cancelButton;
    //@Required(order = 1, message = "Your Email is Required")
    //@Email(order = 2, message = "This Email appears to be Invalid")
    private AutoCompleteTextView mEmail;
    //@Required(order = 3, message = "Please enter your Phone Number")
    //@Regex(order = 4, message = "This Phone Number appears to be Invalid", pattern = "([\\d]{10})", trim = true)
    private AutoCompleteTextView mPhone;
    public static final String TAG = "---viswash---" + SdkSignUpFragmentSdk.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.walletasdk_activity_signup, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSdkLoginSignUpActivity = (WalletSdkLoginSignUpActivity) getActivity();

        mEmail = (AutoCompleteTextView) view.findViewById(R.id.email);

        /*Account[] accounts = AccountManager.get(mSdkLoginSignUpActivity).getAccounts();
        Set<String> emailSet = new HashSet<String>();
        for (Account account : accounts) {
            if (SdkConstants.EMAIL_PATTERN.matcher(account.name).matches()) {
                emailSet.add(account.name);
            }
        }*/
        Set<String> emailSet = new HashSet<String>();
        mEmail.setAdapter(new ArrayAdapter<String>(mSdkLoginSignUpActivity, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>(emailSet)));
        mEmail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mEmail.showDropDown();
                }
                return false;
            }
        });

        mPhone = (AutoCompleteTextView) view.findViewById(R.id.phone_number);

        mSignUp = (Button) view.findViewById(R.id.done);
        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(mEmail == null || mEmail.getText() == null || mEmail.getText().toString().trim().isEmpty()){
                    onValidationFailed(mEmail, "Your Email is Required");
                    return;
                } else if(mEmail != null && !SdkConstants.EMAIL_PATTERN.matcher(mEmail.getText().toString()).matches()){
                    onValidationFailed(mEmail, "This Email appears to be Invalid");
                    return;
                } else if(mPhone == null || mPhone.getText() == null || mPhone.getText().toString().trim().isEmpty()){
                    onValidationFailed(mEmail, "Please enter your Phone Number");
                    return;
                } else if(mPhone != null && !SdkConstants.PHONE_PATTERN.matcher(mPhone.getText().toString()).matches()){
                    onValidationFailed(mPhone, "This Phone Number appears to be Invalid");
                    return;
                } else if (!SdkHelper.checkNetwork(mSdkLoginSignUpActivity)) {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.disconnected_from_internet), true);
                    return;
                }
                onValidationSucceeded();
            }
        });

        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(SdkHelper.isValidClick())
                    mSdkLoginSignUpActivity.close(mSdkLoginSignUpActivity.PAYMENT_CANCELLED, null);
            }
        });

        ((TextView) view.findViewById(R.id.tos_n_privacy)).setMovementMethod(LinkMovementMethod.getInstance());
        mSdkLoginSignUpActivity.showHideLogoutButton(false);
        mSdkLoginSignUpActivity.setTabNewTitle(getString(R.string.walletsdk_sign_up));
        mSdkLoginSignUpActivity.setTabVisibility();
        mSdkLoginSignUpActivity.invalidateActivityOptionsMenu();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSdkLoginSignUpActivity != null)
            mSdkLoginSignUpActivity.setTabVisibility();
    }

    @Override
    public void onPause() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onEventMainThread(SdkCobbocEvent event) {
        if (event.getType() == SdkCobbocEvent.OPEN_REGISTER_USING_OTP_AND_SIGNIN) {
            if (event.getStatus()) {

                if(event != null && event.getValue() != null && event.getValue().toString().equals(SdkConstants.CUSTOMER_REGISTERED)){
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, "OTP sent to Phone Number " + SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.PHONE), false);
                }
                
                Bundle bundle = new Bundle();
                bundle.putString(SdkConstants.EMAIL, mEmail.getText().toString());
                bundle.putString(SdkConstants.PHONE, mPhone.getText().toString());


                FragmentTransaction fragmentTran = mSdkLoginSignUpActivity.getSupportFragmentManager().beginTransaction();
                fragmentTran.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
                SdkSignUpLoginOTPVerifyFragmentSdk homeFragmentMoreOptions = new SdkSignUpLoginOTPVerifyFragmentSdk();

                homeFragmentMoreOptions.setArguments(bundle);

                fragmentTran.replace(R.id.login_signup_fragment_container, homeFragmentMoreOptions, SdkSignUpLoginOTPVerifyFragmentSdk.class.getName());
                fragmentTran.addToBackStack(SdkSignUpLoginOTPVerifyFragmentSdk.class.getName());
                fragmentTran.commitAllowingStateLoss();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setResetButton(true);
                    }
                }, 1000);

            } else {
                // so we have an error
                if (event.getValue() != null) {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, event.getValue().toString(), true);
                } else {
                    SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.something_went_wrong), true);
                }
                setResetButton(true);
            }
        }
    }

    void setResetButton(boolean visibility) {
        mSignUp.setText(visibility ? R.string.continue_string : R.string.please_wait);
        mSignUp.setEnabled(visibility);
        cancelButton.setEnabled(visibility);
    }


    public void onValidationSucceeded() {
        if (mPhone.getText().toString().charAt(0) < '6') {
            SdkHelper.showToastMessage(mSdkLoginSignUpActivity, "Please Enter Valid Phone Number.", true);
            mPhone.requestFocus();
        } else {
            if (SdkHelper.checkNetwork(mSdkLoginSignUpActivity)) {

                SdkSession.getInstance(mSdkLoginSignUpActivity).sendMobileVerificationCode(mEmail.getText().toString(), mPhone.getText().toString());
                setResetButton(false);
            } else {
                SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.disconnected_from_internet), true);
                mSignUp.requestFocus();
            }
        }
    }

    public void onValidationFailed(View view, String msg) {
        SdkHelper.showToastMessage(mSdkLoginSignUpActivity, msg, true);
        view.requestFocus();
    }
}
