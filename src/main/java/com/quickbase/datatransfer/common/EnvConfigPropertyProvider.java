package com.quickbase.datatransfer.common;

import org.springframework.stereotype.Component;

@Component
public class EnvConfigPropertyProvider implements ConfigPropertyProvider {
    @Override
    public String getConfigPropertyValue(String propertyName) {
        return System.getenv(propertyName);
    }

    @Override
    public String getConfigPropertyType() {
        return "environment variable";
    }
}
