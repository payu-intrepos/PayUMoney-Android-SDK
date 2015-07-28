package com.payUMoney.sdk.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.payUMoney.sdk.HomeActivity;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.adapter.StoredCardAdapter;
import com.payUMoney.sdk.entity.Issuer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 */

public class StoredCardFragment extends Fragment {


    String mode;
    ListView listView;

    public StoredCardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d("Sagar", "StoredCardFragment" + "onCreateView");
        View storedCardFragment = inflater.inflate(R.layout.fragment_stored_card, container, false);
        listView = (ListView) (storedCardFragment.findViewById(R.id.storedCardListView));  //Initialize ListView
        final StoredCardAdapter adapter = new StoredCardAdapter(getActivity(), ((HomeActivity) getActivity()).getStoredCardList()); //Initialize the adapter
        listView.setAdapter(adapter);

        listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                final JSONObject selectedCard = (JSONObject) adapterView.getAdapter().getItem(i);


                try {
                    mode = selectedCard.getString("pg");
                    ((HomeActivity) getActivity()).updateDetails(mode);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
    //to show the cvv popup
              //  if (adapter.getSelectedCard() != i) {
                    adapter.setSelectedCard(i);
                    adapter.notifyDataSetInvalidated();
               // }

            }
        });

        return storedCardFragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d("Sagar", "StoredCardFragment" + "onActivityCreated");
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onDestroy() {
        Log.e("ACT", "onDestroy called");
        super.onDestroy();
    }


    public static Issuer getIssuer(String mNumber, String cardMode) {
        if (mNumber.length() > 3) {
            if (mNumber.startsWith("4")) {
                return Issuer.VISA;
            } else if (Arrays.asList("6304", "6706", "6771", "6709").contains(mNumber.substring(0, 4))) {
                return Issuer.LASER;
            }/* else if(mNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}[\\d]+")) {
            return "DISCOVER";
        }*/ else if (mNumber.matches("(5[06-8]|6\\d|\\D)\\d{14}|\\D{14}(\\d{2,3}|\\D{2,3})?[\\d|\\D]+") || mNumber.matches("(5[06-8]|6\\d|\\D)[\\d|\\D]+") || mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d|\\D)[\\d|\\D]+")) {
                return Issuer.MAESTRO;
            } else if (mNumber.matches("^5[1-5][\\d|\\D]+")) {
                return Issuer.MASTERCARD;
            } else if (mNumber.matches("^3[47][\\d|\\D]+")) {
                return Issuer.AMEX;
            } else if (mNumber.startsWith("36") || mNumber.matches("^30[0-5][\\d|\\D]+")) {
                return Issuer.DINER;
            } else if (mNumber.matches("2(014|149)[\\d|\\D]+")) {
                return Issuer.DINER;
            } else if (mNumber.matches("^35(2[89]|[3-8][0-9])[\\d|\\D]+")) {
                return Issuer.JCB;
            } else {
                if (cardMode.contentEquals("CC"))
                    return Issuer.UNKNOWN;
                else if (cardMode.contentEquals("DC"))
                    return Issuer.MASTERCARD;
            }
        }
        return null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // HomeActivity.mPayUpoints.setVisibility(View.GONE);
    }


}
