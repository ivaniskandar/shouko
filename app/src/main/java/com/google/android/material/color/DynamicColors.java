/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.material.color;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.ChecksSdkIntAtLeast;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for applying dynamic colors to application/activities.
 */
public class DynamicColors {
    private static final DeviceSupportCondition DEFAULT_DEVICE_SUPPORT_CONDITION = () -> true;

    @SuppressLint("PrivateApi")
    private static final DeviceSupportCondition SAMSUNG_DEVICE_SUPPORT_CONDITION =
            new DeviceSupportCondition() {
                private Long version;

                @Override
                public boolean isSupported() {
                    if (version == null) {
                        try {
                            Method method = Build.class.getDeclaredMethod("getLong", String.class);
                            method.setAccessible(true);
                            version = (long) method.invoke(null, "ro.build.version.oneui");
                        } catch (Exception e) {
                            version = -1L;
                        }
                    }
                    return version >= 40100L;
                }
            };

    private static final Map<String, DeviceSupportCondition> DYNAMIC_COLOR_SUPPORTED_MANUFACTURERS;

    static {
        Map<String, DeviceSupportCondition> deviceMap = new HashMap<>();
        deviceMap.put("oppo", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("realme", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("oneplus", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("vivo", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("xiaomi", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("motorola", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("itel", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("tecno mobile limited", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("infinix mobility limited", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("hmd global", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("sharp", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("sony", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("tcl", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("lenovo", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("lge", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("google", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("robolectric", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("samsung", SAMSUNG_DEVICE_SUPPORT_CONDITION);
        DYNAMIC_COLOR_SUPPORTED_MANUFACTURERS = Collections.unmodifiableMap(deviceMap);
    }

    private static final Map<String, DeviceSupportCondition> DYNAMIC_COLOR_SUPPORTED_BRANDS;

    static {
        Map<String, DeviceSupportCondition> deviceMap = new HashMap<>();
        deviceMap.put("asus", DEFAULT_DEVICE_SUPPORT_CONDITION);
        deviceMap.put("jio", DEFAULT_DEVICE_SUPPORT_CONDITION);
        DYNAMIC_COLOR_SUPPORTED_BRANDS = Collections.unmodifiableMap(deviceMap);
    }

    private DynamicColors() {}

    /**
     * Returns {@code true} if dynamic colors are available on the current SDK level.
     */
    @SuppressLint("DefaultLocale")
    @ChecksSdkIntAtLeast(api = VERSION_CODES.S)
    public static boolean isDynamicColorAvailable() {
        if (VERSION.SDK_INT < VERSION_CODES.S) {
            return false;
        }
        DeviceSupportCondition deviceSupportCondition =
                DYNAMIC_COLOR_SUPPORTED_MANUFACTURERS.get(Build.MANUFACTURER.toLowerCase());
        if (deviceSupportCondition == null) {
            deviceSupportCondition = DYNAMIC_COLOR_SUPPORTED_BRANDS.get(Build.BRAND.toLowerCase());
        }
        return deviceSupportCondition != null && deviceSupportCondition.isSupported();
    }

    private interface DeviceSupportCondition {
        boolean isSupported();
    }
}
