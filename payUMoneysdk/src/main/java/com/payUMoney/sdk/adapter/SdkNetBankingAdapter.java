package com.payUMoney.sdk.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONArray;

/**
 * Created by franklin on 10/7/14.
 */
public class SdkNetBankingAdapter extends BaseAdapter {

    Context mContext;
    JSONArray availableBanks = null;
    String[] banks;

    public SdkNetBankingAdapter(Context context, String[] availableBanks) {
        this.mContext = context;
        this.banks = availableBanks;
    }

    @Override
    public int getCount() {
        if (banks.length > 0) {
            return banks.length;
        } else {
            return 0;
        }
    }

    @Override
    public String getItem(int position) {
        return banks[position];


    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        if (view == null) {
            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(android.R.layout.simple_list_item_1, null);

           // view.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.button_bkg_selector));
        }
        String bankName = getItem(position);
        ((TextView) view).setText(bankName);
        return view;
    }

}
