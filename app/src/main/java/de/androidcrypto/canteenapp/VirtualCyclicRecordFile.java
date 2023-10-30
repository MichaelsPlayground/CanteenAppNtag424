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
 * The class provides access to a Virtual Cyclic Record File that acts like a real Cyclic Record File
 * (e.g. available on DESFire Light or DESFire EVx tags).
 * <p>
 * Each record has a fixed size of 16 bytes and a maximum number of records of 10 entries.
 * You can add a record, show the last record, get a list with all records (sorted by storage time) and clear the complete file.
 *
 * All writing access is secured by a key using a key derivation (PBKDF2, 10000 iterations, 16 bytes resulting key length,
 * algorithm PBKDF2WithHmacSHA1).
 *
 * All data is secured by a 12 bytes long checksum based on a SHA-256 hash calculation but shortened to a multiple of 16 bytes export
 * size.
 *
 * All data is secured by a 12 bytes long checksum based on a SHA-256 hash calculation.
 * If you are using the maximum of 10 records and a RECORD_SIZE of 16 bytes the complete file takes
 * 208 bytes of size, if you are using the minimum of 1 record the size is 64 bytes.
 */
public class VirtualCyclicRecordFile {
    private static final String TAG = VirtualCyclicRecordFile.class.getName();
    private byte fileNumber;
    private byte[] salt;
    private final int NUMBER_OF_PBKDF2_ITERATIONS = 10000;
    private final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
    private final String SHA_HASH_ALGORITHM = "SHA-256";
    private final int SALT_LENGTH = 16;
    private final int GENERATED_KEY_LENGTH = 16;
    private final int CHECKSUM_LENGTH = 12;
    private final int MAXIMUM_NUMBER_OF_RECORDS = 10; // note: changing of this parameter changes file size, see constructor VirtualCyclicRecordFile(byte[] exportedVcrf)
    private final int RECORD_SIZE = 16; // note: changing of this parameter changes file size, see constructor VirtualCyclicRecordFile(byte[] exportedVcrf)
    private byte[] generatedKey;
    private byte maximumRecords;
    private byte[][] records;
    private byte lastRecord;
    private byte numberOfRecords;
    private byte[] checksum;
    private boolean isVirtualCyclicRecordFileValid = false;

    public VirtualCyclicRecordFile(byte fileNumber, byte maximumRecords, byte[] fileKey) {
        // sanity checks
        if (fileKey == null) {
            Log.e(TAG, "fileKey is NULL, aborted");
            return;
        }
        if ((maximumRecords < 1) || (maximumRecords > MAXIMUM_NUMBER_OF_RECORDS)) {
            Log.e(TAG, "maximumRecords is < 1 or > " + MAXIMUM_NUMBER_OF_RECORDS + ", aborted");
            return;
        }
        this.fileNumber = fileNumber;
        this.maximumRecords = maximumRecords;
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
            records = new byte[maximumRecords][RECORD_SIZE];
            lastRecord = 0;
            numberOfRecords = 0;
            byte[] chkSum = calculateChecksum();
            //Log.d(TAG, Utils.printData("chkSum", chkSum));
            if (chkSum == null) {
                Log.e(TAG, "checksum is invalid, aborted");
                checksum = null;
                isVirtualCyclicRecordFileValid = false;
                return;
            } else {
                checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
            }
            isVirtualCyclicRecordFileValid = true;
            Log.i(TAG, "VirtualCyclicRecordFile is active for fileNumber " + Utils.byteToHex(fileNumber));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Error on constructing the class, aborted. fileNumber: " + Utils.byteToHex(fileNumber));
            isVirtualCyclicRecordFileValid = false;
        }
    }

    public VirtualCyclicRecordFile(byte[] exportedVcrf) {
        if (exportedVcrf == null) {
            Log.e(TAG, "exportedVcrf is NULL, aborted");
            return;
        }
        if (exportedVcrf.length > 208) {
            Log.e(TAG, "exportedVcrf length extends 208 bytes, aborted");
            return;
        }
        if (exportedVcrf.length < 64) {
            Log.e(TAG, "exportedVcrf length is smaller than 208 bytes, aborted");
            return;
        }

        fileNumber = exportedVcrf[0];
        maximumRecords = exportedVcrf[1];
        numberOfRecords = exportedVcrf[2];
        lastRecord = exportedVcrf[3];
        salt = Arrays.copyOfRange(exportedVcrf, 4, (4 + SALT_LENGTH));
        generatedKey = Arrays.copyOfRange(exportedVcrf, (4 + SALT_LENGTH), (4 + SALT_LENGTH + GENERATED_KEY_LENGTH));
        byte[] record;
        records = new byte[MAXIMUM_NUMBER_OF_RECORDS][RECORD_SIZE];
        try {
            for (int i = 0; i < maximumRecords; i++) {
                record = Arrays.copyOfRange(exportedVcrf, (4 + SALT_LENGTH + GENERATED_KEY_LENGTH) + ((i + 0) * RECORD_SIZE), (4 + SALT_LENGTH + GENERATED_KEY_LENGTH + ((i + 1) * RECORD_SIZE)));
                records[i] = record;
            } // size: maximumRecords * RECORD_SIZE, e.g. 10 * 16 = 160 bytes long
            // here we are taking the last CHECKSUM_LENGTH bytes from data
            checksum = Arrays.copyOfRange(exportedVcrf, (exportedVcrf.length - CHECKSUM_LENGTH), (exportedVcrf.length));
        } catch (ArrayIndexOutOfBoundsException e) {
            // this can happen if a corrupted = too short data is presented
            Log.e(TAG, "corrupted data detected, aborted");
            checksum = null;
            isVirtualCyclicRecordFileValid = false;
            return;
        }
        // verify the checksum
        byte[] chkSumOld = calculateChecksum();
        //Log.d(TAG, Utils.printData("chkSum", chkSum));
        if (chkSumOld == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualCyclicRecordFileValid = false;
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
        isVirtualCyclicRecordFileValid = true;
        Log.i(TAG, "VirtualCyclicRecordFile is active for fileNumber " + Utils.byteToHex(fileNumber));
    }

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
            isVirtualCyclicRecordFileValid = false;
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
            isVirtualCyclicRecordFileValid = false;
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
            isVirtualCyclicRecordFileValid = false;
            return false;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
        isVirtualCyclicRecordFileValid = true;
        Log.i(TAG, "VirtualCyclicRecordFile is cleared and active for fileNumber " + Utils.byteToHex(fileNumber));
        return true;
    }

    public byte[] exportVcrf() {
        byte[] export = new byte[212];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fileNumber); // 1  byte long
        baos.write(maximumRecords); // 1  byte long
        baos.write(numberOfRecords); // 1 byte long
        baos.write(lastRecord); // 1 byte long
        baos.write(salt, 0, SALT_LENGTH); // 16 bytes long
        baos.write(generatedKey, 0, GENERATED_KEY_LENGTH); // 16 bytes long
        for (int i = 0; i < maximumRecords; i++) {
            baos.write(records[i], 0, RECORD_SIZE);
        } // size: maximumRecords * RECORD_SIZE, e.g. 10 * 16 = 160 bytes long
        baos.write(checksum, 0, CHECKSUM_LENGTH); // 16 bytes long
        export = baos.toByteArray(); // 212 bytes long
        return export;
    }

    /**
     * section for internal calculations
     */

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
            byte[] calcBasis = new byte[196];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(fileNumber); // 1  byte long
            baos.write(maximumRecords); // 1  byte long
            baos.write(numberOfRecords); // 1 byte long
            baos.write(lastRecord); // 1 byte long
            baos.write(salt, 0, SALT_LENGTH); // 16 bytes long
            baos.write(generatedKey, 0, GENERATED_KEY_LENGTH); // 16 bytes long
            for (int i = 0; i < maximumRecords; i++) {
                baos.write(records[i], 0, RECORD_SIZE);
            } // size: maximumRecords * RECORD_SIZE, e.g. 10 * 16 = 160 bytes long
            calcBasis = baos.toByteArray(); // if maximumRecords = 10: 196 bytes long
            MessageDigest digest = null;
            digest = MessageDigest.getInstance(SHA_HASH_ALGORITHM);
            return digest.digest(calcBasis);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * section for getter
     */

    public byte getFileNumber() {
        return fileNumber;
    }

    public byte[] getSalt() {
        return salt;
    }

    public int getNUMBER_OF_PBKDF2_ITERATIONS() {
        return NUMBER_OF_PBKDF2_ITERATIONS;
    }

    public String getPBKDF2_ALGORITHM() {
        return PBKDF2_ALGORITHM;
    }

    public String getSHA_HASH_ALGORITHM() {
        return SHA_HASH_ALGORITHM;
    }

    public int getSALT_LENGTH() {
        return SALT_LENGTH;
    }

    public int getGENERATED_KEY_LENGTH() {
        return GENERATED_KEY_LENGTH;
    }

    public int getCHECKSUM_LENGTH() {
        return CHECKSUM_LENGTH;
    }

    public byte[] getGeneratedKey() {
        return generatedKey;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public int getMAXIMUM_NUMBER_OF_RECORDS() {
        return MAXIMUM_NUMBER_OF_RECORDS;
    }

    public int getRECORD_SIZE() {
        return RECORD_SIZE;
    }

    public byte getMaximumRecords() {
        return maximumRecords;
    }

    public byte[][] getRecords() {
        return records;
    }

    public byte getLastRecord() {
        return lastRecord;
    }

    public byte getNumberOfRecords() {
        return numberOfRecords;
    }

    public boolean isVirtualCyclicRecordFileValid() {
        return isVirtualCyclicRecordFileValid;
    }

}
