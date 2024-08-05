package com.quickbase.datatransfer.gateway.freshdesk.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class FreshdeskErrorResponse {
    public String description;
    public List<Error> errors;

    @AllArgsConstructor
    public static class Error {
        public String field;
        public String message;
        public String code;
    }
}
