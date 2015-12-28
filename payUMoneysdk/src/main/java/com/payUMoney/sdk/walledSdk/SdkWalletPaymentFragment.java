package com.payUMoney.sdk.walledSdk;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;

import java.math.BigDecimal;
import java.util.HashMap;

import de.greenrobot.event.EventBus;

public class SdkWalletPaymentFragment extends SdkBaseFragment {

    private Button cancelButton, payButton;
    private WalletSdkLoginSignUpActivity mSdkLoginSignUpActivity;
    private HashMap<String, String> userParams;
    private TextView amountToPayTextView, WalletBalanceTextView, walletUsageTextView, remainingWalletBalanceTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mSdkLoginSignUpActivity = (WalletSdkLoginSignUpActivity) getActivity();
        userParams = mSdkLoginSignUpActivity.getMapObject();

        return inflater.inflate(R.layout.wallet_payment_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSdkLoginSignUpActivity.setTabNewTitle(mSdkLoginSignUpActivity.getResources().getString(R.string.wallet_sufficient));
        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        payButton = (Button) view.findViewById(R.id.pay_button);

        amountToPayTextView = (TextView) view.findViewById(R.id.amount_to_pay);
        WalletBalanceTextView = (TextView) view.findViewById(R.id.wal_bal);
        walletUsageTextView = (TextView) view.findViewById(R.id.wal_use);
        remainingWalletBalanceTextView = (TextView) view.findViewById(R.id.rem_wal);

        amountToPayTextView.setText("₹ " + round(Double.parseDouble(userParams.get(SdkConstants.AMOUNT)), 2));
        WalletBalanceTextView.setText("₹ " + round(Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.WALLET_BALANCE)), 2));
        walletUsageTextView.setText("₹ " + round(Double.parseDouble(userParams.get(SdkConstants.AMOUNT)), 2));
        remainingWalletBalanceTextView.setText("₹ " + getRemainingBalance());


        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view1) {

                if (SdkHelper.isValidClick()) {
                    if (!SdkHelper.checkNetwork(getActivity())) {
                        SdkHelper.showToastMessage(getActivity(), getString(R.string.disconnected_from_internet), true);
                    } else {
                        setResetButtons(false);
                        SdkSession.getInstance(mSdkLoginSignUpActivity).debitFromWallet(userParams);
                    }
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(SdkHelper.isValidClick())
                    mSdkLoginSignUpActivity.close(mSdkLoginSignUpActivity.PAYMENT_CANCELLED, null);
            }
        });
        mSdkLoginSignUpActivity.showHideLogoutButton(true);
        mSdkLoginSignUpActivity.setTabVisibility();
        mSdkLoginSignUpActivity.invalidateActivityOptionsMenu();
    }

    private void setResetButtons(boolean visibility) {

        payButton.setText(visibility ? mSdkLoginSignUpActivity.getString(R.string.pay_now) : mSdkLoginSignUpActivity.getString(R.string.please_wait));
        payButton.setEnabled(visibility);
        cancelButton.setEnabled(visibility);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        SdkHelper.dismissProgressDialog();
    }

    public void onEventMainThread(SdkCobbocEvent event) {

        switch (event.getType()) {

            case SdkCobbocEvent.USER_VAULT :
                mSdkLoginSignUpActivity.onEventMainThread(event);
                break;

            case SdkCobbocEvent.DEBIT_WALLET :

                setResetButtons(true);

                if (event.getStatus()) {
                    SdkHelper.showToastMessage(getActivity(), "Payment Success", false);
                    Intent intent = new Intent();
                    intent.putExtra(SdkConstants.PAYMENT_ID, (String)event.getValue());
                    mSdkLoginSignUpActivity.close(mSdkLoginSignUpActivity.PAYMENT_SUCCESS, intent);
                } else {
                    if (((String)event.getValue()).contains(SdkConstants.NOT_ENOUGH_AMOUNT_STRING)) {
                        try {
                            String msg = (String)event.getValue();
                            msg = msg.replaceAll("user", "your");
                            msg = msg.replaceAll("account", "Account.");
                            SdkHelper.showToastMessage(mSdkLoginSignUpActivity, msg, true);

                            //Bug Fix if wallet is not sufficient but this page is being shown somehow
                            SdkSession.getInstance(mSdkLoginSignUpActivity).getUserVaults();

                        } catch (Exception e) {
                            SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.something_went_wrong), true);
                        }
                    } else {
                        SdkHelper.showToastMessage(mSdkLoginSignUpActivity, getString(R.string.something_went_wrong), true);
                    }
                }
                break;
            default:
                // we don't do anything else here
        }
    }

    public static BigDecimal round(double d, int decimalPlace) {

        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    private BigDecimal getRemainingBalance() {
        return round(Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.WALLET_BALANCE))
                - Double.parseDouble(userParams.get(SdkConstants.AMOUNT)), 2);
    }
}
