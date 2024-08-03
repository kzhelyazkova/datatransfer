package com.quickbase.datatransfer.gateway.freshdesk.model;

import java.util.List;

public class FreshdeskErrorResponse {
    public String description;
    public List<Error> errors;

    public static class Error {
        public String field;
        public String message;
        public String code;
    }
}
