package com.payUMoney.sdk.adapter;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.fragment.SdkDebit;
import com.payUMoney.sdk.fragment.SdkNetBankingFragment;
import com.payUMoney.sdk.fragment.SdkStoredCardFragment;

public class SdkExpandableListAdapter extends BaseExpandableListAdapter {

    private Context _context;
    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
    private HashMap<String, Fragment> _listDataChild = null;

    public SdkExpandableListAdapter(Context context, List<String> listDataHeader) {
        this._context = context;
        this._listDataHeader = listDataHeader;
    }

    @Override
    public Fragment getChild(int groupPosition, int childPosititon) {
        return null;

    }
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        LayoutInflater infalInflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


            if (_listDataHeader.get(groupPosition).equals(SdkConstants.PAYMENT_MODE_STORE_CARDS)) {

              //  if(convertView != null && convertView.getParent().equals(""))

                    return new SdkStoredCardFragment(_context).onCreateView(infalInflater, parent);

            } else if (_listDataHeader.get(groupPosition).equals(SdkConstants.PAYMENT_MODE_CC)) {

                   // getGroup
                    return new SdkDebit(_context).onCreateView(infalInflater, parent,SdkConstants.PAYMENT_MODE_CC);

            } else if (_listDataHeader.get(groupPosition).equals(SdkConstants.PAYMENT_MODE_DC)) {

                    return new SdkDebit(_context).onCreateView(infalInflater, parent,SdkConstants.PAYMENT_MODE_DC);

            } else if(_listDataHeader.get(groupPosition).equals(SdkConstants.PAYMENT_MODE_NB)) {

                    return new SdkNetBankingFragment(_context).onCreateView(infalInflater, parent);
            }

            return null;
            //return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;

    }

    @Override
    public Object getGroup(int groupPosition) {
        if(this._listDataHeader != null && this._listDataHeader.size() > groupPosition) {
            return this._listDataHeader.get(groupPosition);
        }
        return null;
    }

    @Override
    public int getGroupCount() {
        if(this._listDataHeader != null) {
            return this._listDataHeader.size();
        }
        return 0;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        Object object = getGroup(groupPosition);
        String headerTitle = "";
        if(object != null) {
            String titleGroup = object.toString();
            if(titleGroup != null) {
                switch (titleGroup) {
                    case SdkConstants.PAYMENT_MODE_STORE_CARDS:
                        headerTitle = "Saved Cards";
                        break;
                    case SdkConstants.PAYMENT_MODE_CC:
                        headerTitle = "Credit Card";
                        break;
                    case SdkConstants.PAYMENT_MODE_DC:
                        headerTitle = "Debit Card";
                        break;
                    case SdkConstants.PAYMENT_MODE_NB:
                        headerTitle = "Net Banking";
                        break;
                }
            }
        }

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.sdk_list_group, null);
        }

        if(headerTitle != null && !headerTitle.isEmpty()) {
            TextView lblListHeader = (TextView) convertView
                    .findViewById(R.id.lblListHeader);
            lblListHeader.setTypeface(null, Typeface.BOLD);
            lblListHeader.setText(headerTitle);
        }
        /*if(isExpanded) {

            convertView.setBackgroundColor(_context.getResources().getColor(R.color.sky_blue));
            lblListHeader.setTextColor(_context.getResources().getColor(R.color.active_text));
        }
        else
        {
            convertView.setBackgroundColor(_context.getResources().getColor(R.color.transparent));
            lblListHeader.setTextColor(_context.getResources().getColor(R.color.gray));
        }*/

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

}
