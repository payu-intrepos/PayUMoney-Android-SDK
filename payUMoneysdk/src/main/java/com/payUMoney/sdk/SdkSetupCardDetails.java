package com.payUMoney.sdk;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.Arrays;

/**
 * Created by franklin.michael on 30-06-2014.
 */
public class SdkSetupCardDetails {

    public static String findIssuer(String mNumber, String cardMode) {
        if(mNumber != null)
        mNumber = mNumber.replaceAll("\\s+","");

        if (mNumber.length() > 5) {

            if (mNumber.matches("^3[47][\\d|\\D]+")) {
                return "AMEX";
            } else if (mNumber.matches("^30[0-5][\\d|\\D]+")) {
                return "DINR";//Diner
            } else if (mNumber.startsWith("36")) {
                return "DINR";//Diners_club
            } else if (mNumber.matches("^35(2[89]|[3-8][0-9])[\\d|\\D]+")) {
                return "JCB";
            } else if (Arrays.asList("6304", "6706", "6771", "6709").contains(mNumber.substring(0, 4))) {
                return "LASER";
            } else if (mNumber.matches("^(4026|417500|4508|4844|491(3|7))[\\d|\\D]+")) {
                return "VISA";//Visa_electron
            } else if (mNumber.startsWith("4")) {
                return "VISA";//visa
            } else if (mNumber.matches("^5[1-5][\\d|\\D]+")) {
                return "MAST";//mastercard
            } else if ((mNumber.substring(0, 6)).matches("^(508[5-9][0-9][0-9])|(60698[5-9])|(60699[0-9])|(607[0-8][0-9][0-9])|(6079[0-7][0-9])|(60798[0-4])|(608[0-4][0-9][0-9])|(608500)|(6528[5-9][0-9])|(6529[0-9][0-9])|(6530[0-9][0-9])|(6531[0-4][0-9])|(6521[5-9][0-9])|(652[2-7][0-9][0-9])|(6528[0-4][0-9])")) {
                return "RUPAY";
            } else if (mNumber.matches("(5[06-8]|6\\d|\\D)\\d{14}|\\D{14}(\\d{2,3}|\\D{2,3})?[\\d|\\D]+") || mNumber.matches("(5[06-8]|6\\d|\\D)[\\d|\\D]+") || mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d|\\D)[\\d|\\D]+")) {
                return "MAES";
            }
            /* else if (mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d)[\\d|\\D]+")) {
                if (cardMode.contentEquals(SdkConstants.PAYMENT_MODE_CC))
                    return SdkConstants.PAYMENT_MODE_CC;
                else
                    return "SMAE";//smaestro
            } else if (mNumber.matches("(5[06-8]|6\\d)")) {
                if (cardMode.contentEquals(SdkConstants.PAYMENT_MODE_CC))
                    return SdkConstants.PAYMENT_MODE_CC;
                else
                    return "MAES";//maestro
            }*/
            else if (mNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}")) {
                return SdkConstants.PAYMENT_MODE_CC;//discover
            } else {
                if (cardMode.contentEquals(SdkConstants.PAYMENT_MODE_CC))
                    return SdkConstants.PAYMENT_MODE_CC;
                else if (cardMode.contentEquals(SdkConstants.PAYMENT_MODE_DC))
                    return "MAST";
            }
            /*    if (mNumber.startsWith("4")) {
                return "VISA";
                } else if (Arrays.asList("6304", "6706", "6771", "6709").contains(mNumber.substring(0, 4))) {
                return "LASER";
            }*//* else if(mNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}[\\d]+")) {
            return "DISCOVER";
        }*//* else if (mNumber.matches("(5[06-8]|6\\d|\\D)\\d{14}|\\D{14}(\\d{2,3}|\\D{2,3})?[\\d|\\D]+") || mNumber.matches("(5[06-8]|6\\d|\\D)[\\d|\\D]+") || mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d|\\D)[\\d|\\D]+")) {
                return "MAES";
            } else if (mNumber.matches("^5[1-5][\\d|\\D]+")) {
                    return "MAST";
            } else if (mNumber.matches("^3[47][\\d|\\D]+")) {
                return "AMEX";
            } else if (mNumber.startsWith("36") || mNumber.matches("^30[0-5][\\d|\\D]+")) {
                    return "DINR";
            } else if (mNumber.matches("2(014|149)[\\d|\\D]+")) {
                    return "DINR";
            } else if (mNumber.matches("^35(2[89]|[3-8][0-9])[\\d|\\D]+")) {
                return "JCB";
            } */
        }
        return null;
    }

    public static Drawable getCardDrawable(Resources resources, String mNumber) {

        Drawable amexDrawable = resources.getDrawable(R.drawable.amex);
        Drawable dinerDrawable = resources.getDrawable(R.drawable.diner);
        Drawable maestroDrawable = resources.getDrawable(R.drawable.maestro);
        Drawable masterDrawable = resources.getDrawable(R.drawable.master);
        Drawable visaDrawable = resources.getDrawable(R.drawable.visa);
        Drawable jcbDrawable = resources.getDrawable(R.drawable.jcb);
        Drawable laserDrawable = resources.getDrawable(R.drawable.laser);
        Drawable discoverDrawable = resources.getDrawable(R.drawable.discover);
        Drawable cardsDrawable = resources.getDrawable(R.drawable.card);
        Drawable dinerClubDrawable = resources.getDrawable(R.drawable.dinners_club);
        Drawable visaElectronDrawable = resources.getDrawable(R.drawable.visa_electron_card);
        Drawable rupayDrawable = resources.getDrawable(R.drawable.rupay_card);

        if (mNumber.matches("^3[47][\\d|\\D]+")) {
            return amexDrawable;
        } else if (mNumber.matches("^30[0-5][\\d|\\D]+")) {
            return dinerDrawable;//Diner
        } else if (mNumber.startsWith("36")) {
            return dinerClubDrawable;//Diners_club
        } else if (mNumber.matches("^35(2[89]|[3-8][0-9])[\\d|\\D]+")) {
            return jcbDrawable;
        } else if (Arrays.asList("6304", "6706", "6771", "6709").contains(mNumber.substring(0, 4))) {
            return laserDrawable;
        } else if (mNumber.matches("^(4026|417500|4508|4844|491(3|7))")) {
            return visaElectronDrawable;//Visa_electron
        } else if (mNumber.startsWith("4")) {
            return visaDrawable;//visa
        } else if (mNumber.matches("^5[1-5][\\d|\\D]+")) {
            return masterDrawable;//mastercard
        } else if (mNumber.substring(0, 6).matches("(?!608000)(^(508[5-9][0-9][0-9])|(60698[5-9])|(60699[0-9])|(607[0-8][0-9][0-9])|(6079[0-7][0-9])|(60798[0-4])|(608[0-4][0-9][0-9])|(608500)|(6528[5-9][0-9])|(6529[0-9][0-9])|(6530[0-9][0-9])|(6531[0-4][0-9])|(6521[5-9][0-9])|(652[2-7][0-9][0-9])|(6528[0-4][0-9]))")) {
            return rupayDrawable;
        } else if (mNumber.matches("(5[06-8]|6\\d|\\D)\\d{14}|\\D{14}(\\d{2,3}|\\D{2,3})?[\\d|\\D]+") || mNumber.matches("(5[06-8]|6\\d|\\D)[\\d|\\D]+") || mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d|\\D)[\\d|\\D]+")) {
            return maestroDrawable;
        }
         /*else if (mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d)")) {
            return maestroDrawable;//smaestro
        } else if (mNumber.matches("(5[06-8]|6\\d)")) {
            return maestroDrawable;//maestro
        }*/
        else if (mNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}")) {
            return discoverDrawable;
        }
        return cardsDrawable;
    }

    /*public static DatePickerDialog customDatePicker(Activity activity, DatePickerDialog.OnDateSetListener mDateSetListener, int mYear, int mMonth, int mDay) {
        DatePickerDialog dpd = new DatePickerDialog(activity, mDateSetListener, mYear, mMonth, mDay);
//        dpd.getDatePicker().setMinDate(new Date().getTime() - 1000);
        if (Build.VERSION.SDK_INT >= 11) {
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        }
        try {
            Field[] datePickerDialogFields = dpd.getClass().getDeclaredFields();
            for (Field datePickerDialogField : datePickerDialogFields) {
                if (datePickerDialogField.getName().equals("mDatePicker")) {
                    datePickerDialogField.setAccessible(true);
                    DatePicker datePicker = (DatePicker) datePickerDialogField.get(dpd);
                    Field datePickerFields[] = datePickerDialogField.getType().getDeclaredFields();
                    for (Field datePickerField : datePickerFields) {
                        if ("mDayPicker".equals(datePickerField.getName()) || "mDaySpinner".equals(datePickerField.getName())) {
                            datePickerField.setAccessible(true);
                            ((View) datePickerField.get(datePicker)).setVisibility(View.GONE);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        dpd.setTitle(null);
        return dpd;
    }
*/
}
