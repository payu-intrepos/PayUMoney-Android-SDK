package com.payUMoney.sdk;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.TextView;

import com.payUMoney.sdk.fragment.SdkForgotPasswordFragment;
import com.payUMoney.sdk.fragment.SdkLoginFragment;
import com.payUMoney.sdk.fragment.SdkSignUpFragment;
import com.payUMoney.sdk.walledSdk.SharedPrefsUtils;


public class SdkLoginSignUpActivity extends FragmentActivity {

    public final int RESULT_BACK = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String allowGuestCheckoutValue = getIntent().getStringExtra(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_VALUE);
        setContentView(R.layout.sdk_activity_login_sign_up);
        setTitle(R.string.app_name);

        ((TextView)findViewById(R.id.login_tab)).setAllCaps(true);
        ((TextView)findViewById(R.id.sign_up_tab)).setAllCaps(true);
        if(allowGuestCheckoutValue != null && allowGuestCheckoutValue.equals(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_ONLY)){
            findViewById(R.id.sign_up_tab).setVisibility(View.GONE);
            findViewById(R.id.titleDivider).setVisibility(View.GONE);
        }
        findViewById(R.id.login_tab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SdkSession.getInstance(SdkLoginSignUpActivity.this).cancelPendingRequests(SdkSession.TAG);
                loadLoginFragment(true);

            }
        });
        findViewById(R.id.sign_up_tab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SdkSession.getInstance(SdkLoginSignUpActivity.this).cancelPendingRequests(SdkSession.TAG);

                findViewById(R.id.sign_up_tab).setAlpha(1);
                findViewById(R.id.login_tab).setAlpha(0.2f);
                if (((TextView) findViewById(R.id.sign_up_tab)).getCurrentTextColor() != getResources().getColor(android.R.color.white)) {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
                    SdkSignUpFragment signUpFragment = new SdkSignUpFragment();
                    fragmentTransaction.replace(R.id.login_signup_fragment_container, signUpFragment, SdkSignUpFragment.class.getName());
                    fragmentTransaction.commitAllowingStateLoss();
                    ((TextView) findViewById(R.id.login_tab)).setTextColor(getResources().getColor(android.R.color.black));
                    ((TextView) findViewById(R.id.sign_up_tab)).setTextColor(getResources().getColor(android.R.color.white));
                }
            }
        });

        loadLoginFragment(false);
    }

    private void loadLoginFragment(boolean animate) {

        findViewById(R.id.sign_up_tab).setAlpha(0.2f);
        findViewById(R.id.login_tab).setAlpha(1);

        if(((TextView) findViewById(R.id.login_tab)).getCurrentTextColor() != getResources().getColor(android.R.color.white)){
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (animate)
                fragmentTransaction.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit);
            SdkLoginFragment loginFragment = new SdkLoginFragment();
            fragmentTransaction.replace(R.id.login_signup_fragment_container, loginFragment, SdkLoginFragment.class.getName());
            fragmentTransaction.commitAllowingStateLoss();
            ((TextView)findViewById(R.id.login_tab)).setTextColor(getResources().getColor(android.R.color.white));
            ((TextView)findViewById(R.id.sign_up_tab)).setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    public void loadForgotPasswordFragment(boolean animate) {
        findViewById(R.id.login_tab).setAlpha(0.2f);
        findViewById(R.id.sign_up_tab).setAlpha(0.2f);

        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit);
        SdkForgotPasswordFragment loginFragment = new SdkForgotPasswordFragment();
        fragmentTransaction.replace(R.id.login_signup_fragment_container, loginFragment, SdkForgotPasswordFragment.class.getName());
        fragmentTransaction.addToBackStack(SdkForgotPasswordFragment.class.getName());
        fragmentTransaction.commitAllowingStateLoss();
        ((TextView)findViewById(R.id.login_tab)).setTextColor(getResources().getColor(android.R.color.black));
        ((TextView)findViewById(R.id.sign_up_tab)).setTextColor(getResources().getColor(android.R.color.black));
    }

    public void close() {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPrefsUtils.setStringPreference(this, SdkConstants.USER_SESSION_COOKIE_PAGE_URL, this.getClass().getSimpleName());
    }
}
