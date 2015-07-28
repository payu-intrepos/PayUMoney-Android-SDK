package com.payUMoney.sdk.adapter;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.payUMoney.sdk.fragment.Credit;
import com.payUMoney.sdk.fragment.Debit;
import com.payUMoney.sdk.fragment.NetBankingFragment;
import com.payUMoney.sdk.fragment.StoredCardFragment;

import java.util.List;

//import com.payUMoney.sdk.fragment.CreditCardFragment;
//import com.payUMoney.sdk.fragment.DebitCardFragment;

/**
 * Created by franklin.michael on 25-06-2014.
 */
public class PaymentModeAdapter extends FragmentStatePagerAdapter {

    Context mContext;
    List<String> mPaymentModes;

    public PaymentModeAdapter(FragmentManager fragmentManager, Context ctx, List availableModes) {
        super(fragmentManager);
        mContext = ctx;
        mPaymentModes = availableModes;
    }

    @Override
    public Fragment getItem(int position) {

        if (mPaymentModes.get(position).equals("STORED_CARDS"))
            return new StoredCardFragment();
        else if (mPaymentModes.get(position).equals("CC"))
            return new Credit();
        else if (mPaymentModes.get(position).equals("DC"))
            return new Debit();
        else if (mPaymentModes.get(position).equals("NB"))
            return new NetBankingFragment();
        else return null;

    }

    @Override
    public int getCount() {
        return mPaymentModes.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {

        if (mPaymentModes.get(position).equals("STORED_CARDS"))
            return "Saved Cards";
        else if (mPaymentModes.get(position).equals("CC"))
            return "Credit Card";
        else if (mPaymentModes.get(position).equals("DC"))
            return "Debit Card";
        else if (mPaymentModes.get(position).equals("NB"))
            return "Net Banking";
        else return null;
    }

}
