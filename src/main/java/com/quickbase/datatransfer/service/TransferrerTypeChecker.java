package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.common.DataType;

/*
 * Each data downloader/uploader should implement this interface to enable its consumers to understand
 * what data type and external system type it works with. This allows consumers to find the appropriate
 * downloader/uploader without coupling to it.
 */
public interface TransferrerTypeChecker {
    boolean dataTypeMatches(DataType dataType);
    boolean systemTypeMatches(String systemType);
}
