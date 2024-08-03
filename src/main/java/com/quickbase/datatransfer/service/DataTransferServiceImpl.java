package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.dto.BaseDTO;
import com.quickbase.datatransfer.exception.UnsupportedOperationException;
import com.quickbase.datatransfer.common.DataType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DataTransferServiceImpl implements DataTransferService {
    private final List<DataDownloader<? extends BaseDTO>> dataDownloaders;
    private final List<DataUploader<? extends BaseDTO>> dataUploaders;

    @Autowired
    public DataTransferServiceImpl(List<DataDownloader<? extends BaseDTO>> dataDownloaders,
                               List<DataUploader<? extends BaseDTO>> dataUploaders) {
        this.dataDownloaders = dataDownloaders;
        this.dataUploaders = dataUploaders;
        log.debug("Found the following data downloaders: {}", dataDownloaders);
        log.debug("Found the following data uploaders: {}", dataUploaders);
    }

    public Mono<Void> transferData(String sourceSystemType, String destSystemType, DataType dataType,
                             Map<String, String> sourceParams, Map<String, String> destParams) {
        return Mono.fromCallable(() -> findDataTransferrer(dataDownloaders, sourceSystemType, dataType, true))
                .flatMap(sourceSystemDataDownloader -> {
                    log.debug("Found suitable data downloader: {}", sourceSystemDataDownloader);

                    @SuppressWarnings("unchecked")
                    DataUploader<BaseDTO> destSystemDataUploader =
                            (DataUploader<BaseDTO>) findDataTransferrer(dataUploaders, destSystemType, dataType, false);
                    log.debug("Found suitable data uploader: {}", destSystemDataUploader);

                    return sourceSystemDataDownloader.downloadData(sourceParams)
                            .flatMap(downloadedData -> destSystemDataUploader.uploadData(destParams, downloadedData));
                })
                .doOnSuccess(__ -> log.info("Successfully transferred data of type '{}' from '{}' to '{}'",
                        dataType, sourceSystemType, destSystemType))
                .doOnError(ex -> log.error("Transferring data of type '{}' from '{}' to '{}' failed:",
                        dataType, sourceSystemType, destSystemType, ex));
    }

    private <T extends TransferrerTypeChecker> T findDataTransferrer(List<T> dataTransferrers, String systemType,
                                                                     DataType dataType, boolean isDownloader) {
        List<T> systemDataTransferrers = dataTransferrers.stream()
                .filter(transferrer -> transferrer.systemTypeMatches(systemType))
                .toList();

        if (systemDataTransferrers.isEmpty()) {
            log.error("No data {} for external system type '{}'",
                    isDownloader ? "downloaders" : "uploaders", systemType);
            throw new UnsupportedOperationException(String.format("Unsupported external system type '%s'", systemType));
        }

        return systemDataTransferrers.stream()
                .filter(downloader -> downloader.dataTypeMatches(dataType))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Data {} for data type '{}' and external system '{}' not found",
                            isDownloader ? "downloader" : "uploader", dataType, systemType);
                    return new UnsupportedOperationException(String.format(
                            "Unsupported data type '%s' for external system '%s'", dataType, systemType));
                });
    }
}
