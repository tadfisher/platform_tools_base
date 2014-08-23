/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.ddmlib;

import com.android.annotations.NonNull;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches and caches 'getprop' values from device.
 */
class PropertyFetcher {
    /** the amount of time to wait between unsuccessful prop fetch attempts */
    private static final long FETCH_BACKOFF_MS = 3000; // 3 seconds
    private static final String GETPROP_COMMAND = "getprop"; //$NON-NLS-1$
    private static final Pattern GETPROP_PATTERN = Pattern.compile("^\\[([^]]+)\\]\\:\\s*\\[(.*)\\]$"); //$NON-NLS-1$
    private static final int GETPROP_TIMEOUT_SEC = 2;
    private static final int EXPECTED_PROP_COUNT = 150;

    private static enum CacheState {
        UNPOPULATED, FETCHING, POPULATED
    }

    /**
     * Represents a getProperty request that has not yet been fulfilled
     */
    private static class PropFuture {
        private final String mName;
        private final SettableFuture<String> mFuture;

        PropFuture(String name, SettableFuture<String> future) {
            mName = name;
            mFuture = future;
        }
    }

    /**
     * Shell output parser for a getprop command
     */
    @VisibleForTesting
    static class GetPropReceiver extends MultiLineReceiver {

        private Map<String, String> mCollectedProperties =
                Maps.newHashMapWithExpectedSize(EXPECTED_PROP_COUNT);

        @Override
        public void processNewLines(String[] lines) {
            // We receive an array of lines. We're expecting
            // to have the build info in the first line, and the build
            // date in the 2nd line. There seems to be an empty line
            // after all that.

            for (String line : lines) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                Matcher m = GETPROP_PATTERN.matcher(line);
                if (m.matches()) {
                    String label = m.group(1);
                    String value = m.group(2);

                    if (!label.isEmpty()) {
                        mCollectedProperties.put(label, value);
                    }
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        Map<String, String> getCollectedProperties() {
            return mCollectedProperties;
        }
    }

    private Map<String, String> mProperties = Maps.newHashMapWithExpectedSize(EXPECTED_PROP_COUNT);
    private final IDevice mDevice;
    private long mLastFetchAttemptTime = 0;
    private CacheState mCacheState = CacheState.UNPOPULATED;
    private List<PropFuture> mPendingRequests = new LinkedList<PropFuture>();
    private ExecutorService mThreadPool = Executors.newCachedThreadPool();

    public PropertyFetcher(IDevice device) {
        mDevice = device;
    }

    /**
     * Return the full list of cached properties.
     */
    public synchronized Map<String, String> getProperties() {
        return mProperties;
    }

    /**
     * Make a possibly asynchronous request for a system property value.
     *
     * @param name the property name to retrieve
     * @return a {@link Future} that can be used to retrieve the prop value
     */
    public synchronized Future<String> getProperty(@NonNull String name) {
        SettableFuture<String> result =  SettableFuture.create();
        switch (mCacheState) {
            case POPULATED:
                result.set(mProperties.get(name));
                break;
            case FETCHING:
                mPendingRequests.add(new PropFuture(name, result));
                break;
            case UNPOPULATED:
                mPendingRequests.add(new PropFuture(name, result));
                mCacheState = CacheState.FETCHING;
                fetchPropertiesAsync();
                break;
        }
        return result;
    }

    private void fetchPropertiesAsync() {
        Runnable fetchRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    waitBackOffTime();
                    GetPropReceiver propReceiver = new GetPropReceiver();
                    mDevice.executeShellCommand(GETPROP_COMMAND, propReceiver, GETPROP_TIMEOUT_SEC,
                            TimeUnit.SECONDS);
                    populateCache(propReceiver.getCollectedProperties());
                } catch (TimeoutException e) {
                    Log.w("PropertyFetcher", String.format(
                            "Connection timeout getting info for device %s",
                            mDevice.getSerialNumber()));

                } catch (AdbCommandRejectedException e) {
                    Log.w("PropertyFetcher", String.format(
                            "Adb rejected command to get  device %1$s info: %2$s",
                            mDevice.getSerialNumber(), e.getMessage()));

                } catch (ShellCommandUnresponsiveException e) {
                    Log.w("PropertyFetcher", String.format(
                            "Adb shell command took too long returning info for device %s",
                            mDevice.getSerialNumber()));

                } catch (IOException e) {
                    Log.w("PropertyFetcher",
                            String.format("IO Error getting info for device %s",
                                    mDevice.getSerialNumber()));
                }
            }
        };
        mThreadPool.submit(fetchRunnable);
    }

    /**
     * Waits for appropriate backoff time to prevent constant queries on an unresponsive device
     */
    private void waitBackOffTime() {
        long waitTime = calculateWaitBackoffTime();
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Log.d("PropFetcher", "interrupted");
            }
        }
    }

    private synchronized long calculateWaitBackoffTime() {
        long waitTime = 0;
        long currentTime = System.currentTimeMillis();
        long timeSinceLastAttempt = currentTime - mLastFetchAttemptTime;
        if (timeSinceLastAttempt < FETCH_BACKOFF_MS) {
            waitTime = FETCH_BACKOFF_MS - timeSinceLastAttempt;
        }
        mLastFetchAttemptTime = currentTime;
        return waitTime;
    }

    private synchronized void populateCache(@NonNull Map<String, String> props) {
        if (props.size() > 0) {
            mProperties.putAll(props);
            mCacheState = CacheState.POPULATED;
            for (PropFuture pendingRequest : mPendingRequests) {
                pendingRequest.mFuture.set(mProperties.get(pendingRequest.mName));
            }
        }
    }

    /**
     * Directly query the device for given property. This should be used for non ro properties whose
     * value can change.
     *
     * @return the property value or <code>null</code> if it could not be retrieved
     */
    public String getPropertySync(@NonNull String name) throws TimeoutException,
            AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        CountDownLatch latch = new CountDownLatch(1);
        CollectingOutputReceiver receiver = new CollectingOutputReceiver(latch);
        mDevice.executeShellCommand(String.format("getprop '%s'", name), receiver,
                GETPROP_TIMEOUT_SEC, TimeUnit.SECONDS);
        try {
            latch.await(GETPROP_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }

        String value = receiver.getOutput().trim();
        if (value.isEmpty()) {
            return null;
        }

        return value;
    }

    /**
     * Return true if cache is populated.
     *
     * @deprecated implementation detail
     */
    @Deprecated
    public synchronized boolean arePropertiesSet() {
        return CacheState.POPULATED.equals(mCacheState);
    }
}
