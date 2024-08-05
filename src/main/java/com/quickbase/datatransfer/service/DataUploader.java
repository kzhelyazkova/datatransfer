package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.data.BaseData;
import reactor.core.publisher.Mono;

import java.util.Map;

/*
 * This interface should be implemented for each external system type and data type to define the algorithm for
 * uploading data of this data type to the external system.
 */
public interface DataUploader<T extends BaseData> extends TransferrerTypeChecker {
    /**
     * Uploads data to external system.
     * <p/>
     * Receives data in generic format and transforms it to external-system-specific format.
     * Identifies where to upload it based on the specified params and uploads it.
     *
     * @param params The parameters used to identify where to upload the data. They might be specific to the external system and data type
     * @return a {@link Mono} which completes when the upload finishes. If something goes wrong during the upload process,
     * it will complete with an error
     */
    Mono<Void> uploadData(Map<String, String> params, T data);
}