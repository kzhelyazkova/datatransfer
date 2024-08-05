package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.data.BaseData;
import reactor.core.publisher.Mono;

import java.util.Map;

/*
 * This interface should be implemented for each external system type and data type to define the algorithm for
 * downloading data of this data type from the external system.
 */
public interface DataDownloader<T extends BaseData> extends TransferrerTypeChecker {
    /**
     * Downloads data from external system.
     * <p/>
     * Identifies the data that needs to be downloaded based on the specified params, downloads it and
     * transforms it to a generic format.
     *
     * @param params The parameters used to identify the data. They might be specific to the external system and data type
     * @return a {@link Mono} emitting the downloaded data in a generic format
     */
    Mono<T> downloadData(Map<String, String> params);
}
