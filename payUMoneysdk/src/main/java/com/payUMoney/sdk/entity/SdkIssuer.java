package com.payUMoney.sdk.entity;

/**
 * The credit card issuer
 */
public enum SdkIssuer {
    VISA, MASTERCARD, MAESTRO, DISCOVER, AMEX, DINER, UNKNOWN, JCB, LASER, RUPAY;

    public static SdkIssuer getIssuer(String issuer) {
        for (SdkIssuer i : SdkIssuer.values()) {
            if (i.name().equals(issuer)) {
                return i;
            }
        }
        return UNKNOWN;
    }
}
