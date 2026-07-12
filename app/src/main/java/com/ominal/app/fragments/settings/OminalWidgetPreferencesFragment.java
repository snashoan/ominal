package com.ominal.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ominal.R;
import com.ominal.shared.runtime.settings.preferences.OminalWidgetAppSharedPreferences;

@Keep
public class OminalWidgetPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(OminalWidgetPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.ominal_widget_preferences, rootKey);
    }

}

class OminalWidgetPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final OminalWidgetAppSharedPreferences mPreferences;

    private static OminalWidgetPreferencesDataStore mInstance;

    private OminalWidgetPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = OminalWidgetAppSharedPreferences.build(context, true);
    }

    public static synchronized OminalWidgetPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new OminalWidgetPreferencesDataStore(context);
        }
        return mInstance;
    }

}
