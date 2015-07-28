package com.payUMoney.sdk.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.payUMoney.sdk.Constants;

/**
 * Helper class for creating, opening and upgrading the database.
 * <p/>
 * All scheme changes to the database happen here. To alter the scheme
 * programmatically, the DATABASE_VERSION is incremented and an according block
 * is added to the method onUpgrade().
 */
class DBOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "payumoney";
    private static final int DATABASE_VERSION = 2;
    private static DBOpenHelper INSTANCE;

    private DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DBOpenHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new DBOpenHelper(context);
        }
        return INSTANCE;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        onUpgrade(database, 0, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (Constants.DEBUG) {
            Log.w(Constants.TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        }
        if (oldVersion < 2) {

            database.execSQL("drop table if exists " + Cards.TABLE_NAME);
            database.execSQL("drop table if exists " + Users.TABLE_NAME);

            // onCreate
            String sql;
            sql = "create table " + Cards.TABLE_NAME + "(";
            sql += Cards.COL_ID + " INTEGER, ";
            sql += Cards.COL_NAME + " STRING, ";
            sql += Cards.COL_LABEL + " STRING, ";
            sql += Cards.COL_TOKEN + " STRING, ";
            sql += Cards.COL_MODE + " STRING, ";
            sql += Cards.COL_NUMBER + " STRING)";
            database.execSQL(sql);

            sql = "ALTER TABLE " + Cards.TABLE_NAME + " ADD COLUMN " + Cards.COL_HIDDEN + " BOOLEAN DEFAULT false";
            database.execSQL(sql);

            sql = "create table " + Users.TABLE_NAME + "(";
            sql += Users.COL_ID + " INTEGER, ";
            sql += Users.COL_AVATAR + " STRING, ";
            sql += Users.COL_NAME + " STRING, ";
//			sql += Users.COL_AMOUNT + " FLOAT, ";
            sql += Users.COL_EMAIL + " STRING, ";
            sql += Users.COL_PHONE + " STRING)";
            database.execSQL(sql);


            sql = "ALTER TABLE " + Users.TABLE_NAME + " ADD COLUMN " + Users.COL_PASSWORD_CHANGED + " BOOLEAN DEFAULT false";
            database.execSQL(sql);
        }
    }
}
