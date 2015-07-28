package com.payUMoney.sdk.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.payUMoney.sdk.entity.User;
import com.payUMoney.sdk.manager.UserManager;

/**
 * See {@link Entities}
 */
public class Users extends Entities<User> {

    public static final int LOADER_ID = 2;

    /**
     * The table name
     */
    public static final String TABLE_NAME = "users";
    /**
     * Column name for COL_NAME
     */
    public static final String COL_NAME = "name";
    /**
     * Column name for COL_PHONE
     */
    public static final String COL_PHONE = "phone";
    /**
     * Column name for COL_EMAIL
     */
    public static final String COL_EMAIL = "email";
    /**
     * Column name for COL_AMOUNT
     */
    public static final String COL_AMOUNT = "amount";
    /**
     * Column name for COL_AVATAR
     */
    public static final String COL_AVATAR = "avatar";

    /**
     * Column name for COL_PASSWORD_CHANGED
     */
    public static final String COL_PASSWORD_CHANGED = "password_changed";

    private static Users INSTANCE;

    /*
     * private constructor for singleton
     */
    private Users(Context context) {
        super(context);
    }

    public static synchronized Users getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Users(context);
            INSTANCE.open();
        }
        return INSTANCE;
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    @Override
    protected ContentValues getContentValues(User a) {
        return UserManager.getContentValues(a);
    }

    public User getUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getLong(cursor.getColumnIndex(Users.COL_ID)));
        user.setName(cursor.getString(cursor.getColumnIndex(Users.COL_NAME)));
        user.setPhone(cursor.getString(cursor.getColumnIndex(Users.COL_PHONE)));
        user.setEmail(cursor.getString(cursor.getColumnIndex(Users.COL_EMAIL)));
        user.setAvatar(cursor.getString(cursor.getColumnIndex(Users.COL_AVATAR)));
//		user.setAmount(cursor.getFloat(cursor.getColumnIndex(Users.COL_AMOUNT)));
        return user;
    }
}
