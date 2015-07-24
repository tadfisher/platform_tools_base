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
import com.android.sdklib.internal.repository.sources.SdkSources;
import com.android.sdklib.repository.descriptors.PkgType;
import com.android.sdklib.repositorycore.api.LocalSdk;
import com.android.sdklib.repositorycore.api.ProgressIndicator;
import com.android.sdklib.repositorycore.api.ProgressRunner;
import com.android.sdklib.repositorycore.api.SdkLoadedCallback;
import com.android.sdklib.repositorycore.api.SdkSourceCategory;
import com.android.sdklib.repositorycore.impl.local.LocalSdkImpl;
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
import java.util.logging.Logger;

public class SdkManagerImpl {

    // TODO: seems lame that this is a separate wrapper around localsdk that has the same
    // caching/instance creation functionality, and separately a different kind of wrapper around
    // remotesdk.

    public final static long DEFAULT_EXPIRATION_PERIOD_MS = TimeUnit.DAYS.toMillis(1);

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

    private SdkManagerImpl(@Nullable File localPath) {
        myLocalSdk = LocalSdkImpl.getInstance(localPath);
        myLocalSdkPath = localPath;
        if (myLocalSdkPath == null) {
            myPackages = new SdkPackages();
        }
        myRemoteSdk = new RemoteSdk();
    }

    /**
     * This shouldn't be needed unless interacting with the internals of the remote sdk.
     */
    @NonNull
    public RemoteSdk getRemoteSdk() {
        return myRemoteSdk;
    }

    @NonNull
    public static SdkManagerImpl getInstance(@Nullable File localPath) {
        synchronized (sSdkStates) {
            for (Iterator<SoftReference<SdkManagerImpl>> it = sSdkStates.iterator();
                    it.hasNext(); ) {
                SoftReference<SdkManagerImpl> ref = it.next();
                SdkManagerImpl s = ref.get();
                if (s == null) {
                    it.remove();
                    continue;
                }
                if (Objects.equal(s.myLocalSdkPath, localPath)) {
                    return s;
                }
            }

            SdkManagerImpl s = new SdkManagerImpl(localPath);
            sSdkStates.add(new SoftReference<SdkManagerImpl>(s));
            return s;
        }
    }

    @NonNull
    public SdkPackages getPackages() {
        return myPackages;
    }

    // TODO: move to remotesdk?
    public void registerSourceCategory(SdkSourceCategory category) {

    }

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
                runner.run(localComplete, myPackages);
            }
            for (SdkLoadedCallback success : onSuccess) {
                runner.run(success, myPackages);
            }
            return false;
        }
        synchronized (myTaskLock) {
            if (myTask != null) {
                myTask.addCallbacks(onLocalComplete, onSuccess, onError);
                return false;
            }

            myTask = new LoadTask(canBeCancelled, onLocalComplete, onSuccess, onError, forceRefresh,
                    sync);
        }
        runner.runWithProgress(myTask);

        return true;
    }

    public boolean loadSynchronously(long timeoutMs,
            boolean canBeCancelled,
            @Nullable SdkLoadedCallback onLocalComplete,
            @Nullable SdkLoadedCallback onSuccess,
            @Nullable final Runnable onError,
            boolean forceRefresh) {
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
                completed.up();
            }
        });
        onErrors.add(new Runnable() {
            @Override
            public void run() {
                completed.release();
            }
        });
        boolean result = load(timeoutMs, canBeCancelled, onLocalCompletes, onSuccesses, onErrors,
                forceRefresh, true);

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

    @NonNull
    private static <T> List<T> createList(@Nullable T r) {
        if (r == null) {
            return Lists.newArrayList();
        }
        return Lists.newArrayList(r);
    }

    // -----

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


    private class LoadTask {

        private final List<SdkLoadedCallback> myOnSuccesses = Lists.newArrayList();

        private final List<Runnable> myOnErrors = Lists.newArrayList();

        private final List<SdkLoadedCallback> myOnLocalCompletes = Lists.newArrayList();

        private final boolean myForceRefresh;

        public LoadTask(boolean canBeCancelled,
                @NonNull List<SdkLoadedCallback> onLocalComplete,
                @NonNull List<SdkLoadedCallback> onSuccess,
                @NonNull List<Runnable> onError,
                boolean forceRefresh,
                boolean modal) {
            addCallbacks(onLocalComplete, onSuccess, onError);
            myForceRefresh = forceRefresh;
        }

        public void addCallbacks(@NonNull List<SdkLoadedCallback> onLocalComplete,
                @NonNull List<SdkLoadedCallback> onSuccess,
                @NonNull List<Runnable> onError) {
            myOnLocalCompletes.addAll(onLocalComplete);
            myOnSuccesses.addAll(onSuccess);
            myOnErrors.addAll(onError);
        }

        public void run(@NonNull ProgressIndicator indicator) {
            boolean success = false;
            try {
                IndicatorLogger logger = new IndicatorLogger(indicator);
                SdkPackages packages = new SdkPackages();
                if (mySdkData != null) {
                    // fetch local sdk
                    indicator.setText("Loading local SDK...");
                    if (myForceRefresh) {
                        mySdkData.getLocalSdk().clearLocalPkg(PkgType.PKG_ALL);
                    }
                    packages.setLocalPkgInfos(
                            mySdkData.getLocalSdk().getPkgsInfos(PkgType.PKG_ALL));
                    indicator.setFraction(0.25);
                }
                if (indicator.isCanceled()) {
                    return;
                }
                synchronized (myTaskLock) {
                    for (SdkLoadedCallback onLocalComplete : myOnLocalCompletes) {
                        onLocalComplete.run(packages);
                    }
                    myOnLocalCompletes.clear();
                }
                // fetch sdk repository sources.
                indicator.setText("Find SDK Repository...");
                indicator.setText2("");
                SdkSources sources = myRemoteSdk
                        .fetchSources(myForceRefresh ? 0 : RemoteSdk.DEFAULT_EXPIRATION_PERIOD_MS,
                                logger);
                indicator.setFraction(0.50);

                if (indicator.isCanceled()) {
                    return;
                }
                // fetch remote sdk
                indicator.setText("Check SDK Repository...");
                indicator.setText2("");
                Multimap<PkgType, RemotePkgInfo> remotes = myRemoteSdk.fetch(sources, logger);
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
                        for (SdkLoadedCallback onLocalComplete : myOnLocalCompletes) {  // in case some were added by another call in the interim.
                            onLocalComplete.run(myPackages);
                        }
                        for (SdkLoadedCallback onSuccess : myOnSuccesses) {
                            onSuccess.run(myPackages);
                        }
                    } else {
                        for (Runnable onError : myOnErrors) {
                            onError.run();
                        }
                    }
                }
            }
        }
    }
}
