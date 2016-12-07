package com.payUMoney.sdk.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Required;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Allows the user to get a new password
 */
public class SdkForgotPasswordFragment extends Fragment implements Validator.ValidationListener /* LoaderManager.LoaderCallbacks<Cursor> */{

    @Required(order = 1, message = "Email_is_required")
    @Email(order = 2, message = "email_is_invalid")
    private AutoCompleteTextView mEmail = null;
    private Button done = null;
    private Validator mValidator = null;
    private Crouton mCrouton = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.sdk_activity_forgot_password, container, false);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mEmail = (AutoCompleteTextView) view.findViewById(R.id.email);
        done = (Button) view.findViewById(R.id.done);

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

        mValidator = new Validator(this);

        mValidator.setValidationListener(this);

        mEmail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mEmail.showDropDown();
                }
                return false;
            }
        });
        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(!SdkHelper.checkNetwork(getActivity())) {
                    Toast.makeText(getActivity(), R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                }else {
                    mValidator.validate();
                }
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mCrouton != null){
            mCrouton.cancel();
            mCrouton = null;
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.sign_in) {
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    /**
     * {@link EventBus} listener for {@link SdkCobbocEvent}
     */
    public void onEventMainThread(SdkCobbocEvent event) {
        switch (event.getType()) {
            case SdkCobbocEvent.FORGOT_PASSWORD:
                if (event.getStatus()) {

                    try {
                        JSONObject details = (JSONObject) event.getValue();
                        if (details.getString("result").equals("User is null")) {

                            Toast.makeText(getActivity(), "Email is not registered with PayUMoney", Toast.LENGTH_LONG).show();
                            done.setText(R.string.recover_password);
                            done.setEnabled(true);
                        }
                        // user was sent the email
                        else {
                            Toast.makeText(getActivity(), R.string.email_sent_apologies_from_us, Toast.LENGTH_LONG).show();
                            getActivity().setResult(Activity.RESULT_OK);
                            getActivity().finish();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (mCrouton != null) {
                        mCrouton.hide();
                        mCrouton = null;
                    }
                    mCrouton = Crouton.makeText(getActivity(), R.string.something_went_wrong, Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
                    mCrouton.show();
                    done.setText(R.string.recover_password);
                    done.setEnabled(true);
                }
                break;
            default:
        }
    }

    @Override
    public void onValidationSucceeded() {
        if (mCrouton != null) {
            mCrouton.cancel();
        }

        done.setText(R.string.please_wait);
        done.setEnabled(false);

        SdkSession.getInstance(getActivity().getApplicationContext()).forgotPassword(mEmail.getText().toString());
    }

    @Override
    public void onValidationFailed(View view, Rule<?> rule) {
        mCrouton = Crouton.makeText(getActivity(), rule.getFailureMessage(), Style.ALERT).setConfiguration(SdkConstants.CONFIGURATION_SHORT);
        mCrouton.show();
        view.requestFocus();
    }

    /*@Override
    public void onPause() {
        if(mCrouton != null){
            mCrouton.cancel();
            mCrouton = null;
        }
        super.onPause();
    }*/

    /*@Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
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
        return null;
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

    }*/

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmail.setAdapter(adapter);
    }

    /*private interface ProfileQuery {
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
