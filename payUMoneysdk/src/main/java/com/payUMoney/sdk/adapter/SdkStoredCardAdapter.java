package com.payUMoney.sdk.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkHomeActivityNew;
import com.payUMoney.sdk.SdkSetupCardDetails;
import com.payUMoney.sdk.fragment.SdkStoredCardFragment;
import com.payUMoney.sdk.utils.SdkHelper;
import com.payUMoney.sdk.utils.SdkLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class SdkStoredCardAdapter extends BaseAdapter {

    MakePaymentListener mCallback;
    private boolean cvvDialogIsShowing = false;

    // private CheckBox saveCvv;
   // private Boolean card_store_check = true;

    public interface MakePaymentListener {
         void goToPayment(String mode, HashMap<String, Object> data) throws JSONException;
    }

    Context mContext;
    JSONArray mStoredCards;
    String mode = null;
    Dialog cvvDialog = null;
    EditText cvv = null;
    Button positiveResponse = null, negativeResponse = null;

    private int mSelectedCard = -1;

    public SdkStoredCardAdapter(Context context, JSONArray storedCards) {
        this.mContext = context;
        this.mStoredCards = storedCards;
        mCallback = (MakePaymentListener)context;

        try {
        } catch (ClassCastException e) {
            throw new ClassCastException(mContext.toString()
                    + " must implement OnHeadlineSelectedListener");
        }

    }

    @Override
    public int getCount() {
        try {
            return mStoredCards.length();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        try {
            return mStoredCards.get(i);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


    private void handleCvvDialog() {


        if (cvvDialogIsShowing)
            return;

        cvvDialog = new Dialog(mContext);

        cvvDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        cvvDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        cvvDialog.setContentView(R.layout.sdk_enter_cvv);


        cvvDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        cvv = (EditText) cvvDialog.findViewById(R.id.cvv);
        positiveResponse = (Button) cvvDialog.findViewById(R.id.makePayment);
        negativeResponse = (Button) cvvDialog.findViewById(R.id.cancel);
        //saveCvv = (CheckBox) cvvDialog.findViewById(R.id.saveCvv);
        negativeResponse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cvvDialog.dismiss();
            }
        });

        /*saveCvv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (saveCvv.isChecked()) {
                    card_store_check = Boolean.TRUE;
                } else {
                    card_store_check = Boolean.FALSE;
                }
            }
        });*/
        SharedPreferences mPref = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
        if (mPref.getBoolean(SdkConstants.ONE_CLICK_PAYMENT, false)){
            cvvDialog.findViewById(R.id.sdk_tnc).setVisibility(View.VISIBLE);
            ((TextView)cvvDialog.findViewById(R.id.sdk_tnc)).setMovementMethod(LinkMovementMethod.getInstance());
        }
        cvv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    cvvDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        cvvDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

                ((SdkHomeActivityNew) mContext).updateDetails("");

                cvvDialogIsShowing = false;
            }
        });
        cvvDialog.setCanceledOnTouchOutside(true);
        cvvDialog.show();
        cvvDialogIsShowing = true;

    }



    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {

        SdkLogger.d(SdkConstants.TAG, ": Position" + position);
        if (view == null) {
            LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.sdk_card, null);
        }
        /*if (view.findViewById(R.id.cvvBox) != null) {
            ((ViewGroup) view.findViewById(R.id.cvvBox).getParent()).removeView(view.findViewById(R.id.cvvBox));
        }
        final View cvvBox = ((LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.enter_cvv, null);*/
        try {
        final JSONObject jsonObject = (JSONObject) getItem(position);

        // set text heren

            int img;
            switch (SdkStoredCardFragment.getIssuer(jsonObject.getString("ccnum"), jsonObject.getString("pg"))) {
                case LASER:
                    img = R.drawable.card_laser;
                    break;
                case VISA:
                    img = R.drawable.card_visa;
                    break;
                case MASTERCARD:
                    img = R.drawable.card_master;
                    break;
                case MAESTRO:
                    img = R.drawable.card_maestro;
                    break;
                case JCB:
                    img = R.drawable.card_jcb;
                    break;
                case DINER:
                    img = R.drawable.card_diner;
                    break;
                case AMEX:
                    img = R.drawable.card_amex;
                    break;
                case RUPAY:
                    img = R.drawable.card_rupay;
                    break;
                default:
                    img = R.drawable.card;
                    break;
            }
            ImageView imageview = (ImageView) view.findViewById(R.id.sdk_card_type_imageView);
            imageview.setImageDrawable(view.getResources().getDrawable(img));

            String cardNumber = jsonObject.getString("ccnum");
            int SHOW_CARD_NUMBERS_COUNT = 4;
            cardNumber = cardNumber.substring(cardNumber.length() - SHOW_CARD_NUMBERS_COUNT, cardNumber.length());
            ((TextView) view.findViewById(R.id.sdk_card_label_textView)).setText(jsonObject.getString("cardName"));
            ((TextView) view.findViewById(R.id.sdk_card_number_textView)).setText("\u2022" + "\u2022" + "\u2022" + "\u2022" + cardNumber);

            //If Card layout is clicked
            /*if (position == getSelectedCard() && jsonObject.has("cardCvvToken") && !jsonObject.isNull("cardCvvToken") ) {

                    cvvToken = jsonObject.getString("cardCvvToken");


            } else */ if (position == getSelectedCard()) {

                handleCvvDialog();

                // final JSONObject jsonObject = (JSONObject) getItem(getSelectedCard());

                final HashMap<String, Object> data = new HashMap<>();

                data.put("storeCardId", jsonObject.getString("cardId"));

                data.put("store_card_token", jsonObject.getString(SdkConstants.CARD_TOKEN));

                data.put(SdkConstants.LABEL, jsonObject.getString("cardName"));

                data.put(SdkConstants.NUMBER, "");

                if (jsonObject.getString("pg").equals(SdkConstants.PAYMENT_MODE_CC))
                    mode = SdkConstants.PAYMENT_MODE_CC;
                else
                    mode = SdkConstants.PAYMENT_MODE_DC;

                data.put("key", ((SdkHomeActivityNew) mContext).getPublicKey());

                if (!SdkSetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode).equals("MAES")) {
                    positiveResponse.setEnabled(false);

                    cvv.setHint("CVV");

                    if (SdkSetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode).equals("AMEX")) {
                        data.put(SdkConstants.BANK_CODE, SdkConstants.AMEX);
                        cvv.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                    } else {
                        data.put(SdkConstants.BANK_CODE, SdkSetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode));

                        cvv.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
                    }

                    cvv.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            try {
                                if (SdkSetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode).equals("AMEX") && editable.toString().length() >= 4) {
                                    positiveResponse.setEnabled(true);
                                    hideKeyboardIfShown(cvv);
                                } else if (!SdkSetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode).equals("AMEX") && editable.toString().length() >= 3) {
                                    positiveResponse.setEnabled(true);
                                    hideKeyboardIfShown(cvv);
                                } else {
                                    positiveResponse.setEnabled(false);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    positiveResponse.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!SdkHelper.checkNetwork(mContext)) {
                                Toast.makeText(mContext, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                            } else {
                                //  Toast.makeText(mContext,String.valueOf(position),Toast.LENGTH_LONG).show();
                                data.put(SdkConstants.CVV, cvv.getText().toString());
                                data.put(SdkConstants.EXPIRY_MONTH, "");
                                data.put(SdkConstants.EXPIRY_YEAR, "");
                                /*if (card_store_check) {
                                    data.put(SdkConstants.STORE_CARD_WITH_CVV, "1");
                                }*/
                                cvvDialog.dismiss();
                                try {
                                    mCallback.goToPayment(mode, data);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

                } else {
                    positiveResponse.setEnabled(true);

                    cvv.setHint("CVV(Optional)");

                    data.put(SdkConstants.BANK_CODE, SdkSetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode));

                    cvv.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

                    cvv.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if (editable.toString().length() == 0) {
                                positiveResponse.setEnabled(true);
                                hideKeyboardIfShown(cvv);
                            } else if (editable.toString().length() > 0 && editable.toString().length() < 3) {
                                positiveResponse.setEnabled(false);
                            } else if (editable.toString().length() > 0 && editable.toString().length() >= 3) {
                                positiveResponse.setEnabled(true);
                                hideKeyboardIfShown(cvv);
                            }
                        }
                    });

                    positiveResponse.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((SdkHomeActivityNew)mContext).hideKeyboardIfShown();
                            if (!SdkHelper.checkNetwork(mContext)) {
                                Toast.makeText(mContext, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                            } else {

                                if (cvv.getText().toString().length() > 0) {
                                    data.put(SdkConstants.CVV, cvv.getText().toString());
                                } else {
                                    data.put(SdkConstants.CVV, "123");
                                }

                                data.put(SdkConstants.EXPIRY_MONTH, "");
                                data.put(SdkConstants.EXPIRY_YEAR, "");
                                /*if (card_store_check) {
                                    data.put(SdkConstants.STORE_CARD_WITH_CVV, "1");
                                }*/

                                cvvDialog.dismiss();

                                try {
                                    mCallback.goToPayment(mode, data);
                                    // Session.getInstance(mContext).sendToPayUWithWallet(((HomeActivity) mContext).getBankObject(), mode, data, StoredCardFragment.cashback_amt,StoredCardFragment.wallet);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                    });

                }
                mSelectedCard = -1;
            }

//
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return view;

    }

    public void hideKeyboardIfShown(EditText cvv) {

        InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(cvv.getWindowToken(), 0);
    }

    public int getSelectedCard() {
        return mSelectedCard;
    }

    public void setSelectedCard(int selectedCard) {
        mSelectedCard = selectedCard;
    }
}
