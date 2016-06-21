package com.payUMoney.sdk.walledSdk;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.payUMoney.sdk.R;
import com.payUMoney.sdk.SdkCobbocEvent;
import com.payUMoney.sdk.SdkConstants;
import com.payUMoney.sdk.SdkSession;
import com.payUMoney.sdk.utils.SdkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.greenrobot.event.EventBus;


public class SdkHistoryActivity extends FragmentActivity {

    private boolean getMoreHistory;
    public static int save_count;
    public static int offset_count;
    public static int lastItem;
    public static int type;
    private SdkWalletHistoryAdapter walletHistoryAdapter;
    private List<SdkWalletHistoryBean> walletHistoryBeanList;
    private TextView footerMessageTextView;
    private boolean resetListAndMetaData;
    private TextView centerMessage;
    private boolean toShowAlert;

    private final int HISTORY_CANCELLED = 21;
    private final int HISTORY_LOGOUT = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sdk_history_activity);

        /*userParams = (HashMap<String, String>) getIntent().getSerializableExtra(SdkConstants.PARAMS);*/

        getMoreHistory = true;
        resetListAndMetaData = false;
        toShowAlert = false;
        centerMessage = (TextView) findViewById(R.id.central_message_text_view);

        findViewById(R.id.no_trans).setVisibility(View.GONE);
        findViewById(R.id.load_more).setVisibility(View.GONE);

        ListView mListView = (ListView) findViewById(R.id.trans_list);

        walletHistoryBeanList = new ArrayList<>();
        walletHistoryAdapter = new SdkWalletHistoryAdapter(this, walletHistoryBeanList);
        mListView.setAdapter(walletHistoryAdapter);

        save_count = 0;
        offset_count = 0;
        getMoreHistory = true;
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView view,
                                 int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
                lastItem = firstVisibleItem + visibleItemCount;
                if (getMoreHistory && lastItem == totalItemCount && lastItem > save_count) {
                    System.out.println("scroll listerner inside");

                    findViewById(R.id.load_more).setVisibility(View.VISIBLE);

                    save_count = lastItem;
                    offset_count++;
                    checkNetworkAndCallServer(offset_count);
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        });
        LayoutInflater inflaterFooter = getLayoutInflater();
        ViewGroup footer = (ViewGroup) inflaterFooter.inflate(
                R.layout.walletsdk_listview_footer, mListView, false);
        mListView.addFooterView(footer);
        footerMessageTextView = (TextView) footer.findViewById(R.id.footer_message);

        checkNetworkAndCallServer(0);
    }

    private void checkNetworkAndCallServer(int offset_count) {

        if (!SdkHelper.checkNetwork(this)) {
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            findViewById(R.id.loading_trans).setVisibility(View.GONE);
            findViewById(R.id.load_more).setVisibility(View.GONE);
            SdkHelper.showToastMessage(this, getString(R.string.disconnected_from_internet), true);
            if (walletHistoryBeanList.size() == 0)
                centerMessage.setVisibility(View.VISIBLE);
            toShowAlert = true;
            getMoreHistory = false;
        } else {
            SdkSession.getInstance(this).getTransactionHistory(offset_count);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
       // SdkSession.getInstance(this).cancelRequests(this);
    }

    @Override
    public void onBackPressed() {
        close(HISTORY_CANCELLED);
    }

    public void close(int resultCode) {

        if (resultCode == HISTORY_CANCELLED) {
            setResult(RESULT_OK, null);
        } else if (resultCode == HISTORY_LOGOUT) {
            setResult(RESULT_CANCELED, null);
        }
        finish();
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, R.id.logout, menu.size(), R.string.logout).setIcon(R.drawable.logout).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {

            if (!SdkHelper.checkNetwork(this)) {
                SdkHelper.showToastMessage(this, getString(R.string.disconnected_from_internet), true);

            } else {
                SdkHelper.showProgressDialog(this, "Logging Out");
                SdkSession.getInstance(this).logout();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private List<SdkWalletHistoryBean> getWalletHistoryBeans(JSONArray historyArray) {

        List<SdkWalletHistoryBean> historyBeans = new ArrayList<>();

        try {
            for (int historyArrayIterator = 0; historyArrayIterator < historyArray.length(); historyArrayIterator++) {
                SdkWalletHistoryBean parseHistory = new SdkWalletHistoryBean();

                JSONObject jsonCardObject = historyArray.getJSONObject(historyArrayIterator);

                parseHistory.setMode(jsonCardObject.optString(SdkConstants.MODE));
                parseHistory.setTransactionDate(jsonCardObject.optLong(SdkConstants.TRANSACTION_DATE));
                parseHistory.setPaymentId(jsonCardObject.optString(SdkConstants.PAYMENT_ID));
                parseHistory.setMerchantId(jsonCardObject.optString(SdkConstants.MERCHANT_ID));
                parseHistory.setMerchantTransactionId(jsonCardObject.optString(SdkConstants.MERCHANT_TXNID));
                parseHistory.setMerchantName(jsonCardObject.optString(SdkConstants.MERCHANT_NAME));
                parseHistory.setAmount(jsonCardObject.optString(SdkConstants.AMOUNT));
                parseHistory.setRefundToSource(jsonCardObject.optString(SdkConstants.REFUND_TO_SOURCE));
                parseHistory.setExternalRefId(jsonCardObject.optString(SdkConstants.EXTERNAL_REF_ID));
                parseHistory.setVaultAction(jsonCardObject.optString(SdkConstants.VAULT_ACTION));
                parseHistory.setVaultTransactionId(jsonCardObject.optString(SdkConstants.VAULT_TRANSACTION_ID));
                parseHistory.setPaymentType(jsonCardObject.optString(SdkConstants.PAYMENT_TYPE));
                parseHistory.setVaultActionMessage(getVaultActionDescription(jsonCardObject.optString(SdkConstants.VAULT_ACTION)));
                parseHistory.setDescription(getVaultActionDescription(jsonCardObject.optString(SdkConstants.DESCRIPTION_STRING)));
                parseHistory.setTransactionStatus(getVisibleStatus(jsonCardObject.optString(SdkConstants.STATUS)));

                historyBeans.add(parseHistory);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return historyBeans;
    }

    private String getVisibleStatus(String realStatus) {
        String status = null;

        switch (realStatus.toLowerCase()) {

            case "success":
            case "successinternal":
                status = getString(R.string.transaction_status_success);//"Money with PayUMoney";
                break;

            case "refunded":
                status = getString(R.string.transaction_status_refund_success);//"Money with PayUMoney";
                break;

            case "pendingfornextmonthprocessing":
            case "pendingforregistration":
            case "pending":
                status = getString(R.string.transaction_status_pending);
                break;

            case "failure":
            case "cancelled":
                status = getString(R.string.transaction_status_failed);//"Failed";
                break;

            default:
                status = getString(R.string.transaction_status_failed);//"Failed";
                break;

        }
        return status;
    }

    public void onEventMainThread(final SdkCobbocEvent event) {
        if (event.getType() == SdkCobbocEvent.USER_HISTORY) {

            if (event.getStatus()) {
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                findViewById(R.id.loading_trans).setVisibility(View.GONE);
                findViewById(R.id.load_more).setVisibility(View.GONE);

                JSONObject myTrans = (JSONObject) event.getValue();//TODO add null check
                JSONArray myPayments;
                /*if (event.getStatus() == true) {
                    walletHistoryBeanList.clear();
                }*/
                try {
                    myPayments = myTrans.getJSONArray(SdkConstants.CONTENT_STRING);
                    if (myPayments.length() > 0) {
                        findViewById(R.id.no_trans).setVisibility(View.GONE);
                        if (resetListAndMetaData) {
                            walletHistoryBeanList.clear();
                            save_count = 0;
                            offset_count = 0;
                            getMoreHistory = true;
                        }

                        List<SdkWalletHistoryBean> walletBeans = getWalletHistoryBeans(myPayments);
                        walletHistoryBeanList.addAll(walletBeans);

                        walletHistoryAdapter.notifyDataSetChanged();
                    } else {
                        walletHistoryAdapter.notifyDataSetChanged();
                        if (walletHistoryBeanList.size() == 0) {
                            findViewById(R.id.no_trans).setVisibility(View.VISIBLE);
                            getMoreHistory = false;
                        } else {
                            footerMessageTextView.setVisibility(View.VISIBLE);
                            getMoreHistory = false;
                        }
                    }
                } catch (JSONException ex) {
                    if (walletHistoryBeanList.size() == 0)
                        centerMessage.setVisibility(View.VISIBLE);
                    SdkHelper.showToastMessage(this, getString(R.string.something_went_wrong), true);
                }
            } else {
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                findViewById(R.id.loading_trans).setVisibility(View.GONE);
                findViewById(R.id.load_more).setVisibility(View.GONE);
                SdkHelper.showToastMessage(this, getString(R.string.something_went_wrong), true);
                getMoreHistory = false;
                if (walletHistoryBeanList.size() == 0)
                    centerMessage.setVisibility(View.VISIBLE);
            }
            resetListAndMetaData = false;
            toShowAlert = true;

        } else if (event.getType() == SdkCobbocEvent.LOGOUT) {

            SdkHelper.dismissProgressDialog();
            if (event.getStatus()) {
                SdkHelper.showToastMessage(this, getString(R.string.logout_success), false);
                close(HISTORY_LOGOUT);
            } else {

                if (!SdkHelper.checkNetwork(this)) {
                    SdkHelper.showToastMessage(this, getString(R.string.disconnected_from_internet), true);
                } else {
                    SdkHelper.showToastMessage(this, getString(R.string.something_went_wrong), true);
                }

            }
        }
    }

    private String getVaultActionDescription(String vaultAction) {
        String msg = null;

        switch (vaultAction) {
            case "usewallet":
                msg = "Used For A Payment"; // when user used wallet amount for transacting
                break;

            case "cancelwallet":
                msg = "Return For Failed Payment";
                break;
            case "topupwallet":
                msg = "Loaded To Wallet";
                break;
            case "refundtopupwallet":
                msg = "Added Against Refund";
                break;
            case "refundwallet":
                msg = "Refunded Back To Wallet";
                break;
            case "topupreversewallet":
                msg = "Load Amount Refunded";
                break;
            case "refundtopupreversewallet":
                msg = "Refunded To Card";
                break;
            case "cashoutwallet":
                msg = "Transferred To Bank";
                break;
            case "nonpaymenttopupwallet":
                msg = "Wallet Load - Other Sources";
                break;
            case "reversetopupwallet":
                msg = "Load Wallet Reversal";
                break;
            case "nonpaymentrevokewallet":
                msg = "Promotional Reversal";
                break;
            case "cashbackwallet":
                msg = "Cashback Into Wallet";
                break;
            case "cashbackreversewallet":
                msg = "Cashback Reversal From Wallet";
                break;
            case "cashoutreversewallet":
                msg = "Transfer Reversal";
                break;
            case "wallettransfercredit":
                msg = "Wallet Transfer Received";
                break;

            case "wallettransferdebit":
                msg = "Wallet Transfer Sent";
                break;


            case "refundwallettransferdebit":
                msg = "Wallet Transfer Reversal";
                break;

            default:
                msg = vaultAction;
        }
        return msg;
    }

}
