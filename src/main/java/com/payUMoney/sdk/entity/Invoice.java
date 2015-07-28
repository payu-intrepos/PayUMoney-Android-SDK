package com.payUMoney.sdk.entity;

/**
 * Invoice Entity
 */
public class Invoice extends Entity {
    private String mCurrency = null;
    private String service = null;
    private String photo = null;
    private String name = null;
    private String user;
    private int seller_id;
    private int buyer_id;
    private int paid;
    private long created_at;
    private double amount;
    private int pin;
    private double discount;

    public String getCurrency() {
        return mCurrency;
    }

    public void setCurrency(String currency) {
        mCurrency = currency;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        if (service != null && !service.equals("null")) {
            this.service = service;
        }
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getSeller_id() {
        return seller_id;
    }

    public void setSeller_id(int seller_id) {
        this.seller_id = seller_id;
    }

    public int getBuyer_id() {
        return buyer_id;
    }

    public void setBuyer_id(int buyer_id) {
        this.buyer_id = buyer_id;
    }

    public int getPaid() {
        return paid;
    }

    public void setPaid(int paid) {
        this.paid = paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid ? 1 : 0;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double value) {
        discount = value;
    }

//	@Override
//	public String toString() {
//		return "Invoice [currency=" + mCurrency + ", service=" + service + ", photo=" + photo + ", name=" + name + ", user=" + user + ", seller_id=" + seller_id + ", buyer_id=" + buyer_id + ", paid=" + paid + ", created_at=" + created_at + ", amount=" + amount + ", pin=" + pin + "]";
//	}
}
