ifeq ($(BOARD_USE_HTC_APPS),true)

LOCAL_PATH:= $(call my-dir)

###############################################################################
include $(CLEAR_VARS)

LOCAL_MODULE := FieldTest
LOCAL_SRC_FILES := proprietary/$(LOCAL_MODULE).apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)

###############################################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := proprietary/debug_tool
LOCAL_MODULE := debug_tool
LOCAL_MODULE_CLASS := EXECUTABLES

include $(BUILD_PREBUILT)

###############################################################################
include $(CLEAR_VARS)

LOCAL_SRC_FILES := proprietary/htclogkernel
LOCAL_MODULE := htclogkernel
LOCAL_MODULE_CLASS := EXECUTABLES

include $(BUILD_PREBUILT)

endif # ifeq ($(BOARD_USE_HTC_APPS),true)
