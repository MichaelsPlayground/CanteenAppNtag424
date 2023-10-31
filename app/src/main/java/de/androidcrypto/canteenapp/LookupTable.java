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
        return String.format("%3.2f", units / 100.0);
    }

    public String formatCreditDebitMarker() {
        if (transactionRecord.getCreditDebitMarker().toUpperCase().equals("C")) {
            return "Credit";
        } else {
            return "Debit";
        }
    }

    public String formatMachineNumber() {
        // simple demo lookup
        int machineNumber = (int) transactionRecord.getMachineNumber();
        if (machineNumber == 1) {
            return "00 cashier";
        } else if((machineNumber > 1) && (machineNumber < 11)) {
            return "01 cafe";
        } else {
            return "11 vending machine";
        }
    }

    public String formatGoodType() {
        // simple demo lookup
        int goodType = (int) transactionRecord.getGoodType();
        if (goodType == 0) {
            return "0 crediting";
        } else if ((goodType > 0) && (goodType < 11)) {
            return "1 food";
        } else {
            return "2 drinks";
        }
    }


}
