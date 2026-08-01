#include <jni.h>
#include <dlfcn.h>
#include <stdlib.h>

static void *display_library;

static void *resolve_display_symbol(JNIEnv *env, const char *name) {
    if (!display_library)
        display_library = dlopen("libominal-display.so", RTLD_NOW | RTLD_LOCAL);
    void *symbol = display_library ? dlsym(display_library, name) : NULL;
    if (!symbol) {
        jclass error = (*env)->FindClass(env, "java/lang/UnsatisfiedLinkError");
        (*env)->ThrowNew(env, error, dlerror() ?: "Native display symbol is unavailable");
    }
    return symbol;
}

JNIEXPORT void JNICALL
Java_com_ominal_x11_CmdEntryPoint_prepareEnvironment(JNIEnv *env, jclass clazz,
                                                      jstring temporary_directory,
                                                      jstring xkb_config_root) {
    (void) clazz;
    const char *temporary = (*env)->GetStringUTFChars(env, temporary_directory, NULL);
    const char *xkb = (*env)->GetStringUTFChars(env, xkb_config_root, NULL);

    setenv("TMPDIR", temporary, 1);
    setenv("XDG_RUNTIME_DIR", temporary, 1);
    setenv("XKB_CONFIG_ROOT", xkb, 1);
    unsetenv("LD_PRELOAD");

    (*env)->ReleaseStringUTFChars(env, temporary_directory, temporary);
    (*env)->ReleaseStringUTFChars(env, xkb_config_root, xkb);
}

JNIEXPORT jboolean JNICALL
Java_com_ominal_x11_CmdEntryPoint_start(JNIEnv *env, jclass clazz, jobjectArray args) {
    typedef jboolean (*start_function)(JNIEnv *, jclass, jobjectArray);
    start_function function = (start_function) resolve_display_symbol(
        env, "Java_com_termux_x11_CmdEntryPoint_start");
    return function ? function(env, clazz, args) : JNI_FALSE;
}

JNIEXPORT jobject JNICALL
Java_com_ominal_x11_CmdEntryPoint_getXConnection(JNIEnv *env, jobject instance) {
    typedef jobject (*connection_function)(JNIEnv *, jobject);
    connection_function function = (connection_function) resolve_display_symbol(
        env, "Java_com_termux_x11_CmdEntryPoint_getXConnection");
    return function ? function(env, instance) : NULL;
}

JNIEXPORT jboolean JNICALL
Java_com_ominal_x11_CmdEntryPoint_connected(JNIEnv *env, jclass clazz) {
    typedef jboolean (*connected_function)(JNIEnv *, jclass);
    connected_function function = (connected_function) resolve_display_symbol(
        env, "Java_com_termux_x11_CmdEntryPoint_connected");
    return function ? function(env, clazz) : JNI_FALSE;
}
