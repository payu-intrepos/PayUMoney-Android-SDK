package com.payUMoney.sdk.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.payUMoney.sdk.HomeActivity;
import com.payUMoney.sdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by piyush on 17/12/14.
 */
public class CouponListAdapter extends BaseAdapter {
    Context mContext;
    static JSONArray mCoupons;
    int coupan_toggle_flag = 0;
    String mode;
    static Double mCoupanamt = 0.0;

    private int mSelectedCard = -1;
    RadioButton mCoupanselected;

    public CouponListAdapter(Context context, JSONArray coupans) {
        this.mContext = context;
        this.mCoupons = coupans;
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
            view = mInflater.inflate(R.layout.coupons, null);
        }
        mCoupanselected = ((RadioButton) view.findViewById(R.id.coupanSelect));
        mCoupanselected.setFocusable(false);

        final JSONObject jsonObject = (JSONObject) getItem(i);


        try {
            SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            String dateString;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(Long.parseLong(jsonObject.getString("expiryDate")));
            dateString = formater.format(calendar.getTime());
            Log.i("Dateformat", dateString);

            ((TextView) view.findViewById(R.id.coupanNameForUser)).setText(jsonObject.getString("couponStringForUser"));
            ((TextView) view.findViewById(R.id.coupanValidDate)).setText("valid upto\n" + dateString);


            if (HomeActivity.choosedItem == i) {
                mCoupanselected.setChecked(true);
            } else {
                mCoupanselected.setChecked(false);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return view;

    }


    public static String coupanString(int i) throws JSONException {

        return ((JSONObject) mCoupons.get(i)).getString("couponStringForUser");
    }

    public static Double coupanAmount() {
        return mCoupanamt;
    }

}
