package com.payUMoney.sdk;


import android.content.SharedPreferences;

import de.keyboardsurfer.android.widget.crouton.Configuration;


/**
 * Constants
 */
@SuppressWarnings("unused")
public class Constants {
    /**
     * Tag for logging
     */
    public static final Configuration CONFIGURATION_INFINITE = new Configuration.Builder().setDuration(Configuration.DURATION_INFINITE).build();

    public static final Configuration CONFIGURATION_LONG = new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build();

    public static final String TAG = "Sagar";
    /**
     * The GCM project ID of Eashmart
     */
    public static final String GCM_SENDER_ID = "716007125784";

    public static final String CRITTERCISM_ID = "53ba89d41787840d8e000003";

    public static final Boolean DEBUG = false;

    /**
     * Name of the {@link SharedPreferences} that are to be used
     */
    public static final String SP_SP_NAME = "PayUMoney";
    //	public static final String GCM_ID = "gcm_id";
    public static final String TOKEN = "token";

    public static final String greenPayU = "#6ac451";

    public static final String RESULT = "result";
    public static final String MESSAGE = "msg";
    public static final String AMEX = "AMEX";
    public static final String CONTENT = "content";
    public static final String PAY_ID = "paymentID";
    public static final String STATUS = "status";
    public static final String PAYMENT_ID = "paymentId";
    public static final String DATA = "data";
    public static final String MID = "mid";
    public static final String NAME = "name";
    public static final String DISPLAY_NAME = "displayName";

    public static final String PHONE = "phone";
    public static final String EMAIL = "email";
    public static final String LOGO = "logo";
    public static final String PASSWORD = "password";
    public static final String AMOUNT = "amount";
    public static final String DESCRIPTION = "description";
    public static final String ADDRESS = "businessOperationAddress";
    public static final String ADDRESS_LINE = "addressLine";
    public static final String CITY = "city";
    public static final String STATE = "state";
    public static final String ZIPCODE = "zipcode";
    public static final String COUNTRY = "country";
    public static final String MERCHANTS = "merchants";
    public static final String RECENT = "recent";
    public static final String FAVORITE = "favourite";
    public static final String IS_FAVORITE = "isFavourite";
    public static final String COOKIE_VALUE = "cookie";
    public static final String PAYMENT_OPTION = "paymentOption";
    public static final String TRANSACTION_DTO = "transactionDto";
    public static final String PAYMENT = "payment";
    public static final String NUMBER = "ccnum";
    public static final String LABEL = "card_name";
    public static final String CVV = "ccvv";
    public static final String EXPIRY_MONTH = "ccexpmon";
    public static final String EXPIRY_YEAR = "ccexpyr";
    public static final String MODE = "mode";
    public static final String STORE = "store_card";
    private static final String BASE_URL_LIVE = "https://www.payumoney.com";

    //URL FOR TEST ENVIRONMENT
  private static final String BASE_URL_DEBUG = "https://mobiletest.payumoney.com";
   private static final String BASE_URL_IMAGE_DEBUG = "https://mobiletestfile.payumoney.com";


//URL FOR PP4 ENVIRONMENT
 //  private static final String BASE_URL_DEBUG = "http://pp4.payumoney.com";
 // private static final String BASE_URL_IMAGE_DEBUG = "http://ppfile4.payumoney.com";

    private static final String BASE_URL_IMAGE_LIVE = "https://file.payumoney.com";

    /**
     * The URL of the server
     */
    public static final String BASE_URL = (DEBUG ? BASE_URL_DEBUG : BASE_URL_LIVE);
    public static final String BASE_URL_IMAGE = (DEBUG ? BASE_URL_IMAGE_DEBUG : BASE_URL_IMAGE_LIVE);

    private static final String BASE_URL_AMIT = "http://10.100.66.141:8080";
    public static final String PROFILE_PICTURE = "profilePicture";
    public static final String FILE_PATH = "filePath";
    public static final String TXNID = "txnid";
    public static final String MERCHANTID = "mid";
    public static final String SURL = "surl";
    public static final String FURL = "furl";
    public static final String PRODUCT_INFO = "productinfo";
    public static final String FIRSTNAME = "firstname";
    public static final String USER_EMAIL = "email";
    public static final String USER_PHONE = "phone";

    public static final String CVV_TITLE = "CVV Required";
    public static final String CVV_MESSAGE = "Enter your CVV to make payment.";
    public static final String CVV_ERROR_MESSAGE = "Please enter valid CVV number";

    public static final String HASH = "hash";
    public static final String KEY = "key";
    public static final String PARAMS = "params";
    public static final String LASTUSEDBANK = "lastUsedBank";

}
