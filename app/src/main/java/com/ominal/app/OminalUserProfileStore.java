package com.ominal.app;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Persists the canonical profile privately and exports one runtime-readable projection. */
final class OminalUserProfileStore {
    static final String PREFERENCE_KEY = "user_profile_v1";
    static final String RUNTIME_PATH = "/root/.ominal/profile.json";

    private OminalUserProfileStore() {
    }

    @NonNull
    static OminalUserProfile load(@NonNull SharedPreferences preferences) {
        String encoded = preferences.getString(PREFERENCE_KEY, "");
        if (encoded == null || encoded.trim().isEmpty()) return OminalUserProfile.empty();
        try {
            return OminalUserProfile.fromJson(new JSONObject(encoded));
        } catch (JSONException ignored) {
            return OminalUserProfile.empty();
        }
    }

    static void save(@NonNull SharedPreferences preferences,
                     @NonNull OminalUserProfile profile) throws JSONException {
        preferences.edit().putString(PREFERENCE_KEY, profile.toJson().toString()).apply();
    }

    static void export(@NonNull File homeDirectory, @NonNull OminalUserProfile profile)
        throws IOException, JSONException {
        File target = new File(homeDirectory, ".ominal/profile.json");
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Could not create profile directory");
        File temporary = new File(target.getAbsolutePath() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary, false)) {
            output.write(profile.toJson().toString(2).getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        temporary.setReadable(false, false);
        temporary.setWritable(false, false);
        temporary.setReadable(true, true);
        temporary.setWritable(true, true);
        if (!temporary.renameTo(target)) {
            if (target.exists() && !target.delete())
                throw new IOException("Could not replace profile projection");
            if (!temporary.renameTo(target))
                throw new IOException("Could not commit profile projection");
        }
        target.setReadable(false, false);
        target.setWritable(false, false);
        target.setReadable(true, true);
        target.setWritable(true, true);
    }
}
