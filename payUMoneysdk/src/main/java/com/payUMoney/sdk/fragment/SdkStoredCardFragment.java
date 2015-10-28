package com.payUMoney.sdk.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkHomeActivityNew;
import com.payUMoney.sdk.SdkSetupCardDetails;
import com.payUMoney.sdk.adapter.SdkStoredCardAdapter;
import com.payUMoney.sdk.entity.SdkIssuer;
import com.payUMoney.sdk.utils.SdkHelper;
import com.payUMoney.sdk.utils.SdkLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 */

public class SdkStoredCardFragment extends View {


    String mode = null;
    ListView listView = null;
    Context mContext;
    private String cardCvvToken;
    MakePaymentListener mCallback;

    public SdkStoredCardFragment(Context context) {
        super(context);
        mContext = context;
        mCallback = (MakePaymentListener) context;
    }

    public interface MakePaymentListener {
        void proceedForCvvLessTransaction(String b);

        void goToPayment(String mode, HashMap<String, Object> data) throws JSONException;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container) {

        SdkLogger.d(SdkConstants.TAG, "StoredCardFragment" + "onCreateView");
        View storedCardFragment = inflater.inflate(R.layout.sdk_fragment_stored_card, container, false);
        listView = (ListView) (storedCardFragment.findViewById(R.id.storedCardListView));  //Initialize ListView
        mContext = container.getContext();
        JSONArray arr = ((SdkHomeActivityNew) mContext).getStoredCardList();
        final SdkStoredCardAdapter adapter = new SdkStoredCardAdapter(mContext, arr); //Initialize the adapter
        listView.setAdapter(adapter);
        listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        listView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // disallow the onTouch for your scrollable parent view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        if (adapter.getCount() > 1) {
            int totalHeight = listView.getPaddingTop() + listView.getPaddingBottom();
            for (int i = 0; i < 2; i++) {
                View listItem = adapter.getView(i, null, listView);
                if (listItem instanceof ViewGroup) {
                    listItem.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight + (listView.getDividerHeight());
            listView.setLayoutParams(params);

        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                final JSONObject selectedCard = (JSONObject) adapterView.getAdapter().getItem(i);
                try {
                    SharedPreferences mPref = mContext.getSharedPreferences(SdkConstants.SP_SP_NAME, Activity.MODE_PRIVATE);
                    mode = selectedCard.getString("pg");
                    ((SdkHomeActivityNew) mContext).updateDetails(mode);

                    if (selectedCard.has("cardCvvToken") && !selectedCard.isNull("cardCvvToken") && mPref != null && mPref.getBoolean(SdkConstants.ONE_TAP_PAYMENT, false)) {
                        cardCvvToken = selectedCard.getString("cardCvvToken");
                        proceedToPay(selectedCard);
                    } else {
                        //to show the cvv popup
                        //  if (adapter.getSelectedCard() != i) {

                        // }
                        adapter.setSelectedCard(i);
                        adapter.notifyDataSetInvalidated();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        return storedCardFragment;
    }

    private void proceedToPay(JSONObject jsonObject) {
        try {

            final HashMap<String, Object> data = new HashMap<>();

            data.put("storeCardId", jsonObject.getString("cardId"));
            data.put("store_card_token", jsonObject.getString("cardToken"));
            data.put(SdkConstants.LABEL, jsonObject.getString("cardName"));
            data.put(SdkConstants.NUMBER, "");
            /*data.put(SdkConstants.CARD_CVV_MERCHANT, jsonObject.getString("cvvToken"));*/

            /*if (jsonObject.getString("cardType").equals("CC"))
                mode = "CC";
            else
                mode = "DC";
*/
            data.put("key", ((SdkHomeActivityNew) mContext).getPublicKey());
            data.put("bankcode", SdkSetupCardDetails.findIssuer(jsonObject.getString("ccnum"), mode));

            if (!SdkHelper.checkNetwork(mContext)) {
                Toast.makeText(mContext, R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
            } else {
                //  Toast.makeText(mContext,String.valueOf(position),Toast.LENGTH_LONG).show();
                // data.put(SdkConstants.CVV, cvv.getText().toString());
                data.put(SdkConstants.EXPIRY_MONTH, "");
                data.put(SdkConstants.EXPIRY_YEAR, "");
                /*if (card_store_check) {
                    data.put(SdkConstants.STORE_CARD_WITH_CVV, "1");
                }*/
                mCallback.proceedForCvvLessTransaction(cardCvvToken);
                mCallback.goToPayment(mode, data);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static SdkIssuer getIssuer(String mNumber, String cardMode) {
        if (mNumber.length() > 3) {
            if (mNumber.startsWith("4")) {
                return SdkIssuer.VISA;
            } else if (Arrays.asList("6304", "6706", "6771", "6709").contains(mNumber.substring(0, 4))) {
                return SdkIssuer.LASER;
            }/* else if(mNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}[\\d]+")) {
            return "DISCOVER";
        }*/ else if (mNumber.matches("(5[06-8]|6\\d|\\D)\\d{14}|\\D{14}(\\d{2,3}|\\D{2,3})?[\\d|\\D]+") || mNumber.matches("(5[06-8]|6\\d|\\D)[\\d|\\D]+") || mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d|\\D)[\\d|\\D]+")) {
                return SdkIssuer.MAESTRO;
            } else if (mNumber.matches("^5[1-5][\\d|\\D]+")) {
                return SdkIssuer.MASTERCARD;
            } else if (mNumber.matches("^3[47][\\d|\\D]+")) {
                return SdkIssuer.AMEX;
            } else if (mNumber.startsWith("36") || mNumber.matches("^30[0-5][\\d|\\D]+")) {
                return SdkIssuer.DINER;
            } else if (mNumber.matches("2(014|149)[\\d|\\D]+")) {
                return SdkIssuer.DINER;
            } else if (mNumber.matches("^35(2[89]|[3-8][0-9])[\\d|\\D]+")) {
                return SdkIssuer.JCB;
            } else {
                if (cardMode.contentEquals("CC"))
                    return SdkIssuer.UNKNOWN;
                else if (cardMode.contentEquals("DC"))
                    return SdkIssuer.MASTERCARD;
            }
        }
        return null;
    }

}
