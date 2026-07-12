LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libominal-bootstrap
LOCAL_SRC_FILES := ominal-bootstrap-zip.S ominal-bootstrap.c
include $(BUILD_SHARED_LIBRARY)
