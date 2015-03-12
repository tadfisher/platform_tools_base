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

package com.android.build.gradle.internal.tasks.processor;

import static com.google.common.truth.Truth.assertThat;

import com.android.build.annotation.BindingInput;
import com.android.build.annotation.BindingOutput;
import com.android.build.annotation.BindingParameter;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test for AndroidTask.
 */
public class AndroidTaskTest {

    @BindingParameter
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Type1 {};

    @BindingParameter
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Type2 {};

    static class Task1 extends DefaultTask {
        @Input
        @Type1
        String input;

        @OutputFile
        @Type2
        String output;
    }

    static class Task2 extends DefaultTask {
        @Input
        @Type1
        private String input;

        @OutputFile
        @Type2
        private String output;
    }

    static class Task3 extends Task2 {
    }

    @Test
    public void introspection() {
        AndroidTask task = new AndroidTask<Task1>("task1", Task1.class);
        assertThat(task.getBindingInputs()).containsExactly(Type1.class);
        assertThat(task.getBindingOutputs()).containsExactly(Type2.class);
    }

    @Test
    public void introspectionOnTaskWithPrivateFields() {
        AndroidTask task = new AndroidTask<Task2>("task1", Task2.class);
        assertThat(task.getBindingInputs()).containsExactly(Type1.class);
        assertThat(task.getBindingOutputs()).containsExactly(Type2.class);
    }

    @Test
    public void introspectionOnInheritedFields() {
        AndroidTask task = new AndroidTask<Task3>("task1", Task3.class);
        assertThat(task.getBindingInputs()).containsExactly(Type1.class);
        assertThat(task.getBindingOutputs()).containsExactly(Type2.class);
    }
}
