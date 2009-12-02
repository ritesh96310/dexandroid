/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.globalsearch.benchmarks;

import android.content.ComponentName;

/**
 * Latency tests for suggestion sources.
 */

/*

To build and run:

mmm packages/apps/GlobalSearch/benchmarks \
&& adb -e install -r $OUT/system/app/GlobalSearchBenchmarks.apk \
&& sleep 10 \
&& adb -e shell am start -a android.intent.action.MAIN \
        -n com.android.globalsearch.benchmarks/.ContactsLatency \
&& adb -e logcat

 */

public class ContactsLatency extends SourceLatency {

    private static final String[] queries = { "", "a", "s", "e", "r", "pub", "sanxjkashasrxae" };
    
    private static ComponentName CONTACTS_COMPONENT =
            new ComponentName("com.android.contacts",
                "com.android.contacts.ContactsListActivity");

    @Override
    protected void onResume() {
        super.onResume();

        testContacts();
    }

    private void testContacts() {
        for (String query : queries) {
            checkSource("CONTACTS", CONTACTS_COMPONENT, query);
        }
    }

}
