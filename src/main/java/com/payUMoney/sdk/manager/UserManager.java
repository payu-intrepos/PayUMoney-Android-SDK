package com.payUMoney.sdk.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.payUMoney.sdk.Constants;
import com.payUMoney.sdk.database.Users;
import com.payUMoney.sdk.entity.Invoice;
import com.payUMoney.sdk.entity.User;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link Manager} for {@link Invoice}s
 */
public class UserManager extends Manager<User> {
    private static UserManager INSTANCE;

    private final Users db;

    private UserManager(Context context) {
        super(context);
        mContext = context;
        db = Users.getInstance(mContext);
    }

    public static synchronized UserManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new UserManager(context);
        }
        return INSTANCE;
    }

    /**
     * Get an {@link Invoice} from a {@link org.json.JSONObject}
     */
    public static User getUser(ContentValues values) {
        User user = new User();
        user.setAvatar(values.getAsString(Users.COL_AVATAR));
        user.setName(values.getAsString(Users.COL_NAME));
//		user.setAmount(values.getAsFloat("amount"));
        user.setEmail(values.getAsString(Users.COL_EMAIL));
        user.setPhone(values.getAsString(Users.COL_PHONE));
        user.setId(values.getAsLong(Users.COL_ID));
        user.setPasswordChanged(values.getAsBoolean(Users.COL_PASSWORD_CHANGED));
        return user;
    }

    /**
     * Get an {@link Invoice} from a {@link android.database.Cursor} positioned at a valid table
     * row
     */
    public static User getUser(Cursor values) {
        User user = new User();
        user.setName(values.getString(values.getColumnIndex(Users.COL_NAME)));
        user.setPhone(values.getString(values.getColumnIndex(Users.COL_PHONE)));
        user.setEmail(values.getString(values.getColumnIndex(Users.COL_EMAIL)));
        user.setAvatar(values.getString(values.getColumnIndex(Users.COL_AVATAR)));
        user.setId(values.getLong(values.getColumnIndex(Users.COL_ID)));
        user.setPasswordChanged(values.getShort(values.getColumnIndex(Users.COL_PASSWORD_CHANGED)) != 0);
//		user.setAmount(values.getFloat(values.getColumnIndex("amount")));
        return user;
    }

    /**
     * Get an {@link Invoice} from a {@link org.json.JSONObject}
     */
    public static User getUser(JSONObject values) {
        User user = new User();
        try {
            user.setId(values.getLong(Users.COL_ID));
            if (!values.isNull(Users.COL_AVATAR)) {
                user.setAvatar(values.getString(Users.COL_AVATAR));
            }
//			if(!values.isNull("amount")) {
//				user.setAmount(values.getDouble("amount"));
//			}
            if (!values.isNull(Users.COL_NAME)) {
                user.setName(values.getString(Users.COL_NAME));
            }
            if (!values.isNull(Users.COL_EMAIL)) {
                user.setEmail(values.getString(Users.COL_EMAIL));
            }
            if (!values.isNull(Users.COL_PHONE)) {
                user.setPhone(values.getString(Users.COL_PHONE));
            }
            if (!values.isNull(Users.COL_PASSWORD_CHANGED)) {
                user.setPasswordChanged(values.getBoolean(Users.COL_PASSWORD_CHANGED));
            }
        } catch (Throwable e) {
            if (Constants.DEBUG) {
                Log.e(Constants.TAG, "InvoiceManager.getInvoice: " + e.getMessage());
            }
        }
        return user;
    }

    /**
     * Get {@link android.content.ContentValues} from an {@link Invoice}
     */
    public static ContentValues getContentValues(User user) {
        ContentValues values = new ContentValues();
        values.put(Users.COL_ID, user.getId());
        values.put(Users.COL_AVATAR, user.getAvatar());
        values.put(Users.COL_NAME, user.getName());
        values.put(Users.COL_EMAIL, user.getEmail());
        values.put(Users.COL_PHONE, user.getPhone());
        values.put(Users.COL_PASSWORD_CHANGED, user.getPasswordChanged());
//		values.put("amount", user.getAmount());
        return values;
    }

    public List<User> getUsers(Cursor result) {
        List<User> users = new ArrayList<User>();
        while (result.getCount() > 0 && !result.isLast()) {
            result.moveToNext();
            users.add(getUser(result));
        }
        result.close();
        return users;
    }

    @Override
    protected Users getDb() {
        return db;
    }

    @Override
    protected void deleteOnServer(int id) {
        // TODO Auto-generated method stub
    }
}
