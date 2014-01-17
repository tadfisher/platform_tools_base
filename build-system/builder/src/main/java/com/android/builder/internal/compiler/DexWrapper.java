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

package com.android.builder.internal.compiler;

import static com.google.common.base.Preconditions.checkNotNull;

import com.android.SdkConstants;
import com.android.annotations.NonNull;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

/**
 * Wrapper to access dx.jar through reflection.
 * <p/>Since there is no proper api to call the method in the dex library, this wrapper is going
 * to access it through reflection.
 */
public final class DexWrapper {

    private static final String DEX_MAIN = "com.android.dx.command.dexer.Main"; //$NON-NLS-1$
    private static final String DEX_CONSOLE = "com.android.dx.command.DxConsole"; //$NON-NLS-1$
    private static final String DEX_ARGS = "com.android.dx.command.dexer.Main$Arguments"; //$NON-NLS-1$

    private static final String MAIN_RUN = "run"; //$NON-NLS-1$

    private Method mRunMethod;

    private Constructor<?> mArgConstructor;
    private Field mArgOutName;
    private Field mArgVerbose;
    private Field mArgJarOutput;
    private Field mArgFileNames;
    private Field mArgForceJumbo;

    private Field mConsoleOut;
    private Field mConsoleErr;

    /**
     * Loads the dex library from a file path.
     *
     * The loaded library can be used via
     * {@link #run(java.io.File, java.util.Collection, boolean, boolean, java.io.PrintStream, java.io.PrintStream)}
     *
     * @param dxFile the location of the dx.jar file.
     */
    public void loadDex(@NonNull File dxFile) {
        try {
            if (!dxFile.isFile()) {
                throw new RuntimeException("Unable to load dx.jar at " + dxFile);
            }
            URL url = dxFile.toURI().toURL();

            URLClassLoader loader = new URLClassLoader(new URL[] { url },
                    DexWrapper.class.getClassLoader());

            // get the classes.
            Class<?> mainClass = loader.loadClass(DEX_MAIN);
            Class<?> consoleClass = loader.loadClass(DEX_CONSOLE);
            Class<?> argClass = loader.loadClass(DEX_ARGS);

            try {
                // now get the fields/methods we need
                mRunMethod = mainClass.getMethod(MAIN_RUN, argClass);

                mArgConstructor = argClass.getConstructor();
                mArgOutName = argClass.getField("outName"); //$NON-NLS-1$
                mArgJarOutput = argClass.getField("jarOutput"); //$NON-NLS-1$
                mArgFileNames = argClass.getField("fileNames"); //$NON-NLS-1$
                mArgVerbose = argClass.getField("verbose"); //$NON-NLS-1$
                mArgForceJumbo = argClass.getField("forceJumbo"); //$NON-NLS-1$

                mConsoleOut = consoleClass.getField("out"); //$NON-NLS-1$
                mConsoleErr = consoleClass.getField("err"); //$NON-NLS-1$

            } catch (SecurityException e) {
                throw new RuntimeException("SecurityException: Unable to find API for dex.jar", e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("SecurityException: Unable to find method for dex.jar", e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("SecurityException: Unable to find field for dex.jar", e);
            }

        } catch (Throwable e) {
            // really this should not happen.
            throw new RuntimeException(
                    String.format("Failed to load %1$s", dxFile), e);
        }
    }

    /**
     * Removes any reference to the dex library.
     * <p/>
     * {@link #loadDex(File)} must be called on the wrapper
     * before {@link #run(java.io.File, java.util.Collection, boolean, boolean, java.io.PrintStream, java.io.PrintStream)}
     * can be used again.
     */
    public void unload() {
        mRunMethod = null;
        mArgConstructor = null;
        mArgOutName = null;
        mArgJarOutput = null;
        mArgFileNames = null;
        mArgVerbose = null;
        mConsoleOut = null;
        mConsoleErr = null;
    }

    /**
     * Runs the dex command.
     * The wrapper must have been initialized via {@link #loadDex(File)} first.
     *
     * @param outputFile the output file
     * @param inputFiles list of input source files (.class and .jar files)
     * @param forceJumbo force jumbo mode.
     * @param verbose verbose mode.
     * @param outStream the stdout console
     * @param errStream the stderr console
     * @return the integer return code of com.android.dx.command.dexer.Main.run()
     */
    public synchronized int run(
            @NonNull File outputFile,
            @NonNull Collection<String> inputFiles,
            boolean forceJumbo,
            boolean verbose,
            @NonNull PrintStream outStream,
            @NonNull PrintStream errStream) {

        checkNotNull(mRunMethod, "dx wrapper was not properly loaded first");
        checkNotNull(mArgConstructor, "dx wrapper was not properly loaded first");
        checkNotNull(mArgOutName, "dx wrapper was not properly loaded first");
        checkNotNull(mArgJarOutput, "dx wrapper was not properly loaded first");
        checkNotNull(mArgFileNames, "dx wrapper was not properly loaded first");
        checkNotNull(mArgFileNames, "dx wrapper was not properly loaded first");
        checkNotNull(mArgForceJumbo, "dx wrapper was not properly loaded first");
        checkNotNull(mArgVerbose, "dx wrapper was not properly loaded first");
        checkNotNull(mConsoleOut, "dx wrapper was not properly loaded first");
        checkNotNull(mConsoleErr, "dx wrapper was not properly loaded first");

        try {
            // set the stream
            mConsoleErr.set(null /* obj: static field */, errStream);
            mConsoleOut.set(null /* obj: static field */, outStream);

            // create the Arguments object.
            Object args = mArgConstructor.newInstance();
            mArgOutName.set(args, outputFile.getAbsolutePath());
            mArgFileNames.set(args, inputFiles.toArray(new String[inputFiles.size()]));
            mArgJarOutput.set(args, outputFile.getPath().endsWith(SdkConstants.DOT_JAR));
            mArgForceJumbo.set(args, forceJumbo);
            mArgVerbose.set(args, verbose);

            // call the run method
            Object res = mRunMethod.invoke(null /* obj: static method */, args);

            if (res instanceof Integer) {
                return (Integer) res;
            }

            return -1;
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }

            String msg = t.getMessage();
            if (msg == null) {
                msg = String.format("%s. Check the Eclipse log for stack trace.",
                        t.getClass().getName());
            }

            throw new RuntimeException(
                    String.format("Unable to execute dex: %1$s", msg), t);
        }
    }
}
