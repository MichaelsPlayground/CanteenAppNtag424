package de.androidcrypto.canteenapp;

import static de.androidcrypto.canteenapp.Constants.*;
import static de.androidcrypto.canteenapp.Utils.doVibrate;
import static de.androidcrypto.canteenapp.Utils.printData;

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
    private com.google.android.material.textfield.TextInputEditText cardholderName, cardId;
    private com.google.android.material.textfield.TextInputEditText resultNfcWriting;

    private final String lineSeparator = "---------------------";
    private PreferencesHandling preferencesHandling;
    private Ntag21xMethods ntagMethods;
    private NfcAdapter mNfcAdapter;
    private Tag discoveredTag;
    private Ntag424DnaMethods ntag424DnaMethods;

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

        cardholderName = getView().findViewById(R.id.etPersonalizeName);
        cardId = getView().findViewById(R.id.etPersonalizeId);
        resultNfcWriting = getView().findViewById(R.id.etPersonalizeResult);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());
        preferencesHandling = new PreferencesHandling(getActivity(), getContext(), resultNfcWriting);
        ntagMethods = new Ntag21xMethods(getActivity(), resultNfcWriting);

        // todo remove testdata
        cardholderName.setText("name");
        cardId.setText("1234");

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
        xx

        1. xxwrite the NDEF template to the tag
        2. xxdisable all existing mirror
        3. xxenable UID mirroring
        4. xxwrite the UID-based MAC to the tag
         */

        // step 1 select the application
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 01: select the application on the tag");
        success = ntag424DnaMethods.selectNdefApplicationIso();
        if (success) {
            writeToUiAppend(resultNfcWriting, "selecting the application was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in selecting the application, aborted");
            return false;
        }

        // step 2 authenticate with default key
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 02: authenticate the application with default key 0");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber0, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 0 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 0, aborted");
            return false;
        }

        // step 3 change the CommunicationMode of file 02 from Plain to Full
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: change the CommunicationMode of file 02 from Plain to Full");
        success = ntag424DnaMethods.changeFileSettings(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, Ntag424DnaMethods.CommunicationSettings.Full, 1, 2, 3, 4, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "change the CommunicationMode of file 02 from Plain to Full was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in changing the CommunicationMode of file 02 from Plain to Full, aborted");
            //todo return false;
        }

        // step 3 change the CommunicationMode of file 03 from Full to Plain
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: change the CommunicationMode of file 03 from Full to Plain");
        success = ntag424DnaMethods.changeFileSettings(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, Ntag424DnaMethods.CommunicationSettings.Plain, 1, 2, 14, 4, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "change the CommunicationMode of file 03 from Full to Plain was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in changing the CommunicationMode of file 03 from Full to Plain, aborted");
            // todo return false;
        }


        // step 1 write the NDEF compatibility container to the file 00

        // step 2 write the NDEF template to the file 02

        // step  authenticate with key 01 (read & write key)
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: authenticate the application with default key 1");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber1, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 1 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 1, aborted");
            return false;
        }

        // step 3 write the personal data (cardholder name and cardId) to file 01
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: write the personal data (cardholder name and cardId) to the file 01");
        String cardholderNameString = cardholderName.getText().toString();
        if (TextUtils.isEmpty(cardholderNameString)) {
            writeToUiAppend(resultNfcWriting, "please enter a cardholder's name");
            return false;
        }
        String cardholderIdString = cardId.getText().toString();
        if (TextUtils.isEmpty(cardholderIdString)) {
            writeToUiAppend(resultNfcWriting, "please enter a cardId");
            return false;
        }

        // the cardholderName is maximum 16 bytes long and written to file 01 @offset 00
        byte[] cardholderNameFull = new byte[16];
        int offsetCardholderName = 0;
        System.arraycopy(cardholderNameString.getBytes(StandardCharsets.UTF_8), 0, cardholderNameFull, 0, cardholderIdString.getBytes(StandardCharsets.UTF_8).length);
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, cardholderNameFull, offsetCardholderName, cardholderNameFull.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the cardholder name was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the cardholder name, aborted");
            return false;
        }


        // the cardId is maximum 16 bytes long and written to file 01 @offset 16
        byte[] cardIdFull = new byte[16];
        int offsetCardId = 16;
        System.arraycopy(cardholderIdString.getBytes(StandardCharsets.UTF_8), 0, cardIdFull, 0, cardholderIdString.getBytes(StandardCharsets.UTF_8).length);
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, cardIdFull, offsetCardId, cardIdFull.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the cardId was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the cardId, aborted");
            return false;
        }

        // step 4 write the Virtual Value File to the file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write the Value File to the file 02");
        int offsetVvf = 32;
        VirtualValueFile vvf = new VirtualValueFile((byte) 0x04, applicationKey4);
        byte[] exportedVvf = vvf.exportVvf();
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVvf, offsetVvf, exportedVvf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Value File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Value File, aborted");
            return false;
        }

        // step 5 write the Virtual Cyclic Records File to the file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write the Cyclic Records File to the file 02");
        int offsetVcrf = 80;
        byte numberOfRecords = (byte) 0x08;
        VirtualCyclicRecordFile vcrf = new VirtualCyclicRecordFile((byte) 0x05, numberOfRecords, applicationKey4);
        byte[] exportedVcrf = vcrf.exportVcrf();
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVcrf, offsetVcrf, exportedVcrf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Cyclic Records File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Cyclic Records File, aborted");
            return false;
        }


        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: read the file settings of file 02");
        FileSettings[] allFileSettings = ntag424DnaMethods.getAllFileSettings();
        FileSettings fs01 = allFileSettings[0];
        writeToUiAppend(resultNfcWriting, fs01.dump());
        FileSettings fs02 = allFileSettings[1];
        writeToUiAppend(resultNfcWriting, fs02.dump());
        FileSettings fs03 = allFileSettings[2];
        writeToUiAppend(resultNfcWriting, fs03.dump());



        // step  authenticate with key 01 (read & write key)
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: authenticate the application with default key 1");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber1, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 1 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 1, aborted");
            return false;
        }

        // read content file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: read the content of file 02");
        byte[] contentCard = ntag424DnaMethods.readStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 0, 32);
        byte[] contentValue = ntag424DnaMethods.readStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 32, 48);
        byte[] contentCyclic1 = ntag424DnaMethods.readStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 80, 112);
        byte[] contentCyclic2 = ntag424DnaMethods.readStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 192, 64);
        byte[] contentCyclic = new byte[contentCyclic1.length + contentCyclic2.length];
        System.arraycopy(contentCyclic1, 0, contentCyclic, 0, contentCyclic1.length);
        System.arraycopy(contentCyclic2, 0, contentCyclic, contentCyclic1.length, contentCyclic2.length);

        if (contentCard != null) {
            writeToUiAppend(resultNfcWriting, printData("content cardData file 02\n", contentCard));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 02, aborted");
            return false;
        }
        if (contentValue != null) {
            writeToUiAppend(resultNfcWriting, printData("content valueFile file 02\n", contentValue));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 02, aborted");
            return false;
        }
        if (contentCyclic != null) {
            writeToUiAppend(resultNfcWriting, printData("content CyclicFile file 02\n", contentCyclic));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 02, aborted");
            return false;
        }
/*
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


 */
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

        // empty result
        getActivity().runOnUiThread(() -> {
            resultNfcWriting.setText("");
        });
        writeToUiAppend(resultNfcWriting, "NFC tag discovered");
        ntag424DnaMethods = new Ntag424DnaMethods(resultNfcWriting, tag, getActivity());

        discoveredTag = tag;
        boolean success = runCompletePersonalize();
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