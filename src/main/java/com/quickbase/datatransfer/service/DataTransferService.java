package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.dto.DataDTO;
import com.quickbase.datatransfer.dto.UserDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;


@Service
public class DataTransferService {
    private final List<DataDownloader<? extends DataDTO>> dataDownloaders;
    private final List<DataUploader<? extends DataDTO>> dataUploaders;

    @Autowired
    public DataTransferService(List<DataDownloader<? extends DataDTO>> dataDownloaders,
                               List<DataUploader<? extends DataDTO>> dataUploaders) {
        this.dataDownloaders = dataDownloaders;
        this.dataUploaders = dataUploaders;
        System.out.println("data downloaders: " + dataDownloaders);
        System.out.println("data uploaders: " + dataUploaders);
    }

    @SuppressWarnings("unchecked")
    public Mono<Void> transferData(String sourceSystemType, String destSystemType, String dataType,
                             Map<String, String> sourceParams, Map<String, String> destParams) {
        DataDownloader<? extends DataDTO> sourceSystemDataDownloader = findDataTransferer(dataDownloaders, sourceSystemType, dataType);
        System.out.println("data downloader: " + sourceSystemDataDownloader);

        Mono<? extends DataDTO> dataForTransfer = sourceSystemDataDownloader.downloadData(sourceParams);
        System.out.println("downloaded data: " + dataForTransfer);
        // System.out.println("downloaded data username: " + ((UserDataDTO) dataForTransfer).username);

        DataUploader<DataDTO> destSystemDataUploader = (DataUploader<DataDTO>) findDataTransferer(dataUploaders, destSystemType, dataType);
        System.out.println("data uploader: " + destSystemDataUploader);
        return dataForTransfer
                .flatMap(downloadedData -> destSystemDataUploader.uploadData(destParams, downloadedData));
    }

    private <T extends TransfererTypeChecker> T findDataTransferer(List<T> dataTransferers, String systemType, String dataType) {
        List<T> systemDataTransferers = dataTransferers.stream()
                .filter(transferer -> transferer.systemTypeMatches(systemType))
                .toList();

        if (systemDataTransferers.isEmpty()) {
            throw new RuntimeException("Unsupported external system type: " + systemType);
        }

        return systemDataTransferers.stream()
                .filter(downloader -> downloader.dataTypeMatches(dataType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported data type: " + dataType + " for external system: " + systemType));
    }
}
