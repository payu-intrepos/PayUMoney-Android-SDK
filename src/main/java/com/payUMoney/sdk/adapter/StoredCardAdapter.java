package com.payUMoney.sdk.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.payUMoney.sdk.Constants;
import com.payUMoney.sdk.HomeActivity;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SetupCardDetails;
import com.payUMoney.sdk.fragment.StoredCardFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class StoredCardAdapter extends BaseAdapter

{
    MakePaymentListener mCallback;
    private boolean cvvDialogIsShowing = false;

    public interface MakePaymentListener {
        public void goToPayment(String mode, HashMap<String, Object> data) throws JSONException;
        public void handleViewDetails();
    }

    Context mContext;
    JSONArray mStoredCards;
    int toggle_flag = 0;
    String mode;
    Dialog cvvDialog;
    EditText cvv;
    Button positiveResponse, negativeResponse;

    private int mSelectedCard = -1;

    public StoredCardAdapter(Context context, JSONArray storedCards) {
        this.mContext = context;
        this.mStoredCards = storedCards;
        try {
            mCallback = (MakePaymentListener) mContext;
        } catch (ClassCastException e) {
            throw new ClassCastException(mContext.toString()
                    + " must implement OnHeadlineSelectedListener");
        }

    }

    @Override
    public int getCount() {
        return mStoredCards.length();
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

        cvvDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        cvvDialog.setContentView(R.layout.enter_cvv);


        cvvDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        cvv = (EditText) cvvDialog.findViewById(R.id.cvv);
        positiveResponse = (Button) cvvDialog.findViewById(R.id.makePayment);
        negativeResponse = (Button) cvvDialog.findViewById(R.id.view_details);


        negativeResponse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                mCallback.handleViewDetails();



            }
        });

        cvvDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {


                ((HomeActivity) mContext).updateDetails("");

                cvvDialogIsShowing = false;
            }
        });

        cvvDialog.setCanceledOnTouchOutside(false);
        cvvDialog.show();
        cvvDialogIsShowing = true;

    }

    @Override
    public View getView(final int position, View view, ViewGroup viewGroup)

    {
        Log.d("Sagar", "Position" + position);
        if (view == null) {
            LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.card, null);

        }

        /*if (view.findViewById(R.id.cvvBox) != null) {
            ((ViewGroup) view.findViewById(R.id.cvvBox).getParent()).removeView(view.findViewById(R.id.cvvBox));
        }

        final View cvvBox = ((LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.enter_cvv, null);*/

        final JSONObject jsonObject = (JSONObject) getItem(position);

        // set text here
        try {
            int img;
            switch (StoredCardFragment.getIssuer(jsonObject.getString("ccnum"), jsonObject.getString("cardType"))) {
                case LASER:
                    img = R.drawable.laser;
                    break;
                case VISA:
                    img = R.drawable.visa;
                    break;
                case MASTERCARD:
                    img = R.drawable.mastercard;
                    break;
                case MAESTRO:
                    img = R.drawable.maestro;
                    break;
                case JCB:
                    img = R.drawable.jcb;
                    break;
                case DINER:
                    img = R.drawable.diner;
                    break;
                case AMEX:
                    img = R.drawable.amex;
                    break;
                default:
                    img = R.drawable.card;
                    break;
            }
            ImageView imageview = (ImageView) view.findViewById(R.id.icon);
            imageview.setImageDrawable(view.getResources().getDrawable(img));

            ((TextView) view.findViewById(R.id.label)).setText(jsonObject.getString("cardName"));
            ((TextView) view.findViewById(R.id.number)).setText(jsonObject.getString("ccnum"));


            //If Card layout is clicked
            if (position == getSelectedCard()) {

                //((ViewGroup) view).addView(cvvBox, 1);

                handleCvvDialog();

                // final JSONObject jsonObject = (JSONObject) getItem(getSelectedCard());

                final HashMap<String, Object> data = new HashMap<String, Object>();

                data.put("storeCardId", jsonObject.getString("cardId"));

                data.put("store_card_token", jsonObject.getString("cardToken"));

                data.put(Constants.LABEL, jsonObject.getString("cardName"));

                data.put(Constants.NUMBER, "");

                if (jsonObject.getString("cardType").equals("CC"))
                    mode = "CC";
                else
                    mode = "DC";

                data.put("key", ((HomeActivity) mContext).getBankObject().getJSONObject("paymentOption").getString("publicKey").replaceAll("\\r", ""));


                if (!SetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode).equals("MAES")) {
                    positiveResponse.findViewById(R.id.makePayment).setEnabled(false);

                    cvv.setHint("CVV");

                    if (SetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode).equals("AMEX")) {
                        data.put("bankcode", Constants.AMEX);
                        cvv.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
                    } else {
                        data.put("bankcode", SetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode));

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
                                if (SetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode).equals("AMEX") && editable.toString().length() >= 4) {
                                    positiveResponse.setEnabled(true);
                                } else if (!SetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode).equals("AMEX") && editable.toString().length() >= 3) {
                                    positiveResponse.setEnabled(true);
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
                            //  Toast.makeText(mContext,String.valueOf(position),Toast.LENGTH_LONG).show();
                            data.put(Constants.CVV, cvv.getText().toString());
                            data.put(Constants.EXPIRY_MONTH, "");
                            data.put(Constants.EXPIRY_YEAR, "");
                            cvvDialog.dismiss();

                            try {
                                mCallback.goToPayment(mode, data);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });

                } else {
                    positiveResponse.setEnabled(true);

                    cvv.setHint("CVV(Optional)");

                    data.put("bankcode", SetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode));

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
                            } else if (editable.toString().length() > 0 && editable.toString().length() < 3) {
                                positiveResponse.setEnabled(false);
                            } else if (editable.toString().length() > 0 && editable.toString().length() >= 3) {
                                positiveResponse.setEnabled(true);
                            }
                        }
                    });

                    positiveResponse.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            if (cvv.getText().toString().length() > 0) {
                                data.put(Constants.CVV, cvv.getText().toString());
                            } else {
                                data.put(Constants.CVV, "123");
                            }

                            data.put(Constants.EXPIRY_MONTH, "");
                            data.put(Constants.EXPIRY_YEAR, "");

                            cvvDialog.dismiss();

                            try {
                                mCallback.goToPayment(mode, data);
                                // Session.getInstance(mContext).sendToPayUWithWallet(((HomeActivity) mContext).getBankObject(), mode, data, StoredCardFragment.cashback_amt,StoredCardFragment.wallet);
                            } catch (JSONException e) {
                                e.printStackTrace();
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

    public int getSelectedCard() {
        return mSelectedCard;
    }

    public void setSelectedCard(int selectedCard) {
        mSelectedCard = selectedCard;
    }
}
