package com.payUMoney.sdk;

/**
 * Created by amit on 25/10/13.
 */
public class SdkCobbocEvent {
    public static final int LOGIN = 1;
    public static final int LOGOUT = 2;
    public static final int CARD_DELETED = 3;
    public static final int CREATE_CARD = 4;
    public static final int CREATE_PAYMENT = 5;
    public static final int DATABASE_UPDATED = 6;
    public static final int INVOICE_DELETED = 7;
    public static final int PAYMENT = 8;
    public static final int MERCHANTS = 9;
    public static final int FEEDBACK = 10;
    public static final int TRANSACION_DETAILS = 11;
    public static final int MERCHANT_DETAILS = 12;
    //	public static final int UNKNOWN_ERROR = 13;
    public static final int ACCOUNT = 14;
    public static final int TWO_STEP_PAGE = 15;
    public static final int SIGN_UP = 16;
    public static final int PAYMENT_DETAILS = 17;
    public static final int TOGGLE_FAVORITE = 18;
    public static final int FORGOT_PASSWORD = 19;
    public static final int MERCHANT_SEARCH = 20;
    public static final int CARDS = 21;
    public static final int GET_CARD_HASH = 22;
    public static final int TRANS_HISTORY = 23;
    public static final int CONTACT_INFO = 24;
    public static final int ADDRESS_INFO = 25;
    public static final int RESET_PASSWORD = 26;
    public static final int CONTACT_INFO_UPDATE = 27;
    public static final int GENERATE_OTP = 28;
    public static final int VERIFY_OTP = 29;
    public static final int ADDRESS_INFO_UPDATE = 30;
    public static final int UPLOAD_IMAGE = 31;
    public static final int GENERATE_EMAIL_CODE = 32;
    public static final int UPLOAD_IMAGE_PROGRESS = 33;
    public static final int APP_VERSION_CHECK = 34;
    public static final int USER_POINTS = 35;
    public static final int WALLET_MONEY = 36;
    public static final int PAYMENT_POINTS = 36;
    public static final int GUEST_CHECKOUT_CREATE_PAYMENT = 37;
    public static final int GENERATE_AND_SEND_OTP = 38;
    public static final int FETCH_MERCHANT_PARAMS = 39;
    public static final int FETCH_USER_PARAMS = 40;


    private boolean STATUS;

    private int TYPE;

    private Object VALUE;

    public SdkCobbocEvent(int type) {
        this(type, true, null);
    }

    public SdkCobbocEvent(int type, boolean status) {
        this(type, false, null);
    }

    public SdkCobbocEvent(int type, boolean status, Object value) {
        TYPE = type;
        STATUS = status;
        VALUE = value;
    }

    public SdkCobbocEvent(int type, boolean status, int value) {
        TYPE = type;
        STATUS = status;
        VALUE = Integer.valueOf(value);
    }

//	public void setStatus(boolean status) {
//		STATUS = status;
//	}

    public boolean getStatus() {
        return STATUS;
    }

//	public void setType(int type) {
//		TYPE = type;
//	}

    public int getType() {
        return TYPE;
    }

//	public void setValue(Object value) {
//		VALUE = value;
//	}

    public Object getValue() {
        return VALUE;
    }
}
