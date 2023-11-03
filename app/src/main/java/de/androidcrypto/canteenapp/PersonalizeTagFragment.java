package de.androidcrypto.canteenapp;

import static de.androidcrypto.canteenapp.Constants.*;
import static de.androidcrypto.canteenapp.Utils.doVibrate;
import static de.androidcrypto.canteenapp.Utils.playSinglePing;
import static de.androidcrypto.canteenapp.Utils.printData;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.skjolber.ndef.Message;
import com.github.skjolber.ndef.MimeRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    private com.google.android.material.textfield.TextInputEditText cardholderName, cardId, remainingDeposit;
    private com.google.android.material.textfield.TextInputEditText resultNfcWriting;

    /**
     * section for transaction recyclerview
     */

    private ArrayList<TransactionModel> transactionModelArrayList;
    private TransactionRVAdapter transactionRVAdapter;
    private RecyclerView transactionRV;




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

        // transaction recyclerview
        transactionModelArrayList = new ArrayList<>();
        transactionRVAdapter = new TransactionRVAdapter(transactionModelArrayList, getContext());
        transactionRV = getView().findViewById(R.id.idRvTransactions);
        remainingDeposit = getView().findViewById(R.id.etPersonalizeRemainingDeposit);

        // setting layout manager for our recycler view.
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        transactionRV.setLayoutManager(linearLayoutManager);

        // setting our adapter to recycler view.
        transactionRV.setAdapter(transactionRVAdapter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_personalize_tag, container, false);
    }

    private boolean runGetFabricSettings() {
        // this method tries to get the file settings and data on a NTAG424DNA tag with fabric settings

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

        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 02: read all file settings");
        FileSettings[] allFileSettings = ntag424DnaMethods.getAllFileSettings();
        FileSettings fs01 = allFileSettings[0];
        writeToUiAppend(resultNfcWriting, fs01.dump());
        FileSettings fs02 = allFileSettings[1];
        writeToUiAppend(resultNfcWriting, fs02.dump());
        FileSettings fs03 = allFileSettings[2];
        writeToUiAppend(resultNfcWriting, fs03.dump());

        // read content files 01, 02 and 03
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: read the contents of files 01 and 02");

        byte[] contentFile01 = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_01, 0, 32);
        byte[] contentFile02 = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 0, 256);

        // step 2 authenticate with default key 3
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 04: authenticate the application with default key 3");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber3, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 3 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 3, aborted");
            return false;
        }

        byte[] contentFile03 = ntag424DnaMethods.readStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, 0, 128);

        if (contentFile01 != null) {
            writeToUiAppend(resultNfcWriting, printData("content file 01\n", contentFile01));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 01, aborted");
            return false;
        }
        if (contentFile02 != null) {
            writeToUiAppend(resultNfcWriting, printData("content file 02\n", contentFile02));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 02, aborted");
            return false;
        }
        if (contentFile03 != null) {
            writeToUiAppend(resultNfcWriting, printData("content file 03\n", contentFile03));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 03, aborted");
            return false;
        }

        return true;
    }

    private boolean runGetPersonalizedSettings() {
        // this method tries to get the file settings and data on a NTAG424DNA tag with personalized settings

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

        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 02: read all file settings");
        FileSettings[] allFileSettings = ntag424DnaMethods.getAllFileSettings();
        FileSettings fs01 = allFileSettings[0];
        writeToUiAppend(resultNfcWriting, fs01.dump());
        FileSettings fs02 = allFileSettings[1];
        writeToUiAppend(resultNfcWriting, fs02.dump());
        FileSettings fs03 = allFileSettings[2];
        writeToUiAppend(resultNfcWriting, fs03.dump());

        // read content files 01, 02 and 03
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: read the contents of files 01 and 02");

        byte[] contentFile01 = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_01, 0, 32);
        byte[] contentFile02 = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 0, 256);

        // step 2 authenticate with default key 3
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 04: authenticate the application with default key 3");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber3, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 3 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 3, aborted");
            return false;
        }

        byte[] contentFile03 = ntag424DnaMethods.readStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, 0, 128);

        if (contentFile01 != null) {
            writeToUiAppend(resultNfcWriting, printData("content file 01\n", contentFile01));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 01, aborted");
            return false;
        }
        if (contentFile02 != null) {
            writeToUiAppend(resultNfcWriting, printData("content file 02\n", contentFile02));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 02, aborted");
            return false;
        }
        if (contentFile03 != null) {
            writeToUiAppend(resultNfcWriting, printData("content file 03\n", contentFile03));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 03, aborted");
            return false;
        }
        return true;
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

        doVibrate(getActivity());

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
        writeToUiAppend(resultNfcWriting, "step 02: authenticate the application with default key 2");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber2, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 2 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 2, aborted");
            return false;
        }

/*
modified:

step 0x: read the file settings of all files
fileNumber: 01
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 00
accessRights R  | W:   E0
accessRights RW:       0
accessRights CAR:      0
accessRights R:        14
accessRights W:        0
fileSize: 32

fileNumber: 02
fileType: 0 (Standard)
communicationSettings: 03 (Encrypted)
accessRights RW | CAR: 12
accessRights R  | W:   34
accessRights RW:       1
accessRights CAR:      2
accessRights R:        3
accessRights W:        4
fileSize: 256

fileNumber: 03
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 12
accessRights R  | W:   E4
accessRights RW:       1
accessRights CAR:      2
accessRights R:        14
accessRights W:        4
fileSize: 128
 */
/*
original/fabric:

fileNumber: 01
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 00
accessRights R  | W:   E0
accessRights RW:       0
accessRights CAR:      0
accessRights R:        14
accessRights W:        0
fileSize: 32

fileNumber: 02
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: E0
accessRights R  | W:   EE
accessRights RW:       14
accessRights CAR:      0
accessRights R:        14
accessRights W:        14
fileSize: 256

fileNumber: 03
fileType: 0 (Standard)
communicationSettings: 03 (Encrypted)
accessRights RW | CAR: 30
accessRights R  | W:   23
accessRights RW:       3
accessRights CAR:      0
accessRights R:        2
accessRights W:        3
fileSize: 128
 */

        // new version
        // step 3 change the CommunicationMode of file 02 from Full to Plain
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: change the CommunicationMode of file 02 from Full to Plain");
        success = ntag424DnaMethods.changeFileSettings(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, Ntag424DnaMethods.CommunicationSettings.Plain, 14, 0, 14, 14, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "change the CommunicationMode of file 02 from Full to Plain was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in changing the CommunicationMode of file 02 from Full to Plain, aborted");
            //todo return false;
        }

        // step 3 change the CommunicationMode of file 03 from Plain to Full
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: change the CommunicationMode of file 03 from Plain to Full");
        success = ntag424DnaMethods.changeFileSettings(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, Ntag424DnaMethods.CommunicationSettings.Full, 1, 2, 3, 4, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "change the CommunicationMode of file 03 from Plain to Full was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in changing the CommunicationMode of file 03 from Plain to Full, aborted");
            // todo return false;
        }

/*
        // old version
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
*/
        // step 2 write the NDEF template to the file 02

        // step  authenticate with key 00 (read & write key)
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: authenticate the application with default key 0");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber0, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 0 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 0, aborted");
            return false;
        }

        // step 1 write the changed NDEF compatibility container to the file 01
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write a modified NDEF compatibility container to file 01");
        //byte[] modifiedNdefCompatibilityContainer = Utils.hexStringToByteArray("0017200080007f0406e105008000000000000000000000000000000000000000");
        byte[] modifiedNdefCompatibilityContainer = Utils.hexStringToByteArray("0017200080007f0406e104008000000000000000000000000000000000000000");
        // This CC has one file only with file ID E104
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_01, modifiedNdefCompatibilityContainer, 0, 32);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 1 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 1, aborted");
            // todo return false;
        }

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


        // step 3 write the personal data (cardholder name and cardId) to file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: write the personal data (cardholder name and cardId) to the file 02");
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
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, cardholderNameFull, offsetCardholderName, cardholderNameFull.length);
        // success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, cardholderNameFull, offsetCardholderName, cardholderNameFull.length, false);
        //
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the cardholder name was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the cardholder name, aborted");
            return false;
        }

        // the cardId is maximum 16 bytes long and written to file 02 @offset 16
        byte[] cardIdFull = new byte[16];
        int offsetCardId = 16;
        System.arraycopy(cardholderIdString.getBytes(StandardCharsets.UTF_8), 0, cardIdFull, 0, cardholderIdString.getBytes(StandardCharsets.UTF_8).length);
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, cardIdFull, offsetCardId, cardIdFull.length);
        // success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, cardIdFull, offsetCardId, cardIdFull.length, false);
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
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVvf, offsetVvf, exportedVvf.length);
        // success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVvf, offsetVvf, exportedVvf.length, false);
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
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVcrf, offsetVcrf, exportedVcrf.length);
        //success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVcrf, offsetVcrf, exportedVcrf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Cyclic Records File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Cyclic Records File, aborted");
            return false;
        }

        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: read all file settings");
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
        byte[] contentCard = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 0, 32);
        byte[] contentValue = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 32, 48);
        byte[] contentCyclic1 = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 80, 112);
        byte[] contentCyclic2 = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 192, 64);
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
        // write to file 03
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write content to file 03");
        byte[] content03 = "Some content for file 03".getBytes(StandardCharsets.UTF_8);
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, content03, 0, content03.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "write content to file 03 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in writing content to file 03, aborted");
            return false;
        }
*/

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

        // read content from file 03
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: read content of file 03");
        byte[] content03Read = ntag424DnaMethods.readStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, 0, 128);
        if (content03Read != null) {
            writeToUiAppend(resultNfcWriting, printData("content file 03\n", content03Read));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 03, aborted");
            return false;
        }

        // run a credit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a credit transaction");
        vvf = new VirtualValueFile(contentValue);
        vcrf = new VirtualCyclicRecordFile(contentCyclic);
        // run a credit/charge transaction
        //TransactionRecord(String timestampShort, String amPmMarker, String creditDebitMarker, String bookingUnits, byte machineNumber, byte goodType) {
        String timestampShort = Utils.getTimestampShort();
        Log.d(TAG, "timestampShort: " + timestampShort);
        String amPmMarker = "P";
        String creditDebitMarker = "C";
        String bookingUnits = "010000";
        byte machineNumber = (byte) 0x01;
        byte goodType = (byte) 0x00;
        TransactionRecord tr = new TransactionRecord(timestampShort, amPmMarker, creditDebitMarker, bookingUnits, machineNumber, goodType);
        if (!tr.isRecordValid()) {
            writeToUiAppend(resultNfcWriting, "Error: TransactionRecord is not valid, aborted");
            return false;
        }
        vvf.credit(applicationKey4, Integer.parseInt(bookingUnits));
        vcrf.addRecord(applicationKey4, tr.getRecord());
        // write data back to files
        exportedVvf = vvf.exportVvf();
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVvf, offsetVvf, exportedVvf.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Value File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Value File, aborted");
            return false;
        }
        exportedVcrf = vcrf.exportVcrf();
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVcrf, offsetVcrf, exportedVcrf.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Cyclic Records File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Cyclic Records File, aborted");
            return false;
        }

        // run a debit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a debit transaction");
        timestampShort = Utils.getTimestampShort();
        Log.d(TAG, "timestampShort: " + timestampShort);
        amPmMarker = "A";
        creditDebitMarker = "D";
        bookingUnits = "000340";
        machineNumber = (byte) 0x09;
        goodType = (byte) 0x01;
        tr = new TransactionRecord(timestampShort, amPmMarker, creditDebitMarker, bookingUnits, machineNumber, goodType);
        if (!tr.isRecordValid()) {
            writeToUiAppend(resultNfcWriting, "Error: TransactionRecord is not valid, aborted");
            return false;
        }
        vvf.debit(Integer.parseInt(bookingUnits));
        vcrf.addRecord(applicationKey4, tr.getRecord());
        // write data back to files
        exportedVvf = vvf.exportVvf();
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVvf, offsetVvf, exportedVvf.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Value File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Value File, aborted");
            return false;
        }
        exportedVcrf = vcrf.exportVcrf();
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVcrf, offsetVcrf, exportedVcrf.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Cyclic Records File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Cyclic Records File, aborted");
            return false;
        }

        // run a second debit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a debit transaction");
        timestampShort = Utils.getTimestampShort();
        Log.d(TAG, "timestampShort: " + timestampShort);
        amPmMarker = "A";
        creditDebitMarker = "D";
        bookingUnits = "000250";
        machineNumber = (byte) 0x11;
        goodType = (byte) 0x15;
        tr = new TransactionRecord(timestampShort, amPmMarker, creditDebitMarker, bookingUnits, machineNumber, goodType);
        if (!tr.isRecordValid()) {
            writeToUiAppend(resultNfcWriting, "Error: TransactionRecord is not valid, aborted");
            return false;
        }
        vvf.debit(Integer.parseInt(bookingUnits));
        vcrf.addRecord(applicationKey4, tr.getRecord());
        // write data back to files
        exportedVvf = vvf.exportVvf();
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVvf, offsetVvf, exportedVvf.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Value File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Value File, aborted");
            return false;
        }
        exportedVcrf = vcrf.exportVcrf();
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVcrf, offsetVcrf, exportedVcrf.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Cyclic Records File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Cyclic Records File, aborted");
            return false;
        }

        writeToUiAppend(resultNfcWriting, "remaining balance on tag: " + vvf.getBalance());

        // show transactions
        List<byte[]> transactionRecordsByte= vcrf.getRecordList();
        List<TransactionRecord> transactionRecordList = new ArrayList<>();
        // fill the list of transactionRecords
        for (int i = 0; i < transactionRecordsByte.size(); i++) {
            TransactionRecord trList = new TransactionRecord(transactionRecordsByte.get(i));
            if (trList.isRecordValid()) {
                Log.d(TAG, "add record " + i);
                transactionRecordList.add(trList);
            }
        }
        // now get the data for transactionModel
        transactionModelArrayList = new ArrayList<>();
        for (int i = 0; i < transactionRecordList.size(); i++) {
            TransactionRecord trSingle = transactionRecordList.get(i);
            LookupTable lt = new LookupTable(trSingle);
            TransactionModel trModel = new TransactionModel(
                    lt.getTransactionTimestamp(),
                    // todo lookups for data
                    lt.formatBookingUnits(),
                    lt.formatCreditDebitMarker(),
                    lt.formatMachineNumber(),
                    lt.formatGoodType()
            );
            transactionModelArrayList.add(trModel);
        }

        transactionRVAdapter = new TransactionRVAdapter(transactionModelArrayList, getContext());
        final int balance = vvf.getBalance();
        getActivity().runOnUiThread(() -> {
            // setting layout manager for our recycler view.
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
            transactionRV.setLayoutManager(linearLayoutManager);
            // setting our adapter to recycler view.
            transactionRV.setAdapter(transactionRVAdapter);
            remainingDeposit.setText(Utils.convertIntegerInFloatString(balance));
        });

        writeToUiAppend(resultNfcWriting, "Number of recorded transactions: " + transactionModelArrayList.size());

        // read content from file 01
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: read content of file 01");
        byte[] content01Read = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_01, 0, 32);
        if (content01Read != null) {
            writeToUiAppend(resultNfcWriting, printData("content file 01\n", content01Read));
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the content of file 01, aborted");
            return false;
        }

        // create a NDEF message
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: create a NDEF message");
        NdefRecord ndefRecord = NdefRecord.createTextRecord("en","English String Balance: 97,40");
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        byte[] ndefMessageByte = ndefMessage.toByteArray();
        writeToUiAppend(resultNfcWriting, printData("ndefMessage", ndefMessageByte));

        // step  authenticate with key 01 (read & write key)
        Log.d(TAG, "Authenticate with default key 1");
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: authenticate the application with default key 1");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber1, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 1 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 1, aborted");
            return false;
        }

        // write NDEF message to file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write NDEF message to file 02");
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, ndefMessageByte, 0, ndefMessageByte.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "write NDEF message to file 02 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in writing NDEF message to file 02, aborted");
            return false;
        }

        // using Virtual File
        Log.d(TAG, "*** Use a Virtual File ***");
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: use a Virtual File in file 03");
        VirtualFile vf = new VirtualFile(applicationKey4);
        byte[] exportedVf = vf.exportVirtualFile();
        Log.d(TAG, printData("** exportedVf**", exportedVf));
        writeToUiAppend(resultNfcWriting, printData("exportedVirtualFile\n", exportedVf));
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, exportedVf, 0, exportedVf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "saving of the Virtual File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Virtual File, aborted");
            //return false;
        }

        // run a credit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a credit transaction");
        // run a credit/charge transaction
        timestampShort = Utils.getTimestampShort();
        Log.d(TAG, "timestampShort: " + timestampShort);
        amPmMarker = "P";
        creditDebitMarker = "C";
        bookingUnits = "010000";
        machineNumber = (byte) 0x01;
        goodType = (byte) 0x00;
        tr = new TransactionRecord(timestampShort, amPmMarker, creditDebitMarker, bookingUnits, machineNumber, goodType);
        if (!tr.isRecordValid()) {
            writeToUiAppend(resultNfcWriting, "Error: TransactionRecord is not valid, aborted");
            return false;
        }
        vf.credit(applicationKey4, Integer.parseInt(bookingUnits));
        vf.addRecord(applicationKey4, tr.getRecord());
        // write data back to files
        exportedVf = vf.exportVirtualFile();
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, exportedVf, 0, exportedVf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Virtual File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Virtual File, aborted");
            //return false;
        }

        // run a debit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a debit transaction");
        timestampShort = Utils.getTimestampShort();
        Log.d(TAG, "timestampShort: " + timestampShort);
        amPmMarker = "A";
        creditDebitMarker = "D";
        bookingUnits = "000340";
        machineNumber = (byte) 0x09;
        goodType = (byte) 0x01;
        tr = new TransactionRecord(timestampShort, amPmMarker, creditDebitMarker, bookingUnits, machineNumber, goodType);
        if (!tr.isRecordValid()) {
            writeToUiAppend(resultNfcWriting, "Error: TransactionRecord is not valid, aborted");
            return false;
        }
        vf.debit(Integer.parseInt(bookingUnits));
        vf.addRecord(applicationKey4, tr.getRecord());
        // write data back to files
        exportedVf = vf.exportVirtualFile();
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, exportedVf, 0, exportedVf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Virtual File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Virtual File, aborted");
            //return false;
        }

        // run a 2.nd debit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a debit transaction");
        timestampShort = Utils.getTimestampShort();
        Log.d(TAG, "timestampShort: " + timestampShort);
        amPmMarker = "A";
        creditDebitMarker = "D";
        bookingUnits = "000230";
        machineNumber = (byte) 0x17;
        goodType = (byte) 0x02;
        tr = new TransactionRecord(timestampShort, amPmMarker, creditDebitMarker, bookingUnits, machineNumber, goodType);
        if (!tr.isRecordValid()) {
            writeToUiAppend(resultNfcWriting, "Error: TransactionRecord is not valid, aborted");
            return false;
        }
        // testing the Virtual File import constructor
        VirtualFile vf2 = new VirtualFile(exportedVf, true);
        if (vf2.isVirtualFileValid()) {
            // proceed
        } else {
            writeToUiAppend(resultNfcWriting, "The Virtual File 2 is not valid, aborted");
            return false;
        }
        vf.debit(Integer.parseInt(bookingUnits));
        vf.addRecord(applicationKey4, tr.getRecord());
        // write data back to files
        exportedVf = vf.exportVirtualFile();
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, exportedVf, 0, exportedVf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Virtual File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Virtual File, aborted");
            //return false;
        }

        // write the balance in a NDEF file 02
        // create a NDEF message
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: create a NDEF message");
        StringBuilder sb = new StringBuilder();
        sb.append("remaining balance on the tag: ");
        sb.append(Utils.convertIntegerInFloatString(vf.getBalance()));
        sb.append("\n");
        sb.append("last transaction: ");
        TransactionRecord trLast = new TransactionRecord(vf.showLastRecord());
        LookupTable lut = new LookupTable(trLast);
        sb.append(lut.getTransactionTimestamp());
        sb.append("\n");
        sb.append("credit/debit: ").append(lut.formatCreditDebitMarker()).append("\n");
        sb.append("booking: ").append(lut.formatBookingUnits()).append("\n");
        sb.append("machine: ").append(lut.formatMachineNumber()).append("\n");
        sb.append("good: ").append(lut.formatGoodType()).append("\n");
        String record = sb.toString();

        /*
        // using ndef tools
        MimeRecord mimeRecord = new MimeRecord();
        mimeRecord.setMimeType("text/plain");
        mimeRecord.setData(record.getBytes(StandardCharsets.UTF_8));
        Message message = new Message(); //  com.github.skjolber.ndef.Message
        message.add(mimeRecord);
        NdefMessage ndefMessage2 = message.getNdefMessage();
        byte[] ndefMessage2Bytes = ndefMessage2.toByteArray();
        writeToUiAppend(resultNfcWriting, printData("ndefMessage2", ndefMessage2Bytes));

         */

        ndefRecord = NdefRecord.createTextRecord("en",record);
        //ndefRecord = NdefRecord.createTextRecord("en","English String Balance: 97,40");
        ndefMessage = new NdefMessage(ndefRecord);
        ndefMessageByte = ndefMessage.toByteArray();
        writeToUiAppend(resultNfcWriting, printData("ndefMessage", ndefMessageByte));

        /*
        // build the ndef header, maximum length is 255
        byte[] ndefMessageFull = new byte[ndefMessageByte.length + 3];
        ndefMessageFull[0] = (byte) 0x00; // not necessary
        ndefMessageFull[1] = (byte) ndefMessageByte.length;
        System.arraycopy(ndefMessageByte, 0, ndefMessageFull, 2, ndefMessageByte.length);
        ndefMessageFull[ndefMessageByte.length + 2] = (byte) 0x00; // not necessary
*/
        // step  authenticate with key 01 (read & write key)
        Log.d(TAG, "Authenticate with default key 1");
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: authenticate the application with default key 1");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber1, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 1 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 1, aborted");
            return false;
        }

        // write NDEF message to file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write NDEF message to file 02");

        // changing tag technology from IsoDep to Ndef

        try {
            IsoDep isoDep = IsoDep.get(discoveredTag);
            isoDep.close();
            Ndef ndef = Ndef.get(discoveredTag);
            ndef.connect();
            ndef.writeNdefMessage(ndefMessage);
            writeToUiAppend(resultNfcWriting, "write NDEF message to file 02 was SUCCESSFUL");
        } catch (IOException | FormatException e) {
            //throw new RuntimeException(e);
            writeToUiAppend(resultNfcWriting, "FAILURE in writing NDEF message to file 02, aborted " + e.getMessage());
            return false;
        }

/*
        //success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, ndefMessageFull, 0, ndefMessageFull.length);
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, ndefMessage2Bytes, 0, ndefMessage2Bytes.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "write NDEF message to file 02 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in writing NDEF message to file 02, aborted");
            return false;
        }
        // working one: 0021d1011d5402656e73616d706c6520746578742042616c616e6365203132332c34350000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
        // not working:     d101205402656e456e676c69736820537472696e672042616c616e63653a2039372c3430a1d6a39d2c08d745be5f980d35b8e66945b28ecfb64d8193d22ba39284000024c22a7f1c2f41608422c6e6ee05080302ec8975c400aade56d4cb2271dddcb22305c78e0357407b9ecb93d2aadb9097ce031120230018430f0c0100000100ffff031120230018440f0d0003400901ffff031120230018440f0d0002501115ffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001c046633cd77b7d07a68a95f
        // new:         009cd101985402656e72656d61696e696e672062616c616e6365206f6e20746865207461673a2039342c33300a6c617374207472616e73616374696f6e3a2030332e31312e323032332031343a33373a30310a6372656469742f64656269743a2044656269740a626f6f6b696e673a20322c33300a6d616368696e653a2031312076656e64696e67206d616368696e650a676f6f643a203120666f6f640a02501115ffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002b5d0549c9c2f1d828011de8
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: read the file settings of all files");
        FileSettings[] allFileSettingsN = ntag424DnaMethods.getAllFileSettings();
        FileSettings fs01N = allFileSettingsN[0];
        writeToUiAppend(resultNfcWriting, fs01N.dump());
        FileSettings fs02N = allFileSettingsN[1];
        writeToUiAppend(resultNfcWriting, fs02N.dump());
        FileSettings fs03N = allFileSettingsN[2];
        writeToUiAppend(resultNfcWriting, fs03N.dump());

 */
/*
modified:

step 0x: read the file settings of all files
fileNumber: 01
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 00
accessRights R  | W:   E0
accessRights RW:       0
accessRights CAR:      0
accessRights R:        14
accessRights W:        0
fileSize: 32

fileNumber: 02
fileType: 0 (Standard)
communicationSettings: 03 (Encrypted)
accessRights RW | CAR: 12
accessRights R  | W:   34
accessRights RW:       1
accessRights CAR:      2
accessRights R:        3
accessRights W:        4
fileSize: 256

fileNumber: 03
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 12
accessRights R  | W:   E4
accessRights RW:       1
accessRights CAR:      2
accessRights R:        14
accessRights W:        4
fileSize: 128
 */
/*
original/fabric:

fileNumber: 01
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 00
accessRights R  | W:   E0
accessRights RW:       0
accessRights CAR:      0
accessRights R:        14
accessRights W:        0
fileSize: 32

fileNumber: 02
fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: E0
accessRights R  | W:   EE
accessRights RW:       14
accessRights CAR:      0
accessRights R:        14
accessRights W:        14
fileSize: 256

fileNumber: 03
fileType: 0 (Standard)
communicationSettings: 03 (Encrypted)
accessRights RW | CAR: 30
accessRights R  | W:   23
accessRights RW:       3
accessRights CAR:      0
accessRights R:        2
accessRights W:        3
fileSize: 128
 */

        writeToUiAppend(resultNfcWriting, "The tag was personalized with SUCCESS");
        Log.d(TAG, "The tag was personalized with SUCCESS");
        playSinglePing(getContext());
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
        //boolean success = runGetFabricSettings();
        //boolean success = runGetPersonalizedSettings();
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