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

package com.android.build.annotation.processor;

import com.android.build.annotation.BindingParameter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotation process to generate Setter classes for tasks that have @BindingParameter annotations
 * on some of their input fields.
 */
@SupportedAnnotationTypes("com.android.build.annotation.BindingParameter")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class BindingParameterProcessor extends AbstractProcessor {

    // so far, we have a very limited set of supported annotations.
    private final ImmutableSet<String> inputAnnotations = new ImmutableSet.Builder<String>()
            .add("@org.gradle.api.tasks.InputFile")
            .build();

    private final ImmutableSet<String> outputAnnotations = ImmutableSet.of(
            "@org.gradle.api.tasks.OutputFile");


    private  final Map<TypeElement, TaskDef> taskDefinitions = new HashMap<TypeElement, TaskDef>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of("*");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        if (!taskDefinitions.isEmpty()) {
            return false;
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "running");
        HashSet<TypeElement> annotationsOfInterest = new HashSet<TypeElement>();
        for (Element elem : roundEnvironment.getElementsAnnotatedWith(BindingParameter.class)) {
            BindingParameter complexity = elem.getAnnotation(BindingParameter.class);
            String message = "annotation found in " + elem.getSimpleName()
                    + " with binding parameter " + complexity;
            if (elem.getKind() == ElementKind.ANNOTATION_TYPE) {
                annotationsOfInterest.add((TypeElement) elem);
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
        }

        for (TypeElement annotation : annotationsOfInterest) {
            for (Element elem : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                String message = "real parameter annotation found in " + elem;
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
                for (AnnotationMirror annotationMirror : elem.getAnnotationMirrors()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "aso annotated with " + annotationMirror.toString());
                    TypeElement declaringClass = (TypeElement) elem.getEnclosingElement();
                    TaskDef taskDef = getTaskDefinition(declaringClass);
                    if (inputAnnotations.contains(annotationMirror.toString())) {
                        taskDef.inputFieldsPerType.put(annotation, elem);
                    }
                    if (outputAnnotations.contains(annotationMirror.toString())) {
                        taskDef.outputFieldsPerType.put(annotation, elem);
                    }
                }
            }
        }

        try {
            for (TaskDef taskDefinition : taskDefinitions.values()) {
                TypeElement targetClass = taskDefinition.typeElement;
                String packageName = targetClass.getEnclosingElement().toString();
                JavaFileObject mySetter = processingEnv.getFiler()
                        .createSourceFile(
                                packageName + "." + targetClass.getSimpleName() + "___Setter",
                                targetClass);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        mySetter.toString());
                Writer writer = mySetter.openWriter();
                /**
                 * This is pretty ugly, maybe consider using velocity of some other template based
                 * code generation tool.
                 */
                try {
                    PrintWriter printWriter = new PrintWriter(writer);
                    printWriter.println("package " + packageName + ";");
                    printWriter.println();

                    printWriter.println("import com.android.build.annotation.TaskSetter;");
                    printWriter.println("import org.gradle.api.Task;");
                    printWriter.println();
                    printWriter.println("import java.util.Map;");

                    printWriter
                            .println("public class " + targetClass.getSimpleName() + "___Setter "
                                    + "implements TaskSetter<" + targetClass.getSimpleName()
                                    + "> {");
                    printWriter.println();
                    printWriter.println(
                            "\tprivate static <T> T getTask(Task target, Class<T> taskType, Map<Class<?>, ?> tasks) {");
                    printWriter.println("\t\tT task = taskType.cast(tasks.get(taskType));");
                    printWriter.println("\t\tif (task != null) {");
                    printWriter.println("\t\t\ttarget.dependsOn(task);");
                    printWriter.println("\t\t}");
                    printWriter.println("\t\treturn task;");
                    printWriter.println("\t}");
                    printWriter.println();
                    printWriter.println("\tpublic void inject(" + targetClass.getSimpleName() + " target, "
                            + "Map<Class<?>, ?> tasks) {");
                    for (Map.Entry<TypeElement, Element> field : taskDefinition.inputFieldsPerType.entries()) {
                        Element providerElement = getProviderElement(field.getKey());
                        if (providerElement != null) {
                            printWriter.println(
                                    "\t\ttarget." + field.getValue().getSimpleName() + " = "
                                        + "getTask(target, "
                                            + providerElement.getEnclosingElement().getSimpleName()
                                            + ".class, tasks)."
                                                + providerElement.getSimpleName() + ";");
                        }
                    }
                    printWriter.println(("\t}"));
                    printWriter.println("}");
                    printWriter.flush();
                } finally {
                    writer.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false; // no further processing of this annotation type
    }

    Element getProviderElement(TypeElement annotation) {
        for (TaskDef taskDefinition : taskDefinitions.values()) {
            if (taskDefinition.outputFieldsPerType.containsKey(annotation)) {
                return taskDefinition.outputFieldsPerType.get(annotation);
            }
        }
        return null;
    }

    TaskDef getTaskDefinition(TypeElement typeElement) {
        TaskDef taskDef = taskDefinitions.get(typeElement);
        if (taskDef == null) {
            taskDef = new TaskDef(typeElement);
            taskDefinitions.put(typeElement, taskDef);
        }
        return taskDef;
    }

    private static class TaskDef {

        final TypeElement typeElement;
        final ListMultimap<TypeElement, Element> inputFieldsPerType = ArrayListMultimap.create();
        final Map<TypeElement, Element> outputFieldsPerType = new HashMap<TypeElement, Element>();

        private TaskDef(TypeElement typeElement) {
            this.typeElement = typeElement;
        }
    }
}
