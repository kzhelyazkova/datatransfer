package com.quickbase.datatransfer.exception;

public class MissingExternalSystemParamException extends RuntimeException {
    public String param;
    public String externalSystemName;

    public MissingExternalSystemParamException(String errorMessage, String param, String externalSystemName) {
        super(errorMessage);
        this.param = param;
        this.externalSystemName = externalSystemName;
    }
}
