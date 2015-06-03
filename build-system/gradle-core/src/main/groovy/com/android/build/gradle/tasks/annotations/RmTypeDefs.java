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

package com.android.build.gradle.tasks.annotations;

import static org.objectweb.asm.Opcodes.ASM5;

import com.android.annotations.NonNull;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Finds and deletes typedef annotation classes (and also warns if their
 * retention was wrong, such that uses embeds
 */
public class RmTypeDefs {
    private static final String ANNOTATION = "java/lang/annotation/Annotation";
    private static final String STRING_DEF = "android/annotation/StringDef";
    private static final String INT_DEF = "android/annotation/IntDef";
    private static final String INT_DEF_DESC = "L" + INT_DEF + ";";
    private static final String STRING_DEF_DESC = "L" + STRING_DEF + ";";
    private static final String RETENTION_DESC = "Ljava/lang/annotation/Retention;";
    private static final String RETENTION_POLICY_DESC = "Ljava/lang/annotation/RetentionPolicy;";
    private static final String SOURCE_RETENTION_VALUE = "SOURCE";

    private final Extractor mExtractor;

    private final boolean mQuiet;
    private final boolean mVerbose;
    private final boolean mDryRun;
    private boolean mHaveError;

    public RmTypeDefs(
            @NonNull Extractor extractor,
            boolean quiet,
            boolean verbose,
            boolean dryRun) {
        mExtractor = extractor;
        mQuiet = quiet;
        mVerbose = verbose;
        mDryRun = dryRun;
    }

    private Set<String> mAnnotationNames = Sets.newHashSet();
    private List<File> mAnnotationClassFiles = Lists.newArrayList();
    private Set<File> mAnnotationOuterClassFiles = Sets.newHashSet();

    public void remove(@NonNull List<File> classes) {
        if (!mQuiet) {
            mExtractor.info("Deleting @IntDef and @StringDef annotation class files");
        }

        // Record typedef annotation names and files
        for (File file : classes) {
            checkClass(file);
        }

        // Rewrite the .class files for any classes that *contain* typedefs as innerclasses
        rewriteOuterClasses();

        // Removes the actual .class files for the typedef annotations
        deleteAnnotationClasses();

        System.exit(mHaveError ? -1 : 0);
    }

    /**
     * Checks the given .class file to see if it's a typedef annotation, and if so
     * records that fact by calling {@link #addTypeDef(String, java.io.File)}
     */
    private void checkClass(File file) {
        String path = file.getPath();
        assert path.endsWith(".class") : path;

        try {
            byte[] bytes = Files.toByteArray(file);
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(new TypeDefVisitor(file), 0);
        } catch (IOException e) {
            Extractor.error("Could not read " + file + ": " + e.getLocalizedMessage());
            System.exit(1);
        }
    }

    /**
     * Records the given class name (internal name) and class file path as corresponding to a
     * typedef annotation
     * */
    private void addTypeDef(String name, File file) {
        mAnnotationClassFiles.add(file);
        mAnnotationNames.add(name);

        String fileName = file.getName();
        int index = fileName.lastIndexOf('$');
        if (index != -1) {
            File parentFile = file.getParentFile();
            assert parentFile != null : file;
            File container = new File(parentFile, fileName.substring(0, index) + ".class");
            if (container.exists()) {
                mAnnotationOuterClassFiles.add(container);
            } else {
                Extractor.error("Warning: Could not find outer class " + container
                        + " for typedef " + file);
                mHaveError = true;
            }
        }
    }

    /**
     * Rewrites the outer classes containing the typedefs such that they no longer refer to
     * the (now removed) typedef annotation inner classes
     */
    private void rewriteOuterClasses() {
        for (File file : mAnnotationOuterClassFiles) {
            byte[] bytes;
            try {
                bytes = Files.toByteArray(file);
            } catch (IOException e) {
                Extractor.error("Could not read " + file + ": " + e.getLocalizedMessage());
                mHaveError = true;
                continue;
            }

            ClassWriter classWriter = new ClassWriter(ASM5);
            ClassVisitor classVisitor = new ClassVisitor(ASM5, classWriter) {
                @Override
                public void visitInnerClass(String name, String outerName, String innerName,
                        int access) {
                    if (!mAnnotationNames.contains(name)) {
                        super.visitInnerClass(name, outerName, innerName, access);
                    }
                }
            };
            ClassReader reader = new ClassReader(bytes);
            reader.accept(classVisitor, 0);
            byte[] rewritten = classWriter.toByteArray();
            try {
                Files.write(rewritten, file);
            } catch (IOException e) {
                Extractor.error("Could not write " + file + ": " + e.getLocalizedMessage());
                mHaveError = true;
                //noinspection UnnecessaryContinue
                continue;
            }
        }
    }

    /**
     * Performs the actual deletion (or display, if in dry-run mode) of the typedef annotation
     * files
     */
    private void deleteAnnotationClasses() {
        for (File mFile : mAnnotationClassFiles) {
            if (mVerbose) {
                if (mDryRun) {
                    mExtractor.info("Would delete " + mFile);
                } else {
                    mExtractor.info("Deleting " + mFile);
                }
            }
            if (!mDryRun) {
                boolean deleted = mFile.delete();
                if (!deleted) {
                    Extractor.warning("Could not delete " + mFile);
                    mHaveError = true;
                }
            }
        }
    }

    /**
     * Visitor which visits .class files and checks whether each class is a typedef annotation
     * (and if so, calls {@link #addTypeDef(String, java.io.File)}
     */
    private class TypeDefVisitor extends ClassVisitor {

        /** Class file name */
        private File mFile;

        /** Class name */
        private String mName;

        /** Is this class an annotation? */
        private boolean mAnnotation;

        /** Is this annotation a typedef? Only applies if {@link #mAnnotation} */
        private boolean mTypedef;

        /** Does the annotation have source retention? Only applies if {@link #mAnnotation} */
        private boolean mSourceRetention;

        public TypeDefVisitor(File file) {
            super(ASM5);
            mFile = file;
        }

        @Override
        public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
            mName = name;
            mAnnotation = interfaces != null && interfaces.length >= 1
                    && ANNOTATION.equals(interfaces[0]);

            // Special case: Also delete the actual @IntDef and @StringDef .class files.
            // These have class file retention
            mTypedef = name.equals(INT_DEF) || name.equals(STRING_DEF);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            mTypedef = desc.equals(INT_DEF_DESC) || desc.equals(STRING_DEF_DESC);
            if (desc.equals(RETENTION_DESC)) {
                return new AnnotationVisitor(ASM5) {
                    @Override
                    public void visitEnum(String name, String desc, String value) {
                        if (desc.equals(RETENTION_POLICY_DESC)) {
                            mSourceRetention = SOURCE_RETENTION_VALUE.equals(value);
                        }
                    }
                };
            }
            return null;
        }

        @Override
        public void visitEnd() {
            if (mAnnotation && mTypedef) {
                if (!mSourceRetention && !mName.equals(STRING_DEF) && !mName.equals(INT_DEF)) {
                    Extractor.error(mFile + ": Warning: Annotation should be annotated "
                            + "with @Retention(RetentionPolicy.SOURCE)");
                    mHaveError = true;
                }

                addTypeDef(mName, mFile);
            }
        }
    }
}