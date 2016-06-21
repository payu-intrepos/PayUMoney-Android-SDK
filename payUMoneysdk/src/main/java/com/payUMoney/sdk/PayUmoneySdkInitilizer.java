package com.payUMoney.sdk;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by amit singh on 2/11/16.
 */
public class PayUmoneySdkInitilizer {

    public static final int PAYU_SDK_PAYMENT_REQUEST_CODE = 1001;
    public static final int RESULT_FAILED = 90;
    public static final int RESULT_BACK = 8;
    private static Boolean isDebugMode = false;

    private static final String BASE_URL_DEBUG = "https://test.payumoney.com";
    private static final String BASE_URL_IMAGE_DEBUG = "https://testfile.payumoney.com";
    private static final String WEBVIEW_REDIRECTION_URL_DEBUG = "https://test.payu.in/_seamless_payment";

    private static final String BASE_URL_LIVE = "https://www.payumoney.com";
    private static final String BASE_URL_IMAGE_LIVE = "https://file.payumoney.com";
    private static final String WEBVIEW_REDIRECTION_URL_LIVE = "https://secure.payu.in/_seamless_payment";

    private static String BASE_URL = "https://www.payumoney.com";
    private static String BASE_URL_IMAGE = "https://file.payumoney.com";
    private static String WEBVIEW_REDIRECTION_URL = "https://secure.payu.in/_seamless_payment";

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static void setBaseUrl(String url) {
        BASE_URL = url;
    }

    public static String getBaseUrlImage() {
        return BASE_URL_IMAGE;
    }

    public static void setBaseUrlImage(String url) {
        BASE_URL_IMAGE = url;
    }

    public static String getWebviewRedirectionUrl() {
        return WEBVIEW_REDIRECTION_URL;
    }

    public static void setWebviewRedirectionUrl(String url) {
        WEBVIEW_REDIRECTION_URL = url;
    }

    public static Boolean IsDebugMode() {
        return isDebugMode;
    }

    public static void setDebugMode(boolean isDebugMode){
        PayUmoneySdkInitilizer.isDebugMode = isDebugMode;
        setBaseUrl(isDebugMode ? BASE_URL_DEBUG : BASE_URL_LIVE);
        setBaseUrlImage(isDebugMode ? BASE_URL_IMAGE_DEBUG : BASE_URL_IMAGE_LIVE);
        setWebviewRedirectionUrl(isDebugMode ? WEBVIEW_REDIRECTION_URL_DEBUG : WEBVIEW_REDIRECTION_URL_LIVE);
    }

    public static void startPaymentActivityForResult(Activity activity, PaymentParam paymentParam){
        SdkSession.startPaymentProcess(activity, paymentParam.getParams());
    }

    /**
     * Created by amit singh on 2/10/16.
     */
    public static class PaymentParam {

        private HashMap<String, String> params = new LinkedHashMap<>();

        private String pipedHash = "";

        public HashMap<String, String> getParams() {
            return params;
        }

        private PaymentParam(Builder builder){

            setDebugMode(builder.isDebug);

            if (TextUtils.isEmpty(builder.key))
                throw new RuntimeException("Merchant Key missing");
            else
                params.put(SdkConstants.KEY,builder.key);

            if (TextUtils.isEmpty(builder.merchantId))
                throw new RuntimeException(" Merchant id missing ,setDebugMerchantId() ");
            else
                params.put(SdkConstants.MERCHANT_ID,builder.merchantId);

            if(TextUtils.isEmpty(builder.tnxId)){
                throw new RuntimeException("TxnId Id missing");
            }else{
                params.put(SdkConstants.TXNID,builder.tnxId);
            }

            if (builder.amount < 0 && builder.amount > 1000000.00 )
                throw new RuntimeException("Amount should be greater 0 and  less than 1000000.00  ");
            else
                params.put(SdkConstants.AMOUNT, builder.amount+"");

            if (TextUtils.isEmpty(builder.sUrl))
                throw new RuntimeException("Surl is missing");
            else
                params.put(SdkConstants.SURL, builder.sUrl );

            if (TextUtils.isEmpty(builder.fUrl))
                throw new RuntimeException("fUrl is missing");
            else
                params.put(SdkConstants.FURL, builder.fUrl );

            if (TextUtils.isEmpty(builder.productName))
                throw new RuntimeException("Product info is missing");
            else
                params.put(SdkConstants.PRODUCT_INFO, builder.productName);


            if (TextUtils.isEmpty(builder.email))
                throw new RuntimeException("email is missing");
            else
                params.put(SdkConstants.EMAIL, builder.email);


            if (TextUtils.isEmpty(builder.firstName))
                throw new RuntimeException("first name is missing");
            else
                params.put(SdkConstants.FIRSTNAME, builder.firstName);


            if (TextUtils.isEmpty(builder.phone))
                throw new RuntimeException("phone is missing");
            else
                params.put(SdkConstants.PHONE, builder.phone);

            if (builder.udf1 == null)
                throw new RuntimeException("udf1 is null, put some value or empty e.g. Builder.setUdf1(\"\")");
            else
                params.put(SdkConstants.UDF1, builder.udf1);

            if (builder.udf2 == null)
                throw new RuntimeException("udf2 is null, put some value or empty e.g. Builder.setUdf2(\"\")");
            else
                params.put(SdkConstants.UDF2, builder.udf2);

            if (builder.udf3 == null)
                throw new RuntimeException("udf3 is null, put some value or empty e.g. Builder.setUdf3(\"\")");
            else
                params.put(SdkConstants.UDF3, builder.udf3);

            if (builder.udf4 == null)
                throw new RuntimeException("udf4 is null, put some value or empty e.g. Builder.setUdf4(\"\")");
            else
                params.put(SdkConstants.UDF4, builder.udf4);

            if (builder.udf5 == null)
                throw new RuntimeException("udf5 is null, put some value or empty e.g. Builder.setUdf5(\"\")");
            else
                params.put(SdkConstants.UDF5, builder.udf5);


            if(IsDebugMode())
                Log.d("hashSeq", pipedHash);

            String hash = hashCal(pipedHash);

            if(IsDebugMode())
                Log.d("hash", hash);


            if(IsDebugMode())
                for (String key: params.keySet()){
                    String value = params.get(key);
                    Log.d("param : ", key + " - " + value);
                }
        }

        public void setMerchantHash(String serverCalculatedHash) {
            params.put(SdkConstants.HASH, serverCalculatedHash);
        }


        public static class Builder {

            private double amount = 0.0;
            private String key ;
            private String merchantId ;
            private String tnxId ;
            private String sUrl ;
            private String fUrl;
            private String productName;
            private String firstName;
            private String email;
            private String phone;
            private String udf1 = "";
            private String udf2 = "";
            private String udf3 = "";
            private String udf4 = "";
            private String udf5 = "";

            private boolean isDebug;

            public Builder setIsDebug(boolean isDebug) {
                this.isDebug = isDebug;
                return this;
            }

            public Builder() {
            }

            public Builder setAmount(double amount) {
                this.amount = amount;
                return this;
            }

            public Builder setKey(String key) {
                this.key = key;
                return this;
            }

            public Builder setMerchantId(String merchantId) {
                this.merchantId = merchantId;
                return this;
            }

            public Builder setTnxId(String tnxId) {
                this.tnxId = tnxId;
                return this;
            }

            public Builder setsUrl(String sUrl) {
                this.sUrl = sUrl;
                return this;
            }

            public Builder setfUrl(String fUrl) {
                this.fUrl = fUrl;
                return this;
            }

            public Builder setProductName(String productName) {
                this.productName = productName;
                return this;
            }

            public Builder setFirstName(String firstName) {
                this.firstName = firstName;
                return this;
            }

            public Builder setEmail(String email) {
                this.email = email;
                return this;
            }

            public Builder setPhone(String phone) {
                this.phone = phone;
                return this;
            }

            public Builder setUdf1(String udf1) {
                this.udf1 = udf1;
                return this;
            }

            public Builder setUdf2(String udf2) {
                this.udf2 = udf2;
                return this;
            }

            public Builder setUdf3(String udf3) {
                this.udf3 = udf3;
                return this;
            }

            public Builder setUdf4(String udf4) {
                this.udf4 = udf4;
                return this;
            }

            public Builder setUdf5(String udf5) {
                this.udf5 = udf5;
                return this;
            }

            public PaymentParam build() {
                return new PaymentParam(this);
            }
        }



        private  static String hashCal(String str) {
            byte[] hashseq = str.getBytes();
            StringBuilder hexString = new StringBuilder();
            try {
                MessageDigest algorithm = MessageDigest.getInstance("SHA-512");
                algorithm.reset();
                algorithm.update(hashseq);
                byte messageDigest[] = algorithm.digest();
                for (byte aMessageDigest : messageDigest) {
                    String hex = Integer.toHexString(0xFF & aMessageDigest);
                    if (hex.length() == 1) {
                        hexString.append("0");
                    }
                    hexString.append(hex);
                }
            } catch (NoSuchAlgorithmException ignored) {
            }
            return hexString.toString();
        }

        @Override
        public String toString() {
            return pipedHash;
        }
    }


}
