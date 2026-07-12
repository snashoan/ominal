package com.ominal.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ominal.R;
import com.ominal.shared.runtime.settings.preferences.OminalFloatAppSharedPreferences;

@Keep
public class OminalFloatPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(OminalFloatPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.ominal_float_preferences, rootKey);
    }

}

class OminalFloatPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final OminalFloatAppSharedPreferences mPreferences;

    private static OminalFloatPreferencesDataStore mInstance;

    private OminalFloatPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = OminalFloatAppSharedPreferences.build(context, true);
    }

    public static synchronized OminalFloatPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new OminalFloatPreferencesDataStore(context);
        }
        return mInstance;
    }

}
