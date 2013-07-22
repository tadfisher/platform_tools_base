package com.android.tools.perflib.vmtrace;

public class MethodInfo {
    public final long id;
    public final String className;
    public final String methodName;
    public final String signature;
    public final String srcPath;
    public final int srcLineNumber;

    public MethodInfo(long id, String className, String methodName, String signature, String srcPath,
            int srcLineNumber) {
        this.id = id;
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.srcPath = srcPath;
        this.srcLineNumber = srcLineNumber;
    }
}
