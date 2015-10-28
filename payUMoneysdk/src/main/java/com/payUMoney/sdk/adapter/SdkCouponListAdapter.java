package com.payUMoney.sdk.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.utils.SdkLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SdkCouponListAdapter extends BaseAdapter {
    Context mContext;
    static JSONArray mCoupons = null;
    RadioButton mCoupanselected = null;

    public SdkCouponListAdapter(Context context, JSONArray coupans) {
        this.mContext = context;
        mCoupons = coupans;
    }

    @Override
    public int getCount() {
        return mCoupons.length();
    }

    @Override
    public Object getItem(int i) {
        try {
            return mCoupons.get(i);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        if (view == null) {
            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.sdk_coupons, null);
        }
        mCoupanselected = ((RadioButton) view.findViewById(R.id.coupanSelect));
        mCoupanselected.setFocusable(false);

        final JSONObject jsonObject = (JSONObject) getItem(i);


        try {
            SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            String dateString;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.parseLong(jsonObject.getString("expiryDate")));
            dateString = formater.format(calendar.getTime());
            SdkLogger.i("Dateformat", dateString);

            ((TextView) view.findViewById(R.id.coupanNameForUser)).setText(jsonObject.getString("couponString"));
            ((TextView) view.findViewById(R.id.coupanValidDate)).setText("valid upto \t \n" + dateString);


            /*if (HomeActivity.choosedItem == i) {
                mCoupanselected.setChecked(true);
            } else {
                mCoupanselected.setChecked(false);

            }*/

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return view;

    }




}
