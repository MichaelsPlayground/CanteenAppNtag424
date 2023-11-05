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

    /**
     * section for complex actions
     */

    private boolean selectApplication(){
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
        return true;
    }

    private boolean authenticate(byte keyNumber, byte[] key) {
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 02: authenticate the application with key " + keyNumber);
        success = ntag424DnaMethods.authenticateAesEv2First(keyNumber, key);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key " + keyNumber + " was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key " + keyNumber + ", aborted");
            return false;
        }
        return true;
    }

    private boolean changeFile02SettingsWriteKey4() {
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: change the file settings of file 02, set r&w and w keys from E to 4");
        success = ntag424DnaMethods.changeFileSettings(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, Ntag424DnaMethods.CommunicationSettings.Plain, 4, 0, 14, 4, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "change the file settings of file 02, set r&w and w keys to 4 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in changing the file settings of file 02, set r&w and w keys to 4, aborted");
            return false;
        }
        return true;
    }

    private boolean changeFile02SettingsWriteKeyE() {
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: change the file settings of file 02, set r&w and w keys from 4E to 4");
        success = ntag424DnaMethods.changeFileSettings(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, Ntag424DnaMethods.CommunicationSettings.Plain, 14, 0, 14, 14, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "change the file settings of file 02, set r&w and w keys to E was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in changing the file settings of file 02, set r&w and w keys to E, aborted");
            return false;
        }
        return true;
    }

    private boolean writeFile01ModifiedCompatibilityContainer(){
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write a modified NDEF compatibility container to file 01");
        //byte[] modifiedNdefCompatibilityContainer = Utils.hexStringToByteArray("00172000c000bf0406e10400c000800000000000000000000000000000000000"); // write access prohibited
        byte[] modifiedNdefCompatibilityContainer = Utils.hexStringToByteArray("00172000c000bf0406e10400c000000000000000000000000000000000000000"); // free write access
        //byte[] originalNdefCompatibilityContainer = Utils.hexStringToByteArray("001720010000ff0406e104010000000506e10500808283000000000000000000");

        // This CC has one file only with file ID E104
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_01, modifiedNdefCompatibilityContainer, 0, 32);
        //success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_01, originalNdefCompatibilityContainer, 0, 32);
        if (success) {
            writeToUiAppend(resultNfcWriting, "write a modified NDEF compatibility container to file 01 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in write a modified NDEF compatibility container to file 01, aborted");
            return false;
        }
        return true;
    }

    private VirtualFile createVirtualFile(){
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: use a Virtual File in file 03");
        VirtualFile vf = new VirtualFile(applicationKey4);
        if (!vf.isVirtualFileValid()) return null;
        return vf;
    }

    private boolean writeVirtualFileToFile03(VirtualFile vf){
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write a Virtual File in file 03");
        byte[] exportedVf = vf.exportVirtualFile();
        if (exportedVf == null) return false;
        writeToUiAppend(resultNfcWriting, printData("exportedVirtualFile\n", exportedVf));
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, exportedVf, 0, exportedVf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "saving of the Virtual File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Virtual File, aborted");
            return false;
        }
        return true;
    }

    private VirtualFile loadVirtualFile() {
        boolean success;
        // read the virtual file
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: read the virtual file from file 03");
        byte[] contentFile03 = ntag424DnaMethods.readStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, 0, 128);
        if ((contentFile03 == null) || (contentFile03.length != 128)) {
            // error when reading the file
            writeToUiAppend(resultNfcWriting, "FAILURE in reading the Virtual File, aborted");
            return null;
        } else {
            writeToUiAppend(resultNfcWriting, "SUCCESS in reading the Virtual File");
        }
        // build the virtual file
        VirtualFile vf = new VirtualFile(contentFile03, true);
        if (!vf.isVirtualFileValid()) {
            writeToUiAppend(resultNfcWriting, "The Virtual File is INVALID, aborted");
            return null;
        }
        return vf;
    }

    private boolean creditTransaction(VirtualFile vf, String bookingUnits, byte machineNumber, byte goodType) {
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a credit transaction");
        // run a credit/charge transaction
        String timestampShort = Utils.getTimestampShort();
        Log.d(TAG, "timestampShort: " + timestampShort);
        String creditDebitMarker = "C";
        TransactionRecord tr = new TransactionRecord(timestampShort, creditDebitMarker, bookingUnits, machineNumber, goodType);
        if (!tr.isRecordValid()) {
            writeToUiAppend(resultNfcWriting, "Error: TransactionRecord is not valid, aborted");
            return false;
        }
        vf.credit(applicationKey4, Integer.parseInt(bookingUnits));
        vf.addRecord(applicationKey4, tr.getRecord());
        // write data back to files
        byte[] exportedVf = vf.exportVirtualFile();
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, exportedVf, 0, exportedVf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Virtual File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Virtual File, aborted");
            return false;
        }
        return true;
    }

    private boolean debitTransaction(VirtualFile vf, String bookingUnits, byte machineNumber, byte goodType) {
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a debit transaction");
        String timestampShort = Utils.getTimestampShort();
        Log.d(TAG, "timestampShort: " + timestampShort);
        String creditDebitMarker = "D";
        TransactionRecord tr = new TransactionRecord(timestampShort, creditDebitMarker, bookingUnits, machineNumber, goodType);
        if (!tr.isRecordValid()) {
            writeToUiAppend(resultNfcWriting, "Error: TransactionRecord is not valid, aborted");
            return false;
        }
        vf.debit(Integer.parseInt(bookingUnits));
        vf.addRecord(applicationKey4, tr.getRecord());
        // write data back to files
        byte[] exportedVf = vf.exportVirtualFile();
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, exportedVf, 0, exportedVf.length, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "save the Virtual File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Virtual File, aborted");
            return false;
        }
        return true;
    }

    private void showTransactions(VirtualFile vf) {
        List<byte[]> transactionRecordsByte= vf.getRecordList();
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
        final int balance = vf.getBalance();
        getActivity().runOnUiThread(() -> {
            // setting layout manager for our recycler view.
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
            transactionRV.setLayoutManager(linearLayoutManager);
            // setting our adapter to recycler view.
            transactionRV.setAdapter(transactionRVAdapter);
            remainingDeposit.setText(Utils.convertIntegerInFloatString(balance));
        });
        writeToUiAppend(resultNfcWriting, "Number of recorded transactions: " + transactionModelArrayList.size());
    }

    private NdefMessage createNdefMessage(VirtualFile vf) {
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
        NdefRecord ndefRecord = NdefRecord.createTextRecord("en", record);
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage;
    }

    private Ndef changeTagTechnologyFromIsoDepToNdef() {
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: change tag technology from IsoDep to Ndef");
        try {
            IsoDep isoDep = IsoDep.get(discoveredTag);
            isoDep.close();
            Ndef ndef = Ndef.get(discoveredTag);
            ndef.connect();
            writeToUiAppend(resultNfcWriting, "change tag technology from IsoDep to Ndef was SUCCESSFUL");
            return ndef;
        } catch (IOException e) {
            //throw new RuntimeException(e);
            writeToUiAppend(resultNfcWriting, "FAILURE in changing tag technology from IsoDep to Ndef, aborted " + e.getMessage());
            return null;
        }
    }

    private boolean changeTagTechnologyFromNdefToIsoDep() {
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: change tag technology from Ndef to IsoDep");
        try {
            Ndef ndef = Ndef.get(discoveredTag);
            ndef.close();
            IsoDep isoDep = IsoDep.get(discoveredTag);
            isoDep.connect();
        } catch (IOException e) {
            //throw new RuntimeException(e);
            writeToUiAppend(resultNfcWriting, "FAILURE in changing tag technology from Ndef to IsoDep, aborted " + e.getMessage());
            return false;
        }
        writeToUiAppend(resultNfcWriting, "change tag technology back from Ndef to IsoDep was SUCCESSFUL");
        return true;
    }

    private boolean writeVirtualIdentificationFileToFile02(byte[] exportedVif){
        boolean success;
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write a Virtual Identification File in file 02");
        if (exportedVif == null) return false;
        writeToUiAppend(resultNfcWriting, printData("exportedVirtualIdentificationFile\n", exportedVif));
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, exportedVif, 192, exportedVif.length);
        if (success) {
            writeToUiAppend(resultNfcWriting, "saving of the Virtual Identification File was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in saving the Virtual Identification File, aborted");
            return false;
        }
        return true;
    }

    private VirtualIdentificationFile loadVirtualIdentificationFileFromFile02(){
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: load a Virtual Identification File from file 02");
        byte[] importedVif = ntag424DnaMethods.readStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, 192, 64);
        if ((importedVif == null) || (importedVif.length != 64)) {
            writeToUiAppend(resultNfcWriting, "FAILURE in loading the Virtual Identification File, aborted");
            return null;
        }
        VirtualIdentificationFile vif = new VirtualIdentificationFile(importedVif);
        if (!vif.isVirtualIdentificationFileValid()) {
            writeToUiAppend(resultNfcWriting, "FAILURE in loading the Virtual Identification File, aborted");
            return null;
        }
        writeToUiAppend(resultNfcWriting, "loading of the Virtual Identification File was SUCCESSFUL");
        return vif;
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
        if (!selectApplication()) return false;

        // change file settings of file 02, set r&w and w keys from E to 4

        // step 2 authenticate with default key 0 = car
        if (!authenticate(Constants.applicationKeyNumber0, defaultApplicationKey)) return false;

        // change file settings of file 02, set r&w and w keys from E to 4
        if (!changeFile02SettingsWriteKey4()) return false;

        // step 2 write the NDEF template to the file 02

        // step  authenticate with key 00 (read & write key)
        if (!authenticate(Constants.applicationKeyNumber0, defaultApplicationKey)) return false;

        // step 1 write the changed NDEF compatibility container to the file 01
        if (!writeFile01ModifiedCompatibilityContainer()) return false;

        /*

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

*/

        // step  authenticate with key 03 (read & write key)
        if (!authenticate(Constants.applicationKeyNumber3, defaultApplicationKey)) return false;

        // create a Virtual File and write to file 03
        VirtualFile vfCreated = createVirtualFile();
        if (!vfCreated.isVirtualFileValid()) return false;
        if (!writeVirtualFileToFile03(vfCreated)) return false;

        // this is for testing a transaction workflow
        // read the virtual file
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: testing the transaction workflow");

        // load the virtual file
        VirtualFile vf = loadVirtualFile();
        if ((vf == null) || (!vf.isVirtualFileValid())) return false;

        // run a credit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a credit transaction");
        // run a credit/charge transaction
        String creditDebitMarker = "C";
        String bookingUnits = "010000";
        byte machineNumber = (byte) 0x01;
        byte goodType = (byte) 0x00;
        if (!creditTransaction(vf, bookingUnits, machineNumber, goodType));

        // run a debit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a debit transaction");
        creditDebitMarker = "D";
        bookingUnits = "000340";
        machineNumber = (byte) 0x09;
        goodType = (byte) 0x01;
        if (!debitTransaction(vf, bookingUnits, machineNumber, goodType));

        // run a 2.nd debit transaction
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: run a debit transaction");
        creditDebitMarker = "D";
        // random data
        int bUnits = Utils.generateRandomIntInRange(100, 1000);
        bookingUnits = String.format("%06d", bUnits); // gives a 6 digit long string
        //bookingUnits = "000230";
        machineNumber = (byte) 0x17;
        goodType = (byte) 0x02;
        if (!debitTransaction(vf, bookingUnits, machineNumber, goodType));

        // write data back to files
        if (!writeVirtualFileToFile03(vf)) return false;

        // show transactions
        showTransactions(vf);

        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: workflow for writing the NDEF message");
        // step  authenticate with key 00 (car key)
        if (!authenticate(Constants.applicationKeyNumber0, defaultApplicationKey)) return false;

        // change the file settings of file 02, set r&w and w key from 4 to E
        if (!changeFile02SettingsWriteKeyE()) return false;

        // this is the personalization with cardholder name and card id
        VirtualIdentificationFile vif = new VirtualIdentificationFile();
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
        vif.setCardholderName(cardholderNameString.getBytes(StandardCharsets.UTF_8));
        vif.setCardId(cardholderIdString.getBytes(StandardCharsets.UTF_8));
        byte[] exportedVif = vif.exportVif();
        if ((exportedVif == null) || (exportedVif.length == 0)) return false;

        // write exportedVif to file 02 at position 193 = offset 192
        // write data back to files
        if (!writeVirtualIdentificationFileToFile02(exportedVif)) return false;

        // load Virtual Identification File
        VirtualIdentificationFile vifLoaded = loadVirtualIdentificationFileFromFile02();
        if (vifLoaded.isVirtualIdentificationFileValid()) {
            writeToUiAppend(resultNfcWriting, "cardholder name: " + new String(vifLoaded.getCardholderName(), StandardCharsets.UTF_8));
            writeToUiAppend(resultNfcWriting, "cardId: " + new String(vifLoaded.getCardId(), StandardCharsets.UTF_8));
        }

        // create a NDEF message
        NdefMessage ndefMessage = createNdefMessage(vf);
        if ((ndefMessage.toByteArray() == null) || (ndefMessage.toByteArray().length == 0)) return false;

        // change tag technology from IsoDep to Ndef
        Ndef ndef = changeTagTechnologyFromIsoDepToNdef();
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 0x: write NDEF message to tag");
        try {
            ndef.writeNdefMessage(ndefMessage);
            writeToUiAppend(resultNfcWriting, "write NDEF message to file 02 was SUCCESSFUL");
        } catch (IOException | FormatException e) {
            //throw new RuntimeException(e);
            writeToUiAppend(resultNfcWriting, "FAILURE in writing NDEF message to file 02, aborted " + e.getMessage());
            return false;
        }

        // change tag technology from Ndef to IsoDep
        if (!changeTagTechnologyFromNdefToIsoDep()) return false;

        if (!selectApplication()) return false;

        // step  authenticate with key 00 (car key)
        if (!authenticate(Constants.applicationKeyNumber0, defaultApplicationKey)) return false;

        // change the file settings of file 02, set r&w and w key from E to 4
        if (!changeFile02SettingsWriteKey4()) return false;

        writeToUiAppend(resultNfcWriting, "The tag was personalized with SUCCESS");
        Log.d(TAG, "The tag was personalized with SUCCESS");
        playSinglePing(getContext());
        return true;
    }

    private boolean runResetToOriginalSettings() {
        /*
        This are the steps that will run when a tag is tapped:

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

        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 1b: read all file settings");
        FileSettings[] allFileSettings = ntag424DnaMethods.getAllFileSettings();
        FileSettings fs01 = allFileSettings[0];
        writeToUiAppend(resultNfcWriting, fs01.dump());
        FileSettings fs02 = allFileSettings[1];
        writeToUiAppend(resultNfcWriting, fs02.dump());
        FileSettings fs03 = allFileSettings[2];
        writeToUiAppend(resultNfcWriting, fs03.dump());

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

        // step 3 change the fileSettings of file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 03: change the fileSettings of file 02");
        success = ntag424DnaMethods.changeFileSettings(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, Ntag424DnaMethods.CommunicationSettings.Plain, 14, 0, 14, 14, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "change the fileSettings of file 02 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in changing the fileSettings of file 02, aborted");
            //todo return false;
        }

        // step 7 authenticate with default key
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 07: authenticate the application with default key 2");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber2, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 2 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 2, aborted");
            return false;
        }

        // step 4 change the fileSettings of file 03
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 04: change the fileSettings of file 03");
        success = ntag424DnaMethods.changeFileSettings(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, Ntag424DnaMethods.CommunicationSettings.Full, 3, 0, 2, 3, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "change the fileSettings of file 03 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in changing the fileSettings of file 03, aborted");
            //todo return false;
        }

        // step 5 restore the data in file 01 (original NDEF compatibility container)
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 5: restore the data in file 01 (original NDEF compatibility container)");
        byte[] originalNdefCompatibilityContainer = Utils.hexStringToByteArray("001720010000ff0406e104010000000506e10500808283000000000000000000");
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_01, originalNdefCompatibilityContainer, 0, 32);
        if (success) {
            writeToUiAppend(resultNfcWriting, "restore the data in file 01 (original NDEF compatibility container) was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in restoring the data in file 01 (original NDEF compatibility container), aborted");
            // todo return false;
        }

        // step 6 restore the data in file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 6: restore the data in file 02");
        byte[] originalFile02Content = new byte[256];
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, originalFile02Content, 0, 256);
        if (success) {
            writeToUiAppend(resultNfcWriting, "restore the data in file 02 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in restoring the data in file 02, aborted");
            // todo return false;
        }

        // step 7 authenticate with default key
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 07: authenticate the application with default key 1");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber1, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 1 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 1, aborted");
            return false;
        }

        // step 8 restore the data in file 03
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 8: restore the data in file 03");
        //
        byte[] originalFile03Content = Utils.hexStringToByteArray("007e000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, originalFile03Content, 0, 128, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "restore the data in file 03 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in restoring the data in file 03, aborted");
            // todo return false;
        }

        writeToUiAppend(resultNfcWriting, "The tag was reset with SUCCESS");
        Log.d(TAG, "The tag was reset with SUCCESS");
        playSinglePing(getContext());
        return true;
    }

    private boolean runRestoreOriginalFileContent() {
        /*
        This are the steps that will run when a tag is tapped:

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

        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 1b: read all file settings");
        FileSettings[] allFileSettings = ntag424DnaMethods.getAllFileSettings();
        FileSettings fs01 = allFileSettings[0];
        writeToUiAppend(resultNfcWriting, fs01.dump());
        FileSettings fs02 = allFileSettings[1];
        writeToUiAppend(resultNfcWriting, fs02.dump());
        FileSettings fs03 = allFileSettings[2];
        writeToUiAppend(resultNfcWriting, fs03.dump());

        // step 4 authenticate with default key
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 04: authenticate the application with default key 0");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber0, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 0 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 0, aborted");
            return false;
        }

        // step 2 restore the data in file 01 (original NDEF compatibility container)
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 2: restore the data in file 01 (original NDEF compatibility container)");
        byte[] originalNdefCompatibilityContainer = Utils.hexStringToByteArray("001720010000ff0406e104010000000506e10500808283000000000000000000");
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_01, originalNdefCompatibilityContainer, 0, 32);
        if (success) {
            writeToUiAppend(resultNfcWriting, "restore the data in file 01 (original NDEF compatibility container) was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in restoring the data in file 01 (original NDEF compatibility container), aborted");
            // todo return false;
        }

        // step 3 restore the data in file 02
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 3: restore the data in file 02");
        byte[] originalFile02Content = new byte[128];
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, originalFile02Content, 0, 128);

        //byte[] originalFile02Content = new byte[256];
        success = ntag424DnaMethods.writeStandardFilePlain(Ntag424DnaMethods.STANDARD_FILE_NUMBER_02, originalFile02Content, 128, 128);

        if (success) {
            writeToUiAppend(resultNfcWriting, "restore the data in file 02 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in restoring the data in file 02, aborted");
            // todo return false;
        }

        // step 4 authenticate with default key
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 04: authenticate the application with default key 3");
        success = ntag424DnaMethods.authenticateAesEv2First(Constants.applicationKeyNumber3, defaultApplicationKey);
        if (success) {
            writeToUiAppend(resultNfcWriting, "authenticate the application with default key 3 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in authenticate the application with default key 3, aborted");
            return false;
        }

        // step 5 restore the data in file 03
        writeToUiAppend(resultNfcWriting, lineSeparator);
        writeToUiAppend(resultNfcWriting, "step 5: restore the data in file 03");
        //
        byte[] originalFile03Content = Utils.hexStringToByteArray("007e000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        success = ntag424DnaMethods.writeStandardFileFull(Ntag424DnaMethods.STANDARD_FILE_NUMBER_03, originalFile03Content, 0, 128, false);
        if (success) {
            writeToUiAppend(resultNfcWriting, "restore the data in file 03 was SUCCESSFUL");
        } else {
            writeToUiAppend(resultNfcWriting, "FAILURE in restoring the data in file 03, aborted");
            // todo return false;
        }

        writeToUiAppend(resultNfcWriting, "The tag was reset with SUCCESS");
        Log.d(TAG, "The tag was reset with SUCCESS");
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
        //boolean success = runResetToOriginalSettings();
        //boolean success = runRestoreOriginalFileContent();
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