package de.androidcrypto.canteenapp;


import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * The class is a placeholder for some data referring to the card holder:
 * a) card holders name (16 bytes)
 * b) card ID (16 bytes)
 * c) reserved data (16 bytes)
 * All data is secured by a 16 bytes long checksum based on a SHA-256 hash calculation.
 * So the data has a total length of 64 bytes
 */
public class VirtualIdentificationFile {
    private static final String TAG = VirtualIdentificationFile.class.getName();
    private byte[] cardholderName;
    private byte[] cardId;
    private byte[] reserved;
    private final String SHA_HASH_ALGORITHM = "SHA-256";
    private final int CHECKSUM_LENGTH = 16;
    private final int CARDHOLDER_NAME_LENGTH = 16;
    private final int CARD_ID_LENGTH = 16;
    private final int RESERVED_LENGTH = 16;
    private byte[] checksum;
    private boolean isVirtualIdentificationFileValid = false;

    public VirtualIdentificationFile() {
        cardholderName = new byte[CARDHOLDER_NAME_LENGTH];
        cardId = new byte[CARD_ID_LENGTH];
        reserved = new byte[RESERVED_LENGTH];
        checksum = new byte[CHECKSUM_LENGTH];
        byte[] chkSum = calculateChecksum();
        if (chkSum == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualIdentificationFileValid = false;
            return;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
        isVirtualIdentificationFileValid = true;
        Log.i(TAG, "VirtualIdentificationFile is active");
    }

    public VirtualIdentificationFile(byte[] exportedVif) {
        if (exportedVif == null) {
            Log.e(TAG, "exportedVif is NULL, aborted");
            return;
        }
        if (exportedVif.length != 64) {
            Log.e(TAG, "exportedVif length is not 64, aborted");
            return;
        }
        cardholderName = Arrays.copyOfRange(exportedVif, 0, 16);
        cardId = Arrays.copyOfRange(exportedVif, 16, 32);
        reserved = Arrays.copyOfRange(exportedVif, 32, 48);
        checksum = Arrays.copyOfRange(exportedVif, 48, 64);
        // verify the checksum
        byte[] chkSumOld = calculateChecksum();
        if (chkSumOld == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualIdentificationFileValid = false;
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
        isVirtualIdentificationFileValid = true;
        Log.i(TAG, "VirtualValueFile is active");
    }

    public void setCardholderName(byte[] cardholderName) {
        // sanity checks
        if ((cardholderName == null) || (cardholderName.length == 0)) cardholderName = "John Doe".getBytes(StandardCharsets.UTF_8);
        // fill up the data
        System.arraycopy(cardholderName, 0, this.cardholderName, 0, cardholderName.length);
        byte[] chkSum = calculateChecksum();
        if (chkSum == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualIdentificationFileValid = false;
            return;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
    }

    public void setCardId(byte[] cardId) {
        // sanity checks
        if ((cardId == null) || (cardId.length == 0)) cardId = "1234567890123456".getBytes(StandardCharsets.UTF_8);
        // fill up the data
        System.arraycopy(cardId, 0, this.cardId, 0, cardId.length);
        byte[] chkSum = calculateChecksum();
        if (chkSum == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualIdentificationFileValid = false;
            return;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
    }

    public void setReserved(byte[] reserved) {
        // sanity checks
        if ((reserved == null) || (reserved.length == 0)) reserved = new byte[RESERVED_LENGTH];
        // fill up the data
        System.arraycopy(reserved, 0, this.reserved, 0, reserved.length);
        byte[] chkSum = calculateChecksum();
        if (chkSum == null) {
            Log.e(TAG, "checksum is invalid, aborted");
            checksum = null;
            isVirtualIdentificationFileValid = false;
            return;
        } else {
            checksum = Arrays.copyOf(chkSum, CHECKSUM_LENGTH);
        }
    }

    public byte[] exportVif() {
        byte[] export = new byte[64];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(cardholderName, 0, cardholderName.length); // 16  bytes long
        baos.write(cardId, 0, cardId.length); // 16 bytes long
        baos.write(reserved, 0, reserved.length); // 16 bytes long
        baos.write(checksum, 0, checksum.length); // 16 bytes long
        export = baos.toByteArray(); // 64 bytes long
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
            byte[] calcBasis = new byte[48];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(cardholderName, 0, cardholderName.length); // 16  bytes long
            baos.write(cardId, 0, cardId.length); // 16 bytes long
            baos.write(reserved, 0, reserved.length); // 16 bytes long
            calcBasis = baos.toByteArray(); // 48 bytes long
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

    public byte[] getCardholderName() {
        return cardholderName;
    }

    public byte[] getCardId() {
        return cardId;
    }

    public byte[] getReserved() {
        return reserved;
    }

    public String getSHA_HASH_ALGORITHM() {
        return SHA_HASH_ALGORITHM;
    }

    public int getCHECKSUM_LENGTH() {
        return CHECKSUM_LENGTH;
    }

    public int getCARDHOLDER_NAME_LENGTH() {
        return CARDHOLDER_NAME_LENGTH;
    }

    public int getCARD_ID_LENGTH() {
        return CARD_ID_LENGTH;
    }

    public int getRESERVED_LENGTH() {
        return RESERVED_LENGTH;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public boolean isVirtualIdentificationFileValid() {
        return isVirtualIdentificationFileValid;
    }
}
