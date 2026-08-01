# Ominal native display dependency

The development APK includes an ARM64 native X server derived from the
GPL-3.0-only [Termux:X11](https://github.com/termux/termux-x11) project.

- Upstream release: `nightly`, based on commit `222d68c`
- Upstream artifact: `app-arm64-v8a-debug.apk`
- Upstream artifact SHA-256: `6dd2563655c6f370ed1e61426a19d72381b5e1684bc43fbcb84a622c46fa0fe1`
- Included library SHA-256: `129049efb9b6fa3f18af826b9d2c6720289a51956bfce1bf59b2569bf96a7879`

The included library changes equal-length runtime class-path, label, build-path,
environment, and private-data strings to Ominal values. Exported command-entry
symbol names retain the upstream ABI because ELF hash tables cannot be renamed
in place; `ominal-display-env.c` contains the narrow Ominal JNI bridge. The
library is renamed to `libominal-display.so`; renderer and X server behavior are
otherwise unchanged.

The corresponding upstream source is available at:

`https://github.com/termux/termux-x11/tree/222d68c`

Ominal's Java integration and environment bridge are in this repository. The
complete application remains GPL-3.0-only under the top-level `LICENSE.md`.
This prebuilt dependency is for the ARM64 development build; the release path
must rebuild the same source transformation in CI and publish corresponding
source with each binary release.
