The `ominal-shared` module is derived from the upstream `termux-shared`
library and remains released under the [MIT](https://opensource.org/licenses/MIT)
license, subject to the exceptions below.

### Exceptions

#### [GPLv3 only](https://www.gnu.org/licenses/gpl-3.0.html)

- [`src/main/java/com/ominal/shared/runtime/*`](src/main/java/com/ominal/shared/runtime).

The `GPLv3 only` license applies to those files unless specifically overridden,
including the MIT-licensed
[`OminalConstants.java`](src/main/java/com/ominal/shared/runtime/OminalConstants.java)
and
[`OminalPropertyConstants.java`](src/main/java/com/ominal/shared/runtime/settings/properties/OminalPropertyConstants.java).
##


#### [GPLv2 only with "Classpath" exception](https://openjdk.java.net/legal/gplv2+ce.html)

- [`src/main/java/com/ominal/shared/file/filesystem/*`](src/main/java/com/ominal/shared/file/filesystem) files that use code from [libcore/ojluni](https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:libcore/ojluni/).
##


#### [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)

- [`src/main/java/com/ominal/shared/shell/StreamGobbler.java`](src/main/java/com/ominal/shared/shell/StreamGobbler.java) uses code from [libsuperuser](https://github.com/Chainfire/libsuperuser).
##
