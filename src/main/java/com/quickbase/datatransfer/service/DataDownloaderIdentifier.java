package com.quickbase.datatransfer.service;

public class DataDownloaderIdentifier {
    public String systemType;
    public String dataType;

    public DataDownloaderIdentifier(String systemType, String dataType) {
        this.systemType = systemType;
        this.dataType = dataType;
    }
}
