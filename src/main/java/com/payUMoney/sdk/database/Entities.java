package com.payUMoney.sdk.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.payUMoney.sdk.Constants;
import com.payUMoney.sdk.entity.Entity;

/**
 * A database abstraction layer for Entity, designed for mirroring a server side
 * entity for asynchronous operations through marking entities hidden until the
 * server confirms the deletion, etc.
 *
 * @param <A> An Entity subclass
 */
public abstract class Entities<A extends Entity> {
    public static final String COL_ID = "id";
    public static final String COL_HIDDEN = "hidden";
    private final DBOpenHelper dbHelper;
    SQLiteDatabase db;

    Entities(Context context) {
        dbHelper = DBOpenHelper.getInstance(context);
    }

    /**
     * Opens the underlying database for writing. If this fails, it is opened
     * for reading only.
     *
     * @throws android.database.sqlite.SQLiteException Opening the database might throw an SQLiteException
     */
    void open() throws SQLiteException {
        if (db != null && db.isOpen()) {
            return;
        }
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLiteException ex) {
            if (Constants.DEBUG) {
                Log.d(Constants.TAG, "Entities.open: couldn't open the db writable because of an Exception: " + ex.getMessage() + "\nTrying to open read only.");
            }
            db = dbHelper.getReadableDatabase();
        }
    }

    /**
     * Gets a cursor to all Entities
     *
     * @return a Cursor to all Entities
     */
    public Cursor findAll() {
        return findAll("");
    }

    /**
     * Gets a cursor to all Entities with ordering
     *
     * @param orderBy order by statement. This must contain "ORDER BY" and is not
     *                sanitised before being used in the query
     * @return a Cursor to all Entities
     */
    public Cursor findAll(String orderBy) {
        return db.rawQuery("select * from " + getTableName() + " WHERE NOT " + COL_HIDDEN + " " + orderBy, null);
    }

    public Cursor findFirst() {
        return db.query(getTableName(), new String[]{"*"}, null, null, null, null, null);
    }

    /**
     * Gets a cursor to "all" Entities with the specified id.
     *
     * @param id id of the entity to be found
     * @return Cursor to zero or one entities
     */
    public Cursor findOne(long id) {
        return db.query(getTableName(), new String[]{"*"}, "id = ?", new String[]{String.valueOf(id)}, null, null, null);
    }

    /**
     * Gets a cursor to "merchant" Entities with the specified mid.
     *
     * @param mid mid of the merchant to be found
     * @return Cursor to zero or one entities
     */
    public Cursor findOneByMid(String mid) {
        return db.query(getTableName(), new String[]{"*"}, Constants.MID + " = ?", new String[]{mid}, null, null, null);
    }

    /**
     * Deletes entity if id is found
     *
     * @param id id of the entity that is to be deleted
     */
    public void delete(long id) {
        db.delete(getTableName(), "id = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Marks Entity deleted if id found
     *
     * @param id id of the entity that is to be marked
     */
    public void markDeleted(int id) {
        markDeleted(id, true);
    }

    /**
     * Marks Entity not deleted if id found
     *
     * @param id id of the entity that is to be marked
     */
    public void markUndeleted(int id) {
        markDeleted(id, false);
    }

    /**
     * Marks all Entities not deleted
     */
    public void markUndeleted() {
        db.rawQuery("UPDATE " + getTableName() + " SET " + COL_HIDDEN + " = 0", null);
    }

    /**
     * Insert one Entity
     *
     * @param entity the Entity that is to be inserted
     * @return the id of the newly inserted row, or -1 if an error occurred
     */
    public void insert(A entity) {
        db.insert(getTableName(), null, getContentValues(entity));
    }

    /**
     * Update one Entity
     *
     * @param entity the Entity that is to be updated
     */
    public void update(A entity) {
        db.update(getTableName(), getContentValues(entity), "id = ?", new String[]{String.valueOf(entity.getId())});
    }

    /**
     * Grants access to the concrete database table name in the abstract class
     *
     * @return name of the database table to be used for the concrete class
     */
    protected abstract String getTableName();

    /**
     * Converts an Entity into ContentValues
     *
     * @param entity The Entity
     * @return ContentValues containing all the Entity fields
     */
    protected abstract ContentValues getContentValues(A entity);

    private void markDeleted(int id, boolean deleted) {
        ContentValues values = new ContentValues();
        values.put(COL_HIDDEN, deleted ? 1 : 0);
        db.update(getTableName(), values, "id = ?", new String[]{"" + id});
    }

    public void deleteAll() {
        db.delete(getTableName(), null, null);
    }
}
