package de.androidcrypto.canteenapp;

public class Constants {

    /**
     * section for key management
     * A NTAG424DNA tag can have up to 5 different keys for the one available application
     */

    public final static byte[] defaultApplicationKey = Utils.hexStringToByteArray("00000000000000000000000000000000");
    public final static byte[] applicationKey0 = Utils.hexStringToByteArray("a0000000000000000000000000000000");
    public final static byte[] applicationKey1 = Utils.hexStringToByteArray("a1111111111111111111111111111111");
    public final static byte[] applicationKey2 = Utils.hexStringToByteArray("a2222222222222222222222222222222");
    public final static byte[] applicationKey3 = Utils.hexStringToByteArray("a3333333333333333333333333333333");
    public final static byte[] applicationKey4 = Utils.hexStringToByteArray("a4444444444444444444444444444444");
    public final static byte applicationKeyNumber0 = (byte) 0x00;
    public final static byte applicationKeyNumber1 = (byte) 0x01;
    public final static byte applicationKeyNumber2 = (byte) 0x02;
    public final static byte applicationKeyNumber3 = (byte) 0x03;
    public final static byte applicationKeyNumber4 = (byte) 0x04;
    public final int applicationKeyVersion = 1;


    public static final String UID_HEADER = "?";
    public static String UID_NAME = ""; // written by NdefSettingsFragment and read from preferences
    public static final String UID_FOOTER = "=";
    public static final String UID_TEMPLATE = "11111111111111";

    public static final String MAC_HEADER = "&";
    public static String MAC_NAME = ""; // written by NdefSettingsFragment and read from preferences
    public static final String MAC_FOOTER = "=";
    public static final String MAC_TEMPLATE = "22222222";

    public static final String NDEF_BASE_URL_TEMPLATE = "http://fluttercrypto.bplaced.net/apps/ntag/get_reg3.php";
    public static final int NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH = 137; // maximum length of an NDEF message on a NTAG213
    public static final String PREFS_NAME = "prefs";
    public static final String PREFS_BASE_URL = "baseUrl";
    public static final String PREFS_UID_NAME = "uid";
    public static final String PREFS_MAC_NAME = "mac";
    public static final String PREFS_TEMPLATE_URL_NAME = "templateUrl";

    /**
     * The following data is used for disabling write access to the tags by setting the write access disabling
     * The tag is secured from the beginning of the 'Capability Container' up to the end
     * I'm using fixed/static data here to prevent from loss of tag access due to app deletion for
     * password and pack values
     * For security reasons consider of randomized data or data derived from a master key and the tag's UID
     */

    public static final byte[] TAG_PASSWORD = Utils.hexStringToByteArray("98765432");
    public static final byte[] TAG_PACK = Utils.hexStringToByteArray("CC00");

    public static final String PREFS_TAG_PASSWORD_NAME = "tagpassword";
    public static final String PREFS_TAG_PACK_NAME = "tagpack";
}
