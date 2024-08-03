package com.quickbase.datatransfer.exception;

public class AmbiguousDataException extends RuntimeException {
    public String externalSystemName;

    public AmbiguousDataException(String errorMessage, String externalSystemName) {
        super(errorMessage);
        this.externalSystemName = externalSystemName;
    }
}
