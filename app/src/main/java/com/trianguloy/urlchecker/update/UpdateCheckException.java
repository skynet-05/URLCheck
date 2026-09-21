package com.trianguloy.urlchecker.update;

/** User-visible update check failure. */
public class UpdateCheckException extends Exception {

    public UpdateCheckException(String message) {
        super(message);
    }
}
