package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/** Provider-neutral user context whose canonical copy stays in app-private storage. */
public final class OminalUserProfile {
    public static final int SCHEMA_VERSION = 1;
    private static final int MAX_SHORT_LENGTH = 120;
    private static final int MAX_ABOUT_LENGTH = 2000;

    @NonNull public final String displayName;
    @NonNull public final String preferredName;
    @NonNull public final String language;
    @NonNull public final String locationOrTimeZone;
    @NonNull public final String about;

    public OminalUserProfile(@Nullable String displayName, @Nullable String preferredName,
                             @Nullable String language, @Nullable String locationOrTimeZone,
                             @Nullable String about) {
        this.displayName = normalize(displayName, MAX_SHORT_LENGTH);
        this.preferredName = normalize(preferredName, MAX_SHORT_LENGTH);
        this.language = normalize(language, MAX_SHORT_LENGTH);
        this.locationOrTimeZone = normalize(locationOrTimeZone, MAX_SHORT_LENGTH);
        this.about = normalize(about, MAX_ABOUT_LENGTH);
    }

    @NonNull
    public static OminalUserProfile empty() {
        return new OminalUserProfile("", "", "", "", "");
    }

    public boolean isEmpty() {
        return displayName.isEmpty() && preferredName.isEmpty() && language.isEmpty()
            && locationOrTimeZone.isEmpty() && about.isEmpty();
    }

    @NonNull
    public String label() {
        if (!preferredName.isEmpty()) return preferredName;
        if (!displayName.isEmpty()) return displayName;
        return isEmpty() ? "Not set" : "Configured";
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject fields = new JSONObject()
            .put("displayName", displayName)
            .put("preferredName", preferredName)
            .put("language", language)
            .put("locationOrTimeZone", locationOrTimeZone)
            .put("about", about);
        return new JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("canonicalStorage", "device")
            .put("scope", "shared-across-runtimes")
            .put("available", !isEmpty())
            .put("fields", fields);
    }

    @NonNull
    public static OminalUserProfile fromJson(@Nullable JSONObject object) {
        if (object == null || object.optInt("schemaVersion", -1) != SCHEMA_VERSION)
            return empty();
        JSONObject fields = object.optJSONObject("fields");
        if (fields == null) fields = object;
        return new OminalUserProfile(fields.optString("displayName", ""),
            fields.optString("preferredName", ""), fields.optString("language", ""),
            fields.optString("locationOrTimeZone", ""), fields.optString("about", ""));
    }

    @NonNull
    private static String normalize(@Nullable String value, int maximumLength) {
        if (value == null) return "";
        StringBuilder clean = new StringBuilder(Math.min(value.length(), maximumLength));
        boolean previousWhitespace = false;
        for (int index = 0; index < value.length() && clean.length() < maximumLength; index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character) && character != '\n' && character != '\t')
                continue;
            if (Character.isWhitespace(character)) {
                if (previousWhitespace) continue;
                clean.append(character == '\n' && maximumLength == MAX_ABOUT_LENGTH ? '\n' : ' ');
                previousWhitespace = true;
            } else {
                clean.append(character);
                previousWhitespace = false;
            }
        }
        return clean.toString().trim();
    }
}
