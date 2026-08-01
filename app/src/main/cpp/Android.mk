LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libominal-bootstrap
LOCAL_SRC_FILES := ominal-bootstrap-zip.S ominal-bootstrap.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libominal-display-env
LOCAL_SRC_FILES := ominal-display-env.c
LOCAL_LDLIBS := -ldl
include $(BUILD_SHARED_LIBRARY)
