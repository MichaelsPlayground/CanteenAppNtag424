package de.androidcrypto.nfcstoragemanagementtagpersonalization;

import static android.content.Context.MODE_PRIVATE;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Constants.*;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Utils.doVibrate;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Utils.printData;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
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
 * Use the {@link ActivateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ActivateFragment extends Fragment implements NfcAdapter.ReaderCallback {
    private static final String TAG = ActivateFragment.class.getName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private com.google.android.material.textfield.TextInputEditText resultNfcWriting;
    private RadioButton rbActivateGetStatus, rbActivateOn, rbActivateOff;

    private Ntag21xMethods ntagMethods;
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;

    private int identifiedNtagConfigurationPage; // this  is the stating point for any work on configuration
    private byte[] tagUid; // written by onDiscovered

    public ActivateFragment() {
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
    public static ActivateFragment newInstance(String param1, String param2) {
        ActivateFragment fragment = new ActivateFragment();
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
                Log.d(TAG,  printData("tagUid", tagUid));
                int nfcaMaxTransceiveLength = nfcA.getMaxTransceiveLength(); // important for the readFast command
                Log.d(TAG, "nfcaMaxTransceiveLength: " + nfcaMaxTransceiveLength);
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
                nfcaContent = nfcaContent + "maxTranceiveLength: " + nfcaMaxTransceiveLength + " bytes\n";
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
                    ntagMemory = ntagMethods.readNdefContent(nfcA, maximumBytesToRead, nfcaMaxTransceiveLength);
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


                    byte[] shortenedHash = ntagMethods.getUidHashShort(tagUid);
                    writeToUiAppend(resultNfcWriting, "tagUid: " + Utils.bytesToHexNpe(tagUid));
                    writeToUiAppend(resultNfcWriting, "shortenedMAC: " + Utils.bytesToHexNpe(shortenedHash));


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
                    ntagMemory = ntagMethods.readNdefContent(nfcA, maximumBytesToRead, nfcaMaxTransceiveLength);
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
                        writeToUiAppend(resultNfcWriting, "Error - insufficient positions found, aborted");
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

    /**
     * reads a value/String from SharedPreferences
     * @param preferenceName
     * @param preferenceHeader
     * @param preferenceFooter
     * @return the value or an empty string if a value is not saved before
     */
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

    /**
     * returns the position of a placeholder ('matchString') within a string
     * @param content
     * @param matchString
     * @return
     */
    private int getPlaceholderPosition(String content, String matchString) {
        return content.indexOf(matchString) + matchString.length();
    }

    /**
     * section for UI service methods
     */

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