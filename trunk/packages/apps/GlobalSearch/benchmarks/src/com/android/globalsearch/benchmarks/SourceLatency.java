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

import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.server.search.SearchableInfo;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Latency tests for suggestion sources.
 */

/*

To build and run:

mmm vendor/google/apps/GlobalSearch/benchmarks \
&& adb -e install -r $OUT/system/app/GlobalSearchBenchmarks.apk \
&& sleep 10 \
&& adb -e shell am start -a android.intent.action.MAIN \
        -n com.android.globalsearch.benchmarks/.SourceLatency \
&& adb -e logcat

 */

public class SourceLatency extends Activity {

    private static final String TAG = "SourceLatency";

    private SearchManager mSearchManager;

    private ExecutorService mExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSearchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: call finish() when all tasks are done
    }

    private SearchableInfo getSearchable(ComponentName componentName) {
        SearchableInfo searchable = mSearchManager.getSearchableInfo(componentName, false);
        if (searchable == null || searchable.getSuggestAuthority() == null) {
            throw new RuntimeException("Component is not searchable: "
                    + componentName.flattenToShortString());
        }
        return searchable;
    }

    /**
     * Keeps track of timings in nanoseconds.
     */
    private static class ElapsedTime {
        private long mTotal = 0;
        private int mCount = 0;
        public synchronized void addTime(long time) {
            mTotal += time;
            mCount++;
        }
        public synchronized long getTotal() {
            return mTotal;
        }
        public synchronized long getAverage() {
            return mTotal / mCount;
        }
        public synchronized int getCount() {
            return mCount;
        }
    }

    public void checkSourceConcurrent(final String src, final ComponentName componentName,
            String query, long delay) {
        final ElapsedTime time = new ElapsedTime();
        final SearchableInfo searchable = getSearchable(componentName);
        int length = query.length();
        for (int end = 0; end <= length; end++) {
            final String prefix = query.substring(0, end);
            (new Thread() {
                @Override
                public void run() {
                    long t = checkSourceInternal(src, searchable, prefix);
                    time.addTime(t);
                }
            }).start();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Log.e(TAG, "sleep() in checkSourceConcurrent() interrupted.");
            }
        }
        int count = length + 1;
        // wait for all requests to finish
        while (time.getCount() < count) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Log.e(TAG, "sleep() in checkSourceConcurrent() interrupted.");
            }
        }
        Log.d(TAG, src + "[DONE]: " + length + " queries in " + formatTime(time.getAverage())
                + " (average), " + formatTime(time.getTotal()) + " (total)");
    }

    public void checkSource(String src, ComponentName componentName, String[] queries) {
        ElapsedTime time = new ElapsedTime();
        int count = queries.length;
        for (int i = 0; i < queries.length; i++) {
            long t = checkSource(src, componentName, queries[i]);
            time.addTime(t);
        }
        Log.d(TAG, src + "[DONE]: " + count + " queries in " + formatTime(time.getAverage())
                + " (average), " + formatTime(time.getTotal()) + " (total)");
    }

    public long checkSource(String src, ComponentName componentName, String query) {
        SearchableInfo searchable = getSearchable(componentName);
        return checkSourceInternal(src, searchable, query);
    }

    private long checkSourceInternal(String src, SearchableInfo searchable, String query) {
        Cursor cursor = null;
        try {
            final long start = System.nanoTime();
            cursor = mSearchManager.getSuggestions(searchable, query);
            long end = System.nanoTime();
            long elapsed = end - start;
            if (cursor == null) {
                Log.d(TAG, src + ": null cursor in " + formatTime(elapsed)
                        + " for '" + query + "'");
            } else {
                Log.d(TAG, src + ": " + cursor.getCount() + " rows in " + formatTime(elapsed)
                        + " for '" + query + "'");
            }
            return elapsed;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static String formatTime(long ns) {
        return (ns / 1000000.0d) + " ms";
    }

    public void checkLiveSource(String src, ComponentName componentName, String query) {
        mExecutorService.submit(new LiveSourceCheck(src, componentName, query));
    }

    private class LiveSourceCheck implements Runnable {

        private String mSrc;
        private SearchableInfo mSearchable;
        private String mQuery;
        private Handler mHandler = new Handler(Looper.getMainLooper());

        public LiveSourceCheck(String src, ComponentName componentName, String query) {
            mSrc = src;
            mSearchable = mSearchManager.getSearchableInfo(componentName, false);
            assert(mSearchable != null);
            assert(mSearchable.getSuggestAuthority() != null);
            mQuery = query;
        }

        public void run() {
            Cursor cursor = null;
            try {
                final long start = System.nanoTime();
                cursor = mSearchManager.getSuggestions(mSearchable, mQuery);
                long end = System.nanoTime();
                long elapsed = (end - start);
                if (cursor == null) {
                    Log.d(TAG, mSrc + ": null cursor in " + formatTime(elapsed)
                            + " for '" + mQuery + "'");
                } else {
                    Log.d(TAG, mSrc + ": " + cursor.getCount() + " rows in " + formatTime(elapsed)
                            + " for '" + mQuery + "'");
                    cursor.registerContentObserver(new ChangeObserver(cursor));
                    cursor.registerDataSetObserver(new MyDataSetObserver(mSrc, start, cursor));
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Log.d(TAG, mSrc + ": interrupted");
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private class ChangeObserver extends ContentObserver {
            private Cursor mCursor;

            public ChangeObserver(Cursor cursor) {
                super(mHandler);
                mCursor = cursor;
            }

            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                mCursor.requery();
            }
        }

        private class MyDataSetObserver extends DataSetObserver {
            private long mStart;
            private Cursor mCursor;
            private int mUpdateCount = 0;

            public MyDataSetObserver(String src, long start, Cursor cursor) {
                mSrc = src;
                mStart = start;
                mCursor = cursor;
            }

            @Override
            public void onChanged() {
                long end = System.nanoTime();
                long elapsed = end - mStart;
                mUpdateCount++;
                Log.d(TAG, mSrc + ", update " + mUpdateCount + ": " + mCursor.getCount()
                        + " rows in " + formatTime(elapsed));
            }

            @Override
            public void onInvalidated() {
                Log.d(TAG, mSrc + ": invalidated");
            }
        }
    }


}
