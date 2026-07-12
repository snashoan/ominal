LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= libominal-terminal
LOCAL_SRC_FILES:= ominal.c
include $(BUILD_SHARED_LIBRARY)
