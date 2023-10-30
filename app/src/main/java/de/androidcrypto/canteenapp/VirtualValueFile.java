package de.androidcrypto.canteenapp;


import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * The class provides access to a Virtual Value File that acts like a real Value File (e.g. available on
 * DESFire Light or DESFire EVx tags.
 * <p>
 * You can credit or debit the value and get the balance.
 * All writing access is secured by a key using a key derivation (PBKDF2, 10000 iterations, 16 bytes
 * long resulting key length).
 * All data is secured by a 12 bytes long checksum based on a SHA-256 hash calculation.
 */
public class VirtualValueFile {
    private static final String TAG = VirtualValueFile.class.getName();
    private byte fileNumber;
    private byte[] salt;
    private final int NUMBER_OF_PBKDF2_ITERATIONS = 10000;
    private final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private final String SHA_HASH_ALGORITHM = "SHA-256";
    private final int SALT_LENGTH = 16;
    private final int GENERATED_KEY_LENGTH = 16;
    private final int CHECKSUM_LENGTH = 11;
    private final int BALANCE_LENGTH = 4;
    private byte[] generatedKey;
    private int balance;
    private byte[] balance_ByteArray;
    private byte[] checksum;
    private boolean isVirtualValueFileValid = false;

    public VirtualValueFile(byte fileNumber, byte[] fileKey) {
        if (fileKey == null) {
            Log.e(TAG, "fileKey is NULL, aborted");
            return;
        }
        this.fileNumber = fileNumber;
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
                isVirtualValueFileValid = false;
                return;
            } else {
                checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
            }
            isVirtualValueFileValid = true;
            Log.i(TAG, "VirtualValueFile is active for fileNumber " + Utils.byteToHex(fileNumber));
            Log.d(TAG, Utils.printData("balance_b", balance_ByteArray));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Error on constructing the class, aborted. fileNumber: " + Utils.byteToHex(fileNumber));
            isVirtualValueFileValid = false;
        }
    }

    public VirtualValueFile(byte[] exportedVvf) {
        if (exportedVvf == null) {
            Log.e(TAG, "exportedVvf is NULL, aborted");
            return;
        }
        if (exportedVvf.length != 48) {
            Log.e(TAG, "exportedVvf length is not 48, aborted");
            return;
        }
        fileNumber = exportedVvf[0];
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
            isVirtualValueFileValid = false;
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
        isVirtualValueFileValid = true;
        Log.i(TAG, "VirtualValueFile is active for fileNumber " + Utils.byteToHex(fileNumber));
    }

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
            isVirtualValueFileValid = false;
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
            isVirtualValueFileValid = false;
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
            isVirtualValueFileValid = false;
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
            isVirtualValueFileValid = false;
            return false;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
        return true;
    }

    public byte[] exportVvf() {
        byte[] export = new byte[48];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(fileNumber); // 1  byte long
        baos.write(salt, 0, SALT_LENGTH); // 16 bytes long
        baos.write(generatedKey, 0, GENERATED_KEY_LENGTH); // 16 bytes long
        baos.write(balance_ByteArray, 0, BALANCE_LENGTH); // 4 bytes long
        baos.write(checksum, 0, CHECKSUM_LENGTH); // 11 bytes long
        export = baos.toByteArray(); // 48 bytes long
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
            baos.write(fileNumber); // 1  byte long
            baos.write(salt, 0, SALT_LENGTH); // 16 bytes long
            baos.write(generatedKey, 0, GENERATED_KEY_LENGTH); // 16 bytes long
            baos.write(balance_ByteArray, 0, BALANCE_LENGTH); // 4 bytes long
            calcBasis = baos.toByteArray(); // 37 bytes long
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

    public int getBALANCE_LENGTH() {
        return BALANCE_LENGTH;
    }

    public byte[] getGeneratedKey() {
        return generatedKey;
    }

    public int getBalance() {
        return balance;
    }

    public byte[] getBalance_ByteArray() {
        return balance_ByteArray;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public boolean isVirtualValueFileValid() {
        return isVirtualValueFileValid;
    }
}
