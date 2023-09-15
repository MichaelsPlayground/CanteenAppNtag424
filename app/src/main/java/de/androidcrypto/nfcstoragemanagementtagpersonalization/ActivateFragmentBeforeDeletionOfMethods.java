package de.androidcrypto.nfcstoragemanagementtagpersonalization;

import static android.content.Context.MODE_PRIVATE;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Constants.*;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Utils.doVibrate;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Utils.testBit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ActivateFragmentBeforeDeletionOfMethods#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ActivateFragmentBeforeDeletionOfMethods extends Fragment implements NfcAdapter.ReaderCallback {

    private static final String TAG = ActivateFragmentBeforeDeletionOfMethods.class.getName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    com.google.android.material.textfield.TextInputEditText resultNfcWriting;
    RadioButton rbActivateGetStatus, rbActivateOn, rbActivateOff;

    Ntag21xMethods ntagMethods;
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;

    private int identifiedNtagConfigurationPage; // this  is the stating point for any work on configuration
    private byte[] tagUid; // written by onDiscovered

    public ActivateFragmentBeforeDeletionOfMethods() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ActivateFragmentBeforeDeletionOfMethods newInstance(String param1, String param2) {
        ActivateFragmentBeforeDeletionOfMethods fragment = new ActivateFragmentBeforeDeletionOfMethods();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_activate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        resultNfcWriting = getView().findViewById(R.id.etActivateResult);
        rbActivateGetStatus = getView().findViewById(R.id.rbActivateShowStatus);
        rbActivateOn = getView().findViewById(R.id.rbActivateOn);
        rbActivateOff = getView().findViewById(R.id.rbActivateOff);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());
        ntagMethods = new Ntag21xMethods(getActivity(), resultNfcWriting);
    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {
// Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        nfcA = NfcA.get(tag);
        try {
            nfcA = NfcA.get(tag);

            if (nfcA != null) {
                writeToUiAppend(resultNfcWriting,"NFC tag is Nfca compatible");
                nfcA.connect();
                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if ((!ntagVersion.equals("213")) && (!ntagVersion.equals("215")) && (!ntagVersion.equals("216"))) {
                    writeToUiAppend(resultNfcWriting,"NFC tag is NOT of type NXP NTAG213/215/216, aborted");
                    return;
                }

                // tagUid
                tagUid = nfcA.getTag().getId();

                int nfcaMaxTranceiveLength = nfcA.getMaxTransceiveLength(); // important for the readFast command
                Log.d(TAG, "nfcaMaxTranceiveLength: " + nfcaMaxTranceiveLength);
                int ntagPages = NfcIdentifyNtag.getIdentifiedNtagPages();
                identifiedNtagConfigurationPage = NfcIdentifyNtag.getIdentifiedNtagConfigurationPage();
                writeToUiAppend(resultNfcWriting, "The configuration is starting in page " + identifiedNtagConfigurationPage);
                int ntagMemoryBytes = NfcIdentifyNtag.getIdentifiedNtagMemoryBytes();
                String tagIdString = Utils.getDec(tag.getId());
                String nfcaContent = "raw data of " + NfcIdentifyNtag.getIdentifiedNtagType() + "\n" +
                        "number of pages: " + ntagPages +
                        " total memory: " + ntagMemoryBytes +
                        " bytes\n" +
                        "tag ID: " + Utils.bytesToHexNpe(NfcIdentifyNtag.getIdentifiedNtagId()) + "\n" +
                        "tag ID: " + tagIdString + "\n";
                nfcaContent = nfcaContent + "maxTranceiveLength: " + nfcaMaxTranceiveLength + " bytes\n";
                writeToUiAppend(resultNfcWriting, nfcaContent);
                // read the complete memory depending on ntag type
                byte[] ntagMemory = new byte[ntagMemoryBytes];
                // read the content of the tag in several runs
                byte[] response = new byte[0];

                boolean isGetActivateStatus = rbActivateGetStatus.isChecked();
                boolean isActivateOn = rbActivateOn.isChecked();
                boolean isActivateOff = rbActivateOff.isChecked();

                if (isGetActivateStatus) {
                    //response = getStatusUidMirrorNdef(nfcA);

                    // read the complete ndef message and find the placeholder
                    // as I'm limiting the maximum ndef message we can read all data in one run
                    // build the matching strings for ud and mac

                    // read the content of the the tag to find the match strings, for this we are reading the complete NDEF content
                    int maximumBytesToRead = NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH + 7; // + 7 NDEF header bytes, so it total 144 bytes
                    //ntagMemory = readNdefContent(nfcA, maximumBytesToRead, nfcaMaxTranceiveLength);
                    ntagMemory = ntagMethods.readNdefContent(nfcA, maximumBytesToRead, nfcaMaxTranceiveLength);
                    if ((ntagMemory == null) || (ntagMemory.length < 10)) {
                        writeToUiAppend(resultNfcWriting, "Error - could not read enough data from tag, aborted");
                        return;
                    }
                    String ntagDataString = new String(ntagMemory, StandardCharsets.UTF_8);
                    writeToUiAppend(resultNfcWriting, "ntagDataString:\n" + ntagDataString);

                    // read the placeholder names from the shared preferences
                    String uidMatchString = getPreferencesMatchString(PREFS_UID_NAME, UID_HEADER, UID_FOOTER);
                    String macMatchString = getPreferencesMatchString(PREFS_MAC_NAME, MAC_HEADER, MAC_FOOTER);

                    // search for match strings and add length of match string for the next position to write the data
                    int positionUidMatch = getPlaceholderPosition(ntagDataString, uidMatchString);
                    int positionMacMatch = getPlaceholderPosition(ntagDataString, macMatchString);
                    // both values need to be > 1
                    writeToUiAppend(resultNfcWriting, "positionUidMatch: " + positionUidMatch + " || positionMacMatch: " + positionMacMatch);
                    if ((positionUidMatch < 1) || (positionMacMatch < 1)) {
                        writeToUiAppend(resultNfcWriting, "Error - insufficient positions found, aborted");;
                    } else {
                        writeToUiAppend(resultNfcWriting, "positive match positions, now checking enabled mirroring");
                    }

                    // now we are reading the configuration
                    //int mirrorPosition = checkUidMirrorStatus(nfcA, identifiedNtagConfigurationPage);
                    int mirrorPosition = ntagMethods.checkUidMirrorStatus(nfcA, identifiedNtagConfigurationPage);
                    writeToUiAppend(resultNfcWriting, "position of UID mirror: " + mirrorPosition);

                    if (response == null) {
                        writeToUiAppend(resultNfcWriting, "status of the UID mirror: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(resultNfcWriting, "status of the UID mirror: SUCCESS - code: " + Utils.bytesToHexNpe(response));
                    }

                }
                if (isActivateOn) {
                    // for activating we need to find the positions where to place the mirror data
                    // read the content of the the tag to find the match strings, for this we are reading the complete NDEF content
                    // note that this can fail is a mirror is active on the tag that overwrites the placeholders
                    int maximumBytesToRead = NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH + 7; // + 7 NDEF header bytes, so it total 144 bytes
                    //ntagMemory = readNdefContent(nfcA, maximumBytesToRead, nfcaMaxTranceiveLength);
                    ntagMemory = ntagMethods.readNdefContent(nfcA, maximumBytesToRead, nfcaMaxTranceiveLength);
                    if ((ntagMemory == null) || (ntagMemory.length < 10)) {
                        writeToUiAppend(resultNfcWriting, "Error - could not read enough data from tag, aborted");
                        return;
                    }
                    String ntagDataString = new String(ntagMemory, StandardCharsets.UTF_8);
                    writeToUiAppend(resultNfcWriting, "ntagDataString:\n" + ntagDataString);

                    // read the placeholder names from the shared preferences
                    String uidMatchString = getPreferencesMatchString(PREFS_UID_NAME, UID_HEADER, UID_FOOTER);
                    String macMatchString = getPreferencesMatchString(PREFS_MAC_NAME, MAC_HEADER, MAC_FOOTER);

                    // search for match strings and add length of match string for the next position to write the data
                    int positionUidMatch = getPlaceholderPosition(ntagDataString, uidMatchString);
                    int positionMacMatch = getPlaceholderPosition(ntagDataString, macMatchString);
                    // both values need to be > 1
                    writeToUiAppend(resultNfcWriting, "positionUidMatch: " + positionUidMatch + " || positionMacMatch: " + positionMacMatch);
                    if ((positionUidMatch < 1) || (positionMacMatch < 1)) {
                        writeToUiAppend(resultNfcWriting, "Error - insufficient positions found, aborted");;
                        return;
                    } else {
                        writeToUiAppend(resultNfcWriting, "positive match positions, now enable mirroring");
                    }

                    //boolean resultEnable = enableUidMirror(nfcA, identifiedNtagConfigurationPage, positionUidMatch);
                    boolean resultEnable = ntagMethods.enableUidMirror(nfcA, identifiedNtagConfigurationPage, positionUidMatch);
                    if (!resultEnable) {
                        writeToUiAppend(resultNfcWriting, "Enabling the UID mirror: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(resultNfcWriting, "Enabling the UID mirror: SUCCESS");
                    }
                    // the mac is calculated from uid using SHA-256 and shortened to 8 bytes length

                    byte[] shortenedHash = ntagMethods.getUidHashShort(tagUid);
                    writeToUiAppend(resultNfcWriting, "tagUid: " + Utils.bytesToHexNpe(tagUid));
                    writeToUiAppend(resultNfcWriting, "shortenedMAC: " + Utils.bytesToHexNpe(shortenedHash));
                    // tagUid:    04437d82355b80
                    // shortened: a2ae56c7

                    // write mac to ndef
                    boolean resultMac = ntagMethods.writeMacToNdef(nfcA, identifiedNtagConfigurationPage, shortenedHash, positionMacMatch);
                    if (!resultMac) {
                        writeToUiAppend(resultNfcWriting, "writing the MAC: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(resultNfcWriting, "writing the MAC: SUCCESS");
                    }
                }
                if (isActivateOff) {
                    //boolean resultEnable = disableAllMirror(nfcA, identifiedNtagConfigurationPage);
                    boolean resultEnable = ntagMethods.disableAllMirror(nfcA, identifiedNtagConfigurationPage);
                    if (!resultEnable) {
                        writeToUiAppend(resultNfcWriting, "Disabling ALL mirror: FAILURE");
                        return;
                    } else {
                        writeToUiAppend(resultNfcWriting, "Disabling ALL mirror: SUCCESS");
                    }
                }


            }
        } catch (IOException e) {
            writeToUiAppend(resultNfcWriting, "ERROR: IOException " + e.toString());
            e.printStackTrace();
        } finally {
        try {
            nfcA.close();
        } catch (IOException e) {
            writeToUiAppend(resultNfcWriting, "ERROR: IOException " + e.toString());
            e.printStackTrace();
        }
    }
        doVibrate(getActivity());
    }

    private String getPreferencesMatchString(String preferenceName, String preferenceHeader, String preferenceFooter) {
        String preference = "";
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            preference = prefs.getString(preferenceName, null);
            if ((preference == null) || (preference.length() < 1)) {
                writeToUiAppend(resultNfcWriting, "Please setup the NDEF settings, aborted");
                return "";
            }
        } catch (NullPointerException e) {
            writeToUiAppend(resultNfcWriting, "Please setup the NDEF settings, aborted");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        // uid
        sb.append(preferenceHeader);
        sb.append(preference);
        sb.append(preferenceFooter);
        String preferenceString = sb.toString();
        writeToUiAppend(resultNfcWriting, "preferenceString: " + preferenceString);
        return preferenceString;
    }


    private int getPlaceholderPosition(String content, String matchString) {
        return content.indexOf(matchString) + matchString.length();
    }

    /**
     * read the content of the user memory up to 'numberOfBytes' - this is because the maximum NDEF length
     * got defined in NdefSettingsFragment
     * The content is used to find matching strings with UID and/or MAC
     * Note: if any mirror is enabled on the tag the returned content is the VIRTUAL content including
     * the mirror content, not the REAL content written in pages !
     *
     * @param nfcA
     * @param numberOfBytesToRead
     * @return
     */
    private byte[] readNdefContentOld(NfcA nfcA, int numberOfBytesToRead, int nfcaMaxTranceiveLength ) {
        int maximumBytesToRead = NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH + 7; // + 7 NDEF header bytes, so it total 144 bytes
        int nfcaMaxTranceive4ByteTrunc = nfcaMaxTranceiveLength / 4; // 63
        int nfcaMaxTranceive4ByteLength = nfcaMaxTranceive4ByteTrunc * 4; // 252 bytes
        int nfcaNrOfFullReadings = maximumBytesToRead / nfcaMaxTranceive4ByteLength;
        int nfcaTotalFullReadingBytes = nfcaNrOfFullReadings * nfcaMaxTranceive4ByteLength; // 3 * 252 = 756
        int nfcaMaxTranceiveModuloLength = maximumBytesToRead - nfcaTotalFullReadingBytes; // 888 bytes - 756 bytes = 132 bytes
        String nfcaContent = "";
        nfcaContent = nfcaContent + "nfcaMaxTranceive4ByteTrunc: " + nfcaMaxTranceive4ByteTrunc + "\n";
        nfcaContent = nfcaContent + "nfcaMaxTranceive4ByteLength: " + nfcaMaxTranceive4ByteLength + "\n";
        nfcaContent = nfcaContent + "nfcaNrOfFullReadings: " + nfcaNrOfFullReadings + "\n";
        nfcaContent = nfcaContent + "nfcaTotalFullReadingBytes: " + nfcaTotalFullReadingBytes + "\n";
        nfcaContent = nfcaContent + "nfcaMaxTranceiveModuloLength: " + nfcaMaxTranceiveModuloLength + "\n";

        byte[] response;
        byte[] ntagMemory = new byte[numberOfBytesToRead];
        try {
        // first round
        for (int i = 0; i < nfcaNrOfFullReadings; i++) {
            //nfcaContent = nfcaContent + "starting round: " + i + "\n";
            System.out.println("starting round: " + i);
            byte[] commandF = new byte[]{
                    (byte) 0x3A,  // FAST_READ
                    (byte) ((4 + (nfcaMaxTranceive4ByteTrunc * i)) & 0x0ff), // page 4 is the first user memory page
                    (byte) ((4 + (nfcaMaxTranceive4ByteTrunc * (i + 1)) - 1) & 0x0ff)
            };
            //nfcaContent = nfcaContent + "i: " + i + " commandF: " + bytesToHex(commandF) + "\n";
            response = nfcA.transceive(commandF);
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                // Log and return
                nfcaContent = nfcaContent + "ERROR: null response";
                String finalNfcaText = nfcaContent;
                writeToUiAppend(resultNfcWriting, finalNfcaText);
                System.out.println(finalNfcaText);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                nfcaContent = nfcaContent + "ERROR: NACK response: " + Utils.bytesToHexNpe(response);
                String finalNfcaText = nfcaContent;
                writeToUiAppend(resultNfcWriting, finalNfcaText);
                System.out.println(finalNfcaText);
                return null;
            } else {
                // success: response contains ACK or actual data
                System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * i), nfcaMaxTranceive4ByteLength);
            }
        } // for
        // now we read the nfcaMaxTranceiveModuloLength bytes, for a NTAG216 = 132 bytes
        //nfcaContent = nfcaContent + "starting last round: " + "\n";
        //System.out.println("starting last round: ");
        byte[] commandF = new byte[]{
                (byte) 0x3A,  // FAST_READ
                (byte) ((4 + (nfcaMaxTranceive4ByteTrunc * nfcaNrOfFullReadings)) & 0x0ff), // page 4 is the first user memory page
                (byte) ((4 + (nfcaMaxTranceive4ByteTrunc * nfcaNrOfFullReadings) + (nfcaMaxTranceiveModuloLength / 4) & 0x0ff))
        };
        //nfcaContent = nfcaContent + "last: " + " commandF: " + bytesToHex(commandF) + "\n";
        response = nfcA.transceive(commandF);
        if (response == null) {
            // either communication to the tag was lost or a NACK was received
            // Log and return
            nfcaContent = nfcaContent + "ERROR: null response";
            String finalNfcaText = nfcaContent;
            writeToUiAppend(resultNfcWriting, finalNfcaText);
            System.out.println(finalNfcaText);
            return null;
        } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
            // NACK response according to Digital Protocol/T2TOP
            // Log and return
            nfcaContent = nfcaContent + "ERROR: NACK response: " + Utils.bytesToHexNpe(response);
            String finalNfcaText = nfcaContent;
            //writeToUiAppend(resultNfcWriting, finalNfcaText);
            System.out.println(finalNfcaText);
            return null;
        } else {
            // success: response contains ACK or actual data
            System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * nfcaNrOfFullReadings), nfcaMaxTranceiveModuloLength);
        }
        } catch (IOException e) {
            writeToUiAppend(resultNfcWriting, "ERROR: IOException " + e.toString());
            e.printStackTrace();
            return null;
        }
        return ntagMemory;
    }

    /**
     * The bit for enabling or disabling the uid mirror is in page 41/131/227 (0x29/0x83/0xE3),
     * depending on the tag type
     *
     * byte 0 of this pages holds the MIRROR byte
     * byte 2 of this pages holds the MIRROR_PAGE byte
     *
     * Mirror byte has these flags
     * bits 6+7 define which mirror shall be used:
     *   00b = no ASCII mirror
     *   01b = Uid ASCII mirror
     *   10b = NFC counter ASCII mirror
     *   11b = Uid and NFC counter ASCII mirror
     * bits 4+5 define the byte position within the page defined in MIRROR_PAGE byte
     *
     * MIRROR_PAGE byte defines the start of mirror.
     *
     * It is import that the end of mirror is within the user memory. These lengths apply:
     * Uid mirror: 14 bytes (UID length is 7 * 2 for hex ascii string encoding)
     * NFC counter mirror: 6 bytes (counter length is 3 * 2 for hex ascii string encoding)
     * Uid + NFC counter mirror: 21 bytes (14 bytes for Uid and 1 byte separation + 6 bytes counter value
     * Separator is 'x' (0x78)
     *
     */

    /**
     * Checks the enabled or disabled UID mirroring on the tag and returns the mirror position
     * in the user memory
     * @param nfca
     * @param pageOfConfiguration page in ntag where configuration is starting, depending on NTAG sub type 213/215/216
     * @return uid mirror position
     * return is the position of UID mirror or '-1' if uid mirror is disabled or errors applied in parameters
     */
    private int checkUidMirrorStatusOld(NfcA nfca, int pageOfConfiguration) {
        // sanity checks
        if ((nfca == null) || (!nfca.isConnected())) {
            writeToUiAppend(resultNfcWriting, "NfcA is not available for reading, aborted");
            return -1;
        }
        if ((pageOfConfiguration != 41) && (pageOfConfiguration != 131) && (pageOfConfiguration != 227)) {
            writeToUiAppend(resultNfcWriting, "wrong parameter for pageOfConfiguration, aborted");
            return -1;
        }
        // read configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, identifiedNtagConfigurationPage);
        int position = -1;
        if ((readPageResponse != null) && (readPageResponse.length > 2)) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(resultNfcWriting, "mirrorPageByte: " + Utils.byteToHex(mirrorPageByte) + " (= " + (int) mirrorPageByte + " dec)");
            writeToUiAppend(resultNfcWriting, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));
            // check that ONLY the UID mirror is active, NOT the COUNTER mirror
            boolean isUidMirror, isCounterMirror, isMirrorBit4, isMirrorBit5;
            isUidMirror = testBit(mirrorByte, 6);
            isCounterMirror = testBit(mirrorByte, 7);
            isMirrorBit4 = testBit(mirrorByte, 4);
            isMirrorBit5 = testBit(mirrorByte, 5);
            writeToUiAppend(resultNfcWriting, "isUidMirror: " + isUidMirror + " || isCounterMirror: " + isCounterMirror);
            writeToUiAppend(resultNfcWriting, "isMirrorBit4: " + isMirrorBit4 + " || isMirrorBit5: " + isMirrorBit5);
            if (isCounterMirror) {
                writeToUiAppend(resultNfcWriting, "the COUNTER mirror is enabled, aborted");
                return position;
            }
            if (!isUidMirror) {
                writeToUiAppend(resultNfcWriting, "the UID mirror is disabled, aborted");
                return position;
            }
            // at this point only the UID mirror is enabled, find the relative position in user memory
            position = ((int) mirrorPageByte - 4) * 4; // 4 header pages are subtracted
            // now we need to add the position within the page, this is encoded in mirrorByte -> mirrorBits 4 + 5
            position += (isMirrorBit4 ? 1 : 0) + (isMirrorBit5 ? 2 : 0);
            return position;
        } else {
            writeToUiAppend(resultNfcWriting, "something gone wrong, aborted");
            return position;
        }
    }

    /**
     * enables the UID mirror and sets the mirror position
     * if Counter mirror should be enabled before it gets disabled
     * @param nfca
     * @param pageOfConfiguration
     * @param positionOfUid : the relative position within the user memory, so starting with 0
     * @return true on success
     */

    private boolean enableUidMirrorOld(NfcA nfca, int pageOfConfiguration, int positionOfUid) {
        // sanity checks
        if ((nfca == null) || (!nfca.isConnected())) {
            writeToUiAppend(resultNfcWriting, "NfcA is not available for reading, aborted");
            return false;
        }
        if ((pageOfConfiguration != 41) && (pageOfConfiguration != 131) && (pageOfConfiguration != 227)) {
            writeToUiAppend(resultNfcWriting, "wrong parameter for pageOfConfiguration, aborted");
            return false;
        }
        // read configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, pageOfConfiguration);
        int position = -1;
        if ((readPageResponse != null) && (readPageResponse.length > 2)) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(resultNfcWriting, "mirrorPageByte: " + Utils.byteToHex(mirrorPageByte) + " (= " + (int) mirrorPageByte + " dec)");
            writeToUiAppend(resultNfcWriting, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));

            // this will activate the UID mirror and DEactivate an activated Counter mirror
            byte mirrorByteNew;
            // unsetting bit 7 = counter, we are doing UID mirroring only
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 6);

            // now we are converting the relative position of UID mirror in 'page' and 'position in page'
            int newMirrorPage = 4 + (positionOfUid / 4); // NTAG 21x has 4 header pages
            writeToUiAppend(resultNfcWriting, "newPage: " + newMirrorPage);
            int positionInPage = (positionOfUid / 4) - ((newMirrorPage - 4) * 4);
            writeToUiAppend(resultNfcWriting, "positionInPage: " + positionInPage);
            // set the bits depending on positionÍnPage - this could be more elegant but...
            if (positionInPage == 0) {
                mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
                mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            } else if (positionInPage == 1) {
                mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 4);
                mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            } else if (positionInPage == 2) {
                mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
                mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 5);
            } else if (positionInPage == 3) {
                mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 4);
                mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 5);
            }
            // as the formula result in values of 0..3 we do not need a final 'else'
            // this is setting the full page
            byte mirrorPageByteNew = (byte) (newMirrorPage & 0x0ff);
            // now copy the new contents to readResponse
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageByteNew;
            writeToUiAppend(resultNfcWriting, "readPageResponse: " + Utils.bytesToHexNpe(readPageResponse));
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, pageOfConfiguration, readPageResponse);
            writeToUiAppend(resultNfcWriting, "write page to tag: " + Utils.bytesToHexNpe(writePageResponse));
            if (writePageResponse != null) {
                writeToUiAppend(resultNfcWriting, "SUCCESS: enabling the UID mirror with response: " + Utils.bytesToHexNpe(writePageResponse));
                return true;
            } else {
                writeToUiAppend(resultNfcWriting, "FAILURE: no enabling of the UID mirror");
                return false;
            }
        } else {
            writeToUiAppend(resultNfcWriting, "something gone wrong, aborted");
            return false;
        }
    }

    /**
     * disables ALL mirrors whether they are set or not and resets the position to facory settings
     * @param nfca
     * @param pageOfConfiguration
     * @return true on success
     */

    private boolean disableAllMirrorOld(NfcA nfca, int pageOfConfiguration) {
        // sanity checks
        if ((nfca == null) || (!nfca.isConnected())) {
            writeToUiAppend(resultNfcWriting, "NfcA is not available for reading, aborted");
            return false;
        }
        if ((pageOfConfiguration != 41) && (pageOfConfiguration != 131) && (pageOfConfiguration != 227)) {
            writeToUiAppend(resultNfcWriting, "wrong parameter for pageOfConfiguration, aborted");
            return false;
        }
        // read configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, pageOfConfiguration);
        int position = -1;
        if ((readPageResponse != null) && (readPageResponse.length > 2)) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(resultNfcWriting, "mirrorPageByte: " + Utils.byteToHex(mirrorPageByte) + " (= " + (int) mirrorPageByte + " dec)");
            writeToUiAppend(resultNfcWriting, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));

            // this will DEactivate the UID mirror and DEactivate an activated Counter mirror as well
            byte mirrorByteNew;
            // unsetting bit 7 = counter, we are doing UID mirroring only
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 6);

            // the mirror position is reset to page 0 (outside the user memory) and positionInPage 0
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 4);
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            byte mirrorPageByteNew = (byte) (0x00);
            // now copy the new contents to readResponse
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageByteNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, pageOfConfiguration, readPageResponse);
            writeToUiAppend(resultNfcWriting, "write page to tag: " + Utils.bytesToHexNpe(writePageResponse));
            if (writePageResponse != null) {
                writeToUiAppend(resultNfcWriting, "SUCCESS: enabling the UID mirror with response: " + Utils.bytesToHexNpe(writePageResponse));
                return true;
            } else {
                writeToUiAppend(resultNfcWriting, "FAILURE: no enabling of the UID mirror");
                return false;
            }
        } else {
            writeToUiAppend(resultNfcWriting, "something gone wrong, aborted");
            return false;
        }
    }


    private byte[] getStatusUidMirrorNdefAA(NfcA nfcA) {
        /**
         * WARNING: this command is hardcoded to work with a NTAG216
         * the bit for enabling or disabling the uid mirror is in pages 41/131/227 (0x29 / 0x83 / 0xE3)
         * depending on the tag type
         *
         * byte 0 of this pages holds the MIRROR byte
         * byte 2 of this pages holds the MIRROR_PAGE byte
         *
         * Mirror byte has these flags
         * bits 6+7 define which mirror shall be used:
         *   00b = no ASCII mirror
         *   01b = Uid ASCII mirror
         *   10b = NFC counter ASCII mirror
         *   11b = Uid and NFC counter ASCII mirror
         * bits 4+5 define the byte position within the page defined in MIRROR_PAGE byte
         *
         * MIRROR_PAGE byte defines the start of mirror.
         *
         * It is import that the end of mirror is within the user memory. These lengths apply:
         * Uid mirror: 14 bytes
         * NFC counter mirror: 6 bytes
         * Uid + NFC counter mirror: 21 bytes (14 bytes for Uid and 1 byte separation + 6 bytes counter value
         * Separator is x (0x78)
         *
         * This function writes the MIRROR_PAGE and MIRROR_BYTE to the place where the WRITE NDEF MESSAGE needs it
         *
         */

        boolean isUidMirror, isCounterMirror, isMirrorBit4, isMirrorBit5;

        writeToUiAppend(resultNfcWriting, "* Start enabling the Counter mirror *");
        // read page 227 = Configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, identifiedNtagConfigurationPage);
        if (readPageResponse != null) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(resultNfcWriting, "mirrorPageByte: " + Utils.byteToHex(mirrorPageByte) + " (= " + (int) mirrorPageByte + " dec)");
            writeToUiAppend(resultNfcWriting, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));

            isUidMirror = testBit(mirrorByte, 6);
            isCounterMirror = testBit(mirrorByte, 7);
            isMirrorBit4 = testBit(mirrorByte, 4);
            isMirrorBit5 = testBit(mirrorByte, 5);
            writeToUiAppend(resultNfcWriting, "isUidMirror: " + isUidMirror + " || isCounterMirror: " + isCounterMirror);
            writeToUiAppend(resultNfcWriting, "isMirrorBit4: " + isMirrorBit4 + " || isMirrorBit5: " + isMirrorBit5);

            byte mirrorByteNew;
            // unsetting bit 7 = counter, we are doing UID mirroring only
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 6);
            // fix: start the mirror from byte 1 of the designated page, so bits are set as follows
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 4);
            writeToUiAppend(resultNfcWriting, "MIRROR content new: " + Utils.printByteBinary(mirrorByteNew));
            // set the page where the mirror is starting, we use a fixed page here:
            int setMirrorPage = 15; // 0x0F
            byte mirrorPageNew = (byte) (setMirrorPage & 0x0ff);
            // rebuild the page data
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageNew;
            return readPageResponse;
/*
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, 227, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(resultNfcWriting, "write page to tag: " + Utils.bytesToHexNpe(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only

            if (writePageResponse != null) {
                writeToUiAppend(resultNfcWriting, "SUCCESS: writing with response: " + Utils.bytesToHexNpe(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(resultNfcWriting, "FAILURE: no writing on the tag");
            }

             */
        }
        return null;
    }

    private byte[] writeEnableUidMirrorNdef(NfcA nfcA) {
        /**
         * The bit for enabling or disabling the uid mirror is in pages 41/131/227 (0x29 / 0x83 / 0xE3)
         * depending on the tag type
         *
         * byte 0 of this pages holds the MIRROR byte
         * byte 2 of this pages holds the MIRROR_PAGE byte
         *
         * Mirror byte has these flags
         * bits 6+7 define which mirror shall be used:
         *   00b = no ASCII mirror
         *   01b = Uid ASCII mirror
         *   10b = NFC counter ASCII mirror
         *   11b = Uid and NFC counter ASCII mirror
         * bits 4+5 define the byte position within the page defined in MIRROR_PAGE byte
         *
         * MIRROR_PAGE byte defines the start of mirror.
         *
         * It is import that the end of mirror is within the user memory. These lengths apply:
         * Uid mirror: 14 bytes
         * NFC counter mirror: 6 bytes
         * Uid + NFC counter mirror: 21 bytes (14 bytes for Uid and 1 byte separation + 6 bytes counter value
         * Separator is x (0x78)
         *
         * This function writes the MIRROR_PAGE and MIRROR_BYTE to the place where the WRITE NDEF MESSAGE needs it
         *
         */


        writeToUiAppend(resultNfcWriting, "* Start enabling the Counter mirror *");
        // read page 227 on NTAG226 = Configuration page 0
        byte[] readPageResponse = getTagDataResponse(nfcA, identifiedNtagConfigurationPage);
        if (readPageResponse != null) {
            // get byte 0 = MIRROR
            byte mirrorByte = readPageResponse[0];
            // get byte 2 = MIRROR_PAGE
            byte mirrorPageByte = readPageResponse[2];
            writeToUiAppend(resultNfcWriting, "MIRROR content old: " + Utils.printByteBinary(mirrorByte));
            byte mirrorByteNew;
            // unsetting bit 7 = counter, we are doing UID mirroring only
            mirrorByteNew = Utils.unsetBitInByte(mirrorByte, 7);
            // setting bit 6
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 6);
            // fix: start the mirror from byte 1 of the designated page, so bits are set as follows
            mirrorByteNew = Utils.unsetBitInByte(mirrorByteNew, 5);
            mirrorByteNew = Utils.setBitInByte(mirrorByteNew, 4);
            writeToUiAppend(resultNfcWriting, "MIRROR content new: " + Utils.printByteBinary(mirrorByteNew));
            // set the page where the mirror is starting, we use a fixed page here:
            int setMirrorPage = 15; // 0x0F
            byte mirrorPageNew = (byte) (setMirrorPage & 0x0ff);
            // rebuild the page data
            readPageResponse[0] = mirrorByteNew;
            readPageResponse[2] = mirrorPageNew;
            // write the page back to the tag
            byte[] writePageResponse = writeTagDataResponse(nfcA, identifiedNtagConfigurationPage, readPageResponse); // this is for NTAG216 only
            writeToUiAppend(resultNfcWriting, "write page to tag: " + Utils.bytesToHexNpe(readPageResponse));
            //byte[] writePageResponse = writeTagDataResponse(nfcA, 5, readPageResponse); // this is for NTAG216 only
            if (writePageResponse != null) {
                writeToUiAppend(resultNfcWriting, "SUCCESS: writing with response: " + Utils.bytesToHexNpe(writePageResponse));
                return readPageResponse;
            } else {
                writeToUiAppend(resultNfcWriting, "FAILURE: no writing on the tag");
            }
        }
        return null;
    }


    private byte[] getTagDataResponse(NfcA nfcA, int page) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0x30,  // READ
                (byte) (page & 0x0ff), // page 0
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(resultNfcWriting, "Error on reading page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(resultNfcWriting, "Error (NACK) on reading page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(resultNfcWriting, "SUCCESS on reading page " + page + " response: " + Utils.bytesToHexNpe(response));
                System.out.println("reading page " + page + ": " + Utils.bytesToHexNpe(response));
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(resultNfcWriting, "ERROR: Tag lost exception on reading");
            return null;
        } catch (IOException e) {
            writeToUiAppend(resultNfcWriting, "ERROR: IOEexception: " + e);
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private byte[] writeTagDataResponse(NfcA nfcA, int page, byte[] dataByte) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0xA2,  // WRITE
                (byte) (page & 0x0ff),
                dataByte[0],
                dataByte[1],
                dataByte[2],
                dataByte[3]
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(resultNfcWriting, "Error on writing page " + page);
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                // Log and return
                writeToUiAppend(resultNfcWriting, "Error (NACK) on writing page " + page);
                return null;
            } else {
                // success: response contains ACK or actual data
                writeToUiAppend(resultNfcWriting, "SUCCESS on writing page " + page + " response: " + Utils.bytesToHexNpe(response));
                System.out.println("response page " + page + ": " + Utils.bytesToHexNpe(response));
                return response;
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(resultNfcWriting, "ERROR: Tag lost exception");
            return null;
        } catch (IOException e) {
            writeToUiAppend(resultNfcWriting, "ERROR: IOEexception: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private void formatNdef(Tag tag) {
        // trying to format the tag
        NdefFormatable format = NdefFormatable.get(tag);
        if(format != null){
            try {
                format.connect();
                format.format(new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)));
                format.close();
                showMessage("Tag formatted, try again to write on tag");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                showMessage("Failed to connect");
                e.printStackTrace();
            } catch (FormatException e) {
                // TODO Auto-generated catch block
                showMessage("Failed Format");
                e.printStackTrace();
            }
        }
        else {
            showMessage("Tag not formattable or already formatted to Ndef");
        }
    }

    private void writeToUiAppend(TextView textView, String message) {
        getActivity().runOnUiThread(() -> {
            String oldString = textView.getText().toString();
            if (TextUtils.isEmpty(oldString)) {
                textView.setText(message);
            } else {
                String newString = message + "\n" + oldString;
                textView.setText(newString);
                System.out.println(message);
            }
        });
    }

    private void showMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
            resultNfcWriting.setText(message);
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(getView().getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}