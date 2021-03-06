LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libmp3lame
LOCAL_CFLAGS := -DSTDC_HEADERS

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
$(LOCAL_PATH)/../include \

LOCAL_SRC_FILES := \
		bitstream.c encoder.c fft.c gain_analysis.c id3tag.c lame.c mpglib_interface.c \
		newmdct.c presets.c psymodel.c quantize.c quantize_pvt.c reservoir.c set_get.c \
		tables.c takehiro.c util.c vbrquantize.c VbrTag.c version.c \


LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)