package com.payUMoney.sdk.manager;

import android.content.Context;
import android.database.Cursor;

import com.payUMoney.sdk.CobbocEvent;
import com.payUMoney.sdk.Session;
import com.payUMoney.sdk.database.Entities;
import com.payUMoney.sdk.entity.Entity;

import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * A Manager connects the server via {@link Session} with the database via
 * {@link Entities}
 *
 * @param <A> An Entity subclass
 */
public abstract class Manager<A extends Entity> {
    protected final Session session;
    protected final EventBus eventBus;
    protected Context mContext;
    private long deleteWarningShownTimestamp;

    public Manager(Context context) {
        mContext = context;
        session = Session.getInstance(context);
        eventBus = EventBus.getDefault();
    }

    /**
     * Fetches the latest data from the server
     */
    public void refreshList() {
//		session.sync();
    }

    /**
     * Tells the server to delete the {@link Entity}s defined by deleteIds.
     *
     * @param id id of the {@link Entity}s that should be deleted
     */
    public void delete(int id) {
        getDb().markDeleted(id);
        deleteOnServer(id);
        postDbUpdatedEvent();
    }

    public Cursor find(long id) {
        return getDb().findOne(id);
    }

    public Cursor findFirst() {
        return getDb().findFirst();
    }

    /**
     * Convenience method for {@link Entities#findAll()}
     */
    public Cursor findAll() {
        return getDb().findAll();
    }

    /**
     * Convenience method for {@link Entities#findAll(String)}
     */
    public Cursor findAll(String orderBy) {
        return getDb().findAll(orderBy);
    }

    /**
     * Stores {@link Entity}s (as received from the server) in the database and
     * sends a {@link com.payu.payumoney.CobbocEvent}
     *
     * @param entities List of Entities. {@link Entity#isDeleted()} is taken as an
     *                 instruction to delete the respective Entity from the database.
     *                 Else, the Entity gets updated or inserted
     */
    public void updateEntities(List<A> entities) {
        for (A entity : entities) {
            if (entity.isDeleted()) {
                getDb().delete(entity.getId());
            } else {
                Cursor result = getDb().findOne(entity.getId());
                if (result.getCount() == 1) {
                    getDb().update(entity);
                } else {
                    getDb().insert(entity);
                }
                result.close();
            }
        }
        postDbUpdatedEvent();
    }

    /**
     * Concrete implementations have to call this on server feedback
     *
     * @param id {@link Entity} id to be deleted
     */
    public void deleteSucceeded(int id) {
        getDb().delete(id);
    }

    /**
     * Concrete implementations have to call this on negative server feedback
     *
     * @param id {@link Entity} id to be unhidden
     */
//    protected void deleteFailed(int id) {
//        getDb().markUndeleted(id);
//        long currentTimeMillis = System.currentTimeMillis();
//        if (deleteWarningShownTimestamp + 2000 < currentTimeMillis) {
//            deleteWarningShownTimestamp = currentTimeMillis;
//            eventBus.post(new CobbocEvent(CobbocEvent.DATABASE_UPDATED, false, mContext.getString(R.string/.delete_failed)));
//        }
//        postDbUpdatedEvent();
    // }
    protected void postDbUpdatedEvent() {
        eventBus.post(new CobbocEvent(CobbocEvent.DATABASE_UPDATED));
    }

    /**
     * Delete {@link Entity} on the server. The server call is not generic,
     * therefore the concrete implementation has to handle this.
     *
     * @param id id of the {@link Entity} that should be deleted
     */
    protected abstract void deleteOnServer(int id);

    /**
     * Get an {@link Entities} instance
     */
    protected abstract Entities<A> getDb();

    public void emptyDb() {
        getDb();
    }
}
