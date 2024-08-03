package com.quickbase.datatransfer.exception;

public class InvalidParamException extends RuntimeException {
    public String param;

    public InvalidParamException(String errorMessage, String param) {
        super(errorMessage);
        this.param = param;
    }
}
