package com.payUMoney.sdk;

/**
 * Created by amit on 25/10/13.
 */
public class SdkCobbocEvent {
    public static final int LOGIN = 1;
    public static final int LOGOUT = 2;
    public static final int CREATE_PAYMENT = 5;
    public static final int PAYMENT = 8;
    public static final int SIGN_UP = 16;
    public static final int FORGOT_PASSWORD = 19;
    public static final int ONE_TAP_OPTION_ALTERED = 27;
    public static final int USER_POINTS = 35;
    public static final int GENERATE_AND_SEND_OTP = 38;
    public static final int FETCH_MERCHANT_PARAMS = 39;
    public static final int FETCH_USER_PARAMS = 40;
    public static final int VERIFY_MANUAL_COUPON = 41;
    public static final int LOAD_WALLET = 42;
    public static final int USER_HISTORY = 43;
    public static final int PAYMENT_POINTS = 44;
    public static final int DEBIT_WALLET = 45;
    public static final int OPEN_SEND_OTP_VERIFICATION = 46;
    public static final int OPEN_REGISTER_USING_OTP_AND_SIGNIN = 48;
    public static final int USER_VAULT = 49;
    public static final int SEND_OTP_TO_USER = 50;
    public static final int CREATE_WALLET = 51;
    public static final int NET_BANKING_STATUS = 52;
    public static final int POST_BACK_PARAM = 53;


    private boolean STATUS;

    private int TYPE;

    private Object VALUE;

    public SdkCobbocEvent(int type) {
        this(type, true, null);
    }

    public SdkCobbocEvent(int type, boolean status) {
        this(type, status, null);
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
