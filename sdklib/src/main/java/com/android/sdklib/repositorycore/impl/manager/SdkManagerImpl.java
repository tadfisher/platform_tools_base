/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.sdklib.repositorycore.impl.manager;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.concurrency.GuardedBy;
import com.android.sdklib.repositorycore.api.Downloader;
import com.android.sdklib.repositorycore.api.LocalSdk;
import com.android.sdklib.repositorycore.api.ProgressIndicator;
import com.android.sdklib.repositorycore.api.ProgressRunner;
import com.android.sdklib.repositorycore.api.SdkLoadedCallback;
import com.android.sdklib.repositorycore.api.SdkSourceCategory;
import com.android.sdklib.repositorycore.api.SettingsController;
import com.android.sdklib.repositorycore.impl.local.LocalSdkImpl;
import com.android.sdklib.repositorycore.impl.remote.RemotePackage;
import com.android.sdklib.repositorycore.impl.remote.RemoteSdk;
import com.android.utils.ILogger;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class SdkManagerImpl {

    // TODO: seems lame that this is a separate wrapper around localsdk that has the same
    // caching/instance creation functionality, and separately a different kind of wrapper around
    // remotesdk.

    public static final long DEFAULT_EXPIRATION_PERIOD_MS = TimeUnit.DAYS.toMillis(1);

    private static final Logger LOG = Logger
            .getLogger(SdkManagerImpl.class.getName());

    @GuardedBy(value = "sSdkStates")
    private static final Set<SoftReference<SdkManagerImpl>> sSdkStates
            = new HashSet<SoftReference<SdkManagerImpl>>();

    @Nullable
    private final LocalSdk myLocalSdk;

    private final RemoteSdk myRemoteSdk;

    private SdkPackages myPackages = new SdkPackages();

    private long myLastRefreshMs;

    private LoadTask myTask;

    private final Object myTaskLock = new Object();

    private SdkManagerImpl(@Nullable File localPath, SettingsController settingsController, Downloader downloader) {
        if (localPath != null) {
            myLocalSdk = new LocalSdkImpl(localPath);
        }
        else {
            myPackages = new SdkPackages();
            myLocalSdk = null;
        }
        myRemoteSdk = new RemoteSdk(settingsController, downloader);
    }

    /**
     * This shouldn't be needed unless interacting with the internals of the remote sdk.
     */
    @NonNull
    public RemoteSdk getRemoteSdk() {
        return myRemoteSdk;
    }

    // TODO: figure out how to handle settings and downloader better
    @NonNull
    public static SdkManagerImpl getInstance(@Nullable File localPath, @Nullable SettingsController settings, @Nullable Downloader downloader) {
        synchronized (sSdkStates) {
            for (Iterator<SoftReference<SdkManagerImpl>> it = sSdkStates.iterator();
                    it.hasNext(); ) {
                SoftReference<SdkManagerImpl> ref = it.next();
                SdkManagerImpl s = ref.get();
                if (s == null) {
                    it.remove();
                    continue;
                }
                if (s.myLocalSdk == null) {
                    if (localPath == null) {
                        return s;
                    }
                }
                else if (Objects.equal(s.myLocalSdk.getLocation(), localPath)) {
                    return s;
                }
            }

            SdkManagerImpl s = new SdkManagerImpl(localPath, settings, downloader);
            sSdkStates.add(new SoftReference<SdkManagerImpl>(s));
            return s;
        }
    }

    @NonNull
    public SdkPackages getPackages() {
        return myPackages;
    }

    // return value: whether a reload was actually done
    public boolean load(long timeoutMs,
            boolean canBeCancelled,
            @NonNull List<SdkLoadedCallback> onLocalComplete,
            @NonNull List<SdkLoadedCallback> onSuccess,
            @NonNull List<Runnable> onError,
            boolean forceRefresh,
            ProgressRunner runner,
            boolean sync) {
        if (!forceRefresh && System.currentTimeMillis() - myLastRefreshMs < timeoutMs) {
            for (SdkLoadedCallback localComplete : onLocalComplete) {
                runner.runSyncWithoutProgress(new CallbackRunnable(localComplete, myPackages));
            }
            for (SdkLoadedCallback success : onSuccess) {
                runner.runSyncWithoutProgress(new CallbackRunnable(success, myPackages));
            }
            return false;
        }

        // TODO: previously thought this should be in the runner. Why?
        final Semaphore completed = new Semaphore(1);
        try {
            completed.acquire();
        } catch (InterruptedException e) {
            // TODO
        }
        if (sync) {
            onSuccess.add(new SdkLoadedCallback() {
                @Override
                public void doRun(@NonNull SdkPackages packages) {
                    completed.release();
                }
            });
            onError.add(new Runnable() {
                @Override
                public void run() {
                    completed.release();
                }
            });
        }

        boolean createdTask = false;

        try {
            synchronized (myTaskLock) {
                if (myTask != null) {
                    myTask.addCallbacks(onLocalComplete, onSuccess, onError);
                } else {
                    myTask = new LoadTask(canBeCancelled, onLocalComplete, onSuccess, onError);
                    createdTask = true;
                }
            }

            if (createdTask) {
                if (sync) {
                    runner.runSyncWithProgress(myTask);
                } else {
                    runner.runAsyncWithProgress(myTask);
                }
            } else if (sync) {
                try {
                    completed.acquire();
                } catch (InterruptedException e) {
                    // TODO
                }
            }
        }
        finally {
            if (createdTask) {
                myTask = null;
            }
        }

        myLastRefreshMs = System.currentTimeMillis();
        return true;
    }



/*
        public boolean loadAsync(long timeoutMs,
            boolean canBeCancelled,
            @Nullable SdkLoadedCallback onLocalComplete,
            @Nullable SdkLoadedCallback onSuccess,
            @Nullable Runnable onError,
            boolean forceRefresh,
            ProgressRunner runner) {
        return load(timeoutMs, canBeCancelled, createList(onLocalComplete), createList(onSuccess),
                createList(onError), forceRefresh, false, runner);
    }

    private boolean load(long timeoutMs,
            boolean canBeCancelled,
            @NonNull List<SdkLoadedCallback> onLocalComplete,
            @NonNull List<SdkLoadedCallback> onSuccess,
            @NonNull List<Runnable> onError,
            boolean forceRefresh,
            boolean sync,
            ProgressRunner runner) {
        if (!forceRefresh && System.currentTimeMillis() - myLastRefreshMs < timeoutMs) {
            for (SdkLoadedCallback localComplete : onLocalComplete) {
                runner.runSyncWithoutProgress(new CallbackRunnable(localComplete, myPackages));
            }
            for (SdkLoadedCallback success : onSuccess) {
                runner.runSyncWithoutProgress(new CallbackRunnable(success, myPackages));
            }
            return false;
        }
        synchronized (myTaskLock) {
            if (myTask != null) {
                myTask.addCallbacks(onLocalComplete, onSuccess, onError);
                return false;
            }

            myTask = new LoadTask(canBeCancelled, onLocalComplete, onSuccess, onError, forceRefresh);
        }
        runner.runAsyncWithProgress(myTask);

        return true;
    }

    public boolean loadSynchronously(long timeoutMs,
            boolean canBeCancelled,
            @Nullable SdkLoadedCallback onLocalComplete,
            @Nullable SdkLoadedCallback onSuccess,
            @Nullable final Runnable onError,
            boolean forceRefresh,
            @NonNull ProgressRunner runner) {
        final Semaphore completed = new Semaphore(1);
        try {
            completed.acquire();
        } catch (InterruptedException e) {
            // TODO
        }

        List<SdkLoadedCallback> onLocalCompletes = createList(onLocalComplete);
        List<SdkLoadedCallback> onSuccesses = createList(onSuccess);
        List<Runnable> onErrors = createList(onError);
        onSuccesses.add(new SdkLoadedCallback(false) {
            @Override
            public void doRun(@NonNull SdkPackages packages) {
                completed.release();
            }
        });
        onErrors.add(new Runnable() {
            @Override
            public void run() {
                completed.release();
            }
        });
        boolean result = load(timeoutMs, canBeCancelled, onLocalCompletes, onSuccesses, onErrors,
                forceRefresh, true, runner);



        if (!ApplicationManager.getApplication().isDispatchThread()) {
            // Not dispatch thread, assume progress is being handled elsewhere.
            if (result) {
                // We don't have to wait since load() ran in-thread.
                return true;
            }
            try {
                completed.waitForUnsafe();
            } catch (InterruptedException e) {
                if (onError != null) {
                    onError.run();
                }
                return false;
            }
            return true;
        }

        // If we are on the dispatch thread, show progress while waiting.
        ProgressManager pm = ProgressManager.getInstance();
        ProgressIndicator indicator = pm.getProgressIndicator();
        boolean startedProgress;
        indicator = (startedProgress = indicator == null) ? new ProgressWindow(false, false, null)
                : indicator;
        if (startedProgress) {
            indicator.start();
        }
        pm.executeProcessUnderProgress(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                try {
                    completed.waitForUnsafe();
                    success = true;
                } catch (InterruptedException e) {
                    LOG.warn(e);
                }

                if (!success) {
                    if (onError != null) {
                        onError.run();
                    }
                }
            }
        }, indicator);
        if (startedProgress) {
            indicator.stop();
        }
        return result;
    }
*/

    @NonNull
    private static <T> List<T> createList(@Nullable T r) {
        if (r == null) {
            return Lists.newArrayList();
        }
        return Lists.newArrayList(r);
    }

    // -----
// TODO: remove/merge with indicator? Need better logging strategy for new stuff.
    private static class IndicatorLogger implements ILogger {

        @NonNull
        private final ProgressIndicator myIndicator;

        public IndicatorLogger(@NonNull ProgressIndicator indicator) {
            myIndicator = indicator;
        }

        @Override
        public void error(@Nullable Throwable t, @Nullable String msgFormat, Object... args) {
            if (msgFormat == null && t != null) {
                myIndicator.setText2(t.toString());
            } else if (msgFormat != null) {
                myIndicator.setText2(String.format(msgFormat, args));
            }
        }

        @Override
        public void warning(@NonNull String msgFormat, Object... args) {
            myIndicator.setText2(String.format(msgFormat, args));
        }

        @Override
        public void info(@NonNull String msgFormat, Object... args) {
            myIndicator.setText2(String.format(msgFormat, args));
        }

        @Override
        public void verbose(@NonNull String msgFormat, Object... args) {
            // skip here, don't log verbose strings
        }
    }


    private class LoadTask implements ProgressRunner.ProgressRunnable {

        private final List<SdkLoadedCallback> myOnSuccesses = Lists.newArrayList();
        private final List<Runnable> myOnErrors = Lists.newArrayList();
        private final List<SdkLoadedCallback> myOnLocalCompletes = Lists.newArrayList();

        private final boolean myCanBeCancelled;

        public LoadTask(boolean canBeCancelled,
                @NonNull List<SdkLoadedCallback> onLocalComplete,
                @NonNull List<SdkLoadedCallback> onSuccess,
                @NonNull List<Runnable> onError) {
            addCallbacks(onLocalComplete, onSuccess, onError);
            myCanBeCancelled = canBeCancelled;
        }

        public void addCallbacks(@NonNull List<SdkLoadedCallback> onLocalComplete,
                @NonNull List<SdkLoadedCallback> onSuccess,
                @NonNull List<Runnable> onError) {
            myOnLocalCompletes.addAll(onLocalComplete);
            myOnSuccesses.addAll(onSuccess);
            myOnErrors.addAll(onError);
        }

        @Override
        public void run(@NonNull ProgressIndicator indicator, ProgressRunner runner) {
            boolean success = false;
            try {
                IndicatorLogger logger = new IndicatorLogger(indicator);
                SdkPackages packages = new SdkPackages();
                if (myLocalSdk != null) {
                    // fetch local sdk
                    indicator.setText("Loading local SDK...");
                    myLocalSdk.invalidate();
                    packages.setLocalPkgInfos(
                            myLocalSdk.getPackages());
                    indicator.setFraction(0.25);
                }
                if (indicator.isCanceled()) {
                    return;
                }
                synchronized (myTaskLock) {
                    for (SdkLoadedCallback onLocalComplete : myOnLocalCompletes) {
                        runner.runSyncWithoutProgress(new CallbackRunnable(onLocalComplete, packages));
                    }
                    myOnLocalCompletes.clear();
                }
                // fetch sdk repository sources.
                indicator.setText("Fetch remote SDK...");
                indicator.setText2("");

                Multimap<String, ? extends RemotePackage> remotes =
                        myRemoteSdk.fetchPackages(indicator);

                // compute updates
                indicator.setText("Compute SDK updates...");
                indicator.setFraction(0.75);
                packages.setRemotePkgInfos(remotes);
                myPackages = packages;
                if (indicator.isCanceled()) {
                    return;
                }
                indicator.setText2("");
                indicator.setFraction(1.0);

                if (indicator.isCanceled()) {
                    return;
                }
                success = true;
                myLastRefreshMs = System.currentTimeMillis();
            } finally {
                myLastRefreshMs = System.currentTimeMillis();
                synchronized (myTaskLock) {
                    // The processing of the task is now complete. To ensure that no more callbacks are added, and to allow another task to be
                    // kicked off when needed, set myTask to null.
                    myTask = null;
                    if (success) {
                        for (final SdkLoadedCallback onLocalComplete : myOnLocalCompletes) {  // in case some were added by another call in the interim.
                            runner.runSyncWithoutProgress(new CallbackRunnable(onLocalComplete, myPackages));
                        }
                        for (final SdkLoadedCallback onSuccess : myOnSuccesses) {
                            runner.runSyncWithoutProgress(new CallbackRunnable(onSuccess, myPackages));
                        }
                    } else {
                        for (final Runnable onError : myOnErrors) {
                            onError.run();
                        }
                    }
                }
            }
        }
    }

    private static class CallbackRunnable implements Runnable {
        SdkLoadedCallback myCallback;
        SdkPackages myPackages;

        public CallbackRunnable(SdkLoadedCallback callback, SdkPackages packages) {
            myCallback = callback;
            myPackages = packages;
        }

        @Override
        public void run() {
            myCallback.doRun(myPackages);
        }
    }
}
