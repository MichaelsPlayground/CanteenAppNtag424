package de.androidcrypto.canteenapp;

import static android.content.Context.MODE_PRIVATE;
import static de.androidcrypto.canteenapp.Constants.*;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

/**
 * The class is responsible for storing of data needed for activating or personalization of tags
 * All storage is done in Plaintext in Shared Preferences, consider using Encrypted Shared Preferences for more security
 */

public class PreferencesHandling {

    private static final String TAG = PreferencesHandling.class.getName();
    private Activity activity;
    private Context context;
    private TextView textView; // used for displaying information's from the methods
    private SharedPreferences prefs;

    public PreferencesHandling(Activity activity, Context context, TextView textView) {
        this.activity = activity;
        this.context = context;
        this.textView = textView;
        this.prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    public String getPreferencesString(String preferenceName) {
        String preference = "";
        try {
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
     * backup and restore methods
     */

    /**
     * Serialize all preferences into an output stream
     * @param os OutputStream to write to
     * @return True if successful
     * // source: https://stackoverflow.com/a/45552474/8166854 by PhilLab
     */
    public boolean serialize(final @NonNull OutputStream os) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(os);
            oos.writeObject(prefs.getAll());
            oos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error serializing preferences", BuildConfig.DEBUG ? e : null);
            return false;
        } finally {
            // Utils.closeQuietly(oos, os);
            try {
                if (os != null) os.close();
            } catch(IOException e) {
                //closing quietly
            }
            try {
                if (oos != null) oos.close();
            } catch(IOException e) {
                //closing quietly
            }
        }
        return true;
    }

    /**
     * Read all preferences from an input stream.
     * Schedules a full preference clean, then deserializes the options present in the given stream.
     * If the given object contains an unknown class, the deserialization is aborted and the underlying
     * preferences are not changed by this method
     * @param is Input stream to load the preferences from
     * @return True iff the new values were successfully written to persistent storage
     *
     * @throws IllegalArgumentException
     * // source: https://stackoverflow.com/a/45552474/8166854 by PhilLab
     */
    public boolean deserialize(final @NonNull InputStream is) {
        ObjectInputStream ois = null;
        Map<String, Object> map = null;
        try {
            ois = new ObjectInputStream(is);
            map = (Map) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Error deserializing preferences", BuildConfig.DEBUG ? e : null);
            return false;
        } finally {
            // Utils.closeQuietly(oos, os);
            try {
                if (is != null) is.close();
            } catch(IOException e) {
                //closing quietly
            }
            try {
                if (ois != null) ois.close();
            } catch(IOException e) {
                //closing quietly
            }
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            // Unfortunately, the editor only provides typed setters
            if (e.getValue() instanceof Boolean) {
                editor.putBoolean(e.getKey(), (Boolean)e.getValue());
            } else if (e.getValue() instanceof String) {
                editor.putString(e.getKey(), (String)e.getValue());
            } else if (e.getValue() instanceof Integer) {
                editor.putInt(e.getKey(), (int)e.getValue());
            } else if (e.getValue() instanceof Float) {
                editor.putFloat(e.getKey(), (float)e.getValue());
            } else if (e.getValue() instanceof Long) {
                editor.putLong(e.getKey(), (Long) e.getValue());
            } else if (e.getValue() instanceof Set) {
                editor.putStringSet(e.getKey(), (Set<String>) e.getValue());
            } else {
                throw new IllegalArgumentException("Type " + e.getValue().getClass().getName() + " is unknown");
            }
        }
        return editor.commit();
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
