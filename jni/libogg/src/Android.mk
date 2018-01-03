LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libogg

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
$(LOCAL_PATH)/../include/

LOCAL_SRC_FILES := framing.c bitwise.c

include $(BUILD_SHARED_LIBRARY)