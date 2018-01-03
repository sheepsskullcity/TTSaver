LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libencode

LOCAL_C_INCLUDES := \
$(LOCAL_PATH)/ \
$(LOCAL_PATH)/../libogg/include/ \
$(LOCAL_PATH)/../libvorbis/include/ \
$(LOCAL_PATH)/../libmp3lame/include/ \

LOCAL_SRC_FILES := encoder.c\

LOCAL_SHARED_LIBRARIES := libogg libvorbis libmp3lame

LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)