package com.payUMoney.sdk.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkLoginSignUpActivity;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Created by amit on 25/07/13.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SdkSignUpFragment extends Fragment implements Validator.ValidationListener/*, LoaderManager.LoaderCallbacks<Cursor>*/ {

    Validator mValidator = null;
    private Button mSignUp = null;
    @Required(order = 1, message = "Your email is required")
    @Email(order = 2, message = "This email appears to be invalid")
    private AutoCompleteTextView mEmail = null;
    @Required(order = 3, message = "Please enter your phone number")
    //@(order = 4, message = "This Phone Number appears to be Invalid", pattern = "([\\d]{10})", trim = true)
    private AutoCompleteTextView mPhone = null;
    /*@Password(order = 5, message = "Password is required")
    @TextRule(order = 6, minLength = 6, message = "Password should be minimum 7 character with atleast 1 letter and 1 number")*/
    private EditText mPassword = null;
    final String regex = "^(?=.{7,}$)((.*[A-Za-z]+.*[0-9]+|.*[0-9]+.*[A-Za-z]+).*$)";
    Pattern patt = Pattern.compile(regex);
    private Crouton mCrouton = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.sdk_activity_signup, container, false);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mValidator = new Validator(this);
        mValidator.setValidationListener(this);

        mEmail = (AutoCompleteTextView) view.findViewById(R.id.email);
        mPhone = (AutoCompleteTextView) view.findViewById(R.id.phone_number);
        mPassword = (EditText) view.findViewById(R.id.password);

        /*Account[] accounts = AccountManager.get(getActivity()).getAccounts();
        Set<String> emailSet = new HashSet<String>();
        for (Account account : accounts) {
            if (SdkConstants.EMAIL_PATTERN.matcher(account.name).matches()) {
                emailSet.add(account.name);
            }
        }*/
        Set<String> emailSet = new HashSet<String>();
        mEmail.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, new ArrayList<String>(emailSet)));
        mEmail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mEmail.showDropDown();
                }
                return false;
            }
        });
        mSignUp = (Button) view.findViewById(R.id.done);
        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!SdkHelper.checkNetwork(getActivity())) {
                    Toast.makeText(getActivity(), R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                }
                else {

                    if (mCrouton != null) {
                        mCrouton.cancel();
                        mCrouton = null;
                    }

                    mValidator.validate();
                }
            }
        });

        mEmail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mEmail.showDropDown();
                }
                return false;
            }
        });

        ((TextView) view.findViewById(R.id.tos_n_privacy)).setMovementMethod(LinkMovementMethod.getInstance());


        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        //populateAutoComplete();
    }

    @Override
    public void onStop() {
        super.onStop();
            if (mCrouton != null) {
                mCrouton.cancel();
                mCrouton = null;
            }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Crouton.cancelAllCroutons();
    }

    public void onEventMainThread(SdkCobbocEvent event) {
        if (event.getType() == SdkCobbocEvent.SIGN_UP) {
            if (event.getStatus()) {

                // we'll now take you to login screen, which will, automatically put in your number, and will now listen for an incoming SMS
                //setResult(RESULT_OK);
                //finish();

                SdkSession.getInstance(getActivity().getApplicationContext()).create(mEmail.getText().toString().trim(), mPassword.getText().toString().trim());
            } else {
                // so we have an error
                mCrouton = Crouton.makeText(getActivity(), (String) event.getValue(), Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_LONG);
                mCrouton.show();
                resetButton();
            }
        } else if (event.getType() == SdkCobbocEvent.LOGIN) {
            if (event.getStatus()) {

                ((SdkLoginSignUpActivity)getActivity()).close();
            }
        }
    }

    void resetButton() {
        mPhone.setText("");
        mSignUp.setText(R.string.sign_up);
        mSignUp.setEnabled(true);
    }

    @Override
    public void onValidationSucceeded() {
        if (mCrouton != null) {
            mCrouton.cancel();
        }
        if (mPhone.getText().toString().length() < 10 || !mPhone.getText().toString().matches("[\\d]{10}$")) {
            mCrouton = Crouton.makeText(getActivity(), "The phone number entered is invalid", Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_LONG);
            mCrouton.show();
        }  else if (!mPassword.getText().toString().matches(regex)) {
            mCrouton = Crouton.makeText(getActivity(), "Password should be minimum 7 character with atleast 1 letter and 1 number", Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_LONG);
            mCrouton.show();
        }else {

            SdkSession.getInstance(getActivity().getApplicationContext()).sign_up(mEmail.getText().toString().trim(), mPhone.getText().toString().trim(), mPassword.getText().toString().trim());
            mSignUp.setText(R.string.please_wait);
            mSignUp.setEnabled(false);
        }
    }

    @Override
    public void onValidationFailed(View view, Rule<?> rule) {
        mCrouton = Crouton.makeText(getActivity(), rule.getFailureMessage(), Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_LONG);
        mCrouton.show();
        view.requestFocus();
    }

    /*@Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(),
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmail.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }*/

    /**
     * Use an AsyncTask to fetch the user's email addresses on a background thread, and update
     * the email text field with results on the main UI thread.
     */
    /*private class SetupEmailAutoCompleteTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... voids) {
            ArrayList<String> emailAddressCollection = new ArrayList<>();

            // Get all emails from the user's contacts and copy them to a list.
            ContentResolver cr = getActivity().getContentResolver();
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
    }*/
}
