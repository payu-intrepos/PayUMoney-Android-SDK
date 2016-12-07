package com.payUMoney.sdk.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.telephony.gsm.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkLoginSignUpActivity;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.dialog.SdkOtpProgressDialog;
import com.payUMoney.sdk.utils.SdkHelper;
import com.payUMoney.sdk.utils.SdkLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 *
 */
public class SdkLoginFragment extends Fragment implements Validator.ValidationListener/*, LoaderManager.LoaderCallbacks<Cursor>*/ {

    AccountManager mAccountManager = null;
    Validator mValidator = null;
    Crouton mCrouton = null;
    @Required(order = 1, message = "Your email is required")
    @Email(order = 2, message = "This email appears to be invalid")
    private AutoCompleteTextView mEmail = null;
    @Password(order = 3, message = "Please enter your password")
    private EditText mPassword = null;
    private Button mLogin = null;
    private FragmentActivity c = null;
    private RadioGroup radioGroup = null;
    private String loginMode = "";
    private View otpProgress = null;
    private Pattern otpPattern = Pattern.compile("(|^)\\d{6}");
    private EditText mOtpEditText = null;
    private BroadcastReceiver receiver = null;
//    private Account[] acc;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.sdk_activity_login, container, false);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        c = getActivity();

        mAccountManager = AccountManager.get(getActivity().getApplicationContext());
//        acc = mAccountManager.getAccountsByType(SdkConstants.ARG_ACCOUNT_TYPE);
        mValidator = new Validator(this);
        mValidator.setValidationListener(this);

        mEmail = (AutoCompleteTextView) view.findViewById(R.id.email);
        mPassword = (EditText) view.findViewById(R.id.password);
        mOtpEditText = (EditText) view.findViewById(R.id.otpEditText);
        mLogin = (Button) view.findViewById(R.id.login);
        mLogin.setEnabled(false);
        /*Account[] accounts = AccountManager.get(getActivity()).getAccounts();
        Set<String> emailSet = new HashSet<String>();
        for (Account account : accounts) {
            if (SdkConstants.EMAIL_PATTERN.matcher(account.name).matches()) {
                emailSet.add(account.name);
            }
        }*/
        Set<String> emailSet = new HashSet<String>();
        mEmail.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, new ArrayList<String>(emailSet)));
        //showPassword = (ImageView) view.findViewById(R.id.show_password_eye);
        otpProgress = view.findViewById(R.id.otpProgress);
        otpProgress.setVisibility(View.INVISIBLE);

        radioGroup = (RadioGroup) view.findViewById(R.id.loginOptions);
        RadioButton passwordLogin = (RadioButton) view.findViewById(R.id.passwordLogin);
        RadioButton guestLogin = (RadioButton) view.findViewById(R.id.guest_login);
        final RadioButton otpLogin = (RadioButton) view.findViewById(R.id.loginotp);

        String allowGuestCheckoutValue = c.getIntent().getStringExtra(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_VALUE);
        String otpLoginValue = c.getIntent().getStringExtra(SdkConstants.OTP_LOGIN);

        if ((allowGuestCheckoutValue != null && !allowGuestCheckoutValue.equals("") && !allowGuestCheckoutValue.equals("0") && !allowGuestCheckoutValue.equals("null")) || (otpLoginValue != null && !otpLoginValue.equals("") && !otpLoginValue.equals("0") && !otpLoginValue.equals("null") )){

            if (allowGuestCheckoutValue.equals(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT)) {

                //signUp.setVisibility(VISIBLE);
                passwordLogin.setVisibility(VISIBLE);
                guestLogin.setVisibility(VISIBLE);
                view.findViewById(R.id.forgot_password).setVisibility(View.VISIBLE);


            } else if (allowGuestCheckoutValue.equals(SdkConstants.MERCHANT_PARAM_ALLOW_GUEST_CHECKOUT_ONLY)) {

                //guestLogin.setVisibility(VISIBLE);
                loginMode = "guestLogin";
                mLogin.setEnabled(true);

            }/*quickGuestCheckout Not Used in the app yet*/
           /* else if (allowGuestCheckoutValue.equals("quickGuestCheckout")) {

            } */
            else if (otpLoginValue.equals("1")) {

                //signUp.setVisibility(VISIBLE);
                otpLogin.setVisibility(VISIBLE);
                passwordLogin.setVisibility(VISIBLE);
                view.findViewById(R.id.forgot_password).setVisibility(View.VISIBLE);

            }
        }
        else  /*default case*/
        {
            //signUp.setVisibility(VISIBLE);
            loginMode = "default";
            view.findViewById(R.id.passwordLayout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.forgot_password).setVisibility(View.VISIBLE);
            // passwordLogin.setVisibility(VISIBLE);
        }

        Button sendOtpButton = (Button) view.findViewById(R.id.sendOtpBtn);

        sendOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SdkSession.getInstance(c.getApplicationContext()).generateAndSendOtp(mEmail.getText().toString());
                hideKeyboardIfShown();
                otpProgress.setVisibility(VISIBLE);
                SdkOtpProgressDialog.showDialog(c.getApplicationContext(), otpProgress);
                ((EditText) view.findViewById(R.id.otpEditText)).setText("");
                ((Button) view.findViewById(R.id.sendOtpBtn)).setText("Resend");
                ((Button) view.findViewById(R.id.sendOtpBtn)).setGravity(Gravity.CENTER);
            }
        });
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // find which radio button is selected and change UI accordingly

                if (mEmail.getText().toString().equals("") && checkedId != -1) {
                    mCrouton = Crouton.makeText(c, R.string.enter_email_id, Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                    mCrouton.show();
                    radioGroup.clearCheck();
                } else if (isEmailValid(mEmail.getText().toString()) && checkedId != -1) {
                    mCrouton = Crouton.makeText(c, R.string.invalid_email_id, Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                    mCrouton.show();
                    radioGroup.clearCheck();
                } else if (checkedId == R.id.passwordLogin) {

                    if(mCrouton != null){
                        mCrouton.cancel();
                        mCrouton = null;
                    }

                    loginMode = "passwordLogin";
                    if (view.findViewById(R.id.password).getVisibility() == GONE) {
                        view.findViewById(R.id.password).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.password).requestFocus();
                    }
                    if (view.findViewById(R.id.passwordLayout).getVisibility() == GONE)
                        view.findViewById(R.id.passwordLayout).setVisibility(View.VISIBLE);
                    if (view.findViewById(R.id.forgot_password).getVisibility() == GONE)
                        view.findViewById(R.id.forgot_password).setVisibility(View.VISIBLE);

                    if (view.findViewById(R.id.loginOTP).getVisibility() == VISIBLE)
                        view.findViewById(R.id.loginOTP).setVisibility(View.GONE);
                    if(otpProgress != null && otpProgress.getVisibility() == View.VISIBLE)
                        otpProgress.setVisibility(View.INVISIBLE);
                } else if (checkedId == R.id.loginotp) {

                    if(mCrouton != null){
                        mCrouton.cancel();
                        mCrouton = null;
                    }
                    loginMode = "otpLogin";
                    if (view.findViewById(R.id.password).getVisibility() == VISIBLE)
                        view.findViewById(R.id.password).setVisibility(View.GONE);
                    if (view.findViewById(R.id.passwordLayout).getVisibility() == VISIBLE)
                        view.findViewById(R.id.passwordLayout).setVisibility(View.GONE);
                    if (view.findViewById(R.id.forgot_password).getVisibility() == VISIBLE)
                        view.findViewById(R.id.forgot_password).setVisibility(View.GONE);

                    if (view.findViewById(R.id.loginOTP).getVisibility() == GONE)
                        view.findViewById(R.id.loginOTP).setVisibility(View.VISIBLE);


                    if(otpLogin.isChecked())
                        SdkSession.getInstance(c.getApplicationContext()).generateAndSendOtp(mEmail.getText().toString());

                    hideKeyboardIfShown();
                    otpProgress.setVisibility(VISIBLE);
                    SdkOtpProgressDialog.showDialog(c.getApplicationContext(), otpProgress);
                    ((EditText) view.findViewById(R.id.otpEditText)).setHint("");
                    ((Button) view.findViewById(R.id.sendOtpBtn)).setText("Resend");
                    ((Button) view.findViewById(R.id.sendOtpBtn)).setGravity(Gravity.CENTER);


                } else if (checkedId == R.id.guest_login) {

                    if(mCrouton != null){
                        mCrouton.cancel();
                        mCrouton = null;
                    }
                    loginMode = "guestLogin";
                    if (view.findViewById(R.id.password).getVisibility() == VISIBLE)
                        view.findViewById(R.id.password).setVisibility(View.GONE);
                    if (view.findViewById(R.id.passwordLayout).getVisibility() == VISIBLE)
                        view.findViewById(R.id.passwordLayout).setVisibility(View.GONE);
                    if (view.findViewById(R.id.forgot_password).getVisibility() == VISIBLE)
                        view.findViewById(R.id.forgot_password).setVisibility(View.GONE);
                    if(otpProgress != null && otpProgress.getVisibility() == View.VISIBLE)
                        otpProgress.setVisibility(View.INVISIBLE);

                    mLogin.setEnabled(true);
                }
            }

        });
        //endofrg
        /*if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(Constants.USER_EMAIL)) {
            mEmail.setText(getIntent().getExtras().getString(Constants.USER_EMAIL));
        }*/
        mEmail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view1, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mEmail.showDropDown();
                }
                if (otpProgress != null && otpProgress.getVisibility() == View.VISIBLE)
                    otpProgress.setVisibility(View.INVISIBLE);
                radioGroup.clearCheck();
                if (view.findViewById(R.id.loginOTP).getVisibility() == VISIBLE)
                    view.findViewById(R.id.loginOTP).setVisibility(View.GONE);
                if (!loginMode.equals("default")) {
                    if (view.findViewById(R.id.password).getVisibility() == VISIBLE)
                        view.findViewById(R.id.password).setVisibility(View.GONE);
                    if (view.findViewById(R.id.passwordLayout).getVisibility() == VISIBLE)
                        view.findViewById(R.id.passwordLayout).setVisibility(View.GONE);
                }
                return false;

            }
        });
        /*signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(c, SdkSignUpFragment.class);
                intent.putExtra(SdkConstants.AMOUNT, c.getIntent().getStringExtra(SdkConstants.AMOUNT));
                intent.putExtra(SdkConstants.MERCHANTID, c.getIntent().getStringExtra(SdkConstants.MERCHANTID));
                intent.putExtra(SdkConstants.PARAMS, c.getIntent().getSerializableExtra(SdkConstants.PARAMS));
                intent.putExtra(SdkConstants.USER_EMAIL, c.getIntent().getStringExtra(SdkConstants.USER_EMAIL));
                intent.putExtra(SdkConstants.USER_PHONE, c.getIntent().getStringExtra(SdkConstants.USER_PHONE));
                startActivityForResult(intent, SIGN_UP);
            }
        });*/
        view.findViewById(R.id.forgot_password).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mCrouton != null) {
                    mCrouton.cancel();
                    mCrouton = null;
                }
                SdkSession.getInstance(getActivity()).cancelPendingRequests(SdkSession.TAG);
                ((SdkLoginSignUpActivity)getActivity()).loadForgotPasswordFragment(true);
            }
        });

/**
 * Login is clicked
 *
 */

        mLogin.setOnClickListener(new View.OnClickListener() {



            @Override
            public void onClick(View arg0) {

                if(!SdkHelper.checkNetwork(getActivity())) {
                    Toast.makeText(getActivity(), R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (mCrouton != null) {
                        mCrouton.hide();
                        mCrouton = null;
                    }
                    /**
                     * Front end validations
                     */
                    if (loginMode == "guestLogin") {
                        if (mEmail.getText().toString().equals("")) {
                            mCrouton = Crouton.makeText(c, R.string.enter_email_id, Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                            mCrouton.show();
                            radioGroup.clearCheck();
                        } else if (isEmailValid(mEmail.getText().toString())) {
                            mCrouton = Crouton.makeText(c, R.string.invalid_email_id, Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                            mCrouton.show();
                            radioGroup.clearCheck();
                        }else {
                            onValidationSucceeded();
                        }
                    }
                    else if (loginMode == "otpLogin") {

                        if (((EditText) view.findViewById(R.id.otpEditText)).getText().toString().isEmpty()) {

                            Toast.makeText(c.getApplicationContext(), "OTP Field is empty", Toast.LENGTH_LONG);
                            return;
                        }
                        EditText otp = (EditText) view.findViewById(R.id.otpEditText);
                        SdkSession.getInstance(c.getApplicationContext()).create(mEmail.getText().toString(), otp.getText().toString());

                    } else
                        mValidator.validate();
                }
            }
        });

       /* showPassword.setOnClickListener(new View.OnClickListener() {
            public boolean passwordShown;

            @Override
            public void onClick(View v) {

                int getSelectionIndex = mPassword.getSelectionStart();

                if (!passwordShown) {
                    mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    passwordShown = true;
                } else {
                    mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    passwordShown = false;
                }
                mPassword.setSelection(getSelectionIndex);

            }
        });*/

        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.toString().trim().length() > 0) {

                    mLogin.setEnabled(true);

                } else {
                    mLogin.setEnabled(false);
                }

            }
        });

        mOtpEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.toString().trim().length() == 6) {

                    otpProgress.setVisibility(View.GONE);
                    hideKeyboardIfShown();
                    //info.setText("Verify OTP");
                    mLogin.setEnabled(true);

                } else {
                    mLogin.setEnabled(false);
                    otpProgress.setVisibility(View.VISIBLE);
                    //info.setText(getString(R.string.waiting_for_otp));
                }

            }
        });

/****Account Manager Functions, for storing new account on device
 * or selecting existing
 * Begin
 */
        //startAccountFetchTask();

        return view;
    }


    /*Start Auth token
    * fetch from device******************/


    /*public void startAccountFetchTask(){
        *//****Account Manager Functions, for storing new account on device
         * or selecting existing
         * Begin
         *//*
        if (c.getIntent() != null && c.getIntent().getStringExtra("logout") != null) {
            if (c.getIntent().getStringExtra("logout").equals("logout") && (acc.length == 1 || acc.length == 0))
            {
                *//*User clicked logout button*//*
            }
        } else if (c.getIntent() != null && c.getIntent().getStringExtra("force") != null) //check for force logout
        {
            *//*Force Logout, token has expired*//*
            if (c.getIntent().getStringExtra("force").equals("force")) {
                mAccountManager.invalidateAuthToken(SdkConstants.ARG_ACCOUNT_TYPE, SdkSession.getInstance(c.getApplicationContext()).getToken());
                Toast.makeText(c.getApplicationContext(), "You have been logged out from device, please add account", Toast.LENGTH_LONG).show();
            }
        } else {
            *//*User lands here when no token is saved . Check if any account is on device*//*
            if (acc.length >= 1) {
                searchAddAcc();
            }
        }
    }

    @SuppressLint("NewApi")
    private void searchAddAcc() {
        if (acc.length == 0) {
            SdkLogger.e("Account", "No accounts of type " + SdkConstants.ARG_ACCOUNT_TYPE + " found");
            // TODO: add account
            try {
                mAccountManager.addAccount(SdkConstants.ARG_ACCOUNT_TYPE,"bearer",null,new Bundle(),c,new OnAccountAddComplete(),null);
            } catch (Exception e) {
                Toast.makeText(c.getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
            }
            return;
        } else {
            SdkLogger.i("main", "Found " + acc.length + " accounts of type " + SdkConstants.ARG_ACCOUNT_TYPE);
            // TODO: show multiple account
            Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{SdkConstants.ARG_ACCOUNT_TYPE}, true, null, "bearer", null, null); //Bearer from token label
            startActivityForResult(intent, ACCOUNT_CHOOSER_ACTIVITY);
        }
    }

    *//**
     * Adding new account completed
     *//*
    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;

            try {
                bundle = result.getResult();
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                e.printStackTrace();
                return;
            }
            mAccount = new Account( bundle.getString(AccountManager.KEY_ACCOUNT_NAME), bundle.getString(AccountManager.KEY_ACCOUNT_TYPE));
            SdkLogger.d(SdkConstants.TAG + " : main", "Added account " + mAccount.name + ", fetching");
            //Start fetch of the new account
            startAuthTokenFetch();
        }
    }

    private void startAuthTokenFetch() {
        Bundle options = new Bundle();
        *//*
        Calling getAuthToken of Service
         *//*
        mAccountManager.getAuthToken( mAccount, "bearer",options, getActivity(), new OnAccountManagerComplete(), new Handler(new OnError()));
    }

    *//**
     * When completed fetching
     *//*
    private class OnAccountManagerComplete implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                e.printStackTrace();
                return;
            }
            *//**
             * Store token in variable and call settoken of Session --> settoken,setuser of msessiondata
             *//*
            String mAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            if(mAuthToken !=null) {
                //Stored token Step 1
                SharedPreferences.Editor editor = c.getSharedPreferences(SdkConstants.SP_SP_NAME, Context.MODE_PRIVATE).edit();
                editor.putString(SdkConstants.TOKEN, mAuthToken);
                editor.putString(SdkConstants.EMAIL, bundle.getString(AccountManager.KEY_ACCOUNT_NAME));
                editor.apply();
                SdkSession.getInstance(c.getApplicationContext()).setToken(mAuthToken);
                SdkSession.getInstance(c.getApplicationContext()).create(bundle.getString(AccountManager.KEY_ACCOUNT_NAME), mAuthToken);
            }else{
                Toast.makeText(c.getApplicationContext(),"Please login again with remember me",Toast.LENGTH_LONG).show();
            }
        }
    }*/


    /*End Auth Token fetch*******************/

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    /*@Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }*/

    public class OnError implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            SdkLogger.e("onError", "ERROR");
            return false;
        }

    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        populateAutoComplete();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
    }

    private void populateAutoComplete() {
        if (Build.VERSION.SDK_INT >= 14) {
            // Use ContactsContract.Profile (API 14+)
            // getSupportLoaderManager().initLoader(0, null, this);
        } else if (Build.VERSION.SDK_INT >= 8) {
            // Use AccountManager (API 8+)
            new SetupEmailAutoCompleteTask().execute(null, null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (mCrouton != null) {
            mCrouton.cancel();
            mCrouton = null;
        }
        if(receiver != null){
            c.unregisterReceiver(receiver);
        }

    }

    private void login(String username, String password) {//Begin Login

        mLogin.setEnabled(false);
        mLogin.setText(R.string.logging_in);

//Now we need to handle login through OTP and guest checkout through this
        if (loginMode.equals("guestLogin")) {
            SdkSession.getInstance(c.getApplicationContext()).setLoginMode(loginMode);
            SdkSession.getInstance(c.getApplicationContext()).setGuestEmail(username);
            Intent intent = new Intent();
            intent.putExtra(SdkConstants.AMOUNT, c.getIntent().getStringExtra(SdkConstants.AMOUNT));
            intent.putExtra(SdkConstants.MERCHANTID, c.getIntent().getStringExtra(SdkConstants.MERCHANTID));
            intent.putExtra(SdkConstants.PARAMS, c.getIntent().getSerializableExtra(SdkConstants.PARAMS));
            //c.setAccountAuthenticatorResult(intent.getExtras());
            c.setResult(Activity.RESULT_OK, intent);
            c.finish();
            //Session.getInstance(getApplicationContext()).createPayment((HashMap<String, String>) getIntent().getSerializableExtra(Constants.PARAMS));
        } else
            SdkSession.getInstance(c.getApplicationContext()).create(username, password); //Create login
    }

    public void onEventMainThread(SdkCobbocEvent event) {
        switch (event.getType() ) {
            case SdkCobbocEvent.GENERATE_AND_SEND_OTP:
                JSONObject result = null;
                result = (JSONObject) event.getValue();
                if (event.getStatus() && result != null) {

                    try {
                        mCrouton = Crouton.makeText(c, result.getString(SdkConstants.MESSAGE), Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                        mCrouton.show();
                        registerOTP();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    if(result != null &&  result.optString(SdkConstants.MESSAGE) != null){
                        mCrouton = Crouton.makeText(c, result.optString(SdkConstants.MESSAGE), Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                    }
                    else{
                        mCrouton = Crouton.makeText(c, "Unable to send OTP now,try resending or use password login...", Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                        mOtpEditText.setHint("Try resending OTP...");
                    }
                    otpProgress.setVisibility(View.INVISIBLE);
                    mCrouton.show();
                }
                break;

            case SdkCobbocEvent.LOGIN:
                if (event.getStatus()) { //Login successful so set some parameters for next time and finish()
                    if(event.getValue().toString().equals("Error")) {
                        Toast.makeText(c,"Error while Login",Toast.LENGTH_LONG).show();
                    } else {
                        ((SdkLoginSignUpActivity)getActivity()).close();
                    }

                } else {
                    mCrouton = Crouton.makeText(c,loginMode == "otpLogin" ? R.string.invalid_otp : R.string.invalid_email_or_password, Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                    mCrouton.show();
                    mLogin.setEnabled(true);
                    mLogin.setText(R.string.login);
                    mPassword.setText("");
                    mPassword.requestFocus();
                }
                break;

            default:
                // we don't do anything else here
        }
    }

    /*private void saveAccountAndFinishLogin(Intent intent) {
        SdkLogger.d(SdkConstants.TAG +" : udinic", "account saving" + "> finishLogin");
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(SdkConstants.PARAM_USER_PASS);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
        if (intent.getBooleanExtra(SdkConstants.ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            SdkLogger.d(SdkConstants.TAG +" : udinic", "account saving" + "> finishLogin > addAccountExplicitly");
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtokenType = intent.getStringExtra(AccountManager.KEY_AUTH_TOKEN_LABEL);
            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, accountPassword, null);
            mAccountManager.setAuthToken(account, authtokenType, authtoken);
        } else {
            SdkLogger.d(SdkConstants.TAG + " : udinic", "account saving" + "> finishLogin > setPassword");
            mAccountManager.setPassword(account, accountPassword);
        }
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }*/

    public void registerOTP() {
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
                    if (bundle != null && mOtpEditText != null) {
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
                                    mOtpEditText.setText(m.group(0));
                                    otpProgress.setVisibility(View.INVISIBLE);
                                    SdkSession.getInstance(c.getApplicationContext()).create(mEmail.getText().toString(), m.group(0));
                                } else {
                                    Toast.makeText(c, "Couldn't read sms, please enter OTP manually", Toast.LENGTH_LONG).show();
                                    mOtpEditText.requestFocus();
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(c, "Couldn't read sms, please enter OTP manually", Toast.LENGTH_LONG).show();
                            mOtpEditText.requestFocus();
                        }
                    } else {
                        Toast.makeText(c, "Couldn't read sms, please enter OTP manually", Toast.LENGTH_LONG).show();
                        mOtpEditText.requestFocus();
                    }
                }
            }
        };
        c.registerReceiver(receiver, filter);
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(c, android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmail.setAdapter(adapter);
    }


    @Override
    public void onValidationSucceeded()  //If format is validated
    {
        if (mCrouton != null) {
            mCrouton.cancel();
        }
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPassword.getApplicationWindowToken(), 0);
        login(mEmail.getText().toString(), mPassword.getText().toString()); //begin login
    }

    @Override
    public void onValidationFailed(View view, Rule<?> rule) {
        mCrouton = Crouton.makeText(c, rule.getFailureMessage(), Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
        mCrouton.show();
        view.requestFocus();
    }

    private class SetupEmailAutoCompleteTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            ArrayList<String> emailAddressCollection = new ArrayList<>();

            // Get all emails from the user's contacts and copy them to a list.
            ContentResolver cr = c.getContentResolver();
            Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    null, null, null);
            while (emailCur.moveToNext()) {
                String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract
                        .CommonDataKinds.Email.DATA));
                emailAddressCollection.add(email);
            }
            emailCur.close();
            return emailAddressCollection;
        }

        @Override
        protected void onPostExecute(List<String> emailAddressCollection) {
            addEmailsToAutoComplete(emailAddressCollection);
        }
    }

    /*public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SIGN_UP) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent();
                intent.putExtra(SdkConstants.AMOUNT, c.getIntent().getStringExtra(SdkConstants.AMOUNT));
                // intent.putExtra(Constants.MERCHANTID, getIntent().getStringExtra(Constants.MERCHANTID));
                intent.putExtra(SdkConstants.PARAMS, c.getIntent().getSerializableExtra(SdkConstants.PARAMS));
                c.setResult(Activity.RESULT_OK, intent);
                c.finish();
            } else if (resultCode == Activity.RESULT_CANCELED) {

            }

        } else if (requestCode == FORGET_PASSWORD) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent();
                intent.putExtra(SdkConstants.AMOUNT, c.getIntent().getStringExtra(SdkConstants.AMOUNT));
                intent.putExtra(SdkConstants.MERCHANTID, c.getIntent().getStringExtra(SdkConstants.MERCHANTID));
                intent.putExtra(SdkConstants.PARAMS, c.getIntent().getSerializableExtra(SdkConstants.PARAMS));
                c.setResult(Activity.RESULT_OK, intent);
                c.finish();
            } else if (resultCode == Activity.RESULT_CANCELED) {

            }

        } else if (requestCode == ACCOUNT_CHOOSER_ACTIVITY && resultCode == Activity.RESULT_OK && data != null)
        {
            Bundle bundle = data.getExtras();
            mAccount = new Account(
                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
            );
            SdkLogger.d(SdkConstants.TAG + " : main", "Selected account " + mAccount.name + ", fetching");
            //startAuthTokenFetch();
        }else if (resultCode == Activity.RESULT_CANCELED) {

        } else {
            //nothing
        }
    }*/

    public static boolean isEmailValid(String email) {
        boolean isValid = false;

        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        if (matcher.matches()) {
            isValid = true;
        }
        return !isValid;
    }
    public void hideKeyboardIfShown() {

        InputMethodManager inputMethodManager = (InputMethodManager) c.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = c.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(c);
        }
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}