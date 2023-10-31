package de.androidcrypto.canteenapp;

public class TransactionModel {

    private String timestamp;
    private String value;
    private String booking;
    private String machine;
    private String good;

    public TransactionModel(String timestamp, String value, String booking, String machine, String good) {
        this.timestamp = timestamp;
        this.value = value;
        this.booking = booking;
        this.machine = machine;
        this.good = good;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getBooking() {
        return booking;
    }

    public void setBooking(String booking) {
        this.booking = booking;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getGood() {
        return good;
    }

    public void setGood(String good) {
        this.good = good;
    }
}
