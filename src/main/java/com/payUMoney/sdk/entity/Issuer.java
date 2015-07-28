package com.payUMoney.sdk.entity;

/**
 * The credit card issuer
 */
public enum Issuer {
    VISA, MASTERCARD, MAESTRO, DISCOVER, AMEX, DINER, UNKNOWN, JCB, LASER;

    public static Issuer getIssuer(String issuer) {
        for (Issuer i : Issuer.values()) {
            if (i.name().equals(issuer)) {
                return i;
            }
        }
        return UNKNOWN;
    }
}
