package com.payUMoney.sdk.entity;

/**
 * User is an Eashmart user as identified by his id, businessId or phone
 */
public class SdkUser extends SdkEntity {
    private String mName = null;
    private String mAvatar = null;
    // private Long businessId;
    private String mPhone = null;
    private String mEmail = null;

    private boolean mPasswordChanged = false;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String avatar) {
        mAvatar = avatar;
    }

    // public Long getBusinessId() {
    // return businessId;
    // }
    //
    // public void setBusinessId(Long businessId) {
    // this.businessId = businessId;
    // }

    public String getPhone() {
        return mPhone;
    }

    public void setPhone(String phone) {
        mPhone = phone;
    }

    @Override
    public String toString() {
        return "" + (mName == null ? (mPhone == null ? mEmail : mPhone) : mName);
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public boolean getPasswordChanged() {
        return mPasswordChanged;
    }

    public void setPasswordChanged(boolean passwordChanged) {
        mPasswordChanged = passwordChanged;
    }

    //	public Boolean getRequiresOrder() {
//		return mRequiresOrder;
//	}

//	public void setRequiresOrder(Boolean requiresOrder) {
//		mRequiresOrder = requiresOrder;
//	}
}
