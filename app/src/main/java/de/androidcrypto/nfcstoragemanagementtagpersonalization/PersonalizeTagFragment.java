package de.androidcrypto.nfcstoragemanagementtagpersonalization;

import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Constants.*;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Utils.doVibrate;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Utils.printData;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.nio.charset.StandardCharsets;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PersonalizeTagFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PersonalizeTagFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private static final String TAG = PersonalizeTagFragment.class.getName();
    private com.google.android.material.textfield.TextInputEditText resultNfcWriting;

    private PreferencesHandling preferencesHandling;
    private Ntag21xMethods ntagMethods;
    private NfcAdapter mNfcAdapter;
    private Tag discoveredTag;
    private NfcA nfcA;
    private Ndef ndef;

    private int identifiedNtagConfigurationPage; // this  is the stating point for any work on configuration
    private int identifiedNtagCapabilityContainerPage;
    private int identifiedNtagPasswordPage;
    private int identifiedNtagPackPage;

    private byte[] tagUid; // written by onDiscovered

    public PersonalizeTagFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReceiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PersonalizeTagFragment newInstance(String param1, String param2) {
        PersonalizeTagFragment fragment = new PersonalizeTagFragment();
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
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this.getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        resultNfcWriting = getView().findViewById(R.id.etPersonalizeResult);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());
        preferencesHandling = new PreferencesHandling(getActivity(), getContext(), resultNfcWriting);
        ntagMethods = new Ntag21xMethods(getActivity(), resultNfcWriting);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_personalize_tag, container, false);
    }

    private boolean runCompletePersonalize() {
        /*
        This are the steps that will run when a tag is tapped:
        1. write the NDEF template to the tag
        2. disable all existing mirror
        3. enable UID mirroring
        4. write the UID-based MAC to the tag
         */

        // step 1 a: connect to NDEF and write the NDEF template to the tag
        boolean success = ntagMethods.connectNdef(nfcA, ndef);
        if (!success) {
            writeToUiAppend(resultNfcWriting, "could not connect with NDEF, aborted");
            return false;
        }

        // step 1 b: check that the tag is writable
        if (!ndef.isWritable()) {
            writeToUiAppend(resultNfcWriting,"NFC tag is not writable, aborted");
            return false;
        }
        Log.d(TAG, "tag is writable");

        // step 1 c: build the template string
        String templateUrlString = preferencesHandling.getPreferencesString(PREFS_TEMPLATE_URL_NAME);
        Log.d(TAG, "templateUrlString: " + templateUrlString);
        if (TextUtils.isEmpty(templateUrlString)) {
            writeToUiAppend(resultNfcWriting, "could not get the templateUrlString, aborted");
            writeToUiAppend(resultNfcWriting, "Did you forget to save the NDEF settings ?");
            return false;
        }

        // step 1 d: write the templateUrlString to the tag
        success = ntagMethods.writeNdefMessageUrl(ndef, templateUrlString);
        if (!success) {
            writeToUiAppend(resultNfcWriting, "could not write the templateUrlString with NDEF, aborted");
            return false;
        }
        Log.d(TAG, "templateUrlString written to the tag");

        // step 2: connect to NcfA and disable all mirrors
        // step 2 a: connect to NcfA
        success = ntagMethods.connectNfca(nfcA, ndef);
        if (!success) {
            writeToUiAppend(resultNfcWriting, "could not connect with NcfA, aborted");
            return false;
        }
        Log.d(TAG, "connected to the  tag using NfcA technology");

        // step 2 b: get the UID of the tag
        byte[] tagUid = ntagMethods.getTagUid(discoveredTag);
        if (tagUid == null) {
            writeToUiAppend(resultNfcWriting,"could not retrieve the UID of the tag, aborted");
            return false;
        }
        Log.d(TAG, printData("tagUid", tagUid));
        writeToUiAppend(resultNfcWriting, Utils.printData("UID", tagUid));

        // step 2 c: identify the tag
        String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tagUid);
        if ((!ntagVersion.equals("213")) && (!ntagVersion.equals("215")) && (!ntagVersion.equals("216"))) {
            writeToUiAppend(resultNfcWriting,"NFC tag is NOT of type NXP NTAG213/215/216, aborted");
            return false;
        }
        Log.d(TAG, "tag is of type NTAG213/215/216");

        // step 2 d: get technical data of NTAG
        int nfcaMaxTransceiveLength = ntagMethods.getTransceiveLength(nfcA);
        if (nfcaMaxTransceiveLength < 1) {
            writeToUiAppend(resultNfcWriting,"maximum transceive length is insufficient, aborted");
            return false;
        }
        int ntagPages = NfcIdentifyNtag.getIdentifiedNtagPages();
        identifiedNtagConfigurationPage = NfcIdentifyNtag.getIdentifiedNtagConfigurationPage();
        Log.d(TAG, "The configuration is starting in page " + identifiedNtagConfigurationPage);
        writeToUiAppend(resultNfcWriting, "The configuration is starting in page " + identifiedNtagConfigurationPage);
        identifiedNtagCapabilityContainerPage = NfcIdentifyNtag.getIdentifiedNtagCapabilityContainerPage();
        identifiedNtagPasswordPage = NfcIdentifyNtag.getIdentifiedNtagPasswordPage();
        identifiedNtagPackPage = NfcIdentifyNtag.getIdentifiedNtagPackPage();

        // step 2 d: disabling all counters
        success = ntagMethods.disableAllMirror(nfcA, identifiedNtagConfigurationPage);
        if (!success) {
            writeToUiAppend(resultNfcWriting, "could not disable all mirrors, aborted");
            return false;
        }
        Log.d(TAG, "All mirroring was disabled");

        // step 3 enable UID mirroring
        // step 3 a:
        int maximumBytesToRead = NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH + 7; // + 7 NDEF header bytes, so it total 144 bytes
        byte[] ntagMemory = ntagMethods.readNdefContent(nfcA, maximumBytesToRead, nfcaMaxTransceiveLength);
        if ((ntagMemory == null) || (ntagMemory.length < 10)) {
            writeToUiAppend(resultNfcWriting, "Error - could not read enough data from tag, aborted");
            return false;
        }
        Log.d(TAG, printData("ntagMemory", ntagMemory));
        String ntagDataString = new String(ntagMemory, StandardCharsets.UTF_8);
        writeToUiAppend(resultNfcWriting, "ntagDataString:\n" + ntagDataString);

        // step 3 b: read the placeholder names from the shared preferences
        String uidMatchString = preferencesHandling.getPreferencesMatchString(PREFS_UID_NAME, UID_HEADER, UID_FOOTER);
        String macMatchString = preferencesHandling.getPreferencesMatchString(PREFS_MAC_NAME, MAC_HEADER, MAC_FOOTER);
        Log.d(TAG, "uidMatchString: " + uidMatchString);
        Log.d(TAG, "macMatchString: " + macMatchString);
        // search for match strings and add length of match string for the next position to write the data
        int positionUidMatch = preferencesHandling.getPlaceholderPosition(ntagDataString, uidMatchString);
        int positionMacMatch = preferencesHandling.getPlaceholderPosition(ntagDataString, macMatchString);
        Log.d(TAG, "positionUidMatch: " + positionUidMatch + " positionMacMatch: " + positionMacMatch);
        // both values need to be > 1
        writeToUiAppend(resultNfcWriting, "positionUidMatch: " + positionUidMatch + " || positionMacMatch: " + positionMacMatch);
        if ((positionUidMatch < 1) || (positionMacMatch < 1)) {
            writeToUiAppend(resultNfcWriting, "Error - insufficient matching positions found, aborted");;
            return false;
        } else {
            writeToUiAppend(resultNfcWriting, "positive matching positions, now enable mirroring");
        }
        Log.d(TAG, "positive matching positions, now enable mirroring");

        // step 3 c: enable UID counter
        success = ntagMethods.enableUidMirror(nfcA, identifiedNtagConfigurationPage, positionUidMatch);
        if (!success) {
            writeToUiAppend(resultNfcWriting, "could not enable UID mirror, aborted");
            return false;
        }
        Log.d(TAG, "UID mirror was enabled on position: " + positionUidMatch);

        // step 3 d: calculate the MAC from uid using SHA-256 and shortened to 8 bytes length
        byte[] shortenedHash = ntagMethods.getUidHashShort(tagUid);
        Log.d(TAG, printData("shortenedHash", shortenedHash));
        writeToUiAppend(resultNfcWriting, printData("shortenedHash", shortenedHash));

        // step 3 e: write mac to tag
        success = ntagMethods.writeMacToNdef(nfcA, identifiedNtagConfigurationPage, shortenedHash, positionMacMatch);
        if (!success) {
            writeToUiAppend(resultNfcWriting, "could not write MAC, aborted");
            return false;
        }
        Log.d(TAG, "MAC was written with success on position: " + positionMacMatch);

        // todo enable write access by password/pack beginning at Compatibility container page

        success = ntagMethods.enableWriteProtection(nfcA, TAG_PASSWORD, TAG_PACK, identifiedNtagCapabilityContainerPage, identifiedNtagPasswordPage, identifiedNtagPackPage);
        if (!success) {
            writeToUiAppend(resultNfcWriting, "could not enable write protection, aborted");
            return false;
        }
        Log.d(TAG, "write protection was enabled, beginning with page " + identifiedNtagCapabilityContainerPage);

        writeToUiAppend(resultNfcWriting, "The tag was personalized with SUCCESS");
        Log.d(TAG, "The tag was personalized with SUCCESS");
        return true;
    }

    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        discoveredTag = tag;
        nfcA = NfcA.get(tag);
        ndef = Ndef.get(tag);

        writeToUiAppend(resultNfcWriting, "NFC tag discovered");

        boolean success = runCompletePersonalize();
        if (success) {
            doVibrate(getActivity());
        }
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

    private void showWirelessSettings() {
        Toast.makeText(this.getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
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
            mNfcAdapter.enableReaderMode(this.getActivity(),
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
            mNfcAdapter.disableReaderMode(this.getActivity());
    }

}