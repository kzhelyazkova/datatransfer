package com.quickbase.datatransfer.exception;

public class UnauthorizedOperationException extends RuntimeException {
    public String externalSystemName;

    public UnauthorizedOperationException(String errorMessage, String externalSystemName) {
        super(errorMessage);
        this.externalSystemName = externalSystemName;
    }
}
