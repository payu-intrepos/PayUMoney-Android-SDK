package com.payUMoney.sdk;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Required;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Created by piyush on 4/11/14.
 */
public class LoginActivity extends AccountAuthenticatorActivity implements Validator.ValidationListener, LoaderManager.LoaderCallbacks<Cursor> {

    boolean mMessageShown = false;
    public static final int RESULT_QUIT = 5;
    static final int SIGN_UP = 6;
    public static final int FORGET_PASSWORD = 9;
    public static final int ACCOUNT_CHOOSER_ACTIVITY = 10;
    public static final int LOGIN = 1;

    AccountManager mAccountManager;
    private AlertDialog mAlertDialog;
    private boolean mInvalidate;

    private String mAccountType = "com.payUMoney.sdk.auth.account";
    private Account mAccount;
    private String mAuthToken;

    private Account[] acc;

    Validator mValidator;
    Crouton mCrouton;
    @Required(order = 1, message = "Your email is required")
    @Email(order = 2, message = "This email appears to be invalid")
    private AutoCompleteTextView mEmail;
    @Password(order = 3, message = "Please enter your password")
    private EditText mPassword;
    public final static String PARAM_USER_PASS = "USER_PASS";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    private Button mLogin;
    Context c;
    private CheckBox showPassword;

    @Override
    @SuppressLint("NewApi")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        c = this;
        setTitle(null);
        mAccountManager = AccountManager.get(getApplicationContext());
        mValidator = new Validator(this);

        mValidator.setValidationListener(this);

        mEmail = (AutoCompleteTextView) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mLogin = (Button) findViewById(R.id.login);
        showPassword = (CheckBox) findViewById(R.id.show_password);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(Constants.USER_EMAIL)) {
            mEmail.setText(getIntent().getExtras().getString(Constants.USER_EMAIL));
        }
        mEmail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mEmail.showDropDown();
                }
                return false;
            }
        });
        findViewById(R.id.sign_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                intent.putExtra(Constants.AMOUNT, getIntent().getStringExtra(Constants.AMOUNT));
                intent.putExtra(Constants.MERCHANTID, getIntent().getStringExtra(Constants.MERCHANTID));
                intent.putExtra(Constants.PARAMS, getIntent().getSerializableExtra(Constants.PARAMS));
                intent.putExtra(Constants.USER_EMAIL, getIntent().getStringExtra(Constants.USER_EMAIL));
                intent.putExtra(Constants.USER_PHONE, getIntent().getStringExtra(Constants.USER_PHONE));
                startActivityForResult(intent, SIGN_UP);
            }
        });
        findViewById(R.id.forgot_password).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivityForResult(intent, FORGET_PASSWORD);
            }
        });

/**
 * Login is clicked
 *
 */
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                if (mCrouton != null) {
                    mCrouton.hide();
                    mCrouton = null;
                }
                /**
                 * Front end validations
                 */
                mValidator.validate();
            }
        });

        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                int getSelectionIndex = mPassword.getSelectionStart();

                if (!isChecked) {
                    mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }

                mPassword.setSelection(getSelectionIndex);

            }
        });

/****Account Manager Functions, for storing new account on device
 * or selecting existing
 * Begin
 */
        acc = mAccountManager.getAccountsByType(mAccountType);
        if (getIntent() != null && getIntent().getStringExtra("logout") != null) {
            if (getIntent().getStringExtra("logout").equals("logout") && (acc.length == 1 || acc.length == 0)) {
                //one account logout
            } else {
                searchAddAcc();
            }
        } else if (getIntent() != null && getIntent().getStringExtra("force") != null) //check for force logout
        {
            if (getIntent().getStringExtra("force").equals("force")) {
                mAccountManager.invalidateAuthToken(mAccountType, Session.getInstance(getApplicationContext()).getToken());
                //mAccountManager.get
            }
        } else {
            //Function to add account to device, a feature we will give as option in login page now
     /*      searchAddAcc();*/
            if (acc.length >= 1) {
                searchAddAcc();
            }

        }

    }

    @SuppressLint("NewApi")
    private void searchAddAcc() {
        if (acc.length == 0) {
            Log.e("Account", "No accounts of type " + mAccountType + " found");
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
            }
            return;

        } else {//Multiple accounts
            Log.i("main", "Found " + acc.length + " accounts of type " + mAccountType);

            Intent intent = AccountManager.newChooseAccountIntent(
                    null,
                    null,
                    new String[]{"com.payUMoney.sdk.auth.account"},
                    false,
                    null,
                    "bearer",
                    null,
                    null);
            startActivityForResult(intent, ACCOUNT_CHOOSER_ACTIVITY);
        }
    }

    /**
     * Fetch Token for the account
     */
    private void startAuthTokenFetch() {
        Bundle options = new Bundle();
        /*
        Calling getAuthToken of Service
         */
        mAccountManager.getAuthToken(
                mAccount,
                "bearer",
                options,
                this,
                new OnAccountManagerComplete(),
                new Handler(new OnError())
        );
    }

    /**
     * When completed fetching
     */
    private class OnAccountManagerComplete implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return;
            } catch (AuthenticatorException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            /**
             * Store token in variable and call settoken of Session --> settoken,setuser of msessiondata
             */

            mAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            Session.getInstance(getApplicationContext()).setokennow(mAuthToken);
           /* if(mAuthToken!=null)
                Toast.makeText(getApplicationContext(),mAuthToken,Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getApplicationContext(),"token is empty",Toast.LENGTH_LONG).show();*/
            // Session.getInstance(getApplicationContext().)
            Log.d("main", "Received authentication token " + mAuthToken);
            Intent intent = new Intent();
            intent.putExtra(Constants.AMOUNT, getIntent().getStringExtra(Constants.AMOUNT));
            intent.putExtra(Constants.MERCHANTID, getIntent().getStringExtra(Constants.MERCHANTID));
            intent.putExtra(Constants.PARAMS, getIntent().getSerializableExtra(Constants.PARAMS));
            setResult(RESULT_OK, intent);
            /**REMEMBER NEXT TIME LOGIN***/
            SharedPreferences.Editor editor = getSharedPreferences(Constants.SP_SP_NAME, MODE_PRIVATE).edit();
            editor.putString(Constants.TOKEN, mAuthToken);
            editor.putString(Constants.EMAIL, AccountManager.KEY_ACCOUNT_NAME);
            editor.commit();

            finish();

        }
    }

    /**
     * Adding new account completed
     */
    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return;
            } catch (AuthenticatorException e) {
                e.printStackTrace();
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
            //Start fetch of the new account
            startAuthTokenFetch();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    public class OnError implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            Log.e("onError", "ERROR");
            return false;
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        populateAutoComplete();
    }


    @Override
    protected void onDestroy() {
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
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private void login(String username, String password) //Begin Login
    {
        mLogin.setEnabled(false);
        mLogin.setText(R.string.logging_in);


        Session.getInstance(getApplicationContext()).create(username, password); //Create login
    }

    public void onEventMainThread(CobbocEvent event) {
        switch (event.getType()) {
            case CobbocEvent.LOGIN:
                if (event.getStatus()) { //Login successful so set some parameters for next time and finish()
                    // This is for success
                    SharedPreferences.Editor editor = getSharedPreferences(Constants.SP_SP_NAME, MODE_PRIVATE).edit();
                    editor.putString(Constants.TOKEN, Session.getInstance(getApplicationContext()).getToken());
                    editor.putString(Constants.EMAIL, Session.getInstance(getApplicationContext()).getUser().getEmail());
                    editor.commit();

                    Intent intent = new Intent();
                    intent.putExtra(Constants.AMOUNT, getIntent().getStringExtra(Constants.AMOUNT));
                    intent.putExtra(Constants.MERCHANTID, getIntent().getStringExtra(Constants.MERCHANTID));
                    intent.putExtra(Constants.PARAMS, getIntent().getSerializableExtra(Constants.PARAMS));
                    this.setAccountAuthenticatorResult(intent.getExtras());
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    mCrouton = Crouton.makeText(this, R.string.invalid_email_or_password, Style.ALERT).setConfiguration(Constants.CONFIGURATION_LONG);
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


    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmail.setAdapter(adapter);
    }

    @Override //Overriden
    public void onValidationSucceeded()  //If format is validated
    {
        if (mCrouton != null) {
            mCrouton.cancel();
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPassword.getApplicationWindowToken(), 0);
        login(mEmail.getText().toString(), mPassword.getText().toString()); //begin login
    }

    @Override
    public void onValidationFailed(View view, Rule<?> rule) {
        mCrouton = Crouton.makeText(this, rule.getFailureMessage(), Style.ALERT).setConfiguration(Constants.CONFIGURATION_LONG);
        mCrouton.show();
        view.requestFocus();
    }

    private class SetupEmailAutoCompleteTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            ArrayList<String> emailAddressCollection = new ArrayList<String>();

            // Get all emails from the user's contacts and copy them to a list.
            ContentResolver cr = getContentResolver();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SIGN_UP) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent();
                intent.putExtra(Constants.AMOUNT, getIntent().getStringExtra(Constants.AMOUNT));
                // intent.putExtra(Constants.MERCHANTID, getIntent().getStringExtra(Constants.MERCHANTID));
                intent.putExtra(Constants.PARAMS, getIntent().getSerializableExtra(Constants.PARAMS));
                setResult(RESULT_OK, intent);
                finish();
            } else if (resultCode == RESULT_CANCELED) {

            }

        } else if (requestCode == FORGET_PASSWORD) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent();
                intent.putExtra(Constants.AMOUNT, getIntent().getStringExtra(Constants.AMOUNT));
                intent.putExtra(Constants.MERCHANTID, getIntent().getStringExtra(Constants.MERCHANTID));
                intent.putExtra(Constants.PARAMS, getIntent().getSerializableExtra(Constants.PARAMS));
                setResult(RESULT_OK, intent);
                finish();
            } else if (resultCode == RESULT_CANCELED) {

            }

        } else if (requestCode == ACCOUNT_CHOOSER_ACTIVITY && resultCode == RESULT_OK && data != null) {
            Bundle bundle = data.getExtras();
            mAccount = new Account(
                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
            );
            Log.d("main", "Selected account " + mAccount.name + ", fetching");
            startAuthTokenFetch();
        } else if (resultCode == RESULT_CANCELED) {

        } else {
            //nothing
        }
    }

    @Override
    public void onBackPressed() {
        Log.i("back", "pressed");
        setResult(RESULT_QUIT);
        finish();
    }
}
