package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.dto.DataDTO;
import com.quickbase.datatransfer.dto.UserDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
public class DataTransferService {
    private final List<DataDownloader<? extends DataDTO>> dataDownloaders;

    @Autowired
    public DataTransferService(List<DataDownloader<? extends DataDTO>> dataDownloaders) {
        this.dataDownloaders = dataDownloaders;
    }

    public void transferData(String sourceSystemType, String destSystemType, String dataType,
                             Map<String, String> sourceParams, Map<String, String> destParams) {
        DataDownloader<? extends DataDTO> dataDownloader = dataDownloaders.stream()
                .filter(downloader -> downloader.test(new DataDownloaderIdentifier(sourceSystemType, dataType)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("unsupported external system type or data type"));

        DataDTO downloadedData = dataDownloader.downloadData(sourceParams);
        System.out.println("downloaded data: " + downloadedData);
        System.out.println("downloaded data username: " + ((UserDataDTO) downloadedData).username);
    }
}
