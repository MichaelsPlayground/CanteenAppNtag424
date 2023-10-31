package de.androidcrypto.canteenapp;


/**
 * This is a simple converter and lookup class for making the Transaction Record data more readable.
 * a) The short transaction timestamp gets a long one
 * b) the booking units are getting formatted
 * c) the credit/debit marker is written out
 * d) the machine number is translated to something readable (demo data)
 * e) the goods type is translated to something readable (demo data)
 */

public class LookupTable {

    private TransactionRecord transactionRecord;

    public LookupTable(TransactionRecord transactionRecord) {
        this.transactionRecord = transactionRecord;
    }

    public String getTransactionTimestamp() {
        return Utils.convertTimestampShortToLong(transactionRecord.getTimestampShort());
    }

    public String formatBookingUnits() {
        System.out.println("*** transactionRecord.getBookingUnits(): " + transactionRecord.getBookingUnits());
        int units = Integer.parseInt(transactionRecord.getBookingUnits());
        System.out.println("*** units: " + units);
        return String.format("%3.2f", units / 100.0);
    }


}
