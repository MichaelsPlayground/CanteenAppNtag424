package de.androidcrypto.canteenapp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

public class BackupActivity extends AppCompatActivity {
    private static final String TAG = BackupActivity.class.getName();
    private com.google.android.material.textfield.TextInputEditText result;
    private RadioButton rbBackupBackup, rbBackupRestore;
    private Button runAction;

    private PreferencesHandling prefs;
    private String exportString = "BackStorePers"; // takes the log data for export
    private byte[] exportData, importData;
    private String exportStringFileName = "bkstps.html"; // takes the log data for export

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        result = findViewById(R.id.etBackupResult);
        rbBackupBackup = findViewById(R.id.rbBackupBackup);
        rbBackupRestore = findViewById(R.id.rbBackupRestore);
        runAction = findViewById(R.id.btnBackupRunAction);

        runAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs = new PreferencesHandling(BackupActivity.this, view.getContext(), result);
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
                    /*
                    if ((importData == null) || (importData.length < 1)) {
                        writeToUiAppend(result, "FAILURE on reading the data from file, aborted");
                        return;
                    }
                    writeToUiAppend(result, Utils.printData("preferencesData", importData));
                    ByteArrayInputStream bis = new ByteArrayInputStream(importData);
                    boolean success = prefs.deserialize(bis);
                    if (!success) {
                        writeToUiAppend(result, "FAILURE on restoring the preferences from file, aborted");
                        return;
                    } else {
                        writeToUiAppend(result, "SUCCESS on restoring the preferences from file");
                    }

                     */
                }
            }
        });

    }

    /**
     * section for reading data from external storage (restore)
     */

    private void readByteArrayFromExternalSharedStorage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        selectReadDataFileActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> selectReadDataFileActivityResultLauncher = registerForActivityResult(
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
                            importData = readByteArrayFromUri(uri);

                            if ((importData == null) || (importData.length < 1)) {
                                writeToUiAppend(result, "FAILURE on reading the data from file, aborted");
                                return;
                            }
                            writeToUiAppend(result, Utils.printData("preferencesData", importData));
                            ByteArrayInputStream bis = new ByteArrayInputStream(importData);
                            boolean success = prefs.deserialize(bis);
                            if (!success) {
                                writeToUiAppend(result, "FAILURE on restoring the preferences from file, aborted");
                                return;
                            } else {
                                writeToUiAppend(result, "SUCCESS on restoring the preferences from file");
                            }

                        }
                    }
                }
            });

    private byte[] readByteArrayFromUri(Uri uri) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = null;
        byte[] buf = new byte[1024];
        int len;
        try {
            ContentResolver r = getApplicationContext().getContentResolver();
            in = r.openInputStream(uri);
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return out.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException: " + e.getMessage());
                }
            }
        }
    }

    /**
     * section for writing data to external storage (backup)
     */

    private void writeByteArrayToExternalSharedStorage() {
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
        if ((exportData == null) || (exportData.length < 1)) {
            writeToUiToast("run the export path before writing the content to a file :-)");
            return;
        }
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        selectWriteDataFileActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> selectWriteDataFileActivityResultLauncher = registerForActivityResult(
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
                            writeByteArrayToUri(uri, exportData);

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

    private void writeTextToUri(Uri uri, String data) throws IOException {
        try {
            System.out.println("** data to write: " + data);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext().getContentResolver().openOutputStream(uri));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            System.out.println("Exception File write failed: " + e.toString());
        }
    }



    /**
     * section for UI service methods
     */

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
                Intent intent = new Intent(BackupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}