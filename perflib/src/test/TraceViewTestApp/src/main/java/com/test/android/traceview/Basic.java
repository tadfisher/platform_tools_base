package com.test.android.traceview;

import android.os.Debug;

public class Basic {
    public static void start() {
        Debug.startMethodTracing("basic");
        foo();
        Debug.stopMethodTracing();
    }

    private static void foo() {
        bar();
    }

    private static void bar() {
        baz();
    }

    private static int baz() {
        return 42;
    }
}
