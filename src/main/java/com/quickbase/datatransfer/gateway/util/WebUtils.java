package com.quickbase.datatransfer.gateway.util;

import com.quickbase.datatransfer.exception.HttpRequestFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static reactor.util.retry.Retry.RetrySignal;
import static reactor.util.retry.Retry.backoff;

@Slf4j
public class WebUtils {
    // in production code, these retry settings should be configurable
    public static final long RETRY_MAX_ATTEMPTS = 5;
    public static final Duration RETRY_MIN_BACKOFF = Duration.ofSeconds(2);
    public static final List<HttpStatus> RETRYABLE_HTTP_STATUSES = List.of(
            HttpStatus.REQUEST_TIMEOUT,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY
    );

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

    public static Retry retryWithDefaultSettings() {
        return backoff(RETRY_MAX_ATTEMPTS, RETRY_MIN_BACKOFF)
                .filter(throwable -> {
                    if (throwable instanceof HttpRequestFailedException ex) {
                        return RETRYABLE_HTTP_STATUSES.stream()
                                .anyMatch(status -> ex.httpStatusCode.value() == status.value());
                    }
                    return false;
                })
                .doBeforeRetry(sig -> {
                    RetrySignal signal = sig.copy();
                    Throwable err = signal.failure();
                    long currentRetries = signal.totalRetries();
                    log.info("Retrying on exception for the {}{} time out of {} times, Reason: {}",
                            currentRetries + 1, getOrdinalIndicator(currentRetries + 1), RETRY_MAX_ATTEMPTS, err.getMessage());
                })
                // propagate the original error, because the Retry API is wrapping it
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private static String getOrdinalIndicator(long number) {
        return switch ((int) number % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
