package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.dto.DataDTO;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DataUploader<T extends DataDTO> extends TransfererTypeChecker {
    Mono<Void> uploadData(Map<String, String> params, T data);
}