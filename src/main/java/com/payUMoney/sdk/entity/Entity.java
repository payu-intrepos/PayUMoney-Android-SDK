package com.payUMoney.sdk.entity;

/**
 * Database entity class
 */
public abstract class Entity {
    private long id;
    private boolean deleted = false;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
