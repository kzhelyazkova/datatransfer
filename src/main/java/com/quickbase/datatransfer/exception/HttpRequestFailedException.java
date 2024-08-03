package com.quickbase.datatransfer.exception;

import org.springframework.http.HttpStatusCode;

public class HttpRequestFailedException extends RuntimeException {
    public String externalSystemName;
    public HttpStatusCode httpStatusCode;

    public HttpRequestFailedException(String errorMessage, String externalSystemName, HttpStatusCode httpStatusCode) {
        super(errorMessage);
        this.externalSystemName = externalSystemName;
        this.httpStatusCode = httpStatusCode;
    }
}
