package de.androidcrypto.canteenapp;

import static android.content.Context.MODE_PRIVATE;
import static de.androidcrypto.canteenapp.Constants.MAC_FOOTER;
import static de.androidcrypto.canteenapp.Constants.MAC_HEADER;
import static de.androidcrypto.canteenapp.Constants.MAC_NAME;
import static de.androidcrypto.canteenapp.Constants.MAC_TEMPLATE;
import static de.androidcrypto.canteenapp.Constants.NDEF_BASE_URL_TEMPLATE;
import static de.androidcrypto.canteenapp.Constants.NDEF_TEMPLATE_STRING_MAXIMUM_LENGTH;
import static de.androidcrypto.canteenapp.Constants.PREFS_BASE_URL;
import static de.androidcrypto.canteenapp.Constants.PREFS_MAC_NAME;
import static de.androidcrypto.canteenapp.Constants.PREFS_NAME;
import static de.androidcrypto.canteenapp.Constants.PREFS_TAG_PACK_NAME;
import static de.androidcrypto.canteenapp.Constants.PREFS_TAG_PASSWORD_NAME;
import static de.androidcrypto.canteenapp.Constants.PREFS_TEMPLATE_URL_NAME;
import static de.androidcrypto.canteenapp.Constants.PREFS_UID_NAME;
import static de.androidcrypto.canteenapp.Constants.TAG_PACK;
import static de.androidcrypto.canteenapp.Constants.TAG_PASSWORD;
import static de.androidcrypto.canteenapp.Constants.UID_FOOTER;
import static de.androidcrypto.canteenapp.Constants.UID_HEADER;
import static de.androidcrypto.canteenapp.Constants.UID_NAME;
import static de.androidcrypto.canteenapp.Constants.UID_TEMPLATE;
import static de.androidcrypto.canteenapp.Utils.bytesToHexNpe;
import static de.androidcrypto.canteenapp.Utils.doVibrate;

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
 * Use the {@link ChargeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChargeFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private com.google.android.material.textfield.TextInputEditText chargeAmount, depositAmount;
    private com.google.android.material.textfield.TextInputEditText ndefResultNfcWriting;

    private static String ndefTemplateString = "";

    private NfcAdapter mNfcAdapter;

    public ChargeFragment() {
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
    public static ChargeFragment newInstance(String param1, String param2) {
        ChargeFragment fragment = new ChargeFragment();
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
        return inflater.inflate(R.layout.fragment_charge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        chargeAmount = getView().findViewById(R.id.etChargeAmount);
        depositAmount = getView().findViewById(R.id.etChargeDepositAmount);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());
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