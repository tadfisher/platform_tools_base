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

package com.android.build.gradle.tasks.annotations;

import static org.objectweb.asm.Opcodes.ASM4;

import com.android.annotations.NonNull;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Transformer which inserts inserts annotation data (derived from source) into an existing
 * .jar file. For example it can be used
 */
@SuppressWarnings({"SpellCheckingInspection", "UnusedDeclaration"})
public class JarRewriter {
    @NonNull
    private final Extractor extractor;
    private int insertedParameterCount;
    private int insertedReturnValueCount;
    private int insertedFieldCount;

    public JarRewriter(@NonNull Extractor extractor) {
        this.extractor = extractor;
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void transformJar(@NonNull Extractor extractor, @NonNull File androidJar,
            @NonNull File outputJar) {
        try {
            new JarRewriter(extractor).transform(androidJar, outputJar);
        } catch (IOException e) {
            Extractor.error(e.toString());
        }
    }

    private void transform(File source, File dest) throws IOException {
        if (dest.exists()) {
            boolean deleted = dest.delete();
            if (!deleted) {
                Extractor.error("Could not delete " + dest);
                return;
            }
        }
        JarInputStream zis = null;
        FileInputStream fis = new FileInputStream(source);
        try {
            FileOutputStream fos = new FileOutputStream(dest);

            zis = new JarInputStream(fis);
            Manifest manifest = zis.getManifest();
            JarOutputStream zos = new JarOutputStream(fos, manifest);

            // TODO: Enable/disable compression:
            //zos.setLevel(9);

            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                transform(entry, zis, zos);
                entry = zis.getNextEntry();
            }
            zos.flush();
            Closeables.close(zos, false);

            String message = "Inserted annotations into " + source +
                    " and wrote the result into " + dest + "\n\n" +
                    insertedReturnValueCount + " method return value annotations inserted\n" +
                    insertedParameterCount + " method parameter annotations inserted\n" +
                    insertedFieldCount + " field annotations inserted\n";
            Extractor.display(message);
        } finally {
            Closeables.close(fis, true);
            Closeables.close(zis, true);
        }
    }

    private void transform(ZipEntry entry, JarInputStream zis, JarOutputStream zos)
            throws IOException {
        String name = entry.getName();

        JarEntry outEntry = new JarEntry(entry.getName());
        if (entry.getTime() != -1L) {
            outEntry.setTime(entry.getTime());
        }
        zos.putNextEntry(outEntry);

        if (!entry.isDirectory()) {
            byte[] bytes = ByteStreams.toByteArray(zis);
            if (bytes != null) {
                if (name.endsWith(".class")) {
                    // Rewrite class
                    ClassReader classReader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(ASM4);
                    ClassVisitor cv = new ClassVisitor(writer, classReader);
                    classReader.accept(cv, 0 /*flags*/);
                    bytes = writer.toByteArray();
                }

                zos.write(bytes);
            }
        }

        zos.closeEntry();
    }

    private class ClassVisitor extends org.objectweb.asm.ClassVisitor {
        private final ClassReader classReader;

        ClassVisitor(ClassWriter writer, ClassReader classReader) {
            super(ASM4, writer);
            this.classReader = classReader;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature,
                Object value) {
            FieldVisitor visitor = super.visitField(access, name, desc, signature, value);
            if (visitor != null) {
                String owner = classReader.getClassName();
                String fqn = JarRewriter.fromVmType(owner);
                String annotation = extractor.getFieldAnnotation(fqn, name);
                if (annotation != null) {
                    String vmName = 'L' + getJvmName(annotation) + ';';
                    org.objectweb.asm.AnnotationVisitor av = visitor.visitAnnotation(vmName, false);
                    av.visitEnd();
                    insertedFieldCount++;
                }
            }

            return visitor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                String[] exceptions) {
            MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
            if (visitor != null) {
                String owner = classReader.getClassName();
                String fqn = JarRewriter.fromVmType(owner);
                String returnType = JarRewriter
                        .fromVmType(desc.substring(desc.lastIndexOf(')') + 1));
                boolean isConstructor = name.equals("<init>");
                String methodName = isConstructor ? fqn.substring(fqn.lastIndexOf('.') + 1) : name;
                String parameterList = JarRewriter.getParameterList(desc);

                // Return value annotations
                String annotation = extractor.getMethodAnnotation(fqn, methodName, returnType,
                        parameterList, isConstructor, -1);
                if (annotation != null) {
                    String vmName = 'L' + getJvmName(annotation) + ';';
                    org.objectweb.asm.AnnotationVisitor av = visitor.visitAnnotation(vmName, false);
                    av.visitEnd();
                    insertedReturnValueCount++;
                }

                // Parameter annotations
                int argCount = getArgCount(desc);
                for (int i = 0; i < argCount; i++) {
                    annotation = extractor.getMethodAnnotation(fqn, methodName, returnType,
                            parameterList, isConstructor, i);
                    if (annotation != null) {
                        String vmName = 'L' + getJvmName(annotation) + ';';
                        org.objectweb.asm.AnnotationVisitor av = visitor.visitParameterAnnotation(i, vmName, false);
                        av.visitEnd();
                        insertedParameterCount++;
                    }
                }
            }
            return visitor;
        }
    }

    /**
     * Compute the argument count of the method given by the given JVM signature
     */
    static int getArgCount(String jvmSignature) {
        int argCount = 0;
        for (int k = 1, max = jvmSignature.length(); k < max; k++) {
            char c = jvmSignature.charAt(k);
            if (c == ')') {
                break;
            } else if (c != '[') { // ignore
                argCount++;
                if (c == 'L') {
                    while (k < max && jvmSignature.charAt(k) != ';') {
                        k++;
                    }
                }
            }
        }
        return argCount;
    }

    /**
     * Computes the internal class name of the given fully qualified class name.
     * For example, it converts foo.bar.Foo.Bar into foo/bar/Foo$Bar
     *
     * @param fqcn the fully qualified class name
     * @return the internal class name
     */
    @NonNull
    private static String getJvmName(@NonNull String fqcn) {
        if (fqcn.indexOf('.') == -1) {
            return fqcn;
        }

        StringBuilder sb = new StringBuilder(fqcn.length());
        String prev = null;
        for (String part : Splitter.on('.').split(fqcn)) {
            if (prev != null && !prev.isEmpty()) {
                if (Character.isUpperCase(prev.charAt(0))) {
                    sb.append('$');
                } else {
                    sb.append('/');
                }
            }
            sb.append(part);
            prev = part;
        }

        return sb.toString();
    }

    private static final Map<Character, String> PRIMITIVES = Maps.newHashMapWithExpectedSize(9);

    static {
        PRIMITIVES.put('I', "int");
        PRIMITIVES.put('Z', "boolean");
        PRIMITIVES.put('B', "byte");
        PRIMITIVES.put('C', "char");
        PRIMITIVES.put('D', "double");
        PRIMITIVES.put('F', "float");
        PRIMITIVES.put('J', "long");
        PRIMITIVES.put('S', "short");
        PRIMITIVES.put('V', "void");
    }

    private static String getParameterList(String methodDesc) {
        StringBuilder sb = new StringBuilder();
        for (int k = 1, max = methodDesc.length(); k < max; k++) {
            char c = methodDesc.charAt(k);
            if (c == '[') {
                continue; // TODO: Handle arrays; not yet needed
            } else if (c == ')') {
                break;
            }

            if (sb.length() > 0) {
                sb.append(',');
            }

            String s = PRIMITIVES.get(c);
            if (s != null) {
                sb.append(s);
                continue;
            }
            if (c == 'L') {
                int begin = k + 1;
                while (k < max && methodDesc.charAt(k) != ';') {
                    k++;
                }
                String type = methodDesc.substring(begin, k);
                String cls = fromVmType(type);
                sb.append(cls);
            } else {
                // Unexpected
                assert false : c;
            }
        }

        return sb.toString();
    }

    private static String fromVmType(String vmType) {
        if (vmType.length() == 1) {
            String s = PRIMITIVES.get(vmType.charAt(0));
            if (s != null) {
                return s;
            }
        }
        return Type.getObjectType(vmType).getClassName();
    }
}
