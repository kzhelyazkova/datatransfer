package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.common.DataType;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DataTransferService {
    /**
     * Transfers data from one external system to another.
     * <p/>
     * Downloads data from the desired data type from one external system, transforms it to a data format which the
     * other system recognizes and uploads it to the other system. What data should be downloaded/uploaded is based
     * on the specified source/dest params.
     *
     * @param sourceSystemType The type of system data would be downloaded from
     * @param destSystemType The type of system data would be uploaded to
     * @param dataType The type of data to be transferred
     * @param sourceParams The params identifying the data to be downloaded from the source system
     * @param destParams The params identifying the data to be uploaded to the destination system
     */
    Mono<Void> transferData(String sourceSystemType, String destSystemType, DataType dataType,
                            Map<String, String> sourceParams, Map<String, String> destParams);
}
