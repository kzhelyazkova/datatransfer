package com.quickbase.datatransfer.gateway.util;

import com.quickbase.datatransfer.exception.HttpRequestFailedException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class WebUtils {

    public static String urlEncode(String value) {
        if (value == null) {
            return null;
        }

        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static Mono<ClientResponse> handleHttpError(ClientResponse response, String externalSystemName,
                                                       Function<HttpStatusCode, String> customHttpErrorCodeToMessageMapper) {
        HttpStatusCode httpStatus = response.statusCode();

        if (httpStatus.isError()) {
            return Mono.error(new HttpRequestFailedException(
                    customHttpErrorCodeToMessageMapper != null ? customHttpErrorCodeToMessageMapper.apply(httpStatus) : "",
                    externalSystemName, httpStatus));
        }

        return Mono.just(response);
    }
}
