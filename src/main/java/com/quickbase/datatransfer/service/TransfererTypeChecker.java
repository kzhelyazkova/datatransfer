package com.quickbase.datatransfer.service;

public interface TransfererTypeChecker {
    boolean dataTypeMatches(String dataType);
    boolean systemTypeMatches(String systemType);
}
