package com.payUMoney.sdk.walledSdk;

import java.util.Date;

public class SdkWalletHistoryBean {

    private String mMode;
    private Date mTransactionDate;
    private String mPaymentId;
    private String mMerchantId;
    private String mMerchantTransactionId;
    private String mMerchantName;
    private String mAmount;
    private String mRefundToSource;
    private String mExternalRefId;
    private String mVaultAction;
    private String mVaultTransactionId;
    private String mPaymentType;
    private String mTransactionStatus;
    private String mVaultActionMessage;
    private String mDescription;

    public String getVaultActionMessage() {
        return mVaultActionMessage;
    }

    public void setVaultActionMessage(String transactionStatus) {
        mVaultActionMessage = transactionStatus;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String transactionStatus) {
        mDescription = transactionStatus;
    }

    public String getTransactionStatus() {
        return mTransactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        mTransactionStatus = transactionStatus;
    }

    public String getPaymentType() {
        return mPaymentType;
    }

    public void setPaymentType(String name) {
        mPaymentType = name;
    }

    public String getExternalRefId() {
        return mExternalRefId;
    }

    public void setExternalRefId(String image) {
        mExternalRefId = image;
    }

    public String getVaultAction() {
        return mVaultAction;
    }

    public void setVaultAction(String type) {
        mVaultAction = type;
    }

    public String getVaultTransactionId() {
        return mVaultTransactionId;
    }

    public void setVaultTransactionId(String rate) {
        mVaultTransactionId = rate;
    }

    public String getRefundToSource() {
        return mRefundToSource;
    }

    public void setRefundToSource(String name) {
        mRefundToSource = name;
    }

    public String getAmount() {
        return mAmount;
    }

    public void setAmount(String image) {
        mAmount = image;
    }

    public String getMerchantName() {
        return mMerchantName;
    }

    public void setMerchantName(String type) {
        mMerchantName = type;
    }

    public String getMerchantTransactionId() {
        return mMerchantTransactionId;
    }

    public void setMerchantTransactionId(String rate) {
        mMerchantTransactionId = rate;
    }

    public String getMerchantId() {
        return mMerchantId;
    }

    public void setMerchantId(String name) {
        mMerchantId = name;
    }

    public String getPaymentId() {
        return mPaymentId;
    }

    public void setPaymentId(String image) {
        mPaymentId = image;
    }

    public Date getTransactionDate() {
        return mTransactionDate;
    }

    public void setTransactionDate(long type) {
        mTransactionDate = new Date(type);
    }

    public String getMode() {
        return mMode;
    }

    public void setMode(String rate) {
        mMode = rate;
    }
}
