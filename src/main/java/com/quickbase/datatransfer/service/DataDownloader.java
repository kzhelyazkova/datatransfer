package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.dto.DataDTO;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DataDownloader<T extends DataDTO> extends TransfererTypeChecker {
    Mono<T> downloadData(Map<String, String> params);
}
