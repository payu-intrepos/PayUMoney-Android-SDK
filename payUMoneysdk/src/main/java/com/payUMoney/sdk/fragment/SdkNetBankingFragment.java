package com.payUMoney.sdk.fragment;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkHomeActivityNew;
import com.payUMoney.sdk.adapter.SdkNetBankingAdapter;
import com.payUMoney.sdk.utils.SdkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 */
public class SdkNetBankingFragment extends View {

    MakePaymentListener mCallback = null;
    private String key = null;
    private View netBankingFragment = null;

    // Container Activity must implement this interface
    public interface MakePaymentListener {
        void goToPayment(String mode, HashMap<String, Object> data) throws JSONException;
        void modifyConvenienceCharges(String cardBankType);
    }


    String bankCode = null, lastUsedBank = null;
    JSONObject bankObject = null;
    Context mContext;

    public SdkNetBankingFragment(Context context) {
        // Required empty public constructor
        super(context);
        mContext = context;
        onAttach((SdkHomeActivityNew)context);
    }


    public void onAttach(Activity activity) {

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (MakePaymentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
        // Inflate the layout for this fragment
        netBankingFragment = inflater.inflate(R.layout.sdk_fragment_net_banking, container, false);
        onActivityCreated();
        netBankingFragment.findViewById(R.id.nbPayButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!SdkHelper.checkNetwork(mContext)) {
                    Toast.makeText(mContext, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                } else {
                    final HashMap<String, Object> data = new HashMap<>();
                    try {

                        data.put(SdkConstants.BANK_CODE, bankCode);
                        data.put("key", key);

                        mCallback.goToPayment(SdkConstants.PAYMENT_MODE_NB, data);
                    } catch (JSONException e) {
                        Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            }
        });

        return netBankingFragment;

    }


    public void onActivityCreated() {

        try {
            JSONObject tempDetails = ((SdkHomeActivityNew) mContext).getBankObject();
            if(tempDetails != null && tempDetails.has(SdkConstants.PAYMENT_OPTION) && !tempDetails.isNull(SdkConstants.PAYMENT_OPTION))
            {
                JSONObject paymentOption = tempDetails.getJSONObject(SdkConstants.PAYMENT_OPTION);
                if(paymentOption != null && paymentOption.has(SdkConstants.OPTIONS) && !paymentOption.isNull(SdkConstants.OPTIONS))
                {
                    JSONObject tempOptions = paymentOption.getJSONObject(SdkConstants.OPTIONS);
                    if(tempOptions != null && tempOptions.has("nb") && !tempOptions.isNull("nb"))
                    {
                        bankObject = new JSONObject(tempOptions.getString("nb"));
                    }
                }
                SharedPreferences sharedPreferences = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Context.MODE_PRIVATE);
                lastUsedBank = sharedPreferences.getString(SdkConstants.LASTUSEDBANK, SdkConstants.XYZ_STRING);
                if(paymentOption != null && paymentOption.has(SdkConstants.CONFIG) && !paymentOption.isNull(SdkConstants.CONFIG)) {
                    JSONObject tempConfig = paymentOption.getJSONObject(SdkConstants.CONFIG);
                    if (tempConfig != null && tempConfig.has(SdkConstants.PREFERRED_PAYMENT_OPTION) && !tempConfig.isNull(SdkConstants.PREFERRED_PAYMENT_OPTION)) {
                        JSONObject tempPreferredPaymentOption = tempConfig.getJSONObject(SdkConstants.PREFERRED_PAYMENT_OPTION);
                        if (tempPreferredPaymentOption != null && tempPreferredPaymentOption.has("optionType") && tempPreferredPaymentOption.optString("optionType", "").equals(SdkConstants.PAYMENT_MODE_NB)) {
                            lastUsedBank = tempPreferredPaymentOption.getString(SdkConstants.BANK_CODE_STRING);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(SdkConstants.LASTUSEDBANK, lastUsedBank);
                            editor.commit();
                        }
                    }
                    if(tempConfig != null && tempConfig.has("publicKey") && !tempConfig.isNull("publicKey"))
                    {
                        key = tempConfig.getString("publicKey").replaceAll("\\r", "");
                    }
                }
            }

            if(bankObject != null) {

                JSONArray keyNames = bankObject.names();
                final String[][] banks1 = new String[102][2];
                for (int j = 0; j < keyNames.length(); j++) {
                    String code = keyNames.getString(j);
                    JSONObject object = bankObject.getJSONObject(code);
                    //sagar getting the last used bank on the top
                    if (lastUsedBank.equals(code)) {
                        banks1[j][0] = Integer.toString(-1);
                        banks1[j][1] = object.getString(SdkConstants.BANK_TITLE_STRING);
                    } else {
                        banks1[j][0] = object.getString("pt_priority");
                        banks1[j][1] = object.getString(SdkConstants.BANK_TITLE_STRING);
                    }
                }
                for (int j = 0; j < keyNames.length(); j++) {
                    for (int k = j + 1; k < keyNames.length(); k++) {
                        if (Integer.valueOf(banks1[k][0]).intValue() < Integer.valueOf(banks1[j][0]).intValue()) {
                            String tmpRow[] = banks1[k];
                            banks1[k] = banks1[j];
                            banks1[j] = tmpRow;
                        }
                    }
                }
                final String banks[] = new String[keyNames.length()];
               // banks[0] = "Select Your Bank";
                for (int j = 0; j < keyNames.length(); j++) {
                    banks[j] = banks1[j][1];
                }
                setupAdapter(banks);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void setupAdapter(String banks[]) {

        /*if (banks.length > 0) {
            banks[banks.length - 1] = "Select Your Bank";
        }*/
        SdkNetBankingAdapter adapter = new SdkNetBankingAdapter(mContext, banks);
        Spinner netBankingSpinner = (Spinner)netBankingFragment.findViewById(R.id.netBankingSpinner);
        netBankingSpinner.setAdapter(adapter);

        //String text = netBankingSpinner.getSelectedItem().toString();
        netBankingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Object item = parent.getItemAtPosition(pos);
                //SdkLogger.i("Item", item.toString());
                Iterator bankCodes = bankObject.keys();
                try {
                    while (bankCodes.hasNext()) {
                        final String code = (String) bankCodes.next();
                        JSONObject object = bankObject.getJSONObject(code);
                        if (object.getString(SdkConstants.BANK_TITLE_STRING).equals(item.toString())) {
                            bankCode = code;
                            ((SdkHomeActivityNew)mContext).findViewById(R.id.nbPayButton).setEnabled(true);
                            mCallback.modifyConvenienceCharges(bankCode);
                            break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


    }


}
