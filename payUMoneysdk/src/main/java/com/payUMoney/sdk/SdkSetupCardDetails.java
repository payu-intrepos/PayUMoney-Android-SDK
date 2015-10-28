package com.payUMoney.sdk;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.DatePicker;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Created by franklin.michael on 30-06-2014.
 */
public class SdkSetupCardDetails {

    public static String findIssuer(String mNumber, String cardMode) {
        if (mNumber.length() > 3) {
            if (mNumber.startsWith("4")) {
                return "VISA";
            } else if (Arrays.asList("6304", "6706", "6771", "6709").contains(mNumber.substring(0, 4))) {
                return "LASER";
            }/* else if(mNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}[\\d]+")) {
            return "DISCOVER";
        }*/ else if (mNumber.matches("(5[06-8]|6\\d|\\D)\\d{14}|\\D{14}(\\d{2,3}|\\D{2,3})?[\\d|\\D]+") || mNumber.matches("(5[06-8]|6\\d|\\D)[\\d|\\D]+") || mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d|\\D)[\\d|\\D]+")) {
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
            } else {
                if (cardMode.contentEquals("CC"))
                    return "CC";
                else if (cardMode.contentEquals("DC"))
                    return "MAST";
            }
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

        if (mNumber.startsWith("4")) {
            return visaDrawable;
        } else if (Arrays.asList("6304", "6706", "6771", "6709").contains(mNumber.substring(0, 4))) {
            //Laser
            return laserDrawable;
        } else if (mNumber.matches("6(?:011|5[0-9]{2})[0-9]{12}[\\d|\\D]+")) {
            //Discover
            return discoverDrawable;
        } else if (mNumber.matches("(5[06-8]|6\\d|\\D)\\d{14}|\\D(\\d{2,3}|\\D{2,3})?[\\d|\\D]+") || mNumber.matches("(5[06-8]|6\\d|\\D)[\\d|\\D]+") || mNumber.matches("((504([435|645|774|775|809|993]))|(60([0206]|[3845]))|(622[018])\\d|\\D)[\\d|\\D]+")) {
            return maestroDrawable;
        } else if (mNumber.matches("^5[1-5][\\d|\\D]+")) {
            return masterDrawable;
        } else if (mNumber.matches("^3[47][\\d|\\D]+")) {
            return amexDrawable;
        } else if (mNumber.startsWith("36") || mNumber.matches("^30[0-5][\\d|\\D]+")) {
            return dinerDrawable;
        } else if (mNumber.matches("2(014|149)[\\d|\\D]+")) {
            return dinerDrawable;
        } else if (mNumber.matches("^35(2[89]|[3-8][0-9])[\\d|\\D]+")) {
            return jcbDrawable;
        }
        return cardsDrawable;
    }

    public static DatePickerDialog customDatePicker(Activity activity, DatePickerDialog.OnDateSetListener mDateSetListener, int mYear, int mMonth, int mDay) {
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

}
