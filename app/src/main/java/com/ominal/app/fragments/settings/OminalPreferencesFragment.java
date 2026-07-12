package com.ominal.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ominal.R;
import com.ominal.shared.runtime.settings.preferences.OminalAppSharedPreferences;

@Keep
public class OminalPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(OminalPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.ominal_preferences, rootKey);
    }

}

class OminalPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final OminalAppSharedPreferences mPreferences;

    private static OminalPreferencesDataStore mInstance;

    private OminalPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = OminalAppSharedPreferences.build(context, true);
    }

    public static synchronized OminalPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new OminalPreferencesDataStore(context);
        }
        return mInstance;
    }

}
