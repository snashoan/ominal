package com.ominal.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ominal.R;
import com.ominal.shared.runtime.settings.preferences.OminalAPIAppSharedPreferences;

@Keep
public class OminalAPIPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(OminalAPIPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.ominal_api_preferences, rootKey);
    }

}

class OminalAPIPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final OminalAPIAppSharedPreferences mPreferences;

    private static OminalAPIPreferencesDataStore mInstance;

    private OminalAPIPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = OminalAPIAppSharedPreferences.build(context, true);
    }

    public static synchronized OminalAPIPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new OminalAPIPreferencesDataStore(context);
        }
        return mInstance;
    }

}
