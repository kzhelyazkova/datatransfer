package com.quickbase.datatransfer.gateway.util;

import com.quickbase.datatransfer.exception.HttpRequestFailedException;
import com.quickbase.datatransfer.exception.UnauthorizedOperationException;
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

    public static String getAuthToken(String authTokenEnvVar, String errMsg, String externalSystemName) {
        String envVarValue = System.getenv(authTokenEnvVar);

        if (envVarValue == null) {
            throw new UnauthorizedOperationException(errMsg, externalSystemName);
        }

        return envVarValue;
    }

    public static Mono<ClientResponse> handleHttpError(ClientResponse response, String externalSystemName,
                                                       Function<HttpStatusCode, String> customHttpErrorCodeHandler) {
        HttpStatusCode httpStatus = response.statusCode();

        if (httpStatus.isError()) {
            return Mono.error(new HttpRequestFailedException(
                    customHttpErrorCodeHandler != null ? customHttpErrorCodeHandler.apply(httpStatus) : "",
                    externalSystemName, httpStatus));
        }

        return Mono.just(response);
    }
}
