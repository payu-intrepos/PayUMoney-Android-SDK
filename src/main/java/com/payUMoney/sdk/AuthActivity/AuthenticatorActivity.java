package com.payUMoney.sdk.AuthActivity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.payUMoney.sdk.CobbocEvent;
import com.payUMoney.sdk.Constants;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.Session;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

//By Karan

public class AuthenticatorActivity extends AccountAuthenticatorActivity implements Validator.ValidationListener, LoaderManager.LoaderCallbacks<Cursor> {

    boolean mMessageShown = false;
    public static final int RESULT_QUIT = 5;
    static final int SIGN_UP = 6;
    public static final int FORGET_PASSWORD = 9;
    public final static String PARAM_USER_PASS = "USER_PASS";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    private String authoken = "", authoken_label = "";


    AccountManager mAccountManager;

    public static final String ARG_AUTH_TYPE = "ARG_AUTH_TYPE";
    public static final String ARG_ACCOUNT_TYPE = "com.payUMoney.sdk.auth.account";


    Validator mValidator;
    Crouton mCrouton;
    @Required(order = 1, message = "Your email is required")
    @Email(order = 2, message = "This email appears to be invalid")
    private AutoCompleteTextView mEmail;
    @Password(order = 3, message = "Please enter your password")
    private EditText mPassword;
    private Button mLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticate);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setTitle(null);

        mValidator = new Validator(this);

        mValidator.setValidationListener(this);

        mAccountManager = AccountManager.get(getBaseContext());

        mEmail = (AutoCompleteTextView) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mLogin = (Button) findViewById(R.id.login);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(Constants.USER_EMAIL)) {
            mEmail.setText(getIntent().getExtras().getString(Constants.USER_EMAIL));

//            new ProgressDialog.Builder(this).setCancelable(false).setMessage(R.string.signing_in).create().show();
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

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mCrouton != null) {
                    mCrouton.hide();
                    mCrouton = null;
                }
                Toast.makeText(getApplicationContext(), "asdas", Toast.LENGTH_LONG).show();
                mValidator.validate();

            }
        });


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
        mLogin.setText("Logging in");


        Session.getInstance(getApplicationContext()).create(username, password); //Create login
    }

    public void onEventMainThread(CobbocEvent event) {
        switch (event.getType()) {
            case CobbocEvent.LOGIN:
                if (event.getStatus()) {

                    //Login successful so set some parameters for next time and finish()
                    // This is for success

                    JSONObject temp = (JSONObject) event.getValue();

                    Bundle data = new Bundle();

                    try {
                        //   authoken =temp.getString("token_type");
                        authoken = temp.getString("access_token");
                        authoken_label = temp.getString("token_type");

                        data.putString(AccountManager.KEY_ACCOUNT_NAME, mEmail.getText().toString());
                        data.putString(AccountManager.KEY_ACCOUNT_TYPE, "com.payUMoney.sdk.auth.account");
                        data.putString(AccountManager.KEY_AUTHTOKEN, authoken);
                        data.putString(AccountManager.KEY_AUTH_TOKEN_LABEL, authoken_label);
                        data.putString(PARAM_USER_PASS, mPassword.getText().toString());

                    } catch (Exception e) {
                        // data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                        Toast.makeText(getBaseContext(), KEY_ERROR_MESSAGE, Toast.LENGTH_SHORT).show();
                    }

                    final Intent res = new Intent();

                    res.putExtras(data);

                    finishLogin(res);


                } else {
                    mCrouton = Crouton.makeText(this, "invalid Email/Pass", Style.ALERT).setConfiguration(Constants.CONFIGURATION_LONG);
                    mCrouton.show();
                    mLogin.setEnabled(true);
                    mLogin.setText("Login");
                    mPassword.setText("");
                    mPassword.requestFocus();
                }
                break;
            default:
                // we don't do anything else here
        }
    }


    private void finishLogin(Intent intent) {
        Log.d("udinic", "account saving" + "> finishLogin");

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            Log.d("udinic", "account saving" + "> finishLogin > addAccountExplicitly");
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtokenType = intent.getStringExtra(AccountManager.KEY_AUTH_TOKEN_LABEL);

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, accountPassword, null);
            mAccountManager.setAuthToken(account, authtokenType, authtoken);
        } else {
            Log.d("udinic", "account saving" + "> finishLogin > setPassword");
            mAccountManager.setPassword(account, accountPassword);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override //Overriden
    public void onValidationSucceeded()  //If format is validated
    {
        if (mCrouton != null) {
            mCrouton.cancel();
        }
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

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmail.setAdapter(adapter);
    }

}
