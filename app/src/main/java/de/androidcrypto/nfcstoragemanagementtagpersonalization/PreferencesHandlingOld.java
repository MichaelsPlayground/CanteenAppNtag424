package de.androidcrypto.nfcstoragemanagementtagpersonalization;

import static android.content.Context.MODE_PRIVATE;
import static de.androidcrypto.nfcstoragemanagementtagpersonalization.Constants.*;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.TextView;

public class PreferencesHandlingOld {
    private Activity activity;
    private Context context;
    private TextView textView; // used for displaying information's from the methods

    public PreferencesHandlingOld(Activity activity, Context context, TextView textView) {
        this.activity = activity;
        this.context = context;
        this.textView = textView;
    }

    public String getPreferencesString(String preferenceName) {
        String preference = "";
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            preference = prefs.getString(preferenceName, null);
            if ((preference == null) || (preference.length() < 1)) {
                writeToUiAppend(textView, "Please setup the NDEF settings, aborted");
                return "";
            }
        } catch (NullPointerException e) {
            writeToUiAppend(textView, "Please setup the NDEF settings, aborted");
            return "";
        }
        return preference;
    }

    /**
     * reads a value/String from SharedPreferences
     * @param preferenceName
     * @param preferenceHeader
     * @param preferenceFooter
     * @return the value or an empty string if a value is not saved before
     */
    public String getPreferencesMatchString(String preferenceName, String preferenceHeader, String preferenceFooter) {
        String preference = "";
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            preference = prefs.getString(preferenceName, null);
            if ((preference == null) || (preference.length() < 1)) {
                writeToUiAppend(textView, "Please setup the NDEF settings, aborted");
                return "";
            }
        } catch (NullPointerException e) {
            writeToUiAppend(textView, "Please setup the NDEF settings, aborted");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        // uid
        sb.append(preferenceHeader);
        sb.append(preference);
        sb.append(preferenceFooter);
        String preferenceString = sb.toString();
        writeToUiAppend(textView, "preferenceString: " + preferenceString);
        return preferenceString;
    }

    /**
     * returns the position of a placeholder ('matchString') within a string
     * @param content
     * @param matchString
     * @return
     */
    public int getPlaceholderPosition(String content, String matchString) {
        return content.indexOf(matchString) + matchString.length();
    }

    /**
     * service methods
     */

    private void writeToUiAppend(TextView textView, String message) {
        activity.runOnUiThread(() -> {
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
}
