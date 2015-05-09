package com.android.build.gradle.model;

import com.android.annotations.Nullable;
import com.android.build.gradle.internal.TaskFactory;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.model.collection.CollectionBuilder;

/**
 * Adaptor to transform CollectionBuilder<Task> into TaskFactory.
 */
public class TaskCollectionBuilderAdaptor implements TaskFactory {

    private final CollectionBuilder<Task> tasks;

    public TaskCollectionBuilderAdaptor(CollectionBuilder<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public boolean containsKey(String name) {
        return tasks.containsKey(name);
    }

    @Override
    public void create(String name) {
        tasks.create(name);
    }

    @Override
    public void create(String name, Action<? super Task> configAction) {
        tasks.create(name, configAction);
    }

    @Override
    public <S extends Task> void create(String name, Class<S> type) {
        tasks.create(name, type);
    }

    @Override
    public <S extends Task> void create(String name, Class<S> type,
            Action<? super S> configAction) {
        tasks.create(name, type, configAction);
    }

    @Override
    public void named(String name, Action<? super Task> configAction) {
        tasks.named(name, configAction);
    }

    @Nullable
    @Override
    public Task named(String name) {
        return tasks.get(name);
    }
}
