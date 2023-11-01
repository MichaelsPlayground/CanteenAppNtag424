package de.androidcrypto.canteenapp;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * This is a combined class representing one Value file and one Cyclic Record File
 * It has a file size of 128 byte so it fits in file 03 of a NTAG424DNA tag
 */

public class VirtualFile {

    private static final String TAG = VirtualFile.class.getName();
    /**
     * section for security
     */
    private byte[] salt;
    private final int NUMBER_OF_PBKDF2_ITERATIONS = 10000;
    private final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private final String SHA_HASH_ALGORITHM = "SHA-256";
    private final int SALT_LENGTH = 16;
    private final int GENERATED_KEY_LENGTH = 16;
    private final int CHECKSUM_LENGTH = 10;
    private byte[] generatedKey;
    private byte[] checksum;
    private boolean isVirtualFileValid = false;

    /**
     * section for Value File
     */
    private final int BALANCE_LENGTH = 4;
    private int balance;
    private byte[] balance_ByteArray;

    /**
     * section for Cyclic Record File
     */

    private final int MAXIMUM_NUMBER_OF_RECORDS = 5; // note: changing of this parameter changes file size, see constructor VirtualFile(byte[] exportedVf)
    private final int RECORD_SIZE = 16; // note: changing of this parameter changes file size, see constructor VirtualFile(byte[] exportedVf)
    private byte maximumRecords;
    private byte[][] records;
    private byte lastRecord;
    private byte numberOfRecords;

    /**
     * section for constructor
     */

    public VirtualFile(byte[] fileKey) {
        if (fileKey == null) {
            Log.e(TAG, "fileKey is NULL, aborted");
            return;
        }
        try {
            // generate a new salt
            salt = new byte[SALT_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);
            // generate a key using the fileKey and salt
            SecretKeyFactory secretKeyFactory = null;
            secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec keySpec = new PBEKeySpec(byteArrayToCharArray(fileKey), salt, NUMBER_OF_PBKDF2_ITERATIONS, GENERATED_KEY_LENGTH * 8);
            generatedKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
            //Log.d(TAG, Utils.printData("generatedKey", generatedKey));
            //Log.d(TAG, Utils.printData("salt", salt));
            balance = 0;
            balance_ByteArray = intToByteArray(balance);
            byte[] chkSum = calculateChecksum();
            //Log.d(TAG, Utils.printData("chkSum", chkSum));
            if (chkSum == null) {
                Log.e(TAG, "checksum is invalid, aborted");
                checksum = null;
                isVirtualFileValid = false;
                return;
            } else {
                checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
            }
            isVirtualFileValid = true;
            Log.i(TAG, "VirtualFile is active");
            Log.d(TAG, Utils.printData("balance_b", balance_ByteArray));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Error on constructing the class, aborted.");
            isVirtualFileValid = false;
        }
    }

    public VirtualFile(byte[] exportedVvf, boolean check) {
        if (exportedVvf == null) {
            Log.e(TAG, "exportedVvf is NULL, aborted");
            return;
        }
        if (exportedVvf.length != 48) {
            Log.e(TAG, "exportedVvf length is not 48, aborted");
            return;
        }
        salt = Arrays.copyOfRange(exportedVvf, 1, (1 + SALT_LENGTH));
        generatedKey = Arrays.copyOfRange(exportedVvf, (1 + SALT_LENGTH), (1 + SALT_LENGTH + GENERATED_KEY_LENGTH));
        balance_ByteArray = Arrays.copyOfRange(exportedVvf, (1 + SALT_LENGTH + GENERATED_KEY_LENGTH), (1 + SALT_LENGTH + GENERATED_KEY_LENGTH + BALANCE_LENGTH));
        checksum = Arrays.copyOfRange(exportedVvf, (1 + SALT_LENGTH + GENERATED_KEY_LENGTH + BALANCE_LENGTH), (1 + SALT_LENGTH + GENERATED_KEY_LENGTH + BALANCE_LENGTH + CHECKSUM_LENGTH));
        // verify the checksum
        byte[] chkSumOld = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSumOld == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualFileValid = false;
            return;
        } else {
            if (Arrays.equals(checksum, Arrays.copyOf(chkSumOld, CHECKSUM_LENGTH))) {
                // do nothing, checksum is valid
            } else {
                // checksum is invalid
                Log.e(TAG, "checksum is invalid, aborted");
                return;
            }
        }
        // checksum is valid, proceed
        balance = intFromByteArray(balance_ByteArray);
        isVirtualFileValid = true;
        Log.i(TAG, "VirtualFile is active");
    }

    /**
     * methods for Value File
     */

    public boolean credit(byte[] fileKey, int creditAmount) {
        // sanity checks
        if ((fileKey == null) || (fileKey.length < 1)) {
            Log.e(TAG, "credit failed, fileKey is NULL or of length 0");
            return false;
        }
        if (creditAmount < 1) {
            Log.e(TAG, "creditAmount is < 1, aborted");
            return false;
        }
        // todo maximum value for credit or balance
        // verify fileKey
        // generate a key using the fileKey and exiting salt
        SecretKeyFactory secretKeyFactory = null;
        byte[] genKey;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec keySpec = new PBEKeySpec(byteArrayToCharArray(fileKey), salt, NUMBER_OF_PBKDF2_ITERATIONS, GENERATED_KEY_LENGTH * 8);
            genKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Error on key derivation, aborted");
            return false;
        }
        if (Arrays.equals(generatedKey, genKey)) {
            // do nothing, fileKey is matching
        } else {
            // fileKey is not matching existing key
            Log.e(TAG, "the fileKey does not match the one used on construction, aborted");
            return false;
        }
        // verify checksum
        byte[] chkSumOld = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSumOld == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualFileValid = false;
            return false;
        } else {
            if (Arrays.equals(checksum, Arrays.copyOf(chkSumOld, CHECKSUM_LENGTH))) {
                // do nothing, checksum is valid
            } else {
                // checksum is invalid
                Log.e(TAG, "checksum is invalid, aborted");
                return false;
            }
        }
        // credit the balance
        balance = balance + creditAmount;
        balance_ByteArray = intToByteArray(balance);
        byte[] chkSum = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSum == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualFileValid = false;
            return false;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
        return true;
    }

    public boolean debit(int debitAmount) {
        // sanity checks
        if (debitAmount < 1) {
            Log.e(TAG, "debitAmount is < 1, aborted");
            return false;
        }
        if (debitAmount > balance) {
            Log.e(TAG, "debitAmount is > balance, aborted");
            return false;
        }
        // todo maximum value for debit
        // verify checksum
        byte[] chkSumOld = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSumOld == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualFileValid = false;
            return false;
        } else {
            if (Arrays.equals(checksum, Arrays.copyOf(chkSumOld, CHECKSUM_LENGTH))) {
                // do nothing, checksum is valid
            } else {
                // checksum is invalid
                Log.e(TAG, "checksum is invalid, aborted");
                return false;
            }
        }
        // debit the balance
        balance = balance - debitAmount;
        balance_ByteArray = intToByteArray(balance);
        byte[] chkSum = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSum == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualFileValid = false;
            return false;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
        return true;
    }

    /**
     * methods for Cyclic Records File
     */

    public boolean addRecord(byte[] fileKey, byte[] record) {
        // sanity checks
        if ((fileKey == null) || (fileKey.length < 1)) {
            Log.e(TAG, "addRecord failed, fileKey is NULL or of length 0");
            return false;
        }
        if ((record == null) || (record.length > RECORD_SIZE)) {
            Log.e(TAG, "record is NULL or size is > " + RECORD_SIZE + ", aborted");
            return false;
        }
        // verify fileKey
        // generate a key using the fileKey and exiting salt
        SecretKeyFactory secretKeyFactory = null;
        byte[] genKey;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec keySpec = new PBEKeySpec(byteArrayToCharArray(fileKey), salt, NUMBER_OF_PBKDF2_ITERATIONS, GENERATED_KEY_LENGTH * 8);
            genKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Error on key derivation, aborted");
            return false;
        }
        if (Arrays.equals(generatedKey, genKey)) {
            // do nothing, fileKey is matching
        } else {
            // fileKey is not matching existing key
            Log.e(TAG, "the fileKey does not match the one used on construction, aborted");
            return false;
        }
        // verify checksum
        byte[] chkSumOld = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSumOld == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualFileValid = false;
            return false;
        } else {
            if (Arrays.equals(checksum, Arrays.copyOf(chkSumOld, CHECKSUM_LENGTH))) {
                // do nothing, checksum is valid
            } else {
                // checksum is invalid
                Log.e(TAG, "checksum is invalid, aborted");
                return false;
            }
        }
        // fill the record if it is not RECORD_SIZE long
        byte[] recordFull = new byte[RECORD_SIZE];
        System.arraycopy(record, 0, recordFull, 0, record.length);
        int lastRecordInt = lastRecord;
        int numberOfRecordsInt = numberOfRecords;
        int maximumRecordsInt = maximumRecords;
        int writeToPositionInt = 0;
        if (numberOfRecordsInt > 0) {
            writeToPositionInt = lastRecordInt + 1;
        }
        if (writeToPositionInt > (maximumRecordsInt - 1)) {
            // we are cycling to the beginning
            writeToPositionInt = 0;
        } else {
            numberOfRecordsInt ++;
        }
        records[writeToPositionInt] = recordFull.clone();
        lastRecordInt = writeToPositionInt;
        // write data back to byte
        lastRecord = (byte) lastRecordInt;
        numberOfRecords = (byte) numberOfRecordsInt;
        // now recalculate the checksum
        byte[] chkSum = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSum == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualFileValid = false;
            return false;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
        return true;
    }

    public byte[] showLastRecord() {
        return records[lastRecord];
    }

    /**
     * This will export all records in descending order, meaning the last record is the first one and the
     * oldest record is the last one.
     * If the record file is NOT FULL only the active records are exported
     */
    public List<byte[]> getRecordList() {
        List<byte[]> recordList = new ArrayList<>();
        int lastRecordInt = lastRecord;
        int numberOfRecordsInt = numberOfRecords;
        int maximumRecordsInt = maximumRecords;
        int positionInt = lastRecordInt;
        if (numberOfRecordsInt == 0) return recordList; // returns an empty list
        for (int i = 0; i < numberOfRecordsInt; i++) {
            byte[] record = records[positionInt];
            recordList.add(record);
            positionInt --;
            if (positionInt < 0) {
                // cycling to the end
                positionInt = maximumRecordsInt - 1;
            }
        }
        return recordList;
    }

    public boolean clearRecords(byte[] fileKey) {
        // sanity checks
        if ((fileKey == null) || (fileKey.length < 1)) {
            Log.e(TAG, "clearRecords failed, fileKey is NULL or of length 0");
            return false;
        }
        // verify fileKey
        // generate a key using the fileKey and exiting salt
        SecretKeyFactory secretKeyFactory = null;
        byte[] genKey;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec keySpec = new PBEKeySpec(byteArrayToCharArray(fileKey), salt, NUMBER_OF_PBKDF2_ITERATIONS, GENERATED_KEY_LENGTH * 8);
            genKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Error on key derivation, aborted");
            return false;
        }
        if (Arrays.equals(generatedKey, genKey)) {
            // do nothing, fileKey is matching
        } else {
            // fileKey is not matching existing key
            Log.e(TAG, "the fileKey does not match the one used on construction, aborted");
            return false;
        }
        // now clearing the data
        lastRecord = 0;
        numberOfRecords = 0;
        records = new byte[MAXIMUM_NUMBER_OF_RECORDS][RECORD_SIZE];
        byte[] chkSum = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSum == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualFileValid = false;
            return false;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
        isVirtualFileValid = true;
        Log.i(TAG, "VirtualCyclicRecordFile is cleared and active");
        return true;
    }

    /**
     * section for import and export
     */

    public byte[] exportVirtualFile() {
        byte[] export = new byte[128];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // security
        baos.write(salt, 0, SALT_LENGTH); // 16 bytes long
        baos.write(generatedKey, 0, GENERATED_KEY_LENGTH); // 16 bytes long
        baos.write(checksum, 0, CHECKSUM_LENGTH); // 10 bytes long
        // value file
        baos.write(balance_ByteArray, 0, BALANCE_LENGTH); // 4 bytes long
        // record file
        baos.write(maximumRecords); // 1  byte long
        baos.write(numberOfRecords); // 1 byte long
        baos.write(lastRecord); // 1 byte long
        for (int i = 0; i < maximumRecords; i++) {
            baos.write(records[i], 0, RECORD_SIZE);
        } // size: maximumRecords * RECORD_SIZE, e.g. 5 * 16 = 80 bytes long
        export = baos.toByteArray(); // 128 bytes long
        return export;
    }


    /**
     * section for internal calculations
     */

    private byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private int intFromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    private char[] byteArrayToCharArray(byte[] input) {
        char[] buffer = new char[input.length >> 1];
        for (int i = 0; i < buffer.length; i++) {
            int bpos = i << 1;
            char c = (char) (((input[bpos] & 0x00FF) << 8) + (input[bpos + 1] & 0x00FF));
            buffer[i] = c;
        }
        return buffer;
    }

    private byte[] calculateChecksum() {
        try {
            byte[] calcBasis = new byte[37];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // security
            baos.write(salt, 0, SALT_LENGTH); // 16 bytes long
            baos.write(generatedKey, 0, GENERATED_KEY_LENGTH); // 16 bytes long
            // value file
            baos.write(balance_ByteArray, 0, BALANCE_LENGTH); // 4 bytes long
            // cyclic record file
            baos.write(maximumRecords); // 1  byte long
            baos.write(numberOfRecords); // 1 byte long
            baos.write(lastRecord); // 1 byte long
            for (int i = 0; i < maximumRecords; i++) {
                baos.write(records[i], 0, RECORD_SIZE);
            } // size: maximumRecords * RECORD_SIZE, e.g. 5 * 16 = 80 bytes long
            // output
            calcBasis = baos.toByteArray(); // 37 bytes long
            MessageDigest digest = null;
            digest = MessageDigest.getInstance(SHA_HASH_ALGORITHM);
            return digest.digest(calcBasis);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
