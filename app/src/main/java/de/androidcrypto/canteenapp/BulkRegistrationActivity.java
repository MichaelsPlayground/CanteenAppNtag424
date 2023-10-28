package de.androidcrypto.canteenapp;

import static de.androidcrypto.canteenapp.Utils.bytesToHexNpe;
import static de.androidcrypto.canteenapp.Utils.printData;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class BulkRegistrationActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private static final String TAG = BulkRegistrationActivity.class.getName();
    private com.google.android.material.textfield.TextInputEditText result, numberOfTags, tagIdList;
    private RadioButton rbBackupBackup, rbBackupRestore;
    private Button runAction;

    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;
    private byte[] tagUid; // written by onDiscovered
    private List<byte[]> tagUidList;
    private int numberOfTagsInt = 0;

    private PreferencesHandling prefs;
    private String exportString = ""; // takes the list of tagUids in hex encoding, separated by a new line
    private byte[] exportData, importData;
    private String exportStringFileName = "bulk.dat"; // takes the log data for export

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bulk_registration);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        result = findViewById(R.id.etBulkRegistrationResult);
        numberOfTags = findViewById(R.id.etBulkRegistrationNumberOfTags);
        tagIdList = findViewById(R.id.etBulkRegistrationTagList);
        rbBackupBackup = findViewById(R.id.rbBulkRegistrationBackup);
        rbBackupRestore = findViewById(R.id.rbBulkRegistrationRestore);
        runAction = findViewById(R.id.btnBulkRegistrationRunAction);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        tagUidList = new ArrayList<>();
        numberOfTagsInt = 0;

        runAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportString = "";
                int numberOfTagInList = tagUidList.size();
                if (numberOfTagInList == 0) {
                    // this should never happen as the  button is enabled on a registration
                    writeToUiAppend("There are not tagUids registered before, aborted");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < numberOfTagInList; i++) {
                    byte[] uid = tagUidList.get(i);
                    sb.append(bytesToHexNpe(uid));
                    if (i < (numberOfTagsInt - 1)) {
                        sb.append("\n");
                    }
                }
                exportString = sb.toString();
                writeStringToExternalSharedStorage();

                /*
                prefs = new PreferencesHandling(BulkRegistrationActivity.this, view.getContext(), result);
                if (rbBackupBackup.isChecked()) {
                    // run the backup path
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    boolean success = prefs.serialize(bos);
                    if (!success) {
                        writeToUiAppend(result, "FAILURE on reading the preferences for writing, aborted");
                        return;
                    }
                    exportData = bos.toByteArray();
                    if (exportData.length < 1) {
                        writeToUiAppend(result, "FAILURE on reading the preferences for writing, aborted");
                        return;
                    }
                    writeToUiAppend(result, Utils.printData("preferencesData", exportData));
                    writeByteArrayToExternalSharedStorage();
                } else {
                    // run the restore path
                    readByteArrayFromExternalSharedStorage();
                    // here we do get the following information, see selectReadDataFileActivityResultLauncher

                }*/
            }
        });

    }

    private void runRegistration() {
        // at this point we are sure it is a NTAG21x
        // check if the tagUid was registered during THIS activity lifetime
        // this does NOT check if the tag was registered before on backend server
        if (!Utils.listContains(tagUidList, tagUid)) {
            String uidString = bytesToHexNpe(tagUid);
            tagUidList.add(tagUid);
            numberOfTagsInt++;
            writeToUiAppend(tagIdList, uidString);
            runOnUiThread(() -> {
                runAction.setEnabled(true);
                numberOfTags.setText(String.valueOf(numberOfTagsInt));
                vibrateShort();
            });
        } else {
            writeToUiAppend("tag is registered before");
        }
    }


    /**
     * section for writing data to external storage (backup)
     */

    private void writeStringToExternalSharedStorage() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        // boolean pickerInitialUri = false;
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        // get filename from edittext
        String filename = exportStringFileName;
        // sanity check
        if (filename.equals("")) {
            writeToUiToast("run the export path before writing the content to a file :-)");
            return;
        }
        if ((exportString == null) || (exportString.length() < 1)) {
            writeToUiToast("run the export path before writing the content to a file :-)");
            return;
        }
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        selectWriteStringFileActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> selectWriteStringFileActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult aResult) {
                    if (aResult.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = aResult.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri uri = null;
                        if (resultData != null) {
                            uri = resultData.getData();
                            // Perform operations on the document using its URI.
                            // the file content is in exportData
                            writeTextToUri(uri, exportString);

/*
                            try {
                                String fileContent = exportString;
                                System.out.println("## data to write: " + exportString);
                                writeTextToUri(uri, fileContent);
                                writeToUiToast("file written to external shared storage");
                            } catch (IOException e) {
                                e.printStackTrace();
                                writeToUiToast("ERROR: " + e.toString());
                                return;
                            }

 */
                        }
                    }
                }
            });

    private void writeByteArrayToUri(Uri uri, byte[] data) {
        try (FileOutputStream fos = (FileOutputStream) getApplicationContext().getContentResolver().openOutputStream(uri)) {
            fos.write(data);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            writeToUiToast("FAILURE on writing to external shared storage");
            return;
        }
        writeToUiAppend(result, "SUCCESS on exporting the preferences");
        writeToUiToast("file written to external shared storage");
    }

    private void writeTextToUri(Uri uri, String data) {
        try {
            Log.d(TAG,"** data to write:\n" + data);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext().getContentResolver().openOutputStream(uri));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            writeToUiToast("FAILURE on writing to external shared storage");
            return;
        }
        writeToUiAppend(result, "SUCCESS on exporting the registration data to " + exportStringFileName);
        writeToUiToast("file written to external shared storage");
    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an NDEF Technology Type
        nfcA = NfcA.get(tag);
        try {
            nfcA = NfcA.get(tag);

            if (nfcA != null) {
                runOnUiThread(() -> {
                   result.setText("");
                });
                writeToUiAppend( "NFC tag is Nfca compatible");
                nfcA.connect();
                // check that the tag is a NTAG213/215/216
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if ((!ntagVersion.equals("213")) && (!ntagVersion.equals("215")) && (!ntagVersion.equals("216"))) {
                    writeToUiAppend("NFC tag is NOT of type NXP NTAG213/215/216, aborted");
                    return;
                }

                // tagUid
                tagUid = nfcA.getTag().getId();
                Log.d(TAG, printData("tagUid", tagUid));
                String nfcaContent = "tag is a " + NfcIdentifyNtag.getIdentifiedNtagType() + "\n" +
                        "tag ID: " + Utils.bytesToHexNpe(NfcIdentifyNtag.getIdentifiedNtagId()) + "\n";
                writeToUiAppend(nfcaContent);

                // proceed only if it is a NTAG21x - this was check at the beginning of this method
                runRegistration();
            }
        } catch (IOException e) {
            writeToUiAppend("ERROR: IOException " + e.toString());
            e.printStackTrace();
        } finally {
            try {
                nfcA.close();
            } catch (IOException e) {
                writeToUiAppend("ERROR: IOException " + e.toString());
                e.printStackTrace();
            }
        }
    }

    private void showWirelessSettings() {
        Toast.makeText(getApplicationContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
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
            mNfcAdapter.enableReaderMode(this,
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
            mNfcAdapter.disableReaderMode(this);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * section for UI service methods
     */

    private void writeToUiAppend(String message) {
        writeToUiAppend(result, message);
    }

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
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

    private void writeToUiToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void vibrateShort() {
        // Make a Sound
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(50, 10));
        } else {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(50);
        }
    }

    /**
     * section for options menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_return_home, menu);

        MenuItem mGoToHome = menu.findItem(R.id.action_return_main);
        mGoToHome.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(BulkRegistrationActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}