package de.androidcrypto.nfcstoragemanagementtagpersonalization;

import static android.content.Context.MODE_PRIVATE;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Constants.*;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Utils.doVibrate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NdefSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NdefSettingsFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private com.google.android.material.textfield.TextInputEditText ndefBaseUrl, ndefUidName, ndefMacName, ndefResultNfcWriting;
    private Button testNdefSettings;
    public static final int UID_LENGTH = 14; // hex encoding
    //public static final int READ_COUNTER_LENGTH = 12; // hex encoding
    public static final int MAC_LENGTH = 8; // hex encoding
    //public static final String UID_HEADER = "?";
    //public static String UID_NAME = "";
    //public static final String UID_FOOTER = "=";
    //public static final String UID_TEMPLATE = "11111111111111";
    //public static final String READ_COUNTER_TEMPLATE = "111111111111";
    //public static final String MAC_HEADER = "&";
    //public static String MAC_NAME = "";
    //public static final String MAC_FOOTER = "=";
    //public static final String MAC_TEMPLATE = "22222222";
/*
    private static String ndefTemplateString = "";
    private static final String NDEF_BASE_URL_TEMPLATE = "http://fluttercrypto.bplaced.net/apps/ntag/get_reg3.php";
    public static final int NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH = 137; // maximum length of an NDEF message on a NTAG213
    public static final String PREFS_NAME = "prefs";
    public static final String PREFS_BASE_URL = "baseUrl";
    public static final String PREFS_UID_NAME = "uid";
    public static final String PREFS_MAC_NAME = "mac";
    public static final String PREFS_TEMPLATE_URL_NAME = "templateUrl";
*/
    private static String ndefTemplateString = "";

    private NfcAdapter mNfcAdapter;

    public NdefSettingsFragment() {
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
    public static NdefSettingsFragment newInstance(String param1, String param2) {
        NdefSettingsFragment fragment = new NdefSettingsFragment();
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
        return inflater.inflate(R.layout.fragment_ndef_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ndefBaseUrl = getView().findViewById(R.id.etNdefSettingsBaseUrl);
        ndefBaseUrl.setText(NDEF_BASE_URL_TEMPLATE);
        //inputField1Decoration = getView().findViewById(R.id.etMainInputline1Decoration);
        ndefUidName = getView().findViewById(R.id.etNdefSettingsUidName);
        //ndefReadCounterName = getView().findViewById(R.id.etNdefSettingsReadCounterName);
        ndefMacName = getView().findViewById(R.id.etNdefSettingsMacName);
        ndefResultNfcWriting = getView().findViewById(R.id.etNdefSettingsResult);
        testNdefSettings = getView().findViewById(R.id.btnNdefSettingsTest);

        fetchButtonClicks(view);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());
    }

    private void fetchButtonClicks(View view){

        testNdefSettings = getView().findViewById(R.id.btnNdefSettingsTest);
        //testNdefSettings = view.findViewById(R.id.btnNdefSettingsTest); // todo check if this is better !
        testNdefSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // test if the complete template string is correct
                // we are going to build the template string
                UID_NAME = ndefUidName.getText().toString();
                MAC_NAME = ndefMacName.getText().toString();
                StringBuilder sb = new StringBuilder();
                sb.append(ndefBaseUrl.getText().toString());
                sb.append(UID_HEADER); // "?"
                sb.append(UID_NAME);
                sb.append(UID_FOOTER); // "="
                sb.append(UID_TEMPLATE);
                // todo check for switch Mac
                sb.append(MAC_HEADER); // "&"
                sb.append(MAC_NAME);
                sb.append(MAC_FOOTER); // "="
                sb.append(MAC_TEMPLATE);
                ndefTemplateString = sb.toString();
                int ndefTemplateStringLength = ndefTemplateString.length();
                if (ndefTemplateStringLength > NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH) {
                    // todo use colored border
                    ndefResultNfcWriting.setText("NDEF template string length is " +
                            ndefTemplateStringLength +
                            " that is longer than the maximum of " +
                            NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH +
                            ", aborted");
                    ndefTemplateString = "";
                    return;
                } else {
                    ndefResultNfcWriting.setText("NDEF template string:\n" +
                            ndefTemplateString);
                }
                // check on valid url
                if (!isValidUrl(ndefTemplateString)) {
                    ndefResultNfcWriting.setText("NDEF template string contains illegal characters, aborted");
                    ndefTemplateString = "";
                    return;
                }
                // save the settings to preferences
                SharedPreferences.Editor editor = getContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString(PREFS_UID_NAME, UID_NAME);
                editor.apply();
                editor.putString(PREFS_MAC_NAME, MAC_NAME);
                editor.apply();
                editor.putString(PREFS_BASE_URL, ndefBaseUrl.getText().toString());
                editor.apply();
                editor.putString(PREFS_TEMPLATE_URL_NAME, ndefTemplateString);
                editor.apply();
                ndefResultNfcWriting.setText("NDEF settings saved");
            }
        });
    }

    boolean isValidUrl(String url) {
        if (url.length() == 0) return false;
        try {
            // it will check only for scheme and not null input
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an NDEF Technology Type

        Ndef mNdef = Ndef.get(tag);

        // Check that it is an Ndef capable card
        if (mNdef != null) {

            // the tag is written here
            try {
                mNdef.connect();

                // check that the tag is writable
                if (!mNdef.isWritable()) {
                    showMessage("NFC tag is not writable");
                    return;
                }

                // check that the tag has sufficient memory to write the ndef message
                int messageSize = ndefTemplateString.length();
                if (messageSize > NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH) {
                    showMessage("Message is too large to write on NFC tag, aborted");
                    return;
                }
                if (messageSize == 0) {
                    showMessage("Message is empty (run 'test and save NDEF settings' first, aborted");
                    return;
                }
                NdefRecord ndefRecord = NdefRecord.createUri(ndefTemplateString);
                NdefMessage ndefMessage= new NdefMessage(ndefRecord);

                mNdef.writeNdefMessage(ndefMessage);
                // Success if got to here
                showMessage("write to NFC success, total message size is " + messageSize);
            } catch (FormatException e) {
                showMessage("FormatException: " + e.getMessage());
                // if the NDEF Message to write is malformed
            } catch (TagLostException e) {
                showMessage("TagLostException: " + e.getMessage());
                // Tag went out of range before operations were complete
            } catch (IOException e) {
                // if there is an I/O failure, or the operation is cancelled
                showMessage("IOException: " + e.getMessage() + " I'm trying to format the tag... please try again");
                // try to format the tag
                formatNdef(tag);
            } finally {
                // Be nice and try and close the tag to
                // Disable I/O operations to the tag from this TagTechnology object, and release resources.
                try {
                    mNdef.close();
                } catch (IOException e) {
                    // if there is an I/O failure, or the operation is cancelled
                    showMessage("IOException on close: " + e.getMessage());
                }
            }
            doVibrate(getActivity());
            //playSinglePing(getContext());
        } else {
            showMessage("mNdef is null, not an NDEF formatted tag, trying to format the tag");
            // trying to format the tag
            formatNdef(tag);
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
            showMessage("Tag is not formattable or already formatted to NDEF");
        }
    }

    private void showMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
            ndefResultNfcWriting.setText(message);
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