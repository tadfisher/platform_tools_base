/*
 * Copyright (C) 2013 The Android Open Source Project
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


package com.android.sdklib.build;

import static com.android.SdkConstants.EXT_BC;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.BuildToolInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Compiles Renderscript files.
 */
public class RenderScriptProcessor {

    // ABI list, as pairs of (android-ABI, toolchain-ABI)
    private static final String[] ABIS = {
            "armeabi-v7a", "armv7-none-linux-gnueabi",
            "mips", "mipsel-unknown-linux",
            "x86", "i686-unknown-linux" };

    public static final String RS_DEPS = "rsDeps";

    @NonNull
    private final List<File> mInputs;

    @NonNull
    private final List<File> mImportFolders;

    @NonNull
    private final File mBuildFolder;

    @NonNull
    private final File mSourceOutputDir;

    @NonNull
    private final File mResOutputDir;

    @NonNull
    private final File mObjOutputDir;

    @NonNull
    private final File mLibOutputDir;

    @NonNull
    private final File mBinFolder;

    @NonNull
    private final BuildToolInfo mBuildToolInfo;

    private final int mTargetApi;

    private final boolean mDebugBuild;

    private final int mOptimLevel;

    private final boolean mSupportMode;

    private final File mLibClCore;

    public interface CommandLineLauncher {
        void launch(
                @NonNull File executable,
                @NonNull List<String> arguments,
                @NonNull Map<String, String> envVariableMap)
                throws IOException, InterruptedException;
    }

    public RenderScriptProcessor(
            @NonNull List<File> inputs,
            @NonNull List<File> importFolders,
            @NonNull File buildFolder,
            @NonNull File sourceOutputDir,
            @NonNull File resOutputDir,
            @NonNull File objOutputDir,
            @NonNull File libOutputDir,
            @NonNull File binFolder,
            @NonNull BuildToolInfo buildToolInfo,
            int targetApi,
            boolean debugBuild,
            int optimLevel,
            boolean supportMode) {
        mInputs = inputs;
        mImportFolders = importFolders;
        mBuildFolder = buildFolder;
        mSourceOutputDir = sourceOutputDir;
        mResOutputDir = resOutputDir;
        mObjOutputDir = objOutputDir;
        mLibOutputDir = libOutputDir;
        mBinFolder = binFolder;
        mBuildToolInfo = buildToolInfo;
        mTargetApi = targetApi;
        mDebugBuild = debugBuild;
        mOptimLevel = optimLevel;
        mSupportMode = supportMode;

        if (supportMode) {
            File rs = new File(mBuildToolInfo.getLocation(), "renderscript");
            mLibClCore = new File(new File(new File(rs, "lib"), "arm"), "libclcore.bc");
        } else {
            mLibClCore = null;
        }
    }

    public void cleanOldOutput(@Nullable Collection<File> oldOutputs) {
        if (oldOutputs != null) {
            for (File file : oldOutputs) {
                file.delete();

                // if the file is a .bc and support mode is on we need to delete the
                // associated .o and .so
                if (mSupportMode && file.getName().endsWith(SdkConstants.DOT_BC)) {

                }
            }
        }
    }

    public void build(@NonNull CommandLineLauncher launcher)
            throws IOException, InterruptedException {

        // get the env var
        Map<String, String> env = Maps.newHashMap();
        if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN) {
            env.put("DYLD_LIBRARY_PATH", mBuildToolInfo.getLocation().getAbsolutePath());
        } else if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_LINUX) {
            env.put("LD_LIBRARY_PATH", mBuildToolInfo.getLocation().getAbsolutePath());
        }

        doMainCompilation(launcher, env);

        if (mSupportMode) {
            createSupportFiles(launcher, env);
        }
    }

    private void doMainCompilation(@NonNull CommandLineLauncher launcher,
            @NonNull Map<String, String> env)
            throws IOException, InterruptedException {
        if (mInputs.isEmpty()) {
            return;
        }

        String renderscript = mBuildToolInfo.getPath(BuildToolInfo.PathId.LLVM_RS_CC);
        if (renderscript == null || !new File(renderscript).isFile()) {
            throw new IllegalStateException(BuildToolInfo.PathId.LLVM_RS_CC + " is missing");
        }

        String rsPath = mBuildToolInfo.getPath(BuildToolInfo.PathId.ANDROID_RS);
        String rsClangPath = mBuildToolInfo.getPath(BuildToolInfo.PathId.ANDROID_RS_CLANG);

        // the renderscript compiler doesn't expect the top res folder,
        // but the raw folder directly.
        File rawFolder = new File(mResOutputDir, SdkConstants.FD_RES_RAW);

        // compile all the files in a single pass
        ArrayList<String> command = Lists.newArrayList();

        if (mDebugBuild) {
            command.add("-g");
        }

        command.add("-O");
        command.add(Integer.toString(mOptimLevel));

        // add all import paths
        command.add("-I");
        command.add(rsPath);
        command.add("-I");
        command.add(rsClangPath);

        for (File importPath : mImportFolders) {
            if (importPath.isDirectory()) {
                command.add("-I");
                command.add(importPath.getAbsolutePath());
            }
        }

        command.add("-d");
        command.add(new File(mBinFolder, RS_DEPS).getAbsolutePath());
        command.add("-MD");

        // source output
        command.add("-p");
        command.add(mSourceOutputDir.getAbsolutePath());

        // res output
        command.add("-o");
        command.add(rawFolder.getAbsolutePath());

        command.add("-target-api");
        command.add(Integer.toString(mTargetApi < 11 ? 11 : mTargetApi));

        // input files
        for (File sourceFile : mInputs) {
            command.add(sourceFile.getAbsolutePath());
        }


        launcher.launch(new File(renderscript), command, env);
    }

    private void createSupportFiles(@NonNull CommandLineLauncher launcher,
            @NonNull Map<String, String> env) throws IOException, InterruptedException {
        // get the generated BC files.
        File rawFolder = new File(mResOutputDir, SdkConstants.FD_RES_RAW);

        SourceSearcher searcher = new SourceSearcher(Collections.singletonList(rawFolder), EXT_BC);
        FileGatherer fileGatherer = new FileGatherer();
        searcher.search(fileGatherer);

        for (File bcFile : fileGatherer.getFiles()) {
            String name = bcFile.getName();
            String objName = name.replaceAll("\\.rs", ".o");
            String soName = name.replaceAll("\\.rs", ".so");

            for (int i = 1 , count = ABIS.length ; i < count ; i += 2) {
                String abi = ABIS[i];
                createSupportObjFile(bcFile, abi, objName, launcher, env);
            }


        }
    }

    private void createSupportObjFile(
            @NonNull File bcFile,
            @NonNull String abi,
            @NonNull String objName,
            @NonNull CommandLineLauncher launcher,
            @NonNull Map<String, String> env) throws IOException, InterruptedException {


        // make sure the dest folder exist
        mObjOutputDir.mkdirs();

        File exe = new File(mBuildToolInfo.getPath(BuildToolInfo.PathId.BCC_COMPAT));

        List<String> args = Lists.newArrayList();

        args.add("-O" + Integer.toString(mOptimLevel));

        args.add("-o");
        args.add(new File(mObjOutputDir, objName).getAbsolutePath());

        args.add("-fPIC");
        args.add("-shared");

        args.add("-rt-path");
        args.add(mLibClCore.getAbsolutePath());

        args.add("-mtriple");
        args.add(abi);

        args.add(bcFile.getAbsolutePath());

        launcher.launch(exe, args, env);
    }
}
