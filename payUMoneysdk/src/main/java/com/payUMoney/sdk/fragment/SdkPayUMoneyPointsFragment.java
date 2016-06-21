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
import android.widget.Toast;
import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkHomeActivityNew;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;
import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Created on 17/2/15.
 */
public class SdkPayUMoneyPointsFragment extends Fragment {
    private TextView tv = null;
    public double amt_net = 0.0, amount = 0.0, amtafterDicount = 0.0, amt_convenience = 0.0, amt_total = 0.0, amt_discount = 0.0;
    String s = null;
    final HashMap<String, Object> data = new HashMap<>();
    ProgressBar pb = null;
    Button checkout = null;
    JSONObject details = null;
    private double discount = 0.0, cashback = 0.0, couponAmt = 0.0;
    private boolean enoughDiscount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sdk_payumoney_points, container, false);
        pb = (ProgressBar) view.findViewById(R.id.progressBar);
        checkout = (Button) view.findViewById(R.id.checkout);
        tv = (TextView) view.findViewById(R.id.tv1);

        s = getArguments().getString("details");
        try {
            details = new JSONObject(s);

            discount = getArguments().getDouble("discount");
            cashback = getArguments().getDouble("cashback");
            couponAmt = getArguments().getDouble("couponAmount");
            enoughDiscount = getArguments().getBoolean("enoughDiscount");
            amt_convenience = getArguments().getDouble("convenienceChargesAmount");

            initiate();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        checkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!SdkHelper.checkNetwork(getActivity())) {
                    Toast.makeText(getActivity(), R.string.disconnected_from_internet, Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        pb.setVisibility(View.VISIBLE);
                        amt_net = round(amt_net, 2);
                        if (couponAmt > 0.0)
                            SdkSession.getInstance(getActivity()).sendToPayU(details, SdkConstants.POINTS, data, Double.valueOf(amt_net), Double.valueOf(couponAmt), amt_convenience);
                        else
                            SdkSession.getInstance(getActivity()).sendToPayU(details, SdkConstants.POINTS, data, Double.valueOf(amt_net), Double.valueOf(discount), amt_convenience);
                    } catch (Exception e) {
                        tv.setText("Something went wrong " + e.toString());
                    }
                }
            }
        });


        return view;
    }

    public void initiate() throws JSONException {
        if (couponAmt > 0.0)
            amt_discount = couponAmt;
        else if (discount > 0.0)
            amt_discount = discount;
        //amount = details.getJSONObject(Constants.PAYMENT).getDouble("totalAmount"); //Get amount user need to pay
        //amt_convenience = new JSONObject(details.getJSONObject(Constants.PAYMENT_OPTION).getString("convenienceFeeCharges")).getJSONObject("WALLET").getDouble(SdkConstants.DEFAULT);
        amount = details.getJSONObject(SdkConstants.PAYMENT).getDouble(SdkConstants.ORDER_AMOUNT);
        amt_total = amount + amt_convenience;
        amtafterDicount = amt_total - amt_discount;
        amt_net = amtafterDicount;

        StringBuffer s;
        if(enoughDiscount) {
            s = new StringBuffer("You don't need to pay anything else for this transaction, please click on \"Pay Now\" to complete transaction" +
                    "\n\n\n" + "Summary: \n" +
                    "\n\n***************************************\n\n" +
                    "Net Amount: " + round(amt_net, 2) + "\n");
        } else {
            s = new StringBuffer("You have enough PayUPoints, please click on \"Pay Now\" to complete transaction" +
                    "\n\n\n" + "Summary: \n" +
                    "\n\n***************************************\n\n" +
                    "Net Amount: " + round(amt_net, 2) + "\n");
        }

        if (amt_convenience > 0.0)
            s.append("Convenience Charge : ").append(round(amt_convenience, 2)).append("\n");
        if(amt_discount > 0.0) {
            if (couponAmt > 0.0)
                s.append("Coupon Discount: ").append(round(couponAmt, 2)).append("\n");
            else if (discount > 0.0)
                s.append("Discount: ").append(round(discount, 2)).append("\n");
        }
        else if (cashback > 0.0)
            s.append("Cashback: ").append(round(cashback, 2)).append("\n");

        s.append("Order Amount: ").append(round(amount, 2));

        if (((SdkHomeActivityNew) getActivity()).getPoints().doubleValue() > 0.0)
            s.append("\n\n***************************************\n\n" + "Available PayUMoney Points ₹ :").append(round(((SdkHomeActivityNew) getActivity()).getPoints().doubleValue(), 2));

        tv.setText(s);

    }


    @Override
    public void onResume() {
        super.onResume();
       // EventBus.getDefault().register(this);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        Intent intent = new Intent();
        intent.putExtra(SdkConstants.RESULT, "cancel");
        getActivity().setResult(getActivity().RESULT_CANCELED, intent);
        getActivity().finish();
    }


    @Override
    public void onPause() {
        super.onPause();
      //  EventBus.getDefault().unregister(this);
    }

    public static float round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

}