package com.payUMoney.sdk.walledSdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkHomeActivityNew;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;

import org.json.JSONObject;

import java.util.HashMap;

import de.greenrobot.event.EventBus;

public class WalletSdkLoginSignUpActivity extends FragmentActivity {

    public final int RESULT_QUIT = 5;
    public final int PAYMENT_SUCCESS = 3;
    public final int PAYMENT_CANCELLED = 4;
    private final int WALLET_HISTORY = 6;
    private final int MORE_OPTIONS_MARGIN_BOTTOM = 9;
    private TextView titleTextView;
    private final int MORE_OPTIONS_MARGIN_LEFT = 10;
    private HashMap<String, String> userParams;
    private PopupWindow popupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down);

        setContentView(R.layout.walletsdk_activity_login_sign_up);

        titleTextView = (TextView) findViewById(R.id.pages_tabs);

        findViewById(R.id.main_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleMoreOptionsClickListener();
            }
        });

        userParams = (HashMap<String, String>) getIntent().getSerializableExtra(SdkConstants.PARAMS);

        checkForRequestType();
    }

    public void startShowingHistory() {
        // Show History
        Intent intent = new Intent(this, SdkHistoryActivity.class);
        intent.putExtra(SdkConstants.PARAMS, userParams);
        startActivityForResult(intent, WALLET_HISTORY);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (findViewById(R.id.more_options_imageView).getVisibility() == View.VISIBLE)
            menu.add(Menu.NONE, R.id.logout, menu.size(), R.string.logout).setIcon(R.drawable.logout).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        else
            menu.clear();
        return super.onCreateOptionsMenu(menu);
    }

    public void invalidateActivityOptionsMenu() {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            SdkSession.getInstance(this).logout();
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkForRequestType() {

        if (SdkSession.getInstance(this).isLoggedIn()) {
            // user logged in, check for payment or history call

            if (userParams.containsKey(SdkConstants.IS_HISTORY_CALL)) {

                startShowingHistory();

            } else {
                //wallet balance to know we can pay without loading wallet or not
                SdkHelper.showProgressDialog(this, "Getting Wallet Balance");
                SdkSession.getInstance(this).getUserVaults();
            }
        } else {
            // make user login first
            loadSignUpFragment(false);
        }
    }

    public void dismissLogoutButton() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public void showHideLogoutButton(boolean visibility) {
        findViewById(R.id.more_options_imageView).setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.more_options_imageView1).setVisibility(visibility ? View.INVISIBLE : View.INVISIBLE);
    }

    private void handleMoreOptionsClickListener() {

        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }

        LayoutInflater layoutInflater
                = (LayoutInflater) getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.layout.walletsdk_screen_popup, null);
        popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.showAtLocation(findViewById(R.id.more_options_imageView), Gravity.TOP | Gravity.RIGHT, MORE_OPTIONS_MARGIN_LEFT, MORE_OPTIONS_MARGIN_BOTTOM + getPixelValue(MORE_OPTIONS_MARGIN_BOTTOM));

        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);

        Button btnClosePopup = (Button) popupView.findViewById(R.id.btn_close_popup);
        btnClosePopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SdkHelper.checkNetwork(WalletSdkLoginSignUpActivity.this)) {
                    SdkHelper.showToastMessage(WalletSdkLoginSignUpActivity.this, getString(R.string.disconnected_from_internet), true);
                } else {
                    dismissLogoutButton();
                    SdkHelper.showProgressDialog(WalletSdkLoginSignUpActivity.this, "Logging Out");
                    SdkSession.getInstance(WalletSdkLoginSignUpActivity.this).logout();
                }
            }
        });
    }

    public HashMap<String, String> getMapObject() {
        return userParams;
    }

    public void setTabNewTitle(String newTitle) {
        titleTextView.setText(newTitle);
    }

    private void checkForWalletOnlyPayment() {

        double amount = Double.parseDouble(userParams.get(SdkConstants.AMOUNT));
        double walletAmount = Double.parseDouble(SharedPrefsUtils.getStringPreference(this, SharedPrefsUtils.Keys.WALLET_BALANCE));
        if (walletAmount >= amount) {
            inflateWalletPaymentFragment(false);
        } else {
            inflateLoadWalletFragment(false);
        }
    }

    public void callSdkToLoadWallet(JSONObject paymentDetailsObject) {

        /*start from here*/
        Intent intent = new Intent(this, SdkHomeActivityNew.class);
        intent.putExtra(SdkConstants.PARAMS, userParams);
        intent.putExtra(SdkConstants.PAYMENT_DETAILS_OBJECT, paymentDetailsObject.toString());

        startActivityForResult(intent, PAYMENT_SUCCESS); //Start the Home Activity

    }

    public void inflateWalletPaymentFragment(boolean animate) {

        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (animate)
            fragmentTransaction.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit);
        SdkWalletPaymentFragment loginFragment = new SdkWalletPaymentFragment();
        fragmentTransaction.replace(R.id.login_signup_fragment_container, loginFragment, SdkWalletPaymentFragment.class.getName());
        fragmentTransaction.commitAllowingStateLoss();
        SdkHelper.dismissProgressDialog();
    }

    public void setTabVisibility() {
        findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
    }

    public void inflateLoadWalletFragment(boolean animate) {

        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (animate)
            fragmentTransaction.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit);
        SdkLoadWalletFragment loginFragment = new SdkLoadWalletFragment();
        fragmentTransaction.replace(R.id.login_signup_fragment_container, loginFragment, SdkLoadWalletFragment.class.getName());
        fragmentTransaction.commitAllowingStateLoss();
        SdkHelper.dismissProgressDialog();
    }

    @Override
    public void onPause() {
        super.onPause();
        SdkHelper.dismissProgressDialog();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {   //< -- register and initialize receiver here. What if server gives 401? user will come here.
        super.onResume();

        SharedPrefsUtils.setStringPreference(this, SdkConstants.USER_SESSION_COOKIE_PAGE_URL, this.getClass().getSimpleName());

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    public void loadSignUpFragment(boolean animate) {

        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (animate)
            fragmentTransaction.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit, R.anim.enter, R.anim.exit);
        SdkSignUpFragmentSdk signUpFragment = new SdkSignUpFragmentSdk();
        fragmentTransaction.replace(R.id.login_signup_fragment_container, signUpFragment, SdkSignUpFragmentSdk.class.getName());
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void finish() {
        super.finish();
        //overridePendingTransition(R.anim.slide_out_up, R.anim.slide_out_up);
    }

    @Override
    public void onBackPressed() {

        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }

        int fragmentCount = getSupportFragmentManager().getBackStackEntryCount();
        super.onBackPressed();
        if (fragmentCount == 0)
            close(Activity.RESULT_CANCELED, null);
    }

    public void onEventMainThread(final SdkCobbocEvent event) {
        if (event.getType() == SdkCobbocEvent.USER_VAULT) {

            if (event.getStatus()) {
                checkForWalletOnlyPayment();
            } else {
                SdkHelper.dismissProgressDialog();
                if (!SdkHelper.checkNetwork(this)) {
                    SdkHelper.showToastMessage(this, getString(R.string.disconnected_from_internet), true);
                    close(Activity.RESULT_CANCELED, null);
                } else {
                    SdkHelper.showToastMessage(this, getString(R.string.something_went_wrong), true);
                    close(Activity.RESULT_CANCELED, null);
                }

            }
        } else if (event.getType() == SdkCobbocEvent.LOGOUT) {

            SdkHelper.dismissProgressDialog();
            if (event.getStatus()) {
                SdkHelper.showToastMessage(this, getString(R.string.logout_success), false);
                loadSignUpFragment(true);
            } else {

                if (!SdkHelper.checkNetwork(this)) {
                    SdkHelper.showToastMessage(this, getString(R.string.disconnected_from_internet), true);
                } else {
                    SdkHelper.showToastMessage(this, getString(R.string.something_went_wrong), true);
                }

            }
        }
    }

    private int getPixelValue(int px) {

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        return ((int) (px * dm.scaledDensity));

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {//When HomeActivity resumes/starts

        if (requestCode == PAYMENT_SUCCESS) {
            if (resultCode == RESULT_OK) {
                SdkSession.getInstance(this).getUserVaults();
                SdkHelper.showToastMessage(WalletSdkLoginSignUpActivity.this, "load wallet success", false);

            } else if (resultCode == RESULT_QUIT) {
                SdkSession.getInstance(this).getUserVaults();
                SdkHelper.showToastMessage(WalletSdkLoginSignUpActivity.this, "load wallet failed", true);

            } else if (resultCode == RESULT_CANCELED) {
                if (data != null && !data.hasExtra(SdkConstants.IS_LOGOUT_CALL)) {
                    SdkSession.getInstance(this).getUserVaults();
                } else {
                    loadSignUpFragment(true);
                }
                SdkHelper.showToastMessage(WalletSdkLoginSignUpActivity.this, "load wallet cancelled", true);
            }
        } else if (requestCode == WALLET_HISTORY) {
            if (resultCode == RESULT_OK) {
                close(WALLET_HISTORY, data);

            } else if (resultCode == RESULT_CANCELED) {
                loadSignUpFragment(true);
            }
        }
    }

    public void close(int resultCode, Intent intent) {

        if (intent == null) {
            intent = new Intent();
        }

        if (resultCode == PAYMENT_CANCELLED) {
            intent.putExtra(SdkConstants.RESULT, "cancel");
            setResult(RESULT_CANCELED, intent);
        } else if (resultCode == PAYMENT_SUCCESS) {

            intent.putExtra(SdkConstants.RESULT, "success");
            setResult(RESULT_OK, intent);
        } else if (resultCode == WALLET_HISTORY) {

            intent.putExtra(SdkConstants.IS_HISTORY_CALL, true);
            setResult(RESULT_OK, intent);
        }
        finish();
    }
}
