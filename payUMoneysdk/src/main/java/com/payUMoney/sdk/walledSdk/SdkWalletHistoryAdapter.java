package com.payUMoney.sdk.walledSdk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkConstants;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

public class SdkWalletHistoryAdapter extends BaseAdapter {
    private Context context;
    private List<SdkWalletHistoryBean> transactionList;
    private LayoutInflater inflater;

    public SdkWalletHistoryAdapter(Context context, List<SdkWalletHistoryBean> transactionList) {
        super();
        this.context = context;
        this.transactionList = transactionList;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return transactionList.size();
    }

    @Override
    public Object getItem(int location) {
        return transactionList.get(location);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (inflater == null)
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.wallet_history, null);
            viewHolder = new ViewHolder();
            viewHolder.amount = (TextView) convertView.findViewById(R.id.amount);
            viewHolder.paymentId = (TextView) convertView.findViewById(R.id.payment_id);
            viewHolder.vaultAction = (TextView) convertView.findViewById(R.id.vault_action);
            viewHolder.transactionDate = (TextView) convertView.findViewById(R.id.transaction_date);
            viewHolder.description = (TextView) convertView.findViewById(R.id.description);
            viewHolder.statusTextMessage = (TextView) convertView.findViewById(R.id.status_text_message);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        SdkWalletHistoryBean walletHistoryBean = transactionList.get(position);

        String amountInString = "0";
        try {
            double amountInDouble = Double.parseDouble(walletHistoryBean.getAmount());
            BigDecimal amountRoundedTwoDigits = round(amountInDouble, 2);
            amountInString = String.valueOf(amountRoundedTwoDigits.doubleValue());
        } catch (NumberFormatException exception) {
            amountInString = "0";
        } catch (Exception e) {
            amountInString = "0";
        }

        viewHolder.amount.setText(context.getString(R.string.rs) + amountInString);
        if (walletHistoryBean.getPaymentId().trim().equals("") || walletHistoryBean.getPaymentId().trim().equalsIgnoreCase("null")) {
            viewHolder.paymentId.setVisibility(View.GONE);
        } else {
            viewHolder.paymentId.setVisibility(View.VISIBLE);
            viewHolder.paymentId.setText(context.getString(R.string.transaction_history_payment_id, walletHistoryBean.getPaymentId()));
        }

        SimpleDateFormat ft = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
        viewHolder.transactionDate.setText(ft.format(walletHistoryBean.getTransactionDate()));

        if (walletHistoryBean.getVaultAction().trim().isEmpty()
                || walletHistoryBean.getVaultAction().trim().equalsIgnoreCase(SdkConstants.NULL_STRING)) {
            viewHolder.description.setVisibility(View.GONE);
        } else {
            viewHolder.description.setText(walletHistoryBean.getVaultAction());
            viewHolder.description.setVisibility(View.VISIBLE);
        }

        if (walletHistoryBean.getTransactionStatus().equalsIgnoreCase(context.getString(R.string.transaction_status_success))) {
            viewHolder.statusTextMessage.setText(context.getResources().getString(R.string.transaction_status_success).toUpperCase());
        } else if (walletHistoryBean.getTransactionStatus().equalsIgnoreCase(context.getString(R.string.transaction_status_failed))) {
            viewHolder.statusTextMessage.setText(context.getString(R.string.transaction_status_failed).toUpperCase());
        } else if (walletHistoryBean.getTransactionStatus().equalsIgnoreCase(context.getString(R.string.transaction_status_in_progress))) {
            viewHolder.statusTextMessage.setText(R.string.history_status_msg_pending);
        } else if (walletHistoryBean.getTransactionStatus().equalsIgnoreCase(context.getString(R.string.transaction_status_pending))) {
            viewHolder.statusTextMessage.setText(R.string.history_status_msg_pending);
        } else if (walletHistoryBean.getTransactionStatus().equalsIgnoreCase(context.getString(R.string.transaction_status_refund_initiated))) {
            viewHolder.statusTextMessage.setText(R.string.history_status_msg_refund_initiated);
        } else if (walletHistoryBean.getTransactionStatus().equalsIgnoreCase(context.getString(R.string.transaction_status_refund_success))) {
            viewHolder.statusTextMessage.setText(R.string.history_status_msg_refund_success);
        }

        if(!walletHistoryBean.getVaultActionMessage().trim().equals("")){
            viewHolder.vaultAction.setText(walletHistoryBean.getVaultActionMessage().trim());
            viewHolder.vaultAction.setVisibility(View.VISIBLE);
        }
        else
            viewHolder.vaultAction.setVisibility(View.GONE);

        return convertView;
    }

    public BigDecimal round(double d, int decimalPlace) {

        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    private static class ViewHolder {
        TextView amount;
        TextView description;
        TextView paymentId;
        TextView transactionDate;
        TextView statusTextMessage;
        TextView vaultAction;
        /*ProgressBar progressBar;*/
    }

}