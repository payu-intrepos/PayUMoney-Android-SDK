package com.payUMoney.sdk;

/**
 * Created by franklin on 12/7/14.
 */

public class SdkLuhn {
    /*public static boolean validate(String ccNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = ccNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(ccNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }*/

    public static boolean validate(String ccNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = ccNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(ccNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        if (sum % 10 == 0) {
            // valid now check length
            if (SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("VISA") && ccNumber.length() == 16) {
                return true;
            } else if (SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("LASER") && ccNumber.length() >= 16 && ccNumber.length() <= 19) {
                return true;
            } else if (SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("MAST") && ccNumber.length() == 16) {
                return true;
            } else if ((SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("MAES")/* || SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("SMAE")*/) && ccNumber.length() >= 12 && ccNumber.length() <= 19) {
                return true;
            } else if (SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("DINR") && ccNumber.length() == 14) {
                return true;
            } else if (SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("AMEX") && ccNumber.length() == 15) {
                return true;
            } else if (SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("JCB") && ccNumber.length() == 16) {
                return true;
            } else if (SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("RUPAY") && (ccNumber.length() == 16 || ccNumber.length() == 19)) {
                return true;
            } else if (SdkSetupCardDetails.findIssuer(ccNumber, SdkConstants.PAYMENT_MODE_CC).contentEquals("JCB") && ccNumber.length() == 16) {
                return true;
            } else if (ccNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}") && ccNumber.length() == 16) {
                return true;//discover
            }

        }
        return false;
    }

}

