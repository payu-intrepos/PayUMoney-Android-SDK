package com.payUMoney.sdk.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.payUMoney.sdk.CobbocEvent;
import com.payUMoney.sdk.Constants;
import com.payUMoney.sdk.HomeActivity;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.Session;
import com.payUMoney.sdk.WebViewActivityPoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;

import de.greenrobot.event.EventBus;

/**
 * Created by karan on 17/2/15.
 */

public class PayUMoneyPointsFragment extends Fragment {
    private TextView tv;
    public double amt_net, amount, amtafterDicount, amt_convenience, amt_total, amt_discount, coupan_amt;
    String s;
    final HashMap<String, Object> data = new HashMap<String, Object>();
    ProgressBar pb;
    Button checkout;
    public static String choosedCoupan;
    JSONObject details;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.payumoney_points, container, false);
        pb = (ProgressBar) view.findViewById(R.id.progressBar);
        checkout = (Button) view.findViewById(R.id.checkout);
        tv = (TextView) view.findViewById(R.id.tv1);

        s = getArguments().getString("details");
        if (s != null) {
            try {
                details = new JSONObject(s);

            } catch (Exception e) {
                tv.setText(e.toString());
            }
        } else {
            tv.setText("Error: Please Try Again");
        }

        try {
            initiate();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        checkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    pb.setVisibility(View.VISIBLE);
                    amt_net = round(amt_net, 2);
                    Session.getInstance(getActivity()).sendToPayU(details, "points", data, amt_net); //amt_net=cashback
                } catch (Exception e) {
                    tv.setText("Something went wrong " + e.toString());
                }
            }
        });


        return view;
    }

    public void onEventMainThread(final CobbocEvent event) {  //If payment goes through
        if (event.getType() == CobbocEvent.PAYMENT_POINTS) {
            if (event.getStatus()) {
                pb.setVisibility(View.INVISIBLE);
                Intent intent = new Intent(getActivity(), WebViewActivityPoints.class);
                intent.putExtra(Constants.RESULT, event.getValue().toString());
                getActivity().startActivityForResult(intent, ((HomeActivity) getActivity()).WEB_VIEW);

            }
        }
    }

    public void initiate() throws JSONException {

            amt_discount = getArguments().getDouble("discount");

        amount = details.getJSONObject(Constants.PAYMENT).getDouble("totalAmount"); //Get amount user need to pay
        amt_convenience = new JSONObject(details.getJSONObject(Constants.TRANSACTION_DTO).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble("DEFAULT");
        amt_total = amount + amt_convenience;
        amtafterDicount = amt_total - amt_discount;
        amt_net = amtafterDicount;


        tv.setText("You have enough PayUPoints, please click on \"Pay Now\" to complete transaction" +
                "\n\n\n" + "Summary: \n" +
                "\n\n***************************************\n\n" +
                "Net Amount: " + round(amt_net, 2) + "\n" +
                "Convenience Charge : " + round(amt_convenience, 2) + "\n" +
                "Discount: " + round(amt_discount, 2) + "\n" +
                "Total Amount: " + round(amount, 2) +
                "\n\n***************************************\n\n" +
                "Available PayUMoney Points (in rs):" + round(((HomeActivity) getActivity()).getPoints(), 2));

    }


    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        Intent intent = new Intent();
        intent.putExtra(Constants.RESULT, "cancel");
        getActivity().setResult(getActivity().RESULT_CANCELED, intent);
        getActivity().finish();
    }


    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public static float round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

}