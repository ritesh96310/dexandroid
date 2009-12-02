ifeq ($(BUILD_WITH_FULL_STAGEFRIGHT),true)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:=       \
	stagefright.cpp

LOCAL_SHARED_LIBRARIES := \
	libstagefright

LOCAL_C_INCLUDES:= \
	$(JNI_H_INCLUDE) \
	frameworks/base/media/libstagefright \
	$(TOP)/external/opencore/extern_libs_v2/khronos/openmax/include

LOCAL_CFLAGS += -Wno-multichar

LOCAL_MODULE:= stagefright

include $(BUILD_EXECUTABLE)

################################################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES:=         \
        SineSource.cpp    \
        record.cpp

LOCAL_SHARED_LIBRARIES := \
	libstagefright

LOCAL_C_INCLUDES:= \
	$(JNI_H_INCLUDE) \
	frameworks/base/media/libstagefright \
	$(TOP)/external/opencore/extern_libs_v2/khronos/openmax/include

LOCAL_CFLAGS += -Wno-multichar

LOCAL_MODULE:= record

include $(BUILD_EXECUTABLE)

endif
