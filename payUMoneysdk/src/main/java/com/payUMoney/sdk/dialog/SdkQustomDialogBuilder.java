package com.payUMoney.sdk.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.payUMoney.sdk.R;


public class SdkQustomDialogBuilder extends AlertDialog.Builder {

    /**
     * The custom_body layout
     */
    private View mDialogView;

    /**
     * optional com.payUMoney.sdk.dialog title layout
     */
    private TextView mTitle;
    /**
     * optional alert com.payUMoney.sdk.dialog image
     */
   // private ImageView mIcon;
    /**
     * optional message displayed below title if title exists
     */
    private TextView mMessage;
    /**
     * The colored holo divider. You can set its color with the setDividerColor method
     */
    private View mDivider;

    public SdkQustomDialogBuilder(Context context) {
        super(context);

        mDialogView = View.inflate(context, R.layout.sdk_qustom_dialog_layout, null);
        setView(mDialogView);

        mTitle = (TextView) mDialogView.findViewById(R.id.alertTitle);
        mMessage = (TextView) mDialogView.findViewById(R.id.message);
      //  mIcon = (ImageView) mDialogView.findViewById(R.id.icon);
        mDivider = mDialogView.findViewById(R.id.titleDivider);
    }

    public SdkQustomDialogBuilder(Context context, int resource) {
        super(context, resource);

        mDialogView = View.inflate(context, R.layout.sdk_qustom_dialog_layout, null);
        setView(mDialogView);
        mTitle = (TextView) mDialogView.findViewById(R.id.alertTitle);
        mMessage = (TextView) mDialogView.findViewById(R.id.message);
       // mIcon = (ImageView) mDialogView.findViewById(R.id.icon);
        mDivider = mDialogView.findViewById(R.id.titleDivider);
    }

    /**
     * Use this method to color the divider between the title and content.
     * Will not display if no title is set.
     *
     * @param colorString for passing "#ffffff"
     */
    public SdkQustomDialogBuilder setDividerColor(String colorString) {
        mDivider.setBackgroundColor(Color.parseColor(colorString));
        return this;
    }

    @Override
    public SdkQustomDialogBuilder setTitle(CharSequence text) {
        mTitle.setText(text);
        return this;
    }

    public SdkQustomDialogBuilder setTitleColor(String colorString) {
        mTitle.setTextColor(Color.parseColor(colorString));
        return this;
    }

    @Override
    public SdkQustomDialogBuilder setMessage(int textResId) {
        mMessage.setText(textResId);
        return this;
    }

    @Override
    public SdkQustomDialogBuilder setMessage(CharSequence text) {
        mMessage.setText(text);
        return this;
    }



    /**
     * This allows you to specify a custom layout for the area below the title divider bar
     * in the com.payUMoney.sdk.dialog. As an example you can look at example_ip_address_layout.xml and how
     * I added it in TestDialogActivity.java
     *
     * @param resId   of the layout you would like to add
     * @param context
     */
    public SdkQustomDialogBuilder setCustomView(int resId, Context context) {
        View customView = View.inflate(context, resId, null);
        ((LinearLayout) mDialogView.findViewById(R.id.topPanel)).addView(customView);
        return this;
    }

    @Override
    public AlertDialog show() {
        if (mTitle.getText().equals(""))
            mDialogView.findViewById(R.id.topPanel).setVisibility(View.GONE);

        return super.show();

    }

}
