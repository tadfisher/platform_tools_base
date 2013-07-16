package com.android.ide.common.internal;

/**
 * Created with IntelliJ IDEA. User: xav Date: 7/16/13 Time: 4:46 PM To change this template use
 * File | Settings | File Templates.
 */
public class LoggedErrorException extends Exception {

    public LoggedErrorException() {
    }

    public LoggedErrorException(String s) {
        super(s);
    }

    public LoggedErrorException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LoggedErrorException(Throwable throwable) {
        super(throwable);
    }
}
