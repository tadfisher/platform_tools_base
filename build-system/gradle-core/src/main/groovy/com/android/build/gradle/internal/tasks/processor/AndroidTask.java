package com.android.build.gradle.internal.tasks.processor;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.concurrency.Immutable;
import com.android.build.annotation.BindingParameter;
import com.android.build.gradle.internal.TaskFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.OutputFiles;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for a {@link Task}, will allow us to keep interesting information like what tasks depend
 * on this task, the which ones this task depends on. Eventually we can tight things further to make
 * sure tasks are never retrieved using string based APIs.
 */
public class AndroidTask<T extends Task> {

    static final List<Class<? extends Annotation>> INPUT_ANNOTATIONS = ImmutableList.of(
            Input.class,
            InputFile.class,
            InputFiles.class,
            InputDirectory.class);

    static final List<Class<? extends Annotation>> OUTPUT_ANNOTATIONS = ImmutableList.of(
            OutputFile.class,
            OutputFiles.class,
            OutputDirectories.class,
            OutputDirectory.class);

    @NonNull
    private final String taskName;
    @Nullable
    private T task;
    @NonNull
    private final List<AndroidTask<? extends Task>> upstreamTasks = new ArrayList<AndroidTask<? extends Task>>();
    @NonNull
    private final List<AndroidTask<? extends Task>> downstreamTasks = new ArrayList<AndroidTask<? extends Task>>();
    private boolean configured = false;
    private boolean userConfigured = false;
    @NonNull
    private final Multimap<Field, Class<?>> inputFields = ArrayListMultimap.create();
    @NonNull
    private Set<Class<?>> bindingInputs = Sets.newHashSet();
    @NonNull
    private final Map<Class<?>, Field> outputFields = Maps.newHashMap();

    public AndroidTask(@NonNull String taskName, @NonNull Class<T> taskType) {
        this.taskName = taskName;
        introspect(taskType);
    }

    public String getName() {
        return taskName;
    }

    public void named(TaskFactory tasks, Action<? super Task> configAction) {
        tasks.named(taskName, configAction);
    }

    public void setTask(@NonNull T task) {
        this.task = task;
    }

    @NonNull
    public String getTaskName() {
        return taskName;
    }

    @Nullable
    public T getTask() {
        return task;
    }

    @NonNull
    public List<AndroidTask<? extends Task>> getUpstreamTasks() {
        return upstreamTasks;
    }

    @NonNull
    public List<AndroidTask<? extends Task>> getDownstreamTasks() {
        return downstreamTasks;
    }

    /**
     * Determine all the input and output fields from annotations.
     */
    private void introspect(Class<?> cls) {
        for (Field f : cls.getDeclaredFields()) {
            boolean isInput = false;
            boolean isOutput = false;

            List<Class<? extends Annotation>> bindingAnnotations = Lists.newArrayList();
            for (Annotation a : f.getAnnotations()) {
                if (INPUT_ANNOTATIONS.contains(a.annotationType())) {
                    isInput = true;
                }
                if (OUTPUT_ANNOTATIONS.contains(a.annotationType())) {
                    isOutput = true;
                }
                BindingParameter bindingParameter =
                        a.annotationType().getAnnotation(BindingParameter.class);
                if (bindingParameter != null) {
                    System.out.println("Binding Parameter for " + a + ": " + f.getName());
                    bindingAnnotations.add(a.annotationType());
                }
            }

            for (Class<?> a: bindingAnnotations) {
                if (isInput) {
                    System.out.println("  Found input for " + a + ": " + f.getName());
                    bindingInputs.add(a);
                    inputFields.put(f, a);
                }
                if (isOutput) {
                    System.out.println("  Found output for " + a + ": " + f.getName());
                    outputFields.put(a, f);
                }
            }
            /*
            BindingInput bindingInput = f.getAnnotation(BindingInput.class);
            if (bindingInput != null) {
                System.out.println("Found input for " + bindingInput.value() + ": " + f.getName());
                inputFields.put(bindingInput.value(), f);
            }

            BindingOutput bindingOutput = f.getAnnotation(BindingOutput.class);
            if (bindingOutput != null) {
                System.out
                        .println("Found output for " + bindingOutput.value() + ": " + f.getName());
                outputFields.put(bindingOutput.value(), f);
            }
            */
        }

        // Also find the fields in parent classes.
        Class<?> parentClass = cls.getSuperclass();
        if (parentClass != null) {
            introspect(parentClass);
        }
    }

    /**
     * Return a set of inputs types in this task.
     */
    @NonNull
    public Set<Class<?>> getBindingInputs() {
        return bindingInputs;
    }

    /**
     * Return a set of output types in this task.
     */
    @NonNull
    public Set<Class<?>> getBindingOutputs() {
        return outputFields.keySet();
    }

    /**
     * Return the value for the field associated with the specified output type.
     */
    public Object getOutput(Class<?> type) {
        Field f = outputFields.get(type);
        try {
            f.setAccessible(true);
            return f.get(task);
        } catch (Exception e) {
            throw new RuntimeException("Exception thrown when getting field: " + e.toString());
        }
    }

    /**
     * this should be called directly by the annotation processor code. As you can see, right now,
     * the dependency is not declared to gradle, just in this internal model. The gradle dependency
     * is added during field wiring.
     *
     * @param other an upstream task.
     */
    public void dependsOn(@NonNull AndroidTask<? extends Task> other) {
        upstreamTasks.add(other);
        other.addDependent(this);
    }

    private void addDependent(AndroidTask<? extends Task> tAndroidTask) {
        downstreamTasks.add(tAndroidTask);
    }

    private void setTaskDependencies() {
        for (AndroidTask<? extends Task> upstreamTask : upstreamTasks) {
            // task should be created at this point.
            assert task != null;
            task.dependsOn(upstreamTask.taskName);
        }
    }

    /**
     * Bind input fields using reflection.
     *
     * TODO: Create annotation processor to generate setter.
     */
    private void configureWithIntrospection() {
        for (Field inputField : inputFields.keySet()) {
            for (Class<?> inputClass : inputFields.get(inputField)) {
                boolean isFieldSet = false;
                for (AndroidTask<? extends Task> upstreamTask : upstreamTasks) {
                    if (upstreamTask.getBindingOutputs().contains(inputClass)) {
                        try {
                            inputField.setAccessible(true);
                            inputField.set(task, upstreamTask.getOutput(inputClass));
                            isFieldSet = true;
                            break;
                        } catch (Exception e) {
                            throw new RuntimeException(
                                    "Exception thrown when setting field: " + e.toString());
                        }
                    }
                }
                if (isFieldSet) {
                    break;
                }
            }
        }
    }

    public void configure() {
        if (configured) {
            return;
        }
        if (!userConfigured) {
            // Task dependencies must be set even if the upstream tasks are not configured yet.
            setTaskDependencies();
        }

        userConfigured = true;

        // if all my upstream tasks are configured, I can configure myself.
        for (AndroidTask<? extends Task> upstreamTask : upstreamTasks) {
            if (!upstreamTask.configured) {
                return;

            }
        }

        configureWithIntrospection();
        configured = true;
        // now that I am configured, let's give a chance to my dependents.
        for (AndroidTask<? extends Task> downstreamTask : downstreamTasks) {
            if (downstreamTask.userConfigured) {
                downstreamTask.configure();
            }
        }
    }
}
