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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches and caches 'getprop' values from device.
 */
class PropertyFetcher {
    private static final long FETCH_FREQ_MS = 10000; // 10 seconds
    private static final String GETPROP_COMMAND = "getprop"; //$NON-NLS-1$
    private static final Pattern GETPROP_PATTERN = Pattern.compile("^\\[([^]]+)\\]\\:\\s*\\[(.*)\\]$"); //$NON-NLS-1$
    private static final int GETPROP_TIMEOUT_SEC = 2;

    private Map<String, String> mProperties = new HashMap<String, String>();
    private final Device mDevice;
    private long mLastFetchTime = 0;

    class GetPropReceiver extends MultiLineReceiver {

        private Map<String, String> mCollectedProperties = new HashMap<String, String>();

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

        @Override
        public void done() {
            populateCache(mCollectedProperties);
            mDevice.update(Device.CHANGE_BUILD_INFO);
        }
    }

    public PropertyFetcher(Device device) {
        mDevice = device;
    }

    /**
     * Retrieve the cached value of given property
     *
     * @return the property value or <code>null</code> if it does not exist in cache
     */
    public synchronized String getProperty(String name) {
        return mProperties.get(name);
    }

    /**
     * Attempt to retrieve the cached value of given property. IF it does not exist in cache,
     * attempt to synchronously populate the cache and try again.
     *
     * @return the property value or <code>null</code> if it could not be retrieved
     */
    public String getPropertyCacheOrSync(String name) throws TimeoutException,
            AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        String prop = getProperty(name);
        if (prop == null) {
            fetchProperties();
        }
        return getProperty(name);
    }

    /**
     * Attempt to populate the property cache
     */
    public void fetchProperties() throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {
        long currentTime = System.currentTimeMillis();
        synchronized (this) {
            mLastFetchTime = currentTime;
        }
        mDevice.executeShellCommand(GETPROP_COMMAND, new GetPropReceiver(), GETPROP_TIMEOUT_SEC,
                TimeUnit.SECONDS);
    }

    private synchronized void populateCache(Map<String, String> props) {
        mProperties.putAll(props);
    }

    /**
     * Attempt to retrieve the cached value of given property. If it does not exist in cache,
     * return, but in background make an attempt to populate the cache if no recent attempts have
     * been made.
     *
     * @return the property value or <code>null</code> if it could not be retrieved
     */
    public String getPropertyAsync(String name) {
        String prop = getProperty(name);
        if (prop == null && !wasRecentFetch()) {
            fetchPropertiesAsync();
        }
        return prop;
    }

    private void fetchPropertiesAsync() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    fetchProperties();
                } catch (TimeoutException e) {
                    Log.w("PropertyFetcher", String.format(
                            "Connection timeout getting info for device %s",
                            mDevice.getSerialNumber()));

                } catch (AdbCommandRejectedException e) {
                    // This should never happen as we only do this once the device is online.
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
        t.start();
    }

    synchronized boolean wasRecentFetch() {
        long currentTime = System.currentTimeMillis();
        boolean wasRecent = currentTime - mLastFetchTime > FETCH_FREQ_MS;
        if (!wasRecent) {
            mLastFetchTime = currentTime;
        }
        return wasRecent;
    }

    /**
     * Directly query the device for given property. This should be used for non ro properties whose
     * value can change.
     *
     * @return the property value or <code>null</code> if it could not be retrieved
     */
    public String getPropertySync(String name) throws TimeoutException,
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
     * Return the full list of cached properties.
     */
    public synchronized Map<String, String> getProperties() {
        return mProperties;
    }

    /**
     * Return true if properties are currently cached.
     */
    public synchronized boolean arePropertiesSet() {
        return mProperties.size() > 0;
    }
}
