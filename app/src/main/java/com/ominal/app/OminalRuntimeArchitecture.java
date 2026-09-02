package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Maps Android ABIs to the architecture expected inside the Linux runtime. */
final class OminalRuntimeArchitecture {

    enum Profile {
        ARM64("arm64-v8a", "arm64"),
        X86_64("x86_64", "amd64");

        @NonNull final String androidAbi;
        @NonNull final String linuxArchitecture;

        Profile(@NonNull String androidAbi, @NonNull String linuxArchitecture) {
            this.androidAbi = androidAbi;
            this.linuxArchitecture = linuxArchitecture;
        }
    }

    private OminalRuntimeArchitecture() {
    }

    @Nullable
    static Profile detect(@Nullable String[] supportedAbis) {
        if (supportedAbis == null) return null;
        for (String abi : supportedAbis) {
            for (Profile profile : Profile.values()) {
                if (profile.androidAbi.equals(abi)) return profile;
            }
        }
        return null;
    }

    static boolean hasPackagedRuntime(@Nullable Profile profile) {
        return profile == Profile.ARM64;
    }
}
