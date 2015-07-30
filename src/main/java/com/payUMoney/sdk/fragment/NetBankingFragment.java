package com.payUMoney.sdk.fragment;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.payUMoney.sdk.Constants;
import com.payUMoney.sdk.HomeActivity;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.adapter.NetBankingAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 */
public class NetBankingFragment extends Fragment {

    MakePaymentListener mCallback;

    // Container Activity must implement this interface
    public interface MakePaymentListener {
        public void goToPayment(String mode, HashMap<String, Object> data) throws JSONException;
    }


    String bankCode, lastUsedBank;
    JSONObject bankObject;
    private ProgressBar pb;


    public NetBankingFragment() {
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
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment


        final View netBankingFragment = inflater.inflate(R.layout.fragment_net_banking, container, false);

        pb = (ProgressBar) netBankingFragment.findViewById(R.id.pb);

        netBankingFragment.findViewById(R.id.nbPayButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final HashMap<String, Object> data = new HashMap<String, Object>();
                try {

                    data.put("bankcode", bankCode);
                    data.put("key", ((HomeActivity) getActivity()).getBankObject().getJSONObject("paymentOption").getString("publicKey").replaceAll("\\r", ""));

                    pb.setVisibility(View.VISIBLE);
                    mCallback.goToPayment("NB", data);
                    // Session.getInstance(getActivity()).sendToPayUWithWallet(((HomeActivity)getActivity()).getBankObject(),"NB",data,getArguments().getDouble("cashback_amt"),getArguments().getDouble("wallet"));
                } catch (JSONException e) {
                    pb.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }


            }
        });
      /*  netBankingFragment.findViewById(R.id.useNewCardButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getFragmentManager().popBackStack();
            }
        });*/

        return netBankingFragment;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // fetch the data once again if last fetched at is less than 15 min
        try {
            bankObject = new JSONObject(((HomeActivity) getActivity()).getBankObject().getJSONObject(Constants.PAYMENT_OPTION).getString("nb"));
            JSONArray keyNames = bankObject.names();
//sagar_start
            JSONObject paymentOption = ((HomeActivity) getActivity()).getBankObject().getJSONObject(Constants.PAYMENT_OPTION);
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Constants.SP_SP_NAME, Context.MODE_PRIVATE);
            lastUsedBank = sharedPreferences.getString(Constants.LASTUSEDBANK, "XYZ");
            if (paymentOption != null && paymentOption.has("priority") && !paymentOption.isNull("priority"))
            {
                JSONObject priority = new JSONObject(paymentOption.getString("priority"));
                if (priority.has("preferredPaymentOption") && !priority.isNull("preferredPaymentOption"))
                {
                    JSONObject preferredPaymentOption = priority.getJSONObject("preferredPaymentOption");
                if (preferredPaymentOption.optString("optionType", "New User").equals("NB")) {
                    if(preferredPaymentOption.has("bankCode") && !preferredPaymentOption.isNull("bankCode")) {
                        lastUsedBank = preferredPaymentOption.getString("bankCode");
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(Constants.LASTUSEDBANK, lastUsedBank);
                        editor.commit();
                    }
                }
            }
        }
//sagar_end
            final String[][] banks1 = new String[102][2];

            for (int j = 0; j < keyNames.length(); j++) {
                String code = keyNames.getString(j);
                JSONObject object = bankObject.getJSONObject(code);
//sagar getting the last used bank on the top
                if (lastUsedBank.equals(code)) {
                    banks1[j][0] = Integer.toString(-1);
                    banks1[j][1] = object.getString("title");
                } else {
                    banks1[j][0] = object.getString("pt_priority");
                    banks1[j][1] = object.getString("title");
                }

            }

            for (int j = 0; j < keyNames.length(); j++) {
                for (int k = j + 1; k < keyNames.length(); k++) {
                    if (Integer.valueOf(banks1[k][0]) < Integer.valueOf(banks1[j][0])) {
                        String tmpRow[] = banks1[k];
                        banks1[k] = banks1[j];
                        banks1[j] = tmpRow;
                    }
                }
            }
            final String banks[] = new String[keyNames.length()];

            for (int j = 0; j < keyNames.length(); j++) {
                banks[j] = banks1[j][1];
            }
            setupAdapter(banks);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void setupAdapter(String banks[]) {

        NetBankingAdapter adapter = new NetBankingAdapter(getActivity(), banks);

        Spinner netBankingSpinner = (Spinner) getActivity().findViewById(R.id.netBankingSpinner);
        netBankingSpinner.setAdapter(adapter);

        //String text = netBankingSpinner.getSelectedItem().toString();
        netBankingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Object item = parent.getItemAtPosition(pos);
                Log.i("Item", item.toString());
                Iterator bankCodes = bankObject.keys();
                try {
                    while (bankCodes.hasNext()) {
                        final String code = (String) bankCodes.next();
                        JSONObject object = bankObject.getJSONObject(code);
                        if (object.getString("title").equals(item.toString())) {
                            bankCode = code;
                            getActivity().findViewById(R.id.nbPayButton).setEnabled(true);
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
