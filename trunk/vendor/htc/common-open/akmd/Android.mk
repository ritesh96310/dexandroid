ifeq ($(BOARD_VENDOR_USE_AKMD),true)

LOCAL_PATH:= $(call my-dir)

PRODUCT_COPY_FILES += \
        $(LOCAL_PATH)/proprietary/akmd:system/bin/akmd

endif # ifeq ($(BOARD_VENDOR_USE_AKMD),true)
