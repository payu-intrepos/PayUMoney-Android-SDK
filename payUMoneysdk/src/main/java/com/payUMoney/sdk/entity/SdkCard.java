package com.payUMoney.sdk.entity;

/**
 * Credit card Entity
 */
public class SdkCard extends SdkEntity {
    private String mName = null;
    private String mNumber = null;
    private String mLabel = null;
    private String mToken = null;
    private String mMode = null;

    public static String getType(String number) {
        SdkCard c = new SdkCard();
        c.setNumber(number);
        switch (c.getIssuer()) {
            case AMEX:
                return "AMEX";
            case VISA:
                return "VISA";
            case MASTERCARD:
                return "MAST";
            case MAESTRO:
                return "MAES";
            case LASER:
                return "LASR";
            default:
                return "VISA";
        }
    }

    public static boolean isValidNumber(String number) {
        if (number.replaceAll("0", "").trim().length() == 0) {
            return false;
        }
        int s1 = 0, s2 = 0;
        String reverse = new StringBuffer(number).reverse().toString();
        for (int i = 0; i < reverse.length(); i++) {
            int digit = Character.digit(reverse.charAt(i), 10);
            if (i % 2 == 0) {//this is for odd digits, they are 1-indexed in the algorithm
                s1 += digit;
            } else {//add 2 * digit for 0-4, add 2 * digit - 9 for 5-9
                s2 += 2 * digit;
                if (digit >= 5) {
                    s2 -= 9;
                }
            }
        }
        return (s1 + s2) % 10 == 0;
    }

    public static boolean isAmex(String number) {
        return number.startsWith("34") || number.startsWith("37");
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        mNumber = number;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public SdkIssuer getIssuer() {
        if (mNumber.startsWith("4")) {
            return SdkIssuer.VISA;
        } else if (mNumber.matches("^((6304)|(6706)|(6771)|(6709))[\\d]+X+\\d+")) {
            return SdkIssuer.LASER;
        } else if (mNumber.matches("(5[06-8]|6\\d)\\d{14}(\\d{2,3})?[\\d]+X+\\d+") || mNumber.matches("(5[06-8]|6\\d)[\\d]+X+\\d+") || mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d)[\\d]+X+\\d+")) {
            return SdkIssuer.MAESTRO;
        } else if (mNumber.matches("^5[1-5][\\d]+X+\\d+")) {
            return SdkIssuer.MASTERCARD;
        } else if (mNumber.matches("^3[47][\\d]+X+\\d+")) {
            return SdkIssuer.AMEX;
        } else if (mNumber.startsWith("36") || mNumber.startsWith("34") || mNumber.startsWith("37") || mNumber.matches("^30[0-5][\\d]+X+\\d+")) {
            return SdkIssuer.DINER;
        } else if (mNumber.matches("2(014|149)[\\d]+X+\\d+")) {
            return SdkIssuer.DINER;
        } else if (mNumber.matches("^35(2[89]|[3-8][0-9])[\\d]+X+\\d+")) {
            return SdkIssuer.JCB;
        } else if (mNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}[\\d]+X+\\d+")) {
            return SdkIssuer.DISCOVER;
        } else {
            return SdkIssuer.UNKNOWN;
        }
    }

    public String getToken() {
        return mToken;
    }

    public void setToken(String token) {
        mToken = token;
    }

    public String getMode() {
        return mMode;
    }

    public void setMode(String mode) {
        mMode = mode;
    }
}
