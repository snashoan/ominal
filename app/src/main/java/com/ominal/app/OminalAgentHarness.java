package com.ominal.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Non-secret identity and capability metadata for an intelligence harness. */
public final class OminalAgentHarness {
    public enum Availability {
        AVAILABLE,
        PLANNED
    }

    public enum AuthMode {
        BROWSER,
        DEVICE_CODE,
        API_KEY
    }

    private final String mId;
    private final String mProviderId;
    private final String mPublisherName;
    private final String mDisplayName;
    private final String mTransport;
    private final Availability mAvailability;
    private final List<AuthMode> mAuthModes;

    public OminalAgentHarness(String id, String providerId, String publisherName,
                              String displayName,
                              String transport, Availability availability,
                              List<AuthMode> authModes) {
        mId = requireValue(id, "id");
        mProviderId = requireValue(providerId, "providerId");
        mPublisherName = requireValue(publisherName, "publisherName");
        mDisplayName = requireValue(displayName, "displayName");
        mTransport = requireValue(transport, "transport");
        if (availability == null) throw new IllegalArgumentException("availability is required");
        if (authModes == null || authModes.isEmpty()) {
            throw new IllegalArgumentException("at least one auth mode is required");
        }
        mAvailability = availability;
        mAuthModes = Collections.unmodifiableList(new ArrayList<>(authModes));
    }

    public String getId() {
        return mId;
    }

    public String getProviderId() {
        return mProviderId;
    }

    public String getPublisherName() {
        return mPublisherName;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getTransport() {
        return mTransport;
    }

    public Availability getAvailability() {
        return mAvailability;
    }

    public List<AuthMode> getAuthModes() {
        return mAuthModes;
    }

    public boolean isAvailable() {
        return mAvailability == Availability.AVAILABLE;
    }

    private static String requireValue(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
