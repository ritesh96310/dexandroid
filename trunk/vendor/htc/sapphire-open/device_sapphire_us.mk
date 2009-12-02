#
# Copyright (C) 2009 The Android Open-Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# To be included directly by a product makefile; do not use inherit-product.

PRODUCT_COPY_FILES += \
        vendor/htc/sapphire-open/proprietary/AudioPara4.csv:system/etc/AudioPara4.csv \
	vendor/htc/sapphire-open/proprietary/gps.conf_US:system/etc/gps.conf

include vendor/htc/sapphire-open/device_sapphire.mk
