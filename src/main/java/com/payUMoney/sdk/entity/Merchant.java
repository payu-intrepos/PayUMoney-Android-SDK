package com.payUMoney.sdk.entity;

import org.json.JSONObject;

/**
 * Created by amit on 6/8/14.
 */
public class Merchant extends Entity {
    private String mName = null;
    private String mLogo = null;
    private String mEmail = null;
    private String mMid = null;
    private String mPhone = null;
    private JSONObject mRegisteredAddress;
    private JSONObject mOperationAddress;
    private int mFavorite;
    private int mRecent;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getLogo() {
        return mLogo;
    }

    public void setLogo(String logo) {
        mLogo = logo;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getMid() {
        return mMid;
    }

    public void setMid(String mid) {
        mMid = mid;
    }

    public String getPhone() {
        return mPhone;
    }

    public void setPhone(String phone) {
        mPhone = phone;
    }

    public JSONObject getRegisteredAddress() {
        return mRegisteredAddress == null ? new JSONObject() : mRegisteredAddress;
    }

    public void setRegisteredAddress(JSONObject registeredAddress) {
        mRegisteredAddress = registeredAddress;
    }

    public JSONObject getOperationAddress() {
        return mOperationAddress == null ? new JSONObject() : mOperationAddress;
    }

    public void setOperationAddress(JSONObject operationAddress) {
        mOperationAddress = operationAddress;
    }

    public boolean isFavorite() {
        return mFavorite == 1;
    }

    public void setFavorite(int favorite) {
        mFavorite = favorite;
    }

    public void setFavorite(boolean favorite) {
        mFavorite = favorite ? 1 : 0;
    }

    public boolean isRecent() {
        return mRecent == 1;
    }

    public void setRecent(boolean recent) {
        mRecent = recent ? 1 : 0;
    }

//	@Override
//	public String toString() {
//		return "Invoice [currency=" + mCurrency + ", service=" + service + ", photo=" + photo + ", name=" + name + ", user=" + user + ", seller_id=" + seller_id + ", buyer_id=" + buyer_id + ", paid=" + paid + ", created_at=" + created_at + ", amount=" + amount + ", pin=" + pin + "]";
//	}
}
