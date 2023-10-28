package de.androidcrypto.canteenapp;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * This class is responsible to build the 16 bytes long transaction log data. There are two ways
 * to build the data:
 *
 * constructor 1:
 *   String timestamp
 *   String "C" or "D" for a credit or debit transaction
 *   String booking units, 6 chars long (e.g. 012345 = 123,45 USD)
 *   Byte machine number (e.g. 01 for charger, 11 for cashier, 21 for vending machine)
 *   Byte good type (e.g. 00 for charging, 01 = food, 31 = drinks)
 *
 * constructor 2:
 *   Byte array 16 bytes long (e.g. 30092023153344 0A 0C 012345 01 00 FFFF)
 *                                  timestamp 7 bytes
 *                                                 am/pm marker, 1 byte
 *                                                    credit/debit marker, 1 byte
 *                                                       amount in units, 3 bytes
 *                                                             machine number, 1 byte
 *                                                                 good type, 1 byte
 *                                                                     reserved, 2 bytes
 *
 */

public class TransactionRecord {

    private String timestampShort; // e.g. 30092023153344 = Sep. 30th 2023 15:33:44
    private String amPmMarker; // A for am or P for pm
    private String creditDebitMarker; // C for credit or D for debit
    private String bookingUnits; // 012345 for 123,45 with trailing 0
    private byte machineNumber; // e.g. (byte) 0x01
    private byte goodType; // e.g. (byte) 0x00
    private byte[] reserved = new byte[]{(byte) 0xff, (byte) 0xff};
    private byte[] record; // a 16 bytes long array
    private Date transactionTimestamp;
    private boolean isRecordValid = false;

    public TransactionRecord(String timestampShort, String amPmMarker, String creditDebitMarker, String bookingUnits, byte machineNumber, byte goodType) {
        this.timestampShort = timestampShort;
        this.amPmMarker = amPmMarker;
        this.creditDebitMarker = creditDebitMarker;
        this.bookingUnits = bookingUnits;
        this.machineNumber = machineNumber;
        this.goodType = goodType;
        if (recordValidation()) {
            isRecordValid = true;
            buildRecord();
        } else {
            record = null;
        }
    }

    private boolean recordValidation() {
        // validate the timestamp byte building a date
        try {
            transactionTimestamp = new SimpleDateFormat("ddMMyyyyHHmmss").parse(timestampShort);
            //transactionTimestamp = new SimpleDateFormat("ddMMyyyyHHmmss").parse("2013-09-18T20:40:00+0000".replace("T"," ").substring(0,19));
        } catch (ParseException e) {
            // throw new RuntimeException(e);
            return false;
        }
        //System.out.println("TS: " + transactionTimestamp);
        if ((!amPmMarker.equals("A")) && (!amPmMarker.equals("P"))) return false;
        if ((!creditDebitMarker.equals("C")) && (!creditDebitMarker.equals("D"))) return false;
        // validate booking units, 1. char == 0
        if (!bookingUnits.startsWith("0")) return false;
        if (!TextUtils.isDigitsOnly(bookingUnits)) return false;
        if (bookingUnits.length() != 6) return false;
        return true;
    }

    private void buildRecord() {
        // generates a 16 bytes long record
        StringBuilder sb = new StringBuilder();
        sb.append(timestampShort);
        sb.append("0");
        String amPm = new SimpleDateFormat("aa").format(transactionTimestamp);
        if (amPm.startsWith("A")) {
            sb.append("A");
        } else {
            sb.append("F");
        }
        sb.append("0");
        sb.append(creditDebitMarker);
        sb.append(bookingUnits);
        sb.append(Utils.byteToHex(machineNumber));
        sb.append(Utils.byteToHex(goodType));
        sb.append(Utils.bytesToHex(reserved));
        record = Utils.hexStringToByteArray(sb.toString());
        if ((record != null) && (record.length == 16)) isRecordValid = true;
    }


    public TransactionRecord(byte[] record) {
        this.record = record;
        parseRecord();
        if (recordValidation()) {
            isRecordValid = true;
        } else {
            record = null;
        }
    }

    private void parseRecord() {
        if ((record == null) || (record.length != 16)) return;
        // split data
        String recordString = Utils.bytesToHexNpeUpperCase(record);
        timestampShort = recordString.substring(0, 14);
        amPmMarker = recordString.substring(15, 16);
        if (amPmMarker.equals("F")) amPmMarker = "P";
        creditDebitMarker = recordString.substring(17, 18);
        bookingUnits = recordString.substring(18, 24);
        machineNumber = Byte.parseByte(recordString.substring(24, 26));
        goodType = Byte.parseByte(recordString.substring(26, 28));
        reserved = Utils.hexStringToByteArray(recordString.substring(28, 32));
    }

    /**
     * section for getter
     */

    public String getTimestampShort() {
        return timestampShort;
    }

    public String getAmPmMarker() {
        return amPmMarker;
    }

    public String getCreditDebitMarker() {
        return creditDebitMarker;
    }

    public String getBookingUnits() {
        return bookingUnits;
    }

    public byte getMachineNumber() {
        return machineNumber;
    }

    public byte getGoodType() {
        return goodType;
    }

    public byte[] getRecord() {
        return record;
    }

    public byte[] getReserved() {
        return reserved;
    }

    public Date getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public boolean isRecordValid() {
        return isRecordValid;
    }
}
