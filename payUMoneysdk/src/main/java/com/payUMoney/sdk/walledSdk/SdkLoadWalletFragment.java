package com.payUMoney.sdk.walledSdk;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;

import de.greenrobot.event.EventBus;

public class SdkLoadWalletFragment extends SdkBaseFragment {

    private Button cancelButton, payButton;
    private WalletSdkLoginSignUpActivity mSdkLoginSignUpActivity;
    private HashMap<String, String> userParams;
    private TextView amountToPayTextView, WalletBalanceTextView, finalWalletBalanceTextView;
    private EditText neededAmountTextView;
    private double initialNeededAmount;
    private final int VALID_AMOUNT = 0;
    private final int LESS_AMOUNT = 1;
    private final int LESS_THEN_ALLOWED_AMOUNT = 2;
    private final int MORE_AMOUNT = 3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mSdkLoginSignUpActivity = (WalletSdkLoginSignUpActivity) getActivity();
        userParams = mSdkLoginSignUpActivity.getMapObject();

        return inflater.inflate(R.layout.walletsdk_load_wallet_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSdkLoginSignUpActivity.setTabNewTitle(mSdkLoginSignUpActivity.getResources().getString(R.string.wallet_in_sufficient));
        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        payButton = (Button) view.findViewById(R.id.pay_button);

        initialNeededAmount = Double.parseDouble(userParams.get(SdkConstants.AMOUNT))
                - Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.WALLET_BALANCE));

        amountToPayTextView = (TextView) view.findViewById(R.id.amount_to_pay);
        WalletBalanceTextView = (TextView) view.findViewById(R.id.wal_bal);
        neededAmountTextView = (EditText) view.findViewById(R.id.wal_use);
        finalWalletBalanceTextView = (TextView) view.findViewById(R.id.rem_wal);

        amountToPayTextView.setText("₹ " + round(Double.parseDouble(userParams.get(SdkConstants.AMOUNT))));
        WalletBalanceTextView.setText("₹ " + round(Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.WALLET_BALANCE))));
        neededAmountTextView.setText("₹ " + round(getNeededBalance()));
        finalWalletBalanceTextView.setText("₹ " + getFinalWalletBalance());

        neededAmountTextView.addTextChangedListener(new SimpleTextWatcher(neededAmountTextView));

        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!SdkHelper.checkNetwork(getActivity())) {
                    SdkHelper.showToastMessage(getActivity(), getString(R.string.disconnected_from_internet), true);
                } else {

                    String inputAmount = neededAmountTextView.getText().toString();
                    int mIsValidAmount = checkForValidInputAmount(inputAmount);
                    if (inputAmount == null || inputAmount.length() <= 2 || !(mIsValidAmount == VALID_AMOUNT)) {
                        if(mIsValidAmount == LESS_AMOUNT)
                            SdkHelper.showToastMessage(getActivity(), getString(R.string.less_input_balance), true);
                        else if(mIsValidAmount == LESS_THEN_ALLOWED_AMOUNT)
                            SdkHelper.showToastMessage(getActivity(), "Minimum allowed wallet load Amount is " + SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE) + ".", true);
                        else
                            SdkHelper.showToastMessage(getActivity(), "Maximum allowed wallet load Amount is " + SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MAX_WALLET_BALANCE) + ".", true);
                        return;
                    }
                    setResetButtons(false);
                    SdkSession.getInstance(mSdkLoginSignUpActivity).loadWallet(userParams, inputAmount.substring(2, inputAmount.length()));
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mSdkLoginSignUpActivity.close(mSdkLoginSignUpActivity.PAYMENT_CANCELLED, null);
            }
        });
        mSdkLoginSignUpActivity.showHideLogoutButton(true);
        mSdkLoginSignUpActivity.setTabVisibility();
        mSdkLoginSignUpActivity.invalidateActivityOptionsMenu();
    }

    private int checkForValidInputAmount(String inputAmount) {

        if (Double.parseDouble(inputAmount.substring(2, inputAmount.length())) < initialNeededAmount) {
            return LESS_AMOUNT;
        } else if(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE) != null
                && !SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE).equals(SdkConstants.NULL_STRING)
                && Double.parseDouble(inputAmount.substring(2, inputAmount.length())) < Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE))){
            return LESS_THEN_ALLOWED_AMOUNT;
        } else if(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MAX_WALLET_BALANCE) != null
                && !SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MAX_WALLET_BALANCE).equals(SdkConstants.NULL_STRING)
                && Double.parseDouble(inputAmount.substring(2, inputAmount.length())) > Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MAX_WALLET_BALANCE))){
            return MORE_AMOUNT;
        }
        return VALID_AMOUNT;
    }

    private class SimpleTextWatcher implements TextWatcher {

        private EditText neededAmountTextView;
        private String currentInputString;

        SimpleTextWatcher(EditText editText) {
            neededAmountTextView = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (s != null)
                currentInputString = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (neededAmountTextView != null && neededAmountTextView.getSelectionStart() == 1) {
                neededAmountTextView.setText(currentInputString);
                neededAmountTextView.setSelection(2);
            }

            if (s != null && s.toString().length() > 2 && isDouble((s.toString()).substring(2, (s.toString()).length()))) {
                payButton.setEnabled(true);
                finalWalletBalanceTextView.setText("₹ " + getFinalWalletBalance());
            } else {
                payButton.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(neededAmountTextView != null && neededAmountTextView.getText().toString().length() <= 1){
                neededAmountTextView.setText("₹ ");
                neededAmountTextView.setSelection(2);
            }
        }
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
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(SdkCobbocEvent event) {
        switch (event.getType()) {
            case SdkCobbocEvent.LOAD_WALLET:

                setResetButtons(true);
                if (event.getStatus()) {
                    mSdkLoginSignUpActivity.callSdkToLoadWallet((JSONObject) event.getValue());
                } else {
                    if (event.getValue() != null) {
                        try {
                            SdkHelper.showToastMessage(mSdkLoginSignUpActivity, ((JSONObject) event.getValue()).getString(SdkConstants.MESSAGE), true);
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

    private void setResetButtons(boolean visibility) {

        payButton.setText(visibility ? mSdkLoginSignUpActivity.getString(R.string.load_wallet) : mSdkLoginSignUpActivity.getString(R.string.please_wait));
        payButton.setEnabled(visibility);
        cancelButton.setEnabled(visibility);
    }

    public static BigDecimal round(double d) {

        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    private double getNeededBalance() {

        double neededAmount = Double.parseDouble(userParams.get(SdkConstants.AMOUNT))
                - Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.WALLET_BALANCE));

        if (SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE) != null
                && SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE).equals(SdkConstants.NULL_STRING)
                && neededAmount < Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE)))
            return (Double.parseDouble(SharedPrefsUtils.getStringPreference(mSdkLoginSignUpActivity, SharedPrefsUtils.Keys.MIN_WALLET_BALANCE)));
        return neededAmount;
    }

    boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private BigDecimal getFinalWalletBalance() {

        String inputAmount = null;
        if (neededAmountTextView != null && neededAmountTextView.getText() != null) {
            inputAmount = neededAmountTextView.getText().toString();
        }

        if (inputAmount != null && inputAmount.length() > 2) {

            double currentInputAmount = Double.parseDouble(inputAmount.substring(2, inputAmount.length()));
            if (currentInputAmount < initialNeededAmount) {
                return round(0.00);
            }
            return round(currentInputAmount - initialNeededAmount);
        }
        return round(0.00);
    }
}
