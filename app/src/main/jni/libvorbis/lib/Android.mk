LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libvorbis

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
$(LOCAL_PATH)/../include \
$(LOCAL_PATH)/../../libogg/include
#Подключаем headers libogg

LOCAL_SRC_FILES := analysis.c bitrate.c block.c codebook.c envelope.c floor0.c floor1.c info.c\
				lookup.c lpc.c lsp.c mapping0.c mdct.c psy.c registry.c res0.c sharedbook.c\
				smallft.c synthesis.c vorbisenc.c vorbisfile.c window.c\

LOCAL_SHARED_LIBRARIES := libogg	#Тут подключаем требуемые библиотеки


LOCAL_LDLIBS := -ldl -lGLESv1_CM -llog

include $(BUILD_SHARED_LIBRARY)