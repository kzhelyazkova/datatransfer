package com.quickbase.datatransfer.common;

public interface ConfigPropertyProvider {
    String getConfigPropertyValue(String propertyName);
    String getConfigPropertyType();
}
