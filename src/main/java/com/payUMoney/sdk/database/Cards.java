package com.payUMoney.sdk.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.payUMoney.sdk.entity.Card;
import com.payUMoney.sdk.manager.CardManager;

/**
 * See {@link Entities}
 */
public class Cards extends Entities<Card> {

    public static final int LOADER_ID = 1;

    /**
     * The table name
     */
    public static final String TABLE_NAME = "cards";
    /**
     * Column name for COL_NAME
     */
    public static final String COL_NAME = "name";
    /**
     * Column name for COL_NUMBER
     */
    public static final String COL_NUMBER = "number";
    /**
     * Column name for COL_LABEL
     */
    public static final String COL_LABEL = "label";
    /**
     * Column name for COL_TOKEN
     */
    public static final String COL_TOKEN = "token";
    /**
     * Column name for COL_MODE
     */
    public static final String COL_MODE = "mode";

    private static Cards INSTANCE;

    /*
     * private constructor for singleton
     */
    private Cards(Context context) {
        super(context);
    }

    public static synchronized Cards getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Cards(context);
            INSTANCE.open();
        }
        return INSTANCE;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected ContentValues getContentValues(Card a) {
        return CardManager.getContentValues(a);
    }

    public Card getCard(Cursor cursor) {
        Card card = new Card();
        card.setId(cursor.getLong(cursor.getColumnIndex(Cards.COL_ID)));
        card.setName(cursor.getString(cursor.getColumnIndex(Cards.COL_NAME)));
        card.setLabel(cursor.getString(cursor.getColumnIndex(Cards.COL_LABEL)));
        card.setNumber(cursor.getString(cursor.getColumnIndex(Cards.COL_NUMBER)));
        card.setMode(cursor.getString(cursor.getColumnIndex(Cards.COL_MODE)));
        card.setToken(cursor.getString(cursor.getColumnIndex(Cards.COL_TOKEN)));
        return card;
    }

    public Cursor findCC() {
        return db.query(getTableName(), new String[]{"*"}, "mode = ?", new String[]{"CC"}, null, null, null);
    }

    ;

    public Cursor findDC() {
        return db.query(getTableName(), new String[]{"*"}, "mode = ?", new String[]{"DC"}, null, null, null);
    }

    ;
}
