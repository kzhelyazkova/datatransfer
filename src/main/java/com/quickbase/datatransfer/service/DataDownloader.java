package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.dto.DataDTO;

import java.util.Map;
import java.util.function.Predicate;

public interface DataDownloader<T extends DataDTO> extends Predicate<DataDownloaderIdentifier> {
    T downloadData(Map<String, String> params);
}
