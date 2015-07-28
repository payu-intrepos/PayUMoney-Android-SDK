package com.payUMoney.sdk.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.payUMoney.sdk.Constants;
import com.payUMoney.sdk.HomeActivity;
import com.payUMoney.sdk.Luhn;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SetupCardDetails;
import com.payUMoney.sdk.entity.Card;
import com.payUMoney.sdk.interfaces.FragmentLifecycle;

import org.json.JSONException;

import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by sagar on 20/5/15.
 */
public class Credit extends Fragment implements FragmentLifecycle {

    MakePaymentListener mCallback;

    @Override
    public void onResumeFragment(HomeActivity activity) {


        EditText expiryDatePickerEditText = (EditText)activity.findViewById(R.id.expiryDatePickerEditText);
        String expiryDatePickerEditTextString = expiryDatePickerEditText.getText().toString();

        String delims = "[/]";
        String[] tokens = null;
        if(expiryDatePickerEditTextString != null)
            tokens = expiryDatePickerEditTextString.split(delims);

        if (tokens != null && tokens.length > 0 && isCvvValid && isCardNumberValid && isExpired) {


            int mnths = Integer.parseInt(tokens[0]);
            int yrs = Integer.parseInt(tokens[1]);

            checkExpiry(expiryDatePickerEditText, yrs, mnths, 0);

            valid(expiryDatePickerEditText, calenderDrawable);

        }

    }

    private void checkExpiry(EditText expiryDatePickerEditText, int i, int i2, int i3) {

        expiryDatePickerEditText.setText("" + (i2 + 1) + " / " + i);

        expiryMonth = i2 + 1;
        expiryYear = i;
        if (expiryYear > Calendar.getInstance().get(Calendar.YEAR)) {
            isExpired = false;
            valid(expiryDatePickerEditText, calenderDrawable);
        } else if (expiryYear == Calendar.getInstance().get(Calendar.YEAR) && expiryMonth - 1 >= Calendar.getInstance().get(Calendar.MONTH)) {
            isExpired = false;
            valid(expiryDatePickerEditText, calenderDrawable);
        } else {
            isExpired = true;
            invalid(expiryDatePickerEditText, calenderDrawable);
        }

    }

    // Container Activity must implement this interface
    public interface MakePaymentListener {
        public void goToPayment(String mode, HashMap<String, Object> data) throws JSONException;
    }

    private int expiryMonth = 7;
    private int expiryYear = 2025;
    private String cardNumber = "";
    private String cvv = "";

    DatePickerDialog.OnDateSetListener mDateSetListener;
    int mYear;
    int mMonth;
    int mDay;
    Boolean isCardNumberValid = false;
    Boolean isExpired = true;
    Boolean isCvvValid = false;
    Boolean card_store_check = true;
    Drawable nameOnCardDrawable;
    Drawable cardNumberDrawable;
    Drawable calenderDrawable;
    Drawable cvvDrawable;
    private CheckBox mCardStore;
    private EditText mCardLabel;
    View creditCardDetails;


    public Credit() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (MakePaymentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {

        Log.d("Sagar", "CreditCardFragment" + "onCreateView");

        creditCardDetails = inflater.inflate(R.layout.fragment_card_details, container, false);


        mYear = Calendar.getInstance().get(Calendar.YEAR);
        mMonth = Calendar.getInstance().get(Calendar.MONTH);
        mDay = Calendar.getInstance().get(Calendar.DATE);
        mCardLabel = (EditText) creditCardDetails.findViewById(R.id.label);
        mCardLabel.setText("");

        mCardStore = (CheckBox) creditCardDetails.findViewById(R.id.store_card);
        super.onActivityCreated(savedInstanceState);
        nameOnCardDrawable = getResources().getDrawable(R.drawable.user);
        cardNumberDrawable = getResources().getDrawable(R.drawable.card);
        calenderDrawable = getResources().getDrawable(R.drawable.calendar);
        cvvDrawable = getResources().getDrawable(R.drawable.lock);

        cardNumberDrawable.setAlpha(100);
        calenderDrawable.setAlpha(100);
        cvvDrawable.setAlpha(100);

        ((TextView) creditCardDetails.findViewById(R.id.enterCardDetailsTextView)).setText(getString(R.string.enter_credit_card_details));

        ((EditText) creditCardDetails.findViewById(R.id.cardNumberEditText)).setText("");
        ((EditText) creditCardDetails.findViewById(R.id.cardNumberEditText)).setCompoundDrawablesWithIntrinsicBounds(null, null, cardNumberDrawable, null);

        ((EditText) creditCardDetails.findViewById(R.id.expiryDatePickerEditText)).setText("");
        ((EditText) creditCardDetails.findViewById(R.id.expiryDatePickerEditText)).setCompoundDrawablesWithIntrinsicBounds(null, null, calenderDrawable, null);

        ((EditText) creditCardDetails.findViewById(R.id.cvvEditText)).setText("");
        ((EditText) creditCardDetails.findViewById(R.id.cvvEditText)).setCompoundDrawablesWithIntrinsicBounds(null, null, cvvDrawable, null);


        ((EditText) creditCardDetails.findViewById(R.id.cardNumberEditText)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                cardNumber = ((EditText) creditCardDetails.findViewById(R.id.cardNumberEditText)).getText().toString();

                if (cardNumber.startsWith("34") || cardNumber.startsWith("37"))
                    ((EditText) creditCardDetails.findViewById(R.id.cvvEditText)).setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                else
                    ((EditText) creditCardDetails.findViewById(R.id.cvvEditText)).setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

                if (cardNumber.length() > 11 && Luhn.validate(cardNumber)) {
                    // valid name on card
                    isCardNumberValid = true;

                    valid(((EditText) creditCardDetails.findViewById(R.id.cardNumberEditText)), SetupCardDetails.getCardDrawable(getResources(), cardNumber));
                } else {
                    isCardNumberValid = false;
                    invalid(((EditText) creditCardDetails.findViewById(R.id.cardNumberEditText)), cardNumberDrawable);
                    cardNumberDrawable.setAlpha(100);

                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        ((EditText) creditCardDetails.findViewById(R.id.cvvEditText)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                cvv = ((EditText) creditCardDetails.findViewById(R.id.cvvEditText)).getText().toString();
                if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) {
                    if (cvv.length() == 4) {
                        isCvvValid = true;
                        valid(((EditText) creditCardDetails.findViewById(R.id.cvvEditText)), cvvDrawable);

                    } else {
                        //invalid
                        isCvvValid = false;
                        invalid(((EditText) creditCardDetails.findViewById(R.id.cvvEditText)), cvvDrawable);
                    }
                } else {
                    if (cvv.length() == 3) {
                        //valid
                        isCvvValid = true;
                        valid(((EditText) creditCardDetails.findViewById(R.id.cvvEditText)), cvvDrawable);
                    } else {
                        //invalid
                        isCvvValid = false;
                        invalid(((EditText) creditCardDetails.findViewById(R.id.cvvEditText)), cvvDrawable);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        creditCardDetails.findViewById(R.id.cardNumberEditText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    makeInvalid();
                }
            }
        });


        creditCardDetails.findViewById(R.id.cvvEditText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    makeInvalid();
                }
            }
        });

        creditCardDetails.findViewById(R.id.expiryDatePickerEditText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    makeInvalid();
                }
            }
        });


        mCardStore.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (mCardStore.isChecked()) {
                    card_store_check = true;
                    mCardLabel.setVisibility(View.VISIBLE);
                } else {
                    card_store_check = false;
                    mCardLabel.setVisibility(View.GONE);
                }
            }
        });


        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int i, int i2, int i3) {

                checkExpiry(((EditText) creditCardDetails.findViewById(R.id.expiryDatePickerEditText)),i,i2,i3);

            }

        };

        calenderDrawable = getResources().getDrawable(R.drawable.calendar);

        creditCardDetails.findViewById(R.id.expiryDatePickerEditText).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    SetupCardDetails.customDatePicker(getActivity(), mDateSetListener, mYear, mMonth, mDay).show();
                }
                return false;
            }
        });

        creditCardDetails.findViewById(R.id.makePayment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String cardNumber = ((TextView) creditCardDetails.findViewById(R.id.cardNumberEditText)).getText().toString();

                final HashMap<String, Object> data = new HashMap<String, Object>();
                try {

                    if (cvv.equals("") || cvv == null)
                        data.put(Constants.CVV, "123");
                    else
                        data.put(Constants.CVV, cvv);

                    data.put(Constants.EXPIRY_MONTH, expiryMonth);
                    data.put(Constants.EXPIRY_YEAR, expiryYear);
                    data.put(Constants.NUMBER, cardNumber);
                    data.put("key", ((HomeActivity) getActivity()).getBankObject().getJSONObject("paymentOption").getString("publicKey").replaceAll("\\r", ""));

                    if (Card.isAmex(cardNumber)) {
                        data.put("bankcode", Constants.AMEX);
                    } else {
                        data.put("bankcode", SetupCardDetails.findIssuer(cardNumber, "CC"));
                    }
                    if (card_store_check == true) {
                        if (mCardLabel.getText().toString().trim().length() == 0) {
                            data.put(Constants.LABEL, "payu");
                            data.put(Constants.STORE, "1");
                        } else {
                            data.put(Constants.LABEL, mCardLabel.getText().toString());
                            data.put(Constants.STORE, "1");
                        }
                    }
                    mCallback.goToPayment("CC", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        return creditCardDetails;
    }


    private void valid(EditText editText, Drawable drawable) {
        drawable.setAlpha(255);
        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
        if (creditCardDetails.findViewById(R.id.expiryCvvLinearLayout).getVisibility() == View.GONE) {
            isExpired = false;
            isCvvValid = true;
        } else {

        }
        if (isCardNumberValid && !isExpired && isCvvValid) {
            creditCardDetails.findViewById(R.id.makePayment).setEnabled(true);
//            creditCardDetails.findViewById(R.id.makePayment).setBackgroundResource(R.drawable.button_enabled);
        } else {
            creditCardDetails.findViewById(R.id.makePayment).setEnabled(false);
//            creditCardDetails.findViewById(R.id.makePayment).setBackgroundResource(R.drawable.button);
        }
    }

    private void invalid(EditText editText, Drawable drawable) {
        drawable.setAlpha(100);
        editText.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
        creditCardDetails.findViewById(R.id.makePayment).setEnabled(false);
        creditCardDetails.findViewById(R.id.makePayment).setBackgroundResource(R.drawable.button);
    }

    private void makeInvalid() {
        if (!isCardNumberValid && cardNumber.length() > 0 && !creditCardDetails.findViewById(R.id.cardNumberEditText).isFocused())
            ((EditText) creditCardDetails.findViewById(R.id.cardNumberEditText)).setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.error_icon), null);
        if (!isCvvValid && cvv.length() > 0 && !creditCardDetails.findViewById(R.id.cvvEditText).isFocused())
            ((EditText) creditCardDetails.findViewById(R.id.cvvEditText)).setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.error_icon), null);
    }

}
