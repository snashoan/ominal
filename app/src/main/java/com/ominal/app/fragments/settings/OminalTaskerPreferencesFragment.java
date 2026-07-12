package com.ominal.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ominal.R;
import com.ominal.shared.runtime.settings.preferences.OminalTaskerAppSharedPreferences;

@Keep
public class OminalTaskerPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(OminalTaskerPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.ominal_tasker_preferences, rootKey);
    }

}

class OminalTaskerPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final OminalTaskerAppSharedPreferences mPreferences;

    private static OminalTaskerPreferencesDataStore mInstance;

    private OminalTaskerPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = OminalTaskerAppSharedPreferences.build(context, true);
    }

    public static synchronized OminalTaskerPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new OminalTaskerPreferencesDataStore(context);
        }
        return mInstance;
    }

}
