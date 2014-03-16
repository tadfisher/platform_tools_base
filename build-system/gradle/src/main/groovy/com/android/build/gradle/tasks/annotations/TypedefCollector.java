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

import static com.android.SdkConstants.INT_DEF_ANNOTATION;
import static com.android.SdkConstants.STRING_DEF_ANNOTATION;

import com.android.annotations.NonNull;
import com.google.common.collect.Maps;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

import java.util.Collection;
import java.util.Map;

/** Gathers information about typedefs (@IntDef and @StringDef */
public class TypedefCollector extends ASTVisitor {
    private Map<String,Annotation> mMap = Maps.newHashMap();

    private boolean mRequireHide;
    private boolean mRequireSourceRetention;
    private CompilationUnitDeclaration mCurrentUnit;

    public TypedefCollector(
            @NonNull Collection<CompilationUnitDeclaration> units,
            boolean requireHide,
            boolean requireSourceRetention) {
        mRequireHide = requireHide;
        mRequireSourceRetention = requireSourceRetention;

        for (CompilationUnitDeclaration unit : units) {
            mCurrentUnit = unit;
            unit.traverse(this, unit.scope);
            mCurrentUnit = null;
        }
    }

    @NonNull
    public static Map<String,Annotation> collect(
            @NonNull Collection<CompilationUnitDeclaration> units,
            boolean requireHide,
            boolean requireSourceRetention) {
        TypedefCollector visitor = new TypedefCollector(units, requireHide,
                requireSourceRetention);
        return visitor.mMap;
    }

    @Override
    public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
        return recordTypedefs(memberTypeDeclaration);

    }

    @Override
    public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
        return recordTypedefs(typeDeclaration);
    }

    private boolean recordTypedefs(TypeDeclaration memberTypeDeclaration) {
        SourceTypeBinding binding = memberTypeDeclaration.binding;
        if (binding == null) {
            return false;
        }
        Annotation[] annotations = memberTypeDeclaration.annotations;
        if (annotations != null) {
            if (memberTypeDeclaration.binding.isAnnotationType()) {
                for (Annotation annotation : annotations) {
                    String typeName = Extractor.getFqn(annotation);
                    if (typeName == null) {
                        continue;
                    }

                    if (typeName.equals(INT_DEF_ANNOTATION) ||
                            typeName.equals(STRING_DEF_ANNOTATION) ||
                            typeName.equals(Extractor.ANDROID_INT_DEF) ||
                            typeName.equals(Extractor.ANDROID_STRING_DEF)) {
                        String fqn = new String(binding.readableName());
                        mMap.put(fqn, annotation);
                        if (mRequireHide) {
                            Javadoc javadoc = memberTypeDeclaration.javadoc;
                            if (javadoc != null) {
                                StringBuffer stringBuffer = new StringBuffer(200);
                                javadoc.print(0, stringBuffer);
                                String documentation = stringBuffer.toString();
                                if (!documentation.contains("@hide")) {
                                    System.out.println(getFileName()
                                            + ": The typedef annotation " + fqn
                                            + " should specify @hide in a doc comment");
                                }
                            }
                        }
                        if (mRequireSourceRetention
                                && !Extractor.hasSourceRetention(annotations)) {
                            System.out.println(getFileName()
                                    + ": The typedef annotation " + fqn
                                    + " must have @Retention(RetentionPolicy.SOURCE)");
                        }
                    }
                }
            }
        }
        return true;
    }

    private String getFileName() {
        return new String(mCurrentUnit.getFileName());
    }
}
