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


            if (_listDataHeader.get(groupPosition).equals("STORED_CARDS")) {

              //  if(convertView != null && convertView.getParent().equals(""))

                    return new SdkStoredCardFragment(_context).onCreateView(infalInflater, parent);

            } else if (_listDataHeader.get(groupPosition).equals("CC")) {

                   // getGroup
                    return new SdkDebit(_context).onCreateView(infalInflater, parent,"CC");

            } else if (_listDataHeader.get(groupPosition).equals("DC")) {

                    return new SdkDebit(_context).onCreateView(infalInflater, parent,"DC");

            } else if(_listDataHeader.get(groupPosition).equals("NB")) {

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
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String titleGroup = getGroup(groupPosition).toString();
        String headerTitle = "No group";
        switch (titleGroup) {
            case "STORED_CARDS":
                headerTitle = "Saved Cards";
                break;
            case "CC":
                headerTitle = "Credit Card";
                break;
            case "DC":
                headerTitle = "Debit Card";
                break;
            case "NB":
                headerTitle = "Net Banking";
                break;
        }
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.sdk_list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);
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
